// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.graphics.PointF;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * {@link CountryPolygons} tests.
 */
@RunWith(AndroidJUnit4.class)
public class CountryPolygonsTest {
	@Test
	public void testIsInsideChina() {
		assertTrue(CountryPolygons.isInside(
			CountryPolygons.CHINA,
			new PointF(97.16487f, 36.19201f)
		));
		assertTrue(CountryPolygons.isInside(
			CountryPolygons.CHINA,
			new PointF(87.30288f, 48.07910f)
		));
		assertFalse(CountryPolygons.isInside(
			CountryPolygons.CHINA,
			new PointF(-98.36215f, 41.67098f)
		));
		assertFalse(CountryPolygons.isInside(
			CountryPolygons.CHINA,
			new PointF(126.91241f, 39.25486f)
		));
	}
}
