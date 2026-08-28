// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.core;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.http.ConnectionMigrationOptions;
import android.net.http.DnsOptions;
import android.net.http.HttpEngine;
import android.net.http.HttpException;
import android.net.http.NetworkException;
import android.net.http.QuicOptions;
import android.net.http.UploadDataProvider;
import android.net.http.UploadDataSink;
import android.net.http.UrlRequest;
import android.net.http.UrlResponseInfo;
import android.os.Build;
import android.util.Pair;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresExtension;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;
import androidx.core.util.Supplier;

import org.polygamma.android.origin.util.AndroidSettings;
import org.polygamma.android.origin.util.Logger;
import org.polygamma.android.origin.util.Preconditions;

import java.io.EOFException;
import java.io.File;
import java.lang.ref.WeakReference;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;

/**
 * {@link HttpModule} implementation using {@linkplain HttpEngine Cronet}.
 */
@RequiresExtension(
	extension = Build.VERSION_CODES.S,
	version = 7
)
final class HttpModuleCronetImpl extends HttpModule.Impl {

	private static final String TAG = HttpModuleCronetImpl.class.getSimpleName();

	/**
	 * Maximum size, in bytes, of I/O buffer pool.
	 */
	private static final int MAX_IO_BUFFER_POOL_SIZE_BYTES = 1048576;

	/**
	 * Maximum size, in bytes, of I/O buffer.
	 */
	private static final int MAX_IO_BUFFER_SIZE_BYTES = 4096;

	/**
	 * Executor which executes tasks inline.
	 */
	private static final Executor DIRECT_EXECUTOR = Runnable::run;

	/**
	 * Normalize HTTP request {@linkplain HttpRequest.Priority priority} to Cronet request priority.
	 *
	 * @param prio request priority to normalize
	 * @return normalized priority
	 */
	private static int cronetRequestPriorityOf(@HttpRequest.Priority int prio) {
		switch (prio) {
		case HttpRequest.PRIORITY_CRITICAL:
			return UrlRequest.REQUEST_PRIORITY_HIGHEST;
		case HttpRequest.PRIORITY_NORMAL:
			return UrlRequest.REQUEST_PRIORITY_MEDIUM;
		case HttpRequest.PRIORITY_LOW:
			return UrlRequest.REQUEST_PRIORITY_LOW;
		case HttpRequest.PRIORITY_IDLE:
			return UrlRequest.REQUEST_PRIORITY_LOWEST;
		}
		return UrlRequest.REQUEST_PRIORITY_IDLE;
	}

	/**
	 * Request body provider.
	 */
	private static final class RequestBodyProvider extends UploadDataProvider {

		private final ByteBuffer body;

		/**
		 * Construct new body provider.
		 *
		 * @param body request body
		 */
		RequestBodyProvider(ByteBuffer body) {
			this.body = body;
		}

		@Override
		public long getLength() {
			return this.body.limit();
		}

		@Override
		public void read(UploadDataSink sink, ByteBuffer dst) {
			if (dst.remaining() >= this.body.remaining()) {
				dst.put(this.body);
			} else {
				int srcPos = this.body.position();
				int srcLim = this.body.limit();

				this.body.limit(srcPos + dst.remaining());
				dst.put(this.body);
				this.body.limit(srcLim);
			}
			sink.onReadSucceeded(false);
		}

		@Override
		public void rewind(UploadDataSink sink) {
			this.body.clear();
			sink.onRewindSucceeded();
		}
	}

	/**
	 * Construct a response header resolver.
	 *
	 * @param info response information to construct resolver for
	 * @return header resolver
	 */
	private static Supplier<Map<String, List<String>>> headerResolverOf(UrlResponseInfo info) {
		return () -> info.getHeaders().getAsMap();
	}

	/**
	 * HTTP {@linkplain HttpRequest request} implementation driving HTTP exchange using {@linkplain
	 * UrlRequest Cronet}.
	 */
	private final class RequestImpl extends HttpRequest implements UrlRequest.Callback {

		private boolean hasResponseBody;

