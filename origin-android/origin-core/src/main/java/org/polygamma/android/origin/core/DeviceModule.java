// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.core;

import android.app.ActivityManager;
import android.app.UiModeManager;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.LocaleList;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Pair;
import android.view.Display;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;

import org.polygamma.android.origin.adcom.context.Device;
import org.polygamma.android.origin.adcom.enums.AdComEnums;
import org.polygamma.android.origin.adcom.enums.DeviceType;
import org.polygamma.android.origin.util.AndroidContexts;
import org.polygamma.android.origin.util.CollectionsCompat;
import org.polygamma.android.origin.util.ExecutingService;
import org.polygamma.android.origin.util.Logger;
import org.polygamma.android.origin.util.Preconditions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Module tracking state of underlying device.
 * <p>The underlying device state is described through a {@link Device descriptor}, accessible
 * using {@link #device()}. As the device state materially changes, an {@linkplain
 * #DEVICE_UPDATE_EVENT update event} is issued, whose data is equal exactly to the return value
 * of {@link #device()}.
 * <p>Note that the descriptor maintains <i>only</i> top-level device hardware descriptions, such
 * as screen, hardware, and software. The descriptor does <b>not</b> maintain descriptions such as
 * {@linkplain Device#geo(int) location} or network {@linkplain Device#connectionType()
 * connectivity}.
 * <h2>Advertising Ids</h2>
 * <p>Advertising ids (IFAs) are exported through {@link Device#advertisingId(int)} of the
 * {@linkplain #device() descriptor}. By default, Google and Mobile Security Alliance (MSA) IFAs
 * are queried. This behaviour can be configured by {@linkplain Provider#ifaClients(IfaClient...)
 * supplying} IFA {@linkplain IfaClient clients} manually. IFA clients are expected to provide IFA
 * values along with the user's preference for tracking.
 * <p>IFA values are queried from configured clients <i>only</i> during startup. Post startup,
 * IFA values are re-used until next startup.
 *
 * @since 0.1
 */
public final class DeviceModule extends OriginModule {

	private static final String TAG = DeviceModule.class.getSimpleName();

	/**
	 * Device module name.
	 *
	 * @since 0.1
	 */
	public static final String NAME = "origin.device";

	/**
	 * Name of {@linkplain Origin#registerModuleEventCallback(OriginModuleEventCallback, Pair)
	 * event} fired when {@link #device() device} descriptor has materially changed.
	 * <p>This event is non-sticky and fired only when the device {@linkplain Device descriptor}
	 * has materially changed. The data associated with the event is the new descriptor.
	 *
	 * @since 0.1
	 */
	public static final @OriginModuleEventName String DEVICE_UPDATE_EVENT = "device-update";

	/**
	 * Name of {@linkplain Origin#registerModuleEventCallback(OriginModuleEventCallback, Pair)
	 * event} fired when device {@linkplain #deviceMemoryState() memory state} descriptor has
	 * materially changed.
	 * <p>This event is non-sticky and fired only when the device memory state descriptor has
	 * materially changed. The data associated with the event is the new descriptor.
	 *
	 * @since 1.2
	 */
	public static final @OriginModuleEventName String DEVICE_MEMORY_STATE_UPDATE_EVENT =
		"device-memory-state";

	/**
	 * Device {@linkplain DeviceModule module} provider.
	 * <p>Device modules expect an IFA {@linkplain IfaClient client}, used to query the advertising
	 * id of the device. If explicit IFA clients are not {@linkplain #ifaClients(IfaClient[])
	 * specified}, then all available clients are used.
	 *
	 * @since 0.1
	 * @see #ofProvider()
	 */
	public static final class Provider extends OriginModule.Provider<DeviceModule> {

		private @Nullable IfaClient[] ifaClients;

		private Provider() {
			super(DeviceModule.class);
		}

		private IfaClient[] ifaClients() {
			return Preconditions.checkNotNullElseGet(
				this.ifaClients,
				() -> {
					List<IfaClient> clients = new ArrayList<>(2);

					try {
						clients.add(IfaClient.ofGms());
					} catch (UnsupportedOperationException ignored) {
					}
					try {
						clients.add(IfaClient.ofHms());
					} catch (UnsupportedOperationException ignored) {
					}
					try {
						clients.add(IfaClient.ofMsa());
					} catch (UnsupportedOperationException ignored) {
					}
					return clients.toArray(new IfaClient[0]);
				}
			);
		}

		/**
		 * Set clients to use for querying advertising id of device.
		 *
		 * @param ifaClients clients array or, empty array to use default
		 * @return {@code this}
		 * @since 0.1
		 */
		public Provider ifaClients(IfaClient... ifaClients) {
			this.ifaClients = ifaClients;
			return this;
		}

		@Override
		protected DeviceModule load(Origin sdk, Context ctxt) {
			DeviceModule mod = new DeviceModule(sdk);
			Resources res = ctxt.getResources();

			mod.updater.configuration = res != null ? res.getConfiguration() : null;
			mod.updater.ifaClients = this.ifaClients();
			mod.updater.schedule();
			return mod;
		}

		@Override
		protected void reload(DeviceModule mod, Context ctxt) {
			mod.updater.ifaClients = this.ifaClients;
			mod.updater.schedule();
		}
	}

	/**
	 * Device descriptor and memory state descriptor update service.
	 */
	@VisibleForTesting
	final class DeviceUpdateService
	extends ExecutingService
	implements ComponentCallbacks2 {
		// non-`null` when device needs to be updated
		@GuardedBy("this")
		@Nullable Configuration configuration;
		// non-`null` when IFAs need to be updated
		@GuardedBy("this")
		@Nullable IfaClient[] ifaClients;
		// memory trim state when memory state needs to be updated, otherwise, `0`
		@GuardedBy("this")
		int memoryTrimState;

		/**
		 * Construct new update service for an SDK.
		 *
		 * @param sdk owning SDK
		 */
		DeviceUpdateService(Origin sdk) {
			super(sdk.backgroundExecutor());
		}

		@Override
		protected void run() {
			Context ctxt = DeviceModule.this.tryContext();

			if (ctxt == null)
				return;

			Configuration cfg;
			IfaClient[] ifa;
			int mem;

			synchronized (this) {
				cfg = this.configuration;
				this.configuration = null;
				ifa = this.ifaClients;
				this.ifaClients = null;
				mem = this.memoryTrimState;
				this.memoryTrimState = 0;
			}

			if (cfg != null || ifa != null) {
				DeviceModule.this.updateDevice(ctxt, cfg, ifa);
				if (ifa != null && (
					DeviceModule.this.device == null ||
					DeviceModule.this.device.advertisingIdCount() != ifa.length
				)) {
					synchronized (this) {
						if (this.ifaClients == null)
							this.ifaClients = ifa;
					}
					super.schedule(5, 5, TimeUnit.SECONDS);
				}
			}
			if (mem != 0) {
				DeviceModule.this.updateDeviceMemoryState(ctxt);
				if (DeviceModule.this.deviceMemoryState.lowMemory()) {
					// on low memory, always recheck memory after some delay
					synchronized (this) {
						if (this.memoryTrimState == 0)
							this.memoryTrimState = mem;
					}
					super.schedule(5, 5, TimeUnit.SECONDS);
				}
			}
		}

		@Override
		public void onTrimMemory(int level) {
			synchronized (this) {
				this.memoryTrimState = level;
			}
			super.schedule();
		}

		@Override
		public void onConfigurationChanged(@NonNull Configuration cfg) {
			synchronized (this) {
				this.configuration = cfg;
			}
			super.schedule();
		}

		@Override
		@SuppressWarnings({ "RedundantSuppression", "deprecation" })
		public void onLowMemory() {
			this.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW);
		}
	}

	/**
	 * Construct new module provider.
	 *
	 * @return new provider instance
	 * @since 0.1
	 */
	public static Provider ofProvider() {
		return new Provider();
	}

	/**
	 * Probe device descriptor containing stable description.
	 * <p>The descriptor builder {@code dst} is updated with only <i>stable</i> descriptions, such
	 * as device {@linkplain Device#type() type} (when known to be stable), {@linkplain
	 * Device#manufacturerName() manufacturer}, {@linkplain Device#modelName() model}, {@linkplain
	 * Device#modelVersion() model version}, {@linkplain Device#operatingSystem() operating
	 * system}, and {@linkplain Device#operatingSystemVersion() operating system version}.
	 *
	 * @param dst descriptor builder to update
	 * @param ctxt context to probe stable descriptions from
	 */
	@SuppressWarnings({ "RedundantSuppression", "deprecation" })
	private static void probeBaseDevice(Device.Builder dst, Context ctxt) {
		PackageManager pman = ctxt.getPackageManager();

		int type =
			pman.hasSystemFeature(PackageManager.FEATURE_PC) ?
			AdComEnums.DevicePc :
			pman.hasSystemFeature(PackageManager.FEATURE_WATCH) ||
			pman.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE) ||
			pman.hasSystemFeature(PackageManager.FEATURE_EMBEDDED) ?
			AdComEnums.DeviceCd :
			pman.hasSystemFeature(PackageManager.FEATURE_LIVE_TV) ||
			pman.hasSystemFeature(PackageManager.FEATURE_LEANBACK) ||
			pman.hasSystemFeature(PackageManager.FEATURE_TELEVISION) ?
			AdComEnums.DeviceCtv :
			AdComEnums.DeviceUnknown;
		int os =
			pman.hasSystemFeature("amazon.hardware.fire_tv") ? AdComEnums.OsAmazonFireOs :
			AdComEnums.OsGoogleAndroid;
		String osv =
			Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ?
			Build.VERSION.RELEASE_OR_CODENAME :
			Build.VERSION.RELEASE;

		dst.type(type)
			.manufacturerName(Build.MANUFACTURER)
			.modelName(Build.MODEL)
			.modelVersion(Build.VERSION.INCREMENTAL)
			.operatingSystem(os)
			.operatingSystemVersion(osv)
			.screenPixelRatio(ctxt.getResources().getDisplayMetrics().density);

		if (os == AdComEnums.OsAmazonFireOs)
			dst.type(AdComEnums.DeviceCtv);

		Display display =
			AndroidContexts.withSystemService(
				ctxt,
				DisplayManager.class,
				Context.DISPLAY_SERVICE,
				man -> man.getDisplay(Display.DEFAULT_DISPLAY)
			);

		if (display != null) {
			Point size = new Point();
			DisplayMetrics metrics = new DisplayMetrics();

			display.getRealMetrics(metrics);
			display.getRealSize(size);
			dst.screenWidthPx(size.x)
				.screenHeightPx(size.y)
				.screenPixelsPerInch(Math.round(Math.max(metrics.xdpi, metrics.ydpi)));

		}
	}

	private static String simCarrierMccMncOf(Configuration cfg) {
		return MccMnc.serialize(
			cfg.mcc == 0 ? -1 : cfg.mcc,
			cfg.mnc == 0 ? -1 :
			cfg.mnc == Configuration.MNC_ZERO ? 0 :
			cfg.mnc
		);
	}

	/**
	 * Append language code from {@linkplain Locale locale} to list.
	 *
	 * @param dst list to append to
	 * @param locale locale to append from
	 */
	private static void addLanguageCode(List<String> dst, @Nullable Locale locale) {
		String code =
			locale == null ? "" :
			Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ? locale.toLanguageTag() :
			locale.getLanguage();

		if (!TextUtils.isEmpty(code))
			dst.add(code);
	}

	/**
	 * Probe primary language code and secondary language codes supported by the device.
	 *
	 * @param ctxt context to probe from
	 * @param cfg configuration to probe from
	 * @return {@code null} if unknown; otherwise, tuple of primary and secondary codes
	 */
	@SuppressWarnings({ "RedundantSuppression", "deprecation" })
	private static @Nullable Pair<String, Collection<String>>
	probeLanguageCodes(Context ctxt, Configuration cfg) {
		List<String> lang = new ArrayList<>(1);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			LocaleList pref = cfg.getLocales();

			for (int i = 0; i < pref.size(); i++)
				addLanguageCode(lang, pref.get(i));
		} else {
			addLanguageCode(lang, cfg.locale);
		}
		AndroidContexts.withSystemService(
			ctxt,
			InputMethodManager.class,
			Context.INPUT_METHOD_SERVICE,
			man -> {
				for (InputMethodInfo meth : man.getEnabledInputMethodList()) {
					for (
						InputMethodSubtype sub :
						man.getEnabledInputMethodSubtypeList(meth, true)
					) {
						String tag =
							Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ?
							sub.getLanguageTag() :
							sub.getLocale();

						if (!TextUtils.isEmpty(tag))
							lang.add(tag);
					}
				}
				return null;
			});

		if (lang.isEmpty())
			return null;

		String primary = lang.get(0);
		Set<String> rest = CollectionsCompat.newArraySet(lang.size() - 1);

		for (int i = 1; i < lang.size(); i++)
			rest.add(lang.get(i));
		rest.remove(primary);
		return new Pair<>(primary, rest);
	}

	/**
	 * Determine the device type based on current UI {@link UiModeManager#getCurrentModeType()
	 * mode}.
	 *
	 * @param ctxt context to use for querying fallback UI mode
	 * @param cfg configuration UI mode
	 * @return resulting device type
	 */
	private static @DeviceType int deviceTypeOfUiMode(Context ctxt, Configuration cfg) {
		int mode = (cfg.uiMode & Configuration.UI_MODE_TYPE_MASK);

		if (mode == Configuration.UI_MODE_TYPE_UNDEFINED) {
			mode = AndroidContexts.withSystemServiceElse(
				ctxt,
				UiModeManager.class,
				Context.UI_MODE_SERVICE,
				UiModeManager::getCurrentModeType,
				Configuration.UI_MODE_TYPE_UNDEFINED
			);
		}
		switch (mode) {
		case Configuration.UI_MODE_TYPE_DESK:
			return AdComEnums.DevicePc;
		case Configuration.UI_MODE_TYPE_APPLIANCE:
		case Configuration.UI_MODE_TYPE_CAR:
		case Configuration.UI_MODE_TYPE_VR_HEADSET:
		case Configuration.UI_MODE_TYPE_WATCH:
			return AdComEnums.DeviceCd;
		case Configuration.UI_MODE_TYPE_TELEVISION:
			return AdComEnums.DeviceCtv;
		default:
			// assume it's a phone or tablet
			return AdComEnums.DeviceMobile;
		}
	}

	private @Nullable Device device;
	private DeviceMemoryState deviceMemoryState;
	private final OriginModuleEventBus deviceUpdateEvent;
	private final OriginModuleEventBus deviceMemoryStateUpdateEvent;
	@VisibleForTesting
	final DeviceUpdateService updater;

	private DeviceModule(Origin sdk) {
		super(NAME, sdk);
		this.deviceMemoryState = new DeviceMemoryState(0, false);
		this.deviceUpdateEvent = super.registerEvent(DEVICE_UPDATE_EVENT, false);
		this.deviceMemoryStateUpdateEvent =
			super.registerEvent(DEVICE_MEMORY_STATE_UPDATE_EVENT, false);
		this.updater = new DeviceUpdateService(sdk);
	}

	/**
	 * Current underlying device descriptor.
	 *
	 * @return device descriptor or {@code null} if not yet available
	 * @since 0.1
	 */
	public @Nullable Device device() {
		return this.device;
	}

	/**
	 * Current memory state descriptor of underlying device.
	 *
	 * @return memory state descriptor
	 * @since 1.2
	 */
	public DeviceMemoryState deviceMemoryState() {
		return this.deviceMemoryState;
	}

	/**
	 * Test whether a device descriptor needs to be published.
	 *
	 * @param curr descriptor to test
	 * @return {@code true} if, and only if, {@code curr} should be published
	 */
	private boolean needPublishDevice(Device curr) {
		Device prev = this.device;

		if (prev == null)
			return true;
		if (prev == curr)
			return false;
		if (prev.landscape() != curr.landscape() || prev.nightMode() != curr.nightMode())
			return true;
		if (prev.screenWidthPx() != curr.screenWidthPx())
			return true;
		if (prev.screenHeightPx() != curr.screenHeightPx())
			return true;
		if (prev.type() != curr.type())
			return true;
		if (!prev.simCarrierMccMnc().equals(curr.simCarrierMccMnc()))
			return true;
		if (!prev.languageCode().equals(curr.languageCode()))
			return true;
		if (prev.extraLanguageCodeCount() != curr.extraLanguageCodeCount())
			return true;
		if (prev.advertisingIdCount() != curr.advertisingIdCount())
			return true;
		for (int i = 0; i < prev.extraLanguageCodeCount(); i++) {
			if (!prev.extraLanguageCode(i).equals(curr.extraLanguageCode(i)))
				return true;
		}
		for (int i = 0; i < prev.advertisingIdCount(); i++) {
			if (!prev.advertisingId(i).equals(curr.advertisingId(i)))
				return true;
		}
		return false;
	}

	/**
	 * Publish updated device descriptor if, and only if, material change occurred.
	 *
	 * @param update updated descriptor builder
	 */
	private void publishDevice(Device.Builder update) {
		Device updated = update.build();

		if (!this.needPublishDevice(updated))
			return;
		this.device = updated;
		this.deviceUpdateEvent.submit(updated);
		super.storeSettings(updated);
		Logger.debug(TAG, "updated device: %s", updated);
	}

	/**
	 * Publish updated device memory state descriptor if, and only if, material change occurred.
	 *
	 * @param update updated descriptor
	 */
	private void publishDeviceMemoryState(DeviceMemoryState update) {
		DeviceMemoryState curr = this.deviceMemoryState;

		if (update.lowMemory() == curr.lowMemory() && update.trimState() == curr.trimState())
			return;

		this.deviceMemoryState = update;
		this.deviceMemoryStateUpdateEvent.submit(update);
		Logger.debug(TAG,
			"updated device memory state: trim=%s, low=%s",
			update.trimState(),
			update.lowMemory()
		);
	}

	/**
	 * Probe device ids sanctioned for advertising.
	 *
	 * @param dst descriptor builder to update
	 * @param ctxt context to query with
	 * @param clients clients to query from
	 */
	@WorkerThread
	private void probeIfa(Device.Builder dst, Context ctxt, IfaClient[] clients) {
		Origin sdk = super.sdk();
		List<Pair<String, String>> ifas = new ArrayList<>(clients.length);
		boolean lmt = false;

		for (IfaClient client : clients) {
			Pair<String, Boolean> res = null;

			try {
				res = super.sdk().callIoInBackground(() -> client.sendRequest(sdk, ctxt)).get();
			} catch (Exception cause) {
				Logger.info(TAG, "failed to query %s id", client.type(), cause);
			}
			if (res == null || TextUtils.isEmpty(res.first)) {
				Device curr = this.device;

				if (curr == null)
					continue;
				for (int i = 0; i < curr.advertisingIdCount(); i++) {
					if (curr.advertisingId(i).first.equals(client.type())) {
						res = new Pair<>(curr.advertisingId(i).second, false);
						break;
					}
				}
			}
			if (res != null) {
				lmt |= Boolean.TRUE.equals(res.second);
				ifas.add(new Pair<>(client.type(), res.first));
			}
		}
		if (lmt)
			dst.limitAdTracking(lmt);
		if (!ifas.isEmpty())
			dst.advertisingIds(ifas);
	}

	/**
	 * Update device descriptor based on configuration.
	 *
	 * @param dst descriptor builder to update
	 * @param ctxt context to update from
	 * @param cfg configuration to update from
	 */
	@WorkerThread
	@SuppressWarnings({ "RedundantSuppression", "deprecation" })
	private void updateDeviceConfiguration(Device.Builder dst, Context ctxt, Configuration cfg) {
		dst.landscape(cfg.orientation == Configuration.ORIENTATION_LANDSCAPE);
		dst.simCarrierMccMnc(simCarrierMccMncOf(cfg));
		if (this.device != null && this.device.type() == AdComEnums.DeviceUnknown)
			dst.type(deviceTypeOfUiMode(ctxt, cfg));
		dst.nightMode(
			(cfg.uiMode & Configuration.UI_MODE_NIGHT_MASK) ==
			Configuration.UI_MODE_NIGHT_YES
		);

		Pair<String, Collection<String>> lang = probeLanguageCodes(ctxt, cfg);

		if (lang != null) {
			dst.languageCode(lang.first);
			dst.extraLanguageCodes(lang.second);
		}
		if (
			this.device != null &&
			this.device.screenWidthPx() != 0 &&
			this.device.screenHeightPx() != 0
		) {
			return;
		}

		WindowManager wman =
			AndroidContexts.systemServiceOf(ctxt, WindowManager.class, Context.WINDOW_SERVICE);

		if (wman == null)
			return;

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			Rect bounds = wman.getMaximumWindowMetrics().getBounds();

			dst.screenWidthPx(bounds.width())
				.screenHeightPx(bounds.height());
		} else {
			Point size = new Point();

			wman.getDefaultDisplay().getRealSize(size);
			dst.screenWidthPx(size.x)
				.screenHeightPx(size.y);
		}
	}

	/**
	 * Update {@linkplain #device current} device descriptor.
	 *
	 * @param ctxt context to update from
	 * @param cfg current device configuration information
	 * @param ifaClients clients to query IFAs from
	 */
	@WorkerThread
	@RestrictTo(RestrictTo.Scope.SUBCLASSES)
	private void
	updateDevice(Context ctxt, @Nullable Configuration cfg, @Nullable IfaClient[] ifaClients) {
		if (this.device == null)
			this.device = super.loadSettings(Device::ofProtobuf);

		Device.Builder update = Preconditions.checkNotNullElse(this.device, Device.of())
			.toBuilder();

		if (this.device == null)
			probeBaseDevice(update, ctxt);
		if (cfg != null)
			this.updateDeviceConfiguration(update, ctxt, cfg);
		if (ifaClients != null)
			this.probeIfa(update, ctxt, ifaClients);
		this.publishDevice(update);
	}

	/**
	 * Update {@linkplain #deviceMemoryState current} device memory state descriptor.
	 *
	 * @param ctxt context to update from
	 */
	@WorkerThread
	@RestrictTo(RestrictTo.Scope.SUBCLASSES)
	private void updateDeviceMemoryState(Context ctxt) {
		ActivityManager act =
			AndroidContexts.systemServiceOf(ctxt, ActivityManager.class, Context.ACTIVITY_SERVICE);

		if (act == null)
			return;

		ActivityManager.MemoryInfo mem = new ActivityManager.MemoryInfo();
		ActivityManager.RunningAppProcessInfo proc = new ActivityManager.RunningAppProcessInfo();

		try {
			act.getMemoryInfo(mem);
		} catch (Throwable cause) {
			Logger.warn(TAG, "failed to query memory info", cause);
		}
		try {
			ActivityManager.getMyMemoryState(proc);
		} catch (Throwable cause) {
			Logger.warn(TAG, "failed to query process info", cause);
		}
		this.publishDeviceMemoryState(new DeviceMemoryState(
			mem.lowMemory ? proc.lastTrimLevel : 0,
			mem.lowMemory
		));
	}

	@Override
	protected void destroy() {
		this.updater.shutdown();
		super.acceptContext(c -> c.unregisterComponentCallbacks(this.updater));

		while (true) {
			try {
				if (!this.updater.awaitShutdown(1, TimeUnit.SECONDS))
					Logger.warn(TAG, "updater taking longer than 1 second to shutdown");
				break;
			} catch (InterruptedException cause) {
				Logger.info(TAG, "interrupted while awaiting updater shutdown", cause);
			}
		}
	}
}
