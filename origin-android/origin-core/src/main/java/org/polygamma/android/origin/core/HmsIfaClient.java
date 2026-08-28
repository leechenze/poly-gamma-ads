// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.core;

import android.content.Context;
import android.util.Pair;

import com.huawei.hms.ads.identifier.AdvertisingIdClient;

import org.polygamma.android.origin.util.Strings;

/**
 * Huawei Mobile Services (HMS) advertising id client.
 *
 * @see IfaClient#ofHms()
 */
final class HmsIfaClient implements IfaClient {

	private static boolean needLoadMsa() {
		try {
			Class.forName("com.bun.miitmdid.interfaces.IIdentifierListener");
			return true;
		} catch (ClassNotFoundException | LinkageError ignored) {
			return false;
		}
	}

	/**
	 * Construct new HMS advertising id client.
	 */
	public HmsIfaClient() {
	}

	@Override
	public String type() {
		return "hmsoaid";
	}

	@Override
	public Pair<String, Boolean> sendRequest(Origin sdk, Context ctxt) throws Exception {
		if (needLoadMsa()) {
			try {
				System.loadLibrary("msaoaidsec");
			} catch (SecurityException | UnsatisfiedLinkError ignored) {
			}
		}

		AdvertisingIdClient.Info info = AdvertisingIdClient.getAdvertisingIdInfo(ctxt);

		//noinspection ConstantValue
		return info == null ? new Pair<>("", Boolean.FALSE) : new Pair<>(
			Strings.nullToEmpty(info.getId()),
			info.isLimitAdTrackingEnabled()
		);
	}
}
