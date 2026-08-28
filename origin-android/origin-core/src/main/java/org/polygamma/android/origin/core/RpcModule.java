// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.core;

import static org.polygamma.android.origin.protobuf.ProtobufField.*;

import android.content.Context;
import android.net.http.HttpEngine;
import android.os.Build;
import android.os.SystemClock;
import android.util.ArrayMap;
import android.util.Base64;
import android.util.Pair;

import androidx.annotation.GuardedBy;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;
import androidx.core.util.Supplier;

import com.google.common.util.concurrent.ListenableFuture;

import org.polygamma.android.origin.protobuf.ProtobufDeserializer;
import org.polygamma.android.origin.protobuf.ProtobufReader;
import org.polygamma.android.origin.protobuf.ProtobufSerializable;
import org.polygamma.android.origin.protobuf.ProtobufWriter;
import org.polygamma.android.origin.util.CollectionsCompat;
import org.polygamma.android.origin.util.ExecutingService;
import org.polygamma.android.origin.util.Flate;
import org.polygamma.android.origin.util.ListenableFutureTask;
import org.polygamma.android.origin.util.Logger;
import org.polygamma.android.origin.util.Preconditions;
import org.polygamma.android.origin.util.Strings;
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
 * #call(String, String, ProtobufSerializable, ProtobufDeserializer, long, TimeUnit)} family of
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

	/**
	 * Service {@linkplain #SETTINGS_RECORD records} were resolved for.
	 */
	private static final @Tag int SETTINGS_SERVICE	= ofString( 1);

	/**
	 * Hostname {@linkplain #SETTINGS_RECORD records} were resolved for.
	 */
	private static final @Tag int SETTINGS_HOST		= ofString( 2);

	/**
	 * {@linkplain RpcHostRecord Host record}.
	 */
	private static final @Tag int SETTINGS_RECORD	= ofMessage(3);

	/**
	 * Service and procedure name pattern.
	 */
	private static final Pattern SERVICE_AND_PROCEDURE_NAME_PATTERN =
		Pattern.compile("^[_A-Za-z][_A-Za-z0-9]*$");

	/**
	 * Default RPC request timeout, in milliseconds.
	 */
	private static final long DEFAULT_CALL_REQUEST_TIMEOUT_MILLIS = TimeUnit.SECONDS.toMillis(30);

	/**
	 * Timeout, in milliseconds, of DNS queries.
	 */
	private static final long DNS_QUERY_TIMEOUT_MILLIS = TimeUnit.SECONDS.toMillis(15);

	/**
	 * Maximum length of RPC call URL path, in bytes, which may be invoked using a {@code GET}.
	 */
	@VisibleForTesting
	static final int HTTP_GET_CALL_PATH_THRESHOLD = 2048;

	/**
	 * Maximum number of HTTP redirects that may be followed before an RPC call is aborted.
	 */
	private static final int MAX_CALL_HTTP_REDIRECT_COUNT = 5;

	/**
	 * Maximum number of times an RPC call can be retried.
	 */
	@VisibleForTesting
	static final int MAX_CALL_RETRY_COUNT = 3;

	/**
	 * Remote procedure call request.
	 */
	@VisibleForTesting
	final class CallRequest extends ListenableFutureTask<Object> {

		final long id;
		final long expireTimestampMillis;
		final String service;
		final String procedure;
		final @Nullable ByteBuffer arguments;
		final boolean argumentsDeflated;
		final boolean useHttpGet;
		final @Nullable ProtobufDeserializer<?> resultDeserializer;

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

		/**
		 * Construct a new call request.
		 *
		 * @param id unique request id
		 * @param timeoutMillis maximum time, in milliseconds, to wait for request to complete
		 * @param svc service to invoke procedure in
		 * @param proc procedure to invoke
		 * @param args arguments to invoke procedure with, if any
		 * @param resDeser result message deserializer, if any
		 */
		CallRequest(
			long id,
			long timeoutMillis,
			String svc,
			String proc,
			@Nullable ProtobufSerializable args,
			@Nullable ProtobufDeserializer<?> resDeser
		) {
			this.id = id;
			this.expireTimestampMillis = SystemClock.uptimeMillis() + timeoutMillis;
			this.service = svc;
			this.procedure = proc;
			this.resultDeserializer = resDeser;

			if (args == null) {
				this.arguments = null;
				this.argumentsDeflated = false;
				this.useHttpGet = true;
			} else {
				ByteBuffer ser = ProtobufWriter.serialize(args);
				long basePathLen = svc.length() + proc.length() + 3; // /<svc>/<proc>/
				boolean get =
					(basePathLen + estimateBase64CodingOf(ser.remaining())) <=
					HTTP_GET_CALL_PATH_THRESHOLD;
				boolean deflate = false;

				if (!get) {
					ByteBuffer comp =
						Flate.compressZlib(ser.duplicate(), Deflater.BEST_COMPRESSION, false);

					deflate = comp.remaining() < ser.remaining();
					if (deflate) {
						ser = comp;
						get =
							(basePathLen + estimateBase64CodingOf(ser.remaining())) <=
							HTTP_GET_CALL_PATH_THRESHOLD;
					}
				}

				this.argumentsDeflated = deflate;
				this.useHttpGet = get;
				this.arguments = ser;
			}
		}

		/**
		 * Owning module.
		 *
		 * @return module
		 */
		RpcModule module() {
			return RpcModule.this;
		}

		private void releaseState(@Nullable Object state) {
			if (state instanceof HttpRequest)
				((HttpRequest) state).cancel();
			else if (state instanceof ListenableFuture<?>)
				((ListenableFuture<?>) state).cancel(false);
		}

		/**
		 * Try and update request state to starting.
		 * <p>If this returns {@code true}, then its guaranteed that {@code http} has been
		 * {@linkplain HttpRequest.Builder#send() sent}.
		 *
		 * @param http builder of HTTP request underlying call
		 * @return {@code true} if, and only if, start state was entered successfully; otherwise,
		 * {@code false} if request is already {@linkplain #isDone() done}
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

		/**
		 * Try and clear request state.
		 *
		 * @return {@code true} if, and only if, state was cleared; otherwise, {@code false} if
		 * request is already {@linkplain #isDone() done}
		 */
		boolean tryClear() {
			synchronized (this) {
				if (this.state == this)
					return false;
				this.state = null;
			}
			return true;
		}

		/**
		 * Complete request.
		 *
		 * @param res {@code null}, result value, or error {@linkplain Throwable cause} if request
		 * completed successfully without a result, completed successfully with a result, or
		 * failed, respectively
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

		/**
		 * Complete request with timeout.
		 */
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
			return String.format(Locale.ROOT, "%s/%s@%s", this.service, this.procedure, this.id);
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

	/**
	 * Calculate number of bytes that would be used to encode a byte sequence.
	 *
	 * @param size length of byte sequence to be encoded
	 * @return encoded length, in bytes, of sequence
	 */
	@VisibleForTesting
	static long estimateBase64CodingOf(int size) {
		return ((size * 8L) + 6 - 1) / 6;
	}

	/**
	 * Retrieve RPC request for an HTTP request.
	 *
	 * @param http HTTP request
	 * @return corresponding RPC request
	 * @throws IllegalArgumentException {@code http} is not associated with an RPC request
	 */
	private static CallRequest callRequestOf(HttpRequest http) {
		CallRequest call = (CallRequest) http.attachment();

		//noinspection SynchronizationOnLocalVariableOrMethodParameter,DataFlowIssue
		synchronized (call) {
			Preconditions.checkArgument(call.state == http, "call detached from HTTP request");
		}
		return call;
	}

	/**
	 * Remote procedure call {@linkplain CallRequest request} HTTP lifecycle listener.
	 */
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
			call.result = call.resultDeserializer.ofProtobuf(new ProtobufReader(body));
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

	/**
	 * Store services host records into module {@linkplain #storeSettings(ByteBuffer) settings}.
	 */
	@WorkerThread
	private void storeServicesHostRecords() {
		if (this.servicesHostRecords.isEmpty())
			return;

		ProtobufWriter writer = new ProtobufWriter();

		for (int i = 0; i < this.servicesHostRecords.size(); i++) {
			RpcServiceHostRecords svcRecs = this.servicesHostRecords.valueAt(i);
			List<RpcHostRecord> recs = svcRecs.toRecords();

			if (recs.isEmpty())
				continue;

			long cookie = writer.beginWriteLen(ofMessage(1));

			writer.writeString(SETTINGS_SERVICE, svcRecs.service());
			writer.writeString(SETTINGS_HOST, svcRecs.host());
			for (RpcHostRecord rec : recs) {
				long recCookie = writer.beginWriteLen(SETTINGS_RECORD);

				rec.toProtobuf(writer);
				writer.endWriteLen(recCookie);
			}
			writer.endWriteLen(cookie);
		}

		super.storeSettings(writer.finish());
	}

	/**
	 * Read host {@linkplain RpcServiceHostRecords records} for a service.
	 *
	 * @param reader reader to read from
	 * @return resulting records or {@code null} if no records were read
	 */
	private static @Nullable RpcServiceHostRecords readServiceHostRecords(ProtobufReader reader) {
		String svc = "";
		String host = "";
		ArrayList<RpcHostRecord> recs = new ArrayList<>();

		while (reader.hasRemaining()) {
			int tag = reader.readTag();

			if (tag == SETTINGS_SERVICE)
				svc = reader.readString();
			else if (tag == SETTINGS_HOST)
				host = reader.readString();
			else if (tag == SETTINGS_RECORD)
				recs.add(reader.readLen(RpcHostRecord::ofProtobuf));
		}
		return svc.isEmpty() || host.isEmpty() || recs.isEmpty() ? null :
			new RpcServiceHostRecords(svc, host, recs);
	}

	/**
	 * Load services host records from module {@linkplain #loadSettings() settings}.
	 */
	@WorkerThread
	private void loadServicesHostRecords() {
		Lock write = this.lock.writeLock();
		ByteBuffer buff = super.loadSettings();

		if (buff == null)
			return;
		try {
			ProtobufReader reader = new ProtobufReader(buff);

			while (reader.hasRemaining()) {
				if (reader.readTag() != ofMessage(1))
					continue;

				int cookie = reader.beginReadLen();
				RpcServiceHostRecords recs = readServiceHostRecords(reader);

				reader.endReadLen(cookie);
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

	/**
	 * Query host records for a service.
	 *
	 * @param svc service name
	 * @param host root service host to query records of
	 * @return resulting records or {@code null} if module has been {@linkplain #destroy()
	 * destroyed}
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
				host,
				super.sdk().backgroundIoExecutor(),
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

	/**
	 * Determine host for a service.
	 *
	 * @param svc service name
	 * @return host name
	 */
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

	/**
	 * Update services host records.
	 * <p>If services host records have not yet been {@linkplain #loadServicesHostRecords()
	 * loaded}, they are loaded. If the host configuration has changed, relevant service records
	 * are updated.
	 *
	 * @return set of updated host records
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

	/**
	 * Process services host records update.
	 * <p>If services host records have not yet been {@linkplain #loadServicesHostRecords()
	 * loaded}, they are loaded. Service host records for {@linkplain #callsAwaitingProcess blocked}
	 * call requests are updated, if required, and the respective requests are {@linkplain
	 * #sendCall(CallRequest, Throwable) sent}.
	 */
	@WorkerThread
	private void processServicesHostRecords() {
		Set<String> updSvcNames = this.updateServicesHostRecords();
		List<CallRequest> calls = new ArrayList<>();

		while (true) {
			CallRequest req = this.callsAwaitingProcess.poll();

			if (req == null)
				break;
			if (!req.isDone())
				calls.add(req);
			if (!updSvcNames.add(req.service))
				continue;

			RpcServiceHostRecords recs =
				this.queryServiceHostRecords(req.service, this.hostOfService(req.service));
			Lock write = this.lock.writeLock();

			if (recs == null)
				break;

			write.lock();
			try {
				this.servicesHostRecords.put(req.service, recs);
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

	/**
	 * Timeout call requests which have {@linkplain CallRequest#expireTimestampMillis expired}.
	 *
	 * @return {@code true} if module has been {@linkplain #destroy() destroyed}; otherwise,
	 * {@code false}
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

	/**
	 * Process request call timeouts and service host records.
	 */
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

	/**
	 * Ensure module has not been {@linkplain #destroy() destroyed}.
	 *
	 * @throws IllegalStateException module has been destroyed
	 */
	private void checkNotDestroyed() {
		Preconditions.checkState(this.activeCalls != null, "module destroyed");
	}

	/**
	 * Resolve method and URL for a call.
	 *
	 * @param req call request to resolve URL and method for
	 * @param rec service host record to resolve URL hostname and port from
	 * @return tuple of HTTP method and URL
	 */
	private Pair<@HttpMethod String, String>
	resolveCallUrlAndMethod(CallRequest req, RpcHostRecord rec) {
		StringBuilder url = new StringBuilder();

		url.append(this.insecure ? "http" : "https")
			.append("://")
			.append(rec.host);
		if (rec.port > 0 && rec.port != (this.insecure ? 80 : 443))
			url.append(':').append(rec.port);
		url.append('/')
			.append(req.service)
			.append('/')
			.append(req.procedure);

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

	/**
	 * Send call request.
	 *
	 * @param req request of call to send
	 */
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

		RpcServiceHostRecords recs = this.servicesHostRecords.get(req.service);
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

	/**
	 * Send call request, if possible.
	 *
	 * @param req request of call to send
	 * @param retryCause reason call is being resent or {@code null} if initial call
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

	/**
	 * Handle call request completion.
	 *
	 * @param req call request which completed
	 */
	@RestrictTo(RestrictTo.Scope.SUBCLASSES)
	private void onCallDone(CallRequest req) {
		ConcurrentSkipListSet<CallRequest> calls = this.activeCalls;

		if (calls != null && calls.remove(req))
			Logger.debug(TAG, "call request %s done", req);
	}

	/**
	 * Invoke remote procedure asynchronously.
	 *
	 * @param <T> result type
	 * @param svc name of service to invoke procedure in
	 * @param name name of procedure to invoke
	 * @param args arguments to invoke procedure with, if any
	 * @param resDeser procedure result deserializer, if any
	 * @param timeout maximum time to wait for remote procedure or {@code 0} for default timeout
	 * @param unit unit {@code timeout} is measured in
	 * @return completion future
	 * @throws IllegalArgumentException {@code svc} or {@code name} is invalid
	 */
	@SuppressWarnings("unchecked")
	private <T> ListenableFuture<T> doCall(
		String svc,
		String name,
		@Nullable ProtobufSerializable args,
		@Nullable ProtobufDeserializer<T> resDeser,
		long timeout,
		TimeUnit unit
	) {
		Preconditions.checkArgument(
			SERVICE_AND_PROCEDURE_NAME_PATTERN.matcher(svc).matches() &&
			SERVICE_AND_PROCEDURE_NAME_PATTERN.matcher(name).matches()
		);

		long expireMs = unit.toMillis(timeout);

		if (expireMs == 0L)
			expireMs = DEFAULT_CALL_REQUEST_TIMEOUT_MILLIS;

		CallRequest req = new CallRequest(
			this.nextCallRequestId.getAndIncrement(),
			expireMs,
			svc,
			name,
			args,
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
	 * @param svc name of service to invoke procedure in
	 * @param name name of procedure to invoke
	 * @param args arguments to invoke procedure with
	 * @param resDeser deserializer to deserialize procedure result with
	 * @param timeout maximum time to wait for remote procedure or {@code 0} for default timeout
	 * @param unit unit {@code timeout} is measured in
	 * @return completion future
	 * @throws IllegalArgumentException {@code svc} or {@code name} is invalid
	 * @since 1.2
	 */
	public <T> ListenableFuture<T> call(
		String svc,
		String name,
		ProtobufSerializable args,
		ProtobufDeserializer<T> resDeser,
		long timeout,
		TimeUnit unit
	) {
		return this.doCall(
			svc,
			name,
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
	 * @param svc name of service to invoke procedure in
	 * @param name name of procedure to invoke
	 * @param args arguments to invoke procedure with
	 * @param resDeser deserializer to deserialize procedure result with
	 * @return completion future
	 * @throws IllegalArgumentException {@code svc} or {@code name} is invalid
	 * @since 1.2
	 * @see #call(String, String, ProtobufSerializable, ProtobufDeserializer, long, TimeUnit)
	 */
	public <T> ListenableFuture<T>
	call(String svc, String name, ProtobufSerializable args, ProtobufDeserializer<T> resDeser) {
		return this.call(svc, name, args, resDeser, 0L, TimeUnit.MILLISECONDS);
	}

	/**
	 * Invoke remote procedure, without arguments, asynchronously.
	 *
	 * @param <T> procedure result type
	 * @param svc name of service to invoke procedure in
	 * @param name name of procedure to invoke
	 * @param resDeser deserializer to deserialize procedure result with
	 * @param timeout maximum time to wait for remote procedure or {@code 0} for default timeout
	 * @param unit unit {@code timeout} is measured in
	 * @return completion future
	 * @throws IllegalArgumentException {@code svc} or {@code name} is invalid
	 * @since 1.2
	 */
	public <T> ListenableFuture<T> callWithoutArguments(
		String svc,
		String name,
		ProtobufDeserializer<T> resDeser,
		long timeout,
		TimeUnit unit
	) {
		return this.doCall(
			svc,
			name,
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
	 * @param svc name of service to invoke procedure in
	 * @param name name of procedure to invoke
	 * @param resDeser deserializer to deserialize procedure result with
	 * @return completion future
	 * @throws IllegalArgumentException {@code svc} or {@code name} is invalid
	 * @since 1.2
	 * @see #callWithoutArguments(String, String, ProtobufDeserializer, long, TimeUnit)
	 */
	public <T> ListenableFuture<T>
	callWithoutArguments(String svc, String name, ProtobufDeserializer<T> resDeser) {
		return this.callWithoutArguments(svc, name, resDeser, 0L, TimeUnit.MILLISECONDS);
	}

	/**
	 * Invoke remote procedure, with {@code void} result, asynchronously.
	 *
	 * @param svc name of service to invoke procedure in
	 * @param name name of procedure to invoke
	 * @param args arguments to invoke procedure with
	 * @param timeout maximum time to wait for remote procedure or {@code 0} for default timeout
	 * @param unit unit {@code timeout} is measured in
	 * @return completion future
	 * @throws IllegalArgumentException {@code svc} or {@code name} is invalid
	 * @since 1.2
	 */
	public ListenableFuture<Void>
	callVoid(String svc, String name, ProtobufSerializable args, long timeout, TimeUnit unit) {
		return this.doCall(
			svc,
			name,
			Preconditions.checkNotNull(args),
			null,
			timeout,
			unit
		);
	}

	/**
	 * Invoke remote procedure, with {@code void} result, asynchronously, with default timeout.
	 *
	 * @param svc name of service to invoke procedure in
	 * @param name name of procedure to invoke
	 * @param args arguments to invoke procedure with
	 * @return completion future
	 * @throws IllegalArgumentException {@code svc} or {@code name} is invalid
	 * @since 1.2
	 */
	public ListenableFuture<Void> callVoid(String svc, String name, ProtobufSerializable args) {
		return this.callVoid(svc, name, args, 0L, TimeUnit.MILLISECONDS);
	}

	/**
	 * Invoke remote procedure, without arguments and with {@code void} result, asynchronously.
	 *
	 * @param svc name of service to invoke procedure in
	 * @param name name of procedure to invoke
	 * @param timeout maximum time to wait for remote procedure or {@code 0} for default timeout
	 * @param unit unit {@code timeout} is measured in
	 * @return completion future
	 * @throws IllegalArgumentException {@code svc} or {@code name} is invalid
	 * @since 1.2
	 */
	public ListenableFuture<Void>
	callVoidWithoutArguments(String svc, String name, long timeout, TimeUnit unit) {
		return this.doCall(svc, name, null, null, timeout, unit);
	}

	/**
	 * Invoke remote procedure, without arguments and with {@code void} result, asynchronously,
	 * with default timeout.
	 *
	 * @param svc name of service to invoke procedure in
	 * @param name name of procedure to invoke
	 * @return completion future
	 * @throws IllegalArgumentException {@code svc} or {@code name} is invalid
	 * @since 1.2
	 */
	public ListenableFuture<Void> callVoidWithoutArguments(String svc, String name) {
		return this.callVoidWithoutArguments(svc, name, 0L, TimeUnit.MILLISECONDS);
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
