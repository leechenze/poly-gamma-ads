// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.os.SystemClock;
import android.util.Log;
import android.util.Pair;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.polygamma.android.origin.util.Futures;
import org.polygamma.android.origin.util.Sync;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * {@link Origin} tests.
 */
@RunWith(AndroidJUnit4.class)
public class OriginTest {

	private static final String TAG = OriginTest.class.getSimpleName();

	public static final class TestModuleA extends TestModule {
		public static Provider<TestModuleA> ofProvider() {
			return new TestProvider<>(
				TestModuleA.class,
				pair -> new TestModuleA(pair.first),
				null
			);
		}

		private TestModuleA(Origin sdk) {
			super("A", sdk);
		}
	}

	public static final class TestModuleB extends TestModule {
		public static Provider<TestModuleB> ofProvider() {
			return new TestProvider<>(
				TestModuleB.class,
				pair -> new TestModuleB(pair.first),
				null
			);
		}

		private TestModuleB(Origin sdk) {
			super("B", sdk);
		}
	}

	public static final class TestModuleC extends TestModule {
		public static Provider<TestModuleC> ofProvider() {
			return new TestProvider<>(
				TestModuleC.class,
				pair -> new TestModuleC(pair.first),
				null
			);
		}

		final TestModuleA a;
		final TestModuleB b;

		private TestModuleC(Origin sdk) {
			super("C", sdk);
			this.a = sdk.loadModule(TestModuleA.class);
			this.b = sdk.loadModule(TestModuleB.class);
		}
	}

	@After
	public void destroy() throws InterruptedException {
		Origin sdk = Origin.tryCurrent();

		if (sdk != null) {
			sdk.shutdown();
			while (!sdk.awaitShutdown(10, TimeUnit.SECONDS))
				Log.w(TAG, "shutdown taking longer than 10 seconds...");
		}
	}

	@Test
	public void testInitializeAndDestroy() throws InterruptedException {
		Context ctxt = TestUtil.context();

		assertThrows(IllegalStateException.class, Origin::current);

		TestUtil.runOnMainSync(() -> Origin.initialize(ctxt));

		Origin sdk = Origin.tryCurrent();

		assertNotNull(sdk);
		assertSame(sdk, Origin.current());
		assertFalse(sdk.isShutdown());
		TestUtil.runOnMainSync(() -> Origin.initialize(ctxt));
		assertSame(sdk, Origin.tryCurrent());
		assertSame(sdk, Origin.current());
		assertSame(ctxt, sdk.context());

		assertTrue(Futures.await(sdk.callInForeground(Sync::isMainThread)));
		assertFalse(Futures.await(sdk.callInBackground(Sync::isMainThread)));
		assertTrue(Futures.await(sdk.foregroundExecutor().submit(Sync::isMainThread)));
		assertFalse(Futures.await(sdk.backgroundExecutor().submit(Sync::isMainThread)));

		assertFalse(sdk.awaitShutdown(10, TimeUnit.MILLISECONDS));
		sdk.shutdown();
		assertTrue(sdk.awaitShutdown(10, TimeUnit.MILLISECONDS));
		assertTrue(sdk.isShutdown());
		assertNull(Origin.tryCurrent());
		assertThrows(IllegalStateException.class, Origin::current);

		assertThrows(RejectedExecutionException.class, () -> sdk.runInForeground(() -> {}));
		assertThrows(
			RejectedExecutionException.class,
			() -> sdk.foregroundExecutor().submit(() -> {})
		);
		assertThrows(
			RejectedExecutionException.class,
			() -> sdk.backgroundIoExecutor().submit(() -> {})
		);
	}

