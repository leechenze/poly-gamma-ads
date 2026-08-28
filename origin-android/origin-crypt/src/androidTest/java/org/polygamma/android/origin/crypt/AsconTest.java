// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.crypt;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.polygamma.android.origin.util.Bits.loadLongLe;
import static org.polygamma.android.origin.util.Bits.storeLongLe;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.bouncycastle.crypto.digests.AsconHash256;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Random;

/**
 * {@link Ascon} tests.
 */
@RunWith(AndroidJUnit4.class)
public class AsconTest {
	@Test
	public void testOf() {
		final int N_PAD = 8;
		byte[] iv = new byte[N_PAD + Ascon.STATE_SIZE + N_PAD];

		// should fail when `iv` has fewer than `STATE_SIZE` bytes
		assertThrows(IndexOutOfBoundsException.class, () -> Ascon.of(iv, iv.length - 1));

		// should access within bounds
		storeLongLe(iv,  0, 123);
		storeLongLe(iv,  8, 456);
		storeLongLe(iv, 16, 789);
		storeLongLe(iv, 24, 987);
		storeLongLe(iv, 32, 654);
		storeLongLe(iv, 40, 321);
		storeLongLe(iv, 48, 123);

		Ascon ascon = Ascon.of(iv, N_PAD);

		assertEquals(456, ascon.x0);
		assertEquals(789, ascon.x1);
		assertEquals(987, ascon.x2);
		assertEquals(654, ascon.x3);
		assertEquals(321, ascon.x4);

		// should not have modified `iv`
		assertEquals(123, loadLongLe(iv,  0));
		assertEquals(456, loadLongLe(iv,  8));
		assertEquals(789, loadLongLe(iv, 16));
		assertEquals(987, loadLongLe(iv, 24));
		assertEquals(654, loadLongLe(iv, 32));
		assertEquals(321, loadLongLe(iv, 40));
		assertEquals(123, loadLongLe(iv, 48));
	}

	@Test
	public void testSerialize() {
		final int N_PAD = 8;
		byte[] iv = new byte[N_PAD + Ascon.STATE_SIZE + N_PAD];
		Ascon ascon = Ascon.ofHash();

		// should fail when `iv` has insufficient capacity
		assertThrows(IndexOutOfBoundsException.class, () -> ascon.serialize(iv, iv.length - 1));

		storeLongLe(iv, 0, 123);
		storeLongLe(iv, iv.length - N_PAD, 123);
		ascon.serialize(iv, N_PAD);

		// should have began storing from `N_PAD`
		assertEquals(123, loadLongLe(iv, 0));
		assertEquals(0x9b1e5494e934d681L, loadLongLe(iv, N_PAD +  0));
		assertEquals(0x4bc3a01e333751d2L, loadLongLe(iv, N_PAD +  8));
		assertEquals(0xae65396c6b34b81aL, loadLongLe(iv, N_PAD + 16));
		assertEquals(0x3c7fd4a4d56a4db3L, loadLongLe(iv, N_PAD + 24));
		assertEquals(0x1a5c464906c5976dL, loadLongLe(iv, N_PAD + 32));
		assertEquals(123, loadLongLe(iv, iv.length - N_PAD));
		// shouldn't have modified its internal state
		assertEquals(0x9b1e5494e934d681L, ascon.x0);
		assertEquals(0x4bc3a01e333751d2L, ascon.x1);
		assertEquals(0xae65396c6b34b81aL, ascon.x2);
		assertEquals(0x3c7fd4a4d56a4db3L, ascon.x3);
		assertEquals(0x1a5c464906c5976dL, ascon.x4);
	}

