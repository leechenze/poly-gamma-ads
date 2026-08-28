// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.adcom.media;

import android.util.SparseArray;

/**
 * Tracker URL utility definitions.
 */
class TrackerUrl {

	private static final char NUL	= '\0';

	/**
	 * Substitute macros embedded within a tracker URL.
	 *
	 * @param url URL to substitute macros of
	 * @param macros mapping of macro type to substitution value
	 * @return resulting URL
	 */
	static String substituteMacros(String url, SparseArray<String> macros) {
		StringBuilder rv = new StringBuilder(url.length());
		int fromPos = 0;

		while (true) {
			int startPos = url.indexOf(NUL, fromPos);

			if (startPos == -1)
				return rv.length() == 0 ? url : rv.append(url.substring(fromPos)).toString();

			int endPos = url.indexOf(NUL, startPos + 1);

			if (endPos == -1) {
				endPos = startPos + 1;
				rv.append(url.substring(fromPos, endPos));
				fromPos = endPos;
				continue;
			}

			int type;

			try {
				type = Integer.parseInt(url.substring(startPos + 1, endPos), 10);
			} catch (RuntimeException ignored) {
				endPos = startPos + 1;
				rv.append(url.substring(fromPos, endPos));
				fromPos = endPos;
				continue;
			}

			rv.append(url.substring(fromPos, startPos))
				.append(macros.get(type, ""));
			fromPos = endPos + 1;
		}
	}

	private TrackerUrl() {
	}
}
