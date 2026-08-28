// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.adcom.media;

import static org.polygamma.android.origin.protobuf.ProtobufField.*;

import android.annotation.SuppressLint;
import android.util.ArrayMap;
import android.util.Pair;

import androidx.annotation.Dimension;
import androidx.annotation.ReturnThis;

import org.polygamma.android.origin.adcom.enums.AdApiCode;
import org.polygamma.android.origin.adcom.enums.AdComEnums;
import org.polygamma.android.origin.adcom.enums.DisplayCreativeType;
import org.polygamma.android.origin.protobuf.ProtobufReader;
import org.polygamma.android.origin.protobuf.ProtobufWriter;
import org.polygamma.android.origin.util.CollectionsCompat;
import org.polygamma.android.origin.util.Preconditions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Display ad media.
 *
 * @since 1.2
 * @see <a href="https://github.com/InteractiveAdvertisingBureau/AdCOM/blob/main/AdCOM%20v1.0%20FINAL.md#object--display-">AdCOM, version 1.0 - Object: Display</a>
 * @see <a href="https://github.com/InteractiveAdvertisingBureau/AdCOM/blob/main/AdCOM%20v1.0%20FINAL.md#object--native-">AdCOM, version 1.0 - Object: Native</a>
 * @see <a href="https://github.com/InteractiveAdvertisingBureau/AdCOM/blob/main/AdCOM%20v1.0%20FINAL.md#object--banner-">AdCOM, version 1.0 - Object: Banner</a>
 */
public final class DisplayAd extends Ad {

	/*private static final @Tag int MIME		= ofString(       1);*/
	private static final @Tag int API			= ofPackedInt32(  2);
	/*private static final @Tag int CTYPE		= ofInt32(        3);*/
	private static final @Tag int W				= ofInt32(        4);
	private static final @Tag int H				= ofInt32(        5);
	private static final @Tag int WRATIO		= ofInt32(        6);
	private static final @Tag int HRATIO		= ofInt32(        7);
	private static final @Tag int PRIV			= ofString(       8);
	private static final @Tag int ADM			= ofString(       9);
	private static final @Tag int CURL			= ofString(      10);
	private static final @Tag int BANNER		= ofMessage(     11);
	private static final @Tag int NATIVE		= ofMessage(     12);
	private static final @Tag int EVENT			= ofMessage(     13);
	private static final @Tag int MINSHOWDUR	= ofInt64(      500);
	private static final @Tag int UNIVID		= ofStringPair( 501);
	private static final @Tag int ICON			= ofMessage(    502);

	// `StaticBannerAd`
	private static final @Tag int BANNER_IMG	= ofString(       1);
	private static final @Tag int BANNER_LINK	= ofMessage(      2);

	// `NativeAd`
	private static final @Tag int NATIVE_LINK	= ofMessage(      1);
	private static final @Tag int NATIVE_ASSET	= ofMessage(      2);

	private static final int FLAG_ADM		= 0x10000000;
	private static final int FLAG_CURL		= 0x20000000;
	private static final int FLAGS_MASK		= 0xf0000000;

	private static final DisplayAd DEFAULT = new DisplayAd();

	static {
		//noinspection ConstantValue
		assert (AdComEnums.MAX_DISPLAY_CREATIVE_TYPE & ~FLAGS_MASK) ==
			AdComEnums.MAX_DISPLAY_CREATIVE_TYPE;
		//noinspection ConstantValue
		assert AdComEnums.MAX_AD_API_CODE < 32;
	}

	/**
	 * Display ad {@linkplain DisplayAd media} builder.
	 *
	 * @since 1.2
	 * @see #ofDisplayAdBuilder()
	 */
	public static final class Builder {

		private DisplayAd display;
		private boolean needClone;

		private Builder(DisplayAd display) {
			this.display = display;
			this.needClone = true;
		}

