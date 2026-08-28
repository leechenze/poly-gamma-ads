// SPDX-License-Identifier: MPL-2.0

/**
 * Global Privacy Platform (GPP) String coding classes.
 * <p>The {@link org.polygamma.android.origin.gppstring.GppString} class serves as the primary
 * model of a well-formed decoded GPP string. GPP strings can be decoded using {@link
 * org.polygamma.android.origin.gppstring.GppString#of(String)}, or constructed manually using
 * {@link org.polygamma.android.origin.gppstring.GppString#ofBuilder()}. GPP strings are composed
 * of {@link org.polygamma.android.origin.gppstring.Section sections}, which contain signals
 * respective to specific laws and regulations. Sections are composed of a {@linkplain
 * org.polygamma.android.origin.gppstring.Segment core} segment, and zero or more optional
 * segments. Segments themselves map signal fields to values.
 *
 * @since 0.2
 */
@NonNull
package org.polygamma.android.origin.gppstring;

import androidx.annotation.NonNull;
