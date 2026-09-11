// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.adcom.placement;

import static org.polygamma.android.origin.protobuf.Protobuf.WIRE_LEN;
import static org.polygamma.android.origin.protobuf.Protobuf.WIRE_VARINT;
import static org.polygamma.android.origin.protobuf.Protobuf.fieldTagOf;

import android.annotation.SuppressLint;

import androidx.annotation.Dimension;
import androidx.annotation.ReturnThis;

import org.polygamma.android.origin.adcom.enums.ActivationBehavior;
import org.polygamma.android.origin.adcom.enums.AdApiCode;
import org.polygamma.android.origin.adcom.enums.AdComEnums;
import org.polygamma.android.origin.protobuf.Protobuf.FieldTag;
import org.polygamma.android.origin.protobuf.ProtobufDecoder;
import org.polygamma.android.origin.protobuf.ProtobufEncoder;
import org.polygamma.android.origin.util.CollectionsCompat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Display ad media format.
 *
 * @since 1.2
 * @see <a href="https://github.com/InteractiveAdvertisingBureau/AdCOM/blob/main/AdCOM%20v1.0%20FINAL.md#object_displayplacement">AdCOM, version 1.0 - Object: DisplayPlacement</a>
 */
public final class DisplayAdFormat extends AdFormat {

	/*private static final @FieldTag int POS		= fieldTagOf( 1, WIRE_VARINT);*/
	private static final @FieldTag int INSTL		= fieldTagOf( 2, WIRE_VARINT);
	/*private static final @FieldTag int TOPFRAME	= fieldTagOf( 3, WIRE_VARINT);*/
	/*private static final @FieldTag int IFRBUST	= fieldTagOf( 4, WIRE_LEN);*/
	private static final @FieldTag int CLKTYPE		= fieldTagOf( 5, WIRE_VARINT);
	/*private static final @FieldTag int AMPREN		= fieldTagOf( 6, WIRE_VARINT);*/
	/*private static final @FieldTag int PTYPE		= fieldTagOf( 7, WIRE_VARINT);*/
	/*private static final @FieldTag int CONTEXT	= fieldTagOf( 8, WIRE_VARINT);*/
	private static final @FieldTag int MIME			= fieldTagOf( 9, WIRE_LEN);
	private static final @FieldTag int API			= fieldTagOf(10, WIRE_LEN);
	private static final @FieldTag int CTYPE		= fieldTagOf(11, WIRE_LEN);
	private static final @FieldTag int W			= fieldTagOf(12, WIRE_VARINT);
	private static final @FieldTag int H			= fieldTagOf(13, WIRE_VARINT);
	private static final @FieldTag int UNIT			= fieldTagOf(14, WIRE_VARINT);
	/*private static final @FieldTag int PRIV		= fieldTagOf(15, WIRE_VARINT);*/
	/*private static final @FieldTag int DISPLAYFMT	= fieldTagOf(16, WIRE_LEN);*/
	private static final @FieldTag int NATIVEFMT	= fieldTagOf(17, WIRE_LEN);
	/*private static final @FieldTag int EVENT		= fieldTagOf(18, WIRE_LEN);*/

	private static final @FieldTag int NATIVE_FORMAT_ASSET = fieldTagOf(1, WIRE_LEN);

	/**
	 * Empty display ad media format.
	 */
	private static final DisplayAdFormat DEFAULT = new DisplayAdFormat();

	/**
	 * Display ad media {@linkplain DisplayAdFormat format} builder.
	 *
	 * @since 1.2
	 * @see #ofBuilder()
	 */
	public static final class Builder {

		private DisplayAdFormat display;
		private boolean needClone;

		private Builder(DisplayAdFormat display) {
			this.display = display;
			this.needClone = true;
		}

		private DisplayAdFormat target() {
			if (this.needClone) {
				this.display = new DisplayAdFormat(this.display);
				this.needClone = false;
			}
			return this.display;
		}

		/**
		 * Set supported MIME types.
		 *
		 * @param supp supported MIME types or {@linkplain Collection#isEmpty() empty} if all MIME
		 * types are supported
		 * @return {@code this}
		 * @since 1.2
		 * @see DisplayAdFormat#supportedMime(int)
		 */
		@ReturnThis
		@SuppressLint("RestrictedApi")
		public Builder supportedMimes(Collection<String> supp) {
			this.target().setSupportedMimes(supp);
			return this;
		}

