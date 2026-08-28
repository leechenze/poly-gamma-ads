// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.adcom.context;

import static org.polygamma.android.origin.protobuf.ProtobufField.*;

import android.annotation.SuppressLint;
import android.util.Pair;

import androidx.annotation.Px;
import androidx.annotation.ReturnThis;

import org.polygamma.android.origin.adcom.enums.AdComEnums;
import org.polygamma.android.origin.adcom.enums.ConnectionType;
import org.polygamma.android.origin.adcom.enums.DeviceType;
import org.polygamma.android.origin.adcom.enums.OsCode;
import org.polygamma.android.origin.protobuf.ProtobufReader;
import org.polygamma.android.origin.protobuf.ProtobufSerializable;
import org.polygamma.android.origin.protobuf.ProtobufWriter;
import org.polygamma.android.origin.util.CollectionsCompat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Context describing device on which ad media will be rendered.
 *
 * @since 0.1
 * @see <a href="https://github.com/InteractiveAdvertisingBureau/AdCOM/blob/main/AdCOM%20v1.0%20FINAL.md#object--device-">AdCOM, version 1.0 - Object: Device</a>
 */
public final class Device implements ProtobufSerializable {

	private static final @Tag int TYPE			= ofInt32(       1);
	private static final @Tag int UA			= ofString(      2);
	/*private static final @Tag int SUA			= ofMessage(     3);*/
	/*private static final @Tag int IFA			= ofString(      4);*/
	/*private static final @Tag int DNT			= ofBool(        5);*/
	private static final @Tag int LMT			= ofBool(        6);
	private static final @Tag int MAKE			= ofString(      7);
	private static final @Tag int MODEL			= ofString(      8);
	private static final @Tag int OS			= ofInt32(       9);
	private static final @Tag int OSV			= ofString(     10);
	private static final @Tag int HWV			= ofString(     11);
	private static final @Tag int H				= ofInt32(      12);
	private static final @Tag int W				= ofInt32(      13);
	private static final @Tag int PPI			= ofInt32(      14);
	private static final @Tag int PXRATIO		= ofFloat(      15);
	/*private static final @Tag int JS			= ofBool(       16);*/
	/*private static final @Tag int LANG		= ofString(     17);*/
	private static final @Tag int LANGB			= ofString(     18);
	/*private static final @Tag int IP			= ofString(     19);*/
	/*private static final @Tag int IPV6		= ofString(     20);*/
	/*private static final @Tag int XFF			= ofString(     21);*/
	/*private static final @Tag int IPTR		= ofBool(       22);*/
	private static final @Tag int CARRIER		= ofString(     23);
	private static final @Tag int MCCMNC		= ofString(     24);
	private static final @Tag int MCCMNCSIM		= ofString(     25);
	private static final @Tag int CONTYPE		= ofInt32(      26);
	/*private static final @Tag int GEOFETCH	= ofBool(       27);*/
	private static final @Tag int GEO			= ofMessage(    28);
	private static final @Tag int ALLIFA		= ofStringPair(500);
	private static final @Tag int EXTRALANGB	= ofString(    501);
	private static final @Tag int NIGHTMODE		= ofBool(      502);
	private static final @Tag int LANDSCAPE		= ofBool(      503);
	private static final @Tag int JSSANDBOX		= ofBool(      504);

	private static final int FLAG_LMT			= 0x01;
	private static final int FLAG_NIGHTMODE		= 0x02;
	private static final int FLAG_LANDSCAPE		= 0x04;
	private static final int FLAG_JSSANDBOX		= 0x08;


	/**
	 * Empty device context.
	 */
	private static final Device DEFAULT = new Device();

	/**
	 * {@linkplain Device} builder.
	 *
	 * @since 0.1
	 * @see #ofBuilder()
	 */
	public static final class Builder {

		private Device device;
		private boolean needClone;

		private Builder(Device device) {
			this.device = device;
			this.needClone = true;
		}

