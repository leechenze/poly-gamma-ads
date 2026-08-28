// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.polygamma.android.origin.util.Preconditions.*;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * {@link Preconditions} tests.
 */
@SuppressWarnings("ObviousNullCheck")
@RunWith(AndroidJUnit4.class)
public class PreconditionsTest {

	@Test
	public void testCheckArgument() {
		assertThrows(IllegalArgumentException.class, () -> checkArgument(false));
		assertThrows(
			"message",
			IllegalArgumentException.class,
			() -> checkArgument(false, "message")
		);
		assertThrows(
			"message one",
			IllegalArgumentException.class,
			() -> checkArgument(false, "message %s", "one")
		);
		checkArgument(true);
	}

	@Test
	public void testCheckState() {
		assertThrows(IllegalStateException.class, () -> checkState(false));
		assertThrows(
			"message",
			IllegalStateException.class,
			() -> checkState(false, "message")
		);
		assertThrows(
			"message one",
			IllegalStateException.class,
			() -> checkState(false, "message %s", "one")
		);
		checkState(true);
	}

	@Test
	public void testCheckNotNull() {
		assertThrows(NullPointerException.class, () -> checkNotNull(null));
		assertThrows(
			"message",
			NullPointerException.class,
			() -> checkNotNull(null, "message")
		);
		assertThrows(
			"message one",
			NullPointerException.class,
			() -> checkNotNull(null, "message %s", "one")
		);
		assertEquals("test", checkNotNull("test"));
		assertEquals("test", checkNotNull("test", "message"));
		assertEquals("test", checkNotNull("test", "message %s", "one"));
	}

	@Test
	public void testCheckNotNullElse() {
		assertThrows(NullPointerException.class, () -> checkNotNullElse(null, null));
		assertEquals("a", checkNotNullElse("a", "b"));
		assertEquals("b", checkNotNullElse(null, "b"));
	}

	@Test
	public void testCheckNotNullElseGet() {
		assertThrows(NullPointerException.class, () -> checkNotNullElseGet(null, () -> null));
		assertEquals("a", checkNotNullElseGet("a", () -> "b"));
		assertEquals("b", checkNotNullElseGet(null, () -> "b"));
	}

	@Test
	public void testCheckIndex() {
		assertThrows(IndexOutOfBoundsException.class, () -> checkIndex(-1, 0));
		assertThrows(IndexOutOfBoundsException.class, () -> checkIndex(-1, 1));
		assertThrows(IndexOutOfBoundsException.class, () -> checkIndex(0, 0));
		assertThrows(IndexOutOfBoundsException.class, () -> checkIndex(1, 1));

		assertEquals(0, checkIndex(0, 1));
		assertEquals(1, checkIndex(1, 2));
	}

	@Test
	public void testCheckFromIndexSize() {
		assertThrows(IndexOutOfBoundsException.class, () -> checkFromIndexSize(-1, 0, 0));
		assertThrows(IndexOutOfBoundsException.class, () -> checkFromIndexSize(-1, 1, 1));
		assertThrows(IndexOutOfBoundsException.class, () -> checkFromIndexSize(0, 0, -1));
		assertThrows(IndexOutOfBoundsException.class, () -> checkFromIndexSize(0, -1, 0));
		assertThrows(IndexOutOfBoundsException.class, () -> checkFromIndexSize(1, 1, 0));

		assertEquals(0, checkFromIndexSize(0, 0, 0));
		assertEquals(0, checkFromIndexSize(0, 1, 2));
		assertEquals(1, checkFromIndexSize(1, 2, 4));
	}

	@Test
	public void testCheckFromToIndex() {
		assertThrows(IndexOutOfBoundsException.class, () -> checkFromToIndex(-1, 0, 0));
		assertThrows(IndexOutOfBoundsException.class, () -> checkFromToIndex(-1, 1, 1));
		assertThrows(IndexOutOfBoundsException.class, () -> checkFromToIndex(0, 0, -1));
		assertThrows(IndexOutOfBoundsException.class, () -> checkFromToIndex(0, -1, 0));
		assertThrows(IndexOutOfBoundsException.class, () -> checkFromToIndex(1, 1, 0));

		assertEquals(0, checkFromToIndex(0, 0, 0));
		assertEquals(0, checkFromToIndex(0, 1, 2));
		assertEquals(1, checkFromToIndex(1, 2, 4));
	}
}
