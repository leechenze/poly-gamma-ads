// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.protobuf;

import android.util.Pair;

import androidx.annotation.VisibleForTesting;

import org.polygamma.android.origin.util.Preconditions;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Protocol buffer message reader.
 * <p>Instances of this read Protobuf messages using the {@code read} family of methods. Callers
 * should continue to read field tags and values until {@link #hasRemaining()} returns {@code
 * false}. Field tag must be read, using {@link #readTag()}, after which the field's value may
 * be read. If {@link #readTag()} is invoked without reading the field's value, the fields value
 * is simply discarded.
 *
 * @since 1.2
 */
public final class ProtobufReader {

	private final ByteBuffer buffer;
	private int currentWireType;

	/**
	 * Construct new reader with an existing buffer.
	 * <p>This retains a {@linkplain ByteBuffer#duplicate() duplicate} reference to {@code buffer}.
	 * While the duplicate reference maintains an independent {@linkplain ByteBuffer#position()
	 * position}, {@linkplain ByteBuffer#limit() limit}, and {@linkplain ByteBuffer#mark() mark},
	 * the data underlying the duplicate buffer is shared with {@code buffer}. As such, any
	 * modifications to the underlying data will be visible in the duplicated buffer reference.
	 *
	 * @param buffer buffer to read from
	 * @since 1.2
	 */
	public ProtobufReader(ByteBuffer buffer) {
		this.buffer = buffer.duplicate()
			.order(ByteOrder.LITTLE_ENDIAN);
		this.currentWireType = -1;
	}

	/**
	 * Test whether additional data is available for reading.
	 *
	 * @return {@code true} if, and only if, additional data is available
	 * @since 1.2
	 */
	public boolean hasRemaining() {
		return this.buffer.hasRemaining();
	}

	/**
	 * Read {@link Wire#TYPE_VARINT varint} of maximum size.
	 *
	 * @param maxSize maximum size, in bytes, of {@code varint}
	 * @param maxTailByte expected tail {@code byte} value
	 * @return read {@code varint}
	 * @throws java.nio.BufferUnderflowException insufficient data remaining in underlying buffer
	 * @throws IllegalStateException coding is malformed
	 */
	private long readVarint(int maxSize, int maxTailByte) {
		int a = this.buffer.get() & 0xff;

		if (a < 0x80)
			return a;

		int b = this.buffer.get() & 0xff;

		a &= 0x7f;
		if (b < 0x80)
			return a | (b << 7);

		long rv = a | ((b & 0x7f) << 7);

		for (int i = 2; i < (maxSize - 1); i++) {
			int c = this.buffer.get() & 0xff;

			rv |= (c & 0x7fL) << (i * 7);
			if (c < 0x80)
				return rv;
		}

		int c = this.buffer.get() & 0xff;

		Preconditions.checkState(c <= maxTailByte, "varint overflows tail byte");
		return rv | (((long) c) << ((maxSize - 1) * 7));
	}

	/**
	 * Read 32-bit {@link Wire#TYPE_VARINT varint}.

	 * @return read {@code varint}
	 * @throws java.nio.BufferUnderflowException insufficient data remaining in underlying buffer
	 * @throws IllegalStateException coding is malformed
	 */
	@VisibleForTesting
	int readVarint32() {
		return (int) this.readVarint(Wire.MAX_INT32_VARINT_SIZE, 0x0f);
	}

	/**
	 * Read 64-bit {@link Wire#TYPE_VARINT varint}.

	 * @return read {@code varint}
	 * @throws java.nio.BufferUnderflowException insufficient data remaining in underlying buffer
	 * @throws IllegalStateException coding is malformed
	 */
	@VisibleForTesting
	long readVarint64() {
		return this.readVarint(Wire.MAX_INT64_VARINT_SIZE, 0x01);
	}

	/**
	 * Read and discard current value.
	 *
	 * @throws java.nio.BufferUnderflowException insufficient data remaining in underlying buffer
	 * @throws IllegalStateException coding is malformed
	 */
	private void readAndDiscard() {
		switch (this.currentWireType) {
		case Wire.TYPE_FIXED32:
			this.buffer.getInt();
			break;
		case Wire.TYPE_FIXED64:
			this.buffer.getLong();
			break;
		case Wire.TYPE_LEN:
			int len = this.readVarint32();

			this.buffer.position(this.buffer.position() + len);
			break;
		case Wire.TYPE_VARINT:
			this.readVarint64();
			break;
		default:
			throw new AssertionError();
		}
		this.currentWireType = -1;
	}

	/**
	 * Read field tag.
	 * <p>Upon return, the relevant {@code read} method for reading the field value type should
	 * be invoked. If a value is not read, then a subsequent invocation of this method will read
	 * and discard the value for the previously returned field tag.
	 *
	 * @return field tag or {@code 0} if end-of-input has been reached
	 * @throws java.nio.BufferUnderflowException insufficient data remaining in underlying buffer
	 * @throws IllegalStateException coding is malformed
	 * @since 1.2
	 */
	public @ProtobufField.Tag int readTag() {
		if (this.currentWireType != -1)
			this.readAndDiscard();
		if (!this.buffer.hasRemaining())
			return 0;

		int tag = this.readVarint32();

		this.currentWireType = tag & ProtobufField.TAG_TYPE_MASK;
		return tag;
	}

	/**
	 * Consume wire type for current field tag, if any.
	 *
	 * @param exp expected wire type
	 * @throws IllegalStateException current wire type is not {@code exp}
	 */
	private void consumeCurrentWireType(@Wire.Type int exp) {
		Preconditions.checkState(
			this.currentWireType == exp,
			"expected wire type %s, got %s",
			this.currentWireType, exp
		);
		this.currentWireType = -1;
	}

	/**
	 * Current field value is a {@code varint} value.
	 *
	 * @return {@code true} if, and only if, current value is a {@code varint} value
	 * @since 1.2
	 */
	public boolean isCurrentVarint() {
		return this.currentWireType == Wire.TYPE_VARINT;
	}

	/**
	 * Current field value is a {@code fixed32} value.
	 *
	 * @return {@code true} if, and only if, current value is a {@code fixed32} value
	 * @since 1.2
	 */
	public boolean isCurrentFixed32() {
		return this.currentWireType == Wire.TYPE_FIXED32;
	}

	/**
	 * Current field value is a {@code fixed64} value.
	 *
	 * @return {@code true} if, and only if, current value is a {@code fixed64} value
	 * @since 1.2
	 */
	public boolean isCurrentFixed64() {
		return this.currentWireType == Wire.TYPE_FIXED64;
	}

	/**
	 * Current field value is a {@code len} sequence.
	 *
	 * @return {@code true} if, and only if, current value is a {@code len} sequence
	 * @since 1.2
	 */
	public boolean isCurrentLen() {
		return this.currentWireType == Wire.TYPE_LEN;
	}

	/**
	 * Read {@code int32} field value.
	 *
	 * @return read value
	 * @throws java.nio.BufferUnderflowException insufficient data remaining in underlying buffer
	 * @throws IllegalStateException coding is malformed or previously read field {@linkplain
	 * #readTag() tag} does not have an {@code int32} field value type
	 * @since 1.2
	 */
	public int readInt32() {
		this.consumeCurrentWireType(Wire.TYPE_VARINT);
		return this.readVarint32();
	}

	/**
	 * Read {@code sint32} field value.
	 *
	 * @return read value
	 * @throws java.nio.BufferUnderflowException insufficient data remaining in underlying buffer
	 * @throws IllegalStateException coding is malformed or previously read field {@linkplain
	 * #readTag() tag} does not have an {@code sint32} field value type
	 * @since 1.2
	 */
	public int readSint32() {
		return Wire.sint32OfVarint(this.readInt32());
	}

	/**
	 * Read {@code fixed32} field value.
	 *
	 * @return read value
	 * @throws java.nio.BufferUnderflowException insufficient data remaining in underlying buffer
	 * @throws IllegalStateException coding is malformed or previously read field {@linkplain
	 * #readTag() tag} does not have an {@code fixed32} field value type
	 * @since 1.2
	 */
	public int readFixed32() {
		this.consumeCurrentWireType(Wire.TYPE_FIXED32);
		return this.buffer.getInt();
	}

	/**
	 * Read {@code int64} field value.
	 *
	 * @return read value
	 * @throws java.nio.BufferUnderflowException insufficient data remaining in underlying buffer
	 * @throws IllegalStateException coding is malformed or previously read field {@linkplain
	 * #readTag() tag} does not have an {@code int64} field value type
	 * @since 1.2
	 */
	public long readInt64() {
		this.consumeCurrentWireType(Wire.TYPE_VARINT);
		return this.readVarint64();
	}

	/**
	 * Read {@code sint64} field value.
	 *
	 * @return read value
	 * @throws java.nio.BufferUnderflowException insufficient data remaining in underlying buffer
	 * @throws IllegalStateException coding is malformed or previously read field {@linkplain
	 * #readTag() tag} does not have an {@code sint64} field value type
	 * @since 1.2
	 */
	public long readSint64() {
		return Wire.sint64OfVarint(this.readInt64());
	}

	/**
	 * Read {@code fixed64} field value.
	 *
	 * @return read value
	 * @throws java.nio.BufferUnderflowException insufficient data remaining in underlying buffer
	 * @throws IllegalStateException coding is malformed or previously read field {@linkplain
	 * #readTag() tag} does not have an {@code fixed64} field value type
	 * @since 1.2
	 */
	public long readFixed64() {
		this.consumeCurrentWireType(Wire.TYPE_FIXED64);
		return this.buffer.getLong();
	}

	/**
	 * Read {@code float} field value.
	 *
	 * @return read value
	 * @throws java.nio.BufferUnderflowException insufficient data remaining in underlying buffer
	 * @throws IllegalStateException coding is malformed or previously read field {@linkplain
	 * #readTag() tag} does not have an {@code float} field value type
	 * @since 1.2
	 */
	public float readFloat() {
		this.consumeCurrentWireType(Wire.TYPE_FIXED32);
		return this.buffer.getFloat();
	}

	/**
	 * Read {@code double} field value.
	 *
	 * @return read value
	 * @throws java.nio.BufferUnderflowException insufficient data remaining in underlying buffer
	 * @throws IllegalStateException coding is malformed or previously read field {@linkplain
	 * #readTag() tag} does not have an {@code double} field value type
	 * @since 1.2
	 */
	public double readDouble() {
		this.consumeCurrentWireType(Wire.TYPE_FIXED64);
		return this.buffer.getDouble();
	}

	/**
	 * Read {@code bool} field value.
	 *
	 * @return read value
	 * @throws java.nio.BufferUnderflowException insufficient data remaining in underlying buffer
	 * @throws IllegalStateException coding is malformed or previously read field {@linkplain
	 * #readTag() tag} does not have an {@code bool} field value type
	 * @since 1.2
	 */
	public boolean readBool() {
		this.consumeCurrentWireType(Wire.TYPE_VARINT);
		return this.readVarint64() != 0L;
	}

	/**
	 * Begin reading {@code len} sequence field.
	 * <p>This prepares this reader for a {@code len} sequence field. Upon return, the {@code len}
	 * sequence must be read. Once the sequence is read, {@link #endReadLen(int)} must be invoked
	 * with the cookie value returned by this.
	 * {@snippet lang="java" :
	 * int cookie = reader.beginReadLen();
	 * // ... read sequence from `reader`
	 * reader.endReadLen(cookie); // @link substring="endReadLen" target="#endReadLen(int)"
	 * }
	 *
	 * @return field cookie
	 * @throws java.nio.BufferUnderflowException insufficient data remaining in underlying buffer
	 * @throws IllegalStateException coding is malformed or previously read field {@linkplain
	 * #readTag() tag} does not have an {@code len} field value type
	 * @since 1.2
	 * @see #endReadLen(int)
	 */
	@SuppressWarnings("JavadocDeclaration")
	public int beginReadLen() {
		this.consumeCurrentWireType(Wire.TYPE_LEN);

		int len = this.readVarint32();
		int lim = this.buffer.limit();

		this.buffer.limit(this.buffer.position() + len);
		return lim;
	}

	/**
	 * End reading {@code len} sequence field.
	 * <p>This completes reading a sequence field began using {@link #beginReadLen()}. This
	 * <b>must</b> be invoked with the cookie returned by {@link #beginReadLen()}.
	 *
	 * @param cookie field cookie
	 * @since 1.2
	 * @see #beginReadLen()
	 */
	public void endReadLen(int cookie) {
		Preconditions.checkState(!this.buffer.hasRemaining(), "`len` sequence not fully consumed");
		this.buffer.limit(cookie);
	}

	/**
	 * Count number of packed {@link Wire#TYPE_VARINT varint}.
	 *
	 * @return {@code varint} count
	 */
	private int countPackedVarint() {
		int pos = this.buffer.position();
		int lim = this.buffer.limit();
		int num = 0;

		for (; (lim - pos) >= 8; pos += 8) {
			long w = this.buffer.getLong(pos);

			// see https://graphics.stanford.edu/%7Eseander/bithacks.html#HasLessInWord
			num += (int) Long.remainderUnsigned(Long.divideUnsigned(
				(~0L - (w & 0x7f7f7f7f7f7f7f7fL)) & ~w & 0x8080808080808080L,
				0x80
			), 0xff);
		}
		for (; lim > pos; pos++) {
			if ((this.buffer.get(pos) & 0xff) < 0x80)
				num++;
		}
		return num;
	}

	/**
	 * Read packed {@code int32} field value as an {@code int} array.
	 *
	 * @return read value
	 * @throws java.nio.BufferUnderflowException insufficient data remaining in underlying buffer
	 * @throws IllegalStateException coding is malformed or previously read field {@linkplain
	 * #readTag() tag} does not have an {@code len} field value type
	 * @since 1.2
	 */
	public int[] readPackedInt32() {
		int cookie = this.beginReadLen();
		int num = this.countPackedVarint();
		int[] rv = new int[num];

		for (int i = 0; i < num; i++)
			rv[i] = this.readVarint32();
		this.endReadLen(cookie);
		return rv;
	}

	/**
	 * Read packed {@code sint32} field value as an {@code int} array.
	 *
	 * @return read value
	 * @throws java.nio.BufferUnderflowException insufficient data remaining in underlying buffer
	 * @throws IllegalStateException coding is malformed or previously read field {@linkplain
	 * #readTag() tag} does not have an {@code len} field value type
	 * @since 1.2
	 */
	public int[] readPackedSint32() {
		int[] rv = this.readPackedInt32();

		for (int i = 0; i < rv.length; i++)
			rv[i] = Wire.sint32OfVarint(rv[i]);
		return rv;
	}

	/**
	 * Read packed {@code fixed32} field value as an {@code int} array.
	 *
	 * @return read value
	 * @throws java.nio.BufferUnderflowException insufficient data remaining in underlying buffer
	 * @throws IllegalStateException coding is malformed or previously read field {@linkplain
	 * #readTag() tag} does not have an {@code len} field value type
	 * @since 1.2
	 */
	public int[] readPackedFixed32() {
		int cookie = this.beginReadLen();
		IntBuffer src = this.buffer.asIntBuffer();
		int[] rv = new int[src.remaining()];

		src.get(rv);
		this.buffer.position(this.buffer.position() + (rv.length * 4));
		this.endReadLen(cookie);
		return rv;
	}

	/**
	 * Read packed {@code int32} field value into a word bitmap.
	 * <p>This is an efficient equivalent of:
	 * {@snippet lang="java" :
	 * long word = 0;
	 *
	 * for (int val : readPackedInt32()) { // @link substring="readPackedInt32" target="#readPackedInt32()"
	 *     int i = val - delta;
	 *
	 *     if (i >= 0 && i <= 63)
	 *         word |= (1L << i);
	 * }
	 * }
	 *
	 * @param delta delta to decrement values by
	 * @return read bitmap
	 * @throws java.nio.BufferUnderflowException insufficient data remaining in underlying buffer
	 * @throws IllegalStateException coding is malformed or previously read field {@linkplain
	 * #readTag() tag} does not have an {@code len} field value type
	 * @since 1.2
	 */
	@SuppressWarnings("JavadocDeclaration")
	public long readWordBitmap(int delta) {
		int cookie = this.beginReadLen();
		long rv = 0L;

		while (this.hasRemaining()) {
			int i = this.readVarint32() - delta;

			if (i >= 0 && i <= 63)
				rv |= (1L << i);
		}
		this.endReadLen(cookie);
		return rv;
	}

	/**
	 * Read packed {@code int64} field value as an {@code long} array.
	 *
	 * @return read value
	 * @throws java.nio.BufferUnderflowException insufficient data remaining in underlying buffer
	 * @throws IllegalStateException coding is malformed or previously read field {@linkplain
	 * #readTag() tag} does not have an {@code len} field value type
	 * @since 1.2
	 */
	public long[] readPackedInt64() {
		int cookie = this.beginReadLen();
		int num = this.countPackedVarint();
		long[] rv = new long[num];

		for (int i = 0; i < num; i++)
			rv[i] = this.readVarint64();
		this.endReadLen(cookie);
		return rv;
	}

	/**
	 * Read packed {@code sint64} field value as an {@code long} array.
	 *
	 * @return read value
	 * @throws java.nio.BufferUnderflowException insufficient data remaining in underlying buffer
	 * @throws IllegalStateException coding is malformed or previously read field {@linkplain
	 * #readTag() tag} does not have an {@code len} field value type
	 * @since 1.2
	 */
	public long[] readPackedSint64() {
		long[] rv = this.readPackedInt64();

		for (int i = 0; i < rv.length; i++)
			rv[i] = Wire.sint64OfVarint(rv[i]);
		return rv;
	}

	/**
	 * Read packed {@code fixed64} field value as an {@code long} array.
	 *
	 * @return read value
	 * @throws java.nio.BufferUnderflowException insufficient data remaining in underlying buffer
	 * @throws IllegalStateException coding is malformed or previously read field {@linkplain
	 * #readTag() tag} does not have an {@code len} field value type
	 * @since 1.2
	 */
	public long[] readPackedFixed64() {
		int cookie = this.beginReadLen();
		LongBuffer src = this.buffer.asLongBuffer();
		long[] rv = new long[src.remaining()];

		src.get(rv);
		this.buffer.position(this.buffer.position() + (rv.length * 8));
		this.endReadLen(cookie);
		return rv;
	}

	/**
	 * Read packed {@code float} field value as an {@code float} array.
	 *
	 * @return read value
	 * @throws java.nio.BufferUnderflowException insufficient data remaining in underlying buffer
	 * @throws IllegalStateException coding is malformed or previously read field {@linkplain
	 * #readTag() tag} does not have an {@code len} field value type
	 * @since 1.2
	 */
	public float[] readPackedFloat() {
		int cookie = this.beginReadLen();
		FloatBuffer src = this.buffer.asFloatBuffer();
		float[] rv = new float[src.remaining()];

		src.get(rv);
		this.buffer.position(this.buffer.position() + (rv.length * 4));
		this.endReadLen(cookie);
		return rv;
	}

	/**
	 * Read packed {@code double} field value as an {@code double} array.
	 *
	 * @return read value
	 * @throws java.nio.BufferUnderflowException insufficient data remaining in underlying buffer
	 * @throws IllegalStateException coding is malformed or previously read field {@linkplain
	 * #readTag() tag} does not have an {@code len} field value type
	 * @since 1.2
	 */
	public double[] readPackedDouble() {
		int cookie = this.beginReadLen();
		DoubleBuffer src = this.buffer.asDoubleBuffer();
		double[] rv = new double[src.remaining()];

		src.get(rv);
		this.buffer.position(this.buffer.position() + (rv.length * 8));
		this.endReadLen(cookie);
		return rv;
	}

	/**
	 * Read packed {@code bool} field value as an {@code boolean} array.
	 *
	 * @return read value
	 * @throws java.nio.BufferUnderflowException insufficient data remaining in underlying buffer
	 * @throws IllegalStateException coding is malformed or previously read field {@linkplain
	 * #readTag() tag} does not have an {@code len} field value type
	 * @since 1.2
	 */
	public boolean[] readPackedBool() {
		int cookie = this.beginReadLen();
		int count = this.countPackedVarint();
		boolean[] rv = new boolean[count];

		for (int i = 0; i < rv.length; i++)
			rv[i] = this.readVarint64() != 0L;
		this.endReadLen(cookie);
		return rv;
	}

	/**
	 * Read {@code len} sequence into a {@code byte} array.
	 *
	 * @return read value
	 * @throws java.nio.BufferUnderflowException insufficient data remaining in underlying buffer
	 * @throws IllegalStateException coding is malformed or previously read field {@linkplain
	 * #readTag() tag} does not have an {@code len} field value type
	 * @since 1.2
	 */
	public byte[] readBytes() {
		int cookie = this.beginReadLen();
		byte[] rv = new byte[this.buffer.remaining()];

		this.buffer.get(rv);
		this.endReadLen(cookie);
		return rv;
	}

	/**
	 * Read {@code len} sequence as a UTF-8 encoded {@code string}.
	 *
	 * @return read value
	 * @throws java.nio.BufferUnderflowException insufficient data remaining in underlying buffer
	 * @throws IllegalStateException coding is malformed or previously read field {@linkplain
	 * #readTag() tag} does not have an {@code len} field value type
	 * @since 1.2
	 */
	public String readString() {
		int cookie = this.beginReadLen();
		String rv = StandardCharsets.UTF_8.decode(this.buffer)
			.toString();

		this.endReadLen(cookie);
		return rv;
	}

	/**
	 * Read {@code len} sequence as a UTF-8 encoded {@code string} pair.
	 *
	 * @return read value
	 * @throws java.nio.BufferUnderflowException insufficient data remaining in underlying buffer
	 * @throws IllegalStateException coding is malformed or previously read field {@linkplain
	 * #readTag() tag} does not have an {@code len} field value type
	 * @since 1.2
	 */
	public Pair<String, String> readStringPair() {
		int cookie = this.beginReadLen();
		String a = "";
		String b = "";

		while (this.hasRemaining()) {
			int tag = this.readTag();

			if (tag == ProtobufField.ofString(1))
				a = this.readString();
			else if (tag == ProtobufField.ofString(2))
				b = this.readString();
		}
		this.endReadLen(cookie);
		return new Pair<>(a, b);
	}

	/**
	 * Read {@code len} sequence as a {@linkplain ProtobufDeserializer deserializable} value.
	 * <p>Efficient equivalent of:
	 * {@snippet lang="java" :
	 * int cookie = reader.beginReadLen(); // @link substring="beginReadLen" target="#beginReadLen()"
	 * T rv = de.ofProtobuf(reader); // @link substring="ofProtobuf" target="ProtobufDeserializer#ofProtobuf(ProtobufReader)"
	 *
	 * reader.endReadLen(cookie); // @link substring="endReadLen" target="#endReadLen(int)"
	 * }
	 *
	 * @param <T> deserialized value type
	 * @param de deserializer to deserialize value with
	 * @return read value
	 * @throws java.nio.BufferUnderflowException insufficient data remaining in underlying buffer
	 * @throws IllegalStateException coding is malformed or previously read field {@linkplain
	 * #readTag() tag} does not have an {@code len} field value type
	 * @since 1.2
	 */
	@SuppressWarnings("JavadocDeclaration")
	public <T> T readLen(ProtobufDeserializer<T> de) {
		int cookie = this.beginReadLen();
		T rv = de.ofProtobuf(this);

		this.endReadLen(cookie);
		return rv;
	}
}
