// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.adcom.media;

import static org.polygamma.android.origin.protobuf.ProtobufField.*;

import android.annotation.SuppressLint;

import androidx.annotation.IntDef;
import androidx.annotation.Px;

import org.polygamma.android.origin.adcom.enums.AdApiCode;
import org.polygamma.android.origin.adcom.enums.PlaybackDeliveryMethod;
import org.polygamma.android.origin.protobuf.ProtobufReader;
import org.polygamma.android.origin.protobuf.ProtobufSerializable;
import org.polygamma.android.origin.protobuf.ProtobufWriter;
import org.polygamma.android.origin.util.Preconditions;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Linear playback {@linkplain PlaybackCreative creative} media asset.
 *
 * @since 1.2
 */
public final class LinearAsset implements ProtobufSerializable {

	private static final @Tag int URL					= ofString(  1);
	private static final @Tag int MIME					= ofString(  2);
	private static final @Tag int INTERACTIVE			= ofMessage( 3);
	private static final @Tag int MEDIA					= ofMessage( 4);
	private static final @Tag int MEZZANINE				= ofMessage( 5);
	private static final @Tag int CAPTION				= ofMessage( 6);

	// `LinearClosedCaptionAsset`
	private static final @Tag int CAPTION_LANG			= ofString(  1);

	// `LinearMediaAsset`
	private static final @Tag int MEDIA_ID				= ofString(  1);
	private static final @Tag int MEDIA_CODEC			= ofString(  2);
	/*private static final @Tag int MEDIA_SIZE			= ofInt64(   3);*/
	private static final @Tag int MEDIA_AVGBITR			= ofInt32(   4);
	private static final @Tag int MEDIA_MINBITR			= ofInt32(   5);
	private static final @Tag int MEDIA_MAXBITR			= ofInt32(   6);
	private static final @Tag int MEDIA_DELIVERY		= ofInt32(   7);
	private static final @Tag int MEDIA_W				= ofInt32(   8);
	private static final @Tag int MEDIA_H				= ofInt32(   9);
	private static final @Tag int MEDIA_SCALE			= ofBool(   10);
	private static final @Tag int MEDIA_ASPECT			= ofBool(   11);

	// `LinearInteractiveAsset`
	private static final @Tag int INTERACTIVE_API		= ofInt32(   1);
	private static final @Tag int INTERACTIVE_VARDUR	= ofBool(    2);

	/**
	 * No asset type.
	 */
	private static final @AssetClass int ASSET_NONE				= 0x00;

	/**
	 * Closed caption asset.
	 */
	private static final @AssetClass int ASSET_CLOSED_CAPTION	= 0x01;

	/**
	 * Media file asset.
	 */
	private static final @AssetClass int ASSET_MEDIA			= 0x02;

	/**
	 * Mezzanine file asset.
	 */
	private static final @AssetClass int ASSET_MEZZANINE		= 0x03;

	/**
	 * Interactive asset.
	 */
	private static final @AssetClass int ASSET_INTERACTIVE		= 0x04;

	/**
	 * Mask of asset classes.
	 */
	private static final int ASSET_CLASS_MASK					= 0x0f;

	private static final int FLAG_INTERACTIVE_VARDUR			= 0x10000000;
	private static final int FLAG_PLAYBACK_SCALE = 0x20000000;
	private static final int FLAG_PLAYBACK_ASPECT = 0x40000000;

	/**
	 * Asset class enumeration discriminant value marker.
	 */
	@Documented
	@Retention(RetentionPolicy.SOURCE)
	@Target(ElementType.TYPE_USE)
	@IntDef({ ASSET_CLOSED_CAPTION, ASSET_INTERACTIVE, ASSET_MEDIA, ASSET_MEZZANINE, ASSET_NONE })
	private @interface AssetClass {
	}

	/**
	 * Construct new asset.
	 *
	 * @param cls asset class
	 * @param url asset URL
	 * @param mime asset MIME type
	 * @return asset instance
	 */
	private static LinearAsset of(@AssetClass int cls, String url, String mime) {
		LinearAsset rv = new LinearAsset();

		rv.url = url;
		rv.mime = mime;
		rv.flagsAndAssetClass = cls;
		return rv;
	}

