// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.crypt;

import static org.polygamma.android.origin.util.Bits.loadIntLe;
import static org.polygamma.android.origin.util.Bits.loadLongLe;

import android.os.Build;

import androidx.annotation.VisibleForTesting;

import org.polygamma.android.origin.util.Preconditions;

/**
 * Cryptographically secure pseudo-random number generator (CSPRNG) definitions.
 * <p>This CSPRNG implementation uses Ascon 256-bit hashing for ingestion and compression, while
 * ChaCha20 is used for extraction. With a high-entropy seed, this provides unpredictability and
 * backward secrecy; however, the caller is responsible for forward secrecy through periodic
 * {@linkplain #mixEntropy(byte[], int, int) mixing} and {@linkplain #reseed() reseeding}.
 * <p>This implementation is based on the Linux kernel's CSPRNG implementation; however, in place
 * of using Blake2 for ingestion and compression, Ascon is used. While Ascon has lower performance
 * it has a smaller state requirement, reducing memory requirements.
 *
 * @since 1.2
 */
@SuppressWarnings("JavadocDeclaration")
public class Csprng {

	/**
	 * Size, in bytes, of input entropy space.
	 *
	 * @since 1.2
	 */
	public static final int INPUT_ENTROPY_SIZE = ChaCha20.KEY_SIZE;

	/**
	 * Construct new generator with initial entropy.
	 * <p>The resulting generator is initialized with {@code len} bytes of entropy from {@code
	 * seed}, starting at position {@code off} (inclusive). When {@code len} is greater than or
	 * equal to {@link #INPUT_ENTROPY_SIZE}, the resulting generator can be considered secure for
	 * cryptographic use. When {@code seed} has fewer than {@code INPUT_ENTROPY_SIZE} bytes of
	 * entropy, the generator can be considered as having entered into the {@code CRNG_EARLY}
	 * state, as defined by the
	 * <a href="https://github.com/torvalds/linux/blob/fce2dfa773ced15f27dd27cd0b482a7473cdcf2a/drivers/char/random.c#L80">Linux's CSPRNG</a>.
	 * Upon subsequent {@linkplain #mixEntropy(byte[], int, int) mixing} of at least an additional
	 * {@code INPUT_ENTROPY_SIZE - len} bytes of high-quality entropy, followed by a {@linkplain
	 * #reseed() reseeding}, the generator can be considered secure for cryptographic use.
	 *
	 * @param seed initial entropy
	 * @param off offset, within {@code seed}, to begin loading from
	 * @param len number of bytes of entropy to load from {@code seed}
	 * @return resulting generator
	 * @throws IndexOutOfBoundsException {@code off} or {@code len} is negative, or {@code
	 * off + len} is greater than {@code seed.length}
	 * @since 1.2
	 */
	public static Csprng ofSeed(byte[] seed, int off, int len) {
		Csprng csprng = new Csprng();

		csprng.mixEntropy(seed, off, len);
		csprng.reseed();
		return csprng;
	}

	@VisibleForTesting
	final ChaCha20 extractor;
	private final Ascon compressor;

	private Csprng() {
		this.extractor = ChaCha20.ofEmpty();
		this.compressor = Ascon.ofHash();
	}

	private Csprng(Csprng that) {
		this.extractor = that.extractor.split();
		this.compressor = that.compressor.split();
	}

	/**
	 * Construct copy of {@code this} at current state.
	 * <p>The copy returned will be initiated with the state underlying {@code this}. Until
	 * {@linkplain #mixEntropy(byte[], int, int) mixing} and {@linkplain #reseed() reseeding},
	 * both generators will generate the same {@code byte} sequences.
	 *
	 * @return copy
	 * @since 1.2
	 */
	public Csprng split() {
		return new Csprng(this);
	}

	// Generate new extractor keystream, consuming first `KEY_SIZE` bytes for new extractor key.
	private void forwardExtractor() {
		this.extractor.applyBlock();
		this.extractor.setKey(this.extractor.keystream, 0);
		this.extractor.keystreamRemaining = ChaCha20.KEYSTREAM_SIZE - ChaCha20.KEY_SIZE;
	}

	/*
	 * Consume `n` bytes from extractor's keystream, where `1 <= n <= KEYSTREAM_SIZE - KEY_SIZE`.
	 * When keystream has fewer than `n` bytes remaining, the extractor is forwarded and the
	 * resulting keystream is consumed from. The offset from where in the keystream `n` bytes can
	 * be consumed from is returned.
	 */
	private int forwardExtractorKeystream(int n) {
		Preconditions.checkArgument(n >= 1 && n <= (ChaCha20.KEYSTREAM_SIZE - ChaCha20.KEY_SIZE));

		int ksRem = this.extractor.keystreamRemaining;

		if (ksRem < n) {
			this.forwardExtractor();
			ksRem = this.extractor.keystreamRemaining;
		}
		this.extractor.keystreamRemaining -= n;
		return ChaCha20.KEYSTREAM_SIZE - ksRem;
	}

