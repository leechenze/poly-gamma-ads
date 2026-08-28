// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.core;

import android.net.http.HttpResponseCache;
import android.os.SystemClock;
import android.util.Pair;

import androidx.annotation.AnyThread;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;
import androidx.core.util.Supplier;

import org.polygamma.android.origin.util.Logger;
import org.polygamma.android.origin.util.Preconditions;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * {@link HttpModule} implementation using {@link HttpURLConnection}.
 */
final class HttpModuleJdkImpl extends HttpModule.Impl {

	private static final String TAG = HttpModuleJdkImpl.class.getSimpleName();

	/**
	 * Maximum number of live requests at any given point in time.
	 * <p>Since thread-per-request model is used, this number should be large enough to allow
	 * for sufficient parallelism, but small enough to avoid saturating system resources.
	 */
	private static final int MAX_LIVE_REQUEST_COUNT	= 12;

	/**
	 * Maximum duration, in milliseconds, a request sender {@linkplain RequestSenderTask task} will
	 * wait for a request before exiting.
	 */
	private static final int MAX_REQUEST_SENDER_TASK_IDLE_MILLIS = 600;

	/**
	 * Size, in bytes, of per request sender {@linkplain RequestSenderTask task} I/O buffer.
	 */
	private static final int IO_BUFFER_SIZE_BYTES = 8192;

	/**
	 * Default timeout, in milliseconds, for per-request I/O.
	 */
	private static final int IO_TIMEOUT_MILLIS = 30000;

	/**
	 * HTTP {@linkplain HttpRequest request} implementation using {@link HttpURLConnection}.
	 */
	private static final class RequestImpl extends HttpRequest {

		private final @HttpMethod String method;
		private final List<Pair<String, String>> headers;
		final @Nullable ByteBuffer body;

		/**
		 * Construct a new request.
		 *
		 * @param builder builder to initialize from
		 */
		RequestImpl(Builder builder) {
			super(builder, builder.url);
			this.method = builder.method;
			this.headers =
				builder.headers == null ? Collections.emptyList() :
				new ArrayList<>(builder.headers);
			this.body = builder.body;
		}

		/**
		 * Release connection and notify task thread of release.
		 *
		 * @param thrAndConn tuple of task thread and connection, respectively
		 */
		private void doRelease(Pair<Thread, HttpURLConnection> thrAndConn) {
			// https://github.com/square/okhttp/issues/657
			if (thrAndConn.first != Thread.currentThread()) {
				try {
					thrAndConn.first.interrupt();
				} catch (Throwable cause) {
					Logger.warn(TAG, "failed to interrupt request service thread", cause);
				}
			}
			try {
				thrAndConn.second.disconnect();
			} catch (Throwable cause) {
				Logger.warn(TAG, "failed to close request connection", cause);
			}
		}

		/**
		 * Attempt to update request state to a redirect state.
		 *
		 * @param url response redirect URL
		 * @param statusCode response status code
		 * @param statusMsg response status message
		 * @param resolveHdrs response headers resolver
		 * @return {@code true} if, and only if, redirect should be followed
		 * @throws Exception error encountered
		 */
		@WorkerThread
		@SuppressWarnings("unchecked")
		boolean tryRedirect(
			URL url,
			int statusCode,
			String statusMsg,
			Supplier<Map<String, List<String>>> resolveHdrs
		) throws Exception {
			Object state = super.updateState(url);

			if (state == this)
				return false;
			try {
				return super.listener().onRedirect(this, url, statusCode, statusMsg, resolveHdrs);
			} finally {
				if (state instanceof Pair)
					this.doRelease((Pair<Thread, HttpURLConnection>) state);
			}
		}

		/**
		 * Prepare a connection for request.
		 *
		 * @param conn connection to prepare
		 * @throws IOException I/O error was encountered
		 */
		@WorkerThread
		private void prepareConnection(HttpURLConnection conn) throws IOException {
			conn.setRequestMethod(this.method);
			for (Pair<String, String> hdr : this.headers)
				conn.addRequestProperty(hdr.first, hdr.second);
			conn.setDoOutput(this.body != null);
			conn.setInstanceFollowRedirects(false);
			conn.setConnectTimeout(IO_TIMEOUT_MILLIS);
			conn.setReadTimeout(IO_TIMEOUT_MILLIS);
			conn.setUseCaches(!super.requiresNetwork);
		}