		private Device target() {
			if (this.needClone) {
				this.device = new Device(this.device);
				this.needClone = false;
			}
			return this.device;
		}

		/**
		 * Set device type.
		 *
		 * @param type type or {@code 0} if unknown
		 * @return {@code this}
		 * @since 0.1
		 * @see Device#type()
		 */
		@ReturnThis
		public Builder type(@DeviceType int type) {
			this.target().type = type;
			return this;
		}

		/**
		 * Set device id, sanctioned for advertising.
		 *
		 * @param ifa tuples of id type and id
		 * @return {@code this}
		 * @since 1.2
		 * @see Device#advertisingId(int)
		 */
		@ReturnThis
		public Builder advertisingIds(Collection<Pair<String, String>> ifa) {
			this.target().advertisingIds =
				CollectionsCompat.toArrayOrEmpty(ifa, DEFAULT.advertisingIds);
			return this;
		}

		@ReturnThis
		private Builder toggleFlag(int flag, boolean set) {
			Device dst = this.target();

			if (set)
				dst.flags |= flag;
			else
				dst.flags &= ~flag;
			return this;
		}

		/**
		 * Set whether the Limit Ad Tracking signal is active for device.
		 *
		 * @param lmt {@code true} if, and only if, Limit Ad Tracking is active
		 * @return {@code this}
		 * @since 0.1
		 * @see Device#limitAdTracking()
		 */
		@ReturnThis
		@SuppressLint("ReturnThis")
		public Builder limitAdTracking(boolean lmt) {
			return this.toggleFlag(FLAG_LMT, lmt);
		}

		/**
		 * Set device manufacturer name.
		 *
		 * @param make name or {@linkplain String#isEmpty() empty} if unknown
		 * @return {@code this}
		 * @since 1.2
		 * @see Device#manufacturerName()
		 */
		@ReturnThis
		public Builder manufacturerName(String make) {
			this.target().manufacturerName = make;
			return this;
		}

		/**
		 * Set device model name.
		 *
		 * @param model name or {@linkplain String#isEmpty() empty} if unknown
		 * @return {@code this}
		 * @since 1.2
		 * @see Device#modelName()
		 */
		@ReturnThis
		public Builder modelName(String model) {
			this.target().modelName = model;
			return this;
		}

		/**
		 * Set device operating-system.
		 *
		 * @param os operating-system or {@code 0} if unknown
		 * @return {@code this}
		 * @since 0.1
		 * @see Device#operatingSystem()
		 */
		@ReturnThis
		public Builder operatingSystem(@OsCode int os) {
			this.target().operatingSystem = os;
			return this;
		}

		/**
		 * Set device operating-system version.
		 *
		 * @param osv version or {@linkplain String#isEmpty() empty} if unknown
		 * @return {@code this}
		 * @since 0.1
		 * @see Device#operatingSystemVersion()
		 */
		@ReturnThis
		public Builder operatingSystemVersion(String osv) {
			this.target().operatingSystemVersion = osv;
			return this;
		}

		/**
		 * Set device hardware version.
		 *
		 * @param hwv version or {@linkplain String#isEmpty() empty} if unknown
		 * @return {@code this}
		 * @since 1.2
		 * @see Device#modelVersion()
		 */
		@ReturnThis
		public Builder modelVersion(String hwv) {
			this.target().modelVersion = hwv;
			return this;
		}

		/**
		 * Set user agent string of device.
		 *
		 * @param ua user agent string or {@linkplain String#isEmpty() empty} if unknown
		 * @return {@code this}
		 * @since 1.2
		 * @see Device#userAgent()
		 */
		@ReturnThis
		public Builder userAgent(String ua) {
			this.target().userAgent = ua;
			return this;
		}

		/**
		 * Set screen height, in pixels.
		 *
		 * @param h height or {@code 0} if unknown
		 * @return {@code this}
		 * @since 1.2
		 * @see Device#screenHeightPx()
		 * @see #screenWidthPx(int)
		 * @see #screenPixelsPerInch(int)
		 * @see #screenPixelRatio(float)
		 */
		@ReturnThis
		public Builder screenHeightPx(@Px int h) {
			this.target().screenHeightPx = h;
			return this;
		}

