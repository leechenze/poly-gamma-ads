// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.util;

import android.app.Instrumentation;
import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

/**
 * Test utility definitions.
 */
public class Tests {

	/**
	 * Test application context.
	 *
	 * @return application context
	 */
	public static Context context() {
		return InstrumentationRegistry.getInstrumentation().getTargetContext();
	}

	/**
	 * Grant permission.
	 *
	 * @param perm permission to grant
	 */
	public static void grantPermission(String perm) {
		Instrumentation instr = InstrumentationRegistry.getInstrumentation();

		instr.getUiAutomation()
			.grantRuntimePermission(instr.getTargetContext().getPackageName(), perm);
	}

	private Tests() {
	}
}
