// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.adcom.enums;

import androidx.annotation.IntDef;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Advertising event tracker error code enumeration value marker.
 * <p>These error codes are defined by the VAST specification, version 4.3.
 *
 * @since 1.2
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE_USE)
@IntDef({
	AdComEnums.AdEventTrackerErrorLoad,
	AdComEnums.AdEventTrackerErrorNone,
	AdComEnums.AdEventTrackerErrorRejected,
	AdComEnums.AdEventTrackerErrorUnsupported,
})
public @interface AdEventTrackerErrorCode {
}
