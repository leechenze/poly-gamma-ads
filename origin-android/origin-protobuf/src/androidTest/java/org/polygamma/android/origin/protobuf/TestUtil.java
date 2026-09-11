// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.protobuf;

import java.util.Random;

public class TestUtil {

	private static final String ALPHA_NUMERIC =
		"~!@#$%^&*()_+-={}|\\][;':\"?><,./ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

	public static String nextRandomString(Random rand, int len) {
		StringBuilder str = new StringBuilder(len);

		for (int i = 0; i < len; i++)
			str.append(ALPHA_NUMERIC.charAt(rand.nextInt(ALPHA_NUMERIC.length())));
		return str.toString();
	}
}
