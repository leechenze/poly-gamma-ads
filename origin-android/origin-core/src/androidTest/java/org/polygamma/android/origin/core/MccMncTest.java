// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.core;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * {@link MccMnc} tests.
 */
@RunWith(AndroidJUnit4.class)
public class MccMncTest {
	@Test
	public void testParse() {
		assertArrayEquals(new int[] { -1, -1 }, MccMnc.parse(""));
		assertArrayEquals(new int[] { 123, -1 }, MccMnc.parse("123"));
		assertArrayEquals(new int[] { 123, -1 }, MccMnc.parse("123-"));
		assertArrayEquals(new int[] { 2, -1 }, MccMnc.parse("002"));
		assertArrayEquals(new int[] { 123, 456 }, MccMnc.parse("123456"));
		assertArrayEquals(new int[] { 123, 456 }, MccMnc.parse("123-456"));
	}

	@Test
	public void testSerialize() {
		assertEquals("", MccMnc.serialize(-1, -1));
		assertEquals("123", MccMnc.serialize(123, -1));
		assertEquals("123001", MccMnc.serialize(123, 1));
		assertEquals("123456", MccMnc.serialize(123, 456));
	}
}
