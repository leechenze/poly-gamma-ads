// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.protobuf;

import static org.polygamma.android.origin.protobuf.Protobuf.BITS_PER_WIRE_TYPE;
import static org.polygamma.android.origin.protobuf.Protobuf.MAX_FIELD_NUMBER;
import static org.polygamma.android.origin.protobuf.Protobuf.MIN_FIELD_NUMBER;
import static org.polygamma.android.origin.protobuf.Protobuf.STRING_PAIR_A_TAG;
import static org.polygamma.android.origin.protobuf.Protobuf.STRING_PAIR_B_TAG;
import static org.polygamma.android.origin.protobuf.Protobuf.TAG_WIRE_TYPE_MASK;
import static org.polygamma.android.origin.protobuf.Protobuf.WIRE_FIXED32;
import static org.polygamma.android.origin.protobuf.Protobuf.WIRE_FIXED64;
import static org.polygamma.android.origin.protobuf.Protobuf.WIRE_LEN;
import static org.polygamma.android.origin.protobuf.Protobuf.WIRE_VARINT;
import static org.polygamma.android.origin.protobuf.Protobuf.isWireTypeValid;
import static org.polygamma.android.origin.protobuf.Protobuf.wireTypeOfFieldTag;
import static org.polygamma.android.origin.util.Bits.loadIntLe;
import static org.polygamma.android.origin.util.Bits.loadLongLe;

import android.os.Build;
import android.util.Pair;

import androidx.annotation.IntRange;
import androidx.annotation.Nullable;

import org.polygamma.android.origin.protobuf.Protobuf.FieldTag;
import org.polygamma.android.origin.protobuf.Protobuf.WireType;
import org.polygamma.android.origin.util.BiFunction;
import org.polygamma.android.origin.util.Function;
import org.polygamma.android.origin.util.IntFunction;
import org.polygamma.android.origin.util.Preconditions;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Protocol buffer decoder.
 * <p>Instances of this decode Protocol buffer wire format from a {@code byte} array. This supports
 * decoding schema, such as field numbers and {@code len} sequence sizes, along with content. By
 * default, decoding is merged as defined by the Protocol buffer version 3 specification.
 * <p>Coding can be split such that schema and content are decoded from separate arrays. This is
 * beneficial in cases where treatmet of content should be separated from schema, such as when
 * used within the context of Fully Homomorphic Encryption (FHE). When coding is split, the
 * encoding for all data-types is the same, except for {@code len} sequences. See {@link
 * ProtobufEncoder} for more information.
 *
 * @since 1.2
 */
@SuppressWarnings("JavadocDeclaration")
public final class ProtobufDecoder {

	/**
	 * Construct a new decoder for subsequence of {@code byte} array.
	 * <p>This returns a non-{@linkplain #isSplit() split} decoder which will decode at most the
	 * first {@code len} bytes of {@code src}, starting at position {@code off} (inclusive).
	 *
	 * @param src array to decode subsequence of
	 * @param off offset, within {@code src}, to begin decoding from
	 * @param len maximum number of bytes to decode
	 * @return resulting decoder
	 * @throws IndexOutOfBoundsException {@code off} or {@code len} is negative, or {@code
	 * off + len} is greater than {@code src.length}
	 * @since 1.2
	 */
	public static ProtobufDecoder of(byte[] src, int off, int len) {
		return new ProtobufDecoder(src, off, len, null);
	}

	/**
	 * Construct new decoder for {@code byte} array.
	 * <p>Shorthand for:
	 * {@snippet :
	 * of(src, 0, src.length); // @link substring="of" target="#of(byte[], int, int)"
	 * }
	 *
	 * @param src array to decode contents of
	 * @return resulting decoder
	 * @since 1.2
	 */
	public static ProtobufDecoder of(byte[] src) {
		return of(src, 0, src.length);
	}

	private static ProtobufDecoder
	ofBuffer0(ByteBuffer src, @Nullable ProtobufDecoder contentDec) {
		if (src.hasArray()) {
			return new ProtobufDecoder(
				src.array(), src.arrayOffset() + src.position(), src.remaining(),
				contentDec
			);
		}

		byte[] arr = new byte[src.remaining()];

		src.duplicate()
			.get(arr);
		return new ProtobufDecoder(arr, 0, arr.length, contentDec);
	}

	/**
	 * Construct a new decoder for existing buffer.
	 * <p>If {@code src} is backed by an {@linkplain ByteBuffer#hasArray() array}, this returns
	 * a decoder which will decode the first {@link ByteBuffer#remaining() src.remaining()} bytes
	 * of the subsequence of the array underlying {@code src}, starting at position {@code
	 * src.arrayOffset() + src.position()} (inclusive). Otherwise, this constructs a copy of the
	 * contents of {@code src}, which is then decoded from.
	 *
	 * @param src buffer to decode contents of
	 * @return resulting decoder
	 * @since 1.2
	 */
	public static ProtobufDecoder ofBuffer(ByteBuffer src) {
		return ofBuffer0(src, null);
	}

	/**
	 * Construct a new decoder which decodes schema and content from different subsequences of
	 * {@code byte} arrays.
	 * <p>This returns a {@linkplain #isSplit() split} decoder which will decode at most {@code
	 * schemaLen} and {@code contentLen} schema and content bytes from {@code schema} and {@code
	 * content}, starting at positon {@code schemaOff} (inclusive) and {@code contentOff}
	 * (inclusive), respectively.
	 *
	 * @param schema array to decode schema from
	 * @param schemaOff offset, within {@code schema}, to begin decoding from
	 * @param schemaLen maximum number of bytes to decode from {@code schema}
	 * @param content array to decode content from
	 * @param contentOff offset, within {@code content}, to begin decoding from
	 * @param contentLen maximum number of bytes to decode from {@code content}
	 * @return resulting decoder
	 * @throws IndexOutOfBoundsException {@code schemaOff}, {@code schemaLen}, {@code contentOff},
	 * or {@code contentLen} is negative, or, {@code schemaOff + schemaLen} or {@code
	 * contentOff + contentLen} is greater than {@code schema.length} or {@code content.length},
	 * respectively
	 * @since 1.2
	 */
	public static ProtobufDecoder ofSplit(
		byte[] schema, int schemaOff, int schemaLen,
		byte[] content, int contentOff, int contentLen
	) {
		return new ProtobufDecoder(
			schema, schemaOff, schemaLen,
			new ProtobufDecoder(content, contentOff, contentLen, null)
		);
	}

	/**
	 * Construct a new decoder which decodes schema and content from different {@code byte} arrays.
	 * <p>Shorthand for:
	 * {@snippet :
	 * ofSplit(schema, 0, schema.length, content, 0, content.length); // @link substring="ofSplit" target="#ofSplit(byte[], int, int, byte[], int, int)"
	 * }
	 *
	 * @param schema array to decode schema from
	 * @param content array to decode content from
	 * @return resulting decoder
	 * @since 1.2
	 */
	public static ProtobufDecoder ofSplit(byte[] schema, byte[] content) {
		return ofSplit(schema, 0, schema.length, content, 0, content.length);
	}

	/**
	 * Construct a new decoder which decodes schema and content from different buffers.
	 * <p>Like {@link #ofBuffer(ByteBuffer)}; however, the resulting decoder will decode schema
	 * and content from the contents of {@code schema} and {@code content}, respectively.
	 *
	 * @param schema buffer to decode schema from
	 * @param content buffer to decode content from
	 * @return resulting decoder
	 * @since 1.2
	 */
	public static ProtobufDecoder ofSplitBuffer(ByteBuffer schema, ByteBuffer content) {
		return ofBuffer0(schema, ofBuffer0(content, null));
	}

	private final byte[] array;
	private int arrayOffset;
	private int arrayLimit;
	private final ProtobufDecoder contentDecoder;

	private ProtobufDecoder(
		byte[] arr, int arrOff, int arrLen,
		@Nullable ProtobufDecoder contentDec
	) {
		Preconditions.checkFromIndexSize(arrOff, arrLen, arr.length);
		this.array = arr;
		this.arrayOffset = arrOff;
		this.arrayLimit = arrOff + arrLen;
		this.contentDecoder = contentDec == null ? this : contentDec;
	}

