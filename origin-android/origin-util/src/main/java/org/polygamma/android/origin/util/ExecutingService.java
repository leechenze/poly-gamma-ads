// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.util;

import android.util.Pair;

import androidx.annotation.CallSuper;
import androidx.annotation.GuardedBy;
import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.WorkerThread;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Service which executes on an {@linkplain ScheduledExecutorService executor}.
 * <p>Implementations define implementation logic in {@link #run()}. Service owners initiate a
 * service using {@link #schedule(long, TimeUnit)}. There may be only a single execution of a
 * service scheduled at any given point in time.
 * <p>Services may be {@linkplain #shutdown() shutdown}, in which case any further scheduling
 * will fail. If the service is running while a shutdown is requested, the service will enter into
 * a terminating state <i>and</i> the executing thread will be interrupted. Once the execution
 * completes, successfully or otherwise, the service will enter into a shutdown state. The
 * completion of a shutdown can be awaited using {@link #awaitShutdown(long, TimeUnit)}.
 * <p>Each service has a state describing it's current status. See the table below for more
 * information.
 * <table>
 *     <caption>Service States</caption>
 *     <tr>
 *         <th>State</th>
 *         <th>Preceeding State</th>
 *         <th>Description</th>
 *     </tr>
 *     <tr>
 *         <td>{@linkplain #STATE_IDLE Idle}</td>
 *         <td>{@linkplain #STATE_SCHEDULED Scheduled}, {@linkplain #STATE_RUNNING Running}</td>
 *         <td>Service is not shutdown, is not executing, nor is an execution scheduled.</td>
 *     </tr>
 *     <tr>
 *         <td>{@linkplain #STATE_SCHEDULED Scheduled}</td>
 *         <td>{@linkplain #STATE_IDLE Idle}, {@linkplain #STATE_RUNNING Running}</td>
 *         <td>Service is not shutdown, is not running, and an execution has been scheduled.</td>
 *     </tr>
 *     <tr>
 *         <td>{@linkplain #STATE_RUNNING Running}</td>
 *         <td>{@linkplain #STATE_SCHEDULED Scheduled}</td>
 *         <td>
 *             Service is not shutdown and is running an execution. Another execution may have
 *             been scheduled.
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>{@linkplain #STATE_TERMINATING Terminating}</td>
 *         <td>{@linkplain #STATE_RUNNING Running}</td>
 *         <td>Service is shutting down and is running an execution.</td>
 *     </tr>
 *     <tr>
 *         <td>{@linkplain #STATE_SHUTDOWN Shutdown}</td>
 *         <td>
 *             {@linkplain #STATE_IDLE Idle},
 *             {@linkplain #STATE_SCHEDULED Scheduled},
 *             {@linkplain #STATE_TERMINATING Terminating}
 *         </td>
 *         <td>Service has shutdown.</td>
 *     </tr>
 * </table>
 *
 * @since 1.2
 */
public abstract class ExecutingService {

	private static final String TAG = ExecutingService.class.getSimpleName();

	/**
	 * Service is idle.
	 *
	 * @since 1.2
	 * @see #state()
	 */
	public static final int STATE_IDLE			= 0;

	/**
	 * Service is scheduled or is being scheduled.
	 *
	 * @since 1.2
	 * @see #state()
	 */
	public static final int STATE_SCHEDULED		= 1;

	/**
	 * Service is running.
	 *
	 * @since 1.2
	 * @see #state()
	 */
	public static final int STATE_RUNNING		= 2;

	/**
	 * Service is shutting down.
	 *
	 * @since 1.2
	 * @see #state()
	 */
	public static final int STATE_TERMINATING	= 3;

	/**
	 * Service has shutdown.
	 *
	 * @since 1.2
	 * @see #state()
	 */
	public static final int STATE_SHUTDOWN		= 4;

	/**
	 * Service {@linkplain #state() state} enumeration value marker.
	 *
	 * @since 1.2
	 */
	@Documented
	@Retention(RetentionPolicy.SOURCE)
	@Target(ElementType.TYPE_USE)
	@IntDef({ STATE_IDLE, STATE_RUNNING, STATE_SCHEDULED, STATE_SHUTDOWN, STATE_TERMINATING })
	public @interface State {
	}

	private static final class RunnableService extends ExecutingService {
		private final String name;
		private final Runnable command;

		private RunnableService(String name, Runnable cmd, ScheduledExecutorService exec) {
			super(exec);
			this.name = name;
			this.command = cmd;
		}

		@Override
		protected void run() throws Exception {
			this.command.run();
		}

		@Override
		public String toString() {
			return this.name;
		}
	}

	/**
	 * Construct a service which is driven by a runnable.
	 *
	 * @param name service name
	 * @param cmd command to execute to provide service
	 * @param exec executor to schedule service execution on
	 * @return resulting service
	 * @since 1.2
	 */
	public static ExecutingService of(String name, Runnable cmd, ScheduledExecutorService exec) {
		return new RunnableService(name, cmd, exec);
	}

	private final ScheduledExecutorService executor;
	private final Lock stateLock;
	/*
	 * Service state:
	 *
	 * * `null` - nothing scheduled
	 * * `Future<?>` - execution scheduled
	 * * `Thread` - executing
	 * * `Pair<Thread, Long>` - executing and delay until next executio
	 * * `Pair<Thread, CountDownLatch>` - executing and awaiting shutdown
	 * * `this` - shutdown
	 */
	@GuardedBy("this.stateLock")
	private volatile @Nullable Object state;

	/**
	 * Construct new service.
	 *
	 * @param exec executor to schedule executions on
	 * @since 1.2
	 */
	public ExecutingService(ScheduledExecutorService exec) {
		this.executor = Preconditions.checkNotNull(exec);
		this.stateLock = new ReentrantLock();
	}

	/**
	 * Determine state number of a state object.
	 *
	 * @param curr state object to determine state number of
	 * @return state number of {@code curr}
	 */
	@SuppressWarnings("unchecked")
	private @State int stateNumberOf(@Nullable Object curr) {
		if (curr == this)
			return STATE_SHUTDOWN;
		if (curr == null)
			return STATE_IDLE;
		if (curr instanceof Future<?>)
			return STATE_SCHEDULED;
		if (curr instanceof Thread)
			return STATE_RUNNING;

		Pair<Thread, ?> exec = (Pair<Thread, ?>) curr;

		return exec.second instanceof CountDownLatch ? STATE_TERMINATING : STATE_RUNNING;
	}

	/**
	 * Current service state.
	 *
	 * @return state
	 * @since 1.2
	 */
	public @State int state() {
		return this.stateNumberOf(this.state);
	}

	/**
	 * Delay, in milliseconds, until next execution.
	 *
	 * @return next execution delay or, {@code -1L} if {@linkplain #STATE_IDLE idle}, {@linkplain
	 * #STATE_TERMINATING terminating}, or {@linkplain #STATE_SHUTDOWN shutdown}
	 * @since 1.2
	 */
	public @IntRange(from = -1L) long nextExecutionDelayMillis() {
		Object state = this.state;

		if (state instanceof Pair<?, ?>) {
			Object delay = ((Pair<?, ?>) state).second;

			return delay instanceof Long ? (long) delay : -1L;
		}
		if (state instanceof ScheduledFuture<?>)
			return ((ScheduledFuture<?>) state).getDelay(TimeUnit.MILLISECONDS);
		if (state instanceof Future<?> || state instanceof Thread)
			return 0L;
		return -1L;
	}

	/**
	 * Run service logic.
	 * <p>This is invoked some time after execution is {@linkplain #schedule(long, TimeUnit)
	 * scheduled}. Until this returns, its guaranteed that {@linkplain #state() state} will be
	 * <i>either</i> {@linkplain #STATE_RUNNING running} or {@linkplain #STATE_TERMINATING
	 * terminating}. If terminating, this <i>should</i> return, see {@link #shutdown() for more
	 * information}.
	 *
	 * @throws Exception error encountered
	 * @since 1.2
	 */
	@RestrictTo(RestrictTo.Scope.SUBCLASSES)
	protected abstract void run() throws Exception;

	@GuardedBy("this.stateLock")
	private @Nullable Future<?> scheduleExecute(long delayMs) {
		Runnable cmd = this::execute;

		try {
			return (
				delayMs == 0L ? this.executor.submit(cmd) :
				this.executor.schedule(cmd, delayMs, TimeUnit.MILLISECONDS)
			);
		} catch (Throwable cause) {
			Logger.warn(TAG, "failed to schedule execution", cause);
			return null;
		}
	}

	/**
	 * Try to enter into the {@linkplain #STATE_RUNNING running} state for the current thread.
	 * <p>If this returns {@code true}, the caller <i>must</i> exit out of the running state by
	 * invoking {@link #exitRunning()}.
	 *
	 * @return {@code true} if, and only if, running state was entered into
	 */
	private boolean tryEnterRunning() {
		this.stateLock.lock();
		try {
			Object state = this.state;

			if (!(state instanceof Future<?>)) {
				/*
				 * we're either shutting down, or the two executions are running at the same
				 * time, either case, bail.
				 */
				return false;
			}
			this.state = Thread.currentThread();
			// cancel the future just in case
			((Future<?>) state).cancel(false);
			return true;
		} finally {
			this.stateLock.unlock();
		}
	}

	/**
	 * Exit out of {@linkplain #STATE_RUNNING running} state.
	 * <p>This may be invoked <i>only</i> by the thread which successfully {@linkplain
	 * #tryEnterRunning() entered} into the running state.
	 */
	@SuppressWarnings("unchecked")
	private void exitRunning() {
		Thread thr = Thread.currentThread();
		CountDownLatch shutdown;

		this.stateLock.lock();
		try {
			if (this.state == thr) {
				// easy, nothing else was scheduled nor were we shutdown: done
				this.state = null;
				return;
			}
			/*
			 * at this point, state must be `Pair<Thread, CountDownLatch | Long>`, if the second part
			 * of the tuple is a `CountDownLatch`, then we're shutdown, otherwise if it's a `Long` then
			 * we're being scheduled again. In either case, synchronize on the state to provide an
			 * exclusion against `shutdown()`.
			 */
			Pair<Thread, ?> state = (Pair<Thread, ?>) this.state;

			Preconditions.checkState(state != null && state.first == thr, "malformed state");
			//noinspection DataFlowIssue
			if (state.second instanceof Number) {
				this.state = this.scheduleExecute(((Number) state.second).longValue());
				return;
			}
			shutdown = (CountDownLatch) state.second;
			this.state = this;
		} finally {
			this.stateLock.unlock();
		}
		// consume any interrupt
		//noinspection ResultOfMethodCallIgnored
		Thread.interrupted();
		shutdown.countDown();
	}

	/**
	 * Try and execute service.
	 */
	private void execute() {
		if (!this.tryEnterRunning()) {
			Logger.debug(TAG, "service %s failed to enter running state", this);
			return;
		}
		Logger.debug(TAG, "service %s entered running state", this);
		try {
			this.run();
		} catch (Throwable cause) {
			Logger.warn(TAG, "service failed", cause);
		} finally {
			this.exitRunning();
			Logger.debug(TAG, "service %s exited running state", this);
		}
	}

	/* @see #scheudle(long, long, TimeUnit) */
	@GuardedBy("this.stateLock")
	private boolean doSchedule(long delay, long slack, TimeUnit unit) {
		long delayMs = unit.toMillis(delay);
		long slackMs = unit.toMillis(slack);
		Object curr = this.state;

		if (curr == this)
			return false;
		if (curr instanceof Pair<?, ?>) {
			// we're running and, shutting down or have a chained execution scheduled
			Pair<?, ?> state = (Pair<?, ?>) curr;

			if (state.second instanceof CountDownLatch) {
				// shutdown, nothing to schedule
				return false;
			}

			// chained execution: if execution is earlier than us, then we're done
			long confDelayMs = (long) state.second;

			if (confDelayMs > delayMs && (confDelayMs - delayMs) > slackMs)
				this.state = new Pair<>(state.first, delayMs);
		} else if (curr instanceof Thread) {
			// already running, chain execution
			this.state = new Pair<>(curr, delayMs);
		} else if (curr instanceof ScheduledFuture<?>) {
			// already scheduled, if scheduled earlier than us, we're done
			ScheduledFuture<?> fut = (ScheduledFuture<?>) curr;
			long confDelayMs = fut.getDelay(TimeUnit.MILLISECONDS);

			if (confDelayMs <= delayMs || (confDelayMs - delayMs) <= slackMs)
				return true;
			/*
			 * cancel the scheduled execution, and if it succeeds, schedule our shorter execution.
			 * if we can't cancel the scheduled execution, then it's probably going to begin soon
			 * anyway.
			 */
			if (fut.cancel(false))
				this.state = this.scheduleExecute(delayMs);
		} else if (!(curr instanceof Future<?>)) {
			this.state = this.scheduleExecute(delayMs);
		}
		return true;
	}

	/**
	 * Schedule execution at a delay, with slack.
	 * <p>If service is {@linkplain #STATE_TERMINATING terminating} or has {@linkplain
	 * #STATE_SHUTDOWN shutdown}, then this simply returns {@code false}. If another execution is
	 * already scheduled at a delay, and the slack between its delay and the specified delay is
	 * less than or equal to {@code slack}, this simply returns {@code true}; otherwise, any
	 * existing scheduled execution, if any, is cancelled, and a new execution is scheduled.
	 *
	 * @param delay delay to schedule execution at
	 * @param slack maximum slack to allow between {@code delay} and any existing scheduled
	 * execution
	 * @param unit unit of measurement {@code delay} and {@code slack} are in
	 * @return {@code true} if, and only if, execution is scheduled; otherwise, {@code false}
	 * if service has {@linkplain #shutdown() shutdown}
	 * @since 1.2
	 */
	public final boolean schedule(long delay, long slack, TimeUnit unit) {
		this.stateLock.lock();
		try {
			return this.doSchedule(delay, slack, unit);
		} finally {
			this.stateLock.unlock();
		}
	}

	/**
	 * Schedule execution at a delay.
	 * <p>Shorthand for:
	 * {@snippet lang="java" :
	 * schedule(delay, 0, unit); // @link substring="schedule" target="#schedule(long, long, TimeUnit)"
	 * }
	 *
	 * @param delay delay to schedule execution at
	 * @param unit unit of measurement {@code delay} is in
	 * @return {@code true} if, and only if, execution is scheduled; otherwise, {@code false}
	 * if service has {@linkplain #shutdown() shutdown}
	 * @since 1.2
	 * @see #schedule(long, long, TimeUnit)
	 */
	@SuppressWarnings("JavadocDeclaration")
	public final boolean schedule(long delay, TimeUnit unit) {
		return this.schedule(delay, 0, unit);
	}

	/**
	 * Schedule immediate execution.
	 * <p>Shorthand for:
	 * {@snippet lang="java" :
	 * schedule(0, TimeUnit.MILLISECONDS); // @link substring="schedule" target="#schedule(long, TimeUnit)"
	 * }
	 *
	 * @return {@code true} if, and only if, execution is scheduled; otherwise, {@code false}
	 * if service has {@linkplain #shutdown() shutdown}
	 * @since 1.2
	 * @see #schedule(long, TimeUnit)
	 */
	@SuppressWarnings("JavadocDeclaration")
	public final boolean schedule() {
		return this.schedule(0, TimeUnit.MILLISECONDS);
	}

	/**
	 * Shutdown service.
	 * <p>If service is {@linkplain #STATE_TERMINATING terminating} or has already {@linkplain
	 * #STATE_SHUTDOWN shutdown}, this simply returns. Otherwise, if service is {@linkplain
	 * #STATE_IDLE idle}, {@linkplain #STATE_SCHEDULED scheduled}, or {@linkplain #STATE_RUNNING
	 * running}, service is shutdown immediately, scheduled execution is cancelled and service is
	 * shutdown immediately or, executing thread is {@linkplain Thread#interrupt() interrupted}
	 * and service is entered into a {@linkplain #STATE_TERMINATING terminating} state,
	 * respectively.
	 *
	 * @since 1.2
	 * @see #awaitShutdown(long, TimeUnit)
	 */
	@CallSuper
	@SuppressWarnings({ "fallthrough", "unchecked" })
	public void shutdown() {
		this.stateLock.lock();
		try {
			Object curr = this.state;

			switch (this.stateNumberOf(curr)) {
			case STATE_SHUTDOWN:
			case STATE_TERMINATING:
				// nothing to do, already shutdown
				return;
			case STATE_RUNNING:
				// we're running, attach a countdown latch so we can be awaited later
				CountDownLatch latch = new CountDownLatch(1);

				if (curr instanceof Thread) {
					this.state = new Pair<>(curr, latch);
					((Thread) curr).interrupt();
					break;
				}
				// to protect against chained schedule in `exitRunning()`, synchronize here
				//noinspection DataFlowIssue
				this.state = new Pair<>(((Pair<?, ?>) curr).first, latch);
				break;
			case STATE_SCHEDULED:
				//noinspection DataFlowIssue
				((Future<?>) curr).cancel(false);
			default:
				this.state = this;
				break;
			}
		} finally {
			this.stateLock.unlock();
		}
		Logger.debug(TAG, "service %s shutting down", this);
	}

	@SuppressWarnings("unchecked")
	private @Nullable CountDownLatch shutdownLatch() {
		Object curr = this.state;
		int num = stateNumberOf(curr);

		if (num == STATE_SHUTDOWN)
			return null;

		Preconditions.checkState(num == STATE_TERMINATING, "not shutting down");

		Pair<Thread, CountDownLatch> thrAndLatch = (Pair<Thread, CountDownLatch>) curr;

		//noinspection DataFlowIssue
		Preconditions.checkState(
			thrAndLatch.first != Thread.currentThread(),
			"cannot await shutdown in `run()`"
		);
		return thrAndLatch.second;
	}

	/**
	 * Await {@linkplain #shutdown() shutdown} completion.
	 * <p>If service has already entered the shutdown {@linkplain #STATE_SHUTDOWN state}, this
	 * simply returns. Otherwise, if the service is {@linkplain #STATE_TERMINATING terminating},
	 * this will wait for at most {@code timeout} duration for the service to end terminating and
	 * enter shutdown state. If {@link #shutdown()} has not been invoked, this {@linkplain
	 * IllegalStateException fails} unconditionally.
	 *
	 * @param timeout maximum duration to wait
	 * @param unit unit {@code timeout} is measured in
	 * @return {@code true} if, and only if, service has shutdown; otherwise, {@code false} if
	 * timeout elapsed
	 * @throws IllegalStateException {@link #shutdown()} has not been invoked
	 * @throws InterruptedException interrupted while awaiting shutdown
	 * @since 1.2
	 */
	@WorkerThread
	public final boolean awaitShutdown(long timeout, TimeUnit unit) throws InterruptedException {
		CountDownLatch latch = this.shutdownLatch();

		return latch == null || latch.await(timeout, unit);
	}

	/**
	 * Await {@linkplain #shutdown() shutdown} completion indefinitely.
	 * <p>Like {@link #awaitShutdown(long, TimeUnit)}; however, this does not impose a time limit.
	 *
	 * @throws IllegalStateException {@link #shutdown()} has not been invoked
	 * @throws InterruptedException interrupted while awaiting shutdown
	 * @since 1.2
	 * @see #awaitShutdown(long, TimeUnit)
	 */
	@WorkerThread
	public final void awaitShutdown() throws InterruptedException {
		CountDownLatch latch = this.shutdownLatch();

		if (latch != null)
			latch.await();
	}
}