	/**
	 * Construct new {@linkplain #isClosedCaptionAsset() closed caption} asset.
	 *
	 * @param url asset URL
	 * @param mime asset MIME type
	 * @param lang ISO 631-1 language code of closed caption
	 * @return asset instance
	 * @since 1.2
	 * @see #isClosedCaptionAsset()
	 */
	public static LinearAsset ofClosedCaptionAsset(String url, String mime, String lang) {
		LinearAsset rv = of(ASSET_CLOSED_CAPTION, url, mime);

		rv.playbackIdOrClosedCaptionLanguageCode = lang;
		return rv;
	}

	/**
	 * Construct new {@linkplain #isInteractiveAsset() interactive} asset.
	 *
	 * @param url asset URL
	 * @param mime asset MIME type
	 * @param api API required to execute asset
	 * @param extDur {@code true} if, and only if, asset may extend playback duration
	 * @return asset instance
	 * @since 1.2
	 * @see #isInteractiveAsset()
	 */
	public static LinearAsset
	ofInteractiveAsset(String url, String mime, @AdApiCode int api, boolean extDur) {
		LinearAsset rv = of(ASSET_INTERACTIVE, url, mime);

		rv.playbackSupportedDeliveryOrInteractiveRequiredApi = api;
		rv.flagsAndAssetClass |= FLAG_INTERACTIVE_VARDUR;
		return rv;
	}

	/**
	 * Construct new playback asset.
	 *
	 * @param cls asset class
	 * @param url asset URL
	 * @param mime asset MIME type
	 * @param id asset identifier, unique to ad
	 * @param codec name of codec used to encode asset
	 * @param avgBitr average bit rate, in Kbps, of asset
	 * @param minBitr minimum bit rate, in Kbps, of asset
	 * @param maxBitr maximum bit rate, in Kbps, of asset
	 * @param delivery delivery method supported for asset
	 * @param w exact width, in pixels, of asset
	 * @param h exact height, in pixels, of asset
	 * @param scale {@code true} if, and only if, asset can be scaled to different dimensions
	 * @param aspect {@code true} if, and only if, aspect ratio of asset must be maintained when
	 * scaled to different dimensions
	 * @return asset instance
	 */
	private static LinearAsset ofPlaybackAsset(
		@AssetClass int cls,
		String url,
		String mime,
		String id,
		String codec,
		int avgBitr,
		int minBitr,
		int maxBitr,
		@PlaybackDeliveryMethod int delivery,
		@Px int w,
		@Px int h,
		boolean scale,
		boolean aspect
	) {
		LinearAsset rv = of(cls, url, mime);

		rv.playbackIdOrClosedCaptionLanguageCode = id;
		rv.playbackCodec = codec;
		rv.playbackAverageBitRateKbps = avgBitr;
		rv.playbackMinBitRateKbps = minBitr;
		rv.playbackMaxBitRateKbps = maxBitr;
		rv.playbackSupportedDeliveryOrInteractiveRequiredApi = delivery;
		rv.playbackWidthPx = w;
		rv.playbackHeightPx = h;
		if (scale)
			rv.flagsAndAssetClass |= FLAG_PLAYBACK_SCALE;
		if (aspect)
			rv.flagsAndAssetClass |= FLAG_PLAYBACK_ASPECT;
		return rv;
	}

	/**
	 * Construct new playback {@linkplain #isMediaAsset() media} asset.
	 *
	 * @param url asset URL
	 * @param mime asset MIME type
	 * @param id asset identifier, unique to ad
	 * @param codec name of codec used to encode asset
	 * @param avgBitr average bit rate, in Kbps, of asset
	 * @param minBitr minimum bit rate, in Kbps, of asset
	 * @param maxBitr maximum bit rate, in Kbps, of asset
	 * @param delivery delivery method supported for asset
	 * @param w exact width, in pixels, of asset
	 * @param h exact height, in pixels, of asset
	 * @param scale {@code true} if, and only if, asset can be scaled to different dimensions
	 * @param aspect {@code true} if, and only if, aspect ratio of asset must be maintained when
	 * scaled to different dimensions
	 * @return asset instance
	 * @since 1.2
	 * @see #isMediaAsset()
	 */
	public static LinearAsset ofMediaAsset(
		String url,
		String mime,
		String id,
		String codec,
		int avgBitr,
		int minBitr,
		int maxBitr,
		@PlaybackDeliveryMethod int delivery,
		@Px int w,
		@Px int h,
		boolean scale,
		boolean aspect
	) {
		return ofPlaybackAsset(
			ASSET_MEDIA,
			url,
			mime,
			id,
			codec,
			avgBitr,
			minBitr,
			maxBitr,
			delivery,
			w,
			h,
			scale,
			aspect
		);
	}

