// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.protobuf;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.util.Pair;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.protobuf.CodedOutputStream;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;

/**
 * Protobuf {@linkplain ProtobufReader reader} tests.
 */
@RunWith(AndroidJUnit4.class)
public class ProtobufReaderTest {

	/**
	 * Function for encoding to a Protobuf coded stream.
	 */
	@FunctionalInterface
	private interface EncodeProtobuf {
		/**
		 * Encode to Protobuf.
		 *
		 * @param out stream to encode into
		 * @throws IOException I/O error encountered
		 */
		void encode(CodedOutputStream out) throws IOException;
	}

	private static ByteBuffer prepareProto(EncodeProtobuf encoder) {
		try (ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
			CodedOutputStream proto = CodedOutputStream.newInstance(bytes);

			encoder.encode(proto);
			proto.flush();
			bytes.flush();
			return ByteBuffer.wrap(bytes.toByteArray());
		} catch (IOException cause) {
			throw new AssertionError(cause);
		}
	}

	@Test
	public void testReadVarint() {
		for (VarintTestCase test : VarintTestCase.INSTANCES) {
			ProtobufReader reader = new ProtobufReader(ByteBuffer.wrap(test.encoded));

			if ((test.decoded & 0xffffffffL) == test.decoded) {
				assertEquals((int) test.decoded, reader.readVarint32());
				reader = new ProtobufReader(ByteBuffer.wrap(test.encoded));
			}
			assertEquals(test.decoded, reader.readVarint64());
		}
	}

	@Test
	public void testReadInt32() {
		int[] vals = {
			0,
			0xff, Byte.MIN_VALUE, Byte.MAX_VALUE,
			0xffff, Short.MIN_VALUE, Short.MAX_VALUE,
			Integer.MIN_VALUE, Integer.MAX_VALUE
		};
		ProtobufReader reader = new ProtobufReader(prepareProto(proto -> {
			int num = 1;

			for (int i = 0; i < 32; i++) {
				proto.writeUInt32(num++, (1 << i) - 1);
				proto.writeUInt32(num++, (1 << i));
			}
			for (int val : vals)
				proto.writeUInt32(num++, val);
			proto.writeByteBuffer(num++, prepareProto(packed -> {
				for (int i = 0; i < 32; i++) {
					packed.writeUInt32NoTag((1 << i) - 1);
					packed.writeUInt32NoTag((1 << i));
				}
				for (int val : vals)
					packed.writeUInt32NoTag(val);
			}));
		}));
		int num = 1;

		for (int i = 0; i < 32; i++) {
			assertTrue(reader.hasRemaining());
			assertEquals(ProtobufField.ofInt32(num++), reader.readTag());
			assertTrue(reader.hasRemaining());
			assertEquals((1 << i) - 1, reader.readInt32());

			assertTrue(reader.hasRemaining());
			assertEquals(ProtobufField.ofInt32(num++), reader.readTag());
			assertTrue(reader.hasRemaining());
			assertEquals((1 << i), reader.readInt32());
		}
		for (int val : vals) {
			assertTrue(reader.hasRemaining());
			assertEquals(ProtobufField.ofInt32(num++), reader.readTag());
			assertTrue(reader.hasRemaining());
			assertEquals(val, reader.readInt32());
		}

		assertTrue(reader.hasRemaining());
		assertEquals(ProtobufField.ofPackedInt32(num++), reader.readTag());
		assertTrue(reader.hasRemaining());

		IntBuffer got = IntBuffer.wrap(reader.readPackedInt32());

		for (int i = 0; i < 32; i++) {
			assertTrue(got.hasRemaining());
			assertEquals((1 << i) - 1, got.get());

			assertTrue(got.hasRemaining());
			assertEquals((1 << i), got.get());
		}
		for (int val : vals) {
			assertTrue(got.hasRemaining());
			assertEquals(val, got.get());
		}

		assertFalse(got.hasRemaining());
		assertFalse(reader.hasRemaining());
	}

