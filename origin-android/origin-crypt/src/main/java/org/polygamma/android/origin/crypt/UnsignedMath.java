// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.crypt;

import android.os.Build;

import androidx.annotation.IntRange;

import org.polygamma.android.origin.util.Preconditions;

/**
 * Unsigned integer definitions.
 */
public class UnsignedMath {

	/**
	 * Upcast 32-bit integer to 64-bit.
	 *
	 * @param a integer to cast
	 * @return {@code a} cast to 64-bit
	 */
	static long u(int a) {
		return a & 0xffffffffL;
	}

	/**
	 * Extract high order bits of 64-bit integer.
	 *
	 * @param a integer to extract bits of
	 * @return high order bits of {@code a}
	 */
	static int h(long a) {
		return (int) (a >>> 32);
	}

	/**
	 * Extract low order bits of 64-bit integer.
	 *
	 * @param a integer to extract bits of
	 * @return low order bits of {@code a}
	 */
	static int l(long a) {
		return (int) (a & 0xffffffffL);
	}

	/**
	 * Test whether a 32-bit integer is greater than another.
	 *
	 * @param a integer to test
	 * @param b integer to test against
	 * @return {@code true} if, and only if, {@code a} is greater than {@code b}
	 */
	static boolean gt(int a, int b) {
		return Integer.compareUnsigned(a, b) > 0;
	}

	/**
	 * Test whether a 32-bit integer is greater than or equal to another.
	 *
	 * @param a integer to test
	 * @param b integer to test against
	 * @return {@code true} if, and only if, {@code a} is greater than or equal to {@code b}
	 */
	static boolean ge(int a, int b) {
		return Integer.compareUnsigned(a, b) >= 0;
	}

	/**
	 * Test whether a 64-bit integer is greater than or equal to another.
	 *
	 * @param a integer to test
	 * @param b integer to test against
	 * @return {@code true} if, and only if, {@code a} is greater than or equal to {@code b}
	 */
	static boolean ge(long a, long b) {
		return Long.compareUnsigned(a, b) >= 0;
	}

	/**
	 * Test whether a 32-bit integer is less than another.
	 *
	 * @param a integer to test
	 * @param b integer to test against
	 * @return {@code true} if, and only if, {@code a} is less than {@code b}
	 */
	static boolean lt(int a, int b) {
		return Integer.compareUnsigned(a, b) < 0;
	}

	/**
	 * Test whether a 32-bit integer is less than or equal to another.
	 *
	 * @param a integer to test
	 * @param b integer to test against
	 * @return {@code true} if, and only if, {@code a} is less than or equal to {@code b}
	 */
	static boolean le(int a, int b) {
		return Integer.compareUnsigned(a, b) <= 0;
	}

	/**
	 * Test whether a 64-bit integer is less than or equal to another.
	 *
	 * @param a integer to test
	 * @param b integer to test against
	 * @return {@code true} if, and only if, {@code a} is less than or equal to {@code b}
	 */
	static boolean le(long a, long b) {
		return Long.compareUnsigned(a, b) <= 0;
	}

	/**
	 * Test whether a 32-bit integer is a power-of-2.
	 *
	 * @param x integer to test
	 * @return {@code true} if, and only if, {@code x} is non-zero and a power-of-2
	 */
	static boolean isPow2(int x) {
		return x != 0 && (x & (x - 1)) == 0;
	}

	/**
	 * Return minimum of two 32-bit integers.
	 *
	 * @param a first integer
	 * @param b second integer
	 * @return {@code a} if {@code a} is less than {@code b}; otherwise, {@code b}
	 */
	static int min(int a, int b) {
		return lt(a, b) ? a : b;
	}

	/**
	 * Return maximum of two 32-bit integers.
	 *
	 * @param a first integer
	 * @param b second integer
	 * @return {@code a} if {@code a} is greater than {@code b}; otherwise, {@code b}
	 */
	static int max(int a, int b) {
		return gt(a, b) ? a : b;
	}

	/**
	 * Calculate log base 2 of 32-bit integer.
	 *
	 * @param x integer to calculate log base 2 of
	 * @return log base 2 of {@code x} or {@code -1} if {@code x} is {@code 0}
	 */
	static @IntRange(from = -1, to = 31) int log2(int x) {
		return 31 - Integer.numberOfLeadingZeros(x);
	}

	/**
	 * Rotate bits of 32-bit integer leftward.
	 *
	 * @param a integer to rotate bits of
	 * @param n number of bits to rotate
	 * @return {@code a} rotated left {@code n} bits
	 */
	static int rotateLeft(int a, int n) {
		/*
		 * Integer::rotateLeft wasn't optimized on pre Android 6.0
		 *
		 * https://android.googlesource.com/platform/art/+/e295be4a95d7861f6ec179edf6565f58cad747cc%5E2..e295be4a95d7861f6ec179edf6565f58cad747cc/
		 */
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
			return Integer.rotateLeft(a, n);
		return (a << n) | (a >>> (32 - n));
	}

