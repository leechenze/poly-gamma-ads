// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.protobuf;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.polygamma.android.origin.protobuf.Protobuf.WIRE_FIXED32;
import static org.polygamma.android.origin.protobuf.Protobuf.WIRE_FIXED64;
import static org.polygamma.android.origin.protobuf.Protobuf.WIRE_LEN;
import static org.polygamma.android.origin.protobuf.Protobuf.WIRE_VARINT;
import static org.polygamma.android.origin.protobuf.Protobuf.fieldNumberOfFieldTag;
import static org.polygamma.android.origin.protobuf.Protobuf.fieldTagOf;
import static org.polygamma.android.origin.protobuf.Protobuf.wireTypeOfFieldTag;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;

public class TestMessage {

	private static final boolean[] EMPTY_BOOL = new boolean[0];
	private static final byte[] EMPTY_BYTE = new byte[0];
	private static final int[] EMPTY_INT = new int[0];
	private static final long[] EMPTY_LONG = new long[0];
	private static final float[] EMPTY_FLOAT = new float[0];
	private static final double[] EMPTY_DOUBLE = new double[0];

	static final int TAG_BOOL				= fieldTagOf( 1, WIRE_VARINT);
	static final int TAG_UINT32				= fieldTagOf( 2, WIRE_VARINT);
	static final int TAG_SINT32				= fieldTagOf( 3, WIRE_VARINT);
	static final int TAG_UINT64				= fieldTagOf( 4, WIRE_VARINT);
	static final int TAG_SINT64				= fieldTagOf( 5, WIRE_VARINT);

	static final int TAG_FIXED32			= fieldTagOf( 6, WIRE_FIXED32);
	static final int TAG_FIXED64			= fieldTagOf( 7, WIRE_FIXED64);

	static final int TAG_FIXED32F			= fieldTagOf( 8, WIRE_FIXED32);
	static final int TAG_FIXED64F			= fieldTagOf( 9, WIRE_FIXED64);

	static final int TAG_PACKEDBOOL			= fieldTagOf(10, WIRE_LEN);
	static final int TAG_PACKEDUINT32		= fieldTagOf(11, WIRE_LEN);
	static final int TAG_PACKEDSINT32		= fieldTagOf(12, WIRE_LEN);
	static final int TAG_PACKEDUINT64		= fieldTagOf(13, WIRE_LEN);
	static final int TAG_PACKEDSINT64		= fieldTagOf(14, WIRE_LEN);

	static final int TAG_PACKEDFIXED32		= fieldTagOf(15, WIRE_LEN);
	static final int TAG_PACKEDFIXED64		= fieldTagOf(16, WIRE_LEN);

	static final int TAG_PACKEDFIXED32F		= fieldTagOf(17, WIRE_LEN);
	static final int TAG_PACKEDFIXED64F		= fieldTagOf(18, WIRE_LEN);

	static final int TAG_BYTES				= fieldTagOf(19, WIRE_LEN);
	static final int TAG_STRING				= fieldTagOf(20, WIRE_LEN);

	static final int TAG_MESSAGE			= fieldTagOf(21, WIRE_LEN);

	private static boolean[] nextRandomBooleanArray(Random rand) {
		int n = rand.nextInt(32);

		if (n == 0)
			return EMPTY_BOOL;

		boolean[] rv = new boolean[n];

		for (int i = 0; i < n; i++)
			rv[i] = rand.nextBoolean();
		return rv;
	}

	private static int[] nextRandomIntArray(Random rand) {
		int n = rand.nextInt(32);

		if (n == 0)
			return EMPTY_INT;

		int[] rv = new int[n];

		for (int i = 0; i < n; i++)
			rv[i] = rand.nextInt();
		return rv;
	}

	private static long[] nextRandomLongArray(Random rand) {
		int n = rand.nextInt(32);

		if (n == 0)
			return EMPTY_LONG;

		long[] rv = new long[n];

		for (int i = 0; i < n; i++)
			rv[i] = rand.nextLong();
		return rv;
	}