		private DisplayAd target() {
			if (this.needClone) {
				this.display = new DisplayAd(this.display);
				this.needClone = false;
			}
			return this.display;
		}

		/**
		 * Set ad identifier, unique to vendor.
		 *
		 * @param id ad identifier
		 * @return {@code this}
		 * @since 1.2
		 * @see DisplayAd#id()
		 */
		@ReturnThis
		@SuppressLint("RestrictedApi")
		public Builder id(String id) {
			this.target().setId(id);
			return this;
		}

		/**
		 * Set ad serving identifier, unique to ad server.
		 *
		 * @param id serving identifier
		 * @return {@code this}
		 * @since 1.2
		 * @see DisplayAd#serveId()
		 */
		@ReturnThis
		@SuppressLint("RestrictedApi")
		public Builder serveId(String id) {
			this.target().setServeId(id);
			return this;
		}

		/**
		 * Set trackers to execute for ad media events.
		 *
		 * @param trkr event trackers
		 * @return {@code this}
		 * @since 1.2
		 * @see DisplayAd#eventTracker(int)
		 */
		@ReturnThis
		@SuppressLint("RestrictedApi")
		public Builder eventTrackers(Collection<AdEventTracker> trkr) {
			this.target().setEventTrackers(trkr);
			return this;
		}

		/**
		 * Set whether ad media assets are delivered securely via HTTPS.
		 *
		 * @param secure {@code true} if, and only if, assets are delivered securely
		 * @return {@code this}
		 * @since 1.2
		 * @see DisplayAd#secure()
		 */
		@ReturnThis
		@SuppressLint("RestrictedApi")
		public Builder secure(boolean secure) {
			this.target().setSecure(secure);
			return this;
		}

		/**
		 * Set minimum duration, in seconds, ad media should be executed for.
		 *
		 * @param secs minimum show duration
		 * @return {@code this}
		 * @since 1.2
		 * @see DisplayAd#minShowDurationSeconds()
		 */
		@ReturnThis
		public Builder minShowDurationSeconds(long secs) {
			this.target().minShowDurationSeconds = secs;
			return this;
		}

		/**
		 * Set URL of buy-side privacy policy.
		 *
		 * @param url privacy policy URL or {@linkplain String#isEmpty() empty} if unavailable
		 * @return {@code this}
		 * @since 1.2
		 * @see DisplayAd#buyerPrivacyPolicyUrl()
		 */
		@ReturnThis
		public Builder buyerPrivacyPolicyUrl(String url) {
			this.target().buyerPrivacyPolicyUrl = url;
			return this;
		}

		/**
		 * Set universal ad identifier registry domain to identifier value mapping.
		 *
		 * @param univIds universal identifier mapping
		 * @return {@code this}
		 * @since 1.2
		 * @see DisplayAd#universalAdIdRegistry(int)
		 * @see DisplayAd#universalAdIdValue(int)
		 */
		@ReturnThis
		public Builder universalAdIds(Map<String, String> univIds) {
			this.target().universalAdIds =
				univIds.isEmpty() ? DEFAULT.universalAdIds :
				CollectionsCompat.arrayMapCopyOf(univIds);
			return this;
		}

		/**
		 * Set icon overlay assets.
		 *
		 * @param icons icon overlays
		 * @return {@code this}
		 * @since 1.2
		 * @see DisplayAd#icon(int)
		 */
		@ReturnThis
		public Builder icons(Collection<IconAsset> icons) {
			this.target().icons = CollectionsCompat.toArrayOrEmpty(icons, DEFAULT.icons);
			return this;
		}