	/**
	 * Construct new playback {@linkplain #isMezzanineAsset() mezzanine} asset.
	 *
	 * @param url asset URL
	 * @param mime asset MIME type
	 * @param id asset identifier, unique to ad
	 * @param codec name of codec used to encode asset
	 * @param avgBitr average bit rate, in Kbps, of asset
	 * @param minBitr minimum bit rate, in Kbps, of asset
	 * @param maxBitr maximum bit rate, in Kbps, of asset
	 * @param delivery delivery method supported for asset
	 * @param w exact width, in pixels, of asset
	 * @param h exact height, in pixels, of asset
	 * @param scale {@code true} if, and only if, asset can be scaled to different dimensions
	 * @param aspect {@code true} if, and only if, aspect ratio of asset must be maintained when
	 * scaled to different dimensions
	 * @return asset instance
	 * @since 1.2
	 * @see #isMezzanineAsset()
	 */
	public static LinearAsset ofMezzanineAsset(
		String url,
		String mime,
		String id,
		String codec,
		int avgBitr,
		int minBitr,
		int maxBitr,
		@PlaybackDeliveryMethod int delivery,
		@Px int w,
		@Px int h,
		boolean scale,
		boolean aspect
	) {
		return ofPlaybackAsset(
			ASSET_MEZZANINE,
			url,
			mime,
			id,
			codec,
			avgBitr,
			minBitr,
			maxBitr,
			delivery,
			w,
			h,
			scale,
			aspect
		);
	}

	/**
	 * Deserialize linear media asset from Protobuf message.
	 *
	 * @param reader reader to deserialize from
	 * @return deserialized asset
	 * @throws RuntimeException coding is malformed
	 * @since 1.2
	 */
	public static LinearAsset ofProtobuf(ProtobufReader reader) {
		LinearAsset rv = new LinearAsset();

		while (reader.hasRemaining()) {
			int tag = reader.readTag();

			if (tag == URL) {
				rv.url = reader.readString();
			} else if (tag == MIME) {
				rv.mime = reader.readString();
			} else if (tag == CAPTION) {
				int cookie = reader.beginReadLen();

				rv.flagsAndAssetClass = ASSET_CLOSED_CAPTION;
				while (reader.hasRemaining()) {
					tag = reader.readTag();
					if (tag == CAPTION_LANG)
						rv.playbackIdOrClosedCaptionLanguageCode = reader.readString();
				}
				reader.endReadLen(cookie);
			} else if (tag == INTERACTIVE) {
				int cookie = reader.beginReadLen();

				rv.flagsAndAssetClass = ASSET_INTERACTIVE;
				while (reader.hasRemaining()) {
					tag = reader.readTag();
					if (tag == INTERACTIVE_API)
						rv.playbackSupportedDeliveryOrInteractiveRequiredApi = reader.readInt32();
					else if (tag == INTERACTIVE_VARDUR && reader.readBool())
						rv.flagsAndAssetClass |= FLAG_INTERACTIVE_VARDUR;
				}
				reader.endReadLen(cookie);
			} else if (tag == MEDIA || tag == MEZZANINE) {
				int cookie = reader.beginReadLen();

				rv.flagsAndAssetClass = tag == MEDIA ? ASSET_MEDIA : ASSET_MEZZANINE;
				while (reader.hasRemaining()) {
					tag = reader.readTag();
					if (tag == MEDIA_ID)
						rv.playbackIdOrClosedCaptionLanguageCode = reader.readString();
					else if (tag == MEDIA_CODEC)
						rv.playbackCodec = reader.readString();
					else if (tag == MEDIA_AVGBITR)
						rv.playbackAverageBitRateKbps = reader.readInt32();
					else if (tag == MEDIA_MINBITR)
						rv.playbackMinBitRateKbps = reader.readInt32();
					else if (tag == MEDIA_MAXBITR)
						rv.playbackMaxBitRateKbps = reader.readInt32();
					else if (tag == MEDIA_DELIVERY)
						rv.playbackSupportedDeliveryOrInteractiveRequiredApi = reader.readInt32();
					else if (tag == MEDIA_W)
						rv.playbackWidthPx = reader.readInt32();
					else if (tag == MEDIA_H)
						rv.playbackHeightPx = reader.readInt32();
					else if (tag == MEDIA_SCALE && reader.readBool())
						rv.flagsAndAssetClass |= FLAG_PLAYBACK_SCALE;
					else if (tag == MEDIA_ASPECT && reader.readBool())
						rv.flagsAndAssetClass |= FLAG_PLAYBACK_ASPECT;
				}
				reader.endReadLen(cookie);
			}
		}
		return rv;
	}

