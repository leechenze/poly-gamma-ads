// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.protobuf;

import android.annotation.SuppressLint;

import androidx.annotation.IntDef;
import androidx.annotation.IntRange;

import org.polygamma.android.origin.util.Bits;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Protocol buffer field constants.
 *
 * @since 1.2
 */
public class ProtobufField {

	/**
	 * Minimum (inclusive) field number.
	 *
	 * @since 1.2
	 */
	public static final int MIN_NUMBER = 1;

	/**
	 * Maximum (inclusive) field number.
	 *
	 * @since 1.2
	 */
	public static final int MAX_NUMBER = 0x1fffffff;

	/**
	 * Mask of wire type in field tag.
	 */
	static final int TAG_TYPE_MASK = 0x7;

	static {
		/*
		 * Make sure our constants are fine. We don't use the code below in our constants because
		 * some of them are used in annotations, and Java's constant evaluation is crap.
		 */
		//noinspection ConstantValue
		assert TAG_TYPE_MASK ==
			((~0 - 1 + 1) & (~0 >>> (Bits.SIZE_OF_INT - 1 - (Wire.BITS_PER_TYPE - 1))));
		//noinspection ConstantValue
		assert MAX_NUMBER == ((1 << (Bits.SIZE_OF_INT - Wire.BITS_PER_TYPE)) - 1);
	}

	/**
	 * Protocol buffer field number value marker.
	 *
	 * @since 1.2
	 * @see #MAX_NUMBER
	 * @see #MIN_NUMBER
	 */
	@Documented
	@Retention(RetentionPolicy.SOURCE)
	@Target(ElementType.TYPE_USE)
	@IntRange(from = MIN_NUMBER, to = MAX_NUMBER)
	public @interface Number {
	}

	/**
	 * Protocol buffer field tag value marker.
	 *
	 * @since 1.2
	 * @see #ofInt32(int)
	 */
	@Documented
	@Retention(RetentionPolicy.SOURCE)
	@Target(ElementType.TYPE_USE)
	@IntDef
	public @interface Tag {
	}

	/**
	 * Construct tag from field {@linkplain Number number} and value wire {@linkplain Wire.Type
	 * type}.
	 *
	 * @param num field number
	 * @param type field value wire type
	 * @return resulting tag
	 */
	@SuppressLint("WrongConstant")
	private static @Tag int of(@Number int num, @Wire.Type int type) {
		return (num << Wire.BITS_PER_TYPE) | type;
	}

	/**
	 * Extract field number from field tag.
	 *
	 * @param tag tag to extract number from
	 * @return field number extracted from {@code tag}
	 * @since 1.2
	 */
	public static @Number int numberOf(@Tag int tag) {
		return tag >>> Wire.BITS_PER_TYPE;
	}

	/**
	 * Construct {@code int32} field tag.
	 *
	 * @param num field number
	 * @return resulting tag
	 * @since 1.2
	 */
	public static @Tag int ofInt32(@Number int num) {
		return of(num, Wire.TYPE_VARINT);
	}

	/**
	 * Construct {@code sint32} field tag.
	 *
	 * @param num field number
	 * @return resulting tag
	 * @since 1.2
	 */
	public static @Tag int ofSint32(@Number int num) {
		return of(num, Wire.TYPE_VARINT);
	}

	/**
	 * Construct {@code fixed32} field tag.
	 *
	 * @param num field number
	 * @return resulting tag
	 * @since 1.2
	 */
	public static @Tag int ofFixed32(@Number int num) {
		return of(num, Wire.TYPE_FIXED32);
	}

	/**
	 * Construct {@code int64} field tag.
	 *
	 * @param num field number
	 * @return resulting tag
	 * @since 1.2
	 */
	public static @Tag int ofInt64(@Number int num) {
		return of(num, Wire.TYPE_VARINT);
	}

	/**
	 * Construct {@code sint64} field tag.
	 *
	 * @param num field number
	 * @return resulting tag
	 * @since 1.2
	 */
	public static @Tag int ofSint64(@Number int num) {
		return of(num, Wire.TYPE_VARINT);
	}

	/**
	 * Construct {@code fixed64} field tag.
	 *
	 * @param num field number
	 * @return resulting tag
	 * @since 1.2
	 */
	public static @Tag int ofFixed64(@Number int num) {
		return of(num, Wire.TYPE_FIXED64);
	}

