// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;
import android.util.Pair;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.polygamma.android.origin.util.ExecutingService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;

/**
 * {@link ConnectivityModule} tests.
 */
@RunWith(AndroidJUnit4.class)
public class ConnectivityModuleTest {

	private static final long POLL_DELAY_SECONDS = 5;

	/**
	 * Test whether an updater is asynchronous.
	 *
	 * @param updater updater to test
	 * @return {@code true} if, and only if, updater is asynchronous
	 */
	private static boolean isAsyncUpdater(ConnectivityUpdater updater) {
		if (
			Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
			updater instanceof ConnectivityNetworkUpdater.ImplLollipop
		) {
			return ((ConnectivityNetworkUpdater.ImplLollipop) updater).onUpdate != null;
		}
		if (
			Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1 &&
			updater instanceof ConnectivitySubscriptionUpdater.ImplLollipopMr1
		) {
			ConnectivitySubscriptionUpdater.ImplLollipopMr1 impl =
				(ConnectivitySubscriptionUpdater.ImplLollipopMr1) updater;

			return (
				impl.onActiveSubscriptionChanged != null &&
				(impl.subscriptions == null || impl.onSubscriptionChanged != null)
			);
		}
		return false;
	}

	private ConnectivityModule module;

	@Before
	public void setup() {
		this.module = TestUtil.callOnMainSync(
			() -> Origin.initialize(TestUtil.context())
				.loadModule(
					ConnectivityModule.ofProvider()
						.pollDelay(POLL_DELAY_SECONDS, TimeUnit.SECONDS)
				)
		);
	}

	@After
	public void destroy() throws InterruptedException {
		Origin sdk = this.module.sdk();

		sdk.shutdown();
		while (!sdk.awaitShutdown(10, TimeUnit.SECONDS)) {
			Log.w(
				ConnectivityModuleTest.class.getSimpleName(),
				"shutdown taking longer than 10 seconds..."
			);
		}
		assertTrue(this.module.pendingUpdaters.isEmpty());
		assertTrue(this.module.pollingUpdaters.isEmpty());
		assertNull(this.module.updaters);
		assertEquals(ExecutingService.STATE_SHUTDOWN, this.module.updateService.state());
	}

	private boolean supportsNetworkUpdater() {
		return this.module.context()
			.getSystemService(Context.CONNECTIVITY_SERVICE) != null;
	}

	private boolean supportsSubscriptionUpdater() {
		return this.module.context()
			.getSystemService(Context.TELEPHONY_SERVICE) != null;
	}

	@Test
	public void testUpdaters() {
		assertNotNull(this.module.updaters);

		ArrayList<ConnectivityUpdater> updaters = new ArrayList<>(2);
		boolean suppNet = this.supportsNetworkUpdater();
		boolean suppSub = this.supportsSubscriptionUpdater();

		assertEquals((suppNet ? 1 : 0) + (suppSub ? 1 : 0), this.module.updaters.length);
		Collections.addAll(updaters, this.module.updaters);
		while (!updaters.isEmpty()) {
			ConnectivityUpdater updater = updaters.remove(updaters.size() - 1);

			if (updater instanceof ConnectivityNetworkUpdater) {
				assertTrue(suppNet);
				suppNet = false;
			} else {
				assertTrue(updater instanceof ConnectivitySubscriptionUpdater);
				assertTrue(suppSub);
				suppSub = false;
			}
		}

		while (true) {
			if (this.module.updateService.nextExecutionDelayMillis() <= 15) {
				SystemClock.sleep(5);
				continue;
			}
			synchronized (this.module) {
				if (this.module.updateService.nextExecutionDelayMillis() <= 15)
					continue;
				for (ConnectivityUpdater updater : this.module.updaters) {
					if (isAsyncUpdater(updater))
						assertFalse(this.module.pollingUpdaters.containsKey(updater));
					else
						assertTrue(this.module.pollingUpdaters.containsKey(updater));
				}
				for (ConnectivityUpdater updater : this.module.updaters) {
					this.module.updateAvailable(updater);
					assertTrue(this.module.pendingUpdaters.containsKey(updater));
					assertEquals(0L, this.module.updateService.nextExecutionDelayMillis());
				}
			}
			break;
		}
	}

