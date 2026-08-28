// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PointF;
import android.util.Pair;
import android.util.SparseBooleanArray;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import org.polygamma.android.origin.adcom.context.Device;
import org.polygamma.android.origin.adcom.context.Geo;
import org.polygamma.android.origin.adcom.context.Regs;
import org.polygamma.android.origin.gppstring.GppIds;
import org.polygamma.android.origin.gppstring.GppString;
import org.polygamma.android.origin.gppstring.Section;
import org.polygamma.android.origin.util.AndroidSettings;
import org.polygamma.android.origin.util.ExecutingService;
import org.polygamma.android.origin.util.Logger;
import org.polygamma.android.origin.util.Preconditions;
import org.polygamma.android.origin.util.Strings;

import java.util.Arrays;
import java.util.Iterator;

/**
 * Module tracking {@linkplain Regs regulations} applicable to device.
 *
 * @since 0.2
 */
public final class RegulationsModule extends OriginModule {

	private static final String TAG = RegulationsModule.class.getSimpleName();

	// TCFv2 - https://github.com/InteractiveAdvertisingBureau/GDPR-Transparency-and-Consent-Framework/blob/master/TCFv2/IAB%20Tech%20Lab%20-%20CMP%20API%20v2.md
	@VisibleForTesting
	static final String TCF_GDPR_APPLIES_KEY			= "IABTCF_gdprApplies";
	@VisibleForTesting
	static final String TCF_TC_STRING_KEY				= "IABTCF_TCString";
	@VisibleForTesting
	static final String TCF_PURPOSE_CONSENTS_KEY		= "IABTCF_PurposeConsents";

	// US Privacy - https://github.com/InteractiveAdvertisingBureau/USPrivacy/blob/master/CCPA/USP%20API.md
	@VisibleForTesting
	static final String US_PRIVACY_STRING				= "IABUSPrivacy_String";

	// GPP - https://github.com/InteractiveAdvertisingBureau/Global-Privacy-Platform/blob/main/Core/CMP%20API%20Specification.md
	@VisibleForTesting
	static final String GPP_STRING_KEY					= "IABGPP_HDR_GppString";
	@VisibleForTesting
	static final String GPP_SID_KEY						= "IABGPP_GppSID";

	private static final String[] PREFERENCE_KEYS =
		{
			TCF_GDPR_APPLIES_KEY,
			TCF_TC_STRING_KEY,
			TCF_PURPOSE_CONSENTS_KEY,
			US_PRIVACY_STRING,
			GPP_STRING_KEY,
			GPP_SID_KEY
		};

	static {
		Arrays.sort(PREFERENCE_KEYS);
	}

	/**
	 * Regulations module name.
	 *
	 * @since 0.2
	 */
	public static final String NAME = "origin.regulations";

	/**
	 * Name of {@linkplain Origin#registerModuleEventCallback(OriginModuleEventCallback, Pair)
	 * event} fired when {@linkplain #regs() regulations} descriptor has materially changed.
	 * <p>This event is fired when regulations {@linkplain Regs descriptor} has materially
	 * changed. The data associated with the event is the return value of {@link #regs()}.
	 *
	 * @since 0.2
	 */
	public static final @OriginModuleEventName String REGS_UPDATE_EVENT = "regs-update";

	/**
	 * Construct a new module {@linkplain Provider provider}.
	 *
	 * @return provider instance
	 * @since 0.2
	 */
	public static Provider<RegulationsModule> ofProvider() {
		return new Provider<RegulationsModule>(RegulationsModule.class) {
			@Override
			protected RegulationsModule load(Origin sdk, Context ctxt) {
				RegulationsModule mod = new RegulationsModule(sdk, ctxt);

				mod.init();
				return mod;
			}
		};
	}

	@VisibleForTesting
	final SharedPreferences preferences;
	private @Nullable SharedPreferences.OnSharedPreferenceChangeListener
		onPreferenceChangeListener;
	private final LocationModule location;
	private final ConnectivityModule connectivity;
	private final DeviceModule device;
	private final OriginModuleEventCallback locationUpdateCallback;
	private final OriginModuleEventBus regsUpdateEvent;
	private @Nullable String region;
	private Regs regs;
	@VisibleForTesting
	final ExecutingService updater;

