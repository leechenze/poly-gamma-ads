// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.crypt;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import android.util.Pair;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.bouncycastle.crypto.engines.XTEAEngine;
import org.bouncycastle.crypto.params.KeyParameter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.polygamma.android.origin.util.Bits;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Random;

/**
 * {@link Xtea} tests.
 */
@RunWith(AndroidJUnit4.class)
public class XteaTest {
	@Test
	public void testOfKeyAndSetKey() {
		final int K0 = 0xdeadbeef;
		final int K1 = 0xcafebabe;
		final int K2 = 0xdeadcafe;
		final int K3 = 0xcafebeef;

		byte[] key = ByteBuffer.allocate(Xtea.KEY_SIZE)
			.order(ByteOrder.LITTLE_ENDIAN)
			.putInt(K0)
			.putInt(K1)
			.putInt(K2)
			.putInt(K3)
			.array();
		byte[] pad = { 1, 2, 3, 4 };
		byte[] buffer = new byte[pad.length + key.length + pad.length];

		System.arraycopy(pad, 0, buffer, 0, pad.length);
		System.arraycopy(key, 0, buffer, pad.length, key.length);
		System.arraycopy(pad, 0, buffer, buffer.length - pad.length, pad.length);

		assertThrows(IndexOutOfBoundsException.class, () -> Xtea.ofKey(buffer, buffer.length - 1));
		assertThrows(
			IndexOutOfBoundsException.class,
			() -> Xtea.ofEmpty().setKey(buffer, buffer.length - 1)
		);

		Xtea g1 = Xtea.ofKey(buffer, pad.length);
		Xtea g2 = Xtea.ofEmpty().setKey(buffer, pad.length);

		assertEquals(K0, g1.key[0]);
		assertEquals(K1, g1.key[1]);
		assertEquals(K2, g1.key[2]);
		assertEquals(K3, g1.key[3]);
		assertArrayEquals(g1.key, g2.key);

		assertArrayEquals(pad, Arrays.copyOfRange(buffer, 0, pad.length));
		assertArrayEquals(key, Arrays.copyOfRange(buffer, pad.length, pad.length + key.length));
		assertArrayEquals(pad, Arrays.copyOfRange(buffer, pad.length + key.length, buffer.length));
	}

	private static Pair<XTEAEngine, Xtea> newCipherPair(Random rand) {
		byte[] key = new byte[Xtea.KEY_SIZE];
		XTEAEngine bc = new XTEAEngine();
		Xtea og = Xtea.ofEmpty();

		rand.nextBytes(key);
		og.setKey(key, 0);
		for (int i = 0; i < key.length; i += 4)
			Bits.storeIntBe(key, i, Bits.loadIntLe(key, i));
		bc.init(true, new KeyParameter(key));
		return new Pair<>(bc, og);
	}

	@Test
	public void testEncipher() {
		Random rand = new Random(44);
		Pair<XTEAEngine, Xtea> ciphers = newCipherPair(rand);
		XTEAEngine bc = ciphers.first;
		Xtea og = ciphers.second;

		assertThrows(IllegalArgumentException.class, () -> og.encipher(
			new byte[Xtea.BLOCK_SIZE], 0,
			new byte[Xtea.BLOCK_SIZE], 0,
			Xtea.BLOCK_SIZE - 1
		));
		assertThrows(IndexOutOfBoundsException.class, () -> og.encipher(
			new byte[Xtea.BLOCK_SIZE], 1,
			new byte[Xtea.BLOCK_SIZE], 0,
			Xtea.BLOCK_SIZE
		));
		assertThrows(IndexOutOfBoundsException.class, () -> og.encipher(
			new byte[Xtea.BLOCK_SIZE], 0,
			new byte[Xtea.BLOCK_SIZE], 1,
			Xtea.BLOCK_SIZE
		));
		for (int i = 0; i < 1000; i++) {
			final int N = UnsignedMath.divideCeil(i + 1, Xtea.BLOCK_SIZE) * Xtea.BLOCK_SIZE;
			final int N_PAD = 8;
			byte[] exp = new byte[N];
			byte[] pad = new byte[N_PAD];
			byte[] buffer = new byte[N_PAD + N + N_PAD + N + N_PAD];
			int bufferPtOff = N_PAD;
			int bufferCtOff = bufferPtOff + N + N_PAD;

			rand.nextBytes(pad);
			rand.nextBytes(exp);
			System.arraycopy(pad, 0, buffer, 0, N_PAD);
			System.arraycopy(pad, 0, buffer, bufferPtOff + N, N_PAD);
			System.arraycopy(pad, 0, buffer, bufferCtOff + N, N_PAD);
			System.arraycopy(exp, 0, buffer, bufferPtOff, N);

			og.encipher(buffer, bufferCtOff, buffer, bufferPtOff, N);

			assertArrayEquals(pad, Arrays.copyOfRange(buffer, 0, N_PAD));
			assertArrayEquals(exp, Arrays.copyOfRange(buffer, bufferPtOff, bufferPtOff + N));
			assertArrayEquals(
				pad,
				Arrays.copyOfRange(buffer, bufferPtOff + N, bufferPtOff + N + N_PAD)
			);
			assertArrayEquals(
				pad,
				Arrays.copyOfRange(buffer, bufferCtOff + N, bufferCtOff + N + N_PAD)
			);

			for (int j = 0; j < N; j += 4)
				Bits.storeIntBe(exp, j, Bits.loadIntLe(exp, j));
			for (int j = 0; j < N;)
				j += bc.processBlock(exp, j, exp, j);
			for (int j = 0; j < N; j += 4)
				Bits.storeIntLe(exp, j, Bits.loadIntBe(exp, j));
			assertArrayEquals(exp, Arrays.copyOfRange(buffer, bufferCtOff, bufferCtOff + N));
		}
	}