	private static float[] nextRandomFloatArray(Random rand) {
		int n = rand.nextInt(32);

		if (n == 0)
			return EMPTY_FLOAT;

		float[] rv = new float[n];

		for (int i = 0; i < n; i++)
			rv[i] = rand.nextFloat();
		return rv;
	}

	private static double[] nextRandomDoubleArray(Random rand) {
		int n = rand.nextInt(32);

		if (n == 0)
			return EMPTY_DOUBLE;

		double[] rv = new double[n];

		for (int i = 0; i < n; i++)
			rv[i] = rand.nextDouble();
		return rv;
	}

	private static TestMessage nextRandom(Random rand, int depth) {
		TestMessage rv = new TestMessage();

		rv.bool = rand.nextBoolean();
		rv.uint32 = rand.nextBoolean() ? rand.nextInt() : 0;
		rv.sint32 = rand.nextBoolean() ? rand.nextInt() : 0;
		rv.uint64 = rand.nextBoolean() ? rand.nextLong() : 0;
		rv.sint64 = rand.nextBoolean() ? rand.nextLong() : 0;

		rv.fixed32 = rand.nextBoolean() ? rand.nextInt() : 0;
		rv.fixed64 = rand.nextBoolean() ? rand.nextLong() : 0;

		rv.fixed32f = rand.nextBoolean() ? rand.nextFloat() : 0;
		rv.fixed64f = rand.nextBoolean() ? rand.nextDouble() : 0;

		rv.packedBool = nextRandomBooleanArray(rand);
		rv.packedUint32 = nextRandomIntArray(rand);
		rv.packedSint32 = nextRandomIntArray(rand);
		rv.packedUint64 = nextRandomLongArray(rand);
		rv.packedSint64 = nextRandomLongArray(rand);

		rv.packedFixed32 = nextRandomIntArray(rand);
		rv.packedFixed64 = nextRandomLongArray(rand);

		rv.packedFixed32f = nextRandomFloatArray(rand);
		rv.packedFixed64f = nextRandomDoubleArray(rand);

		int n = rand.nextInt(32);

		if (n > 0) {
			rv.bytes = new byte[n];
			rand.nextBytes(rv.bytes);
		}

		n = rand.nextInt(32);
		if (n > 0)
			rv.string = TestUtil.nextRandomString(rand, n);

		if (depth < 8 && rand.nextBoolean())
			rv.message = nextRandom(rand, depth + 1);
		return rv;
	}

	static TestMessage nextRandom(Random rand) {
		return nextRandom(rand, 1);
	}

	boolean bool;
	int uint32;
	int sint32;
	long uint64;
	long sint64;

	int fixed32;
	long fixed64;

	float fixed32f;
	double fixed64f;

	boolean[] packedBool = EMPTY_BOOL;
	int[] packedUint32 = EMPTY_INT;
	int[] packedSint32 = EMPTY_INT;
	long[] packedUint64 = EMPTY_LONG;
	long[] packedSint64 = EMPTY_LONG;

	int[] packedFixed32 = EMPTY_INT;
	long[] packedFixed64 = EMPTY_LONG;

	float[] packedFixed32f = EMPTY_FLOAT;
	double[] packedFixed64f = EMPTY_DOUBLE;

	byte[] bytes = EMPTY_BYTE;
	String string = "";

	TestMessage message;

	boolean isEmpty() {
		return !this.bool &&
			this.uint32 == 0 &&
			this.sint32 == 0 &&
			this.uint64 == 0L &&
			this.sint64 == 0L &&
			this.fixed32 == 0 &&
			this.fixed64 == 0L &&
			this.fixed32f == 0.f &&
			this.fixed64f == 0. &&
			this.packedBool.length == 0 &&
			this.packedUint32.length == 0 &&
			this.packedSint32.length == 0 &&
			this.packedUint64.length == 0 &&
			this.packedSint64.length == 0 &&
			this.packedFixed32.length == 0 &&
			this.packedFixed64.length == 0 &&
			this.packedFixed32f.length == 0 &&
			this.packedFixed64f.length == 0 &&
			this.bytes.length == 0 &&
			this.string.isEmpty() &&
			(this.message == null || this.message.isEmpty());
	}