		/**
		 * Open connection to target URL, if possible.
		 *
		 * @return opened connection or {@code null} if request is {@linkplain #isDone() done}
		 * @throws IOException I/O error was encountered
		 * @throws IllegalStateException request state is malformed
		 */
		@WorkerThread
		@SuppressWarnings("unchecked")
		@Nullable HttpURLConnection openConnection() throws IOException {
			Pair<Object, Object> state = super.computeState((url) -> {
				Preconditions.checkState(url instanceof URL, "expected request URL");
				try {
					//noinspection DataFlowIssue
					return new Pair<>(Thread.currentThread(), ((URL) url).openConnection());
				} catch (IOException cause) {
					return cause;
				}
			});

			if (state == null)
				return null;
			if (state.second instanceof Throwable)
				throw new IOException((Throwable) state.second);

			Pair<Thread, HttpURLConnection> thrAndConn =
				(Pair<Thread, HttpURLConnection>) state.second;

			if (state.first == this) {
				this.doRelease(thrAndConn);
				return null;
			}

			try {
				this.prepareConnection(thrAndConn.second);
			} catch (IOException cause) {
				/*
				 * any i/o exceptions thrown here are *not* recoverable, and are usually caused by
				 * bad configuration.
				 */
				throw new IllegalStateException("failed to prepare connection", cause);
			}
			return thrAndConn.second;
		}

		@Override
		@SuppressWarnings("unchecked")
		void release(@Nullable Object state) {
			if (state instanceof Pair)
				this.doRelease((Pair<Thread, HttpURLConnection>) state);
		}
	}

	/**
	 * Request sending task bound to a single thread.
	 */
	@WorkerThread
	private final class RequestSenderTask implements Runnable {
		final ArrayList<RequestImpl> worklist;
		private @Nullable ByteBuffer buffer;

		/**
		 * Construct new task with an initial request.
		 *
		 * @param initialReq initial request to send
		 */
		@AnyThread
		RequestSenderTask(RequestImpl initialReq) {
			this.worklist = new ArrayList<>(1);
			this.worklist.add(initialReq);
		}

		/**
		 * Handle request redirect.
		 *
		 * @param req request which was redirected
		 * @param conn connection on which request was redirected
		 * @throws Exception error encountered
		 */
		private void handleRedirect(RequestImpl req, HttpURLConnection conn) throws Exception {
			String loc = conn.getHeaderField("Location");
			URL url;

			try {
				url = new URL(Preconditions.checkNotNullElse(conn.getURL(), req.url()), loc);
			} catch (MalformedURLException | NullPointerException cause) {
				throw new IllegalStateException("invalid redirect location", cause);
			}

			if (req.tryRedirect(
				url,
				conn.getResponseCode(),
				conn.getResponseMessage(),
				conn::getHeaderFields
			)) {
				this.worklist.add(req);
			} else {
				req.setSuccess();
			}
		}

		/**
		 * Get or allocate I/O buffer.
		 *
		 * @return I/O buffer
		 */
		private ByteBuffer getOrAllocateBuffer() {
			ByteBuffer buff = this.buffer;

			if (buff == null) {
				buff = ByteBuffer.allocate(IO_BUFFER_SIZE_BYTES);
				this.buffer = buff;
			}
			buff.clear();
			return buff;
		}

		/**
		 * Read body of response for a request.
		 *
		 * @param req request to read response of
		 * @param conn connection on which response was received
		 * @throws Exception error encountered
		 */
		private void readResponseBody(RequestImpl req, HttpURLConnection conn) throws Exception {
			InputStream err = conn.getErrorStream();
			boolean any = false;

			try (InputStream in = err == null ? conn.getInputStream() : err) {
				ByteBuffer buff = this.getOrAllocateBuffer();

				while (!req.isDone()) {
					int nb = in.read(
						buff.array(),
						buff.arrayOffset() + buff.position(),
						buff.remaining()
					);

					if (req.isDone())
						break;
					if (nb == -1) {
						if (any)
							req.listener().onResponseBodyPart(req, (ByteBuffer) buff.flip(), true);
						break;
					}
					buff.position(buff.position() + nb);
					if (!buff.hasRemaining()) {
						req.listener().onResponseBodyPart(req, (ByteBuffer) buff.flip(), false);
						buff.clear();
					}
					any = true;
				}
			}
		}

