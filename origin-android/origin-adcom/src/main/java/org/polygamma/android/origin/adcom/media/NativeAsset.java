// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.adcom.media;

import static org.polygamma.android.origin.protobuf.ProtobufField.*;

import android.annotation.SuppressLint;

import androidx.annotation.Dimension;
import androidx.annotation.IntDef;
import androidx.annotation.IntRange;

import org.polygamma.android.origin.adcom.enums.AdComEnums;
import org.polygamma.android.origin.adcom.enums.NativeDataAssetType;
import org.polygamma.android.origin.adcom.enums.NativeImageAssetType;
import org.polygamma.android.origin.protobuf.ProtobufReader;
import org.polygamma.android.origin.protobuf.ProtobufSerializable;
import org.polygamma.android.origin.protobuf.ProtobufWriter;
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

	private static final @Tag int ID				= ofInt32(    1);
	private static final @Tag int REQ				= ofBool(     2);
	private static final @Tag int TITLE				= ofMessage(  3);
	private static final @Tag int IMAGE				= ofMessage(  4);
	private static final @Tag int VIDEO				= ofMessage(  5);
	private static final @Tag int DATA				= ofMessage(  6);
	private static final @Tag int LINK				= ofMessage(  7);

	// `NativeTitleAsset`
	private static final @Tag int TITLE_TEXT		= ofString(   1);

	// `NativeDataAsset`
	private static final @Tag int DATA_VALUE		= ofString(   1);
	private static final @Tag int DATA_TYPE			= ofInt32(    3);

	// `NativeImageAsset`
	private static final @Tag int IMAGE_URL			= ofString(   1);
	private static final @Tag int IMAGE_W			= ofInt32(    2);
	private static final @Tag int IMAGE_H			= ofInt32(    3);
	private static final @Tag int IMAGE_TYPE		= ofInt32(    4);

	// `NativeVideoAsset`
	/*private static final @Tag int VIDEO_ADM		= ofString(   1);*/
	/*private static final @Tag int VIDEO_CURL		= ofString(   2);*/
	private static final @Tag int VIDEO_PLAYBACK	= ofMessage(500);

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
	 * Deserialize title asset.
	 *
	 * @param dst asset to deserialize into
	 * @param src reader to deserialize from
	 * @throws RuntimeException coding is malformed
	 */
	private static void deserializeTitleAsset(NativeAsset dst, ProtobufReader src) {
		dst.data = "";
		dst.setAssetClass(ASSET_TITLE);

		int cookie = src.beginReadLen();

		while (src.hasRemaining()) {
			int tag = src.readTag();

			if (tag == TITLE_TEXT)
				dst.data = src.readString();
		}
		src.endReadLen(cookie);
	}

	/**
	 * Deserialize data asset.
	 *
	 * @param dst asset to deserialize into
	 * @param src reader to deserialize from
	 * @throws RuntimeException coding is malformed
	 */
	private static void deserializeDataAsset(NativeAsset dst, ProtobufReader src) {
		dst.data = "";
		dst.setType(AdComEnums.NativeDataAssetUnknown);
		dst.setAssetClass(ASSET_DATA);

		int cookie = src.beginReadLen();

		while (src.hasRemaining()) {
			int tag = src.readTag();

			if (tag == DATA_VALUE) {
				dst.data = src.readString();
			} else if (tag == DATA_TYPE) {
				int type = src.readInt32();

				if (type >= 0 && type <= AdComEnums.MAX_NATIVE_DATA_ASSET_TYPE)
					dst.setType(type);
			}
		}
		src.endReadLen(cookie);
	}

	/**
	 * Deserialize image asset.
	 *
	 * @param dst asset to deserialize into
	 * @param src reader to deserialize from
	 * @throws RuntimeException coding is malformed
	 */
	private static void deserializeImageAsset(NativeAsset dst, ProtobufReader src) {
		dst.widthDp = 0;
		dst.heightDp = 0;
		dst.data = "";
		dst.setType(AdComEnums.NativeImageAssetUnknown);
		dst.setAssetClass(ASSET_IMAGE);

		int cookie = src.beginReadLen();

		while (src.hasRemaining()) {
			int tag = src.readTag();

			if (tag == IMAGE_URL) {
				dst.data = src.readString();
			} else if (tag == IMAGE_W) {
				dst.widthDp = src.readInt32();
			} else if (tag == IMAGE_H) {
				dst.heightDp = src.readInt32();
			} else if (tag == IMAGE_TYPE) {
				int type = src.readInt32();

				if (type >= 0 && type <= AdComEnums.MAX_NATIVE_IMAGE_ASSET_TYPE)
					dst.setType(type);
			}
		}
		src.endReadLen(cookie);
	}

	/**
	 * Deserialize video asset.
	 *
	 * @param dst asset to deserialize into
	 * @param src reader to deserialize from
	 * @throws RuntimeException coding is malformed
	 */
	private static void deserializeVideoAsset(NativeAsset dst, ProtobufReader src) {
		dst.data = PlaybackAd.ofVideoAd();
		dst.setAssetClass(ASSET_VIDEO);

		int cookie = src.beginReadLen();

		while (src.hasRemaining()) {
			int tag = src.readTag();

			if (tag == VIDEO_PLAYBACK)
				dst.data = src.readLen(PlaybackAd::ofPlaybackAdProtobuf);
		}
		src.endReadLen(cookie);
	}

	/**
	 * Deserialize asset from Protobuf message.
	 *
	 * @param reader reader to deserialize from
	 * @return deserialized asset
	 * @throws RuntimeException coding is malformed
	 * @since 1.2
	 */
	public static NativeAsset ofProtobuf(ProtobufReader reader) {
		NativeAsset rv = new NativeAsset();

		while (reader.hasRemaining()) {
			int tag = reader.readTag();

			if (tag == ID)
				rv.setId(reader.readInt32());
			else if (tag == REQ)
				rv.setRequired(reader.readBool());
			else if (tag == TITLE)
				deserializeTitleAsset(rv, reader);
			else if (tag == IMAGE)
				deserializeImageAsset(rv, reader);
			else if (tag == VIDEO)
				deserializeVideoAsset(rv, reader);
			else if (tag == DATA)
				deserializeDataAsset(rv, reader);
			else if (tag == LINK)
				rv.link = reader.readLen(LinkAsset::ofProtobuf);
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
	public void toProtobuf(ProtobufWriter writer) {
		writer.writeInt32(ID, this.id());
		writer.writeBool(REQ, this.required());
		writer.writeLen(LINK, this.link);

		long cookie;

		if (this.isTitleAsset()) {
			cookie = writer.beginWriteLen(TITLE);
			writer.writeString(TITLE_TEXT, this.titleText());
		} else if (this.isDataAsset()) {
			cookie = writer.beginWriteLen(DATA);
			writer.writeString(DATA_VALUE, this.dataValue());
			writer.writeInt32(DATA_TYPE, this.dataAssetType());
		} else if (this.isImageAsset()) {
			cookie = writer.beginWriteLen(IMAGE);
			writer.writeString(IMAGE_URL, this.imageUrl());
			writer.writeInt32(IMAGE_W, this.imageWidthDp());
			writer.writeInt32(IMAGE_H, this.imageHeightDp());
			writer.writeInt32(IMAGE_TYPE, this.imageAssetType());
		} else if (this.isVideoAsset()) {
			cookie = writer.beginWriteLen(VIDEO);
			writer.writeLen(VIDEO_PLAYBACK, this.video()::toPlaybackAdProtobuf);
		} else {
			return;
		}
		writer.endWriteLen(cookie);
	}
}
