// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.gppstring;

import android.util.SparseBooleanArray;

import java.util.Date;

/**
 * GPP {@linkplain Segment segment} field type definitions.
 */
interface FieldTypes {

	/**
	 * Single bit {@code boolean}.
	 */
	@FieldType int Boolean					= 0;

	/**
	 * Fixed length {@code int}.
	 */
	@FieldType int FixedInt					= 1;

	/**
	 * Variable length {@code int}.
	 */
	@FieldType int FibonacciInt				= 2;

	/**
	 * Fixed length {@code string}.
	 */
	@FieldType int FixedString				= 3;

	/**
	 * 36-bit {@code datetime} expressed as decaseconds since UNIX epoch.
	 */
	@FieldType int Datetime					= 4;

	/**
	 * Fixed length {@code bitfield}.
	 */
	@FieldType int FixedBitfield			= 5;

	/**
	 * Variable length {@code bitfield}.
	 */
	@FieldType int Bitfield					= 6;

	/**
	 * Range of 16-bit {@code int}s.
	 */
	@FieldType int FixedInt16Range			= 7;

	/**
	 * Range of variable length {@code int}s.
	 */
	@FieldType int FibonacciIntRange		= 8;

	/**
	 * Range of {@code int}s using either {@link #FibonacciIntRange} or {@link #Bitfield}.
	 */
	@FieldType int OptimizedIntRange		= 9;

	/**
	 * Range of {@code int}s using either {@link #FixedInt16Range} or {@link #Bitfield}.
	 */
	@FieldType int OptimizedIntRange2		= 10;

	/**
	 * Array of {@code int} ranges, using {@link #OptimizedIntRange2}, tagged with 6-bit keys and
	 * 2-bit types.
	 */
	@FieldType int ArrayOfIntRanges			= 11;

	/**
	 * Array of {@code int} ranges, using {@link #OptimizedIntRange}, tagged with fixed length
	 * keys and types.
	 */
	@FieldType int ArrayOfFixedIntRanges	= 12;

	/**
	 * Array of {@code int} values.
	 */
	@FieldType int FixedIntList				= 13;

	/**
	 * Size, in bits, of a fixed-bit length.
	 */
	int BITS_PER_FIXED_BIT_LENGTH			= 5;

	/**
	 * Fixed-bit length mask.
	 */
	int FIXED_BIT_LENGTH_MASK				= (~0 >>> (32 - BITS_PER_FIXED_BIT_LENGTH));

	/**
	 * Determine the Java type a field type decodes to.
	 *
	 * @param type field type
	 * @return corresponding Java type
	 * @throws IllegalArgumentException {@code type} is invalid
	 */
	static Class<?> decodedTypeOf(@FieldType int type) {
		switch (type) {
		case Boolean:
			return boolean.class;
		case Datetime:
			return Date.class;
		case FibonacciInt:
		case FixedInt:
			return int.class;
		case FixedString:
			return String.class;
		case Bitfield:
		case FixedBitfield:
			return SparseBooleanArray.class;
		case FibonacciIntRange:
		case FixedInt16Range:
		case FixedIntList:
		case OptimizedIntRange:
		case OptimizedIntRange2:
			return int[].class;
		case ArrayOfIntRanges:
		case ArrayOfFixedIntRanges:
			return TaggedIds[].class;
		default:
			throw new IllegalArgumentException();
		}
	}
}
