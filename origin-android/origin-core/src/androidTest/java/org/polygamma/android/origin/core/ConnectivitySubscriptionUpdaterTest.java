// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.telephony.PhoneStateListener;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;
import android.util.Pair;

import androidx.annotation.RequiresApi;
import androidx.core.os.ExecutorCompat;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.rule.GrantPermissionRule;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.polygamma.android.origin.adcom.enums.AdComEnums;
import org.polygamma.android.origin.util.AndroidContexts;
import org.polygamma.android.origin.util.ListeningExecutor;
import org.polygamma.android.origin.util.Preconditions;
import org.polygamma.android.origin.util.Strings;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;

/**
 * {@link ConnectivitySubscriptionUpdater} test.
 */
@RunWith(AndroidJUnit4.class)
public class ConnectivitySubscriptionUpdaterTest {

	@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
	@SuppressLint("UseSdkSuppress")
	private static class Test22 {
		private static int activeSubscriptionId() {
			int id =
				Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ?
					SubscriptionManager.getActiveDataSubscriptionId() : -1;

			if (id == -1 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
				id = SubscriptionManager.getDefaultDataSubscriptionId();
			return id;
		}

		private static void assertSubscriptionEqual(
			TelephonyManager expTele,
			SubscriptionInfo expSub,
			ConnectivitySubscription got
		) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
				TelephonyManager tele = null;

				try {
					tele = expTele.createForSubscriptionId(expSub.getSubscriptionId());
				} catch (Exception ignored) {
				}
				if (tele != null) {
					ConnectivitySubscriptionUpdaterTest.assertSubscriptionEqual(tele, got);
					return;
				}
			}

