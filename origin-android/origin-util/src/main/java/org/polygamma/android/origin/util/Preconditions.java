// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.util;

import android.os.Build;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Supplier;

import java.util.Locale;
import java.util.Objects;

/**
 * Utility methods for ensuring preconditions.
 *
 * @since 0.1
 */
public class Preconditions {

	private static void throwIllegalArgument(String msg) {
		throw new IllegalArgumentException(msg);
	}

	/**
	 * Ensure an argument is valid, failing with {@linkplain String#format(String, Object...)
	 * formatted} message if invalid.
	 *
	 * @param ok {@code true} if, and only if, argument is valid
	 * @param msg format of message to fail with on invalid argument
	 * @param args message format arguments
	 * @throws IllegalArgumentException {@code ok} is {@code false}
	 * @since 0.1
	 */
	public static void checkArgument(boolean ok, String msg, Object... args) {
		if (!ok)
			throwIllegalArgument(String.format(Locale.ROOT, msg, args));
	}

	/**
	 * Ensure an argument is valid, failing with message if invalid.
	 *
	 * @param ok {@code true} if, and only if, argument is valid
	 * @param msg message to fail with on invalid argument
	 * @throws IllegalArgumentException {@code ok} is {@code false}
	 * @since 0.1
	 */
	public static void checkArgument(boolean ok, String msg) {
		if (!ok)
			throwIllegalArgument(msg);
	}

	/**
	 * Ensure an argument is valid, failing if invalid.
	 *
	 * @param ok {@code true} if, and only if, argument is valid
	 * @throws IllegalArgumentException {@code ok} is {@code false}
	 * @since 0.1
	 */
	public static void checkArgument(boolean ok) {
		checkArgument(ok, "illegal argument");
	}

	private static void throwIllegalState(String msg) {
		throw new IllegalStateException(msg);
	}

	/**
	 * Ensure state is valid, failing with {@linkplain String#format(String, Object...) formatted}
	 * message if invalid.
	 *
	 * @param ok {@code true} if, and only if, state is valid
	 * @param msg format of message to fail with on invalid state
	 * @param args message format arguments
	 * @throws IllegalStateException {@code ok} is {@code false}
	 * @since 0.1
	 */
	public static void checkState(boolean ok, String msg, Object... args) {
		if (!ok)
			throwIllegalState(String.format(Locale.ROOT, msg, args));
	}

	/**
	 * Ensure state is valid, failing with message if invalid.
	 *
	 * @param ok {@code true} if, and only if, state is valid
	 * @param msg message to fail with on invalid state
	 * @throws IllegalStateException {@code ok} is {@code false}
	 * @since 0.1
	 */
	public static void checkState(boolean ok, String msg) {
		if (!ok)
			throwIllegalState(msg);
	}

	/**
	 * Ensure state is valid, failing if invalid.
	 *
	 * @param ok {@code true} if, and only if, state is valid
	 * @throws IllegalStateException {@code ok} is {@code false}
	 * @since 0.1
	 */
	public static void checkState(boolean ok) {
		checkState(ok, "illegal state");
	}

	private static void throwNullPointer(String msg) {
		throw new NullPointerException(msg);
	}

	/**
	 * Ensure value is non-{@code null}, failing with formatted message otherwise.
	 *
	 * @param <T> value type
	 * @param val value to ensure
	 * @param fmt failure message format
	 * @param args failure message format arguments
	 * @return {@code val}
	 * @throws NullPointerException {@code val} is {@code null}
	 * @since 0.1
	 */
	public static <T> @NonNull T checkNotNull(@Nullable T val, String fmt, Object...args) {
		if (val == null)
			throwNullPointer(String.format(Locale.ROOT, fmt, args));
		return val;
	}

	/**
	 * Ensure value is non-{@code null}, failing with message otherwise.
	 *
	 * @param <T> value type
	 * @param val value to ensure
	 * @param msg failure message
	 * @return {@code val}
	 * @throws NullPointerException {@code val} is {@code null}
	 * @since 0.1
	 */
	public static <T> @NonNull T checkNotNull(@Nullable T val, String msg) {
		return Objects.requireNonNull(val, msg);
	}

	/**
	 * Ensure value is non-{@code null}, failing otherwise.
	 *
	 * @param <T> value type
	 * @param val value to ensure
	 * @return {@code val}
	 * @throws NullPointerException {@code val} is {@code null}
	 * @since 0.1
	 */
	public static <T> @NonNull T checkNotNull(@NonNull T val) {
		return Objects.requireNonNull(val);
	}

