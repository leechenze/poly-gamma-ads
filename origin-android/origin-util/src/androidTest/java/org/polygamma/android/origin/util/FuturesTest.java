// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.polygamma.android.origin.util.Futures.*;

import android.os.SystemClock;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * {@link Futures} tests.
 */
@RunWith(AndroidJUnit4.class)
public class FuturesTest {

	@Test
	public void testOf() throws ExecutionException, InterruptedException {
		ListenableFuture<String> a = of("abc");

		assertTrue(a.isDone());
		assertFalse(a.isCancelled());
		assertEquals("abc", a.get());

		boolean[] v = new boolean[1];

		a.addListener(() -> v[0] = true, Runnable::run);
		assertTrue(v[0]);
	}

	@Test
	public void testOfVoid() throws ExecutionException, InterruptedException {
		ListenableFuture<Void> a = ofVoid();

		assertTrue(a.isDone());
		assertFalse(a.isCancelled());
		assertNull(a.get());

		boolean[] v = new boolean[1];

		a.addListener(() -> v[0] = true, Runnable::run);
		assertTrue(v[0]);
	}

	@Test
	public void testOfError() {
		IllegalArgumentException err = new IllegalArgumentException();
		ListenableFuture<String> a = ofError(err);

		assertTrue(a.isDone());
		assertFalse(a.isCancelled());
		assertEquals(err, assertThrows(ExecutionException.class, a::get).getCause());

		boolean[] v = new boolean[1];

		a.addListener(() -> v[0] = true, Runnable::run);
		assertTrue(v[0]);
	}

	@Test
	public void testAwait() {
		assertTrue(
			assertThrows(
				RuntimeException.class,
				() -> Futures.await(Futures.ofError(new IllegalArgumentException()))
			).getCause()
			instanceof
			ExecutionException
		);
		assertEquals("abc", Futures.await(Futures.of("abc")));
		assertNull(Futures.await(Futures.ofVoid()));

		FutureTask<Boolean> fut = new FutureTask<>(() -> true);

		(new Thread(() -> {
			try {
				Thread.sleep(500);
			} catch (InterruptedException ignored) {
			} finally {
				fut.run();
			}
		})).start();

		assertTrue(Futures.await(fut));
	}

	@Test
	public void testCancel() throws InterruptedException {
		assertTrue(Futures.cancel(null, false));
		assertTrue(Futures.cancel(null, true));
		assertTrue(Futures.cancel(new FutureTask<>(() -> true), false));
		assertTrue(Futures.cancel(new FutureTask<>(() -> true), true));
		assertFalse(Futures.cancel(Futures.ofVoid(), false));
		assertFalse(Futures.cancel(Futures.ofVoid(), true));

		CountDownLatch enterFuture = new CountDownLatch(1);
		CountDownLatch exitFuture = new CountDownLatch(1);
		AtomicBoolean interrupted = new AtomicBoolean();
		FutureTask<?> fut =
			new FutureTask<>(() -> {
				enterFuture.countDown();
				while (true) {
					try {
						exitFuture.await();
						return null;
					} catch (InterruptedException ignored) {
						interrupted.set(true);
					}
				}
			});

		(new Thread(fut)).start();

		enterFuture.await();
		assertTrue(Futures.cancel(fut, true));
		SystemClock.sleep(600); /* wait for interrupt to be delivered */
		exitFuture.countDown();
		assertTrue(fut.isCancelled());
		assertTrue(interrupted.get());
	}
}
