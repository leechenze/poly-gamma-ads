// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.core;

import static org.polygamma.android.origin.protobuf.Protobuf.WIRE_LEN;
import static org.polygamma.android.origin.protobuf.Protobuf.fieldTagOf;

import android.content.Context;
import android.net.http.HttpEngine;
import android.os.Build;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Base64;
import android.util.Pair;

import androidx.annotation.GuardedBy;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;

import com.google.common.util.concurrent.ListenableFuture;

import org.polygamma.android.origin.protobuf.Protobuf.FieldTag;
import org.polygamma.android.origin.protobuf.ProtobufDecoder;
import org.polygamma.android.origin.protobuf.ProtobufEncoder;
import org.polygamma.android.origin.protobuf.ProtobufSerializable;
import org.polygamma.android.origin.util.CollectionsCompat;
import org.polygamma.android.origin.util.ExecutingService;
import org.polygamma.android.origin.util.Flate;
import org.polygamma.android.origin.util.Function;
import org.polygamma.android.origin.util.ListenableFutureTask;
import org.polygamma.android.origin.util.Logger;
import org.polygamma.android.origin.util.Preconditions;
import org.polygamma.android.origin.util.Strings;
import org.polygamma.android.origin.util.Supplier;
import org.polygamma.android.origin.util.Time;

import java.io.IOException;
import java.net.HttpRetryException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Pattern;
import java.util.zip.Deflater;

/**
 * Module providing remote procedure call (RPC) of Origin services.
 * <p>This module provides access to remote Origin services. The default {@linkplain #ofProvider()
 * provider} configures services host dynamically by default. To use an explicit host, set
 * {@linkplain Provider#host(String) host} and, optionally, {@linkplain Provider#port(int) port}.
 * <p>HTTP connections are used to transport call requests and responses. By default, HTTP/3 is
 * used whenever available, using the {@linkplain HttpEngine Cronet} HTTP engine; otherwise, HTTP/1
 * or HTTP/2 is used based on platform and available features.
 * <p>Remote procedures can be invoked using the {@link
 * #call(String, ProtobufSerializable, Function, long, TimeUnit)} family of
 * methods.
 *
 * @since 1.0
 */
public class RpcModule extends OriginModule {

	private static final String TAG = RpcModule.class.getSimpleName();

	/**
	 * Network module name.
	 *
	 * @since 1.0
	 */
	public static final String NAME = "origin.rpc";

	// Service {@linkplain #SETTINGS_RECORD records} were resolved for.
	private static final @FieldTag int SETTINGS_SERVICE	= fieldTagOf(1, WIRE_LEN);

	// Hostname {@linkplain #SETTINGS_RECORD records} were resolved for.
	private static final @FieldTag int SETTINGS_HOST	= fieldTagOf(2, WIRE_LEN);

	// {@linkplain RpcHostRecord Host record}.
	private static final @FieldTag int SETTINGS_RECORD	= fieldTagOf(3, WIRE_LEN);

	// Service and procedure name pattern.
	private static final Pattern SERVICE_AND_PROCEDURE_NAME_PATTERN =
		Pattern.compile("^[_A-Za-z][_A-Za-z0-9]*$");

	// Service version pattern.
	private static final Pattern SERVICE_VERSION_PATTERN =
		Pattern.compile("^[1-9][0-9]*\\.[0-9]+$");

	/*
	 * Procedure ids are composed of a service name, optional service version, and procedure name,
	 * delimited by forward slash `/`.
	 */
	private static final Pattern PROCEDURE_ID_PATTERN =
		Pattern.compile("^/([_A-Za-z][_A-Za-z0-9]*)(/[1-9][0-9]*\\.[0-9]+)?/([_A-Za-z][_A-Za-z0-9]*)$");

	// Default RPC request timeout, in milliseconds.
	private static final long DEFAULT_CALL_REQUEST_TIMEOUT_MILLIS = TimeUnit.SECONDS.toMillis(30);

	// Timeout, in milliseconds, of DNS queries.
	private static final long DNS_QUERY_TIMEOUT_MILLIS = TimeUnit.SECONDS.toMillis(15);

	// Maximum length of RPC call URL path, in bytes, which may be invoked using a {@code GET}.
	@VisibleForTesting
	static final int HTTP_GET_CALL_PATH_THRESHOLD = 2048;

	// Maximum number of HTTP redirects that may be followed before an RPC call is aborted.
	private static final int MAX_CALL_HTTP_REDIRECT_COUNT = 5;

	// Maximum number of times an RPC call can be retried.
	@VisibleForTesting
	static final int MAX_CALL_RETRY_COUNT = 3;

	// Remote procedure call request.
	@VisibleForTesting
	final class CallRequest extends ListenableFutureTask<Object> {

		final long id;
		final long expireTimestampMillis;
		final String procedureId;
		final @Nullable ByteBuffer arguments;
		final boolean argumentsDeflated;
		final boolean useHttpGet;
		final @Nullable Function<ProtobufDecoder, ?> resultDeserializer;

		/*
		 * Either `null`, `HttpRequest`, `ListenableFuture<?>` or `this` if request has not yet
		 * started, HTTP request call is being made with, future of when request will start, or
		 * request is done, respectively.
		 */
		@GuardedBy("this")
		@Nullable Object state;

		/*
		 * Either `null`, `ByteBuffer`, or `ProtoMessage` if no result, result is being buffered,
		 * or result has been received and deserialized.
		 */
		@Nullable Object result;

		// record of host request was made to
		@Nullable RpcHostRecord hostRecord;

		// number of times HTTP request has been redirected
		int httpRedirectCount;
		// number of times HTTP request has been retried
		int httpRetryCount;

