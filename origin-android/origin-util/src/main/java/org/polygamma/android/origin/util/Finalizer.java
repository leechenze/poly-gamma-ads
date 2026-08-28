// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.util;

import android.os.Build;
import android.os.SystemClock;

import androidx.annotation.GuardedBy;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.lang.ref.Cleaner;
import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;

/**
 * Reference finalizer.
 *
 * @since 1.2
 */
public abstract class Finalizer {

	/**
	 * Phantom {@linkplain QueueFinalizer queued} reference to an object.
	 *
	 * @param <T> referenced object type
	 */
	private static final class QueuedReference<T> extends PhantomReference<T> implements Runnable {
		@GuardedBy("this")
		private @Nullable Runnable command;

		private QueuedReference(T ref, ReferenceQueue<? super T> queue, Runnable cmd) {
			super(ref, queue);
			this.command = cmd;
		}

		@Override
		public void run() {
			Runnable cmd;

			synchronized (this) {
				cmd = this.command;
				this.command = null;
			}

			if (cmd != null) {
				cmd.run();
				super.clear();
			}
		}

		@Override
		public void clear() {
			try {
				this.run();
			} finally {
				super.clear();
			}
		}
	}

	/**
	 * {@linkplain ReferenceQueue Queue} based finalizer.
	 */
	private static final class QueueFinalizer extends Finalizer implements Runnable {

		private static final String TAG = QueueFinalizer.class.getSimpleName();

		private final ReferenceQueue<Object> queue;

		private QueueFinalizer() {
			this.queue = new ReferenceQueue<>();

			Thread thr = new Thread(this);

			thr.setDaemon(true);
			thr.start();
		}

		@Override
		Runnable registerImpl(Object ref, Runnable cmd) {
			return new QueuedReference<>(ref, this.queue, cmd);
		}

		@Override
		@SuppressWarnings("InfiniteLoopStatement")
		public void run() {
			while (true) {
				QueuedReference<?> ref = null;

				try {
					ref = (QueuedReference<?>) this.queue.remove();
				} catch (InterruptedException cause) {
					Logger.warn(TAG, "queued reference removal failed", cause);
					SystemClock.sleep(1000);
				}

				if (ref != null) {
					try {
						ref.clear();
					} catch (Throwable cause) {
						Logger.warn(TAG, "failed to clear queued reference", cause);
					}
				}
			}
		}
	}

	/**
	 * {@link Cleaner} based reference finalizer.
	 */
	@RequiresApi(Build.VERSION_CODES.TIRAMISU)
	private static final class CleaningFinalizer extends Finalizer {

		private final Cleaner cleaner;

		private CleaningFinalizer() {
			this.cleaner = Cleaner.create();
		}

		@Override
		Runnable registerImpl(Object ref, Runnable cmd) {
			return this.cleaner.register(ref, cmd)::clean;
		}
	}

	@SuppressWarnings("StaticInitializerReferencesSubClass")
	private static final Finalizer IMPL =
		Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ? new CleaningFinalizer() :
		new QueueFinalizer();

	/**
	 * Register finalizing action for an object reference.
	 * <p>The {@code cmd} specified is invoked when {@code ref} is {@linkplain PhantomReference
	 * phantom} reachable. The returned action can be used to apply {@code cmd} manually. When
	 * the returned action is {@linkplain Runnable#run() ran}, subsequent invocations of the
	 * returned action will result in a no-op, including when {@code ref} becomes phantom
	 * reachable.
	 *
	 * @param ref target reference
	 * @param cmd finalizing command
	 * @return finalizing command invoker
	 * @since 1.2
	 */
	public static Runnable register(Object ref, Runnable cmd) {
		return IMPL.registerImpl(Preconditions.checkNotNull(ref), cmd);
	}

	private Finalizer() {
	}

	abstract Runnable registerImpl(Object ref, Runnable cmd);
}
