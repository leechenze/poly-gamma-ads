// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.protobuf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.polygamma.android.origin.protobuf.Protobuf.WIRE_FIXED32;
import static org.polygamma.android.origin.protobuf.Protobuf.WIRE_FIXED64;
import static org.polygamma.android.origin.protobuf.Protobuf.WIRE_LEN;
import static org.polygamma.android.origin.protobuf.Protobuf.WIRE_VARINT;

import android.util.Pair;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.protobuf.CodedInputStream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.polygamma.android.origin.util.BiConsumer;
import org.polygamma.android.origin.util.Consumer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Random;

/**
 * {@link ProtobufEncoder} tests.
 */
@RunWith(AndroidJUnit4.class)
public class ProtobufEncoderTest {

	private static CodedInputStream prepareTest(Consumer<ProtobufEncoder> enc) {
		ProtobufEncoder e = ProtobufEncoder.of();

		enc.accept(e);
		return CodedInputStream.newInstance(e.asBuffer());
	}

	@Test
	public void testDecodeVarint() {
		ProtobufEncoder enc = ProtobufEncoder.of();

		for (VarintTestCase test : VarintTestCase.INSTANCES) {
			if ((test.decoded & 0xffffffffL) == test.decoded) {
				enc.reset();
				enc.encodeUint32((int) test.decoded);
				assertEquals(ByteBuffer.wrap(test.encoded), enc.asBuffer());
			}

			enc.reset();
			enc.encodeUint64(test.decoded);
			if (!ByteBuffer.wrap(test.encoded).equals(enc.asBuffer())) {
				StringBuilder a = new StringBuilder();
				StringBuilder b = new StringBuilder();

				for (int i = 0; i < test.encoded.length; i++) {
					a.append(Integer.toString(test.encoded[i] & 0xff, 16))
						.append(',');
				}
				for (int i = 0; i < enc.arrayOffset(); i++) {
					b.append(Integer.toString(enc.array()[i] & 0xff, 16))
						.append(',');
				}
			}
			assertEquals(ByteBuffer.wrap(test.encoded), enc.asBuffer());
		}
	}

	@Test
	public void testEncodeUint32() throws IOException {
		CodedInputStream dec = prepareTest(enc -> {
			int num = 1;

			assertThrows(IllegalArgumentException.class, () -> enc.encodePackedUint32ArrayField(
				Protobuf.fieldTagOf(1, WIRE_VARINT),
				TestCases.INT
			));
			for (int val : TestCases.INT) {
				enc.encodeUint32(val);
				enc.encodeUnsignedIntField(Protobuf.fieldTagOf(num++, WIRE_VARINT), val);
			}
			for (int i = 0; i <= TestCases.INT.length; i++) {
				enc.encodePackedUint32Array(TestCases.INT, i, TestCases.INT.length - i);
				enc.encodePackedUint32ArrayField(
					Protobuf.fieldTagOf(num++, WIRE_LEN),
					TestCases.INT, i, TestCases.INT.length - i
				);
			}
		});
		int num = 1;

		for (int val : TestCases.INT) {
			assertFalse(dec.isAtEnd());
			assertEquals(val, dec.readUInt32());
			if (val == 0) {
				num++;
			} else {
				assertFalse(dec.isAtEnd());
				assertEquals(Protobuf.fieldTagOf(num++, WIRE_VARINT), dec.readTag());
				assertFalse(dec.isAtEnd());
				assertEquals(val, dec.readUInt32());
			}
		}
		for (int i = 0; i <= TestCases.INT.length; i++) {
			int len = TestCases.INT.length - i;

			assertFalse(dec.isAtEnd());

			CodedInputStream packed = CodedInputStream.newInstance(dec.readByteArray());

			for (int j = 0; j < len; j++) {
				assertFalse(packed.isAtEnd());
				assertEquals(TestCases.INT[i + j], packed.readUInt32());
			}

			if (len == 0) {
				num++;
				continue;
			}

			assertFalse(dec.isAtEnd());
			assertEquals(Protobuf.fieldTagOf(num++, WIRE_LEN), dec.readTag());
			assertFalse(dec.isAtEnd());

			packed = CodedInputStream.newInstance(dec.readByteArray());
			for (int j = 0; j < len; j++) {
				assertFalse(packed.isAtEnd());
				assertEquals(TestCases.INT[i + j], packed.readUInt32());
			}
			assertTrue(packed.isAtEnd());
		}
		assertTrue(dec.isAtEnd());
	}

