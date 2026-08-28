// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.adcom.placement;

import androidx.annotation.RestrictTo;

import org.polygamma.android.origin.adcom.enums.AdApiCode;
import org.polygamma.android.origin.adcom.enums.AdComEnums;
import org.polygamma.android.origin.protobuf.ProtobufField;
import org.polygamma.android.origin.protobuf.ProtobufSerializable;
import org.polygamma.android.origin.protobuf.ProtobufWriter;
import org.polygamma.android.origin.util.CollectionsCompat;

import java.util.Collection;
import java.util.Collections;

/**
 * Ad media format.
 *
 * @since 1.2
 */
public abstract class AdFormat implements ProtobufSerializable {

	static {
		//noinspection ConstantValue
		assert AdComEnums.MAX_AD_API_CODE < 32;
	}

	private String[] supportedMimes;
	private int supportedAdApiMask;

	/**
	 * Construct new empty ad format.
	 */
	AdFormat() {
		this.supportedMimes = CollectionsCompat.toStringArrayOrEmpty(Collections.emptyList());
	}

	/**
	 * Construct new ad format, copying from another.
	 *
	 * @param that format to copy from
	 */
	AdFormat(AdFormat that) {
		this.supportedMimes = that.supportedMimes;
		this.supportedAdApiMask = that.supportedAdApiMask;
	}

	/**
	 * Count of supported MIME types.
	 *
	 * @return supported MIME type count
	 * @since 1.2
	 * @see #supportedMime(int)
	 */
	public final int supportedMimeCount() {
		return this.supportedMimes.length;
	}

	/**
	 * Supported MIME type at index.
	 *
	 * @param i index to retrieve MIME type at
	 * @return MIME type at {@code i}
	 * @throws IndexOutOfBoundsException {@code i} is negative or, greater than or equal to
	 * supported MIME type {@linkplain #supportedMimeCount() count}
	 * @since 1.2
	 * @see #supportedMimeCount()
	 */
	public final String supportedMime(int i) {
		return this.supportedMimes[i];
	}

	/**
	 * Set supported MIME types.
	 *
	 * @param supp supported MIME types
	 */
	@RestrictTo(RestrictTo.Scope.SUBCLASSES)
	final void setSupportedMimes(Collection<String> supp) {
		this.supportedMimes = CollectionsCompat.toStringArrayOrEmpty(supp);
	}

	/**
	 * Test whether an ad API is supported, by code.
	 *
	 * @param code API code to test
	 * @return {@code true} if, and only if, API is supported
	 * @since 1.2
	 */
	public final boolean isAdApiSupported(@AdApiCode int code) {
		return (
			code >= 0 &&
			code <= AdComEnums.MAX_AD_API_CODE &&
			(this.supportedAdApiMask & (1 << code)) != 0
		);
	}

	/**
	 * Add supported ad API, by code.
	 *
	 * @param code code of API to add
	 */
	@RestrictTo(RestrictTo.Scope.SUBCLASSES)
	final void addSupportedAdApi(@AdApiCode int code) {
		if (code >= 0 && code <= AdComEnums.MAX_AD_API_CODE)
			this.supportedAdApiMask |= (1 << code);
	}

	/**
	 * Set supported ad APIs, by code.
	 *
	 * @param codes supported API codes
	 */
	@RestrictTo(RestrictTo.Scope.SUBCLASSES)
	final void setSupportedAdApis(@AdApiCode int... codes) {
		this.supportedAdApiMask = 0;
		for (int code : codes) {
			if (code >= 0 && code <= AdComEnums.MAX_AD_API_CODE)
				this.supportedAdApiMask |= (1 << code);
		}
	}

	/**
	 * Set mask of supported ad APIs.
	 *
	 * @param mask supported APIs mask
	 */
	final void setSupportedAdApiMask(long mask) {
		this.supportedAdApiMask = (int) (mask & 0xffffffffL);
	}

	/**
	 * Write common Protobuf fields.
	 *
	 * @param writer writer to write fields to
	 * @param mimeTag MIME field tag
	 * @param apiTag API field tag
	 */
	final void writeCommonProtobufFields(
		ProtobufWriter writer,
		@ProtobufField.Tag int mimeTag,
		@ProtobufField.Tag int apiTag
	) {
		writer.writeRepeatString(mimeTag, this.supportedMimes);
		writer.writeWordBitmap(apiTag, Integer.toUnsignedLong(this.supportedAdApiMask), 0);
	}
}
