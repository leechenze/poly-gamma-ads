// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin;

import androidx.annotation.IntDef;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * SDK capability flag marker.
 *
 * @since 1.2
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE_USE)
@IntDef(flag = true, value = { Origin.CAPABILITY_ADS, Origin.CAPABILITY_ANTIFRAUD })
public @interface OriginCapability {
}