	@Test
	public void testEncodeSint32() throws IOException {
		CodedInputStream dec = prepareTest(enc -> {
			int num = 1;

			assertThrows(IllegalArgumentException.class, () -> enc.encodePackedSint32ArrayField(
				Protobuf.fieldTagOf(1, WIRE_VARINT),
				TestCases.INT
			));
			for (int val : TestCases.INT) {
				enc.encodeSint32(val);
				enc.encodeSignedIntField(Protobuf.fieldTagOf(num++, WIRE_VARINT), val);
			}
			for (int i = 0; i <= TestCases.INT.length; i++) {
				enc.encodePackedSint32Array(TestCases.INT, i, TestCases.INT.length - i);
				enc.encodePackedSint32ArrayField(
					Protobuf.fieldTagOf(num++, WIRE_LEN),
					TestCases.INT, i, TestCases.INT.length - i
				);
			}
		});
		int num = 1;

		for (int val : TestCases.INT) {
			assertFalse(dec.isAtEnd());
			assertEquals(val, dec.readSInt32());
			if (val == 0) {
				num++;
			} else {
				assertFalse(dec.isAtEnd());
				assertEquals(Protobuf.fieldTagOf(num++, WIRE_VARINT), dec.readTag());
				assertFalse(dec.isAtEnd());
				assertEquals(val, dec.readSInt32());
			}
		}
		for (int i = 0; i <= TestCases.INT.length; i++) {
			int len = TestCases.INT.length - i;

			assertFalse(dec.isAtEnd());

			CodedInputStream packed = CodedInputStream.newInstance(dec.readByteArray());

			for (int j = 0; j < len; j++) {
				assertFalse(packed.isAtEnd());
				assertEquals(TestCases.INT[i + j], packed.readSInt32());
			}

			if (len == 0) {
				num++;
				continue;
			}

			assertFalse(dec.isAtEnd());
			assertEquals(Protobuf.fieldTagOf(num++, WIRE_LEN), dec.readTag());
			assertFalse(dec.isAtEnd());

			packed = CodedInputStream.newInstance(dec.readByteArray());
			for (int j = 0; j < len; j++) {
				assertFalse(packed.isAtEnd());
				assertEquals(TestCases.INT[i + j], packed.readSInt32());
			}
			assertTrue(packed.isAtEnd());
		}
		assertTrue(dec.isAtEnd());
	}

	@Test
	public void testEncodeFixed32() throws IOException {
		CodedInputStream dec = prepareTest(enc -> {
			int num = 1;

			assertThrows(IllegalArgumentException.class, () -> enc.encodePackedFixed32ArrayField(
				Protobuf.fieldTagOf(1, WIRE_FIXED32),
				TestCases.INT
			));
			for (int val : TestCases.INT) {
				enc.encodeFixed32(val);
				enc.encodeUnsignedIntField(Protobuf.fieldTagOf(num++, WIRE_FIXED32), val);
			}
			for (int i = 0; i <= TestCases.INT.length; i++) {
				enc.encodePackedFixed32Array(TestCases.INT, i, TestCases.INT.length - i);
				enc.encodePackedFixed32ArrayField(
					Protobuf.fieldTagOf(num++, WIRE_LEN),
					TestCases.INT, i, TestCases.INT.length - i
				);
			}
		});
		int num = 1;

		for (int val : TestCases.INT) {
			assertFalse(dec.isAtEnd());
			assertEquals(val, dec.readFixed32());
			if (val == 0) {
				num++;
			} else {
				assertFalse(dec.isAtEnd());
				assertEquals(Protobuf.fieldTagOf(num++, WIRE_FIXED32), dec.readTag());
				assertFalse(dec.isAtEnd());
				assertEquals(val, dec.readFixed32());
			}
		}
		for (int i = 0; i <= TestCases.INT.length; i++) {
			int len = TestCases.INT.length - i;

			assertFalse(dec.isAtEnd());

			CodedInputStream packed = CodedInputStream.newInstance(dec.readByteArray());

			for (int j = 0; j < len; j++) {
				assertFalse(packed.isAtEnd());
				assertEquals(TestCases.INT[i + j], packed.readFixed32());
			}

			if (len == 0) {
				num++;
				continue;
			}

			assertFalse(dec.isAtEnd());
			assertEquals(Protobuf.fieldTagOf(num++, WIRE_LEN), dec.readTag());
			assertFalse(dec.isAtEnd());

			packed = CodedInputStream.newInstance(dec.readByteArray());
			for (int j = 0; j < len; j++) {
				assertFalse(packed.isAtEnd());
				assertEquals(TestCases.INT[i + j], packed.readFixed32());
			}
			assertTrue(packed.isAtEnd());
		}
		assertTrue(dec.isAtEnd());
	}