		// Construct a new call request.
		CallRequest(
			long id,
			long timeoutMillis,
			String procId,
			@Nullable ByteBuffer args,
			@Nullable Function<ProtobufDecoder, ?> resDeser
		) {
			this.id = id;
			this.expireTimestampMillis = SystemClock.uptimeMillis() + timeoutMillis;
			this.procedureId = procId;
			this.resultDeserializer = resDeser;

			if (args == null) {
				this.arguments = null;
				this.argumentsDeflated = false;
				this.useHttpGet = true;
			} else {
				long basePathLen = procId.length() + 1;
				boolean get =
					(basePathLen + estimateBase64CodingOf(args.remaining())) <=
					HTTP_GET_CALL_PATH_THRESHOLD;
				boolean deflate = false;

				if (!get) {
					ByteBuffer comp =
						Flate.compressZlib(args.duplicate(), Deflater.BEST_COMPRESSION, false);

					deflate = comp.remaining() < args.remaining();
					if (deflate) {
						args = comp;
						get =
							(basePathLen + estimateBase64CodingOf(args.remaining())) <=
							HTTP_GET_CALL_PATH_THRESHOLD;
					}
				}

				this.argumentsDeflated = deflate;
				this.useHttpGet = get;
				this.arguments = args;
			}
		}

		// Service name.
		String serviceName() {
			return this.procedureId.substring(1, this.procedureId.indexOf('/', 1));
		}

		// Owning module.
		RpcModule module() {
			return RpcModule.this;
		}

		private void releaseState(@Nullable Object state) {
			if (state instanceof HttpRequest)
				((HttpRequest) state).cancel();
			else if (state instanceof ListenableFuture<?>)
				((ListenableFuture<?>) state).cancel(false);
		}

		/*
		 * Try and update request state to starting. If this returns `true`, then it's guaranteed
		 * that `http` has been sent; otherwise, `false` if request is already done.
		 */
		boolean tryStart(HttpRequest.Builder http) {
			Object state;

			synchronized (this) {
				state = this.state;
				if (state == this)
					return false;
				this.httpRedirectCount = 0;
				this.state = http.send();
			}
			this.releaseState(state);
			return true;
		}

		/*
		 * Try and clear request state. Returns `true` if state was cleared; otherwise, `false` if
		 * request is already done.
		 */
		boolean tryClear() {
			synchronized (this) {
				if (this.state == this)
					return false;
				this.state = null;
			}
			return true;
		}

		/*
		 * Complete request with `res`, where `res` is `null`, result value, or a `Throwable` if
		 * request completed successfully without a result, completed successfully with a result,
		 * or failed, respectively.
		 */
		void complete(@Nullable Object res) {
			RpcHostRecord rec = this.hostRecord;
			if (res instanceof Throwable) {
				if (rec != null && res instanceof IOException)
					rec.updateLastFailureTimestamp();
				super.setException((Throwable) res);
			} else {
				if (rec != null)
					rec.clearLastFailureTimestamp();
				super.set(res);
			}
		}

		// Complete request with timeout.
		void timeout() {
			this.complete(new TimeoutException());
		}

		@Override
		public void run() {
			synchronized (this) {
				if (!(this.state instanceof ListenableFuture<?>))
					return;
				this.state = null;
			}
			RpcModule.this.sendCall(this, null);
		}

		@Override
		protected void done() {
			Object state;

			synchronized (this) {
				state = this.state;
				this.state = this;
			}

			try {
				super.done();
				this.releaseState(state);
			} finally {
				RpcModule.this.onCallDone(this);
			}
		}

		@Override
		public int hashCode() {
			return Long.hashCode(this.id);
		}

		@Override
		public boolean equals(@Nullable Object that) {
			return that instanceof CallRequest && ((CallRequest) that).id == this.id;
		}

		@Override
		public String toString() {
			return String.format(Locale.ROOT, "%s@%d", this.procedureId, this.id);
		}
	}

	/**
	 * Remote procedure call {@linkplain RpcModule module} provider.
	 *
	 * @since 1.0
	 * @see #ofProvider()
	 */
	public static final class Provider extends OriginModule.Provider<RpcModule> {
		private boolean insecure;
		private @Nullable String host;
		private int port;

		private Provider() {
			super(RpcModule.class);
		}

		/**
		 * Set whether insecure (non-HTTPS) connections should be used.
		 *
		 * @param use {@code true} if, and only if, insecure connections should be used
		 * @return {@code this}
		 * @since 1.0
		 */
		public Provider insecure(boolean use) {
			this.insecure = use;
			return this;
		}

		/**
		 * Set root host to invoke service procedures in.
		 * <p>If namespaced services are supported by the host, then {@code host} <i>should</i>
		 * contain a single {@code %s}, which is substituted with the service name during procedure
		 * invocation.
		 *
		 * @param host root host or, {@code null} or {@linkplain String#isEmpty() empty} for
		 * default
		 * @return {@code this}
		 * @since 1.0
		 */
		public Provider host(@Nullable String host) {
			this.host = Strings.emptyToNull(host);
			return this;
		}

		/**
		 * Set port to access service host on.
		 *
		 * @param port port to access host at or {@code 0} for default
		 * @return {@code this}
		 * @throws IllegalArgumentException {@code port} is invalid
		 * @since 1.1
		 */
		public Provider port(int port) {
			Preconditions.checkArgument((port & 0xffff) == port);
			this.port = port;
			return this;
		}

		@Override
		protected RpcModule load(Origin sdk, Context ctxt) {
			RpcModule module = new RpcModule(sdk);

			this.reload(module, ctxt);
			return module;
		}

