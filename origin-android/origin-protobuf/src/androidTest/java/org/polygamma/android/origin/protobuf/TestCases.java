// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.protobuf;

import android.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@SuppressWarnings("unchecked")
public class TestCases {

	static final class Varint {
		final long decoded;
		final byte[] encoded;

		private Varint(long decoded, int... encoded) {
			this.decoded = decoded;
			this.encoded = new byte[encoded.length];
			for (int i = 0; i < encoded.length; i++)
				this.encoded[i] = (byte) (encoded[i] & 0xff);
		}
	}

	static final Varint[] VARINT = {
		new Varint(0,              0x00),
		new Varint(1,              0x01),

		new Varint((1L << 7) - 1,  0x7f),
		new Varint((1L << 7),      0x80, 0x01),
		new Varint(0x12cL,         0xac, 0x02),

		new Varint((1L << 14) - 1, 0xff, 0x7f),
		new Varint((1L << 14),     0x80, 0x80, 0x01),

		new Varint((1L << 21) - 1, 0xff, 0xff, 0x7f),
		new Varint((1L << 21),     0x80, 0x80, 0x80, 0x01),

		new Varint((1L << 28) - 1, 0xff, 0xff, 0xff, 0x7f),
		new Varint((1L << 28),     0x80, 0x80, 0x80, 0x80,
			                       0x01),

		new Varint((1L << 35) - 1, 0xff, 0xff, 0xff, 0xff,
		                           0x7f),
		new Varint((1L << 35),     0x80, 0x80, 0x80, 0x80,
			                       0x80, 0x01),

		new Varint((1L << 42) - 1, 0xff, 0xff, 0xff, 0xff,
			                       0xff, 0x7f),
		new Varint((1L << 42),     0x80, 0x80, 0x80, 0x80,
			                       0x80, 0x80, 0x01),

		new Varint((1L << 49) - 1, 0xff, 0xff, 0xff, 0xff,
			                       0xff, 0xff, 0x7f),
		new Varint((1L << 49),     0x80, 0x80, 0x80, 0x80,
			                       0x80, 0x80, 0x80, 0x01),

		new Varint((1L << 56) - 1, 0xff, 0xff, 0xff, 0xff,
			                       0xff, 0xff, 0xff, 0x7f),
		new Varint((1L << 56),     0x80, 0x80, 0x80, 0x80,
			                       0x80, 0x80, 0x80, 0x80,
			                       0x01),

		new Varint((1L << 63) - 1, 0xff, 0xff, 0xff, 0xff,
			                       0xff, 0xff, 0xff, 0xff,
			                       0x7f),
		new Varint((1L << 63),     0x80, 0x80, 0x80, 0x80,
			                       0x80, 0x80, 0x80, 0x80,
			                       0x80, 0x01),

		new Varint(~0L,            0xff, 0xff, 0xff, 0xff,
			                       0xff, 0xff, 0xff, 0xff,
			                       0xff, 0x01)
	};

	static final int[] INT = {
		0,
		0xff, Byte.MIN_VALUE, Byte.MAX_VALUE,
		0xffff, Short.MIN_VALUE, Short.MAX_VALUE,
		Integer.MIN_VALUE, Integer.MAX_VALUE,

		(1 <<  0) - 1, (1 <<  0),
		(1 <<  1) - 1, (1 <<  1),
		(1 <<  2) - 1, (1 <<  2),
		(1 <<  3) - 1, (1 <<  3),
		(1 <<  4) - 1, (1 <<  4),
		(1 <<  5) - 1, (1 <<  5),
		(1 <<  6) - 1, (1 <<  6),
		(1 <<  7) - 1, (1 <<  7),
		(1 <<  8) - 1, (1 <<  8),
		(1 <<  9) - 1, (1 <<  9),
		(1 << 10) - 1, (1 << 10),
		(1 << 11) - 1, (1 << 11),
		(1 << 12) - 1, (1 << 12),
		(1 << 13) - 1, (1 << 13),
		(1 << 14) - 1, (1 << 14),
		(1 << 15) - 1, (1 << 15),
		(1 << 16) - 1, (1 << 16),
		(1 << 17) - 1, (1 << 17),
		(1 << 18) - 1, (1 << 18),
		(1 << 19) - 1, (1 << 19),
		(1 << 20) - 1, (1 << 20),
		(1 << 21) - 1, (1 << 21),
		(1 << 22) - 1, (1 << 22),
		(1 << 23) - 1, (1 << 23),
		(1 << 24) - 1, (1 << 24),
		(1 << 25) - 1, (1 << 25),
		(1 << 26) - 1, (1 << 27),
		(1 << 28) - 1, (1 << 28),
		(1 << 29) - 1, (1 << 29),
		(1 << 30) - 1, (1 << 30),
		(1 << 31) - 1, (1 << 31)
	};

