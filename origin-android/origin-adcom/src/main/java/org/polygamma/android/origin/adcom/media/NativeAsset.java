// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.adcom.media;

import static org.polygamma.android.origin.protobuf.Protobuf.WIRE_LEN;
import static org.polygamma.android.origin.protobuf.Protobuf.WIRE_VARINT;
import static org.polygamma.android.origin.protobuf.Protobuf.fieldTagOf;

import android.annotation.SuppressLint;

import androidx.annotation.Dimension;
import androidx.annotation.IntDef;
import androidx.annotation.IntRange;

import org.polygamma.android.origin.adcom.enums.AdComEnums;
import org.polygamma.android.origin.adcom.enums.NativeDataAssetType;
import org.polygamma.android.origin.adcom.enums.NativeImageAssetType;
import org.polygamma.android.origin.protobuf.Protobuf.FieldTag;
import org.polygamma.android.origin.protobuf.ProtobufDecoder;
import org.polygamma.android.origin.protobuf.ProtobufEncoder;
import org.polygamma.android.origin.protobuf.ProtobufSerializable;
import org.polygamma.android.origin.util.Preconditions;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Native creative asset.
 *
 * @since 1.2
 * @see <a href="https://github.com/InteractiveAdvertisingBureau/AdCOM/blob/main/AdCOM%20v1.0%20FINAL.md#object--asset-">AdCOM, version 1.0 - Object: Asset</a>
 * @see <a href="https://github.com/InteractiveAdvertisingBureau/AdCOM/blob/main/AdCOM%20v1.0%20FINAL.md#object--titleasset-">AdCOM, version 1.0 - Object: TitleAsset</a>
 * @see <a href="https://github.com/InteractiveAdvertisingBureau/AdCOM/blob/main/AdCOM%20v1.0%20FINAL.md#object--imageasset-">AdCOM, version 1.0 - Object: ImageAsset</a>
 * @see <a href="https://github.com/InteractiveAdvertisingBureau/AdCOM/blob/main/AdCOM%20v1.0%20FINAL.md#object--videoasset-">AdCOM, version 1.0 - Object: VideoAsset</a>
 * @see <a href="https://github.com/InteractiveAdvertisingBureau/AdCOM/blob/main/AdCOM%20v1.0%20FINAL.md#object--titleasset-">AdCOM, version 1.0 - Object: DataAsset</a>
 */
public class NativeAsset implements ProtobufSerializable {

	private static final @FieldTag int ID				= fieldTagOf(  1, WIRE_VARINT);
	private static final @FieldTag int REQ				= fieldTagOf(  2, WIRE_VARINT);
	private static final @FieldTag int TITLE			= fieldTagOf(  3, WIRE_LEN);
	private static final @FieldTag int IMAGE			= fieldTagOf(  4, WIRE_LEN);
	private static final @FieldTag int VIDEO			= fieldTagOf(  5, WIRE_LEN);
	private static final @FieldTag int DATA				= fieldTagOf(  6, WIRE_LEN);
	private static final @FieldTag int LINK				= fieldTagOf(  7, WIRE_LEN);

	// `NativeTitleAsset`
	private static final @FieldTag int TITLE_TEXT		= fieldTagOf(  1, WIRE_LEN);

	// `NativeDataAsset`
	private static final @FieldTag int DATA_VALUE		= fieldTagOf(  1, WIRE_LEN);
	private static final @FieldTag int DATA_TYPE		= fieldTagOf(  3, WIRE_VARINT);

	// `NativeImageAsset`
	private static final @FieldTag int IMAGE_URL		= fieldTagOf(  1, WIRE_LEN);
	private static final @FieldTag int IMAGE_W			= fieldTagOf(  2, WIRE_VARINT);
	private static final @FieldTag int IMAGE_H			= fieldTagOf(  3, WIRE_VARINT);
	private static final @FieldTag int IMAGE_TYPE		= fieldTagOf(  4, WIRE_VARINT);