		/**
		 * Set underlying creative to {@linkplain AdComEnums#DisplayCreativeNative native} creative.
		 * <p>If another creative was previously assigned, it is removed.
		 *
		 * @param link top-level navigation link
		 * @param assets creative assets
		 * @return {@code this}
		 * @since 1.2
		 * @see DisplayAd#creativeType()
		 * @see DisplayAd#link()
		 * @see DisplayAd#nativeAsset(int)
		 */
		@ReturnThis
		public Builder nativeCreative(LinkAsset link, Collection<NativeAsset> assets) {
			DisplayAd dst = this.target();

			dst.creativeTypeAndFlags = AdComEnums.DisplayCreativeNative;
			dst.link = link;
			dst.creative = assets.toArray(new NativeAsset[0]);
			return this;
		}

		/**
		 * Set underlying creative to {@linkplain AdComEnums#DisplayCreativeImage banner image}
		 * creative.
		 * <p>If another creative was previously assigned, it is removed.
		 *
		 * @param link link to navigate to when creative is activated (i.e. clicked)
		 * @param imgUrl banner image URL
		 * @return {@code this}
		 * @since 1.2
		 * @see DisplayAd#creativeType()
		 * @see DisplayAd#link()
		 * @see DisplayAd#bannerImageUrl()
		 */
		@ReturnThis
		public Builder bannerImageCreative(LinkAsset link, String imgUrl) {
			DisplayAd dst = this.target();

			dst.creativeTypeAndFlags = AdComEnums.DisplayCreativeImage;
			dst.link = link;
			dst.creative = imgUrl;
			return this;
		}

		/**
		 * Set underlying creative to inline {@linkplain AdComEnums#DisplayCreativeHtml HTML banner}
		 * creative markup.
		 * <p>If another creative was previously assigned, it is removed.
		 *
		 * @param adm creative HTML markup
		 * @return {@code this}
		 * @since 1.2
		 * @see DisplayAd#creativeType()
		 * @see DisplayAd#bannerHtmlMarkup()
		 */
		@ReturnThis
		public Builder bannerHtmlCreativeMarkup(String adm) {
			DisplayAd dst = this.target();

			dst.creativeTypeAndFlags = AdComEnums.DisplayCreativeHtml | FLAG_ADM;
			dst.link = LinkAsset.of();
			dst.creative = adm;
			return this;
		}

		/**
		 * Set underlying creative to inline {@linkplain AdComEnums#DisplayCreativeHtml HTML banner}
		 * creative markup URL.
		 * <p>If another creative was previously assigned, it is removed.
		 *
		 * @param url creative HTML markup
		 * @return {@code this}
		 * @since 1.2
		 * @see DisplayAd#creativeType()
		 * @see DisplayAd#bannerHtmlUrl()
		 */
		@ReturnThis
		public Builder bannerHtmlCreativeUrl(String url) {
			DisplayAd dst = this.target();

			dst.creativeTypeAndFlags = AdComEnums.DisplayCreativeHtml | FLAG_CURL;
			dst.link = LinkAsset.of();
			dst.creative = url;
			return this;
		}

		/**
		 * Set advertising APIs required, by code, to execute creative.
		 *
		 * @param codes required API codes
		 * @return {@code this}
		 * @since 1.2
		 * @see DisplayAd#isAdApiRequired(int)
		 */
		@ReturnThis
		public Builder requiredAdApis(@AdApiCode int... codes) {
			int mask = 0;

			for (int code : codes) {
				if (code >= 0 && code <= AdComEnums.MAX_AD_API_CODE)
					mask |= (1 << code);
			}
			this.target().requiredAdApisMask = mask;
			return this;
		}

		/**
		 * Set exact width, in device independent pixels, of creative.
		 *
		 * @param w creative width
		 * @return {@code this}
		 * @since 1.2
		 * @see DisplayAd#widthDp()
		 * @see #heightDp(int)
		 */
		@ReturnThis
		public Builder widthDp(@Dimension(unit = Dimension.DP) int w) {
			this.target().widthDp = w;
			return this;
		}

