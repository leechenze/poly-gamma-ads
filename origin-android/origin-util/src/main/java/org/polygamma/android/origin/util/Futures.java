// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.util;

import androidx.annotation.Nullable;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Utility definitions for operating on {@linkplain ListenableFuture futures}.
 *
 * @since 0.1
 */
public class Futures {

	private static final String TAG = Futures.class.getSimpleName();

	/**
	 * Executor which executes command on calling thread.
	 */
	static final Executor SAME_THREAD_EXECUTOR = cmd -> {
		try {
			cmd.run();
		} catch (Throwable err) {
			Logger.warn(TAG, "%s encountered error during direct execution", cmd, err);
		}
	};

	private static final class ImmediateFuture<V> implements ListenableFuture<V> {

		private final V value;
		private final @Nullable Throwable error;

		private ImmediateFuture(V value) {
			this.value = value;
			this.error = null;
		}

		private ImmediateFuture(Throwable err) {
			this.value = null;
			this.error = Preconditions.checkNotNull(err);
		}

		@Override
		public void addListener(Runnable cmd, Executor exec) {
			try {
				exec.execute(cmd);
			} catch (Throwable ignored) {
			}
		}

		@Override
		public boolean cancel(boolean ignored) {
			return false;
		}

		@Override
		public boolean isCancelled() {
			return false;
		}

		@Override
		public boolean isDone() {
			return true;
		}

		@Override
		public V get() throws ExecutionException {
			if (this.error != null)
				throw new ExecutionException(this.error);
			return this.value;
		}

		@Override
		public V get(long timeout, TimeUnit unit) throws ExecutionException {
			return this.get();
		}
	}

	/**
	 * Construct a {@linkplain ListenableFuture#isDone() immediate} future with a precomputed value.
	 *
	 * @param <V> future value type
	 * @param val precomputed value
	 * @return resulting future
	 * @since 0.1
	 */
	public static <V> ListenableFuture<V> of(@Nullable V val) {
		return new ImmediateFuture<>(val);
	}

	/**
	 * {@linkplain ListenableFuture#isDone() Immediate} future whose value is always {@code null}.
	 *
	 * @return resulting future
	 * @since 0.1
	 */
	public static ListenableFuture<Void> ofVoid() {
		return of(null);
	}

	/**
	 * Construct a {@linkplain ListenableFuture#isDone() immediate} future which fails with an
	 * error.
	 *
	 * @param <V> future value type
	 * @param err error cause
	 * @return resulting future
	 * @since 0.1
	 */
	public static <V> ListenableFuture<V> ofError(Throwable err) {
		return new ImmediateFuture<>(err);
	}

	/**
	 * Add {@linkplain ListenableFuture#addListener(Runnable, Executor) listener} to listenable
	 * future which is invoked on the same thread on which the future completes on.
	 * <p>Care must be taken as forward progress of {@code fut} <i>may</i> be blocked until {@code
	 * cmd} returns.
	 *
	 * @param fut future to add listener to
	 * @param cmd command to execute when future completes
	 * @since 1.1
	 */
	public static void addDirectListener(ListenableFuture<?> fut, Runnable cmd) {
		fut.addListener(cmd, SAME_THREAD_EXECUTOR);
	}

	/**
	 * Wait for completion of future, returning value or failing.
	 * <p>This {@linkplain Future#get() waits}, indefinitely, for {@code fut} to complete. If
	 * the wait is interrupted or the future fails to complete, the error is {@linkplain
	 * RuntimeException wrapped} and rethrown.
	 *
	 * @param <V> completion value type
	 * @param fut future to wait for completion of
	 * @return completion value
	 * @throws java.util.concurrent.CancellationException {@code fut} was {@linkplain
	 * Future#isCancelled() cancelled}
	 * @throws RuntimeException wait was {@linkplain InterruptedException interrupted} or future
	 * {@linkplain ExecutionException failed}
	 * @since 0.1
	 */
	public static <V> V await(Future<V> fut) {
		try {
			return fut.get();
		} catch (ExecutionException | InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Wait for completion of future, discarding result.
	 * <p>Unlike {@link #await(Future)}, this ignores any result of {@code fut}, successful or
	 * otherwise.
	 *
	 * @param fut future to wait for completion of
	 * @since 0.3
	 */
	public static void awaitUnchecked(Future<?> fut) {
		try {
			await(fut);
		} catch (RuntimeException ignored) {
		}
	}

	/**
	 * Cancel possibly {@code null} future.
	 *
	 * @param fut future to cancel, or {@code null}
	 * @param inter {@code true} if, and only if, computation of {@code fut}, if any, can be
	 * interrupted if currently running
	 * @return {@code true} if, and only if, {@code fut} is {@code null}, already {@linkplain
	 * Future#isCancelled() cancelled}, or is now cancelled
	 * @since 0.1
	 * @see Future#cancel(boolean)
	 */
	public static boolean cancel(@Nullable Future<?> fut, boolean inter) {
		return fut == null || fut.cancel(inter);
	}

	private Futures() {
	}
}
