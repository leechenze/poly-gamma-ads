// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.protobuf;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.polygamma.android.origin.protobuf.Protobuf.WIRE_FIXED32;
import static org.polygamma.android.origin.protobuf.Protobuf.WIRE_FIXED64;
import static org.polygamma.android.origin.protobuf.Protobuf.WIRE_LEN;
import static org.polygamma.android.origin.protobuf.Protobuf.WIRE_VARINT;

import android.util.Pair;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.protobuf.CodedOutputStream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.polygamma.android.origin.util.Consumer;
import org.polygamma.android.origin.util.Function;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.Arrays;
import java.util.Random;

/**
 * {@link ProtobufDecoder} tests.
 */
@RunWith(AndroidJUnit4.class)
public class ProtobufDecoderTest {

	// Function for encoding to a Protobuf coded stream.
	@FunctionalInterface
	private interface EncodeProtobuf {
		// Encode to Protobuf.
		void encode(CodedOutputStream out) throws IOException;
	}

	// Function for encoding to split Protobuf coded streams.
	@FunctionalInterface
	private interface EncodeSplitProtobuf {
		// Encode to Protobuf.
		void encode(CodedOutputStream schema, CodedOutputStream content) throws IOException;
	}

	private byte[] prepareTest(EncodeProtobuf encode) {
		try (ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
			CodedOutputStream proto = CodedOutputStream.newInstance(bytes);

			encode.encode(proto);
			proto.flush();
			bytes.flush();
			return bytes.toByteArray();
		} catch (IOException cause) {
			throw new AssertionError(cause);
		}
	}

	private byte[][] prepareSplitTest(EncodeSplitProtobuf encode) {
		try (
			ByteArrayOutputStream schemaBytes = new ByteArrayOutputStream();
			ByteArrayOutputStream contentBytes = new ByteArrayOutputStream()
		) {
			CodedOutputStream schema = CodedOutputStream.newInstance(schemaBytes);
			CodedOutputStream content = CodedOutputStream.newInstance(contentBytes);

			encode.encode(schema, content);
			schema.flush();
			content.flush();
			return new byte[][] { schemaBytes.toByteArray(), contentBytes.toByteArray() };
		} catch (IOException cause) {
			throw new AssertionError(cause);
		}
	}

	private void runTest(byte[] enc, Consumer<ProtobufDecoder> test) {
		byte[] pad = { 1, 2, 3 };
		byte[] padded = new byte[pad.length + enc.length + pad.length];

		System.arraycopy(pad, 0, padded, 0, pad.length);
		System.arraycopy(enc, 0, padded, pad.length, enc.length);
		System.arraycopy(pad, 0, padded, pad.length + enc.length, pad.length);

		test.accept(ProtobufDecoder.of(padded, pad.length, enc.length));
		assertArrayEquals(pad, Arrays.copyOfRange(padded, 0, pad.length));
		assertArrayEquals(enc, Arrays.copyOfRange(padded, pad.length, pad.length + enc.length));
		assertArrayEquals(pad, Arrays.copyOfRange(padded, pad.length + enc.length, padded.length));
	}

	private void runTest(EncodeProtobuf encode, Consumer<ProtobufDecoder> test) {
		runTest(prepareTest(encode), test);
	}

	@Test
	public void testDecodeVarint() {
		for (VarintTestCase test : VarintTestCase.INSTANCES) {
			runTest(test.encoded, dec -> {
				ProtobufDecoder fail =
					ProtobufDecoder.of(dec.array(), dec.arrayOffset(), dec.arrayRemaining() - 1);

				assertThrows(IndexOutOfBoundsException.class, fail::decodeUint32);
				assertTrue(dec.hasRemaining());
				assertEquals((int) (test.decoded & 0xffffffffL), dec.decodeUint32());
				assertFalse(dec.hasRemaining());
			});
			runTest(test.encoded, dec -> {
				assertTrue(dec.hasRemaining());
				assertEquals(test.decoded, dec.decodeUint64());
				assertFalse(dec.hasRemaining());
			});
		}
	}