		/**
		 * Set exact height, in device independent pixels, of creative.
		 *
		 * @param h creative height
		 * @return {@code this}
		 * @since 1.2
		 * @see DisplayAd#heightDp()
		 * @see #widthDp(int)
		 */
		@ReturnThis
		public Builder heightDp(@Dimension(unit = Dimension.DP) int h) {
			this.target().heightDp = h;
			return this;
		}

		/**
		 * Set relative width, expressed as a ratio, of creative.
		 *
		 * @param wratio creative width ratio
		 * @return {@code this}
		 * @since 1.2
		 * @see DisplayAd#widthRatio()
		 * @see #heightRatio(int)
		 */
		@ReturnThis
		public Builder widthRatio(int wratio) {
			this.target().widthRatio = wratio;
			return this;
		}

		/**
		 * Set relative height, expressed as a ratio, of creative.
		 *
		 * @param hratio creative height ratio
		 * @return {@code this}
		 * @since 1.2
		 * @see DisplayAd#heightRatio()
		 * @see #widthRatio(int)
		 */
		@ReturnThis
		public Builder heightRatio(int hratio) {
			this.target().heightRatio = hratio;
			return this;
		}

		/**
		 * Build resulting display ad media instance.
		 *
		 * @return resulting display instance
		 * @since 1.2
		 */
		public DisplayAd build() {
			this.needClone = true;
			return this.display;
		}
	}

	/**
	 * Default empty display ad media instance.
	 *
	 * @return empty display instance
	 * @since 1.2
	 */
	public static DisplayAd ofDisplayAd() {
		return DEFAULT;
	}

	/**
	 * Construct new empty {@linkplain Builder builder}.
	 *
	 * @return empty builder instance
	 * @since 1.2
	 */
	public static Builder ofDisplayAdBuilder() {
		return DEFAULT.toBuilder();
	}

	/**
	 * Deserialize {@code DisplayAd} from Protobuf message.
	 *
	 * @param reader reader to deserialize from
	 * @return deserialized display ad media
	 * @throws RuntimeException coding is malformed
	 * @since 1.2
	 * @see #toDisplayAdProtobuf(ProtobufWriter)
	 */
	public static DisplayAd ofDisplayAdProtobuf(ProtobufReader reader) {
		DisplayAd rv = new DisplayAd(DEFAULT);
		ArrayMap<String, String> univIds = new ArrayMap<>(0);
		List<IconAsset> icons = new ArrayList<>(0);
		List<AdEventTracker> trackers = new ArrayList<>(0);

		while (reader.hasRemaining()) {
			int tag = reader.readTag();

			if (tag == MINSHOWDUR) {
				rv.minShowDurationSeconds = reader.readInt64();
			} else if (tag == API) {
				rv.requiredAdApisMask = (int) (reader.readWordBitmap(0) & 0xffffffffL);
			} else if (tag == PRIV) {
				rv.buyerPrivacyPolicyUrl = reader.readString();
			} else if (tag == UNIVID) {
				Pair<String, String> univId = reader.readStringPair();

				univIds.put(univId.first, univId.second);
			} else if (tag == ICON) {
				icons.add(reader.readLen(IconAsset::ofProtobuf));
			} else if (tag == ADM || tag == CURL) {
				rv.creativeTypeAndFlags =
					AdComEnums.DisplayCreativeHtml |
					(tag == CURL ? FLAG_CURL : FLAG_ADM);
				rv.creative = reader.readString();
			} else if (tag == BANNER) {
				int cookie = reader.beginReadLen();

				rv.creativeTypeAndFlags = AdComEnums.DisplayCreativeImage;
				rv.creative = "";
				while (reader.hasRemaining()) {
					tag = reader.readTag();
					if (tag == BANNER_IMG)
						rv.creative = reader.readString();
					else if (tag == BANNER_LINK)
						rv.link = reader.readLen(LinkAsset::ofProtobuf);
				}
				reader.endReadLen(cookie);
			} else if (tag == NATIVE) {
				List<NativeAsset> assets = new ArrayList<>();
				int cookie = reader.beginReadLen();

				rv.creativeTypeAndFlags = AdComEnums.DisplayCreativeNative;
				while (reader.hasRemaining()) {
					tag = reader.readTag();
					if (tag == NATIVE_LINK)
						rv.link = reader.readLen(LinkAsset::ofProtobuf);
					else if (tag == NATIVE_ASSET)
						assets.add(reader.readLen(NativeAsset::ofProtobuf));
				}
				rv.creative = assets.toArray(new NativeAsset[0]);
				reader.endReadLen(cookie);
			} else if (tag == EVENT) {
				trackers.add(reader.readLen(AdEventTracker::ofProtobuf));
			} else if (tag == W) {
				rv.widthDp = reader.readInt32();
			} else if (tag == H) {
				rv.heightDp = reader.readInt32();
			} else if (tag == WRATIO) {
				rv.widthRatio = reader.readInt32();
			} else if (tag == HRATIO) {
				rv.heightRatio = reader.readInt32();
			}
		}
		rv.icons = CollectionsCompat.toArrayOrEmpty(icons, DEFAULT.icons);
		rv.setEventTrackers(trackers);
		if (!univIds.isEmpty())
			rv.universalAdIds = univIds;
		return rv;
	}