	void assertEqual(TestMessage got) {
		assertEquals(this.bool, got.bool);
		assertEquals(this.uint32, got.uint32);
		assertEquals(this.sint32, got.sint32);
		assertEquals(this.uint64, got.uint64);
		assertEquals(this.sint64, got.sint64);

		assertEquals(this.fixed32, got.fixed32);
		assertEquals(this.fixed64, got.fixed64);

		assertEquals(this.fixed32f, got.fixed32f, 0.f);
		assertEquals(this.fixed64f, got.fixed64f, 0.);

		assertArrayEquals(this.packedBool, got.packedBool);
		assertArrayEquals(this.packedUint32, got.packedUint32);
		assertArrayEquals(this.packedSint32, got.packedSint32);
		assertArrayEquals(this.packedUint64, got.packedUint64);
		assertArrayEquals(this.packedSint64, got.packedSint64);

		assertArrayEquals(this.packedFixed32, got.packedFixed32);
		assertArrayEquals(this.packedFixed64, got.packedFixed64);

		assertEquals(FloatBuffer.wrap(this.packedFixed32f), FloatBuffer.wrap(got.packedFixed32f));
		assertEquals(DoubleBuffer.wrap(this.packedFixed64f), DoubleBuffer.wrap(got.packedFixed64f));

		assertArrayEquals(this.bytes, got.bytes);
		assertEquals(this.string, got.string);

		if (this.message == null || this.message.isEmpty()) {
			assertTrue(got.message == null || got.message.isEmpty());
		} else {
			assertTrue(got.message != null && !got.message.isEmpty());
			this.message.assertEqual(got.message);
		}
	}

	private static boolean[] decodePackedBool(ByteBuffer src) throws IOException {
		CodedInputStream in = CodedInputStream.newInstance(src);
		boolean[] rv = new boolean[32];
		int i = 0;

		while (!in.isAtEnd()) {
			if (i == rv.length)
				rv = new boolean[rv.length * 2];
			rv[i++] = in.readBool();
		}
		return i == 0 ? EMPTY_BOOL : Arrays.copyOf(rv, i);
	}

	private static int[] decodePackedUint32(ByteBuffer src) throws IOException {
		CodedInputStream in = CodedInputStream.newInstance(src);
		int[] rv = new int[32];
		int i = 0;

		while (!in.isAtEnd()) {
			if (i == rv.length)
				rv = new int[rv.length * 2];
			rv[i++] = in.readUInt32();
		}
		return i == 0 ? EMPTY_INT : Arrays.copyOf(rv, i);
	}

	private static int[] decodePackedSint32(ByteBuffer src) throws IOException {
		CodedInputStream in = CodedInputStream.newInstance(src);
		int[] rv = new int[32];
		int i = 0;

		while (!in.isAtEnd()) {
			if (i == rv.length)
				rv = new int[rv.length * 2];
			rv[i++] = in.readSInt32();
		}
		return i == 0 ? EMPTY_INT : Arrays.copyOf(rv, i);
	}

	private static long[] decodePackedUint64(ByteBuffer src) throws IOException {
		CodedInputStream in = CodedInputStream.newInstance(src);
		long[] rv = new long[32];
		int i = 0;

		while (!in.isAtEnd()) {
			if (i == rv.length)
				rv = new long[rv.length * 2];
			rv[i++] = in.readUInt64();
		}
		return i == 0 ? EMPTY_LONG : Arrays.copyOf(rv, i);
	}

	private static long[] decodePackedSint64(ByteBuffer src) throws IOException {
		CodedInputStream in = CodedInputStream.newInstance(src);
		long[] rv = new long[32];
		int i = 0;

		while (!in.isAtEnd()) {
			if (i == rv.length)
				rv = new long[rv.length * 2];
			rv[i++] = in.readSInt64();
		}
		return i == 0 ? EMPTY_LONG : Arrays.copyOf(rv, i);
	}

	private static int[] decodePackedFixed32(ByteBuffer src) throws IOException {
		CodedInputStream in = CodedInputStream.newInstance(src);
		int[] rv = new int[32];
		int i = 0;

		while (!in.isAtEnd()) {
			if (i == rv.length)
				rv = new int[rv.length * 2];
			rv[i++] = in.readFixed32();
		}
		return i == 0 ? EMPTY_INT : Arrays.copyOf(rv, i);
	}