		@Override
		protected void reload(RpcModule module, Context ctxt) {
			module.insecure = this.insecure;
			module.host = this.host;
			module.port = this.port;
			module.lock.readLock().lock();
			try {
				module.processor.schedule();
			} finally {
				module.lock.readLock().unlock();
			}
		}
	}

	// Calculate number of bytes that would be used to base64 encode a byte sequence `size`.
	@VisibleForTesting
	static long estimateBase64CodingOf(int size) {
		return ((size * 8L) + 6 - 1) / 6;
	}

	/*
	 * Retrieve RPC request for an HTTP request `http`. If `http` is not associated with an RPC
	 * request, this fails with `IllegalArgumentException`.
	 */
	private static CallRequest callRequestOf(HttpRequest http) {
		CallRequest call = (CallRequest) http.attachment();

		//noinspection SynchronizationOnLocalVariableOrMethodParameter,DataFlowIssue
		synchronized (call) {
			Preconditions.checkArgument(call.state == http, "call detached from HTTP request");
		}
		return call;
	}

	// Remote procedure call request HTTP lifecycle listener.
	private static final HttpRequest.Listener
	CALL_HTTP_REQUEST_LISTENER = new HttpRequest.Listener() {
		@Override
		public void onFailed(HttpRequest http, Throwable cause, boolean recov) {
			CallRequest call = callRequestOf(http);

			if (!recov)
				call.complete(cause);
			else if (call.tryClear())
				call.module().sendCall(call, cause);
		}

		@Override
		public void onCancel(HttpRequest http) {
			CallRequest call = (CallRequest) http.attachment();

			if (call != null) {
				//noinspection SynchronizationOnLocalVariableOrMethodParameter
				synchronized (call) {
					if (call.state != http)
						return;
					call.state = null;
				}
				call.cancel(false);
			}
		}

		@Override
		public boolean onRedirect(
			HttpRequest http,
			URL url,
			int statusCode,
			String statusMsg,
			Supplier<Map<String, List<String>>> resolveHdrs
		) {
			CallRequest call = callRequestOf(http);

			if (call.httpRedirectCount >= MAX_CALL_HTTP_REDIRECT_COUNT) {
				if (call.tryClear())
					call.complete(new HttpRetryException(statusMsg, statusCode, url.toString()));
				return false;
			}
			call.httpRedirectCount++;
			return true;
		}

		@Override
		public void onResponseStart(
			HttpRequest http,
			int statusCode,
			String statusMsg,
			Supplier<Map<String, List<String>>> resolveHdrs
		) {
			CallRequest call = callRequestOf(http);

			call.result = null;
			if (statusCode == 200 || statusCode == 204)
				return;

			try {
				if (!call.tryClear())
					return;
				if (statusCode == 408 || statusCode == 429 || statusCode >= 500) {
					call.module()
						.sendCall(call, new HttpRetryException(statusMsg, statusCode));
				} else {
					call.complete(new IllegalStateException(String.format(
						Locale.ROOT,
						"invalid status %s: %s",
						statusCode,
						statusMsg
					)));
				}
			} finally {
				http.cancel();
			}
		}

		@Override
		public void onResponseBodyPart(HttpRequest http, ByteBuffer part, boolean last) {
			CallRequest call = callRequestOf(http);

			if (call.resultDeserializer == null)
				return;

			ByteBuffer body = (ByteBuffer) call.result;

			if (body == null && last) {
				body = part;
			} else if (body == null) {
				body = ByteBuffer.allocate(Math.max(part.remaining() + 2048, 4096))
					.put(part);
				call.result = body;
				return;
			} else {
				if (body.remaining() < part.remaining()) {
					int newCap = Math.max(body.capacity() + part.remaining(), body.capacity() * 2);

					body = ByteBuffer.allocate(newCap)
						.put((ByteBuffer) body.flip());
					call.result = body;
				}
				body.put(part);
				if (!last)
					return;
				body.flip();
			}
			call.result = call.resultDeserializer.apply(ProtobufDecoder.ofBuffer(body));
		}

		@Override
		public void onSuccess(HttpRequest http) {
			CallRequest call = callRequestOf(http);

			call.complete(call.result);
		}
	};

	/**
	 * Construct new default module provider.
	 *
	 * @return default provider
	 * @since 1.0
	 */
	public static Provider ofProvider() {
		return new Provider();
	}

	/**
	 * Generate id of a remote procedure within a service.
	 *
	 * @param svc service in which procedure is defined
	 * @param name name of procedure
	 * @param ver version of service procedure is defined in or, {@code null} or {@linkplain
	 * String#isEmpty() empty} string for version {@code 0}
	 * @return procedure id
	 * @throws IllegalArgumentException {@code svc} or {@code name} is malformed, or {@code
	 * ver} is not {@code null}, not empty, and is malformed
	 * @since 1.2
	 */
	public static @RpcProcedureId String
	idOfProcedure(String svc, String name, @Nullable String ver) {
		Preconditions.checkArgument(
			SERVICE_AND_PROCEDURE_NAME_PATTERN.matcher(svc).matches() &&
			SERVICE_AND_PROCEDURE_NAME_PATTERN.matcher(name).matches()
		);
		if (TextUtils.isEmpty(ver))
			return String.format("/%s/%s", svc, name);
		Preconditions.checkArgument(SERVICE_VERSION_PATTERN.matcher(ver).matches());
		return String.format("/%s/%s/%s", svc, ver, name);
	}