	/**
	 * Test whether schema and content are decoded from separate {@code byte} arrays.
	 * <p>If this returns {@code true}, schema and content are being decoded from separate {@code
	 * byte} arrays, accessible using {@link #schemaArray()} and {@link #contentArray()},
	 * respectively; otherwise, schema and content are decoded linearly from a single {@code byte}
	 * array accessible using {@link #array()}.
	 *
	 * @return {@code true} if, and only if, schema and content are decoded separately
	 * @since 1.2
	 */
	public boolean isSplit() {
		return this.contentDecoder != this;
	}

	// Ensure we're decoding from a single array.
	private void checkMerged() {
		Preconditions.checkState(this.contentDecoder == this);
	}

	// Ensure we're decoding schema and content from separate arrays.
	private void checkSplit() {
		Preconditions.checkState(this.contentDecoder != this);
	}

	/**
	 * Array from which schema and content are decoded.
	 * <p>If decoding is not {@linkplain #isSplit() split}, this returns the array from which
	 * both schema and content are linearly decoded. Any subsequent decode operation performed on
	 * {@code this} is guaranteed to decode at most {@link #arrayRemaining()} bytes, starting at
	 * {@link #arrayOffset()} (inclusive).
	 *
	 * @return encoded contents array
	 * @throw IllegalStateException coding is split
	 * @since 1.2
	 * @see #arrayOffset()
	 * @see #arrayRemaining()
	 */
	public byte[] array() {
		this.checkMerged();
		return this.array;
	}

	/**
	 * Offset at which next decode operation will decode from within linear array.
	 * <p>If decoding is not {@linkplain #isSplit() split}, this returns the position within
	 * {@link #array()} at which the next decode operation will decode from.
	 *
	 * @return decode offset
	 * @throw IllegalStateException coding is split
	 * @since 1.2
	 * @see #array()
	 * @see #arrayRemaining()
	 */
	public int arrayOffset() {
		this.checkMerged();
		return this.arrayOffset;
	}

	/**
	 * Number of bytes remaining to be decoded from linear array.
	 * <p>If decoding is not {@linkplain #isSplit() split}, this returns the number of bytes
	 * remaining within {@link #array()}, starting from position {@link #arrayOffset()}
	 * (inclusive), which have not yet been decoded.
	 *
	 * @return number of bytes remaining to be decoded
	 * @since 1.2
	 * @see #array()
	 * @see #arrayOffset()
	 */
	public int arrayRemaining() {
		this.checkMerged();
		return this.arrayLimit - this.arrayOffset;
	}

	/**
	 * Construct buffer viewing array from which schema and content are decoded.
	 * <p>Shorthand for:
	 * {@snippet :
	 * ByteBuffer.wrap(
	 *     array(), // @link substring="array" target="#array()"
	 *     arrayOffset(), // @link substring="arrayOffset" target="#arrayOffset()"
	 *     arrayRemaining() // @link substring="arrayRemaining" target="#arrayRemaining()"
	 * );
	 * }
	 *
	 * @return buffer viewing encoded contents array
	 * @since 1.2
	 * @see #array()
	 * @see #arrayOffset()
	 * @see #arrayRemaining()
	 */
	public ByteBuffer asBuffer() {
		this.checkMerged();
		return ByteBuffer.wrap(this.array, this.arrayOffset, this.arrayLimit - this.arrayOffset);
	}

	/**
	 * Array from which schema is decoded.
	 * <p>If decoding is {@linkplain #isSplit() split}, this returns the array from which
	 * schema is decoded. Any subsequent decode operation performed on {@code this} is guaranteed
	 * to decode at most {@link #schemaArrayRemaining()} bytes, starting at {@link
	 * #schemaArrayOffset()} (inclusive).
	 *
	 * @return encoded schema array
	 * @throw IllegalStateException coding is not split
	 * @since 1.2
	 * @see #schemaArrayOffset()
	 * @see #schemaArrayRemaining()
	 */
	public byte[] schemaArray() {
		this.checkSplit();
		return this.array;
	}

	/**
	 * Offset at which next schema decode operation will decode from within schema array.
	 * <p>If decoding is {@linkplain #isSplit() split}, this returns the position within
	 * {@link #schemaArray()} at which the next schema decode operation will decode from.
	 *
	 * @return decode offset
	 * @throw IllegalStateException coding is not split
	 * @since 1.2
	 * @see #schemaArray()
	 * @see #schemaArrayRemaining()
	 */
	public int schemaArrayOffset() {
		this.checkSplit();
		return this.arrayOffset;
	}

	/**
	 * Number of bytes remaining to be decoded from schema array.
	 * <p>If decoding is {@linkplain #isSplit() split}, this returns the number of bytes
	 * remaining within {@link #schemaArray()}, starting from position {@link #schemaArrayOffset()}
	 * (inclusive), which have not yet been decoded.
	 *
	 * @return number of schema bytes remaining to be decoded
	 * @since 1.2
	 * @see #schemaArray()
	 * @see #schemaArrayOffset()
	 */
	public int schemaArrayRemaining() {
		this.checkSplit();
		return this.arrayLimit - this.arrayOffset;
	}

	/**
	 * Construct buffer viewing array from which schema is decoded.
	 * <p>Shorthand for:
	 * {@snippet :
	 * ByteBuffer.wrap(
	 *     schemaArray(), // @link substring="schemaArray" target="#schemaArray()"
	 *     schemaArrayOffset(), // @link substring="schemaArrayOffset" target="#schemaArrayOffset()"
	 *     schemaArrayRemaining() // @link substring="schemaArrayRemaining" target="#schemaArrayRemaining()"
	 * );
	 * }
	 *
	 * @return buffer viewing encoded schema array
	 * @since 1.2
	 * @see #schemaArray()
	 * @see #schemaArrayOffset()
	 * @see #schemaArrayRemaining()
	 */
	public ByteBuffer asSchemaBuffer() {
		this.checkSplit();
		return ByteBuffer.wrap(this.array, this.arrayOffset, this.arrayLimit - this.arrayOffset);
	}

	/**
	 * Array from which content is decoded.
	 * <p>If decoding is {@linkplain #isSplit() split}, this returns the array from which
	 * content is decoded. Any subsequent decode operation performed on {@code this} is guaranteed
	 * to decode at most {@link #contentArrayRemaining()} bytes, starting at {@link
	 * #contentArrayOffset()} (inclusive).
	 *
	 * @return encoded content array
	 * @throw IllegalStateException coding is not split
	 * @since 1.2
	 * @see #contentArrayOffset()
	 * @see #contentArrayRemaining()
	 */
	public byte[] contentArray() {
		this.checkSplit();
		return this.contentDecoder.array;
	}

	/**
	 * Offset at which next content decode operation will decode from within content array.
	 * <p>If decoding is {@linkplain #isSplit() split}, this returns the position within
	 * {@link #contentArray()} at which the next content decode operation will decode from.
	 *
	 * @return decode offset
	 * @throw IllegalStateException coding is not split
	 * @since 1.2
	 * @see #contentArray()
	 * @see #contentArrayRemaining()
	 */
	public int contentArrayOffset() {
		this.checkSplit();
		return this.contentDecoder.arrayOffset;
	}

	/**
	 * Number of bytes remaining to be decoded from content array.
	 * <p>If decoding is {@linkplain #isSplit() split}, this returns the number of bytes
	 * remaining within {@link #contentArray()}, starting from position {@link
	 * #contentArrayOffset()} (inclusive), which have not yet been decoded.
	 *
	 * @return number of content bytes remaining to be decoded
	 * @since 1.2
	 * @see #contentArray()
	 * @see #contentArrayOffset()
	 */
	public int contentArrayRemaining() {
		this.checkSplit();
		return this.contentDecoder.arrayLimit - this.contentDecoder.arrayOffset;
	}

	/**
	 * Construct buffer viewing array from which content is decoded.
	 * <p>Shorthand for:
	 * {@snippet :
	 * ByteBuffer.wrap(
	 *     contentArray(), // @link substring="contentArray" target="#contentArray()"
	 *     contentArrayOffset(), // @link substring="contentArrayOffset" target="#contentArrayOffset()"
	 *     contentArrayRemaining() // @link substring="contentArrayRemaining" target="#contentArrayRemaining()"
	 * );
	 * }
	 *
	 * @return buffer viewing encoded content array
	 * @since 1.2
	 * @see #contentArray()
	 * @see #contentArrayOffset()
	 * @see #contentArrayRemaining()
	 */
	public ByteBuffer asContentBuffer() {
		this.checkSplit();
		return ByteBuffer.wrap(
			this.contentDecoder.array,
			this.contentDecoder.arrayOffset,
			this.contentDecoder.arrayLimit - this.contentDecoder.arrayOffset
		);
	}

