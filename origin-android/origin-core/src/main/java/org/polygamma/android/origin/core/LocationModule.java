// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.core;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationRequest;
import android.os.Build;
import android.text.TextUtils;

import androidx.annotation.CallSuper;
import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;

import org.polygamma.android.origin.adcom.context.Geo;
import org.polygamma.android.origin.util.AndroidContexts;
import org.polygamma.android.origin.util.ExecutingService;
import org.polygamma.android.origin.util.Logger;
import org.polygamma.android.origin.util.Preconditions;
import org.polygamma.android.origin.util.Strings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Exchanger;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Module tracking device location.
 * <p>This module does <b>not</b> perform active tracking of the device location. Instead, the
 * {@linkplain LocationManager#getLastKnownLocation(String) last known} location of each location
 * {@linkplain LocationManager#getAllProviders() provider} is polled at an {@linkplain
 * Provider#updateInterval(long, TimeUnit) interval}. This ensures there is no drain on the
 * performance or battery life of the user's device, while allowing for marginally accurate
 * loaction.
 * <p>The current location descriptors can be retrieved using {@link #geos()}. This method returns
 * a list of current location descriptors, where the first entry, if any, is the best location
 * descriptor. This method may return {@code null} while location has not yet been updated.
 * Additionally, an {@linkplain List#isEmpty() empty} list will be returned if the device is
 * not capable of location queries <i>or</i> location permissions are not available. When the
 * return value of {@link #geos()} changes materially, the sticky {@linkplain #GEOS_UPDATE_EVENT
 * update} event is fired with the updated return value.
 *
 * @since 0.1
 */
public class LocationModule extends OriginModule {

	private static final String TAG = LocationModule.class.getSimpleName();

	/**
	 * Maximum time, in milliseconds, to wait for a reverse geocode result.
	 */
	private static final long REVERSE_GEOCODE_TIMEOUT_MILLIS = 250;

	/**
	 * Location module name.
	 *
	 * @since 0.1
	 */
	public static final String NAME = "origin.location";

	/**
	 * Name of {@linkplain
	 * Origin#registerModuleEventCallback(OriginModuleEventCallback, android.util.Pair) event}
	 * fired when {@linkplain #geos() location} descriptors have materially changed.
	 * <p>This event is sticky and fired when location {@linkplain Geo descriptors} have materially
	 * changed. The data associated with the event is the non-{@code null} return value of {@link
	 * #geos()}.
	 *
	 * @since 0.1
	 */
	public static final @OriginModuleEventName String GEOS_UPDATE_EVENT = "geos-update";

	/**
	 * Default interval, in seconds, between location updates.
	 *
	 * @since 0.3
	 */
	public static final long DEFAULT_UPDATE_INTERVAL_SECONDS = TimeUnit.MINUTES.toSeconds(30);

	/**
	 * Specialization of location {@linkplain LocationModule module} for Android S and above.
	 * <p>This uses passive location updates to trigger updates. The result is that the fixed
	 * delay is used as a maximum delay. In a well functioning environment, this results in
	 * cleaner location update triggers.
	 */
	@RequiresApi(Build.VERSION_CODES.S)
	@VisibleForTesting
	static final class SImpl extends LocationModule implements LocationListener {

		private @Nullable Location latestPassiveLocation;
		@VisibleForTesting
		boolean updatesRequested;

		private SImpl(Origin sdk, Context ctxt, long updateIntervalSecs) {
			super(sdk, ctxt, updateIntervalSecs);
		}

		/**
		 * Request passive location updates if possible.
		 * <p>This must be invoked with {@link #updateLock update lock} held and only when updates
		 * have not yet been {@link #updatesRequested requested}. If the request for passive
		 * updates was successful, {@link #updatesRequested} will be set to {@code true}.
		 *
		 * @param ctxt context to request updates from
		 */
		@SuppressLint("MissingPermission")
		private void tryRequestUpdates(Context ctxt) {
			LocationManager man = super.manager(ctxt);

			if (man == null) {
				Logger.debug(TAG, "skipping passive updates request, location unavailable");
				return;
			}
			if (!man.hasProvider(LocationManager.PASSIVE_PROVIDER)) {
				Logger.debug(TAG, "skipping passive updates request, location permissions unavailable, passive provider unavailable");
				return;
			}

			try {
				man.requestLocationUpdates(
					LocationManager.PASSIVE_PROVIDER,
					(new LocationRequest.Builder(LocationRequest.PASSIVE_INTERVAL))
						.setMinUpdateIntervalMillis(TimeUnit.MINUTES.toMillis(1))
						.build(),
					super.sdk().backgroundIoExecutor(),
					this
				);
				this.updatesRequested = true;
				Logger.debug(TAG, "requested passive updates");
			} catch (Throwable e) {
				Logger.info(TAG, "failed to request passive updates", e);
			}
		}

		@Override
		@Nullable Location pollPooledLocation(Context ctxt) {
			Location curr = this.latestPassiveLocation;

			this.latestPassiveLocation = null;
			if (curr == null && !this.updatesRequested)
				this.tryRequestUpdates(ctxt);
			if (curr != null && TextUtils.isEmpty(curr.getProvider())) {
				curr = new Location(curr);
				curr.setProvider(LocationManager.PASSIVE_PROVIDER);
			}
			return curr;
		}

		@Override
		public void onLocationChanged(Location curr) {
			super.updateLock.lock();
			try {
				this.latestPassiveLocation = curr;
				super.scheduleUpdate(true);
			} finally {
				super.updateLock.unlock();
			}
		}

		@Override
		public void onLocationChanged(List<Location> curr) {
			if (!curr.isEmpty())
				this.onLocationChanged(curr.get(curr.size() - 1));
		}

		@Override
		@SuppressLint("MissingPermission")
		protected void destroy() {
			super.updateLock.lock();
			try {
				if (super.manager != null && this.updatesRequested) {
					try {
						super.manager.removeUpdates(this);
					} catch (Throwable e) {
						// This usually happens only when we didn't actually register a listener
						Logger.debug(TAG, "failed to remove update listener", e);
					}
					this.updatesRequested = false;
				}
			} finally {
				super.updateLock.unlock();
				super.destroy();
			}
		}
	}

	/**
	 * Location {@linkplain LocationModule module} provider.
	 *
	 * @since 0.1
	 * @see #ofProvider()
	 */
	public static final class Provider extends OriginModule.Provider<LocationModule> {

		private long updateIntervalSeconds;

		private Provider() {
			super(LocationModule.class);
			this.updateIntervalSeconds = DEFAULT_UPDATE_INTERVAL_SECONDS;
		}

		/**
		 * Set interval at which location descriptors are updated.
		 *
		 * @param dur interval, or {@code 0} for {@linkplain #DEFAULT_UPDATE_INTERVAL_SECONDS
		 * default}
		 * @param unit unit {@code dur} is specified in
		 * @return {@code this}
		 * @throws IllegalArgumentException {@code dur} is negative
		 * @since 0.1
		 */
		public Provider updateInterval(long dur, TimeUnit unit) {
			Preconditions.checkArgument(dur >= 0);
			dur = unit.toSeconds(dur);
			this.updateIntervalSeconds = dur == 0 ? DEFAULT_UPDATE_INTERVAL_SECONDS : dur;
			return this;
		}

		@Override
		protected LocationModule load(Origin sdk, Context ctxt) {
			LocationModule mod =
				Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ?
				new SImpl(sdk, ctxt, this.updateIntervalSeconds) :
				new LocationModule(sdk, ctxt, this.updateIntervalSeconds);

			mod.init();
			return mod;
		}

		@Override
		protected void reload(LocationModule mod, Context ctxt) {
			if (mod.updateIntervalSeconds != this.updateIntervalSeconds) {
				mod.updateIntervalSeconds = this.updateIntervalSeconds;
				mod.init();
			}
		}
	}

	/**
	 * Construct a new module provider.
	 *
	 * @return module provider
	 * @since 0.1
	 */
	public static Provider ofProvider() {
		return new Provider();
	}

	private long updateIntervalSeconds;
	private final OriginModuleEventBus geosUpdateEvent;
	private final @Nullable LocationManager manager;
	private final @Nullable Geocoder geocoder;
	// Lock protecting against updates.
	private final Lock updateLock;
	/*
	 * Location descriptors, where the 0-th descriptor, if any, is the best location. When this is
	 * `null`, location has not yet been updated. When location is not available, this is an empty
	 * list.
	 */
	private @Nullable List<Geo> geos;
	@VisibleForTesting
	final ExecutingService updater;

	private LocationModule(Origin sdk, Context ctxt, long updateIntervalSecs) {
		super(NAME, sdk);
		this.updateIntervalSeconds = updateIntervalSecs;
		this.geosUpdateEvent = super.registerEvent(GEOS_UPDATE_EVENT, true);
		this.manager =
			AndroidContexts.systemServiceOf(
				ctxt,
				LocationManager.class,
				Context.LOCATION_SERVICE
			);
		this.geocoder = Geocoder.isPresent() ? new Geocoder(ctxt) : null;
		this.updateLock = new ReentrantLock();
		this.updater = ExecutingService.of(NAME, this::update, sdk.backgroundExecutor());
	}

	/**
	 * Underlying location manager.
	 *
	 * @param ctxt context with which to access location manager
	 * @return location manager or {@code null} if location cannot be polled
	 */
	private @Nullable LocationManager manager(Context ctxt) {
		LocationManager man = this.manager;

		return (
			man != null &&
			AndroidContexts.hasAnyPermission(
				ctxt,
				Manifest.permission.ACCESS_COARSE_LOCATION,
				Manifest.permission.ACCESS_FINE_LOCATION
			) ? man : null
		);
	}

	/**
	 * Test whether module has been destroyed.
	 *
	 * @return {@code true} if, and only if, module has been destroyed
	 */
	@VisibleForTesting
	boolean isDestroyed() {
		return this.updateIntervalSeconds == -1;
	}

	/**
	 * List of all location descriptors.
	 * <p>The first location in the returned list, if any, is the descriptor of the best location
	 * with respect to {@linkplain Geo#horizontalAccuracyMeters() accuracy} and fix {@linkplain
	 * Geo#timestampSeconds() timestamp}.
	 *
	 * @return location descriptors list, {@linkplain List#isEmpty() empty} list if location is
	 * unavailable, or {@code null} if location has not yet been updated
	 * @since 0.1
	 */
	public @Nullable List<Geo> geos() {
		return this.geos;
	}

	/**
	 * Retrieve best location descriptor, if any.
	 * <p>Shorthand for:
	 * {@snippet lang="java" :
	 * List<Geo> geos = geos(); // @link substring="geos()" target="#geos()"
	 *
	 * if (geos == null || geos.isEmpty())
	 *     return null;
	 * else
	 *     return geos.get(0);
	 * }
	 *
	 * @return descriptor of best location or {@code null} if unavailable
	 * @since 1.1
	 * @see #geos()
	 */
	@SuppressWarnings("JavadocDeclaration")
	public @Nullable Geo geo() {
		List<Geo> geos = this.geos;

		return geos == null || geos.isEmpty() ? null : geos.get(0);
	}

	@RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
	private static final class ReverseGeocodeListener implements Geocoder.GeocodeListener {

		private final Exchanger<List<Address>> exchanger;

		ReverseGeocodeListener(Exchanger<List<Address>> xchg) {
			this.exchanger = xchg;
		}

		@Override
		public void onGeocode(@NonNull List<Address> addrs) {
			try {
				this.exchanger.exchange(addrs, 1, TimeUnit.SECONDS);
			} catch (InterruptedException | TimeoutException err) {
				Logger.warn(TAG, "failed to exchange address result", err);
			}
		}

		@Override
		public void onError(@Nullable String msg) {
			Logger.info(TAG, "failed to reverse geocode: %s", msg);
			this.onGeocode(Collections.emptyList());
		}
	}

	/**
	 * Perform reverse geocode lookup for a geographic location.
	 *
	 * @param geo location to lookup
	 * @return lookup result or {@code null} if unavailable
	 * @throws Exception error encountered while performing lookup
	 */
	@SuppressWarnings({ "RedundantSuppression", "deprecation" })
	private @Nullable Address reverseGeocode(Geo geo) throws Exception {
		if (this.geocoder == null || (geo.latitudeDegrees() == 0 && geo.longitudeDegrees() == 0))
			return null;

		List<Address> res;

		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
			//noinspection resource
			res = super.sdk().callIoInBackground(() -> this.geocoder.getFromLocation(
					geo.latitudeDegrees(),
					geo.longitudeDegrees(),
					1
				))
				.get(REVERSE_GEOCODE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
		} else {
			Exchanger<List<Address>> xchg = new Exchanger<>();

			this.geocoder.getFromLocation(
				geo.latitudeDegrees(),
				geo.longitudeDegrees(),
				1,
				new ReverseGeocodeListener(xchg)
			);
			res = super.sdk().callIoInBackground(
				() -> xchg.exchange(null, REVERSE_GEOCODE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
			).get();
		}
		return res.isEmpty() ? null : res.get(0);
	}

	/**
	 * Supplement best geographic location with reverse geocode information, if required.
	 *
	 * @param geos location descriptions to update
	 * @return updated geographic location descriptions or {@code null} if no location was updated
	 */
	private @Nullable List<Geo> reverseGeocodeBest(List<Geo> geos) {
		Geo best = geos.get(0);

		if (!best.countryCode().isEmpty())
			return null;

		Address addr = null;

		try {
			addr = this.reverseGeocode(best);
		} catch (Exception err) {
			Logger.warn(TAG, "reverse geocode failed", err);
		}

		if (addr == null)
			return null;

		best =
			best.toBuilder()
				.countryCode(Strings.nullToEmpty(addr.getCountryCode()))
				.build();

		geos = new ArrayList<>(geos);
		geos.set(0, best);
		return geos;
	}

	/**
	 * Sort geos based on accuracy and staleness.
	 * <p>Upon return, the first element of {@code geos} will be the location with the highest
	 * {@linkplain Geo#horizontalAccuracyMeters() accuracy} and latest {@linkplain
	 * Geo#timestampSeconds() timestamp}. Additionally, a reverse geocode {@linkplain
	 * #reverseGeocode(Geo) lookup} will be performed, if possible.
	 *
	 * @param geos non-{@linkplain List#isEmpty() empty} descriptors to shift
	 */
	private void shiftBestGeo(List<Geo> geos) {
		Preconditions.checkArgument(!geos.isEmpty());

		Geo head = geos.get(0);
		Geo best = head;
		int bestIdx = 0;

		for (int i = 1; i < geos.size(); i++) {
			Geo curr = geos.get(i);

			if (
				Long.compareUnsigned(best.timestampSeconds(), curr.timestampSeconds()) < 0 &&
				best.horizontalAccuracyMeters() < curr.horizontalAccuracyMeters()
			) {
				best = curr;
				bestIdx = i;
			}
		}

		geos.set(0, best);
		geos.set(bestIdx, head);
	}

	/**
	 * Prepare location descriptors.
	 *
	 * @param locs updated locations
	 * @param currGeos current location descriptors
	 * @return updated descriptors or {@code currDescs} if no update
	 */
	private @Nullable List<Geo> prepare(List<Location> locs, @Nullable List<Geo> currGeos) {
		if (locs.isEmpty())
			return currGeos;

		ArrayList<Geo> newGeos = new ArrayList<>(locs.size());
		boolean any = false;

		if (currGeos != null)
			newGeos.addAll(currGeos);

nextLoc:
		for (int i = 0; i < locs.size(); i++) {
			Geo newGeo = Geo.ofLocation(locs.get(i));

			for (int j = 0; j < newGeos.size(); j++) {
				Geo oldGeo = newGeos.get(j);

				if (!oldGeo.providerName().equals(newGeo.providerName()))
					continue;
				if (
					oldGeo.timestampSeconds() < newGeo.timestampSeconds() ||
					oldGeo.timestampSeconds() == 0
				) {
					// New location is newer, just replace old one.
					newGeos.set(j, newGeo);
					any = true;
				}
				continue nextLoc;
			}

			// New loation does not have a descriptor yet, append it.
			newGeos.add(newGeo);
			any = true;
		}
		if (any) {
			shiftBestGeo(newGeos);
			return newGeos;
		}
		return currGeos;
	}

	/**
	 * Prepare and publish new location descriptors.
	 * <p>This must be invoked without {@linkplain #updateLock update lock} held.
	 *
	 * @param locs locations to build descriptors from, or empty list if location is unavailable
	 */
	@WorkerThread
	private void publish(List<Location> locs) {
		List<Geo> res = null;

		while (true) {
			List<Geo> currDescs = this.geos;
			List<Geo> updDescs = this.prepare(locs, currDescs);

			if (updDescs == null || updDescs.isEmpty())
				updDescs = Collections.emptyList();
			else if (currDescs == updDescs)
				break;

			this.updateLock.lock();
			try {
				if (this.geos == currDescs) {
					res = updDescs;
					this.geos = updDescs;
					this.geosUpdateEvent.submit(updDescs);
					Logger.debug(TAG, "location updated: %s", updDescs);
					break;
				}
			} finally {
				this.updateLock.unlock();
			}
		}

		if (res == null || res.isEmpty())
			return;

		List<Geo> rgc = this.reverseGeocodeBest(res);

		if (rgc == null)
			return;

		this.updateLock.lock();
		try {
			if (this.geos == res) {
				this.geos = rgc;
				this.geosUpdateEvent.submit(rgc);
				Logger.debug(TAG, "reverse geocoded location updated: %s", rgc);
			}
		} finally {
			this.updateLock.unlock();
		}
	}

	/**
	 * Poll the latest pooled location, if any.
	 * <p>This must be invoked with {@linkplain #updateLock update lock} held.
	 *
	 * @param ctxt context to poll from
	 * @return pooled location or {@code null}
	 * @see SImpl
	 */
	@WorkerThread
	@GuardedBy("updateLock")
	@Nullable Location pollPooledLocation(Context ctxt) {
		return null;
	}

	/**
	 * Perform a single location descriptor update.
	 * <p>This must be invoked without {@linkplain #updateLock update lock} held.
	 *
	 * @param ctxt context to update from
	 * @param pooled pooled location to update with, if any
	 */
	@WorkerThread
	@SuppressLint("MissingPermission")
	private void doUpdate(Context ctxt, @Nullable Location pooled) {
		LocationManager man = this.manager(ctxt);

		if (man == null) {
			this.publish(
				pooled == null ? Collections.emptyList() :
				Collections.singletonList(pooled)
			);
			return;
		}

		List<String> provs = man.getAllProviders();
		ArrayList<Location> locs = new ArrayList<>(provs.size());

		for (String prov : provs) {
			Location curr = null;

			try {
				curr = man.getLastKnownLocation(prov);
			} catch (IllegalArgumentException | SecurityException ignored) {
			}

			if (curr == null)
				continue;
			if (curr.getProvider() == null)
				curr.setProvider(prov);
			if (
				pooled != null &&
				TextUtils.equals(pooled.getProvider(), curr.getProvider())
			) {
				if (pooled.getTime() > curr.getTime())
					curr = pooled;
				pooled = null;
			}
			locs.add(curr);
		}
		if (pooled != null)
			locs.add(pooled);
		this.publish(locs);
	}

	/**
	 * Update location descriptors if required.
	 * <p>This performs an initial location update, followed by continuous updates of location
	 * descriptors for as long as a {@linkplain #pollPooledLocation(Context) pooled} location is
	 * available. Upon return, a new update is {@linkplain #scheduleUpdate(boolean) scheduled}, if
	 * possible.
	 */
	@WorkerThread
	private void update() {
		this.updateLock.lock();
		try {
			Context ctxt = super.context();
			Location pooled = this.pollPooledLocation(ctxt);

			this.updateLock.unlock();
			try {
				this.doUpdate(ctxt, pooled);
			} catch (Throwable e) {
				Logger.warn(TAG, "update failed", e);
			} finally {
				this.updateLock.lock();
			}
			this.scheduleUpdate(false);
		} finally {
			this.updateLock.unlock();
		}
	}

	/**
	 * Schedule a location update.
	 * <p>This must be invoked with {@linkplain #updateLock update lock} held. If there is already
	 * a location update scheduled, this simply returns. Otherwise, if this module has not yet been
	 * {@linkplain #isDestroyed() destroyed}, an update is scheduled at the configured {@linkplain
	 * #updateIntervalSeconds interval}.
	 * <p>If {@code now} is {@code true}, then an update is scheduled immediately. If an existing
	 * update has been scheduled but is <b>not</b> executing, then it is cancelled, and an
	 * immediate update is scheduled instead.
	 */
	@GuardedBy("updateLock")
	private void scheduleUpdate(boolean now) {
		this.updater.schedule(
			now ? 0 : TimeUnit.SECONDS.toMillis(this.updateIntervalSeconds),
			10,
			TimeUnit.MILLISECONDS
		);
	}

	/**
	 * Initialize or re-initialize module.
	 */
	private void init() {
		if (this.manager == null) {
			super.sdk().runInBackground(() -> this.publish(Collections.emptyList()));
			return;
		}

		this.updateLock.lock();
		try {
			this.scheduleUpdate(true);
		} finally {
			this.updateLock.unlock();
		}
	}

	@Override
	@CallSuper
	protected void destroy() {
		this.updater.shutdown();
		this.updateLock.lock();
		try {
			this.updateIntervalSeconds = -1;
		} finally {
			this.updateLock.unlock();
		}

		for (int i = 0; i < 5; i++) {
			try {
				if (this.updater.awaitShutdown(1, TimeUnit.SECONDS))
					break;
				Logger.warn(TAG, "updater shutdown taking longer than 1 second");
			} catch (InterruptedException cause) {
				Logger.info(TAG, "interrupted while awaiting updater shutdown", cause);
			}
		}
	}
}
