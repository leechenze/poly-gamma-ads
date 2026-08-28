// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.gppstring;

import static org.polygamma.android.origin.gppstring.FieldTypes.*;

import androidx.annotation.IntDef;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Privacy signal field type enumeration value marker.
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE_USE)
@IntDef({
	ArrayOfFixedIntRanges,
	ArrayOfIntRanges,
	Bitfield,
	Boolean,
	Datetime,
	FibonacciInt,
	FibonacciIntRange,
	FixedBitfield,
	FixedInt,
	FixedInt16Range,
	FixedIntList,
	FixedString,
	OptimizedIntRange,
	OptimizedIntRange2
})
@interface FieldType {
}
