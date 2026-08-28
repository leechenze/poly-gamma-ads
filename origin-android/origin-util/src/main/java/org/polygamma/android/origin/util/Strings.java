// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.util;

import android.text.TextUtils;

import androidx.annotation.Nullable;

import java.util.Iterator;

/**
 * Utility operations operating on {@linkplain String string} values.
 *
 * @since 0.2
 */
public class Strings {

	/**
	 * Return string or {@linkplain String#isEmpty() empty} string if string is {@code null}.
	 *
	 * @param str string
	 * @return {@code str} or empty string if {@code str} is {@code null}
	 * @since 0.2
	 */
	public static String nullToEmpty(@Nullable String str) {
		return Preconditions.checkNotNullElse(str, "");
	}

	/**
	 * Return string or {@code null} if string is {@linkplain String#isEmpty() empty}.
	 *
	 * @param str string
	 * @return {@code str} or, {@code null} if {@code str} is {@code null} or empty
	 * @since 0.2
	 */
	public static @Nullable String emptyToNull(@Nullable String str) {
		return nullToEmpty(str).isEmpty() ? null : str;
	}

	/**
	 * Split string on {@code char} delimiter.
	 * <p>This has less overhead than {@link String#split(String)}, which requires regular
	 * expressions and performs an array allocation.
	 *
	 * @param str string to split
	 * @param delim delimiter to split on
	 * @return iterator over parts of {@code str} split on {@code delim}
	 * @since 0.2
	 */
	public static Iterator<String> split(String str, char delim) {
		TextUtils.StringSplitter splitter = new TextUtils.SimpleStringSplitter(delim);

		splitter.setString(str);
		return splitter.iterator();
	}

	private Strings() {
	}
}
