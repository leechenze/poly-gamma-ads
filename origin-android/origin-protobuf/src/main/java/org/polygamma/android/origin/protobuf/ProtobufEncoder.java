// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.protobuf;

import static org.polygamma.android.origin.protobuf.Protobuf.STRING_PAIR_A_TAG;
import static org.polygamma.android.origin.protobuf.Protobuf.STRING_PAIR_B_TAG;
import static org.polygamma.android.origin.protobuf.Protobuf.WIRE_FIXED32;
import static org.polygamma.android.origin.protobuf.Protobuf.WIRE_FIXED64;
import static org.polygamma.android.origin.protobuf.Protobuf.WIRE_LEN;
import static org.polygamma.android.origin.protobuf.Protobuf.WIRE_VARINT;
import static org.polygamma.android.origin.protobuf.Protobuf.checkFieldWireType;
import static org.polygamma.android.origin.protobuf.Protobuf.varintSizeOfBits;
import static org.polygamma.android.origin.protobuf.Protobuf.wireTypeOfFieldTag;
import static org.polygamma.android.origin.util.Bits.loadLongLe;
import static org.polygamma.android.origin.util.Bits.storeIntLe;
import static org.polygamma.android.origin.util.Bits.storeLongLe;

import android.os.Build;
import android.util.Pair;

import androidx.annotation.IntRange;
import androidx.annotation.Nullable;

