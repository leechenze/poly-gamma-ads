/*
 * SPDX-License-Identifier: MIT OR Apache-2.0
 *
 * This is ported from `tfhe-ntt` [1].
 *
 * [1] https://github.com/zama-ai/tfhe-rs/tree/main/tfhe-ntt
 */

package org.polygamma.android.origin.crypt;

import static org.polygamma.android.origin.crypt.UnsignedMath.addMod;
import static org.polygamma.android.origin.crypt.UnsignedMath.findPrimitiveRoot;
import static org.polygamma.android.origin.crypt.UnsignedMath.isPow2;
import static org.polygamma.android.origin.crypt.UnsignedMath.l;
import static org.polygamma.android.origin.crypt.UnsignedMath.le;
import static org.polygamma.android.origin.crypt.UnsignedMath.log2;
import static org.polygamma.android.origin.crypt.UnsignedMath.lt;
import static org.polygamma.android.origin.crypt.UnsignedMath.min;
import static org.polygamma.android.origin.crypt.UnsignedMath.multiplyFull;
import static org.polygamma.android.origin.crypt.UnsignedMath.multiplyHigh;
import static org.polygamma.android.origin.crypt.UnsignedMath.multiplyMod;
import static org.polygamma.android.origin.crypt.UnsignedMath.powMod;
import static org.polygamma.android.origin.crypt.UnsignedMath.reciprocalsOf;
import static org.polygamma.android.origin.crypt.UnsignedMath.shoupFactorOf;
import static org.polygamma.android.origin.crypt.UnsignedMath.subtractMod;
import static org.polygamma.android.origin.crypt.UnsignedMath.u;
import static org.polygamma.android.origin.util.Bits.loadInt;
import static org.polygamma.android.origin.util.Bits.storeInt;

import androidx.annotation.IntRange;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;

import org.polygamma.android.origin.util.Preconditions;

import java.math.BigInteger;

/**
 * Engine for a 1-dimensional Number Theoretic Transform (NTT) over a 32-bit prime field.
 */
@SuppressWarnings("JavadocDeclaration")
abstract class NttPrime32 {

	// Threshold at which we begin to transform flat breadth-first.
	private static final int RECURSION_THRESHOLD = 2048;

	// Size of scalar we transform.
	private static final int SCALAR_SIZE = 4;

	// Load `i`-th scalar from `x` at byte offset `off`.
	private static int loadScalar(byte[] x, int off, int i) {
		return loadInt(x, off + i * SCALAR_SIZE);
	}

	// Store `a` into `i`-th scalar within `x` at byte offset `off`.
	private static void storeScalar(byte[] x, int off, int i, int a) {
		storeInt(x, off + i * SCALAR_SIZE, a);
	}

	// Generic implementation for any 32-bit prime.
	@VisibleForTesting
	static final class Of32 extends NttPrime32 {
		private Of32(
			int n, int p,
			long pRecip64, long pRecip128High, long pRecip128Low,
			int pMu, int pK,
			boolean canUseFastReduc
		) {
			super(n, p, pRecip64, pRecip128High, pRecip128Low, pMu, pK, canUseFastReduc);

			int s = 32 - Integer.numberOfTrailingZeros(n);
			int w = findPrimitiveRoot(2 * n, p, pRecip128High, pRecip128Low);

			Preconditions.checkArgument(w != p);
			super.forwardTwiddles[0] = super.inverseTwiddles[0] = 1;
			for (int k = 1, wk = 1; k < n; k++) {
				int fi = Integer.reverse(k) >>> s;
				int ii = Integer.reverse((n - k) % n) >>> s;

				wk = multiplyMod(wk, w, p, pRecip128High, pRecip128Low);
				super.forwardTwiddles[fi] = wk;
				super.inverseTwiddles[ii] = p - wk;
			}
		}

		@Override
		void forwardIntermediateChunk(byte[] x, int off, int t, int wIdx) {
			int w = super.forwardTwiddles[wIdx];
			int p = super.prime;
			long pRecipHigh = super.primeReciprocal128High;
			long pRecipLow = super.primeReciprocal128Low;

			for (int i = 0; i < t; i++) {
				int z0 = loadScalar(x, off, i);
				int z1 = loadScalar(x, off, t + i);
				int z1w = multiplyMod(z1, w, p, pRecipHigh, pRecipLow);

				storeScalar(x, off, i, addMod(z0, z1w, p));
				storeScalar(x, off, t + i, subtractMod(z0, z1w, p));
			}
		}

