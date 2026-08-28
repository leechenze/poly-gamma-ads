// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.location.provider.ProviderProperties;
import android.os.Build;
import android.os.SystemClock;
import android.util.Pair;

import androidx.annotation.RequiresApi;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.GrantPermissionRule;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.polygamma.android.origin.adcom.context.Geo;
import org.polygamma.android.origin.util.AndroidContexts;
import org.polygamma.android.origin.util.ExecutingService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;

/**
 * {@link LocationModule} tests.
 */
@RunWith(AndroidJUnit4.class)
public class LocationModuleTest extends TestWithSdk {

	private static final String[] PROVIDER_NAMES =
		{
			LocationManager.GPS_PROVIDER,
			LocationManager.NETWORK_PROVIDER
		};

	/**
	 * {@link LocationModule} tests with location permissions.
	 */
	public static class WithPermissionTest extends LocationModuleTest {

		@ClassRule
		public static final GrantPermissionRule permissionRule =
			GrantPermissionRule.grant(
				Manifest.permission.ACCESS_COARSE_LOCATION,
				Manifest.permission.ACCESS_FINE_LOCATION
			);

		@ClassRule
		@RequiresApi(api = Build.VERSION_CODES.Q)
		public static final GrantPermissionRule backgroundPermissionRule =
			GrantPermissionRule.grant(Manifest.permission.ACCESS_BACKGROUND_LOCATION);

		@Override
		boolean hasLocation() {
			return AndroidContexts.hasSystemFeature(
				sdk.context(),
				PackageManager.FEATURE_LOCATION
			);
		}
	}

	@ClassRule
	public final static GrantPermissionRule permissionRule;

