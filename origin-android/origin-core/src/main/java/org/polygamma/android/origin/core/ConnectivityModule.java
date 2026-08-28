// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.core;

import android.Manifest;
import android.content.Context;
import android.net.ConnectivityManager;
import android.telephony.TelephonyManager;
import android.util.ArrayMap;
import android.util.Pair;

import androidx.annotation.GuardedBy;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.util.Consumer;

import org.polygamma.android.origin.adcom.enums.AdComEnums;
import org.polygamma.android.origin.adcom.enums.ConnectionType;
import org.polygamma.android.origin.util.AndroidContexts;
import org.polygamma.android.origin.util.ExecutingService;
import org.polygamma.android.origin.util.Logger;
import org.polygamma.android.origin.util.Preconditions;
import org.polygamma.android.origin.util.Time;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Module tracking connectivity state of underlying device.
 * <p>The underlying connectivity state is described through a connectivity {@linkplain
 * Connectivity descriptor}, accessible using {@link #connectivity()}. As the connectivity state
 * materially changes, an {@linkplain #CONNECTIVITY_UPDATE_EVENT update event} is issued, whose
 * data is equal exactly to the return value of {@link #connectivity()}.
 *
 * @since 0.3
 */
public final class ConnectivityModule extends OriginModule {

	private static final String TAG = ConnectivityModule.class.getSimpleName();

	/**
	 * Connectivity module name.
	 *
	 * @since 0.3
	 */
	public static final String NAME = "origin.connectivity";

	/**
	 * Name of {@linkplain Origin#registerModuleEventCallback(OriginModuleEventCallback, Pair)
	 * event} fired when {@link #connectivity() connectivity} descriptor has materially changed.
	 * <p>This event is sticky and fired only when the connectivity {@linkplain Connectivity
	 * descriptor} has materially changed. The data associated with the event is the new
	 * descriptor.
	 *
	 * @since 0.3
	 */
	public static final @OriginModuleEventName String CONNECTIVITY_UPDATE_EVENT =
		"connectivity-update";

	/**
	 * Default delay, in seconds, between descriptor updates.
	 *
	 * @since 0.3
	 */
	public static final long DEFAULT_POLL_DELAY_SECONDS = TimeUnit.MINUTES.toSeconds(30);

	/**
	 * Retrieve AdCOM connection type for connectivity manager based connection type.
	 *
	 * @param type connectivity manager connection type
	 * @return AdCOM connection type
	 */
	@SuppressWarnings({ "RedundantSuppression", "deprecation" })
	static @ConnectionType int connectionTypeOfConnectivity(int type) {
		switch (type) {
		case ConnectivityManager.TYPE_ETHERNET:
			return AdComEnums.ConnectionWired;
		case ConnectivityManager.TYPE_MOBILE:
		case ConnectivityManager.TYPE_MOBILE_DUN:
		case ConnectivityManager.TYPE_MOBILE_HIPRI:
		case ConnectivityManager.TYPE_MOBILE_MMS:
		case ConnectivityManager.TYPE_MOBILE_SUPL:
			return AdComEnums.ConnectionCell;
		case ConnectivityManager.TYPE_VPN:
			return AdComEnums.ConnectionVpn;
		case ConnectivityManager.TYPE_WIFI:
			return AdComEnums.ConnectionWifi;
		case ConnectivityManager.TYPE_WIMAX:
			return AdComEnums.ConnectionWiMax;
		case ConnectivityManager.TYPE_BLUETOOTH:
			return AdComEnums.ConnectionBluetooth;
		default:
			Logger.debug(TAG, "unknown connectivity type: %s", type);
			return AdComEnums.ConnectionUnknown;
		}
	}

	/**
	 * Retrieve AdCOM connection type for network type.
	 *
	 * @param type network type
	 * @return AdCOM connection type
	 */
	@SuppressWarnings({ "RedundantSuppression", "deprecation", "fallthrough" })
	static @ConnectionType int connectionTypeOfNetwork(int type) {
		switch (type) {
		case TelephonyManager.NETWORK_TYPE_1xRTT:
		case TelephonyManager.NETWORK_TYPE_CDMA:
		case TelephonyManager.NETWORK_TYPE_EDGE:
		case TelephonyManager.NETWORK_TYPE_IDEN:
		case TelephonyManager.NETWORK_TYPE_GPRS:
		case TelephonyManager.NETWORK_TYPE_GSM:
			return AdComEnums.ConnectionCell2G;
		case TelephonyManager.NETWORK_TYPE_EHRPD:
		case TelephonyManager.NETWORK_TYPE_EVDO_0:
		case TelephonyManager.NETWORK_TYPE_EVDO_A:
		case TelephonyManager.NETWORK_TYPE_EVDO_B:
		case TelephonyManager.NETWORK_TYPE_HSDPA:
		case TelephonyManager.NETWORK_TYPE_HSPA:
		case TelephonyManager.NETWORK_TYPE_HSPAP:
		case TelephonyManager.NETWORK_TYPE_HSUPA:
		case TelephonyManager.NETWORK_TYPE_TD_SCDMA:
		case TelephonyManager.NETWORK_TYPE_UMTS:
			return AdComEnums.ConnectionCell3G;
		case TelephonyManager.NETWORK_TYPE_LTE:
			return AdComEnums.ConnectionCell4G;
		case TelephonyManager.NETWORK_TYPE_NR:
			return AdComEnums.ConnectionCell5G;
		case TelephonyManager.NETWORK_TYPE_IWLAN:
			return AdComEnums.ConnectionWifi;
		case TelephonyManager.NETWORK_TYPE_UNKNOWN:
		default:
			Logger.debug(TAG, "unknown network type: %s", type);
			return AdComEnums.ConnectionUnknown;
		}
	}

	/**
	 * Connectivity {@linkplain ConnectivityModule module} provider.
	 *
	 * @since 0.3
	 * @see #ofProvider()
	 */
	public static final class Provider extends OriginModule.Provider<ConnectivityModule> {

		private long pollDelaySeconds;

		private Provider() {
			super(ConnectivityModule.class);
			this.pollDelaySeconds = DEFAULT_POLL_DELAY_SECONDS;
		}

		/**
		 * Set delay at which connectivity descriptor is polled.
		 *
		 * @param delay delay, or {@code 0} for {@linkplain #DEFAULT_POLL_DELAY_SECONDS default}
		 * @param unit unit {@code delay} is specified in
		 * @return {@code this}
		 * @throws IllegalArgumentException {@code delay} is negative
		 * @since 0.3
		 */
		public Provider pollDelay(long delay, TimeUnit unit) {
			Preconditions.checkArgument(delay >= 0);
			delay = unit.toMillis(delay);
			this.pollDelaySeconds = delay == 0 ? DEFAULT_POLL_DELAY_SECONDS : delay;
			return this;
		}

		@Override
		protected ConnectivityModule load(Origin sdk, Context ctxt) {
			ConnectivityModule module = new ConnectivityModule(sdk, this.pollDelaySeconds);

			module.init(ctxt);
			return module;
		}

		@Override
		protected void reload(ConnectivityModule module, Context ctxt) {
			module.pollDelaySeconds = this.pollDelaySeconds;
		}
	}

	/**
	 * Connectivity update service.
	 */
	@VisibleForTesting
	final class UpdateService extends ExecutingService {
		// timestamp, in seconds, of last poll
		long lastPollTimestampSeconds;

		/**
		 * Construct new update service.
		 *
		 * @param sdk owning SDK
		 */
		UpdateService(Origin sdk) {
			super(sdk.backgroundExecutor());
		}

		@Override
		protected void run() {
			Context ctxt = ConnectivityModule.this.tryContext();

			if (ctxt == null)
				return;

			boolean polled = this.lastPollTimestampSeconds == ~0L;

			if (
				!polled &&
				Time.durationBetween(Time.nowUptimeSeconds(), this.lastPollTimestampSeconds) <=
				ConnectivityModule.this.pollDelaySeconds
			) {
				polled = true;
				synchronized (ConnectivityModule.this) {
					for (
						ConnectivityUpdater updater :
						ConnectivityModule.this.pollingUpdaters.keySet()
					) {
						if (!ConnectivityModule.this.isDestroyed())
							ConnectivityModule.this.pendingUpdaters.put(updater, updater);
					}
				}
			}
			ConnectivityModule.this.update(ctxt);

			if (
				ConnectivityModule.this.isDestroyed() ||
				ConnectivityModule.this.pollingUpdaters.isEmpty()
			) {
				return;
			}

			long delaySec;
			long now = Time.nowUptimeSeconds();

			if (polled) {
				this.lastPollTimestampSeconds = now;
				delaySec = ConnectivityModule.this.pollDelaySeconds;
			} else {
				delaySec = Time.durationBetween(now, this.lastPollTimestampSeconds);
			}
			super.schedule(delaySec, TimeUnit.SECONDS);
		}
	}

	/**
	 * Construct a new module provider.
	 *
	 * @return module provider
	 * @since 0.3
	 */
	public static Provider ofProvider() {
		return new Provider();
	}

	// Items marked with [L] must be accessed within `synchronized (this)`.

	private final OriginModuleEventBus updateEvent;
	// [L] Updaters which require poll driven updates:
	@GuardedBy("this")
	@VisibleForTesting
	final ArrayMap<ConnectivityUpdater, ConnectivityUpdater> pollingUpdaters;
	// [L] Updaters which are pending an update:
	@GuardedBy("this")
	@VisibleForTesting
	final ArrayMap<ConnectivityUpdater, ConnectivityUpdater> pendingUpdaters;
	// Current connectivity description:
	private @Nullable Connectivity connectivity;
	// All updaters we have available:
	@VisibleForTesting
	@Nullable ConnectivityUpdater[] updaters;
	// Update service.
	@VisibleForTesting
	final UpdateService updateService;
	// Delay between polls.
	private long pollDelaySeconds;
	private boolean canAccessInternet;

	private ConnectivityModule(Origin sdk, long pollDelaySecs) {
		super(NAME, sdk);
		this.updateEvent = super.registerEvent(CONNECTIVITY_UPDATE_EVENT, true);
		this.pollingUpdaters = new ArrayMap<>();
		this.pendingUpdaters = new ArrayMap<>();
		this.updateService = new UpdateService(sdk);
		this.pollDelaySeconds = pollDelaySecs;
		// assume we can access internet
		this.canAccessInternet = true;
	}

	/**
	 * Current connectivity descriptor.
	 *
	 * @return descriptor or {@code null} if not yet available
	 * @since 0.3
	 */
	public @Nullable Connectivity connectivity() {
		return this.connectivity;
	}

	/**
	 * Test whether at least <i>one</i> network is available which can access the internet.
	 *
	 * @return {@code true} if, and only if, at least one internet accessible network is available
	 * @since 1.2
	 */
	public boolean canAccessInternet() {
		return this.canAccessInternet;
	}

	/**
	 * Test whether module has been destroyed.
	 *
	 * @return {@code true} if, and only if, module has been destroyed
	 */
	private boolean isDestroyed() {
		return this.updaters == null;
	}

	/**
	 * Apply updates from one updater.
	 *
	 * @param dst descriptor to apply update to
	 * @param updater updater to update with
	 * @param ctxt context to update from
	 * @param wasAsync {@code true} if, and only if, {@code updater} was marked for asynchronous
	 * updates
	 * @return resulting descriptor
	 */
	private Connectivity updateOne(
		Connectivity dst,
		ConnectivityUpdater updater,
		Context ctxt,
		boolean wasAsync
	) {
		Pair<Connectivity, Boolean> res;

		Logger.debug(TAG, "applying update from %s", updater);
		try {
			res = updater.update(dst, ctxt);
		} catch (Throwable err) {
			Logger.warn(TAG, "failed to apply updater %s", updater, err);
			res = new Pair<>(dst, false);
		}

		if (res.second.equals(wasAsync))
			return res.first;

		synchronized (this) {
			if (this.isDestroyed())
				return res.first;
			if (res.second)
				this.pollingUpdaters.remove(updater);
			else
				this.pollingUpdaters.put(updater, updater);
		}
		Logger.debug(
			TAG,
			"%s updater %s to %s",
			res.second ? "promoted" : "demoted",
			updater, res.second ? "asynchronous" : "polling"
		);
		return res.first;
	}

	/**
	 * Update connectivity descriptor from all available updaters.
	 *
	 * @param ctxt context to update from
	 */
	private void update(Context ctxt) {
		Connectivity prev = this.connectivity;
		Connectivity curr = Preconditions.checkNotNullElse(prev, Connectivity.of());

		while (true) {
			ConnectivityUpdater updater;
			boolean wasAsync;

			synchronized (this) {
				int rem = this.pendingUpdaters.size();

				if (this.isDestroyed() || rem == 0) {
					// we've been destroyed or have no pending updates, stop updating
					break;
				}

				updater = this.pendingUpdaters.removeAt(rem - 1);
				wasAsync = !this.pollingUpdaters.containsKey(updater);
			}
			curr = this.updateOne(curr, updater, ctxt, wasAsync);
		}

		if (curr.equals(prev))
			return;

		/*
		 * when `ACCESS_NETWORK_STATE` isn't available, `desc::networks()` is always going to be
		 * empty, in this case, we are always going to assume the network is available.
		 */
		boolean hasInternet =
			!AndroidContexts.hasPermission(ctxt, Manifest.permission.ACCESS_NETWORK_STATE);

		for (int i = 0; i < curr.networkCount(); i++)
			hasInternet |= curr.network(i).hasCapability(ConnectivityNetwork.CapabilityInternet);

		this.canAccessInternet = hasInternet;
		this.connectivity = curr;
		this.updateEvent.submit(curr);
		Logger.debug(
			TAG,
			"updated connectivity descriptor %s, canAccessInternet=%s",
			curr,
			hasInternet
		);
	}

	/**
	 * Mark descriptor updater for update.
	 *
	 * @param updater updater to mark
	 */
	@VisibleForTesting
	void updateAvailable(ConnectivityUpdater updater) {
		synchronized (this) {
			if (this.isDestroyed()) {
				// we may have been `destroy()`ed, just return.
				return;
			}
			if (this.pendingUpdaters.put(updater, updater) != updater)
				this.updateService.schedule();
		}
	}

	/**
	 * Initialize module.
	 *
	 * @param ctxt context to initialize with
	 */
	@SuppressWarnings("resource")
	private void init(Context ctxt) {
		ArrayList<ConnectivityUpdater> updaters = new ArrayList<>(2);
		Consumer<ConnectivityUpdater> onUpdateAvailable = this::updateAvailable;

		for (ConnectivityUpdater updater : new ConnectivityUpdater[] {
			ConnectivityNetworkUpdater.of(ctxt),
			ConnectivitySubscriptionUpdater.of(
				ctxt,
				super.sdk().backgroundExecutor(),
				super.sdk().foregroundExecutor()
			)
		}) {
			if (updater != null) {
				updaters.add(updater);
				updater.setOnUpdateAvailable(onUpdateAvailable);
				this.pendingUpdaters.put(updater, updater);
			}
		}

		this.updaters = updaters.toArray(new ConnectivityUpdater[0]);

		if (this.updaters.length != 0)
			this.updateService.schedule();
	}

	@Override
	protected void destroy() {
		ConnectivityUpdater[] updaters = this.updaters;

		this.updaters = null;
		this.updateService.shutdown();
		synchronized (this) {
			this.pollingUpdaters.clear();
			this.pendingUpdaters.clear();
		}

		for (int i = 0; i < 5; i++) {
			try {
				if (this.updateService.awaitShutdown(1, TimeUnit.SECONDS))
					break;
				Logger.warn(TAG, "updater shutdown taking longer than 1 second");
			} catch (InterruptedException cause) {
				Logger.info(TAG, "interrupted while awaiting updater shutdown", cause);
			}
		}
		if (updaters != null) {
			for (ConnectivityUpdater updater : updaters) {
				try {
					updater.close();
				} catch (Exception err) {
					Logger.warn(TAG, "failed to close updater %s", updater, err);
				}
			}
		}
	}
}
