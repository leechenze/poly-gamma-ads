// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.adcom.enums;

import androidx.annotation.IntDef;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Playback delivery method enumeration value marker.
 *
 * @since 1.2
 * @see <a href="https://github.com/InteractiveAdvertisingBureau/AdCOM/blob/main/AdCOM%20v1.0%20FINAL.md#list--delivery-methods-">AdCOM, version 1.0 - List: Delivery Methods</a>
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE_USE)
@IntDef({
	AdComEnums.PlaybackDeliveryDownload,
	AdComEnums.PlaybackDeliveryProgressive,
	AdComEnums.PlaybackDeliveryStreaming,
	AdComEnums.PlaybackDeliveryUnknown
})
public @interface PlaybackDeliveryMethod {
}