		/**
		 * Construct new request.
		 *
		 * @param builder builder to initialize from
		 */
		RequestImpl(Builder builder) {
			super(builder, null);

			UrlRequest.Builder req = HttpModuleCronetImpl.this.cronet
				.newUrlRequestBuilder(builder.url.toString(), DIRECT_EXECUTOR, this)
				.setHttpMethod(builder.method)
				.setPriority(cronetRequestPriorityOf(builder.priority))
				.setDirectExecutorAllowed(true);

			if (super.requiresNetwork)
				req.setCacheDisabled(true);
			if (builder.headers != null) {
				for (Pair<String, String> hdr : builder.headers)
					req.addHeader(hdr.first, hdr.second);
			}
			if (builder.body != null)
				req.setUploadDataProvider(new RequestBodyProvider(builder.body), DIRECT_EXECUTOR);
			super.updateState(req.build());
		}

		/**
		 * Start request.
		 *
		 * @return {@code true} if, and only if, request was started; otherwise, {@code false} if
		 * request is {@linkplain #isDone() done}
		 */
		@SuppressWarnings("DataFlowIssue")
		boolean start() {
			try {
				Pair<Object, UrlRequest> state = super.computeState((curr) -> {
					Preconditions.checkState(curr instanceof UrlRequest);
					((UrlRequest) curr).start();
					return (UrlRequest) curr;
				});

				if (state == null)
					return false;
				if (state.first != this)
					return true;
				state.second.cancel();
				return false;
			} catch (Throwable cause) {
				Logger.warn(TAG, "failed to start request", cause);
				super.cancel();
				return false;
			}
		}

		@Override
		@SuppressWarnings("unchecked")
		void release(@Nullable Object state) {
			try {
				if (state instanceof UrlRequest) {
					((UrlRequest) state).cancel();
				} else if (state instanceof Pair) {
					Pair<UrlRequest, ByteBuffer> reqAndIoBuff =
						(Pair<UrlRequest, ByteBuffer>) state;

					try {
						reqAndIoBuff.first.cancel();
					} finally {
						HttpModuleCronetImpl.this.releaseIoBuffer(reqAndIoBuff.second);
					}
				}
			} catch (Throwable cause) {
				Logger.warn(TAG, "failed to release request state: %s", state, cause);
			}
		}

		@Override
		@SuppressLint("WrongThread")
		public void onRedirectReceived(UrlRequest inner, UrlResponseInfo info, String url)
		throws Exception {
			if (super.isDone())
				return;
			if (super.listener().onRedirect(
				this,
				new URL(new URL(info.getUrl()), url),
				info.getHttpStatusCode(),
				info.getHttpStatusText(),
				headerResolverOf(info)
			) && !inner.isDone()) {
				inner.followRedirect();
			} else {
				super.setSuccess();
				inner.cancel();
			}
		}

		@Override
		@SuppressLint("WrongThread")
		public void onResponseStarted(UrlRequest inner, UrlResponseInfo info) throws Exception {
			Pair<Object, Pair<UrlRequest, ByteBuffer>> state = super.computeState((curr) -> {
				Preconditions.checkState(curr instanceof UrlRequest);
				//noinspection DataFlowIssue
				return new Pair<>((UrlRequest) curr, HttpModuleCronetImpl.this.acquireIoBuffer());
			});

			if (state == null) {
				inner.cancel();
				return;
			}

			Pair<UrlRequest, ByteBuffer> reqAndIoBuff = state.second;

			if (state.first == this) {
				inner.cancel();
				HttpModuleCronetImpl.this.releaseIoBuffer(reqAndIoBuff.second);
				return;
			}

			super.listener().onResponseStart(
				this,
				info.getHttpStatusCode(),
				info.getHttpStatusText(),
				headerResolverOf(info)
			);
			inner.read(reqAndIoBuff.second);
		}

		@Override
		@SuppressLint("WrongThread")
		public void onReadCompleted(UrlRequest inner, UrlResponseInfo info, ByteBuffer buff)
		throws Exception {
			this.hasResponseBody = true;
			if (super.isDone())
				return;
			if (!buff.hasRemaining()) {
				super.listener().onResponseBodyPart(this, (ByteBuffer) buff.flip(), false);
				buff.clear();
			}
			inner.read(buff);
		}

