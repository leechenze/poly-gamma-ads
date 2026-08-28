// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.core;

import static org.junit.Assert.assertTrue;

import android.util.Log;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.util.concurrent.TimeUnit;

/**
 * Initialize SDK before all tests and destroy after all tests.
 */
public abstract class TestWithSdk {

	private static final String TAG = TestWithSdk.class.getSimpleName();

	/**
	 * Current SDK instance.
	 */
	protected static Origin sdk;

	@BeforeClass
	public static void setupSdk() {
		sdk = TestUtil.callOnMainSync(() -> Origin.initialize(TestUtil.context()));
		sdk.settings()
			.edit()
			.clear()
			.apply();
	}

	@AfterClass
	public static void destroySdk() throws InterruptedException {
		if (sdk == null)
			return;

		sdk.settings()
			.edit()
			.clear()
			.apply();
		sdk.shutdown();
		while (!sdk.awaitShutdown(10, TimeUnit.SECONDS))
			Log.w(TAG, "shutdown taking longer than 10 seconds...");
		assertTrue(sdk.isShutdown());
		sdk = null;
	}
}
