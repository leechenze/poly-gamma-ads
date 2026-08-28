// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.polygamma.android.origin.ads.AdsModule;
import org.polygamma.android.origin.antifraud.AntifraudModule;
import org.polygamma.android.origin.core.ConnectivityModule;
import org.polygamma.android.origin.core.DeviceModule;
import org.polygamma.android.origin.core.HttpModule;
import org.polygamma.android.origin.core.IfaClient;
import org.polygamma.android.origin.core.LocationModule;
import org.polygamma.android.origin.core.RegulationsModule;
import org.polygamma.android.origin.core.RpcModule;
import org.polygamma.android.origin.util.Preconditions;

/**
 * Origin software development kit (SDK) entry-point.
 *
 * @since 1.2
 */
public class Origin {

	/**
	 * {@linkplain AdsModule Ads} capability.
	 *
	 * @since 1.2
	 */
	public static final int CAPABILITY_ADS			= 0x01;

	/**
	 * {@linkplain AntifraudModule Antifraud} capability.
	 *
	 * @since 1.2
	 */
	public static final int CAPABILITY_ANTIFRAUD	= 0x02;

	private static @Nullable AdsModule ads;
	private static @Nullable AntifraudModule antifraud;

	/**
	 * Initialize SDK with {@linkplain OriginOptions options}, if not already initialized.
	 * <p>If another SDK instance has already been initialized, it is used; otherwise, a new SDK
	 * instance is initialized. New capabilities are loaded based on the capabilities specified in
	 * {@code opts}.
	 *
	 * @param ctxt context to initialize with
	 * @param opts options to initialize with
	 * @throws IllegalArgumentException no {@linkplain OriginOptions#addCapability(int) capability}
	 * specified in {@code opts}
	 * @since 1.2
	 */
	public static void initializeWithOptions(Context ctxt, OriginOptions opts) {
		Preconditions.checkArgument(
			(opts.capabilities & (CAPABILITY_ADS | CAPABILITY_ANTIFRAUD)) != 0,
			"at least one capability must be specified"
		);

		DeviceModule.Provider dev = DeviceModule.ofProvider();

		if ("cn".equals(BuildConfig.ORIGIN_SDK_REGION))
			opts.addAndroidDeviceId(ctxt);
		if (!opts.deviceIdClients.isEmpty())
			dev.ifaClients(opts.deviceIdClients.toArray(new IfaClient[0]));

		org.polygamma.android.origin.core.Origin sdk =
			org.polygamma.android.origin.core.Origin.initialize(
				ctxt,
				// XXX: ORDER MATTERS HERE! `ofProvider()` is renamed!
				dev,
				ConnectivityModule.ofProvider(),
				LocationModule.ofProvider(),
				RegulationsModule.ofProvider(),
				HttpModule.ofProvider(),
				RpcModule.ofProvider()
			);

		if ((opts.capabilities & (CAPABILITY_ADS | CAPABILITY_ANTIFRAUD)) != 0)
			antifraud = sdk.loadModule(AntifraudModule.ofProvider());
		if ((opts.capabilities & CAPABILITY_ADS) != 0)
			ads = sdk.loadModule(AdsModule.ofProvider());
	}

	/**
	 * Initialize SDK with capabilities, if not already initialized.
	 * <p>Shorthand for:
	 * {@snippet lang="java" :
	 * initializeWithOptions(ctxt, (new OriginOptions()).capabilities(caps)); // @link substring="initializeWithOptions" target="#initializeWithOptions(Context, OriginOptions)"
	 * }
	 *
	 * @param ctxt context to initialize with
	 * @param caps capabilities to enable
	 * @throws IllegalArgumentException {@code caps} has no valid capability set
	 * @since 1.2
	 * @see #initializeWithOptions(Context, OriginOptions)
	 */
	public static void initialize(Context ctxt, @OriginCapability int caps) {
		initializeWithOptions(ctxt, (new OriginOptions()).capabilities(caps));
	}

	/**
	 * Initialize SDK with all capabilities, if not already initialized.
	 * <p>Shorthand for:
	 * {@snippet lang="java" :
	 * initialize(ctxt, CAPABILITY_ADS | CAPABILITY_ANTIFRAUD); // @link substring="initialize" target="#initialize(Context, int)"
	 * }
	 *
	 * @param ctxt context to initialize with
	 * @since 1.2
	 * @see #initialize(Context, int)
	 */
	public static void initialize(Context ctxt) {
		initialize(ctxt, CAPABILITY_ADS | CAPABILITY_ANTIFRAUD);
	}

	/**
	 * Ads capability module.
	 *
	 * @return ads module
	 * @throws IllegalStateException ads {@linkplain #CAPABILITY_ADS capability} not enabled
	 * @since 1.2
	 * @see #CAPABILITY_ADS
	 */
	public static @NonNull AdsModule ads() {
		Preconditions.checkState(ads != null, "ads capability not enabled");
		//noinspection DataFlowIssues
		return ads;
	}

	/**
	 * Antifraud capability module.
	 *
	 * @return antifraud module
	 * @throws IllegalStateException antifraud {@linkplain #CAPABILITY_ANTIFRAUD capability} not
	 * enabled
	 * @since 1.2
	 * @see #CAPABILITY_ANTIFRAUD
	 */
	public static @NonNull AntifraudModule antifraud() {
		Preconditions.checkState(antifraud != null, "antifraud capability not enabled");
		//noinspection DataFlowIssues
		return antifraud;
	}

	private Origin() {
	}
}
