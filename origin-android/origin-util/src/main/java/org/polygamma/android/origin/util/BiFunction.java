// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.util;

/**
 * Function which produces some result given two arguments.
 *
 * @param <T> first argument type
 * @param <U> second argument type
 * @param <R> result type
 * @since 1.2
 */
@FunctionalInterface
public interface BiFunction<T, U, R> {
	/**
	 * Apply function to arguments.
	 *
	 * @param first first argument to apply function to
	 * @param second second argument to apply function to
	 * @return result
	 * @since 1.2
	 */
	R apply(T first, U second);
}
