// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.util;

/**
 * Result supplier.
 *
 * @param <T> result type
 * @since 1.2
 */
@FunctionalInterface
public interface Supplier<T> {
	/**
	 * Supply result.
	 *
	 * @return result
	 * @since 1.2
	 */
	T get();
}
