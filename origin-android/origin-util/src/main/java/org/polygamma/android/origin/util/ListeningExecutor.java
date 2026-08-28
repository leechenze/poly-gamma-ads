// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.util;

import android.os.Handler;
import android.os.SystemClock;

import androidx.annotation.Nullable;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Delayed;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Executor which returns {@linkplain ListenableFuture listenable} execution futures.
 * <p>Work submitted for execution is executed on an underlying {@linkplain Executor executor}.
 * Delayed work, however, is scheduled two folds, once on a {@linkplain Handler handler} for the
 * delayed part of the work, and on an underlying executor for the actual work.
 *
 * @since 0.1
 */
public final class ListeningExecutor extends AbstractExecutorService
implements ScheduledExecutorService {

	private static final int STATE_RUNNING = 0;
	private static final int STATE_SHUTDOWN = 1;
	private static final int STATE_TERMINATED = 2;

	/**
	 * Task pending execution.
	 *
	 * @param <V> computed value type
	 */
	@SuppressWarnings("overrides")
	private static class Task<V> extends ListenableFutureTask<V> {

		final ListeningExecutor parent;

		Task(ListeningExecutor parent, Runnable cmd, @Nullable V val) {
			super(Preconditions.checkNotNull(cmd), val);
			this.parent = parent;
		}

		Task(ListeningExecutor parent, Callable<V> cmd) {
			super(Preconditions.checkNotNull(cmd));
			this.parent = parent;
		}

		@Override
		protected void done() {
			try {
				this.parent.onTaskDone(this);
			} finally {
				super.done();
			}
		}

		@Override
		public boolean equals(@Nullable Object that) {
			return this == that;
		}
	}

	/**
	 * Task pending delayed execution.
	 *
	 * @param <V> computed value type
	 */
	private static class DelayedTask<V> extends Task<V>
	implements ListenableScheduledFuture<V> {
		long timeoutMillis;

		DelayedTask(ListeningExecutor parent, Runnable cmd, long expiryMillis) {
			super(parent, cmd, null);
			this.timeoutMillis = SystemClock.uptimeMillis() + expiryMillis;
		}

		DelayedTask(ListeningExecutor parent, Callable<V> cmd, long expiryMillis) {
			super(parent, cmd);
			this.timeoutMillis = SystemClock.uptimeMillis() + expiryMillis;
		}

		final void callSetException(Exception err) {
			super.setException(err);
		}

		@Override
		public void run() {
			/*
			 * It's possible we've been cancelled, `done()` has been called, and we're being
			 * called for the first time. This is possible when a we're cancelled before our delay
			 * expires. In these cases, just call `parent::onTaskDone()` to make sure we're finally
			 * removed. If we're cancelled while we're running, we don't really care, since the
			 * normal `done()` flow will take care of removal.
			 */
			if (super.isCancelled())
				super.parent.onTaskDone(this);
			else
				super.run();
		}

		@Override
		public long getDelay(TimeUnit unit) {
			return unit.convert(
				this.timeoutMillis - SystemClock.uptimeMillis(),
				TimeUnit.MILLISECONDS
			);
		}

		@Override
		public int compareTo(Delayed that) {
			return Long.compare(
				this.getDelay(TimeUnit.MILLISECONDS),
				that.getDelay(TimeUnit.MILLISECONDS)
			);
		}
	}

	/**
	 * Task with recurring execution.
	 */
	private static final class RecurringTask extends DelayedTask<Void> {

		private final long periodMillis;
		private final boolean fixedRate;

		RecurringTask(
			ListeningExecutor parent,
			Runnable cmd,
			long initialDelayMillis,
			long periodMillis,
			boolean fixedRate
		) {
			super(parent, cmd, initialDelayMillis);
			Preconditions.checkArgument(initialDelayMillis >= 0 && periodMillis >= 0);
			this.periodMillis = periodMillis;
			this.fixedRate = fixedRate;
		}

		@Override
		public void run() {
			if (super.runAndReset()) {
				if (this.fixedRate)
					super.timeoutMillis += this.periodMillis;
				else
					super.timeoutMillis = SystemClock.uptimeMillis() + this.periodMillis;
				try {
					super.parent.submitDelayed(this);
				} catch (RejectedExecutionException err) {
					super.setException(err);
				}
			}
		}
	}

	// lock protecting against shutdown; W=write side of this lock, R=read side of this lock
	private final ReadWriteLock shutdownLock;
	// executor state, [W] protects writes to this
	private int state;
	private final Handler handler;
	private final Executor executor;
	private final ConcurrentHashMap<Task<?>, Object> pendingTasks;
	// latch protecting termination
	private final CountDownLatch terminated;

	/**
	 * Construct a new listening executor.
	 * <p>The handler specified must <b>not</b> be shutdown while work is pending execution on the
	 * constructed executor. If the handler is shutdown, then the constructed executor must also
	 * be shutdown.
	 *
	 * @param handler handler to use for time keeping
	 * @param executor executor to submit work onto
	 * @since 0.1
	 */
	public ListeningExecutor(Handler handler, Executor executor) {
		this.shutdownLock = new ReentrantReadWriteLock();
		this.state = STATE_RUNNING;
		this.handler = Preconditions.checkNotNull(handler);
		this.executor = Preconditions.checkNotNull(executor);
		this.pendingTasks = new ConcurrentHashMap<>();
		this.terminated = new CountDownLatch(1);
	}

	/**
	 * Underlying handler used for time keeping.
	 *
	 * @return underlying handler
	 * @since 0.1
	 */
	public Handler handler() {
		return this.handler;
	}

	/**
	 * Underlying executor on which work is submitted for final execution.
	 *
	 * @return underlying executor
	 * @since 0.1
	 */
	public Executor executor() {
		return this.executor;
	}

	/**
	 * Test whether executor state matches an expected state.
	 *
	 * @param expect expected state
	 * @return {@code true} if, and only if, executor state is {@code expect}
	 */
	private boolean isState(int expect) {
		Lock read = this.shutdownLock.readLock();

		read.lock();
		try {
			return this.state == expect;
		} finally {
			read.unlock();
		}
	}

	/**
	 * Ensure executor is in a running state.
	 * <p>This should be invoked with read or write -side of {@link #shutdownLock} held. If {@code
	 * task} is non-{@code null}, exists within {@link #pendingTasks}, and {@link #state} is not
	 * {@link #STATE_TERMINATED}, then this simply returns. Otherwise, this adds {@code task} into
	 * {@link #pendingTasks} if, and only if, {@link #state} is {@link #STATE_RUNNING}.
	 *
	 * @param task task to requesting execution
	 * @throws RejectedExecutionException executor has shutdown
	 */
	private void checkRunning(@Nullable Task<?> task) {
		if (
			(
				task == null ||
				this.state == STATE_TERMINATED ||
				this.pendingTasks.put(task, task) != task
			) &&
			this.state != STATE_RUNNING
		) {
			if (task != null)
				this.pendingTasks.remove(task);
			throw new RejectedExecutionException();
		}
	}

	/**
	 * Handle task completion.
	 *
	 * @param task completed task
	 */
	private void onTaskDone(Task<?> task) {
		this.pendingTasks.remove(task);
		if (!this.pendingTasks.isEmpty() || !this.isState(STATE_SHUTDOWN))
			return;

		Lock write = this.shutdownLock.writeLock();

		write.lock();
		try {
			if (this.pendingTasks.isEmpty()) {
				this.terminated.countDown();
				this.state = STATE_TERMINATED;
			}
		} finally {
			write.unlock();
		}
	}

	@Override
	@SuppressWarnings("ClassEscapesDefinedScope")
	protected <T> Task<T> newTaskFor(Runnable cmd, @Nullable T val) {
		return new Task<>(this, cmd, val);
	}

	@Override
	@SuppressWarnings("ClassEscapesDefinedScope")
	protected <T> Task<T> newTaskFor(Callable<T> cmd) {
		return new Task<>(this, cmd);
	}

	private void shutdown(boolean cancel) {
		Lock write = this.shutdownLock.writeLock();

		write.lock();
		try {
			if (this.state == STATE_RUNNING) {
				this.state = STATE_SHUTDOWN;
				if (this.pendingTasks.isEmpty()) {
					this.terminated.countDown();
					this.state = STATE_TERMINATED;
				}
			}
		} finally {
			write.unlock();
		}

		if (cancel) {
			Enumeration<Task<?>> pending = this.pendingTasks.keys();

			while (pending.hasMoreElements())
				pending.nextElement().cancel(true);
		}
	}

	@Override
	public void shutdown() {
		this.shutdown(false);
	}

	@Override
	public List<Runnable> shutdownNow() {
		this.shutdown(true);
		return Collections.emptyList();
	}

	@Override
	public boolean isTerminated() {
		return this.isState(STATE_TERMINATED);
	}

	@Override
	public boolean isShutdown() {
		return !this.isState(STATE_RUNNING);
	}

	@Override
	public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
		return this.terminated.await(timeout, unit);
	}

	@Override
	public void execute(Runnable cmd) {
		Task<?> task = cmd instanceof Task ? (Task<?>) cmd : null;
		Lock read = this.shutdownLock.readLock();

		read.lock();
		try {
			this.checkRunning(task);
			this.executor.execute(cmd);
		} finally {
			read.unlock();
		}
	}

	@Override
	public <T> ListenableFuture<T> submit(Callable<T> cmd) {
		Task<T> task = this.newTaskFor(cmd);

		this.execute(task);
		return task;
	}

	@Override
	public <T> ListenableFuture<T> submit(Runnable cmd, @Nullable T result) {
		Task<T> task = this.newTaskFor(cmd, result);

		this.execute(task);
		return task;
	}

	@Override
	public ListenableFuture<?> submit(Runnable cmd) {
		return this.submit(cmd, null);
	}

	private <T extends DelayedTask<?>> T submitDelayed(T task) {
		if (Long.compareUnsigned(task.timeoutMillis, SystemClock.uptimeMillis()) <= 0) {
			this.execute(task);
			return task;
		}

		Runnable submit = () -> {
			try {
				this.execute(task);
			} catch (Exception err) {
				task.callSetException(err);
			}
		};
		Lock read = this.shutdownLock.readLock();
		boolean ok;

		read.lock();
		try {
			this.checkRunning(task);
			ok = this.handler.postAtTime(submit, task.timeoutMillis);
		} finally {
			read.unlock();
		}
		if (!ok) {
			this.onTaskDone(task);
			throw new RejectedExecutionException();
		}
		return task;
	}

	@Override
	public  ListenableScheduledFuture<?> schedule(Runnable cmd, long delay, TimeUnit unit) {
		return this.submitDelayed(new DelayedTask<>(this, cmd, unit.toMillis(delay)));
	}

	@Override
	public <V> ListenableScheduledFuture<V> schedule(Callable<V> cmd, long delay, TimeUnit unit) {
		return this.submitDelayed(new DelayedTask<>(this, cmd, unit.toMillis(delay)));
	}

	@Override
	public ListenableScheduledFuture<?>
	scheduleAtFixedRate(Runnable cmd, long initialDelay, long period, TimeUnit unit) {
		return this.submitDelayed(new RecurringTask(
			this,
			cmd,
			unit.toMillis(initialDelay),
			unit.toMillis(period),
			true
		));
	}

	@Override
	public ListenableScheduledFuture<?>
	scheduleWithFixedDelay(Runnable cmd, long initialDelay, long delay, TimeUnit unit) {
		return this.submitDelayed(new RecurringTask(
			this,
			cmd,
			unit.toMillis(initialDelay),
			unit.toMillis(delay),
			false
		));
	}
}