	/**
	 * Test whether there is additional data remaining to be decoded.
	 *
	 * @return {@code true} if, and only if, there is additional data remaining to be decoded
	 * @since 1.2
	 */
	public boolean hasRemaining() {
		return
			this.arrayOffset < this.arrayLimit ||
			this.contentDecoder.arrayOffset < this.contentDecoder.arrayLimit;
	}

	/*
	 * Ensure there's sufficient data remaining in `dec` to decode `size` bytes. If there isn't
	 * sufficient data remaining, this fails with `IndexOutOfBoundsException`; otherwise, this
	 * advances the array offset of `dec` by `size` and returns the offset at which `size` bytes
	 * may be decoded.
	 */
	private static int advance(ProtobufDecoder dec, int size) {
		int off = dec.arrayOffset;

		/*
		 * `arrayLimit` should always be less than or equal to `dec.array.length`; however, we
		 * do this to signal to JIT that access of `size` bytes from `off` is within range.
		 */
		Preconditions.checkFromIndexSize(off, size, Math.min(dec.arrayLimit, dec.array.length));
		dec.arrayOffset += size;
		return off;
	}

	// Zig-zag decode 32-bit `a`.
	private static int zigzagOf32(int a) {
		return (a >>> 1) ^ -(a & 1);
	}

	// Zig-zag decode 64-bit `a`.
	private static long zigzagOf64(long a) {
		return (a >>> 1) ^ -(a & 1);
	}

	// Construct mask where each byte within `a` less than or equal to `0x7f` is set to non-zero.
	private static long isolateTerminatingVarintBytes(long a) {
		// Repeating `0x7f` that we want to mask in.
		final long LOW = 0x7f7f7f7f7f7f7f7fL;
		// Repeating `0x80` that we want to mask out.
		final long HIGH = 0x8080808080808080L;

		return (~0L - (a & LOW)) & ~a & HIGH;
	}

	// Count size of varint `a`. This returns `9` if entirety of `a` has continuation marker.
	private static @IntRange(from = 1, to = 9) int countVarintBytes(long a) {
		return (Long.numberOfTrailingZeros(isolateTerminatingVarintBytes(a)) / 8) + 1;
	}

	/*
	 * Decode next 64-bit `varint` from array underlying `dec`. Upon successful return, array
	 * offset of `dec` is updated to reflect the number of bytes decoded; otherwise, this fails
	 * with `IndexOutOfBoundsException` or `IllegalStateException` if there's insufficient data
	 * to decode a `varint` or coding is malformed, respectively.
	 */
	private static long decodeVarint64Impl(ProtobufDecoder dec) {
		int off = dec.arrayOffset;
		byte[] src = dec.array;
		long a =
			(off + 8) <= src.length ? loadLongLe(src, off) :
			loadLongLe(Arrays.copyOfRange(src, off, off + 8), 0);
		int n = countVarintBytes(a);

		Preconditions.checkFromIndexSize(off, n, dec.arrayLimit);
		a &= (0x80L << (Math.min(n - 1, 7) * 8)) - 1;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
			a = Long.compress(a, 0x7f7f7f7f7f7f7f7fL);
		} else {
			a = ( a & 0x000000000000007fL) |
				((a & 0x0000000000007f00L) >>> 1) |
				((a & 0x00000000007f0000L) >>> 2) |
				((a & 0x000000007f000000L) >>> 3) |
				((a & 0x0000007f00000000L) >>> 4) |
				((a & 0x00007f0000000000L) >>> 5) |
				((a & 0x007f000000000000L) >>> 6) |
				((a & 0x7f00000000000000L) >>> 7);
		}
		if (n <= 8) {
			dec.arrayOffset += n;
			return a;
		}

		int b = src[off + 8] & 0xff;