	/**
	 * Generate id of a remote procedure within version {@code 0} of a service.
	 *
	 * @param svc service in which procedure is defined
	 * @param name name of procedure
	 * @return procedure id
	 * @throws IllegalArgumentException {@code svc} or {@code name} is malformed
	 * @since 1.2
	 */
	public static @RpcProcedureId String idOfProcedure(String svc, String name) {
		return idOfProcedure(svc, name, null);
	}

	private final RegulationsModule regulations;
	private final OriginModuleEventCallback regulationsUpdateCallback;
	private final HttpModule http;
	/*
	 * request id counter
	 *
	 * request ids are used for secondary sorting, since primary sorting is based on request
	 * expiry, in order to be fair the request id, which is lower for requests created before
	 * another, serves as a mechanism to ensure the request is handled in the order it was
	 * submitted.
	 */
	private final AtomicLong nextCallRequestId;
	/*
	 * lock protecting against timeouts and destroy
	 *
	 * write-side of this lock protects modifications to `serviceHostRecords`, protects against
	 * destroy, and protects against scheduling multiple `process()` tasks.
	 */
	@VisibleForTesting
	final ReadWriteLock lock;
	/*
	 * set of call requests currently active, or `null` if module has shutdown
	 *
	 * this is `null` when module has been destroyed
	 */
	@GuardedBy("this.lock")
	@VisibleForTesting
	@Nullable ConcurrentSkipListSet<CallRequest> activeCalls;
	// mapping of service name to service host records
	@GuardedBy("this.lock")
	@VisibleForTesting
	final ArrayMap<String, RpcServiceHostRecords> servicesHostRecords;
	// queue of requests which are blocked by a host record not being available for their service
	private final Queue<CallRequest> callsAwaitingProcess;
	// process service
	@VisibleForTesting
	final ExecutingService processor;
	private @Nullable String host;
	private int port;
	private boolean insecure;

	private RpcModule(Origin sdk) {
		super(NAME, sdk);
		this.regulations = sdk.loadModule(RegulationsModule.class);
		this.http = sdk.loadModule(HttpModule.class);
		this.nextCallRequestId = new AtomicLong();
		this.lock = new ReentrantReadWriteLock();
		this.activeCalls = new ConcurrentSkipListSet<>((a, b) -> {
			int sig = Long.compareUnsigned(a.expireTimestampMillis, b.expireTimestampMillis);

			return sig != 0 ? sig : Long.compareUnsigned(a.id, b.id);
		});
		this.servicesHostRecords = new ArrayMap<>();
		this.callsAwaitingProcess =
			Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ? new LinkedTransferQueue<>() :
			new ConcurrentLinkedQueue<>();
		this.processor =
			ExecutingService.of(NAME + "/processor", this::process, sdk.backgroundExecutor());

		// update default host as required
		this.regulationsUpdateCallback = (_mod, _name, _data, _when) -> this.processor.schedule();
		this.regulations.registerEventCallback(
			this.regulationsUpdateCallback,
			RegulationsModule.REGS_UPDATE_EVENT
		);
	}

	// Store service host records into module settings.
	@WorkerThread
	private void storeServicesHostRecords() {
		if (this.servicesHostRecords.isEmpty())
			return;

		ProtobufEncoder enc = ProtobufEncoder.of();

		for (int i = 0; i < this.servicesHostRecords.size(); i++) {
			enc.encodeLenField(
				fieldTagOf(1, WIRE_LEN),
				this.servicesHostRecords.valueAt(i),
				(svcRecs, svcRecsEnc) -> {
					List<RpcHostRecord> recs = svcRecs.toRecords();

					if (recs.isEmpty())
						return;
					svcRecsEnc.encodeStringField(SETTINGS_SERVICE, svcRecs.service());
					svcRecsEnc.encodeStringField(SETTINGS_HOST, svcRecs.host());
					for (RpcHostRecord rec : recs)
						svcRecsEnc.encodeLenField(SETTINGS_RECORD, rec, RpcHostRecord::toProtobuf);
				}
			);
		}

		super.storeSettings(enc.asBuffer());
	}

	// Read `RpcServiceHostRecords` for a service, returns `null` if no records were read.
	private static @Nullable RpcServiceHostRecords readServiceHostRecords(ProtobufDecoder dec) {
		String svc = "";
		String host = "";
		ArrayList<RpcHostRecord> recs = new ArrayList<>();

		while (dec.hasRemaining()) {
			int tag = dec.decodeFieldTag();

			if (tag == SETTINGS_SERVICE)
				svc = dec.decodeString();
			else if (tag == SETTINGS_HOST)
				host = dec.decodeString();
			else if (tag == SETTINGS_RECORD)
				recs.add(dec.decodeLen(RpcHostRecord::ofProtobuf));
			else
				dec.skipFieldValue(tag);
		}
		return svc.isEmpty() || host.isEmpty() || recs.isEmpty() ? null :
			new RpcServiceHostRecords(svc, host, recs);
	}

	// Load services host records from module {@linkplain #loadSettings() settings}.
	@WorkerThread
	private void loadServicesHostRecords() {
		Lock write = this.lock.writeLock();
		ByteBuffer buff = super.loadSettings();

		if (buff == null)
			return;
		try {
			ProtobufDecoder dec = ProtobufDecoder.ofBuffer(buff);

			while (dec.hasRemaining()) {
				int tag = dec.decodeFieldTag();

				if (tag != fieldTagOf(1, WIRE_LEN)) {
					dec.skipFieldValue(tag);
					continue;
				}

				RpcServiceHostRecords recs = dec.decodeLen(RpcModule::readServiceHostRecords);

				if (recs == null)
					continue;

				Logger.debug(
					TAG,
					"loaded host records for %s: %s",
					recs.service(),
					recs.toRecords()
				);
				write.lock();
				try {
					this.servicesHostRecords.put(recs.service(), recs);
				} finally {
					write.unlock();
				}
			}
		} catch (RuntimeException cause) {
			Logger.debug(TAG, "failed to load services host records", cause);
		}
	}

