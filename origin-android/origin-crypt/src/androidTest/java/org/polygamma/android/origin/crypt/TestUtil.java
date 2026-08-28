// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.crypt;

import org.polygamma.android.origin.util.Preconditions;

import java.math.BigInteger;

/**
 * Test utility definitions.
 */
class TestUtil {

	static boolean isPrime(int p) {
		return BigInteger.valueOf(p & 0xffffffffL)
			.isProbablePrime(40);
	}

	static int findLargestPrimeOfForm(int factor, int offset, int min, int max) {
		Preconditions.checkArgument(UnsignedMath.le(min, max) && UnsignedMath.le(offset, max));

		if (factor == 0) {
			Preconditions.checkArgument(
				UnsignedMath.le(min, offset) &&
				UnsignedMath.ge(max, offset) &&
				isPrime(offset)
			);
			return offset;
		}

		int xMin = Integer.divideUnsigned(UnsignedMath.max(min, offset) - offset, factor);

		if (Integer.remainderUnsigned(UnsignedMath.max(min, offset) - offset, factor) != 0)
			xMin++;

		int x = Integer.divideUnsigned(max - offset, factor);

		do {
			int v = factor * x + offset;

			if (isPrime(v))
				return v;
			if (x == xMin)
				throw new IllegalArgumentException("no prime found");
			x--;
		} while (true);
	}

	private TestUtil() {
	}
}