		@Override
		void forwardFinalChunk(byte[] x, int off, int wIdx) {
			// we don't need a cleanup pass
			this.forwardIntermediateChunk(x, off, 1, wIdx);
		}

		@Override
		void inverseIntermediateChunk(byte[] x, int off, int t, int wIdx) {
			int w = super.inverseTwiddles[wIdx];
			int p = super.prime;
			long pRecipHigh = super.primeReciprocal128High;
			long pRecipLow = super.primeReciprocal128Low;

			for (int i = 0; i < t; i++) {
				int z0 = loadScalar(x, off, i);
				int z1 = loadScalar(x, off, t + i);

				storeScalar(x, off, i, addMod(z0, z1, p));
				storeScalar(
					x, off, t + i,
					multiplyMod(subtractMod(z0, z1, p), w, p, pRecipHigh, pRecipLow)
				);
			}
		}

		@Override
		void inverseFinalChunk(byte[] x, int off, int t, int wIdx) {
			// we don't need a cleanup pass
			this.inverseIntermediateChunk(x, off, t, wIdx);
		}
	}

	// Specialization for when prime fits in 31 bits.
	@VisibleForTesting
	static class Of31 extends NttPrime32 {
		// Forward twiddle Shoup constants.
		@RestrictTo(RestrictTo.Scope.SUBCLASSES)
		final int[] forwardTwiddlesShoup;

		// Inverse twiddle Shoup constants.
		@RestrictTo(RestrictTo.Scope.SUBCLASSES)
		final int[] inverseTwiddlesShoup;

		private Of31(
			int n, int p,
			long pRecip64, long pRecip128High, long pRecip128Low,
			int pMu, int pK,
			boolean canUseFastReduc
		) {
			super(n, p, pRecip64, pRecip128High, pRecip128Low, pMu, pK, canUseFastReduc);

			int s = 32 - Integer.numberOfTrailingZeros(n);
			int w = findPrimitiveRoot(2 * n, p, pRecip128High, pRecip128Low);

			Preconditions.checkArgument(w != p);
			this.forwardTwiddlesShoup = new int[n];
			this.inverseTwiddlesShoup = new int[n];
			super.forwardTwiddles[0] = super.inverseTwiddles[0] = 1;
			this.forwardTwiddlesShoup[0] = this.inverseTwiddlesShoup[0] =
				shoupFactorOf(1, pRecip128High, pRecip128Low);
			for (int k = 1, wk = 1, wkMu; k < n; k++) {
				int fi = Integer.reverse(k) >>> s;
				int ii = Integer.reverse((n - k) % n) >>> s;

				wk = multiplyMod(wk, w, p, pRecip128High, pRecip128Low);
				wkMu = shoupFactorOf(wk, pRecip128High, pRecip128Low);
				super.forwardTwiddles[fi] = wk;
				super.inverseTwiddles[ii] = p - wk;
				this.forwardTwiddlesShoup[fi] = wkMu;
				/*
				 * Shoup(p - w_{k}) =
				 * (p - w_{k}) * 2^{32} / p =
				 * 2^{32} - (w_{k} * 2^{32} / p) =
				 * 2^{32} - Shoup(w_{k}) - 1
				 */
				this.inverseTwiddlesShoup[ii] = ~wkMu;
			}
		}

		@Override
		void forwardIntermediateChunk(byte[] x, int off, int t, int wIdx) {
			int w = super.forwardTwiddles[wIdx];
			int wMu = this.forwardTwiddlesShoup[wIdx];
			int p = super.prime;

			for (int i = 0; i < t; i++) {
				int z0 = loadScalar(x, off, i);
				int z1 = loadScalar(x, off, t + i);
				int tz1 = (z1 * w) + (multiplyHigh(z1, wMu) * -p);

				tz1 = min(tz1, tz1 - p);
				z0 = min(z0, z0 - p);
				storeScalar(x, off, i, z0 + tz1);
				storeScalar(x, off, t + i, (z0 - tz1) + p);
			}
		}