	static final long[] LONG = {
		0,
		0xff, Byte.MIN_VALUE, Byte.MAX_VALUE,
		0xffff, Short.MIN_VALUE, Short.MAX_VALUE,
		0xffffffffL, Integer.MIN_VALUE, Integer.MAX_VALUE,
		Long.MIN_VALUE, Long.MAX_VALUE,

		(1L <<  0) - 1, (1L <<  0),
		(1L <<  1) - 1, (1L <<  1),
		(1L <<  2) - 1, (1L <<  2),
		(1L <<  3) - 1, (1L <<  3),
		(1L <<  4) - 1, (1L <<  4),
		(1L <<  5) - 1, (1L <<  5),
		(1L <<  6) - 1, (1L <<  6),
		(1L <<  7) - 1, (1L <<  7),
		(1L <<  8) - 1, (1L <<  8),
		(1L <<  9) - 1, (1L <<  9),
		(1L << 10) - 1, (1L << 10),
		(1L << 11) - 1, (1L << 11),
		(1L << 12) - 1, (1L << 12),
		(1L << 13) - 1, (1L << 13),
		(1L << 14) - 1, (1L << 14),
		(1L << 15) - 1, (1L << 15),
		(1L << 16) - 1, (1L << 16),
		(1L << 17) - 1, (1L << 17),
		(1L << 18) - 1, (1L << 18),
		(1L << 19) - 1, (1L << 19),
		(1L << 20) - 1, (1L << 20),
		(1L << 21) - 1, (1L << 21),
		(1L << 22) - 1, (1L << 22),
		(1L << 23) - 1, (1L << 23),
		(1L << 24) - 1, (1L << 24),
		(1L << 25) - 1, (1L << 25),
		(1L << 26) - 1, (1L << 27),
		(1L << 28) - 1, (1L << 28),
		(1L << 29) - 1, (1L << 29),
		(1L << 30) - 1, (1L << 30),
		(1L << 31) - 1, (1L << 31),
		(1L << 32) - 1, (1L << 32),
		(1L << 33) - 1, (1L << 33),
		(1L << 34) - 1, (1L << 34),
		(1L << 35) - 1, (1L << 35),
		(1L << 36) - 1, (1L << 36),
		(1L << 37) - 1, (1L << 37),
		(1L << 38) - 1, (1L << 38),
		(1L << 39) - 1, (1L << 39),
		(1L << 40) - 1, (1L << 40),
		(1L << 41) - 1, (1L << 41),
		(1L << 42) - 1, (1L << 42),
		(1L << 43) - 1, (1L << 43),
		(1L << 44) - 1, (1L << 44),
		(1L << 45) - 1, (1L << 45),
		(1L << 46) - 1, (1L << 46),
		(1L << 47) - 1, (1L << 47),
		(1L << 48) - 1, (1L << 48),
		(1L << 49) - 1, (1L << 49),
		(1L << 50) - 1, (1L << 50),
		(1L << 51) - 1, (1L << 51),
		(1L << 52) - 1, (1L << 52),
		(1L << 53) - 1, (1L << 53),
		(1L << 54) - 1, (1L << 54),
		(1L << 55) - 1, (1L << 55),
		(1L << 56) - 1, (1L << 56),
		(1L << 57) - 1, (1L << 57),
		(1L << 58) - 1, (1L << 58),
		(1L << 59) - 1, (1L << 59),
		(1L << 60) - 1, (1L << 60),
		(1L << 61) - 1, (1L << 61),
		(1L << 62) - 1, (1L << 62),
		(1L << 63) - 1, (1L << 63)
	};

	static final boolean[] BOOLEAN = new boolean[128];
	static final float[] FLOAT = new float[128];
	static final double[] DOUBLE = new double[128];
	static final byte[] BYTES = new byte[1024];

	static final String[] STRING = new String[2];
	static final Pair<String, String>[] STRING_PAIR;

	static {
		Random rand = new Random(44);

		rand.nextBytes(BYTES);
		for (int i = 0; i < BOOLEAN.length; i++)
			BOOLEAN[i] = rand.nextBoolean();
		for (int i = 0; i < FLOAT.length; i++)
			FLOAT[i] = rand.nextFloat();
		for (int i = 0; i < DOUBLE.length; i++)
			DOUBLE[i] = rand.nextDouble();

		STRING[0] = "";
		STRING[1] = TestUtil.nextRandomString(new Random(44), 128);

		List<Pair<String, String>> strPairs = new ArrayList<>();

		for (int i = 0; i <= 32; i++) {
			String a = STRING[1].substring(0, i);

			for (int j = 0; j <= 32; j++) {
				String b = STRING[1].substring(0, j);

				strPairs.add(new Pair<>(a, b));
			}
		}
		STRING_PAIR = strPairs.toArray(new Pair[0]);
	}

	private TestCases() {
	}
}
