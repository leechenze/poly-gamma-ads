// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.util;

/**
 * Function which produces some result given an {@code int} argument.
 *
 * @param <R> result type
 * @since 1.2
 */
@FunctionalInterface
public interface IntFunction<R> {
	/**
	 * Apply function to argument.
	 *
	 * @param arg argument to apply function to
	 * @return result
	 * @since 1.2
	 */
	R apply(int arg);
}
