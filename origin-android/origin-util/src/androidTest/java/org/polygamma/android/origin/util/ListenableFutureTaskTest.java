// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import androidx.annotation.Nullable;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * {@link ListenableFutureTask} tests.
 */
@RunWith(AndroidJUnit4.class)
public class ListenableFutureTaskTest {

	private static final class TestListener implements Runnable {

		final BlockingQueue<TestListener> called;
		final @Nullable Thread expectThread;
		@Nullable Thread callThread;
		int callCount;

		TestListener(BlockingQueue<TestListener> called, @Nullable Thread expThread) {
			this.called = called;
			this.expectThread = expThread;
		}

		@Override
		public void run() {
			this.callCount++;
			this.callThread = Thread.currentThread();
			this.called.add(this);
		}
	}

	@Test
	public void testRun() throws Exception {
		int[] runCount = new int[1];
		LinkedBlockingQueue<TestListener> calledListeners = new LinkedBlockingQueue<>();
		ListenableFutureTask<Integer> task = new ListenableFutureTask<>(() -> ++runCount[0]);
		ExecutorService exec = Executors.newSingleThreadExecutor();

		try {
			Futures.addDirectListener(task, new TestListener(calledListeners, Thread.currentThread()));
			task.addListener(new TestListener(calledListeners, null), exec);

			assertFalse(task.isDone());
			task.run();
			assertTrue(task.isDone());

			for (int i = 0; i < 2; i++) {
				TestListener called = calledListeners.take();

				if (called.expectThread == null)
					assertNotSame(Thread.currentThread(), called.callThread);
				else
					assertSame(called.expectThread, called.callThread);
				assertEquals(1, called.callCount);
			}
			assertNull(calledListeners.poll());

			Futures.addDirectListener(task, new TestListener(calledListeners, Thread.currentThread()));
			task.addListener(new TestListener(calledListeners, null), exec);

			for (int i = 0; i < 2; i++) {
				TestListener called = calledListeners.take();

				if (called.expectThread == null)
					assertNotSame(Thread.currentThread(), called.callThread);
				else
					assertSame(called.expectThread, called.callThread);
				assertEquals(1, called.callCount);
			}
			assertNull(calledListeners.poll());

			// running again shouldn't do anything
			task.run();
			assertNull(calledListeners.poll(15, TimeUnit.MILLISECONDS));

			// result should be 1
			assertEquals(1, (int) task.get());
			assertEquals(1, runCount[0]);

			// cancelling should have no effect
			assertFalse(task.cancel(false));

			assertTrue(task.isDone());
			assertFalse(task.isCancelled());
		} finally {
			exec.shutdown();
		}
	}

	@Test
	public void testFail() throws Exception {
		LinkedBlockingQueue<TestListener> calledListeners = new LinkedBlockingQueue<>();
		ListenableFutureTask<Integer> task = new ListenableFutureTask<>(() -> {
			throw new IllegalStateException("test");
		});
		ExecutorService exec = Executors.newSingleThreadExecutor();

		try {
			Futures.addDirectListener(task, new TestListener(calledListeners, Thread.currentThread()));
			task.addListener(new TestListener(calledListeners, null), exec);

			assertFalse(task.isDone());
			task.run();
			assertTrue(task.isDone());

			for (int i = 0; i < 2; i++) {
				TestListener called = calledListeners.take();

				if (called.expectThread == null)
					assertNotSame(Thread.currentThread(), called.callThread);
				else
					assertSame(called.expectThread, called.callThread);
				assertEquals(1, called.callCount);
			}
			assertNull(calledListeners.poll());

			Futures.addDirectListener(task, new TestListener(calledListeners, Thread.currentThread()));
			task.addListener(new TestListener(calledListeners, null), exec);

			for (int i = 0; i < 2; i++) {
				TestListener called = calledListeners.take();

				if (called.expectThread == null)
					assertNotSame(Thread.currentThread(), called.callThread);
				else
					assertSame(called.expectThread, called.callThread);
				assertEquals(1, called.callCount);
			}
			assertNull(calledListeners.poll());

			// running again shouldn't do anything
			task.run();
			assertNull(calledListeners.poll(15, TimeUnit.MILLISECONDS));

			assertThrows(ExecutionException.class, task::get);
		} finally {
			exec.shutdown();
		}
	}

	@Test
	public void testCancel() throws Exception {
		LinkedBlockingQueue<TestListener> calledListeners = new LinkedBlockingQueue<>();
		ListenableFutureTask<Integer> task = new ListenableFutureTask<>(() -> {
			throw new IllegalStateException("test");
		});
		ExecutorService exec = Executors.newSingleThreadExecutor();

		try {
			Futures.addDirectListener(task, new TestListener(calledListeners, Thread.currentThread()));
			task.addListener(new TestListener(calledListeners, null), exec);

			assertFalse(task.isDone());
			task.cancel(false);
			assertTrue(task.isDone());
			assertTrue(task.isCancelled());

			for (int i = 0; i < 2; i++) {
				TestListener called = calledListeners.take();

				if (called.expectThread == null)
					assertNotSame(Thread.currentThread(), called.callThread);
				else
					assertSame(called.expectThread, called.callThread);
				assertEquals(1, called.callCount);
			}
			assertNull(calledListeners.poll());

			// running again shouldn't do anything
			task.run();
			assertNull(calledListeners.poll(15, TimeUnit.MILLISECONDS));

			assertThrows(CancellationException.class, task::get);
		} finally {
			exec.shutdown();
		}
	}
}