	@SuppressWarnings("deprecation")
	private RegulationsModule(Origin sdk, Context ctxt) {
		super(NAME, sdk);
		this.preferences = android.preference.PreferenceManager.getDefaultSharedPreferences(ctxt);
		this.location = sdk.loadModule(LocationModule.class);
		this.connectivity = sdk.loadModule(ConnectivityModule.class);
		this.device = sdk.loadModule(DeviceModule.class);
		this.locationUpdateCallback = (_src, _name, _data, _when) -> this.onUpdateLocation();
		this.regsUpdateEvent = super.registerEvent(REGS_UPDATE_EVENT, false);
		this.regs = Regs.of();
		this.updater = ExecutingService.of(NAME, this::update, sdk.backgroundExecutor());
	}

	/**
	 * Descriptor of applicable laws and regulations.
	 *
	 * @return descriptor
	 * @since 0.2
	 */
	public Regs regs() {
		return this.regs;
	}

	private String probeRegion() {
		String region = super.sdk().queryAppMetadata("region").get("region");

		//noinspection DataFlowIssue - `queryAppMetadata()` ensures ids are always mapped
		if (region.isEmpty())
			region = BuildConfig.ORIGIN_SDK_REGION;
		region =
			region.equals("cn") ? "cn" :
			region.equals("eu") ? "eu" :
			region.equals("us") ? "us" :
			"global";
		this.region = region;
		return region;
	}

	/**
	 * SDK region.
	 * <p>Region SDK or application is built for. This region constant is used to determine
	 * the base features, laws, and regulations which are always applicable. This is always equal
	 * to {@code "cn"}, {@code "eu"}, {@code "us"}, or {@code "global"} for China, European Union,
	 * United States, or global, respectively.
	 *
	 * @return region
	 * @since 1.2
	 */
	public String region() {
		return Preconditions.checkNotNullElseGet(this.region, this::probeRegion);
	}

	/**
	 * Test whether Personal Information Protection Law (PIPL), as established by the People's
	 * Republic of China, are applicable.
	 *
	 * @return {@code true} if, and only if, PIPL is applicable
	 * @since 1.2
	 */
	public boolean isPiplApplicable() {
		return "cn".equals(this.region()) || this.regs.pipl();
	}

	/**
	 * Test whether General Data Protection Regulation (GDPR), as established by the European
	 * Union, is applicable.
	 *
	 * @return {@code true} if, and only if, GDPR is applicable
	 * @since 1.2
	 */
	public boolean isGdprApplicable() {
		return "eu".equals(this.region()) || this.regs.gdpr();
	}

	/**
	 * Test whether ISO 3166-1 alpha-2 country code is a country where PIPL applies.
	 *
	 * @param code country code to test
	 * @return {@code true} if, and only if, PIPL laws apply to {@code code}
	 */
	private static boolean isPiplCountry(String code) {
		return (
			"cn".equalsIgnoreCase(code) ||
			"hk".equalsIgnoreCase(code) ||
			"mo".equalsIgnoreCase(code)
		);
	}

	/**
	 * Test whether a mobile country code (MCC) identifies a country where PIPL applies.
	 *
	 * @param mcc MCC to test
	 * @return {@code true} if, and only if, PIPL laws apply to {@code mcc}
	 */
	private static boolean isPiplMcc(int mcc) {
		return mcc == 454 || mcc == 455 || mcc == 460;
	}