	private long minShowDurationSeconds;
	private String buyerPrivacyPolicyUrl;
	private ArrayMap<String, String> universalAdIds;
	private IconAsset[] icons;
	private LinkAsset link;
	private Object creative;
	private int creativeTypeAndFlags;
	private int requiredAdApisMask;
	private @Dimension(unit = Dimension.DP) int widthDp;
	private @Dimension(unit = Dimension.DP) int heightDp;
	private int widthRatio;
	private int heightRatio;

	private DisplayAd() {
		super();
		this.buyerPrivacyPolicyUrl = "";
		this.universalAdIds = new ArrayMap<>(0);
		this.icons = new IconAsset[0];
		this.link = LinkAsset.of();
		this.creative = "";
		this.creativeTypeAndFlags = AdComEnums.DisplayCreativeUnknown;
	}

	private DisplayAd(DisplayAd that) {
		super(that);
		this.minShowDurationSeconds = that.minShowDurationSeconds;
		this.buyerPrivacyPolicyUrl = that.buyerPrivacyPolicyUrl;
		this.universalAdIds = that.universalAdIds;
		this.icons = that.icons;
		this.link = that.link;
		this.creative = that.creative;
		this.creativeTypeAndFlags = that.creativeTypeAndFlags;
		this.requiredAdApisMask = that.requiredAdApisMask;
		this.widthDp = that.widthDp;
		this.heightDp = that.heightDp;
		this.widthRatio = that.widthRatio;
		this.heightRatio = that.heightRatio;
	}

	/**
	 * Minimum duration, in seconds, ad media should be executed for.
	 *
	 * @return minimum show duration
	 * @since 1.2
	 * @see Builder#minShowDurationSeconds(long)
	 */
	public long minShowDurationSeconds() {
		return this.minShowDurationSeconds;
	}

	/**
	 * URL of buy-side privacy policy.
	 *
	 * @return privacy policy URL or {@linkplain String#isEmpty() empty} if unavailable
	 * @since 1.2
	 * @see Builder#buyerPrivacyPolicyUrl(String)
	 */
	public String buyerPrivacyPolicyUrl() {
		return this.buyerPrivacyPolicyUrl;
	}

	/**
	 * Universal ad identifier registry to value mapping.
	 *
	 * @return universal ad id mapping
	 */
	ArrayMap<String, String> universalAdIds() {
		return this.universalAdIds;
	}

	/**
	 * Universal ad identifier count.
	 *
	 * @return universal ad id count
	 * @since 1.2
	 * @see Builder#universalAdIds(Map)
	 * @see #universalAdIdRegistry(int)
	 * @see #universalAdIdValue(int)
	 */
	public int universalAdIdCount() {
		return this.universalAdIds.size();
	}

