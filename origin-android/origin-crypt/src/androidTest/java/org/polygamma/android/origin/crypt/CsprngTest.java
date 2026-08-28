// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.crypt;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.util.Pair;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.bouncycastle.crypto.digests.AsconHash256;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.polygamma.android.origin.util.Bits;
import org.polygamma.android.origin.util.Preconditions;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Random;

/**
 * {@link Csprng} tests.
 */
@RunWith(AndroidJUnit4.class)
public class CsprngTest {
	private static final class TestCsprng {
		final byte[] key;
		final TestChaCha20Engine extractor;
		final AsconHash256 compressor;
		final ByteBuffer keystream;

		TestCsprng() {
			this.key = new byte[ChaCha20.KEY_SIZE];
			this.extractor = new TestChaCha20Engine(0);
			this.compressor = new AsconHash256();
			this.keystream = ByteBuffer.allocate(ChaCha20.KEYSTREAM_SIZE);
			this.keystream.position(ChaCha20.KEYSTREAM_SIZE);
			this.extractor.init(true, new ParametersWithIV(
				new KeyParameter(this.key),
				new byte[ChaCha20.NONCE_SIZE]
			));
		}

		void mixEntropy(byte[] ent, int off, int len) {
			this.compressor.update(ent, off, len);
		}

		void forwardExtractor() {
			this.keystream.position(0);
			this.keystream.put(this.extractor.generateKeystream())
				.flip();
			this.keystream.get(this.key);

			this.extractor.counter++;
			this.extractor.reset();
			this.extractor.init(true, new ParametersWithIV(
				new KeyParameter(this.key),
				new byte[ChaCha20.NONCE_SIZE]
			));
		}

		void reseed() {
			this.nextBytes(this.key, 0, ChaCha20.KEY_SIZE);
			this.compressor.update(this.key, 0, ChaCha20.KEY_SIZE);
			this.compressor.doFinal(this.key, 0);
			this.compressor.reset();
			this.compressor.update(this.key, 0, ChaCha20.KEY_SIZE);

			this.extractor.counter = 0;
			this.extractor.reset();
			this.extractor.init(true, new ParametersWithIV(
				new KeyParameter(this.key),
				new byte[ChaCha20.NONCE_SIZE]
			));
			this.forwardExtractor();
		}

		void nextBytes(byte[] dst, int off, int len) {
			while (len > 0) {
				if (!this.keystream.hasRemaining())
					this.forwardExtractor();

				int lim = Math.min(len, this.keystream.remaining());

				this.keystream.get(dst, off, lim);
				off += lim;
				len -= lim;
			}
		}

		long nextWord(int len) {
			Preconditions.checkArgument(len >= 1 && len <= 8);

			if (this.keystream.remaining() < len)
				this.forwardExtractor();

			long w = 0;

			for (int i = 0; i < len; i++)
				w |= (this.keystream.get() & 0xffL) << (i * 8);
			return w;
		}
	}

	private static void assertCsprngEquals(TestCsprng exp, Csprng got) {
		assertEquals(Bits.loadLongLe(exp.key,  0), got.extractor.key0);
		assertEquals(Bits.loadLongLe(exp.key,  8), got.extractor.key64);
		assertEquals(Bits.loadLongLe(exp.key, 16), got.extractor.key128);
		assertEquals(Bits.loadLongLe(exp.key, 24), got.extractor.key192);
		assertEquals(exp.keystream.remaining(), got.extractor.keystreamRemaining);
		assertEquals(exp.extractor.counter, got.extractor.counter);
	}

	private static Pair<TestCsprng, Csprng> newCsprngPair(byte[] seed) {
		TestCsprng bc = new TestCsprng();

		bc.mixEntropy(seed, 0, seed.length);
		bc.reseed();
		return new Pair<>(bc, Csprng.ofSeed(seed, 0, seed.length));
	}

