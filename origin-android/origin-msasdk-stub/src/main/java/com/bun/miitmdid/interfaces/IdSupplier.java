// SPDX-License-Identifier: MIT OR Apache-2.0

package com.bun.miitmdid.interfaces;

/**
 * Open Advertising Id supplier.
 */
public interface IdSupplier {

	/**
	 * Test whether id is supported.
	 *
	 * @return {@code true} if, and only if, id is supported
	 */
	boolean isSupported();

	/**
	 * Test whether ad tracking should be limited.
	 *
	 * @return {@code true} if, and only if, tracking should be limited
	 */
	boolean isLimited();

	/**
	 * Get OAID.
	 *
	 * @return id
	 */
	String getOAID();

	/**
	 * Get VAID.
	 *
	 * @return id
	 */
	String getVAID();

	/**
	 * Get AAID.
	 *
	 * @return id
	 */
	String getAAID();
}
