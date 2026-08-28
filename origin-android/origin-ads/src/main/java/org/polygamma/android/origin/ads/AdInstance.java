// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.ads;

import android.util.Pair;

import org.polygamma.android.origin.adcom.media.Ad;
import org.polygamma.android.origin.adcom.media.DisplayAd;
import org.polygamma.android.origin.adcom.media.PlaybackAd;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * Instance of an ad allocated for a placement.
 *
 * @since 1.2
 */
public final class AdInstance {

	private String id;
	private Ad ad;
	private String priceCurrencyCode;
	private long priceMilli;
	private long rewardItemCount;
	private String rewardItemType;
	private float previewImageScaleFactor;

	/**
	 * Construct new empty ad instance.
	 */
	AdInstance() {
		this.id = "";
		this.ad = Ad.of();
		this.priceCurrencyCode = "";
		this.rewardItemType = "";
	}

	/**
	 * Globally unique instance id.
	 *
	 * @return global instance id
	 * @since 1.2
	 */
	public String id() {
		return this.id;
	}

	/**
	 * Set globally unique instance id.
	 *
	 * @param id global instance id
	 */
	void setId(String id) {
		this.id = id;
	}

	/**
	 * Advertising media allocated for execution.
	 *
	 * @return adveritisng media
	 */
	Ad ad() {
		return this.ad;
	}

	/**
	 * Set advertising media allocated for execution.
	 *
	 * @param ad advertising media
	 */
	void setAd(Ad ad) {
		this.ad = ad;
	}

	/**
	 * Advertising media identifier, unique to buyer.
	 *
	 * @return advertising media identifier
	 * @since 1.2
	 */
	public String adId() {
		return this.ad.id();
	}

	/**
	 * Test whether advertising media is display media.
	 *
	 * @return {@code true} if, and only if, display media
	 * @since 1.2
	 * @see #isPlaybackAd()
	 */
	public boolean isDisplayAd() {
		return this.ad instanceof DisplayAd;
	}

	/**
	 * Test whether advertising media is playback media.
	 *
	 * @return {@code true} if, and only if, playback media
	 * @since 1.2
	 * @see #isAudioAd()
	 * @see #isDisplayAd()
	 * @see #isVideoAd()
	 */
	public boolean isPlaybackAd() {
		return this.ad instanceof PlaybackAd;
	}

	/**
	 * Test whether advertising media is audio playback media.
	 *
	 * @return {@code true} if, and only if, audio playback media
	 * @since 1.2
	 * @see #isDisplayAd()
	 * @see #isPlaybackAd()
	 * @see #isVideoAd()
	 */
	public boolean isAudioAd() {
		return this.isPlaybackAd() && ((PlaybackAd) this.ad).isAudioAd();
	}

	/**
	 * Test whether advertising media is video playback media.
	 *
	 * @return {@code true} if, and only if, video playback media
	 * @since 1.2
	 * @see #isAudioAd()
	 * @see #isDisplayAd()
	 * @see #isPlaybackAd()
	 */
	public boolean isVideoAd() {
		return this.isPlaybackAd() && ((PlaybackAd) this.ad).isVideoAd();
	}

	/**
	 * Price and ISO 4217 currency code.
	 * <p>The price returned is what the advertiser has agreed to pay if, and only if, ad is
	 * distributed to the user and its billing model is met.
	 *
	 * @return tuple of price and currency code, respectively
	 * @since 1.2
	 */
	public Pair<BigDecimal, String> priceAndCurrencyCode() {
		return new Pair<>(BigDecimal.valueOf(this.priceMilli, 3), this.priceCurrencyCode);
	}

	/**
	 * Set price, in one-thousandths, buyer has agreed to pay for distribution of ad media.
	 *
	 * @param price milli price
	 */
	void setPriceMilli(long price) {
		this.priceMilli = Math.max(price, 0);
	}

	/**
	 * Set ISO 4217 code of currency {@linkplain #setPriceMilli(long) price} is specified in.
	 *
	 * @param code currency code
	 */
	void setPriceCurrencyCode(String code) {
		this.priceCurrencyCode = code;
	}

	/**
	 * Test whether user is rewarded for viewing ad media.
	 *
	 * @return {@code true} if, and only if, user is rewarded
	 * @since 1.2
	 * @see #rewardItemCount()
	 * @see #rewardItemType()
	 */
	public boolean isRewarded() {
		return this.rewardItemCount > 0;
	}

	/**
	 * Count of item user is rewarded with for viewing ad media.
	 *
	 * @return reward item count
	 * @since 1.2
	 * @see #isRewarded()
	 * @see #rewardItemType()
	 */
	public long rewardItemCount() {
		return this.rewardItemCount;
	}

	/**
	 * Set count of item user is rewarded with for viewing ad media.
	 *
	 * @param count reward item count
	 */
	void setRewardItemCount(long count) {
		this.rewardItemCount = Math.max(count, 0);
	}

	/**
	 * Type of item user is rewarded with for viewing ad media.
	 *
	 * @return reward item type or {@linkplain String#isEmpty() empty} if undefined
	 * @since 1.2
	 * @see #isRewarded()
	 * @see #rewardItemCount()
	 */
	public String rewardItemType() {
		return this.rewardItemType;
	}

	/**
	 * Set type of item user is rewarded with for viewing ad media.
	 *
	 * @param type reward item type
	 */
	void setRewardItemType(String type) {
		this.rewardItemType = type;
	}

	/**
	 * Test whether a preview image of the executed ad media is required.
	 *
	 * @return {@code true} if, and only if, preview image is required
	 * @see #previewImageScaleFactor()
	 */
	boolean needPreviewImage() {
		return this.previewImageScaleFactor != 0;
	}

	/**
	 * Scaling factor to apply to preview image captured from ad media execution.
	 *
	 * @return scaling factor
	 * @see #needPreviewImage()
	 */
	float previewImageScaleFactor() {
		return this.previewImageScaleFactor;
	}

	/**
	 * Set scaling factor to apply to preview image captured from ad media execution.
	 *
	 * @param scale scaling factor or {@code 0} if preview image is not required
	 */
	void setPreviewImageScaleFactor(float scale) {
		this.previewImageScaleFactor = Math.max(scale, 0);
	}

	@Override
	public String toString() {
		return String.format(
			Locale.ROOT,
			"AdInstance {" +
				"priceMilli=%s," +
				"priceCurrencyCode=%s," +
				"id=%s" +
			"}",
			this.priceMilli,
			this.priceCurrencyCode,
			this.id
		);
	}
}