	private static long[] decodePackedFixed64(ByteBuffer src) throws IOException {
		CodedInputStream in = CodedInputStream.newInstance(src);
		long[] rv = new long[32];
		int i = 0;

		while (!in.isAtEnd()) {
			if (i == rv.length)
				rv = new long[rv.length * 2];
			rv[i++] = in.readFixed64();
		}
		return i == 0 ? EMPTY_LONG : Arrays.copyOf(rv, i);
	}

	private static float[] decodePackedFloat(ByteBuffer src) throws IOException {
		CodedInputStream in = CodedInputStream.newInstance(src);
		float[] rv = new float[32];
		int i = 0;

		while (!in.isAtEnd()) {
			if (i == rv.length)
				rv = new float[rv.length * 2];
			rv[i++] = in.readFloat();
		}
		return i == 0 ? EMPTY_FLOAT : Arrays.copyOf(rv, i);
	}

	private static double[] decodePackedDouble(ByteBuffer src) throws IOException {
		CodedInputStream in = CodedInputStream.newInstance(src);
		double[] rv = new double[32];
		int i = 0;

		while (!in.isAtEnd()) {
			if (i == rv.length)
				rv = new double[rv.length * 2];
			rv[i++] = in.readDouble();
		}
		return i == 0 ? EMPTY_DOUBLE : Arrays.copyOf(rv, i);
	}

	void decode(CodedInputStream in) throws IOException {
		while (!in.isAtEnd()) {
			int tag = in.readTag();

			if (tag == TAG_BOOL) {
				this.bool = in.readBool();
			} else if (tag == TAG_UINT32) {
				this.uint32 = in.readUInt32();
			} else if (tag == TAG_SINT32) {
				this.sint32 = in.readSInt32();
			} else if (tag == TAG_UINT64) {
				this.uint64 = in.readUInt64();
			} else if (tag == TAG_SINT64) {
				this.sint64 = in.readSInt64();
			} else if (tag == TAG_FIXED32) {
				this.fixed32 = in.readFixed32();
			} else if (tag == TAG_FIXED64) {
				this.fixed64 = in.readFixed64();
			} else if (tag == TAG_FIXED32F) {
				this.fixed32f = in.readFloat();
			} else if (tag == TAG_FIXED64F) {
				this.fixed64f = in.readDouble();
			} else if (tag == TAG_PACKEDBOOL) {
				this.packedBool = decodePackedBool(in.readByteBuffer());
			} else if (tag == TAG_PACKEDUINT32) {
				this.packedUint32 = decodePackedUint32(in.readByteBuffer());
			} else if (tag == TAG_PACKEDSINT32) {
				this.packedSint32 = decodePackedSint32(in.readByteBuffer());
			} else if (tag == TAG_PACKEDUINT64) {
				this.packedUint64 = decodePackedUint64(in.readByteBuffer());
			} else if (tag == TAG_PACKEDSINT64) {
				this.packedSint64 = decodePackedSint64(in.readByteBuffer());
			} else if (tag == TAG_PACKEDFIXED32) {
				this.packedFixed32 = decodePackedFixed32(in.readByteBuffer());
			} else if (tag == TAG_PACKEDFIXED64) {
				this.packedFixed64 = decodePackedFixed64(in.readByteBuffer());
			} else if (tag == TAG_PACKEDFIXED32F) {
				this.packedFixed32f = decodePackedFloat(in.readByteBuffer());
			} else if (tag == TAG_PACKEDFIXED64F) {
				this.packedFixed64f = decodePackedDouble(in.readByteBuffer());
			} else if (tag == TAG_BYTES) {
				this.bytes = in.readByteArray();
			} else if (tag == TAG_STRING) {
				this.string = in.readString();
			} else {
				assertEquals(TAG_MESSAGE, tag);
				this.message = new TestMessage();
				this.message.decode(CodedInputStream.newInstance(in.readByteBuffer()));
			}
		}
	}

