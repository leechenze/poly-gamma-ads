// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.TelephonyNetworkSpecifier;
import android.net.wifi.WifiInfo;
import android.os.Build;
import android.os.SystemClock;
import android.util.Pair;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.rule.GrantPermissionRule;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.polygamma.android.origin.adcom.enums.AdComEnums;
import org.polygamma.android.origin.util.AndroidContexts;
import org.polygamma.android.origin.util.Strings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * {@link ConnectivityNetworkUpdater} tests.
 */
@RunWith(AndroidJUnit4.class)
@SuppressWarnings("deprecation")
public class ConnectivityNetworkUpdaterTest {

	@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
	@SuppressLint("UseSdkSuppress")
	private static class Test21 {
		static List<ConnectivityManager.NetworkCallback>
		fillNetworkCallbackQueue(ConnectivityManager man) {
			List<ConnectivityManager.NetworkCallback> cbs = new ArrayList<>(100);

			while (true) {
				try {
					ConnectivityManager.NetworkCallback cb =
						new ConnectivityManager.NetworkCallback();

					man.registerNetworkCallback(
						(new NetworkRequest.Builder())
							.addTransportType(NetworkCapabilities.TRANSPORT_BLUETOOTH)
							.addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
							.addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
							.addTransportType(NetworkCapabilities.TRANSPORT_VPN)
							.addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
							.build(),
						cb
					);
					cbs.add(cb);
				} catch (Exception ignored) {
					break;
				}
				if (cbs.size() > 110) {
					// API levels below 25 for some reason don't enforce the limit...
					for (ConnectivityManager.NetworkCallback cb : cbs)
						man.unregisterNetworkCallback(cb);
					return Collections.emptyList();
				}
			}
			return cbs;
		}

		static Pair<
			Pair<Network, Pair<LinkProperties, NetworkCapabilities>>,
			List<Pair<Network, Pair<LinkProperties, NetworkCapabilities>>>
		> probeNetworks(ConnectivityManager man) {
			List<Pair<Network, Pair<LinkProperties, NetworkCapabilities>>> exp = new ArrayList<>();
			int actIdx = -1;

			for (Network net : man.getAllNetworks()) {
				if (
					Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
					net.equals(man.getActiveNetwork())
				) {
					actIdx = exp.size();
				}
				exp.add(new Pair<>(net, new Pair<>(
					man.getLinkProperties(net),
					man.getNetworkCapabilities(net)
				)));
			}
			return new Pair<>(actIdx == -1 ? null : exp.get(actIdx), exp);
		}

