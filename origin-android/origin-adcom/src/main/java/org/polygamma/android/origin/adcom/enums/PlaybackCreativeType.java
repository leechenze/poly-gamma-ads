// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.adcom.enums;

import androidx.annotation.IntDef;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Playback creative type enumeration value marker.
 *
 * @since 1.2
 * @see <a href="https://github.com/InteractiveAdvertisingBureau/AdCOM/blob/main/AdCOM%20v1.0%20FINAL.md#list--creative-subtypes---audiovideo-">AdCOM, version 1.0 - List: Creative Subtypes - Audio/Video</a>
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE_USE)
@IntDef({
	AdComEnums.PlaybackCreativeDaast10,
	AdComEnums.PlaybackCreativeDaast10Wrapper,
	AdComEnums.PlaybackCreativeStructured,
	AdComEnums.PlaybackCreativeUnknown,
	AdComEnums.PlaybackCreativeVast10,
	AdComEnums.PlaybackCreativeVast10Wrapper,
	AdComEnums.PlaybackCreativeVast20,
	AdComEnums.PlaybackCreativeVast20Wrapper,
	AdComEnums.PlaybackCreativeVast30,
	AdComEnums.PlaybackCreativeVast30Wrapper,
	AdComEnums.PlaybackCreativeVast40,
	AdComEnums.PlaybackCreativeVast40Wrapper,
	AdComEnums.PlaybackCreativeVast41,
	AdComEnums.PlaybackCreativeVast41Wrapper,
	AdComEnums.PlaybackCreativeVast42,
	AdComEnums.PlaybackCreativeVast42Wrapper,
	AdComEnums.PlaybackCreativeVast43,
	AdComEnums.PlaybackCreativeVast43Wrapper
})
public @interface PlaybackCreativeType {
}