		/**
		 * Set screen width, in pixels.
		 *
		 * @param w width or {@code 0} if unknown
		 * @return {@code this}
		 * @since 0.1
		 * @see Device#screenWidthPx()
		 * @see #screenHeightPx(int)
		 * @see #screenPixelsPerInch(int)
		 * @see #screenPixelRatio(float)
		 */
		@ReturnThis
		public Builder screenWidthPx(@Px int w) {
			this.target().screenWidthPx = w;
			return this;
		}

		/**
		 * Set screen size, expressed as pixels per linear inch.
		 *
		 * @param ppi size or {@code 0} if unknown
		 * @return {@code this}
		 * @since 0.1
		 * @see Device#screenPixelsPerInch()
		 * @see Device#screenPixelRatio()
		 * @see #screenWidthPx(int)
		 * @see #screenHeightPx(int)
		 * @see #screenPixelRatio(float)
		 */
		@ReturnThis
		public Builder screenPixelsPerInch(int ppi) {
			this.target().screenPixelsPerInch = ppi;
			return this;
		}

		/**
		 * Set ratio of physical screen pixels to device independent pixels (DIPS).
		 *
		 * @param pxratio ratio or {@code 0} if unknown
		 * @return {@code this}
		 * @since 0.1
		 * @see Device#screenPixelRatio()
		 * @see #screenWidthPx(int)
		 * @see #screenHeightPx(int)
		 * @see #screenPixelsPerInch(int)
		 */
		@ReturnThis
		public Builder screenPixelRatio(float pxratio) {
			this.target().screenPixelRatio = pxratio;
			return this;
		}

		/**
		 * Set whether device is in landscape or portrait mode.
		 *
		 * @param orient {@code true} or {@code false} if device is in landscape or portrait mode,
		 * respectively
		 * @return {@code this}
		 * @since 0.1
		 * @see Device#landscape()
		 */
		@ReturnThis
		@SuppressLint("ReturnThis")
		public Builder landscape(boolean orient) {
			return this.toggleFlag(FLAG_LANDSCAPE, orient);
		}

		/**
		 * Set whether device has dark or light theme enabled.
		 *
		 * @param on {@code true} or {@code false} if device has dark or light theme enabled,
		 * respectively
		 * @return {@code this}
		 * @since 0.1
		 * @see Device#nightMode()
		 */
		@ReturnThis
		@SuppressLint("ReturnThis")
		public Builder nightMode(boolean on) {
			return this.toggleFlag(FLAG_NIGHTMODE, on);
		}

		/**
		 * Set whether device supports JavaScript sandbox.
		 *
		 * @param has {@code true} if, and only if, JavaScript sandbox execution is supported
		 * @return {@code this}
		 * @since 1.2
		 * @see Device#supportsJavaScriptSandbox()
		 */
		@ReturnThis
		@SuppressLint("ReturnThis")
		public Builder supportsJavaScriptSandbox(boolean has) {
			return this.toggleFlag(FLAG_JSSANDBOX, has);
		}

		/**
		 * Set IETF BCP 47 code identifying primary language device is configured for.
		 *
		 * @param langb code or {@linkplain String#isEmpty() empty} if unknown
		 * @return {@code this}
		 * @since 0.1
		 * @see Device#languageCode()
		 * @see #extraLanguageCodes(Collection)
		 */
		@ReturnThis
		public Builder languageCode(String langb) {
			this.target().languageCode = langb;
			return this;
		}

		/**
		 * Set IETF BCP 47 codes identifying secondary languages device supports.
		 *
		 * @param langb codes, possibly empty
		 * @return {@code this}
		 * @since 1.2
		 * @see #languageCode(String)
		 * @see Device#extraLanguageCode(int)
		 */
		@ReturnThis
		public Builder extraLanguageCodes(Collection<String> langb) {
			this.target().extraLanguageCodes = CollectionsCompat.toStringArrayOrEmpty(langb);
			return this;
		}

