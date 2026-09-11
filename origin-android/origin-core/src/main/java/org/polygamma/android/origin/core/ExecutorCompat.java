// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.core;

import android.os.Handler;

import org.polygamma.android.origin.util.Preconditions;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

/**
 * Executor compatibility definitions.
 */
class ExecutorCompat {

	private static final class HandlerExecutor implements Executor {

		private static void throwExecutionRejected() {
			throw new RejectedExecutionException();
		}

		private final Handler handler;

		HandlerExecutor(Handler handler) {
			this.handler = handler;
		}

		@Override
		public void execute(Runnable cmd) {
			if (!this.handler.post(cmd))
				throwExecutionRejected();
		}
	}

	/**
	 * Construct new executor which executes each tasks onto a handler.
	 *
	 * @param handler handler to execute on
	 * @return resulting executor
	 */
	static Executor create(Handler handler) {
		return new HandlerExecutor(Preconditions.checkNotNull(handler));
	}

	private ExecutorCompat() {
	}
}