	@Test
	public void testDecodeUint32() {
		runTest(enc -> {
			for (int val : TestCases.INT)
				enc.writeUInt32NoTag(val);
			for (int i = 0; i < TestCases.INT.length; i++) {
				int off = i;
				int len = TestCases.INT.length - i;
				byte[] packed = prepareTest(packedEnc -> {
					for (int j = 0; j < len; j++)
						packedEnc.writeUInt32NoTag(TestCases.INT[off + j]);
				});

				enc.writeByteArrayNoTag(packed);
				enc.writeByteArrayNoTag(packed);
			}
		}, dec -> {
			for (int val : TestCases.INT) {
				assertTrue(dec.hasRemaining());
				assertEquals(val, dec.decodeUint32());
			}
			for (int i = 0; i < TestCases.INT.length; i++) {
				IntBuffer exp = IntBuffer.wrap(TestCases.INT, i, TestCases.INT.length - i);
				int[] got = new int[exp.remaining()];

				assertTrue(dec.hasRemaining());
				assertSame(got, dec.decodePackedUint32Array(got));
				assertEquals(exp, IntBuffer.wrap(got));

				assertTrue(dec.hasRemaining());
				got = dec.decodePackedUint32Array((i % 2) == 0 ? null : new int[got.length - 1]);
				assertEquals(exp, IntBuffer.wrap(got));
			}
			assertFalse(dec.hasRemaining());
		});
	}

	@Test
	public void testDecodeSint32() {
		runTest(enc -> {
			for (int val : TestCases.INT)
				enc.writeSInt32NoTag(val);
			for (int i = 0; i < TestCases.INT.length; i++) {
				int off = i;
				int len = TestCases.INT.length - i;
				byte[] packed = prepareTest(packedEnc -> {
					for (int j = 0; j < len; j++)
						packedEnc.writeSInt32NoTag(TestCases.INT[off + j]);
				});

				enc.writeByteArrayNoTag(packed);
				enc.writeByteArrayNoTag(packed);
			}
		}, dec -> {
			for (int val : TestCases.INT) {
				assertTrue(dec.hasRemaining());
				assertEquals(val, dec.decodeSint32());
			}
			for (int i = 0; i < TestCases.INT.length; i++) {
				IntBuffer exp = IntBuffer.wrap(TestCases.INT, i, TestCases.INT.length - i);
				int[] got = new int[exp.remaining()];

				assertTrue(dec.hasRemaining());
				assertSame(got, dec.decodePackedSint32Array(got));
				assertEquals(exp, IntBuffer.wrap(got));

				assertTrue(dec.hasRemaining());
				got = dec.decodePackedSint32Array((i % 2) == 0 ? null : new int[got.length - 1]);
				assertEquals(exp, IntBuffer.wrap(got));
			}
			assertFalse(dec.hasRemaining());
		});
	}

	@Test
	public void testDecodeFixed32() {
		runTest(enc -> {
			for (int val : TestCases.INT)
				enc.writeFixed32NoTag(val);
			for (int i = 0; i < TestCases.INT.length; i++) {
				int off = i;
				int len = TestCases.INT.length - i;
				byte[] packed = prepareTest(packedEnc -> {
					for (int j = 0; j < len; j++)
						packedEnc.writeFixed32NoTag(TestCases.INT[off + j]);
				});

				enc.writeByteArrayNoTag(packed);
				enc.writeByteArrayNoTag(packed);
			}
		}, dec -> {
			for (int val : TestCases.INT) {
				assertTrue(dec.hasRemaining());
				assertEquals(val, dec.decodeFixed32());
			}
			for (int i = 0; i < TestCases.INT.length; i++) {
				IntBuffer exp = IntBuffer.wrap(TestCases.INT, i, TestCases.INT.length - i);
				int[] got = new int[exp.remaining()];

				assertTrue(dec.hasRemaining());
				assertSame(got, dec.decodePackedFixed32Array(got));
				assertEquals(exp, IntBuffer.wrap(got));

				assertTrue(dec.hasRemaining());
				got = dec.decodePackedFixed32Array((i % 2) == 0 ? null : new int[got.length - 1]);
				assertEquals(exp, IntBuffer.wrap(got));
			}
			assertFalse(dec.hasRemaining());
		});
	}