		@Override
		void forwardFinalChunk(byte[] x, int off, int wIdx) {
			int p = super.prime;
			int z0 = loadScalar(x, off, 0);
			int z1 = loadScalar(x, off, 1);
			int tz1 =
				(z1 * super.forwardTwiddles[wIdx]) +
				(multiplyHigh(z1, this.forwardTwiddlesShoup[wIdx]) * -p);

			tz1 = min(tz1, tz1 - p);
			z0 = min(z0, z0 - p);

			int r0 = z0 + tz1;
			int r1 = (z0 - tz1) + p;

			storeScalar(x, off, 0, min(r0, r0 - p));
			storeScalar(x, off, 1, min(r1, r1 - p));
		}

		@Override
		void inverseIntermediateChunk(byte[] x, int off, int t, int wIdx) {
			int w = super.inverseTwiddles[wIdx];
			int wMu = this.inverseTwiddlesShoup[wIdx];
			int p = super.prime;

			for (int i = 0; i < t; i++) {
				int z0 = loadScalar(x, off, i);
				int z1 = loadScalar(x, off, t + i);
				int y0 = z0 + z1;
				int tz = (z0 - z1) + p;
				int y1 = (tz * w) + (multiplyHigh(tz, wMu) * -p);

				storeScalar(x, off, i, min(y0, y0 - p));
				storeScalar(x, off, t + i, min(y1, y1 - p));
			}
		}

		@Override
		void inverseFinalChunk(byte[] x, int off, int t, int wIdx) {
			this.inverseIntermediateChunk(x, off, t, wIdx);
		}
	}

	// Specialization for when prime fits in 30 bits.
	@VisibleForTesting
	static final class Of30 extends Of31 {
		private Of30(
			int n, int p,
			long pRecip64, long pRecip128High, long pRecip128Low,
			int pMu, int pK
		) {
			super(n, p, pRecip64, pRecip128High, pRecip128Low, pMu, pK, true);
		}

		@Override
		void forwardIntermediateChunk(byte[] x, int off, int t, int wIdx) {
			int w = super.forwardTwiddles[wIdx];
			int wMu = super.forwardTwiddlesShoup[wIdx];
			int p = super.prime;

			for (int i = 0; i < t; i++) {
				int z0 = loadScalar(x, off, i);
				int z1 = loadScalar(x, off, t + i);
				int tz1 = (z1 * w) + (multiplyHigh(z1, wMu) * -p);

				z0 = min(z0, z0 - 2 * p);
				storeScalar(x, off, i, z0 + tz1);
				storeScalar(x, off, t + i, (z0 - tz1) + 2 * p);
			}
		}

		@Override
		void forwardFinalChunk(byte[] x, int off, int wIdx) {
			int p = super.prime;
			int z0 = loadScalar(x, off, 0);
			int z1 = loadScalar(x, off, 1);
			int tz1 =
				(z1 * super.forwardTwiddles[wIdx]) +
				(multiplyHigh(z1, super.forwardTwiddlesShoup[wIdx]) * -p);

			tz1 = min(tz1, tz1 - p);
			z0 = min(z0, z0 - (2 * p));
			z0 = min(z0, z0 - p);

			int r0 = z0 + tz1;
			int r1 = (z0 - tz1) + p;

			storeScalar(x, off, 0, min(r0, r0 - p));
			storeScalar(x, off, 1, min(r1, r1 - p));
		}

		@Override
		void inverseIntermediateChunk(byte[] x, int off, int t, int wIdx) {
			int w = super.inverseTwiddles[wIdx];
			int wMu = super.inverseTwiddlesShoup[wIdx];
			int p = super.prime;

			for (int i = 0; i < t; i++) {
				int z0 = loadScalar(x, off, i);
				int z1 = loadScalar(x, off, t + i);
				int y0 = z0 + z1;
				int tz = (z0 - z1) + 2 * p;
				int y1 = (tz * w) + (multiplyHigh(tz, wMu) * -p);

				storeScalar(x, off, i, min(y0, y0 - 2 * p));
				storeScalar(x, off, t + i, y1);
			}
		}

		@Override
		void inverseFinalChunk(byte[] x, int off, int t, int wIdx) {
			int w = super.inverseTwiddles[wIdx];
			int wMu = super.inverseTwiddlesShoup[wIdx];
			int p = super.prime;

			for (int i = 0; i < t; i++) {
				int z0 = loadScalar(x, off, i);
				int z1 = loadScalar(x, off, t + i);
				int y0 = z0 + z1;
				int tz = (z0 - z1) + 2 * p;
				int y1 = (tz * w) + (multiplyHigh(tz, wMu) * -p);

				y0 = min(y0, y0 - 2 * p);
				storeScalar(x, off, i, min(y0, y0 - p));
				storeScalar(x, off, t + i, min(y1, y1 - p));
			}
		}
	}

