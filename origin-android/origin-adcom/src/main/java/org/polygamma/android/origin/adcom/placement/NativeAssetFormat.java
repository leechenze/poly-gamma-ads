// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.adcom.placement;

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
import org.polygamma.android.origin.util.CollectionsCompat;
import org.polygamma.android.origin.util.Preconditions;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Native ad media asset format.
 *
 * @since 1.2
 * @see <a href="https://github.com/InteractiveAdvertisingBureau/AdCOM/blob/main/AdCOM%20v1.0%20FINAL.md#object--assetformat-">AdCOM, version 1.0 - Object: AssetFormat</a>
 * @see <a href="https://github.com/InteractiveAdvertisingBureau/AdCOM/blob/main/AdCOM%20v1.0%20FINAL.md#object--titleassetformat-">AdCOM, version 1.0 - Object: TitleAssetFormat</a>
 * @see <a href="https://github.com/InteractiveAdvertisingBureau/AdCOM/blob/main/AdCOM%20v1.0%20FINAL.md#object--imageassetformat-">AdCOM, version 1.0 - Object: ImageAssetFormat</a>
 * @see <a href="https://github.com/InteractiveAdvertisingBureau/AdCOM/blob/main/AdCOM%20v1.0%20FINAL.md#object--dataassetformat-">AdCOM, version 1.0 - Object: DataAssetFormat</a>
 */
public final class NativeAssetFormat implements ProtobufSerializable {

	private static final @FieldTag int ID				= fieldTagOf(1, WIRE_VARINT);
	private static final @FieldTag int REQ				= fieldTagOf(2, WIRE_VARINT);
	private static final @FieldTag int TITLE			= fieldTagOf(3, WIRE_LEN);
	private static final @FieldTag int IMG				= fieldTagOf(4, WIRE_LEN);
	private static final @FieldTag int VIDEO			= fieldTagOf(5, WIRE_LEN);
	private static final @FieldTag int DATA				= fieldTagOf(6, WIRE_LEN);

	// `NativeTitleAssetFormat`
	private static final @FieldTag int TITLE_LEN		= fieldTagOf(1, WIRE_VARINT);

	// `NativeDataAssetFormat`
	private static final @FieldTag int DATA_TYPE		= fieldTagOf(1, WIRE_VARINT);
	private static final @FieldTag int DATA_LEN			= fieldTagOf(2, WIRE_VARINT);

	// `NativeImageAssetFormat`
	private static final @FieldTag int IMAGE_TYPE		= fieldTagOf(1, WIRE_VARINT);
	private static final @FieldTag int IMAGE_MIME		= fieldTagOf(2, WIRE_LEN);
	private static final @FieldTag int IMAGE_W			= fieldTagOf(3, WIRE_VARINT);
	private static final @FieldTag int IMAGE_H			= fieldTagOf(4, WIRE_VARINT);
	/*private static final @FieldTag int IMAGE_WMIN		= fieldTagOf(5, WIRE_VARINT);*/
	/*private static final @FieldTag int IMAGE_HMIN		= fieldTagOf(6, WIRE_VARINT);*/
	/*private static final @FieldTag int IMAGE_WRATIO	= fieldTagOf(7, WIRE_VARINT);*/
	/*private static final @FieldTag int IMAGE_HRATIO	= fieldTagOf(8, WIRE_VARINT);*/

	/**
	 * Flag set in {@link #idAndRequired} to indicate asset is required.
	 */
	private static final int REQUIRED_MASK	= 1 << 31;

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

	private static NativeAssetFormat
	of(@AssetClass int cls, @IntRange(from = 0) int id, boolean req) {
		NativeAssetFormat fmt = new NativeAssetFormat();

		fmt.setAssetClass(cls);
		fmt.setId(id);
		fmt.setRequired(req);
		return fmt;
	}

	/**
	 * Construct new {@linkplain #isTitleAsset() title} asset format.
	 *
	 * @param id asset format identifier
	 * @param req {@code true} if, and only if, asset is required for format
	 * @param maxLen maximum length, in characters, of title text
	 * @return new format instance
	 * @throws IllegalArgumentException {@code id} is negative
	 * @since 1.2
	 */
	public static NativeAssetFormat
	ofTitleAsset(@IntRange(from = 0) int id, boolean req, int maxLen) {
		NativeAssetFormat fmt = of(ASSET_TITLE, id, req);

		fmt.widthDpOrMaxLength = maxLen;
		return fmt;
	}

