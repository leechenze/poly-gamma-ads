// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.core;

import android.os.SystemClock;

import androidx.annotation.GuardedBy;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.util.Consumer;

import com.google.common.util.concurrent.ListenableFuture;

import org.polygamma.android.origin.util.CollectionsCompat;
import org.polygamma.android.origin.util.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Bus for delivering a single {@linkplain OriginModule module} event.
 *
 * @since 0.1
 * @see OriginModule#registerEvent(String, boolean)
 */
public final class OriginModuleEventBus {

	private static final String TAG = OriginModuleEventBus.class.getSimpleName();

	/**
	 * Instance of an event, composed of a timestamp and event data, if any.
	 */
	private static final class Event {

		private final long timestamp;
		private final @Nullable Object data;

		Event(long timestamp, @Nullable Object data) {
			this.timestamp = timestamp;
			this.data = data;
		}
	}

	private final OriginModule module;
	private final @OriginModuleEventName String eventName;
	@VisibleForTesting
	final Lock invokeLock;
	/*
	 * This is lazily allocated when a callback is first registered with this bus. When the last
	 * callback is deregistered, this goes back to `null`.
	 */
	@GuardedBy("invokeLock")
	private @Nullable Set<OriginModuleEventCallback> callbacks;
	/*
	 * This is set to a non-`null` list of callbacks to invoke when `invokeCallbacks()` does not
	 * need to fire a regular event, but fire the *current* sticky event.
	 */
	@GuardedBy("invokeLock")
	private @Nullable ArrayList<OriginModuleEventCallback> initialCallbacks;
	/*
	 * Pooled events. When the underlying event for this bus is sticky, this is *always* an
	 * instance of `Event`; otherwise, this is a `List<Event>` that is lazily constructed and
	 * populated with the events that `invokeCallbacks()` needs to fire.
	 */
	@GuardedBy("invokeLock")
	private @Nullable Object events;
	/*
	 * Future which resolves when `invokeCallbacks()` is invoked. When this is `null` or non-`null`
	 * invocation of callbacks is pending or complete, respectively.
	 */
	@GuardedBy("invokeLock")
	private @Nullable ListenableFuture<?> invokeCallbacksFuture;

	/**
	 * Construct a new event bus.
	 *
	 * @param module module for which the bus delivers events for
	 * @param eventName name of event bus delivers
	 * @param sticky {@code true} if, and only if, event is sticky
	 */
	OriginModuleEventBus(
		OriginModule module,
		@OriginModuleEventName String eventName,
		boolean sticky
	) {
		this.module = module;
		this.eventName = eventName;
		this.invokeLock = new ReentrantLock();
		this.events = sticky ? new Event(0, null) : null;
	}

	/**
	 * Name of event delivered by bus.
	 *
	 * @return event name
	 * @since 0.1
	 */
	public String eventName() {
		return this.eventName;
	}

	/**
	 * Test whether event delivered by bus is sticky.
	 *
	 * @return {@code true} if, and only if, event is sticky
	 * @since 0.1
	 */
	public boolean isSticky() {
		return this.events instanceof Event;
	}

	/**
	 * Invoke a {@linkplain Consumer consumer} with each {@linkplain
	 * #registerCallback(OriginModuleEventCallback) registered} callback.
	 * <p>Note that invocations of {@link #registerCallback(OriginModuleEventCallback)},
	 * {@link #unregisterCallback(OriginModuleEventCallback)}, and {@link #submit(Object)} are
	 * all blocked until this returns.
	 *
	 * @param cons consumer to invoke
	 */
	void forEachCallback(Consumer<OriginModuleEventCallback> cons) {
		this.invokeLock.lock();
		try {
			if (this.callbacks == null)
				return;
			for (OriginModuleEventCallback callback : this.callbacks)
				cons.accept(callback);
		} finally {
			this.invokeLock.unlock();
		}
	}

	/**
	 * Invoke a single callback with an event.
	 *
	 * @param callback callback to invoke
	 * @param event event to invoke callback with
	 */
	private void invokeCallback(OriginModuleEventCallback callback, Event event) {
		try {
			callback.onOriginModuleEvent(
				this.module,
				this.eventName,
				event.data,
				event.timestamp
			);
		} catch (Exception err) {
			Logger.info(TAG, "event handler failed", err);
		}
	}