	/**
	 * Top-level domain name of universal ad identifier registry, at index.
	 *
	 * @param i index to retrieve registry domain at
	 * @return registry domain at index {@code i}
	 * @throws IndexOutOfBoundsException {@code i} is negative or, greater than or equal to
	 * universal ad identifier {@linkplain #universalAdIdCount() count}
	 * @since 1.2
	 * @see Builder#universalAdIds(Map)
	 * @see #universalAdIdCount()
	 * @see #universalAdIdValue(int)
	 */
	public String universalAdIdRegistry(int i) {
		return this.universalAdIds.keyAt(i);
	}

	/**
	 * Universal ad identifier, at index.
	 *
	 * @param i index to retrieve identifier at
	 * @return identifier at index {@code i}
	 * @throws IndexOutOfBoundsException {@code i} is negative or, greater than or equal to
	 * universal ad identifier {@linkplain #universalAdIdCount() count}
	 * @since 1.2
	 * @see Builder#universalAdIds(Map)
	 * @see #universalAdIdCount()
	 * @see #universalAdIdRegistry(int)
	 */
	public String universalAdIdValue(int i) {
		return this.universalAdIds.valueAt(i);
	}

	/**
	 * Icon overlay asset count.
	 *
	 * @return icon count
	 * @since 1.2
	 * @see #icon(int)
	 */
	public int iconCount() {
		return this.icons.length;
	}

	/**
	 * Icon overlay asset, at index.
	 *
	 * @param i index to retrieve asset at
	 * @return asset at index {@code i}
	 * @throws IndexOutOfBoundsException {@code i} is negative or, greater than or equal to icon
	 * {@linkplain #iconCount() count}
	 * @since 1.2
	 * @see Builder#icons(Collection)
	 * @see #iconCount()
	 */
	public IconAsset icon(int i) {
		return this.icons[i];
	}

	/**
	 * Top-level navigation link asset.
	 * <p>If {@linkplain #creativeType() creative} is {@linkplain AdComEnums#DisplayCreativeNative
	 * native}, the link returned represents the link to which user should be navigated when an
	 * {@linkplain #nativeAsset(int) asset}, which does not have an associated link, is activated
	 * (i.e. clicked).
	 *
	 * @return navigation link asset
	 * @throws IllegalStateException underlying {@linkplain #creativeType() creative} is not
	 * {@linkplain AdComEnums#DisplayCreativeImage banner image} and
	 * {@linkplain AdComEnums#DisplayCreativeNative native}
	 * @since 1.2
	 * @see #creativeType()
	 * @see Builder#nativeCreative(LinkAsset, Collection)
	 * @see Builder#bannerImageCreative(LinkAsset, String)
	 */
	public LinkAsset link() {
		int ctype = this.creativeType();

		Preconditions.checkState(
			ctype == AdComEnums.DisplayCreativeImage ||
			ctype == AdComEnums.DisplayCreativeNative
		);
		return this.link;
	}

	/**
	 * Display creative type.
	 *
	 * @return creative type
	 * @since 1.2
	 * @see Builder#bannerHtmlCreativeMarkup(String)
	 * @see Builder#bannerHtmlCreativeUrl(String)
	 * @see Builder#bannerImageCreative(LinkAsset, String)
	 * @see Builder#nativeCreative(LinkAsset, Collection)
	 */
	@SuppressLint("WrongConstant")
	public @DisplayCreativeType int creativeType() {
		return this.creativeTypeAndFlags & ~FLAGS_MASK;
	}

	/**
	 * Ensure creative {@linkplain #creativeType() type} is of expected type.
	 *
	 * @param exp expected creative type
	 * @throws IllegalStateException creative type is not {@code exp}
	 */
	private void checkCreativeType(@DisplayCreativeType int exp) {
		Preconditions.checkState(this.creativeType() == exp);
	}