	@Test
	public void testEncodeUint64() throws IOException {
		CodedInputStream dec = prepareTest(enc -> {
			int num = 1;

			assertThrows(IllegalArgumentException.class, () -> enc.encodePackedUint64ArrayField(
				Protobuf.fieldTagOf(1, WIRE_VARINT),
				TestCases.LONG
			));
			for (long val : TestCases.LONG) {
				enc.encodeUint64(val);
				enc.encodeUnsignedLongField(Protobuf.fieldTagOf(num++, WIRE_VARINT), val);
			}
			for (int i = 0; i <= TestCases.LONG.length; i++) {
				enc.encodePackedUint64Array(TestCases.LONG, i, TestCases.LONG.length - i);
				enc.encodePackedUint64ArrayField(
					Protobuf.fieldTagOf(num++, WIRE_LEN),
					TestCases.LONG, i, TestCases.LONG.length - i
				);
			}
		});
		int num = 1;

		for (long val : TestCases.LONG) {
			assertFalse(dec.isAtEnd());
			assertEquals(val, dec.readUInt64());
			if (val == 0) {
				num++;
			} else {
				assertFalse(dec.isAtEnd());
				assertEquals(Protobuf.fieldTagOf(num++, WIRE_VARINT), dec.readTag());
				assertFalse(dec.isAtEnd());
				assertEquals(val, dec.readUInt64());
			}
		}
		for (int i = 0; i <= TestCases.LONG.length; i++) {
			int len = TestCases.LONG.length - i;

			assertFalse(dec.isAtEnd());

			CodedInputStream packed = CodedInputStream.newInstance(dec.readByteArray());

			for (int j = 0; j < len; j++) {
				assertFalse(packed.isAtEnd());
				assertEquals(TestCases.LONG[i + j], packed.readUInt64());
			}

			if (len == 0) {
				num++;
				continue;
			}

			assertFalse(dec.isAtEnd());
			assertEquals(Protobuf.fieldTagOf(num++, WIRE_LEN), dec.readTag());
			assertFalse(dec.isAtEnd());

			packed = CodedInputStream.newInstance(dec.readByteArray());
			for (int j = 0; j < len; j++) {
				assertFalse(packed.isAtEnd());
				assertEquals(TestCases.LONG[i + j], packed.readUInt64());
			}
			assertTrue(packed.isAtEnd());
		}
		assertTrue(dec.isAtEnd());
	}

	@Test
	public void testEncodeSint64() throws IOException {
		CodedInputStream dec = prepareTest(enc -> {
			int num = 1;

			assertThrows(IllegalArgumentException.class, () -> enc.encodePackedSint64ArrayField(
				Protobuf.fieldTagOf(1, WIRE_VARINT),
				TestCases.LONG
			));
			for (long val : TestCases.LONG) {
				enc.encodeSint64(val);
				enc.encodeSignedLongField(Protobuf.fieldTagOf(num++, WIRE_VARINT), val);
			}
			for (int i = 0; i <= TestCases.LONG.length; i++) {
				enc.encodePackedSint64Array(TestCases.LONG, i, TestCases.LONG.length - i);
				enc.encodePackedSint64ArrayField(
					Protobuf.fieldTagOf(num++, WIRE_LEN),
					TestCases.LONG, i, TestCases.LONG.length - i
				);
			}
		});
		int num = 1;

		for (long val : TestCases.LONG) {
			assertFalse(dec.isAtEnd());
			assertEquals(val, dec.readSInt64());
			if (val == 0) {
				num++;
			} else {
				assertFalse(dec.isAtEnd());
				assertEquals(Protobuf.fieldTagOf(num++, WIRE_VARINT), dec.readTag());
				assertFalse(dec.isAtEnd());
				assertEquals(val, dec.readSInt64());
			}
		}
		for (int i = 0; i <= TestCases.LONG.length; i++) {
			int len = TestCases.LONG.length - i;

			assertFalse(dec.isAtEnd());

			CodedInputStream packed = CodedInputStream.newInstance(dec.readByteArray());

			for (int j = 0; j < len; j++) {
				assertFalse(packed.isAtEnd());
				assertEquals(TestCases.LONG[i + j], packed.readSInt64());
			}

			if (len == 0) {
				num++;
				continue;
			}

			assertFalse(dec.isAtEnd());
			assertEquals(Protobuf.fieldTagOf(num++, WIRE_LEN), dec.readTag());
			assertFalse(dec.isAtEnd());

			packed = CodedInputStream.newInstance(dec.readByteArray());

			for (int j = 0; j < len; j++) {
				assertFalse(packed.isAtEnd());
				assertEquals(TestCases.LONG[i + j], packed.readSInt64());
			}
			assertTrue(packed.isAtEnd());
		}
		assertTrue(dec.isAtEnd());
	}