	@Test
	public void testDecodeUint64() {
		runTest(enc -> {
			for (long val : TestCases.LONG)
				enc.writeUInt64NoTag(val);
			for (int i = 0; i < TestCases.LONG.length; i++) {
				int off = i;
				int len = TestCases.LONG.length - i;
				byte[] packed = prepareTest(packedEnc -> {
					for (int j = 0; j < len; j++)
						packedEnc.writeUInt64NoTag(TestCases.LONG[off + j]);
				});

				enc.writeByteArrayNoTag(packed);
				enc.writeByteArrayNoTag(packed);
			}
		}, dec -> {
			for (long val : TestCases.LONG) {
				assertTrue(dec.hasRemaining());
				assertEquals(val, dec.decodeUint64());
			}
			for (int i = 0; i < TestCases.LONG.length; i++) {
				LongBuffer exp = LongBuffer.wrap(TestCases.LONG, i, TestCases.LONG.length - i);
				long[] got = new long[exp.remaining()];

				assertTrue(dec.hasRemaining());
				assertSame(got, dec.decodePackedUint64Array(got));
				assertEquals(exp, LongBuffer.wrap(got));

				assertTrue(dec.hasRemaining());
				got = dec.decodePackedUint64Array((i % 2) == 0 ? null : new long[got.length - 1]);
				assertEquals(exp, LongBuffer.wrap(got));
			}
			assertFalse(dec.hasRemaining());
		});
	}

	@Test
	public void testDecodeSint64() {
		runTest(enc -> {
			for (long val : TestCases.LONG)
				enc.writeSInt64NoTag(val);
			for (int i = 0; i < TestCases.LONG.length; i++) {
				int off = i;
				int len = TestCases.LONG.length - i;
				byte[] packed = prepareTest(packedEnc -> {
					for (int j = 0; j < len; j++)
						packedEnc.writeSInt64NoTag(TestCases.LONG[off + j]);
				});

				enc.writeByteArrayNoTag(packed);
				enc.writeByteArrayNoTag(packed);
			}
		}, dec -> {
			for (long val : TestCases.LONG) {
				assertTrue(dec.hasRemaining());
				assertEquals(val, dec.decodeSint64());
			}
			for (int i = 0; i < TestCases.LONG.length; i++) {
				LongBuffer exp = LongBuffer.wrap(TestCases.LONG, i, TestCases.LONG.length - i);
				long[] got = new long[exp.remaining()];

				assertTrue(dec.hasRemaining());
				assertSame(got, dec.decodePackedSint64Array(got));
				assertEquals(exp, LongBuffer.wrap(got));

				assertTrue(dec.hasRemaining());
				got = dec.decodePackedSint64Array((i % 2) == 0 ? null : new long[got.length - 1]);
				assertEquals(exp, LongBuffer.wrap(got));
			}
			assertFalse(dec.hasRemaining());
		});
	}

	@Test
	public void testDecodeFixed64() {
		runTest(enc -> {
			for (long val : TestCases.LONG)
				enc.writeFixed64NoTag(val);
			for (int i = 0; i < TestCases.LONG.length; i++) {
				int off = i;
				int len = TestCases.LONG.length - i;
				byte[] packed = prepareTest(packedEnc -> {
					for (int j = 0; j < len; j++)
						packedEnc.writeFixed64NoTag(TestCases.LONG[off + j]);
				});

				enc.writeByteArrayNoTag(packed);
				enc.writeByteArrayNoTag(packed);
			}
		}, dec -> {
			for (long val : TestCases.LONG) {
				assertTrue(dec.hasRemaining());
				assertEquals(val, dec.decodeFixed64());
			}
			for (int i = 0; i < TestCases.LONG.length; i++) {
				LongBuffer exp = LongBuffer.wrap(TestCases.LONG, i, TestCases.LONG.length - i);
				long[] got = new long[exp.remaining()];

				assertTrue(dec.hasRemaining());
				assertSame(got, dec.decodePackedFixed64Array(got));
				assertEquals(exp, LongBuffer.wrap(got));

				assertTrue(dec.hasRemaining());
				got = dec.decodePackedFixed64Array((i % 2) == 0 ? null : new long[got.length - 1]);
				assertEquals(exp, LongBuffer.wrap(got));
			}
			assertFalse(dec.hasRemaining());
		});
	}