	/**
	 * Check whether China's PIPL laws may be applicable to the device based on location and
	 * connectivity.
	 *
	 * @return {@code true} if, and only if, PIPL laws maybe applicable
	 */
	private boolean checkMaybePiplApplies() {
		// On Oppo phones, we have a neat OEM region system property we can use
		for (String setting : new String[] {
			"persist.sys.oem.region",
			"persist.sys.oplus.region"
		}) {
			if ("CN".equalsIgnoreCase(AndroidSettings.getSystemString(setting, "--")))
				return true;
		}

		Device device = this.device.device();

		if (device != null) {
			for (String mccmnc : new String[] {
				device.carrierMccMnc(),
				device.simCarrierMccMnc()
			}) {
				if (isPiplMcc(MccMnc.parse(mccmnc)[0]))
					return true;
			}
		}

		Connectivity conn = this.connectivity.connectivity();

		if (conn != null) {
			for (int i = 0; i < conn.subscriptionCount(); i++) {
				ConnectivitySubscription sub = conn.subscription(i);

				for (String cc : new String[] {
					sub.operatorCountryCode(),
					sub.networkOperatorCountryCode()
				}) {
					if (isPiplCountry(cc))
						return true;
				}
				for (int mcc : new int[] { sub.operatorMcc(), sub.networkOperatorMcc() }) {
					if (isPiplMcc(mcc))
						return true;
				}
			}
		}

		Geo geo = this.location.geo();

		if (geo == null) {
			// not yet polled (shouldn't happen, but that's okay).
			return false;
		}

		return (
			!geo.countryCode().isEmpty() ? isPiplCountry(geo.countryCode()) :
			CountryPolygons.isInside(CountryPolygons.CHINA, new PointF(
				(float) geo.longitudeDegrees(),
				(float) geo.latitudeDegrees()
			))
		);
	}

	/**
	 * Retrieve a {@code boolean} preference.
	 *
	 * @param key preference key
	 * @return preference value
	 */
	private boolean getBooleanPreference(@SuppressWarnings("SameParameterValue") String key) {
		try {
			int val = this.preferences.getInt(key, -1);

			return val != 0 && val != -1;
		} catch (ClassCastException ignored) {
			try {
				return this.preferences.getBoolean(key, false);
			} catch (ClassCastException err) {
				Logger.debug(TAG, "%s is not a boolean key", key, err);
				return false;
			}
		}
	}

	/**
	 * Retrieve a {@code string} preference.
	 *
	 * @param key preference key
	 * @return preference value
	 */
	private String getStringPreference(String key) {
		try {
			return this.preferences.getString(key, "");
		} catch (ClassCastException err) {
			Logger.debug(TAG, "%s is not a string key", key, err);
			return "";
		}
	}

	/**
	 * Retrieve GPP SID preference.
	 *
	 * @return GPP SID
	 */
	private SparseBooleanArray getGppSidPreference() {
		SparseBooleanArray sids = new SparseBooleanArray();

		try {
			switch (this.region()) {
			case "eu":
				sids.put(GppIds.toGppSectionId(GppIds.TcfEuV2.ID), true);
				break;
			case "us":
				sids.put(GppIds.toGppSectionId(GppIds.UsNational.ID), true);
				break;
			case "cn":
				sids.put(GppIds.toGppSectionId(GppIds.CnPrivacyV1.ID), true);
				break;
			}

			Iterator<String> iter = Strings.split(this.getStringPreference(GPP_SID_KEY), '_');

			while (iter.hasNext())
				sids.put(Integer.parseInt(iter.next(), 10), true);
		} catch (Exception err) {
			Logger.warn(TAG, "malformed GPP SID preference", err);
		}
		return sids;
	}

	/**
	 * Construct GPP string from preferences.
	 *
	 * @return GPP string
	 */
	private String gppStringOfPreferences() {
		GppString.Builder gpp = GppString.ofBuilder();
		String pref;

		pref = this.getStringPreference(US_PRIVACY_STRING);
		if (!pref.isEmpty())
			gpp.section(Section.of(GppIds.UsPrivacyV1.ID, pref));
		pref = this.getStringPreference(TCF_TC_STRING_KEY);
		if (!pref.isEmpty())
			gpp.section(Section.of(GppIds.TcfEuV2.ID, pref));

		GppString rv = gpp.build();

		return rv.sectionCount() == 0 ? "" : rv.toString();
	}