	@Test
	public void testInitializeAndDestroyWithModules() throws InterruptedException {
		OriginModule.Provider<?> a = TestModuleA.ofProvider();
		OriginModule.Provider<?> b = TestModuleB.ofProvider();
		OriginModule.Provider<?> c = TestModuleC.ofProvider();

		TestUtil.runOnMainSync(() -> Origin.initialize(TestUtil.context(), a));

		Origin sdk = Origin.current();
		TestModule mod = sdk.findModule(TestModuleA.class);

		assertNotNull(mod);
		assertNull(sdk.findModule(TestModuleB.class));
		assertNull(sdk.findModule(TestModuleC.class));
		assertSame(a, mod.provider);

		this.destroy();

		TestUtil.runOnMainSync(() -> Origin.initialize(TestUtil.context(), b));
		sdk = Origin.current();
		mod = sdk.findModule(TestModuleB.class);

		assertNotNull(mod);
		assertNull(sdk.findModule(TestModuleA.class));
		assertNull(sdk.findModule(TestModuleC.class));
		assertSame(b, mod.provider);

		this.destroy();

		TestUtil.runOnMainSync(() -> Origin.initialize(TestUtil.context(), c));
		sdk = Origin.current();
		mod = sdk.findModule(TestModuleC.class);

		assertNotNull(mod);
		assertNotNull(sdk.findModule(TestModuleA.class));
		assertNotNull(sdk.findModule(TestModuleB.class));
		assertSame(c, mod.provider);
		assertSame(sdk.findModule(TestModuleA.class), ((TestModuleC) mod).a);
		assertSame(sdk.findModule(TestModuleB.class), ((TestModuleC) mod).b);

		this.destroy();

		TestUtil.runOnMainSync(() -> Origin.initialize(TestUtil.context(), c, b, a));
		sdk = Origin.current();
		mod = sdk.findModule(TestModuleC.class);
		assertNotNull(mod);
		assertSame(c, mod.provider);
		assertSame(sdk.findModule(TestModuleA.class), ((TestModuleC) mod).a);
		assertSame(sdk.findModule(TestModuleB.class), ((TestModuleC) mod).b);

		mod = sdk.findModule(TestModuleB.class);
		assertNotNull(mod);
		assertSame(b, mod.provider);

		mod = sdk.findModule(TestModuleA.class);
		assertNotNull(mod);
		assertSame(a, mod.provider);

		this.destroy();
	}

	@Test
	public void testLoadModule() throws InterruptedException {
		TestUtil.runOnMainSync(() -> Origin.initialize(TestUtil.context()));

		OriginModule.Provider<TestModuleA> ap = TestModuleA.ofProvider();
		OriginModule.Provider<TestModuleB> bp = TestModuleB.ofProvider();
		OriginModule.Provider<TestModuleC> cp = TestModuleC.ofProvider();

		// loading module C should load A and B, in that order
		Origin sdk = Origin.current();
		TestModuleC c = sdk.loadModule(cp);
		TestModuleA a = sdk.findModule(TestModuleA.class);
		TestModuleB b = sdk.findModule(TestModuleB.class);

		assertNotNull(a);
		assertNotNull(b);
		assertSame(c, sdk.loadModule(TestModuleC.class));
		assertSame(a, sdk.loadModule(TestModuleA.class));
		assertSame(b, sdk.loadModule(TestModuleB.class));
		assertSame(cp, c.provider);
		assertSame(a, c.a);
		assertSame(b, c.b);

		assertSame(a, sdk.loadModule(ap));
		assertSame(ap, a.provider);

		assertSame(b, sdk.loadModule(bp));
		assertSame(bp, b.provider);

		assertSame(c, sdk.loadModule(cp));
		assertSame(cp, c.provider);
		assertSame(a, c.a);
		assertSame(b, c.b);

		this.destroy();

		// C must be destroyed before A and B
		assertTrue(a.destroyedOn != 0L);
		assertTrue(b.destroyedOn != 0L);
		assertTrue(c.destroyedOn != 0L);
		assertTrue(c.destroyedOn < b.destroyedOn && c.destroyedOn < a.destroyedOn);

		// C should reuse A, and load B
		TestUtil.runOnMainSync(() -> Origin.initialize(TestUtil.context()));
		sdk = Origin.current();
		a = sdk.loadModule(TestModuleA.class);
		c = sdk.loadModule(TestModuleC.class);
		b = sdk.loadModule(TestModuleB.class);

		assertSame(a, c.a);
		assertSame(b, c.b);

		this.destroy();

		// C must be destroyed before A and B
		assertTrue(a.destroyedOn != 0L);
		assertTrue(b.destroyedOn != 0L);
		assertTrue(c.destroyedOn != 0L);
		assertTrue(c.destroyedOn < b.destroyedOn && c.destroyedOn < a.destroyedOn);
	}