	static {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
			TestUtil.executeShellCommand("settings put secure mock_location 1");
			TestUtil.executeShellCommand(String.format(
				"appops set %s android:mock_location allow",
				TestUtil.packageName()
			));
			permissionRule = null;
		} else {
			permissionRule = GrantPermissionRule.grant("android.permission.ACCESS_MOCK_LOCATION");
		}
	}

	private static LocationModule module;

	@BeforeClass
	public static void setup() {
		module =
			sdk.loadModule(
				LocationModule.ofProvider()
					.updateInterval(1, TimeUnit.SECONDS)
			);

		LocationManager man =
			(LocationManager) sdk.context()
				.getSystemService(Context.LOCATION_SERVICE);

		if (man != null) {
			for (String name : PROVIDER_NAMES) {
				man.addTestProvider(
					name,
					false,
					false,
					false,
					false,
					false,
					false,
					false,
					ProviderProperties.POWER_USAGE_LOW,
					ProviderProperties.ACCURACY_FINE
				);
				man.setTestProviderEnabled(name, false);
			}
		}
	}

	@AfterClass
	public static void destroy() throws InterruptedException, IOException {
		TestWithSdk.destroySdk();

		assertTrue(module.isDestroyed());
		assertEquals(ExecutingService.STATE_SHUTDOWN, module.updater.state());

		LocationManager man =
			(LocationManager) TestUtil.context()
				.getSystemService(Context.LOCATION_SERVICE);

		if (man != null) {
			for (String name : PROVIDER_NAMES)
				man.removeTestProvider(name);
		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1)
			TestUtil.executeShellCommand("settings put secure mock_location 0");
	}

	/**
	 * Test whether location should be available.
	 *
	 * @return {@code true} if, and only if, location should be available
	 */
	boolean hasLocation() {
		return false;
	}

	/**
	 * Test whether auto updates are supported by module.
	 *
	 * @return {@code true} if, and only if, module supports auto update
	 */
	private boolean hasAutoUpdate() {
		return (
			Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
			module instanceof LocationModule.SImpl &&
			((LocationModule.SImpl) module).updatesRequested
		);
	}

	private LocationManager locationManager() {
		return (LocationManager) sdk.context().getSystemService(Context.LOCATION_SERVICE);
	}

	@Before
	public void enableProviders() {
		if (!this.hasLocation())
			return;

		LocationManager man = this.locationManager();

		for (String name : PROVIDER_NAMES)
			man.setTestProviderEnabled(name, true);
	}

	@After
	public void disableProviders() {
		if (!this.hasLocation())
			return;

		LocationManager loc =
			(LocationManager) sdk.context().getSystemService(Context.LOCATION_SERVICE);

		for (String name : PROVIDER_NAMES)
			loc.setTestProviderEnabled(name, false);
	}

	/**
	 * Generate a random location.
	 *
	 * @param provName name of provider to generate location for
	 * @return random location
	 */
	private static Location randomLocation(String provName) {
		Random rand = new Random();
		Location rv = new Location(provName);

		// see `MAX_ACCURACY_M` https://android.googlesource.com/platform/frameworks/base/+/refs/heads/master/location/java/android/location/LocationResult.java
		rv.setAccuracy(1000000 - rand.nextInt(1000));
		rv.setLatitude(-90 + (rand.nextDouble() * (90 - -90)));
		rv.setLongitude(-180 + (rand.nextDouble() * (180 - -180)));
		rv.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
		rv.setTime(System.currentTimeMillis());
		return rv;
	}

	/**
	 * Update providers with random locations and return resulting descriptors.
	 *
	 * @return resulting descriptors, where first descriptor is descriptor of best location
	 */
	private List<Geo> updateProviders() {
		LocationManager man =
			(LocationManager) sdk.context().getSystemService(Context.LOCATION_SERVICE);
		List<Geo> geos = new ArrayList<>(PROVIDER_NAMES.length);

		for (String name : PROVIDER_NAMES) {
			Location loc = randomLocation(name);
			Geo geo = Geo.ofLocation(loc);

			man.setTestProviderLocation(name, loc);
			if (geos.isEmpty() || (
				geos.get(0).horizontalAccuracyMeters() > geo.horizontalAccuracyMeters() &&
				Long.compareUnsigned(geos.get(0).timestampSeconds(), geo.timestampSeconds()) > 0
			)) {
				geos.add(geo);
			} else {
				geos.add(0, geo);
			}
		}
		return geos;
	}

	@Test
	public void testLocationFeature() {
		boolean has = AndroidContexts.hasSystemFeature(
			sdk.context(),
			PackageManager.FEATURE_LOCATION
		);
		int state = module.updater.state();

		if (has) {
			assertTrue(
				state == ExecutingService.STATE_SCHEDULED ||
				state == ExecutingService.STATE_RUNNING
			);
		} else {
			assertEquals(ExecutingService.STATE_IDLE, module.updater.state());
		}
	}

	@Test
	public void testGeos() {
		List<Geo> geos = module.geos();

		for (int i = 0; i < 2; i++) {
			geos = module.geos();
			if (geos != null)
				break;
			SystemClock.sleep(50);
		}

		assertNotNull(geos);
		if (!geos.isEmpty())
			assertSame(geos.get(0), module.geo());
		else
			assertNull(module.geo());

		for (int i = 0, n = this.hasLocation() ? 5 : 0; i < n; i++) {
			List<Geo> exp = this.updateProviders();

			SystemClock.sleep(TimeUnit.SECONDS.toMillis(2));

			geos = module.geos();
			assertEquals(exp, geos);
			assertSame(geos.get(0), module.geo());
		}

		SystemClock.sleep(TimeUnit.SECONDS.toMillis(2));
		assertSame(geos, module.geos());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testGeosUpdateEvent() throws InterruptedException {
		LinkedTransferQueue<List<Geo>> updates = new LinkedTransferQueue<>();

		sdk.registerModuleEventCallback(
			(_mod, _name, data, _when) -> {
				assertSame(module.geos(), data);
				updates.add((List<Geo>) data);
			},
			new Pair<>(module, LocationModule.GEOS_UPDATE_EVENT)
		);

		assertSame(module.geos(), updates.poll(2, TimeUnit.SECONDS));

		if (!this.hasLocation())
			return;

		List<List<Geo>> exp = new ArrayList<>(5);

		for (int i = 0; i < 5; i++) {
			exp.add(this.updateProviders());
			SystemClock.sleep(this.hasAutoUpdate() ? 1000 : 1500);
		}

		List<List<Geo>> got = new ArrayList<>(5);

		updates.drainTo(got);
		assertEquals(exp, got);

		SystemClock.sleep(2000);

		assertTrue(updates.isEmpty());
	}
}