	private String url;
	private String mime;
	private String playbackIdOrClosedCaptionLanguageCode;
	private String playbackCodec;
	private int flagsAndAssetClass;
	private int playbackSupportedDeliveryOrInteractiveRequiredApi;
	private @Px int playbackWidthPx;
	private @Px int playbackHeightPx;
	private int playbackAverageBitRateKbps;
	private int playbackMinBitRateKbps;
	private int playbackMaxBitRateKbps;

	private LinearAsset() {
		this.url = "";
		this.mime = "";
		this.playbackIdOrClosedCaptionLanguageCode = "";
		this.playbackCodec = "";
	}

	/**
	 * Asset URL.
	 *
	 * @return URL
	 * @since 1.2
	 */
	public String url() {
		return this.url;
	}

	/**
	 * Asset MIME type.
	 *
	 * @return MIME type
	 * @since 1.2
	 */
	public String mime() {
		return this.mime;
	}

	/**
	 * {@return Asset class}
	 */
	@SuppressLint("WrongConstant")
	private @AssetClass int assetClass() {
		return this.flagsAndAssetClass & ASSET_CLASS_MASK;
	}

	/**
	 * Ensure asset is of expected {@linkplain #assetClass() class}.
	 *
	 * @param exp expected asset class
	 * @throws IllegalStateException asset class is not {@code exp}
	 */
	private void checkAssetClass(@AssetClass int exp) {
		Preconditions.checkState(this.assetClass() == exp);
	}

	/**
	 * Test whether asset is interactive creative media.
	 *
	 * @return {@code true} if, and only if, interactive creative media asset
	 * @since 1.2
	 */
	public boolean isInteractiveAsset() {
		return this.assetClass() == ASSET_INTERACTIVE;
	}

	/**
	 * Ad API, by code, required to execute interactive creative media.
	 *
	 * @return required ad API
	 * @throws IllegalStateException asset is not {@linkplain #isInteractiveAsset() interactive}
	 * media
	 * @since 1.2
	 * @see #isInteractiveAsset()
	 */
	public @AdApiCode int interactiveRequiredAdApi() {
		this.checkAssetClass(ASSET_INTERACTIVE);
		return this.playbackSupportedDeliveryOrInteractiveRequiredApi;
	}

	/**
	 * Interactive creative media may extend playback duration beyond playback media asset.
	 *
	 * @return {@code true} if, and only if, playback duration can be extended by media
	 * @throws IllegalStateException asset is not {@linkplain #isInteractiveAsset() interactive}
	 * media
	 * @since 1.2
	 * @see #isInteractiveAsset()
	 */
	public boolean interactiveCanExtendPlaybackDuration() {
		this.checkAssetClass(ASSET_INTERACTIVE);
		return (this.flagsAndAssetClass & FLAG_INTERACTIVE_VARDUR) != 0;
	}

	/**
	 * Test whether asset is playback media.
	 *
	 * @return {@code true} if, and only if, playback media asset
	 * @since 1.2
	 */
	public boolean isMediaAsset() {
		return this.assetClass() == ASSET_MEDIA;
	}

	/**
	 * Test whether asset is playback mezzanine.
	 *
	 * @return {@code true} if, and only if, playback mezzanine asset
	 * @since 1.2
	 */
	public boolean isMezzanineAsset() {
		return this.assetClass() == ASSET_MEZZANINE;
	}

	/**
	 * Ensure asset is media or mezzanine.
	 *
	 * @throws IllegalStateException asset is not media or mezzanine
	 */
	private void checkPlaybackAsset() {
		int cls = this.assetClass();

		Preconditions.checkState(cls == ASSET_MEDIA || cls == ASSET_MEZZANINE);
	}