		static void assertNetworkEqual(
			Pair<Network, Pair<LinkProperties, NetworkCapabilities>> exp,
			ConnectivityNetwork got
		) {
			if (exp == null) {
				assertNull(got);
				return;
			}

			LinkProperties expLink = exp.second.first;
			NetworkCapabilities expCaps = exp.second.second;
			long id =
				Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ?
				exp.first.getNetworkHandle() : -1;

			assertEquals(id, got.id());
			if (expLink == null) {
				assertEquals(0, got.linkNameCount());
				assertEquals("", got.interfaceName());
				assertEquals("", got.proxyHost());
			} else {
				List<LinkAddress> expAddrs = expLink.getLinkAddresses();

				assertEquals(
					Strings.nullToEmpty(expLink.getInterfaceName()),
					got.interfaceName()
				);
				assertEquals(
					expLink.getHttpProxy() == null ? "" :
					Strings.nullToEmpty(expLink.getHttpProxy().getHost()),
					got.proxyHost()
				);
				assertEquals(expAddrs.size(), got.linkNameCount());
				for (int j = 0; j < expAddrs.size(); j++)
					assertEquals(expAddrs.get(j).getAddress(), got.linkName(j));
			}

			for (int[] expAndGot : new int[][] {
				{
					NetworkCapabilities.NET_CAPABILITY_INTERNET,
					ConnectivityNetwork.CapabilityInternet
				},
				{
					NetworkCapabilities.NET_CAPABILITY_MMS,
					ConnectivityNetwork.CapabilityMms
				},
				{
					Build.VERSION.SDK_INT < Build.VERSION_CODES.P ? -1 :
						NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING,
					ConnectivityNetwork.CapabilityNotRoaming
				},
				{
					NetworkCapabilities.NET_CAPABILITY_VALIDATED,
					ConnectivityNetwork.CapabilityValidated
				},
				{
					NetworkCapabilities.NET_CAPABILITY_NOT_METERED,
					ConnectivityNetwork.CapabilityUnmetered
				}
			}) {
				assertEquals(
					String.format("capability %s", expAndGot[0]),
					expAndGot[0] != -1 && expCaps != null && expCaps.hasCapability(expAndGot[0]),
					got.hasCapability(expAndGot[1])
				);
			}

			for (int[] expAndGot: new int[][] {
				{ NetworkCapabilities.TRANSPORT_BLUETOOTH, AdComEnums.ConnectionBluetooth },
				{ NetworkCapabilities.TRANSPORT_CELLULAR, AdComEnums.ConnectionCell },
				{ NetworkCapabilities.TRANSPORT_ETHERNET, AdComEnums.ConnectionWired },
				{ NetworkCapabilities.TRANSPORT_WIFI, AdComEnums.ConnectionWifi },
				{ NetworkCapabilities.TRANSPORT_VPN, AdComEnums.ConnectionVpn }
			}) {
				assertEquals(
					String.format("transport %s", expAndGot[0]),
					expCaps != null && expCaps.hasTransport(expAndGot[0]),
					got.supportsConnection(expAndGot[1])
				);
			}

			if (expCaps == null) {
				assertEquals(0, got.downstreamKbps());
				assertEquals(0, got.upstreamKbps());
				assertEquals("", got.wifiAddress());
				assertEquals("", got.interfaceAddress());
				assertEquals(-1, got.subscriptionId());
			} else {
				assertEquals(expCaps.getLinkDownstreamBandwidthKbps(), got.downstreamKbps());
				assertEquals(expCaps.getLinkUpstreamBandwidthKbps(), got.upstreamKbps());

				if (
					Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
						expCaps.getTransportInfo() instanceof WifiInfo
				) {
					WifiInfo info = (WifiInfo) expCaps.getTransportInfo();

					assertEquals(info.getBSSID(), got.wifiAddress());
					assertEquals(info.getMacAddress(), got.interfaceAddress());
				} else {
					assertEquals("", got.wifiAddress());
					assertEquals("", got.interfaceAddress());
				}

				if (
					Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
						expCaps.getNetworkSpecifier() instanceof TelephonyNetworkSpecifier
				) {
					assertEquals(
						((TelephonyNetworkSpecifier) expCaps.getNetworkSpecifier())
							.getSubscriptionId(),
						got.subscriptionId()
					);
				} else {
					assertEquals(-1, got.subscriptionId());
				}
			}
		}

