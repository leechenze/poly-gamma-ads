// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.protobuf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.util.Pair;

import androidx.core.util.Consumer;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.protobuf.CodedInputStream;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Protobuf {@linkplain ProtobufWriter writer} tests.
 */
@RunWith(AndroidJUnit4.class)
public class ProtobufWriterTest {

	private static final int[] INT_TEST_CASES = {
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

	private static final long[] LONG_TEST_CASES = {
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

	private static CodedInputStream prepareProto(Consumer<ProtobufWriter> encoder) {
		ProtobufWriter writer = new ProtobufWriter();

		encoder.accept(writer);
		return CodedInputStream.newInstance(writer.finish());
	}

	@Test
	public void testWriteVarint() {
		ProtobufWriter writer = new ProtobufWriter();
		for (VarintTestCase test : VarintTestCase.INSTANCES) {
			if ((test.decoded & 0xffffffffL) == test.decoded) {
				writer.writeVarint32((int) test.decoded);
				assertEquals(ByteBuffer.wrap(test.encoded), writer.finish());
			}
			writer.writeVarint64(test.decoded);
			assertEquals(ByteBuffer.wrap(test.encoded), writer.finish());
		}
	}

	@Test
	public void testWriteInt32() throws IOException {
		CodedInputStream input = prepareProto(writer -> {
			int num = 1;

			for (int val : INT_TEST_CASES)
				writer.writeInt32(ProtobufField.ofInt32(num++), val);
			writer.writePackedInt32(ProtobufField.ofPackedInt32(num++), INT_TEST_CASES);
		});
		int num = 1;

		for (int val : INT_TEST_CASES) {
			if (val == 0) {
				num++;
				continue;
			}
			assertFalse(input.isAtEnd());
			assertEquals(ProtobufField.ofInt32(num++), input.readTag());
			assertFalse(input.isAtEnd());
			assertEquals(val, input.readUInt32());
		}

		assertFalse(input.isAtEnd());
		assertEquals(ProtobufField.ofPackedInt32(num++), input.readTag());
		assertFalse(input.isAtEnd());

		CodedInputStream packed = CodedInputStream.newInstance(input.readByteBuffer());

		for (int val : INT_TEST_CASES) {
			assertFalse(packed.isAtEnd());
			assertEquals(val, packed.readUInt32());
		}

		assertTrue(packed.isAtEnd());
		assertTrue(input.isAtEnd());
	}

	@Test
	public void testWriteSint32() throws IOException {
		CodedInputStream input = prepareProto(writer -> {
			int num = 1;

			for (int val : INT_TEST_CASES)
				writer.writeSint32(ProtobufField.ofSint32(num++), val);
			writer.writePackedSint32(ProtobufField.ofPackedSint32(num++), INT_TEST_CASES);
		});
		int num = 1;

		for (int val : INT_TEST_CASES) {
			if (val == 0) {
				num++;
				continue;
			}
			assertFalse(input.isAtEnd());
			assertEquals(ProtobufField.ofSint32(num++), input.readTag());
			assertFalse(input.isAtEnd());
			assertEquals(val, input.readSInt32());
		}

		assertFalse(input.isAtEnd());
		assertEquals(ProtobufField.ofPackedSint32(num++), input.readTag());
		assertFalse(input.isAtEnd());

		CodedInputStream packed = CodedInputStream.newInstance(input.readByteBuffer());

		for (int val : INT_TEST_CASES) {
			assertFalse(packed.isAtEnd());
			assertEquals(val, packed.readSInt32());
		}

		assertTrue(packed.isAtEnd());
		assertTrue(input.isAtEnd());
	}

	@Test
	public void testWriteFixed32() throws IOException {
		CodedInputStream input = prepareProto(writer -> {
			int num = 1;

			for (int val : INT_TEST_CASES)
				writer.writeFixed32(ProtobufField.ofFixed32(num++), val);
			writer.writePackedFixed32(ProtobufField.ofPackedFixed32(num++), INT_TEST_CASES);
		});
		int num = 1;

		for (int val : INT_TEST_CASES) {
			if (val == 0) {
				num++;
				continue;
			}
			assertFalse(input.isAtEnd());
			assertEquals(ProtobufField.ofFixed32(num++), input.readTag());
			assertFalse(input.isAtEnd());
			assertEquals(val, input.readFixed32());
		}

		assertFalse(input.isAtEnd());
		assertEquals(ProtobufField.ofPackedFixed32(num++), input.readTag());
		assertFalse(input.isAtEnd());

		CodedInputStream packed = CodedInputStream.newInstance(input.readByteBuffer());

		for (int val : INT_TEST_CASES) {
			assertFalse(packed.isAtEnd());
			assertEquals(val, packed.readFixed32());
		}

		assertTrue(packed.isAtEnd());
		assertTrue(input.isAtEnd());
	}

	@Test
	public void testWriteInt64() throws IOException {
		CodedInputStream input = prepareProto(writer -> {
			int num = 1;

			for (long val : LONG_TEST_CASES)
				writer.writeInt64(ProtobufField.ofInt64(num++), val);
			writer.writePackedInt64(ProtobufField.ofPackedInt64(num++), LONG_TEST_CASES);
		});
		int num = 1;

		for (long val : LONG_TEST_CASES) {
			if (val == 0L) {
				num++;
				continue;
			}
			assertFalse(input.isAtEnd());
			assertEquals(ProtobufField.ofInt64(num++), input.readTag());
			assertFalse(input.isAtEnd());
			assertEquals(val, input.readUInt64());
		}

		assertFalse(input.isAtEnd());
		assertEquals(ProtobufField.ofPackedInt64(num++), input.readTag());
		assertFalse(input.isAtEnd());

		CodedInputStream packed = CodedInputStream.newInstance(input.readByteBuffer());

		for (long val : LONG_TEST_CASES) {
			assertFalse(packed.isAtEnd());
			assertEquals(val, packed.readUInt64());
		}

		assertTrue(packed.isAtEnd());
		assertTrue(input.isAtEnd());
	}

	@Test
	public void testWriteSint64() throws IOException {
		CodedInputStream input = prepareProto(writer -> {
			int num = 1;

			for (long val : LONG_TEST_CASES)
				writer.writeSint64(ProtobufField.ofSint64(num++), val);
			writer.writePackedSint64(ProtobufField.ofPackedSint64(num++), LONG_TEST_CASES);
		});
		int num = 1;

		for (long val : LONG_TEST_CASES) {
			if (val == 0L) {
				num++;
				continue;
			}
			assertFalse(input.isAtEnd());
			assertEquals(ProtobufField.ofSint64(num++), input.readTag());
			assertFalse(input.isAtEnd());
			assertEquals(val, input.readSInt64());
		}

		assertFalse(input.isAtEnd());
		assertEquals(ProtobufField.ofPackedSint64(num++), input.readTag());
		assertFalse(input.isAtEnd());

		CodedInputStream packed = CodedInputStream.newInstance(input.readByteBuffer());

		for (long val : LONG_TEST_CASES) {
			assertFalse(packed.isAtEnd());
			assertEquals(val, packed.readSInt64());
		}

		assertTrue(packed.isAtEnd());
		assertTrue(input.isAtEnd());
	}

	@Test
	public void testWriteFixed64() throws IOException {
		CodedInputStream input = prepareProto(writer -> {
			int num = 1;

			for (long val : LONG_TEST_CASES)
				writer.writeFixed64(ProtobufField.ofFixed64(num++), val);
			writer.writePackedFixed64(ProtobufField.ofPackedFixed64(num++), LONG_TEST_CASES);
		});
		int num = 1;

		for (long val : LONG_TEST_CASES) {
			if (val == 0L) {
				num++;
				continue;
			}
			assertFalse(input.isAtEnd());
			assertEquals(ProtobufField.ofFixed64(num++), input.readTag());
			assertFalse(input.isAtEnd());
			assertEquals(val, input.readFixed64());
		}

		assertFalse(input.isAtEnd());
		assertEquals(ProtobufField.ofPackedFixed64(num++), input.readTag());
		assertFalse(input.isAtEnd());

		CodedInputStream packed = CodedInputStream.newInstance(input.readByteBuffer());

		for (long val : LONG_TEST_CASES) {
			assertFalse(packed.isAtEnd());
			assertEquals(val, packed.readFixed64());
		}

		assertTrue(packed.isAtEnd());
		assertTrue(input.isAtEnd());
	}

	@Test
	public void testWriteWordBitmap() throws IOException {
		int[] vals = {
			0, 3, 5, 7, 9, 11, 13, 15, 17, 19, 21, 23, 25, 27, 29, 31,
			34, 36, 38, 40, 42, 44, 46, 48, 50, 52, 54, 56, 58, 60, 62
		};
		CodedInputStream input = prepareProto(writer -> {
			long exp = 0L;

			for (int val : vals)
				exp |= (1L << val);
			writer.writeWordBitmap(ProtobufField.ofPackedInt32(1), exp, 0);
		});

		assertFalse(input.isAtEnd());
		assertEquals(ProtobufField.ofPackedInt32(1), input.readTag());
		assertFalse(input.isAtEnd());

		CodedInputStream packed = CodedInputStream.newInstance(input.readByteBuffer());

		for (int val : vals) {
			assertFalse(packed.isAtEnd());
			assertEquals(val, packed.readUInt32());
		}
		assertTrue(packed.isAtEnd());
		assertTrue(input.isAtEnd());
	}

	@Test
	public void testWriteBool() throws IOException {
		boolean[] vals = { false, true, false, true, true, false, true };
		CodedInputStream input = prepareProto(writer -> {
			int num = 1;

			for (boolean val : vals)
				writer.writeBool(ProtobufField.ofBool(num++), val);
			writer.writePackedBool(ProtobufField.ofPackedBool(num++), vals);
		});
		int num = 1;

		for (boolean val : vals) {
			if (!val) {
				num++;
				continue;
			}
			assertFalse(input.isAtEnd());
			assertEquals(ProtobufField.ofBool(num++), input.readTag());
			assertFalse(input.isAtEnd());
			assertEquals(val, input.readBool());
		}

		assertFalse(input.isAtEnd());
		assertEquals(ProtobufField.ofPackedBool(num++), input.readTag());
		assertFalse(input.isAtEnd());

		CodedInputStream packed = CodedInputStream.newInstance(input.readByteBuffer());

		for (boolean val : vals) {
			assertFalse(packed.isAtEnd());
			assertEquals(val, packed.readBool());
		}

		assertTrue(packed.isAtEnd());
		assertTrue(input.isAtEnd());
	}

	@Test
	public void testWriteFloat() throws IOException {
		float[] vals = {
			0,
			0xff, Byte.MIN_VALUE, Byte.MAX_VALUE,
			0xffff, Short.MIN_VALUE, Short.MAX_VALUE,
			Integer.MIN_VALUE, Integer.MAX_VALUE,
			Float.MIN_VALUE, -Float.MIN_VALUE,
			Float.MAX_VALUE, -Float.MAX_VALUE
		};
		CodedInputStream input = prepareProto(writer -> {
			int num = 1;

			for (float val : vals)
				writer.writeFloat(ProtobufField.ofFloat(num++), val);
			writer.writePackedFloat(ProtobufField.ofPackedFloat(num++), vals);
		});
		int num = 1;

		for (float val : vals) {
			if (Float.floatToIntBits(val) == 0) {
				num++;
				continue;
			}
			assertFalse(input.isAtEnd());
			assertEquals(ProtobufField.ofFloat(num++), input.readTag());
			assertFalse(input.isAtEnd());
			assertEquals(Float.floatToIntBits(val), Float.floatToIntBits(input.readFloat()));
		}

		assertFalse(input.isAtEnd());
		assertEquals(ProtobufField.ofPackedFloat(num++), input.readTag());
		assertFalse(input.isAtEnd());

		CodedInputStream packed = CodedInputStream.newInstance(input.readByteBuffer());

		for (float val : vals) {
			assertFalse(packed.isAtEnd());
			assertEquals(Float.floatToIntBits(val), Float.floatToIntBits(packed.readFloat()));
		}
		assertTrue(packed.isAtEnd());
		assertTrue(input.isAtEnd());
	}

	@Test
	public void testWriteDouble() throws IOException {
		double[] vals = {
			0,
			0xff, Byte.MIN_VALUE, Byte.MAX_VALUE,
			0xffff, Short.MIN_VALUE, Short.MAX_VALUE,
			Integer.MIN_VALUE, Integer.MAX_VALUE,
			Float.MIN_VALUE, -Float.MIN_VALUE,
			Float.MAX_VALUE, -Float.MAX_VALUE,
			Double.MIN_VALUE, -Double.MIN_VALUE,
			Double.MAX_VALUE, -Double.MAX_VALUE
		};
		CodedInputStream input = prepareProto(writer -> {
			int num = 1;

			for (double val : vals)
				writer.writeDouble(ProtobufField.ofDouble(num++), val);
			writer.writePackedDouble(ProtobufField.ofPackedDouble(num++), vals);
		});
		int num = 1;

		for (double val : vals) {
			if (Double.doubleToLongBits(val) == 0L) {
				num++;
				continue;
			}
			assertFalse(input.isAtEnd());
			assertEquals(ProtobufField.ofDouble(num++), input.readTag());
			assertFalse(input.isAtEnd());
			assertEquals(Double.doubleToLongBits(val), Double.doubleToLongBits(input.readDouble()));
		}

		assertFalse(input.isAtEnd());
		assertEquals(ProtobufField.ofPackedDouble(num++), input.readTag());
		assertFalse(input.isAtEnd());

		CodedInputStream packed = CodedInputStream.newInstance(input.readByteBuffer());

		for (double val : vals) {
			assertFalse(packed.isAtEnd());
			assertEquals(
				Double.doubleToLongBits(val),
				Double.doubleToLongBits(packed.readDouble())
			);
		}
		assertTrue(packed.isAtEnd());
		assertTrue(input.isAtEnd());
	}

	@Test
	public void testWriteString() throws IOException {
		String[] vals = {
			"",
			"abc",
			"defghi",
			"jklmnopqr",
			"stuvwxyz0123"
		};
		CodedInputStream input = prepareProto(writer -> {
			int num = 1;

			for (String val : vals)
				writer.writeString(ProtobufField.ofString(num++), val);
		});
		int num = 1;

		for (String val : vals) {
			if (val.isEmpty()) {
				num++;
				continue;
			}
			assertFalse(input.isAtEnd());
			assertEquals(ProtobufField.ofString(num++), input.readTag());
			assertFalse(input.isAtEnd());
			assertEquals(val, input.readString());
		}
		assertTrue(input.isAtEnd());
	}

	@Test
	public void testWriteStringPair() throws IOException {
		String[] vals = {
			"abc", "",
			"",    "abc",
			"def", "ghi",
			"jkl", "mno",
			"pqr", "stu",
			"vwx", "yz0"
		};
		CodedInputStream input = prepareProto(writer -> {
			int num = 1;

			for (int i = 0; i < (vals.length / 2); i++) {
				String a = vals[i * 2 + 0];
				String b = vals[i * 2 + 1];

				writer.writeStringPair(ProtobufField.ofStringPair(num++), new Pair<>(a, b));
			}
		});
		int num = 1;

		for (int i = 0; i < (vals.length / 2); i++) {
			assertFalse(input.isAtEnd());
			assertEquals(ProtobufField.ofStringPair(num++), input.readTag());
			assertFalse(input.isAtEnd());

			CodedInputStream pair = CodedInputStream.newInstance(input.readByteBuffer());
			String a = "";
			String b = "";

			while (!pair.isAtEnd()) {
				int tag = pair.readTag();

				if (tag == ProtobufField.ofString(1)) {
					a = pair.readString();
				} else if (tag == ProtobufField.ofString(2)) {
					b = pair.readString();
				}
			}
			assertEquals(vals[i * 2 + 0], a);
			assertEquals(vals[i * 2 + 1], b);
		}
		assertTrue(input.isAtEnd());
	}
}