	/*
	 * Query host records for a service `svc` with root service host `host`. The resulting records
	 * are returned or `null` if module has been destroyed.
	 */
	@WorkerThread
	private @Nullable RpcServiceHostRecords queryServiceHostRecords(String svc, String host) {
		Collection<RpcHostRecord> recs;
		Lock read = this.lock.readLock();

		read.lock();
		try {
			if (this.activeCalls == null)
				return null;
			recs = super.sdk().callIoInBackground(() -> RpcHostRecord.ofQuery(
				super.sdk().context(),
				super.sdk().backgroundIoExecutor(),
				host,
				DNS_QUERY_TIMEOUT_MILLIS
			)).get();
		} catch (Throwable cause) {
			Logger.warn(TAG, "failed to query records of %s", host, cause);
			recs = Collections.emptyList();
		} finally {
			read.unlock();
		}

		if (recs.isEmpty()) {
			recs = Collections.singletonList(RpcHostRecord.ofHost(host, this.port));
			Logger.debug(TAG, "no records resolved for %s, defaulting to %s", host, recs);
		} else {
			Logger.debug(TAG, "resolved %s to: %s", host, recs);
		}
		return new RpcServiceHostRecords(svc, host, recs);
	}

	// Determine host for a service `svc`.
	@VisibleForTesting
	String hostOfService(String svc) {
		String host = this.host;

		if (host == null) {
			host =
				this.regulations.isPiplApplicable() ? BuildConfig.ORIGIN_CHINA_RPC_HOST :
				BuildConfig.ORIGIN_GLOBAL_RPC_HOST;
		}
		return host.replace("%s", svc);
	}

	/*
	 * Update services host records. If services host records have not yet been loaded, they are
	 * loaded. If host configuration has changed, relevant service records are updated. The set of
	 * updated records is returned.
	 */
	private Set<String> updateServicesHostRecords() {
		boolean loaded = false;

		/*
		 * We're the only ones that can modify `servicesHostRecords`, so we don't need a lock for
		 * this check.
		 */
		if (this.servicesHostRecords.isEmpty()) {
			try {
				this.loadServicesHostRecords();
				loaded = true;
			} catch (Throwable cause) {
				Logger.info(TAG, "failed to load services host records", cause);
			}
		}

		Set<String> updSvcNames = CollectionsCompat.newArraySet();

		for (int i = 0; i < this.servicesHostRecords.size(); i++) {
			RpcServiceHostRecords recs = this.servicesHostRecords.valueAt(i);
			String newHost = this.hostOfService(recs.service());

			if (newHost.equals(recs.host()) && recs.size() != 0) {
				//noinspection ConstantValue
				if (loaded)
					updSvcNames.add(recs.service());
				continue;
			}

			/*
			 * no need to lock here, we're only updating the underlying array element, not adding
			 * or removing an element
			 */
			recs = this.queryServiceHostRecords(recs.service(), newHost);
			if (recs == null)
				break;
			this.servicesHostRecords.setValueAt(i, recs);
			updSvcNames.add(recs.service());
		}
		return updSvcNames;
	}

	/*
	 * Process services host records update. If services host records have not yet been
	 * loaded, they are loaded. Service host records for `callsAwaitingProcess` call requests are
	 * updated, if required, and the respective requests are sent.
	 */
	@WorkerThread
	private void processServicesHostRecords() {
		Set<String> updSvcNames = this.updateServicesHostRecords();
		List<CallRequest> calls = new ArrayList<>();

		while (true) {
			CallRequest req = this.callsAwaitingProcess.poll();

			if (req == null)
				break;

			String svc = req.serviceName();

			if (!req.isDone())
				calls.add(req);
			if (!updSvcNames.add(svc))
				continue;

			RpcServiceHostRecords recs = this.queryServiceHostRecords(svc, this.hostOfService(svc));
			Lock write = this.lock.writeLock();

			if (recs == null)
				break;

			write.lock();
			try {
				this.servicesHostRecords.put(svc, recs);
			} finally {
				write.unlock();
			}
		}

		if (!updSvcNames.isEmpty()) {
			try {
				this.storeServicesHostRecords();
			} catch (Throwable cause) {
				Logger.info(TAG, "failed to store services host records", cause);
			}
		}

		for (CallRequest call : calls)
			this.sendCall(call, null);
	}

	/*
	 * Timeout call requests which have expired according to `CallRequest::expireTimestampMillis`.
	 * This returns `true` if module has been destroyed.
	 */
	private boolean processTimeouts() {
		long now = SystemClock.uptimeMillis();
		Lock read = this.lock.readLock();

		while (true) {
			CallRequest req;

			read.lock();
			try {
				if (this.activeCalls == null)
					return true;
				try {
					req = this.activeCalls.first();
				} catch (NoSuchElementException ignored) {
					break;
				}
			} finally {
				read.unlock();
			}

			if (Long.compareUnsigned(now, req.expireTimestampMillis) < 0)
				break;
			Logger.debug(TAG, "call request %s timed out", req);
			req.timeout();
		}
		return false;
	}