	/**
	 * Construct {@code float} field tag.
	 *
	 * @param num field number
	 * @return resulting tag
	 * @since 1.2
	 */
	public static @Tag int ofFloat(@Number int num) {
		return ofFixed32(num);
	}

	/**
	 * Construct {@code double} field tag.
	 *
	 * @param num field number
	 * @return resulting tag
	 * @since 1.2
	 */
	public static @Tag int ofDouble(@Number int num) {
		return ofFixed64(num);
	}

	/**
	 * Construct {@code bool} field tag.
	 *
	 * @param num field number
	 * @return resulting tag
	 * @since 1.2
	 */
	public static @Tag int ofBool(@Number int num) {
		return of(num, Wire.TYPE_VARINT);
	}

	/**
	 * Construct packed {@code int32} field tag.
	 *
	 * @param num field number
	 * @return resulting tag
	 * @since 1.2
	 */
	public static @Tag int ofPackedInt32(@Number int num) {
		return of(num, Wire.TYPE_LEN);
	}

	/**
	 * Construct packed {@code sint32} field tag.
	 *
	 * @param num field number
	 * @return resulting tag
	 * @since 1.2
	 */
	public static @Tag int ofPackedSint32(@Number int num) {
		return of(num, Wire.TYPE_LEN);
	}

	/**
	 * Construct packed {@code fixed32} field tag.
	 *
	 * @param num field number
	 * @return resulting tag
	 * @since 1.2
	 */
	public static @Tag int ofPackedFixed32(@Number int num) {
		return of(num, Wire.TYPE_LEN);
	}

	/**
	 * Construct packed {@code int64} field tag.
	 *
	 * @param num field number
	 * @return resulting tag
	 * @since 1.2
	 */
	public static @Tag int ofPackedInt64(@Number int num) {
		return of(num, Wire.TYPE_LEN);
	}

	/**
	 * Construct packed {@code sint64} field tag.
	 *
	 * @param num field number
	 * @return resulting tag
	 * @since 1.2
	 */
	public static @Tag int ofPackedSint64(@Number int num) {
		return of(num, Wire.TYPE_LEN);
	}

	/**
	 * Construct packed {@code fixed64} field tag.
	 *
	 * @param num field number
	 * @return resulting tag
	 * @since 1.2
	 */
	public static @Tag int ofPackedFixed64(@Number int num) {
		return of(num, Wire.TYPE_LEN);
	}

	/**
	 * Construct packed {@code float} field tag.
	 *
	 * @param num field number
	 * @return resulting tag
	 * @since 1.2
	 */
	public static @Tag int ofPackedFloat(@Number int num) {
		return of(num, Wire.TYPE_LEN);
	}

	/**
	 * Construct packed {@code double} field tag.
	 *
	 * @param num field number
	 * @return resulting tag
	 * @since 1.2
	 */
	public static @Tag int ofPackedDouble(@Number int num) {
		return of(num, Wire.TYPE_LEN);
	}

	/**
	 * Construct packed {@code bool} field tag.
	 *
	 * @param num field number
	 * @return resulting tag
	 * @since 1.2
	 */
	public static @Tag int ofPackedBool(@Number int num) {
		return of(num, Wire.TYPE_LEN);
	}

	/**
	 * Construct {@code byte} sequence field tag.
	 *
	 * @param num field number
	 * @return resulting tag
	 * @since 1.2
	 */
	public static @Tag int ofBytes(@Number int num) {
		return of(num, Wire.TYPE_LEN);
	}

	/**
	 * Construct {@code string} field tag.
	 *
	 * @param num field number
	 * @return resulting tag
	 * @since 1.2
	 */
	public static @Tag int ofString(@Number int num) {
		return of(num, Wire.TYPE_LEN);
	}

	/**
	 * Construct {@code string} pair field tag.
	 *
	 * @param num field number
	 * @return resulting tag
	 * @since 1.2
	 */
	public static @Tag int ofStringPair(@Number int num) {
		return of(num, Wire.TYPE_LEN);
	}

	/**
	 * Construct {@code message} field tag.
	 *
	 * @param num field number
	 * @return resulting tag
	 * @since 1.2
	 */
	public static @Tag int ofMessage(@Number int num) {
		return of(num, Wire.TYPE_LEN);
	}

	private ProtobufField() {
	}
}
