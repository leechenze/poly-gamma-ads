// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.os.SystemClock;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@link ExecutingService} test.
 */
@RunWith(AndroidJUnit4.class)
public class ExecutingServiceTest {

	private static final class TestExecutor implements ScheduledExecutorService {

		private final ScheduledExecutorService inner;
		final AtomicInteger taskCount;

		TestExecutor() {
			this.inner = Executors.newSingleThreadScheduledExecutor();
			this.taskCount = new AtomicInteger();
		}

		@Override
		public void execute(Runnable cmd) {
			this.taskCount.incrementAndGet();
			this.inner.execute(cmd);
		}

		@Override
		public <T> Future<T> submit(Callable<T> task) {
			this.taskCount.incrementAndGet();
			return this.inner.submit(task);
		}

		@Override
		public <T> Future<T> submit(Runnable task, T result) {
			this.taskCount.incrementAndGet();
			return this.inner.submit(task, result);
		}

		@Override
		public Future<?> submit(Runnable task) {
			this.taskCount.incrementAndGet();
			return this.inner.submit(task);
		}

		@Override
		public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
		throws InterruptedException {
			this.taskCount.incrementAndGet();
			return this.inner.invokeAll(tasks);
		}

		@Override
		public <T> List<Future<T>>
		invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
		throws InterruptedException {
			this.taskCount.incrementAndGet();
			return this.inner.invokeAll(tasks, timeout, unit);
		}

		@Override
		public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
		throws ExecutionException, InterruptedException {
			this.taskCount.incrementAndGet();
			return this.inner.invokeAny(tasks);
		}

		@Override
		public <T> T
		invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
		throws ExecutionException, InterruptedException, TimeoutException {
			this.taskCount.incrementAndGet();
			return this.inner.invokeAny(tasks, timeout, unit);
		}

		@Override
		public ScheduledFuture<?> schedule(Runnable cmd, long delay, TimeUnit unit) {
			this.taskCount.incrementAndGet();
			return this.inner.schedule(cmd, delay, unit);
		}

		@Override
		public <V> ScheduledFuture<V> schedule(Callable<V> cmd, long delay, TimeUnit unit) {
			this.taskCount.incrementAndGet();
			return this.inner.schedule(cmd, delay, unit);
		}

		@Override
		public ScheduledFuture<?>
		scheduleAtFixedRate(Runnable cmd, long initialDelay, long period, TimeUnit unit) {
			this.taskCount.incrementAndGet();
			return this.inner.scheduleAtFixedRate(cmd, initialDelay, period, unit);
		}

		@Override
		public ScheduledFuture<?>
		scheduleWithFixedDelay(Runnable cmd, long initialDelay, long delay, TimeUnit unit) {
			this.taskCount.incrementAndGet();
			return this.inner.scheduleAtFixedRate(cmd, initialDelay, delay, unit);
		}

		@Override
		public void shutdown() {
			this.inner.shutdown();
		}

		@Override
		public List<Runnable> shutdownNow() {
			return this.inner.shutdownNow();
		}

		@Override
		public boolean isShutdown() {
			return this.inner.isShutdown();
		}

		@Override
		public boolean isTerminated() {
			return this.inner.isTerminated();
		}