	// `NativeVideoAsset`
	/*private static final @FieldTag int VIDEO_ADM		= fieldTagOf(  1, WIRE_LEN);*/
	/*private static final @FieldTag int VIDEO_CURL		= fieldTagOf(  2, WIRE_LEN);*/
	private static final @FieldTag int VIDEO_PLAYBACK	= fieldTagOf(500, WIRE_LEN);

	/**
	 * Flag set in {@link #idAndRequired} to indicate asset is required.
	 */
	private static final int REQUIRED_MASK = 1 << 31;

	/**
	 * Title asset class.
	 */
	private static final int ASSET_TITLE		= 0x10000000;

	/**
	 * Data asset class.
	 */
	private static final int ASSET_DATA			= 0x20000000;

	/**
	 * Image asset class.
	 */
	private static final int ASSET_IMAGE		= 0x40000000;

	/**
	 * Video asset class.
	 */
	private static final int ASSET_VIDEO		= 0x80000000;

	/**
	 * Mask of asset classes.
	 */
	private static final int ASSET_CLASS_MASK	= 0xf0000000;

	/**
	 * Asset class enumeration value marker.
	 */
	@Retention(RetentionPolicy.SOURCE)
	@Target(ElementType.TYPE_USE)
	@IntDef({ASSET_DATA, ASSET_IMAGE, ASSET_TITLE, ASSET_VIDEO})
	private @interface AssetClass {
	}

	private static NativeAsset
	of(@AssetClass int cls, @IntRange(from = 0) int id, boolean req, LinkAsset link) {
		NativeAsset rv = new NativeAsset();

		rv.setAssetClass(cls);
		rv.setId(id);
		rv.setRequired(req);
		rv.link = link;
		return rv;
	}

	/**
	 * Construct new {@linkplain #isTitleAsset() title} text asset.
	 *
	 * @param id identifier of {@linkplain
	 * org.polygamma.android.origin.adcom.placement.NativeAssetFormat format} asset is for
	 * @param req {@code true} if, and only if, asset must be rendered
	 * @param link link to navigate user to when asset is activated (i.e. clicked)
	 * @param text title text
	 * @return title asset instance
	 * @throws IllegalArgumentException {@code id} is negative
	 * @since 1.2
	 * @see #isTitleAsset()
	 * @see #titleText()
	 */
	public static NativeAsset
	ofTitleAsset(@IntRange(from = 0) int id, boolean req, LinkAsset link, String text) {
		NativeAsset asset = of(ASSET_TITLE, id, req, link);

		asset.data = text;
		return asset;
	}

	/**
	 * Construct new {@linkplain #isDataAsset() data} text value asset.
	 *
	 * @param id identifier of {@linkplain
	 * org.polygamma.android.origin.adcom.placement.NativeAssetFormat format} asset is for
	 * @param req {@code true} if, and only if, asset must be rendered
	 * @param link link to navigate user to when asset is activated (i.e. clicked)
	 * @param type data value type
	 * @param value data value text
	 * @return data asset instance
	 * @throws IllegalArgumentException {@code id} is negative
	 * @since 1.2
	 * @see #isDataAsset()
	 * @see #dataAssetType()
	 * @see #dataValue()
	 */
	public static NativeAsset ofDataAsset(
		@IntRange(from = 0) int id,
		boolean req,
		LinkAsset link,
		@NativeDataAssetType int type,
		String value
	) {
		NativeAsset asset = of(ASSET_DATA, id, req, link);

		asset.setType(type);
		asset.data = value;
		return asset;
	}

