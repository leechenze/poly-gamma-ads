// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.crypt;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.polygamma.android.origin.crypt.UnsignedMath.addMod;
import static org.polygamma.android.origin.crypt.UnsignedMath.gt;
import static org.polygamma.android.origin.crypt.UnsignedMath.l;
import static org.polygamma.android.origin.crypt.UnsignedMath.le;
import static org.polygamma.android.origin.crypt.UnsignedMath.log2;
import static org.polygamma.android.origin.crypt.UnsignedMath.lt;
import static org.polygamma.android.origin.crypt.UnsignedMath.multiplyFull;
import static org.polygamma.android.origin.crypt.UnsignedMath.subtractMod;
import static org.polygamma.android.origin.crypt.UnsignedMath.u;
import static org.polygamma.android.origin.util.Bits.loadInt;
import static org.polygamma.android.origin.util.Bits.storeInt;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Random;

/**
 * {@link NttPrime32} tests.
 */
@RunWith(Parameterized.class)
public class NttPrime32Test {
	@Parameterized.Parameters(name = "n={0},p={1}")
	public static Iterable<Object[]> generateParameters() {
		return () -> new Iterator<Object[]>() {
			final int[] primes = {
				Prime32.P0, Prime32.P1, Prime32.P2,
				TestUtil.findLargestPrimeOfForm(1 << 16, 1, 1 << 29, 1 << 30),
				TestUtil.findLargestPrimeOfForm(1 << 16, 1, 1 << 30, 1 << 31),
				TestUtil.findLargestPrimeOfForm(1 << 16, 1, 1 << 31, ~0)
			};
			final int[] lengths = {
				2, 4, 8, 16, 32, 64, 128, 256, 512,
				1024, 2048, 4096, 8192,
				16384, 32768
			};
			int nextLengthIndex = 0;
			int nextPrimeIndex = 0;

			@Override
			public boolean hasNext() {
				return this.nextLengthIndex < this.lengths.length;
			}

			@Override
			public Object[] next() {
				int ni = this.nextLengthIndex;
				int pi = this.nextPrimeIndex;

				if (++this.nextPrimeIndex == this.primes.length) {
					this.nextLengthIndex++;
					this.nextPrimeIndex = 0;
				}
				return new Object[] { this.lengths[ni], this.primes[pi] };
			}
		};
	}

	private final Random random;
	private final NttPrime32 ntt;
	private final int prime;
	private final int length;

	public NttPrime32Test(int n, int p) {
		this.random = new Random(Integer.hashCode(p) ^ Integer.hashCode(n));
		this.ntt = NttPrime32.of(n, p);
		this.prime = p;
		this.length = n;
	}

	private byte[] newRandomCoefficients() {
		byte[] rv = new byte[this.length * 4];

		for (int i = 0; i < this.length; i++)
			storeInt(rv, i * 4, Integer.remainderUnsigned(this.random.nextInt(), this.prime));
		return rv;
	}

