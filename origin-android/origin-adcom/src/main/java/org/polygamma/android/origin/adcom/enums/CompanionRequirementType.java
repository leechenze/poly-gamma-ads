// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.adcom.enums;

import androidx.annotation.IntDef;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Companion ad media execution requirement type enumeration value marker.
 *
 * @since 1.2
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE_USE)
@IntDef({
	AdComEnums.CompanionRequirementAll,
	AdComEnums.CompanionRequirementAny,
	AdComEnums.CompanionRequirementNone
})
public @interface CompanionRequirementType {
}