		/**
		 * Write request body to connection.
		 *
		 * @param req request to write body of
		 * @param conn connection to write body to
		 * @param src body to write
		 * @throws IOException I/O error was encountered
		 */
		private void writeRequestBody(RequestImpl req, HttpURLConnection conn, ByteBuffer src)
		throws IOException {
			conn.setFixedLengthStreamingMode(src.remaining());

			try (OutputStream out = conn.getOutputStream()) {
				if (src.hasArray()) {
					if (req.isDone())
						return;
					out.write(src.array(), src.arrayOffset() + src.position(), src.remaining());
					return;
				}

				ByteBuffer buff = this.getOrAllocateBuffer();

				while (src.hasRemaining()) {
					if (req.isDone())
						return;
					if (buff.remaining() >= src.remaining()) {
						buff.put(src);
					} else {
						int srcPos = src.position();
						int srcLim = src.limit();

						src.limit(srcPos + buff.limit());
						buff.put(src);
						src.limit(srcLim);
					}
					out.write(buff.array(), buff.arrayOffset(), buff.position());
					buff.clear();
				}
			}
		}

		/**
		 * Send a request.
		 *
		 * @param req request to send
		 * @throws Exception error encountered while sending request
		 */
		private void send(RequestImpl req) throws Exception {
			HttpURLConnection conn;

			try {
				conn = req.openConnection();
			} catch (IllegalStateException cause) {
				req.setFailed(cause, false);
				Logger.warn(TAG, "request %s state is malformed", req.url(), cause);
				return;
			}
			if (conn == null)
				return;

			if (req.body != null)
				this.writeRequestBody(req, conn, (ByteBuffer) req.body.clear());
			if (req.isDone())
				return;

			int status = conn.getResponseCode();

			if (status >= 300 && status <= 399) {
				this.handleRedirect(req, conn);
				return;
			} else if (req.isDone()) {
				return;
			}
			if (status == -1) {
				req.setFailed(new IllegalStateException("invalid HTTP status code"), false);
				return;
			}

			req.listener()
				.onResponseStart(req, status, conn.getResponseMessage(), conn::getHeaderFields);
			this.readResponseBody(req, conn);
			req.setSuccess();
		}

		/**
		 * Retrieve next request to send.
		 *
		 * @return next request or {@code null} if task is complete
		 * @throws InterruptedException interrupted while waiting for next request
		 */
		private @Nullable RequestImpl next() throws InterruptedException {
			if (!this.worklist.isEmpty())
				return this.worklist.remove(0);
			if (HttpModuleJdkImpl.this.isDestroyed())
				return null;

			Object rv = HttpModuleJdkImpl.this.pendingRequest
				.poll(MAX_REQUEST_SENDER_TASK_IDLE_MILLIS, TimeUnit.MILLISECONDS);

			if (rv == null || rv == HttpModuleJdkImpl.this)
				return null;
			return (RequestImpl) rv;
		}

		@Override
		public void run() {
			HttpModuleJdkImpl.this.threadRequestSenderTasks.put(Thread.currentThread(), this);
			try {
				while (true) {
					RequestImpl req;

					try {
						req = this.next();
					} catch (InterruptedException cause) {
						if (HttpModuleJdkImpl.this.isDestroyed())
							break;
						Logger.info(TAG, "request sender task interrupted", cause);
						continue;
					}
					if (req == null)
						break;
					try {
						this.send(req);
					} catch (Throwable cause) {
						req.setFailed(cause, cause instanceof IOException);
						Logger.info(TAG, "failed to send request to %s", req.url(), cause);
					}
					if (!req.requiresNetwork)
						HttpModuleJdkImpl.this.flushCache();
				}
			} catch (Throwable cause) {
				// in case we encountered an error anywhere, clear out our worklist
				for (RequestImpl req : this.worklist)
					req.setFailed(cause, true);
				Logger.warn(TAG, "request sender task failed", cause);
			} finally {
				HttpModuleJdkImpl.this.threadRequestSenderTasks.remove(Thread.currentThread());
			}
		}
	}

