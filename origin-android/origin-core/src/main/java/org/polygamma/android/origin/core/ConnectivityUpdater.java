// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.core;

import android.content.Context;
import android.util.Pair;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.core.util.Consumer;

/**
 * Connectivity {@linkplain Connectivity descriptor} updater.
 */
abstract class ConnectivityUpdater implements AutoCloseable {

	private @Nullable Consumer<ConnectivityUpdater> onUpdateAvailable;

	/**
	 * Construct new descriptor updater.
	 */
	ConnectivityUpdater() {
	}

	/**
	 * Update descriptor.
	 *
	 * @param dst descriptor to provide update in
	 * @param ctxt context to update from
	 * @return pair of updated descriptor and {@code true} if, and only if, updates do not need to
	 * be polled at an interval and can be scheduled {@linkplain #setOnUpdateAvailable(Consumer)
	 * asynchronously}
	 * @throws Exception error encountered while updating
	 */
	@WorkerThread
	abstract Pair<Connectivity, Boolean> update(Connectivity dst, Context ctxt) throws Exception;

	/**
	 * Notify owner that a descriptor update is available.
	 */
	final void onUpdateAvailable() {
		Consumer<ConnectivityUpdater> callback = this.onUpdateAvailable;

		if (callback != null)
			callback.accept(this);
	}

	/**
	 * Set callback to invoke when descriptor {@linkplain #update(Connectivity, Context)
	 * update} is available.
	 *
	 * @param callback callback to invoke or {@code null} to not invoke any callback
	 */
	final void setOnUpdateAvailable(@Nullable Consumer<ConnectivityUpdater> callback) {
		this.onUpdateAvailable = callback;
	}

	@Override
	@WorkerThread
	public void close() {
	}

	@Override
	public boolean equals(@Nullable Object that) {
		return this == that;
	}
}