	private byte[] multiply(byte[] lhs, byte[] rhs) {
		int n = this.length;
		int[] full = new int[n * 2];
		byte[] prod = new byte[n * 4];

		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				int a = loadInt(lhs, i * 4);
				int b = loadInt(rhs, j * 4);
				int t = l(Long.remainderUnsigned(multiplyFull(a, b), u(this.prime)));

				full[i + j] = addMod(full[i + j], t, this.prime);
			}
		}
		for (int i = 0; i < n; i++)
			storeInt(prod, i * 4, subtractMod(full[i], full[i + n], this.prime));
		return prod;
	}

	@Test
	public void testOfExplicit() {
		NttPrime32 got = NttPrime32.ofExplicit(
			this.length, this.prime, NttPrime32.bitsOfPrime(this.prime),
			this.ntt.primeReciprocal64,
			this.ntt.primeReciprocal128High, this.ntt.primeReciprocal128Low,
			this.ntt.primeBarrettFactor, this.ntt.primeLimbCount,
			this.ntt.canUseFastReduction
		);

		assertEquals(this.length, got.length());
		assertEquals(this.prime, got.prime);
		assertEquals(this.ntt.primeReciprocal64, got.primeReciprocal64);
		assertEquals(this.ntt.primeReciprocal128High, got.primeReciprocal128High);
		assertEquals(this.ntt.primeReciprocal128Low, got.primeReciprocal128Low);
		assertEquals(this.ntt.primeBarrettFactor, got.primeBarrettFactor);
		assertEquals(this.ntt.primeLimbCount, got.primeLimbCount);
		assertEquals(this.ntt.canUseFastReduction, got.canUseFastReduction);

		assertArrayEquals(this.ntt.forwardTwiddles, got.forwardTwiddles);
		assertArrayEquals(this.ntt.inverseTwiddles, got.inverseTwiddles);
		if (this.ntt instanceof NttPrime32.Of32) {
			assertTrue(got instanceof NttPrime32.Of32);
		} else {
			assertTrue(got instanceof NttPrime32.Of31);
			assertEquals(this.ntt instanceof NttPrime32.Of30, got instanceof NttPrime32.Of30);
			assertArrayEquals(
				((NttPrime32.Of31) this.ntt).forwardTwiddlesShoup,
				((NttPrime32.Of31) got).forwardTwiddlesShoup
			);
			assertArrayEquals(
				((NttPrime32.Of31) this.ntt).inverseTwiddlesShoup,
				((NttPrime32.Of31) got).inverseTwiddlesShoup
			);
		}
	}

	@Test
	public void testCanUseFastReduction() {
		if (lt(this.prime, 1431655766)) {
			assertTrue(this.ntt.canUseFastReduction);
		} else if (gt(this.prime, 1 << 31)) {
			assertFalse(this.ntt.canUseFastReduction);
		} else {
			int k = log2(this.prime) + 1;
			long beta = Long.remainderUnsigned(1L << (k + 31), u(this.prime));

			assertEquals(le(beta, u(this.prime) - (1L << (k - 1))), this.ntt.canUseFastReduction);
		}
	}

	@Test
	public void testForwardAndInverse() {
		int p = this.prime;
		int n = this.length;
		int ns = n * 4;
		byte[] lhs = this.newRandomCoefficients();
		byte[] rhs = this.newRandomCoefficients();
		// [pad,prod,pad,modLhs,pad,modRhs,pad]
		byte[] pad = new byte[3];
		int bufferProdOff = pad.length;
		int bufferModLhsOff = bufferProdOff + ns + pad.length;
		int bufferModRhsOff = bufferModLhsOff + ns + pad.length;
		byte[] buffer = new byte[bufferModRhsOff + ns + pad.length];

		assertThrows(IndexOutOfBoundsException.class, () -> this.ntt.forward(lhs, 1));
		assertThrows(IndexOutOfBoundsException.class, () -> this.ntt.inverse(lhs, 1));

		this.random.nextBytes(pad);
		System.arraycopy(pad, 0, buffer, 0, pad.length);
		System.arraycopy(pad, 0, buffer, bufferProdOff + ns, pad.length);
		System.arraycopy(pad, 0, buffer, bufferModLhsOff + ns, pad.length);
		System.arraycopy(pad, 0, buffer, bufferModRhsOff + ns, pad.length);

		System.arraycopy(lhs, 0, buffer, bufferModLhsOff, ns);
		System.arraycopy(rhs, 0, buffer, bufferModRhsOff, ns);
		this.ntt.forward(buffer, bufferModLhsOff);
		this.ntt.forward(buffer, bufferModRhsOff);

		for (int i = 0; i < n; i++) {
			int modLhs = loadInt(buffer, bufferModLhsOff + i * 4);
			int modRhs = loadInt(buffer, bufferModRhsOff + i * 4);

			assertTrue(lt(modLhs, p));
			assertTrue(lt(modRhs, p));
			storeInt(
				buffer, bufferProdOff + i * 4,
				l(Long.remainderUnsigned(multiplyFull(modLhs, modRhs), u(p)))
			);
		}
		this.ntt.inverse(buffer, bufferProdOff);

		byte[] expProd = this.multiply(lhs, rhs);

		for (int i = 0; i < n; i++) {
			int got = loadInt(buffer, bufferProdOff + i * 4);
			int exp = l(Long.remainderUnsigned(multiplyFull(loadInt(expProd, i * 4), n), u(p)));

			assertTrue(lt(got, p));
			assertEquals(exp, got);
		}

		this.ntt.multiplyAndNormalize(
			buffer, bufferProdOff,
			buffer, bufferModLhsOff,
			buffer, bufferModRhsOff
		);
		this.ntt.inverse(buffer, bufferProdOff);
		assertArrayEquals(expProd, Arrays.copyOfRange(buffer, bufferProdOff, bufferProdOff + ns));

		this.ntt.forward(lhs, 0);
		this.ntt.forward(rhs, 0);
		assertArrayEquals(lhs, Arrays.copyOfRange(buffer, bufferModLhsOff, bufferModLhsOff + ns));
		assertArrayEquals(rhs, Arrays.copyOfRange(buffer, bufferModRhsOff, bufferModRhsOff + ns));

		assertArrayEquals(pad, Arrays.copyOfRange(buffer, 0, pad.length));
		assertArrayEquals(
			pad,
			Arrays.copyOfRange(buffer, bufferProdOff + ns, bufferProdOff + ns + pad.length)
		);
		assertArrayEquals(
			pad,
			Arrays.copyOfRange(buffer, bufferModLhsOff + ns, bufferModLhsOff + ns + pad.length)
		);
		assertArrayEquals(
			pad,
			Arrays.copyOfRange(buffer, bufferModRhsOff + ns, bufferModRhsOff + ns + pad.length)
		);
	}

	@Test
	public void testMultiply() {
		int p = this.prime;
		int n = this.length;
		int ns = this.length * 4;
		byte[] prod = new byte[ns];
		byte[] lhs = this.newRandomCoefficients();
		byte[] rhs = this.newRandomCoefficients();
		// [pad,prod,pad,modLhs,pad,modRhs,pad]
		byte[] pad = new byte[3];
		int bufferProdOff = pad.length;
		int bufferModLhsOff = bufferProdOff + ns + pad.length;
		int bufferModRhsOff = bufferModLhsOff + ns + pad.length;
		byte[] buffer = new byte[bufferModRhsOff + ns + pad.length];

		assertThrows(
			IndexOutOfBoundsException.class,
			() -> this.ntt.multiply(prod, 1, lhs, 0, rhs, 0)
		);
		assertThrows(
			IndexOutOfBoundsException.class,
			() -> this.ntt.multiply(prod, 0, lhs, 1, rhs, 0)
		);
		assertThrows(
			IndexOutOfBoundsException.class,
			() -> this.ntt.multiply(prod, 0, lhs, 0, rhs, 1)
		);

		this.random.nextBytes(pad);
		System.arraycopy(pad, 0, buffer, 0, pad.length);
		System.arraycopy(pad, 0, buffer, bufferProdOff + ns, pad.length);
		System.arraycopy(pad, 0, buffer, bufferModLhsOff + ns, pad.length);
		System.arraycopy(pad, 0, buffer, bufferModRhsOff + ns, pad.length);

		System.arraycopy(lhs, 0, buffer, bufferModLhsOff, ns);
		System.arraycopy(rhs, 0, buffer, bufferModRhsOff, ns);
		this.ntt.multiply(buffer, bufferProdOff, buffer, bufferModLhsOff, buffer, bufferModRhsOff);

		// lhs and rhs should not have been modified
		assertArrayEquals(lhs, Arrays.copyOfRange(buffer, bufferModLhsOff, bufferModLhsOff + ns));
		assertArrayEquals(rhs, Arrays.copyOfRange(buffer, bufferModRhsOff, bufferModRhsOff + ns));

		for (int i = 0; i < n; i++) {
			int a = loadInt(lhs, i * 4);
			int b = loadInt(rhs, i * 4);

			storeInt(prod, i * 4, l(Long.remainderUnsigned(multiplyFull(a, b), u(p))));
		}
		assertArrayEquals(prod, Arrays.copyOfRange(buffer, bufferProdOff, bufferProdOff + ns));
		assertArrayEquals(pad, Arrays.copyOfRange(buffer, 0, pad.length));
		assertArrayEquals(
			pad,
			Arrays.copyOfRange(buffer, bufferProdOff + ns, bufferProdOff + ns + pad.length)
		);
		assertArrayEquals(
			pad,
			Arrays.copyOfRange(buffer, bufferModLhsOff + ns, bufferModLhsOff + ns + pad.length)
		);
		assertArrayEquals(
			pad,
			Arrays.copyOfRange(buffer, bufferModRhsOff + ns, bufferModRhsOff + ns + pad.length)
		);
	}

	@Test
	public void testNormalize() {
		int p = this.prime;
		int n = this.length;
		int ns = this.length * 4;
		int invN = BigInteger.valueOf(u(n))
			.modPow(BigInteger.valueOf(u(p - 2)), BigInteger.valueOf(u(p)))
			.and(BigInteger.valueOf(u(~0)))
			.intValue();
		byte[] vec = this.newRandomCoefficients();
		// [pad,src,pad,dst,pad]
		byte[] pad = new byte[3];
		int bufferSrcOff = pad.length;
		int bufferDstOff = bufferSrcOff + ns + pad.length;
		byte[] buffer = new byte[bufferDstOff + ns + pad.length];

		assertThrows(IndexOutOfBoundsException.class, () -> this.ntt.normalize(vec, 1, vec, 0));
		assertThrows(IndexOutOfBoundsException.class, () -> this.ntt.normalize(vec, 0, vec, 1));

		this.random.nextBytes(pad);
		System.arraycopy(pad, 0, buffer, 0, pad.length);
		System.arraycopy(pad, 0, buffer, bufferSrcOff + ns, pad.length);
		System.arraycopy(pad, 0, buffer, bufferDstOff + ns, pad.length);

		System.arraycopy(vec, 0, buffer, bufferSrcOff, ns);
		this.ntt.normalize(buffer, bufferDstOff, buffer, bufferSrcOff);

		assertArrayEquals(vec, Arrays.copyOfRange(buffer, bufferSrcOff, bufferSrcOff + ns));
		for (int i = 0; i < n; i++) {
			storeInt(
				vec, i * 4,
				l(Long.remainderUnsigned(multiplyFull(loadInt(vec, i * 4), invN), u(p)))
			);
		}
		assertArrayEquals(vec, Arrays.copyOfRange(buffer, bufferDstOff, bufferDstOff + ns));
		assertArrayEquals(pad, Arrays.copyOfRange(buffer, 0, pad.length));
		assertArrayEquals(
			pad,
			Arrays.copyOfRange(buffer, bufferSrcOff + ns, bufferSrcOff + ns + pad.length)
		);
		assertArrayEquals(
			pad,
			Arrays.copyOfRange(buffer, bufferDstOff + ns, bufferDstOff + ns + pad.length)
		);
	}

	@Test
	public void testMultiplyAndNormalize() {
		int p = this.prime;
		int n = this.length;
		int ns = this.length * 4;
		int invN = BigInteger.valueOf(u(n))
			.modPow(BigInteger.valueOf(u(p - 2)), BigInteger.valueOf(u(p)))
			.and(BigInteger.valueOf(u(~0)))
			.intValue();
		byte[] prod = new byte[ns];
		byte[] lhs = this.newRandomCoefficients();
		byte[] rhs = this.newRandomCoefficients();
		// [pad,prod,pad,lhs,pad,rhs,pad]
		byte[] pad = new byte[3];
		int bufferProdOff = pad.length;
		int bufferLhsOff = bufferProdOff + ns + pad.length;
		int bufferRhsOff = bufferLhsOff + ns + pad.length;
		byte[] buffer = new byte[bufferRhsOff + ns + pad.length];

		assertThrows(
			IndexOutOfBoundsException.class,
			() -> this.ntt.multiplyAndNormalize(prod, 1, lhs, 0, rhs, 0)
		);
		assertThrows(
			IndexOutOfBoundsException.class,
			() -> this.ntt.multiplyAndNormalize(prod, 0, lhs, 1, rhs, 0)
		);
		assertThrows(
			IndexOutOfBoundsException.class,
			() -> this.ntt.multiplyAndNormalize(prod, 0, lhs, 0, rhs, 1)
		);

		this.random.nextBytes(pad);
		System.arraycopy(pad, 0, buffer, 0, pad.length);
		System.arraycopy(pad, 0, buffer, bufferProdOff + ns, pad.length);
		System.arraycopy(pad, 0, buffer, bufferLhsOff + ns, pad.length);
		System.arraycopy(pad, 0, buffer, bufferRhsOff + ns, pad.length);

		System.arraycopy(lhs, 0, buffer, bufferLhsOff, ns);
		System.arraycopy(rhs, 0, buffer, bufferRhsOff, ns);
		this.ntt.multiplyAndNormalize(
			buffer, bufferProdOff,
			buffer, bufferLhsOff,
			buffer, bufferRhsOff
		);

		assertArrayEquals(lhs, Arrays.copyOfRange(buffer, bufferLhsOff, bufferLhsOff + ns));
		assertArrayEquals(rhs, Arrays.copyOfRange(buffer, bufferRhsOff, bufferRhsOff + ns));

		for (int i = 0; i < n; i++) {
			int a = loadInt(lhs, i * 4);
			int b = loadInt(rhs, i * 4);
			int c = l(Long.remainderUnsigned(multiplyFull(a, b), u(p)));

			storeInt(prod, i * 4, l(Long.remainderUnsigned(multiplyFull(c, invN), u(p))));
		}
		assertArrayEquals(prod, Arrays.copyOfRange(buffer, bufferProdOff, bufferProdOff + ns));
		assertArrayEquals(pad, Arrays.copyOfRange(buffer, 0, pad.length));
		assertArrayEquals(
			pad,
			Arrays.copyOfRange(buffer, bufferProdOff + ns, bufferProdOff + ns + pad.length)
		);
		assertArrayEquals(
			pad,
			Arrays.copyOfRange(buffer, bufferLhsOff + ns, bufferLhsOff + ns + pad.length)
		);
		assertArrayEquals(
			pad,
			Arrays.copyOfRange(buffer, bufferRhsOff + ns, bufferRhsOff + ns + pad.length)
		);
	}
}