		/**
		 * Set supported ad APIs, by code.
		 *
		 * @param codes supported APIs
		 * @return {@code this}
		 * @since 1.2
		 * @see DisplayAdFormat#isAdApiSupported(int)
		 */
		@ReturnThis
		@SuppressLint("RestrictedApi")
		public Builder supportedAdApis(@AdApiCode int... codes) {
			this.target().setSupportedAdApis(codes);
			return this;
		}

		/**
		 * Set behavior when ad media is activated (i.e. clicked).
		 *
		 * @param act activation behavior
		 * @return {@code this}
		 * @since 1.2
		 * @see DisplayAdFormat#activationBehavior()
		 */
		@ReturnThis
		public Builder activationBehavior(@ActivationBehavior int act) {
			this.target().activationBehavior = act;
			return this;
		}

		/**
		 * Set maximum width, in device independent pixels, supported.
		 *
		 * @param w maximum width
		 * @return {@code this}
		 * @since 1.2
		 * @see #heightDp(int)
		 * @see DisplayAdFormat#widthDp()
		 */
		@ReturnThis
		public Builder widthDp(@Dimension(unit = Dimension.DP) int w) {
			this.target().widthDp = w;
			return this;
		}

		/**
		 * Set maximum height, in device independent pixels, supported.
		 *
		 * @param h maximum height
		 * @return {@code this}
		 * @since 1.2
		 * @see #widthDp(int)
		 * @see DisplayAdFormat#heightDp()
		 */
		@ReturnThis
		public Builder heightDp(@Dimension(unit = Dimension.DP) int h) {
			this.target().heightDp = h;
			return this;
		}

		/**
		 * Set supported native ad media asset formats.
		 *
		 * @param assets supported asset formats or {@linkplain Collection#isEmpty() empty} if
		 * native ad media is not supported
		 * @return {@code this}
		 * @since 1.2
		 * @see DisplayAdFormat#nativeAsset(int)
		 */
		@ReturnThis
		public Builder nativeAssets(Collection<NativeAssetFormat> assets) {
			this.target().nativeAssets =
				CollectionsCompat.toArrayOrEmpty(assets, DEFAULT.nativeAssets);
			return this;
		}

		/**
		 * Set whether ad media is rendered in interstitial.
		 *
		 * @param instl {@code true} if, and only if, interstitial
		 * @return {@code this}
		 * @since 1.2
		 * @see DisplayAdFormat#interstitial()
		 */
		@ReturnThis
		public Builder interstitial(boolean instl) {
			this.target().interstitial = instl;
			return this;
		}

		/**
		 * Build resulting format.
		 *
		 * @return format instance
		 * @since 1.2
		 */
		public DisplayAdFormat build() {
			this.needClone = true;
			return this.display;
		}
	}

	/**
	 * Empty display ad media format.
	 *
	 * @return empty format instance
	 * @since 1.2
	 */
	public static DisplayAdFormat of() {
		return DEFAULT;
	}

	/**
	 * Construct new empty {@linkplain Builder builder}.
	 *
	 * @return empty builder instance
	 * @since 1.2
	 */
	public static Builder ofBuilder() {
		return DEFAULT.toBuilder();
	}

	private static NativeAssetFormat[] decodeNativeFormat(ProtobufDecoder dec) {
		List<NativeAssetFormat> assets = new ArrayList<>();

		while (dec.hasRemaining()) {
			int tag = dec.decodeFieldTag();

			if (tag == NATIVE_FORMAT_ASSET)
				assets.add(dec.decodeLen(NativeAssetFormat::ofProtobuf));
			else
				dec.skipFieldValue(tag);
		}
		return CollectionsCompat.toArrayOrEmpty(assets, DEFAULT.nativeAssets);
	}

	/**
	 * Deserialize display ad media format from Protobuf message.
	 *
	 * @param dec decoder to deserialize from
	 * @return deserialized format
	 * @throws RuntimeException coding is malformed
	 * @since 1.2
	 */
	public static DisplayAdFormat ofProtobuf(ProtobufDecoder dec) {
		DisplayAdFormat rv = new DisplayAdFormat(DEFAULT);
		List<String> mime = new ArrayList<>();

		while (dec.hasRemaining()) {
			int tag = dec.decodeFieldTag();

			if (tag == MIME) {
				mime.add(dec.decodeString());
			} else if (tag == API) {
				rv.setSupportedAdApiMask(dec.decodePackedUint32Bitmap64());
			} else if (tag == CLKTYPE) {
				rv.activationBehavior = dec.decodeUint32();
			} else if (tag == W) {
				rv.widthDp = dec.decodeUint32();
			} else if (tag == H) {
				rv.heightDp = dec.decodeUint32();
			} else if (tag == NATIVEFMT) {
				rv.nativeAssets = dec.decodeLen(DisplayAdFormat::decodeNativeFormat);
			} else if (tag == INSTL) {
				rv.interstitial = dec.decodeBool();
			} else {
				dec.skipFieldValue(tag);
			}
		}
		rv.setSupportedMimes(mime);
		return rv;
	}