	private static Pair<TestCsprng, Csprng> newCsprngPair(Random rand) {
		byte[] seed = new byte[Csprng.INPUT_ENTROPY_SIZE];

		rand.nextBytes(seed);
		return newCsprngPair(seed);
	}

	@Test
	public void testOfSeed() {
		final int N_PAD = 8;

		// should fail when bounds fall outside of seed
		assertThrows(IndexOutOfBoundsException.class, () -> Csprng.ofSeed(new byte[4], 1, 4));

		Random rand = new Random(44);

		for (int i = 0; i < (Csprng.INPUT_ENTROPY_SIZE * 2); i++) {
			byte[] seed = new byte[i];
			byte[] pad = new byte[N_PAD];
			byte[] buffer = new byte[N_PAD + i + N_PAD];

			rand.nextBytes(seed);
			rand.nextBytes(pad);
			System.arraycopy(pad, 0, buffer, 0, N_PAD);
			System.arraycopy(seed, 0, buffer, N_PAD, i);
			System.arraycopy(pad, 0, buffer, N_PAD + i, N_PAD);

			assertCsprngEquals(newCsprngPair(seed).first, Csprng.ofSeed(buffer, N_PAD, i));
			assertArrayEquals(pad, Arrays.copyOfRange(buffer, 0, N_PAD));
			assertArrayEquals(seed, Arrays.copyOfRange(buffer, N_PAD, N_PAD + i));
			assertArrayEquals(pad, Arrays.copyOfRange(buffer, N_PAD + i, N_PAD + i + N_PAD));
		}
	}

	@Test
	public void testMixEntropyAndReseed() {
		final int N_PAD = 8;
		Random rand = new Random(44);
		Pair<TestCsprng, Csprng> csprngs = newCsprngPair(rand);
		TestCsprng bc = csprngs.first;
		Csprng og = csprngs.second;

		assertThrows(IndexOutOfBoundsException.class, () -> og.mixEntropy(new byte[4], 1, 4));
		for (int i = 0; i < (Csprng.INPUT_ENTROPY_SIZE * 2); i++) {
			byte[] seed = new byte[i];
			byte[] pad = new byte[N_PAD];
			byte[] buffer = new byte[N_PAD + i + N_PAD];

			rand.nextBytes(seed);
			rand.nextBytes(pad);
			System.arraycopy(pad, 0, buffer, 0, N_PAD);
			System.arraycopy(seed, 0, buffer, N_PAD, i);
			System.arraycopy(pad, 0, buffer, N_PAD + i, N_PAD);

			bc.mixEntropy(seed, 0, seed.length);
			og.mixEntropy(buffer, N_PAD, i);

			assertCsprngEquals(bc, og);
			assertArrayEquals(pad, Arrays.copyOfRange(buffer, 0, N_PAD));
			assertArrayEquals(seed, Arrays.copyOfRange(buffer, N_PAD, N_PAD + i));
			assertArrayEquals(pad, Arrays.copyOfRange(buffer, N_PAD + i, N_PAD + i + N_PAD));
		}

		bc.reseed();
		og.reseed();
		assertCsprngEquals(bc, og);
	}

	@Test
	public void testNext() {
		final int N_PAD = 8;
		Random rand = new Random(44);
		Pair<TestCsprng, Csprng> csprngs = newCsprngPair(rand);
		TestCsprng bc = csprngs.first;
		Csprng og = csprngs.second;
		byte[] word = new byte[8];

		assertThrows(IndexOutOfBoundsException.class, () -> og.nextBytes(new byte[4], 1, 4));
		for (int i = 0; i < 1000; i++) {
			byte[] exp = new byte[i];
			byte[] pad = new byte[N_PAD];
			byte[] buffer = new byte[N_PAD + i + N_PAD];

			rand.nextBytes(pad);
			System.arraycopy(pad, 0, buffer, 0, N_PAD);
			System.arraycopy(pad, 0, buffer, N_PAD + i, N_PAD);

			bc.nextBytes(exp, 0, exp.length);
			og.nextBytes(buffer, N_PAD, exp.length);

			assertArrayEquals(exp, Arrays.copyOfRange(buffer, N_PAD, N_PAD + i));
			assertArrayEquals(pad, Arrays.copyOfRange(buffer, 0, N_PAD));
			assertArrayEquals(pad, Arrays.copyOfRange(buffer, N_PAD + i, N_PAD + i + N_PAD));

			assertEquals(bc.nextWord(8), og.nextLong());
			assertEquals((int) (bc.nextWord(4) & 0xffffffffL), og.nextInt());
			assertEquals((byte) (bc.nextWord(1) & 0xffL), og.nextByte());
			assertEquals((bc.nextWord(1) & 1) != 0, og.nextBoolean());

			assertCsprngEquals(bc, og);
		}
	}