	/*
	 * Calculate number of bits required to represent a 32-bit prime **within** the bounds of
	 * internal specializations.
	 */
	@VisibleForTesting
	static @IntRange(from = 30, to = 32) int bitsOfPrime(int p) {
		//noinspection NumericOverflow
		return lt(p, 1 << 30) ? 30 : lt(p, 1 << 31) ? 31 : 32;
	}

	/**
	 * Construct new engine with explicit decomposition of prime.
	 * <p>Behaviour, including resulting engine, is undefined when {@code p} is not a prime or
	 * decomposition terms of {@code p} are malformed.
	 *
	 * @param n length of transform
	 * @param p prime over which transform is applied
	 * @param pBits size, in bits, of {@code p}
	 * @param pRecip64 64-bit reciprocal of {@code p}
	 * @param pRecip128High high order bits of 128-bit reciprocal of {@code p}
	 * @param pRecip128Low low order bits of 128-bit reciprocal of {@code p}
	 * @param pMu Barrett factor of {@code p}
	 * @param pK number of limbs required to represent {@code p}
	 * @param canUseFastReduc {@code true} if, and only if, fast reduction algorithms can be used
	 * for {@code p}
	 * @return resulting engine
	 * @throws IllegalArgumentException {@code n} is less than {@code 2} or not a power-of-2, or
	 * {@code p} has no {@code 2n}-th degree root of unity
	 */
	static NttPrime32 ofExplicit(
		int n, int p, @IntRange(from = 30, to = 32) int pBits,
		long pRecip64, long pRecip128High, long pRecip128Low,
		int pMu, int pK,
		boolean canUseFastReduc
	) {
		Preconditions.checkArgument(n > 0 && isPow2(n));
		if (pBits == 30) {
			Preconditions.checkArgument(lt(p, 1 << 30) && canUseFastReduc);
			return new Of30(n, p, pRecip64, pRecip128High, pRecip128Low, pMu, pK);
		} else if (pBits == 31) {
			Preconditions.checkArgument(lt(p, 1 << 31));
			return new Of31(n, p, pRecip64, pRecip128High, pRecip128Low, pMu, pK, canUseFastReduc);
		} else {
			return new Of32(n, p, pRecip64, pRecip128High, pRecip128Low, pMu, pK, canUseFastReduc);
		}
	}

