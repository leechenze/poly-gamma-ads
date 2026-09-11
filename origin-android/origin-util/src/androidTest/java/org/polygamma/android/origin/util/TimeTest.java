// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.os.SystemClock;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

/**
 * {@link Time} tests.
 */
@RunWith(AndroidJUnit4.class)
public class TimeTest {
	@Test
	public void testNowUtc() {
		long exp = System.currentTimeMillis();
		long got = Time.nowUtcMillis();
		long delta = Math.abs(got - exp);

		assertTrue(delta >= 0 && delta <= 15);

		delta = Math.abs(TimeUnit.MILLISECONDS.toSeconds(got) - Time.nowUtcSeconds());
		assertTrue(delta >= 0 && delta <= 1);
	}

	@Test
	public void testNowUptime() {
		long got = Time.nowUptimeSeconds();
		long exp = TimeUnit.MILLISECONDS.toSeconds(SystemClock.uptimeMillis());
		long delta = got - exp;

		assertTrue(delta >= 0 && delta <= 1);
	}

	@Test
	public void testDurationBetween() {
		assertEquals(0, Time.durationBetween(11, 10));
		assertEquals(0, Time.durationBetween(10, 10));
		assertEquals(1, Time.durationBetween(10, 11));
	}
}