	/**
	 * Construct new {@linkplain #isImageAsset() image} media asset.
	 *
	 * @param id identifier of {@linkplain
	 * org.polygamma.android.origin.adcom.placement.NativeAssetFormat format} asset is for
	 * @param req {@code true} if, and only if, asset must be rendered
	 * @param link link to navigate user to when asset is activated (i.e. clicked)
	 * @param type image asset type
	 * @param w exact width, in device independent pixels, of image
	 * @param h exact height, in device independent pixels, of image
	 * @param url image URL
	 * @return image media asset instance
	 * @throws IllegalArgumentException {@code id} is negative
	 * @since 1.2
	 * @see #isImageAsset()
	 * @see #imageAssetType()
	 * @see #imageWidthDp()
	 * @see #imageHeightDp()
	 * @see #imageUrl()
	 */
	public static NativeAsset ofImageAsset(
		@IntRange(from = 0) int id,
		boolean req,
		LinkAsset link,
		@NativeImageAssetType int type,
		@Dimension(unit = Dimension.DP) int w,
		@Dimension(unit = Dimension.DP) int h,
		String url
	) {
		NativeAsset rv = of(ASSET_IMAGE, id, req, link);

		rv.setType(type);
		rv.widthDp = w;
		rv.heightDp = h;
		rv.data = url;
		return rv;
	}

	/**
	 * Construct new {@linkplain #isVideoAsset() video} media asset.
	 *
	 * @param id identifier of {@linkplain
	 * org.polygamma.android.origin.adcom.placement.NativeAssetFormat format} asset is for
	 * @param req {@code true} if, and only if, asset must be rendered
	 * @param link link to navigate user to when asset is activated (i.e. clicked)
	 * @param video video media of asset
	 * @return video media asset instance
	 * @throws IllegalArgumentException {@code id} is negative
	 * @since 1.2
	 * @see #isVideoAsset()
	 * @see #video()
	 */
	public static NativeAsset ofVideoAsset(
		@IntRange(from = 0) int id,
		boolean req,
		LinkAsset link,
		PlaybackAd video
	) {
		NativeAsset asset = of(ASSET_VIDEO, id, req, link);

		asset.data = video;
		return asset;
	}

	/**
	 * Deserialize asset from Protobuf message.
	 *
	 * @param dec decoder to deserialize from
	 * @return deserialized asset
	 * @throws RuntimeException coding is malformed
	 * @since 1.2
	 */
	public static NativeAsset ofProtobuf(ProtobufDecoder dec) {
		NativeAsset rv = new NativeAsset();

		while (dec.hasRemaining()) {
			int tag = dec.decodeFieldTag();

			if (tag == ID)
				rv.setId(dec.decodeUint32());
			else if (tag == REQ)
				rv.setRequired(dec.decodeBool());
			else if (tag == TITLE)
				dec.decodeLen(rv, NativeAsset::mergeTitleAssetProtobuf);
			else if (tag == IMAGE)
				dec.decodeLen(rv, NativeAsset::mergeImageAssetProtobuf);
			else if (tag == VIDEO)
				dec.decodeLen(rv, NativeAsset::mergeVideoAssetProtobuf);
			else if (tag == DATA)
				dec.decodeLen(rv, NativeAsset::mergeDataAssetProtobuf);
			else if (tag == LINK)
				rv.link = dec.decodeLen(LinkAsset::ofProtobuf);
			else
				dec.skipFieldValue(tag);
		}
		return rv;
	}

	// Low-order 30 bits are format id, while 31st bit is set only if asset is required.
	private int idAndRequired;

	// Image or data asset type, followed by `ASSET_CLASS_MASK` for asset class.
	private int typeAndClass;

	// Image width, in dips.
	private int widthDp;

	// Image width, in dips.
	private int heightDp;

	// Image URL, data/title text, or `PlaybackAd`.
	private Object data;

	// Navigation link.
	private LinkAsset link;

	private NativeAsset() {
		this.link = LinkAsset.of();
	}

	// Deserialize title asset.
	private NativeAsset mergeTitleAssetProtobuf(ProtobufDecoder src) {
		this.data = "";
		this.setAssetClass(ASSET_TITLE);

		while (src.hasRemaining()) {
			int tag = src.decodeFieldTag();

			if (tag == TITLE_TEXT)
				this.data = src.decodeString();
			else
				src.skipFieldValue(tag);
		}
		return this;
	}