	private double chiSquareThresholdOf(int df, double alpha) {
		double p = 1.0 - alpha;

		Preconditions.checkArgument(p > 0 && p < 1);

		double t = Math.sqrt(-2.0 * Math.log(p < 0.5 ? p : 1.0 - p));
		double n = 2.515517 + 0.802853 * t + 0.010328 * t * t;
		double d = 1.0 + 1.432788 * t + 0.189269 * t * t + 0.001308 * t * t;
		double z = t - (n / d);

		if (p < 0.5)
			z = -z;

		double f = 2.0 / (9.0 * df);
		double b = 1.0 - f + z * Math.sqrt(f);

		return df * Math.pow(b, 3);
	}

	private void testChiSquare(double[] samples, int[] bins, double alpha) {
		double samplesPerBin = ((double) samples.length) / bins.length;
		double sq = 0.0;

		for (int n : bins)
			sq += Math.pow(n - samplesPerBin, 2) / samplesPerBin;
		assertTrue(sq <= chiSquareThresholdOf(bins.length - 1, alpha));
	}

	private void testSerialCorrelation(double[] samples) {
		int n = samples.length - 1;
		double x = 0;
		double y = 0;

		for (int i = 0; i < n; i++) {
			x += samples[i + 0];
			y += samples[i + 1];
		}

		double muX = x / n;
		double muY = y / n;
		double nu = 0;
		double dX = 0;
		double dY = 0;

		for (int i = 0; i < n; i++) {
			double ix = samples[i + 0] - muX;
			double iy = samples[i + 1] - muY;

			nu += ix * iy;
			dX += ix * ix;
			dY += iy * iy;
		}

		double r = nu / Math.sqrt(dX * dY);
		double t = (1 / Math.sqrt(samples.length)) * 2;

		assertTrue(Math.abs(r) <= t);
	}

	@Test
	public void testNextDouble() {
		final int N = 1000000;
		Csprng csprng = newCsprngPair(new Random(44)).second;
		double[] samples = new double[N];
		int[] bins = new int[100];

		for (int i = 0; i < N; i++) {
			double s = csprng.nextDouble();

			assertTrue(s >= 0. && s <= 1.);
			samples[i] = s;
			bins[Math.min((int) (s * bins.length), bins.length - 1)]++;
		}
		testChiSquare(samples, bins, 0.05);
		testSerialCorrelation(samples);
	}