	private static final class TestEvent {
		final OriginModule source;
		final String name;
		final Object data;
		final long timestamp;
		final OriginModuleEventCallback callback;

		TestEvent(
			OriginModule src,
			String name,
			Object data,
			long when,
			OriginModuleEventCallback cb
		) {
			this.source = src;
			this.name = name;
			this.data = data;
			this.timestamp = when;
			this.callback = cb;
		}

		TestEvent(OriginModule src, String name, Object data, OriginModuleEventCallback cb) {
			this(src, name, data, SystemClock.uptimeMillis(), cb);
		}

		TestEvent withCallback(OriginModuleEventCallback cb) {
			return new TestEvent(this.source, this.name, this.data, this.timestamp, cb);
		}
	}

	private static final class TestEventCallback implements OriginModuleEventCallback {
		private final LinkedTransferQueue<TestEvent> events;

		TestEventCallback(LinkedTransferQueue<TestEvent> events) {
			this.events = events;
		}

		@Override
		public void onOriginModuleEvent(OriginModule src, String name, Object data, long when) {
			events.add(new TestEvent(src, name, data, when, this));
		}
	}

	/**
	 * Ensure events within a queue are present.
	 *
	 * @param got events to test
	 * @param expect expected events
	 * @throws InterruptedException interrupted while waiting for events
	 */
	private static void assertEvent(LinkedTransferQueue<TestEvent> got, TestEvent... expect)
	throws InterruptedException {
		List<TestEvent> worklist = new ArrayList<>(expect.length);

		Collections.addAll(worklist, expect);
		while (!worklist.isEmpty()) {
			TestEvent gotOne = got.poll(10, TimeUnit.SECONDS);
			TestEvent expOne = null;

			if (gotOne == null)
				assertEquals(Collections.emptyList(), worklist);
			assertNotNull(gotOne);
			for (int i = 0; i < worklist.size(); i++, expOne = null) {
				expOne = worklist.get(i);
				if (
					expOne.source == gotOne.source &&
					expOne.name.equals(gotOne.name) &&
					expOne.callback == gotOne.callback
				) {
					worklist.remove(i);
					break;
				}
			}
			assertNotNull(expOne);
			assertEquals(expOne.data, gotOne.data);
			assertTrue(expOne.timestamp <= gotOne.timestamp);
		}
		assertNull(got.poll(50, TimeUnit.MILLISECONDS));
	}