	// Deserialize data asset.
	private NativeAsset mergeDataAssetProtobuf(ProtobufDecoder src) {
		this.data = "";
		this.setType(AdComEnums.NativeDataAssetUnknown);
		this.setAssetClass(ASSET_DATA);

		while (src.hasRemaining()) {
			int tag = src.decodeFieldTag();

			if (tag == DATA_VALUE) {
				this.data = src.decodeString();
			} else if (tag == DATA_TYPE) {
				int type = src.decodeUint32();

				if (type >= 0 && type <= AdComEnums.MAX_NATIVE_DATA_ASSET_TYPE)
					this.setType(type);
			} else {
				src.skipFieldValue(tag);
			}
		}
		return this;
	}

	// Deserialize image asset.
	private NativeAsset mergeImageAssetProtobuf(ProtobufDecoder src) {
		this.widthDp = 0;
		this.heightDp = 0;
		this.data = "";
		this.setType(AdComEnums.NativeImageAssetUnknown);
		this.setAssetClass(ASSET_IMAGE);

		while (src.hasRemaining()) {
			int tag = src.decodeFieldTag();

			if (tag == IMAGE_URL) {
				this.data = src.decodeString();
			} else if (tag == IMAGE_W) {
				this.widthDp = src.decodeUint32();
			} else if (tag == IMAGE_H) {
				this.heightDp = src.decodeUint32();
			} else if (tag == IMAGE_TYPE) {
				int type = src.decodeUint32();

				if (type >= 0 && type <= AdComEnums.MAX_NATIVE_IMAGE_ASSET_TYPE)
					this.setType(type);
			} else {
				src.skipFieldValue(tag);
			}
		}
		return this;
	}

	// Deserialize video asset.
	private NativeAsset mergeVideoAssetProtobuf(ProtobufDecoder src) {
		this.data = PlaybackAd.ofVideoAd();
		this.setAssetClass(ASSET_VIDEO);

		while (src.hasRemaining()) {
			int tag = src.decodeFieldTag();

			if (tag == VIDEO_PLAYBACK)
				this.data = src.decodeLen(PlaybackAd::ofPlaybackAdProtobuf);
			else
				src.skipFieldValue(tag);
		}
		return this;
	}

	/**
	 * Set identifier of {@linkplain org.polygamma.android.origin.adcom.placement.NativeAssetFormat
	 * format} asset is for.
	 *
	 * @param id asset format identifier
	 */
	private void setId(@IntRange(from = 0) int id) {
		Preconditions.checkArgument(id >= 0, "id cannot be negative");
		this.idAndRequired = id | (this.idAndRequired & REQUIRED_MASK);
	}

	/**
	 * Identifier of {@linkplain org.polygamma.android.origin.adcom.placement.NativeAssetFormat
	 * format} asset is for.
	 *
	 * @return asset format identifier
	 * @since 1.2
	 * @see org.polygamma.android.origin.adcom.placement.NativeAssetFormat#id()
	 */
	public @IntRange(from = 0) int id() {
		return this.idAndRequired & ~REQUIRED_MASK;
	}

	/**
	 * Set whether asset must be rendered.
	 *
	 * @param req {@code true} if, and only if, asset must be rendered
	 */
	private void setRequired(boolean req) {
		if (req)
			this.idAndRequired |= REQUIRED_MASK;
		else
			this.idAndRequired &= ~REQUIRED_MASK;
	}

	/**
	 * Asset must be rendered.
	 *
	 * @return {@code true} if, and only if, asset must be rendered
	 * @since 1.2
	 */
	public boolean required() {
		return (this.idAndRequired & REQUIRED_MASK) != 0;
	}

	/**
	 * Set asset value type.
	 *
	 * @param type asset value type
	 * @throws IllegalArgumentException {@code type} overflows
	 */
	private void setType(int type) {
		Preconditions.checkArgument((type & ~ASSET_CLASS_MASK) == type, "type overflow");
		this.typeAndClass = type | (this.typeAndClass & ASSET_CLASS_MASK);
	}

	/**
	 * Set asset class.
	 *
	 * @param cls asset class
	 */
	private void setAssetClass(@AssetClass int cls) {
		assert (cls & ASSET_CLASS_MASK) == cls;
		this.typeAndClass = (this.typeAndClass & ~ASSET_CLASS_MASK) | cls;
	}

