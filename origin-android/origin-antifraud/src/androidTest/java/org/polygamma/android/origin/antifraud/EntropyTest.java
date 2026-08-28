// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.antifraud;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.nio.ByteBuffer;

/**
 * {@link Entropy} tests.
 */
@RunWith(AndroidJUnit4.class)
public class EntropyTest {

	@Test
	public void testOf() {
		ByteBuffer dec = ByteBuffer.allocate(1024);

		while (dec.hasRemaining())
			dec.putLong(Double.doubleToLongBits(Math.random()));

		dec.flip();

		ByteBuffer enc = Entropy.of(dec);

		assertEquals(1024 + 8, enc.remaining());
		assertNotEquals(dec, enc);

		// empty input has no tangible entropy
		enc = Entropy.of(ByteBuffer.allocate(0));
		assertFalse(enc.hasRemaining());

		// entropy is always word aligned
		enc = Entropy.of(ByteBuffer.allocate(1));
		assertEquals(16, enc.remaining());
	}

	@Test
	public void testOfValue() {
		assertFalse(Entropy.ofValue(null).hasRemaining());
		assertFalse(Entropy.ofValue("").hasRemaining());
		assertTrue(Entropy.ofValue("test").hasRemaining());
	}
}
