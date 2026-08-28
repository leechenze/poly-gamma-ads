// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.adcom.enums;

import androidx.annotation.IntDef;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Advertising error code enumeration value marker.
 * <p>These error codes are defined by the VAST specification, version 4.3.
 *
 * @since 1.2
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE_USE)
@IntDef({
	AdComEnums.AdErrorCatMissing,
	AdComEnums.AdErrorCompanion,
	AdComEnums.AdErrorCompanionFetch,
	AdComEnums.AdErrorCompanionRender,
	AdComEnums.AdErrorCompanionSize,
	AdComEnums.AdErrorDisplay,
	AdComEnums.AdErrorDisplayFetch,
	AdComEnums.AdErrorDisplayLoad,
	AdComEnums.AdErrorIgnored,
	AdComEnums.AdErrorNone,
	AdComEnums.AdErrorPlayback,
	AdComEnums.AdErrorPlaybackInteractive,
	AdComEnums.AdErrorPlaybackInteractiveIgnored,
	AdComEnums.AdErrorPlaybackMediaError,
	AdComEnums.AdErrorPlaybackMediaNotFound,
	AdComEnums.AdErrorPlaybackMediaTimeout,
	AdComEnums.AdErrorPlaybackOverlay,
	AdComEnums.AdErrorPlaybackOverlayFetch,
	AdComEnums.AdErrorPlaybackOverlaySize,
	AdComEnums.AdErrorRenderTimeout,
	AdComEnums.AdErrorUndefined,
	AdComEnums.AdErrorUnsupportedCompanion,
	AdComEnums.AdErrorUnsupportedDuration,
	AdComEnums.AdErrorUnsupportedLinearity,
	AdComEnums.AdErrorUnsupportedSize,
	AdComEnums.AdErrorUnsupportedType,
	AdComEnums.AdErrorUnsupportedPlaybackMedia,
	AdComEnums.AdErrorUnsupportedPlaybackOverlay
})
public @interface AdErrorCode {
}
