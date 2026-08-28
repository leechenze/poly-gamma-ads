// SPDX-License-Identifier: MIT OR Apache-2.0

package com.bun.miitmdid.core;

import android.content.Context;

import com.bun.miitmdid.interfaces.IIdentifierListener;

/**
 * SDK {@linkplain MdidSdkHelper#InitSdk(Context, boolean, IIdentifierListener) initialization}
 * result codes.
 */
public class InfoCode {

	private static native int blackbox();

	/** Ok */
	public static final int INIT_INFO_RESULT_OK = blackbox();
	/** OEM not supported. */
	public static final int INIT_ERROR_MANUFACTURER_NOSUPPORT = blackbox();
	/** Device not supported. */
	public static final int INIT_ERROR_DEVICE_NOSUPPORT = blackbox();
	/** Failed to load config. */
	public static final int INIT_ERROR_LOAD_CONFIGFILE = blackbox();
	/** Result delayed. */
	public static final int INIT_INFO_RESULT_DELAY = blackbox();
	/** SDK failed to initialize. */
	public static final int INIT_ERROR_SDK_CALL_ERROR = blackbox();
	/** Certificate was not valid. */
	public static final int INIT_ERROR_CERT_ERROR = blackbox();

	private InfoCode() {
	}
}