	@Test
	public void testReadSint32() {
		int[] vals = {
			0,
			0xff, Byte.MIN_VALUE, Byte.MAX_VALUE,
			0xffff, Short.MIN_VALUE, Short.MAX_VALUE,
			Integer.MIN_VALUE, Integer.MAX_VALUE
		};
		ProtobufReader reader = new ProtobufReader(prepareProto(proto -> {
			int num = 1;

			for (int i = 0; i < 32; i++) {
				proto.writeSInt32(num++, (1 << i) - 1);
				proto.writeSInt32(num++, (1 << i));
			}
			for (int val : vals)
				proto.writeSInt32(num++, val);
			proto.writeByteBuffer(num++, prepareProto(packed -> {
				for (int i = 0; i < 32; i++) {
					packed.writeSInt32NoTag((1 << i) - 1);
					packed.writeSInt32NoTag((1 << i));
				}
				for (int val : vals)
					packed.writeSInt32NoTag(val);
			}));
		}));
		int num = 1;

		for (int i = 0; i < 32; i++) {
			assertTrue(reader.hasRemaining());
			assertEquals(ProtobufField.ofSint32(num++), reader.readTag());
			assertTrue(reader.hasRemaining());
			assertEquals((1 << i) - 1, reader.readSint32());

			assertTrue(reader.hasRemaining());
			assertEquals(ProtobufField.ofSint32(num++), reader.readTag());
			assertTrue(reader.hasRemaining());
			assertEquals((1 << i), reader.readSint32());
		}
		for (int val : vals) {
			assertTrue(reader.hasRemaining());
			assertEquals(ProtobufField.ofSint32(num++), reader.readTag());
			assertTrue(reader.hasRemaining());
			assertEquals(val, reader.readSint32());
		}

		assertTrue(reader.hasRemaining());
		assertEquals(ProtobufField.ofPackedSint32(num++), reader.readTag());
		assertTrue(reader.hasRemaining());

		IntBuffer got = IntBuffer.wrap(reader.readPackedSint32());

		for (int i = 0; i < 32; i++) {
			assertTrue(got.hasRemaining());
			assertEquals((1 << i) - 1, got.get());

			assertTrue(got.hasRemaining());
			assertEquals((1 << i), got.get());
		}
		for (int val : vals) {
			assertTrue(got.hasRemaining());
			assertEquals(val, got.get());
		}

		assertFalse(got.hasRemaining());
		assertFalse(reader.hasRemaining());
	}

	@Test
	public void testReadFixed32() {
		int[] vals = {
			0,
			0xff, Byte.MIN_VALUE, Byte.MAX_VALUE,
			0xffff, Short.MIN_VALUE, Short.MAX_VALUE,
			Integer.MIN_VALUE, Integer.MAX_VALUE
		};
		ProtobufReader reader = new ProtobufReader(prepareProto(proto -> {
			int num = 1;

			for (int i = 0; i < 32; i++) {
				proto.writeFixed32(num++, (1 << i) - 1);
				proto.writeFixed32(num++, (1 << i));
			}
			for (int val : vals)
				proto.writeFixed32(num++, val);
			proto.writeByteBuffer(num++, prepareProto(packed -> {
				for (int i = 0; i < 32; i++) {
					packed.writeFixed32NoTag((1 << i) - 1);
					packed.writeFixed32NoTag((1 << i));
				}
				for (int val : vals)
					packed.writeFixed32NoTag(val);
			}));
		}));
		int num = 1;

		for (int i = 0; i < 32; i++) {
			assertTrue(reader.hasRemaining());
			assertEquals(ProtobufField.ofFixed32(num++), reader.readTag());
			assertTrue(reader.hasRemaining());
			assertEquals((1 << i) - 1, reader.readFixed32());

			assertTrue(reader.hasRemaining());
			assertEquals(ProtobufField.ofFixed32(num++), reader.readTag());
			assertTrue(reader.hasRemaining());
			assertEquals((1 << i), reader.readFixed32());
		}
		for (int val : vals) {
			assertTrue(reader.hasRemaining());
			assertEquals(ProtobufField.ofFixed32(num++), reader.readTag());
			assertTrue(reader.hasRemaining());
			assertEquals(val, reader.readFixed32());
		}

		assertTrue(reader.hasRemaining());
		assertEquals(ProtobufField.ofPackedFixed32(num++), reader.readTag());
		assertTrue(reader.hasRemaining());

		IntBuffer got = IntBuffer.wrap(reader.readPackedFixed32());

		for (int i = 0; i < 32; i++) {
			assertTrue(got.hasRemaining());
			assertEquals((1 << i) - 1, got.get());

			assertTrue(got.hasRemaining());
			assertEquals((1 << i), got.get());
		}
		for (int val : vals) {
			assertTrue(got.hasRemaining());
			assertEquals(val, got.get());
		}

		assertFalse(got.hasRemaining());
		assertFalse(reader.hasRemaining());
	}

