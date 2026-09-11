// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.util;

/**
 * Value consumer.
 *
 * @param <T> value type
 * @since 1.2
 */
@FunctionalInterface
public interface Consumer<T> {
	/**
	 * Accept value.
	 *
	 * @param val value to accept
	 * @since 1.2
	 */
	void accept(T val);
}