		@Override
		@SuppressLint("WrongThread")
		@SuppressWarnings("unchecked")
		public void onSucceeded(UrlRequest inner, UrlResponseInfo info) {
			if (!this.hasResponseBody) {
				super.setSuccess();
				return;
			}

			Object state = super.updateState(null);

			if (state == this)
				return;

			try {
				@SuppressWarnings("DataFlowIssue")
				ByteBuffer buff = ((Pair<UrlRequest, ByteBuffer>) state).second;

				super.listener().onResponseBodyPart(this, (ByteBuffer) buff.flip(), true);
			} catch (Throwable cause) {
				super.setFailed(cause, false);
				return;
			} finally {
				this.release(state);
			}
			super.setSuccess();
		}

		@Override
		public void onFailed(UrlRequest inner, UrlResponseInfo info, HttpException cause) {
			if (!(cause instanceof NetworkException)) {
				super.setFailed(cause, false);
				return;
			}

			NetworkException net = (NetworkException) cause;
			String msg = net.getMessage();
			boolean retry = net.isImmediatelyRetryable();
			Throwable normalized = null;

			switch (net.getErrorCode()) {
			case NetworkException.ERROR_ADDRESS_UNREACHABLE:
				normalized = new NoRouteToHostException(msg);
				break;
			case NetworkException.ERROR_CONNECTION_CLOSED:
				normalized = new EOFException(msg);
				break;
			case NetworkException.ERROR_CONNECTION_REFUSED:
				normalized =
					new ConnectException(Preconditions.checkNotNullElse(msg, "connection refused"));
				break;
			case NetworkException.ERROR_CONNECTION_RESET:
			case NetworkException.ERROR_QUIC_PROTOCOL_FAILED:
				retry = true;
				break;
			case NetworkException.ERROR_CONNECTION_TIMED_OUT:
				normalized =
					new ConnectException(Preconditions.checkNotNullElse(msg, "connect timeout"));
				break;
			case NetworkException.ERROR_HOSTNAME_NOT_RESOLVED:
				normalized = new UnknownHostException(msg);
				break;
			case NetworkException.ERROR_INTERNET_DISCONNECTED:
			case NetworkException.ERROR_NETWORK_CHANGED:
				retry = true;
				HttpModuleCronetImpl.this.scheduleCheckInternetAccessible();
				break;
			case NetworkException.ERROR_TIMED_OUT:
				retry = true;
				normalized = new SocketTimeoutException(msg);
				break;
			}
			super.setFailed(normalized != null ? normalized.initCause(cause) : cause, retry);
		}

		@Override
		public void onCanceled(UrlRequest inner, UrlResponseInfo info) {
			super.cancel();
		}
	}

	/**
	 * Open a new Cronet instance.
	 *
	 * @param ctxt context to open instance with
	 * @param cacheDir directory to use for persistent cache
	 * @param maxCacheSizeBytes maximum size, in bytes, of cache
	 * @return resulting instance
	 */
	private static HttpEngine
	openCronet(Context ctxt, @Nullable File cacheDir, long maxCacheSizeBytes) {
		HttpEngine.Builder cronet = new HttpEngine.Builder(ctxt);
		DnsOptions.Builder dns = (new DnsOptions.Builder())
			.setUseHttpStackDnsResolver(DnsOptions.DNS_OPTION_ENABLED)
			.setStaleDns(DnsOptions.DNS_OPTION_ENABLED)
			.setStaleDnsOptions(
				(new DnsOptions.StaleDnsOptions.Builder())
					.setAllowCrossNetworkUsage(DnsOptions.DNS_OPTION_ENABLED)
					.setUseStaleOnNameNotResolved(DnsOptions.DNS_OPTION_ENABLED)
					.build()
			);

		if (cacheDir != null) {
			cronet.setStoragePath(cacheDir.getAbsolutePath())
				.setEnableHttpCache(HttpEngine.Builder.HTTP_CACHE_DISK, maxCacheSizeBytes);
			dns.setPersistHostCache(DnsOptions.DNS_OPTION_ENABLED);
		} else {
			cronet.setEnableHttpCache(HttpEngine.Builder.HTTP_CACHE_IN_MEMORY, maxCacheSizeBytes);
		}

		// Cronet ships via APEX on Xiaomi phones, and it's just totally broken for QUIC.
		if (!AndroidSettings.getSystemString("ro.miui.ui.version.name").isEmpty()) {
			cronet.setEnableQuic(false);
		} else {
			cronet.setEnableQuic(true)
				.setDnsOptions(dns.build())
				.setConnectionMigrationOptions(
					(new ConnectionMigrationOptions.Builder())
						.setAllowNonDefaultNetworkUsage(
							ConnectionMigrationOptions.MIGRATION_OPTION_ENABLED
						)
						.setDefaultNetworkMigration(
							ConnectionMigrationOptions.MIGRATION_OPTION_ENABLED
						)
						.setPathDegradationMigration(
							ConnectionMigrationOptions.MIGRATION_OPTION_ENABLED
						)
						.build()
				)
				.setQuicOptions(
					(new QuicOptions.Builder())
						.setInMemoryServerConfigsCacheSize(1024)
						.build()
				);
		}
		return cronet.setEnableHttp2(true)
			.build();
	}

