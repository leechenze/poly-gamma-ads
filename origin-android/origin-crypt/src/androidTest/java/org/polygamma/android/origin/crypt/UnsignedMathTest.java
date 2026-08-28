// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.crypt;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.math.BigInteger;
import java.util.Random;

/**
 * {@link UnsignedMath} tests.
 */
@RunWith(AndroidJUnit4.class)
public class UnsignedMathTest {

	private static BigInteger ub(int x) {
		return BigInteger.valueOf(x & 0xffffffffL);
	}

	private static BigInteger ub(long x) {
		return new BigInteger(Long.toUnsignedString(x));
	}

	@Test
	public void testBasic() {
		assertEquals(0L, UnsignedMath.u(0));
		assertEquals(1L, UnsignedMath.u(1));
		assertEquals(0xffffffffL, UnsignedMath.u(~0));

		assertEquals(0, UnsignedMath.h(0L));
		assertEquals(0, UnsignedMath.h(1L));
		assertEquals(0xdeadbeef, UnsignedMath.h(0xdeadbeefcafebabeL));

		assertEquals(0, UnsignedMath.l(0L));
		assertEquals(1, UnsignedMath.l(1L));
		assertEquals(0xcafebabe, UnsignedMath.l(0xdeadbeefcafebabeL));

		assertTrue(UnsignedMath.gt(1, 0));
		assertTrue(UnsignedMath.gt(~0, Integer.MAX_VALUE));
		assertFalse(UnsignedMath.gt(0, 1));
		assertFalse(UnsignedMath.gt(Integer.MAX_VALUE, ~0));
		assertFalse(UnsignedMath.gt(0, 0));
		assertFalse(UnsignedMath.gt(1, 1));
		assertFalse(UnsignedMath.gt(~0, ~0));

		assertTrue(UnsignedMath.ge(1, 0));
		assertTrue(UnsignedMath.ge(~0, Integer.MAX_VALUE));
		assertFalse(UnsignedMath.ge(0, 1));
		assertFalse(UnsignedMath.ge(Integer.MAX_VALUE, ~0));
		assertTrue(UnsignedMath.ge(0, 0));
		assertTrue(UnsignedMath.ge(1, 1));
		assertTrue(UnsignedMath.ge(~0, ~0));

		assertFalse(UnsignedMath.lt(1, 0));
		assertFalse(UnsignedMath.lt(~0, Integer.MAX_VALUE));
		assertTrue(UnsignedMath.lt(0, 1));
		assertTrue(UnsignedMath.lt(Integer.MAX_VALUE, ~0));
		assertFalse(UnsignedMath.lt(0, 0));
		assertFalse(UnsignedMath.lt(1, 1));
		assertFalse(UnsignedMath.lt(~0, ~0));

		assertFalse(UnsignedMath.le(1, 0));
		assertFalse(UnsignedMath.le(~0, Integer.MAX_VALUE));
		assertTrue(UnsignedMath.le(0, 1));
		assertTrue(UnsignedMath.le(Integer.MAX_VALUE, ~0));
		assertTrue(UnsignedMath.le(0, 0));
		assertTrue(UnsignedMath.le(1, 1));
		assertTrue(UnsignedMath.le(~0, ~0));

		assertFalse(UnsignedMath.isPow2(0));
		assertTrue(UnsignedMath.isPow2(1));
		assertTrue(UnsignedMath.isPow2(2));
		assertFalse(UnsignedMath.isPow2(3));

		assertEquals(0, UnsignedMath.min(0, 0));
		assertEquals(0, UnsignedMath.min(0, 1));
		assertEquals(0, UnsignedMath.min(0, ~0));
		assertEquals(Integer.MAX_VALUE, UnsignedMath.min(Integer.MAX_VALUE, ~0));

		assertEquals(0, UnsignedMath.max(0, 0));
		assertEquals(1, UnsignedMath.max(0, 1));
		assertEquals(~0, UnsignedMath.max(0, ~0));
		assertEquals(~0, UnsignedMath.max(Integer.MAX_VALUE, ~0));

		assertEquals(-1, UnsignedMath.log2(0));
		assertEquals(0, UnsignedMath.log2(1));
		assertEquals(31, UnsignedMath.log2(~0));

		assertEquals(0xeadbeefd, UnsignedMath.rotateLeft(0xdeadbeef, 4));
		assertEquals(0xedeadbeefcafebabL, UnsignedMath.rotateRight(0xdeadbeefcafebabeL, 4));

		Random rand = new Random(44);

		for (int i = 0; i < 1000; i++) {
			int a = rand.nextInt();

			assertEquals(a, UnsignedMath.multiplyFull(a, 1));
			assertEquals(a, UnsignedMath.multiplyFull(1, a));
			assertEquals(UnsignedMath.u(a) * 2, UnsignedMath.multiplyFull(a, 2));
		}
	}