	void decodeSplit(CodedInputStream schema, CodedInputStream content) throws IOException {
		while (!schema.isAtEnd()) {
			int tag = schema.readTag();
			int wlen = wireTypeOfFieldTag(tag) == WIRE_LEN ? schema.readUInt32() : 0;
			int slen = wireTypeOfFieldTag(tag) == WIRE_LEN ? schema.readUInt32() : 0;

			if (tag == TAG_BOOL) {
				this.bool = content.readBool();
			} else if (tag == TAG_UINT32) {
				this.uint32 = content.readUInt32();
			} else if (tag == TAG_SINT32) {
				this.sint32 = content.readSInt32();
			} else if (tag == TAG_UINT64) {
				this.uint64 = content.readUInt64();
			} else if (tag == TAG_SINT64) {
				this.sint64 = content.readSInt64();
			} else if (tag == TAG_FIXED32) {
				this.fixed32 = content.readFixed32();
			} else if (tag == TAG_FIXED64) {
				this.fixed64 = content.readFixed64();
			} else if (tag == TAG_FIXED32F) {
				this.fixed32f = content.readFloat();
			} else if (tag == TAG_FIXED64F) {
				this.fixed64f = content.readDouble();
			} else if (tag == TAG_PACKEDBOOL) {
				assertEquals(0, slen);
				this.packedBool = decodePackedBool(ByteBuffer.wrap(content.readRawBytes(wlen)));
			} else if (tag == TAG_PACKEDUINT32) {
				assertEquals(0, slen);
				this.packedUint32 = decodePackedUint32(ByteBuffer.wrap(content.readRawBytes(wlen)));
			} else if (tag == TAG_PACKEDSINT32) {
				assertEquals(0, slen);
				this.packedSint32 = decodePackedSint32(ByteBuffer.wrap(content.readRawBytes(wlen)));
			} else if (tag == TAG_PACKEDUINT64) {
				assertEquals(0, slen);
				this.packedUint64 = decodePackedUint64(ByteBuffer.wrap(content.readRawBytes(wlen)));
			} else if (tag == TAG_PACKEDSINT64) {
				assertEquals(0, slen);
				this.packedSint64 = decodePackedSint64(ByteBuffer.wrap(content.readRawBytes(wlen)));
			} else if (tag == TAG_PACKEDFIXED32) {
				assertEquals(0, slen);
				this.packedFixed32 =
					decodePackedFixed32(ByteBuffer.wrap(content.readRawBytes(wlen)));
			} else if (tag == TAG_PACKEDFIXED64) {
				assertEquals(0, slen);
				this.packedFixed64 =
					decodePackedFixed64(ByteBuffer.wrap(content.readRawBytes(wlen)));
			} else if (tag == TAG_PACKEDFIXED32F) {
				assertEquals(0, slen);
				this.packedFixed32f =
					decodePackedFloat(ByteBuffer.wrap(content.readRawBytes(wlen)));
			} else if (tag == TAG_PACKEDFIXED64F) {
				assertEquals(0, slen);
				this.packedFixed64f =
					decodePackedDouble(ByteBuffer.wrap(content.readRawBytes(wlen)));
			} else if (tag == TAG_BYTES) {
				assertEquals(0, slen);
				this.bytes = content.readRawBytes(wlen);
			} else if (tag == TAG_STRING) {
				assertEquals(0, slen);
				this.string = new String(content.readRawBytes(wlen), StandardCharsets.UTF_8);
			} else {
				assertEquals(TAG_MESSAGE, tag);

				CodedInputStream msgSchema =
					CodedInputStream.newInstance(schema.readRawBytes(slen));
				CodedInputStream msgContent =
					CodedInputStream.newInstance(content.readRawBytes(wlen - slen));

				this.message = new TestMessage();
				this.message.decodeSplit(msgSchema, msgContent);
			}
		}
		assertTrue(content.isAtEnd());
	}