	/**
	 * Construct new {@linkplain #isDataAsset() data} asset format.
	 *
	 * @param id asset format identifier
	 * @param req {@code true} if, and only if, asset is required for format
	 * @param type asset type
	 * @param maxLen maximum length, in characters, of data value text
	 * @return new format instance
	 * @throws IllegalArgumentException {@code id} is negative
	 * @since 1.2
	 */
	public static NativeAssetFormat ofDataAsset(
		@IntRange(from = 0) int id,
		boolean req,
		@NativeDataAssetType int type,
		int maxLen
	) {
		NativeAssetFormat fmt = of(ASSET_DATA, id, req);

		fmt.setType(type);
		fmt.widthDpOrMaxLength = maxLen;
		return fmt;
	}

	/**
	 * Construct new {@linkplain #isImageAsset() image} asset format.
	 *
	 * @param id asset format identifier
	 * @param req {@code true} if, and only if, asset is required for format
	 * @param type asset type
	 * @param w absolute image width, in device independent pixels
	 * @param h absolute image height, in device independent pixels
	 * @param mimes supported MIME types or {@linkplain Collection#isEmpty() empty} if all MIME
	 * types are supported
	 * @return new format instance
	 * @throws IllegalArgumentException {@code id} is negative
	 * @since 1.2
	 */
	public static NativeAssetFormat ofImageAsset(
		@IntRange(from = 0) int id,
		boolean req,
		@NativeImageAssetType int type,
		@Dimension(unit = Dimension.DP) int w,
		@Dimension(unit = Dimension.DP) int h,
		Collection<String> mimes
	) {
		NativeAssetFormat fmt = of(ASSET_IMAGE, id, req);

		fmt.setType(type);
		fmt.widthDpOrMaxLength = w;
		fmt.heightDp = h;
		fmt.data = CollectionsCompat.toStringArrayOrEmpty(mimes);
		return fmt;
	}

	/**
	 * Construct new {@linkplain #isVideoAsset() video} asset format.
	 *
	 * @param id asset format identifier
	 * @param req {@code true} if, and only if, asset is required for format
	 * @param video supported video media format
	 * @return new format instance
	 * @throws IllegalArgumentException {@code id} is negative or {@code video} is not a
	 * {@linkplain PlaybackAdFormat#isVideoAd() video} ad media format
	 * @since 1.2
	 */
	public static NativeAssetFormat
	ofVideoAsset(@IntRange(from = 0) int id, boolean req, PlaybackAdFormat video) {
		Preconditions.checkArgument(video.isVideoAd());

		NativeAssetFormat fmt = of(ASSET_VIDEO, id, req);

		fmt.data = video;
		return fmt;
	}

	/**
	 * Deserialize native asset format from Protobuf message.
	 *
	 * @param dec decoder to deserialize from
	 * @return deserialized format
	 * @throws RuntimeException coding is malformed
	 * @since 1.2
	 */
	public static NativeAssetFormat ofProtobuf(ProtobufDecoder dec) {
		NativeAssetFormat rv = new NativeAssetFormat();

		while (dec.hasRemaining()) {
			int tag = dec.decodeFieldTag();

			if (tag == ID) {
				rv.setId(dec.decodeUint32());
			} else if (tag == REQ) {
				rv.setRequired(dec.decodeBool());
			} else if (tag == TITLE) {
				dec.decodeLen(rv, NativeAssetFormat::mergeTitleAssetFormatProtobuf);
			} else if (tag == IMG) {
				dec.decodeLen(rv, NativeAssetFormat::mergeImageAssetFormatProtobuf);
			} else if (tag == VIDEO) {
				rv.setAssetClass(ASSET_VIDEO);
				rv.data = dec.decodeLen(PlaybackAdFormat::ofVideoAdProtobuf);
			} else if (tag == DATA) {
				dec.decodeLen(rv, NativeAssetFormat::mergeDataAssetFormatProtobuf);
			} else {
				dec.skipFieldValue(tag);
			}
		}
		return rv;
	}