		@Override
		public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
			return this.inner.awaitTermination(timeout, unit);
		}
	}

	private static final class TestService extends ExecutingService {
		final BlockingQueue<Long> executions;
		final CyclicBarrier continueBarrier;
		boolean interrupted;

		TestService() {
			super(executor);
			this.executions = new LinkedBlockingQueue<>();
			this.continueBarrier = new CyclicBarrier(2);
		}

		@Override
		protected synchronized void run() throws Exception {
			this.executions.add(SystemClock.uptimeMillis());
			try {
				this.continueBarrier.await();
			} catch (InterruptedException ignored) {
				this.continueBarrier.reset();
				this.interrupted = true;
				Thread.interrupted();
				this.continueBarrier.await();
			}
		}
	}

	private static TestExecutor executor;

	@BeforeClass
	public static void init() {
		executor = new TestExecutor();
	}

	@AfterClass
	public static void destroy() {
		if (executor != null) {
			executor.shutdown();
			executor = null;
		}
	}

	@Before
	public void reset() {
		executor.taskCount.set(0);
	}

	@Test
	public void testSchedule() throws InterruptedException, BrokenBarrierException {
		TestService svc = new TestService();
		long now = SystemClock.uptimeMillis();
		long delta;
		int state;

		assertEquals(ExecutingService.STATE_IDLE, svc.state());
		assertTrue(svc.schedule());
		assertEquals(0L, svc.nextExecutionDelayMillis());

		state = svc.state();
		assertTrue(
			state == ExecutingService.STATE_SCHEDULED ||
			state == ExecutingService.STATE_RUNNING
		);

		delta = svc.executions.take() - now;
		assertTrue(delta >= 0 && delta <= 15);
		assertEquals(ExecutingService.STATE_RUNNING, svc.state());
		svc.continueBarrier.await();

		for (int i = 0; i < 5 && svc.state() != ExecutingService.STATE_IDLE; i++)
			SystemClock.sleep(5);
		assertEquals(ExecutingService.STATE_IDLE, svc.state());
		assertEquals(-1L, svc.nextExecutionDelayMillis());
		assertEquals(1, executor.taskCount.getAndSet(0));
		svc.continueBarrier.reset();

		now = SystemClock.uptimeMillis();
		assertTrue(svc.schedule(100, TimeUnit.MILLISECONDS));
		assertEquals(ExecutingService.STATE_SCHEDULED, svc.state());

		delta = svc.nextExecutionDelayMillis();
		assertTrue(delta >= 85 && delta <= 115);

		delta = svc.executions.take() - now;
		assertTrue(delta >= 85 && delta <= 115);
		assertEquals(ExecutingService.STATE_RUNNING, svc.state());
		svc.continueBarrier.await();

		for (int i = 0; i < 5 && svc.state() != ExecutingService.STATE_IDLE; i++)
			SystemClock.sleep(5);
		assertEquals(ExecutingService.STATE_IDLE, svc.state());
		assertEquals(-1L, svc.nextExecutionDelayMillis());
		assertEquals(1, executor.taskCount.getAndSet(0));
		svc.continueBarrier.reset();
	}

	@Test
	public void testReschedule() throws InterruptedException, BrokenBarrierException {
		TestService svc = new TestService();
		long now = 0;
		long delta;

		assertEquals(ExecutingService.STATE_IDLE, svc.state());

		for (long delay : new long[] { 5000, 1000, 100 }) {
			now = SystemClock.uptimeMillis();
			assertTrue(svc.schedule(delay, TimeUnit.MILLISECONDS));
			assertEquals(ExecutingService.STATE_SCHEDULED, svc.state());

			delta = svc.nextExecutionDelayMillis();
			assertTrue(delta >= (delay - 15) && delta <= (delay + 15));
		}

		delta = svc.executions.take() - now;
		assertTrue(delta >= 85 && delta <= 115);
		assertEquals(ExecutingService.STATE_RUNNING, svc.state());
		svc.continueBarrier.await();

		for (int i = 0; i < 5 && svc.state() != ExecutingService.STATE_IDLE; i++)
			SystemClock.sleep(5);
		assertEquals(ExecutingService.STATE_IDLE, svc.state());
		assertEquals(-1L, svc.nextExecutionDelayMillis());
		assertEquals(3, executor.taskCount.getAndSet(0));
		svc.continueBarrier.reset();
	}

	@Test
	public void testShutdown() throws InterruptedException {
		TestService svc = new TestService();
		long start;
		long delta;

		assertEquals(ExecutingService.STATE_IDLE, svc.state());
		assertThrows(IllegalStateException.class, svc::awaitShutdown);
		svc.shutdown();
		assertEquals(ExecutingService.STATE_SHUTDOWN, svc.state());

		start = SystemClock.uptimeMillis();
		svc.awaitShutdown();
		assertTrue(svc.awaitShutdown(1, TimeUnit.SECONDS));

		delta = SystemClock.uptimeMillis() - start;
		assertTrue(delta >= 0 && delta <= 15);
		assertFalse(svc.schedule());
		assertNull(svc.executions.poll());
		assertEquals(0, executor.taskCount.get());
	}

	@Test
	public void testScheduleAndShutdown() throws InterruptedException, BrokenBarrierException {
		TestService svc = new TestService();
		long start;
		long delta;

		assertEquals(ExecutingService.STATE_IDLE, svc.state());
		assertTrue(svc.schedule(100, TimeUnit.MILLISECONDS));
		assertEquals(ExecutingService.STATE_SCHEDULED, svc.state());
		svc.shutdown();
		assertEquals(ExecutingService.STATE_SHUTDOWN, svc.state());

		start = SystemClock.uptimeMillis();
		svc.awaitShutdown();
		assertTrue(svc.awaitShutdown(1, TimeUnit.SECONDS));

		delta = SystemClock.uptimeMillis() - start;
		assertTrue(delta >= 0 && delta <= 15);
		assertFalse(svc.schedule());
		assertNull(svc.executions.poll());
		assertEquals(1, executor.taskCount.getAndSet(0));

		// test when service is actively executing
		start = SystemClock.uptimeMillis();
		svc = new TestService();
		assertTrue(svc.schedule());

		delta = svc.executions.take() - start;
		assertTrue(delta >= 0 && delta <= 15);

		// shutting down here should interrupt our service
		svc.shutdown();
		SystemClock.sleep(15);
		assertTrue(svc.interrupted);
		assertEquals(ExecutingService.STATE_TERMINATING, svc.state());
		svc.continueBarrier.await();

		svc.awaitShutdown();
		assertEquals(ExecutingService.STATE_SHUTDOWN, svc.state());
		assertEquals(-1L, svc.nextExecutionDelayMillis());
		assertEquals(1, executor.taskCount.getAndSet(0));
		svc.continueBarrier.reset();
	}
}
