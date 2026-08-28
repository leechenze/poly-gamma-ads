// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Pair;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.polygamma.android.origin.adcom.context.Device;
import org.polygamma.android.origin.util.ExecutingService;
import org.polygamma.android.origin.util.Sync;

import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * {@link DeviceModule} tests.
 */
public class DeviceModuleTest extends TestWithSdk {

	private static DeviceModule module;

	@BeforeClass
	public static void setup() {
		module =
			sdk.loadModule(
				DeviceModule.ofProvider()
					.ifaClients(
						IfaClient.ofStatic("test-1", "test-id-1"),
						IfaClient.ofStatic("test-2", "test-id-2"),
						new IfaClient() {
							@Override
							public String type() {
								return "test-3";
							}

							@Override
							public Pair<String, Boolean> sendRequest(Origin sdk, Context ctxt) {
								assertFalse(Sync.isMainThread());
								return new Pair<>("test-id-3", false);
							}
						},
						new IfaClient() {
							@Override
							public String type() {
								return "test-4";
							}

							@Override
							public Pair<String, Boolean> sendRequest(Origin sdk, Context ctxt) {
								throw new UnsupportedOperationException();
							}
						}
					)
			);
	}

	@AfterClass
	public static void destroy() throws InterruptedException {
		TestWithSdk.destroySdk();
		assertEquals(ExecutingService.STATE_SHUTDOWN, module.updater.state());
	}

	@Test
	public void testDevice() throws RemoteException, InterruptedException {
		UiDevice ui = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
		Configuration cfg = sdk.context().getResources().getConfiguration();
		Device device = null;

		for (int i = 0; i < 2; i++) {
			device = module.device();
			if (device != null)
				break;
			SystemClock.sleep(50);
		}

		assertNotNull(device);
		assertEquals(3, device.advertisingIdCount());
		assertEquals(new Pair<>("test-1", "test-id-1"), device.advertisingId(0));
		assertEquals(new Pair<>("test-2", "test-id-2"), device.advertisingId(1));
		assertEquals(new Pair<>("test-3", "test-id-3"), device.advertisingId(2));
		assertEquals(ui.getDisplayWidth(), device.screenWidthPx());
		assertEquals(ui.getDisplayHeight(), device.screenHeightPx());
		assertEquals(cfg.orientation == Configuration.ORIENTATION_LANDSCAPE, device.landscape());
		assertEquals(
			cfg.mcc == 0 ? "" : String.format(Locale.ROOT, "%03d%03d", cfg.mcc, cfg.mnc),
			device.simCarrierMccMnc()
		);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			assertEquals(cfg.getLocales().get(0).toLanguageTag(), device.languageCode());
			assertEquals(cfg.getLocales().size() - 1, device.extraLanguageCodeCount());
			for (int i = 1; i < cfg.getLocales().size(); i++) {
				assertEquals(
					cfg.getLocales()
						.get(i)
						.toLanguageTag(),
					device.extraLanguageCode(i - 1)
				);
			}
		} else {
			assertEquals(
				Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ? cfg.locale.toLanguageTag() :
				cfg.locale.getLanguage(),
				device.languageCode()
			);
		}

		// configuration change should trigger an update

		CountDownLatch updateLatch = new CountDownLatch(1);

		sdk.registerModuleEventCallback(
			(_src, _name, _data, _when) -> updateLatch.countDown(),
			new Pair<>(module, DeviceModule.DEVICE_UPDATE_EVENT)
		);

		if (device.landscape())
			ui.setOrientationPortrait();
		else
			ui.setOrientationLandscape();

		if (!updateLatch.await(2, TimeUnit.SECONDS)) {
			// Sometimes the automator fails to apply orientation change, force it
			Resources res = sdk.context().getResources();
			Configuration newCfg = new Configuration(cfg);

			newCfg.orientation =
				device.landscape() ? Configuration.ORIENTATION_PORTRAIT :
				Configuration.ORIENTATION_LANDSCAPE;
			module.updater.onConfigurationChanged(newCfg);
		}
		assertTrue(updateLatch.await(1, TimeUnit.SECONDS));
		assertNotEquals(device.landscape(), module.device().landscape());
	}
}
