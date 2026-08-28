// SPDX-License-Identifier: MIT OR Apache-2.0

/**
 * Base AdCOM definitions, specialized for Android.
 * <p>AdCOM objects are grouped into 3 groups, {@code media}, {@code placement}, and {@code
 * context}. Each group is implemented by their respective package. Each object can be serialized
 * into or deserialized from a Protobuf payload.
 *
 * @since 0.1
 * @see <a href="https://github.com/InteractiveAdvertisingBureau/AdCOM/blob/main/AdCOM%20v1.0%20FINAL.md">AdCOM, version 1.0</a>
 */
@NonNull
package org.polygamma.android.origin.adcom;

import androidx.annotation.NonNull;