	/**
	 * Try to install HTTP response cache.
	 *
	 * @param dir directory to persist cache in, or {@code null}
	 * @param sizeBytes maximum size, in bytes, of cache
	 */
	@SuppressWarnings("resource")
	private static void tryInstallHttpResponseCache(@Nullable File dir, long sizeBytes) {
		if (dir == null)
			return;

		try {
			if (HttpResponseCache.getInstalled() == null) {
				HttpResponseCache.install(dir, sizeBytes);
				Logger.debug(TAG, "installed HTTP response cache in %s", dir);
			}
		} catch (Throwable cause) {
			Logger.warn(TAG, "failed to install HTTP response cache in %s", dir, cause);
		}
	}

	/*
	 * Shared queue that all idle tasks poll from. During normal operations, this'll be populated
	 * with requests. During shutdown, this'll be populated with `this`.
	 */
	private final SynchronousQueue<Object> pendingRequest;
	@VisibleForTesting
	final ConcurrentHashMap<Thread, RequestSenderTask> threadRequestSenderTasks;
	private final Lock cacheFlushLock;

	/**
	 * Construct new client.
	 *
	 * @param module owning module
	 * @param cacheDir directory to store disk cache in or {@code null} to disable disk cache
	 * @param maxCacheSizeBytes maximum size, in bytes, of disk cache
	 */
	HttpModuleJdkImpl(HttpModule module, @Nullable File cacheDir, long maxCacheSizeBytes) {
		super(module, MAX_LIVE_REQUEST_COUNT);
		this.pendingRequest = new SynchronousQueue<>();
		this.threadRequestSenderTasks = new ConcurrentHashMap<>(MAX_LIVE_REQUEST_COUNT);
		this.cacheFlushLock = new ReentrantLock();

		tryInstallHttpResponseCache(cacheDir, maxCacheSizeBytes);
	}

	/**
	 * Flush response cache, if required.
	 */
	@WorkerThread
	private void flushCache() {
		// if someone else is flushing the cache, bail
		if (!this.cacheFlushLock.tryLock())
			return;
		try {
			HttpResponseCache cache = HttpResponseCache.getInstalled();

			if (cache != null)
				cache.flush();
		} finally {
			this.cacheFlushLock.unlock();
		}
	}

	@Override
	boolean sendRequest(HttpRequest req) {
		RequestImpl reqImpl = (RequestImpl) req;

		if (reqImpl.isDone())
			return false;

		RequestSenderTask task = this.threadRequestSenderTasks.get(Thread.currentThread());

		/*
		 * If we're in a task, go ahead and enqueue request to its worklist; otherwise, see if
		 * there's any task waiting on `pendingRequest`, if so, punt the request to them. If we
		 * have no tasks at all, create one.
		 */
		if (task != null) {
			task.worklist.add(reqImpl);
			return true;
		}
		if (this.pendingRequest.offer(reqImpl))
			return true;

		try {
			//noinspection resource
			super.module.sdk()
				.backgroundIoExecutor()
				.execute(new RequestSenderTask(reqImpl));
			return true;
		} catch (RejectedExecutionException cause) {
			reqImpl.setFailed(cause, true);
			return false;
		}
	}

	@Override
	HttpRequest buildRequest(HttpRequest.Builder builder) {
		return new RequestImpl(builder);
	}

	@Override
	protected void destroy() {
		for (
			int i = 0;
			i < (MAX_LIVE_REQUEST_COUNT * 12) &&
			!this.threadRequestSenderTasks.isEmpty();
			i++
		) {
			if (this.pendingRequest.offer(this))
				SystemClock.sleep(10);
		}
		// anyone that's still alive will just get an interrupt
		for (
			Map.Entry<Thread, RequestSenderTask> en :
			this.threadRequestSenderTasks.entrySet()
		) {
			if (this.threadRequestSenderTasks.remove(en.getKey()) != null)
				en.getValue();
		}
	}
}