	/**
	 * Playback media asset identifier, unique to ad.
	 *
	 * @return asset identifier
	 * @throws IllegalStateException asset is not {@linkplain #isMediaAsset() media} or
	 * {@linkplain #isMezzanineAsset() mezzanine} asset
	 * @since 1.2
	 * @see #isMediaAsset()
	 * @see #isMezzanineAsset()
	 */
	public String playbackId() {
		this.checkPlaybackAsset();
		return this.playbackIdOrClosedCaptionLanguageCode;
	}

	/**
	 * Name of codec used to encode playback asset.
	 *
	 * @return codec name
	 * @throws IllegalStateException asset is not {@linkplain #isMediaAsset() media} or
	 * {@linkplain #isMezzanineAsset() mezzanine} asset
	 * @since 1.2
	 * @see #isMediaAsset()
	 * @see #isMezzanineAsset()
	 */
	public String playbackCodec() {
		this.checkPlaybackAsset();
		return this.playbackCodec;
	}

	/**
	 * Average bit rate, in Kbps, of playback asset.
	 *
	 * @return average bit rate
	 * @throws IllegalStateException asset is not {@linkplain #isMediaAsset() media} or
	 * {@linkplain #isMezzanineAsset() mezzanine} asset
	 * @since 1.2
	 * @see #isMediaAsset()
	 * @see #isMezzanineAsset()
	 * @see #playbackMaxBitRateKbps()
	 * @see #playbackMinBitRateKbps()
	 */
	public int playbackAverageBitRateKbps() {
		this.checkPlaybackAsset();
		return this.playbackAverageBitRateKbps;
	}

	/**
	 * Minimum bit rate, in Kbps, of playback asset.
	 *
	 * @return minimum bit rate
	 * @throws IllegalStateException asset is not {@linkplain #isMediaAsset() media} or
	 * {@linkplain #isMezzanineAsset() mezzanine} asset
	 * @since 1.2
	 * @see #isMediaAsset()
	 * @see #isMezzanineAsset()
	 * @see #playbackAverageBitRateKbps()
	 * @see #playbackMaxBitRateKbps()
	 */
	public int playbackMinBitRateKbps() {
		this.checkPlaybackAsset();
		return this.playbackMinBitRateKbps;
	}

	/**
	 * Maximum bit rate, in Kbps, of playback asset.
	 *
	 * @return minimum bit rate
	 * @throws IllegalStateException asset is not {@linkplain #isMediaAsset() media} or
	 * {@linkplain #isMezzanineAsset() mezzanine} asset
	 * @since 1.2
	 * @see #isMediaAsset()
	 * @see #isMezzanineAsset()
	 * @see #playbackAverageBitRateKbps()
	 * @see #playbackMinBitRateKbps()
	 */
	public int playbackMaxBitRateKbps() {
		this.checkPlaybackAsset();
		return this.playbackMaxBitRateKbps;
	}

	/**
	 * Delivery method supported for playback asset.
	 *
	 * @return supported delivery method
	 * @throws IllegalStateException asset is not {@linkplain #isMediaAsset() media} or
	 * {@linkplain #isMezzanineAsset() mezzanine} asset
	 * @since 1.2
	 * @see #isMediaAsset()
	 * @see #isMezzanineAsset()
	 */
	public @PlaybackDeliveryMethod int playbackSupportedDelivery() {
		this.checkPlaybackAsset();
		return this.playbackSupportedDeliveryOrInteractiveRequiredApi;
	}

	/**
	 * Exact width, in pixels, of playback asset.
	 *
	 * @return asset width
	 * @throws IllegalStateException asset is not {@linkplain #isMediaAsset() media} or
	 * {@linkplain #isMezzanineAsset() mezzanine} asset
	 * @since 1.2
	 * @see #isMediaAsset()
	 * @see #isMezzanineAsset()
	 * @see #playbackHeightPx()
	 */
	public @Px int playbackWidthPx() {
		this.checkPlaybackAsset();
		return this.playbackWidthPx;
	}

