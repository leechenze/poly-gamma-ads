// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.util;

import androidx.annotation.IntDef;
import androidx.annotation.Nullable;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Message logger.
 *
 * @since 0.1
 */
@FunctionalInterface
public interface Logger {

	/**
	 * Log message level enumeration value marker.
	 *
	 * @since 0.1
	 */
	@Documented
	@Retention(RetentionPolicy.SOURCE)
	@Target({ElementType.FIELD, ElementType.LOCAL_VARIABLE, ElementType.PARAMETER})
	@IntDef({
		DEBUG,
		INFO,
		WARN,
		ERROR
	})
	@interface LogLevel {
	}

	/**
	 * Debug log message.
	 *
	 * @since 0.1
	 */
	@LogLevel
	int DEBUG	= 0;

	/**
	 * Informational log message.
	 *
	 * @since 0.1
	 */
	@LogLevel
	int INFO	= 1;

	/**
	 * Warning log message.
	 *
	 * @since 0.1
	 */
	@LogLevel
	int WARN	= 2;

	/**
	 * Un-recoverable error log message.
	 *
	 * @since 0.1
	 */
	@LogLevel
	int ERROR	= 3;

	/**
	 * Set logger to use for logging messages.
	 *
	 * @param instance logger or {@code null} to use default {@link android.util.Log} based logger
	 * @since 0.1
	 * @see #disableLogging()
	 */
	static void setLogger(@Nullable Logger instance) {
		LoggerInternal.instance = instance;
	}

	/**
	 * Disable logging.
	 *
	 * @since 0.1
	 */
	static void disableLogging() {
		setLogger((lvl, tag, fmt, args) -> {});
	}

	/**
	 * Log formatted debug message.
	 * <p>This invokes {@link #log(int, String, String, Object...)} with {@link #DEBUG}.
	 *
	 * @param tag message tag
	 * @param fmt message format string
	 * @param args message format arguments
	 * @since 0.1
	 * @see #log(int, String, String, Object...)
	 */
	static void debug(String tag, String fmt, Object... args) {
		LoggerInternal.log(DEBUG, tag, fmt, args);
	}

	/**
	 * Log debug message, with error cause.
	 * <p>This invokes {@link #log(int, String, String, Object...)} with {@link #DEBUG}.
	 *
	 * @param tag message tag
	 * @param msg message string
	 * @param err error cause
	 * @since 0.1
	 * @see #log(int, String, String, Object...)
	 */
	static void debug(String tag, String msg, Throwable err) {
		LoggerInternal.log(DEBUG, tag, msg, err);
	}

	/**
	 * Log debug message.
	 * <p>This invokes {@link #log(int, String, String, Object...)} with {@link #DEBUG}.
	 *
	 * @param tag message tag
	 * @param msg message string
	 * @since 0.1
	 * @see #log(int, String, String, Object...)
	 */
	static void debug(String tag, String msg) {
		LoggerInternal.log(DEBUG, tag, msg);
	}

	/**
	 * Log formatted informational message.
	 * <p>This invokes {@link #log(int, String, String, Object...)} with {@link #INFO}.
	 *
	 * @param tag message tag
	 * @param fmt message format string
	 * @param args message format arguments
	 * @since 0.1
	 * @see #log(int, String, String, Object...)
	 */
	static void info(String tag, String fmt, Object... args) {
		LoggerInternal.log(INFO, tag, fmt, args);
	}

	/**
	 * Log informational message, with error cause.
	 * <p>This invokes {@link #log(int, String, String, Object...)} with {@link #INFO}.
	 *
	 * @param tag message tag
	 * @param msg message string
	 * @param err error cause
	 * @since 0.1
	 * @see #log(int, String, String, Object...)
	 */
	static void info(String tag, String msg, Throwable err) {
		LoggerInternal.log(INFO, tag, msg, err);
	}

	/**
	 * Log informational message.
	 * <p>This invokes {@link #log(int, String, String, Object...)} with {@link #INFO}.
	 *
	 * @param tag message tag
	 * @param msg message string
	 * @since 0.1
	 * @see #log(int, String, String, Object...)
	 */
	static void info(String tag, String msg) {
		LoggerInternal.log(INFO, tag, msg);
	}

	/**
	 * Log formatted warning message.
	 * <p>This invokes {@link #log(int, String, String, Object...)} with {@link #WARN}.
	 *
	 * @param tag message tag
	 * @param fmt message format string
	 * @param args message format arguments
	 * @since 0.1
	 * @see #log(int, String, String, Object...)
	 */
	static void warn(String tag, String fmt, Object... args) {
		LoggerInternal.log(WARN, tag, fmt, args);
	}

	/**
	 * Log warning message, with error cause.
	 * <p>This invokes {@link #log(int, String, String, Object...)} with {@link #WARN}.
	 *
	 * @param tag message tag
	 * @param msg message string
	 * @param err error cause
	 * @since 0.1
	 * @see #log(int, String, String, Object...)
	 */
	static void warn(String tag, String msg, Throwable err) {
		LoggerInternal.log(WARN, tag, msg, err);
	}

	/**
	 * Log warning message.
	 * <p>This invokes {@link #log(int, String, String, Object...)} with {@link #WARN}.
	 *
	 * @param tag message tag
	 * @param msg message string
	 * @since 0.1
	 * @see #log(int, String, String, Object...)
	 */
	static void warn(String tag, String msg) {
		LoggerInternal.log(WARN, tag, msg);
	}

	/**
	 * Log formatted error message.
	 * <p>This invokes {@link #log(int, String, String, Object...)} with {@link #ERROR}.
	 *
	 * @param tag message tag
	 * @param fmt message format string
	 * @param args message format arguments
	 * @since 0.1
	 * @see #log(int, String, String, Object...)
	 */
	static void error(String tag, String fmt, Object... args) {
		LoggerInternal.log(ERROR, tag, fmt, args);
	}

	/**
	 * Log error message, with error cause.
	 * <p>This invokes {@link #log(int, String, String, Object...)} with {@link #ERROR}.
	 *
	 * @param tag message tag
	 * @param msg message string
	 * @param err error cause
	 * @since 0.1
	 * @see #log(int, String, String, Object...)
	 */
	static void error(String tag, String msg, Throwable err) {
		LoggerInternal.log(ERROR, tag, msg, err);
	}

	/**
	 * Log error message.
	 * <p>This invokes {@link #log(int, String, String, Object...)} with {@link #ERROR}.
	 *
	 * @param tag message tag
	 * @param msg message string
	 * @since 0.1
	 * @see #log(int, String, String, Object...)
	 */
	static void error(String tag, String msg) {
		LoggerInternal.log(ERROR, tag, msg);
	}

	/**
	 * Log message.
	 * <p>If {@code args} is not empty, and its last element is a {@linkplain Throwable throwable},
	 * then the last argument is <b>not</b> used for formatting, and is used as an error cause.
	 *
	 * @param level log level
	 * @param tag message tag
	 * @param fmt message format
	 * @param args message format arguments
	 * @since 0.1
	 */
	@SuppressWarnings("EmptyMethod")
	void log(@LogLevel int level, String tag, String fmt, Object... args);
}
