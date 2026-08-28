// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.polygamma.android.origin.util.AndroidContexts.*;

import android.Manifest;
import android.content.Context;
import android.os.Build;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;

/**
 * {@link AndroidContexts} tests.
 */
@RunWith(AndroidJUnit4.class)
// we require UiAutomation::grantRuntimePermission
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
public class AndroidContextsTest {

	@Test
	public void testHasPermission() {
		Context ctxt = Tests.context();

		for (String perm : new String[] {
			Manifest.permission.ACCESS_COARSE_LOCATION,
			Manifest.permission.ACCESS_FINE_LOCATION,
			Manifest.permission.READ_CONTACTS
		}) {
			assertFalse(hasPermission(ctxt, perm));

			Tests.grantPermission(perm);
			assertTrue(hasPermission(ctxt, perm));
		}
	}

	@Test
	public void testHasAllPermissions() {
		Context ctxt = Tests.context();
		String[] has = new String[] {
			Manifest.permission.ACCESS_COARSE_LOCATION,
			Manifest.permission.ACCESS_FINE_LOCATION,
			Manifest.permission.READ_CONTACTS
		};

		for (String perm : has)
			Tests.grantPermission(perm);

		for (int i = 0; i <= has.length; i++) {
			assertTrue(hasAllPermissions(ctxt, Arrays.copyOf(has, i)));

			String[] newHas = Arrays.copyOf(has, i + 1);

			newHas[newHas.length - 1] = Manifest.permission.WRITE_CONTACTS;
			assertFalse(hasAllPermissions(ctxt, newHas));
		}
	}

	@Test
	public void testHasAnyPermission() {
		Context ctxt = Tests.context();
		String[] has = new String[] {
			Manifest.permission.ACCESS_COARSE_LOCATION,
			Manifest.permission.ACCESS_FINE_LOCATION,
			Manifest.permission.READ_CONTACTS
		};

		for (String perm : has)
			Tests.grantPermission(perm);

		for (int i = 0; i <= has.length; i++) {
			assertTrue(hasAnyPermission(ctxt, Arrays.copyOf(has, i)));

			String[] newHas = Arrays.copyOf(has, i + 1);

			newHas[newHas.length - 1] = Manifest.permission.WRITE_CONTACTS;
			if (i == 0)
				assertFalse(hasAnyPermission(ctxt, newHas));
			else
				assertTrue(hasAnyPermission(ctxt, newHas));
		}
	}
}
