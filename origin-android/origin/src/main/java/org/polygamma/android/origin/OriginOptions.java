// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin;

import android.annotation.SuppressLint;
import android.content.Context;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Pair;

import androidx.annotation.ReturnThis;
import androidx.core.util.Function;

import org.polygamma.android.origin.core.IfaClient;
import org.polygamma.android.origin.util.AndroidContexts;
import org.polygamma.android.origin.util.AndroidSettings;
import org.polygamma.android.origin.util.Preconditions;
import org.polygamma.android.origin.util.Strings;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Origin {@linkplain Origin SDK} options.
 *
 * @since 1.2
 * @see Origin#initializeWithOptions(Context, OriginOptions)
 */
public final class OriginOptions {

	private static final class DynamicIfaClient implements IfaClient {

		private final String type;
		private final Function<Context, Pair<String, Boolean>> provider;

		DynamicIfaClient(String type, Function<Context, Pair<String, Boolean>> prov) {
			this.type = Preconditions.checkNotNull(type);
			this.provider = Preconditions.checkNotNull(prov);
		}

		@Override
		public String type() {
			return this.type;
		}

		@Override
		public Pair<String, Boolean>
		sendRequest(org.polygamma.android.origin.core.Origin sdk, Context ctxt) {
			return provider.apply(ctxt);
		}
	}

	@OriginCapability int capabilities;
	final List<IfaClient> deviceIdClients;

	/**
	 * Construct new default options.
	 *
	 * @since 1.2
	 */
	public OriginOptions() {
		this.deviceIdClients = new ArrayList<>();
		try {
			this.deviceIdClients.add(IfaClient.ofGms());
		} catch (UnsupportedOperationException ignored) {
		}
		try {
			this.deviceIdClients.add(IfaClient.ofHms());
		} catch (UnsupportedOperationException ignored) {
		}
		try {
			this.deviceIdClients.add(IfaClient.ofMsa());
		} catch (UnsupportedOperationException ignored) {
		}
	}

	/**
	 * Set capabilities to initialize SDK with.
	 *
	 * @param caps mask of capabilities to initialize with
	 * @return {@code this}
	 * @since 1.2
	 * @see Origin#CAPABILITY_ADS
	 * @see Origin#CAPABILITY_ANTIFRAUD
	 */
	@ReturnThis
	public OriginOptions capabilities(@OriginCapability int caps) {
		this.capabilities = caps;
		return this;
	}

	/**
	 * Add capability to initialize SDK with.
	 *
	 * @param caps capability to initialize
	 * @return {@code this}
	 * @since 1.2
	 * @see Origin#CAPABILITY_ADS
	 * @see Origin#CAPABILITY_ANTIFRAUD
	 */
	@ReturnThis
	public OriginOptions addCapability(@OriginCapability int caps) {
		this.capabilities |= caps;
		return this;
	}

	/**
	 * Clear device id provider list.
	 *
	 * @return {@code this}
	 * @since 1.2
	 */
	@ReturnThis
	public OriginOptions clearDeviceIdProviders() {
		this.deviceIdClients.clear();
		return this;
	}

	/**
	 * Add a dynamic device id provider.
	 * <p>The provider specified is invoked with the application context, and is expected to return
	 * a tuple of the device id and a flag indicating whether user has requested limited ad
	 * tracking. The provider function is <i>always</i> invoked on a worker thread, and may be
	 * invoked more than once.
	 *
	 * @param type device id type
	 * @param provider device id provider
	 * @return {@code this}
	 * @since 1.2
	 */
	@ReturnThis
	public OriginOptions
	addDynamicDeviceId(String type, Function<Context, Pair<String, Boolean>> provider) {
		this.deviceIdClients.add(0, new DynamicIfaClient(type, provider));
		return this;
	}

	/**
	 * Add a static device id.
	 * <p>Efficient equivalent of:
	 * {@snippet lang="java" :
	 * addDynamicDeviceId(type, (ctxt) -> new Pair<>(id, lmt)); // @link substring="addDynamicDeviceId" target="#addDynamicDeviceId(String, Function)"
	 * }
	 *
	 * @param type device id type
	 * @param id device id value
	 * @param lmt {@code true} if, and only if, user has requested limited ad tracking
	 * @return {@code this}
	 * @since 1.2
	 */
	@ReturnThis
	public OriginOptions addStaticDeviceId(String type, String id, boolean lmt) {
		this.deviceIdClients.add(0, IfaClient.ofStatic(type, id));
		return this;
	}

	/**
	 * Add static device id sourced from {@link android.provider.Settings.Secure#ANDROID_ID}.
	 *
	 * @param ctxt context to resolve id from
	 * @return {@code this}
	 * @since 1.2
	 */
	@ReturnThis
	public OriginOptions addAndroidDeviceId(Context ctxt) {
		this.addStaticDeviceId(
			"andid-raw",
			AndroidSettings.getSecureString(
				ctxt.getContentResolver(),
				Settings.Secure.ANDROID_ID,
				""
			),
			false
		);
		return this;
	}

	/**
	 * Add static device id sourced from {@link TelephonyManager}.
	 *
	 * @param ctxt context to resolve id with
	 * @return {@code this}
	 * @since 1.2
	 * @deprecated Telephony based identifiers must be manually generated.
	 */
	@Deprecated
	@ReturnThis
	@SuppressLint({ "HardwareIds", "MissingPermission" })
	@SuppressWarnings("deprecation")
	public OriginOptions addTelephonyDeviceId(Context ctxt) {
		return this;
	}
}
