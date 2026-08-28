// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.util;

import androidx.annotation.CallSuper;
import androidx.annotation.GuardedBy;
import androidx.annotation.Nullable;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;

/**
 * Future {@linkplain FutureTask task} which supports completion listeners.
 *
 * @param <V> future completion value type
 * @since 1.2
 */
public class ListenableFutureTask<V> extends FutureTask<V> implements ListenableFuture<V> {

	private static final String TAG = ListenableFutureTask.class.getSimpleName();

	private static final Runnable NOOP = () -> {};

	/**
	 * Listener used to denote listeners have been called.
	 */
	private static final Listener DONE = new Listener(NOOP, Futures.SAME_THREAD_EXECUTOR);

	/**
	 * Listener attached to a future.
	 */
	private static final class Listener {
		private final Runnable callback;
		private final Executor executor;
		@Nullable Listener next;

		/**
		 * Construct new listener.
		 *
		 * @param callback callback to invoke
		 * @param exec executor to invoke callback on
		 */
		Listener(Runnable callback, Executor exec) {
			this.callback = Preconditions.checkNotNull(callback);
			this.executor = Preconditions.checkNotNull(exec);
		}

		/**
		 * Invoke callback.
		 */
		void call() {
			try {
				this.executor.execute(this.callback);
			} catch (Exception cause) {
				Logger.warn(TAG, "failed to execute callback", cause);
			}
		}
	}

	@GuardedBy("this")
	private @Nullable Listener headListener;

	/**
	 * Construct new task with runnable command and precomputed completion value.
	 *
	 * @param cmd task command
	 * @param val precomputed completion value, if any
	 * @since 1.2
	 */
	public ListenableFutureTask(Runnable cmd, @Nullable V val) {
		super(cmd, val);
	}

	/**
	 * Construct new task with callable command.
	 *
	 * @param cmd task command
	 * @since 1.2
	 */
	public ListenableFutureTask(Callable<V> cmd) {
		super(cmd);
	}

	/**
	 * Construct new task which performs no operation when {@linkplain #run() ran}.
	 *
	 * @since 1.2
	 */
	protected ListenableFutureTask() {
		this(NOOP, null);
	}

	@Override
	@CallSuper
	public void addListener(Runnable cmd, Executor exec) {
		Listener newHead = new Listener(cmd, exec);

		synchronized (this) {
			if (this.headListener != DONE) {
				newHead.next = this.headListener;
				this.headListener = newHead;
				return;
			}
		}
		newHead.call();
	}

	@Override
	@CallSuper
	protected void done() {
		Listener head;

		synchronized (this) {
			head = this.headListener;
			this.headListener = DONE;
		}
		if (head == DONE)
			return;
		while (head != null) {
			head.call();
			head = head.next;
		}
	}
}