	@Test
	public void testReadInt64() {
		long[] vals = {
			0,
			0xff, Byte.MIN_VALUE, Byte.MAX_VALUE,
			0xffff, Short.MIN_VALUE, Short.MAX_VALUE,
			0xffffffffL, Integer.MIN_VALUE, Integer.MAX_VALUE,
			Long.MIN_VALUE, Long.MAX_VALUE
		};
		ProtobufReader reader = new ProtobufReader(prepareProto(proto -> {
			int num = 1;

			for (int i = 0; i < 64; i++) {
				proto.writeUInt64(num++, (1L << i) - 1);
				proto.writeUInt64(num++, (1L << i));
			}
			for (long val : vals)
				proto.writeUInt64(num++, val);
			proto.writeByteBuffer(num++, prepareProto(packed -> {
				for (int i = 0; i < 64; i++) {
					packed.writeUInt64NoTag((1L << i) - 1);
					packed.writeUInt64NoTag((1L << i));
				}
				for (long val : vals)
					packed.writeUInt64NoTag(val);
			}));
		}));
		int num = 1;

		for (int i = 0; i < 64; i++) {
			assertTrue(reader.hasRemaining());
			assertEquals(ProtobufField.ofInt64(num++), reader.readTag());
			assertTrue(reader.hasRemaining());
			assertEquals((1L << i) - 1, reader.readInt64());

			assertTrue(reader.hasRemaining());
			assertEquals(ProtobufField.ofInt64(num++), reader.readTag());
			assertTrue(reader.hasRemaining());
			assertEquals((1L << i), reader.readInt64());
		}
		for (long val : vals) {
			assertTrue(reader.hasRemaining());
			assertEquals(ProtobufField.ofInt64(num++), reader.readTag());
			assertTrue(reader.hasRemaining());
			assertEquals(val, reader.readInt64());
		}

		assertTrue(reader.hasRemaining());
		assertEquals(ProtobufField.ofPackedInt64(num++), reader.readTag());
		assertTrue(reader.hasRemaining());

		LongBuffer got = LongBuffer.wrap(reader.readPackedInt64());

		for (int i = 0; i < 64; i++) {
			assertTrue(got.hasRemaining());
			assertEquals((1L << i) - 1, got.get());

			assertTrue(got.hasRemaining());
			assertEquals((1L << i), got.get());
		}
		for (long val : vals) {
			assertTrue(got.hasRemaining());
			assertEquals(val, got.get());
		}

		assertFalse(got.hasRemaining());
		assertFalse(reader.hasRemaining());
	}

	@Test
	public void testReadSint64() {
		long[] vals = {
			0,
			0xff, Byte.MIN_VALUE, Byte.MAX_VALUE,
			0xffff, Short.MIN_VALUE, Short.MAX_VALUE,
			0xffffffffL, Integer.MIN_VALUE, Integer.MAX_VALUE,
			Long.MIN_VALUE, Long.MAX_VALUE
		};
		ProtobufReader reader = new ProtobufReader(prepareProto(proto -> {
			int num = 1;

			for (int i = 0; i < 64; i++) {
				proto.writeSInt64(num++, (1L << i) - 1);
				proto.writeSInt64(num++, (1L << i));
			}
			for (long val : vals)
				proto.writeSInt64(num++, val);
			proto.writeByteBuffer(num++, prepareProto(packed -> {
				for (int i = 0; i < 64; i++) {
					packed.writeSInt64NoTag((1L << i) - 1);
					packed.writeSInt64NoTag((1L << i));
				}
				for (long val : vals)
					packed.writeSInt64NoTag(val);
			}));
		}));
		int num = 1;

		for (int i = 0; i < 64; i++) {
			assertTrue(reader.hasRemaining());
			assertEquals(ProtobufField.ofSint64(num++), reader.readTag());
			assertTrue(reader.hasRemaining());
			assertEquals((1L << i) - 1, reader.readSint64());

			assertTrue(reader.hasRemaining());
			assertEquals(ProtobufField.ofSint64(num++), reader.readTag());
			assertTrue(reader.hasRemaining());
			assertEquals((1L << i), reader.readSint64());
		}
		for (long val : vals) {
			assertTrue(reader.hasRemaining());
			assertEquals(ProtobufField.ofSint64(num++), reader.readTag());
			assertTrue(reader.hasRemaining());
			assertEquals(val, reader.readSint64());
		}

		assertTrue(reader.hasRemaining());
		assertEquals(ProtobufField.ofPackedSint64(num++), reader.readTag());
		assertTrue(reader.hasRemaining());

		LongBuffer got = LongBuffer.wrap(reader.readPackedSint64());

		for (int i = 0; i < 64; i++) {
			assertTrue(got.hasRemaining());
			assertEquals((1L << i) - 1, got.get());

			assertTrue(got.hasRemaining());
			assertEquals((1L << i), got.get());
		}
		for (long val : vals) {
			assertTrue(got.hasRemaining());
			assertEquals(val, got.get());
		}

		assertFalse(got.hasRemaining());
		assertFalse(reader.hasRemaining());
	}