	@Test
	public void testDecipher() {
		Random rand = new Random(44);
		Pair<XTEAEngine, Xtea> ciphers = newCipherPair(rand);
		XTEAEngine bc = ciphers.first;
		Xtea og = ciphers.second;

		assertThrows(IllegalArgumentException.class, () -> og.decipher(
			new byte[Xtea.BLOCK_SIZE], 0,
			new byte[Xtea.BLOCK_SIZE], 0,
			Xtea.BLOCK_SIZE - 1
		));
		assertThrows(IndexOutOfBoundsException.class, () -> og.decipher(
			new byte[Xtea.BLOCK_SIZE], 1,
			new byte[Xtea.BLOCK_SIZE], 0,
			Xtea.BLOCK_SIZE
		));
		assertThrows(IndexOutOfBoundsException.class, () -> og.decipher(
			new byte[Xtea.BLOCK_SIZE], 0,
			new byte[Xtea.BLOCK_SIZE], 1,
			Xtea.BLOCK_SIZE
		));
		for (int i = 0; i < 1000; i++) {
			final int N = UnsignedMath.divideCeil(i + 1, Xtea.BLOCK_SIZE) * Xtea.BLOCK_SIZE;
			final int N_PAD = 8;
			byte[] ct = new byte[N];
			byte[] pt = new byte[N];
			byte[] pad = new byte[N_PAD];
			byte[] buffer = new byte[N_PAD + N + N_PAD + N + N_PAD];
			int bufferPtOff = N_PAD;
			int bufferCtOff = bufferPtOff + N + N_PAD;

			rand.nextBytes(pad);
			rand.nextBytes(pt);
			System.arraycopy(pad, 0, buffer, 0, N_PAD);
			System.arraycopy(pad, 0, buffer, bufferPtOff + N, N_PAD);
			System.arraycopy(pad, 0, buffer, bufferCtOff + N, N_PAD);

			for (int j = 0; j < N;)
				j += bc.processBlock(pt, j, ct, j);
			for (int j = 0; j < N; j += 4) {
				Bits.storeIntLe(ct, j, Bits.loadIntBe(ct, j));
				Bits.storeIntLe(pt, j, Bits.loadIntBe(pt, j));
			}
			System.arraycopy(ct, 0, buffer, bufferCtOff, N);
			og.decipher(buffer, bufferPtOff, buffer, bufferCtOff, N);

			assertArrayEquals(pad, Arrays.copyOfRange(buffer, 0, N_PAD));
			assertArrayEquals(pt, Arrays.copyOfRange(buffer, bufferPtOff, bufferPtOff + N));
			assertArrayEquals(
				pad,
				Arrays.copyOfRange(buffer, bufferPtOff + N, bufferPtOff + N + N_PAD)
			);
			assertArrayEquals(ct, Arrays.copyOfRange(buffer, bufferCtOff, bufferCtOff + N));
			assertArrayEquals(
				pad,
				Arrays.copyOfRange(buffer, bufferCtOff + N, bufferCtOff + N + N_PAD)
			);
		}
	}
}
