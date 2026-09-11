// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.core;

import androidx.annotation.StringDef;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Id of a procedure which may be invoked remotely.
 *
 * @since 1.2
 * @see RpcModule#idOfProcedure(String, String, String)
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE_USE)
@StringDef("$$RPC$$")
public @interface RpcProcedureId {
}
