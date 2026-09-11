// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.antifraud;

import android.util.Base64;

import androidx.annotation.Nullable;

import org.polygamma.android.origin.antifraud.CheckWire.IvtRating;

import java.util.Arrays;

/**
 * Anti-fraud status description of a device.
 *
 * @since 1.1
 * @see AntifraudModule#status()
 */
public final class AntifraudStatus {

	/**
	 * Status digest.
	 */
	final @Nullable byte[] digest;

	/**
	 * Fraudlent rating.
	 */
	final @IvtRating int rating;

	/**
	 * Confidence, within range {@code [0; 100]}, of rating.
	 */
	final int confidence;

	/**
	 * Construct new status.
	 *
	 * @param digest status digest or {@code null} if unavailable
	 * @param rating fraudulent rating
	 * @param conf confidence, between {@code 0} and {@code 100}, of rating
	 */
	AntifraudStatus(@Nullable byte[] digest, @IvtRating int rating, int conf) {
		this.digest = digest == null || digest.length == 0 ? null : digest;
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
		return this.digest == null ? "" : Base64.encodeToString(
			this.digest,
			Base64.NO_PADDING | Base64.NO_WRAP | Base64.URL_SAFE
		);
	}

	/**
	 * Device has been marked as fraudulent.
	 *
	 * @return {@code true} if, and only if, device is a fraudulent
	 * @since 1.1
	 */
	public boolean isFraudulent() {
		return this.rating == CheckWire.IvtRatingNonHuman;
	}

	/**
	 * Device has been marked as legitimate.
	 *
	 * @return {@code true} if, and only if, device is not fraudulent
	 * @since 1.1
	 */
	public boolean isLegitimate() {
		return this.rating == CheckWire.IvtRatingHuman;
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
			Arrays.equals(this.digest, that.digest) &&
			this.rating == that.rating &&
			this.confidence == that.confidence
		);
	}
}
