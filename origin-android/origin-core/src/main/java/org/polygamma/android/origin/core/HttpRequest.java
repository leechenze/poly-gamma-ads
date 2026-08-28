// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.core;

import android.util.Pair;

import androidx.annotation.AnyThread;
import androidx.annotation.GuardedBy;
import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.WorkerThread;
import androidx.core.util.Function;
import androidx.core.util.Supplier;

import org.polygamma.android.origin.util.Logger;
import org.polygamma.android.origin.util.Preconditions;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * HTTP request.
 * <p>Instances of this represent an HTTP request. HTTP requests can be built using {@link
 * HttpModule#newRequestBuilder(String, String, HttpRequest.Listener)}, and initiated by invoking
 * the resulting builder's {@link Builder#send()} method. The lifecycle of a request is tracked
 * through the {@linkplain Listener listener} that is specified when its builder is constructed.
 * Requests allow an opaque {@linkplain #attachment() value} to be {@linkplain
 * Builder#attachment(Object) attached} to them. This opaque value can be used within a shared
 * listener.
 * <h2>Priorities</h2>
 * <p>Requests may optionally be {@linkplain Builder#priority(int) assigned} a property. If a
 * request has no priority assigned, it's given the {@linkplain #PRIORITY_NORMAL normal} priority.
 * The priority of a request determines how quickly it will be processed, given device and network
 * constraints. The table below can be used as a <i>reference</i> point for request priorities.
 * <table>
 *     <caption>Request Priorities</caption>
 *     <thead>
 *         <tr>
 *             <th>Priority</th>
 *             <th>Description</th>
 *         </tr>
 *     </thead>
 *     <tbody>
 *         <tr>
 *             <td>{@linkplain #PRIORITY_CRITICAL Critical}</td>
 *             <td>
 *                 Request will not be cancelled when device is under memory pressure. Request
 *                 will be dispatched as soon as possible.
 *             </td>
 *         </tr>
 *         <tr>
 *             <td>{@linkplain #PRIORITY_NORMAL Normal}</td>
 *             <td>
 *                 Request will be dispatched after any critical priority requests. Request may
 *                 be cancelled if device is under memory pressure <i>and</i> request has not yet
 *                 started.
 *             </td>
 *         </tr>
 *         <tr>
 *             <td>{@linkplain #PRIORITY_LOW Low}</td>
 *             <td>
 *                 Request will be dispatched after any normal priority requests. Request will
 *                 be cancelled if device is under memory pressure.
 *             </td>
 *         </tr>
 *         <tr>
 *             <td>{@linkplain #PRIORITY_IDLE Idle}</td>
 *             <td>
 *                 Request will be dispatched after any low priority requests. Request will
 *                 be cancelled if device is under memory pressure.
 *             </td>
 *         </tr>
 *     </tbody>
 * </table>
 *
 * @since 1.2
 * @see HttpModule#newRequestBuilder(String, String, Listener)
 */
@AnyThread
public abstract class HttpRequest {

	private static final String TAG = HttpRequest.class.getSimpleName();

	/**
	 * Request must be serviced as soon as possible.
	 *
	 * @since 1.2
	 * @see #priority()
	 */
	public static final @Priority int PRIORITY_CRITICAL		= 3;

	/**
	 * Request can be serviced after {@linkplain #PRIORITY_CRITICAL critical} requests are
	 * processed.
	 *
	 * @since 1.2
	 * @see #priority()
	 */
	public static final @Priority int PRIORITY_NORMAL		= 2;

	/**
	 * Request can be serviced after {@linkplain #PRIORITY_NORMAL normal} requests are
	 * processed.
	 *
	 * @since 1.2
	 * @see #priority()
	 */
	public static final @Priority int PRIORITY_LOW			= 1;

	/**
	 * Request can be serviced when resources are available.
	 *
	 * @since 1.2
	 * @see #priority()
	 */
	public static final @Priority int PRIORITY_IDLE			= 0;

	/**
	 * HTTP request {@linkplain #priority() priority} enumeration value marker.
	 *
	 * @since 1.2
	 * @see #priority()
	 */
	@Documented
	@Retention(RetentionPolicy.SOURCE)
	@Target(ElementType.TYPE_USE)
	@IntDef({ PRIORITY_CRITICAL, PRIORITY_IDLE, PRIORITY_LOW, PRIORITY_NORMAL })
	public @interface Priority {
	}

	/**
	 * HTTP {@linkplain HttpRequest request} lifecycle listener.
	 * <p>Listener methods <i>may</i> be invoked on the <i>same</i> thread which processes HTTP
	 * requests. As such, processing of all requests may be effected if a listener method takes
	 * too long.
	 *
	 * @since 1.2
	 */
	@WorkerThread
	public interface Listener {
		/**
		 * Error encountered while servicing HTTP request.
		 * <p>This is invoked whenever a request encounters an error. If the error is recoverable,
		 * {@code recoverable} will be {@code true}. Recoverable errors are errors which are
		 * temporary, for example, errors caused by device network connectivity.
		 *
		 * @param req request which encountered error
		 * @param cause cause of error
		 * @param recoverable {@code true} if, and only if, error is recoverable
		 * @since 1.2
		 */
		@AnyThread
		void onFailed(HttpRequest req, Throwable cause, boolean recoverable);

		/**
		 * Request cancelled.
		 *
		 * @param req request which was cancelled
		 * @since 1.2
		 * @see HttpRequest#cancel()
		 */
		@AnyThread
		void onCancel(HttpRequest req);

		/**
		 * Remote server redirected an HTTP request.
		 * <p>This is invoked whenever the remove server sends a redirect HTTP response. The
		 * redirect status code and URL, as specified by the server, are specified in {@code
		 * statusCode} and {@code url}, respectively. This may return {@code true} if {@code url}
		 * should be loaded, with the same headers {@code req} was constructed with.
		 * <p>If this returns {@code false}, then the request is {@linkplain
		 * #onSuccess(HttpRequest) completed} immediately.
		 *
		 * @param req request which encountered redirect
		 * @param url response redirect URL
		 * @param statusCode response status code
		 * @param statusMsg response status message
		 * @param resolveHdrs response headers resolver
		 * @throws Exception error to invoke {@link #onFailed(HttpRequest, Throwable, boolean)}
		 * with
		 * @return {@code true} if, and only if, redirect should be followed
		 * @since 1.2
		 */
		boolean onRedirect(
			HttpRequest req,
			URL url,
			int statusCode,
			String statusMsg,
			Supplier<Map<String, List<String>>> resolveHdrs
		) throws Exception;

		/**
		 * HTTP response, post all {@linkplain #onRedirect(HttpRequest, URL, int, String,
		 * Supplier) redirects}, is being processed.
		 * <p>This is invoked, after any redirects, when a response is being processed for an
		 * HTTP request. The {@code resolveHdrs} supplier can be used to resolve the response
		 * headers of the HTTP response. Note that {@code resolveHdrs} may be invoked only until
		 * this method returns, attempting to invoke it after this returns will result in undefined
		 * behavior.
		 *
		 * @param req request for which a response is being processed
		 * @param statusCode response status code
		 * @param statusMsg response status message
		 * @param resolveHdrs response headers resolver
		 * @throws Exception error to invoke {@link #onFailed(HttpRequest, Throwable, boolean)}
		 * with
		 * @since 1.2
		 */
		void onResponseStart(
			HttpRequest req,
			int statusCode,
			String statusMsg,
			Supplier<Map<String, List<String>>> resolveHdrs
		) throws Exception;

		/**
		 * Body part received for HTTP request.
		 * <p>This is invoked zero or more times, <i>after</i> response processing {@linkplain
		 * #onResponseStart(HttpRequest, int, String, Supplier) starts}, with body payloads
		 * received from the response. The body payload {@code body} is valid <i>only</i> within
		 * the scope of an invocation of this. Attempting to use {@code body} after this returns
		 * will result in undefined behavior.
		 *
		 * @param req request for which body part was received
		 * @param body body part
		 * @param last {@code true} if, and only if, {@code body} represents last part of body
		 * @throws Exception error to invoke {@link #onFailed(HttpRequest, Throwable, boolean)} with
		 * @since 1.2
		 */
		void onResponseBodyPart(HttpRequest req, ByteBuffer body, boolean last) throws Exception;

		/**
		 * Response has been processed successfully for an HTTP request.
		 * <p>This is invoked after response processing {@linkplain
		 * #onResponseStart(HttpRequest, int, String, Supplier) starts} and all body parts are
		 * {@linkplain #onResponseBodyPart(HttpRequest, ByteBuffer, boolean) received}. After this
		 * is invoked, {@code req} can be considered complete.
		 *
		 * @param req request for which response was completely processed
		 * @since 1.2
		 */
		void onSuccess(HttpRequest req);
	}

	/**
	 * HTTP {@linkplain HttpRequest request} builder.
	 * <p>Request builders are constructed using {@link
	 * HttpModule#newRequestBuilder(String, String, Listener)}. Builders can be used to configure
	 * and {@linkplain #send() send} a request.
	 *
	 * @since 1.2
	 * @see HttpModule#newRequestBuilder(String, String, Listener)
	 */
	@AnyThread
	public static final class Builder {
		/**
		 * Owning module.
		 */
		final HttpModule module;

		/**
		 * URL request is to.
		 */
		final URL url;

		/**
		 * Method request is performed with.
		 */
		final @HttpMethod String method;

		/**
		 * Request lifecycle listener.
		 */
		final Listener listener;

		/** {@link #addHeader(String, String)} */
		@Nullable List<Pair<String, String>> headers;

		/** {@link #body(ByteBuffer)} */
		@Nullable ByteBuffer body;

		/** {@link #attachment(Object)} */
		@Nullable Object attachment;

		/** {@link #priority(int)} */
		@IntRange(from = 0, to = 4) int priority;

		private boolean requiresNetwork;
		private boolean hasContentType;

		/**
		 * Construct new empty builder.
		 *
		 * @param module owning module
		 * @param url URL request is to
		 * @param meth method to perform request with
		 * @param listener request lifecycle listener
		 * @throws IllegalArgumentException {@code url} is malformed or {@code meth} is invalid
		 */
		Builder(HttpModule module, String url, @HttpMethod String meth, Listener listener) {
			Preconditions.checkArgument(
				"DELETE".equals(meth) ||
				"GET".equals(meth) ||
				"HEAD".equals(meth) ||
				"POST".equals(meth) ||
				"PUT".equals(meth),
				"invalid request method"
			);
			this.module = module;
			try {
				this.url = new URL(url);
			} catch (MalformedURLException cause) {
				throw new IllegalArgumentException("invalid request URL", cause);
			}
			this.method = meth;
			this.listener = Preconditions.checkNotNull(listener);
			this.priority = PRIORITY_NORMAL;
			this.requiresNetwork = !"GET".equals(meth) && !"HEAD".equalsIgnoreCase(meth);
		}

		/**
		 * Set value to attach to resulting request.
		 * <p>The resulting request have {@code val} {@linkplain HttpRequest#attachment() attached}
		 * to it. Attachment values are opaque, and can be used to include custom information in
		 * an HTTP request, possibly for identification purposes.
		 *
		 * @param val value to attach
		 * @return {@code this}
		 * @since 1.2
		 * @see HttpRequest#attachment()
		 */
		public Builder attachment(@Nullable Object val) {
			this.attachment = val;
			return this;
		}

		/**
		 * Append header value.
		 *
		 * @param name name of header to append value to
		 * @param val header value to append
		 * @return {@code this}
		 * @throws IllegalArgumentException {@code name} or {@code val} is {@linkplain
		 * String#isEmpty() empty}
		 * @since 1.2
		 */
		public Builder addHeader(String name, String val) {
			Preconditions.checkArgument(!name.isEmpty() && !val.isEmpty());

			if ("Content-Type".equalsIgnoreCase(name)) {
				this.hasContentType = true;
			} else if ("Cache-Control".equalsIgnoreCase(name)) {
				this.requiresNetwork |=
					val.contains("max-age=0") ||
					val.contains("no-cache") ||
					val.contains("no-store");
			}
			if (this.headers == null)
				this.headers = new ArrayList<>();
			this.headers.add(new Pair<>(name, val));
			return this;
		}

		/**
		 * Set request body.
		 * <p>The body specified is sent to the remote server. The buffer must not be used or
		 * modified, after the underlying request is {@linkplain #send() sent}, until the request
		 * completes {@linkplain Listener#onSuccess(HttpRequest) successfully} or {@linkplain
		 * Listener#onFailed(HttpRequest, Throwable, boolean) erroneously}.
		 * <p>If a body is set, and the {@code Content-Type} header is not {@linkplain
		 * #addHeader(String, String) set}, before {@link #send()} is invoked, then {@link #send()}
		 * will fail.
		 * <p>The {@linkplain ByteBuffer#position() position} and {@linkplain ByteBuffer#limit()
		 * limit} of {@code body} is guaranteed to not be modified.
		 *
		 * @param body request body
		 * @return {@code this}
		 * @since 1.2
		 */
		public Builder body(ByteBuffer body) {
			this.body = body.slice();
			return this;
		}

		/**
		 * Set request priority advisory.
		 *
		 * @param prio priority
		 * @return {@code this}
		 * @throws IllegalArgumentException {@code prio} is not valid
		 * @since 1.2
		 * @see HttpRequest#priority()
		 */
		public Builder priority(@Priority int prio) {
			Preconditions.checkArgument(prio >= PRIORITY_IDLE && prio <= PRIORITY_CRITICAL);
			this.priority = prio;
			return this;
		}

		/**
		 * Send resulting request.
		 *
		 * @return resulting request
		 * @throws IllegalStateException request body was {@linkplain #body(ByteBuffer) specified}
		 * but {@code Content-Type} header was not or owning module has been {@linkplain
		 * HttpModule#isDestroyed() destroyed}
		 * @since 1.2
		 */
		public HttpRequest send() {
			Preconditions.checkState(
				this.body == null || this.hasContentType,
				"body specified without `Content-Type` header"
			);
			return this.module.buildAndSendRequest(this);
		}
	}

	/**
	 * Listener which simply cancels the tracked request.
	 */
	private static final Listener CANCELLING_LISTENER = new Listener() {
		@Override
		public void onFailed(HttpRequest req, Throwable cause, boolean recoverable) {
		}

		@Override
		public void onCancel(HttpRequest req) {
		}

		@Override
		public boolean onRedirect(
			HttpRequest req,
			URL url,
			int statusCode,
			String statusMsg,
			Supplier<Map<String, List<String>>> resolveHdrs
		) {
			return false;
		}

		@Override
		public void onResponseStart(
			HttpRequest req,
			int statusCode,
			String statusMsg,
			Supplier<Map<String, List<String>>> resolveHdrs
		) {
			req.cancel();
		}

		@Override
		public void onResponseBodyPart(HttpRequest req, ByteBuffer body, boolean last) {
			req.cancel();
		}

		@Override
		public void onSuccess(HttpRequest req) {
		}
	};

	/**
	 * Owning module.
	 */
	final HttpModule module;
	@GuardedBy("this")
	private @Nullable Object state;
	private Listener listener;
	private final URL url;
	private final @Nullable Object attachment;
	private final @Priority int priority;
	/**
	 * Request definitely requires network.
	 */
	final boolean requiresNetwork;

	/**
	 * Construct new request from builder.
	 *
	 * @param builder builder to construct from
	 * @param state request state
	 */
	HttpRequest(Builder builder, @Nullable Object state) {
		this.module = builder.module;
		this.listener = builder.listener;
		this.url = builder.url;
		this.attachment = builder.attachment;
		this.state = state;
		this.priority = builder.priority;
		this.requiresNetwork = builder.requiresNetwork;
	}

	/**
	 * URL request was originally to.
	 *
	 * @return target URL
	 * @since 1.2
	 */
	public final URL url() {
		return this.url;
	}

	/**
	 * Value {@linkplain Builder#attachment(Object) attached} to request.
	 *
	 * @return attached value, if any
	 * @since 1.2
	 * @see Builder#attachment(Object)
	 */
	public final @Nullable Object attachment() {
		return this.attachment;
	}

	/**
	 * Priority of request.
	 *
	 * @return priority
	 * @since 1.2
	 * @see Builder#priority(int)
	 */
	public final @Priority int priority() {
		return this.priority;
	}

	/**
	 * Test whether request completed {@linkplain Listener#onSuccess(HttpRequest) successfully},
	 * {@linkplain Listener#onFailed(HttpRequest, Throwable, boolean) erroneously}, or was
	 * {@linkplain #cancel() cancelled}.
	 *
	 * @return {@code true} if, and only if, request is done
	 * @since 1.2
	 */
	public final boolean isDone() {
		return this.state == this;
	}

	/**
	 * Lifecycle listener.
	 *
	 * @return listener
	 */
	final Listener listener() {
		return this.listener;
	}

	/**
	 * Log listener callback failure.
	 *
	 * @param what method which failed
	 * @param cause failure cause
	 */
	private void logListenerFailed(String what, Throwable cause) {
		Logger.warn(TAG, "%s::%s failed", this.listener.getClass(), what, cause);
	}

	/**
	 * Compute new request state.
	 * <p>If request is not {@linkplain #isDone() done}, {@code fn} is invoked with the current
	 * state, and its return value is used as the new state. If between computing the new state
	 * and the attempt to update the state, the request is marked as done, this returns a tuple
	 * of {@code this} and computed uncomitted state, respectively.
	 *
	 * @param <O> computed state type
	 * @param fn function to compute state with
	 * @return tuple of previous and updated state, tuple of {@code this} and computed uncomitted
	 * state, or {@code null} if state was computed and updated successfully, state was computed
	 * but request was marked done before state could be comitted, or if state was already done
	 * before new state could be computed, respectively
	 */
	@RestrictTo(RestrictTo.Scope.SUBCLASSES)
	final <O> @Nullable Pair<Object, O> computeState(Function<Object, O> fn) {
		Object prev;
		O curr;

		synchronized (this) {
			prev = this.state;
			if (prev == this)
				return null;

			curr = fn.apply(prev);
			this.state = curr;
		}
		return new Pair<>(prev, curr);
	}

	/**
	 * Update request state.
	 *
	 * @param update updated state
	 * @return previous state or {@code this} if request is done
	 */
	@RestrictTo(RestrictTo.Scope.SUBCLASSES)
	final @Nullable Object updateState(@Nullable Object update) {
		synchronized (this) {
			Object curr = this.state;

			if (curr == this)
				return this;
			this.state = update;
			return curr;
		}
	}

	/**
	 * Release underlying resources.
	 * <p>This is invoked whenever the request is marked as {@linkplain #isDone() done}, with
	 * {@code state} equal to the request state aprior being marked done. Implementations
	 * <b>cannot</b> fail with an exception.
	 *
	 * @param state request state prior to request being marked as done
	 */
	@RestrictTo(RestrictTo.Scope.SUBCLASSES)
	abstract void release(@Nullable Object state);

	/**
	 * Mark this request as {@linkplain #isDone() done}.
	 *
	 * @return listener to notify of completion or {@link #CANCELLING_LISTENER} if request is
	 * already done
	 */
	private Listener setDone() {
		Listener listen = this.listener;

		this.listener = CANCELLING_LISTENER;

		Object prev = this.updateState(this);

		if (prev == this)
			return CANCELLING_LISTENER;

		this.release(prev);
		this.module.onRequestDone(this);
		return listen;
	}

	/**
	 * Handle error encountered while servicing this request.
	 * <p>If request is not {@linkplain #isDone() done}, it is marked as completing erroneously,
	 * any associated state is {@linkplain #release(Object) released}, and the associated listener
	 * is {@linkplain Listener#onFailed(HttpRequest, Throwable, boolean) notified}.
	 *
	 * @param cause error cause
	 * @param recov {@code true} if, and only if, error is recoverable
	 */
	final void setFailed(Throwable cause, boolean recov) {
		Listener listener = this.setDone();

		if (listener != CANCELLING_LISTENER) {
			try {
				listener.onFailed(this, cause, recov);
			} catch (Throwable listenCause) {
				this.logListenerFailed("onError", listenCause);
			}
		}
	}

	/**
	 * Mark request as completed successfully.
	 * <p>If request is not {@linkplain #isDone() done}, it is marked as completed, any
	 * associated state is {@linkplain #release(Object) released}, and the associated listener is
	 * {@linkplain Listener#onSuccess(HttpRequest) notified}.
	 */
	@WorkerThread
	final void setSuccess() {
		Listener listener = this.setDone();

		if (listener != CANCELLING_LISTENER) {
			try {
				listener.onSuccess(this);
			} catch (Throwable cause) {
				this.logListenerFailed("onComplete", cause);
			}
		}
	}

	/**
	 * Cancel request.
	 * <p>If request is not {@linkplain #isDone() done}, it is cancelled and the associated
	 * listener's {@link Listener#onCancel(HttpRequest)} method is invoked on the {@linkplain
	 * Thread#currentThread() current} thread.
	 *
	 * @return {@code true} if, and only if, request was cancelled; otherwise, {@code false} if
	 * request is {@linkplain #isDone() done}
	 * @since 1.2
	 */
	public final boolean cancel() {
		Listener listener = this.setDone();

		if (listener == CANCELLING_LISTENER)
			return false;

		try {
			listener.onCancel(this);
		} catch (Throwable cause) {
			this.logListenerFailed("onCancel", cause);
		}
		return true;
	}

	@Override
	public final boolean equals(@Nullable Object that) {
		return this == that;
	}

	@Override
	public final String toString() {
		return this.url.toString();
	}
}
