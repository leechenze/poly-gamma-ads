// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.ads;

import androidx.annotation.IntDef;
import androidx.annotation.Nullable;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Signals that a placement exception of some sort has occurred.
 * <p>Instances of this are described by an error {@linkplain #errorCode() code}. Certain errors,
 * such as {@linkplain #ERROR_CODE_NETWORK network} errors, are {@linkplain #isRecoverable()
 * recoverable}. Recoverable errors are automatically recovered, without re-creation a renderer or
 * re-{@linkplain PlacementRenderer#beginRequestingAds() requesting} ads.
 *
 * @since 1.2
 * @see PlacementEvent#errorCause()
 */
public final class PlacementException extends Exception {

	/**
	 * Unknown error.
	 * <p>Error cause is unknown and is not {@linkplain #isRecoverable() recoverable}.
	 *
	 * @since 1.2
	 * @see #errorCode()
	 */
	public static final int ERROR_CODE_UNKNOWN				= 0;

	/**
	 * Network error.
	 * <p>{@linkplain #isRecoverable() Recoverable} error caused by a failed network operation.
	 * This may be due to device losing network connectivity or remote Origin platform encountering
	 * a resource error. The ads module will automatically re-attempt the failing network operation.
	 *
	 * @since 1.2
	 * @see #errorCode()
	 */
	public static final int ERROR_CODE_NETWORK				= 1;

	/**
	 * Device error.
	 * <p>{@linkplain #isRecoverable() Recoverable} error caused by temporary device resource
	 * constraints. The ads module will automatically re-attempt the failing operation once device
	 * resources become available.
	 *
	 * @since 1.2
	 * @see #errorCode()
	 */
	public static final int ERROR_CODE_DEVICE				= 2;

	/**
	 * Unknown placement error.
	 * <p>Non-{@linkplain #isRecoverable() recoverable} error caused by placement not being
	 * recognized by remote Origin platform. This may be caused by attempting to {@linkplain
	 * PlacementRenderer#bindToPlacement(String) bind} a renderer to a placement id not registered
	 * with the remote Origin platform.
	 *
	 * @since 1.2
	 * @see #errorCode()
	 */
	public static final int ERROR_CODE_INVALID_ID			= 3;

	/**
	 * Placement disabled error.
	 * <p>Non-{@linkplain #isRecoverable() recoverable} error caused by placement being disabled
	 * on the remote Origin platform.
	 *
	 * @since 1.2
	 * @see #errorCode()
	 */
	public static final int ERROR_CODE_DISABLED				= 4;

	/**
	 * Placement {@linkplain PlacementRenderer renderer} error.
	 * <p>Non-{@linkplain #isRecoverable() recoverable} error caused by placement {@linkplain
	 * PlacementRenderer renderer} not being capable of rendering ad media, when it should be
	 * able to.
	 *
	 * @since 1.2
	 * @see #errorCode()
	 */
	public static final int ERROR_CODE_RENDERER				= 5;

	/**
	 * Placement {@linkplain PlacementException exception} error code enumeration value marker.
	 *
	 * @since 1.2
	 * @see PlacementException#errorCode()
	 */
	@Documented
	@Retention(RetentionPolicy.SOURCE)
	@Target(ElementType.TYPE_USE)
	@IntDef({
		ERROR_CODE_DEVICE,
		ERROR_CODE_DISABLED,
		ERROR_CODE_INVALID_ID,
		ERROR_CODE_NETWORK,
		ERROR_CODE_RENDERER,
		ERROR_CODE_UNKNOWN
	})
	public @interface ErrorCode {
	}

	/**
	 * Test whether an error is recoverable.
	 *
	 * @param code error code to test
	 * @return {@code true} if, and only if, error is recoverable
	 */
	static boolean isErrorRecoverable(@ErrorCode int code) {
		switch (code) {
		case ERROR_CODE_DEVICE:
		case ERROR_CODE_NETWORK:
			return true;
		default:
			return false;
		}
	}

	private final @ErrorCode int errorCode;

	/**
	 * Construct new placement exception.
	 *
	 * @param errCode code describing error
	 * @param msg human-readable error message
	 * @param cause error cause, if any
	 */
	PlacementException(@ErrorCode int errCode, String msg, @Nullable Throwable cause) {
		super(msg, cause);
		this.errorCode = errCode;
	}

	/**
	 * Code describing exception.
	 *
	 * @return error code
	 * @since 1.2
	 */
	public @ErrorCode int errorCode() {
		return this.errorCode;
	}

	/**
	 * Test whether exception is recoverable.
	 *
	 * @return {@code true} if, and only if, exception is recoverable
	 * @since 1.2
	 */
	public boolean isRecoverable() {
		return isErrorRecoverable(this.errorCode);
	}
}
