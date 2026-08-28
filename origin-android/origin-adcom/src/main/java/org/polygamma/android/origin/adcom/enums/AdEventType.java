// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.adcom.enums;

import androidx.annotation.IntDef;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Advertising media event type enumeration value marker.
 *
 * @since 1.2
 * @see <a href="https://github.com/InteractiveAdvertisingBureau/AdCOM/blob/main/AdCOM%20v1.0%20FINAL.md#list--event-types-">AdCOM, version 1.0 - List: Event Types</a>
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE_USE)
@IntDef({
	AdComEnums.AdEventActivated,
	AdComEnums.AdEventActivatedLink,
	AdComEnums.AdEventClosed,
	AdComEnums.AdEventCollapsed,
	AdComEnums.AdEventError,
	AdComEnums.AdEventExpanded,
	AdComEnums.AdEventIgnored,
	AdComEnums.AdEventImpression,
	AdComEnums.AdEventInteracted,
	AdComEnums.AdEventLoaded,
	AdComEnums.AdEventMediaPlayerCollapse,
	AdComEnums.AdEventMediaPlayerExpand,
	AdComEnums.AdEventMediaPlayerMuted,
	AdComEnums.AdEventMediaPlayerPaused,
	AdComEnums.AdEventMediaPlayerRewind,
	AdComEnums.AdEventMediaPlayerUnmuted,
	AdComEnums.AdEventMediaPlayerUnpaused,
	AdComEnums.AdEventMinimized,
	AdComEnums.AdEventNotViewable,
	AdComEnums.AdEventPlaybackInteractiveStart,
	AdComEnums.AdEventPlaybackOverlayAccept,
	AdComEnums.AdEventPlaybackOverlayViewDur,
	AdComEnums.AdEventPlaybackProgress,
	AdComEnums.AdEventPlaybackTerminated,
	AdComEnums.AdEventSkipped,
	AdComEnums.AdEventUnknown,
	AdComEnums.AdEventViewableMrc100,
	AdComEnums.AdEventViewableMrc50,
	AdComEnums.AdEventViewableVideo50,
	AdComEnums.AdEventViewUndetermined
})
public @interface AdEventType {
}
