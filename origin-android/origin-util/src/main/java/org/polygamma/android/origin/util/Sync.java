// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.util;

import android.annotation.SuppressLint;
import android.app.Application;
import android.os.Build;
import android.os.Looper;

import java.lang.reflect.Method;

/**
 * Synchronization utility definitions.
 *
 * @since 0.1
 */
public class Sync {

	private static final String TAG = Sync.class.getSimpleName();

	/**
	 * Test whether {@linkplain Thread#currentThread() current} thread is main (UI) thread.
	 *
	 * @return {@code true} if, and only if, current thread is main thread
	 * @since 0.1
	 */
	public static boolean isMainThread() {
		return Thread.currentThread().equals(Looper.getMainLooper().getThread());
	}

	/**
	 * Ensure {@linkplain Thread#currentThread() current} thread is main (UI) thread.
	 *
	 * @throws IllegalStateException current thread is not main thread
	 * @since 0.1
	 * @see #isMainThread()
	 */
	public static void checkMainThread() {
		Preconditions.checkState(isMainThread(), "executing on worker thread");
	}

	/**
	 * Ensure {@linkplain Thread#currentThread() current} thread is not main (UI) thread.
	 *
	 * @throws IllegalStateException current thread is main thread
	 * @since 0.1
	 * @see #isMainThread()
	 */
	public static void checkWorkerThread() {
		Preconditions.checkState(!isMainThread(), "executing on main thread");
	}

	/**
	 * Determine name of current executing process.
	 *
	 * @return process name or {@linkplain String#isEmpty() empty} if unknown
	 * @since 0.1
	 */
	public static String currentProcessName() {
		try {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
				return Strings.nullToEmpty(Application.getProcessName());

			@SuppressLint("PrivateApi")
			Class<?> ActivityThread = Class.forName("android.app.ActivityThread");
			@SuppressLint("DiscouragedPrivateApi")
			Method currentProcessName = ActivityThread.getDeclaredMethod("currentProcessName");

			return Strings.nullToEmpty((String) currentProcessName.invoke(null));
		} catch (Throwable err) {
			Logger.warn(TAG, "failed to query current process name", err);
			return "";
		}
	}

	/**
	 * Determine name of current executing process, excluding package name.
	 *
	 * @return process name or {@linkplain String#isEmpty() empty} if main process
	 * @since 1.2
	 */
	public static String currentProcessSimpleName() {
		String name = currentProcessName();
		int idx = name.indexOf(':');

		return idx != -1 ? name.substring(idx + 1) : "";
	}

	private Sync() {
	}
}