	/**
	 * Construct new engine.
	 *
	 * @param n length of transform
	 * @param p prime over which transform is applied
	 * @return resulting engine
	 * @throws IllegalArgumentException {@code n} is less than {@code 2} or not a power-of-2,
	 * {@code p} has no {@code 2n}-th degree root of unity, or {@code p} is not a prime
	 */
	static NttPrime32 of(int n, int p) {
		Preconditions.checkArgument(
			n > 0 && isPow2(n) &&
			// 40 gives us a deterministic result
			BigInteger.valueOf(u(p))
				.isProbablePrime(40)
		);

		long[] recip = reciprocalsOf(p);
		int k = log2(p) + 1;
		long twoPowL = 1L << (k + 31); // equivalent to 2^{2k} from the zk security blog
		int mu = l(Long.divideUnsigned(twoPowL, u(p)));
		long beta = Long.remainderUnsigned(twoPowL, u(p));
		/*
		 * Barrett reduction estimates the quotient `q ~= floor(x * mu / 2^k)`, meaning the
		 * preliminary remainder `r = x - q * p` is guaranteed to fall into a tight bounding
		 * interval, usually `[0, 2p)` or `[0, 3p)` depending on `k`. Thus when
		 * `beta <= m - b^{k-1}` we can be sure the correction threshold is not reached;
		 * otherwise, we'll need to perform a correction step to bring the estimated remainder
		 * completely inside the valid range `[0, p)`.
		 */
		long correctionThreshold = u(p) - (1L << (k - 1));
		boolean needCorrectionStep = le(beta, correctionThreshold);
		/*
		 * From tfhe-ntt, see https://blog.zksecurity.xyz/posts/barrett-tighter-bound/.
		 *
		 * Barrett reduction gives an approximate value of `q_approx` of the quotient `q` we are
		 * looking to compute the reduction, which can differ from `q` by at most 2:
		 *
		 *     q_approx in {q - 2, q - 1, q}
		 *
		 * Given we subtract `q * p` from our result to start the reduction, we have:
		 *
		 *     prod = to_reduce - q * p
		 *     prod = true_prod + c * p with c in {0,1,2}
		 *
		 * We need to make sure that `prod` does not overflow 32-bits. We have `true_prod` which
		 * is reduced `mod p`, so `true_prod <= p - 1`
		 *
		 *     prod <= 2^31 - 1 <=>
		 *     true_prod + 2 * p <= 2^32 - 1 <=>
		 *     3 * p - 1 <= 2^32 - 1 <=>
		 *     p <= 2^32 / 3 <= 1431655765.3333333 < 1431655766
		 *
		 * After the computation of `prod` a first reduction step is performed, meaning we now
		 * have:
		 *
		 *     prod = true_prod + c * p with c in {0,1}
		 *
		 * We are accumulating in `acc` which is already reduced, so `acc <= p - 1`, the
		 * accumulation yields:
		 *
		 *     acc + prod <= 2^32 - 1 <=>
		 *     (p - 1) + (p - 1) + p <= 2^32 - 1 <=>
		 *     3p - 2 <= 2^32 - 1 <=>
		 *     3p <= 2^32 + 1 <=>
		 *     p <= (2^32 + 1) / 3 <= 1431655765.3333333 < 1431655766
		 *
		 * It is the same criterion. Now for cases where moduli are known to yield a Barrett
		 * reduction with a single step of reduction, the conditions become:
		 *
		 *     true_prod + p <= 2^32 - 1 <=>
		 *     2p - 1 <= 2^32 - 1 <=>
		 *     p <= 2^31
		 */
		boolean canUseFastReduc = lt(p, 1431655766) || (needCorrectionStep && le(p, 1 << 31));

		return ofExplicit(
			n, p, bitsOfPrime(p),
			recip[0], recip[1], recip[2],
			mu, k,
			canUseFastReduc
		);
	}

	// Roots of unity.
	@RestrictTo(RestrictTo.Scope.SUBCLASSES)
	final int[] forwardTwiddles;

	// Pre-scaled inverse roots of unity.
	@RestrictTo(RestrictTo.Scope.SUBCLASSES)
	final int[] inverseTwiddles;

	// 64-bit reciprocal of `prime`.
	@VisibleForTesting
	final long primeReciprocal64;

	// 128-bit reciprocal of `prime`.
	@VisibleForTesting
	final long primeReciprocal128High;
	@VisibleForTesting
	final long primeReciprocal128Low;

	// Prime over which transform is applied.
	@VisibleForTesting
	final int prime;

	// Approximation of the reciprocal of `prime`.
	@VisibleForTesting
	final int primeBarrettFactor;

	// Number of limbs required to represent `prime`.
	@VisibleForTesting
	final int primeLimbCount;

	// Modular multiplicative inverse of the transform length modulo `prime`.
	private final int inverseLengthModPrime;

	// Fast reduction operations can be utilized.
	@VisibleForTesting
	final boolean canUseFastReduction;

	private NttPrime32(
		int n,
		int p, long pRecip64, long pRecip128High, long pRecip128Low, int pMu, int pK,
		boolean canUseFastReduc
	) {
		this.forwardTwiddles = new int[n];
		this.inverseTwiddles = new int[n];
		this.prime = p;
		this.primeReciprocal64 = pRecip64;
		this.primeReciprocal128High = pRecip128High;
		this.primeReciprocal128Low = pRecip128Low;
		this.primeBarrettFactor = pMu;
		this.primeLimbCount = pK;
		this.inverseLengthModPrime = powMod(n, p - 2, p, pRecip128High, pRecip128Low);
		this.canUseFastReduction = canUseFastReduc;
	}

	/**
	 * Transform length.
	 *
	 * @return length
	 */
	final int length() {
		return this.forwardTwiddles.length;
	}

	/**
	 * Transform intermediate chunk for a forward NTT.
	 *
	 * @param x vector to transform
	 * @param off offset, within {@code x}, to begin transforming at
	 * @param t half-block size
	 * @param wIdx twiddle factor index
	 */
	@RestrictTo(RestrictTo.Scope.SUBCLASSES)
	abstract void forwardIntermediateChunk(byte[] x, int off, int t, int wIdx);

