// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.adcom.enums;

import androidx.annotation.IntDef;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Display creative type enumeration value marker.
 *
 * @since 1.2
 * @see <a href="https://github.com/InteractiveAdvertisingBureau/AdCOM/blob/main/AdCOM%20v1.0%20FINAL.md#list_creativesubtypesdisplay">AdCOM, version 1.0 - List: </a>
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE_USE)
@IntDef({
	AdComEnums.DisplayCreativeAmpHtml,
	AdComEnums.DisplayCreativeHtml,
	AdComEnums.DisplayCreativeImage,
	AdComEnums.DisplayCreativeNative,
	AdComEnums.DisplayCreativeUnknown
})
public @interface DisplayCreativeType {
}
