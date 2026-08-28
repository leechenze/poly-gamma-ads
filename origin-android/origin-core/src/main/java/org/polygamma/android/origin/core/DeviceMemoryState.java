// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.core;

/**
 * Device memory state descriptor.
 *
 * @since 1.2
 * @see DeviceModule#deviceMemoryState()
 */
public final class DeviceMemoryState {

	private final int trimState;
	private final boolean lowMemory;

	/**
	 * Construct new descriptor.
	 *
	 * @param trimState memory trim state of device
	 * @param lowMem {@code true} if, and only if, device is running low on memory
	 */
	DeviceMemoryState(int trimState, boolean lowMem) {
		this.trimState = trimState;
		this.lowMemory = lowMem;
	}

	/**
	 * Device memory trim state.
	 *
	 * @return trim state
	 * @since 1.2
	 */
	public int trimState() {
		return this.trimState;
	}

	/**
	 * Device is running low on memory.
	 *
	 * @return {@code true} if, and only if, device is running low on memory
	 * @since 1.2
	 */
	public boolean lowMemory() {
		return this.lowMemory;
	}
}