	// Low-order 30 bits are format id, while 31st bit is set only if asset is required.
	private int idAndRequired;

	// Image or data asset type, followed by `ASSET_CLASS_MASK` for asset class.
	private int typeAndClass;

	// Image width, in dips, or maximum data/title length
	private int widthDpOrMaxLength;

	// Image height.
	private int heightDp;

	// Supported image asset MIMEs or video media format.
	private Object data;

	private NativeAssetFormat() {
	}

	// Deserialize native title asset format from Protobuf message.
	private NativeAssetFormat mergeTitleAssetFormatProtobuf(ProtobufDecoder src) {
		this.widthDpOrMaxLength = 0;
		this.setAssetClass(ASSET_TITLE);

		while (src.hasRemaining()) {
			int tag = src.decodeFieldTag();

			if (tag == TITLE_LEN)
				this.widthDpOrMaxLength = src.decodeUint32();
			else
				src.skipFieldValue(tag);
		}
		return this;
	}

	// Deserialize native image asset format from Protobuf message.
	private NativeAssetFormat mergeImageAssetFormatProtobuf(ProtobufDecoder src) {
		this.widthDpOrMaxLength = 0;
		this.heightDp = 0;
		this.setType(AdComEnums.NativeImageAssetUnknown);
		this.setAssetClass(ASSET_IMAGE);

		List<String> mimes = new ArrayList<>();

		while (src.hasRemaining()) {
			int tag = src.decodeFieldTag();

			if (tag == IMAGE_TYPE) {
				int type = src.decodeUint32();

				if (type >= 0 && type <= AdComEnums.MAX_NATIVE_IMAGE_ASSET_TYPE)
					this.setType(type);
			} else if (tag == IMAGE_MIME) {
				mimes.add(src.decodeString());
			} else if (tag == IMAGE_W) {
				this.widthDpOrMaxLength = src.decodeUint32();
			} else if (tag == IMAGE_H) {
				this.heightDp = src.decodeUint32();
			} else {
				src.skipFieldValue(tag);
			}
		}
		this.data = CollectionsCompat.toStringArrayOrEmpty(mimes);
		return this;
	}

	// Deserialize native data asset format from Protobuf message.
	private NativeAssetFormat mergeDataAssetFormatProtobuf(ProtobufDecoder src) {
		this.widthDpOrMaxLength = 0;
		this.setType(AdComEnums.NativeDataAssetUnknown);
		this.setAssetClass(ASSET_DATA);

		while (src.hasRemaining()) {
			int tag = src.decodeFieldTag();

			if (tag == DATA_TYPE) {
				int type = src.decodeUint32();

				if (type >= 0 && type <= AdComEnums.MAX_NATIVE_DATA_ASSET_TYPE)
					this.setType(type);
			} else if (tag == DATA_LEN) {
				this.widthDpOrMaxLength = src.decodeUint32();
			} else {
				src.skipFieldValue(tag);
			}
		}
		return this;
	}

	/**
	 * Set format identifier, unique to placement.
	 *
	 * @param id non-negative identifier
	 * @throws IllegalArgumentException {@code id} is negative
	 */
	private void setId(@IntRange(from = 0) int id) {
		Preconditions.checkArgument(id >= 0, "id cannot be negative");
		this.idAndRequired = id | (this.idAndRequired & REQUIRED_MASK);
	}

	/**
	 * Format identifier, unique to placement.
	 *
	 * @return non-negative identifier
	 * @since 1.2
	 */
	public @IntRange(from = 0) int id() {
		return this.idAndRequired & ~REQUIRED_MASK;
	}

	/**
	 * Set whether asset is required for format.
	 *
	 * @param req {@code true} if, and only if, asset is required for format
	 */
	private void setRequired(boolean req) {
		if (req)
			this.idAndRequired |= REQUIRED_MASK;
		else
			this.idAndRequired &= ~REQUIRED_MASK;
	}

	/**
	 * Asset is required for format.
	 *
	 * @return {@code true} if, and only if, asset is required for format
	 * @since 1.2
	 */
	public boolean required() {
		return (this.idAndRequired & REQUIRED_MASK) != 0;
	}