		/**
		 * Set carrier or ISP through which device is connected.
		 *
		 * @param carrier carrier or ISP, or {@linkplain String#isEmpty() empty} if unknown or
		 * disconnected
		 * @return {@code this}
		 * @since 1.2
		 * @see Device#carrierName()
		 * @see #carrierMccMnc(String)
		 * @see #simCarrierMccMnc(String)
		 * @see #connectionType(int)
		 */
		@ReturnThis
		public Builder carrierName(String carrier) {
			this.target().carrierName = carrier;
			return this;
		}

		/**
		 * Set MCCMNC code of carrier through which device is connected.
		 *
		 * @param mccmnc code or, {@linkplain String#isEmpty() empty} if not connected through a
		 * carrier
		 * @return {@code this}
		 * @since 0.1
		 * @see Device#carrierMccMnc()
		 * @see #carrierName(String)
		 * @see #simCarrierMccMnc(String)
		 * @see #connectionType(int)
		 */
		@ReturnThis
		public Builder carrierMccMnc(String mccmnc) {
			this.target().carrierMccMnc = mccmnc;
			return this;
		}

		/**
		 * Set MCCMNC code of carrier which provides cellular capabilities to device.
		 *
		 * @param mccmncsim code or, {@linkplain String#isEmpty() empty} if unknown or unavailable
		 * @return {@code this}
		 * @since 0.1
		 * @see Device#simCarrierMccMnc()
		 * @see #carrierName(String)
		 * @see #carrierMccMnc(String)
		 * @see #connectionType(int)
		 */
		@ReturnThis
		public Builder simCarrierMccMnc(String mccmncsim) {
			this.target().simCarrierMccMnc = mccmncsim;
			return this;
		}

		/**
		 * Set network connection type of device.
		 *
		 * @param contype connection type or, {@code 0} if unknown or disconnected
		 * @return {@code this}
		 * @since 0.1
		 * @see Device#connectionType()
		 * @see #carrierName(String)
		 * @see #carrierMccMnc(String)
		 * @see #simCarrierMccMnc(String)
		 */
		@ReturnThis
		public Builder connectionType(@ConnectionType int contype) {
			this.target().connectionType = contype;
			return this;
		}

		/**
		 * Set gegraphic location contexts of device.
		 *
		 * @param geo location contexts, possibly empty
		 * @return {@code this}
		 * @since 1.2
		 * @see Device#geo(int)
		 */
		@ReturnThis
		public Builder geos(Collection<Geo> geo) {
			this.target().geos = CollectionsCompat.toArrayOrEmpty(geo, DEFAULT.geos);
			return this;
		}

		/**
		 * Build resulting device.
		 *
		 * @return resulting device instance.
		 * @since 1.2
		 */
		public Device build() {
			this.needClone = true;
			return this.device;
		}
	}

	/**
	 * Empty device instance.
	 *
	 * @return device instance
	 * @since 1.2
	 */
	public static Device of() {
		return DEFAULT;
	}

	/**
	 * Construct new empty {@linkplain Builder builder}.
	 *
	 * @return empty builder instance
	 * @since 0.1
	 */
	public static Builder ofBuilder() {
		return DEFAULT.toBuilder();
	}