	// Process request call timeouts and service host records.
	@WorkerThread
	private void process() {
		if (this.processTimeouts())
			return;

		this.processServicesHostRecords();

		long now = SystemClock.uptimeMillis();
		Lock write = this.lock.writeLock();

		write.lock();
		try {
			long delayMs = 0L;

			if (this.activeCalls == null) {
				delayMs = -1L;
			} else if (this.callsAwaitingProcess.isEmpty()) {
				try {
					CallRequest earliest = this.activeCalls.first();

					delayMs = Time.durationBetween(now, earliest.expireTimestampMillis);
				} catch (NoSuchElementException ignored) {
					delayMs = -1L;
				}
			}
			if (delayMs != -1L)
				this.processor.schedule(delayMs, 5, TimeUnit.MILLISECONDS);
		} finally {
			write.unlock();
		}
	}

	// Fail with `IllegalStateException` if module has been destroyed.
	private void checkNotDestroyed() {
		Preconditions.checkState(this.activeCalls != null, "module destroyed");
	}

	// Resolve method and URL for a call request `req`. The resulting URL is based on `rec`.
	private Pair<@HttpMethod String, String>
	resolveCallUrlAndMethod(CallRequest req, RpcHostRecord rec) {
		StringBuilder url = new StringBuilder();

		url.append(this.insecure ? "http" : "https")
			.append("://")
			.append(rec.host);
		if (rec.port > 0 && rec.port != (this.insecure ? 80 : 443))
			url.append(':').append(rec.port);
		url.append(req.procedureId);

		if (req.arguments == null || !req.arguments.hasRemaining())
			return new Pair<>("GET", url.toString());
		if (!req.useHttpGet)
			return new Pair<>("POST", url.toString());
		return new Pair<>(
			"GET",
			url.append('/')
				.append(Base64.encodeToString(
					req.arguments.array(),
					req.arguments.arrayOffset() + req.arguments.position(),
					req.arguments.remaining(),
					Base64.NO_PADDING | Base64.NO_WRAP | Base64.URL_SAFE
				))
				.toString()
		);
	}

	// Send call request `req`.
	@GuardedBy("this.lock.readLock()")
	private void doSendCall(CallRequest req) {
		if (req.isDone())
			return;

		long expireDurMs =
			Time.durationBetween(SystemClock.uptimeMillis(), req.expireTimestampMillis);

		if (expireDurMs <= 0) {
			req.timeout();
			return;
		}

		RpcServiceHostRecords recs = this.servicesHostRecords.get(req.serviceName());
		RpcHostRecord rec = recs == null ? null : recs.next();

		if (rec == null) {
			if (this.callsAwaitingProcess.add(req))
				this.processor.schedule();
			else
				req.complete(new IllegalStateException("blocked request queue overflow"));
			return;
		}

		Pair<@HttpMethod String, String> methAndUrl = this.resolveCallUrlAndMethod(req, rec);
		String storeId = super.sdk().app().storeId();
		HttpRequest.Builder http = this.http
			.newRequestBuilder(methAndUrl.second, methAndUrl.first, CALL_HTTP_REQUEST_LISTENER)
			.attachment(req)
			.addHeader("Cache-Control", "no-cache,no-store")
			.addHeader("origin-build", String.format(
				Locale.ROOT,
				"%s.%s/%s-android",
				Origin.VENDOR,
				Origin.NAME,
				Origin.VERSION
			));

		if (!storeId.isEmpty())
			http.addHeader("x-requested-with", storeId);
		if (req.arguments != null) {
			if (methAndUrl.first.equals("POST")) {
				http.body(req.arguments)
					.addHeader("Content-Type", "application/octet-stream");
				if (req.argumentsDeflated)
					http.addHeader("Content-Encoding", "deflate");
			} else if (req.argumentsDeflated) {
				http.addHeader("origin-encoding", "deflate");
			}
		}
		if (req.tryStart(http)) {
			req.hostRecord = rec;
			this.processor.schedule(expireDurMs, 5, TimeUnit.MILLISECONDS);
			Logger.debug(TAG, "started call request %s to %s", req, rec);
		}
	}

	/*
	 * Send call request, if possible. If `retryCause` is null, then call is an initial call;
	 * otherwise, `retryCause` must be the reason why the call is being resent.
	 */
	@RestrictTo(RestrictTo.Scope.SUBCLASSES)
	private void sendCall(CallRequest req, @Nullable Throwable retryCause) {
		Lock read = this.lock.readLock();

		read.lock();
		try {
			this.checkNotDestroyed();
			if (retryCause == null) {
				this.doSendCall(req);
				return;
			}

			// note the failure down
			if (req.hostRecord != null)
				req.hostRecord.updateLastFailureTimestamp();
			if (req.httpRetryCount++ >= MAX_CALL_RETRY_COUNT) {
				req.complete(retryCause);
			} else {
				Logger.debug(
					TAG,
					"retrying call request %s, retry-count=%s",
					req,
					req.httpRetryCount
				);
				this.doSendCall(req);
			}
		} catch (Throwable cause) {
			req.complete(cause);
		} finally {
			read.unlock();
		}
	}

	// Handle call request completion for `req`.
	@RestrictTo(RestrictTo.Scope.SUBCLASSES)
	private void onCallDone(CallRequest req) {
		ConcurrentSkipListSet<CallRequest> calls = this.activeCalls;

		if (calls != null && calls.remove(req))
			Logger.debug(TAG, "call request %s done", req);
	}

	private static @Nullable ByteBuffer prepareCallArguments(@Nullable Object args) {
		if (args instanceof ProtobufSerializable) {
			ProtobufEncoder enc = ProtobufEncoder.of();

			((ProtobufSerializable) args).toProtobuf(enc);
			return enc.asBuffer();
		} else if (args instanceof byte[]) {
			return ByteBuffer.wrap((byte[]) args);
		} else if (args instanceof ByteBuffer) {
			return (ByteBuffer) args;
		}
		Preconditions.checkArgument(args == null);
		return null;
	}