	/**
	 * Set asset type.
	 *
	 * @param type asset type to set to
	 * @throws IllegalArgumentException {@code type} overflows
	 */
	private void setType(int type) {
		Preconditions.checkArgument((type & ~ASSET_CLASS_MASK) == type, "type overflow");
		this.typeAndClass = type | (this.typeAndClass & ASSET_CLASS_MASK);
	}

	/**
	 * Set asset class.
	 *
	 * @param cls asset class to set to
	 */
	private void setAssetClass(@AssetClass int cls) {
		assert (cls & ASSET_CLASS_MASK) == cls;
		this.typeAndClass = (this.typeAndClass & ~ASSET_CLASS_MASK) | cls;
	}

	/**
	 * Test whether {@code this} is of a specific asset class.
	 *
	 * @param cls asset class to compare against
	 * @return {@code true} if, and only if, {@code this} is of asset class {@code cls}
	 */
	private boolean isAssetClass(@AssetClass int cls) {
		return (this.typeAndClass & cls) != 0;
	}

	/**
	 * Ensure {@code this} is of a specific asset class.
	 *
	 * @param cls asset class to ensure
	 * @throws IllegalStateException {@code this} is not of asset class {@code cls}
	 */
	private void checkAssetClass(@AssetClass int cls) {
		Preconditions.checkState(this.isAssetClass(cls));
	}

	/**
	 * Test whether format is for a title asset.
	 *
	 * @return {@code true} if, and only if, format is for title asset
	 * @since 1.2
	 * @see #maxTitleTextLength()
	 */
	public boolean isTitleAsset() {
		return this.isAssetClass(ASSET_TITLE);
	}

	/**
	 * Maximum length, in character, of title asset text.
	 *
	 * @return maximum length
	 * @throws IllegalStateException not a {@linkplain #isTitleAsset() title asset format}
	 * @since 1.2
	 * @see #isTitleAsset()
	 */
	public int maxTitleTextLength() {
		this.checkAssetClass(ASSET_TITLE);
		return this.widthDpOrMaxLength;
	}

	/**
	 * Test whether format is for a data asset.
	 *
	 * @return {@code true} if, and only if, format is for data asset
	 * @since 1.2
	 * @see #dataAssetType()
	 * @see #maxDataValueLength()
	 */
	public boolean isDataAsset() {
		return this.isAssetClass(ASSET_DATA);
	}

	/**
	 * Data asset type.
	 *
	 * @return asset type
	 * @throws IllegalStateException not a {@linkplain #isDataAsset() data asset format}
	 * @since 1.2
	 * @see #isDataAsset()
	 * @see #maxDataValueLength()
	 */
	@SuppressLint("WrongConstant")
	public @NativeDataAssetType int dataAssetType() {
		this.checkAssetClass(ASSET_DATA);
		return this.typeAndClass & ~ASSET_CLASS_MASK;
	}

	/**
	 * Maximum length, in characters, of data asset value.
	 *
	 * @return maximum length
	 * @throws IllegalStateException not a {@linkplain #isDataAsset() data asset format}
	 * @since 1.2
	 * @see #isDataAsset()
	 * @see #dataAssetType()
	 */
	public int maxDataValueLength() {
		this.checkAssetClass(ASSET_DATA);
		return this.widthDpOrMaxLength;
	}

	/**
	 * Test whether format is for an image asset.
	 *
	 * @return {@code true} if, and only if, format is for image asset
	 * @since 1.2
	 * @see #imageAssetType()
	 * @see #imageWidthDp()
	 * @see #imageHeightDp()
	 */
	public boolean isImageAsset() {
		return this.isAssetClass(ASSET_IMAGE);
	}

	/**
	 * Image asset type.
	 *
	 * @return asset type
	 * @throws IllegalStateException not an {@linkplain #isImageAsset() image asset format}
	 * @since 1.2
	 * @see #isImageAsset()
	 * @see #imageWidthDp()
	 * @see #imageHeightDp()
	 */
	@SuppressLint("WrongConstant")
	public @NativeImageAssetType int imageAssetType() {
		this.checkAssetClass(ASSET_IMAGE);
		return this.typeAndClass & ~ASSET_CLASS_MASK;
	}