	/**
	 * Rotate bits of 64-bit integer rightward.
	 *
	 * @param a integer to rotate bits of
	 * @param n number of bits to rotate
	 * @return {@code a} rotated right {@code n} bits
	 */
	static long rotateRight(long a, int n) {
		/*
		 * Long::rotateRight wasn't optimized on pre Android 6.0
		 *
		 * https://android.googlesource.com/platform/art/+/e295be4a95d7861f6ec179edf6565f58cad747cc%5E2..e295be4a95d7861f6ec179edf6565f58cad747cc/
		 */
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
			return Long.rotateRight(a, n);
		return (a >>> n) | (a << (64 - n));
	}

	/**
	 * Multiply two 32-bit integers, returning full 64-bit product.
	 *
	 * @param x integer to multiply
	 * @param y integer to multiply by
	 * @return full product
	 */
	static long multiplyFull(int x, int y) {
		return u(x) * u(y);
	}

	/**
	 * Multiply two 32-bit integers, returning high order bits of full 64-bit product.
	 *
	 * @param x integer to multiply
	 * @param y integer to multiply by
	 * @return high order bits of full product
	 */
	static int multiplyHigh(int x, int y) {
		return h(multiplyFull(x, y));
	}

	/**
	 * Multiply two 64-bit integers, returning high order bits of full 128-bit product.
	 *
	 * @param x integer to multiply
	 * @param y integer to multiply by
	 * @return high order bits of full product
	 */
	static long multiplyHigh(long x, long y) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
			return Math.unsignedMultiplyHigh(x, y);
		} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
			long high = Math.multiplyHigh(x, y);

			high += (y & (x >> 63));
			high += (x & (y >> 63));
			return high;
		} else {
			// From Hacker's Delight:
			long zl = multiplyFull(l(x), l(y));
			long t = multiplyFull(h(x), l(y)) + u(h(zl));

			return multiplyFull(h(x), h(y)) +
				u(h(t)) +
				u(h(u(l(t)) + multiplyFull(l(x), h(y))));
		}
	}

	/**
	 * Multiply 64-bit integer by 32-bit integer, returning high order bits of full 96-bit product.
	 *
	 * @param x integer to multiply
	 * @param y integer to multiply by
	 * @return high order bits of full product
	 */
	static int multiplyHigh(long x, int y) {
		return l(multiplyHigh(x, u(y)));
	}

	/**
	 * Multiply 128-bit integer by 64-bit integer, returning high order bits of full 192-bit
	 * product.
	 *
	 * @param xHigh high order bits of integer to multiply
	 * @param xLow low order bits of integer to multiply
	 * @param y integer to multiply by
	 * @return high order bits of full product
	 */
	static long multiplyHigh(long xHigh, long xLow, long y) {
		long lowH = multiplyHigh(xLow, y);
		long highL= xHigh * y;
		long highH = multiplyHigh(xHigh, y);
		long mid = highL + lowH;

		return highH + (Long.compareUnsigned(mid, highL) < 0 ? 1 : 0);
	}

	/**
	 * Calculate 64-bit and 128-bit reciprocals of a 32-bit integer.
	 *
	 * @param x integer to compute reciprocal of
	 * @return tuple of 64-bit, high order 128-bit, and low order 128-bit reciprocals of {@code x}
	 * @throws IllegalArgumentException {@code x} is zero
	 */
	static long[] reciprocalsOf(int x) {
		Preconditions.checkArgument(x != 0);

		long divisor = u(x);
		long qH = Long.divideUnsigned(~0L, divisor);
		long rH = Long.remainderUnsigned(~0L, divisor);
		long dLH = (rH << 32) | 0xffffffffL;
		long qLH = Long.divideUnsigned(dLH, divisor);
		long rLH = Long.remainderUnsigned(dLH, divisor);
		long dLL = (rLH << 32) | 0xffffffffL;
		long qLL = Long.divideUnsigned(dLL, divisor);
		long qL = (qLH << 32) | qLL;

		long sRecip = qH + 1;
		long dRecipH = qH;
		long dRecipL = qL;

		if (++dRecipL == 0)
			++dRecipH;
		return new long[] { sRecip, dRecipH, dRecipL };
	}

	/**
	 * Calculate quotient of 32-bit dividend and divisor through reciprocal multiplication.
	 *
	 * @param n dividend
	 * @param dRecip 64-bit reciprocal of divisor
	 * @return quotient
	 */
	static int divideReciprocal(int n, long dRecip) {
		return multiplyHigh(dRecip, n);
	}

	/**
	 * Calculate remainder of 32-bit dividend and divisor through reciprocal multiplication.
	 *
	 * @param n dividend
	 * @param d divisor
	 * @param dRecip 64-bit reciprocal of {@code d}
	 * @return remainder
	 */
	static int remainderReciprocal(int n, int d, long dRecip) {
		return multiplyHigh(dRecip * u(n), d);
	}

	/**
	 * Calculate quotient of 64-bit dividend and 32-bit divisor through reciprocal multiplication.
	 *
	 * @param n dividend
	 * @param dRecipHigh high order bits of 128-bit reciprocal of divisor
	 * @param dRecipLow low order bits of 128-bit reciprocal of divisor
	 * @return quotient
	 */
	static long divideReciprocal(long n, long dRecipHigh, long dRecipLow) {
		return multiplyHigh(dRecipHigh, dRecipLow, n);
	}

	/**
	 * Calculate remainder of 64-bit dividend and 32-bit divisor through reciprocal multiplication.
	 *
	 * @param n dividend
	 * @param d divisor
	 * @param dRecipHigh high order bits of 128-bit reciprocal of {@code d}
	 * @param dRecipLow low order bits of 128-bit reciprocal of {@code d}
	 * @return remainder
	 */
	static int remainderReciprocal(long n, int d, long dRecipHigh, long dRecipLow) {
		long low = dRecipLow * n;
		long lowHigh = multiplyHigh(dRecipLow, n);
		long highLow = dRecipHigh * n;
		long high = highLow + lowHigh;

		return l(multiplyHigh(high, low, u(d)));
	}

	/**
	 * Divide two unsigned 64-bit integers, rounding quotient to nearest whole integer.
	 *
	 * @param n dividend
	 * @param d divisor
	 * @return resulting quotient
	 */
	static long divideRound(long n, long d) {
		long q = Long.divideUnsigned(n, d);

		return ge(Long.remainderUnsigned(n, d), d >>> 1) ? q + 1 : q;
	}

	/**
	 * Divide two unsigned 32-bit integers, rounding up.
	 * <p>The quotient returned will overflow if {@code n + d} overflows a 32-bit integer.
	 *
	 * @param n dividend
	 * @param d divisor
	 * @return resulting quotient
	 */
	public static int divideCeil(int n, int d) {
		return (n + d - 1) / d;
	}

	/**
	 * Compute Shoup factor of a 32-bit integer through reciprocal multiplication.
	 *
	 * @param x integer to compute Shoup factor of
	 * @param dRecipHigh high order bits of 128-bit reciprocal of modulus
	 * @param dRecipLow low order bits of 128-bit reciprocal of modulus
	 * @return Shoup factor of {@code x}
	 */
	static int shoupFactorOf(int x, long dRecipHigh, long dRecipLow) {
		return l(divideReciprocal(u(x) << 32, dRecipHigh, dRecipLow));
	}

	/**
	 * Perform modular addition with 32-bit inputs.
	 *
	 * @param a integer to add to
	 * @param b integer to add
	 * @param m modulus boundary
	 * @return {@code (a + b) (mod m)}
	 */
	static int addMod(int a, int b, int m) {
		int n = m - b;

		return ge(a, n) ? (a - n) : (a + b);
	}

	/**
	 * Perform modular subtraction with 32-bit inputs.
	 *
	 * @param a integer to add to
	 * @param b integer to add
	 * @param m modulus boundary
	 * @return {@code (a - b) (mod m)}
	 */
	static int subtractMod(int a, int b, int m) {
		return ge(a, b) ? (a - b) : (a + (m - b));
	}

	/**
	 * Perform modular multiplication with 32-bit inputs.
	 *
	 * @param a integer to add to
	 * @param b integer to add
	 * @param m modulus boundary
	 * @return {@code (a * b) (mod m)}
	 */
	static int multiplyMod(int a, int b, int m, long mRecipHigh, long mRecipLow) {
		return remainderReciprocal(multiplyFull(a, b), m, mRecipHigh, mRecipLow);
	}

	/**
	 * Perform modular multiplication with 32-bit inputs, using 64-bit multiplication and division.
	 *
	 * @param a integer to add to
	 * @param b integer to add
	 * @param m modulus boundary
	 * @return {@code (a * b) (mod m)}
	 */
	static int multiplyMod(int a, int b, int m) {
		return l(Long.remainderUnsigned(multiplyFull(a, b), u(m)));
	}

	/**
	 * Calculate modular exponential of 32-bit integer.
	 *
	 * @param b integer to multiply
	 * @param e exponent to raise to
	 * @param m modulus boundary
	 * @param mRecipHigh high order bits of 128-bit reciprocal of {@code m}
	 * @param mRecipLow low order bits of 128-bit reciprocal of {@code m}
	 * @return {@code b^{e} (mod m)}
	 */
	static int powMod(int b, int e, int m, long mRecipHigh, long mRecipLow) {
		if (e == 0)
			return 1;

		int x = b;
		int y = 1;

		do {
			if ((e & 1) == 1)
				y = multiplyMod(x, y, m, mRecipHigh, mRecipLow);
			x = multiplyMod(x, x, m, mRecipHigh, mRecipLow);
			e >>>= 1;
		} while (gt(e, 1));
		return multiplyMod(x, y, m, mRecipHigh, mRecipLow);
	}

	/**
	 * Find quadratic non-residue of a 32-bit prime.
	 *
	 * @param p prime to find quadratic non-residue of
	 * @param pRecipHigh high order bits of 128-bit reciprocal of {@code p}
	 * @param pRecipLow low order bits of 128-bit reciprocal of {@code p}
	 * @return quadratic non-residue of {@code p} if found; otherwise, {@code 0}
	 */
	static int findQuadraticNonResidue(int p, long pRecipHigh, long pRecipLow) {
		for (int n = 2; lt(n, p); n++) {
			if (powMod(n, (p - 1) >>> 1, p, pRecipHigh, pRecipLow) == (p - 1))
				return n;
		}
		return 0;
	}

	/**
	 * Calculate odd component {@code q} and maximum power-of-2 factor {@code s} of a 32-bit prime.
	 * <p>Result is undefined when {@code p} is not a prime.
	 *
	 * @param p prime to decompose
	 * @return tuple of {@code q} and {@code s} in high and low -order bits, respectively
	 */
	static long decomposeTonelliShanksGroupOrder(int p) {
		int t = p - 1;
		int s = Integer.numberOfTrailingZeros(t);
		int q = t >>> s;

		return (u(q) << 32) | u(s);
	}

	/**
	 * Find modular square root of 32-bit integer using Tonelli-Shanks algorithm with precomputed
	 * parameters.
	 *
	 * @param n integer to compute square root of
	 * @param p prime modulus
	 * @param pRecipHigh high order bits of 128-bit reciprocal of {@code p}
	 * @param pRecipLow low order bits of 128-bit reciprocal of {@code p}
	 * @param pQ odd component of {@code p - 1}
	 * @param pS power of 2 in {@code p - 1}
	 * @param pZ quadratic non-residue generator of {@code p}
	 * @return modular square root of {@code n} or {@code p} is no such root exists
	 */
	static int
	sqrtModExplicit(int n, int p, long pRecipHigh, long pRecipLow, int pQ, int pS, int pZ) {
		int c = powMod(pZ, pQ, p, pRecipHigh, pRecipLow);
		int t = powMod(n, pQ, p, pRecipHigh, pRecipLow);
		int r = powMod(n, (pQ >>> 1) + (pQ & 1), p, pRecipHigh, pRecipLow);

		while (true) {
			if (t == 0)
				return 0;
			if (t == 1)
				return r;

			int i = 0;

			for (int tPow = t; lt(i, pS);) {
				tPow = multiplyMod(tPow, tPow, p, pRecipHigh, pRecipLow);
				i++;
				if (tPow == 1)
					break;
			}

			if (i == pS)
				return p;

			int b = powMod(c, 1 << (pS - i - 1), p, pRecipHigh, pRecipLow);

			pS = i;
			c = multiplyMod(b, b, p, pRecipHigh, pRecipLow);
			t = multiplyMod(t, c, p, pRecipHigh, pRecipLow);
			r = multiplyMod(r, b, p, pRecipHigh, pRecipLow);
		}
	}

	/**
	 * Find generator for a cyclic subgroup of a given order within the multiplicative group of
	 * 32-bit integers modulo a given prime.
	 *
	 * @param degree order of element to find
	 * @param p odd prime modulus
	 * @param pRecipHigh high order bits of 128-bit reciprocal of {@code p}
	 * @param pRecipLow low order bits of 128-bit reciprocal of {@code p}
	 * @return primitive {@code degree}-th root of unity or {@code p} if no such root exists
	 */
	static int findPrimitiveRoot(int degree, int p, long pRecipHigh, long pRecipLow) {
		if (le(degree, 1) || !isPow2(degree))
			return p;

		long qs = decomposeTonelliShanksGroupOrder(p);
		int q = h(qs);
		int s = l(qs);
		int z = findQuadraticNonResidue(p, pRecipHigh, pRecipLow);

		if (z == 0)
			return p;

		int root = p - 1;

		for (int i = 0, n = Integer.numberOfTrailingZeros(degree) - 1; i < n; i++) {
			root = sqrtModExplicit(root, p, pRecipHigh, pRecipLow, q, s, z);
			if (root == p)
				return p;
		}
		return root;
	}

	private UnsignedMath() {
	}
}
