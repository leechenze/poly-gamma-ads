// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.core;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Coding utilities for mobile country and network codes.
 */
class MccMnc {

	private static final Pattern PATTERN =
		Pattern.compile("^(\\d{3})[^\\d]*(\\d{2,3})?.*$");

	/**
	 * Parse MCC and MNC pair from string.
	 * <p>This returns a tuple of the MCC and MNC integers parsed from {@code}, respectively. If
	 * either the MCC or MNC part of {@code code} is malformed, its respective parse result is
	 * {@code -1}.
	 *
	 * @param code string to decode from
	 * @return tuple of MCC and MNC, respectively
	 */
	static int[] parse(String code) {
		Matcher matcher = PATTERN.matcher(code);
		int[] rv = new int[] { -1, -1 };

		if (!matcher.matches())
			return rv;

		String mcc = matcher.group(1);
		String mnc = matcher.group(2);

		if (mcc == null)
			return rv;

		try {
			rv[0] = Integer.parseInt(mcc, 10);
			if (mnc != null)
				rv[1] = Integer.parseInt(mnc, 10);
		} catch (NumberFormatException ignored) {
		}
		return rv;
	}

	/**
	 * Serialize MCC and MNC to a string.
	 * <p>If {@code mcc} or {@code mnc} is {@code -1}, this returns an empty string or {@code mcc}
	 * serialized, respectively; otherwise, this returns a full MCC and MNC string.
	 *
	 * @param mcc mobile country code or {@code -1}
	 * @param mnc mobile network code or {@code -1}
	 * @return resulting string
	 */
	static String serialize(int mcc, int mnc) {
		return (
			mcc == -1 ? "" :
			mnc == -1 ? String.format(Locale.ROOT, "%03d", mcc) :
			String.format(Locale.ROOT, "%03d%03d", mcc, mnc)
		);
	}

	private MccMnc() {
	}
}
