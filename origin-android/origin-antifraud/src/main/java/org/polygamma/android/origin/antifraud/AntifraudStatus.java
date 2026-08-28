// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.antifraud;

import androidx.annotation.IntDef;
import androidx.annotation.Nullable;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anti-fraud status description of a device.
 *
 * @since 1.1
 * @see AntifraudModule#status()
 */
public final class AntifraudStatus {

	/**
	 * Unknown whether device is fraudulent or not.
	 */
	static final @Rating int RatingUnknown		= 0;

	/**
	 * Device is controlled by non-human agent.
	 */
	static final @Rating int RatingNonHuman		= 1;

	/**
	 * Device is controlled by human user.
	 */
	static final @Rating int RatingHuman		= 2;

	/**
	 * Fraudulent rating enumeration discriminant value marker.
	 */
	@Documented
	@Retention(RetentionPolicy.SOURCE)
	@Target(ElementType.TYPE_USE)
	@IntDef({ RatingHuman, RatingNonHuman, RatingUnknown })
	@interface Rating {
	}

	private final String digest;
	private final @Rating int rating;
	private final int confidence;

	/**
	 * Construct new status.
	 *
	 * @param digest status digest
	 * @param rating fraudulent rating
	 * @param conf confidence, between {@code 0} and {@code 100}, of rating
	 */
	AntifraudStatus(String digest, @Rating int rating, int conf) {
		this.digest = digest;
		this.rating = rating;
		this.confidence = conf;
	}

	/**
	 * Status validation digest.
	 *
	 * @return validation digest
	 * @since 1.1
	 */
	public String digest() {
		return this.digest;
	}

	/**
	 * Fradulent rating.
	 *
	 * @return rating
	 */
	@Rating int rating() {
		return this.rating;
	}

	/**
	 * Device has been marked as fraudulent.
	 *
	 * @return {@code true} if, and only if, device is a fraudulent
	 * @since 1.1
	 */
	public boolean isFraudulent() {
		return this.rating == RatingNonHuman;
	}

	/**
	 * Device has been marked as legitimate.
	 *
	 * @return {@code true} if, and only if, device is not fraudulent
	 * @since 1.1
	 */
	public boolean isLegitimate() {
		return this.rating == RatingHuman;
	}

	/**
	 * Confidence of marking.
	 *
	 * @return value between {@code 0} (inclusive) and {@code 100} (inclusive), where {@code 0} is
	 * no confidence in rating and {@code 100} is highest confidence in rating
	 * @since 1.1
	 */
	public int confidence() {
		return this.confidence;
	}

	@Override
	public int hashCode() {
		return Integer.hashCode(this.rating);
	}

	@Override
	public boolean equals(@Nullable Object other) {
		if (!(other instanceof AntifraudStatus))
			return false;

		AntifraudStatus that = (AntifraudStatus) other;

		return (
			this.digest.equals(that.digest) &&
			this.rating == that.rating &&
			this.confidence == that.confidence
		);
	}
}