	@VisibleForTesting
	final HttpEngine cronet;
	private final ArrayBlockingQueue<WeakReference<ByteBuffer>> ioBufferPool;

	/**
	 * Construct new client.
	 *
	 * @param module owning module
	 * @param cacheDir directory to store disk cache in or {@code null} to disable disk cache
	 * @param maxCacheSizeBytes maximum size, in bytes, of disk cache
	 */
	HttpModuleCronetImpl(HttpModule module, @Nullable File cacheDir, long maxCacheSizeBytes) {
		super(module, MAX_IO_BUFFER_POOL_SIZE_BYTES / MAX_IO_BUFFER_SIZE_BYTES);
		this.cronet = openCronet(module.sdk().context(), cacheDir, maxCacheSizeBytes);
		this.ioBufferPool =
			new ArrayBlockingQueue<>(MAX_IO_BUFFER_POOL_SIZE_BYTES / MAX_IO_BUFFER_SIZE_BYTES);
	}

	/**
	 * Acquire an I/O buffer.
	 *
	 * @return I/O buffer
	 * @throws IllegalStateException pooled buffer was not available and system ran out of direct
	 * memory to allocate a new buffer
	 */
	@WorkerThread
	@RestrictTo(RestrictTo.Scope.SUBCLASSES)
	ByteBuffer acquireIoBuffer() {
		while (true) {
			WeakReference<ByteBuffer> ref = this.ioBufferPool.poll();

			if (ref == null)
				break;

			ByteBuffer buff = ref.get();

			if (buff != null)
				return (ByteBuffer) buff.clear();
		}
		try {
			return ByteBuffer.allocateDirect(MAX_IO_BUFFER_SIZE_BYTES);
		} catch (OutOfMemoryError cause) {
			throw new IllegalStateException("failed to allocate direct I/O buffer", cause);
		}
	}

	/**
	 * Release an I/O buffer.
	 *
	 * @param buff buffer to release
	 */
	@RestrictTo(RestrictTo.Scope.SUBCLASSES)
	void releaseIoBuffer(ByteBuffer buff) {
		offer: while (!this.ioBufferPool.offer(new WeakReference<>(buff))) {
			for (
				Iterator<WeakReference<ByteBuffer>> iter = this.ioBufferPool.iterator();
				iter.hasNext();
			) {
				if (iter.next().get() == null) {
					iter.remove();
					continue offer;
				}
			}
			// nothing was removed, we don't have any space available
			break;
		}
	}

	@Override
	boolean sendRequest(HttpRequest req) {
		return ((RequestImpl) req).start();
	}

	@Override
	HttpRequest buildRequest(HttpRequest.Builder builder) {
		return new RequestImpl(builder);
	}

	@Override
	protected void destroy() {
		try {
			this.cronet.shutdown();
		} catch (Throwable cause) {
			Logger.warn(TAG, "failed to shutdown Cronet", cause);
		}
	}
}