	@Test
	public void testEncodeFixed64() throws IOException {
		CodedInputStream dec = prepareTest(enc -> {
			int num = 1;

			assertThrows(IllegalArgumentException.class, () -> enc.encodePackedFixed64ArrayField(
				Protobuf.fieldTagOf(1, WIRE_FIXED64),
				TestCases.LONG
			));
			for (long val : TestCases.LONG) {
				enc.encodeFixed64(val);
				enc.encodeUnsignedLongField(Protobuf.fieldTagOf(num++, WIRE_FIXED64), val);
			}
			for (int i = 0; i <= TestCases.LONG.length; i++) {
				enc.encodePackedFixed64Array(TestCases.LONG, i, TestCases.LONG.length - i);
				enc.encodePackedFixed64ArrayField(
					Protobuf.fieldTagOf(num++, WIRE_LEN),
					TestCases.LONG, i, TestCases.LONG.length - i
				);
			}
		});
		int num = 1;

		for (long val : TestCases.LONG) {
			assertFalse(dec.isAtEnd());
			assertEquals(val, dec.readFixed64());
			if (val == 0) {
				num++;
			} else {
				assertFalse(dec.isAtEnd());
				assertEquals(Protobuf.fieldTagOf(num++, WIRE_FIXED64), dec.readTag());
				assertFalse(dec.isAtEnd());
				assertEquals(val, dec.readFixed64());
			}
		}
		for (int i = 0; i <= TestCases.LONG.length; i++) {
			int len = TestCases.LONG.length - i;

			assertFalse(dec.isAtEnd());

			CodedInputStream packed = CodedInputStream.newInstance(dec.readByteArray());

			for (int j = 0; j < len; j++) {
				assertFalse(packed.isAtEnd());
				assertEquals(TestCases.LONG[i + j], packed.readFixed64());
			}

			if (len == 0) {
				num++;
				continue;
			}

			assertFalse(dec.isAtEnd());
			assertEquals(Protobuf.fieldTagOf(num++, WIRE_LEN), dec.readTag());
			assertFalse(dec.isAtEnd());

			packed = CodedInputStream.newInstance(dec.readByteArray());
			for (int j = 0; j < len; j++) {
				assertFalse(packed.isAtEnd());
				assertEquals(TestCases.LONG[i + j], packed.readFixed64());
			}
			assertTrue(packed.isAtEnd());
		}
		assertTrue(dec.isAtEnd());
	}

	@Test
	public void testEncodeBool() throws IOException {
		CodedInputStream dec = prepareTest(enc -> {
			int num = 1;

			assertThrows(
				IllegalArgumentException.class,
				() -> enc.encodeBoolField(Protobuf.fieldTagOf(1, WIRE_FIXED32), true)
			);
			assertThrows(IllegalArgumentException.class, () -> enc.encodePackedBoolArrayField(
				Protobuf.fieldTagOf(1, WIRE_VARINT),
				TestCases.BOOLEAN
			));
			for (boolean val : TestCases.BOOLEAN) {
				enc.encodeBool(val);
				enc.encodeBoolField(Protobuf.fieldTagOf(num++, WIRE_VARINT), val);
			}
			for (int i = 0; i <= TestCases.BOOLEAN.length; i++) {
				enc.encodePackedBoolArray(TestCases.BOOLEAN, i, TestCases.BOOLEAN.length - i);
				enc.encodePackedBoolArrayField(
					Protobuf.fieldTagOf(num++, WIRE_LEN),
					TestCases.BOOLEAN, i, TestCases.BOOLEAN.length - i
				);
			}
		});
		int num = 1;

		for (boolean val : TestCases.BOOLEAN) {
			assertFalse(dec.isAtEnd());
			assertEquals(val, dec.readBool());
			if (!val) {
				num++;
			} else {
				assertFalse(dec.isAtEnd());
				assertEquals(Protobuf.fieldTagOf(num++, WIRE_VARINT), dec.readTag());
				assertFalse(dec.isAtEnd());
				assertEquals(val, dec.readBool());
			}
		}
		for (int i = 0; i <= TestCases.BOOLEAN.length; i++) {
			int len = TestCases.BOOLEAN.length - i;

			assertFalse(dec.isAtEnd());

			CodedInputStream packed = CodedInputStream.newInstance(dec.readByteArray());

			for (int j = 0; j < len; j++) {
				assertFalse(packed.isAtEnd());
				assertEquals(TestCases.BOOLEAN[i + j], packed.readBool());
			}

			if (len == 0) {
				num++;
				continue;
			}

			assertFalse(dec.isAtEnd());
			assertEquals(Protobuf.fieldTagOf(num++, WIRE_LEN), dec.readTag());
			assertFalse(dec.isAtEnd());

			packed = CodedInputStream.newInstance(dec.readByteArray());
			for (int j = 0; j < len; j++) {
				assertFalse(packed.isAtEnd());
				assertEquals(TestCases.BOOLEAN[i + j], packed.readBool());
			}
			assertTrue(packed.isAtEnd());
		}
		assertTrue(dec.isAtEnd());
	}

