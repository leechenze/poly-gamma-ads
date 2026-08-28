// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.crypt;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.polygamma.android.origin.util.Bits.loadIntLe;
import static org.polygamma.android.origin.util.Bits.loadLongLe;

import android.util.Pair;

import androidx.annotation.Nullable;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Random;

/**
 * {@link ChaCha20} tests.
 */
@RunWith(AndroidJUnit4.class)
public class ChaCha20Test {

	@Test
	public void testOfKeyAndSetKey() {
		final int N_PAD = 8;
		byte[] key = new byte[N_PAD + ChaCha20.KEY_SIZE + N_PAD];

		(new Random(44)).nextBytes(key);

		// should fail when key is too small
		assertThrows(IndexOutOfBoundsException.class, () -> ChaCha20.ofKey(key, key.length - 1));
		assertThrows(
			IndexOutOfBoundsException.class,
			() -> ChaCha20.ofEmpty().setKey(key, key.length - 1)
		);

		// should load from correct offset
		byte[] origKey = key.clone();
		ChaCha20 cipher = ChaCha20.ofKey(key, N_PAD);

		assertEquals(loadLongLe(key, N_PAD +  0), cipher.key0);
		assertEquals(loadLongLe(key, N_PAD +  8), cipher.key64);
		assertEquals(loadLongLe(key, N_PAD + 16), cipher.key128);
		assertEquals(loadLongLe(key, N_PAD + 24), cipher.key192);
		assertArrayEquals(origKey, key);

		cipher.key0 = cipher.key64 = cipher.key128 = cipher.key192 = 0;
		cipher.keystreamRemaining = ChaCha20.KEYSTREAM_SIZE;

		cipher.setKey(key, N_PAD);
		assertEquals(loadLongLe(key, N_PAD +  0), cipher.key0);
		assertEquals(loadLongLe(key, N_PAD +  8), cipher.key64);
		assertEquals(loadLongLe(key, N_PAD + 16), cipher.key128);
		assertEquals(loadLongLe(key, N_PAD + 24), cipher.key192);
		assertEquals(0, cipher.keystreamRemaining);
		assertArrayEquals(origKey, key);
	}

	@Test
	public void testSetCounter() {
		ChaCha20 cipher = ChaCha20.ofEmpty();

		cipher.keystreamRemaining = ChaCha20.KEYSTREAM_SIZE;
		cipher.setCounter(32);
		assertEquals(32, cipher.counter);
		assertEquals(0, cipher.keystreamRemaining);
	}

	@Test
	public void testSetNonceAndClearNonce() {
		final int N_PAD = 8;
		ChaCha20 cipher = ChaCha20.ofEmpty();
		byte[] nonce = new byte[N_PAD + ChaCha20.NONCE_SIZE + N_PAD];

		(new Random(44)).nextBytes(nonce);

		// should fail when key is too small
		assertThrows(
			IndexOutOfBoundsException.class,
			() -> cipher.setNonce(nonce, nonce.length - 1)
		);

		// should load from correct offset
		byte[] origNonce = nonce.clone();

		cipher.keystreamRemaining = ChaCha20.KEYSTREAM_SIZE;
		cipher.setNonce(nonce, N_PAD);
		assertEquals(0, cipher.keystreamRemaining);
		assertEquals(loadIntLe(nonce, N_PAD + 0), cipher.nonce0);
		assertEquals(loadIntLe(nonce, N_PAD + 4), cipher.nonce32);
		assertEquals(loadIntLe(nonce, N_PAD + 8), cipher.nonce64);
		assertArrayEquals(origNonce, nonce);

		cipher.keystreamRemaining = ChaCha20.KEYSTREAM_SIZE;
		cipher.clearNonce();
		assertEquals(0, cipher.keystreamRemaining);
		assertEquals(0, cipher.nonce0);
		assertEquals(0, cipher.nonce32);
		assertEquals(0, cipher.nonce64);
	}

	private static Pair<TestChaCha20Engine, ChaCha20> newEnginePair(@Nullable Random rand) {
		if (rand == null)
			rand = new Random(44);

		byte[] key = new byte[ChaCha20.KEY_SIZE];
		byte[] nonce = new byte[ChaCha20.NONCE_SIZE];
		int counter = rand.nextInt() % 32768;
		TestChaCha20Engine bc = new TestChaCha20Engine(counter);

		bc.init(true, new ParametersWithIV(new KeyParameter(key), nonce));
		return new Pair<>(bc, ChaCha20.ofKey(key, 0).setCounter(counter).setNonce(nonce, 0));
	}

	@Test
	public void testApplyBlock() {
		Pair<TestChaCha20Engine, ChaCha20> engines = newEnginePair(null);
		TestChaCha20Engine bc = engines.first;
		ChaCha20 og = engines.second;
		int counter = og.counter;

		for (int i = 0; i < 1000; i++) {
			bc.counter = counter + i;
			bc.resetCounter();
			og.applyBlock();
			assertEquals(ChaCha20.KEYSTREAM_SIZE, og.keystreamRemaining);
			assertArrayEquals(bc.generateKeystream(), og.keystream);
		}
		assertEquals(bc.counter, og.counter - 1);
	}

	@Test
	public void testXor() {
		final int N = 512;
		Random rand = new Random(44);

		for (int i = 0; i < 1000; i++) {
			final int N_PAD = i % 10;
			byte[] pad = new byte[N_PAD];
			byte[] buffer = new byte[N_PAD + N + N_PAD + N + N_PAD];
			int bufferPtOff = N_PAD;
			int bufferCtOff = bufferPtOff + N + N_PAD;

			rand.nextBytes(pad);
			System.arraycopy(pad, 0, buffer, 0, N_PAD);
			System.arraycopy(pad, 0, buffer, bufferPtOff + N, N_PAD);
			System.arraycopy(pad, 0, buffer, bufferCtOff + N, N_PAD);

			Pair<TestChaCha20Engine, ChaCha20> engines = newEnginePair(rand);
			TestChaCha20Engine bc = engines.first;
			ChaCha20 og = engines.second;
			byte[] exp = new byte[N];

			bc.processBytes(buffer, bufferPtOff, N, exp, 0);

			// make sure full transform is valid
			og.xor(buffer, bufferCtOff, buffer, bufferPtOff, N);
			assertArrayEquals(exp, Arrays.copyOfRange(buffer, bufferCtOff, bufferCtOff + N));

			// make sure chunked transform is valid
			og.setCounter(bc.counter);
			Arrays.fill(buffer, bufferCtOff, bufferCtOff + N, (byte) 0);
			for (int j = 0; j < N; j += 10)
				og.xor(buffer, bufferCtOff + j, buffer, bufferPtOff + j, Math.min(10, N - j));
			assertArrayEquals(exp, Arrays.copyOfRange(buffer, bufferCtOff, bufferCtOff + N));

			// make sure state is well formed still
			assertEquals(bc.counter + (N / 64), og.counter);

			// make sure transform was within bounds
			assertArrayEquals(pad, Arrays.copyOfRange(buffer, 0, N_PAD));
			assertArrayEquals(
				pad,
				Arrays.copyOfRange(buffer, bufferPtOff + N, bufferPtOff + N + N_PAD)
			);
			assertArrayEquals(
				pad,
				Arrays.copyOfRange(buffer, bufferCtOff + N, bufferCtOff + N + N_PAD)
			);
		}
	}
}