	/**
	 * Deserialize device from Protobuf message.
	 *
	 * @param reader reader to deserialize from
	 * @return deserialized device
	 * @throws RuntimeException coding is malformed
	 * @since 1.2
	 */
	public static Device ofProtobuf(ProtobufReader reader) {
		Device rv = new Device(DEFAULT);
		List<String> extraLangb = new ArrayList<>();
		List<Pair<String, String>> allIfa = new ArrayList<>();
		List<Geo> geos = new ArrayList<>();

		while (reader.hasRemaining()) {
			int tag = reader.readTag();

			if (tag == UA) {
				rv.userAgent = reader.readString();
			} else if (tag == MAKE) {
				rv.manufacturerName = reader.readString();
			} else if (tag == MODEL) {
				rv.modelName = reader.readString();
			} else if (tag == HWV) {
				rv.modelVersion = reader.readString();
			} else if (tag == TYPE) {
				rv.type = reader.readInt32();
			} else if (tag == OS) {
				rv.operatingSystem = reader.readInt32();
			} else if (tag == OSV) {
				rv.operatingSystemVersion = reader.readString();
			} else if (tag == PXRATIO) {
				rv.screenPixelRatio = reader.readFloat();
			} else if (tag == PPI) {
				rv.screenPixelsPerInch = reader.readInt32();
			} else if (tag == W) {
				rv.screenWidthPx = reader.readInt32();
			} else if (tag == H) {
				rv.screenHeightPx = reader.readInt32();
			} else if (tag == CONTYPE) {
				rv.connectionType = reader.readInt32();
			} else if (tag == CARRIER) {
				rv.carrierName = reader.readString();
			} else if (tag == MCCMNC) {
				rv.carrierMccMnc = reader.readString();
			} else if (tag == MCCMNCSIM) {
				rv.simCarrierMccMnc = reader.readString();
			} else if (tag == LANGB) {
				rv.languageCode = reader.readString();
			} else if (tag == EXTRALANGB) {
				extraLangb.add(reader.readString());
			} else if (tag == ALLIFA) {
				allIfa.add(reader.readStringPair());
			} else if (tag == GEO) {
				geos.add(reader.readLen(Geo::ofProtobuf));
			} else if (tag == LMT) {
				rv.flags |= reader.readBool() ? FLAG_LMT : 0;
			} else if (tag == NIGHTMODE) {
				rv.flags |= reader.readBool() ? FLAG_NIGHTMODE : 0;
			} else if (tag == LANDSCAPE) {
				rv.flags |= reader.readBool() ? FLAG_LANDSCAPE : 0;
			} else if (tag == JSSANDBOX) {
				rv.flags |= reader.readBool() ? FLAG_JSSANDBOX : 0;
			}
		}
		rv.extraLanguageCodes = CollectionsCompat.toStringArrayOrEmpty(extraLangb);
		rv.advertisingIds = CollectionsCompat.toArrayOrEmpty(allIfa, rv.advertisingIds);
		rv.geos = CollectionsCompat.toArrayOrEmpty(geos, rv.geos);
		return rv;
	}

	private String userAgent;
	private String manufacturerName;
	private String modelName;
	private String modelVersion;
	private @DeviceType int type;
	private @OsCode int operatingSystem;
	private String operatingSystemVersion;
	private float screenPixelRatio;
	private int screenPixelsPerInch;
	private @Px int screenWidthPx;
	private @Px int screenHeightPx;
	private @ConnectionType int connectionType;
	private String carrierName;
	private String carrierMccMnc;
	private String simCarrierMccMnc;
	private String languageCode;
	private String[] extraLanguageCodes;
	private Pair<String, String>[] advertisingIds;
	private Geo[] geos;
	private int flags;

	@SuppressWarnings("unchecked")
	private Device() {
		this.userAgent = "";
		this.manufacturerName = "";
		this.modelName = "";
		this.modelVersion = "";
		this.type = AdComEnums.DeviceUnknown;
		this.operatingSystem = AdComEnums.OsUnknown;
		this.operatingSystemVersion = "";
		this.connectionType = AdComEnums.ConnectionUnknown;
		this.carrierName = "";
		this.carrierMccMnc = "";
		this.simCarrierMccMnc = "";
		this.languageCode = "";
		this.extraLanguageCodes = CollectionsCompat.toStringArrayOrEmpty(Collections.emptyList());
		this.advertisingIds = (Pair<String, String>[]) new Pair<?, ?>[0];
		this.geos = new Geo[0];
	}