		a |= (b & 0x7fL) << 56;
		if (b >= 0x80) {
			Preconditions.checkFromIndexSize(off, ++n, dec.arrayLimit);
			b = src[off + 9] & 0xff;
			a |= (b & 0x7fL) << 63;
			Preconditions.checkState(a < 0x80);
		}
		dec.arrayOffset += n;
		return a;
	}

	/*
	 * If `dec` has sufficient data remaining to be decoded and next byte is less than `0x80`,
	 * it is returned and position of `dec` is advanced by `1`; otherwise, this returns `0x80`.
	 */
	private static int decodeVarint7Impl(ProtobufDecoder dec) {
		int off = dec.arrayOffset;
		int a = off >= dec.arrayLimit ? 0x80 : dec.array[off] & 0xff;

		if (a < 0x80)
			dec.arrayOffset++;
		return a;
	}

	// 32-bit variant of `decodeVarint64Impl()`.
	private static int decodeVarint32Impl(ProtobufDecoder dec) {
		int a = decodeVarint7Impl(dec);

		return a < 0x80 ? a : (int) (decodeVarint64Impl(dec) & 0xffffffffL);
	}

	/*
	 * Decode `fixed32` from `dec`, advancing by `4` bytes. Fails with `IndexOutOfBoundsException`
	 * if `dec` has fewer than 4 bytes remaining.
	 */
	private static int decodeFixed32Impl(ProtobufDecoder dec) {
		return loadIntLe(dec.array, advance(dec, 4));
	}

	// `fixed64` variant of `decodeFixed64Impl()`.
	private static long decodeFixed64Impl(ProtobufDecoder dec) {
		return loadLongLe(dec.array, advance(dec, 8));
	}

	/**
	 * Decode next field tag.
	 *
	 * @return field tag
	 * @throws IndexOutOfBoundsException insufficient data {@linkplain #hasRemaining() remaining}
	 * @throws IllegalStateException coding is malformed
	 * @since 1.2
	 */
	public @FieldTag int decodeFieldTag() {
		long a = decodeVarint64Impl(this);
		int t = (int) (a & 0xffffffffL);
		int n = t >>> BITS_PER_WIRE_TYPE;
		int w = t & TAG_WIRE_TYPE_MASK;

		//noinspection ConstantValue
		Preconditions.checkState(
			a == (t & 0xffffffffL) &&
			n >= MIN_FIELD_NUMBER && n <= MAX_FIELD_NUMBER &&
			isWireTypeValid(w)
		);
		return t;
	}

	/**
	 * Decode next {@code varint} as {@code bool}.
	 *
	 * @return decoded value
	 * @throws IndexOutOfBoundsException insufficient data {@linkplain #hasRemaining() remaining}
	 * @throws IllegalStateException coding is malformed
	 * @since 1.2
	 */
	public boolean decodeBool() {
		int a = decodeVarint7Impl(this.contentDecoder);

		return a < 0x80 ? (a != 0) : decodeVarint64Impl(this.contentDecoder) != 0L;
	}

	/**
	 * Decode next {@code varint} as {@code uint32}.
	 *
	 * @return decoded value
	 * @throws IndexOutOfBoundsException insufficient data {@linkplain #hasRemaining() remaining}
	 * @throws IllegalStateException coding is malformed
	 * @since 1.2
	 */
	public int decodeUint32() {
		return decodeVarint32Impl(this.contentDecoder);
	}

	/**
	 * Decode next {@code varint} as {@code sint32}.
	 *
	 * @return decoded value
	 * @throws IndexOutOfBoundsException insufficient data {@linkplain #hasRemaining() remaining}
	 * @throws IllegalStateException coding is malformed
	 * @since 1.2
	 */
	public int decodeSint32() {
		return zigzagOf32(this.decodeUint32());
	}

	/**
	 * Decode next {@code varint} as {@code uint64}.
	 *
	 * @return decoded value
	 * @throws IndexOutOfBoundsException insufficient data {@linkplain #hasRemaining() remaining}
	 * @throws IllegalStateException coding is malformed
	 * @since 1.2
	 */
	public long decodeUint64() {
		return decodeVarint64Impl(this.contentDecoder);
	}

	/**
	 * Decode next {@code varint} as {@code sint64}.
	 *
	 * @return decoded value
	 * @throws IndexOutOfBoundsException insufficient data {@linkplain #hasRemaining() remaining}
	 * @throws IllegalStateException coding is malformed
	 * @since 1.2
	 */
	public long decodeSint64() {
		return zigzagOf64(this.decodeUint64());
	}

	/**
	 * Decode next {@code fixed32} as {@code int}.
	 *
	 * @return decoded value
	 * @throws IndexOutOfBoundsException insufficient data {@linkplain #hasRemaining() remaining}
	 * @throws IllegalStateException coding is malformed
	 * @since 1.2
	 */
	public int decodeFixed32() {
		return decodeFixed32Impl(this.contentDecoder);
	}

	/**
	 * Decode next {@code fixed64} as {@code long}.
	 *
	 * @return decoded value
	 * @throws IndexOutOfBoundsException insufficient data {@linkplain #hasRemaining() remaining}
	 * @throws IllegalStateException coding is malformed
	 * @since 1.2
	 */
	public long decodeFixed64() {
		return decodeFixed64Impl(this.contentDecoder);
	}

	/**
	 * Decode next {@code fixed32} as {@code float}.
	 *
	 * @return decoded value
	 * @throws IndexOutOfBoundsException insufficient data {@linkplain #hasRemaining() remaining}
	 * @throws IllegalStateException coding is malformed
	 * @since 1.2
	 */
	public float decodeFloat() {
		return Float.intBitsToFloat(this.decodeFixed32());
	}

	/**
	 * Decode next {@code fixed64} as {@code double}.
	 *
	 * @return decoded value
	 * @throws IndexOutOfBoundsException insufficient data {@linkplain #hasRemaining() remaining}
	 * @throws IllegalStateException coding is malformed
	 * @since 1.2
	 */
	public double decodeDouble() {
		return Double.longBitsToDouble(this.decodeFixed64());
	}

	/*
	 * Begin decoding `len` sequence. This returns a tuple of schema and content array limits
	 * in the low and high -order bits. If coding is malformed or there is insufficient data
	 * remaining to decode sequence this fails with `IllegalStateException` or
	 * `IndexOutOfBoundsException`.
	 *
	 * Upon return, the limit of `schema` and `content` is updated to reflect the number of
	 * readable bytes within the sequence. The `endDecodeLenImpl()` method should be used to
	 * restore the limits after decoding the sequence.
	 */
	private static long beginDecodeLenImpl(ProtobufDecoder schema, ProtobufDecoder content) {
		int schemaLim = Math.min(schema.arrayLimit, schema.array.length);
		int seqLen = decodeVarint32Impl(schema);

		Preconditions.checkState(seqLen >= 0);
		if (schema == content) {
			// coding isn't split, we don't expect schema length
			Preconditions.checkFromIndexSize(schema.arrayOffset, seqLen, schemaLim);
			schema.arrayLimit = schema.arrayOffset + seqLen;
			return schemaLim;
		}

		/*
		 * Split `len` coding has two `varint` values, first is the usual `len` sequence length
		 * `seqLen` and second is `seqSchemaLen`.
		 */
		int contentLim = Math.min(content.arrayLimit, content.array.length);
		int seqSchemaLen = decodeVarint32Impl(schema);
		int seqContentLen = seqLen - seqSchemaLen;

		Preconditions.checkState(seqSchemaLen >= 0 && seqSchemaLen <= seqLen);
		Preconditions.checkFromIndexSize(schema.arrayOffset, seqSchemaLen, schemaLim);
		Preconditions.checkFromIndexSize(content.arrayOffset, seqContentLen, contentLim);
		schema.arrayLimit = schema.arrayOffset + seqSchemaLen;
		content.arrayLimit = content.arrayOffset + seqContentLen;
		return ((contentLim & 0xffffffffL) << 32) | (schemaLim & 0xffffffffL);
	}

	/*
	 * Restore array limits of schema and content decoders based on `lims` tuple containing
	 * schema and content limits in low and high -order bits, respectively. Fails with
	 * `IndexOutOfBoundsException` if `lims` is malformed.
	 */
	private static void
	endDecodeLenImpl(ProtobufDecoder schema, ProtobufDecoder content, long lims) {
		int schemaLim = (int) (lims & 0xffffffffL);
		int contentLim = (int) (lims >>> 32);

		Preconditions.checkFromToIndex(schema.arrayLimit, schemaLim, schema.array.length);
		schema.arrayOffset = schema.arrayLimit;
		schema.arrayLimit = schemaLim;
		if (schema != content) {
			Preconditions.checkFromToIndex(content.arrayLimit, contentLim, content.array.length);
			content.arrayOffset = content.arrayLimit;
			content.arrayLimit = contentLim;
		}
	}

	/**
	 * Decode next {@code len} sequence into arbitrary value given a state.
	 * <p>This does, in the following order,
	 * <ol>
	 *     <li>Decode length limit {@code n} for next {@code len} sequence.</li>
	 *     <li>Prepares {@code this} to decode {@code n} bytes of the respective sequence.</li>
	 *     <li>Invokes {@code decodeData} with {@code state} and {@code this}.</li>
	 *     <li>Prepares {@code this} to decode content following the decoded sequence.</li>
	 *     <li>Returns value returned by {@code decodeData}.</li>
	 * </ol>
	 *
	 * @param <S> state type
	 * @param <T> decoded value type
	 * @param state state to decode with
	 * @param decodeData function to decode value with
	 * @return decoded value
	 * @throws IndexOutOfBoundsException insufficient data {@linkplain #hasRemaining() remaining}
	 * @throws IllegalStateException coding is malformed
	 * @since 1.2
	 */
	public <S, T> T decodeLen(S state, BiFunction<S, ProtobufDecoder, T> decodeData) {
		long lims = beginDecodeLenImpl(this, this.contentDecoder);
		T rv = decodeData.apply(state, this);

		endDecodeLenImpl(this, this.contentDecoder, lims);
		return rv;
	}

	/**
	 * Decode next {@code len} sequence into arbitrary value.
	 * <p>Shorthand for:
	 * {@snippet :
	 * decodeLen(decodeData, Function::apply); // @link substring="decodeLen" target="#decodeLen(Object, BiFunction)"
	 * }
	 *
	 * @param <T> decoded value type
	 * @param decodeData function to decode value with
	 * @return decoded value
	 * @throws IndexOutOfBoundsException insufficient data {@linkplain #hasRemaining() remaining}
	 * @throws IllegalStateException coding is malformed
	 * @since 1.2
	 */
	public <T> T decodeLen(Function<ProtobufDecoder, T> decodeData) {
		return this.decodeLen(decodeData, Function::apply);
	}

	/**
	 * Skip next value of a wire type.
	 *
	 * @param type wire type of value to skip
	 * @return {@code this}
	 * @throws IndexOutOfBoundsException insufficient data {@linkplain #hasRemaining() remaining}
	 * @throws IllegalStateException coding is malformed
	 * @since 1.2
	 */
	public ProtobufDecoder skip(@WireType int type) {
		switch (type) {
		case WIRE_FIXED32:
			advance(this.contentDecoder, 4);
			break;
		case WIRE_FIXED64:
			advance(this.contentDecoder, 8);
			break;
		case WIRE_VARINT:
			decodeVarint64Impl(this.contentDecoder);
			break;
		default:
			Preconditions.checkArgument(type == WIRE_LEN);
			endDecodeLenImpl(
				this, this.contentDecoder,
				beginDecodeLenImpl(this, this.contentDecoder)
			);
			break;
		}
		return this;
	}

	/**
	 * Skip next value of a field.
	 * <p>Shorthand for:
	 * {@snippet :
	 * skip(Protobuf.wireTypeOfFieldTag(tag)); // @link substring="skip" target="#skip(int)"
	 * }
	 *
	 * @param tag tag of field to skip value of
	 * @return {@code this}
	 * @throws IndexOutOfBoundsException insufficient data {@linkplain #hasRemaining() remaining}
	 * @throws IllegalStateException coding is malformed
	 * @since 1.2
	 */
	public ProtobufDecoder skipFieldValue(@FieldTag int tag) {
		return this.skip(wireTypeOfFieldTag(tag));
	}

	/*
	 * Begin decoding content-only `len` sequence. This is the same as `beginDecodeLenImpl()` when
	 * coding isn't split. When coding is split, this ensures the sequence schema length is `0`.
	 * This returns the original limit of `content` which must be used to end the decoding using
	 * `endDecodeLenContentImpl()`.
	 */
	private static int beginDecodeLenContentImpl(ProtobufDecoder schema, ProtobufDecoder content) {
		int contentLim = Math.min(content.arrayLimit, content.array.length);
		int seqLen = decodeVarint32Impl(schema);

		Preconditions.checkState(
			seqLen >= 0 &&
			// if coding is split, we expect a `0` schema length for the sequence
			(schema == content || decodeVarint7Impl(schema) == 0)
		);
		Preconditions.checkFromIndexSize(content.arrayOffset, seqLen, contentLim);
		content.arrayLimit = content.arrayOffset + seqLen;
		return contentLim;
	}

	// Restore array limits of content decoder based on `lim`.
	private static void endDecodeLenContentImpl(ProtobufDecoder content, int lim) {
		Preconditions.checkFromToIndex(content.arrayLimit, lim, content.array.length);
		content.arrayOffset = content.arrayLimit;
		content.arrayLimit = lim;
	}

	/**
	 * Decode next {@code len} sequence returning buffer viewing the decoded sequence.
	 * <p>This decodes the next {@code len} sequence, returning a buffer which <i>views</i> the
	 * bytes of the decoded sequence. Thus, the buffer returned is guaranteed to be backed by
	 * the {@linkplain ByteBuffer#array() array} from which content is decoded, either {@link
	 * #array()} or {@link #contentArray()}. The {@linkplain ByteBuffer#arrayOffset() array offset}
	 * of the returned buffer is guaranteed to be the offset within the underlying array at which
	 * the sequence begins, while number of bytes {@linkplain ByteBuffer#remaining() remaining} in
	 * the buffer is guaranteed to equal the length of the sequence.
	 * <p>If coding is {@linkplain #isSplit() split}, this expects decoded sequence to have exactly
	 * {@code 0} schema bytes. If the sequence has non-zero schema bytes, this assumes the coding
	 * is malformed and an error is thrown.
	 *
	 * @return buffer viewing decoded sequence bytes
	 * @throws IndexOutOfBoundsException insufficient data {@linkplain #hasRemaining() remaining}
	 * @throws IllegalStateException coding is malformed
	 * @since 1.2
	 */
	public ByteBuffer decodeByteBufferView() {
		int lim = beginDecodeLenContentImpl(this, this.contentDecoder);
		ByteBuffer view = ByteBuffer.wrap(
			this.contentDecoder.array,
			this.contentDecoder.arrayOffset,
			this.contentDecoder.arrayLimit - this.contentDecoder.arrayOffset
		);

		endDecodeLenContentImpl(this.contentDecoder, lim);
		return view.order(ByteOrder.LITTLE_ENDIAN);
	}

	/**
	 * Decode next {@code len} sequence into newly allocated buffer.
	 * <p>This decodes the next {@code len} sequence, similar to {@link #decodeByteBufferView()};
	 * however, instead of returning a <i>view</i> of the sequence bytes, this constructs a buffer,
	 * using {@code ctor}, stores the sequence bytes into the constructed buffer, and returns the
	 * constructed buffer. The {@linkplain ByteBuffer#position() position} of the returned buffer
	 * is guaranteed to equal the position (exclusive) of the last {@code byte} of the sequence.
	 *
	 * @param ctor function to allocate buffer with
	 * @return buffer into which sequence was stored
	 * @throws java.nio.ReadOnlyBufferException buffer returned by {@code ctor} was {@linkplain
	 * ByteBuffer#isReadOnly() read-only}
	 * @throws java.nio.BufferOverflowException buffer returned by {@code ctor} had fewer bytes
	 * {@linkplain ByteBuffer#remaining() remaining} than the length {@code ctor} was invoked with
	 * @throws IndexOutOfBoundsException insufficient data {@linkplain #hasRemaining() remaining}
	 * @throws IllegalStateException coding is malformed
	 * @since 1.2
	 */
	public ByteBuffer decodeByteBuffer(IntFunction<ByteBuffer> ctor) {
		ByteBuffer view = this.decodeByteBufferView();

		return ctor.apply(view.remaining())
			.put(view);
	}

	/**
	 * Decode next {@code len} sequence into existing or newly allocated {@code byte} array.
	 * <p>Efficient equivalent of:
	 * {@snippet :
	 * return decodeByteBuffer(len -> { // @link substring="decodeByteBuffer" target="#decodeByteBuffer(IntFunction)"
	 *     if (val != null && len == val.length)
	 *         return ByteBuffer.wrap(val);
	 *     return ByteBuffer.allocate(len);
	 * }).array();
	 * }
	 *
	 * @param val existing array to decode sequence into
	 * @return array into which sequence was decoded into
	 * @throws IndexOutOfBoundsException insufficient data {@linkplain #hasRemaining() remaining}
	 * @throws IllegalStateException coding is malformed
	 * @since 1.2
	 */
	public byte[] decodeByteArray(@Nullable byte[] val) {
		int lim = beginDecodeLenContentImpl(this, this.contentDecoder);
		int len = this.contentDecoder.arrayLimit - this.contentDecoder.arrayOffset;

		if (val == null || val.length != len)
			val = new byte[len];
		System.arraycopy(this.contentDecoder.array, this.contentDecoder.arrayOffset, val, 0, len);
		endDecodeLenContentImpl(this.contentDecoder, lim);
		return val;
	}

	/**
	 * Decode next {@code len} sequence into newly allocated {@code byte} array.
	 * <p>Shorthand for:
	 * {@snippet :
	 * decodeByteArray(null); // @link substring="decodeByteArray" target="#decodeByteArray(byte[])"
	 * }
	 *
	 * @return array into which sequence was decoded into
	 * @throws IndexOutOfBoundsException insufficient data {@linkplain #hasRemaining() remaining}
	 * @throws IllegalStateException coding is malformed
	 * @since 1.2
	 */
	public byte[] decodeByteArray() {
		return this.decodeByteArray(null);
	}

	/**
	 * Decode next {@code len} sequence as a UTF-8 encoded string.
	 * <p>Efficient equivalent of:
	 * {@snippet :
	 * new String(decodeByteArray(), StandardCharsets.UTF_8); // @link substring="decodeByteArray" target="#decodeByteArray()"
	 * }
	 *
	 * @return decoded value
	 * @throws IndexOutOfBoundsException insufficient data {@linkplain #hasRemaining() remaining}
	 * @throws IllegalStateException coding is malformed
	 * @since 1.2
	 */
	public String decodeString() {
		int lim = beginDecodeLenContentImpl(this, this.contentDecoder);
		String str = new String(
			this.contentDecoder.array,
			this.contentDecoder.arrayOffset,
			this.contentDecoder.arrayLimit - this.contentDecoder.arrayOffset,
			StandardCharsets.UTF_8
		);

		endDecodeLenContentImpl(this.contentDecoder, lim);
		return str;
	}

	/**
	 * Decode next {@code len} sequence as pair of UTF-8 encoded strings.
	 * <p>Efficient equivalent of:
	 * {@snippet :
	 * decodeLen(dec -> { // @link substring="decodeLen" target="#decodeLen(Function)"
	 *     String a = "";
	 *     String b = "";
	 *
	 *     while (dec.hasRemaining()) { // @link substring="hasRemaining" target="#hasRemaining()"
	 *         int tag = dec.decodeFieldTag(); // @link substring="decodeFieldTag" target="#decodeFieldTag()"
	 *
	 *         if (tag == Protobuf.fieldTagOf(1, Protobuf.WIRE_LEN))
	 *             a = dec.decodeString(); // @link substring="decodeString" target="#decodeString()"
	 *         else if (tag == Protobuf.fieldTagOf(2, Protobuf.WIRE_LEN))
	 *             b = dec.decodeString();
	 * 	   }
	 *     return new Pair<>(a, b);
	 * });
	 * }
	 *
	 * @return decoded value
	 * @throws IndexOutOfBoundsException insufficient data {@linkplain #hasRemaining() remaining}
	 * @throws IllegalStateException coding is malformed
	 * @since 1.2
	 */
	public Pair<String, String> decodeStringPair() {
		long lims = beginDecodeLenImpl(this, this.contentDecoder);
		String a = "";
		String b = "";

		while (this.hasRemaining()) {
			int tag = this.decodeFieldTag();

			if (tag != STRING_PAIR_A_TAG && tag != STRING_PAIR_B_TAG) {
				this.skipFieldValue(tag);
				continue;
			}

			String val = this.decodeString();

			if (tag == STRING_PAIR_A_TAG)
				a = val;
			else
				b = val;
		}
		endDecodeLenImpl(this, this.contentDecoder, lims);
		return new Pair<>(a, b);
	}

	// Count number of `varint` packed into a sequence.
	private static int countPackedVarint(ProtobufDecoder dec) {
		byte[] src = dec.array;
		int off = dec.arrayOffset;
		int lim = dec.arrayLimit;
		int len = 0;

		for (; (off + 8) <= lim; off += 8) {
			long a = isolateTerminatingVarintBytes(loadLongLe(src, off));

			len += (int) Long.remainderUnsigned(Long.divideUnsigned(a, 0x80), 0xff);
		}
		for (; off < lim; off++) {
			if ((src[off] & 0xff) < 0x80)
				len++;
		}
		return len;
	}

	/**
	 * Decode next {@code len} sequence of packed {@code varint} as {@code bool} values into
	 * existing or newly allocated {@code boolean} array.
	 * <p>Efficient equivalent of:
	 * {@snippet :
	 * return decodeLen(dst, (dst, dec) -> {
	 *     int n = 0;
	 *
	 *     if (dst == null)
	 * 	       dst = new boolean[0];
	 *     while (dec.hasRemaining()) { // @link substring="hasRemaining" target="#hasRemaining()"
	 * 	       if (n == dst.length)
	 * 	           dst = new boolean[n + 1];
	 *         dst[n++] = dec.decodeBool(); // @link substring="decodeBool" target="#decodeBool()"
	 *     }
	 * 	   if (dst.length != n)
	 * 	       dst = Arrays.copyOf(dst, n);
	 * 	   return dst;
	 * });
	 * }
	 *
	 * @param dst existing array to decode sequence into
	 * @return array into which sequence was decoded into
	 * @throws IndexOutOfBoundsException insufficient data {@linkplain #hasRemaining() remaining}
	 * @throws IllegalStateException coding is malformed
	 * @since 1.2
	 */
	public boolean[] decodePackedBoolArray(@Nullable boolean[] dst) {
		int lim = beginDecodeLenContentImpl(this, this.contentDecoder);
		int len = countPackedVarint(this.contentDecoder);

		if (dst == null || dst.length != len)
			dst = new boolean[len];
		if (len == (this.contentDecoder.arrayLimit - this.contentDecoder.arrayOffset)) {
			// Single `byte` varints!
			for (int i = 0; i < len; i++)
				dst[i] = this.contentDecoder.array[this.contentDecoder.arrayOffset + i] != 0;
		} else {
			for (int i = 0; i < len; i++)
				dst[i] = this.decodeBool();
		}
		endDecodeLenContentImpl(this.contentDecoder, lim);
		return dst;
	}

	/**
	 * Decode next {@code len} sequence of packed {@code varint} as {@code bool} values into
	 * newly allocated {@code boolean} array.
	 * <p>Shorthand for:
	 * {@snippet :
	 * decodePackedBoolArray(null); // @link substring="decodePackedBoolArray" target="#decodePackedBoolArray(boolean[])"
	 * }
	 *
	 * @return array into which sequence was decoded into
	 * @throws IndexOutOfBoundsException insufficient data {@linkplain #hasRemaining() remaining}
	 * @throws IllegalStateException coding is malformed
	 * @since 1.2
	 */
	public boolean[] decodePackedBoolArray() {
		return this.decodePackedBoolArray(null);
	}

	/**
	 * Decode next {@code len} sequence of packed {@code varint} as {@code uint32} values into
	 * existing or newly allocated {@code int} array.
	 * <p>Efficient equivalent of:
	 * {@snippet :
	 * return decodeLen(dst, (dst, dec) -> {
	 *     int n = 0;
	 *
	 *     if (dst == null)
	 * 	       dst = new int[0];
	 *     while (dec.hasRemaining()) { // @link substring="hasRemaining" target="#hasRemaining()"
	 * 	       if (n == dst.length)
	 * 	           dst = new int[n + 1];
	 *         dst[n++] = dec.decodeUint32(); // @link substring="decodeUint32" target="#decodeUint32()"
	 *     }
	 * 	   if (dst.length != n)
	 * 	       dst = Arrays.copyOf(dst, n);
	 * 	   return dst;
	 * });
	 * }
	 *
	 * @param dst existing array to decode sequence into
	 * @return array into which sequence was decoded into
	 * @throws IndexOutOfBoundsException insufficient data {@linkplain #hasRemaining() remaining}
	 * @throws IllegalStateException coding is malformed
	 * @since 1.2
	 */
	public int[] decodePackedUint32Array(@Nullable int[] dst) {
		int lim = beginDecodeLenContentImpl(this, this.contentDecoder);
		int len = countPackedVarint(this.contentDecoder);

		if (dst == null || dst.length != len)
			dst = new int[len];
		for (int i = 0; i < len; i++)
			dst[i] = this.decodeUint32();
		endDecodeLenContentImpl(this.contentDecoder, lim);
		return dst;
	}

	/**
	 * Decode next {@code len} sequence of packed {@code varint} as {@code uint32} values into
	 * newly allocated {@code int} array.
	 * <p>Shorthand for:
	 * {@snippet :
	 * decodePackedUint32Array(null); // @link substring="decodePackedUint32Array" target="#decodePackedUint32Array(int[])"
	 * }
	 *
	 * @return array into which sequence was decoded into
	 * @throws IndexOutOfBoundsException insufficient data {@linkplain #hasRemaining() remaining}
	 * @throws IllegalStateException coding is malformed
	 * @since 1.2
	 */
	public int[] decodePackedUint32Array() {
		return this.decodePackedUint32Array(null);
	}

	/**
	 * Decode next {@code len} sequence of packed {@code varint} as {@code sint32} values into
	 * existing or newly allocated {@code int} array.
	 * <p>Efficient equivalent of:
	 * {@snippet :
	 * return decodeLen(dst, (dst, dec) -> {
	 *     int n = 0;
	 *
	 *     if (dst == null)
	 * 	       dst = new int[0];
	 *     while (dec.hasRemaining()) { // @link substring="hasRemaining" target="#hasRemaining()"
	 * 	       if (n == dst.length)
	 * 	           dst = new int[n + 1];
	 *         dst[n++] = dec.decodeSint32(); // @link substring="decodeSint32" target="#decodeSint32()"
	 *     }
	 * 	   if (dst.length != n)
	 * 	       dst = Arrays.copyOf(dst, n);
	 * 	   return dst;
	 * });
	 * }
	 *
	 * @param dst existing array to decode sequence into
	 * @return array into which sequence was decoded into
	 * @throws IndexOutOfBoundsException insufficient data {@linkplain #hasRemaining() remaining}
	 * @throws IllegalStateException coding is malformed
	 * @since 1.2
	 */
	public int[] decodePackedSint32Array(@Nullable int[] dst) {
		dst = this.decodePackedUint32Array(dst);
		for (int i = 0; i < dst.length; i++)
			dst[i] = zigzagOf32(dst[i]);
		return dst;
	}

	/**
	 * Decode next {@code len} sequence of packed {@code varint} as {@code sint32} values into
	 * newly allocated {@code int} array.
	 * <p>Shorthand for:
	 * {@snippet :
	 * decodePackedSint32Array(null); // @link substring="decodePackedSint32Array" target="#decodePackedSint32Array(int[])"
	 * }
	 *
	 * @return array into which sequence was decoded into
	 * @throws IndexOutOfBoundsException insufficient data {@linkplain #hasRemaining() remaining}
	 * @throws IllegalStateException coding is malformed
	 * @since 1.2
	 */
	public int[] decodePackedSint32Array() {
		return this.decodePackedSint32Array(null);
	}

	/**
	 * Decode next {@code len} sequence of packed {@code varint} as {@code uint64} values into
	 * existing or newly allocated {@code long} array.
	 * <p>Efficient equivalent of:
	 * {@snippet :
	 * return decodeLen(dst, (dst, dec) -> {
	 *     int n = 0;
	 *
	 *     if (dst == null)
	 * 	       dst = new long[0];
	 *     while (dec.hasRemaining()) { // @link substring="hasRemaining" target="#hasRemaining()"
	 * 	       if (n == dst.length)
	 * 	           dst = new long[n + 1];
	 *         dst[n++] = dec.decodeUint64(); // @link substring="decodeUint64" target="#decodeUint64()"
	 *     }
	 * 	   if (dst.length != n)
	 * 	       dst = Arrays.copyOf(dst, n);
	 * 	   return dst;
	 * });
	 * }
	 *
	 * @param dst existing array to decode sequence into
	 * @return array into which sequence was decoded into
	 * @throws IndexOutOfBoundsException insufficient data {@linkplain #hasRemaining() remaining}
	 * @throws IllegalStateException coding is malformed
	 * @since 1.2
	 */
	public long[] decodePackedUint64Array(@Nullable long[] dst) {
		int lim = beginDecodeLenContentImpl(this, this.contentDecoder);
		int len = countPackedVarint(this.contentDecoder);

		if (dst == null || dst.length != len)
			dst = new long[len];
		for (int i = 0; i < len; i++)
			dst[i] = this.decodeUint64();
		endDecodeLenContentImpl(this.contentDecoder, lim);
		return dst;
	}

	/**
	 * Decode next {@code len} sequence of packed {@code varint} as {@code uint64} values into
	 * newly allocated {@code long} array.
	 * <p>Shorthand for:
	 * {@snippet :
	 * decodePackedUint64Array(null); // @link substring="decodePackedUint64Array" target="#decodePackedUint64Array(long[])"
	 * }
	 *
	 * @return array into which sequence was decoded into
	 * @throws IndexOutOfBoundsException insufficient data {@linkplain #hasRemaining() remaining}
	 * @throws IllegalStateException coding is malformed
	 * @since 1.2
	 */
	public long[] decodePackedUint64Array() {
		return this.decodePackedUint64Array(null);
	}

	/**
	 * Decode next {@code len} sequence of packed {@code varint} as {@code sint64} values into
	 * existing or newly allocated {@code long} array.
	 * <p>Efficient equivalent of:
	 * {@snippet :
	 * return decodeLen(dst, (dst, dec) -> {
	 *     int n = 0;
	 *
	 *     if (dst == null)
	 * 	       dst = new long[0];
	 *     while (dec.hasRemaining()) { // @link substring="hasRemaining" target="#hasRemaining()"
	 * 	       if (n == dst.length)
	 * 	           dst = new long[n + 1];
	 *         dst[n++] = dec.decodeSint64(); // @link substring="decodeSint64" target="#decodeSint64()"
	 *     }
	 * 	   if (dst.length != n)
	 * 	       dst = Arrays.copyOf(dst, n);
	 * 	   return dst;
	 * });
	 * }
	 *
	 * @param dst existing array to decode sequence into
	 * @return array into which sequence was decoded into
	 * @throws IndexOutOfBoundsException insufficient data {@linkplain #hasRemaining() remaining}
	 * @throws IllegalStateException coding is malformed
	 * @since 1.2
	 */
	public long[] decodePackedSint64Array(@Nullable long[] dst) {
		dst = this.decodePackedUint64Array(dst);
		for (int i = 0; i < dst.length; i++)
			dst[i] = zigzagOf64(dst[i]);
		return dst;
	}

	/**
	 * Decode next {@code len} sequence of packed {@code varint} as {@code sint64} values into
	 * newly allocated {@code long} array.
	 * <p>Shorthand for:
	 * {@snippet :
	 * decodePackedSint64Array(null); // @link substring="decodePackedSint64Array" target="#decodePackedSint64Array(long[])"
	 * }
	 *
	 * @return array into which sequence was decoded into
	 * @throws IndexOutOfBoundsException insufficient data {@linkplain #hasRemaining() remaining}
	 * @throws IllegalStateException coding is malformed
	 * @since 1.2
	 */
	public long[] decodePackedSint64Array() {
		return this.decodePackedSint64Array(null);
	}

	/**
	 * Decode next {@code len} sequence of packed {@code varint} as {@code uint32} values, minus
	 * a delta, into a 64-bit bitmap.
	 * <p>Efficient equivalent of:
	 * {@snippet :
	 * long bitmap = 0;
	 *
	 * for (int a : decodePackedUint32Array()) { // @link substring="decodePackedUint32Array" target="#decodePackedUint32Array()"
	 *     a -= delta;
	 * 	   if (a >= 0 && a <= 63)
	 *         bitmap |= (1L << a);
	 * }
	 * }
	 *
	 * @param delta delta to subtract from each decoded value
	 * @return decoded value
	 * @throws IndexOutOfBoundsException insufficient data {@linkplain #hasRemaining() remaining}
	 * @throws IllegalStateException coding is malformed
	 * @since 1.2
	 */
	public long decodePackedUint32Bitmap64(int delta) {
		int lim = beginDecodeLenContentImpl(this, this.contentDecoder);
		long bits = 0;

		while (this.contentDecoder.hasRemaining()) {
			int i = this.decodeUint32() - delta;

			if (i >= 0 && i <= 63)
				bits |= (1L << i);
		}
		endDecodeLenContentImpl(this.contentDecoder, lim);
		return bits;
	}

	/**
	 * Decode next {@code len} sequence of packed {@code varint} as {@code uint32} values into a
	 * 64-bit bitmap.
	 * <p>Shorthand for:
	 * {@snippet :
	 * decodePackedUint32Bitmap64(0); // @link substring="decodePackedUint32Bitmap64" target="#decodePackedUint32Bitmap64(int)"
	 * }
	 *
	 * @return decoded value
	 * @throws IndexOutOfBoundsException insufficient data {@linkplain #hasRemaining() remaining}
	 * @throws IllegalStateException coding is malformed
	 * @since 1.2
	 */
	public long decodePackedUint32Bitmap64() {
		return this.decodePackedUint32Bitmap64(0);
	}

	/**
	 * Decode next {@code len} sequence of packed {@code varint} as {@code uint32} values, minus
	 * a delta, into a 32-bit bitmap.
	 * <p>Efficient equivalent of:
	 * {@snippet :
	 * int bitmap = 0;
	 *
	 * for (int a : decodePackedUint32Array()) { // @link substring="decodePackedUint32Array" target="#decodePackedUint32Array()"
	 *     a -= delta;
	 * 	   if (a >= 0 && a <= 31)
	 *         bitmap |= (1 << a);
	 * }
	 * }
	 *
	 * @param delta delta to subtract from each decoded value
	 * @return decoded value
	 * @throws IndexOutOfBoundsException insufficient data {@linkplain #hasRemaining() remaining}
	 * @throws IllegalStateException coding is malformed
	 * @since 1.2
	 */
	public int decodePackedUint32Bitmap32(int delta) {
		return (int) (this.decodePackedUint32Bitmap64(delta) & 0xffffffffL);
	}

	/**
	 * Decode next {@code len} sequence of packed {@code varint} as {@code uint32} values into a
	 * 32-bit bitmap.
	 * <p>Shorthand for:
	 * {@snippet :
	 * decodePackedUint32Bitmap32(0); // @link substring="decodePackedUint32Bitmap32" target="#decodePackedUint32Bitmap32(int)"
	 * }
	 *
	 * @return decoded value
	 * @throws IndexOutOfBoundsException insufficient data {@linkplain #hasRemaining() remaining}
	 * @throws IllegalStateException coding is malformed
	 * @since 1.2
	 */
	public int decodePackedUint32Bitmap32() {
		return this.decodePackedUint32Bitmap32(0);
	}

	/**
	 * Decode next {@code len} sequence of packed {@code fixed32} into existing or newly allocated
	 * {@code int} array.
	 * <p>Efficient equivalent of:
	 * {@snippet :
	 * return decodeLen(dst, (dst, dec) -> {
	 *     int n = 0;
	 *
	 *     if (dst == null)
	 * 	       dst = new int[0];
	 *     while (dec.hasRemaining()) { // @link substring="hasRemaining" target="#hasRemaining()"
	 * 	       if (n == dst.length)
	 * 	           dst = new int[n + 1];
	 *         dst[n++] = dec.decodeFixed32(); // @link substring="decodeFixed32" target="#decodeFixed32()"
	 *     }
	 * 	   if (dst.length != n)
	 * 	       dst = Arrays.copyOf(dst, n);
	 * 	   return dst;
	 * });
	 * }
	 *
	 * @param dst existing array to decode sequence into
	 * @return array into which sequence was decoded into
	 * @throws IndexOutOfBoundsException insufficient data {@linkplain #hasRemaining() remaining}
	 * @throws IllegalStateException coding is malformed
	 * @since 1.2
	 */
	public int[] decodePackedFixed32Array(@Nullable int[] dst) {
		int lim = beginDecodeLenContentImpl(this, this.contentDecoder);
		int off = this.contentDecoder.arrayOffset;
		int lenBytes = this.contentDecoder.arrayLimit - off;
		int len = lenBytes / 4;
		byte[] src = this.contentDecoder.array;

		Preconditions.checkState((lenBytes % 4) == 0);
		if (dst == null || dst.length != len)
			dst = new int[len];
		for (int i = 0; i < len; i++)
			dst[i] = loadIntLe(src, off + i * 4);
		endDecodeLenContentImpl(this.contentDecoder, lim);
		return dst;
	}

	/**
	 * Decode next {@code len} sequence of packed {@code fixed32} into newly allocated {@code int}
	 * array.
	 * <p>Shorthand for:
	 * {@snippet :
	 * decodePackedFixed32Array(null); // @link substring="decodePackedFixed32Array" target="#decodePackedFixed32Array(int[])"
	 * }
	 *
	 * @return array into which sequence was decoded into
	 * @throws IndexOutOfBoundsException insufficient data {@linkplain #hasRemaining() remaining}
	 * @throws IllegalStateException coding is malformed
	 * @since 1.2
	 */
	public int[] decodePackedFixed32Array() {
		return this.decodePackedFixed32Array(null);
	}

	/**
	 * Decode next {@code len} sequence of packed {@code fixed64} into existing or newly allocated
	 * {@code long} array.
	 * <p>Efficient equivalent of:
	 * {@snippet :
	 * return decodeLen(dst, (dst, dec) -> {
	 *     int n = 0;
	 *
	 *     if (dst == null)
	 * 	       dst = new long[0];
	 *     while (dec.hasRemaining()) { // @link substring="hasRemaining" target="#hasRemaining()"
	 * 	       if (n == dst.length)
	 * 	           dst = new long[n + 1];
	 *         dst[n++] = dec.decodeFixed64(); // @link substring="decodeFixed64" target="#decodeFixed64()"
	 *     }
	 * 	   if (dst.length != n)
	 * 	       dst = Arrays.copyOf(dst, n);
	 * 	   return dst;
	 * });
	 * }
	 *
	 * @param dst existing array to decode sequence into
	 * @return array into which sequence was decoded into
	 * @throws IndexOutOfBoundsException insufficient data {@linkplain #hasRemaining() remaining}
	 * @throws IllegalStateException coding is malformed
	 * @since 1.2
	 */
	public long[] decodePackedFixed64Array(@Nullable long[] dst) {
		int lim = beginDecodeLenContentImpl(this, this.contentDecoder);
		int off = this.contentDecoder.arrayOffset;
		int lenBytes = this.contentDecoder.arrayLimit - off;
		int len = lenBytes / 8;
		byte[] src = this.contentDecoder.array;

		Preconditions.checkState((lenBytes % 8) == 0);
		if (dst == null || dst.length != len)
			dst = new long[len];
		for (int i = 0; i < len; i++)
			dst[i] = loadLongLe(src, off + i * 8);
		endDecodeLenContentImpl(this.contentDecoder, lim);
		return dst;
	}

	/**
	 * Decode next {@code len} sequence of packed {@code fixed64} into newly allocated {@code long}
	 * array.
	 * <p>Shorthand for:
	 * {@snippet :
	 * decodePackedFixed64Array(null); // @link substring="decodePackedFixed64Array" target="#decodePackedFixed64Array(long[])"
	 * }
	 *
	 * @return array into which sequence was decoded into
	 * @throws IndexOutOfBoundsException insufficient data {@linkplain #hasRemaining() remaining}
	 * @throws IllegalStateException coding is malformed
	 * @since 1.2
	 */
	public long[] decodePackedFixed64Array() {
		return this.decodePackedFixed64Array(null);
	}

	/**
	 * Decode next {@code len} sequence of packed {@code fixed32} into existing or newly allocated
	 * {@code float} array.
	 * <p>Efficient equivalent of:
	 * {@snippet :
	 * return decodeLen(dst, (dst, dec) -> {
	 *     int n = 0;
	 *
	 *     if (dst == null)
	 * 	       dst = new float[0];
	 *     while (dec.hasRemaining()) { // @link substring="hasRemaining" target="#hasRemaining()"
	 * 	       if (n == dst.length)
	 * 	           dst = new float[n + 1];
	 *         dst[n++] = dec.decodeFloat(); // @link substring="decodeFloat" target="#decodeFloat()"
	 *     }
	 * 	   if (dst.length != n)
	 * 	       dst = Arrays.copyOf(dst, n);
	 * 	   return dst;
	 * });
	 * }
	 *
	 * @param dst existing array to decode sequence into
	 * @return array into which sequence was decoded into
	 * @throws IndexOutOfBoundsException insufficient data {@linkplain #hasRemaining() remaining}
	 * @throws IllegalStateException coding is malformed
	 * @since 1.2
	 */
	public float[] decodePackedFloatArray(@Nullable float[] dst) {
		int lim = beginDecodeLenContentImpl(this, this.contentDecoder);
		int off = this.contentDecoder.arrayOffset;
		int lenBytes = this.contentDecoder.arrayLimit - off;
		int len = lenBytes / 4;
		byte[] src = this.contentDecoder.array;

		Preconditions.checkState((lenBytes % 4) == 0);
		if (dst == null || dst.length != len)
			dst = new float[len];
		for (int i = 0; i < len; i++)
			dst[i] = Float.intBitsToFloat(loadIntLe(src, off + i * 4));
		endDecodeLenContentImpl(this.contentDecoder, lim);
		return dst;
	}

	/**
	 * Decode next {@code len} sequence of packed {@code fixed32} into newly allocated
	 * {@code float} array.
	 * <p>Shorthand for:
	 * {@snippet :
	 * decodePackedFloatArray(null); // @link substring="decodePackedFloatArray" target="#decodePackedFloatArray(float[])"
	 * }
	 *
	 * @return array into which sequence was decoded into
	 * @throws IndexOutOfBoundsException insufficient data {@linkplain #hasRemaining() remaining}
	 * @throws IllegalStateException coding is malformed
	 * @since 1.2
	 */
	public float[] decodePackedFloatArray() {
		return this.decodePackedFloatArray(null);
	}

	/**
	 * Decode next {@code len} sequence of packed {@code fixed64} into existing or newly allocated
	 * {@code double} array.
	 * <p>Efficient equivalent of:
	 * {@snippet :
	 * return decodeLen(dst, (dst, dec) -> {
	 *     int n = 0;
	 *
	 *     if (dst == null)
	 * 	       dst = new double[0];
	 *     while (dec.hasRemaining()) { // @link substring="hasRemaining" target="#hasRemaining()"
	 * 	       if (n == dst.length)
	 * 	           dst = new double[n + 1];
	 *         dst[n++] = dec.decodeDouble(); // @link substring="decodeDouble" target="#decodeDouble()"
	 *     }
	 * 	   if (dst.length != n)
	 * 	       dst = Arrays.copyOf(dst, n);
	 * 	   return dst;
	 * });
	 * }
	 *
	 * @param dst existing array to decode sequence into
	 * @return array into which sequence was decoded into
	 * @throws IndexOutOfBoundsException insufficient data {@linkplain #hasRemaining() remaining}
	 * @throws IllegalStateException coding is malformed
	 * @since 1.2
	 */
	public double[] decodePackedDoubleArray(@Nullable double[] dst) {
		int lim = beginDecodeLenContentImpl(this, this.contentDecoder);
		int off = this.contentDecoder.arrayOffset;
		int lenBytes = this.contentDecoder.arrayLimit - off;
		int len = lenBytes / 8;
		byte[] src = this.contentDecoder.array;

		Preconditions.checkState((lenBytes % 8) == 0);
		if (dst == null || dst.length != len)
			dst = new double[len];
		for (int i = 0; i < len; i++)
			dst[i] = Double.longBitsToDouble(loadLongLe(src, off + i * 8));
		endDecodeLenContentImpl(this.contentDecoder, lim);
		return dst;
	}

	/**
	 * Decode next {@code len} sequence of packed {@code fixed64} into newly allocated
	 * {@code double} array.
	 * <p>Shorthand for:
	 * {@snippet :
	 * decodePackedDoubleArray(null); // @link substring="decodePackedDoubleArray" target="#decodePackedDoubleArray(double[])"
	 * }
	 *
	 * @return array into which sequence was decoded into
	 * @throws IndexOutOfBoundsException insufficient data {@linkplain #hasRemaining() remaining}
	 * @throws IllegalStateException coding is malformed
	 * @since 1.2
	 */
	public double[] decodePackedDoubleArray() {
		return this.decodePackedDoubleArray(null);
	}
}
