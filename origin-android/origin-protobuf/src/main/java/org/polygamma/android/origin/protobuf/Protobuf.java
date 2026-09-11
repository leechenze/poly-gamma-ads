// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.protobuf;

import android.annotation.SuppressLint;

import androidx.annotation.IntDef;
import androidx.annotation.IntRange;

import org.polygamma.android.origin.util.Preconditions;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Protocol buffer constant definitions.
 * <p>Protocol buffers support 4 wire types, used to encode all high-level Protocol buffer value
 * types. Within a message, field tags are used to denote which field a value is assigned to. A
 * field tag is comprised of a field number and a value wire type.
 * <table style="border: 1px solid black; border-collapse: collapse;">
 *     <caption>Protocol Buffer Types</caption>
 *     <thead>
 *         <tr>
 *             <th>Field Type</th>
 *             <th>Wire Type</th>
 *         </tr>
 *     </thead>
 *     <tbody>
 *         <tr>
 *             <td>{@code double}</td>
 *             <td>{@link #WIRE_FIXED64 fixed64}</td>
 *         </tr>
 *         <tr>
 *             <td>{@code float}</td>
 *             <td>{@link #WIRE_FIXED32 fixed32}</td>
 *         </tr>
 *         <tr>
 *             <td>{@code fixed64}</td>
 *             <td>{@link #WIRE_FIXED64 fixed64}</td>
 *         </tr>
 *         <tr>
 *             <td>{@code fixed32}</td>
 *             <td>{@link #WIRE_FIXED32 fixed32}</td>
 *         </tr>
 *         <tr>
 *             <td>{@code uint32}</td>
 *             <td>{@link #WIRE_VARINT varint}</td>
 *         </tr>
 *         <tr>
 *             <td>{@code sint32}</td>
 *             <td>{@link #WIRE_VARINT varint}</td>
 *         </tr>
 *         <tr>
 *             <td>{@code uint64}</td>
 *             <td>{@link #WIRE_VARINT varint}</td>
 *         </tr>
 *         <tr>
 *             <td>{@code sint64}</td>
 *             <td>{@link #WIRE_VARINT varint}</td>
 *         </tr>
 *         <tr>
 *             <td>{@code bool}</td>
 *             <td>{@link #WIRE_VARINT varint}</td>
 *         </tr>
 *         <tr>
 *             <td>{@code string}</td>
 *             <td>{@link #WIRE_LEN len}</td>
 *         </tr>
 *         <tr>
 *             <td>{@code bytes}</td>
 *             <td>{@link #WIRE_LEN len}</td>
 *         </tr>
 *         <tr>
 *             <td>{@code message}</td>
 *             <td>{@link #WIRE_LEN len}</td>
 *         </tr>
 *     </tbody>
 * </table>
 *
 * @since 1.2
 */
public class Protobuf {

	/**
	 * Number of bits required to encode wire type.
	 */
	static final int BITS_PER_WIRE_TYPE = 3;

	/**
	 * Mask isolating wire type in field tag.
	 */
	static final int TAG_WIRE_TYPE_MASK = (1 << BITS_PER_WIRE_TYPE) - 1;

	/**
	 * Minimum, inclusive, possible field number.
	 *
	 * @since 1.2
	 */
	public static final @FieldNumber int MIN_FIELD_NUMBER = 1;

	/**
	 * Maximum, inclusive, possible field number.
	 *
	 * @since 1.2
	 */
	public static final @FieldNumber int MAX_FIELD_NUMBER =
		(~0 & ~TAG_WIRE_TYPE_MASK) >>> BITS_PER_WIRE_TYPE;

	/**
	 * Variable length integer.
	 *
	 * @since 1.2
	 */
	public static final @WireType int WIRE_VARINT  = 0;

	/**
	 * Fixed 64-bit value.
	 * <p>This is coded as an 8-byte value, in little-endian byte-order.
	 *
	 * @since 1.2
	 */
	public static final @WireType int WIRE_FIXED64 = 1;

	/**
	 * Length delimited sequence.
	 * <p>This is coded as the sequence length encoded as a {@link #WIRE_VARINT varint}
	 * followed by the sequence bytes.
	 *
	 * @since 1.2
	 */
	public static final @WireType int WIRE_LEN     = 2;

	/**
	 * Fixed 32-bit value.
	 * <p>This is coded as an 4-byte value, in little-endian byte-order.
	 *
	 * @since 1.2
	 */
	public static final @WireType int WIRE_FIXED32 = 5;

	/**
	 * Maximum size, in bytes, of a {@link #WIRE_VARINT varint} encoded from 32-bit integer.
	 */
	static final int MAX_VARINT32_SIZE = varintSizeOfBits(32);

	/**
	 * Maximum size, in bytes, of a {@link #WIRE_VARINT varint} encoded from 64-bit integer.
	 */
	static final int MAX_VARINT64_SIZE = varintSizeOfBits(64);

	/**
	 * Tag of field containing first string in a string pair.
	 */
	static final int STRING_PAIR_A_TAG = fieldTagOf(1, WIRE_LEN);

	/**
	 * Tag of field containing second string in a string pair.
	 */
	static final int STRING_PAIR_B_TAG = fieldTagOf(2, WIRE_LEN);

	/**
	 * Protocol buffer wire type enumeration value marker.
	 *
	 * @since 1.2
	 */
	@Documented
	@Retention(RetentionPolicy.SOURCE)
	@Target(ElementType.TYPE_USE)
	@IntDef({WIRE_FIXED32, WIRE_FIXED64, WIRE_LEN, WIRE_VARINT})
	public @interface WireType {
	}

	/**
	 * Field number marker.
	 * <p>Protocol buffer field numbers are positive integers, with a maximum of {@link
	 * #MAX_FIELD_NUMBER}.
	 *
	 * @since 1.2
	 */
	@Documented
	@Retention(RetentionPolicy.SOURCE)
	@Target(ElementType.TYPE_USE)
	@IntRange(from = MIN_FIELD_NUMBER, to = MAX_FIELD_NUMBER)
	public @interface FieldNumber {
	}

	/**
	 * Field tag value marker.
	 * <p>Field tags are comprised of a field {@linkplain FieldNumber number} and a {@linkplain
	 * WireType wire type}.
	 *
	 * @since 1.2
	 * @see #fieldTagOf(int, int)
	 */
	@Documented
	@Retention(RetentionPolicy.SOURCE)
	@Target(ElementType.TYPE_USE)
	@IntDef()
	public @interface FieldTag {
	}

	/**
	 * Test whether an {@code int} is a valid wire type.
	 *
	 * @param wtype {@code int} to test
	 * @return {@code true} if, and only if, {@code wtype} is a valid wire type
	 */
	static boolean isWireTypeValid(int wtype) {
		return
			wtype == WIRE_VARINT ||
			wtype == WIRE_FIXED64 ||
			wtype == WIRE_LEN ||
			wtype == WIRE_FIXED32;
	}

	/**
	 * Construct field tag from a field number and wire type.
	 *
	 * @param num field number
	 * @param wtype field value wire type
	 * @return resulting field tag
	 * @since 1.2
	 */
	@SuppressLint("WrongConstant")
	public static @FieldTag int fieldTagOf(@FieldNumber int num, @WireType int wtype) {
		return (num << BITS_PER_WIRE_TYPE) | wtype;
	}

	/**
	 * Extract field number from a field tag.
	 *
	 * @param tag tag to extract from
	 * @return field number of {@code tag}
	 * @throws IllegalArgumentException {@code tag} is malformed
	 * @since 1.2
	 */
	public static @FieldNumber int fieldNumberOfFieldTag(@FieldTag int tag) {
		int num = tag >>> BITS_PER_WIRE_TYPE;

		//noinspection ConstantValue
		Preconditions.checkArgument(num >= MIN_FIELD_NUMBER && num <= MAX_FIELD_NUMBER);
		return num;
	}

	/**
	 * Extract wire type from a field tag.
	 *
	 * @param tag tag to extract from
	 * @return wire type of {@code tag}
	 * @throws IllegalArgumentException {@code tag} is malformed
	 * @since 1.2
	 */
	public static @WireType int wireTypeOfFieldTag(@FieldTag int tag) {
		@SuppressLint("WrongConstant") int wtype = tag & TAG_WIRE_TYPE_MASK;

		Preconditions.checkArgument(isWireTypeValid(wtype));
		return wtype;
	}

	/**
	 * Calculate maximum size, in bytes, of a {@code varint} encoded from an {@code n}-bit integer.
	 *
	 * @param nbits size, in bits, of integer to be encoded
	 * @return size, in bytes, of {@code varint}
	 */
	static int varintSizeOfBits(@IntRange(from = 1, to = 64) int nbits) {
		// 7 bits per varint byte
		return (nbits + 7 - 1) / 7;
	}

	/**
	 * Ensure wire type of field tag is expected.
	 *
	 * @param tag field tag to test wire type of
	 * @param exp expected wire type
	 * @throws IllegalArgumentException wire type of {@code tag} is not {@code exp}
	 */
	static void checkFieldWireType(@FieldTag int tag, @WireType int exp) {
		Preconditions.checkArgument(wireTypeOfFieldTag(tag) == exp);
	}

	private Protobuf() {
	}
}