	/**
	 * Banner creative image URL.
	 *
	 * @return image URL
	 * @throws IllegalStateException underlying {@linkplain #creativeType() creative} is not
	 * {@linkplain AdComEnums#DisplayCreativeImage banner image}
	 * @since 1.2
	 * @see Builder#bannerImageCreative(LinkAsset, String)
	 * @see #creativeType()
	 */
	public String bannerImageUrl() {
		this.checkCreativeType(AdComEnums.DisplayCreativeImage);
		return (String) this.creative;
	}

	/**
	 * Native creative asset array.
	 *
	 * @return native asset array
	 * @throws IllegalStateException underlying {@linkplain #creativeType() creative} is not
	 * {@linkplain AdComEnums#DisplayCreativeNative native}
	 */
	private NativeAsset[] nativeAssets() {
		this.checkCreativeType(AdComEnums.DisplayCreativeNative);
		return (NativeAsset[]) this.creative;
	}

	/**
	 * Native creative asset count.
	 *
	 * @return native asset count
	 * @throws IllegalStateException underlying {@linkplain #creativeType() creative} is not
	 * {@linkplain AdComEnums#DisplayCreativeNative native}
	 * @since 1.2
	 * @see Builder#nativeCreative(LinkAsset, Collection)
	 * @see #creativeType()
	 * @see #nativeAsset(int)
	 */
	public int nativeAssetCount() {
		return this.nativeAssets().length;
	}

	/**
	 * Native creative asset, at index.
	 *
	 * @param i index to retrieve asset at
	 * @return native asset at index {@code i}
	 * @throws IllegalStateException underlying {@linkplain #creativeType() creative} is not
	 * {@linkplain AdComEnums#DisplayCreativeNative native}
	 * @throws IndexOutOfBoundsException {@code i} is negative or, greater than or equal to native
	 * asset {@linkplain #nativeAssetCount() count}
	 * @since 1.2
	 * @see Builder#nativeCreative(LinkAsset, Collection)
	 * @see #creativeType()
	 * @see #nativeAssetCount()
	 */
	public NativeAsset nativeAsset(int i) {
		return this.nativeAssets()[i];
	}

	/**
	 * Banner creative HTML markup.
	 *
	 * @return HTML markup or {@linkplain String#isEmpty() empty} if inline markup is not present
	 * @throws IllegalStateException underlying {@linkplain #creativeType() creative} is not
	 * {@linkplain AdComEnums#DisplayCreativeHtml HTML}
	 * @since 1.2
	 * @see Builder#bannerHtmlCreativeMarkup(String)
	 * @see #creativeType()
	 * @see #bannerHtmlUrl()
	 */
	public String bannerHtmlMarkup() {
		this.checkCreativeType(AdComEnums.DisplayCreativeHtml);
		return (this.creativeTypeAndFlags & FLAG_ADM) != 0 ? (String) this.creative : "";
	}

	/**
	 * Banner creative HTML markup URL.
	 *
	 * @return HTML markup URL or {@linkplain String#isEmpty() empty} if markup URL is not present
	 * @throws IllegalStateException underlying {@linkplain #creativeType() creative} is not
	 * {@linkplain AdComEnums#DisplayCreativeHtml HTML}
	 * @since 1.2
	 * @see Builder#bannerHtmlCreativeUrl(String)
	 * @see #creativeType()
	 * @see #bannerHtmlMarkup()
	 */
	public String bannerHtmlUrl() {
		this.checkCreativeType(AdComEnums.DisplayCreativeHtml);
		return (this.creativeTypeAndFlags & FLAG_CURL) != 0 ? (String) this.creative : "";
	}

