// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.core;

import android.os.Build;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.google.common.util.concurrent.ListenableFuture;

import org.polygamma.android.origin.util.Futures;
import org.polygamma.android.origin.util.ListenableFutureTask;

import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinPool;

/**
 * Blocker which simply invokes a callable.
 *
 * @param <V> completion value
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
final class ManagedCallableBlocker<V> implements ForkJoinPool.ManagedBlocker {

	/**
	 * Call command in a managed block context.
	 *
	 * @param <V> completion value
	 * @param cmd command to call
	 * @return completed result future
	 */
	static <V> ListenableFuture<V> managedBlock(Callable<V> cmd) {
		ManagedCallableBlocker<V> mb = new ManagedCallableBlocker<>(cmd);

		try {
			ForkJoinPool.managedBlock(mb);
		} catch (InterruptedException cause) {
			return Futures.ofError(cause);
		}
		return mb.error != null ? Futures.ofError(mb.error) : Futures.of(mb.result);
	}

	private @Nullable Callable<V> command;
	private @Nullable V result;
	private @Nullable Throwable error;

	private ManagedCallableBlocker(Callable<V> cmd) {
		this.command = cmd;
	}

	@Override
	public boolean block() throws InterruptedException {
		Callable<V> cmd = this.command;

		if (cmd == null)
			return true;

		this.command = null;
		try {
			this.result = cmd.call();
		} catch (Exception cause) {
			this.error = cause;
		}
		return true;
	}

	@Override
	public boolean isReleasable() {
		return this.command == null;
	}
}
