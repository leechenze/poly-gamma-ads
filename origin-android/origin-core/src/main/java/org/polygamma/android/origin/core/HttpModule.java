// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.core;

import android.content.Context;
import android.os.Build;
import android.os.ext.SdkExtensions;
import android.util.Base64;

import androidx.annotation.CallSuper;
import androidx.annotation.DoNotInline;
import androidx.annotation.GuardedBy;
import androidx.annotation.IntRange;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;

import com.google.common.util.concurrent.ListenableFuture;

import org.polygamma.android.origin.util.Futures;
import org.polygamma.android.origin.util.ListenableScheduledFuture;
import org.polygamma.android.origin.util.Logger;
import org.polygamma.android.origin.util.Preconditions;
import org.polygamma.android.origin.util.Sync;

import java.io.File;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Module providing HTTP client functionality.
 * <p>This module is used to send HTTP requests, supporting HTTP version 1.1, 2, and 3, depending
 * on executing platform. Requests are constructed using a {@linkplain HttpRequest.Builder builder},
 * constructed using {@link #newRequestBuilder(String, String, HttpRequest.Listener)}. Builders
 * configure and {@linkplain HttpRequest.Builder#send() send} a request.
 * {@snippet lang="java":
 * HttpModule module = Origin.current().loadModule(HttpModule.class);
 * HttpRequest.Listener listener = new HttpRequest.Listener() { // @link substring="HttpRequest.Listener" target="HttpRequest.Listener"
 *     @Override
 *     public void onError(HttpRequest req, Throwable cause, boolean recoverable) {
 *         assert req.isDone();
 *         Log.w(TAG, "Request to %s encountered error", req.url(), cause);
 *         if (recoverable) {
 *             // failure may be temporary, retrying request might succeed
 *         }
 *     }
 *
 *     @Override
 *     public void onCancel(HttpRequest req) {
 *         assert req.isDone();
 *         // Someone called `req.cancel()`.
 *         Log.i(TAG, "Request to %s cancelled", req.url());
 *     }
 *
 *     @Override
 *     public boolean onRedirect(
 *         HttpRequest req,
 *         URL url,
 *         int statusCode,
 *         String statusMsg,
 *         Supplier<Map<String, List<String>>> resolveHdrs
 *     ) {
 *         assert !req.isDone();
 *         // Server is redirecting request. We can return `false` if we don't want to follow the
 *         // the redirect. In this case, `onComplete()` will be invoked after we return.
 *         Log.i(TAG, "Request to %s was redirected to %s", req.url(), url);
 *         return true;
 *     }
 *
 *     @Override
 *     public void onStart(
 *         HttpRequest req,
 *         int statusCode,
 *         String statusMsg,
 *         Supplier<Map<String, List<String>>> resolveHdrs
 *     ) {
 *         assert !req.isDone();
 *         // Any redirects have been followed. Next call after we return will be to `onBodyPart()`,
 *         // `onError()`, or `onComplete()` if a body was sent, error was encountered, or body was
 *         // not sent, respectively.
 *         Map<String, List<String>> hdrs = resolveHdrs.get();
 *         List<String> lenVals = hdrs.get("Content-Length");
 *         long len = -1;
 *
 *         if (lenVals != null && !lenVals.isEmpty()) {
 *             len = Long.parseLong(lenVals.get(lenVals.size() - 1), 10);
 *         }
 *         Log.i(TAG, "Request to %s beginning, content-length=%s", req.url(), len);
 *     }
 *
 *     @Override
 *     public void onBodyPart(HttpRequest req, ByteBuffer body) throws IOException {
 *         Log.i(TAG, "Request to %s received %s bytes", req.url(), body.remaining());
 *         ((java.nio.channels.WritableByteChannel) req.attachment()).write(body);
 *     }
 *
 *     @Override
 *     public void onComplete(HttpRequest req) {
 *         Log.i(TAG, "Request to %s complete", req.url());
 *     }
 * };
 *
 * module.newRequestBuilder("https://example.com", "GET", listener) // @link substring="newRequestBuilder" target="#newRequestBuilder(String, String, HttpRequest.Listener)"
 *     .attachment(java.nio.channels.Channels.newChannel(new ByteArrayOutputStream())) // @link substring="attachment" target="HttpRequest.Builder#attachment(Object)"
 *     .addHeader("Cache-Control", "no-store") // @link substring="addHeader" target="HttpRequest.Builder#addHeader(String, String)"
 *     .priority(HttpRequest.PRIORITY_CRITICAL) // @link substring="priority" target="HttpRequest.Builder#priority(int)"
 *     .send(); // @link substring="send" target="HttpRequest.Builder#send()"
 *}
 * <h2>Lifetime</h2>
 * <p>The lifecycle of a request is tracked using a {@linkplain HttpRequest.Listener listener},
 * specified when constructing a request's builder. Opaque values may be {@linkplain
 * HttpRequest#attachment() attached} to a request. These opaque values may be used to implement
 * shared listeners whose logic can be applied to distinct requests.
 * <p>Requests are considered active for so long as they are not in a {@linkplain
 * HttpRequest#isDone() done} state. The done state is entered into for a request when:
 * <ul>
 *     <li>Request is {@linkplain HttpRequest#cancel() cancelled}.</li>
 *     <li>
 *         Error is {@linkplain HttpRequest.Listener#onFailed(HttpRequest, Throwable, boolean)
 *         encountered} while servicing the request. Errors can be encountered due to I/O,
 *         listener invocation, or owning SDK being {@linkplain Origin#isShutdown() shutdown}.
 *         Requests which fail due to I/O <i>may</i> be considered recoverable, in which case
 *         attempting to resend the request <i>may</i> result in a successful completion.
 *     </li>
 *     <li>
 *         Request {@linkplain HttpRequest.Listener#onSuccess(HttpRequest) completes}
 *         successfully.
 *     </li>
 * </ul>
 * <p>In certain cases, when internet is inaccessible, sending a request may not immediately send
 * the request. Instead, the request is queued and sent if, and only if, the request has not been
 * cancelled, the owning SDK has not been destroyed, <i>and</i> internet is available.
 * <h2>Caching</h2>
 * <p>Responses, which permit caching, are cached by default so long as the corresponding request
 * does not prohibit caching. The standard {@code Cache-Control} family of request and response
 * headers are used to determine cache viability of request and response exchanges.
 * <p>Caches are maintained in memory and on disk. The total size of the cache can be configured
 * using {@link Provider#maxCacheSizeBytes(long)}.
 * <p>Whenever possible, cache request headers should always be specified explicitly. These headers
 * are used to determine whether a request does indeed always require a network connection, for
 * example, when {@code Cache-Control} is {@code no-cache}. For requests which are determined to
 * not require a network connection, in other words, where a cached response is acceptable for the
 * request, this module attempts to use the cached response when network connectivity isn't
 * available.
 *
 * @since 1.2
 */
@SuppressWarnings({ "JavadocDeclaration", "JavadocLinkAsPlainText" })
public final class HttpModule extends OriginModule {

	private static final String TAG = HttpModule.class.getSimpleName();

	/**
	 * HTTP module name.
	 *
	 * @since 1.2
	 */
	public static final String NAME = "origin.http";

	/**
	 * {@linkplain #state State} indicating internet is accessible.
	 */
	@VisibleForTesting
	static final int STATE_INTERNET_ACCESSIBLE		= 0;

	/**
	 * {@linkplain #state State} indicating internet is inaccessible.
	 */
	@VisibleForTesting
	static final int STATE_INTERNET_INACCESSIBLE	= 1;

	/**
	 * {@linkplain #state State} indicating module has been {@linkplain #destroy() destroyed}.
	 */
	@VisibleForTesting
	static final int STATE_DESTROYED				= 2;

	/**
	 * Delay, in milliseconds, of internet accessibility check after a failed check.
	 *
	 * @see #checkInternetAccessible()
	 */
	private static final int INTERNET_ACCESSIBILITY_RECHECK_DELAY_MILLIS = 15000;

	/**
	 * Default size, in bytes, of cache.
	 */
	private static final long DEFAULT_MAX_CACHE_SIZE_BYTES = 50L * 1024 * 1024;

	/**
	 * HTTP client implementation.
	 */
	static abstract class Impl {

		/**
		 * Owning HTTP module.
		 */
		final HttpModule module;

		/**
		 * Maximum number of requests that can be live at any given point in time.
		 */
		@VisibleForTesting
		final int maxLiveRequestCount;

		/**
		 * Construct new client implementation.
		 *
		 * @param module owning module
		 * @param maxLiveReqCnt maximum number of requests that may be live at any given point in
		 * time
		 * @throws IllegalArgumentException {@code maxLiveReqCnt} is less than {@code 1}
		 */
		Impl(HttpModule module, int maxLiveReqCnt) {
			Preconditions.checkArgument(maxLiveReqCnt > 0);
			this.module = module;
			this.maxLiveRequestCount = maxLiveReqCnt;
		}

		/**
		 * Test whether client has been destroyed.
		 *
		 * @return {@code true} if, and only if, client has been destroyed
		 */
		@RestrictTo(RestrictTo.Scope.SUBCLASSES)
		final boolean isDestroyed() {
			return this.module.isDestroyed();
		}

		/**
		 * Schedule internet accessibility {@linkplain #checkInternetAccessible() check}, if required.
		 */
		@RestrictTo(RestrictTo.Scope.SUBCLASSES)
		final void scheduleCheckInternetAccessible() {
			this.module.scheduleCheckInternetAccessible();
		}

		/**
		 * Send request.
		 * <p>This is guaranteed to be invoked only <i>once</i> for any request instance. The
		 * lifecycle of {@code req} must be tracked through the {@code onRequest} family of
		 * methods of {@code this}. Implementations <b>cannot</b> fail with an exception.
		 *
		 * @param req request to send
		 * @return {@code true} if, and only if, request was sent; otherwise, {@code false} if
		 * request was cancelled before it could be sent
		 */
		@GuardedBy("HttpModule.this.lock")
		abstract boolean sendRequest(HttpRequest req);

		/**
		 * Build request from {@linkplain HttpRequest.Builder builder}.
		 * <p>This may be invoked even if module has been destroyed.
		 *
		 * @param builder builder to build request from
		 * @return resulting request
		 */
		abstract HttpRequest buildRequest(HttpRequest.Builder builder);

		/**
		 * Destroy client.
		 */
		abstract void destroy();
	}

	private interface CronetImplProvider {
		@DoNotInline
		static @Nullable Impl
		provide(HttpModule module, @Nullable File cacheDir, long maxCacheSizeByts)
		throws Exception {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE || (
				Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
				SdkExtensions.getExtensionVersion(Build.VERSION_CODES.S) >= 7
			)) {
				Class.forName("android.net.http.HttpEngine");
				return new HttpModuleCronetImpl(module, cacheDir, maxCacheSizeByts);
			}
			return null;
		}
	}

	/**
	 * HTTP {@linkplain HttpModule module} provider.
	 *
	 * @since 1.2
	 * @see #ofProvider()
	 */
	public static final class Provider extends OriginModule.Provider<HttpModule> {
		/**
		 * Max cache size, in bytes.
		 */
		private long maxCacheSizeBytes;

		private Provider() {
			super(HttpModule.class);
			this.maxCacheSizeBytes = DEFAULT_MAX_CACHE_SIZE_BYTES;
		}

		/**
		 * Set maximum cache size, in bytes.
		 *
		 * @param size cache size or value less than {@code 1} for default
		 * @return {@code this}
		 * @since 1.2
		 */
		public Provider maxCacheSizeBytes(long size) {
			this.maxCacheSizeBytes = size <= 0 ? DEFAULT_MAX_CACHE_SIZE_BYTES : size;
			return this;
		}

		/**
		 * Maximum cache size, in bytes.
		 *
		 * @return cache size
		 */
		@IntRange(from = 1) long maxCacheSizeBytes() {
			return this.maxCacheSizeBytes;
		}

		@Override
		protected HttpModule load(Origin sdk, Context ctxt) {
			return new HttpModule(sdk, this.maxCacheSizeBytes);
		}
	}

	/**
	 * Construct new module {@linkplain Provider provider}.
	 *
	 * @return provider instance
	 * @since 1.2
	 */
	public static Provider ofProvider() {
		return new Provider();
	}

	/**
	 * Resolve HTTP cache directory.
	 *
	 * @return directory to persist HTTP cache in or {@code null} if unavailable
	 */
	private static @Nullable File resolveHttpCacheDirectory(HttpModule module) {
		File httpCacheDir;

		try {
			File rootCacheDir = module.resolveCacheDirectory();
			String procName = Sync.currentProcessSimpleName();

			if (procName.isEmpty())
				procName = ":main:";
			httpCacheDir = new File(rootCacheDir, Base64.encodeToString(
				procName.getBytes(StandardCharsets.UTF_8),
				Base64.NO_PADDING | Base64.NO_WRAP | Base64.URL_SAFE
			));
			Preconditions.checkState(
				httpCacheDir.exists() || httpCacheDir.mkdirs(),
				"failed to create HTTP cache directory %s",
				httpCacheDir
			);
		} catch (Throwable cause) {
			Logger.warn(TAG, "failed to resolve HTTP cache directory", cause);
			httpCacheDir = null;
		}
		return httpCacheDir;
	}

	/**
	 * Lock protecting against {@linkplain #destroy() destroy} and requests.
	 */
	@VisibleForTesting
	final ReentrantReadWriteLock lock;

	/**
	 * Lock protecting against request entering {@linkplain HttpRequest#isDone() done} state.
	 * <p>Read and write -sides of this lock protect against {@link #liveRequests} modifications
	 * and backlog processing, respectively.
	 */
	@VisibleForTesting
	final ReentrantReadWriteLock doneLock;

	/**
	 * Priority based request queues for requests awaiting internet accessibility or live request
	 * completion in case of backlog.
	 */
	@VisibleForTesting
	final Queue<HttpRequest>[] priorityBacklogs;

	/**
	 * Identity map of requests currently live.
	 */
	@VisibleForTesting
	final ConcurrentHashMap<HttpRequest, HttpRequest> liveRequests;

	/**
	 * Number of live requests.
	 * <p>Live request count is tracked here instead of relying on {@link #liveRequests
	 * this.liveRequests.size()} because atomic updates to live request count is used to determine
	 * if module {@linkplain #state state} should be updated.
	 */
	@VisibleForTesting
	final AtomicInteger liveRequestCount;

	/**
	 * HTTP implementation.
	 * <p>This is set to {@link Future} when an implementation is being selected. Post
	 * initialization, this is an {@link Impl} instance.
	 */
	@GuardedBy("this.lock")
	private Object impl;

	/**
	 * Module state.
	 * <p>Reading from and writing to this requires read and write -side of {@link #lock this.lock}
	 * being held, respectively.
	 *
	 * @see #STATE_INTERNET_ACCESSIBLE
	 * @see #STATE_INTERNET_INACCESSIBLE
	 * @see #STATE_DESTROYED
	 */
	@GuardedBy("this.lock")
	@VisibleForTesting
	int state;

	@GuardedBy("this.lock.writeLock()")
	private @Nullable ListenableScheduledFuture<?> checkInternetAccessibleFuture;
	private final RegulationsModule regulations;
	private final ConnectivityModule connectivity;
	private final DeviceModule device;
	private final OriginModuleEventCallback connectivityCallback;
	private final OriginModuleEventCallback deviceMemoryCallback;
	private @Nullable Throwable lowDeviceMemoryError;

	/**
	 * Construct new module.
	 *
	 * @param sdk owning SDK
	 * @param maxCacheSizeBytes maximum size, in bytes, of cache
	 */
	@SuppressWarnings({ "RedundantSuppression", "raw", "rawtypes", "unchecked" })
	private HttpModule(Origin sdk, long maxCacheSizeBytes) {
		super(NAME, sdk);

		this.lock = new ReentrantReadWriteLock();
		this.doneLock = new ReentrantReadWriteLock();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			this.priorityBacklogs = new LinkedTransferQueue[HttpRequest.PRIORITY_CRITICAL + 1];
			for (int i = 0; i < this.priorityBacklogs.length; i++)
				this.priorityBacklogs[i] = new LinkedTransferQueue<>();
		} else {
			this.priorityBacklogs = new ConcurrentLinkedQueue[HttpRequest.PRIORITY_CRITICAL + 1];
			for (int i = 0; i < this.priorityBacklogs.length; i++)
				this.priorityBacklogs[i] = new ConcurrentLinkedQueue<>();
		}
		this.liveRequests = new ConcurrentHashMap<>();
		this.liveRequestCount = new AtomicInteger();
		this.impl = sdk.runInBackground(() -> {
			File cacheDir = resolveHttpCacheDirectory(this);
			Impl impl = null;

			try {
				impl = CronetImplProvider.provide(this, cacheDir, maxCacheSizeBytes);
			} catch (Throwable cause) {
				Logger.warn(TAG, "failed to construct Cronet HTTP module", cause);
			}
			if (impl == null)
				impl = new HttpModuleJdkImpl(this, cacheDir, maxCacheSizeBytes);
			this.impl = impl;
		});
		this.regulations = sdk.loadModule(RegulationsModule.class);
		this.connectivity = sdk.loadModule(ConnectivityModule.class);
		this.device = sdk.loadModule(DeviceModule.class);
		this.connectivityCallback = this::onConnectivityUpdate;
		this.deviceMemoryCallback = this::onDeviceMemoryUpdate;
		this.state =
			this.connectivity.canAccessInternet() ? STATE_INTERNET_ACCESSIBLE :
			STATE_INTERNET_INACCESSIBLE;

		this.connectivity.registerEventCallback(
			this.connectivityCallback,
			ConnectivityModule.CONNECTIVITY_UPDATE_EVENT
		);
		this.device.registerEventCallback(
			this.deviceMemoryCallback,
			DeviceModule.DEVICE_MEMORY_STATE_UPDATE_EVENT
		);
	}

	/**
	 * Resolve HTTP implementation.
	 *
	 * @return implementation
	 */
	@VisibleForTesting
	Impl resolveImpl() {
		Object impl = this.impl;

		if (impl instanceof Future<?>) {
			Futures.awaitUnchecked((Future<?>) impl);
			impl = this.impl;
		}
		return (Impl) impl;
	}

	/**
	 * Test whether module has been destroyed.
	 *
	 * @return {@code true} if, and only if, module is destroyed
	 * @since 1.2
	 */
	public boolean isDestroyed() {
		this.lock.readLock().lock();
		try {
			return this.state == STATE_DESTROYED;
		} finally {
			this.lock.readLock().unlock();
		}
	}

	/**
	 * Enqueue request into backlog.
	 *
	 * @param req request to enqueue
	 */
	@GuardedBy("this.lock.readLock()")
	private void enqueueBacklogRequest(HttpRequest req) {
		// if we're under memory pressure and the request is not critical, drop it
		if (req.priority() <= HttpRequest.PRIORITY_NORMAL) {
			Throwable lowMemErr = this.lowDeviceMemoryError;

			if (lowMemErr != null && this.device.deviceMemoryState().lowMemory()) {
				req.setFailed(lowMemErr, true);
				return;
			}
		}
		try {
			this.priorityBacklogs[req.priority()].add(req);
			Logger.debug(TAG, "backlogged request %s", req.url());
		} catch (IllegalStateException cause) {
			// our beautiful unbound queue is bound
			req.setFailed(cause, true);
		}
	}

	/**
	 * Send a backlogged request, if available, without ensuring internet is {@linkplain
	 * #STATE_INTERNET_ACCESSIBLE accessible}.
	 *
	 * @return {@code true} if, and only if, a backlogged request was sent
	 */
	@GuardedBy("this.lock")
	private boolean doSendBacklogRequest() {
		Impl impl = this.resolveImpl();
		Lock write = this.doneLock.writeLock();
		List<HttpRequest> ineligible = null;

		for (int i = HttpRequest.PRIORITY_CRITICAL; i >= 0; i--) {
			Queue<HttpRequest> backlog = this.priorityBacklogs[i];

			try {
				while (true) {
					HttpRequest req = backlog.poll();

					if (req == null)
						break;
					if (req.isDone())
						continue;
					if (this.state == STATE_INTERNET_INACCESSIBLE && req.requiresNetwork) {
						if (ineligible == null)
							ineligible = new ArrayList<>(1);
						ineligible.add(req);
						continue;
					}
					write.lock();
					try {
						this.liveRequests.put(req, req);
						// unlike `buildAndSendRequest`, `req` can be cancelled by owner
						if (impl.sendRequest(req)) {
							Logger.debug(TAG, "sent backlogged request %s", req.url());
							return true;
						}
						this.liveRequests.remove(req);
					} finally {
						write.unlock();
					}
				}
			} finally {
				if (ineligible != null && !ineligible.isEmpty()) {
					backlog.addAll(ineligible);
					ineligible.clear();
				}
			}
		}
		return false;
	}

	/**
	 * Send a backlogged request, if available.
	 *
	 * @return {@code true} if, and only if, a backlogged request was sent
	 */
	private boolean sendBacklogRequest() {
		Lock read = this.lock.readLock();

		read.lock();
		try {
			return this.state == STATE_INTERNET_ACCESSIBLE && this.doSendBacklogRequest();
		} finally {
			read.unlock();
		}
	}

	/**
	 * Handle request completing erroneously or successfully.
	 *
	 * @param req request to handle
	 */
	void onRequestDone(HttpRequest req) {
	 	// @see HttpRequest#setDone()
		Logger.debug(TAG, "request done %s", req);

		if (this.doneLock.isWriteLockedByCurrentThread()) {
			/*
			 * We're in `doSendBacklogRequest()` and `req` was just cancelled, in which case its
			 * `sendRequest` is going to return `false`, and the requests live slot will be given
			 * up by `doSendBacklogRequest()`. We don't need to do anything.
			 */
			return;
		}

		Lock read = this.doneLock.readLock();

		read.lock();
		try {
			if (this.liveRequests.remove(req) != req) {
				// request wasn't live, don't bother draining backlog
				return;
			}
		} finally {
			read.unlock();
		}

		/*
		 * If we have surpassed live request count, then we don't want to decrement immediately,
		 * as we may have some requests backlogged. If we decrement now, `buildAndSendRequest` may
		 * steal out slot. Instead, if the live request count is above the max live request limit,
		 * see if we have anything in the backlog.
		 */
		if (!this.sendBacklogRequest())
			this.liveRequestCount.decrementAndGet();
	}

	/**
	 * Try and {@linkplain Impl#sendRequest(HttpRequest) request}, if possible.
	 *
	 * @param impl implementation to try and send with
	 * @param req request to try and send
	 * @return {@code true} if live request {@linkplain #liveRequestCount count} did not reached
	 * the {@linkplain Impl#maxLiveRequestCount limit} and {@code req} was sent
	 */
	@GuardedBy("this.lock.readLock()")
	private boolean trySendRequest(Impl impl, HttpRequest req) {
		while (true) {
			int n = this.liveRequestCount.get();

			if (n >= impl.maxLiveRequestCount)
				return false;
			if (this.liveRequestCount.compareAndSet(n, n + 1))
				break;
		}

		this.liveRequests.put(req, req);
		if (impl.sendRequest(req)) {
			Logger.debug(TAG, "sent request %s", req.url());
		} else {
			this.liveRequests.remove(req);
			if (!this.doSendBacklogRequest())
				this.liveRequestCount.decrementAndGet();
		}
		return true;
	}

	/**
	 * Build request from {@linkplain HttpRequest.Builder builder} and send it.
	 *
	 * @param builder builder to build request from
	 * @return resulting request
	 * @throws IllegalStateException module has been {@linkplain #isDestroyed() destroyed}
	 */
	HttpRequest buildAndSendRequest(HttpRequest.Builder builder) {
		int state;
		HttpRequest req;
		Impl impl = this.resolveImpl();
		Lock read = this.lock.readLock();

		read.lock();
		try {
			state = this.state;
			Preconditions.checkState(state != STATE_DESTROYED, "module destroyed");

			req = impl.buildRequest(builder);
			if (
				(state == STATE_INTERNET_ACCESSIBLE || !req.requiresNetwork) &&
				this.trySendRequest(impl, req)
			) {
				return req;
			}
			this.enqueueBacklogRequest(req);
		} finally {
			read.unlock();
		}
		if (state == STATE_INTERNET_INACCESSIBLE)
			this.scheduleCheckInternetAccessible();
		return req;
	}

	/**
	 * Construct new request builder.
	 * <p>The builder returned can be used to prepare a request. The builder's {@link
	 * HttpRequest.Builder#send()} method can be used to dispatch the request.
	 *
	 * @param url URL request is to
	 * @param meth method to request with
	 * @param listener request lifecycle listener
	 * @return resulting request builder
	 * @throws IllegalArgumentException {@code url} is malformed or {@code meth} is invalid
	 * @since 1.2
	 */
	public HttpRequest.Builder
	newRequestBuilder(String url, @HttpMethod String meth, HttpRequest.Listener listener) {
		return new HttpRequest.Builder(this, url, meth, listener);
	}

	/**
	 * Handle device memory state updates.
	 */
	private void onDeviceMemoryUpdate(
		@SuppressWarnings("unused")
		OriginModule source,
		@SuppressWarnings("unused")
		@OriginModuleEventName String name,
		@SuppressWarnings("unused")
		@Nullable Object data,
		@SuppressWarnings("unused")
		long timestamp
	) {
		DeviceMemoryState state = this.device.deviceMemoryState();

		if (!state.lowMemory()) {
			this.lowDeviceMemoryError = null;
			return;
		}

		int num = 0;
		Throwable cause = new IllegalStateException("device low on memory");

		cause.fillInStackTrace();
		this.lowDeviceMemoryError = cause;
		for (int i = HttpRequest.PRIORITY_NORMAL; i >= 0; i--) {
			while (true) {
				HttpRequest req = this.priorityBacklogs[i].poll();

				if (req == null)
					break;
				req.setFailed(cause, true);
				num++;
			}
		}
		Logger.info(TAG, "released %s non-critical requests, device under memory pressure", num);
	}

	/**
	 * Check whether connection to host on port succeeds.
	 *
	 * @param host host to connect to
	 * @param port port to connect on
	 * @return {@code true} if, and only if, connection was successful
	 */
	private static boolean checkCanConnect(Object host, int port) {
		try (Socket sock = new Socket()) {
			if (host instanceof String) {
				host = InetAddress.getByName((String) host);
			} else if (host instanceof Integer) {
				host = InetAddress.getByAddress(
					ByteBuffer.allocate(4)
						.putInt(0, (int) host)
						.array()
				);
			}
			sock.connect(new InetSocketAddress((InetAddress) host, port), 15000);
			Logger.debug(TAG, "connected successfully to %s:%s", host, port);
			return true;
		} catch (Throwable cause) {
			Logger.debug(TAG, "could not connect to %s:%s", host, port, cause);
			return false;
		}
	}

	/**
	 * Check whether internet is accessible by attempting to connect to well known hosts.
	 *
	 * @return {@code true} if, and only if, internet is accessible
	 */
	private boolean checkCanConnectWellKnown() {
		for (Queue<HttpRequest> priorityBacklog : this.priorityBacklogs) {
			HttpRequest req = priorityBacklog.peek();

			if (req == null)
				continue;

			int port = req.url().getPort();

			if (port == -1)
				port = req.url().getDefaultPort();
			if (port != -1 && checkCanConnect(req.url().getHost(), port))
				return true;
		}

		boolean cn = this.regulations.isPiplApplicable();

		for (int ip : new int[] {
			cn ? 0x1020408 : 0x8080808,
			cn ? 0xd2020408 : 0x8080404,
			0x1010101
		}) {
			if (checkCanConnect(ip, 53))
				return true;
		}
		return checkCanConnect("example.com", 443);
	}

	/**
	 * Update {@linkplain #state state} to internet {@linkplain #STATE_INTERNET_INACCESSIBLE
	 * inaccessible}.
	 * <p>Upon return, if {@code this} was not {@linkplain #STATE_DESTROYED destroyed}, the state
	 * is updated to internet inaccessible, and a check is {@linkplain
	 * #doScheduleCheckInternetAccessible(int) scheduled} after a {@linkplain
	 * #INTERNET_ACCESSIBILITY_RECHECK_DELAY_MILLIS delay}.
	 */
	@WorkerThread
	private void setInternetInaccessible() {
		Lock write = this.lock.writeLock();

		write.lock();
		try {
			if (this.state == STATE_DESTROYED)
				return;
			this.state = STATE_INTERNET_INACCESSIBLE;
			this.doScheduleCheckInternetAccessible(INTERNET_ACCESSIBILITY_RECHECK_DELAY_MILLIS);
		} finally {
			write.unlock();
		}
		Logger.debug(TAG, "internet inaccessible");
	}

	/**
	 * Update {@linkplain #state state} to internet {@linkplain #STATE_INTERNET_ACCESSIBLE
	 * accessible}.
	 * <p>Upon return, if {@code this} was not {@linkplain #STATE_DESTROYED destroyed}, the state
	 * is updated to internet accessible, and any backlogged requests are sent, up to the available
	 * limit.
	 */
	@WorkerThread
	private void setInternetAccessible() {
		Impl impl = this.resolveImpl();
		Lock write = this.lock.writeLock();

		write.lock();
		try {
			if (this.state != STATE_INTERNET_INACCESSIBLE)
				return;

			this.state = STATE_INTERNET_ACCESSIBLE;
			/*
			 * before releasing the write lock, drain the backlog, we can mess with
			 * `liveRequestCount` as we want, because `buildAndSendRequest()` won't be sending any
			 * requests
			 */
			int n = this.liveRequestCount.get();

			while (n < impl.maxLiveRequestCount) {
				if (!this.doSendBacklogRequest())
					break;
				n = this.liveRequestCount.incrementAndGet();
			}
		} finally {
			write.unlock();
		}
		Logger.debug(TAG, "internet accessible");
	}

	/**
	 * Check whether internet is accessible.
	 * <p>If internet is now accessible and was previously {@linkplain #STATE_INTERNET_INACCESSIBLE
	 * inaccessible}, then it is marked accessible and any backlogged requests, up to live request
	 * {@linkplain Impl#maxLiveRequestCount limit}, are sent. Otherwise, internet is set
	 * inaccessible and another check is scheduled.
	 */
	@WorkerThread
	private void checkInternetAccessible() {
		ListenableFuture<?> fut = this.checkInternetAccessibleFuture;

		try {
			if (this.checkCanConnectWellKnown())
				this.setInternetAccessible();
			else
				this.setInternetInaccessible();
		} finally {
			if (this.checkInternetAccessibleFuture == fut)
				this.checkInternetAccessibleFuture = null;
		}
	}

	/**
	 * Schedule internet accessibility {@linkplain #checkInternetAccessible() check}.
	 */
	@GuardedBy("this.lock.writeLock()")
	private void doScheduleCheckInternetAccessible(int delayMsec) {
		//noinspection resource
		this.checkInternetAccessibleFuture =
			super.sdk().backgroundIoExecutor()
				.schedule(this::checkInternetAccessible, delayMsec, TimeUnit.MILLISECONDS);
	}

	/**
	 * Handle connectivity module update.
	 */
	private void onConnectivityUpdate(
		@SuppressWarnings("unused")
		OriginModule source,
		@SuppressWarnings("unused")
		@OriginModuleEventName String name,
		@SuppressWarnings("unused")
		@Nullable Object data,
		@SuppressWarnings("unused")
		long timestamp
	) {
		boolean newAcc = this.connectivity.canAccessInternet();
		boolean currAcc = (this.state == STATE_INTERNET_ACCESSIBLE);

		if (newAcc == currAcc)
			return;

		Logger.debug(TAG, "internet %s possibly access", newAcc ? "is" : "is not");

		/*
		 * We're thinking internet {is,isn't} accessible when connectivity module is saying
		 * it {isn't,is}. If we have a check scheduled, see if it's soon enough, if so, just let
		 * it sort this out; otherwise, check ourself.
		 */
		Lock write = this.lock.writeLock();

		write.lock();
		try {
			if (this.state == STATE_DESTROYED)
				return;
			if (!newAcc)
				this.state = STATE_INTERNET_INACCESSIBLE;

			ListenableScheduledFuture<?> fut = this.checkInternetAccessibleFuture;

			if (fut != null && (fut.getDelay(TimeUnit.MILLISECONDS) <= 10 || !fut.cancel(false)))
				return;
			this.doScheduleCheckInternetAccessible(0);
		} finally {
			write.unlock();
		}
	}

	/**
	 * Schedule internet accessibility {@linkplain #checkInternetAccessible() check}, if required.
	 */
	private void scheduleCheckInternetAccessible() {
		Lock write = this.lock.writeLock();

		write.lock();
		try {
			if (
				this.state == STATE_INTERNET_INACCESSIBLE &&
				this.checkInternetAccessibleFuture == null
			) {
				this.doScheduleCheckInternetAccessible(0);
			}
		} catch (RejectedExecutionException cause) {
			Logger.warn(TAG, "failed to schedule internet accessibility check", cause);
		} finally {
			write.unlock();
		}
	}

	@Override
	@CallSuper
	protected void destroy() {
		Lock write = this.lock.writeLock();

		write.lock();
		try {
			if (this.state == STATE_DESTROYED)
				return;
			this.state = STATE_DESTROYED;
			Futures.cancel(this.checkInternetAccessibleFuture, false);
			this.checkInternetAccessibleFuture = null;
		} finally {
			write.unlock();
		}

		this.connectivity.unregisterEventCallback(
			this.connectivityCallback,
			ConnectivityModule.CONNECTIVITY_UPDATE_EVENT
		);
		this.device.unregisterEventCallback(
			this.deviceMemoryCallback,
			DeviceModule.DEVICE_MEMORY_STATE_UPDATE_EVENT
		);

		IllegalStateException error = new IllegalStateException("module destroyed");

		error.fillInStackTrace();

		// everything in the backlog gets cancelled immediately
		for (Queue<HttpRequest> q : this.priorityBacklogs) {
			while (true) {
				HttpRequest req = q.poll();

				if (req == null)
					break;
				req.setFailed(error, false);
			}
		}

		// live requests will only be removed from, nothing will be added since state is destroyed
		for (HttpRequest req : this.liveRequests.values())
			req.setFailed(error, false);
		this.resolveImpl().destroy();
	}
}
