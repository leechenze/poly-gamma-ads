// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.crypt;

import org.bouncycastle.crypto.engines.ChaCha7539Engine;

final class TestChaCha20Engine extends ChaCha7539Engine {
	int counter;

	TestChaCha20Engine(int counter) {
		this.counter = counter;
	}

	@Override
	protected void resetCounter() {
		super.engineState[12] = counter;
	}

	byte[] generateKeystream() {
		byte[] rv = new byte[ChaCha20.KEYSTREAM_SIZE];

		super.generateKeyStream(rv);
		return rv;
	}
}