	/**
	 * Transform final chunk for a forward NTT.
	 *
	 * @param x vector to transform
	 * @param off offset, within {@code x}, to begin transforming at
	 * @param wIdx twiddle factor index
	 */
	@RestrictTo(RestrictTo.Scope.SUBCLASSES)
	abstract void forwardFinalChunk(byte[] x, int off, int wIdx);

	// Apply a single stage in forward NTT starting at layer `layer` and branch `branch`.
	private void forwardStage(byte[] x, int off, int n, int layer, int branch) {
		int t = n / 2;
		int wIdx = (1 << layer) + branch;

		if (n > RECURSION_THRESHOLD) {
			this.forwardIntermediateChunk(x, off, t, wIdx);
			this.forwardStage(x, off, t, layer + 1, branch * 2);
			this.forwardStage(x, off + (t * SCALAR_SIZE), t, layer + 1, branch * 2 + 1);
			return;
		}
		for (int m = 1; m < n && t > 1; t /= 2, m *= 2, wIdx *= 2) {
			for (int i = 0; i < (n / (t * 2)); i++)
				this.forwardIntermediateChunk(x, off + (i * (t * 2)) * SCALAR_SIZE, t, wIdx + i);
		}
		if (t == 1) {
			for (int i = 0; i < (n / 2); i++)
				this.forwardFinalChunk(x, off + (i * 2) * SCALAR_SIZE, wIdx + i);
		}
	}

	/**
	 * Apply forward transform in-place.
	 * <p>This transforms each 32-bit element in {@code x} from its coefficient form into the
	 * respective point-value evaluation form, for each element in {@code x} from {@code off}
	 * (inclusive) to {@code off + n * 4} (exclusive), where {@code n} is equal to the transform
	 * {@linkplain #length() length}.
	 *
	 * @param x vector to transform
	 * @param off offset, within {@code x}, to begin transforming from
	 * @throws IndexOutOfBoundsException {@code off} is negative or {@code off + n * 4} is greater
	 * than {@code x.length}
	 */
	final void forward(byte[] x, int off) {
		int n = this.length();

		Preconditions.checkFromIndexSize(off, n * SCALAR_SIZE, x.length);
		this.forwardStage(x, off, n, 0, 0);
	}

	/**
	 * Transform intermediate chunk for an inverse NTT.
	 *
	 * @param x vector to transform
	 * @param off offset, within {@code x}, to begin transforming at
	 * @param t half-block size
	 * @param wIdx twiddle factor index
	 */
	@RestrictTo(RestrictTo.Scope.SUBCLASSES)
	abstract void inverseIntermediateChunk(byte[] x, int off, int t, int wIdx);

	/**
	 * Transform final chunk for an inverse NTT.
	 *
	 * @param x vector to transform
	 * @param off offset, within {@code x}, to begin transforming at
	 * @param t half-block size
	 * @param wIdx twiddle factor index
	 */
	@RestrictTo(RestrictTo.Scope.SUBCLASSES)
	abstract void inverseFinalChunk(byte[] x, int off, int t, int wIdx);

	// Apply a single stage in inverse NTT starting at layer `layer` and branch `branch`.
	private void inverseStage(byte[] x, int off, int n, int layer, int branch) {
		if (n > RECURSION_THRESHOLD) {
			int t = n / 2;
			int wIdx = (1 << layer) + branch;

			this.inverseStage(x, off, t, layer + 1, branch * 2);
			this.inverseStage(x, off + t * SCALAR_SIZE, t, layer + 1, branch * 2 + 1);
			if (layer == 0 && branch == 0)
				this.inverseFinalChunk(x, off, t, wIdx);
			else
				this.inverseIntermediateChunk(x, off, t, wIdx);
			return;
		}

		for (int m = n, t = 1, wIdx = (m << layer) + (m * branch); m > 1; t *= 2) {
			m /= 2;
			wIdx /= 2;
			if (m == 1 && layer == 0 && branch == 0) {
				for (int i = 0; i < (n / (t * 2)); i++)
					this.inverseFinalChunk(x, off + (i * (t * 2)) * SCALAR_SIZE, t, wIdx + i);
			} else {
				for (int i = 0; i < (n / (t * 2)); i++) {
					this.inverseIntermediateChunk(
						x, off + (i * (t * 2)) * SCALAR_SIZE, t,
						wIdx + i
					);
				}
			}
		}
	}

