// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.util;

/**
 * Operation accepting two input arguments, returning no result.
 *
 * @param <T> first argument type
 * @param <U> second argument type
 * @since 1.2
 */
@FunctionalInterface
public interface BiConsumer<T, U> {
	/**
	 * Apply operation on arguments.
	 *
	 * @param a first argument to apply operation on
	 * @param b second argument to apply operation on
	 * @since 1.2
	 */
	void accept(T a, U b);
}