	/**
	 * Absolute width, in device independent pixels, of image asset.
	 *
	 * @return image width
	 * @throws IllegalStateException not an {@linkplain #isImageAsset() image asset format}
	 * @since 1.2
	 * @see #isImageAsset()
	 * @see #imageAssetType()
	 * @see #imageHeightDp()
	 */
	public @Dimension(unit = Dimension.DP) int imageWidthDp() {
		this.checkAssetClass(ASSET_IMAGE);
		return this.widthDpOrMaxLength;
	}

	/**
	 * Absolute height, in device independent pixels, of image asset.
	 *
	 * @return image height
	 * @throws IllegalStateException not an {@linkplain #isImageAsset() image asset format}
	 * @since 1.2
	 * @see #isImageAsset()
	 * @see #imageAssetType()
	 * @see #imageWidthDp()
	 */
	public int imageHeightDp() {
		this.checkAssetClass(ASSET_IMAGE);
		return this.heightDp;
	}

	/**
	 * Count of MIME types supported for image asset.
	 *
	 * @return image asset MIME count
	 * @throws IllegalStateException not an {@linkplain #isImageAsset() image asset format}
	 * @since 1.2
	 * @see #isImageAsset()
	 * @see #supportedImageMime(int)
	 */
	public int supportedImageMimeCount() {
		this.checkAssetClass(ASSET_IMAGE);
		return ((String[]) this.data).length;
	}

	/**
	 * MIME type, supported for image asset, at index.
	 *
	 * @param i index to retrieve MIME type at
	 * @return MIME type at index {@code i}
	 * @throws IllegalStateException not an {@linkplain #isImageAsset() image asset format}
	 * @throws IndexOutOfBoundsException {@code i} is negative or, greater than or equal to
	 * MIME {@linkplain #supportedImageMimeCount() count}
	 * @since 1.2
	 * @see #isImageAsset()
	 * @see #supportedImageMimeCount()
	 */
	public String supportedImageMime(int i) {
		this.checkAssetClass(ASSET_IMAGE);
		return ((String[]) this.data)[i];
	}

	/**
	 * Test whether format is for a video asset.
	 *
	 * @return {@code true} if, and only if, format is for video asset
	 * @since 1.2
	 * @see #video()
	 */
	public boolean isVideoAsset() {
		return this.isAssetClass(ASSET_VIDEO);
	}

	/**
	 * Supported video media format.
	 *
	 * @return video media format
	 * @throws IllegalStateException not a {@linkplain #isVideoAsset() video asset format}
	 * @since 1.2
	 * @see #isVideoAsset()
	 */
	public PlaybackAdFormat video() {
		this.checkAssetClass(ASSET_VIDEO);
		return (PlaybackAdFormat) this.data;
	}

	@Override
	public void toProtobuf(ProtobufEncoder enc) {
		enc.encodeUnsignedIntField(ID, this.id())
			.encodeBoolField(REQ, this.required());

		switch (this.typeAndClass & ASSET_CLASS_MASK) {
		case ASSET_DATA:
			enc.encodeLenField(
				DATA, this,
				(data, dataEnc) ->
					dataEnc.encodeUnsignedIntField(DATA_TYPE, data.typeAndClass & ~ASSET_CLASS_MASK)
						.encodeUnsignedIntField(DATA_LEN, data.widthDpOrMaxLength)
			);
			break;
		case ASSET_IMAGE:
			enc.encodeLenField(IMG, this, (img, imgEnc) -> {
				imgEnc.encodeUnsignedIntField(IMAGE_TYPE, img.typeAndClass & ~ASSET_CLASS_MASK)
					.encodeUnsignedIntField(IMAGE_W, img.widthDpOrMaxLength)
					.encodeUnsignedIntField(IMAGE_H, img.heightDp);
				for (String mime : (String[]) img.data)
					imgEnc.encodeStringField(IMAGE_MIME, mime);
			});
			break;
		case ASSET_TITLE:
			enc.encodeLenField(TITLE, this, (title, titleEnc) -> titleEnc.encodeUnsignedIntField(
				TITLE_LEN,
				title.widthDpOrMaxLength
			));
			break;
		case ASSET_VIDEO:
			enc.encodeMessageField(VIDEO, (PlaybackAdFormat) this.data);
			break;
		default:
			break;
		}
	}
}