	/**
	 * Apply inverse transform in-place.
	 * <p>This transforms each 32-bit element in {@code x} from its point-value evaluation form
	 * into the respective coefficient form, for each element in {@code x} from {@code off}
	 * (inclusive) to {@code off + n * 4} (exclusive), where {@code n} is equal to the transform
	 * {@linkplain #length() length}.
	 *
	 * @param x vector to transform
	 * @param off offset, within {@code x}, to begin transforming from
	 * @throws IndexOutOfBoundsException {@code off} is negative or {@code off + n * 4} is greater
	 * than {@code x.length}
	 */
	final void inverse(byte[] x, int off) {
		int n = this.length();

		Preconditions.checkFromIndexSize(off, n, x.length);
		this.inverseStage(x, off, n, 0, 0);
	}

	/**
	 * Compute element-wise product of two vectors, in frequency domain, into another.
	 * <p>This computes the element-wise product of each 32-bit element within {@code [0; n)},
	 * where {@code n} is equal to the transform {@linkplain #length() length}, of {@code lhs}
	 * and {@code rhs} into {@code dst}, starting at position {@code lhsOff} (inclusive), {@code
	 * rhsOff} (inclusive), and {@code dstOff} (inclusive), respectively.
	 *
	 * @param dst vector to store product into
	 * @param dstOff offset, within {@code dst}, to begin storing from
	 * @param lhs left-hand side vector to compute product of
	 * @param lhsOff offset, within {@code lhs}, to begin loading from
	 * @param rhs right-hand side vector to compute product of
	 * @param rhsOff offset, within {@code rhs}, to begin loading from
	 * @throws IndexOutOfBoundsException {@code dstOff}, {@code lhsOff}, or {@code rhsOff} is
	 * negative, or, {@code dstOff + n * 4}, {@code lhsOff + n * 4}, or {@code rhsOff + n * 4} is
	 * greater than {@code dst.length}, {@code lhs.length}, or {@code rhs.length}, respectively
	 */
	final void multiply(byte[] dst, int dstOff, byte[] lhs, int lhsOff, byte[] rhs, int rhsOff) {
		int n = this.length();
		int p = this.prime;
		long pRecipHigh = this.primeReciprocal128High;
		long pRecipLow = this.primeReciprocal128Low;

		Preconditions.checkFromIndexSize(dstOff, n * SCALAR_SIZE, dst.length);
		Preconditions.checkFromIndexSize(lhsOff, n * SCALAR_SIZE, lhs.length);
		Preconditions.checkFromIndexSize(rhsOff, n * SCALAR_SIZE, rhs.length);
		if (!this.canUseFastReduction) {
			for (int i = 0; i < n; i++) {
				int x = loadScalar(lhs, lhsOff, i);
				int y = loadScalar(rhs, rhsOff, i);

				storeScalar(dst, dstOff, i, multiplyMod(x, y, p, pRecipHigh, pRecipLow));
			}
			return;
		}

		int pK = this.primeLimbCount - 1;
		int pMu = this.primeBarrettFactor;

		for (int i = 0; i < n; i++) {
			long prodFull = multiplyFull(loadScalar(lhs, lhsOff, i), loadScalar(rhs, rhsOff, i));
			int prod = l(prodFull) - (p * multiplyHigh((int) (prodFull >>> pK), pMu));

			storeScalar(dst, dstOff, i, min(prod, prod - p));
		}
	}

