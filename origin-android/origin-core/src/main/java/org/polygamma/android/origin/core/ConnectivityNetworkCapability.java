// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.core;

import static org.polygamma.android.origin.core.ConnectivityNetwork.*;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Network capabilities enumeration value marker.
 *
 * @since 0.3
 * @see ConnectivityNetwork#hasCapability(int)
 */
@Retention(RetentionPolicy.SOURCE)
@IntDef({
	CapabilityInternet,
	CapabilityMms,
	CapabilityNotRoaming,
	CapabilityUnmetered,
	CapabilityValidated
})
public @interface ConnectivityNetworkCapability {
}