	@Test
	public void testMultiplyHigh() {
		Random rand = new Random(44);

		for (int i = 0; i < 1000; i++) {
			BigInteger x = ub(rand.nextLong());
			BigInteger y = ub(rand.nextLong());
			BigInteger z = ub(rand.nextLong())
				.shiftLeft(64)
				.or(ub(rand.nextLong()));

			assertEquals(
				x.shiftRight(32)
					.multiply(y.shiftRight(32))
					.shiftRight(32)
					.intValue(),
				UnsignedMath.multiplyHigh(x.shiftRight(32).intValue(), y.shiftRight(32).intValue())
			);
			assertEquals(
				x.multiply(y)
					.shiftRight(64)
					.longValue(),
				UnsignedMath.multiplyHigh(x.longValue(), y.longValue())
			);
			assertEquals(
				x.multiply(y.shiftRight(32))
					.shiftRight(64)
					.intValue(),
				UnsignedMath.multiplyHigh(x.longValue(), y.shiftRight(32).intValue())
			);
			assertEquals(
				z.multiply(y)
					.shiftRight(128)
					.longValue(),
				UnsignedMath.multiplyHigh(
					z.shiftRight(64)
						.longValue(),
					z.longValue(),
					y.longValue()
				)
			);
		}
	}

	@Test
	public void testReciprocalsOf() {
		assertThrows(IllegalArgumentException.class, () -> UnsignedMath.reciprocalsOf(0));

		Random rand = new Random(44);
		BigInteger max128 = BigInteger.ONE
			.shiftLeft(128)
			.subtract(BigInteger.ONE);

		for (int i = 0; i < 1000; i++) {
			int x;

			do {
				x = rand.nextInt();
			} while (x == 0);

			BigInteger exp128 = max128.divide(ub(x))
				.add(BigInteger.ONE);

			assertArrayEquals(
				new long[] {
					Long.divideUnsigned(~0L, UnsignedMath.u(x)) + 1,
					exp128.shiftRight(64)
						.longValue(),
					exp128.and(ub(~0L))
						.longValue()
				},
				UnsignedMath.reciprocalsOf(x)
			);
		}
	}

	@Test
	public void testDivideAndRemainderReciprocal() {
		Random rand = new Random(44);

		for (int i = 0; i < 1000; i++) {
			int d;

			do {
				d = rand.nextInt();
			} while (d == 0);

			long[] r = UnsignedMath.reciprocalsOf(d);
			long r64 = r[0];
			long r128High = r[1];
			long r128Low = r[2];

			// 32-bit dividend
			assertEquals(0, UnsignedMath.divideReciprocal(0, r64));
			assertEquals(0, UnsignedMath.remainderReciprocal(0, d, r64));

			assertEquals(1, UnsignedMath.divideReciprocal(d, r64));
			assertEquals(0, UnsignedMath.remainderReciprocal(d, d, r64));

			assertEquals(0, UnsignedMath.divideReciprocal(d - 1, r64));
			assertEquals(d - 1, UnsignedMath.remainderReciprocal(d - 1, d, r64));

			assertEquals(Integer.divideUnsigned(~0, d), UnsignedMath.divideReciprocal(~0, r64));
			assertEquals(
				Integer.remainderUnsigned(~0, d),
				UnsignedMath.remainderReciprocal(~0, d, r64)
			);

			// 64-bit dividend
			assertEquals(0L, UnsignedMath.divideReciprocal(0L, r128High, r128Low));
			assertEquals(0L, UnsignedMath.remainderReciprocal(0L, d, r128High, r128Low));

			assertEquals(1L, UnsignedMath.divideReciprocal(UnsignedMath.u(d), r128High, r128Low));
			assertEquals(0, UnsignedMath.remainderReciprocal(
				UnsignedMath.u(d),
				d, r128High, r128Low
			));

			assertEquals(0, UnsignedMath.divideReciprocal(
				UnsignedMath.u(d) - 1,
				r128High, r128Low
			));
			assertEquals(d - 1, UnsignedMath.remainderReciprocal(
				UnsignedMath.u(d) - 1,
				d, r128High, r128Low
			));

			assertEquals(
				Long.divideUnsigned(~0L, UnsignedMath.u(d)),
				UnsignedMath.divideReciprocal(~0L, r128High, r128Low)
			);
			assertEquals(
				Long.remainderUnsigned(~0L, UnsignedMath.u(d)),
				UnsignedMath.u(UnsignedMath.remainderReciprocal(~0L, d, r128High, r128Low))
			);
		}
	}

	@Test
	public void testShoupFactorOf() {
		Random rand = new Random(44);

		for (int i = 0; i < 1000; i++) {
			int n;

			do {
				n = rand.nextInt();
			} while (n == 0 || UnsignedMath.ge(n, 0x80000000));

			long[] nRecip = UnsignedMath.reciprocalsOf(n);
			int w = rand.nextInt();

			assertEquals(
				UnsignedMath.l(Long.divideUnsigned(UnsignedMath.u(w) << 32, UnsignedMath.u(n))),
				UnsignedMath.shoupFactorOf(w, nRecip[1], nRecip[2])
			);
		}
	}