	/**
	 * Test whether asset is of certain class.
	 *
	 * @param cls class to test
	 * @return {@code true} if, and only if, asset is of class {@code cls}
	 */
	private boolean isAssetClass(@AssetClass int cls) {
		return (this.typeAndClass & cls) != 0;
	}

	/**
	 * Ensure asset is of certain class.
	 *
	 * @param cls class to ensure
	 * @throws IllegalStateException asset is not of {@code cls} class
	 */
	private void checkAssetClass(@AssetClass int cls) {
		Preconditions.checkState(this.isAssetClass(cls));
	}

	/**
	 * Test whether asset is title text asset.
	 *
	 * @return {@code true} if, and only if, title text asset
	 * @since 1.2
	 * @see #ofTitleAsset(int, boolean, LinkAsset, String)
	 * @see #titleText()
	 */
	public boolean isTitleAsset() {
		return this.isAssetClass(ASSET_TITLE);
	}

	/**
	 * Title asset text.
	 *
	 * @return title text
	 * @throws IllegalStateException asset is not a {@linkplain #isTitleAsset() title} asset
	 * @since 1.2
	 * @see #ofTitleAsset(int, boolean, LinkAsset, String)
	 * @see #isTitleAsset()
	 */
	public String titleText() {
		this.checkAssetClass(ASSET_TITLE);
		return (String) this.data;
	}

	/**
	 * Test whether asset is a data text value asset.
	 *
	 * @return {@code true} if, and only if, data text value asset
	 * @since 1.2
	 * @see #ofDataAsset(int, boolean, LinkAsset, int, String)
	 * @see #dataAssetType()
	 * @see #dataValue()
	 */
	public boolean isDataAsset() {
		return this.isAssetClass(ASSET_DATA);
	}

	/**
	 * Data asset value type.
	 *
	 * @return data asset type
	 * @throws IllegalStateException asset is not a {@linkplain #isDataAsset() data} asset
	 * @since 1.2
	 * @see #ofDataAsset(int, boolean, LinkAsset, int, String)
	 * @see #isDataAsset()
	 * @see #dataValue()
	 */
	@SuppressLint("WrongConstant")
	public @NativeDataAssetType int dataAssetType() {
		this.checkAssetClass(ASSET_DATA);
		return this.typeAndClass & ~ASSET_CLASS_MASK;
	}

	/**
	 * Data asset text value.
	 *
	 * @return data text value
	 * @throws IllegalStateException asset is not a {@linkplain #isDataAsset() data} asset
	 * @since 1.2
	 * @see #ofDataAsset(int, boolean, LinkAsset, int, String)
	 * @see #isDataAsset()
	 * @see #dataAssetType()
	 */
	public String dataValue() {
		this.checkAssetClass(ASSET_DATA);
		return (String) this.data;
	}

	/**
	 * Test whether asset is an image media asset.
	 *
	 * @return {@code true} if, and only if, image media asset
	 * @since 1.2
	 * @see #ofImageAsset(int, boolean, LinkAsset, int, int, int, String)
	 * @see #imageAssetType()
	 * @see #imageWidthDp()
	 * @see #imageHeightDp()
	 * @see #imageUrl()
	 */
	public boolean isImageAsset() {
		return this.isAssetClass(ASSET_IMAGE);
	}

	/**
	 * Image asset type.
	 *
	 * @return asset type
	 * @throws IllegalStateException asset is not a {@linkplain #isImageAsset() image} asset
	 * @since 1.2
	 * @see #ofImageAsset(int, boolean, LinkAsset, int, int, int, String)
	 * @see #isImageAsset()
	 */
	@SuppressLint("WrongConstant")
	public @NativeImageAssetType int imageAssetType() {
		this.checkAssetClass(ASSET_IMAGE);
		return this.typeAndClass & ~ASSET_CLASS_MASK;
	}