	void encode(CodedOutputStream out) throws IOException {
		if (this.bool)
			out.writeBool(fieldNumberOfFieldTag(TAG_BOOL), this.bool);
		if (this.uint32 != 0)
			out.writeUInt32(fieldNumberOfFieldTag(TAG_UINT32), this.uint32);
		if (this.sint32 != 0)
			out.writeSInt32(fieldNumberOfFieldTag(TAG_SINT32), this.sint32);
		if (this.uint64 != 0L)
			out.writeUInt64(fieldNumberOfFieldTag(TAG_UINT64), this.uint64);
		if (this.sint64 != 0L)
			out.writeSInt64(fieldNumberOfFieldTag(TAG_SINT64), this.sint64);

		if (this.fixed32 != 0)
			out.writeFixed32(fieldNumberOfFieldTag(TAG_FIXED32), this.fixed32);
		if (this.fixed64 != 0L)
			out.writeFixed64(fieldNumberOfFieldTag(TAG_FIXED64), this.fixed64);

		if (this.fixed32f != 0.f)
			out.writeFloat(fieldNumberOfFieldTag(TAG_FIXED32F), this.fixed32f);
		if (this.fixed64f != 0.)
			out.writeDouble(fieldNumberOfFieldTag(TAG_FIXED64F), this.fixed64f);

		if (this.packedBool.length != 0) {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			CodedOutputStream packed = CodedOutputStream.newInstance(bytes);

			for (boolean val : this.packedBool)
				packed.writeBoolNoTag(val);
			packed.flush();
			out.writeByteArray(fieldNumberOfFieldTag(TAG_PACKEDBOOL), bytes.toByteArray());
		}
		if (this.packedUint32.length != 0) {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			CodedOutputStream packed = CodedOutputStream.newInstance(bytes);

			for (int val : this.packedUint32)
				packed.writeUInt32NoTag(val);
			packed.flush();
			out.writeByteArray(fieldNumberOfFieldTag(TAG_PACKEDUINT32), bytes.toByteArray());
		}
		if (this.packedSint32.length != 0) {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			CodedOutputStream packed = CodedOutputStream.newInstance(bytes);

			for (int val : this.packedSint32)
				packed.writeSInt32NoTag(val);
			packed.flush();
			out.writeByteArray(fieldNumberOfFieldTag(TAG_PACKEDSINT32), bytes.toByteArray());
		}
		if (this.packedUint64.length != 0) {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			CodedOutputStream packed = CodedOutputStream.newInstance(bytes);

			for (long val : this.packedUint64)
				packed.writeUInt64NoTag(val);
			packed.flush();
			out.writeByteArray(fieldNumberOfFieldTag(TAG_PACKEDUINT64), bytes.toByteArray());
		}
		if (this.packedSint64.length != 0) {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			CodedOutputStream packed = CodedOutputStream.newInstance(bytes);

			for (long val : this.packedSint64)
				packed.writeSInt64NoTag(val);
			packed.flush();
			out.writeByteArray(fieldNumberOfFieldTag(TAG_PACKEDSINT64), bytes.toByteArray());
		}

		if (this.packedFixed32.length != 0) {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			CodedOutputStream packed = CodedOutputStream.newInstance(bytes);

			for (int val : this.packedFixed32)
				packed.writeFixed32NoTag(val);
			packed.flush();
			out.writeByteArray(fieldNumberOfFieldTag(TAG_PACKEDFIXED32), bytes.toByteArray());
		}
		if (this.packedFixed64.length != 0) {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			CodedOutputStream packed = CodedOutputStream.newInstance(bytes);

			for (long val : this.packedFixed64)
				packed.writeFixed64NoTag(val);
			packed.flush();
			out.writeByteArray(fieldNumberOfFieldTag(TAG_PACKEDFIXED64), bytes.toByteArray());
		}

		if (this.packedFixed32f.length != 0) {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			CodedOutputStream packed = CodedOutputStream.newInstance(bytes);

			for (float val : this.packedFixed32f)
				packed.writeFloatNoTag(val);
			packed.flush();
			out.writeByteArray(fieldNumberOfFieldTag(TAG_PACKEDFIXED32F), bytes.toByteArray());
		}
		if (this.packedFixed64f.length != 0) {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			CodedOutputStream packed = CodedOutputStream.newInstance(bytes);

			for (double val : this.packedFixed64f)
				packed.writeDoubleNoTag(val);
			packed.flush();
			out.writeByteArray(fieldNumberOfFieldTag(TAG_PACKEDFIXED64F), bytes.toByteArray());
		}

		if (this.bytes.length != 0)
			out.writeByteArray(fieldNumberOfFieldTag(TAG_BYTES), this.bytes);
		if (!this.string.isEmpty())
			out.writeString(fieldNumberOfFieldTag(TAG_STRING), this.string);

		if (this.message != null && !this.message.isEmpty()) {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			CodedOutputStream packed = CodedOutputStream.newInstance(bytes);

			this.message.encode(packed);
			packed.flush();
			out.writeByteArray(fieldNumberOfFieldTag(TAG_MESSAGE), bytes.toByteArray());
		}
	}

