// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.gppstring;

import static org.polygamma.android.origin.gppstring.GppIds.*;

import androidx.annotation.LongDef;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Privacy signal field enumeration value marker.
 *
 * @since 0.2
 * @see Segment
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE_USE)
@LongDef({
	Header.Core.Sections,
	Header.Core.Type,
	Header.Core.Version
})
public @interface FieldId {
}