	/**
	 * Mix entropy.
	 * <p>This mixes {@code len} bytes of entropy from {@code ent}, starting from position {@code
	 * off}, into the internal 256-bit entropy space. Note that while the entropy specified is
	 * mixed into the entropy state of the generator, subsequent {@linkplain
	 * #nextBytes(byte[], int, int) extraction} will <b>not</b> utilize the updated internal
	 * entropy space until the generator is {@linkplain #reseed() reseeded}.
	 *
	 * @param ent entropy to mix
	 * @param off offset, within {@code ent}, to begin loading from
	 * @param len number of bytes of entropy to mix
	 * @throws IndexOutOfBoundsException {@code off} or {@code len} is negative, or {@code
	 * off + len} is greater than {@code ent.length}
	 * @since 1.2
	 */
	public void mixEntropy(byte[] ent, int off, int len) {
		this.compressor.updateHash(ent, off, len);
	}

	/**
	 * Reseed extraction state.
	 * <p>Upon return, the extraction state is updated with previously {@linkplain
	 * #mixEntropy(byte[], int, int) mixed} entropy. When enough entropy is mixed, invoking this
	 * ensures forward secrecy.
	 *
	 * @since 1.2
	 */
	public void reseed() {
		byte[] key = new byte[ChaCha20.KEY_SIZE];

		/*
		 * First we need to generate a new key. Absorb 256 bits, from the current extractor, into
		 * the current entropy compressor, then generate the new key. Then we can clear out the
		 * compressor and compress the newly generated key.
		 */
		this.nextBytes(key, 0, ChaCha20.KEY_SIZE);
		this.compressor.updateHash(key, 0, ChaCha20.KEY_SIZE);
		this.compressor.finishHash(key, 0);
		this.compressor.resetHash();
		this.compressor.updateHash(key, 0, ChaCha20.KEY_SIZE);

		/*
		 * With a new key, we can update the extractor. We don't want to use the new key as-is,
		 * instead, we'll use the new key to generate the initial keystream, forward the key,
		 * and use the remaining keystream bytes as our remaining output.
		 */
		this.extractor.setKey(key, 0)
			.setCounter(0)
			.clearNonce();
		this.forwardExtractor();
	}

	/**
	 * Extract uniform random bytes.
	 *
	 * @param dst buffer to extract into
	 * @param off offset, within {@code dst}, to begin extracting from
	 * @param len number of bytes to extract
	 * @throws IndexOutOfBoundsException {@code off} or {@code len} is negative, or {@code
	 * off + len} is greater than {@code dst.length}
	 * @since 1.2
	 */
	public void nextBytes(byte[] dst, int off, int len) {
		Preconditions.checkFromIndexSize(off, len, dst.length);

		int ksRem = this.extractor.keystreamRemaining;

		while (len > 0) {
			if (ksRem <= 0) {
				this.forwardExtractor();
				ksRem = this.extractor.keystreamRemaining;
			}

			int lim = Math.min(len, ksRem);

			System.arraycopy(
				this.extractor.keystream, ChaCha20.KEYSTREAM_SIZE - ksRem,
				dst, off,
				lim
			);
			off += lim;
			len -= lim;
			ksRem -= lim;
		}
		this.extractor.keystreamRemaining = ksRem;
	}

	/**
	 * Extract uniform variable width integer.
	 *
	 * @param size number of bytes to extract
	 * @return {@code long} whose least-significant {@code size} bytes contain uniform random
	 * @since 1.2
	 */
	public long nextInteger(int size) {
		Preconditions.checkArgument(size >= 1 && size <= 8);

		byte[] ks = this.extractor.keystream;
		int off = this.forwardExtractorKeystream(size);

		if (size == 8)
			return loadLongLe(ks, off);
		if ((off + 8) <= ks.length)
			return loadLongLe(ks, off) & ~(~0L << (size * 8));
		return loadLongLe(ks, ks.length - 8) >>> ((8 - size) * 8);
	}

	/**
	 * Extract uniform {@code long}.
	 *
	 * @return extracted {@code long}
	 * @since 1.2
	 */
	public long nextLong() {
		return loadLongLe(this.extractor.keystream, this.forwardExtractorKeystream(8));
	}

	/**
	 * Extract uniform {@code int}.
	 *
	 * @return extracted {@code int}
	 * @since 1.2
	 */
	public int nextInt() {
		return loadIntLe(this.extractor.keystream, this.forwardExtractorKeystream(4));
	}

