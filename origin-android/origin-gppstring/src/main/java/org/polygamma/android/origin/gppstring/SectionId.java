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
 * Privacy signals section enumeration value marker.
 *
 * @since 0.2
 * @see Section#id()
 * @see <a href="https://github.com/InteractiveAdvertisingBureau/Global-Privacy-Platform/blob/main/Sections/Section%20Information.md">GPP - Section IDs</a>
 */
@SuppressWarnings("deprecation")
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE_USE)
@IntDef({
	CnPrivacyV1.ID,
	Header.ID,
	SignalIntegrity.ID,
	TcfCaV1.ID,
	TcfEuV1.ID,
	TcfEuV2.ID,
	UsNational.ID,
	UsPrivacyV1.ID,
	UsStateCa.ID,
	UsStateCo.ID,
	UsStateCt.ID,
	UsStateDe.ID,
	UsStateFl.ID,
	UsStateIa.ID,
	UsStateMt.ID,
	UsStateNe.ID,
	UsStateNh.ID,
	UsStateNj.ID,
	UsStateOr.ID,
	UsStateTn.ID,
	UsStateTx.ID,
	UsStateUt.ID,
	UsStateVa.ID
})
public @interface SectionId {
}
