// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.polygamma.android.origin.util.Sync.*;

import android.os.Build;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.Exchanger;

/**
 * {@link Sync} tests.
 */
@RunWith(AndroidJUnit4.class)
public class SyncTest {

	@Test
	public void testIsMainThread() throws InterruptedException {
		InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
			assertTrue(isMainThread());
			checkMainThread();
			assertThrows(IllegalStateException.class, Sync::checkWorkerThread);
		});

		Exchanger<Throwable> err = new Exchanger<>();

		(new Thread(() -> {
			try {
				assertFalse(isMainThread());
				assertThrows(IllegalStateException.class, Sync::checkMainThread);
				checkWorkerThread();
				err.exchange(null);
			} catch (Exception e) {
				try {
					err.exchange(e);
				} catch (InterruptedException e2) {
					throw new AssertionError(e2);
				}
			}
		})).start();
		assertNull(err.exchange(null));
	}

	@Test
	public void testCurrentProcessName() {
		assertEquals(
			Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
			InstrumentationRegistry.getInstrumentation().getProcessName() :
			Tests.context().getPackageName(),
			Sync.currentProcessName()
		);

		assertTrue(Sync.currentProcessSimpleName().isEmpty());
	}
}