	@Test
	public void testConnectivity() {
		Connectivity desc = null;

		for (int i = 0; i < 5; i++) {
			desc = this.module.connectivity();
			if (desc != null)
				break;
			SystemClock.sleep(50);
		}
		assertNotNull(desc);

		if (!this.supportsNetworkUpdater())
			assertEquals(0, desc.networkCount());
		else
			assertNotEquals(0, desc.networkCount());

		if (!this.supportsSubscriptionUpdater())
			assertEquals(0, desc.subscriptionCount());
		else
			assertNotEquals(0, desc.subscriptionCount());
	}

	@Test
	public void testConnectivityUpdateEvent() throws InterruptedException {
		LinkedTransferQueue<Connectivity> updates = new LinkedTransferQueue<>();
		OriginModuleEventCallback onUpdate =
			(_mod, _name, data, _when) -> updates.add((Connectivity) data);
		Connectivity got = null;

		this.module.sdk().registerModuleEventCallback(onUpdate, new Pair<>(
			this.module,
			ConnectivityModule.CONNECTIVITY_UPDATE_EVENT
		));

		while (true) {
			Connectivity update = updates.poll(POLL_DELAY_SECONDS, TimeUnit.SECONDS);

			if (update == null) {
				assertSame(this.module.connectivity(), got);
				break;
			}
			got = update;
		}

		if (this.module.updaters.length == 0)
			return;

		while (true) {
			synchronized (this.module) {
				if (this.module.updateService.state() == ExecutingService.STATE_RUNNING) {
					SystemClock.sleep(5);
					continue;
				}
				for (int i = 0; i < this.module.updaters.length; i++) {
					ConnectivityUpdater updater = this.module.updaters[i];

					this.module.updaters[i] = new ConnectivityUpdater() {
						@Override
						Pair<Connectivity, Boolean> update(Connectivity dst, Context ctxt) {
							return new Pair<>(
								dst.withNetworks(Collections.singletonList(
									ConnectivityNetwork.ofBuilder()
										.proxyHost("test.com")
										.build()
								), 0)
									.withSubscriptions(Collections.emptyList(), 0),
								false
							);
						}

						@Override
						public void close() {
							updater.close();
						}
					};
					if (this.module.pollingUpdaters.containsKey(updater)) {
						this.module.pollingUpdaters.remove(updater);
						this.module.pollingUpdaters.put(
							this.module.updaters[i],
							this.module.updaters[i]
						);
					}
					if (this.module.pendingUpdaters.containsKey(updater)) {
						this.module.pendingUpdaters.remove(updater);
						this.module.pendingUpdaters.put(
							this.module.updaters[i],
							this.module.updaters[i]
						);
					}
				}
				for (ConnectivityUpdater updater : this.module.updaters)
					this.module.updateAvailable(updater);
			}
			break;
		}

		assertEquals(
			Connectivity.of()
				.withNetworks(Collections.singletonList(
					ConnectivityNetwork.ofBuilder()
						.proxyHost("test.com")
						.build()
				), 0),
			updates.poll(POLL_DELAY_SECONDS, TimeUnit.SECONDS)
		);
	}

	private void waitCanAccessInternet(LinkedTransferQueue<Connectivity> updates, boolean can) {
		while (true) {
			try {
				assertNotNull(updates.poll(1, TimeUnit.MINUTES));
			} catch (InterruptedException ignored) {
				continue;
			}

			if (this.module.canAccessInternet() != can)
				continue;

			while (true) {
				try {
					if (updates.poll(5, TimeUnit.SECONDS) == null)
						break;
				} catch (InterruptedException ignored) {
				}
			}

			if (this.module.canAccessInternet() == can)
				break;
		}
	}

	@Test
	@SdkSuppress(minSdkVersion = Build.VERSION_CODES.M)
	public void testCanAccessInternet() {
		LinkedTransferQueue<Connectivity> updates = new LinkedTransferQueue<>();
		OriginModuleEventCallback onUpdate =
			(_mod, _name, data, _when) -> updates.add((Connectivity) data);

		this.module.sdk().registerModuleEventCallback(onUpdate, new Pair<>(
			this.module,
			ConnectivityModule.CONNECTIVITY_UPDATE_EVENT
		));

		// wait until we get an event that shows internet is accessible
		TestUtil.toggleInternetAccess(true);
		this.waitCanAccessInternet(updates, true);

		TestUtil.toggleInternetAccess(false);
		try {
			// wait until we get an event that shows internet is accessible
			this.waitCanAccessInternet(updates, false);
		} finally {
			TestUtil.toggleInternetAccess(true);
		}

		// internet should be available again
		this.waitCanAccessInternet(updates, true);
	}
}