	private @ActivationBehavior int activationBehavior;
	private @Dimension(unit = Dimension.DP) int widthDp;
	private @Dimension(unit = Dimension.DP) int heightDp;
	private NativeAssetFormat[] nativeAssets;
	private boolean interstitial;

	private DisplayAdFormat() {
		super();
		this.activationBehavior = AdComEnums.ActivationNone;
		this.nativeAssets = new NativeAssetFormat[0];
	}

	private DisplayAdFormat(DisplayAdFormat that) {
		super(that);
		this.activationBehavior = that.activationBehavior;
		this.widthDp = that.widthDp;
		this.heightDp = that.heightDp;
		this.nativeAssets = that.nativeAssets;
		this.interstitial = that.interstitial;
	}

	/**
	 * Behavior when ad media is activated (i.e. clicked).
	 *
	 * @return activation behavior
	 * @since 1.2
	 * @see Builder#activationBehavior(int)
	 */
	public @ActivationBehavior int activationBehavior() {
		return this.activationBehavior;
	}

	/**
	 * Maximum width, in device independent pixels, supported.
	 *
	 * @return maximum width
	 * @since 1.2
	 * @see #heightDp()
	 * @see Builder#widthDp(int)
	 */
	public @Dimension(unit = Dimension.DP) int widthDp() {
		return this.widthDp;
	}

	/**
	 * Maximum height, in device independent pixels, supported.
	 *
	 * @return maximum height
	 * @since 1.2
	 * @see #widthDp()
	 * @see Builder#heightDp(int)
	 */
	public @Dimension(unit = Dimension.DP) int heightDp() {
		return this.heightDp;
	}

	/**
	 * Count of native asset formats.
	 *
	 * @return asset format count or {@code 0} if native media is not supported
	 * @since 1.2
	 * @see #nativeAsset(int)
	 * @see Builder#nativeAssets(Collection)
	 */
	public int nativeAssetCount() {
		return this.nativeAssets.length;
	}

	/**
	 * Native asset format at index.
	 *
	 * @param i index to retrieve asset format at
	 * @return asset format at index {@code i}
	 * @throws IndexOutOfBoundsException {@code i} is negative or, greater than or equal to asset
	 * {@linkplain #nativeAssetCount() count}
	 * @since 1.2
	 * @see #nativeAssetCount()
	 * @see Builder#nativeAssets(Collection)
	 */
	public NativeAssetFormat nativeAsset(int i) {
		return this.nativeAssets[i];
	}

	/**
	 * Ad media is rendered interstitial.
	 *
	 * @return {@code true} if, and only if, interstitial format
	 * @since 1.2
	 * @see Builder#interstitial(boolean)
	 */
	public boolean interstitial() {
		return this.interstitial;
	}

	/**
	 * Construct new builder initialized from {@code this}.
	 *
	 * @return initialized builder instance
	 * @since 1.2
	 */
	public Builder toBuilder() {
		return new Builder(this);
	}

	@Override
	public void toProtobuf(ProtobufEncoder enc) {
		super.encodeCommonProtobufFields(enc, MIME, API);

		enc.encodeUnsignedIntField(CLKTYPE, this.activationBehavior)
			.encodePackedUint32ArrayField(
				CTYPE,
				this.nativeAssets.length == 0 ? new int[] {
					AdComEnums.DisplayCreativeHtml,
					AdComEnums.DisplayCreativeImage
				} : new int[] {
					AdComEnums.DisplayCreativeHtml,
					AdComEnums.DisplayCreativeImage,
					AdComEnums.DisplayCreativeNative
				}
			)
			.encodeUnsignedIntField(W, this.widthDp)
			.encodeUnsignedIntField(H, this.heightDp)
			.encodeUnsignedIntField(UNIT, AdComEnums.DimensionDp)
			.encodeBoolField(INSTL, this.interstitial);

		if (this.nativeAssets.length != 0) {
			enc.encodeLenField(NATIVEFMT, this.nativeAssets, (nat, natEnc) -> {
				for (NativeAssetFormat fmt : nat)
					natEnc.encodeMessageField(NATIVE_FORMAT_ASSET, fmt);
			});
		}
	}
}