	/**
	 * Exact height, in pixels, of playback asset.
	 *
	 * @return asset height
	 * @throws IllegalStateException asset is not {@linkplain #isMediaAsset() media} or
	 * {@linkplain #isMezzanineAsset() mezzanine} asset
	 * @since 1.2
	 * @see #isMediaAsset()
	 * @see #isMezzanineAsset()
	 * @see #playbackWidthPx()
	 */
	public @Px int playbackHeightPx() {
		this.checkPlaybackAsset();
		return this.playbackHeightPx;
	}

	/**
	 * Playback asset can scale to different dimensions.
	 *
	 * @return {@code true} if, and only if, asset can be scaled
	 * @throws IllegalStateException asset is not {@linkplain #isMediaAsset() media} or
	 * {@linkplain #isMezzanineAsset() mezzanine} asset
	 * @since 1.2
	 * @see #isMediaAsset()
	 * @see #isMezzanineAsset()
	 */
	public boolean playbackCanScale() {
		this.checkPlaybackAsset();
		return (this.flagsAndAssetClass & FLAG_PLAYBACK_SCALE) != 0;
	}

	/**
	 * Aspect ratio of playback asset must be maintained when scaled to different dimensions.
	 *
	 * @return {@code true} if, and only if, asset aspect ratio must be maintained
	 * @throws IllegalStateException asset is not {@linkplain #isMediaAsset() media} or
	 * {@linkplain #isMezzanineAsset() mezzanine} asset
	 * @since 1.2
	 * @see #isMediaAsset()
	 * @see #isMezzanineAsset()
	 */
	public boolean playbackMaintainAspectRatio() {
		this.checkPlaybackAsset();
		return (this.flagsAndAssetClass & FLAG_PLAYBACK_ASPECT) != 0;
	}

	/**
	 * Test whether asset is closed caption media.
	 *
	 * @return {@code true} if, and only if, closed caption media
	 * @since 1.2
	 */
	public boolean isClosedCaptionAsset() {
		return this.assetClass() == ASSET_CLOSED_CAPTION;
	}

	/**
	 * ISO 631-1 language code of closed caption media.
	 *
	 * @return language code
	 * @throws IllegalStateException asset is not {@linkplain #isClosedCaptionAsset() closed
	 * caption} media
	 * @since 1.2
	 * @see #isClosedCaptionAsset()
	 */
	public String closedCaptionLanguageCode() {
		this.checkAssetClass(ASSET_CLOSED_CAPTION);
		return this.playbackIdOrClosedCaptionLanguageCode;
	}

	@Override
	public void toProtobuf(ProtobufWriter writer) {
		writer.writeString(URL, this.url);
		writer.writeString(MIME, this.mime);

		long cookie;

		switch (this.assetClass()) {
		case ASSET_CLOSED_CAPTION:
			cookie = writer.beginWriteLen(CAPTION);
			writer.writeString(CAPTION_LANG, this.playbackIdOrClosedCaptionLanguageCode);
			break;
		case ASSET_INTERACTIVE:
			cookie = writer.beginWriteLen(INTERACTIVE);
			writer.writeInt32(
				INTERACTIVE_API,
				this.playbackSupportedDeliveryOrInteractiveRequiredApi
			);
			writer.writeBool(INTERACTIVE_VARDUR, this.interactiveCanExtendPlaybackDuration());
			break;
		case ASSET_MEDIA:
		case ASSET_MEZZANINE:
			cookie = writer.beginWriteLen(this.isMediaAsset() ? MEDIA : MEZZANINE);
			writer.writeString(MEDIA_ID, this.playbackIdOrClosedCaptionLanguageCode);
			writer.writeString(MEDIA_CODEC, this.playbackCodec);
			writer.writeInt32(MEDIA_AVGBITR, this.playbackAverageBitRateKbps);
			writer.writeInt32(MEDIA_MINBITR, this.playbackMinBitRateKbps);
			writer.writeInt32(MEDIA_MAXBITR, this.playbackMaxBitRateKbps);
			writer.writeInt32(
				MEDIA_DELIVERY,
				this.playbackSupportedDeliveryOrInteractiveRequiredApi
			);
			writer.writeInt32(MEDIA_W, this.playbackWidthPx);
			writer.writeInt32(MEDIA_H, this.playbackHeightPx);
			writer.writeBool(MEDIA_SCALE, this.playbackCanScale());
			writer.writeBool(MEDIA_ASPECT, this.playbackMaintainAspectRatio());
			break;
		default:
			return;
		}
		writer.endWriteLen(cookie);
	}
}
