// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.protobuf;

import androidx.annotation.IntDef;

import org.polygamma.android.origin.util.Bits;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Protocol buffer wire constants.
 */
class Wire {

	/**
	 * Variable-length integer.
	 */
	static final int TYPE_VARINT = 0;

	/**
	 * Fixed 64-bit integer.
	 */
	static final int TYPE_FIXED64 = 1;

	/**
	 * Length-delimited {@code byte} sequence.
	 */
	static final int TYPE_LEN = 2;

	/**
	 * Fixed 32-bit integer.
	 */
	static final int TYPE_FIXED32 = 5;

	/**
	 * Maximum (inclusive) wire type number.
	 */
	static final @Type int MAX_TYPE = TYPE_FIXED32;

	/**
	 * Number of bits used to encode wire type.
	 */
	static final int BITS_PER_TYPE = 3;

	/**
	 * Maximum size, in bytes, of an {@code int32} encoded as a {@link #TYPE_VARINT varint}.
	 */
	static final int MAX_INT32_VARINT_SIZE	= 5;

	/**
	 * Maximum size, in bytes, of an {@code int64} encoded as a {@link #TYPE_VARINT varint}.
	 */
	static final int MAX_INT64_VARINT_SIZE	= 10;

	static {
		/*
		 * Make sure our constants are fine. We don't use the code below in our constants because
		 * some of them are used in annotations, and Java's constant evaluation is crap.
		 */
		//noinspection ConstantValue
		assert BITS_PER_TYPE == (
			(Bits.SIZE_OF_INT - Integer.numberOfLeadingZeros(MAX_TYPE + 1) - 1) +
			(((MAX_TYPE + 1) & MAX_TYPE) != 0 ? 1 : 0)
		);
	}

	/**
	 * Protocol buffer wire type enumeration value marker.
	 *
	 * @see #TYPE_FIXED32
	 * @see #TYPE_FIXED64
	 * @see #TYPE_LEN
	 * @see #TYPE_VARINT
	 */
	@Documented
	@Retention(RetentionPolicy.SOURCE)
	@Target(ElementType.TYPE_USE)
	@IntDef({ TYPE_FIXED32, TYPE_FIXED64, TYPE_LEN, TYPE_VARINT })
	@interface Type {
	}

	/**
	 * Calculate size, in bytes, of a {@link #TYPE_VARINT varint}.
	 *
	 * @param v {@code varint} to calculate size of
	 * @return size, in bytes, of {@code v}
	 */
	static int sizeOfVarint(long v) {
		return ((Bits.SIZE_OF_LONG - Long.numberOfLeadingZeros(v | 1)) + 6) / 7;
	}

	/**
	 * Encode {@code sint32} into a {@link #TYPE_VARINT varint}.
	 *
	 * @param v value to encode
	 * @return encoded value
	 */
	static int varintOfSint32(int v) {
		return (v << 1) ^ (v >> 31);
	}

	/**
	 * Decode {@code sint32} from a {@link #TYPE_VARINT varint}.
	 *
	 * @param v value to decode
	 * @return decoded value
	 */
	static int sint32OfVarint(int v) {
		return (v >>> 1) ^ -(v & 1);
	}

	/**
	 * Encode {@code sint64} into a {@link #TYPE_VARINT varint}.
	 *
	 * @param v value to encode
	 * @return encoded value
	 */
	static long varintOfSint64(long v) {
		return (v << 1) ^ (v >> 63);
	}

	/**
	 * Decode {@code sint64} from a {@link #TYPE_VARINT varint}.
	 *
	 * @param v value to decode
	 * @return decoded value
	 */
	static long sint64OfVarint(long v) {
		return (v >>> 1) ^ -(v & 1L);
	}

	private Wire() {
	}
}
