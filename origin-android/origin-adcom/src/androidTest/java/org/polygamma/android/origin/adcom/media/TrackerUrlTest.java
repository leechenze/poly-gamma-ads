// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.adcom.media;

import static org.junit.Assert.assertEquals;

import android.util.SparseArray;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.polygamma.android.origin.adcom.enums.AdComEnums;

import java.util.Locale;

/**
 * {@link TrackerUrl} tests.
 */
@RunWith(AndroidJUnit4.class)
public class TrackerUrlTest {
	@Test
	public void testSubstituteMacros() {
		assertEquals(
			"https://foo.com/",
			TrackerUrl.substituteMacros("https://foo.com/", new SparseArray<>(0))
		);

		assertEquals(
			"https://foo.com/bar?\000123",
			TrackerUrl.substituteMacros("https://foo.com/bar?\000123", new SparseArray<>(0))
		);

		String base = String.format(
			Locale.ROOT,
			"https://foo.com/bar?tid=\0%s\0\0%s\0&lmt=\0%s\0\0%s\0&baz=1",
			AdComEnums.AdTrackerUrlMacroTransactionId,
			AdComEnums.AdTrackerUrlMacroAppBundle,
			AdComEnums.AdTrackerUrlMacroLmt,
			AdComEnums.AdTrackerUrlMacroAdAssetUrl
		);
		String exp = "https://foo.com/bar?tid=123com.foo.bar&lmt=false&baz=1";
		SparseArray<String> macros = new SparseArray<>();

		macros.put(AdComEnums.AdTrackerUrlMacroTransactionId, "123");
		macros.put(AdComEnums.AdTrackerUrlMacroAppBundle, "com.foo.bar");
		macros.put(AdComEnums.AdTrackerUrlMacroLmt, "false");
		assertEquals(exp, TrackerUrl.substituteMacros(base, macros));
	}
}