	private Device(Device that) {
		this.userAgent = that.userAgent;
		this.manufacturerName = that.manufacturerName;
		this.modelName = that.modelName;
		this.modelVersion = that.modelVersion;
		this.type = that.type;
		this.operatingSystem = that.operatingSystem;
		this.operatingSystemVersion = that.operatingSystemVersion;
		this.screenPixelRatio = that.screenPixelRatio;
		this.screenPixelsPerInch = that.screenPixelsPerInch;
		this.screenWidthPx = that.screenWidthPx;
		this.screenHeightPx = that.screenHeightPx;
		this.connectionType = that.connectionType;
		this.carrierName = that.carrierName;
		this.carrierMccMnc = that.carrierMccMnc;
		this.simCarrierMccMnc = that.simCarrierMccMnc;
		this.languageCode = that.languageCode;
		this.extraLanguageCodes = that.extraLanguageCodes;
		this.advertisingIds = that.advertisingIds;
		this.geos = that.geos;
		this.flags = that.flags;
	}

	/**
	 * Device type.
	 *
	 * @return type or {@code 0} if unknown
	 * @since 0.1
	 * @see Builder#type(int)
	 */
	public @DeviceType int type() {
		return this.type;
	}

	/**
	 * Number of ids, sanctioned for advertising, available for device.
	 *
	 * @return advertising id count
	 * @since 1.2
	 * @see #advertisingId(int)
	 * @see Builder#advertisingIds(Collection)
	 */
	public int advertisingIdCount() {
		return this.advertisingIds.length;
	}

	/**
	 * Advertising sanctioned id type and value tuple at index.
	 *
	 * @param i index to retrieve id at
	 * @return tuple of id type and id value at index
	 * @throws IndexOutOfBoundsException {@code i} is negative or, greater than or equal to id
	 * {@linkplain #advertisingIdCount() count}
	 * @since 1.2
	 * @see #advertisingIdCount()
	 * @see Builder#advertisingIds(Collection)
	 */
	public Pair<String, String> advertisingId(int i) {
		return this.advertisingIds[i];
	}

	/**
	 * Limit Ad Tracking signal is active for device.
	 *
	 * @return {@code true} if, and only if, Limit Ad Tracking is active
	 * @since 0.1
	 * @see Builder#limitAdTracking(boolean)
	 */
	public boolean limitAdTracking() {
		return (this.flags & FLAG_LMT) != 0;
	}

	/**
	 * Device manufacturer name.
	 *
	 * @return name or {@linkplain String#isEmpty() empty} if unknown
	 * @since 1.2
	 * @see Builder#manufacturerName(String)
	 */
	public String manufacturerName() {
		return this.manufacturerName;
	}

	/**
	 * Device model name.
	 *
	 * @return name or {@linkplain String#isEmpty() empty} if unknown
	 * @since 1.2
	 * @see Builder#modelName(String)
	 */
	public String modelName() {
		return this.modelName;
	}

	/**
	 * Device hardware version.
	 *
	 * @return version or {@linkplain String#isEmpty() empty} if unknown
	 * @since 1.2
	 * @see Builder#modelVersion(String)
	 */
	public String modelVersion() {
		return this.modelVersion;
	}

	/**
	 * Device operating-system.
	 *
	 * @return operating-system
	 * @since 0.1
	 * @see Builder#operatingSystem(int)
	 */
	public @OsCode int operatingSystem() {
		return this.operatingSystem;
	}

	/**
	 * Device operating-system version.
	 *
	 * @return version or {@linkplain String#isEmpty() empty} if unknown
	 * @since 0.1
	 * @see Builder#operatingSystemVersion(String)
	 */
	public String operatingSystemVersion() {
		return this.operatingSystemVersion;
	}

	/**
	 * User agent string.
	 *
	 * @return device user agent string or {@linkplain String#isEmpty() empty} if unknown
	 * @since 1.2
	 * @see Builder#userAgent(String)
	 */
	public String userAgent() {
		return this.userAgent;
	}