	@Test
	public void testNextGaussianPair() {
		final int N = 1000000;
		final double MU = 15;
		final double STDDEV = 3.5;

		Random rand = new Random(44);
		Csprng csprng = newCsprngPair(rand).second;
		double pad = rand.nextDouble();
		double[] samples = new double[N];
		int[] bins = new int[100];
		double[] cutoffs = new double[bins.length - 1];

		assertThrows(
			IndexOutOfBoundsException.class,
			() -> csprng.nextGaussianPair(samples, samples.length - 1)
		);
		assertThrows(
			IllegalArgumentException.class,
			() -> csprng.nextGaussianPair(samples, 0, 0, -1)
		);

		for (int i = 0; i < cutoffs.length; i++) {
			double p = (i + 1) / ((double) bins.length);
			double x = 2 * p - 1;
			double logT = Math.log(1 - x * x);
			double t1 = 2 / (Math.PI * 0.147) + logT / 2;
			double iSq = t1 * t1 - logT / 0.147;

			cutoffs[i] = Math.sqrt(2) * (Math.signum(x) * Math.sqrt(Math.sqrt(iSq) - t1));
		}
		for (int i = 0; i < (N / 2); i++) {
			double[] pair = new double[4];

			pair[0] = pair[3] = pad;
			csprng.nextGaussianPair(pair, 1, MU, STDDEV);

			// padding should not have been modified at all
			assertEquals(pad, pair[0], 0.);
			assertEquals(pad, pair[3], 0.);

			double u = pair[1];
			double v = pair[2];
			double zU = (u - MU) / STDDEV;
			double zV = (v - MU) / STDDEV;
			int bU = Arrays.binarySearch(cutoffs, zU);
			int bV = Arrays.binarySearch(cutoffs, zV);

			bins[bU < 0 ? (-bU - 1) : bU]++;
			bins[bV < 0 ? (-bV - 1) : bV]++;
			samples[i * 2 + 0] = u;
			samples[i * 2 + 1] = v;
		}

		testChiSquare(samples, bins, 0.05);

		double sum = 0;

		for (double s : samples)
			sum += s;

		double mu = sum / samples.length;
		double sqSum = 0;

		for (double s : samples)
			sqSum += Math.pow(s - mu, 2);

		double stddev = Math.sqrt(sqSum / (samples.length - 1));

		assertEquals(MU, mu, 0.01);
		assertEquals(STDDEV, stddev, 0.01);
	}

	private static Csprng newCsprngReal() {
		return Csprng.ofSeed(
			SecureRandom.getSeed(Csprng.INPUT_ENTROPY_SIZE),
			0,
			Csprng.INPUT_ENTROPY_SIZE
		);
	}

	@Test
	public void testMonobitFrequency() {
		final int N = 5 * 1024 * 1024;
		final long N_BITS = N * 8L;

		Csprng csprng = newCsprngReal();
		int n0 = 0;
		int n1 = 0;

		for (int i = 0; i < N; i += 4) {
			int w = Integer.bitCount(csprng.nextInt());

			n1 += w;
			n0 += (32 - w);
		}

		double f = Math.pow(n1 - n0, 2) / N_BITS;

		// threshold at 95% confidence is 3.841
		assertTrue(String.format("bit bias: %s", f), f < 3.841);
	}

	@Test
	public void testBitPairDistribution() {
		final int N = 5 * 1024 * 1024;

		Csprng csprng = newCsprngReal();
		long[] pairs = new long[4]; // 00=0, 01=1, 10=2, 11=3
		long nPairs = 0;

		for (int i = 0; i < N; i += 4) {
			int w = csprng.nextInt();

			for (int j = 0; j < 16; j += 2) {
				pairs[(w >>> j) & 3]++;
				nPairs++;
			}
		}

		for (int i = 0; i < 4; i++) {
			long p = Math.round(100 * (((double) pairs[i]) / nPairs));

			// ideal distribution is 25pct
			assertTrue(String.format("%2s: %s", Integer.toBinaryString(i), p), p >= 25);
		}
	}

	@Test
	public void testByteDistribution() {
		final int N = 5 * 1024 * 1024;

		Csprng csprng = newCsprngReal();
		long[] freq = new long[256];

		for (int i = 0; i < N; i++)
			freq[csprng.nextByte() & 0xff]++;

		double entropy = 0.;

		for (int i = 0; i < 256; i++) {
			if (freq[i] == 0)
				continue;

			double p = ((double) freq[i]) / N;

			entropy -= p * (Math.log(p) / Math.log(2));
		}

		// we should converge near 7.999+
		assertTrue(String.format("shannon entropy: %.6f bps", entropy), entropy > 7.999);
	}
}