	/**
	 * Invoke all {@linkplain #registerCallback(OriginModuleEventCallback) registered} callbacks
	 * with pending {@linkplain #events events}.
	 * <p>If {@link #initialCallbacks} is non-{@code null} <b>and</b> the underlying event is
	 * {@linkplain #isSticky() sticky}, then the callbacks specified within it are invoked with
	 * the current sticky event and {@code initialCallbacks} is set to {@code null}. Otherwise, all
	 * {@link #callbacks}, if any, are invoked with the current pending events. When the underlying
	 * event is non-sticky, {@link #events} is set to {@code null}.
	 * <p>Upon return, {@link #invokeCallbacksFuture} is set to {@code null}.
	 */
	@SuppressWarnings("unchecked")
	private void invokeCallbacks() {
		Collection<OriginModuleEventCallback> callbacks;
		Object events;

		this.invokeLock.lock();
		try {
			callbacks = this.initialCallbacks;
			if (callbacks != null)
				this.initialCallbacks = null;
			else if (this.callbacks != null)
				callbacks = new ArrayList<>(this.callbacks);
			events = this.events;
			if (!(events instanceof Event))
				this.events = null;
		} finally {
			this.invokeCallbacksFuture = null;
			this.invokeLock.unlock();
		}

		if (callbacks == null || callbacks.isEmpty())
			return;
		if (events instanceof Event) {
			Event event = (Event) events;

			for (OriginModuleEventCallback callback : callbacks)
				this.invokeCallback(callback, event);
		} else if (events instanceof List) {
			for (Event event : (List<Event>) events) {
				for (OriginModuleEventCallback callback : callbacks)
					this.invokeCallback(callback, event);
			}
		}
	}

	/**
	 * Schedule an invocation of {@link #invokeCallbacks()} if not already scheduled.
	 * <p>This <b>must</b> be invoked with {@link #invokeLock} held.
	 */
	@GuardedBy("invokeLock")
	private void scheduleInvokeCallbacks() {
		if (this.invokeCallbacksFuture != null)
			return;

		try {
			//noinspection resource
			this.invokeCallbacksFuture =
				this.module.sdk()
					.backgroundExecutor()
					.submit(this::invokeCallbacks);
		} catch (RejectedExecutionException err) {
			// sdk is shutting down, that's fine
			Logger.debug(TAG, "failed to schedule callback invocation", err);
		}
	}

	/**
	 * Submit an event.
	 * <p>If the event underlying this bus is {@linkplain #isSticky() sticky} <b>and</b> there
	 * is a pending fire, then the pending fire is replaced with the new fire. Otherwise, a
	 * fire is scheduled, and {@linkplain #registerCallback(OriginModuleEventCallback) registered}
	 * callbacks are invoked.
	 *
	 * @param data event data, if any
	 * @since 0.1
	 */
	@SuppressWarnings("unchecked")
	public void submit(@Nullable Object data) {
		Event event = new Event(SystemClock.uptimeMillis(), data);

		this.invokeLock.lock();
		try {
			if (this.isSticky()) {
				this.events = event;
				this.initialCallbacks = null;
			}
			if (this.callbacks == null)
				return;

			if (!this.isSticky()) {
				List<Event> events = (List<Event>) this.events;

				if (events == null)
					this.events = events = new ArrayList<>(1);
				events.add(event);
			}
			this.scheduleInvokeCallbacks();
		} finally {
			this.invokeLock.unlock();
		}
	}

	/**
	 * Register a callback to be invoked.
	 * <p>If {@code callback} has already been registered, this simply returns {@code false};
	 * otherwise, this registers {@code callback} and returns {@code true}. If the callback was not
	 * previously registered <b>and</b> the underlying event is {@linkplain #isSticky() sticky},
	 * {@code callback} is scheduled for invocation with the current sticky event, if any.
	 *
	 * @param callback callback to register
	 * @return {@code true} or {@code false} if {@code callback} was registered or was already
	 * registered, respectively
	 */
	boolean registerCallback(OriginModuleEventCallback callback) {
		this.invokeLock.lock();
		try {
			if (this.callbacks == null)
				this.callbacks = CollectionsCompat.newArraySet();
			if (!this.callbacks.add(callback))
				return false;
			if (this.isSticky() && ((Event) this.events).timestamp != 0) {
				if (this.invokeCallbacksFuture == null)
					this.initialCallbacks = new ArrayList<>(1);
				if (this.initialCallbacks != null) {
					this.initialCallbacks.add(callback);
					this.scheduleInvokeCallbacks();
				}
			}
			return true;
		} finally {
			this.invokeLock.unlock();
		}
	}

	/**
	 * Unregister a callback.
	 * <p>If {@code callback} has not yet been {@linkplain
	 * #registerCallback(OriginModuleEventCallback) registered}, this simply returns {@code false};
	 * otherwise, this unregisters {@code callback} from <i>future</i> event fires. In other words,
	 * upon return it is guaranteed that a subsequent event {@linkplain #submit(Object) firing} will
	 * <b>not</b> be passed onto {@code callback}.
	 *
	 * @param callback callback to unregister
	 * @return {@code true} or {@code false} if {@code callback} was unregistered or was already
	 * unregistered, respectively
	 */
	boolean unregisterCallback(OriginModuleEventCallback callback) {
		this.invokeLock.lock();
		try {
			if (this.callbacks == null || !this.callbacks.remove(callback))
				return false;
			if (this.callbacks.isEmpty())
				this.callbacks = null;
			return true;
		} finally {
			this.invokeLock.unlock();
		}
	}
}
