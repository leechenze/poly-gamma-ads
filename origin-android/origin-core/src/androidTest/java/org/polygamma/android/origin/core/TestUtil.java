// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.core;

import android.app.UiAutomation;
import android.content.Context;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.util.Pair;

import androidx.annotation.RequiresApi;
import androidx.test.platform.app.InstrumentationRegistry;

import org.polygamma.android.origin.util.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Utility definitions for tests.
 */
public class TestUtil {

	private static final String TAG = TestUtil.class.getSimpleName();

	/**
	 * Test application context.
	 *
	 * @return context
	 */
	public static Context context() {
		return InstrumentationRegistry.getInstrumentation()
			.getTargetContext()
			.getApplicationContext();
	}

	/**
	 * Retrieve application package name.
	 *
	 * @return package name
	 */
	public static String packageName() {
		return context().getPackageName();
	}

	/**
	 * Execute command on main thread, awaiting completion.
	 *
	 * @param cmd command to execute
	 */
	public static void runOnMainSync(Runnable cmd) {
		InstrumentationRegistry.getInstrumentation()
			.runOnMainSync(cmd);
	}

	/**
	 * Execute command on main thread, awaiting completion, and returning result.
	 *
	 * @param <T> command value type
	 * @param cmd command to execute
	 * @return result
	 * @throws RuntimeException {@code cmd} failed
	 */
	public static <T> T callOnMainSync(Callable<T> cmd) {
		AtomicReference<Pair<T, Throwable>> xchg = new AtomicReference<>();

		runOnMainSync(() -> {
			Pair<T, Throwable> rv;

			try {
				rv = new Pair<>(cmd.call(), null);
			} catch (Throwable err) {
				rv = new Pair<>(null, err);
			}
			xchg.set(rv);
		});

		Pair<T, Throwable> rv = xchg.getAndSet(null);

		if (rv.second != null)
			throw new RuntimeException(rv.second);
		return rv.first;
	}

	/**
	 * Execute a shell command, if supported.
	 *
	 * @param cmd command to execute
	 */
	@RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
	public static void executeShellCommand(String cmd) {
		UiAutomation autom = InstrumentationRegistry.getInstrumentation()
			.getUiAutomation();

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
			autom.adoptShellPermissionIdentity();
		try {
			ParcelFileDescriptor pfd = autom.executeShellCommand(cmd);

			try (BufferedReader reader = new BufferedReader(new InputStreamReader(
				new ParcelFileDescriptor.AutoCloseInputStream(pfd)
			))) {
				while (true) {
					String ln = reader.readLine();

					if (ln == null)
						break;
					Logger.info(TAG, "shell command `%s` - %s", cmd, ln);
				}
				pfd.checkError();
			} catch (IOException cause) {
				Logger.warn(TAG, "shell command `%s` failed", cmd, cause);
			}
			// small delay here, sometimes commands don't take effect immediately for some reason..
			SystemClock.sleep(1000);
		} finally {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
				autom.dropShellPermissionIdentity();
		}
	}

	/**
	 * Enable or disable internet access.
	 *
	 * @param on {@code true} or {@code false} to enable or disable internet access, respectively
	 */
	@RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
	public static void toggleInternetAccess(boolean on) {
		if (on) {
			executeShellCommand("cmd connectivity airplane-mode disable");
			executeShellCommand("svc wifi enable");
			executeShellCommand("svc data enable");
		} else {
			executeShellCommand("svc wifi disable");
			executeShellCommand("svc data disable");
			executeShellCommand("cmd connectivity airplane-mode enable");
		}
	}

	private TestUtil() {
	}
}