	@Test
	public void testEncodeFloat() throws IOException {
		CodedInputStream dec = prepareTest(enc -> {
			int num = 1;

			assertThrows(IllegalArgumentException.class, () -> enc.encodePackedFloatArrayField(
				Protobuf.fieldTagOf(1, WIRE_VARINT),
				TestCases.FLOAT
			));
			for (float val : TestCases.FLOAT) {
				enc.encodeFloat(val);
				enc.encodeFloatField(Protobuf.fieldTagOf(num++, WIRE_FIXED32), val);
			}
			for (int i = 0; i <= TestCases.FLOAT.length; i++) {
				enc.encodePackedFloatArray(TestCases.FLOAT, i, TestCases.FLOAT.length - i);
				enc.encodePackedFloatArrayField(
					Protobuf.fieldTagOf(num++, WIRE_LEN),
					TestCases.FLOAT, i, TestCases.FLOAT.length - i
				);
			}
		});
		int num = 1;

		for (float val : TestCases.FLOAT) {
			assertFalse(dec.isAtEnd());
			assertEquals(val, dec.readFloat(), 0.f);
			if (val == 0) {
				num++;
			} else {
				assertFalse(dec.isAtEnd());
				assertEquals(Protobuf.fieldTagOf(num++, WIRE_FIXED32), dec.readTag());
				assertFalse(dec.isAtEnd());
				assertEquals(val, dec.readFloat(), 0.f);
			}
		}
		for (int i = 0; i <= TestCases.FLOAT.length; i++) {
			int len = TestCases.FLOAT.length - i;

			assertFalse(dec.isAtEnd());

			CodedInputStream packed = CodedInputStream.newInstance(dec.readByteArray());

			for (int j = 0; j < len; j++) {
				assertFalse(packed.isAtEnd());
				assertEquals(TestCases.FLOAT[i + j], packed.readFloat(), 0.f);
			}

			if (len == 0) {
				num++;
				continue;
			}

			assertFalse(dec.isAtEnd());
			assertEquals(Protobuf.fieldTagOf(num++, WIRE_LEN), dec.readTag());
			assertFalse(dec.isAtEnd());

			packed = CodedInputStream.newInstance(dec.readByteArray());
			for (int j = 0; j < len; j++) {
				assertFalse(packed.isAtEnd());
				assertEquals(TestCases.FLOAT[i + j], packed.readFloat(), 0.f);
			}
			assertTrue(packed.isAtEnd());
		}
		assertTrue(dec.isAtEnd());
	}

	@Test
	public void testEncodeDouble() throws IOException {
		CodedInputStream dec = prepareTest(enc -> {
			int num = 1;

			assertThrows(IllegalArgumentException.class, () -> enc.encodePackedDoubleArrayField(
				Protobuf.fieldTagOf(1, WIRE_FIXED64),
				TestCases.DOUBLE
			));
			for (double val : TestCases.DOUBLE) {
				enc.encodeDouble(val);
				enc.encodeDoubleField(Protobuf.fieldTagOf(num++, WIRE_FIXED64), val);
			}
			for (int i = 0; i <= TestCases.DOUBLE.length; i++) {
				enc.encodePackedDoubleArray(TestCases.DOUBLE, i, TestCases.DOUBLE.length - i);
				enc.encodePackedDoubleArrayField(
					Protobuf.fieldTagOf(num++, WIRE_LEN),
					TestCases.DOUBLE, i, TestCases.DOUBLE.length - i
				);
			}
		});
		int num = 1;

		for (double val : TestCases.DOUBLE) {
			assertFalse(dec.isAtEnd());
			assertEquals(val, dec.readDouble(), 0.);
			if (val == 0) {
				num++;
			} else {
				assertFalse(dec.isAtEnd());
				assertEquals(Protobuf.fieldTagOf(num++, WIRE_FIXED64), dec.readTag());
				assertFalse(dec.isAtEnd());
				assertEquals(val, dec.readDouble(), 0.);
			}
		}
		for (int i = 0; i <= TestCases.DOUBLE.length; i++) {
			int len = TestCases.DOUBLE.length - i;

			assertFalse(dec.isAtEnd());

			CodedInputStream packed = CodedInputStream.newInstance(dec.readByteArray());

			for (int j = 0; j < len; j++) {
				assertFalse(packed.isAtEnd());
				assertEquals(TestCases.DOUBLE[i + j], packed.readDouble(), 0.);
			}

			if (len == 0) {
				num++;
				continue;
			}

			assertFalse(dec.isAtEnd());
			assertEquals(Protobuf.fieldTagOf(num++, WIRE_LEN), dec.readTag());
			assertFalse(dec.isAtEnd());

			packed = CodedInputStream.newInstance(dec.readByteArray());
			for (int j = 0; j < len; j++) {
				assertFalse(packed.isAtEnd());
				assertEquals(TestCases.DOUBLE[i + j], packed.readDouble(), 0.);
			}
			assertTrue(packed.isAtEnd());
		}
		assertTrue(dec.isAtEnd());
	}