	/**
	 * Exact width, in device independent pixels, of image asset.
	 *
	 * @return image width
	 * @throws IllegalStateException asset is not a {@linkplain #isImageAsset() image} asset
	 * @since 1.2
	 * @see #ofImageAsset(int, boolean, LinkAsset, int, int, int, String)
	 * @see #isImageAsset()
	 * @see #imageHeightDp()
	 */
	public @Dimension(unit = Dimension.DP) int imageWidthDp() {
		this.checkAssetClass(ASSET_IMAGE);
		return this.widthDp;
	}

	/**
	 * Exact height, in device independent pixels, of image asset.
	 *
	 * @return image height
	 * @throws IllegalStateException asset is not a {@linkplain #isImageAsset() image} asset
	 * @since 1.2
	 * @see #ofImageAsset(int, boolean, LinkAsset, int, int, int, String)
	 * @see #isImageAsset()
	 * @see #imageWidthDp()
	 */
	public @Dimension(unit = Dimension.DP) int imageHeightDp() {
		this.checkAssetClass(ASSET_IMAGE);
		return this.heightDp;
	}

	/**
	 * Image asset URL.
	 *
	 * @return image URL
	 * @throws IllegalStateException asset is not a {@linkplain #isImageAsset() image} asset
	 * @since 1.2
	 * @see #ofImageAsset(int, boolean, LinkAsset, int, int, int, String)
	 * @see #isImageAsset()
	 */
	public String imageUrl() {
		this.checkAssetClass(ASSET_IMAGE);
		return (String) this.data;
	}

	/**
	 * Test whether asset is video media.
	 *
	 * @return {@code true} if, and only if, video media asset
	 * @since 1.2
	 * @see #ofVideoAsset(int, boolean, LinkAsset, PlaybackAd)
	 * @see #video()
	 */
	public boolean isVideoAsset() {
		return this.isAssetClass(ASSET_VIDEO);
	}

	/**
	 * Video asset media.
	 *
	 * @return video media
	 * @throws IllegalStateException asset is not a {@linkplain #isVideoAsset() video} asset
	 * @since 1.2
	 * @see #ofVideoAsset(int, boolean, LinkAsset, PlaybackAd)
	 * @see #isVideoAsset()
	 */
	public PlaybackAd video() {
		this.checkAssetClass(ASSET_VIDEO);
		return (PlaybackAd) this.data;
	}

	/**
	 * Link to navigate user to when asset is activated (i.e. clicked).
	 *
	 * @return navigation link asset
	 * @since 1.2
	 * @see DisplayAd#link()
	 */
	public LinkAsset link() {
		return this.link;
	}

	@Override
	public void toProtobuf(ProtobufEncoder enc) {
		enc.encodeUnsignedIntField(ID, this.id())
			.encodeBoolField(REQ, this.required())
			.encodeMessageField(LINK, this.link);

		if (this.isTitleAsset()) {
			enc.encodeLenField(
				TITLE, this.titleText(),
				(title, titleEnc) -> titleEnc.encodeStringField(TITLE_TEXT, title)
			);
		} else if (this.isDataAsset()) {
			enc.encodeLenField(
				DATA, this,
				(data, dataEnc) -> dataEnc.encodeStringField(DATA_VALUE, data.dataValue())
					.encodeUnsignedIntField(DATA_TYPE, data.dataAssetType())
			);
		} else if (this.isImageAsset()) {
			enc.encodeLenField(
				IMAGE, this,
				(img, imgEnc) -> imgEnc.encodeStringField(IMAGE_URL, img.imageUrl())
					.encodeUnsignedIntField(IMAGE_W, img.imageWidthDp())
					.encodeUnsignedIntField(IMAGE_H, img.imageHeightDp())
					.encodeUnsignedIntField(IMAGE_TYPE, img.imageAssetType())
			);
		} else if (this.isVideoAsset()) {
			enc.encodeLenField(VIDEO, this.video(), (vid, vidEnc) -> vidEnc.encodeLenField(
				VIDEO_PLAYBACK,
				vid,
				PlaybackAd::toPlaybackAdProtobuf
			));
		}
	}
}