	/**
	 * Scale coefficients by modular inverse of transform length (polynomial degree).
	 * <p>This scales each 32-bit coefficient, within {@code [0; n)} where {@code n} is equal to
	 * the transform {@linkplain #length() length}, in {@code src}, starting at position {@code
	 * srcOff} (inclusive), by {@code n^{-1}} modulo {@code p}, where {@code p} is equal to the
	 * prime modulus of the finite field, into {@code dst}, starting at position {@code dstOff}
	 * (inclusive).
	 *
	 * @param dst vector to store normalized coefficients, within range {@code [0, p)}, into
	 * @param dstOff offset, within {@code dst}, to begin storing from
	 * @param src vector to scale unscaled coefficients from
	 * @param srcOff offset, within {@code src}, to begin loading from
	 * @throws IndexOutOfBoundsException {@code dstOff} or {@code srcOff} is negative, or,
	 * {@code dstOff + n * 4} or {@code srcOff + n * 4} is greater than {@code dst.length} or
	 * {@code src.length}, respectively
	 */
	final void normalize(byte[] dst, int dstOff, byte[] src, int srcOff) {
		int n = this.length();
		int invN = this.inverseLengthModPrime;
		int p = this.prime;
		long pRecipHigh = this.primeReciprocal128High;
		long pRecipLow = this.primeReciprocal128Low;

		Preconditions.checkFromIndexSize(dstOff, n * SCALAR_SIZE, dst.length);
		Preconditions.checkFromIndexSize(srcOff, n * SCALAR_SIZE, src.length);
		if (!this.canUseFastReduction) {
			for (int i = 0; i < n; i++) {
				storeScalar(dst, dstOff, i, multiplyMod(
					loadScalar(src, srcOff, i), invN,
					p, pRecipHigh, pRecipLow
				));
			}
			return;
		}

		int invNMu = shoupFactorOf(invN, pRecipHigh, pRecipLow);

		for (int i = 0; i < n; i++) {
			int a = loadScalar(src, srcOff, i);
			int s = (a * invN) - (multiplyHigh(a, invNMu) * p);

			storeScalar(dst, dstOff, i, min(s, s - p));
		}
	}

	/**
	 * Compute scaled element-wise product of two vectors, in frequency domain, into another.
	 * <p>Efficient equivalent of:
	 * {@snippet :
	 * multiply(dst, dstOff, lhs, lhsOff, rhs, rhsOff); // @link substring="multiply" target="#multiply(byte[], int, byte[], int, byte[], int)"
	 * scale(dst, dstOff, dst, dstOff); // @link substring="scale" target="#scale(byte[], int, byte[], int)"
	 * }
	 *
	 * @param dst vector to store scaled product into
	 * @param dstOff offset, within {@code dst}, to begin storing from
	 * @param lhs left-hand side vector to compute product of
	 * @param lhsOff offset, within {@code lhs}, to begin loading from
	 * @param rhs right-hand side vector to compute product of
	 * @param rhsOff offset, within {@code rhs}, to begin loading from
	 * @throws IndexOutOfBoundsException {@code dstOff}, {@code lhsOff}, or {@code rhsOff} is
	 * negative, or, {@code dstOff + n * 4}, {@code lhsOff + n * 4}, or {@code rhsOff + n * 4},
	 * where {@code n} is equal to transform {@linkplain #length() length}, is greater than {@code
	 * dst.length}, {@code lhs.length}, or {@code rhs.length}, respectively
	 */
	final void
	multiplyAndNormalize(byte[] dst, int dstOff, byte[] lhs, int lhsOff, byte[] rhs, int rhsOff) {
		int n = this.length();
		int invN = this.inverseLengthModPrime;
		int p = this.prime;
		long pRecipHigh = this.primeReciprocal128High;
		long pRecipLow = this.primeReciprocal128Low;

		Preconditions.checkFromIndexSize(dstOff, n * SCALAR_SIZE, dst.length);
		Preconditions.checkFromIndexSize(lhsOff, n * SCALAR_SIZE, lhs.length);
		Preconditions.checkFromIndexSize(rhsOff, n * SCALAR_SIZE, rhs.length);
		if (!this.canUseFastReduction) {
			for (int i = 0; i < n; i++) {
				int prod = multiplyMod(
					loadScalar(lhs, lhsOff, i),
					loadScalar(rhs, rhsOff, i),
					p, pRecipHigh, pRecipLow
				);

				storeScalar(dst, dstOff, i, multiplyMod(prod, invN, p, pRecipHigh, pRecipLow));
			}
			return;
		}

		int invNMu = shoupFactorOf(invN, pRecipHigh, pRecipLow);
		int pK = this.primeLimbCount - 1;
		int pMu = this.primeBarrettFactor;

		for (int i = 0; i < n; i++) {
			long prodFull = multiplyFull(loadScalar(lhs, lhsOff, i), loadScalar(rhs, rhsOff, i));
			int prod = l(prodFull) - (p * multiplyHigh((int) (prodFull >>> pK), pMu));
			int scal = (prod * invN) - (multiplyHigh(prod, invNMu) * p);

			storeScalar(dst, dstOff, i, min(scal, scal - p));
		}
	}
}
