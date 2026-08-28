// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.util;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ScheduledFuture;

/**
 * A {@linkplain ListenableFuture listenable} scheduled future.
 *
 * @param <V> completion value type
 * @since 0.1
 */
public interface ListenableScheduledFuture<V> extends ListenableFuture<V>, ScheduledFuture<V> {
}