	/*
	 * Invoke remote procedure `procId`, asynchronously, with optional arguments `args`. If
	 * `args` is non-`null`, it must be `ProtobufSerializable`, `byte` array, or `ByteBuffer`.
	 * When `args` is `ProtobufSerializable`, the arguments for the procedure are serialized from
	 * it, otherwise, `byte` array and buffer arguments are transmitted as-is. The `resDeser`,
	 * if non-`null`, is used to deserialize the result from the procedure, if any.
	 *
	 * The call is made with a maximum time `timeout`. When `timeout` is `0`, the request waits for
	 * the default timeout.
	 */
	@SuppressWarnings("unchecked")
	private <T> ListenableFuture<T> doCall(
		@RpcProcedureId String procId,
		@Nullable Object args,
		@Nullable Function<ProtobufDecoder, T> resDeser,
		long timeout,
		TimeUnit unit
	) {
		Preconditions.checkArgument(PROCEDURE_ID_PATTERN.matcher(procId).matches());

		long expireMs = unit.toMillis(timeout);

		if (expireMs == 0L)
			expireMs = DEFAULT_CALL_REQUEST_TIMEOUT_MILLIS;

		CallRequest req = new CallRequest(
			this.nextCallRequestId.getAndIncrement(),
			expireMs,
			procId,
			prepareCallArguments(args),
			resDeser
		);
		Lock read = this.lock.readLock();

		read.lock();
		try {
			this.checkNotDestroyed();
			//noinspection DataFlowIssue
			this.activeCalls.add(req);
			this.doSendCall(req);
		} catch (Throwable cause) {
			req.complete(cause);
		} finally {
			read.unlock();
		}
		return (ListenableFuture<T>) req;
	}

	/**
	 * Invoke remote procedure asynchronously.
	 *
	 * @param <T> procedure result type
	 * @param procId id of procedure to invoke
	 * @param args arguments to invoke procedure with
	 * @param resDeser deserializer to deserialize procedure result with
	 * @param timeout maximum time to wait for remote procedure or {@code 0} for default timeout
	 * @param unit unit {@code timeout} is measured in
	 * @return completion future
	 * @throws IllegalArgumentException {@code procId} is invalid
	 * @since 1.2
	 */
	public <T> ListenableFuture<T> call(
		@RpcProcedureId String procId,
		ProtobufSerializable args,
		Function<ProtobufDecoder, T> resDeser,
		long timeout,
		TimeUnit unit
	) {
		return this.doCall(
			procId,
			Preconditions.checkNotNull(args),
			Preconditions.checkNotNull(resDeser),
			timeout,
			unit
		);
	}

	/**
	 * Invoke remote procedure asynchronously, with default timeout.
	 *
	 * @param <T> procedure result type
	 * @param procId id of procedure to invoke
	 * @param args arguments to invoke procedure with
	 * @param resDeser deserializer to deserialize procedure result with
	 * @return completion future
	 * @throws IllegalArgumentException {@code procId} is invalid
	 * @since 1.2
	 * @see #call(String, ProtobufSerializable, Function, long, TimeUnit)
	 */
	public <T> ListenableFuture<T> call(
		@RpcProcedureId String procId,
		ProtobufSerializable args,
		Function<ProtobufDecoder, T> resDeser
	) {
		return this.call(procId, args, resDeser, 0L, TimeUnit.MILLISECONDS);
	}

	/**
	 * Invoke remote procedure, with serialized arguments, asynchronously.
	 *
	 * @param <T> procedure result type
	 * @param procId id of procedure to invoke
	 * @param args serialized arguments to invoke procedure with
	 * @param resDeser deserializer to deserialize procedure result with
	 * @param timeout maximum time to wait for remote procedure or {@code 0} for default timeout
	 * @param unit unit {@code timeout} is measured in
	 * @return completion future
	 * @throws IllegalArgumentException {@code procId} is invalid
	 * @since 1.2
	 */
	public <T> ListenableFuture<T> callWithBytes(
		@RpcProcedureId String procId,
		ByteBuffer args,
		Function<ProtobufDecoder, T> resDeser,
		long timeout,
		TimeUnit unit
	) {
		return this.doCall(
			procId,
			Preconditions.checkNotNull(args),
			Preconditions.checkNotNull(resDeser),
			timeout,
			unit
		);
	}

	/**
	 * Invoke remote procedure, with serialized arguments, asynchronously, with default timeout.
	 *
	 * @param <T> procedure result type
	 * @param procId id of procedure to invoke
	 * @param args serialized arguments to invoke procedure with
	 * @param resDeser deserializer to deserialize procedure result with
	 * @return completion future
	 * @throws IllegalArgumentException {@code procId} is invalid
	 * @since 1.2
	 * @see #callWithBytes(String, ByteBuffer, Function, long, TimeUnit)
	 */
	public <T> ListenableFuture<T> callWithBytes(
		@RpcProcedureId String procId,
		ByteBuffer args,
		Function<ProtobufDecoder, T> resDeser
	) {
		return this.callWithBytes(procId, args, resDeser, 0L, TimeUnit.MILLISECONDS);
	}