			if (
				expSub.getSubscriptionId() != -1 &&
				activeSubscriptionId() == expSub.getSubscriptionId()
			) {
				ConnectivitySubscriptionUpdaterTest.assertSubscriptionEqual(expTele, got);
			} else {
				assertEquals(expSub.getSubscriptionId(), got.id());
				assertEquals(AdComEnums.ConnectionCell, got.connectionType());
				assertEquals(Strings.nullToEmpty(expSub.getCountryIso()), got.operatorCountryCode());
				assertEquals(expSub.getMcc(), got.operatorMcc());
				assertEquals(expSub.getMnc(), got.operatorMnc());
			}
		}
	}

	@SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP_MR1)
	public static final class TestWithReadPhoneStatePermission
	extends ConnectivitySubscriptionUpdaterTest {
		@ClassRule
		public static final GrantPermissionRule permissionRule =
			GrantPermissionRule.grant(Manifest.permission.READ_PHONE_STATE);
	}

	private static ExecutorService backgroundExecutor;
	private static ExecutorService foregroundExecutor;

	private static boolean isNotSupported() {
		return TestUtil.context().getSystemService(Context.TELEPHONY_SERVICE) == null;
	}

	@BeforeClass
	public static void setup() {
		Handler handler = new Handler(TestUtil.context().getMainLooper());

		backgroundExecutor = Executors.newFixedThreadPool(1);
		foregroundExecutor = new ListeningExecutor(handler, ExecutorCompat.create(handler));
	}

	@AfterClass
	public static void destroy() {
		backgroundExecutor.shutdownNow();
		foregroundExecutor.shutdownNow();
	}

	@Test
	public void testOf() {
		ConnectivitySubscriptionUpdater updater =
			ConnectivitySubscriptionUpdater.of(
				TestUtil.context(),
				backgroundExecutor,
				foregroundExecutor
			);

		if (isNotSupported()) {
			assertNull(updater);
		} else {
			assertNotNull(updater);

			try {
				assertNotNull(updater.telephonyManager);
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
					assertTrue(updater instanceof ConnectivitySubscriptionUpdater.ImplLollipopMr1);

					ConnectivitySubscriptionUpdater.ImplLollipopMr1 impl =
						(ConnectivitySubscriptionUpdater.ImplLollipopMr1) updater;

					assertNull(impl.onSubscriptionChanged);
					assertNull(impl.onActiveSubscriptionChanged);
				} else {
					assertEquals(ConnectivitySubscriptionUpdater.class, updater.getClass());
				}
			} finally {
				updater.close();
			}
		}
	}

	private static void
	assertSubscriptionEqual(TelephonyManager exp, ConnectivitySubscription got) {
		assertEquals(
			Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ? exp.getSubscriptionId() : -1,
			got.id()
		);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
			assertEquals(exp.getSimCarrierId(), got.carrierId());
			assertEquals(
				Preconditions.checkNotNullElse(exp.getSimCarrierIdName(), "").toString(),
				got.carrierName()
			);
		} else {
			assertEquals(-1, got.carrierId());
		}

		assertEquals(exp.getSimCountryIso(), got.operatorCountryCode());
		if (exp.getSimState() == TelephonyManager.SIM_STATE_READY) {
			assertEquals(Strings.nullToEmpty(exp.getSimOperator()), got.operatorCode());
			assertEquals(Strings.nullToEmpty(exp.getSimOperatorName()), got.operatorName());
		} else {
			assertEquals("", got.operatorCode());
			assertEquals("", got.operatorName());
		}

		assertEquals(Strings.nullToEmpty(exp.getNetworkOperator()), got.networkOperatorCode());
		assertEquals(Strings.nullToEmpty(exp.getNetworkOperatorName()), got.networkOperatorName());
		assertEquals(
			Strings.nullToEmpty(exp.getNetworkCountryIso()),
			got.networkOperatorCountryCode()
		);

		int expNetType = TelephonyManager.NETWORK_TYPE_UNKNOWN;

		if (AndroidContexts.hasAnyPermission(
			TestUtil.context(),
			Manifest.permission.READ_BASIC_PHONE_STATE,
			Manifest.permission.READ_PHONE_STATE
		)) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
				expNetType = exp.getDataNetworkType();
			if (expNetType == TelephonyManager.NETWORK_TYPE_UNKNOWN)
				expNetType = exp.getNetworkType();
		}
		assertEquals(ConnectivityModule.connectionTypeOfNetwork(expNetType), got.connectionType());
	}

	@Test
	@SdkSuppress(maxSdkVersion = Build.VERSION_CODES.LOLLIPOP)
	public void test19() throws InterruptedException {
		if (isNotSupported())
			return;

		LinkedTransferQueue<ConnectivityUpdater> updates = new LinkedTransferQueue<>();

		try (
			ConnectivitySubscriptionUpdater updater =
				ConnectivitySubscriptionUpdater.of(
					TestUtil.context(),
					backgroundExecutor,
					foregroundExecutor
				)
		) {
			assertNotNull(updater);
			updater.setOnUpdateAvailable(updates::add);

			Pair<Connectivity, Boolean> updRes =
				updater.update(Connectivity.of(), TestUtil.context());

			assertFalse(updRes.second);

			Connectivity got = updRes.first;

			assertEquals(0, got.networkCount());
			assertNull(got.activeNetwork());
			assertEquals(1, got.subscriptionCount());
			assertSame(got.subscription(0), got.activeSubscription());
			assertSubscriptionEqual(updater.telephonyManager, got.activeSubscription());
		}
		assertNull(updates.poll(5, TimeUnit.SECONDS));
	}

	@Test
	@SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP_MR1)
	public void test22() throws InterruptedException {
		if (isNotSupported())
			return;

		boolean canReadPhoneState =
			AndroidContexts.hasPermission(
				TestUtil.context(),
				Manifest.permission.READ_PHONE_STATE
			);
		LinkedTransferQueue<ConnectivityUpdater> updates = new LinkedTransferQueue<>();
		ConnectivitySubscriptionUpdater.ImplLollipopMr1 updater =
			(ConnectivitySubscriptionUpdater.ImplLollipopMr1)
				ConnectivitySubscriptionUpdater.of(
					TestUtil.context(),
					backgroundExecutor,
					foregroundExecutor
				);

		assertNotNull(updater);
		try {
			updater.setOnUpdateAvailable(updates::add);

			List<SubscriptionInfo> exp =
				updater.subscriptions == null || !canReadPhoneState ? null :
				Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ?
				updater.subscriptions.getAllSubscriptionInfoList() :
				updater.subscriptions.getActiveSubscriptionInfoList();
			Pair<Connectivity, Boolean> updRes =
				updater.update(Connectivity.of(), TestUtil.context());
			boolean async = updRes.second;
			Connectivity got = updRes.first;

			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || (
				Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
				!canReadPhoneState
			)) {
				assertNull(updater.onActiveSubscriptionChanged);
			} else {
				assertNotNull(updater.onActiveSubscriptionChanged);
			}
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || (
				Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
				!canReadPhoneState
			)) {
				assertFalse(async);
			} else {
				assertTrue(async);
			}
			if (exp == null)
				assertNull(updater.onSubscriptionChanged);
			else
				assertNotNull(updater.onSubscriptionChanged);

			assertEquals(0, got.networkCount());
			assertNull(got.activeNetwork());
			assertSame(exp == null ? null : updater, updates.poll(1, TimeUnit.SECONDS));
			if (exp == null || exp.isEmpty()) {
				assertEquals(1, got.subscriptionCount());
				assertSame(got.subscription(0), got.activeSubscription());
				assertSubscriptionEqual(updater.telephonyManager, got.activeSubscription());
			} else {
				assertEquals(exp.size(), got.subscriptionCount());

				int activeId = Test22.activeSubscriptionId();

				for (int i = 0; i < exp.size(); i++) {
					Test22.assertSubscriptionEqual(
						updater.telephonyManager,
						exp.get(i),
						got.subscription(i)
					);
					if (activeId != -1 && exp.get(i).getSubscriptionId() == activeId)
						assertSame(got.subscription(i), got.activeSubscription());
				}
				if (activeId == -1)
					assertNull(got.activeSubscription());
			}

			if (exp == null && updater.onActiveSubscriptionChanged == null)
				return;

			if (exp != null) {
				((SubscriptionManager.OnSubscriptionsChangedListener)
					updater.onSubscriptionChanged
				).onSubscriptionsChanged();
				assertSame(updater, updates.poll(1, TimeUnit.SECONDS));
			}
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
				((TelephonyCallback.ActiveDataSubscriptionIdListener)
					updater.onActiveSubscriptionChanged
				).onActiveDataSubscriptionIdChanged(Test22.activeSubscriptionId());
			} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
				((PhoneStateListener) updater.onActiveSubscriptionChanged)
					.onActiveDataSubscriptionIdChanged(Test22.activeSubscriptionId());
			}
			if (updater.onActiveSubscriptionChanged != null)
				assertSame(updater, updates.poll(1, TimeUnit.SECONDS));

			updRes = updater.update(Connectivity.of(), TestUtil.context());
			assertEquals(async, updRes.second);
			assertEquals(got, updRes.first);
		} finally {
			updater.close();
			assertNull(updater.onSubscriptionChanged);
			assertNull(updater.onActiveSubscriptionChanged);
		}
	}
}
