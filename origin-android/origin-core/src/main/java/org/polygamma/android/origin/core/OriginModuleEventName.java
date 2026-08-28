// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.core;

import androidx.annotation.StringDef;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@linkplain OriginModule Module} event name value marker.
 *
 * @since 0.1
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE_USE)
@StringDef(open = true)
public @interface OriginModuleEventName {
}