	@Test
	public void testEvents() throws InterruptedException {
		TestUtil.runOnMainSync(() -> Origin.initialize(TestUtil.context()));

		// loading module C should load A and B, in that order
		Origin sdk = Origin.current();
		TestModuleA a = sdk.loadModule(TestModuleA.class);
		TestModuleB b = sdk.loadModule(TestModuleB.class);
		OriginModuleEventBus aa = sdk.registerModuleEvent(a, "a", false);
		OriginModuleEventBus ab = sdk.registerModuleEvent(a, "b", true);
		OriginModuleEventBus ba = sdk.registerModuleEvent(b, "a", false);
		LinkedTransferQueue<TestEvent> events = new LinkedTransferQueue<>();
		OriginModuleEventCallback cb1 = new TestEventCallback(events);
		OriginModuleEventCallback cb2 = new TestEventCallback(events);
		OriginModuleEventCallback cbw = new TestEventCallback(events);
		long start = SystemClock.uptimeMillis();

		sdk.registerModuleEventCallback(cb1, new Pair<>(a, "a"));
		// registering with same callback should fail
		assertThrows(
			IllegalArgumentException.class,
			() -> sdk.registerModuleEventCallback(cb1, new Pair<>(a, "a"))
		);
		// registering an unknown event should fail
		assertThrows(
			IllegalArgumentException.class,
			() -> sdk.registerModuleEventCallback(cb1, new Pair<>(a, "z"))
		);

		for (int i = 0; i < 5; i++) {
			TestEvent exp = new TestEvent(a, "a", String.format("aa.%s", i + 1), cb1);

			aa.submit(exp.data);
			ab.submit("ab.0");
			ba.submit("ba.0");
			assertEvent(events, exp);
		}

		// b is a sticky event, it should fire immediately when we register
		sdk.registerModuleEventCallback(cb2, new Pair<>(a, "b"));
		sdk.registerModuleEventCallback(cbw, null);
		assertEvent(
			events,
			new TestEvent(a, "b", "ab.0", start, cb2),
			new TestEvent(a, "b", "ab.0", start, cbw)
		);

		for (int i = 0; i < 5; i++) {
			TestEvent expAa = new TestEvent(a, "a", String.format("aa.%s", i + 1), cb1);
			TestEvent expAb = new TestEvent(a, "b", String.format("ab.%s", i + 1), cb2);
			TestEvent expBa = new TestEvent(b, "a", String.format("ba.%s", i + 1), cbw);

			aa.submit(expAa.data);
			// hold the invoke lock so we can pool events to make sure sticky events fire once
			ab.invokeLock.lock();
			try {
				ab.submit("ab.0");
				ab.submit(expAb.data);
			} finally {
				ab.invokeLock.unlock();
			}
			ba.submit(expBa.data);

			assertEvent(
				events,
				expAa,
				expAa.withCallback(cbw),
				expAb,
				expAb.withCallback(cbw),
				expBa
			);
		}

		sdk.unregisterModuleEventCallback(cb1, new Pair<>(a, "a"));
		// unregistering an unregistered event should fail
		assertThrows(
			IllegalArgumentException.class,
			() -> sdk.unregisterModuleEventCallback(cb1, new Pair<>(a, "a"))
		);

		for (int i = 0; i < 5; i++) {
			TestEvent expAa = new TestEvent(a, "a", String.format("aa.%s", i + 1), cbw);
			TestEvent expAb = new TestEvent(a, "b", String.format("ab.%s", i + 1), cb2);
			TestEvent expBa = new TestEvent(b, "a", String.format("ba.%s", i + 1), cbw);

			aa.submit(expAa.data);
			// hold the invoke lock so we can pool events to make sure sticky events fire once
			ab.invokeLock.lock();
			try {
				ab.submit("ab.0");
				ab.submit(expAb.data);
			} finally {
				ab.invokeLock.unlock();
			}
			ba.submit(expBa.data);

			assertEvent(
				events,
				expAa,
				expAb,
				expAb.withCallback(cbw),
				expBa
			);
		}

		sdk.unregisterModuleEventCallback(cbw, null);
		// unregistering an unregistered event should fail
		assertThrows(
			IllegalArgumentException.class,
			() -> sdk.unregisterModuleEventCallback(cbw, null)
		);

		for (int i = 0; i < 5; i++) {
			TestEvent expAb = new TestEvent(a, "b", String.format("ab.%s", i + 1), cb2);

			aa.submit(String.format("aa.%s", i));
			// hold the invoke lock so we can pool events to make sure sticky events fire once
			ab.invokeLock.lock();
			try {
				ab.submit("ab.0");
				ab.submit(expAb.data);
			} finally {
				ab.invokeLock.unlock();
			}
			ba.submit(String.format("ba.%s", i));

			assertEvent(events, expAb);
		}

		sdk.unregisterModuleEventCallback(cb2, new Pair<>(a, "b"));

		for (int i = 0; i < 5; i++) {
			aa.submit(String.format("aa.%s", i));
			ab.submit(String.format("ab.%s", i));
			ba.submit(String.format("ba.%s", i));

			assertEvent(events);
		}
	}
}
