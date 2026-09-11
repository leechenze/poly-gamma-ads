// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.adcom.media;

import static org.polygamma.android.origin.protobuf.Protobuf.WIRE_LEN;
import static org.polygamma.android.origin.protobuf.Protobuf.WIRE_VARINT;
import static org.polygamma.android.origin.protobuf.Protobuf.fieldTagOf;

import android.annotation.SuppressLint;

import androidx.annotation.IntDef;
import androidx.annotation.Px;

import org.polygamma.android.origin.adcom.enums.AdApiCode;
import org.polygamma.android.origin.adcom.enums.PlaybackDeliveryMethod;
import org.polygamma.android.origin.protobuf.Protobuf.FieldTag;
import org.polygamma.android.origin.protobuf.ProtobufDecoder;
import org.polygamma.android.origin.protobuf.ProtobufEncoder;
import org.polygamma.android.origin.protobuf.ProtobufSerializable;
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

	private static final @FieldTag int URL					= fieldTagOf( 1, WIRE_LEN);
	private static final @FieldTag int MIME					= fieldTagOf( 2, WIRE_LEN);
	private static final @FieldTag int INTERACTIVE			= fieldTagOf( 3, WIRE_LEN);
	private static final @FieldTag int MEDIA				= fieldTagOf( 4, WIRE_LEN);
	private static final @FieldTag int MEZZANINE			= fieldTagOf( 5, WIRE_LEN);
	private static final @FieldTag int CAPTION				= fieldTagOf( 6, WIRE_LEN);

	// `LinearClosedCaptionAsset`
	private static final @FieldTag int CAPTION_LANG			= fieldTagOf( 1, WIRE_LEN);

	// `LinearMediaAsset`
	private static final @FieldTag int MEDIA_ID				= fieldTagOf( 1, WIRE_LEN);
	private static final @FieldTag int MEDIA_CODEC			= fieldTagOf( 2, WIRE_LEN);
	/*private static final @FieldTag int MEDIA_SIZE			= fieldTagOf( 3, WIRE_VARINT);*/
	private static final @FieldTag int MEDIA_AVGBITR		= fieldTagOf( 4, WIRE_VARINT);
	private static final @FieldTag int MEDIA_MINBITR		= fieldTagOf( 5, WIRE_VARINT);
	private static final @FieldTag int MEDIA_MAXBITR		= fieldTagOf( 6, WIRE_VARINT);
	private static final @FieldTag int MEDIA_DELIVERY		= fieldTagOf( 7, WIRE_VARINT);
	private static final @FieldTag int MEDIA_W				= fieldTagOf( 8, WIRE_VARINT);
	private static final @FieldTag int MEDIA_H				= fieldTagOf( 9, WIRE_VARINT);
	private static final @FieldTag int MEDIA_SCALE			= fieldTagOf(10, WIRE_VARINT);
	private static final @FieldTag int MEDIA_ASPECT			= fieldTagOf(11, WIRE_VARINT);

	// `LinearInteractiveAsset`
	private static final @FieldTag int INTERACTIVE_API		= fieldTagOf( 1, WIRE_VARINT);
	private static final @FieldTag int INTERACTIVE_VARDUR	= fieldTagOf( 2, WIRE_VARINT);

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
	 * @param dec decoder to deserialize from
	 * @return deserialized asset
	 * @throws RuntimeException coding is malformed
	 * @since 1.2
	 */
	public static LinearAsset ofProtobuf(ProtobufDecoder dec) {
		LinearAsset rv = new LinearAsset();

		while (dec.hasRemaining()) {
			int tag = dec.decodeFieldTag();

			if (tag == URL) {
				rv.url = dec.decodeString();
			} else if (tag == MIME) {
				rv.mime = dec.decodeString();
			} else if (tag == CAPTION) {
				dec.decodeLen(rv, LinearAsset::mergeCaptionProtobuf);
			} else if (tag == INTERACTIVE) {
				dec.decodeLen(rv, LinearAsset::mergeInteractiveProtobuf);
			} else if (tag == MEDIA || tag == MEZZANINE) {
				rv.flagsAndAssetClass = tag == MEDIA ? ASSET_MEDIA : ASSET_MEZZANINE;
				dec.decodeLen(rv, LinearAsset::mergeMediaOrMezzanineProtobuf);
			} else {
				dec.skipFieldValue(tag);
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

	private LinearAsset mergeCaptionProtobuf(ProtobufDecoder dec) {
		this.flagsAndAssetClass = ASSET_CLOSED_CAPTION;
		while (dec.hasRemaining()) {
			int tag = dec.decodeFieldTag();

			if (tag == CAPTION_LANG)
				this.playbackIdOrClosedCaptionLanguageCode = dec.decodeString();
			else
				dec.skipFieldValue(tag);
		}
		return this;
	}

	private LinearAsset mergeInteractiveProtobuf(ProtobufDecoder dec) {
		this.flagsAndAssetClass = ASSET_INTERACTIVE;
		while (dec.hasRemaining()) {
			int tag = dec.decodeFieldTag();

			if (tag == INTERACTIVE_API)
				this.playbackSupportedDeliveryOrInteractiveRequiredApi = dec.decodeUint32();
			else if (tag == INTERACTIVE_VARDUR)
				this.flagsAndAssetClass |= dec.decodeBool() ? FLAG_INTERACTIVE_VARDUR : 0;
			else
				dec.skipFieldValue(tag);
		}
		return this;
	}

	private LinearAsset mergeMediaOrMezzanineProtobuf(ProtobufDecoder dec) {
		while (dec.hasRemaining()) {
			int tag = dec.decodeFieldTag();

			if (tag == MEDIA_ID)
				this.playbackIdOrClosedCaptionLanguageCode = dec.decodeString();
			else if (tag == MEDIA_CODEC)
				this.playbackCodec = dec.decodeString();
			else if (tag == MEDIA_AVGBITR)
				this.playbackAverageBitRateKbps = dec.decodeUint32();
			else if (tag == MEDIA_MINBITR)
				this.playbackMinBitRateKbps = dec.decodeUint32();
			else if (tag == MEDIA_MAXBITR)
				this.playbackMaxBitRateKbps = dec.decodeUint32();
			else if (tag == MEDIA_DELIVERY)
				this.playbackSupportedDeliveryOrInteractiveRequiredApi = dec.decodeUint32();
			else if (tag == MEDIA_W)
				this.playbackWidthPx = dec.decodeUint32();
			else if (tag == MEDIA_H)
				this.playbackHeightPx = dec.decodeUint32();
			else if (tag == MEDIA_SCALE)
				this.flagsAndAssetClass |= dec.decodeBool() ? FLAG_PLAYBACK_SCALE : 0;
			else if (tag == MEDIA_ASPECT)
				this.flagsAndAssetClass |= dec.decodeBool() ? FLAG_PLAYBACK_ASPECT : 0;
			else
				dec.skipFieldValue(tag);
		}
		return this;
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
	public void toProtobuf(ProtobufEncoder enc) {
		enc.encodeStringField(URL, this.url)
			.encodeStringField(MIME, this.mime);

		switch (this.assetClass()) {
		case ASSET_CLOSED_CAPTION:
			enc.encodeLenField(
				CAPTION,
				this.playbackIdOrClosedCaptionLanguageCode,
				(lang, langEnc) -> langEnc.encodeStringField(CAPTION_LANG, lang)
			);
			break;
		case ASSET_INTERACTIVE:
			enc.encodeLenField(
				INTERACTIVE, this,
				(inter, interEnc) ->
					interEnc.encodeUnsignedIntField(
						INTERACTIVE_API,
						inter.playbackSupportedDeliveryOrInteractiveRequiredApi
					)
						.encodeBoolField(
							INTERACTIVE_VARDUR,
							inter.interactiveCanExtendPlaybackDuration()
						)
			);
			break;
		case ASSET_MEDIA:
		case ASSET_MEZZANINE:
			enc.encodeLenField(
				this.isMediaAsset() ? MEDIA : MEZZANINE, this,
				(med, medEnc) ->
					medEnc.encodeStringField(MEDIA_ID, med.playbackIdOrClosedCaptionLanguageCode)
						.encodeStringField(MEDIA_CODEC, med.playbackCodec)
						.encodeUnsignedIntField(MEDIA_AVGBITR, med.playbackAverageBitRateKbps)
						.encodeUnsignedIntField(MEDIA_MINBITR, med.playbackMinBitRateKbps)
						.encodeUnsignedIntField(MEDIA_MAXBITR, med.playbackMaxBitRateKbps)
						.encodeUnsignedIntField(
							MEDIA_DELIVERY,
							med.playbackSupportedDeliveryOrInteractiveRequiredApi
						)
						.encodeUnsignedIntField(MEDIA_W, med.playbackWidthPx)
						.encodeUnsignedIntField(MEDIA_H, med.playbackHeightPx)
						.encodeBoolField(MEDIA_SCALE, med.playbackCanScale())
						.encodeBoolField(MEDIA_ASPECT, med.playbackMaintainAspectRatio())
			);
			break;
		default:
			break;
		}
	}
}