	/**
	 * Test whether an advertising API, by code, is required to execute creative.
	 *
	 * @param code API code to test
	 * @return {@code true} if, and only if, {@code code} is required
	 * @since 1.2
	 * @see Builder#requiredAdApis(int...)
	 */
	public boolean isAdApiRequired(@AdApiCode int code) {
		return (
			code >= 0 &&
			code <= AdComEnums.MAX_AD_API_CODE &&
			(this.requiredAdApisMask & (1 << code)) != 0
		);
	}

	/**
	 * Exact width, in device independent pixels, of creative.
	 *
	 * @return creative width
	 * @since 1.2
	 * @see Builder#widthDp(int)
	 * @see #heightDp()
	 * @see #widthRatio()
	 */
	public @Dimension(unit = Dimension.DP) int widthDp() {
		return this.widthDp;
	}

	/**
	 * Exact height, in device independent pixels, of creative.
	 *
	 * @return creative height
	 * @since 1.2
	 * @see Builder#heightDp(int)
	 * @see #widthDp()
	 * @see #heightRatio()
	 */
	public @Dimension(unit = Dimension.DP) int heightDp() {
		return this.heightDp;
	}

	/**
	 * Relative width, expressed as a ratio, of creative.
	 *
	 * @return creative width ratio
	 * @since 1.2
	 * @see Builder#widthRatio(int)
	 * @see #heightRatio()
	 * @see #widthDp()
	 */
	public int widthRatio() {
		return this.widthRatio;
	}

	/**
	 * Relative height, expressed as a ratio, of creative.
	 *
	 * @return creative height ratio
	 * @since 1.2
	 * @see Builder#heightRatio(int)
	 * @see #widthRatio()
	 * @see #heightDp()
	 */
	public int heightRatio() {
		return this.heightRatio;
	}

	/**
	 * Construct new {@linkplain Builder builder} initialized with {@code this}.
	 *
	 * @return initialized builder instance
	 * @since 1.2
	 */
	public Builder toBuilder() {
		return new Builder(this);
	}

	/**
	 * Serialize as a {@code DisplayAd} Protobuf message.
	 *
	 * @param writer writer to serialize to
	 * @since 1.2
	 * @see #ofDisplayAdProtobuf(ProtobufReader)
	 */
	public void toDisplayAdProtobuf(ProtobufWriter writer) {
		long cookie;

		writer.writeInt64(MINSHOWDUR, this.minShowDurationSeconds);
		writer.writeWordBitmap(API, Integer.toUnsignedLong(this.requiredAdApisMask), 0);
		writer.writeInt32(W, this.widthDp);
		writer.writeInt32(H, this.heightDp);
		writer.writeInt32(WRATIO, this.widthRatio);
		writer.writeInt32(HRATIO, this.heightRatio);
		writer.writeString(PRIV, this.buyerPrivacyPolicyUrl);
		writer.writeRepeatLen(ICON, this.icons);

		for (int i = 0; i < this.universalAdIds.size(); i++) {
			writer.writeStringPair(UNIVID, new Pair<>(
				this.universalAdIds.keyAt(i),
				this.universalAdIds.valueAt(i)
			));
		}

		switch (this.creativeType()) {
		case AdComEnums.DisplayCreativeHtml:
			writer.writeString(
				(this.creativeTypeAndFlags & FLAG_ADM) != 0 ? ADM : CURL,
				(String) this.creative
			);
			break;
		case AdComEnums.DisplayCreativeImage:
			cookie = writer.beginWriteLen(BANNER);
			writer.writeString(BANNER_IMG, (String) this.creative);
			writer.writeLen(BANNER_LINK, this.link);
			writer.endWriteLen(cookie);
			break;
		case AdComEnums.DisplayCreativeNative:
			cookie = writer.beginWriteLen(NATIVE);
			writer.writeLen(NATIVE_LINK, this.link);
			writer.writeRepeatLen(NATIVE_ASSET, (NativeAsset[]) this.creative);
			writer.endWriteLen(cookie);
			break;
		default:
			break;
		}
		writer.writeRepeatLen(EVENT, super.eventTrackers());
	}
}
