// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.util;

import android.util.Log;

import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.Locale;

/**
 * Internal {@link Logger} definitions.
 */
final class LoggerInternal {

	/**
	 * Current logger instance.
	 */
	public static @Nullable Logger instance;

	/**
	 * Log message at level.
	 * <p>If {@linkplain #instance current} logger is non-{@code null}, log message is forwarded to
	 * it; otherwise, {@link Log} is used.
	 *
	 * @param level message log level
	 * @param tag message tag
	 * @param fmt message format string
	 * @param args message format arguments
	 */
	public static void log(@Logger.LogLevel int level, String tag, String fmt, Object... args) {
		if (instance != null) {
			instance.log(level, tag, fmt, args);
			return;
		}

		String msg;
		@Nullable Throwable err;

		if (args.length > 0 && args[args.length - 1] instanceof Throwable) {
			msg = String.format(Locale.ROOT, fmt, Arrays.copyOfRange(args, 0, args.length - 1));
			err = (Throwable) args[args.length - 1];
		} else {
			msg = String.format(Locale.ROOT, fmt, args);
			err = null;
		}

		switch (level) {
		case Logger.DEBUG:
			Log.d(tag, msg, err);
			break;
		case Logger.INFO:
			Log.i(tag, msg, err);
			break;
		case Logger.WARN:
			Log.w(tag, msg, err);
			break;
		case Logger.ERROR:
			Log.e(tag, msg, err);
			break;
		}
	}

	private LoggerInternal() {
	}
}
