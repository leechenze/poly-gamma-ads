// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.core;

import androidx.annotation.StringDef;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * HTTP {@linkplain HttpModule#newRequestBuilder(String, String, HttpRequest.Listener) request}
 * method enumeration value marker.
 *
 * @since 1.2
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE_USE)
@StringDef({
	"DELETE",
	"GET",
	"HEAD",
	"POST",
	"PUT"
})
public @interface HttpMethod {
}
