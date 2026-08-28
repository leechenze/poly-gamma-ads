// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.crypt;

import static org.polygamma.android.origin.crypt.UnsignedMath.l;
import static org.polygamma.android.origin.crypt.UnsignedMath.multiplyFull;
import static org.polygamma.android.origin.crypt.UnsignedMath.u;

import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.math.BigInteger;

/**
 * Constants tests.
 */
@RunWith(AndroidJUnit4.class)
public class ConstantsTest {
	// USED BY `generateConstants` gradle task! Don't touch.
	@Test
	public void testGenerateConstants() {
		int p0 = Prime32.P0;
		int p1 = Prime32.P1;
		int p2 = Prime32.P2;
		int[] primes = { p0, p1, p2 };

		for (int i = 0; i < primes.length; i++) {
			int p = primes[i];
			NttPrime32 t = NttPrime32.of(2, p);

			Log.d("GEN", String.format("P%s_BITS=%d", i, NttPrime32.bitsOfPrime(p)));
			Log.d("GEN", String.format("P%s_RECIPROCAL64=0x%xL", i, t.primeReciprocal64));
			Log.d("GEN", String.format("P%s_RECIPROCAL128_HIGH=0x%xL", i, t.primeReciprocal128High));
			Log.d("GEN", String.format("P%s_RECIPROCAL128_LOW=0x%xL", i, t.primeReciprocal128Low));
			Log.d("GEN", String.format("P%s_BARRETT_FACTOR=0x%x", i, t.primeBarrettFactor));
			Log.d("GEN", String.format("P%s_LIMB_COUNT=0x%x", i, t.primeLimbCount));
			Log.d("GEN", String.format("P%s_CAN_USE_FAST_REDUCTION=%s", i, t.canUseFastReduction));
		}

		int p0InvModP1 = BigInteger.valueOf(u(p0))
			.modPow(BigInteger.valueOf(u(p1 - 2)), BigInteger.valueOf(u(p1)))
			.and(BigInteger.valueOf(u(~0)))
			.intValue();
		int p01InvModP2 =
			BigInteger.valueOf(u(l(Long.remainderUnsigned(multiplyFull(p0, p1), u(p2)))))
				.modPow(BigInteger.valueOf(u(p2 - 2)), BigInteger.valueOf(u(p2)))
				.and(BigInteger.valueOf(u(~0)))
				.intValue();

		Log.d("GEN", String.format("P0_INVERSE_MOD_P1=0x%x", p0InvModP1));
		Log.d("GEN", String.format("P01_INVERSE_MOD_P2=0x%x", p01InvModP2));
	}
}
