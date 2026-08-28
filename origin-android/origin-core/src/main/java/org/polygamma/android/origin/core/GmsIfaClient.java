// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.core;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Pair;

import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailabilityLight;

import org.polygamma.android.origin.util.Preconditions;
import org.polygamma.android.origin.util.Strings;

/**
 * Google Play Services advertising id client implementation.
 *
 * @see IfaClient#ofGms()
 */
final class GmsIfaClient implements IfaClient {

	/**
	 * Construct new GMS advertising id client.
	 */
	@SuppressWarnings(/* XXX: invoked via reflection */ "unused")
	public GmsIfaClient() {
	}

	@Override
	public String type() {
		return "gaid";
	}

	@Override
	@SuppressLint("AdvertisingIdPolicy")
	public Pair<String, Boolean> sendRequest(Origin sdk, Context ctxt) throws Exception {
		GoogleApiAvailabilityLight gms = GoogleApiAvailabilityLight.getInstance();

		Preconditions.checkState(
			gms.isGooglePlayServicesAvailable(ctxt) == ConnectionResult.SUCCESS,
			"GMS not available"
		);

		AdvertisingIdClient.Info info = AdvertisingIdClient.getAdvertisingIdInfo(ctxt);
		String ifa = Strings.nullToEmpty(info.getId());

		if (ifa.matches("^[-0]+$"))
			ifa = "";
		return new Pair<>(ifa, info.isLimitAdTrackingEnabled());
	}
}
