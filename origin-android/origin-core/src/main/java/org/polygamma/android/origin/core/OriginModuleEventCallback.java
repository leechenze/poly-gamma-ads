// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.core;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

/**
 * Callback invoked when a {@link OriginModule module} issues an event.
 *
 * @since 0.1
 */
@FunctionalInterface
public interface OriginModuleEventCallback {
	/**
	 * Handle module event.
	 *
	 * @param source module which issued the event
	 * @param name name of event, local to {@code source}
	 * @param data data associated with event, if any
	 * @param timestamp timestamp of when event was generated, in milliseconds since system
	 * {@link android.os.SystemClock#uptimeMillis() boot}
	 * @since 0.1
	 */
	@WorkerThread
	void onOriginModuleEvent(
		OriginModule source,
		@OriginModuleEventName String name,
		@Nullable Object data,
		long timestamp
	);
}