	@Test
	public void testEncodePackedBitmap() throws IOException {
		Random rand = new Random(44);
		int[] vals = {
			0, 3, 5, 7, 9, 11, 13, 15, 17, 19, 21, 23, 25, 27, 29, 31,
			34, 36, 38, 40, 42, 44, 46, 48, 50, 52, 54, 56, 58, 60, 62
		};
		long exp = 0L;

		for (int val : vals)
			exp |= (1L << val);

		long expF = exp;

		for (int base : new int[] { 0, rand.nextInt(), rand.nextInt() }) {
			CodedInputStream dec = prepareTest(enc -> {
				assertThrows(
					IllegalArgumentException.class,
					() -> enc.encodePackedUint32Bitmap64Field(
						Protobuf.fieldTagOf(1, WIRE_VARINT),
						1L,
						1
					)
				);
				enc.encodePackedUint32Bitmap64(expF, base);
				enc.encodePackedUint32Bitmap64Field(Protobuf.fieldTagOf(1, WIRE_LEN), expF, base);
				enc.encodePackedUint32Bitmap64Field(Protobuf.fieldTagOf(1, WIRE_LEN), 0L, base);
			});
			CodedInputStream packed;

			assertFalse(dec.isAtEnd());
			packed = CodedInputStream.newInstance(dec.readByteArray());
			for (int val : vals) {
				assertFalse(packed.isAtEnd());
				assertEquals(base + val, packed.readUInt32());
			}
			assertTrue(packed.isAtEnd());

			assertFalse(dec.isAtEnd());
			assertEquals(Protobuf.fieldTagOf(1, WIRE_LEN), dec.readTag());
			assertFalse(dec.isAtEnd());
			packed = CodedInputStream.newInstance(dec.readByteArray());
			for (int val : vals) {
				assertFalse(packed.isAtEnd());
				assertEquals(base + val, packed.readUInt32());
			}
			assertTrue(packed.isAtEnd());
			assertTrue(dec.isAtEnd());
		}
	}

	@Test
	public void testEncodeBytes() throws IOException {
		CodedInputStream dec = prepareTest(enc -> {
			assertThrows(IllegalArgumentException.class, () -> enc.encodeByteArrayField(
				Protobuf.fieldTagOf(1, WIRE_VARINT),
				TestCases.BYTES
			));
			for (int i = 0; i <= TestCases.BYTES.length; i++) {
				enc.encodeByteArray(TestCases.BYTES, i, TestCases.BYTES.length - i);
				enc.encodeByteArrayField(
					Protobuf.fieldTagOf(i + 1, WIRE_LEN),
					TestCases.BYTES, i, TestCases.BYTES.length - i
				);
			}
		});

		for (int i = 0; i <= TestCases.BYTES.length; i++) {
			int len = TestCases.BYTES.length - i;

			assertEquals(ByteBuffer.wrap(TestCases.BYTES, i, len), dec.readByteBuffer());
			if (len == 0)
				continue;
			assertEquals(Protobuf.fieldTagOf(i + 1, WIRE_LEN), dec.readTag());
			assertEquals(ByteBuffer.wrap(TestCases.BYTES, i, len), dec.readByteBuffer());
		}
		assertTrue(dec.isAtEnd());
	}

	@Test
	public void testEncodeString() throws IOException {
		CodedInputStream dec = prepareTest(enc -> {
			assertThrows(
				IllegalArgumentException.class,
				() -> enc.encodeStringField(Protobuf.fieldTagOf(1, WIRE_VARINT), "A")
			);

			for (int i = 0; i < TestCases.STRING.length; i++) {
				enc.encodeString(TestCases.STRING[i]);
				enc.encodeStringField(Protobuf.fieldTagOf(i + 1, WIRE_LEN), TestCases.STRING[i]);
			}
		});

		for (int i = 0; i < TestCases.STRING.length; i++) {
			assertEquals(TestCases.STRING[i], dec.readString());
			if (TestCases.STRING[i].isEmpty())
				continue;
			assertEquals(Protobuf.fieldTagOf(i + 1, WIRE_LEN), dec.readTag());
			assertEquals(TestCases.STRING[i], dec.readString());
		}
		assertTrue(dec.isAtEnd());
	}