	@Test
	public void testModularArithmetic() {
		Random rand = new Random(44);

		for (int i = 0; i < 1000; i++) {
			int m;

			do {
				m = rand.nextInt();
			} while (m == 0);

			long[] mRecip = UnsignedMath.reciprocalsOf(m);
			int a = Integer.remainderUnsigned(rand.nextInt(), m);
			int b = Integer.remainderUnsigned(rand.nextInt(), m);

			assertEquals(
				(UnsignedMath.u(a) + UnsignedMath.u(b)) % UnsignedMath.u(m),
				UnsignedMath.u(UnsignedMath.addMod(a, b, m))
			);
			assertEquals(
				UnsignedMath.ge(a, b) ? UnsignedMath.u(a - b) :
				(UnsignedMath.u(a) - UnsignedMath.u(b) + UnsignedMath.u(m)),
				UnsignedMath.u(UnsignedMath.subtractMod(a, b, m))
			);
			assertEquals(
				Long.remainderUnsigned(UnsignedMath.multiplyFull(a, b), UnsignedMath.u(m)),
				UnsignedMath.u(UnsignedMath.multiplyMod(a, b, m, mRecip[1], mRecip[2]))
			);
			assertEquals(
				ub(a).modPow(ub(b), ub(m))
					.intValue(),
				UnsignedMath.powMod(a, b, m, mRecip[1], mRecip[2])
			);
		}
	}

	@Test
	public void testFindQuadraticNonResidue() {
		for (int m : new int[] { 1 << 29, 1 << 30, 1 << 31, ~0 }) {
			int p = TestUtil.findLargestPrimeOfForm(1 << 10, 1, 0, m);
			long[] pRecip = UnsignedMath.reciprocalsOf(p);
			int z = UnsignedMath.findQuadraticNonResidue(p, pRecip[1], pRecip[2]);

			// make sure it's a quadratic non-residue
			assertEquals(p - 1, UnsignedMath.powMod(z, (p - 1) >>> 1, p, pRecip[1], pRecip[2]));
			// make sure it's the smallest one
			for (int j = 2; UnsignedMath.lt(j, z); j++)
				assertEquals(1, UnsignedMath.powMod(j, (p - 1) >>> 1, p, pRecip[1], pRecip[2]));
		}
	}

	@Test
	public void testDecomposeTonelliShanksGroupOrder() {
		for (int m : new int[] { 1 << 29, 1 << 30, 1 << 31, ~0 }) {
			int p = TestUtil.findLargestPrimeOfForm(1 << 10, 1, 0, m);
			long qs = UnsignedMath.decomposeTonelliShanksGroupOrder(p);
			int q = UnsignedMath.h(qs);
			int s = UnsignedMath.l(qs);

			assertTrue(s >= 1 && s < 32);
			assertEquals(p - 1, q * (1 << s));
			assertEquals(1, q & 1);
		}
	}

	@Test
	public void testSqrtModExplicit() {
		for (int m : new int[] { 1 << 29, 1 << 30, 1 << 31, ~0 }) {
			int p = TestUtil.findLargestPrimeOfForm(1 << 10, 1, 0, m);
			long[] pRecip = UnsignedMath.reciprocalsOf(p);
			long pRecipHigh = pRecip[1];
			long pRecipLow = pRecip[2];
			long qs = UnsignedMath.decomposeTonelliShanksGroupOrder(p);
			int q = UnsignedMath.h(qs);
			int s = UnsignedMath.l(qs);
			int z = UnsignedMath.findQuadraticNonResidue(p, pRecipHigh, pRecipLow);
			int i = UnsignedMath.sqrtModExplicit(p - 1, p, pRecipHigh, pRecipLow, q, s, z);
			int j = UnsignedMath.sqrtModExplicit(i, p, pRecipHigh, pRecipLow, q, s, z);

			assertEquals(p - 1, UnsignedMath.multiplyMod(i, i, p, pRecipHigh, pRecipLow));
			assertEquals(i, UnsignedMath.multiplyMod(j, j, p, pRecipHigh, pRecipLow));
			assertEquals(p, UnsignedMath.sqrtModExplicit(z, p, pRecipHigh, pRecipLow, q, s, z));
		}
	}

	@Test
	public void testFindPrimitiveRoot() {
		for (int d = 2; d <= 32768; d *= 2) {
			int p = TestUtil.findLargestPrimeOfForm(d, 1, 0, ~0);
			long[] pRecip = UnsignedMath.reciprocalsOf(p);
			long pRecipHigh = pRecip[1];
			long pRecipLow = pRecip[2];
			int root = UnsignedMath.findPrimitiveRoot(d, p, pRecipHigh, pRecipLow);

			assertNotEquals(p, root);
			for (int j = 1; j < d; j++)
				assertNotEquals(1, UnsignedMath.powMod(root, j, p, pRecipHigh, pRecipLow));
			assertEquals(1, UnsignedMath.powMod(root, d, p, pRecipHigh, pRecipLow));
		}
	}
}