	@Test
	public void testReadFixed64() {
		long[] vals = {
			0,
			0xff, Byte.MIN_VALUE, Byte.MAX_VALUE,
			0xffff, Short.MIN_VALUE, Short.MAX_VALUE,
			0xffffffffL, Integer.MIN_VALUE, Integer.MAX_VALUE,
			Long.MIN_VALUE, Long.MAX_VALUE
		};
		ProtobufReader reader = new ProtobufReader(prepareProto(proto -> {
			int num = 1;

			for (int i = 0; i < 64; i++) {
				proto.writeFixed64(num++, (1L << i) - 1);
				proto.writeFixed64(num++, (1L << i));
			}
			for (long val : vals)
				proto.writeFixed64(num++, val);
			proto.writeByteBuffer(num++, prepareProto(packed -> {
				for (int i = 0; i < 64; i++) {
					packed.writeFixed64NoTag((1L << i) - 1);
					packed.writeFixed64NoTag((1L << i));
				}
				for (long val : vals)
					packed.writeFixed64NoTag(val);
			}));
		}));
		int num = 1;

		for (int i = 0; i < 64; i++) {
			assertTrue(reader.hasRemaining());
			assertEquals(ProtobufField.ofFixed64(num++), reader.readTag());
			assertTrue(reader.hasRemaining());
			assertEquals((1L << i) - 1, reader.readFixed64());

			assertTrue(reader.hasRemaining());
			assertEquals(ProtobufField.ofFixed64(num++), reader.readTag());
			assertTrue(reader.hasRemaining());
			assertEquals((1L << i), reader.readFixed64());
		}
		for (long val : vals) {
			assertTrue(reader.hasRemaining());
			assertEquals(ProtobufField.ofFixed64(num++), reader.readTag());
			assertTrue(reader.hasRemaining());
			assertEquals(val, reader.readFixed64());
		}

		assertTrue(reader.hasRemaining());
		assertEquals(ProtobufField.ofPackedFixed64(num++), reader.readTag());
		assertTrue(reader.hasRemaining());

		LongBuffer got = LongBuffer.wrap(reader.readPackedFixed64());

		for (int i = 0; i < 64; i++) {
			assertTrue(got.hasRemaining());
			assertEquals((1L << i) - 1, got.get());

			assertTrue(got.hasRemaining());
			assertEquals((1L << i), got.get());
		}
		for (long val : vals) {
			assertTrue(got.hasRemaining());
			assertEquals(val, got.get());
		}

		assertFalse(got.hasRemaining());
		assertFalse(reader.hasRemaining());
	}

	@Test
	public void testReadWordBitmap() {
		int[] vals = {
			0, 3, 5, 7, 9, 11, 13, 15, 17, 19, 21, 23, 25, 27, 29, 31,
			34, 36, 38, 40, 42, 44, 46, 48, 50, 52, 54, 56, 58, 60, 62
		};
		ProtobufReader reader = new ProtobufReader(prepareProto(proto -> {
			proto.writeByteBuffer(1, prepareProto(packed -> {
				for (int val : vals)
					packed.writeUInt32NoTag(val);
			}));
		}));

		assertTrue(reader.hasRemaining());
		assertEquals(ProtobufField.ofPackedInt32(1), reader.readTag());
		assertTrue(reader.hasRemaining());

		long got = reader.readWordBitmap(0);
		long exp = 0L;

		for (int val : vals)
			exp |= (1L << val);
		assertEquals(exp, got);
		assertFalse(reader.hasRemaining());
	}

	@Test
	public void testReadBool() {
		boolean[] vals = { false, true, false, true, true, false, true };
		ProtobufReader reader = new ProtobufReader(prepareProto(proto -> {
			int num = 1;

			for (boolean val : vals)
				proto.writeBool(num++, val);
			proto.writeByteBuffer(num++, prepareProto(packed -> {
				for (boolean val : vals)
					packed.writeBoolNoTag(val);
			}));
		}));
		int num = 1;

		for (boolean val : vals) {
			assertTrue(reader.hasRemaining());
			assertEquals(ProtobufField.ofBool(num++), reader.readTag());
			assertTrue(reader.hasRemaining());
			assertEquals(val, reader.readBool());
		}

		assertTrue(reader.hasRemaining());
		assertEquals(ProtobufField.ofPackedBool(num++), reader.readTag());
		assertTrue(reader.hasRemaining());

		assertArrayEquals(vals, reader.readPackedBool());

		assertFalse(reader.hasRemaining());
	}