	private Pair<String, String> decodeStringPair(CodedInputStream dec) throws IOException {
		CodedInputStream pairDec = CodedInputStream.newInstance(dec.readByteArray());
		String a = "";
		String b = "";

		while (!pairDec.isAtEnd()) {
			int tag = pairDec.readTag();
			int num = Protobuf.fieldNumberOfFieldTag(tag);

			assertEquals(WIRE_LEN, Protobuf.wireTypeOfFieldTag(tag));
			assertTrue(num >= 1 && num <= 2);

			String c = pairDec.readString();

			assertNotEquals("", c);
			if (num == 1) {
				assertEquals("", a);
				a = c;
			} else {
				assertEquals("", b);
				b = c;
			}

			if (num == 1)
				a = c;
			else
				b = c;
		}
		return new Pair<>(a, b);
	}

	@Test
	public void testEncodeStringPair() throws IOException {
		CodedInputStream dec = prepareTest(enc -> {
			assertThrows(IllegalArgumentException.class, () -> enc.encodeStringPairField(
				Protobuf.fieldTagOf(1, WIRE_VARINT),
				new Pair<>("A", "B")
			));
			for (int i = 0; i < TestCases.STRING_PAIR.length; i++) {
				Pair<String, String> exp = TestCases.STRING_PAIR[i];

				enc.encodeStringPair(exp);
				enc.encodeStringPairField(Protobuf.fieldTagOf(i + 1, WIRE_LEN), exp);
			}
		});

		for (int i = 0; i < TestCases.STRING_PAIR.length; i++) {
			Pair<String, String> exp = TestCases.STRING_PAIR[i];

			assertFalse(dec.isAtEnd());

			Pair<String, String> got = decodeStringPair(dec);

			assertEquals(exp.first, got.first);
			assertEquals(exp.second, got.second);

			if (exp.first.isEmpty() && exp.second.isEmpty())
				continue;

			assertFalse(dec.isAtEnd());
			assertEquals(Protobuf.fieldTagOf(i + 1, WIRE_LEN), dec.readTag());

			assertFalse(dec.isAtEnd());
			got = decodeStringPair(dec);
			assertEquals(exp.first, got.first);
			assertEquals(exp.second, got.second);
		}
		assertTrue(dec.isAtEnd());
	}

	@Test
	public void testEncodeLen() throws IOException {
		final int A_TAG = Protobuf.fieldTagOf(1, WIRE_LEN);
		final int B_TAG = Protobuf.fieldTagOf(2, WIRE_LEN);

		BiConsumer<Pair<String, String>, ProtobufEncoder> encode = (src, dst) -> {
			dst.encodeStringField(A_TAG, src.first);
			dst.encodeStringField(B_TAG, src.second);
		};
		CodedInputStream dec = prepareTest(enc -> {
			assertThrows(
				IllegalArgumentException.class,
				() -> enc.encodeLenField(
					Protobuf.fieldTagOf(1, WIRE_VARINT),
					new Pair<>("a", "b"),
					(src, dst) -> { throw new AssertionError("should not have been invoked"); }
				)
			);
			for (int i = 0; i < TestCases.STRING_PAIR.length; i++) {
				Pair<String, String> exp = TestCases.STRING_PAIR[i];

				enc.encodeLen(exp, encode);
				enc.encodeLenField(Protobuf.fieldTagOf(i + 1, WIRE_LEN), exp, encode);
			}
		});

		for (int i = 0; i < TestCases.STRING_PAIR.length; i++) {
			Pair<String, String> exp = TestCases.STRING_PAIR[i];

			assertFalse(dec.isAtEnd());

			Pair<String, String> got = decodeStringPair(dec);

			assertEquals(exp.first, got.first);
			assertEquals(exp.second, got.second);

			if (exp.first.isEmpty() && exp.second.isEmpty())
				continue;

			assertFalse(dec.isAtEnd());
			assertEquals(Protobuf.fieldTagOf(i + 1, WIRE_LEN), dec.readTag());

			assertFalse(dec.isAtEnd());
			got = decodeStringPair(dec);
			assertEquals(exp.first, got.first);
			assertEquals(exp.second, got.second);
		}
		assertTrue(dec.isAtEnd());
	}