import org.polygamma.android.origin.protobuf.Protobuf.FieldTag;
import org.polygamma.android.origin.util.BiConsumer;
import org.polygamma.android.origin.util.Bits;
import org.polygamma.android.origin.util.Consumer;
import org.polygamma.android.origin.util.Preconditions;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Protocol buffer encoder.
 * <p>Instances of this encode Protocol buffer wire format into a {@code byte} array. This supports
 * encoding schema, such as field numbers and {@code len} sequence sizes, along with content.
 * By default, coding is merged as defined by the Protocol buffer version 3 specification.
 * <p>Coding can be split such that schema and content are encoded into separate arrays. This is
 * beneficial in cases where treatment of content should be separated from schema, such as when
 * used within the context of Fully Homomorphic Encryption (FHE). When coding is split, the
 * encoding for all data-types is the same, except for {@code len} sequences. A merged {@code len}
 * sequence is composed of a schema {@link #encodeUint32(int) uint32} containing the size, in
 * bytes, of the sequence, followed by the content sequence bytes. When coding is split, the
 * schema contains two {@link #encodeUint32(int) uint32}, the first contains the full size of
 * the sequence, in bytes, while the second contains the number of bytes within the sequence that
 * are schema specific. This is important for encoding nested messages, as it allows a decoder to
 * determine how many bytes from the schema and content buffers belong to a {@code len} sequence.
 *
 * @since 1.2
 */
@SuppressWarnings("JavadocDeclaration")
public final class ProtobufEncoder {

	// Default initial capacity for schema array.
	private static final int DEFAULT_INITIAL_SCHEMA_CAPACITY = 16;

	// Default initial capacity for content array.
	private static final int DEFAULT_INITIAL_CONTENT_CAPACITY =
		DEFAULT_INITIAL_SCHEMA_CAPACITY * 2;

	// Default initial capacity for array containing both schema and value.
	private static final int DEFAULT_INITIAL_CAPACITY =
		DEFAULT_INITIAL_SCHEMA_CAPACITY +
		DEFAULT_INITIAL_CONTENT_CAPACITY;

	/**
	 * Construct new encoder with initial underlying target array.
	 *
	 * @param arr initial underlying target array to encode into
	 * @return resulting encoder
	 * @since 1.2
	 */
	public static ProtobufEncoder ofArray(byte[] arr) {
		return new ProtobufEncoder(arr, null);
	}

	/**
	 * Construct new encoder with initial capacity for underlying target array.
	 *
	 * @param cap initial capacity, in bytes
	 * @return resulting encoder
	 * @since 1.2
	 */
	public static ProtobufEncoder ofInitialCapacity(int cap) {
		return ofArray(new byte[cap]);
	}

	/**
	 * Construct new encoder with default initial capacity for underlying target array.
	 *
	 * @return resulting encoder
	 * @since 1.2
	 */
	public static ProtobufEncoder of() {
		return ofInitialCapacity(DEFAULT_INITIAL_CAPACITY);
	}

	/**
	 * Construct new {@linkplain #isSplit() split} encoder with initial underlying arrays.
	 *
	 * @param schemaArr initial array to encode schema into
	 * @param contentArr initial array to encode content into
	 * @return resulting encoder
	 * @throws IllegalArgumentException {@code schemaArr} and {@code contentArr} reference the same
	 * memory region
	 * @since 1.2
	 */
	public static ProtobufEncoder ofSplitArray(byte[] schemaArr, byte[] contentArr) {
		Preconditions.checkArgument(schemaArr != contentArr);
		return new ProtobufEncoder(schemaArr, ofArray(contentArr));
	}

	/**
	 * Construct new {@linkplain #isSplit() split} encoder with initial capacities.
	 *
	 * @param schemaCap initial capacity, in bytes, of schema array
	 * @param contentCap initial capacity, in bytes, of content array
	 * @return resulting encoder
	 * @since 1.2
	 */
	public static ProtobufEncoder ofSplitInitialCapacity(int schemaCap, int contentCap) {
		return ofSplitArray(new byte[schemaCap], new byte[contentCap]);
	}

	/**
	 * Construct new {@linkplain #isSplit() split} encoder with default initial capacities.
	 *
	 * @return resulting encoder
	 * @since 1.2
	 */
	public static ProtobufEncoder ofSplit() {
		return ofSplitInitialCapacity(
			DEFAULT_INITIAL_SCHEMA_CAPACITY,
			DEFAULT_INITIAL_CONTENT_CAPACITY
		);
	}

	/*
	 * When encoding is serial, this is assigned to `this`; otherwise, this is the encoder whose
	 * array we'll be encoding content into while our array we'll be encoding schema into.
	 */
	private final ProtobufEncoder contentEncoder;
	// Array we're encoding into.
	private byte[] array;
	// Offset, within `buffer`, we're encoding from.
	private int arrayOffset;

	private ProtobufEncoder(byte[] arr, @Nullable ProtobufEncoder contentEnc) {
		this.array = Preconditions.checkNotNull(arr);
		this.arrayOffset = 0;
		this.contentEncoder = contentEnc == null ? this : contentEnc;
	}

	/**
	 * Test whether schema and content are encoded into separate {@code byte} arrays.
	 * <p>If this returns {@code true}, schema and content are encoded into separate {@code byte}
	 * arrays, accessible using {@link #schemaArray()} and {@link #contentArray()}, respectively;
	 * otherwise, schema and content are encoded linearly in a single {@code byte} array accessible
	 * using {@link #array()}.
	 *
	 * @return {@code true} if, and only if, schema and content are encoded separately
	 * @since 1.2
	 */
	public boolean isSplit() {
		return this.contentEncoder != this;
	}

	// Ensure we're encoding into a single array.
	private void checkMerged() {
		Preconditions.checkState(this.contentEncoder == this);
	}

	// Ensure we're encoding schema and content separate arrays.
	private void checkSplit() {
		Preconditions.checkState(this.contentEncoder != this);
	}

	/**
	 * Array in which schema and content are encoded.
	 * <p>If encoding is not {@linkplain #isSplit() split}, this returns the array into which
	 * both schema and content are linearly encoded. The first {@link #arrayOffset() n} bytes
	 * of the array are guaranteed to contain bytes encoded from preceeding encode operations
	 * performed on {@code this}.
	 * <p>Note that subsequent encode operations may reallocate the underlying array. Thus the
	 * array returned is valid only until the next encode operation.
	 *
	 * @return encoded contents array
	 * @throws IllegalStateException coding is split
	 * @since 1.2
	 * @see #arrayOffset()
	 */
	public byte[] array() {
		this.checkMerged();
		return this.array;
	}

	/**
	 * Offset at which next encode operation will encode bytes into linear array.
	 * <p>If encoding is not {@linkplain #isSplit() split}, this returns the position
	 * within {@link #array()} at which the next encode operation will encode from.
	 *
	 * @return encode offset
	 * @throws IllegalStateException coding is split
	 * @since 1.2
	 * @see #array()
	 */
	public int arrayOffset() {
		this.checkMerged();
		return this.arrayOffset;
	}

	/**
	 * Construct buffer viewing array into which schema and content are encoded.
	 * <p>Shorthand for:
	 * {@snippet :
	 * ByteBuffer.wrap(
	 *     array(), // @link substring="array" target="#array()"
	 *     0,
	 *     arrayOffset() // @link substring="arrayOffset" target="#arrayOffset()"
	 * );
	 * }
	 *
	 * @return buffer viewing encoded contents array
	 * @since 1.2
	 * @see #array()
	 * @see #arrayOffset()
	 */
	public ByteBuffer asBuffer() {
		this.checkMerged();
		return ByteBuffer.wrap(this.array, 0, this.arrayOffset);
	}

	/**
	 * Construct copy of array into which schema and content are encoded.
	 * <p>Shorthand for:
	 * {@snippet :
	 * Arrays.copyOfRange( // @link substring="copyOfRange" target="Arrays#copyOfRange(byte[], int, int)"
	 *     array(), // @link substring="array" target="#array()"
	 *     0,
	 *     arrayOffset() // @link substring="arrayOffset" target="#arrayOffset()"
	 * );
	 * }
	 *
	 * @return copy of encoded contents array
	 * @since 1.2
	 * @see #array()
	 * @see #arrayOffset()
	 */
	public byte[] intoArray() {
		this.checkMerged();
		return Arrays.copyOfRange(this.array, 0, this.arrayOffset);
	}

	/**
	 * Array in which schema is encoded.
	 * <p>If encoding is {@linkplain #isSplit() split}, this returns the array into which
	 * schema, such as field tags and {@code len} sequence lengths, is encoded. The first
	 * {@link #schemaArrayOffset() n} bytes of the array are guaranteed to contain schema bytes
	 * encoded from preceeding encode operations performed on {@code this}.
	 * <p>Note that subsequent encode operations may reallocate the underlying array. Thus the
	 * array returned is valid only until the next encode operation.
	 *
	 * @return encoded contents array
	 * @throws IllegalStateException coding is not split
	 * @since 1.2
	 * @see #schemaArrayOffset()
	 */
	public byte[] schemaArray() {
		this.checkSplit();
		return this.array;
	}

	/**
	 * Offset at which schema for next encode operation will encode bytes into schema array.
	 * <p>If encoding is {@linkplain #isSplit() split}, this returns the position within {@link
	 * #schemaArray()} at which the schema contents for next encode operation will encode from.
	 *
	 * @return encode offset
	 * @throws IllegalStateException coding is not split
	 * @since 1.2
	 * @see #schemaArray()
	 */
	public int schemaArrayOffset() {
		this.checkSplit();
		return this.arrayOffset;
	}

	/**
	 * Construct buffer viewing array into which schema is encoded.
	 * <p>Shorthand for:
	 * {@snippet :
	 * ByteBuffer.wrap(
	 *     schemaArray(), // @link substring="schemaArray" target="#schemaArray()"
	 *     0,
	 *     schemaArrayOffset() // @link substring="schemaArrayOffset" target="#schemaArrayOffset()"
	 * );
	 * }
	 *
	 * @return buffer viewing encoded schema array
	 * @since 1.2
	 * @see #schemaArray()
	 * @see #schemaArrayOffset()
	 */
	public ByteBuffer asSchemaBuffer() {
		this.checkSplit();
		return ByteBuffer.wrap(this.array, 0, this.arrayOffset);
	}

	/**
	 * Construct copy of array into which schema is encoded.
	 * <p>Shorthand for:
	 * {@snippet :
	 * Arrays.copyOfRange( // @link substring="copyOfRange" target="Arrays#copyOfRange(byte[], int, int)"
	 *     array(), // @link substring="schemaArray" target="#schemaArray()"
	 *     0,
	 *     arrayOffset() // @link substring="schemaArrayOffset" target="#schemaArrayOffset()"
	 * );
	 * }
	 *
	 * @return copy of encoded contents array
	 * @since 1.2
	 * @see #schemaArray()
	 * @see #schemaArrayOffset()
	 */
	public byte[] intoSchemaArray() {
		this.checkSplit();
		return Arrays.copyOfRange(this.array, 0, this.arrayOffset);
	}

	/**
	 * Array in which content is encoded.
	 * <p>If encoding is {@linkplain #isSplit() split}, this returns the array into which
	 * content is encoded. The first {@link #contentArrayOffset() n} bytes of the array are
	 * guaranteed to contain content bytes encoded from preceeding encode operations performed on
	 * {@code this}.
	 * <p>Note that subsequent encode operations may reallocate the underlying array. Thus the
	 * array returned is valid only until the next encode operation.
	 *
	 * @return encoded contents array
	 * @throws IllegalStateException coding is not split
	 * @since 1.2
	 * @see #contentArrayOffset()
	 */
	public byte[] contentArray() {
		this.checkSplit();
		return this.contentEncoder.array;
	}

	/**
	 * Offset at which content for next encode operation will encode bytes into content array.
	 * <p>If encoding is {@linkplain #isSplit() split}, this returns the position within {@link
	 * #contentArray()} at which the content for next encode operation will encode from.
	 *
	 * @return encode offset
	 * @throws IllegalStateException coding is not split
	 * @since 1.2
	 * @see #contentArray()
	 */
	public int contentArrayOffset() {
		this.checkSplit();
		return this.contentEncoder.arrayOffset;
	}

	/**
	 * Construct buffer viewing array into which content is encoded.
	 * <p>Shorthand for:
	 * {@snippet :
	 * ByteBuffer.wrap(
	 *     contentArray(), // @link substring="contentArray" target="#contentArray()"
	 *     0,
	 *     contentArrayOffset() // @link substring="contentArrayOffset" target="#contentArrayOffset()"
	 * );
	 * }
	 *
	 * @return buffer viewing encoded content array
	 * @since 1.2
	 * @see #contentArray()
	 * @see #contentArrayOffset()
	 */
	public ByteBuffer asContentBuffer() {
		this.checkSplit();
		return ByteBuffer.wrap(this.contentEncoder.array, 0, this.contentEncoder.arrayOffset);
	}

	/**
	 * Construct copy of array into which content is encoded.
	 * <p>Shorthand for:
	 * {@snippet :
	 * Arrays.copyOfRange( // @link substring="copyOfRange" target="Arrays#copyOfRange(byte[], int, int)"
	 *     array(), // @link substring="contentArray" target="#contentArray()"
	 *     0,
	 *     arrayOffset() // @link substring="contentArrayOffset" target="#contentArrayOffset()"
	 * );
	 * }
	 *
	 * @return copy of encoded contents array
	 * @since 1.2
	 * @see #contentArray()
	 * @see #contentArrayOffset()
	 */
	public byte[] intoContentArray() {
		this.checkSplit();
		return Arrays.copyOfRange(this.contentEncoder.array, 0, this.contentEncoder.arrayOffset);
	}

	/**
	 * Reset encoder.
	 * <p>Upon return, offsets within underlying arrays is reset such that the next encode
	 * operation will operate on the beginning of the underlying arrays.
	 *
	 * @return {@code this}
	 * @since 1.2
	 */
	public ProtobufEncoder reset() {
		this.arrayOffset = 0;
		this.contentEncoder.arrayOffset = 0;
		return this;
	}

	/*
	 * Ensure array underlying `enc` has sufficient capacity remaining to encode an additional
	 * `size` bytes. If the array does not have sufficient capacity, it is reallocated to
	 * accomodate at minimum or exactly `size` additional bytes if `exact` is `false` or `true`,
	 * respectively.
	 *
	 * This fails with `IllegalArgumentException` or `IllegalStateException` if `size` is negative
	 * or adding additional `size` bytes of capacity would overflow 2^31.
	 *
	 * NOTE: This always adds `8` to `size`, and aligns `size` to `8`-byte boundary.
	 */
	private static void ensureCapacity(ProtobufEncoder enc, int size, boolean exact) {
		Preconditions.checkArgument(size >= 0);

		int off = enc.arrayOffset;
		long minCapFull = ((long) off) + (((size + 8L) + 7L) & ~7);
		int minCap = (int) minCapFull;
		int currCap = enc.array.length;

		Preconditions.checkState(minCapFull == ((long) minCap));
		if (minCap <= currCap)
			return;

		int newCap =
			exact ? minCap :
			Math.max(minCap, currCap + Math.max(minCap - currCap, currCap >> 1));

		enc.array = Arrays.copyOf(enc.array, newCap);
	}

	// Ensure there's sufficient capacity for schema and content.
	private static void ensureSplitCapacity(
		ProtobufEncoder schema, int schemaSize,
		ProtobufEncoder content, int contentSize,
		boolean exact
	) {
		if (schema == content) {
			ensureCapacity(schema, schemaSize + contentSize, exact);
		} else {
			ensureCapacity(schema, schemaSize, exact);
			ensureCapacity(content, contentSize, exact);
		}
	}

	// Calculate size of a `varint` encoded from a 32-bit `a`.
	private static @IntRange(from = 1, to = 5) int sizeOfVarint32(int a) {
		return varintSizeOfBits(32 - Integer.numberOfLeadingZeros(a | 1));
	}

	// Calculate size of a `varint` encoded from a 64-bit `a`.
	private static @IntRange(from = 1, to = 10) int sizeOfVarint64(long a) {
		return varintSizeOfBits(64 - Long.numberOfLeadingZeros(a | 1));
	}

	// Zig-zag encode 32-bit `a`.
	private static int zigzagOf32(int a) {
		return (a << 1) ^ (a >> 31);
	}

	// Zig-zag encode 64-bit `a`.
	private static long zigzagOf64(long a) {
		return (a << 1) ^ (a >> 63);
	}

	/*
	 * Encode `a` into array underlying `enc`, given an encoded size `size`, in bytes, of `a`. This
	 * assumes array underlying `enc` has sufficient capacity remaining to encode `max(size, 8)`
	 * additional bytes. Coding *will* be malformed if `size` is not equal to `sizeOfVarint32(a)`.
	 */
	private static void
	encodeVarint32Impl(ProtobufEncoder enc, int a, @IntRange(from = 1, to = 5) int size) {
		long v;

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
			v = Long.expand(a & 0xffffffffL, 0xf7f7f7f7fL);
		} else {
			v =  (a & 0x0000007fL) |
				((a & 0x00003f80L) << 1) |
				((a & 0x001fc000L) << 2) |
				((a & 0x0fe00000L) << 3) |
				((a & 0xf0000000L) << 4);
		}

		long mask = ~0L << (size * 8);

		v |= (0x80808080L & ((0x80L << ((size - 1) * 8)) - 1));
		storeLongLe(
			enc.array, enc.arrayOffset,
			(loadLongLe(enc.array, enc.arrayOffset) & mask) | (v & ~mask)
		);
		enc.arrayOffset += size;
	}

	// 64-bit variant of `encodeVarint32Impl()`.
	private static void
	encodeVarint64Impl(ProtobufEncoder enc, long a, @IntRange(from = 1, to = 10) int size) {
		long v;

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
			v = Long.expand(a, 0x7f7f7f7f7f7f7f7fL);
		} else {
			v = (a & 0x000000000fffffffL) | ((a & 0x0fffffff0000000L) << 4);
			v = (v & 0x00003fff00003fffL) | ((v & 0x0fffc0000fffc000L) << 2);
			v = (v & 0x007f007f007f007fL) | ((v & 0x3f803f803f803f80L) << 1);
		}

		if (size < 8) {
			long mask = ~0L << (size * 8);

			v |= 0x8080808080808080L & ((0x80L << ((size - 1) * 8)) - 1);
			v = (loadLongLe(enc.array, enc.arrayOffset) & mask) | (v & ~mask);
		} else if (size == 8) {
			v |= 0x0080808080808080L;
		} else {
			v |= 0x8080808080808080L;
			if (size == 9) {
				enc.array[enc.arrayOffset + 8] = (byte) ((a >>> 56) & 0x7fL);
			} else if (size == 10) {
				enc.array[enc.arrayOffset + 8] = (byte) (((a >>> 56) & 0x7fL) | 0x80L);
				enc.array[enc.arrayOffset + 9] = (byte) (a >>> 63);
			}
		}
		storeLongLe(enc.array, enc.arrayOffset, v);
		enc.arrayOffset += size;
	}

	/*
	 * Encode 32-bit `a` in little-endian byte-order. This expects there's sufficient capacity to
	 * encode an additional `4` bytes.
	 */
	private static void encodeFixed32Impl(ProtobufEncoder enc, int a) {
		storeIntLe(enc.array, enc.arrayOffset, a);
		enc.arrayOffset += 4;
	}

	/*
	 * Encode 32-bit `a` in little-endian byte-order. This expects there's sufficient capacity to
	 * encode an additional `4` bytes.
	 */
	private static void encodeFixed64Impl(ProtobufEncoder enc, long a) {
		storeLongLe(enc.array, enc.arrayOffset, a);
		enc.arrayOffset += 8;
	}

	/**
	 * Encode field tag.
	 *
	 * @param tag tag to encode
	 * @return {@code this}
	 * @throws IllegalStateException there was insufficient capacity to encode {@code tag} and
	 * allocating additional capacity is not possible
	 * @since 1.2
	 */
	public ProtobufEncoder encodeFieldTag(@FieldTag int tag) {
		int size = sizeOfVarint32(tag);

		ensureCapacity(this, size, true);
		encodeVarint32Impl(this, tag, size);
		return this;
	}

	/**
	 * Encode {@code bool} value as {@code varint}.
	 *
	 * @param a value to encode
	 * @return {@code this}
	 * @throws IllegalStateException there was insufficient capacity to encode {@code a} and
	 * allocating additional capacity is not possible
	 * @since 1.2
	 */
	public ProtobufEncoder encodeBool(boolean a) {
		ensureCapacity(this.contentEncoder, 1, true);
		this.contentEncoder.array[this.contentEncoder.arrayOffset++] = (byte) (a ? 1 : 0);
		return this;
	}

	/**
	 * Encode {@code varint} field with {@code bool} value.
	 * <p>Efficient equivalent of:
	 * {@snippet :
	 * if (a) {
	 *     encodeFieldTag(tag) // @link substring="encodeFieldTag" target="#encodeFieldTag(int)"
	 * 	       .encodeBool(a); // @link substring="encodeBool" target="#encodeBool(boolean)"
	 * }
	 * }
	 *
	 * @param tag tag of field to encode
	 * @param a value to encode
	 * @return {@code this}
	 * @throws IllegalArgumentException wire type of {@code tag} is not {@link
	 * Protobuf#WIRE_VARINT varint}
	 * @throws IllegalStateException there was insufficient capacity to encode {@code a} and
	 * allocating additional capacity is not possible
	 * @since 1.2
	 */
	public ProtobufEncoder encodeBoolField(@FieldTag int tag, boolean a) {
		checkFieldWireType(tag, WIRE_VARINT);
		if (a) {
			int tagSize = sizeOfVarint32(tag);

			ensureSplitCapacity(this, tagSize, this.contentEncoder, 1, true);
			encodeVarint32Impl(this, tag, tagSize);
			this.contentEncoder.array[this.contentEncoder.arrayOffset++] = (byte) 1;
		}
		return this;
	}

	/**
	 * Encode {@code uint32} value as {@code varint}.
	 *
	 * @param a value to encode
	 * @return {@code this}
	 * @throws IllegalStateException there was insufficient capacity to encode {@code a} and
	 * allocating additional capacity is not possible
	 * @since 1.2
	 */
	public ProtobufEncoder encodeUint32(int a) {
		int size = sizeOfVarint32(a);

		ensureCapacity(this.contentEncoder, size, true);
		encodeVarint32Impl(this.contentEncoder, a, size);
		return this;
	}

	/**
	 * Encode {@code sint32} value as {@code varint}.
	 *
	 * @param a value to encode
	 * @return {@code this}
	 * @throws IllegalStateException there was insufficient capacity to encode {@code a} and
	 * allocating additional capacity is not possible
	 * @since 1.2
	 */
	public ProtobufEncoder encodeSint32(int a) {
		return this.encodeUint32(zigzagOf32(a));
	}

	/**
	 * Encode {@code uint64} value as {@code varint}.
	 *
	 * @param a value to encode
	 * @return {@code this}
	 * @throws IllegalStateException there was insufficient capacity to encode {@code a} and
	 * allocating additional capacity is not possible
	 * @since 1.2
	 */
	public ProtobufEncoder encodeUint64(long a) {
		int size = sizeOfVarint64(a);

		ensureCapacity(this.contentEncoder, size, true);
		encodeVarint64Impl(this.contentEncoder, a, size);
		return this;
	}

	/**
	 * Encode {@code sint64} value as {@code varint}.
	 *
	 * @param a value to encode
	 * @return {@code this}
	 * @throws IllegalStateException there was insufficient capacity to encode {@code a} and
	 * allocating additional capacity is not possible
	 * @since 1.2
	 */
	public ProtobufEncoder encodeSint64(long a) {
		return this.encodeUint64(zigzagOf64(a));
	}

	/**
	 * Encode {@code int} value as {@code fixed32}.
	 *
	 * @param a value to encode
	 * @return {@code this}
	 * @throws IllegalStateException there was insufficient capacity to encode {@code a} and
	 * allocating additional capacity is not possible
	 * @since 1.2
	 */
	public ProtobufEncoder encodeFixed32(int a) {
		ensureCapacity(this.contentEncoder, 4, true);
		encodeFixed32Impl(this.contentEncoder, a);
		return this;
	}

	/**
	 * Encode {@code long} value as {@code fixed64}.
	 *
	 * @param a value to encode
	 * @return {@code this}
	 * @throws IllegalStateException there was insufficient capacity to encode {@code a} and
	 * allocating additional capacity is not possible
	 * @since 1.2
	 */
	public ProtobufEncoder encodeFixed64(long a) {
		ensureCapacity(this.contentEncoder, 8, true);
		encodeFixed64Impl(this.contentEncoder, a);
		return this;
	}

	/**
	 * Encode {@code float} value as {@code fixed32}.
	 *
	 * @param a value to encode
	 * @return {@code this}
	 * @throws IllegalStateException there was insufficient capacity to encode {@code a} and
	 * allocating additional capacity is not possible
	 * @since 1.2
	 */
	public ProtobufEncoder encodeFloat(float a) {
		return this.encodeFixed32(Float.floatToIntBits(a));
	}

	/**
	 * Encode {@code double} value as {@code fixed64}.
	 *
	 * @param a value to encode
	 * @return {@code this}
	 * @throws IllegalStateException there was insufficient capacity to encode {@code a} and
	 * allocating additional capacity is not possible
	 * @since 1.2
	 */
	public ProtobufEncoder encodeDouble(double a) {
		return this.encodeFixed64(Double.doubleToLongBits(a));
	}

	/**
	 * Encode {@code varint} or {@code fixed32} field with unsigned {@code int} value.
	 * <p>Efficient equivalent of:
	 * {@snippet :
	 * if (a != 0) {
	 *     encodeFieldTag(tag); // @link substring="encodeFieldTag" target="#encodeFieldTag(int)"
	 *     if (Protobuf.wireTypeOfFieldTag(tag) == WIRE_VARINT)
	 *         encodeUint32(a); // @link substring="encodeUint32" target="#encodeUint32(int)"
	 *     else
	 *         encodeFixed32(a); // @link substring="encodeFixed32" target="#encodeFixed32(int)"
	 * }
	 * }
	 *
	 * @param tag tag of field to encode
	 * @param a value to encode
	 * @return {@code this}
	 * @throws IllegalArgumentException wire type of {@code tag} is not {@link
	 * Protobuf#WIRE_VARINT varint} or {@link Protobuf#WIRE_FIXED32 fixed32}
	 * @throws IllegalStateException there was insufficient capacity to encode {@code a} and
	 * allocating additional capacity is not possible
	 * @since 1.2
	 */
	public ProtobufEncoder encodeUnsignedIntField(@FieldTag int tag, int a) {
		if (a == 0)
			return this;

		boolean varint = wireTypeOfFieldTag(tag) == WIRE_VARINT;
		int tagSize = sizeOfVarint32(tag);
		int aSize = varint ? sizeOfVarint32(a) : 4;

		if (!varint)
			checkFieldWireType(tag, WIRE_FIXED32);
		ensureSplitCapacity(this, tagSize, this.contentEncoder, aSize, true);
		encodeVarint32Impl(this, tag, tagSize);
		if (varint)
			encodeVarint32Impl(this.contentEncoder, a, aSize);
		else
			encodeFixed32Impl(this.contentEncoder, a);
		return this;
	}

	/**
	 * Encode {@code varint} or {@code fixed32} field with signed {@code int} value.
	 * <p>Efficient equivalent of:
	 * {@snippet :
	 * if (a != 0) {
	 *     encodeFieldTag(tag); // @link substring="encodeFieldTag" target="#encodeFieldTag(int)"
	 *     if (Protobuf.wireTypeOfFieldTag(tag) == WIRE_VARINT)
	 *         encodeSint32(a); // @link substring="encodeSint32" target="#encodeSint32(int)"
	 *     else
	 *         encodeFixed32(a); // @link substring="encodeFixed32" target="#encodeFixed32(int)"
	 * }
	 * }
	 *
	 * @param tag tag of field to encode
	 * @param a value to encode
	 * @return {@code this}
	 * @throws IllegalArgumentException wire type of {@code tag} is not {@link
	 * Protobuf#WIRE_VARINT varint} or {@link Protobuf#WIRE_FIXED32 fixed32}
	 * @throws IllegalStateException there was insufficient capacity to encode {@code a} and
	 * allocating additional capacity is not possible
	 * @since 1.2
	 */
	public ProtobufEncoder encodeSignedIntField(@FieldTag int tag, int a) {
		if (wireTypeOfFieldTag(tag) == WIRE_VARINT)
			a = zigzagOf32(a);
		return this.encodeUnsignedIntField(tag, a);
	}

	/**
	 * Encode {@code varint} or {@code fixed64} field with unsigned {@code long} value.
	 * <p>Efficient equivalent of:
	 * {@snippet :
	 * if (a != 0) {
	 *     encodeFieldTag(tag); // @link substring="encodeFieldTag" target="#encodeFieldTag(int)"
	 *     if (Protobuf.wireTypeOfFieldTag(tag) == WIRE_VARINT)
	 *         encodeUint64(a); // @link substring="encodeUint64" target="#encodeUint64(long)"
	 *     else
	 *         encodeFixed64(a); // @link substring="encodeFixed64" target="#encodeFixed64(long)"
	 * }
	 * }
	 *
	 * @param tag tag of field to encode
	 * @param a value to encode
	 * @return {@code this}
	 * @throws IllegalArgumentException wire type of {@code tag} is not {@link
	 * Protobuf#WIRE_VARINT varint} or {@link Protobuf#WIRE_FIXED64 fixed64}
	 * @throws IllegalStateException there was insufficient capacity to encode {@code a} and
	 * allocating additional capacity is not possible
	 * @since 1.2
	 */
	public ProtobufEncoder encodeUnsignedLongField(@FieldTag int tag, long a) {
		if (a == 0L)
			return this;

		boolean varint = wireTypeOfFieldTag(tag) == WIRE_VARINT;
		int tagSize = sizeOfVarint32(tag);
		int aSize = varint ? sizeOfVarint64(a) : 8;

		if (!varint)
			checkFieldWireType(tag, WIRE_FIXED64);
		ensureSplitCapacity(this, tagSize, this.contentEncoder, aSize, true);
		encodeVarint32Impl(this, tag, tagSize);
		if (varint)
			encodeVarint64Impl(this.contentEncoder, a, aSize);
		else
			encodeFixed64Impl(this.contentEncoder, a);
		return this;
	}

	/**
	 * Encode {@code varint} or {@code fixed64} field with signed {@code long} value.
	 * <p>Efficient equivalent of:
	 * {@snippet :
	 * if (a != 0) {
	 *     encodeFieldTag(tag); // @link substring="encodeFieldTag" target="#encodeFieldTag(int)"
	 *     if (Protobuf.wireTypeOfFieldTag(tag) == WIRE_VARINT)
	 *         encodeSint64(a); // @link substring="encodeSint64" target="#encodeSint64(long)"
	 *     else
	 *         encodeFixed64(a); // @link substring="encodeFixed64" target="#encodeFixed64(long)"
	 * }
	 * }
	 *
	 * @param tag tag of field to encode
	 * @param a value to encode
	 * @return {@code this}
	 * @throws IllegalArgumentException wire type of {@code tag} is not {@link
	 * Protobuf#WIRE_VARINT varint} or {@link Protobuf#WIRE_FIXED64 fixed64}
	 * @throws IllegalStateException there was insufficient capacity to encode {@code a} and
	 * allocating additional capacity is not possible
	 * @since 1.2
	 */
	public ProtobufEncoder encodeSignedLongField(@FieldTag int tag, long a) {
		if (wireTypeOfFieldTag(tag) == WIRE_VARINT)
			a = zigzagOf64(a);
		return this.encodeUnsignedLongField(tag, a);
	}

	/**
	 * Encode {@code fixed32} field with {@code float} value.
	 * <p>Efficient equivalent of:
	 * {@snippet :
	 * if (a != 0.f) {
	 *     encodeFieldTag(tag) // @link substring="encodeFieldTag" target="#encodeFieldTag(int)"
	 *         .encodeFloat(a); // @link substring="encodeFloat" target="#encodeFloat(float)"
	 * }
	 * }
	 *
	 * @param tag tag of field to encode
	 * @param a value to encode
	 * @return {@code this}
	 * @throws IllegalArgumentException wire type of {@code tag} is not {@link
	 * Protobuf#WIRE_FIXED32 fixed32}
	 * @throws IllegalStateException there was insufficient capacity to encode {@code a} and
	 * allocating additional capacity is not possible
	 * @since 1.2
	 */
	public ProtobufEncoder encodeFloatField(@FieldTag int tag, float a) {
		checkFieldWireType(tag, WIRE_FIXED32);
		return this.encodeUnsignedIntField(tag, Float.floatToIntBits(a));
	}

	/**
	 * Encode {@code fixed64} field with {@code double} value.
	 * <p>Efficient equivalent of:
	 * {@snippet :
	 * if (a != 0.f) {
	 *     encodeFieldTag(tag) // @link substring="encodeFieldTag" target="#encodeFieldTag(int)"
	 *         .encodeDouble(a); // @link substring="encodeDouble" target="#encodeDouble(double)"
	 * }
	 * }
	 *
	 * @param tag tag of field to encode
	 * @param a value to encode
	 * @return {@code this}
	 * @throws IllegalArgumentException wire type of {@code tag} is not {@link
	 * Protobuf#WIRE_FIXED64 fixed64}
	 * @throws IllegalStateException there was insufficient capacity to encode {@code a} and
	 * allocating additional capacity is not possible
	 * @since 1.2
	 */
	public ProtobufEncoder encodeDoubleField(@FieldTag int tag, double a) {
		checkFieldWireType(tag, WIRE_FIXED64);
		return this.encodeUnsignedLongField(tag, Double.doubleToLongBits(a));
	}

	// Encode schema for empty `len` sequence, if `tag` is zero.
	private static void
	encodeEmptyLenSchema(ProtobufEncoder schema, ProtobufEncoder content, int tag) {
		if (tag == 0) {
			ensureCapacity(schema, 2, true);
			schema.array[schema.arrayOffset++] = 0;
			if (schema != content)
				schema.array[schema.arrayOffset++] = 0;
		}
	}

	/*
	 * Encode schema for `len` sequence with size `seqLen` and schema size `seqSchemaLen`, where
	 * `seqLen - seqSchemaLen` is equal to the content length of the sequence. If `seqSchemaLen`
	 * is non-zero, this ensures there's sufficient capacity in `schema` and `content` to encode
	 * `tagSize + seqLenSize + sizeOfVarint32(seqSchemaLen)` and `seqLen - seqSchemaLen`,
	 * respectively.
	 */
	private static void encodeLenSchema(
		ProtobufEncoder schema, ProtobufEncoder content,
		int tag, int tagSize,
		int seqLen, int seqLenSize,
		int seqSchemaLen, int seqSchemaLenSize
	) {
		if (tag != 0)
			encodeVarint32Impl(schema, tag, tagSize);
		encodeVarint32Impl(schema, seqLen, seqLenSize);
		if (schema != content)
			encodeVarint32Impl(schema, seqSchemaLen, seqSchemaLenSize);
	}

	/*
	 * If `schema` and `content` are identical, this ensures there's sufficient capacity in
	 * `schema` and `content` to encode
	 * `sizeOfVarint32(tag) + sizeOfVarint32(seqLen) + seqSchemaLen` and
	 * `seqLen - seqSchemaLen`, respectively. Otherwise, this ensures `schema` and `content` have
	 * sufficient capacity to encode
	 * `sizeOfVarint32(tag) + sizeOfVarint32(seqLen) + seqSchemaLen + sizeOfVarint32(seqSchemaLen)`
	 * and `seqLen - seqSchemaLen`, respectively.
	 *
	 * After capacity is ensured, this is equivalent to invoking `encodeLenSchema`.
	 */
	private static void ensureCapacityAndEncodeLenSchema(
		ProtobufEncoder schema, ProtobufEncoder content,
		int tag, int seqLen, int seqSchemaLen
	) {
		int tagSize = tag == 0 ? 0 : sizeOfVarint32(tag);
		int seqLenSize = sizeOfVarint32(seqLen);
		int seqSchemaLenSize = schema == content ? 0 : sizeOfVarint32(seqSchemaLen);

		ensureSplitCapacity(
			schema, tagSize + seqLenSize + seqSchemaLenSize + seqSchemaLen,
			content, seqLen - seqSchemaLen,
			true
		);
		encodeLenSchema(
			schema, content,
			tag, tagSize,
			seqLen, seqLenSize,
			seqSchemaLen, seqSchemaLenSize
		);
	}

	/*
	 * Encode len sequence from subsequence of `src` (which may be a subsequence of `schema` or
	 * `content`). If `tag` is non-zero and `len` is `0`, this simply returns.
	 */
	private static void encodeByteArrayImpl(
		ProtobufEncoder schema, ProtobufEncoder content,
		int tag, @Nullable byte[] src, int off, int len
	) {
		Preconditions.checkFromIndexSize(off, len, src == null ? 0 : src.length);

		if (len == 0) {
			encodeEmptyLenSchema(schema, content, tag);
			return;
		}

		int tagSize = tag == 0 ? 0 : sizeOfVarint32(tag);
		int seqContentSize = sizeOfVarint32(len);
		int seqSchemaSize = schema == content ? 0 : 1;
		int schemaSize = tagSize + seqContentSize + seqSchemaSize;

		/*
		 * `schema` and `content` may be the same, in which case, we want to copy the value
		 * first since the value may be a subsequence of our underlying array (see `encodeLen`).
		 * Through this, we won't overwrite the value with the schema.
		 */
		ensureSplitCapacity(schema, schemaSize, content, len, true);
		schema.arrayOffset += schemaSize;
		//noinspection DataFlowIssue
		System.arraycopy(src, off, content.array, content.arrayOffset, len);
		schema.arrayOffset -= schemaSize;
		encodeLenSchema(schema, content, tag, tagSize, len, seqContentSize, 0, 1);
		content.arrayOffset += len;
	}

	/**
	 * Encode subsequence of {@code byte} array as {@code len} sequence.
	 *
	 * @param src array to encode subsequence of
	 * @param off offset, within {@code src}, to begin encoding from
	 * @param len number of bytes to encode
	 * @return {@code this}
	 * @throws IndexOutOfBoundsException {@code off} or {@code len} is negative, {@code len} is
	 * non-zero and {@code src} is {@code null}, or {@code src} is non-{@code null} and {@code
	 * off + len} is greater than {@code src.length}
	 * @throws IllegalStateException there was insufficient capacity to encode {@code src} and
	 * allocating additional capacity is not possible
	 * @since 1.2
	 */
	public ProtobufEncoder encodeByteArray(@Nullable byte[] src, int off, int len) {
		encodeByteArrayImpl(this, this.contentEncoder, 0, src, off, len);
		return this;
	}

	/**
	 * Encode {@code byte} array as {@code len} sequence.
	 * <p>Shorthand for:
	 * {@snippet :
	 * encodeByteArray(src, 0, src == null ? 0 : src.length); // @link substring="encodeByteArray" target="#encodeByteArray(byte[], int, int)"
	 * }
	 *
	 * @param src array to encode subsequence of
	 * @return {@code this}
	 * @throws IllegalStateException there was insufficient capacity to encode {@code src} and
	 * allocating additional capacity is not possible
	 * @since 1.2
	 */
	public ProtobufEncoder encodeByteArray(@Nullable byte[] src) {
		return this.encodeByteArray(src, 0, src == null ? 0 : src.length);
	}

	/**
	 * Encode {@code len} sequence field with subsequence of {@code byte} array.
	 * <p>Efficient equivalent of:
	 * {@snippet :
	 * if (src != null && len > 0) {
	 *     encodeFieldTag(tag) // @link substring="encodeFieldTag" target="#encodeFieldTag(int)"
	 *         .encodeByteArray(src, off, len); // @link substring="encodeByteArray" target="#encodeByteArray(byte[], int, int)"
	 * }
	 * }
	 *
	 * @param tag tag of field to encode
	 * @param src array to encode subsequence of
	 * @param off offset, within {@code src}, to begin encoding from
	 * @param len number of bytes to encode
	 * @return {@code this}
	 * @throws IllegalArgumentException wire type of {@code tag} is not {@link Protobuf#WIRE_LEN
	 * len}
	 * @throws IndexOutOfBoundsException {@code off} or {@code len} is negative, {@code len} is
	 * non-zero and {@code src} is {@code null}, or {@code src} is non-{@code null} and {@code
	 * off + len} is greater than {@code src.length}
	 * @throws IllegalStateException there was insufficient capacity to encode {@code src} and
	 * allocating additional capacity is not possible
	 * @since 1.2
	 */
	public ProtobufEncoder
	encodeByteArrayField(@FieldTag int tag, @Nullable byte[] src, int off, int len) {
		checkFieldWireType(tag, WIRE_LEN);
		encodeByteArrayImpl(this, this.contentEncoder, tag, src, off, len);
		return this;
	}

	/**
	 * Encode {@code len} sequence field with {@code byte} array.
	 * <p>Shorthand for:
	 * {@snippet :
	 * encodeByteArrayField(tag, src, 0, src == null ? 0 : src.length); // @link substring="encodeByteArrayField" target="#encodeByteArrayField(int, byte[], int, int)"
	 * }
	 * }
	 *
	 * @param tag tag of field to encode
	 * @param src array to encode subsequence of
	 * @return {@code this}
	 * @throws IllegalArgumentException wire type of {@code tag} is not {@link Protobuf#WIRE_LEN
	 * len}
	 * @throws IllegalStateException there was insufficient capacity to encode {@code src} and
	 * allocating additional capacity is not possible
	 * @since 1.2
	 */
	public ProtobufEncoder encodeByteArrayField(@FieldTag int tag, @Nullable byte[] src) {
		return this.encodeByteArrayField(tag, src, 0, src == null ? 0 : src.length);
	}

	/*
	 * Encode len sequence from `src` (which may be a subsequence of `schema` or `content`). If
	 * `tag` is non-zero and `len` is `0`, this simply returns.
	 */
	private static void encodeByteBufferImpl(
		ProtobufEncoder schema, ProtobufEncoder content,
		int tag, @Nullable ByteBuffer src
	) {
		int len = src == null ? 0 : src.remaining();

		if (len == 0) {
			encodeEmptyLenSchema(schema, content, tag);
		} else if (src.hasArray()) {
			encodeByteArrayImpl(
				schema, content,
				tag,
				src.array(), src.arrayOffset() + src.position(), len
			);
			src.position(src.position() + len);
		} else {
			/*
			 * `src` may still be backed by an array, and it may be a subsequence of us, take
			 * same precaution as in `encodeByteArrayImpl()`.
			 */
			int tagSize = tag == 0 ? 0 : sizeOfVarint32(tag);
			int seqContentSize = sizeOfVarint32(len);
			int seqSchemaSize = schema == content ? 0 : 1;
			int schemaSize = tagSize + seqContentSize + seqSchemaSize;

			ensureSplitCapacity(schema, schemaSize, content, len, true);
			schema.arrayOffset += schemaSize;
			src.get(content.array, content.arrayOffset, len);
			schema.arrayOffset -= schemaSize;
			encodeLenSchema(schema, content, tag, tagSize, len, seqContentSize, 0, 1);
			content.arrayOffset += len;
		}
	}

	/**
	 * Encode contents of {@code byte} buffer as {@code len} sequence.
	 * <p>Efficient equivalent of:
	 * {@snippet :
	 * byte[] seq = new byte[src == null ? 0 : src.remaining()];
	 *
	 * src.get(seq);
	 * encodeByteArray(seq); // @link substring="encodeByteArray" target="#encodeByteArray(byte[])"
	 * }
	 *
	 * @param src buffer to encode contents of
	 * @return {@code this}
	 * @throws IllegalStateException there was insufficient capacity to encode {@code src} and
	 * allocating additional capacity is not possible
	 * @since 1.2
	 */
	public ProtobufEncoder encodeByteBuffer(@Nullable ByteBuffer src) {
		encodeByteBufferImpl(this, this.contentEncoder, 0, src);
		return this;
	}

	/**
	 * Encode {@code len} sequence field with contents of {@code byte} buffer.
	 * <p>Efficient equivalent of:
	 * {@snippet :
	 * if (src != null && src.hasRemaining()) {
	 *     encodeFieldTag(tag) // @link substring="encodeFieldTag" target="#encodeFieldTag(int)"
	 *         .encodeByteBuffer(src); // @link substring="encodeByteBuffer" target="#encodeByteBuffer(ByteBuffer)"
	 * }
	 * }
	 *
	 * @param tag tag of field to encode
	 * @param src buffer to encode contents of
	 * @return {@code this}
	 * @throws IllegalArgumentException wire type of {@code tag} is not {@link Protobuf#WIRE_LEN
	 * len}
	 * @throws IllegalStateException there was insufficient capacity to encode {@code src} and
	 * allocating additional capacity is not possible
	 * @since 1.2
	 */
	public ProtobufEncoder encodeByteBufferField(@FieldTag int tag, @Nullable ByteBuffer src) {
		checkFieldWireType(tag, WIRE_LEN);
		encodeByteBufferImpl(this, this.contentEncoder, tag, src);
		return this;
	}

	/**
	 * Encode UTF-8 encoding of string as {@code len} sequence.
	 * <p>Shorthand for:
	 * {@snippet :
	 * encodeByteArray(str == null ? null : str.getBytes(StandardCharsets.UTF_8)); // @link substring="encodeByteArray" target="#encodeByteArray(byte[])"
	 * }
	 *
	 * @param str string to encode
	 * @return {@code this}
	 * @throws IllegalStateException there was insufficient capacity to encode {@code src} and
	 * allocating additional capacity is not possible
	 * @since 1.2
	 */
	public ProtobufEncoder encodeString(@Nullable String str) {
		return this.encodeByteArray(str == null ? null : str.getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * Encode {@code len} sequence field with string value.
	 * <p>Efficient equivalent of:
	 * {@snippet :
	 * if (src != null && !str.isEmpty()) {
	 *     encodeFieldTag(tag) // @link substring="encodeFieldTag" target="#encodeFieldTag(int)"
	 *         .encodeString(str); // @link substring="encodeString" target="#encodeString(String)"
	 * }
	 * }
	 *
	 * @param tag tag of field to encode
	 * @param str string to encode
	 * @return {@code this}
	 * @throws IllegalArgumentException wire type of {@code tag} is not {@link Protobuf#WIRE_LEN
	 * len}
	 * @throws IllegalStateException there was insufficient capacity to encode {@code src} and
	 * allocating additional capacity is not possible
	 * @since 1.2
	 */
	public ProtobufEncoder encodeStringField(@FieldTag int tag, @Nullable String str) {
		return this.encodeByteArrayField(
			tag,
			str == null ? null :
			str.getBytes(StandardCharsets.UTF_8)
		);
	}

	// Encode len sequence of 2 len sequences from string pair.
	private static void encodeStringPairImpl(
		ProtobufEncoder schema, ProtobufEncoder content,
		int tag, @Nullable Pair<String, String> src
	) {
		byte[] a =
			src == null || src.first == null ? null :
			src.first.getBytes(StandardCharsets.UTF_8);
		byte[] b =
			src == null || src.second == null ? null :
			src.second.getBytes(StandardCharsets.UTF_8);
		int aLen = a == null ? 0 : a.length;
		int bLen = b == null ? 0 : b.length;

		if (aLen == 0 && bLen == 0) {
			encodeEmptyLenSchema(schema, content, tag);
			return;
		}

		int aLenSize = 0, aTagSize = 0;
		int bLenSize = 0, bTagSize = 0;

		if (aLen != 0) {
			aLenSize = sizeOfVarint32(aLen);
			aTagSize = sizeOfVarint32(STRING_PAIR_A_TAG);
		}
		if (bLen != 0) {
			bLenSize = sizeOfVarint32(bLen);
			bTagSize = sizeOfVarint32(STRING_PAIR_B_TAG);
		}

		int seqSchemaLen = (aTagSize + aLenSize) + (bTagSize + bLenSize);
		int seqLen = seqSchemaLen + (aLen + bLen);

		ensureCapacityAndEncodeLenSchema(schema, content, tag, seqLen, seqSchemaLen);
		if (aLen != 0) {
			encodeLenSchema(schema, content, STRING_PAIR_A_TAG, aTagSize, aLen, aLenSize, 0, 1);
			System.arraycopy(a, 0, content.array, content.arrayOffset, aLen);
			content.arrayOffset += aLen;
		}
		if (bLen != 0) {
			encodeLenSchema(schema, content, STRING_PAIR_B_TAG, bTagSize, bLen, bLenSize, 0, 1);
			System.arraycopy(b, 0, content.array, content.arrayOffset, bLen);
			content.arrayOffset += bLen;
		}
	}

	/**
	 * Encode UTF-8 encoding of string pair as {@code len} sequence.
	 * <p>Efficient equivalent of:
	 * {@snippet :
	 * encodeLen(
	 *     val,
	 *     (val, enc) ->
	 *         enc.encodeStringField(Protobuf.fieldTagOf(1, Protobuf.WIRE_LEN), val.first)
	 *             .encodeStringField(Protobuf.fieldTagOf(2, Protobuf.WIRE_LEN), val.second)
	 * );
	 * }
	 *
	 * @param val pair of strings to encode
	 * @return {@code this}
	 * @throws IllegalStateException there was insufficient capacity to encode {@code src} and
	 * allocating additional capacity is not possible
	 * @since 1.2
	 */
	public ProtobufEncoder encodeStringPair(@Nullable Pair<String, String> val) {
		encodeStringPairImpl(this, this.contentEncoder, 0, val);
		return this;
	}

	/**
	 * Encode {@code len} sequence field with UTF-8 encoding of string pair.
	 * <p>Efficient equivalent of:
	 * {@snippet :
	 * encodeLenField(
	 *     tag, val,
	 *     (val, enc) ->
	 *         enc.encodeStringField(Protobuf.fieldTagOf(1, Protobuf.WIRE_LEN), val.first)
	 *             .encodeStringField(Protobuf.fieldTagOf(2, Protobuf.WIRE_LEN), val.second)
	 * );
	 * }
	 *
	 * @param tag tag of field to encode
	 * @param val pair of strings to encode
	 * @return {@code this}
	 * @throws IllegalArgumentException wire type of {@code tag} is not {@link Protobuf#WIRE_LEN
	 * len}
	 * @throws IllegalStateException there was insufficient capacity to encode {@code src} and
	 * allocating additional capacity is not possible
	 * @since 1.2
	 */
	public ProtobufEncoder
	encodeStringPairField(@FieldTag int tag, @Nullable Pair<String, String> val) {
		checkFieldWireType(tag, WIRE_LEN);
		encodeStringPairImpl(this, this.contentEncoder, tag, val);
		return this;
	}

	// Encode optional tag and required `len` sequence length for a dynamic `len` encoding.
	private static void finishEncodeLenImpl(
		ProtobufEncoder schema, int startSchemaOff,
		ProtobufEncoder content, int startContentOff,
		int tag
	) {
		int seqSchemaLen = schema == content ? 0 : schema.arrayOffset - startSchemaOff;
		int seqContentLen = content.arrayOffset - startContentOff;
		int seqLen = seqSchemaLen + seqContentLen;

		if (seqLen == 0) {
			encodeEmptyLenSchema(schema, content, tag);
		} else if (schema == content || seqSchemaLen == 0) {
			/*
			 * Schema and content are merged *or* schema wasn't touched, we can just use
			 * `encodeByteArrayImpl`.
			 */
			schema.arrayOffset = startSchemaOff;
			content.arrayOffset = startContentOff;
			encodeByteArrayImpl(schema, content, tag, content.array, startContentOff, seqLen);
		} else {
			// We only need to fixup schema, content doesn't need to be touched.
			int tagSize = sizeOfVarint32(tag);
			int seqSchemaLenSize = sizeOfVarint32(seqSchemaLen);
			int seqLenSize = sizeOfVarint32(seqLen);
			int schemaLen = tagSize + seqLenSize + seqSchemaLenSize + seqSchemaLen;

			schema.arrayOffset = startSchemaOff;
			ensureCapacity(schema, schemaLen, true);
			System.arraycopy(
				schema.array, startSchemaOff,
				schema.array, startSchemaOff + (schemaLen - seqSchemaLen),
				seqSchemaLen
			);
			encodeLenSchema(
				schema, content,
				tag, tagSize,
				seqLen, seqLenSize,
				seqSchemaLen, seqSchemaLenSize
			);
			schema.arrayOffset += seqSchemaLen;
		}
	}

	// Encode dynamic `len` sequence.
	private static <T> void encodeLenImpl(
		ProtobufEncoder schema, ProtobufEncoder content,
		int tag,
		@Nullable T data, BiConsumer<T, ProtobufEncoder> encodeData, int estSize
	) {
		Preconditions.checkArgument(estSize >= 0);
		ensureSplitCapacity(schema, tag == 0 ? 8 : 16, content, estSize, false);

		int schemaOff = schema.arrayOffset;
		int contentOff = content.arrayOffset;

		if (data != null)
			encodeData.accept(data, schema);
		finishEncodeLenImpl(schema, schemaOff, content, contentOff, tag);
	}

	/**
	 * Encode arbitrary value as {@code len} sequence, with an estimated encoded size.
	 * <p>This initializes {@code this} for encoding an arbitrary {@code len} sequence from
	 * {@code data} using {@code encodeData}. When {@code data} is {@code null}, an {@code 0}
	 * sized {@code len} sequence is encoded. Otherwise, {@code encodeData} is invoked with
	 * {@code data} and {@code this}, respectively. The data encoded by {@code encodeData} into
	 * {@code this} is considered the {@code len} sequence.
	 * <p>If an estimated size, in bytes, of the sequence is known, it may be specified in {@code
	 * estSize}; othewrise, {@code estSize} must be {@code 0}.
	 *
	 * @param <T> type of value to encode
	 * @param data value to encode
	 * @param encodeData function to encode value with
	 * @param estSize estimated size, in bytes, of encoded sequence
	 * @return {@code this}
	 * @throws IllegalArgumentException {@code estSize} is negative
	 * @throws IllegalStateException there was insufficient capacity to encode {@code src} and
	 * allocating additional capacity is not possible
	 * @since 1.2
	 */
	public <T> ProtobufEncoder
	encodeLen(@Nullable T data, BiConsumer<T, ProtobufEncoder> encodeData, int estSize) {
		encodeLenImpl(this, this.contentEncoder, 0, data, encodeData, estSize);
		return this;
	}

	/**
	 * Encode arbitrary value as {@code len} sequence.
	 * <p>Shorthand for:
	 * {@snippet :
	 * encodeLen(data, encodeData, 0); // @link substring="encodeLen" target="#encodeLen(Object, BiConsumer, int)"
	 * }
	 *
	 * @param <T> type of value to encode
	 * @param data value to encode
	 * @param encodeData function to encode value with
	 * @return {@code this}
	 * @throws IllegalStateException there was insufficient capacity to encode {@code src} and
	 * allocating additional capacity is not possible
	 * @since 1.2
	 */
	public <T> ProtobufEncoder
	encodeLen(@Nullable T data, BiConsumer<T, ProtobufEncoder> encodeData) {
		return this.encodeLen(data, encodeData, 0);
	}

	/**
	 * Encode {@code len} sequence field with arbitrary value and estimated encoded value size.
	 * <p>This initializes {@code this} for encoding an arbitrary {@code len} sequence field with
	 * value from {@code data} using {@code encodeData}. When {@code data} is {@code null}
	 * <i>or</i> the {@code encodeData} encoded no bytes, this simply returns. See {@link
	 * #encodeLen(Object, BiConsumer, int)} for more information.
	 *
	 * @param <T> type of value to encode
	 * @param tag tag of field to encode
	 * @param data value to encode
	 * @param encodeData function to encode value with
	 * @param estSize estimated size, in bytes, of encoded sequence
	 * @return {@code this}
	 * @throws IllegalArgumentException {@code estSize} is negative or wire type of {@code tag} is
	 * not {@link Protobuf#WIRE_LEN len}
	 * @throws IllegalStateException there was insufficient capacity to encode {@code src} and
	 * allocating additional capacity is not possible
	 * @since 1.2
	 */
	public <T> ProtobufEncoder encodeLenField(
		@FieldTag int tag,
		@Nullable T data, BiConsumer<T, ProtobufEncoder> encodeData,
		int estSize
	) {
		checkFieldWireType(tag, WIRE_LEN);
		encodeLenImpl(this, this.contentEncoder, tag, data, encodeData, estSize);
		return this;
	}

	/**
	 * Encode {@code len} sequence field with arbitrary value.
	 * <p>Shorthand for:
	 * {@snippet :
	 * encodeLenField(tag, data, encodeData, 0); // @link substring="encodeLenField" target="#encodeLenField(int, Object, BiConsumer, int)"
	 * }
	 *
	 * @param <T> type of value to encode
	 * @param tag tag of field to encode
	 * @param data value to encode
	 * @param encodeData function to encode value with
	 * @return {@code this}
	 * @throws IllegalArgumentException wire type of {@code tag} is not {@link Protobuf#WIRE_LEN
	 * len}
	 * @throws IllegalStateException there was insufficient capacity to encode {@code src} and
	 * allocating additional capacity is not possible
	 * @since 1.2
	 */
	public <T> ProtobufEncoder encodeLenField(
		@FieldTag int tag,
		@Nullable T data, BiConsumer<T, ProtobufEncoder> encodeData
	) {
		return this.encodeLenField(tag, data, encodeData, 0);
	}

	/**
	 * Encode arbitrary {@code len} sequence, with an estimated encoded size.
	 * <p>Shorthand for:
	 * {@snippet :
	 * encodeLen(encodeData, Consumer::accept, estSize); // @link substring="encodeLen" target="encodeLen(Object, BiConsumer, int)"
	 * }
	 *
	 * @param encodeData function to encode sequence with
	 * @param estSize estimated size, in bytes, of encoded sequence
	 * @return {@code this}
	 * @throws IllegalArgumentException {@code estSize} is negative
	 * @throws IllegalStateException there was insufficient capacity to encode {@code src} and
	 * allocating additional capacity is not possible
	 * @since 1.2
	 */
	public ProtobufEncoder encodeLen(Consumer<ProtobufEncoder> encodeData, int estSize) {
		return this.encodeLen(encodeData, Consumer::accept, estSize);
	}

	/**
	 * Encode arbitrary {@code len} sequence.
	 * <p>Shorthand for:
	 * {@snippet :
	 * encodeLen(encodeData, 0); // @link substring="encodeLen" target="encodeLen(Consumer, int)"
	 * }
	 *
	 * @param encodeData function to encode sequence with
	 * @return {@code this}
	 * @throws IllegalStateException there was insufficient capacity to encode {@code src} and
	 * allocating additional capacity is not possible
	 * @since 1.2
	 */
	public ProtobufEncoder encodeLen(Consumer<ProtobufEncoder> encodeData) {
		return this.encodeLen(encodeData, 0);
	}

	/**
	 * Encode arbitrary {@code len} sequence field, with an estimated encoded size.
	 * <p>Shorthand for:
	 * {@snippet :
	 * encodeLenField(tag, encodeData, Consumer::accept, estSize); // @link substring="encodeLenField" target="encodeLenField(int, Object, BiConsumer, int)"
	 * }
	 *
	 * @param tag tag of field to encode
	 * @param encodeData function to encode sequence with
	 * @param estSize estimated size, in bytes, of encoded sequence
	 * @return {@code this}
	 * @throws IllegalArgumentException {@code estSize} is negative or wire type of {@code tag} is
	 * not {@link Protobuf#WIRE_LEN len}
	 * @throws IllegalStateException there was insufficient capacity to encode {@code src} and
	 * allocating additional capacity is not possible
	 * @since 1.2
	 */
	public ProtobufEncoder
	encodeLenField(@FieldTag int tag, Consumer<ProtobufEncoder> encodeData, int estSize) {
		return this.encodeLenField(tag, encodeData, Consumer::accept, estSize);
	}

	/**
	 * Encode arbitrary {@code len} sequence field, with an estimated encoded size.
	 * <p>Shorthand for:
	 * {@snippet :
	 * encodeLenField(tag, encodeData, 0); // @link substring="encodeLenField" target="encodeLenField(int, Consumer, int)"
	 * }
	 *
	 * @param tag tag of field to encode
	 * @param encodeData function to encode sequence with
	 * @return {@code this}
	 * @throws IllegalArgumentException wire type of {@code tag} is not {@link Protobuf#WIRE_LEN
	 * len}
	 * @throws IllegalStateException there was insufficient capacity to encode {@code src} and
	 * allocating additional capacity is not possible
	 * @since 1.2
	 */
	public ProtobufEncoder
	encodeLenField(@FieldTag int tag, Consumer<ProtobufEncoder> encodeData) {
		return this.encodeLenField(tag, encodeData, 0);
	}

	/**
	 * Encode message as {@code len} sequence.
	 * <p>Shorthand for:
	 * {@snippet :
	 * encodeLen(msg, ProtobufSerializable::toProtobuf); // @link substring="encodeLen" target="#encodeLen(Object, BiConsumer)"
	 * }
	 *
	 * @param msg message to encode
	 * @return {@code this}
	 * @throws IllegalStateException there was insufficient capacity to encode {@code src} and
	 * allocating additional capacity is not possible
	 * @since 1.2
	 */
	public ProtobufEncoder encodeMessage(@Nullable ProtobufSerializable msg) {
		return this.encodeLen(msg, ProtobufSerializable::toProtobuf);
	}

	/**
	 * Encode {@code len} sequence field with message.
	 * <p>Shorthand for:
	 * {@snippet :
	 * encodeLenField(tag, msg, ProtobufSerializable::toProtobuf); // @link substring="encodeLenField" target="#encodeLenField(int, Object, BiConsumer)"
	 * }
	 *
	 * @param tag tag of field to encode
	 * @param msg message to encode
	 * @return {@code this}
	 * @throws IllegalArgumentException wire type of {@code tag} is not {@link Protobuf#WIRE_LEN
	 * len}
	 * @throws IllegalStateException there was insufficient capacity to encode {@code src} and
	 * allocating additional capacity is not possible
	 * @since 1.2
	 */
	public ProtobufEncoder
	encodeMessageField(@FieldTag int tag, @Nullable ProtobufSerializable msg) {
		return this.encodeLenField(tag, msg, ProtobufSerializable::toProtobuf);
	}

	/*
	 * Encode fixed `len` sequence with size `size`, returning the offset at which sequence bytes
	 * may be encoded within `content.array`.
	 */
	private static int encodeLenExactImpl(
		ProtobufEncoder schema, ProtobufEncoder content,
		int tag, int size
	) {
		Preconditions.checkArgument(size >= 0);

		if (size == 0) {
			encodeEmptyLenSchema(schema, content, tag);
			return content.arrayOffset;
		}

		ensureCapacityAndEncodeLenSchema(schema, content, tag, size, 0);

		int off = content.arrayOffset;

		content.arrayOffset += size;
		return off;
	}

	/**
	 * Reserve and encode {@code len} sequence with exact encoded size.
	 * <p>This encodes {@code size} as the size, in bytes, of the {@code len} sequence, reserves
	 * {@code size} bytes of capacity within the underlying content array, returning a buffer
	 * <i>viewing</i> the subsequence of the content array into which the {@code len} sequence may
	 * be encoded. The {@code len} sequence <i>must</i> be encoded into the returned buffer
	 * <i>before</i> a subsequent encode operation is performed on {@code this}.
	 *
	 * @param size size, in bytes, of sequence
	 * @return buffer into which sequence may be encoded
	 * @throws IllegalArgumentException {@code size} is negative
	 * @throws IllegalStateException there was insufficient capacity to encode {@code src} and
	 * allocating additional capacity is not possible
	 * @since 1.2
	 */
	public ByteBuffer encodeLenExact(int size) {
		int off = encodeLenExactImpl(this, this.contentEncoder, 0, size);

		return ByteBuffer.wrap(this.contentEncoder.array, off, size);
	}

	/**
	 * Reserve and encode {@code len} sequence field with exact encoded size.
	 * <p>Efficient equivalent of:
	 * {@snippet :
	 * if (size > 0) {
	 *     return encodeFieldTag(tag) // @link substring="encodeFieldTag" target="#encodeFieldTag(int)"
	 *         .encodeLenExact(size);
	 * }
	 * return ByteBuffer.allocate(0);
	 * }
	 *
	 * @param tag tag of field to encode
	 * @param size size, in bytes, of sequence
	 * @return buffer into which sequence may be encoded
	 * @throws IllegalArgumentException {@code size} is negative or wire type of {@code tag} is
	 * not {@link Protobuf#WIRE_LEN len}
	 * @throws IllegalStateException there was insufficient capacity to encode {@code src} and
	 * allocating additional capacity is not possible
	 * @since 1.2
	 */
	public ByteBuffer encodeLenExactField(@FieldTag int tag, int size) {
		checkFieldWireType(tag, WIRE_LEN);

		int off = encodeLenExactImpl(this, this.contentEncoder, tag, size);

		return ByteBuffer.wrap(this.contentEncoder.array, off, size);
	}

	// Encode subsequence of `boolean` array as packed `varint` sequence.
	private static void encodePackedBoolArrayImpl(
		ProtobufEncoder schema, ProtobufEncoder content,
		int tag,
		@Nullable boolean[] src, int off, int len
	) {
		Preconditions.checkFromIndexSize(off, len, src == null ? 0 : src.length);

		int dstOff = encodeLenExactImpl(schema, content, tag, len);
		byte[] dst = content.array;

		for (int i = 0; i < len; i++) {
			//noinspection DataFlowIssue
			byte a = (byte) (src[off + i] ? 1 : 0);

			dst[dstOff + i] = a;
		}
	}

	/**
	 * Encode subsequence of {@code boolean} array as {@code len} sequence of packed {@code varint}.
	 * <p>Efficient equivalent of:
	 * {@snippet :
	 * encodeLen(enc -> {
	 *     for (int i = 0; i < len; i++)
	 *         enc.encodeBool(src[off + i]); // @link substring="encodeBool" target="#encodeBool(boolean)"
	 * });
	 * }
	 *
	 * @param src array to encode subsequence of
	 * @param off offset, within {@code src}, to begin encoding from
	 * @param len number of values to encode
	 * @return {@code this}
	 * @throws IndexOutOfBoundsException {@code off} or {@code len} is negative, {@code src} is
	 * {@code null} and {@code len} is non-zero, or {@code src} is non-{@code null} and {@code
	 * off + len} is greater than {@code src.length}
	 * @throws IllegalStateException there was insufficient capacity to encode {@code src} and
	 * allocating additional capacity is not possible
	 * @since 1.2
	 */
	public ProtobufEncoder encodePackedBoolArray(@Nullable boolean[] src, int off, int len) {
		encodePackedBoolArrayImpl(this, this.contentEncoder, 0, src, off, len);
		return this;
	}

	/**
	 * Encode {@code boolean} array as {@code len} sequence of packed {@code varint}.
	 * <p>Shorthand for:
	 * {@snippet :
	 * encodePackedBoolArray(src, 0, src == null ? 0 : src.length); // @link substring="encodePackedBoolArray" target="#encodePackedBoolArray(boolean[], int, int)"
	 * }
	 *
	 * @param src array to encode subsequence of
	 * @return {@code this}
	 * @throws IllegalStateException there was insufficient capacity to encode {@code src} and
	 * allocating additional capacity is not possible
	 * @since 1.2
	 */
	public ProtobufEncoder encodePackedBoolArray(@Nullable boolean[] src) {
		return this.encodePackedBoolArray(src, 0, src == null ? 0 : src.length);
	}

	/**
	 * Encode {@code len} sequence field with subsequence of {@code boolean} array packed into
	 * {@code varint} sequence.
	 * <p>Efficient equivalent of:
	 * {@snippet :
	 * if (src != null && len > 0) {
	 *     encodeFieldTag(tag) // @link substring="encodeFieldTag" target="#encodeFieldTag(int)"
	 *         .encodePackedBoolArray(src, off, len); // @link substring="encodePackedBoolArray" target="#encodePackedBoolArray(boolean[], int, int)"
	 * }
	 * }
	 *
	 * @param tag tag of field to encode
	 * @param src array to encode subsequence of
	 * @param off offset, within {@code src}, to begin encoding from
	 * @param len number of values to encode
	 * @return {@code this}
	 * @throws IllegalArgumentException wire type of {@code tag} is not {@link Protobuf#WIRE_LEN
	 * len}
	 * @throws IndexOutOfBoundsException {@code off} or {@code len} is negative, {@code src} is
	 * {@code null} and {@code len} is non-zero, or {@code src} is non-{@code null} and {@code
	 * off + len} is greater than {@code src.length}
	 * @throws IllegalStateException there was insufficient capacity to encode {@code src} and
	 * allocating additional capacity is not possible
	 * @since 1.2
	 */
	public ProtobufEncoder
	encodePackedBoolArrayField(@FieldTag int tag, @Nullable boolean[] src, int off, int len) {
		checkFieldWireType(tag, WIRE_LEN);
		encodePackedBoolArrayImpl(this, this.contentEncoder, tag, src, off, len);
		return this;
	}

	/**
	 * Encode {@code len} sequence field with {@code boolean} array packed into {@code varint}
	 * sequence.
	 * <p>Shorthand for:
	 * {@snippet :
	 * encodePackedBoolArrayField(tag, src, off, len); // @link substring="encodePackedBoolArrayField" target="#encodePackedBoolArrayField(int, boolean[], int, int)"
	 * }
	 *
	 * @param tag tag of field to encode
	 * @param src array to encode subsequence of
	 * @return {@code this}
	 * @throws IllegalArgumentException wire type of {@code tag} is not {@link Protobuf#WIRE_LEN
	 * len}
	 * @throws IllegalStateException there was insufficient capacity to encode {@code src} and
	 * allocating additional capacity is not possible
	 * @since 1.2
	 */
	public ProtobufEncoder encodePackedBoolArrayField(@FieldTag int tag, @Nullable boolean[] src) {
		return this.encodePackedBoolArrayField(tag, src, 0, src == null ? 0 : src.length);
	}

	// Encode subsequence of `int` array as packed `varint` sequence.
	private static void encodePackedInt32ArrayImpl(
		ProtobufEncoder schema, ProtobufEncoder content,
		int tag,
		@Nullable int[] src, int off, int len, boolean signed
	) {
		Preconditions.checkFromIndexSize(off, len, src == null ? 0 : src.length);

		if (len == 0) {
			encodeEmptyLenSchema(schema, content, tag);
			return;
		}

		long seqLen = 0;
		// we'll use 4 bits per varint size, since max size of a varint32 could be `5`
		byte[] srcSizeMap = new byte[((len * 4) + 8 - 1) / 8];

		for (int i = 0; i < (len / 2); i++) {
			//noinspection DataFlowIssue
			int a = src[off + i * 2 + 0];
			int b = src[off + i * 2 + 1];

			if (signed) {
				a = zigzagOf32(a);
				b = zigzagOf32(b);
			}

			int an = sizeOfVarint32(a);
			int bn = sizeOfVarint32(b);

			seqLen += an + bn;
			srcSizeMap[i] = (byte) (an | (bn << 4));
		}
		if ((len % 2) != 0) {
			int i = len / 2;
			//noinspection DataFlowIssue
			int a = src[off + (len - 1)];
			int n = sizeOfVarint32(signed ? zigzagOf32(a) : a);

			seqLen += n;
			srcSizeMap[i] = (byte) n;
		}

		Preconditions.checkState((seqLen & 0xffffffffL) == seqLen);

		int startContentOff = encodeLenExactImpl(schema, content, tag, (int) seqLen);
		int endContentOff = content.arrayOffset;

		content.arrayOffset = startContentOff;
		for (int i = 0; i < len; i++) {
			//noinspection DataFlowIssue
			int a = src[off + i];
			int n = ((srcSizeMap[i / 2] & 0xff) >>> ((i % 2) * 4)) & 0x0f;

			if (signed)
				a = zigzagOf32(a);
			encodeVarint32Impl(content, a, n);
		}
		Preconditions.checkState(content.arrayOffset == endContentOff);
	}

	/**
	 * Encode subsequence of unsigned {@code int} array as {@code len} sequence of packed {@code
	 * varint}.
	 * <p>Efficient equivalent of:
	 * {@snippet :
	 * encodeLen(enc -> {
	 *     for (int i = 0; i < len; i++)
	 *         enc.encodeUint32(src[off + i]); // @link substring="encodeUint32" target="#encodeUint32(int)"
	 * });
	 * }
	 *
	 * @param src array to encode subsequence of
	 * @param off offset, within {@code src}, to begin encoding from
	 * @param len number of values to encode
	 * @return {@code this}
	 * @throws IndexOutOfBoundsException {@code off} or {@code len} is negative, {@code src} is
	 * {@code null} and {@code len} is non-zero, or {@code src} is non-{@code null} and {@code
	 * off + len} is greater than {@code src.length}
	 * @throws IllegalStateException there was insufficient capacity to encode {@code src} and
	 * allocating additional capacity is not possible
	 * @since 1.2
	 */
	public ProtobufEncoder encodePackedUint32Array(@Nullable int[] src, int off, int len) {
		encodePackedInt32ArrayImpl(this, this.contentEncoder, 0, src, off, len, false);
		return this;
	}

	/**
	 * Encode unsigned {@code int} array as {@code len} sequence of packed {@code varint}.
	 * <p>Shorthand for:
	 * {@snippet :
	 * encodePackedUint32Array(src, 0, src == null ? 0 : src.length); // @link substring="encodePackedUint32Array" target="#encodePackedUint32Array(int[], int, int)"
	 * }
	 *
	 * @param src array to encode subsequence of
	 * @return {@code this}
	 * @throws IllegalStateException there was insufficient capacity to encode {@code src} and
	 * allocating additional capacity is not possible
	 * @since 1.2
	 */
	public ProtobufEncoder encodePackedUint32Array(@Nullable int[] src) {
		return this.encodePackedUint32Array(src, 0, src == null ? 0 : src.length);
	}

	/**
	 * Encode {@code len} sequence field with subsequence of unsigned {@code int} array packed into
	 * {@code varint} sequence.
	 * <p>Efficient equivalent of:
	 * {@snippet :
	 * if (src != null && len > 0) {
	 *     encodeFieldTag(tag) // @link substring="encodeFieldTag" target="#encodeFieldTag(int)"
	 *         .encodePackedUint32Array(src, off, len); // @link substring="encodePackedUint32Array" target="#encodePackedUint32Array(int[], int, int)"
	 * }
	 * }
	 *
	 * @param tag tag of field to encode
	 * @param src array to encode subsequence of
	 * @param off offset, within {@code src}, to begin encoding from
	 * @param len number of values to encode
	 * @return {@code this}
	 * @throws IllegalArgumentException wire type of {@code tag} is not {@link Protobuf#WIRE_LEN
	 * len}
	 * @throws IndexOutOfBoundsException {@code off} or {@code len} is negative, {@code src} is
	 * {@code null} and {@code len} is non-zero, or {@code src} is non-{@code null} and {@code
	 * off + len} is greater than {@code src.length}
	 * @throws IllegalStateException there was insufficient capacity to encode {@code src} and
	 * allocating additional capacity is not possible
	 * @since 1.2
	 */
	public ProtobufEncoder
	encodePackedUint32ArrayField(@FieldTag int tag, @Nullable int[] src, int off, int len) {
		checkFieldWireType(tag, WIRE_LEN);
		encodePackedInt32ArrayImpl(this, this.contentEncoder, tag, src, off, len, false);
		return this;
	}

	/**
	 * Encode {@code len} sequence field with unsigned {@code int} array packed into {@code varint}
	 * sequence.
	 * <p>Shorthand for:
	 * {@snippet :
	 * encodePackedUint32ArrayField(tag, src, off, len); // @link substring="encodePackedUint32ArrayField" target="#encodePackedUint32ArrayField(int, int[], int, int)"
	 * }
	 *
	 * @param tag tag of field to encode
	 * @param src array to encode subsequence of
	 * @return {@code this}
	 * @throws IllegalArgumentException wire type of {@code tag} is not {@link Protobuf#WIRE_LEN
	 * len}
	 * @throws IllegalStateException there was insufficient capacity to encode {@code src} and
	 * allocating additional capacity is not possible
	 * @since 1.2
	 */
	public ProtobufEncoder encodePackedUint32ArrayField(@FieldTag int tag, @Nullable int[] src) {
		return this.encodePackedUint32ArrayField(tag, src, 0, src == null ? 0 : src.length);
	}

	/**
	 * Encode subsequence of signed {@code int} array as {@code len} sequence of packed {@code
	 * varint}.
	 * <p>Efficient equivalent of:
	 * {@snippet :
	 * encodeLen(enc -> {
	 *     for (int i = 0; i < len; i++)
	 *         enc.encodeSint32(src[off + i]); // @link substring="encodeSint32" target="#encodeSint32(int)"
	 * });
	 * }
	 *
	 * @param src array to encode subsequence of
	 * @param off offset, within {@code src}, to begin encoding from
	 * @param len number of values to encode
	 * @return {@code this}
	 * @throws IndexOutOfBoundsException {@code off} or {@code len} is negative, {@code src} is
	 * {@code null} and {@code len} is non-zero, or {@code src} is non-{@code null} and {@code
	 * off + len} is greater than {@code src.length}
	 * @throws IllegalStateException there was insufficient capacity to encode {@code src} and
	 * allocating additional capacity is not possible
	 * @since 1.2
	 */
	public ProtobufEncoder encodePackedSint32Array(@Nullable int[] src, int off, int len) {
		encodePackedInt32ArrayImpl(this, this.contentEncoder, 0, src, off, len, true);
		return this;
	}

	/**
	 * Encode signed {@code int} array as {@code len} sequence of packed {@code varint}.
	 * <p>Shorthand for:
	 * {@snippet :
	 * encodePackedSint32Array(src, 0, src == null ? 0 : src.length); // @link substring="encodePackedSint32Array" target="#encodePackedSint32Array(int[], int, int)"
	 * }
	 *
	 * @param src array to encode subsequence of
	 * @return {@code this}
	 * @throws IllegalStateException there was insufficient capacity to encode {@code src} and
	 * allocating additional capacity is not possible
	 * @since 1.2
	 */
	public ProtobufEncoder encodePackedSint32Array(@Nullable int[] src) {
		return this.encodePackedSint32Array(src, 0, src == null ? 0 : src.length);
	}

	/**
	 * Encode {@code len} sequence field with subsequence of signed {@code int} array packed into
	 * {@code varint} sequence.
	 * <p>Efficient equivalent of:
	 * {@snippet :
	 * if (src != null && len > 0) {
	 *     encodeFieldTag(tag) // @link substring="encodeFieldTag" target="#encodeFieldTag(int)"
	 *         .encodePackedSint32Array(src, off, len); // @link substring="encodePackedSint32Array" target="#encodePackedSint32Array(int[], int, int)"
	 * }
	 * }
	 *
	 * @param tag tag of field to encode
	 * @param src array to encode subsequence of
	 * @param off offset, within {@code src}, to begin encoding from
	 * @param len number of values to encode
	 * @return {@code this}
	 * @throws IllegalArgumentException wire type of {@code tag} is not {@link Protobuf#WIRE_LEN
	 * len}
	 * @throws IndexOutOfBoundsException {@code off} or {@code len} is negative, {@code src} is
	 * {@code null} and {@code len} is non-zero, or {@code src} is non-{@code null} and {@code
	 * off + len} is greater than {@code src.length}
	 * @throws IllegalStateException there was insufficient capacity to encode {@code src} and
	 * allocating additional capacity is not possible
	 * @since 1.2
	 */
	public ProtobufEncoder
	encodePackedSint32ArrayField(@FieldTag int tag, @Nullable int[] src, int off, int len) {
		checkFieldWireType(tag, WIRE_LEN);
		encodePackedInt32ArrayImpl(this, this.contentEncoder, tag, src, off, len, true);
		return this;
	}

	/**
	 * Encode {@code len} sequence field with signed {@code int} array packed into {@code varint}
	 * sequence.
	 * <p>Shorthand for:
	 * {@snippet :
	 * encodePackedSint32ArrayField(tag, src, off, len); // @link substring="encodePackedSint32ArrayField" target="#encodePackedSint32ArrayField(int, int[], int, int)"
	 * }
	 *
	 * @param tag tag of field to encode
	 * @param src array to encode subsequence of
	 * @return {@code this}
	 * @throws IllegalArgumentException wire type of {@code tag} is not {@link Protobuf#WIRE_LEN
	 * len}
	 * @throws IllegalStateException there was insufficient capacity to encode {@code src} and
	 * allocating additional capacity is not possible
	 * @since 1.2
	 */
	public ProtobufEncoder encodePackedSint32ArrayField(@FieldTag int tag, @Nullable int[] src) {
		return this.encodePackedSint32ArrayField(tag, src, 0, src == null ? 0 : src.length);
	}

	// Encode subsequence of `long` array as packed `varint` sequence.
	private static void encodePackedInt64ArrayImpl(
		ProtobufEncoder schema, ProtobufEncoder content,
		int tag,
		@Nullable long[] src, int off, int len, boolean signed
	) {
		Preconditions.checkFromIndexSize(off, len, src == null ? 0 : src.length);

		if (len == 0) {
			encodeEmptyLenSchema(schema, content, tag);
			return;
		}

		long seqLen = 0;
		// we'll use 4 bits per varint size, since max size of a varint64 could be `10`
		byte[] srcSizeMap = new byte[((len * 4) + 8 - 1) / 8];

		for (int i = 0; i < (len / 2); i++) {
			//noinspection DataFlowIssue
			long a = src[off + i * 2 + 0];
			long b = src[off + i * 2 + 1];

			if (signed) {
				a = zigzagOf64(a);
				b = zigzagOf64(b);
			}

			int an = sizeOfVarint64(a);
			int bn = sizeOfVarint64(b);

			seqLen += an + bn;
			srcSizeMap[i] = (byte) (an | (bn << 4));
		}
		if ((len % 2) != 0) {
			int i = len / 2;
			//noinspection DataFlowIssue
			long a = src[off + (len - 1)];
			int n = sizeOfVarint64(signed ? zigzagOf64(a) : a);

			seqLen += n;
			srcSizeMap[i] = (byte) n;
		}

		Preconditions.checkState((seqLen & 0xffffffffL) == seqLen);

		int startContentOff = encodeLenExactImpl(schema, content, tag, (int) seqLen);
		int endContentOff = content.arrayOffset;

		content.arrayOffset = startContentOff;
		for (int i = 0; i < len; i++) {
			//noinspection DataFlowIssue
			long a = src[off + i];
			int n = ((srcSizeMap[i / 2] & 0xff) >>> ((i % 2) * 4)) & 0x0f;

			if (signed)
				a = zigzagOf64(a);
			encodeVarint64Impl(content, a, n);
		}
		Preconditions.checkState(content.arrayOffset == endContentOff);
	}

	/**
	 * Encode subsequence of unsigned {@code long} array as {@code len} sequence of packed {@code
	 * varint}.
	 * <p>Efficient equivalent of:
	 * {@snippet :
	 * encodeLen(enc -> {
	 *     for (int i = 0; i < len; i++)
	 *         enc.encodeUint64(src[off + i]); // @link substring="encodeUint64" target="#encodeUint64(long)"
	 * });
	 * }
	 *
	 * @param src array to encode subsequence of
	 * @param off offset, within {@code src}, to begin encoding from
	 * @param len number of values to encode
	 * @return {@code this}
	 * @throws IndexOutOfBoundsException {@code off} or {@code len} is negative, {@code src} is
	 * {@code null} and {@code len} is non-zero, or {@code src} is non-{@code null} and {@code
	 * off + len} is greater than {@code src.length}
	 * @throws IllegalStateException there was insufficient capacity to encode {@code src} and
	 * allocating additional capacity is not possible
	 * @since 1.2
	 */
	public ProtobufEncoder encodePackedUint64Array(@Nullable long[] src, int off, int len) {
		encodePackedInt64ArrayImpl(this, this.contentEncoder, 0, src, off, len, false);
		return this;
	}

	/**
	 * Encode unsigned {@code long} array as {@code len} sequence of packed {@code varint}.
	 * <p>Shorthand for:
	 * {@snippet :
	 * encodePackedUint64Array(src, 0, src == null ? 0 : src.length); // @link substring="encodePackedUint64Array" target="#encodePackedUint64Array(long[], int, int)"
	 * }
	 *
	 * @param src array to encode subsequence of
	 * @return {@code this}
	 * @throws IllegalStateException there was insufficient capacity to encode {@code src} and
	 * allocating additional capacity is not possible
	 * @since 1.2
	 */
	public ProtobufEncoder encodePackedUint64Array(@Nullable long[] src) {
		return this.encodePackedUint64Array(src, 0, src == null ? 0 : src.length);
	}

	/**
	 * Encode {@code len} sequence field with subsequence of unsigned {@code long} array packed into
	 * {@code varint} sequence.
	 * <p>Efficient equivalent of:
	 * {@snippet :
	 * if (src != null && len > 0) {
	 *     encodeFieldTag(tag) // @link substring="encodeFieldTag" target="#encodeFieldTag(int)"
	 *         .encodePackedUint64Array(src, off, len); // @link substring="encodePackedUint64Array" target="#encodePackedUint64Array(long[], int, int)"
	 * }
	 * }
	 *
	 * @param tag tag of field to encode
	 * @param src array to encode subsequence of
	 * @param off offset, within {@code src}, to begin encoding from
	 * @param len number of values to encode
	 * @return {@code this}
	 * @throws IllegalArgumentException wire type of {@code tag} is not {@link Protobuf#WIRE_LEN
	 * len}
	 * @throws IndexOutOfBoundsException {@code off} or {@code len} is negative, {@code src} is
	 * {@code null} and {@code len} is non-zero, or {@code src} is non-{@code null} and {@code
	 * off + len} is greater than {@code src.length}
	 * @throws IllegalStateException there was insufficient capacity to encode {@code src} and
	 * allocating additional capacity is not possible
	 * @since 1.2
	 */
	public ProtobufEncoder
	encodePackedUint64ArrayField(@FieldTag int tag, @Nullable long[] src, int off, int len) {
		checkFieldWireType(tag, WIRE_LEN);
		encodePackedInt64ArrayImpl(this, this.contentEncoder, tag, src, off, len, false);
		return this;
	}

	/**
	 * Encode {@code len} sequence field with unsigned {@code long} array packed into {@code varint}
	 * sequence.
	 * <p>Shorthand for:
	 * {@snippet :
	 * encodePackedUint64ArrayField(tag, src, off, len); // @link substring="encodePackedUint64ArrayField" target="#encodePackedUint64ArrayField(int, long[], int, int)"
	 * }
	 *
	 * @param tag tag of field to encode
	 * @param src array to encode subsequence of
	 * @return {@code this}
	 * @throws IllegalArgumentException wire type of {@code tag} is not {@link Protobuf#WIRE_LEN
	 * len}
	 * @throws IllegalStateException there was insufficient capacity to encode {@code src} and
	 * allocating additional capacity is not possible
	 * @since 1.2
	 */
	public ProtobufEncoder encodePackedUint64ArrayField(@FieldTag int tag, @Nullable long[] src) {
		return this.encodePackedUint64ArrayField(tag, src, 0, src == null ? 0 : src.length);
	}

	/**
	 * Encode subsequence of signed {@code long} array as {@code len} sequence of packed {@code
	 * varint}.
	 * <p>Efficient equivalent of:
	 * {@snippet :
	 * encodeLen(enc -> {
	 *     for (int i = 0; i < len; i++)
	 *         enc.encodeSint64(src[off + i]); // @link substring="encodeSint64" target="#encodeSint64(long)"
	 * });
	 * }
	 *
	 * @param src array to encode subsequence of
	 * @param off offset, within {@code src}, to begin encoding from
	 * @param len number of values to encode
	 * @return {@code this}
	 * @throws IndexOutOfBoundsException {@code off} or {@code len} is negative, {@code src} is
	 * {@code null} and {@code len} is non-zero, or {@code src} is non-{@code null} and {@code
	 * off + len} is greater than {@code src.length}
	 * @throws IllegalStateException there was insufficient capacity to encode {@code src} and
	 * allocating additional capacity is not possible
	 * @since 1.2
	 */
	public ProtobufEncoder encodePackedSint64Array(@Nullable long[] src, int off, int len) {
		encodePackedInt64ArrayImpl(this, this.contentEncoder, 0, src, off, len, true);
		return this;
	}

	/**
	 * Encode signed {@code long} array as {@code len} sequence of packed {@code varint}.
	 * <p>Shorthand for:
	 * {@snippet :
	 * encodePackedSint64Array(src, 0, src == null ? 0 : src.length); // @link substring="encodePackedSint64Array" target="#encodePackedSint64Array(long[], int, int)"
	 * }
	 *
	 * @param src array to encode subsequence of
	 * @return {@code this}
	 * @throws IllegalStateException there was insufficient capacity to encode {@code src} and
	 * allocating additional capacity is not possible
	 * @since 1.2
	 */
	public ProtobufEncoder encodePackedSint64Array(@Nullable long[] src) {
		return this.encodePackedSint64Array(src, 0, src == null ? 0 : src.length);
	}

	/**
	 * Encode {@code len} sequence field with subsequence of signed {@code long} array packed into
	 * {@code varint} sequence.
	 * <p>Efficient equivalent of:
	 * {@snippet :
	 * if (src != null && len > 0) {
	 *     encodeFieldTag(tag) // @link substring="encodeFieldTag" target="#encodeFieldTag(int)"
	 *         .encodePackedSint64Array(src, off, len); // @link substring="encodePackedSint64Array" target="#encodePackedSint64Array(long[], int, int)"
	 * }
	 * }
	 *
	 * @param tag tag of field to encode
	 * @param src array to encode subsequence of
	 * @param off offset, within {@code src}, to begin encoding from
	 * @param len number of values to encode
	 * @return {@code this}
	 * @throws IllegalArgumentException wire type of {@code tag} is not {@link Protobuf#WIRE_LEN
	 * len}
	 * @throws IndexOutOfBoundsException {@code off} or {@code len} is negative, {@code src} is
	 * {@code null} and {@code len} is non-zero, or {@code src} is non-{@code null} and {@code
	 * off + len} is greater than {@code src.length}
	 * @throws IllegalStateException there was insufficient capacity to encode {@code src} and
	 * allocating additional capacity is not possible
	 * @since 1.2
	 */
	public ProtobufEncoder
	encodePackedSint64ArrayField(@FieldTag int tag, @Nullable long[] src, int off, int len) {
		checkFieldWireType(tag, WIRE_LEN);
		encodePackedInt64ArrayImpl(this, this.contentEncoder, tag, src, off, len, true);
		return this;
	}

	/**
	 * Encode {@code len} sequence field with signed {@code long} array packed into {@code varint}
	 * sequence.
	 * <p>Shorthand for:
	 * {@snippet :
	 * encodePackedSint64ArrayField(tag, src, off, len); // @link substring="encodePackedSint64ArrayField" target="#encodePackedSint64ArrayField(int, long[], int, int)"
	 * }
	 *
	 * @param tag tag of field to encode
	 * @param src array to encode subsequence of
	 * @return {@code this}
	 * @throws IllegalArgumentException wire type of {@code tag} is not {@link Protobuf#WIRE_LEN
	 * len}
	 * @throws IllegalStateException there was insufficient capacity to encode {@code src} and
	 * allocating additional capacity is not possible
	 * @since 1.2
	 */
	public ProtobufEncoder encodePackedSint64ArrayField(@FieldTag int tag, @Nullable long[] src) {
		return this.encodePackedSint64ArrayField(tag, src, 0, src == null ? 0 : src.length);
	}

	// Encode bits of a 64-bit bitmap as packed `varint` sequence.
	private static void encodePackedUint32Bitmap64Impl(
		ProtobufEncoder schema, ProtobufEncoder content,
		int tag, long bitmap, int delta
	) {
		if (bitmap == 0L) {
			encodeEmptyLenSchema(schema, content, tag);
			return;
		}

		// low 4 bits contain varint size, high bits contain bit position
		short[] seqAndSize = new short[Long.bitCount(bitmap)];
		int seqLen = 0;

		for (
			int i = 0, j = Bits.firstSetBitOf(bitmap);
			j < Bits.SIZE_OF_LONG;
			i++, j = Bits.nextSetBitOf(bitmap, j + 1)
		) {
			int n = sizeOfVarint32(delta + j);

			seqLen += n;
			seqAndSize[i] = (short) (n | (j << 4));
		}

		int startContentOff = encodeLenExactImpl(schema, content, tag, seqLen);
		int endContentOff = content.arrayOffset;

		content.arrayOffset = startContentOff;
		for (short ss : seqAndSize)
			encodeVarint32Impl(content, delta + ((ss & 0xffff) >>> 4), (ss & 0x0f));
		Preconditions.checkState(content.arrayOffset == endContentOff);
	}

	/**
	 * Encode bits, with unsigned 32-bit delta, set within 64-bit bitmap as {@code len} sequence
	 * of packed {@code varint}.
	 * <p>Efficient equivalent of:
	 * {@snippet :
	 * int[] seq = new int[64];
	 * int seqLen = 0;
	 *
	 * for (int i = 0; i < 64; i++) {
	 *     if ((bitmap & (1L << i)) != 0L)
	 *         seq[seqLen++] = delta + i;
	 * }
	 * encodePackedUint32Array(seq, 0, seqLen); // @link substring="encodePackedUint32Array" target="#encodePackedUint32Array(int[], int, int)"
	 * }
	 *
	 * @param bitmap bitmap to encode
	 * @param delta delta to add to each set bit position
	 * @return {@code this}
	 * @throws IllegalStateException there was insufficient capacity to encode {@code bitmap} and
	 * allocating additional capacity is not possible
	 * @since 1.2
	 */
	public ProtobufEncoder encodePackedUint32Bitmap64(long bitmap, int delta) {
		encodePackedUint32Bitmap64Impl(this, this.contentEncoder, 0, bitmap, delta);
		return this;
	}

	/**
	 * Encode bits set within 64-bit bitmap as {@code len} sequence of packed {@code varint}.
	 * <p>Shorthand for:
	 * {@snippet :
	 * encodePackedUint32Bitmap64(bitmap, 0); // @link substring="encodePackedUint32Bitmap64" target="#encodePackedUint32Bitmap64(long, int)"
	 * }
	 *
	 * @param bitmap bitmap to encode
	 * @return {@code this}
	 * @throws IllegalStateException there was insufficient capacity to encode {@code bitmap} and
	 * allocating additional capacity is not possible
	 * @since 1.2
	 */
	public ProtobufEncoder encodePackedUint32Bitmap64(long bitmap) {
		return this.encodePackedUint32Bitmap64(bitmap, 0);
	}

	/**
	 * Encode bits, with unsigned 32-bit delta, set within 32-bit bitmap as {@code len} sequence
	 * of packed {@code varint}.
	 * <p>Efficient equivalent of:
	 * {@snippet :
	 * int[] seq = new int[32];
	 * int seqLen = 0;
	 *
	 * for (int i = 0; i < 32; i++) {
	 *     if ((bitmap & (1 << i)) != 0)
	 *         seq[seqLen++] = delta + i;
	 * }
	 * encodePackedUint32Array(seq, 0, seqLen); // @link substring="encodePackedUint32Array" target="#encodePackedUint32Array(int[], int, int)"
	 * }
	 *
	 * @param bitmap bitmap to encode
	 * @param delta delta to add to each set bit position
	 * @return {@code this}
	 * @throws IllegalStateException there was insufficient capacity to encode {@code bitmap} and
	 * allocating additional capacity is not possible
	 * @since 1.2
	 */
	public ProtobufEncoder encodePackedUint32Bitmap32(int bitmap, int delta) {
		return this.encodePackedUint32Bitmap64(bitmap & 0xffffffffL, delta);
	}

	/**
	 * Encode bits set within 32-bit bitmap as {@code len} sequence of packed {@code varint}.
	 * <p>Shorthand for:
	 * {@snippet :
	 * encodePackedUint32Bitmap32(bitmap, 0); // @link substring="encodePackedUint32Bitmap32" target="#encodePackedUint32Bitmap32(int, int)"
	 * }
	 *
	 * @param bitmap bitmap to encode
	 * @return {@code this}
	 * @throws IllegalStateException there was insufficient capacity to encode {@code bitmap} and
	 * allocating additional capacity is not possible
	 * @since 1.2
	 */
	public ProtobufEncoder encodePackedUint32Bitmap32(int bitmap) {
		return this.encodePackedUint32Bitmap32(bitmap, 0);
	}

	/**
	 * Encode {@code len} sequence field with bits, with unsigned 32-bit delta, set within 64-bit
	 * bitmap packed into {@code varint} sequence.
	 * <p>Efficient equivalent of:
	 * {@snippet :
	 * if (bitmap != 0L) {
	 *     encodeFieldTag(tag) // @link substring="encodeFieldTag" target="#encodeFieldTag(int)"
	 *         .encodePackedUint32Bitmap64(bitmap, delta); // @link substring="encodePackedUint32Bitmap64" target="#encodePackedUint32Bitmap64(long, int)"
	 * }
	 * }
	 *
	 * @param tag tag of field to encode
	 * @param bitmap bitmap to encode
	 * @param delta delta to add to each set bit position
	 * @return {@code this}
	 * @throws IllegalArgumentException wire type of {@code tag} is not {@link Protobuf#WIRE_LEN
	 * len}
	 * @throws IllegalStateException there was insufficient capacity to encode {@code bitmap} and
	 * allocating additional capacity is not possible
	 * @since 1.2
	 */
	public ProtobufEncoder
	encodePackedUint32Bitmap64Field(@FieldTag int tag, long bitmap, int delta) {
		checkFieldWireType(tag, WIRE_LEN);
		encodePackedUint32Bitmap64Impl(this, this.contentEncoder, tag, bitmap, delta);
		return this;
	}

	/**
	 * Encode {@code len} sequence field with bits set within 64-bit bitmap packed into {@code
	 * varint} sequence.
	 * <p>Shorthand for:
	 * {@snippet :
	 * encodePackedUint32Bitmap64Field(tag, bitmap, 0); // @link substring="encodePackedUint32Bitmap64Field" target="#encodePackedUint32Bitmap64Field(int, long, int)"
	 * }
	 *
	 * @param tag tag of field to encode
	 * @param bitmap bitmap to encode
	 * @return {@code this}
	 * @throws IllegalArgumentException wire type of {@code tag} is not {@link Protobuf#WIRE_LEN
	 * len}
	 * @throws IllegalStateException there was insufficient capacity to encode {@code bitmap} and
	 * allocating additional capacity is not possible
	 * @since 1.2
	 */
	public ProtobufEncoder encodePackedUint32Bitmap64Field(@FieldTag int tag, long bitmap) {
		return this.encodePackedUint32Bitmap64Field(tag, bitmap, 0);
	}
	/**
	 * Encode {@code len} sequence field with bits, with unsigned 32-bit delta, set within 32-bit
	 * bitmap packed into {@code varint} sequence.
	 * <p>Efficient equivalent of:
	 * {@snippet :
	 * if (bitmap != 0L) {
	 *     encodeFieldTag(tag) // @link substring="encodeFieldTag" target="#encodeFieldTag(int)"
	 *         .encodePackedUint32Bitmap32(bitmap, delta); // @link substring="encodePackedUint32Bitmap32" target="#encodePackedUint32Bitmap32(int, int)"
	 * }
	 * }
	 *
	 * @param tag tag of field to encode
	 * @param bitmap bitmap to encode
	 * @param delta delta to add to each set bit position
	 * @return {@code this}
	 * @throws IllegalArgumentException wire type of {@code tag} is not {@link Protobuf#WIRE_LEN
	 * len}
	 * @throws IllegalStateException there was insufficient capacity to encode {@code bitmap} and
	 * allocating additional capacity is not possible
	 * @since 1.2
	 */
	public ProtobufEncoder
	encodePackedUint32Bitmap32Field(@FieldTag int tag, int bitmap, int delta) {
		return this.encodePackedUint32Bitmap64Field(tag, bitmap & 0xffffffffL, delta);
	}

	/**
	 * Encode {@code len} sequence field with bits set within 64-bit bitmap packed into {@code
	 * varint} sequence.
	 * <p>Shorthand for:
	 * {@snippet :
	 * encodePackedUint32Bitmap32Field(tag, bitmap, 0); // @link substring="encodePackedUint32Bitmap32Field" target="#encodePackedUint32Bitmap32Field(int, int, int)"
	 * }
	 *
	 * @param tag tag of field to encode
	 * @param bitmap bitmap to encode
	 * @return {@code this}
	 * @throws IllegalArgumentException wire type of {@code tag} is not {@link Protobuf#WIRE_LEN
	 * len}
	 * @throws IllegalStateException there was insufficient capacity to encode {@code bitmap} and
	 * allocating additional capacity is not possible
	 * @since 1.2
	 */
	public ProtobufEncoder encodePackedUint32Bitmap32Field(@FieldTag int tag, int bitmap) {
		return this.encodePackedUint32Bitmap32Field(tag, bitmap, 0);
	}

	// Encode subsequence of `int` array as packed `fixed32` sequence.
	private static void encodePackedFixed32ArrayImpl(
		ProtobufEncoder schema, ProtobufEncoder content,
		int tag,
		@Nullable int[] src, int off, int len
	) {
		Preconditions.checkFromIndexSize(off, len, src == null ? 0 : src.length);

		int dstOff = encodeLenExactImpl(schema, content, tag, len * 4);
		byte[] dst = content.array;

		for (int i = 0; i < len; i++) {
			//noinspection DataFlowIssue
			int a = src[off + i];

			storeIntLe(dst, dstOff + i * 4, a);
		}
	}

	/**
	 * Encode subsequence of {@code int} array as {@code len} sequence of packed {@code fixed32}.
	 * <p>Efficient equivalent of:
	 * {@snippet :
	 * encodeLen(enc -> {
	 *     for (int i = 0; i < len; i++)
	 *         enc.encodeFixed32(src[off + i]); // @link substring="encodeFixed32" target="#encodeFixed32(int)"
	 * });
	 * }
	 *
	 * @param src array to encode subsequence of
	 * @param off offset, within {@code src}, to begin encoding from
	 * @param len number of values to encode
	 * @return {@code this}
	 * @throws IndexOutOfBoundsException {@code off} or {@code len} is negative, {@code src} is
	 * {@code null} and {@code len} is non-zero, or {@code src} is non-{@code null} and {@code
	 * off + len} is greater than {@code src.length}
	 * @throws IllegalStateException there was insufficient capacity to encode {@code src} and
	 * allocating additional capacity is not possible
	 * @since 1.2
	 */
	public ProtobufEncoder encodePackedFixed32Array(@Nullable int[] src, int off, int len) {
		encodePackedFixed32ArrayImpl(this, this.contentEncoder, 0, src, off, len);
		return this;
	}

	/**
	 * Encode {@code int} array as {@code len} sequence of packed {@code fixed32}.
	 * <p>Shorthand for:
	 * {@snippet :
	 * encodePackedFixed32Array(src, 0, src == null ? 0 : src.length); // @link substring="encodePackedFixed32Array" target="#encodePackedFixed32Array(int[], int, int)"
	 * }
	 *
	 * @param src array to encode subsequence of
	 * @return {@code this}
	 * @throws IllegalStateException there was insufficient capacity to encode {@code src} and
	 * allocating additional capacity is not possible
	 * @since 1.2
	 */
	public ProtobufEncoder encodePackedFixed32Array(@Nullable int[] src) {
		return this.encodePackedFixed32Array(src, 0, src == null ? 0 : src.length);
	}

	/**
	 * Encode {@code len} sequence field with subsequence of {@code int} array packed into
	 * {@code fixed32} sequence.
	 * <p>Efficient equivalent of:
	 * {@snippet :
	 * if (src != null && len > 0) {
	 *     encodeFieldTag(tag) // @link substring="encodeFieldTag" target="#encodeFieldTag(int)"
	 *         .encodePackedFixed32Array(src, off, len); // @link substring="encodePackedFixed32Array" target="#encodePackedFixed32Array(int[], int, int)"
	 * }
	 * }
	 *
	 * @param tag tag of field to encode
	 * @param src array to encode subsequence of
	 * @param off offset, within {@code src}, to begin encoding from
	 * @param len number of values to encode
	 * @return {@code this}
	 * @throws IllegalArgumentException wire type of {@code tag} is not {@link Protobuf#WIRE_LEN
	 * len}
	 * @throws IndexOutOfBoundsException {@code off} or {@code len} is negative, {@code src} is
	 * {@code null} and {@code len} is non-zero, or {@code src} is non-{@code null} and {@code
	 * off + len} is greater than {@code src.length}
	 * @throws IllegalStateException there was insufficient capacity to encode {@code src} and
	 * allocating additional capacity is not possible
	 * @since 1.2
	 */
	public ProtobufEncoder
	encodePackedFixed32ArrayField(@FieldTag int tag, @Nullable int[] src, int off, int len) {
		checkFieldWireType(tag, WIRE_LEN);
		encodePackedFixed32ArrayImpl(this, this.contentEncoder, tag, src, off, len);
		return this;
	}

	/**
	 * Encode {@code len} sequence field with {@code int} array packed into {@code fixed32}
	 * sequence.
	 * <p>Shorthand for:
	 * {@snippet :
	 * encodePackedFixed32ArrayField(tag, src, off, len); // @link substring="encodePackedFixed32ArrayField" target="#encodePackedFixed32ArrayField(int, int[], int, int)"
	 * }
	 *
	 * @param tag tag of field to encode
	 * @param src array to encode subsequence of
	 * @return {@code this}
	 * @throws IllegalArgumentException wire type of {@code tag} is not {@link Protobuf#WIRE_LEN
	 * len}
	 * @throws IllegalStateException there was insufficient capacity to encode {@code src} and
	 * allocating additional capacity is not possible
	 * @since 1.2
	 */
	public ProtobufEncoder encodePackedFixed32ArrayField(@FieldTag int tag, @Nullable int[] src) {
		return this.encodePackedFixed32ArrayField(tag, src, 0, src == null ? 0 : src.length);
	}

	// Encode subsequence of `long` array as packed `fixed64` sequence.
	private static void encodePackedFixed64ArrayImpl(
		ProtobufEncoder schema, ProtobufEncoder content,
		int tag,
		@Nullable long[] src, int off, int len
	) {
		Preconditions.checkFromIndexSize(off, len, src == null ? 0 : src.length);

		int dstOff = encodeLenExactImpl(schema, content, tag, len * 8);
		byte[] dst = content.array;

		for (int i = 0; i < len; i++) {
			//noinspection DataFlowIssue
			long a = src[off + i];

			storeLongLe(dst, dstOff + i * 8, a);
		}
	}

	/**
	 * Encode subsequence of {@code long} array as {@code len} sequence of packed {@code fixed64}.
	 * <p>Efficient equivalent of:
	 * {@snippet :
	 * encodeLen(enc -> {
	 *     for (int i = 0; i < len; i++)
	 *         enc.encodeFixed64(src[off + i]); // @link substring="encodeFixed64" target="#encodeFixed64(long)"
	 * });
	 * }
	 *
	 * @param src array to encode subsequence of
	 * @param off offset, within {@code src}, to begin encoding from
	 * @param len number of values to encode
	 * @return {@code this}
	 * @throws IndexOutOfBoundsException {@code off} or {@code len} is negative, {@code src} is
	 * {@code null} and {@code len} is non-zero, or {@code src} is non-{@code null} and {@code
	 * off + len} is greater than {@code src.length}
	 * @throws IllegalStateException there was insufficient capacity to encode {@code src} and
	 * allocating additional capacity is not possible
	 * @since 1.2
	 */
	public ProtobufEncoder encodePackedFixed64Array(@Nullable long[] src, int off, int len) {
		encodePackedFixed64ArrayImpl(this, this.contentEncoder, 0, src, off, len);
		return this;
	}

	/**
	 * Encode {@code long} array as {@code len} sequence of packed {@code fixed64}.
	 * <p>Shorthand for:
	 * {@snippet :
	 * encodePackedFixed64Array(src, 0, src == null ? 0 : src.length); // @link substring="encodePackedFixed64Array" target="#encodePackedFixed64Array(long[], int, int)"
	 * }
	 *
	 * @param src array to encode subsequence of
	 * @return {@code this}
	 * @throws IllegalStateException there was insufficient capacity to encode {@code src} and
	 * allocating additional capacity is not possible
	 * @since 1.2
	 */
	public ProtobufEncoder encodePackedFixed64Array(@Nullable long[] src) {
		return this.encodePackedFixed64Array(src, 0, src == null ? 0 : src.length);
	}

	/**
	 * Encode {@code len} sequence field with subsequence of {@code long} array packed into
	 * {@code fixed64} sequence.
	 * <p>Efficient equivalent of:
	 * {@snippet :
	 * if (src != null && len > 0) {
	 *     encodeFieldTag(tag) // @link substring="encodeFieldTag" target="#encodeFieldTag(int)"
	 *         .encodePackedFixed64Array(src, off, len); // @link substring="encodePackedFixed64Array" target="#encodePackedFixed64Array(long[], int, int)"
	 * }
	 * }
	 *
	 * @param tag tag of field to encode
	 * @param src array to encode subsequence of
	 * @param off offset, within {@code src}, to begin encoding from
	 * @param len number of values to encode
	 * @return {@code this}
	 * @throws IllegalArgumentException wire type of {@code tag} is not {@link Protobuf#WIRE_LEN
	 * len}
	 * @throws IndexOutOfBoundsException {@code off} or {@code len} is negative, {@code src} is
	 * {@code null} and {@code len} is non-zero, or {@code src} is non-{@code null} and {@code
	 * off + len} is greater than {@code src.length}
	 * @throws IllegalStateException there was insufficient capacity to encode {@code src} and
	 * allocating additional capacity is not possible
	 * @since 1.2
	 */
	public ProtobufEncoder
	encodePackedFixed64ArrayField(@FieldTag int tag, @Nullable long[] src, int off, int len) {
		checkFieldWireType(tag, WIRE_LEN);
		encodePackedFixed64ArrayImpl(this, this.contentEncoder, tag, src, off, len);
		return this;
	}

	/**
	 * Encode {@code len} sequence field with {@code long} array packed into {@code fixed64}
	 * sequence.
	 * <p>Shorthand for:
	 * {@snippet :
	 * encodePackedFixed64ArrayField(tag, src, off, len); // @link substring="encodePackedFixed64ArrayField" target="#encodePackedFixed64ArrayField(int, long[], int, int)"
	 * }
	 *
	 * @param tag tag of field to encode
	 * @param src array to encode subsequence of
	 * @return {@code this}
	 * @throws IllegalArgumentException wire type of {@code tag} is not {@link Protobuf#WIRE_LEN
	 * len}
	 * @throws IllegalStateException there was insufficient capacity to encode {@code src} and
	 * allocating additional capacity is not possible
	 * @since 1.2
	 */
	public ProtobufEncoder encodePackedFixed64ArrayField(@FieldTag int tag, @Nullable long[] src) {
		return this.encodePackedFixed64ArrayField(tag, src, 0, src == null ? 0 : src.length);
	}

	// Encode subsequence of `float` array as packed `fixed32` sequence.
	private static void encodePackedFloatArrayImpl(
		ProtobufEncoder schema, ProtobufEncoder content,
		int tag,
		@Nullable float[] src, int off, int len
	) {
		Preconditions.checkFromIndexSize(off, len, src == null ? 0 : src.length);

		int dstOff = encodeLenExactImpl(schema, content, tag, len * 4);
		byte[] dst = content.array;

		for (int i = 0; i < len; i++) {
			//noinspection DataFlowIssue
			int a = Float.floatToIntBits(src[off + i]);

			storeIntLe(dst, dstOff + i * 4, a);
		}
	}

	/**
	 * Encode subsequence of {@code float} array as {@code len} sequence of packed {@code fixed32}.
	 * <p>Efficient equivalent of:
	 * {@snippet :
	 * encodeLen(enc -> {
	 *     for (int i = 0; i < len; i++)
	 *         enc.encodeFloat(src[off + i]); // @link substring="encodeFloat" target="#encodeFloat(float)"
	 * });
	 * }
	 *
	 * @param src array to encode subsequence of
	 * @param off offset, within {@code src}, to begin encoding from
	 * @param len number of values to encode
	 * @return {@code this}
	 * @throws IndexOutOfBoundsException {@code off} or {@code len} is negative, {@code src} is
	 * {@code null} and {@code len} is non-zero, or {@code src} is non-{@code null} and {@code
	 * off + len} is greater than {@code src.length}
	 * @throws IllegalStateException there was insufficient capacity to encode {@code src} and
	 * allocating additional capacity is not possible
	 * @since 1.2
	 */
	public ProtobufEncoder encodePackedFloatArray(@Nullable float[] src, int off, int len) {
		encodePackedFloatArrayImpl(this, this.contentEncoder, 0, src, off, len);
		return this;
	}

	/**
	 * Encode {@code float} array as {@code len} sequence of packed {@code fixed32}.
	 * <p>Shorthand for:
	 * {@snippet :
	 * encodePackedFloatArray(src, 0, src == null ? 0 : src.length); // @link substring="encodePackedFloatArray" target="#encodePackedFloatArray(float[], int, int)"
	 * }
	 *
	 * @param src array to encode subsequence of
	 * @return {@code this}
	 * @throws IllegalStateException there was insufficient capacity to encode {@code src} and
	 * allocating additional capacity is not possible
	 * @since 1.2
	 */
	public ProtobufEncoder encodePackedFloatArray(@Nullable float[] src) {
		return this.encodePackedFloatArray(src, 0, src == null ? 0 : src.length);
	}

	/**
	 * Encode {@code len} sequence field with subsequence of {@code float} array packed into
	 * {@code fixed32} sequence.
	 * <p>Efficient equivalent of:
	 * {@snippet :
	 * if (src != null && len > 0) {
	 *     encodeFieldTag(tag) // @link substring="encodeFieldTag" target="#encodeFieldTag(int)"
	 *         .encodePackedFloatArray(src, off, len); // @link substring="encodePackedFloatArray" target="#encodePackedFloatArray(float[], int, int)"
	 * }
	 * }
	 *
	 * @param tag tag of field to encode
	 * @param src array to encode subsequence of
	 * @param off offset, within {@code src}, to begin encoding from
	 * @param len number of values to encode
	 * @return {@code this}
	 * @throws IllegalArgumentException wire type of {@code tag} is not {@link Protobuf#WIRE_LEN
	 * len}
	 * @throws IndexOutOfBoundsException {@code off} or {@code len} is negative, {@code src} is
	 * {@code null} and {@code len} is non-zero, or {@code src} is non-{@code null} and {@code
	 * off + len} is greater than {@code src.length}
	 * @throws IllegalStateException there was insufficient capacity to encode {@code src} and
	 * allocating additional capacity is not possible
	 * @since 1.2
	 */
	public ProtobufEncoder
	encodePackedFloatArrayField(@FieldTag int tag, @Nullable float[] src, int off, int len) {
		checkFieldWireType(tag, WIRE_LEN);
		encodePackedFloatArrayImpl(this, this.contentEncoder, tag, src, off, len);
		return this;
	}

	/**
	 * Encode {@code len} sequence field with {@code float} array packed into {@code fixed32}
	 * sequence.
	 * <p>Shorthand for:
	 * {@snippet :
	 * encodePackedFloatArrayField(tag, src, off, len); // @link substring="encodePackedFloatArrayField" target="#encodePackedFloatArrayField(int, float[], int, int)"
	 * }
	 *
	 * @param tag tag of field to encode
	 * @param src array to encode subsequence of
	 * @return {@code this}
	 * @throws IllegalArgumentException wire type of {@code tag} is not {@link Protobuf#WIRE_LEN
	 * len}
	 * @throws IllegalStateException there was insufficient capacity to encode {@code src} and
	 * allocating additional capacity is not possible
	 * @since 1.2
	 */
	public ProtobufEncoder encodePackedFloatArrayField(@FieldTag int tag, @Nullable float[] src) {
		return this.encodePackedFloatArrayField(tag, src, 0, src == null ? 0 : src.length);
	}

	// Encode subsequence of `double` array as packed `fixed64` sequence.
	private static void encodePackedDoubleArrayImpl(
		ProtobufEncoder schema, ProtobufEncoder content,
		int tag,
		@Nullable double[] src, int off, int len
	) {
		Preconditions.checkFromIndexSize(off, len, src == null ? 0 : src.length);

		int dstOff = encodeLenExactImpl(schema, content, tag, len * 8);
		byte[] dst = content.array;

		for (int i = 0; i < len; i++) {
			//noinspection DataFlowIssue
			long a = Double.doubleToLongBits(src[off + i]);

			storeLongLe(dst, dstOff + i * 8, a);
		}
	}

	/**
	 * Encode subsequence of {@code double} array as {@code len} sequence of packed {@code fixed64}.
	 * <p>Efficient equivalent of:
	 * {@snippet :
	 * encodeLen(enc -> {
	 *     for (int i = 0; i < len; i++)
	 *         enc.encodeDouble(src[off + i]); // @link substring="encodeDouble" target="#encodeDouble(double)"
	 * });
	 * }
	 *
	 * @param src array to encode subsequence of
	 * @param off offset, within {@code src}, to begin encoding from
	 * @param len number of values to encode
	 * @return {@code this}
	 * @throws IndexOutOfBoundsException {@code off} or {@code len} is negative, {@code src} is
	 * {@code null} and {@code len} is non-zero, or {@code src} is non-{@code null} and {@code
	 * off + len} is greater than {@code src.length}
	 * @throws IllegalStateException there was insufficient capacity to encode {@code src} and
	 * allocating additional capacity is not possible
	 * @since 1.2
	 */
	public ProtobufEncoder encodePackedDoubleArray(@Nullable double[] src, int off, int len) {
		encodePackedDoubleArrayImpl(this, this.contentEncoder, 0, src, off, len);
		return this;
	}

	/**
	 * Encode {@code double} array as {@code len} sequence of packed {@code fixed64}.
	 * <p>Shorthand for:
	 * {@snippet :
	 * encodePackedDoubleArray(src, 0, src == null ? 0 : src.length); // @link substring="encodePackedDoubleArray" target="#encodePackedDoubleArray(double[], int, int)"
	 * }
	 *
	 * @param src array to encode subsequence of
	 * @return {@code this}
	 * @throws IllegalStateException there was insufficient capacity to encode {@code src} and
	 * allocating additional capacity is not possible
	 * @since 1.2
	 */
	public ProtobufEncoder encodePackedDoubleArray(@Nullable double[] src) {
		return this.encodePackedDoubleArray(src, 0, src == null ? 0 : src.length);
	}

	/**
	 * Encode {@code len} sequence field with subsequence of {@code double} array packed into
	 * {@code fixed64} sequence.
	 * <p>Efficient equivalent of:
	 * {@snippet :
	 * if (src != null && len > 0) {
	 *     encodeFieldTag(tag) // @link substring="encodeFieldTag" target="#encodeFieldTag(int)"
	 *         .encodePackedDoubleArray(src, off, len); // @link substring="encodePackedDoubleArray" target="#encodePackedDoubleArray(double[], int, int)"
	 * }
	 * }
	 *
	 * @param tag tag of field to encode
	 * @param src array to encode subsequence of
	 * @param off offset, within {@code src}, to begin encoding from
	 * @param len number of values to encode
	 * @return {@code this}
	 * @throws IllegalArgumentException wire type of {@code tag} is not {@link Protobuf#WIRE_LEN
	 * len}
	 * @throws IndexOutOfBoundsException {@code off} or {@code len} is negative, {@code src} is
	 * {@code null} and {@code len} is non-zero, or {@code src} is non-{@code null} and {@code
	 * off + len} is greater than {@code src.length}
	 * @throws IllegalStateException there was insufficient capacity to encode {@code src} and
	 * allocating additional capacity is not possible
	 * @since 1.2
	 */
	public ProtobufEncoder
	encodePackedDoubleArrayField(@FieldTag int tag, @Nullable double[] src, int off, int len) {
		checkFieldWireType(tag, WIRE_LEN);
		encodePackedDoubleArrayImpl(this, this.contentEncoder, tag, src, off, len);
		return this;
	}

	/**
	 * Encode {@code len} sequence field with {@code double} array packed into {@code fixed64}
	 * sequence.
	 * <p>Shorthand for:
	 * {@snippet :
	 * encodePackedDoubleArrayField(tag, src, off, len); // @link substring="encodePackedDoubleArrayField" target="#encodePackedDoubleArrayField(int, double[], int, int)"
	 * }
	 *
	 * @param tag tag of field to encode
	 * @param src array to encode subsequence of
	 * @return {@code this}
	 * @throws IllegalArgumentException wire type of {@code tag} is not {@link Protobuf#WIRE_LEN
	 * len}
	 * @throws IllegalStateException there was insufficient capacity to encode {@code src} and
	 * allocating additional capacity is not possible
	 * @since 1.2
	 */
	public ProtobufEncoder
	encodePackedDoubleArrayField(@FieldTag int tag, @Nullable double[] src) {
		return this.encodePackedDoubleArrayField(tag, src, 0, src == null ? 0 : src.length);
	}
}
