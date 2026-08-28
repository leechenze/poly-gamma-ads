// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.util;

import android.os.SystemClock;

import java.util.concurrent.TimeUnit;

/**
 * Utilities for accessing time.
 *
 * @since 1.2
 */
public class Time {

	private static final String TAG = Time.class.getSimpleName();

	/**
	 * Current time, in milliseconds since UNIX epoch, from highest quality time provider.
	 *
	 * @return current time, in milliseconds
	 * @since 1.2
	 */
	public static long nowUtcMillis() {
		return System.currentTimeMillis();
	}

	/**
	 * Current time, since UNIX epoch, from highest quality time provider.
	 * <p>This invokes {@link #nowUtcMillis()}, returning the resulting millisecond timestamp
	 * converted to {@code unit}.
	 *
	 * @param unit unit of measurement to return timestamp in
	 * @return current time, in {@code unit}
	 * @since 1.2
	 * @see #nowUtcMillis()
	 */
	public static long nowUtc(TimeUnit unit) {
		return unit.convert(nowUtcMillis(), TimeUnit.MILLISECONDS);
	}

	/**
	 * Current time, in seconds since UNIX epoch, from highest quality time provider.
	 * <p>Shorthand for:
	 * {@snippet lang="java" :
	 * nowUtc(TimeUnit.SECONDS); // @link substring="nowUtc" target="#nowUtc(TimeUnit)"
	 * }
	 *
	 * @return current time, in seconds
	 * @since 1.2
	 * @see #nowUtc(TimeUnit)
	 */
	@SuppressWarnings("JavadocDeclaration")
	public static long nowUtcSeconds() {
		return nowUtc(TimeUnit.SECONDS);
	}

	/**
	 * Current time, in seconds since boot.
	 *
	 * @return current time, in seconds
	 * @since 1.2
	 * @see SystemClock#uptimeMillis()
	 */
	public static long nowUptimeSeconds() {
		return TimeUnit.MILLISECONDS.toSeconds(SystemClock.uptimeMillis());
	}

	/**
	 * Current time, in seconds since boot, including time spent in sleep.
	 *
	 * @return current time, in seconds
	 * @since 1.2
	 * @see SystemClock#elapsedRealtime()
	 */
	public static long nowRealtimeSeconds() {
		return TimeUnit.MILLISECONDS.toSeconds(SystemClock.elapsedRealtime());
	}

	/**
	 * Calculate duration between two points in time.
	 *
	 * @param a first point
	 * @param b second point
	 * @return duration between {@code a} and {@code b}, or {@code 0} if {@code a} is after {@code
	 * b}
	 * @since 1.2
	 */
	public static long durationBetween(long a, long b) {
		return Long.compareUnsigned(a, b) < 0 ? b - a : 0;
	}

	private Time() {
	}
}
