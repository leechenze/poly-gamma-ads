// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.polygamma.android.origin.util.Strings.*;

import androidx.test.espresso.util.Iterators;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * {@link Strings} tests.
 */
@RunWith(AndroidJUnit4.class)
public class StringsTest {

	@Test
	public void testNullToEmpty() {
		assertEquals("", nullToEmpty(null));
		assertEquals("", nullToEmpty(""));
		assertEquals("foo", nullToEmpty("foo"));
	}

	@Test
	public void testEmptyToNull() {
		assertNull(emptyToNull(""));
		assertNull(emptyToNull(null));
		assertEquals("foo", emptyToNull("foo"));
	}

	@Test
	public void testSplit() {
		assertFalse(split("", ',').hasNext());
		assertArrayEquals(
			new String[] { "foo" },
			Iterators.toArray(split("foo", ','), String.class)
		);
		assertArrayEquals(
			new String[] { "foo", "bar" },
			Iterators.toArray(split("foo,bar", ','), String.class)
		);
		assertArrayEquals(
			new String[] { "foo ", " bar", " baz" },
			Iterators.toArray(split("foo , bar, baz", ','), String.class)
		);
		assertArrayEquals(
			new String[] { " foo", "bar", "baz " },
			Iterators.toArray(split(" foo,bar,baz ", ','), String.class)
		);
	}
}