	/**
	 * Ensure array and its elements are non-{@code null}, failing otherwise.
	 *
	 * @param <T> value type
	 * @param vals array of values to ensure
	 * @return {@code vals}
	 * @throws NullPointerException {@code vals} or, one or more of its elements are {@code null}
	 * @since 0.3
	 */
	public static <T> T[] checkNotNullArray(@NonNull T[] vals) {
		for (T val : checkNotNull(vals)) {
			//noinspection ResultOfMethodCallIgnored
			checkNotNull(val);
		}
		return vals;
	}

	/**
	 * Ensure pair and its elements are non-{@code null}, failing otherwise.
	 *
	 * @param <A> first pair element type
	 * @param <B> second pair element type
	 * @param pair pair to test
	 * @return {@code pair}
	 * @throws NullPointerException {@code pair} or, one or more of its elements is {@code null}
	 * @since 1.2
	 */
	@SuppressWarnings("ResultOfMethodCallIgnored")
	public static <A, B> Pair<A, B> checkNotNullPair(@NonNull Pair<A, B> pair) {
		checkNotNull(pair.first);
		checkNotNull(pair.second);
		return pair;
	}

	/**
	 * Ensure value is non-{@code null}, returning default value otherwise.
	 *
	 * @param <T> value type
	 * @param a value to ensure
	 * @param b default value to return
	 * @return {@code a} if, and only if, {@code a} is non-{@code null}; otherwise {@code b}
	 * @throws NullPointerException both {@code a} and {@code b} are {@code null}
	 * @since 0.1
	 */
	public static <T> @NonNull T checkNotNullElse(@Nullable T a, @NonNull T b) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
			return Objects.requireNonNullElse(a, b);
		return a == null ? Objects.requireNonNull(b) : a;
	}

	/**
	 * Ensure value is non-{@code null}, returning default value, from supplier, otherwise.
	 *
	 * @param <T> value type
	 * @param a value to ensure
	 * @param b supplier to retrieve default value to return
	 * @return {@code a} if, and only if, {@code a} is non-{@code null}; otherwise value returned
	 * from {@code b}
	 * @throws NullPointerException both {@code a} and value returned from {@code b} are {@code
	 * null}
	 * @since 0.1
	 */
	public static <T> @NonNull T checkNotNullElseGet(@Nullable T a, Supplier<T> b) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
			return Objects.requireNonNullElseGet(a, b::get);
		return a == null ? Objects.requireNonNull(b.get()) : a;
	}

	private static void throwIndexOutOfBounds() {
		throw new IndexOutOfBoundsException();
	}

	/**
	 * Ensure index falls within bounds, failing otherwise.
	 *
	 * @param index index to ensure
	 * @param length maximum length
	 * @return {@code index}
	 * @throws IndexOutOfBoundsException {@code length} or {@code index} is negative, or, {@code
	 * index} is greater than or equal to {@code length}
	 * @since 0.1
	 */
	public static int checkIndex(int index, int length) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
			return Objects.checkIndex(index, length);
		if (index < 0 || index >= length)
			throwIndexOutOfBounds();
		return index;
	}

	/**
	 * Ensure subsequence range falls within bounds, failing otherwise.
	 *
	 * @param fromIndex index of subsequence to ensure
	 * @param size length of subsequence to ensure
	 * @param length maximum length
	 * @return {@code fromIndex}
	 * @throws IndexOutOfBoundsException {@code length}, {@code size}, or {@code fromIndex} is
	 * negative, or, {@code fromIndex + size} is greater than or equal to {@code length}
	 * @since 0.1
	 */
	public static int checkFromIndexSize(int fromIndex, int size, int length) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
			return Objects.checkFromIndexSize(fromIndex, size, length);
		if ((fromIndex | size | length) < 0 || Math.addExact(fromIndex, size) > length)
			throwIndexOutOfBounds();
		return fromIndex;
	}

	/**
	 * Ensure fixed subsequence range falls within bounds, failing otherwise.
	 *
	 * @param fromIndex start index of subsequence to ensure (inclusive)
	 * @param toIndex end index of subsequence to ensure (exclusive)
	 * @param length maximum length
	 * @return {@code fromIndex}
	 * @throws IndexOutOfBoundsException {@code length} or {@code fromIndex} is negative,
	 * {@code fromIndex} is greater than {@code toIndex}, or {@code toIndex} is greater than
	 * {@code length}
	 * @since 0.1
	 */
	public static int checkFromToIndex(int fromIndex, int toIndex, int length) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
			return Objects.checkFromToIndex(fromIndex, toIndex, length);
		if ((fromIndex | length) < 0 || fromIndex > toIndex || toIndex > length)
			throwIndexOutOfBounds();
		return fromIndex;
	}

	private Preconditions() {
	}
}
