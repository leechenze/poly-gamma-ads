// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.core;

import android.content.Context;
import android.os.Build;
import android.util.Pair;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import org.polygamma.android.origin.util.Preconditions;
import org.polygamma.android.origin.util.Strings;

/**
 * Advertising identifier (IFA) client.
 *
 * @since 0.1
 */
public interface IfaClient {

	/**
	 * Construct a new client which returns a Google Mobile Services (GMS) device identifier.
	 *
	 * @return resulting client
	 * @throws UnsupportedOperationException the GMS SDK is not available
	 * @since 0.1
	 */
	@SuppressWarnings("deprecation")
	static IfaClient ofGms() {
		try {
			Class.forName("com.google.android.gms.common.GoogleApiAvailabilityLight");
			Class.forName("com.google.android.gms.ads.identifier.AdvertisingIdClient");

			return (IfaClient) Class.forName("org.polygamma.android.origin.core.GmsIfaClient")
				.newInstance();
		} catch (Throwable e) {
			throw new UnsupportedOperationException("GMS not available", e);
		}
	}

	/**
	 * Construct a new client which returns a Huawei Mobile Services (HMS) device identifier.
	 *
	 * @return resulting client
	 * @throws UnsupportedOperationException the HMS is not available
	 * @since 1.2
	 */
	@SuppressWarnings("deprecation")
	static IfaClient ofHms() {
		try {
			//noinspection DataFlowIssue
			Preconditions.checkState(Build.BRAND.equalsIgnoreCase("huawei") && (
				(Integer) Class.forName("com.huawei.android.os.BuildEx$VERSION")
					.getDeclaredField("EMUI_SDK_INT")
					.get(null)
			) > 0);

			Class.forName("com.huawei.hms.ads.identifier.AdvertisingIdClient");

			return (IfaClient) Class.forName("org.polygamma.android.origin.core.HmsIfaClient")
				.newInstance();
		} catch (Throwable e) {
			throw new UnsupportedOperationException("HMS not available", e);
		}
	}

	/**
	 * Construct a new client which returns an Mobile Security Alliance (MSA) device identifier.
	 *
	 * @return resulting client
	 * @throws UnsupportedOperationException the MSA SDK is not available
	 * @since 0.1
	 */
	@SuppressWarnings("deprecation")
	static IfaClient ofMsa() {
		try {
			Class.forName("com.bun.miitmdid.core.MdidSdkHelper");
			Class.forName("com.bun.miitmdid.interfaces.IIdentifierListener");

			return (IfaClient) Class.forName("org.polygamma.android.origin.core.MsaIfaClient")
				.newInstance();
		} catch (Throwable e) {
			throw new UnsupportedOperationException("MSA not available", e);
		}
	}

	/**
	 * Construct a new client which always returns a static identifier.
	 *
	 * @param type identifier type or {@code null} if unknown
	 * @param id identifier or, {@code null} or {@linkplain String#isEmpty() empty} string if
	 * unavailable
	 * @return resulting client
	 * @since 0.1
	 */
	static IfaClient ofStatic(@Nullable String type, @Nullable String id) {
		return new IfaClient() {
			@Override
			public String type() {
				return Strings.nullToEmpty(type);
			}

			@Override
			public Pair<String, Boolean> sendRequest(Origin _sdk, Context _ctxt) {
				return new Pair<>(Strings.nullToEmpty(id), Boolean.FALSE);
			}
		};
	}

	/**
	 * Construct a new default client.
	 *
	 * @return {@linkplain #ofGms() GMS}, {@linkplain #ofHms() HMS}, {@linkplain #ofMsa() MSA}, or
	 * stub client based on availability
	 * @since 0.1
	 */
	static IfaClient of() {
		try {
			return ofGms();
		} catch (Exception ignored) {
		}
		try {
			return ofHms();
		} catch (Exception ignored) {
		}
		try {
			return ofMsa();
		} catch (Exception ignored) {
		}
		return ofStatic("", "");
	}

	/**
	 * Identifier type produced by client.
	 *
	 * @return identifier type, such as {@code gaid} or {@code oaid} for Google Play Ad Id or
	 * Open Anonymous Device Identifier, respectively.
	 * @since 0.1
	 */
	String type();

	/**
	 * Send IDFA request.
	 *
	 * @param sdk SDK requesting identifier
	 * @param ctxt application context with which to send request
	 * @return tuple of identifier and, {@code true} or {@code false} if limited ad tracking has
	 * been request by the user; otherwise, tuple of empty string and {@code false}
	 * @throws Exception an error was encountered
	 * @since 0.1
	 */
	@WorkerThread
	Pair<String, Boolean> sendRequest(Origin sdk, Context ctxt) throws Exception;
}