	@Test
	public void testReadFloat() {
		float[] vals = {
			0,
			0xff, Byte.MIN_VALUE, Byte.MAX_VALUE,
			0xffff, Short.MIN_VALUE, Short.MAX_VALUE,
			Integer.MIN_VALUE, Integer.MAX_VALUE,
			Float.MIN_VALUE, -Float.MIN_VALUE,
			Float.MAX_VALUE, -Float.MAX_VALUE
		};
		ProtobufReader reader = new ProtobufReader(prepareProto(proto -> {
			int num = 1;

			for (float val : vals)
				proto.writeFloat(num++, val);
			proto.writeByteBuffer(num++, prepareProto(packed -> {
				for (float val : vals)
					packed.writeFloatNoTag(val);
			}));
		}));
		int num = 1;

		for (float val : vals) {
			assertTrue(reader.hasRemaining());
			assertEquals(ProtobufField.ofFloat(num++), reader.readTag());
			assertTrue(reader.hasRemaining());
			assertEquals(Float.floatToIntBits(val), Float.floatToIntBits(reader.readFloat()));
		}

		assertTrue(reader.hasRemaining());
		assertEquals(ProtobufField.ofPackedFloat(num++), reader.readTag());
		assertTrue(reader.hasRemaining());

		FloatBuffer got = FloatBuffer.wrap(reader.readPackedFloat());

		for (float val : vals) {
			assertTrue(got.hasRemaining());
			assertEquals(Float.floatToIntBits(val), Float.floatToIntBits(got.get()));
		}

		assertFalse(got.hasRemaining());
		assertFalse(reader.hasRemaining());
	}

	@Test
	public void testReadDouble() {
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
		ProtobufReader reader = new ProtobufReader(prepareProto(proto -> {
			int num = 1;

			for (double val : vals)
				proto.writeDouble(num++, val);
			proto.writeByteBuffer(num++, prepareProto(packed -> {
				for (double val : vals)
					packed.writeDoubleNoTag(val);
			}));
		}));
		int num = 1;

		for (double val : vals) {
			assertTrue(reader.hasRemaining());
			assertEquals(ProtobufField.ofDouble(num++), reader.readTag());
			assertTrue(reader.hasRemaining());
			assertEquals(
				Double.doubleToLongBits(val),
				Double.doubleToLongBits(reader.readDouble())
			);
		}

		assertTrue(reader.hasRemaining());
		assertEquals(ProtobufField.ofPackedDouble(num++), reader.readTag());
		assertTrue(reader.hasRemaining());

		DoubleBuffer got = DoubleBuffer.wrap(reader.readPackedDouble());

		for (double val : vals) {
			assertTrue(got.hasRemaining());
			assertEquals(Double.doubleToLongBits(val), Double.doubleToLongBits(got.get()));
		}

		assertFalse(got.hasRemaining());
		assertFalse(reader.hasRemaining());
	}

	@Test
	public void testReadString() {
		String[] vals = {
			"",
			"abc",
			"defghi",
			"jklmnopqr",
			"stuvwxyz0123"
		};
		ProtobufReader reader = new ProtobufReader(prepareProto(proto -> {
			int num = 1;

			for (String val : vals)
				proto.writeString(num++, val);
		}));
		int num = 1;

		for (String val : vals) {
			assertTrue(reader.hasRemaining());
			assertEquals(ProtobufField.ofString(num++), reader.readTag());
			assertTrue(reader.hasRemaining());
			assertEquals(val, reader.readString());
		}
		assertFalse(reader.hasRemaining());
	}

	@Test
	public void testReadStringPair() {
		String[] vals = {
			"abc", "",
			"",    "abc",
			"def", "ghi",
			"jkl", "mno",
			"pqr", "stu",
			"vwx", "yz0"
		};
		ProtobufReader reader = new ProtobufReader(prepareProto(proto -> {
			int num = 1;

			for (int i = 0; i < (vals.length / 2); i++) {
				String a = vals[i * 2 + 0];
				String b = vals[i * 2 + 1];

				proto.writeByteBuffer(num++, prepareProto(pair -> {
					pair.writeString(1, a);
					pair.writeString(2, b);
				}));
			}
		}));
		int num = 1;

		for (int i = 0; i < (vals.length / 2); i++) {
			assertTrue(reader.hasRemaining());
			assertEquals(ProtobufField.ofStringPair(num++), reader.readTag());
			assertTrue(reader.hasRemaining());

			Pair<String, String> got = reader.readStringPair();

			assertEquals(vals[i * 2 + 0], got.first);
			assertEquals(vals[i * 2 + 1], got.second);
		}
		assertFalse(reader.hasRemaining());
	}
}
