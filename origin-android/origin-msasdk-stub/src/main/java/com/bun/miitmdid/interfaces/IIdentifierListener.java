// SPDX-License-Identifier: MIT OR Apache-2.0

package com.bun.miitmdid.interfaces;

/**
 * Listener {@linkplain #onSupport(IdSupplier) invoked} when {@linkplain
 * com.bun.miitmdid.core.MdidSdkHelper SDK} initializes successfully.
 */
public interface IIdentifierListener {
	/**
	 * Invoked when Open Advertising id is supported.
	 *
	 * @param supplier id supplier
	 */
	void onSupport(IdSupplier supplier);

	/**
	 * Invoked when Open Advertising id is supported.
	 *
	 * @param exists {@code true} if, and only if, id is supported
	 * @param supplier id supplier
	 */
	void OnSupport(boolean exists, IdSupplier supplier);
}
