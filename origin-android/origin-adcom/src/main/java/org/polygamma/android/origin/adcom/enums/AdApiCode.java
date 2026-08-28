// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.adcom.enums;

import androidx.annotation.IntDef;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Advertising API code enumeration value marker.
 *
 * @since 1.2
 * @see <a href="https://github.com/InteractiveAdvertisingBureau/AdCOM/blob/main/AdCOM%20v1.0%20FINAL.md#list_apiframeworks">AdCOM, version 1.0 - List: API Frameworks</a>
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE_USE)
@IntDef({
	AdComEnums.AdApiMraid10,
	AdComEnums.AdApiMraid20,
	AdComEnums.AdApiMraid30,
	AdComEnums.AdApiOmid10,
	AdComEnums.AdApiOrmma,
	AdComEnums.AdApiSimid10,
	AdComEnums.AdApiSimid11,
	AdComEnums.AdApiUnknown,
	AdComEnums.AdApiVpaid10,
	AdComEnums.AdApiVpaid20
})
public @interface AdApiCode {
}