	/**
	 * Invoke remote procedure, without arguments, asynchronously.
	 *
	 * @param <T> procedure result type
	 * @param procId id of procedure to invoke
	 * @param resDeser deserializer to deserialize procedure result with
	 * @param timeout maximum time to wait for remote procedure or {@code 0} for default timeout
	 * @param unit unit {@code timeout} is measured in
	 * @return completion future
	 * @throws IllegalArgumentException {@code procId} is invalid
	 * @since 1.2
	 */
	public <T> ListenableFuture<T> callWithoutArguments(
		@RpcProcedureId String procId,
		Function<ProtobufDecoder, T> resDeser,
		long timeout,
		TimeUnit unit
	) {
		return this.doCall(
			procId,
			null,
			Preconditions.checkNotNull(resDeser),
			timeout,
			unit
		);
	}

	/**
	 * Invoke remote procedure, without arguments, asynchronously, with default timeout.
	 *
	 * @param <T> procedure result type
	 * @param procId id of procedure to invoke
	 * @param resDeser deserializer to deserialize procedure result with
	 * @return completion future
	 * @throws IllegalArgumentException {@code procId} is invalid
	 * @since 1.2
	 * @see #callWithoutArguments(String, Function, long, TimeUnit)
	 */
	public <T> ListenableFuture<T>
	callWithoutArguments(@RpcProcedureId String procId, Function<ProtobufDecoder, T> resDeser) {
		return this.callWithoutArguments(procId, resDeser, 0L, TimeUnit.MILLISECONDS);
	}

	/**
	 * Invoke remote procedure, with {@code void} result, asynchronously.
	 *
	 * @param procId id of procedure to invoke
	 * @param args arguments to invoke procedure with
	 * @param timeout maximum time to wait for remote procedure or {@code 0} for default timeout
	 * @param unit unit {@code timeout} is measured in
	 * @return completion future
	 * @throws IllegalArgumentException {@code procId} is invalid
	 * @since 1.2
	 */
	public ListenableFuture<Void> callVoid(
		@RpcProcedureId String procId,
		ProtobufSerializable args,
		long timeout, TimeUnit unit
	) {
		return this.doCall(
			procId,
			Preconditions.checkNotNull(args),
			null,
			timeout,
			unit
		);
	}

	/**
	 * Invoke remote procedure, with {@code void} result, asynchronously, with default timeout.
	 *
	 * @param procId id of procedure to invoke
	 * @param args arguments to invoke procedure with
	 * @return completion future
	 * @throws IllegalArgumentException {@code procId} is invalid
	 * @since 1.2
	 */
	public ListenableFuture<Void>
	callVoid(@RpcProcedureId String procId, ProtobufSerializable args) {
		return this.callVoid(procId, args, 0L, TimeUnit.MILLISECONDS);
	}

	/**
	 * Invoke remote procedure, with {@code void} result and serialized arguments, asynchronously.
	 *
	 * @param procId id of procedure to invoke
	 * @param args serialized arguments to invoke procedure with
	 * @param timeout maximum time to wait for remote procedure or {@code 0} for default timeout
	 * @param unit unit {@code timeout} is measured in
	 * @return completion future
	 * @throws IllegalArgumentException {@code procId} is invalid
	 * @since 1.2
	 */
	public ListenableFuture<Void> callVoidWithBytes(
		@RpcProcedureId String procId,
		ByteBuffer args,
		long timeout, TimeUnit unit
	) {
		return this.doCall(
			procId,
			Preconditions.checkNotNull(args),
			null,
			timeout,
			unit
		);
	}

	/**
	 * Invoke remote procedure, with {@code void} result and serialized arguments, asynchronously,
	 * with default timeout.
	 *
	 * @param procId id of procedure to invoke
	 * @param args arguments to invoke procedure with
	 * @return completion future
	 * @throws IllegalArgumentException {@code procId} is invalid
	 * @since 1.2
	 */
	public ListenableFuture<Void>
	callVoidWithBytes(@RpcProcedureId String procId, ByteBuffer args) {
		return this.callVoidWithBytes(procId, args, 0L, TimeUnit.MILLISECONDS);
	}

	/**
	 * Invoke remote procedure, without arguments and with {@code void} result, asynchronously.
	 *
	 * @param procId id of procedure to invoke
	 * @param timeout maximum time to wait for remote procedure or {@code 0} for default timeout
	 * @param unit unit {@code timeout} is measured in
	 * @return completion future
	 * @throws IllegalArgumentException {@code procId} is invalid
	 * @since 1.2
	 */
	public ListenableFuture<Void>
	callVoidWithoutArguments(@RpcProcedureId String procId, long timeout, TimeUnit unit) {
		return this.doCall(procId, null, null, timeout, unit);
	}

	/**
	 * Invoke remote procedure, without arguments and with {@code void} result, asynchronously,
	 * with default timeout.
	 *
	 * @param procId id of procedure to invoke
	 * @return completion future
	 * @throws IllegalArgumentException {@code procId} is invalid
	 * @since 1.2
	 */
	public ListenableFuture<Void> callVoidWithoutArguments(@RpcProcedureId String procId) {
		return this.callVoidWithoutArguments(procId, 0L, TimeUnit.MILLISECONDS);
	}

	@Override
	protected void destroy() {
		ConcurrentSkipListSet<CallRequest> reqs;
		Lock write = this.lock.writeLock();

		write.lock();
		try {
			reqs = this.activeCalls;
			this.activeCalls = null;
		} finally {
			write.unlock();
		}

		this.processor.shutdown();
		this.callsAwaitingProcess.clear();
		if (reqs == null)
			return;

		this.regulations.unregisterEventCallback(
			this.regulationsUpdateCallback,
			RegulationsModule.REGS_UPDATE_EVENT
		);

		IllegalStateException cause = new IllegalStateException("module destroyed");

		while (true) {
			CallRequest req = reqs.pollFirst();

			if (req == null)
				break;
			req.complete(cause);
		}
	}
}
