// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.os.Handler;
import android.os.SystemClock;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Exchanger;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@link ListeningExecutor} tests.
 */
@RunWith(AndroidJUnit4.class)
public class ListeningExecutorTest {

	private Thread executorThread;
	private ListeningExecutor executor;
	private ExecutorService listenerExecutor;

	@Before
	public void setup() {
		ExecutorService base = Executors.newFixedThreadPool(1);

		this.executorThread = Futures.await(base.submit(Thread::currentThread));
		this.executor = new ListeningExecutor(
			new Handler(Tests.context().getMainLooper()),
			base
		);
		this.listenerExecutor = Executors.newSingleThreadExecutor();
	}

	@After
	public void destroy() {
		this.executor.shutdownNow();
		((ExecutorService) this.executor).shutdownNow();
		this.listenerExecutor.shutdownNow();
	}

	private void awaitListener(ListenableFuture<?> fut, Runnable listener) {
		Exchanger<Object> error = new Exchanger<>();

		fut.addListener(() -> {
			try {
				listener.run();
				error.exchange(true);
			} catch (Exception err) {
				try {
					error.exchange(err);
				} catch (InterruptedException err2) {
					throw new AssertionError(err2);
				}
			}
		}, this.listenerExecutor);

		Object res;

		try {
			res = error.exchange(true);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		if (res instanceof Throwable)
			throw new AssertionError("listener failed", (Throwable) res);
	}

	@Test
	public void testShutdown() {
		assertFalse(this.executor.isShutdown());
		assertFalse(this.executor.isTerminated());

		this.executor.shutdown();

		assertTrue(this.executor.isShutdown());
		assertTrue(this.executor.isTerminated());

		assertThrows(RejectedExecutionException.class, () -> this.executor.submit(() -> {}));
	}

	@Test
	public void testShutdownNow() throws InterruptedException {
		CountDownLatch latch = new CountDownLatch(1);
		Callable<Boolean> task = () -> {
			latch.await();
			return true;
		};
		Future<Boolean> futA = this.executor.submit(task);
		Future<Boolean> futB = this.executor.schedule(task, 5, TimeUnit.SECONDS);

		assertFalse(futA.isDone());
		assertFalse(futB.isDone());
		this.executor.shutdownNow();
		assertTrue(this.executor.isShutdown());
		assertThrows(CancellationException.class, futA::get);
		assertThrows(CancellationException.class, futB::get);
		assertTrue(this.executor.isTerminated());
	}

	@Test
	public void testSubmit() {
		ListenableFuture<Boolean> futA = this.executor.submit(() -> {
			assertEquals(this.executorThread, Thread.currentThread());
			return true;
		});

		this.awaitListener(futA, () -> {
			assertTrue(futA.isDone());
			assertFalse(futA.cancel(false));
			assertEquals(true, Futures.await(futA));
		});

		ListenableFuture<Boolean> futB = this.executor.submit(() -> {
			assertEquals(this.executorThread, Thread.currentThread());
			throw new Exception();
		});

		this.awaitListener(futB, () -> {
			assertTrue(futB.isDone());
			assertFalse(futB.cancel(false));
			assertThrows(ExecutionException.class, futB::get);
		});

		CountDownLatch latch = new CountDownLatch(1);
		ListenableFuture<Boolean> futC = this.executor.submit(() -> {
			latch.await();
			return true;
		});
		ListenableFuture<Boolean> futD = this.executor.submit(() -> true);

		assertTrue(futD.cancel(false));
		assertFalse(futD.cancel(true));
		this.awaitListener(futD, () -> {
			assertTrue(futD.isDone());
			assertTrue(futD.isCancelled());
			assertFalse(futD.cancel(false));
			assertThrows(CancellationException.class, futD::get);
		});
		latch.countDown();
		assertTrue(Futures.await(futC));
	}

	@Test
	public void testSchedule() {
		long nowA = SystemClock.uptimeMillis();
		ListenableFuture<Long> futA =
			this.executor.schedule(SystemClock::uptimeMillis, 0, TimeUnit.MILLISECONDS);

		this.awaitListener(futA, () -> {
			assertTrue(futA.isDone());
			assertTrue((Futures.await(futA) - nowA) >= 0);
		});

		long nowB = SystemClock.uptimeMillis();
		ListenableFuture<Long> futB =
			this.executor.schedule(SystemClock::uptimeMillis, 100, TimeUnit.MILLISECONDS);

		this.awaitListener(futB, () -> {
			assertTrue(futB.isDone());
			assertTrue((Futures.await(futB) - nowB) >= 100);
		});

		ListenableFuture<Long> futC =
			this.executor.schedule(SystemClock::uptimeMillis, 100, TimeUnit.MILLISECONDS);

		futC.cancel(true);
		SystemClock.sleep(200);
		assertTrue(futC.isDone());
		assertTrue(futC.isCancelled());
	}

	@Test
	public void testExecuteAndShutdown() throws InterruptedException {
		CountDownLatch latch = new CountDownLatch(1);
		Callable<Boolean> task = () -> {
			latch.await();
			return true;
		};
		Future<Boolean> futA = this.executor.submit(task);
		Future<Boolean> futB = this.executor.schedule(task, 5, TimeUnit.SECONDS);

		this.executor.shutdown();
		assertTrue(this.executor.isShutdown());
		assertFalse(this.executor.isTerminated());
		assertThrows(RejectedExecutionException.class, () -> this.executor.submit(() -> {}));
		latch.countDown();
		assertTrue(Futures.await(futA));
		assertFalse(futB.isDone());
		assertFalse(this.executor.isTerminated());
		assertTrue(this.executor.awaitTermination(7, TimeUnit.SECONDS));
		assertTrue(futB.isDone());
		assertTrue(Futures.await(futB));
		assertTrue(this.executor.isShutdown());
		assertTrue(this.executor.isTerminated());
		assertThrows(RejectedExecutionException.class, () -> this.executor.submit(() -> {}));
	}

	@Test
	public void testScheduleAtFixedRate() {
		final int NUM_PERIODS = 10;
		final long PERIOD_MS = 100;

		AtomicInteger periods = new AtomicInteger();
		long nowA = SystemClock.uptimeMillis();
		ListenableFuture<?> futA = this.executor.scheduleAtFixedRate(
			() -> {
				int period = periods.getAndIncrement();

				assertTrue(
					(SystemClock.uptimeMillis() - nowA) >=
					((period * PERIOD_MS) + PERIOD_MS)
				);
			},
			PERIOD_MS,
			PERIOD_MS,
			TimeUnit.MILLISECONDS
		);

		for (int i = 1; i <= NUM_PERIODS; i++) {
			SystemClock.sleep(PERIOD_MS + 5);

			int period = periods.get();

			assertTrue(period >= i && period <= (i + 1));
		}
		futA.cancel(false);
		this.awaitListener(futA, () -> {
			assertThrows(CancellationException.class, futA::get);
			assertTrue(futA.isCancelled());
			assertTrue(futA.isDone());
			assertTrue(periods.get() >= NUM_PERIODS);
		});
	}

	@Test
	public void testScheduleAtFixedRateAndShutdown() throws InterruptedException {
		final int NUM_PERIODS = 10;
		final long PERIOD_MS = 100;

		AtomicInteger periods = new AtomicInteger();
		long nowA = SystemClock.uptimeMillis();
		ListenableFuture<?> futA = this.executor.scheduleAtFixedRate(
			() -> {
				int period = periods.getAndIncrement();

				assertTrue(
					(SystemClock.uptimeMillis() - nowA) >=
						((period * PERIOD_MS) + PERIOD_MS)
				);
			},
			PERIOD_MS,
			PERIOD_MS,
			TimeUnit.MILLISECONDS
		);

		this.executor.shutdown();
		assertTrue(this.executor.isShutdown());
		assertFalse(this.executor.isTerminated());
		assertThrows(
			RejectedExecutionException.class,
			() -> this.executor.scheduleAtFixedRate(() -> {}, 10, 10, TimeUnit.MILLISECONDS)
		);
		for (int i = 1; i <= NUM_PERIODS; i++) {
			assertFalse(this.executor.awaitTermination(PERIOD_MS + 5, TimeUnit.MILLISECONDS));

			int period = periods.get();

			assertTrue(period >= i && period <= (i + 1));
		}
		futA.cancel(false);
		this.awaitListener(futA, () -> {
			assertThrows(CancellationException.class, futA::get);
			assertTrue(futA.isCancelled());
			assertTrue(futA.isDone());
			assertTrue(periods.get() >= NUM_PERIODS);
		});

		assertTrue(this.executor.awaitTermination(PERIOD_MS, TimeUnit.MILLISECONDS));
		assertTrue(this.executor.isTerminated());
		assertTrue(this.executor.awaitTermination(0, TimeUnit.MILLISECONDS));
	}

	@Test
	public void testScheduleAtFixedRateAndShutdownNow() throws InterruptedException {
		final int NUM_PERIODS = 10;
		final long PERIOD_MS = 100;

		AtomicInteger periods = new AtomicInteger();
		long nowA = SystemClock.uptimeMillis();
		ListenableFuture<?> futA = this.executor.scheduleAtFixedRate(
			() -> {
				int period = periods.getAndIncrement();

				assertTrue(
					(SystemClock.uptimeMillis() - nowA) >=
						((period * PERIOD_MS) + PERIOD_MS)
				);
			},
			PERIOD_MS,
			PERIOD_MS,
			TimeUnit.MILLISECONDS
		);

		for (int i = 1; i <= NUM_PERIODS; i++) {
			assertFalse(this.executor.awaitTermination(PERIOD_MS + 5, TimeUnit.MILLISECONDS));

			int period = periods.get();

			assertTrue(period >= i && period <= (i + 1));
		}
		this.executor.shutdownNow();
		assertTrue(this.executor.isShutdown());
		assertTrue(this.executor.isTerminated());
		this.awaitListener(futA, () -> {
			assertThrows(CancellationException.class, futA::get);
			assertTrue(futA.isCancelled());
			assertTrue(futA.isDone());
			assertTrue(periods.get() >= NUM_PERIODS);
		});

		assertTrue(this.executor.awaitTermination(0, TimeUnit.MILLISECONDS));
		assertTrue(this.executor.isTerminated());
	}

	@Test
	public void testScheduleWithFixedDelayAndShutdown() throws InterruptedException {
		final int NUM_PERIODS = 10;
		final long PERIOD_MS = 100;

		AtomicInteger periods = new AtomicInteger();
		long nowA = SystemClock.uptimeMillis();
		ListenableFuture<?> futA = this.executor.scheduleWithFixedDelay(
			() -> {
				int period = periods.getAndIncrement();

				assertTrue(
					(SystemClock.uptimeMillis() - nowA) >=
					((period * PERIOD_MS) + PERIOD_MS)
				);
			},
			PERIOD_MS,
			PERIOD_MS,
			TimeUnit.MILLISECONDS
		);

		this.executor.shutdown();
		assertTrue(this.executor.isShutdown());
		assertFalse(this.executor.isTerminated());
		assertThrows(
			RejectedExecutionException.class,
			() -> this.executor.scheduleWithFixedDelay(() -> {}, 10, 10, TimeUnit.MILLISECONDS)
		);
		for (int i = 1; i <= NUM_PERIODS; i++) {
			assertFalse(this.executor.awaitTermination(PERIOD_MS + 5, TimeUnit.MILLISECONDS));

			int period = periods.get();

			assertTrue(period >= i && period <= (i + 1));
		}
		futA.cancel(false);
		this.awaitListener(futA, () -> {
			assertThrows(CancellationException.class, futA::get);
			assertTrue(futA.isCancelled());
			assertTrue(futA.isDone());
			assertTrue(periods.get() >= NUM_PERIODS);
		});

		assertTrue(this.executor.awaitTermination(PERIOD_MS, TimeUnit.MILLISECONDS));
		assertTrue(this.executor.isTerminated());
		assertTrue(this.executor.awaitTermination(0, TimeUnit.MILLISECONDS));
	}

	@Test
	public void testScheduleWithFixedDelayAndShutdownNow() throws InterruptedException {
		final int NUM_PERIODS = 10;
		final long PERIOD_MS = 100;

		AtomicInteger periods = new AtomicInteger();
		long nowA = SystemClock.uptimeMillis();
		ListenableFuture<?> futA = this.executor.scheduleWithFixedDelay(
			() -> {
				int period = periods.getAndIncrement();

				assertTrue(
					(SystemClock.uptimeMillis() - nowA) >=
					((period * PERIOD_MS) + PERIOD_MS)
				);
			},
			PERIOD_MS,
			PERIOD_MS,
			TimeUnit.MILLISECONDS
		);

		for (int i = 1; i <= NUM_PERIODS; i++) {
			assertFalse(this.executor.awaitTermination(PERIOD_MS + 5, TimeUnit.MILLISECONDS));

			int period = periods.get();

			assertTrue(period >= i && period <= (i + 1));
		}
		this.executor.shutdownNow();
		assertTrue(this.executor.isShutdown());
		assertTrue(this.executor.isTerminated());
		this.awaitListener(futA, () -> {
			assertThrows(CancellationException.class, futA::get);
			assertTrue(futA.isCancelled());
			assertTrue(futA.isDone());
			assertTrue(periods.get() >= NUM_PERIODS);
		});

		assertTrue(this.executor.awaitTermination(0, TimeUnit.MILLISECONDS));
		assertTrue(this.executor.isTerminated());
	}
}
