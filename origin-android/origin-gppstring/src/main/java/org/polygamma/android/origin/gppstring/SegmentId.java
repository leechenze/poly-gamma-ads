// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.gppstring;

import static org.polygamma.android.origin.gppstring.GppIds.*;

import androidx.annotation.IntDef;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Privacy signals segment enumeration value marker.
 *
 * @since 0.2
 * @see Segment#id()
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE_USE)
@IntDef({
	TcfCaV1.Core.ID,
	TcfCaV1.DisclosedVendors.ID,
	TcfCaV1.PublisherPurposes.ID,
	TcfEuV2.Core.ID,
	TcfEuV2.DisclosedVendors.ID,
	TcfEuV2.PublisherPurposes.ID,
})
public @interface SegmentId {
}
