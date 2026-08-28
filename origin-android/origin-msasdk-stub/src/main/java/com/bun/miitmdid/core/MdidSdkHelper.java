// SPDX-License-Identifier: MIT OR Apache-2.0

package com.bun.miitmdid.core;

import android.content.Context;

import com.bun.miitmdid.interfaces.IIdentifierListener;

/**
 * MSA SDK helper root.
 */
public class MdidSdkHelper {

	private static native int blackbox();

	/**
	 * SDK version number.
	 */
	public static final int SDK_VERSION_CODE = blackbox();

	/**
	 * Set global initialization timeout.
	 *
	 * @param timeout timeout
	 * @return {@code true} if, and only if {@code timeout} is valid
	 */
	public static boolean setGlobalTimeout(long timeout) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Initialize certificate.
	 *
	 * @param ctxt context to initialize with
	 * @param cert certificate to initialize from
	 * @return {@code true} if, and only if, initialization was successful
	 */
	public static boolean InitCert(Context ctxt, String cert) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Initialize SDK.
	 *
	 * @param ctxt context to initialize with
	 * @param log {@code true} if, and only if, logging should be enabled
	 * @param listener listener to update with initialization status
	 * @return initilization status code
	 */
	public static int InitSdk(Context ctxt, boolean log, IIdentifierListener listener) {
		throw new UnsupportedOperationException();
	}

	private MdidSdkHelper() {
	}
}