	private static void encodeTestMessage(TestMessage msg, ProtobufEncoder enc) {
		final int N_PAD = 3;

		enc.encodeBoolField(TestMessage.TAG_BOOL, msg.bool)
			.encodeUnsignedIntField(TestMessage.TAG_UINT32, msg.uint32)
			.encodeSignedIntField(TestMessage.TAG_SINT32, msg.sint32)
			.encodeUnsignedLongField(TestMessage.TAG_UINT64, msg.uint64)
			.encodeSignedLongField(TestMessage.TAG_SINT64, msg.sint64)
			.encodeUnsignedIntField(TestMessage.TAG_FIXED32, msg.fixed32)
			.encodeUnsignedLongField(TestMessage.TAG_FIXED64, msg.fixed64)
			.encodeFloatField(TestMessage.TAG_FIXED32F, msg.fixed32f)
			.encodeDoubleField(TestMessage.TAG_FIXED64F, msg.fixed64f);

		boolean[] bools = new boolean[N_PAD + msg.packedBool.length + N_PAD];

		System.arraycopy(msg.packedBool, 0, bools, N_PAD, msg.packedBool.length);
		enc.encodePackedBoolArrayField(
			TestMessage.TAG_PACKEDBOOL,
			bools, N_PAD, msg.packedBool.length
		);

		int[] ints = new int[N_PAD + msg.packedUint32.length + N_PAD];

		System.arraycopy(msg.packedUint32, 0, ints, N_PAD, msg.packedUint32.length);
		enc.encodePackedUint32ArrayField(
			TestMessage.TAG_PACKEDUINT32,
			ints, N_PAD, msg.packedUint32.length
		);

		ints = new int[N_PAD + msg.packedSint32.length + N_PAD];
		System.arraycopy(msg.packedSint32, 0, ints, N_PAD, msg.packedSint32.length);
		enc.encodePackedSint32ArrayField(
			TestMessage.TAG_PACKEDSINT32,
			ints, N_PAD, msg.packedSint32.length
		);

		long[] longs = new long[N_PAD + msg.packedUint64.length + N_PAD];

		System.arraycopy(msg.packedUint64, 0, longs, N_PAD, msg.packedUint64.length);
		enc.encodePackedUint64ArrayField(
			TestMessage.TAG_PACKEDUINT64,
			longs, N_PAD, msg.packedUint64.length
		);

		longs = new long[N_PAD + msg.packedSint64.length + N_PAD];
		System.arraycopy(msg.packedSint64, 0, longs, N_PAD, msg.packedSint64.length);
		enc.encodePackedSint64ArrayField(
			TestMessage.TAG_PACKEDSINT64,
			longs, N_PAD, msg.packedSint64.length
		);

		ints = new int[N_PAD + msg.packedFixed32.length + N_PAD];
		System.arraycopy(msg.packedFixed32, 0, ints, N_PAD, msg.packedFixed32.length);
		enc.encodePackedFixed32ArrayField(
			TestMessage.TAG_PACKEDFIXED32,
			ints, N_PAD, msg.packedFixed32.length
		);

		longs = new long[N_PAD + msg.packedFixed64.length + N_PAD];
		System.arraycopy(msg.packedFixed64, 0, longs, N_PAD, msg.packedFixed64.length);
		enc.encodePackedFixed64ArrayField(
			TestMessage.TAG_PACKEDFIXED64,
			longs, N_PAD, msg.packedFixed64.length
		);

		float[] floats = new float[N_PAD + msg.packedFixed32f.length + N_PAD];

		System.arraycopy(msg.packedFixed32f, 0, floats, N_PAD, msg.packedFixed32f.length);
		enc.encodePackedFloatArrayField(
			TestMessage.TAG_PACKEDFIXED32F,
			floats, N_PAD, msg.packedFixed32f.length
		);

		double[] doubles = new double[N_PAD + msg.packedFixed64f.length + N_PAD];

		System.arraycopy(msg.packedFixed64f, 0, doubles, N_PAD, msg.packedFixed64f.length);
		enc.encodePackedDoubleArrayField(
			TestMessage.TAG_PACKEDFIXED64F,
			doubles, N_PAD, msg.packedFixed64f.length
		);

		byte[] bytes = new byte[N_PAD + msg.bytes.length + N_PAD];

		System.arraycopy(msg.bytes, 0, bytes, N_PAD, msg.bytes.length);
		enc.encodeByteArrayField(TestMessage.TAG_BYTES, bytes, N_PAD, msg.bytes.length);

		enc.encodeStringField(TestMessage.TAG_STRING, msg.string)
			.encodeLenField(
				TestMessage.TAG_MESSAGE,
				msg.message,
				ProtobufEncoderTest::encodeTestMessage
			);
	}

	@Test
	public void testFull() throws IOException {
		Random rand = new Random(44);

		for (int i = 0; i < 10000; i++) {
			TestMessage exp = TestMessage.nextRandom(rand);
			ProtobufEncoder enc = ProtobufEncoder.of();

			assertFalse(enc.isSplit());
			encodeTestMessage(exp, enc);

			TestMessage got = new TestMessage();

			got.decode(CodedInputStream.newInstance(enc.asBuffer()));
			exp.assertEqual(got);

			enc = ProtobufEncoder.ofSplit();
			assertTrue(enc.isSplit());
			encodeTestMessage(exp, enc);

			got = new TestMessage();
			got.decodeSplit(
				CodedInputStream.newInstance(enc.asSchemaBuffer()),
				CodedInputStream.newInstance(enc.asContentBuffer())
			);
			exp.assertEqual(got);
		}
	}
}
