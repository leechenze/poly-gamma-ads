// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.adcom.placement;

import static org.polygamma.android.origin.protobuf.ProtobufField.*;

import android.annotation.SuppressLint;

import androidx.annotation.Dimension;
import androidx.annotation.ReturnThis;

import org.polygamma.android.origin.adcom.enums.ActivationBehavior;
import org.polygamma.android.origin.adcom.enums.AdApiCode;
import org.polygamma.android.origin.adcom.enums.AdComEnums;
import org.polygamma.android.origin.protobuf.ProtobufReader;
import org.polygamma.android.origin.protobuf.ProtobufWriter;
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

	/*private static final @Tag int POS			= ofInt32(       1);*/
	private static final @Tag int INSTL			= ofBool(        2);
	/*private static final @Tag int TOPFRAME	= ofBool(        3);*/
	/*private static final @Tag int IFRBUST		= ofString(      4);*/
	private static final @Tag int CLKTYPE		= ofInt32(       5);
	/*private static final @Tag int AMPREN		= ofInt32(       6);*/
	/*private static final @Tag int PTYPE		= ofInt32(       7);*/
	/*private static final @Tag int CONTEXT		= ofInt32(       8);*/
	private static final @Tag int MIME			= ofString(      9);
	private static final @Tag int API			= ofPackedInt32(10);
	private static final @Tag int CTYPE			= ofPackedInt32(11);
	private static final @Tag int W				= ofInt32(      12);
	private static final @Tag int H				= ofInt32(      13);
	private static final @Tag int UNIT			= ofInt32(      14);
	/*private static final @Tag int PRIV		= ofBool(       15);*/
	/*private static final @Tag int DISPLAYFMT	= ofMessage(    16);*/
	private static final @Tag int NATIVEFMT		= ofMessage(    17);
	/*private static final @Tag int EVENT		= ofMessage(    18);*/

	private static final @Tag int NATIVE_FORMAT_ASSET = ofMessage(1);

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

	/**
	 * Deserialize display ad media format from Protobuf message.
	 *
	 * @param reader reader to deserialize from
	 * @return deserialized format
	 * @throws RuntimeException coding is malformed
	 * @since 1.2
	 */
	public static DisplayAdFormat ofProtobuf(ProtobufReader reader) {
		DisplayAdFormat rv = new DisplayAdFormat(DEFAULT);
		List<String> mime = new ArrayList<>();
		List<NativeAssetFormat> assets = new ArrayList<>();

		while (reader.hasRemaining()) {
			int tag = reader.readTag();

			if (tag == MIME) {
				mime.add(reader.readString());
			} else if (tag == API) {
				rv.setSupportedAdApiMask(reader.readWordBitmap(0));
			} else if (tag == CLKTYPE) {
				rv.activationBehavior = reader.readInt32();
			} else if (tag == W) {
				rv.widthDp = reader.readInt32();
			} else if (tag == H) {
				rv.heightDp = reader.readInt32();
			} else if (tag == NATIVEFMT) {
				int cookie = reader.beginReadLen();

				while (reader.hasRemaining()) {
					if (reader.readTag() == NATIVE_FORMAT_ASSET)
						assets.add(reader.readLen(NativeAssetFormat::ofProtobuf));
				}
				reader.endReadLen(cookie);
			} else if (tag == INSTL) {
				rv.interstitial = reader.readBool();
			}
		}
		rv.nativeAssets = CollectionsCompat.toArrayOrEmpty(assets, rv.nativeAssets);
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
	public void toProtobuf(ProtobufWriter writer) {
		super.writeCommonProtobufFields(writer, MIME, API);
		writer.writeInt32(CLKTYPE, this.activationBehavior);
		writer.writePackedInt32(
			CTYPE,
			this.nativeAssets.length == 0 ? new int[] {
				AdComEnums.DisplayCreativeHtml,
				AdComEnums.DisplayCreativeImage
			} : new int[] {
				AdComEnums.DisplayCreativeHtml,
				AdComEnums.DisplayCreativeImage,
				AdComEnums.DisplayCreativeNative
			}
		);
		writer.writeInt32(W, this.widthDp);
		writer.writeInt32(H, this.heightDp);
		writer.writeInt32(UNIT, AdComEnums.DimensionDp);

		if (this.nativeAssets.length != 0) {
			long cookie = writer.beginWriteLen(NATIVEFMT);

			writer.writeRepeatLen(NATIVE_FORMAT_ASSET, this.nativeAssets);
			writer.endWriteLen(cookie);
		}
		writer.writeBool(INSTL, this.interstitial);
	}
}