	@Test
	public void testDecodeBool() {
		runTest(enc -> {
			for (int i = 0; i < TestCases.BOOLEAN.length; i++) {
				boolean val = TestCases.BOOLEAN[i];

				if ((i % 2) == 0)
					enc.writeBoolNoTag(val);
				else
					enc.writeUInt64NoTag(val ? ~0L : 0);
			}
			for (int i = 0; i < TestCases.BOOLEAN.length; i++) {
				int off = i;
				int len = TestCases.BOOLEAN.length - i;

				enc.writeByteArrayNoTag(prepareTest(packedEnc -> {
					for (int j = 0; j < len; j++)
						packedEnc.writeBoolNoTag(TestCases.BOOLEAN[off + j]);
				}));
				enc.writeByteArrayNoTag(prepareTest(packedEnc -> {
					for (int j = 0; j < len; j++)
						packedEnc.writeUInt64NoTag(TestCases.BOOLEAN[off + j] ? ~0L : 0);
				}));
			}
		}, dec -> {
			for (boolean val : TestCases.BOOLEAN) {
				assertTrue(dec.hasRemaining());
				assertEquals(val, dec.decodeBool());
			}
			for (int i = 0; i < TestCases.BOOLEAN.length; i++) {
				boolean[] exp = Arrays.copyOfRange(TestCases.BOOLEAN, i, TestCases.BOOLEAN.length);
				boolean[] got = new boolean[exp.length];

				assertTrue(dec.hasRemaining());
				assertSame(got, dec.decodePackedBoolArray(got));
				assertArrayEquals(exp, got);

				assertTrue(dec.hasRemaining());
				got = dec.decodePackedBoolArray((i % 2) == 0 ? null : new boolean[got.length - 1]);
				assertArrayEquals(exp, got);
			}
			assertFalse(dec.hasRemaining());
		});
	}

	@Test
	public void testDecodeFloat() {
		runTest(enc -> {
			for (float val : TestCases.FLOAT)
				enc.writeFloatNoTag(val);
			for (int i = 0; i < TestCases.FLOAT.length; i++) {
				int off = i;
				int len = TestCases.FLOAT.length - i;
				byte[] packed = prepareTest(packedEnc -> {
					for (int j = 0; j < len; j++)
						packedEnc.writeFloatNoTag(TestCases.FLOAT[off + j]);
				});

				enc.writeByteArrayNoTag(packed);
				enc.writeByteArrayNoTag(packed);
			}
		}, dec -> {
			for (float val : TestCases.FLOAT) {
				assertTrue(dec.hasRemaining());
				assertEquals(val, dec.decodeFloat(), 0.f);
			}
			for (int i = 0; i < TestCases.FLOAT.length; i++) {
				FloatBuffer exp = FloatBuffer.wrap(TestCases.FLOAT, i, TestCases.FLOAT.length - i);
				float[] got = new float[exp.remaining()];

				assertTrue(dec.hasRemaining());
				assertSame(got, dec.decodePackedFloatArray(got));
				assertEquals(exp, FloatBuffer.wrap(got));

				assertTrue(dec.hasRemaining());
				got = dec.decodePackedFloatArray((i % 2) == 0 ? null : new float[got.length - 1]);
				assertEquals(exp, FloatBuffer.wrap(got));
			}
			assertFalse(dec.hasRemaining());
		});
	}

	@Test
	public void testDecodeDouble() {
		runTest(enc -> {
			for (double val : TestCases.DOUBLE)
				enc.writeDoubleNoTag(val);
			for (int i = 0; i < TestCases.DOUBLE.length; i++) {
				int off = i;
				int len = TestCases.DOUBLE.length - i;
				byte[] packed = prepareTest(packedEnc -> {
					for (int j = 0; j < len; j++)
						packedEnc.writeDoubleNoTag(TestCases.DOUBLE[off + j]);
				});

				enc.writeByteArrayNoTag(packed);
				enc.writeByteArrayNoTag(packed);
			}
		}, dec -> {
			for (double val : TestCases.DOUBLE) {
				assertTrue(dec.hasRemaining());
				assertEquals(val, dec.decodeDouble(), 0.);
			}
			for (int i = 0; i < TestCases.DOUBLE.length; i++) {
				DoubleBuffer exp =
					DoubleBuffer.wrap(TestCases.DOUBLE, i, TestCases.DOUBLE.length - i);
				double[] got = new double[exp.remaining()];

				assertTrue(dec.hasRemaining());
				assertSame(got, dec.decodePackedDoubleArray(got));
				assertEquals(exp, DoubleBuffer.wrap(got));

				assertTrue(dec.hasRemaining());
				got = dec.decodePackedDoubleArray((i % 2) == 0 ? null : new double[got.length - 1]);
				assertEquals(exp, DoubleBuffer.wrap(got));
			}
			assertFalse(dec.hasRemaining());
		});
	}