	/**
	 * Probe current applicable laws and regulations.
	 *
	 * @return current regulations
	 */
	private Regs probe() {
		String gpp = this.getStringPreference(GPP_STRING_KEY);
		SparseBooleanArray sids = this.getGppSidPreference();
		boolean gdpr = this.getBooleanPreference(TCF_GDPR_APPLIES_KEY);
		boolean pipl = this.checkMaybePiplApplies();

		if (gpp.isEmpty()) {
			try {
				gpp = this.gppStringOfPreferences();
			} catch (Exception err) {
				Logger.info(TAG, "failed to construct GPP string", err);
			}
		}
		if (!gpp.isEmpty()) {
			try {
				GppString str =
					GppString.of(
						gpp,
						err -> Logger.info(TAG, "failed to decode section %s", err)
					);

				for (int i = 0; i < str.sectionCount(); i++)
					sids.put(GppIds.toGppSectionId(str.sectionAt(i).id()), true);
			} catch (Exception err) {
				Logger.info(TAG, "failed to probe section ids", err);
			}
		}

		Regs.Builder update = Regs.ofBuilder();
		int[] sidInts = new int[sids.size()];

		for (int i = 0; i < sids.size(); i++) {
			int sid = sids.keyAt(i);

			sidInts[i] = sid;
			gdpr |= GppIds.toGppSectionId(GppIds.TcfEuV2.ID) == sid;
			pipl |= GppIds.toGppSectionId(GppIds.CnPrivacyV1.ID) == sid;
		}
		update.applicableGppSectionIds(sidInts);

		return update.gdpr(gdpr)
			.pipl(pipl)
			.gpp(gpp)
			.build();
	}

	/**
	 * Update applicable laws and regulations.
	 */
	private void update() {
		Regs update;

		try {
			update = this.probe();
		} catch (Exception err) {
			Logger.warn(TAG, "failed to update regulations", err);
			update = this.regs;
		}

		synchronized (this) {
			if (this.updater.state() != ExecutingService.STATE_RUNNING || (
				this.regs.gpp().equals(update.gpp()) &&
				this.regs.coppa() == update.coppa() &&
				this.regs.gdpr() == update.gdpr() &&
				this.regs.pipl() == update.pipl()
			)) {
				return;
			}
			this.regs = update;
			this.regsUpdateEvent.submit(update);
		}
		Logger.debug(TAG, "updated regulations: %s", update);
	}

	/**
	 * Handle preferences based regulations update.
	 *
	 * @param prefs {@link #preferences}
	 * @param key updated preference key or {@code null} to update all preference keys
	 */
	private void
	onPreferenceChange(@SuppressWarnings("unused") SharedPreferences prefs, @Nullable String key) {
		if (key == null || Arrays.binarySearch(PREFERENCE_KEYS, key) >= 0)
			this.updater.schedule();
	}

	/**
	 * Handle location update.
	 * <p>This primarily checks if the user's location has updated to within China or not. If the
	 * the location has updated to or from within China, the PIPL regulations are enforced or
	 * removed, as required.
	 */
	private void onUpdateLocation() {
		if (this.checkMaybePiplApplies() != this.regs.pipl())
			this.updater.schedule();
	}

	/**
	 * Initialize module.
	 */
	private void init() {
		this.updater.schedule();
		if (this.onPreferenceChangeListener == null) {
			SharedPreferences.OnSharedPreferenceChangeListener listener = this::onPreferenceChange;

			try {
				this.preferences.registerOnSharedPreferenceChangeListener(listener);
				this.onPreferenceChangeListener = listener;
			} catch (Exception cause) {
				Logger.info(TAG, "failed to listen for regulation preference changes", cause);
			}
		}
		this.location.registerEventCallback(
			this.locationUpdateCallback,
			LocationModule.GEOS_UPDATE_EVENT
		);
		this.connectivity.registerEventCallback(
			this.locationUpdateCallback,
			ConnectivityModule.CONNECTIVITY_UPDATE_EVENT
		);
		this.device.registerEventCallback(
			this.locationUpdateCallback,
			DeviceModule.DEVICE_UPDATE_EVENT
		);
	}

	@Override
	protected void destroy() {
		if (this.onPreferenceChangeListener != null) {
			this.preferences
				.unregisterOnSharedPreferenceChangeListener(this.onPreferenceChangeListener);
		}
		this.location.unregisterEventCallback(
			this.locationUpdateCallback,
			LocationModule.GEOS_UPDATE_EVENT
		);
		this.connectivity.unregisterEventCallback(
			this.locationUpdateCallback,
			ConnectivityModule.CONNECTIVITY_UPDATE_EVENT
		);
		this.device.unregisterEventCallback(
			this.locationUpdateCallback,
			DeviceModule.DEVICE_UPDATE_EVENT
		);
		this.updater.shutdown();
	}
}
