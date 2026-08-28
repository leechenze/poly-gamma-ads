// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.protobuf;

import android.text.TextUtils;
import android.util.Pair;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import org.polygamma.android.origin.util.Bits;
import org.polygamma.android.origin.util.Preconditions;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Protocol buffer message writer.
 * <p>Instances of this write Protobuf messages using the {@code write} family of methods. Upon
 * writing a full message, the {@link #finish()} method should be invoked to retrieve a
 * <i>reference</i> to the underlying buffer.
 * <p>Each {@code write} method accepts, as its first argument, a field {@linkplain
 * ProtobufField.Tag tag}, followed by the field value. If the field value is the default value
 * for the value type, the respective {@code write} method simply returns. For example, attempting
 * to write an {@link #writeSint32(int, int) sint32} field with a value of {@code 0} will simply
 * return.
 *
 * @since 1.2
 */
public final class ProtobufWriter {

	/**
	 * Serialize {@linkplain ProtobufSerializable serializable} value into Protobuf message.
	 *
	 * @param ser value to serialize
	 * @return serialized message
	 * @since 1.2
	 */
	public static ByteBuffer serialize(ProtobufSerializable ser) {
		ProtobufWriter writer = new ProtobufWriter();

		ser.toProtobuf(writer);
		return writer.finish();
	}

	private ByteBuffer buffer;

	/**
	 * Construct new writer with an initial capacity.
	 *
	 * @param cap initial internal buffer capacity, in bytes
	 * @since 1.2
	 */
	public ProtobufWriter(int cap) {
		this.buffer = ByteBuffer.allocate(cap)
			.order(ByteOrder.LITTLE_ENDIAN);
	}

	/**
	 * Construct new writer with default initial capacity.
	 *
	 * @since 1.2
	 */
	public ProtobufWriter() {
		this(128);
	}

	/**
	 * Finish writing and return shared view of internal buffer.
	 * <p>The buffer returned will share its underlying data with this writer. The buffers
	 * {@linkplain ByteBuffer#position() position} and {@linkplain ByteBuffer#limit() limit} will
	 * be equal to {@code 0} and the position of the last non-written {@code byte}, respectively.
	 * Since the buffer is a <i>view</i> of this writers internal buffer, any further write
	 * operations, performed on {@code this}, <i>will</i> result in overwriting of the returned
	 * buffer.
	 *
	 * @return internal buffer view
	 * @since 1.2
	 */
	public ByteBuffer finish() {
		ByteBuffer rv = this.buffer.duplicate();

		rv.flip();
		this.buffer.clear();
		return rv;
	}

	/**
	 * Ensure minimum writable capacity of {@linkplain #buffer underlying buffer}.
	 *
	 * @param min minimum remaining capacity, in bytes, to ensure
	 */
	private void ensureRemainingCapacity(int min) {
		if (this.buffer.remaining() >= min)
			return;

		int curCap = this.buffer.capacity();
		int newCap = Math.max(curCap * 2, curCap + min);

		this.buffer = ByteBuffer.allocate(newCap)
			.put((ByteBuffer) this.buffer.flip())
			.order(ByteOrder.LITTLE_ENDIAN);
	}

	/**
	 * Write length-limited {@link Wire#TYPE_VARINT varint}.
	 *
	 * @param maxSize maximum size, in bytes, of encoded value
	 * @param val value to write
	 */
	private void writeVarint(int maxSize, long val) {
		for (; maxSize > 0 && (val & ~0x7fL) != 0L; val >>>= 7, maxSize--)
			this.buffer.put((byte) ((val & 0x7fL) | 0x80));
		if (maxSize != 0)
			this.buffer.put((byte) (val & 0xffL));
	}

	/**
	 * Write 32-bit {@link Wire#TYPE_VARINT varint}.
	 *
	 * @param val value to write
	 */
	@VisibleForTesting
	void writeVarint32(int val) {
		this.writeVarint(Wire.MAX_INT32_VARINT_SIZE, Integer.toUnsignedLong(val));
	}

	/**
	 * Write 64-bit {@link Wire#TYPE_VARINT varint}.
	 *
	 * @param val value to write
	 */
	@VisibleForTesting
	void writeVarint64(long val) {
		this.writeVarint(Wire.MAX_INT64_VARINT_SIZE, val);
	}

	/**
	 * Write field tag.
	 *
	 * @param tag tag to write
	 * @param expValSize expected value size, in bytes
	 */
	private void writeFieldTag(@ProtobufField.Tag int tag, int expValSize) {
		this.ensureRemainingCapacity(Wire.MAX_INT32_VARINT_SIZE + expValSize);
		this.writeVarint32(tag);
	}

	/**
	 * Write {@code int32} field.
	 *
	 * @param tag field tag
	 * @param val field value
	 * @since 1.2
	 */
	public void writeInt32(@ProtobufField.Tag int tag, int val) {
		if (val != 0) {
			this.writeFieldTag(tag, Wire.MAX_INT32_VARINT_SIZE);
			this.writeVarint32(val);
		}
	}

	/**
	 * Write {@code sint32} field.
	 *
	 * @param tag field tag
	 * @param val field value
	 * @since 1.2
	 */
	public void writeSint32(@ProtobufField.Tag int tag, int val) {
		this.writeInt32(tag, Wire.varintOfSint32(val));
	}

	/**
	 * Write {@code fixed32} field.
	 *
	 * @param tag field tag
	 * @param val field value
	 * @since 1.2
	 */
	public void writeFixed32(@ProtobufField.Tag int tag, int val) {
		if (val != 0) {
			this.writeFieldTag(tag, 4);
			this.buffer.putInt(val);
		}
	}

	/**
	 * Write {@code int64} field.
	 *
	 * @param tag field tag
	 * @param val field value
	 * @since 1.2
	 */
	public void writeInt64(@ProtobufField.Tag int tag, long val) {
		if (val != 0L) {
			this.writeFieldTag(tag, Wire.MAX_INT64_VARINT_SIZE);
			this.writeVarint64(val);
		}
	}

	/**
	 * Write {@code sint64} field.
	 *
	 * @param tag field tag
	 * @param val field value
	 * @since 1.2
	 */
	public void writeSint64(@ProtobufField.Tag int tag, long val) {
		this.writeInt64(tag, Wire.varintOfSint64(val));
	}

	/**
	 * Write {@code fixed64} field.
	 *
	 * @param tag field tag
	 * @param val field value
	 * @since 1.2
	 */
	public void writeFixed64(@ProtobufField.Tag int tag, long val) {
		if (val != 0) {
			this.writeFieldTag(tag, 8);
			this.buffer.putLong(val);
		}
	}

	/**
	 * Write {@code float} field.
	 *
	 * @param tag field tag
	 * @param val field value
	 * @since 1.2
	 */
	public void writeFloat(@ProtobufField.Tag int tag, float val) {
		this.writeFixed32(tag, Float.floatToIntBits(val));
	}

	/**
	 * Write {@code double} field.
	 *
	 * @param tag field tag
	 * @param val field value
	 * @since 1.2
	 */
	public void writeDouble(@ProtobufField.Tag int tag, double val) {
		this.writeFixed64(tag, Double.doubleToLongBits(val));
	}

	/**
	 * Write {@code bool} field.
	 *
	 * @param tag field tag
	 * @param val field value
	 * @since 1.2
	 */
	public void writeBool(@ProtobufField.Tag int tag, boolean val) {
		if (val) {
			this.writeFieldTag(tag, 1);
			this.buffer.put((byte) 1);
		}
	}

	/**
	 * Begin writing fixed-length {@link Wire#TYPE_LEN len} sequence field.
	 * <p>This writes the {@code len} sequence field tag and corresponding sequence length,
	 * {@code valSize}. The position returned and the sequence length {@code valSize} must be
	 * used to invoke {@link #endWriteFixedLen(int, int)} <i>after</i> sequence has been written.
	 *
	 * @param tag field tag
	 * @param valSize sequence size, in bytes
	 * @return position, in bytes, at which sequence value is to be written
	 */
	private int beginWriteFixedLen(@ProtobufField.Tag int tag, int valSize) {
		this.writeFieldTag(tag, Wire.MAX_INT32_VARINT_SIZE * 2 + valSize);
		this.writeVarint32(valSize);
		return this.buffer.position();
	}

	/**
	 * End writing fixed-length {@link Wire#TYPE_LEN len} sequence field.
	 * <p>This must be invoked with the position returned by {@link #beginWriteFixedLen(int, int)} and
	 * the sequence length, in bytes, in {@code expValSize}.
	 *
	 * @param valStartPos position, in bytes, at which sequence value was written
	 * @param expValSize expected sequence size, in bytes
	 * @throws IllegalStateException if fewer than {@code expValSize} bytes were written
	 */
	private void endWriteFixedLen(int valStartPos, int expValSize) {
		int gotValSize = this.buffer.position() - valStartPos;

		Preconditions.checkState(
			gotValSize == expValSize,
			"expected to write %s bytes, wrote %s",
			expValSize,
			gotValSize
		);
	}

	/**
	 * Write packed {@code int32} or {@code sint32} field.
	 *
	 * @param tag field tag
	 * @param val field value
	 * @param signed {@code true} or {@code false} to write {@code val} as {@code int32} or
	 * {@code sint32}, respectively
	 */
	private void
	writePackedXint32(@ProtobufField.Tag int tag, @Nullable int[] val, boolean signed) {
		if (val == null || val.length == 0)
			return;

		int len = 0;

		for (int v : val) {
			if (signed)
				v = Wire.varintOfSint32(v);
			len += Wire.sizeOfVarint(Integer.toUnsignedLong(v));
		}

		if (len == 0)
			return;

		int pos = this.beginWriteFixedLen(tag, len);

		for (int v : val) {
			if (signed)
				v = Wire.varintOfSint32(v);
			this.writeVarint32(v);
		}
		this.endWriteFixedLen(pos, len);
	}

	/**
	 * Write packed {@code int32} field from {@code int} array.
	 *
	 * @param tag field tag
	 * @param val field value
	 * @since 1.2
	 */
	public void writePackedInt32(@ProtobufField.Tag int tag, @Nullable int[] val) {
		this.writePackedXint32(tag, val, false);
	}

	/**
	 * Write packed {@code sint32} field from {@code int} array.
	 *
	 * @param tag field tag
	 * @param val field value
	 * @since 1.2
	 */
	public void writePackedSint32(@ProtobufField.Tag int tag, @Nullable int[] val) {
		this.writePackedXint32(tag, val, true);
	}

	/**
	 * Write packed {@code fixed32} field from {@code int} {@linkplain IntBuffer buffer}.
	 *
	 * @param tag field tag
	 * @param val field value
	 * @since 1.2
	 */
	public void writePackedFixed32(@ProtobufField.Tag int tag, @Nullable int[] val) {
		int len = val == null ? 0 : val.length * 4;

		if (len == 0)
			return;

		int pos = this.beginWriteFixedLen(tag, len);

		this.buffer.asIntBuffer()
			.put(val);
		this.buffer.position(pos + len);
	}

	/**
	 * Write packed {@code int32} field from a word bitmap.
	 * <p>This writes {@code i + delta} as an {@code int32} where {@code i} is the index of each
	 * set bit in {@code bits}. This is an efficient equivalent of:
	 * {@snippet lang="java" :
	 * int[] val = new int[Long.countBits(bits)];
	 *
	 * for (int i = 0, j = 0; i < Long.SIZE; i++) {
	 *     if ((bits & (1L << i)) != 0L)
	 *         val[j++] = delta + i;
	 * }
	 * writePackedInt32(tag, val); // @link substring="writePackedInt32" target="#writePackedInt32(int, int[])"
	 * }
	 *
	 * @param tag field tag
	 * @param bits bits to derive field value from
	 * @param delta delta to increment bit position by
	 * @since 1.2
	 */
	@SuppressWarnings("JavadocDeclaration")
	public void writeWordBitmap(@ProtobufField.Tag int tag, long bits, int delta) {
		if (bits == 0)
			return;

		long cookie = this.beginWriteLen(tag);
		int bit = Bits.firstSetBitOf(bits);

		do {
			int val = delta + bit;

			this.ensureRemainingCapacity(Wire.sizeOfVarint(Integer.toUnsignedLong(val)));
			this.writeVarint32(val);
			bit = Bits.nextSetBitOf(bits, bit + 1);
		} while (bit < Bits.SIZE_OF_LONG);
		this.endWriteLen(cookie);
	}

	/**
	 * Write packed {@code int64} or {@code sint64} field.
	 *
	 * @param tag field tag
	 * @param val field value
	 * @param signed {@code true} or {@code false} to write {@code val} as {@code int64} or
	 * {@code sint64}, respectively
	 */
	private void
	writePackedXint64(@ProtobufField.Tag int tag, @Nullable long[] val, boolean signed) {
		if (val == null || val.length == 0)
			return;

		int len = 0;

		for (long v : val) {
			if (signed)
				v = Wire.varintOfSint64(v);
			len += Wire.sizeOfVarint(v);
		}

		if (len == 0)
			return;

		int pos = this.beginWriteFixedLen(tag, len);

		for (long v : val) {
			if (signed)
				v = Wire.varintOfSint64(v);
			this.writeVarint64(v);
		}
		this.endWriteFixedLen(pos, len);
	}

	/**
	 * Write packed {@code int64} field from {@code long} array.
	 *
	 * @param tag field tag
	 * @param val field value
	 * @since 1.2
	 */
	public void writePackedInt64(@ProtobufField.Tag int tag, @Nullable long[] val) {
		this.writePackedXint64(tag, val, false);
	}

	/**
	 * Write packed {@code sint64} field from {@code long} array.
	 *
	 * @param tag field tag
	 * @param val field value
	 * @since 1.2
	 */
	public void writePackedSint64(@ProtobufField.Tag int tag, @Nullable long[] val) {
		this.writePackedXint64(tag, val, true);
	}

	/**
	 * Write packed {@code fixed64} field from {@code long} array.
	 *
	 * @param tag field tag
	 * @param val field value
	 * @since 1.2
	 */
	public void writePackedFixed64(@ProtobufField.Tag int tag, @Nullable long[] val) {
		int len = val == null ? 0 : val.length * 8;

		if (len == 0)
			return;

		int pos = this.beginWriteFixedLen(tag, len);

		this.buffer.asLongBuffer()
			.put(val);
		this.buffer.position(pos + len);
	}

	/**
	 * Write packed {@code float} field from {@code float} array.
	 *
	 * @param tag field tag
	 * @param val field value
	 * @since 1.2
	 */
	public void writePackedFloat(@ProtobufField.Tag int tag, @Nullable float[] val) {
		int len = val == null ? 0 : val.length * 4;

		if (len == 0)
			return;

		int pos = this.beginWriteFixedLen(tag, len);

		this.buffer.asFloatBuffer()
			.put(val);
		this.buffer.position(pos + len);
	}

	/**
	 * Write packed {@code double} field from {@code float} array.
	 *
	 * @param tag field tag
	 * @param val field value
	 * @since 1.2
	 */
	public void writePackedDouble(@ProtobufField.Tag int tag, @Nullable double[] val) {
		int len = val == null ? 0 : val.length * 8;

		if (len == 0)
			return;

		int pos = this.beginWriteFixedLen(tag, len);

		this.buffer.asDoubleBuffer()
			.put(val);
		this.buffer.position(pos + len);
	}

	/**
	 * Write packed {@code bool} field from {@code boolean} array.
	 *
	 * @param tag field tag
	 * @param val field value
	 * @since 1.2
	 */
	public void writePackedBool(@ProtobufField.Tag int tag, @Nullable boolean[] val) {
		if (val == null || val.length == 0)
			return;

		this.beginWriteFixedLen(tag, val.length);
		for (boolean v : val)
			this.buffer.put((byte) (v ? 1 : 0));
	}

	/**
	 * Write {@code len} field from {@code byte} array.
	 *
	 * @param tag field tag
	 * @param val field value
	 * @since 1.2
	 */
	public void writeBytes(@ProtobufField.Tag int tag, @Nullable byte[] val) {
		if (val == null || val.length == 0)
			return;

		this.beginWriteFixedLen(tag, val.length);
		this.buffer.put(val);
	}

	/**
	 * Write {@code len} field from {@code byte} {@linkplain ByteBuffer buffer}.
	 *
	 * @param tag field tag
	 * @param val field value
	 * @since 1.2
	 */
	public void writeBytes(@ProtobufField.Tag int tag, @Nullable ByteBuffer val) {
		if (val == null || !val.hasRemaining())
			return;

		this.beginWriteFixedLen(tag, val.remaining());
		this.buffer.put(val);
	}

	/**
	 * Write repeating {@code len} field from iterable of {@code byte} arrays.
	 *
	 * @param tag field tag
	 * @param val field value
	 * @since 1.2
	 */
	public void writeRepeatBytes(@ProtobufField.Tag int tag, @Nullable byte[][] val) {
		if (val != null) {
			for (byte[] v : val)
				this.writeBytes(tag, v);
		}
	}

	/**
	 * Write {@code len} field from UTF-8 encoding of {@code string}.
	 *
	 * @param tag field tag
	 * @param val field value
	 * @since 1.2
	 */
	public void writeString(@ProtobufField.Tag int tag, @Nullable String val) {
		if (!TextUtils.isEmpty(val))
			this.writeBytes(tag, val.getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * Write repeating {@code len} field from UTF-8 encodings of {@code string} iterable.
	 *
	 * @param tag field tag
	 * @param val field value
	 * @since 1.2
	 */
	public void writeRepeatString(@ProtobufField.Tag int tag, @Nullable String[] val) {
		if (val != null) {
			for (String v : val)
				this.writeString(tag, v);
		}
	}

	/**
	 * Write {@code len} field from UTF-8 encodings of {@code string} pair.
	 *
	 * @param tag field tag
	 * @param val field value
	 * @since 1.2
	 */
	public void writeStringPair(@ProtobufField.Tag int tag, @Nullable Pair<String, String> val) {
		if (val == null)
			return;

		byte[][] ab = {
			TextUtils.isEmpty(val.first) ? null : val.first.getBytes(StandardCharsets.UTF_8),
			TextUtils.isEmpty(val.second) ? null : val.second.getBytes(StandardCharsets.UTF_8)
		};
		int len = 0;

		for (int i = 0; i < ab.length; i++) {
			byte[] p = ab[i];

			if (p == null)
				continue;
			len += Wire.sizeOfVarint(Integer.toUnsignedLong(ProtobufField.ofString(i + 1))) +
				Wire.sizeOfVarint(Integer.toUnsignedLong(p.length)) +
				p.length;
		}

		if (len == 0)
			return;

		int pos = this.beginWriteFixedLen(tag, len);

		for (int i = 0; i < ab.length; i++) {
			byte[] p = ab[i];

			if (p == null)
				continue;

			this.writeVarint32(ProtobufField.ofString(i + 1));
			this.writeVarint32(p.length);
			this.buffer.put(p);
		}
		this.endWriteFixedLen(pos, len);
	}

	/**
	 * Write repeating {@code len} field from UTF-8 encodings of {@code string} pairs.
	 *
	 * @param tag field tag
	 * @param val field value
	 * @since 1.2
	 */
	public void
	writeRepeatStringPair(@ProtobufField.Tag int tag, @Nullable Pair<String, String>[] val) {
		if (val != null) {
			for (Pair<String, String> v : val)
				this.writeStringPair(tag, v);
		}
	}

	/**
	 * Begin writing {@code len} sequence field.
	 * <p>This prepares this writer for a {@code len} sequence field with tag {@code tag}. Upon
	 * return, the {@code len} sequence must be written. Once the sequence is written, {@link
	 * #endWriteLen(long)} must be invoked with the cookie value returned by this.
	 * {@snippet lang="java" :
	 * long cookie = writer.beginWriteLen(TAG);
	 * // ... write sequence to `writer`
	 * writer.endWriteLen(cookie); // @link substring="endWriteLen" target="#endWriteLen(long)"
	 * }
	 *
	 * @param tag field tag
	 * @return field cookie
	 * @since 1.2
	 * @see #endWriteLen(long)
	 */
	@SuppressWarnings("JavadocDeclaration")
	public long beginWriteLen(@ProtobufField.Tag int tag) {
		int tagPos = this.buffer.position();

		this.writeFieldTag(tag, Wire.MAX_INT32_VARINT_SIZE);

		int lenPos = this.buffer.position();

		this.buffer.position(lenPos + Wire.MAX_INT32_VARINT_SIZE);
		return (Integer.toUnsignedLong(lenPos) << 32) | Integer.toUnsignedLong(tagPos);
	}

	/**
	 * End writing {@code len} sequence field.
	 * <p>This completes writing a sequence field began using {@link #beginWriteLen(int)}. This
	 * <b>must</b> be invoked with the cookie returned by {@link #beginWriteLen(int)}.
	 *
	 * @param cookie field cookie
	 * @since 1.2
	 * @see #beginWriteLen(int)
	 */
	public void endWriteLen(long cookie) {
		int tagPos = (int) (cookie & 0xffffffffL);
		int lenPos = (int) (cookie >>> 32);
		int msgPos = lenPos + Wire.MAX_INT32_VARINT_SIZE;
		int msgLen = this.buffer.position() - msgPos;

		if (msgLen == 0) {
			// empty message, bail
			this.buffer.position(tagPos);
			return;
		}

		ByteBuffer msg = this.buffer.duplicate();

		msg.position(msgPos)
			.limit(msgPos + msgLen);

		this.buffer.position(lenPos);
		this.writeVarint32(msgLen);
		this.buffer.put(msg);
	}

	/**
	 * Write {@code len} sequence field with a {@linkplain ProtobufSerializable serializable} value.
	 * <p>Efficient equivalent of:
	 * {@snippet lang="java" :
	 * long cookie = writer.beginWriteLen(tag); // @link substring="beginWriteLen" target="#beginWriteLen(int)"
	 *
	 * if (val != null)
	 *     val.serializeInto(writer); // @link substring="serializeInto" target="ProtobufSerializable#serializeInto(ProtobufWriter)"
	 * writer.endWriteLen(cookie); // @link substring="endWriteLen" target="#endWriteLen(long)"
	 * }
	 *
	 * @param tag field tag
	 * @param val field value
	 * @since 1.2
	 */
	@SuppressWarnings("JavadocDeclaration")
	public void writeLen(@ProtobufField.Tag int tag, @Nullable ProtobufSerializable val) {
		if (val != null) {
			long cookie = this.beginWriteLen(tag);

			val.toProtobuf(this);
			this.endWriteLen(cookie);
		}
	}

	/**
	 * Write repeating {@code len} sequence field from iterable of {@linkplain ProtobufSerializable
	 * serializable} values.
	 *
	 * @param tag field tag
	 * @param val field values
	 * @since 1.2
	 */
	public void writeRepeatLen(@ProtobufField.Tag int tag, @Nullable ProtobufSerializable[] val) {
		if (val != null) {
			for (ProtobufSerializable v : val)
				this.writeLen(tag, v);
		}
	}
}