		static void assertNetworksEqual(
			List<Pair<Network, Pair<LinkProperties, NetworkCapabilities>>> exp,
			ConnectivityNetwork[] got
		) {
			exp = new ArrayList<>(exp);

			assertEquals(exp.size(), got.length);
			for (int i = 0; i < got.length; i++) {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
					int expIdx = -1;

					for (int j = 0; j < exp.size(); j++) {
						if (exp.get(j).first.getNetworkHandle() == got[i].id()) {
							expIdx = j;
							break;
						}
					}
					assertNotEquals(String.format("%s not expected", got[i]), -1, expIdx);
					assertNetworkEqual(exp.remove(expIdx), got[i]);
				} else {
					boolean any = false;

					for (int j = 0; j < exp.size(); j++) {
						try {
							assertNetworkEqual(exp.get(j), got[i]);
							exp.remove(j);
							any = true;
							break;
						} catch (Throwable ignored) {
						}
					}
					assertTrue(String.format("%s not expected", got[i]), any);
				}
			}
		}
	}

	@SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
	public static class TestWithAccessFineLocationPermission
		extends ConnectivityNetworkUpdaterTest {
		@ClassRule
		public static GrantPermissionRule permissionRule =
			GrantPermissionRule.grant(Manifest.permission.ACCESS_FINE_LOCATION);
	}

	private static boolean isNotSupported() {
		return TestUtil.context().getSystemService(Context.CONNECTIVITY_SERVICE) == null;
	}

	@Test
	public void testOf() {
		ConnectivityNetworkUpdater updater = ConnectivityNetworkUpdater.of(TestUtil.context());

		if (isNotSupported()) {
			assertNull(updater);
		} else {
			assertNotNull(updater);

			try {
				assertNotNull(updater.connectivityManager);
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
					assertTrue(updater instanceof ConnectivityNetworkUpdater.ImplLollipop);
					assertNull(((ConnectivityNetworkUpdater.ImplLollipop) updater).onUpdate);
				} else {
					assertEquals(ConnectivityNetworkUpdater.class, updater.getClass());
				}
			} finally {
				updater.close();
			}
		}
	}

	@Test
	@SdkSuppress(maxSdkVersion = Build.VERSION_CODES.KITKAT_WATCH)
	public void test19() throws InterruptedException {
		if (isNotSupported())
			return;

		LinkedTransferQueue<ConnectivityUpdater> updates = new LinkedTransferQueue<>();

		try (
			ConnectivityNetworkUpdater updater =
				ConnectivityNetworkUpdater.of(TestUtil.context())
		) {
			assertNotNull(updater);
			updater.setOnUpdateAvailable(updates::add);

			NetworkInfo info = updater.connectivityManager.getActiveNetworkInfo();
			Pair<Connectivity, Boolean> updRes =
				updater.update(Connectivity.of(), TestUtil.context());
			Connectivity got = updRes.first;

			assertFalse(updRes.second);
			assertEquals(0, got.subscriptionCount());
			assertNull(got.activeSubscription());
			if (info == null) {
				assertEquals(0, got.networkCount());
				assertNull(got.activeNetwork());
			} else {
				assertEquals(1, got.networkCount());
				assertSame(got.network(0), got.activeNetwork());

				ConnectivityNetwork gotNet = got.activeNetwork();

				assertEquals(
					info.isAvailable(),
					gotNet.hasCapability(ConnectivityNetwork.CapabilityInternet)
				);
				assertEquals(
					info.isConnected(),
					gotNet.hasCapability(ConnectivityNetwork.CapabilityValidated)
				);
				assertTrue(
					gotNet.supportsConnection(ConnectivityModule.connectionTypeOfNetwork(
						info.getSubtype()
					)) ||
					gotNet.supportsConnection(ConnectivityModule.connectionTypeOfConnectivity(
						info.getType()
					))
				);
			}
		}
		assertNull(updates.poll(5, TimeUnit.SECONDS));
	}

	@Test
	@SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
	public void test21() throws InterruptedException {
		if (isNotSupported())
			return;

		LinkedTransferQueue<ConnectivityUpdater> updates = new LinkedTransferQueue<>();
		ConnectivityNetworkUpdater.ImplLollipop updater =
			(ConnectivityNetworkUpdater.ImplLollipop)
			ConnectivityNetworkUpdater.of(TestUtil.context());

		assertNotNull(updater);
		try {
			updater.setOnUpdateAvailable(updates::add);

			List<ConnectivityManager.NetworkCallback> cbs =
				Test21.fillNetworkCallbackQueue(updater.connectivityManager);
			Pair<
				Pair<Network, Pair<LinkProperties, NetworkCapabilities>>,
				List<Pair<Network, Pair<LinkProperties, NetworkCapabilities>>>
			> exp = Test21.probeNetworks(updater.connectivityManager);
			Pair<Connectivity, Boolean> updRes;
			Connectivity got;

			// we should have at least one network connection
			assertFalse(exp.second.isEmpty());
			assertNull(updater.onUpdate);
			if (!cbs.isEmpty()) {
				// first update should fail because we've filled up the callback queue
				updRes = updater.update(Connectivity.of(), TestUtil.context());
				assertFalse(updRes.second);
				assertNull(updater.onUpdate);

				got = updRes.first;
				assertEquals(0, got.subscriptionCount());
				assertNull(got.activeSubscription());
				assertEquals(exp.second.size(), got.networkCount());
				Test21.assertNetworkEqual(exp.first, got.activeNetwork());
				Test21.assertNetworksEqual(exp.second, got.networks);
				// there is no update callback, so there should be no queued updates
				assertNull(updates.poll(1, TimeUnit.SECONDS));
			}

			// now test with the update callback available
			for (ConnectivityManager.NetworkCallback cb : cbs)
				updater.connectivityManager.unregisterNetworkCallback(cb);

			SystemClock.sleep(1500);
			updRes = updater.update(Connectivity.of(), TestUtil.context());
			assertTrue(updRes.second);
			assertNotNull(updates.poll(1, TimeUnit.SECONDS));
			got = updRes.first;

			assertNotNull(updater.onUpdate);
			assertEquals(exp.second.size(), got.networkCount());
			Test21.assertNetworkEqual(exp.first, got.activeNetwork());
			Test21.assertNetworksEqual(exp.second, got.networks);

			// wait for callbacks to stabilize
			SystemClock.sleep(1500);

			// test removing a network
			updater.onUpdate.onLost(exp.second.get(0).first);
			assertSame(updater, updates.poll(1, TimeUnit.SECONDS));

			updRes = updater.update(Connectivity.of(), TestUtil.context());
			assertTrue(updRes.second);
			got = updRes.first;
			assertEquals(exp.second.size() - 1, got.networkCount());
			Test21.assertNetworksEqual(exp.second.subList(1, exp.second.size()), got.networks);

			// test adding a network
			updater.onUpdate.onAvailable(exp.second.get(0).first);
			assertSame(updater, updates.poll(1, TimeUnit.SECONDS));

			updRes = updater.update(Connectivity.of(), TestUtil.context());
			assertTrue(updRes.second);
			got = updRes.first;
			assertEquals(exp.second.size(), got.networkCount());
			Test21.assertNetworkEqual(exp.first, got.activeNetwork());
			Test21.assertNetworksEqual(exp.second, got.networks);
		} finally {
			updater.close();
			assertNull(updater.onUpdate);
		}
	}

	private static @Nullable ConnectivityManager connectivityManager() {
		return AndroidContexts.systemServiceOf(
			TestUtil.context(),
			ConnectivityManager.class,
			Context.CONNECTIVITY_SERVICE
		);
	}

	private static Connectivity currentDescription(ConnectivityNetworkUpdater upd) {
		return upd.update(Connectivity.of(), TestUtil.context()).first;
	}

	@Test
	@SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP_MR1)
	public void testUpdate19() {
		ConnectivityManager mgr = connectivityManager();
		List<String> disabledSvcNames = new ArrayList<>();

		if (mgr == null)
			return;

		try (ConnectivityNetworkUpdater upd = new ConnectivityNetworkUpdater(mgr)) {
			while (true) {
				NetworkInfo exp = mgr.getActiveNetworkInfo();
				ConnectivityNetwork got = currentDescription(upd).activeNetwork();

				if (exp == null) {
					assertNull(got);
					break;
				}

				int expType = ConnectivityModule.connectionTypeOfNetwork(exp.getSubtype());

				if (expType == 0)
					expType = ConnectivityModule.connectionTypeOfConnectivity(exp.getType());

				assertNotNull(got);
				assertEquals(
					exp.isAvailable(),
					got.hasCapability(ConnectivityNetwork.CapabilityInternet)
				);
				assertEquals(
					exp.isConnected(),
					got.hasCapability(ConnectivityNetwork.CapabilityValidated)
				);
				assertTrue(String.format("expType: %s", expType), got.supportsConnection(expType));
				assertTrue(
					expType == AdComEnums.ConnectionCell ||
					expType == AdComEnums.ConnectionCell2G ||
					expType == AdComEnums.ConnectionCell3G ||
					expType == AdComEnums.ConnectionCell4G ||
					expType == AdComEnums.ConnectionCell5G ||
					expType == AdComEnums.ConnectionWifi
				);

				String svcName = expType == AdComEnums.ConnectionWifi ? "wifi" : "data";

				assertFalse(svcName, disabledSvcNames.contains(svcName));
				TestUtil.executeShellCommand(String.format("svc %s disable", svcName));
				disabledSvcNames.add(svcName);
			}
		} finally {
			for (String svcName : disabledSvcNames)
				TestUtil.executeShellCommand(String.format("svc %s enable", svcName));
		}
	}

	@Test
	@SdkSuppress(minSdkVersion = Build.VERSION_CODES.M)
	public void testUpdate21() {
		ConnectivityManager mgr = connectivityManager();
		List<String> disabledSvcNames = new ArrayList<>();

		if (mgr == null)
			return;

		try (
			ConnectivityNetworkUpdater.ImplLollipop upd =
				new ConnectivityNetworkUpdater.ImplLollipop(mgr)
		) {
			AtomicBoolean updateAvail = new AtomicBoolean();

			upd.setOnUpdateAvailable((ign) -> updateAvail.set(true));
			while (true) {
				Network exp = mgr.getActiveNetwork();
				ConnectivityNetwork got = currentDescription(upd).activeNetwork();

				if (exp == null) {
					assertNull(got);
					break;
				}

				NetworkCapabilities expCaps = mgr.getNetworkCapabilities(exp);

				assertNotNull(got);
				assertNotNull(expCaps);
				assertEquals(
					expCaps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET),
					got.hasCapability(ConnectivityNetwork.CapabilityInternet)
				);
				assertEquals(
					expCaps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR),
					got.supportsConnection(AdComEnums.ConnectionCell)
				);
				assertEquals(
					expCaps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI),
					got.supportsConnection(AdComEnums.ConnectionWifi)
				);

				String svcName =
					got.supportsConnection(AdComEnums.ConnectionWifi) ? "wifi" : "data";

				assertFalse(disabledSvcNames.contains(svcName));
				TestUtil.executeShellCommand(String.format("svc %s disable", svcName));
				disabledSvcNames.add(svcName);
				// wait for callback to propagate up
				SystemClock.sleep(1000);
				while (updateAvail.getAndSet(false))
					SystemClock.sleep(10);
			}
		} finally {
			for (String svcName : disabledSvcNames)
				TestUtil.executeShellCommand(String.format("svc %s enable", svcName));
		}
	}
}