	/**
	 * Screen height, in pixels.
	 *
	 * @return height or {@code 0} if unknown
	 * @since 1.2
	 * @see Builder#screenHeightPx(int)
	 * @see #screenWidthPx()
	 * @see #screenPixelsPerInch()
	 * @see #screenPixelRatio()
	 */
	public @Px int screenHeightPx() {
		return this.screenHeightPx;
	}

	/**
	 * Screen width, in pixels.
	 *
	 * @return width or {@code 0} if unknown
	 * @since 1.2
	 * @see Builder#screenWidthPx(int)
	 * @see #screenHeightPx()
	 * @see #screenPixelsPerInch()
	 * @see #screenPixelRatio()
	 */
	public @Px int screenWidthPx() {
		return this.screenWidthPx;
	}

	/**
	 * Screen size, expressed as pixels per linear inch.
	 *
	 * @return size or {@code 0} if unknown
	 * @since 0.1
	 * @see Builder#screenPixelsPerInch(int)
	 * @see #screenWidthPx()
	 * @see #screenHeightPx()
	 * @see #screenPixelRatio()
	 */
	public int screenPixelsPerInch() {
		return this.screenPixelsPerInch;
	}

	/**
	 * Ratio of physical screen pixels to device independent pixels (DIPS).
	 *
	 * @return ratio or {@code 0} if unknown
	 * @since 0.1
	 * @see Builder#screenPixelRatio(float)
	 * @see #screenWidthPx()
	 * @see #screenHeightPx()
	 * @see #screenPixelsPerInch()
	 */
	public float screenPixelRatio() {
		return this.screenPixelRatio;
	}

	/**
	 * Device is in landscape mode.
	 *
	 * @return {@code true} or {@code false} if device is in landscape or portrait mode,
	 * respectively
	 * @since 0.1
	 * @see Builder#landscape(boolean)
	 */
	public boolean landscape() {
		return (this.flags & FLAG_LANDSCAPE) != 0;
	}

	/**
	 * Device has dark theme enabled.
	 *
	 * @return {@code true} or {@code false} if device has dark or light theme enabled,
	 * respectively
	 * @since 0.1
	 * @see Builder#nightMode(boolean)
	 */
	public boolean nightMode() {
		return (this.flags & FLAG_NIGHTMODE) != 0;
	}

	/**
	 * Device supports JavaScript sandbox execution.
	 *
	 * @return {@code true} if, and only if, sandbox execution is supported
	 * @since 1.2
	 * @see Builder#supportsJavaScriptSandbox(boolean)
	 */
	public boolean supportsJavaScriptSandbox() {
		return (this.flags & FLAG_JSSANDBOX) != 0;
	}

	/**
	 * IETF BCP 47 code identifying primary language device is configured for.
	 *
	 * @return code or {@linkplain String#isEmpty() empty} if unknown
	 * @since 0.1
	 * @see Builder#languageCode(String)
	 * @see #extraLanguageCode(int)
	 */
	public String languageCode() {
		return this.languageCode;
	}

	/**
	 * Count of IETF BCP 47 codes identifying secondary languages device supports.
	 *
	 * @return language code count
	 * @since 1.2
	 * @see #extraLanguageCode(int)
	 * @see Builder#extraLanguageCodes(Collection)
	 */
	public int extraLanguageCodeCount() {
		return this.extraLanguageCodes.length;
	}

	/**
	 * IETF BCP 47 code identifying secondary language device supports, at index.
	 *
	 * @param i index to retrieve language code at
	 * @return language code at index {@code i}
	 * @throws IndexOutOfBoundsException {@code i} is negative or, greater than or equal to
	 * language {@linkplain #extraLanguageCodeCount() count}
	 * @since 1.2
	 * @see #extraLanguageCodeCount()
	 * @see Builder#extraLanguageCodes(Collection)
	 */
	public String extraLanguageCode(int i) {
		return this.extraLanguageCodes[i];
	}

