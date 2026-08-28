// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.adcom.placement;

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

	private static final @Tag int ID				= ofInt32(  1);
	private static final @Tag int REQ				= ofBool(   2);
	private static final @Tag int TITLE				= ofMessage(3);
	private static final @Tag int IMG				= ofMessage(4);
	private static final @Tag int VIDEO				= ofMessage(5);
	private static final @Tag int DATA				= ofMessage(6);

	// `NativeTitleAssetFormat`
	private static final @Tag int TITLE_LEN			= ofInt32(1);

	// `NativeDataAssetFormat`
	private static final @Tag int DATA_TYPE			= ofInt32(1);
	private static final @Tag int DATA_LEN			= ofInt32(2);

	// `NativeImageAssetFormat`
	private static final @Tag int IMAGE_TYPE		= ofInt32( 1);
	private static final @Tag int IMAGE_MIME		= ofString(2);
	private static final @Tag int IMAGE_W			= ofInt32( 3);
	private static final @Tag int IMAGE_H			= ofInt32( 4);
	/*private static final @Tag int IMAGE_WMIN		= ofInt32( 5);*/
	/*private static final @Tag int IMAGE_HMIN		= ofInt32( 6);*/
	/*private static final @Tag int IMAGE_WRATIO	= ofInt32( 7);*/
	/*private static final @Tag int IMAGE_HRATIO	= ofInt32( 8);*/

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
	 * Deserialize native title asset format from Protobuf message.
	 *
	 * @param dst format to deserialize into
	 * @param src reader to deserialize from
	 * @throws RuntimeException coding is malformed
	 */
	private static void deserializeTitleAssetFormat(NativeAssetFormat dst, ProtobufReader src) {
		dst.widthDpOrMaxLength = 0;
		dst.setAssetClass(ASSET_TITLE);

		int cookie = src.beginReadLen();

		while (src.hasRemaining()) {
			if (src.readTag() == TITLE_LEN)
				dst.widthDpOrMaxLength = src.readInt32();
		}
		src.endReadLen(cookie);
	}

	/**
	 * Deserialize native data asset format from Protobuf message.
	 *
	 * @param dst format to deserialize into
	 * @param src reader to deserialize from
	 * @throws RuntimeException coding is malformed
	 */
	private static void deserializeDataAssetFormat(NativeAssetFormat dst, ProtobufReader src) {
		dst.widthDpOrMaxLength = 0;
		dst.setType(AdComEnums.NativeDataAssetUnknown);
		dst.setAssetClass(ASSET_DATA);

		int cookie = src.beginReadLen();

		while (src.hasRemaining()) {
			int tag = src.readTag();

			if (tag == DATA_TYPE) {
				int type = src.readInt32();

				if (type >= 0 && type <= AdComEnums.MAX_NATIVE_DATA_ASSET_TYPE)
					dst.setType(type);
			} else if (tag == DATA_LEN) {
				dst.widthDpOrMaxLength = src.readInt32();
			}
		}
		src.endReadLen(cookie);
	}

	/**
	 * Deserialize native image asset format from Protobuf message.
	 *
	 * @param dst format to deserialize into
	 * @param src reader to deserialize from
	 * @throws RuntimeException coding is malformed
	 */
	private static void deserializeImageAssetFormat(NativeAssetFormat dst, ProtobufReader src) {
		dst.widthDpOrMaxLength = 0;
		dst.heightDp = 0;
		dst.setType(AdComEnums.NativeImageAssetUnknown);
		dst.setAssetClass(ASSET_IMAGE);

		List<String> mimes = new ArrayList<>();
		int cookie = src.beginReadLen();

		while (src.hasRemaining()) {
			int tag = src.readTag();

			if (tag == IMAGE_TYPE) {
				int type = src.readInt32();

				if (type >= 0 && type <= AdComEnums.MAX_NATIVE_IMAGE_ASSET_TYPE)
					dst.setType(type);
			} else if (tag == IMAGE_MIME) {
				mimes.add(src.readString());
			} else if (tag == IMAGE_W) {
				dst.widthDpOrMaxLength = src.readInt32();
			} else if (tag == IMAGE_H) {
				dst.heightDp = src.readInt32();
			}
		}
		src.endReadLen(cookie);
		dst.data = CollectionsCompat.toStringArrayOrEmpty(mimes);
	}

	/**
	 * Deserialize native asset format from Protobuf message.
	 *
	 * @param reader reader to deserialize from
	 * @return deserialized format
	 * @throws RuntimeException coding is malformed
	 * @since 1.2
	 */
	public static NativeAssetFormat ofProtobuf(ProtobufReader reader) {
		NativeAssetFormat rv = new NativeAssetFormat();

		while (reader.hasRemaining()) {
			int tag = reader.readTag();

			if (tag == ID) {
				rv.setId(reader.readInt32());
			} else if (tag == REQ) {
				rv.setRequired(reader.readBool());
			} else if (tag == TITLE) {
				deserializeTitleAssetFormat(rv, reader);
			} else if (tag == IMG) {
				deserializeImageAssetFormat(rv, reader);
			} else if (tag == VIDEO) {
				rv.setAssetClass(ASSET_VIDEO);
				rv.data = reader.readLen(PlaybackAdFormat::ofVideoAdProtobuf);
			} else if (tag == DATA) {
				deserializeDataAssetFormat(rv, reader);
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
	public void toProtobuf(ProtobufWriter writer) {
		long cookie;

		writer.writeInt32(ID, this.id());
		writer.writeBool(REQ, this.required());

		switch (this.typeAndClass & ASSET_CLASS_MASK) {
		case ASSET_DATA:
			cookie = writer.beginWriteLen(DATA);
			writer.writeInt32(DATA_TYPE, this.typeAndClass & ~ASSET_CLASS_MASK);
			writer.writeInt32(DATA_LEN, this.widthDpOrMaxLength);
			break;
		case ASSET_IMAGE:
			cookie = writer.beginWriteLen(IMG);
			writer.writeInt32(IMAGE_TYPE, this.typeAndClass & ~ASSET_CLASS_MASK);
			writer.writeRepeatString(IMAGE_MIME, (String[]) this.data);
			writer.writeInt32(IMAGE_W, this.widthDpOrMaxLength);
			writer.writeInt32(IMAGE_H, this.heightDp);
			break;
		case ASSET_TITLE:
			cookie = writer.beginWriteLen(TITLE);
			writer.writeInt32(TITLE_LEN, this.widthDpOrMaxLength);
			break;
		case ASSET_VIDEO:
			writer.writeLen(VIDEO, (PlaybackAdFormat) this.data);
			return;
		default:
			return;
		}
		writer.endWriteLen(cookie);
	}
}