	void encodeSplit(CodedOutputStream schema, CodedOutputStream content) throws IOException {
		if (this.bool) {
			schema.writeUInt32NoTag(TAG_BOOL);
			content.writeBoolNoTag(this.bool);
		}
		if (this.uint32 != 0) {
			schema.writeUInt32NoTag(TAG_UINT32);
			content.writeUInt32NoTag(this.uint32);
		}
		if (this.sint32 != 0) {
			schema.writeUInt32NoTag(TAG_SINT32);
			content.writeSInt32NoTag(this.sint32);
		}
		if (this.uint64 != 0L) {
			schema.writeUInt32NoTag(TAG_UINT64);
			content.writeUInt64NoTag(this.uint64);
		}
		if (this.sint64 != 0L) {
			schema.writeUInt32NoTag(TAG_SINT64);
			content.writeSInt64NoTag(this.sint64);
		}

		if (this.fixed32 != 0) {
			schema.writeUInt32NoTag(TAG_FIXED32);
			content.writeFixed32NoTag(this.fixed32);
		}
		if (this.fixed64 != 0L) {
			schema.writeUInt32NoTag(TAG_FIXED64);
			content.writeFixed64NoTag(this.fixed64);
		}

		if (this.fixed32f != 0.f) {
			schema.writeUInt32NoTag(TAG_FIXED32F);
			content.writeFloatNoTag(this.fixed32f);
		}
		if (this.fixed64f != 0.) {
			schema.writeUInt32NoTag(TAG_FIXED64F);
			content.writeDoubleNoTag(this.fixed64f);
		}

		if (this.packedBool.length != 0) {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			CodedOutputStream packed = CodedOutputStream.newInstance(bytes);

			for (boolean val : this.packedBool)
				packed.writeBoolNoTag(val);
			packed.flush();

			byte[] val = bytes.toByteArray();

			schema.writeUInt32NoTag(TAG_PACKEDBOOL);
			schema.writeUInt32NoTag(val.length);
			schema.writeUInt32NoTag(0);
			content.writeRawBytes(val);
		}
		if (this.packedUint32.length != 0) {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			CodedOutputStream packed = CodedOutputStream.newInstance(bytes);

			for (int val : this.packedUint32)
				packed.writeUInt32NoTag(val);
			packed.flush();

			byte[] val = bytes.toByteArray();

			schema.writeUInt32NoTag(TAG_PACKEDUINT32);
			schema.writeUInt32NoTag(val.length);
			schema.writeUInt32NoTag(0);
			content.writeRawBytes(val);
		}
		if (this.packedSint32.length != 0) {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			CodedOutputStream packed = CodedOutputStream.newInstance(bytes);

			for (int val : this.packedSint32)
				packed.writeSInt32NoTag(val);
			packed.flush();

			byte[] val = bytes.toByteArray();

			schema.writeUInt32NoTag(TAG_PACKEDSINT32);
			schema.writeUInt32NoTag(val.length);
			schema.writeUInt32NoTag(0);
			content.writeRawBytes(val);
		}
		if (this.packedUint64.length != 0) {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			CodedOutputStream packed = CodedOutputStream.newInstance(bytes);

			for (long val : this.packedUint64)
				packed.writeUInt64NoTag(val);
			packed.flush();

			byte[] val = bytes.toByteArray();

			schema.writeUInt32NoTag(TAG_PACKEDUINT64);
			schema.writeUInt32NoTag(val.length);
			schema.writeUInt32NoTag(0);
			content.writeRawBytes(val);
		}
		if (this.packedSint64.length != 0) {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			CodedOutputStream packed = CodedOutputStream.newInstance(bytes);

			for (long val : this.packedSint64)
				packed.writeSInt64NoTag(val);
			packed.flush();

			byte[] val = bytes.toByteArray();

			schema.writeUInt32NoTag(TAG_PACKEDSINT64);
			schema.writeUInt32NoTag(val.length);
			schema.writeUInt32NoTag(0);
			content.writeRawBytes(val);
		}

		if (this.packedFixed32.length != 0) {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			CodedOutputStream packed = CodedOutputStream.newInstance(bytes);

			for (int val : this.packedFixed32)
				packed.writeFixed32NoTag(val);
			packed.flush();

			byte[] val = bytes.toByteArray();

			schema.writeUInt32NoTag(TAG_PACKEDFIXED32);
			schema.writeUInt32NoTag(val.length);
			schema.writeUInt32NoTag(0);
			content.writeRawBytes(val);
		}
		if (this.packedFixed64.length != 0) {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			CodedOutputStream packed = CodedOutputStream.newInstance(bytes);

			for (long val : this.packedFixed64)
				packed.writeFixed64NoTag(val);
			packed.flush();

			byte[] val = bytes.toByteArray();

			schema.writeUInt32NoTag(TAG_PACKEDFIXED64);
			schema.writeUInt32NoTag(val.length);
			schema.writeUInt32NoTag(0);
			content.writeRawBytes(val);
		}

		if (this.packedFixed32f.length != 0) {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			CodedOutputStream packed = CodedOutputStream.newInstance(bytes);

			for (float val : this.packedFixed32f)
				packed.writeFloatNoTag(val);
			packed.flush();

			byte[] val = bytes.toByteArray();

			schema.writeUInt32NoTag(TAG_PACKEDFIXED32F);
			schema.writeUInt32NoTag(val.length);
			schema.writeUInt32NoTag(0);
			content.writeRawBytes(val);
		}
		if (this.packedFixed64f.length != 0) {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			CodedOutputStream packed = CodedOutputStream.newInstance(bytes);

			for (double val : this.packedFixed64f)
				packed.writeDoubleNoTag(val);
			packed.flush();

			byte[] val = bytes.toByteArray();

			schema.writeUInt32NoTag(TAG_PACKEDFIXED64F);
			schema.writeUInt32NoTag(val.length);
			schema.writeUInt32NoTag(0);
			content.writeRawBytes(val);
		}

		if (this.bytes.length != 0) {
			schema.writeUInt32NoTag(TAG_BYTES);
			schema.writeUInt32NoTag(this.bytes.length);
			schema.writeUInt32NoTag(0);
			content.writeRawBytes(this.bytes);
		}
		if (!this.string.isEmpty()) {
			byte[] val = this.string.getBytes(StandardCharsets.UTF_8);

			schema.writeUInt32NoTag(TAG_STRING);
			schema.writeUInt32NoTag(val.length);
			schema.writeUInt32NoTag(0);
			content.writeRawBytes(val);
		}

		if (this.message != null && !this.message.isEmpty()) {
			ByteArrayOutputStream msgSchemaBytes = new ByteArrayOutputStream();
			ByteArrayOutputStream msgContentBytes = new ByteArrayOutputStream();
			CodedOutputStream msgSchema = CodedOutputStream.newInstance(msgSchemaBytes);
			CodedOutputStream msgContent = CodedOutputStream.newInstance(msgContentBytes);

			this.message.encodeSplit(msgSchema, msgContent);

			msgSchema.flush();
			msgContent.flush();

			byte[] a = msgSchemaBytes.toByteArray();
			byte[] b = msgContentBytes.toByteArray();

			schema.writeUInt32NoTag(TAG_MESSAGE);
			schema.writeUInt32NoTag(a.length + b.length);
			schema.writeUInt32NoTag(a.length);
			schema.writeRawBytes(a);
			content.writeRawBytes(b);
		}
	}
}