	@Test
	public void testReset() {
		final int N_PAD = 8;
		byte[] iv = new byte[N_PAD + Ascon.STATE_SIZE + N_PAD];
		Ascon ascon = Ascon.ofEmpty();

		// should fail when `iv` has fewer than `STATE_SIZE` bytes
		assertThrows(IndexOutOfBoundsException.class, () -> ascon.reset(iv, iv.length - 1));

		// should access within bounds
		storeLongLe(iv,  0, 123);
		storeLongLe(iv,  8, 456);
		storeLongLe(iv, 16, 789);
		storeLongLe(iv, 24, 987);
		storeLongLe(iv, 32, 654);
		storeLongLe(iv, 40, 321);
		storeLongLe(iv, 48, 123);
		ascon.reset(iv, N_PAD);

		assertEquals(456, ascon.x0);
		assertEquals(789, ascon.x1);
		assertEquals(987, ascon.x2);
		assertEquals(654, ascon.x3);
		assertEquals(321, ascon.x4);

		// should not have modified `iv`
		assertEquals(123, loadLongLe(iv,  0));
		assertEquals(456, loadLongLe(iv,  8));
		assertEquals(789, loadLongLe(iv, 16));
		assertEquals(987, loadLongLe(iv, 24));
		assertEquals(654, loadLongLe(iv, 32));
		assertEquals(321, loadLongLe(iv, 40));
		assertEquals(123, loadLongLe(iv, 48));
	}

	@Test
	public void testHash() {
		Random rand = new Random(44);
		AsconHash256 bc = new AsconHash256();
		Ascon og = Ascon.ofHash();

		assertThrows(IndexOutOfBoundsException.class, () -> og.updateHash(new byte[4], 0, 8));
		assertThrows(IndexOutOfBoundsException.class, () -> og.updateHash(new byte[4], 4, 4));
		assertThrows(
			IndexOutOfBoundsException.class,
			() -> og.finishHash(new byte[Ascon.HASH_SIZE], 1)
		);
		for (int i = 1; i < 1000; i++) {
			final int N_PAD = i % 10;
			byte[] exp = new byte[Ascon.HASH_SIZE];
			byte[] buffer = new byte[N_PAD + i + N_PAD + Ascon.HASH_SIZE + N_PAD];
			byte[] pad = new byte[N_PAD];
			int bufferInputOff = N_PAD;
			int bufferHashOff = bufferInputOff + i + N_PAD;

			rand.nextBytes(buffer);
			rand.nextBytes(pad);
			System.arraycopy(pad, 0, buffer, 0, N_PAD);
			System.arraycopy(pad, 0, buffer, bufferInputOff + i, N_PAD);
			System.arraycopy(pad, 0, buffer, bufferHashOff + Ascon.HASH_SIZE, N_PAD);

			bc.reset();
			bc.update(buffer, bufferInputOff, i);
			bc.doFinal(exp, 0);

			// test full input hashing
			og.resetHash();
			og.updateHash(buffer, bufferInputOff, i);
			og.finishHash(buffer, bufferHashOff);
			assertArrayEquals(exp, Arrays.copyOfRange(
				buffer,
				bufferHashOff,
				bufferHashOff + Ascon.HASH_SIZE
			));

			// now partial input hashing
			Arrays.fill(buffer, bufferHashOff, bufferHashOff + Ascon.HASH_SIZE, (byte) 0);
			og.resetHash();
			for (int n = 0; n < i; n++)
				og.updateHash(buffer, bufferInputOff + n, 1);
			og.finishHash(buffer, bufferHashOff);
			assertArrayEquals(exp, Arrays.copyOfRange(
				buffer,
				bufferHashOff,
				bufferHashOff + Ascon.HASH_SIZE
			));

			// make sure padding wasn't touched
			assertArrayEquals(pad, Arrays.copyOfRange(buffer, 0, N_PAD));
			assertArrayEquals(pad, Arrays.copyOfRange(
				buffer,
				bufferInputOff + i,
				bufferInputOff + i + N_PAD
			));
			assertArrayEquals(pad, Arrays.copyOfRange(
				buffer,
				bufferHashOff + Ascon.HASH_SIZE,
				bufferHashOff + Ascon.HASH_SIZE + N_PAD
			));
		}
	}
}