	/**
	 * Carrier or ISP through which device is connected.
	 *
	 * @return carrier or ISP, or {@linkplain String#isEmpty() empty} if unknown or disconnected
	 * @since 1.2
	 * @see Builder#carrierName(String)
	 * @see #carrierMccMnc()
	 * @see #simCarrierMccMnc()
	 * @see #connectionType()
	 */
	public String carrierName() {
		return this.carrierName;
	}

	/**
	 * MCCMNC code of carrier through which device is connected.
	 *
	 * @return code or, {@linkplain String#isEmpty() empty} if not connected through a carrier
	 * @since 0.1
	 * @see Builder#carrierMccMnc(String)
	 * @see #carrierName()
	 * @see #simCarrierMccMnc()
	 * @see #connectionType()
	 */
	public String carrierMccMnc() {
		return this.carrierMccMnc;
	}

	/**
	 * MCCMNC code of carrier which provides cellular capabilities to device.
	 *
	 * @return code or, {@linkplain String#isEmpty() empty} if unknown or unavailable
	 * @since 0.1
	 * @see Builder#simCarrierMccMnc(String)
	 * @see #carrierName()
	 * @see #carrierMccMnc()
	 * @see #connectionType()
	 */
	public String simCarrierMccMnc() {
		return this.simCarrierMccMnc;
	}

	/**
	 * Network connection type of device.
	 *
	 * @return connection type or, {@code 0} if unknown or disconnected
	 * @since 0.1
	 * @see Builder#connectionType(int)
	 * @see #carrierName()
	 * @see #carrierMccMnc()
	 * @see #simCarrierMccMnc()
	 */
	public @ConnectionType int connectionType() {
		return this.connectionType;
	}

	/**
	 * Count of geographic location contexts of device.
	 *
	 * @return location context count
	 * @since 1.2
	 * @see #geo(int)
	 * @see Builder#geos(Collection)
	 */
	public int geoCount() {
		return this.geos.length;
	}

	/**
	 * Geographic location context of device at index.
	 *
	 * @param i index to retrieve location context at
	 * @return location context at index {@code i}
	 * @throws IndexOutOfBoundsException {@code i} is negative or, greater than or equal to
	 * location context {@linkplain #geoCount() count}
	 * @since 1.2
	 * @see #geoCount()
	 * @see Builder#geos(Collection)
	 */
	public Geo geo(int i) {
		return this.geos[i];
	}

	/**
	 * Construct new builder initialized from {@code this}.
	 *
	 * @return builder instance
	 * @since 1.2
	 */
	public Builder toBuilder() {
		return new Builder(this);
	}

	@Override
	public void toProtobuf(ProtobufWriter writer) {
		writer.writeString(UA, this.userAgent);
		writer.writeString(MAKE, this.manufacturerName);
		writer.writeString(MODEL, this.modelName);
		writer.writeString(HWV, this.modelVersion);
		writer.writeInt32(TYPE, this.type);
		writer.writeInt32(OS, this.operatingSystem);
		writer.writeString(OSV, this.operatingSystemVersion);
		writer.writeFloat(PXRATIO, this.screenPixelRatio);
		writer.writeInt32(PPI, this.screenPixelsPerInch);
		writer.writeInt32(W, this.screenWidthPx);
		writer.writeInt32(H, this.screenHeightPx);
		writer.writeInt32(CONTYPE, this.connectionType);
		writer.writeString(CARRIER, this.carrierName);
		writer.writeString(MCCMNC, this.carrierMccMnc);
		writer.writeString(MCCMNCSIM, this.simCarrierMccMnc);
		writer.writeString(LANGB, this.languageCode);
		writer.writeRepeatString(EXTRALANGB, this.extraLanguageCodes);
		writer.writeRepeatStringPair(ALLIFA, this.advertisingIds);
		writer.writeRepeatLen(GEO, this.geos);
		writer.writeBool(LMT, this.limitAdTracking());
		writer.writeBool(NIGHTMODE, this.nightMode());
		writer.writeBool(LANDSCAPE, this.landscape());
		writer.writeBool(JSSANDBOX, this.supportsJavaScriptSandbox());
	}
}