	@Test
	public void testDecodePackedBitmap() {
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
			runTest(enc -> {
				byte[] packed = prepareTest(packedEnc -> {
					for (int val : vals)
						packedEnc.writeUInt32NoTag(val + base);
				});

				enc.writeByteArrayNoTag(packed);
			}, dec -> {
				assertTrue(dec.hasRemaining());
				assertEquals(expF, dec.decodePackedUint32Bitmap64(base));
				assertFalse(dec.hasRemaining());
				assertThrows(
					IndexOutOfBoundsException.class,
					() -> dec.decodePackedUint32Bitmap64(base)
				);
			});
		}
	}

	@Test
	public void testDecodeBytes() {
		runTest(enc -> {
			for (int i = 0; i < TestCases.BYTES.length; i++) {
				byte[] exp = Arrays.copyOfRange(TestCases.BYTES, 0, i);

				enc.writeByteArrayNoTag(exp);
				enc.writeByteArrayNoTag(exp);
			}
		}, dec -> {
			for (int i = 0; i < TestCases.BYTES.length; i++) {
				byte[] exp = Arrays.copyOfRange(TestCases.BYTES, 0, i);
				byte[] got = new byte[exp.length];

				assertTrue(dec.hasRemaining());
				assertSame(got, dec.decodeByteArray(got));
				assertArrayEquals(exp, got);

				assertTrue(dec.hasRemaining());
				got = dec.decodeByteArray((i % 2) == 0 ? null : new byte[got.length - 1]);
				assertArrayEquals(exp, got);
			}
			assertFalse(dec.hasRemaining());
		});
	}

	@Test
	public void testDecodeString() {
		runTest(enc -> {
			for (String s : TestCases.STRING)
				enc.writeStringNoTag(s);
		}, dec -> {
			for (String exp : TestCases.STRING) {
				assertTrue(dec.hasRemaining());
				assertEquals(exp, dec.decodeString());
			}
			assertFalse(dec.hasRemaining());
		});
	}

	@Test
	public void testDecodeStringPair() {
		runTest(enc -> {
			for (Pair<String, String> exp : TestCases.STRING_PAIR) {
				enc.writeByteArrayNoTag(prepareTest(pairEnc -> {
					pairEnc.writeString(1, exp.first);
					pairEnc.writeString(2, exp.second);
				}));
			}
		}, dec -> {
			for (Pair<String, String> exp : TestCases.STRING_PAIR) {
				assertTrue(dec.hasRemaining());

				Pair<String, String> got = dec.decodeStringPair();

				assertEquals(exp.first, got.first);
				assertEquals(exp.second, got.second);
			}
			assertFalse(dec.hasRemaining());
		});
	}

	@Test
	public void testDecodeLen() {
		runTest(enc -> {
			for (int i = 0; i < TestCases.BYTES.length; i++) {
				int len = i;
				byte[] seq =
					prepareTest(msgEnc -> msgEnc.writeByteArray(1, TestCases.BYTES, 0, len));

				enc.writeByteArrayNoTag(seq);
			}
		}, dec -> {
			Function<ProtobufDecoder, ByteBuffer> decodeMsg = msgDec -> {
				if (!msgDec.hasRemaining())
					return ByteBuffer.allocate(0);

				assertEquals(Protobuf.fieldTagOf(1, WIRE_LEN), msgDec.decodeFieldTag());
				assertTrue(msgDec.hasRemaining());

				byte[] got = msgDec.decodeByteArray();

				assertFalse(msgDec.hasRemaining());
				return ByteBuffer.wrap(got);
			};

			for (int i = 0; i < TestCases.BYTES.length; i++) {
				ByteBuffer exp = ByteBuffer.wrap(TestCases.BYTES, 0, i);

				assertTrue(dec.hasRemaining());
				assertEquals(exp, dec.decodeLen(decodeMsg));
			}
			assertFalse(dec.hasRemaining());
		});
	}

	private static TestMessage decodeTestMessage(ProtobufDecoder dec) {
		TestMessage msg = new TestMessage();

		while (dec.hasRemaining()) {
			int tag = dec.decodeFieldTag();

			if (tag == TestMessage.TAG_BOOL) {
				msg.bool = dec.decodeBool();
			} else if (tag == TestMessage.TAG_UINT32) {
				msg.uint32 = dec.decodeUint32();
			} else if (tag == TestMessage.TAG_SINT32) {
				msg.sint32 = dec.decodeSint32();
			} else if (tag == TestMessage.TAG_UINT64) {
				msg.uint64 = dec.decodeUint64();
			} else if (tag == TestMessage.TAG_SINT64) {
				msg.sint64 = dec.decodeSint64();
			} else if (tag == TestMessage.TAG_FIXED32) {
				msg.fixed32 = dec.decodeFixed32();
			} else if (tag == TestMessage.TAG_FIXED64) {
				msg.fixed64 = dec.decodeFixed64();
			} else if (tag == TestMessage.TAG_FIXED32F) {
				msg.fixed32f = dec.decodeFloat();
			} else if (tag == TestMessage.TAG_FIXED64F) {
				msg.fixed64f = dec.decodeDouble();
			} else if (tag == TestMessage.TAG_PACKEDBOOL) {
				msg.packedBool = dec.decodePackedBoolArray(msg.packedBool);
			} else if (tag == TestMessage.TAG_PACKEDUINT32) {
				msg.packedUint32 = dec.decodePackedUint32Array(msg.packedUint32);
			} else if (tag == TestMessage.TAG_PACKEDSINT32) {
				msg.packedSint32 = dec.decodePackedSint32Array(msg.packedSint32);
			} else if (tag == TestMessage.TAG_PACKEDUINT64) {
				msg.packedUint64 = dec.decodePackedUint64Array(msg.packedUint64);
			} else if (tag == TestMessage.TAG_PACKEDSINT64) {
				msg.packedSint64 = dec.decodePackedSint64Array(msg.packedSint64);
			} else if (tag == TestMessage.TAG_PACKEDFIXED32) {
				msg.packedFixed32 = dec.decodePackedFixed32Array(msg.packedFixed32);
			} else if (tag == TestMessage.TAG_PACKEDFIXED64) {
				msg.packedFixed64 = dec.decodePackedFixed64Array(msg.packedFixed64);
			} else if (tag == TestMessage.TAG_PACKEDFIXED32F) {
				msg.packedFixed32f = dec.decodePackedFloatArray(msg.packedFixed32f);
			} else if (tag == TestMessage.TAG_PACKEDFIXED64F) {
				msg.packedFixed64f = dec.decodePackedDoubleArray(msg.packedFixed64f);
			} else if (tag == TestMessage.TAG_BYTES) {
				msg.bytes = dec.decodeByteArray(msg.bytes);
			} else if (tag == TestMessage.TAG_STRING) {
				msg.string = dec.decodeString();
			} else {
				assertEquals(TestMessage.TAG_MESSAGE, tag);
				msg.message = dec.decodeLen(ProtobufDecoderTest::decodeTestMessage);
			}
		}
		return msg;
	}

	@Test
	public void testFull() {
		Random rand = new Random(44);

		for (int i = 0; i < 10000; i++) {
			TestMessage exp = TestMessage.nextRandom(rand);

			runTest(exp::encode, dec -> exp.assertEqual(decodeTestMessage(dec)));

			byte[][] arr = prepareSplitTest(exp::encodeSplit);
			byte[] pad = new byte[3];
			byte[] full =
				new byte[pad.length + arr[0].length + pad.length + arr[1].length + pad.length];
			int schemaOff = pad.length;
			int contentOff = schemaOff + arr[0].length + pad.length;

			rand.nextBytes(pad);
			System.arraycopy(pad, 0, full, 0, pad.length);
			System.arraycopy(arr[0], 0, full, schemaOff, arr[0].length);
			System.arraycopy(pad, 0, full, schemaOff + arr[0].length, pad.length);
			System.arraycopy(arr[1], 0, full, contentOff, arr[1].length);
			System.arraycopy(pad, 0, full, contentOff + arr[1].length, pad.length);

			ProtobufDecoder dec = ProtobufDecoder.ofSplit(
				full, schemaOff, arr[0].length,
				full, contentOff, arr[1].length
			);

			assertEquals(arr[0].length > 0 || arr[1].length > 0, dec.hasRemaining());

			TestMessage got = decodeTestMessage(dec);

			exp.assertEqual(got);
			assertFalse(dec.hasRemaining());

			assertArrayEquals(pad, Arrays.copyOfRange(full, 0, schemaOff));
			assertArrayEquals(pad, Arrays.copyOfRange(
				full,
				schemaOff + arr[0].length,
				schemaOff + arr[0].length + pad.length
			));
			assertArrayEquals(pad, Arrays.copyOfRange(
				full,
				contentOff + arr[1].length,
				contentOff + arr[1].length + pad.length
			));
		}
	}
}