	/**
	 * Extract uniform {@code byte}.
	 *
	 * @return extracted {@code byte}
	 * @since 1.2
	 */
	public byte nextByte() {
		return this.extractor.keystream[this.forwardExtractorKeystream(1)];
	}

	/**
	 * Extract uniform {@code boolean}.
	 *
	 * @return extracted {@code boolean}
	 * @since 1.2
	 */
	public boolean nextBoolean() {
		return ((this.nextByte() & 0xff) & 1) != 0;
	}

	/**
	 * Extract uniform {@code double}.
	 *
	 * @return extracted {@code double} uniformly distributed between {@code 0} and {@code 1}
	 * @since 1.2
	 */
	public double nextDouble() {
		long x = this.nextLong();
		long x0 = ((x & 0xffffffffL) >>> (32 - (53 /* Double.PRECISION */ - 27))) << 27;
		long x1 = x >>> 37;

		return (x0 + x1) * 1.1102230246251565e-16 /* 1 / (1 << Double.PRECISION) */;
	}

	/**
	 * Extract pair of {@code double} values sampled from Gaussian distribution.
	 * <p>Upon return, the two extracted values are stored into {@code dst} at positions {@code
	 * dstOff} and {@code dstOff + 1}.
	 *
	 * @param dst array to store extracted values into
	 * @param dstOff offset, within {@code dst}, to begin storing from
	 * @param mean mean of distribution to sample from
	 * @param stddev standard deviation of distribution to sample from
	 * @throws IllegalArgumentException {@code stddev} is negative
	 * @throws IndexOutOfBoundsException {@code dstOff} is negative or {@code dstOff + 2} is
	 * greater than {@code dst.length}
	 * @since 1.2
	 */
	public void nextGaussianPair(double[] dst, int dstOff, double mean, double stddev) {
		Preconditions.checkArgument(stddev >= 0);
		Preconditions.checkFromIndexSize(dstOff, 2, dst.length);

		double u, v, s;

		do {
			u = this.nextDouble();
			v = this.nextDouble();
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
				u = Math.fma(2, u, -1);
				v = Math.fma(2, v, -1);
				s = Math.fma(u, u, v * v);
			} else {
				u = 2 * u - 1;
				v = 2 * v - 1;
				s = u * u + v * v;
			}
		} while (s <= 0 || s >= 1);

		double m = Math.sqrt(-2 * Math.log(s) / s);

		u *= m;
		v *= m;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			u = Math.fma(u, stddev, mean);
			v = Math.fma(v, stddev, mean);
		} else {
			u = u * stddev + mean;
			v = v * stddev + mean;
		}
		dst[dstOff + 0] = u;
		dst[dstOff + 1] = v;
	}

	/**
	 * Extract pair of {@code double} values sampled from Gaussian distribution with mean {@code
	 * 0} and standard deviation {@code 1}.
	 * <p>Shorthand for:
	 * {@snippet :
	 * nextGaussianPair(dst, dstOff, 0, 1); // @link substring="nextGaussianPair" target="#nextGaussianPair(double[], int, double, double)"
	 * }
	 *
	 * @param dst array to store extracted values into
	 * @param dstOff offset, within {@code dst}, to begin storing from
	 * @throws IndexOutOfBoundsException {@code dstOff} is negative or {@code dstOff + 2} is
	 * greater than {@code dst.length}
	 * @since 1.2
	 * @see #nextGaussianPair(double[], int, double, double)
	 */
	public void nextGaussianPair(double[] dst, int dstOff) {
		this.nextGaussianPair(dst, dstOff, 0, 1);
	}

	/**
	 * Extract {@code double} value sampled from Gaussian distribution.
	 *
	 * @param mean mean of distribution to sample from
	 * @param stddev standard deviation of distribution to sample from
	 * @return extracted {@code double}
	 * @throws IllegalArgumentException {@code stddev} is negative
	 * @since 1.2
	 * @see #nextGaussianPair(double[], int, double, double)
	 */
	public double nextGaussian(double stddev, double mean) {
		double[] pair = new double[2];

		this.nextGaussianPair(pair, 0, stddev, mean);
		return pair[0];
	}

	/**
	 * Extract {@code double} value sampled from Gaussian distribution with mean {@code 0} and
	 * standard deviation {@code 1}.
	 * <p>Shorthand for:
	 * {@snippet :
	 * nextGaussian(0, 1); // @link substring="nextGaussian" target="#nextGaussian(double, double)"
	 * }
	 *
	 * @return extracted {@code double}
	 * @since 1.2
	 * @see #nextGaussian(double, double)
	 */
	public double nextGaussian() {
		return this.nextGaussian(0, 1);
	}
}
