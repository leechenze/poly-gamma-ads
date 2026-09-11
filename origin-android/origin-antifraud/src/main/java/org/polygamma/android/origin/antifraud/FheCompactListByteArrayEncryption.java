// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.antifraud;

import org.polygamma.android.origin.crypt.TorusFhe;
import org.polygamma.android.origin.crypt.TorusFhe.ScalarVector;
import org.polygamma.android.origin.util.Bits;
import org.polygamma.android.origin.util.Preconditions;

/**
 * Compact FHE list encryption into {@code byte} array.
 */
final class FheCompactListByteArrayEncryption extends TorusFhe.CompactListEncryption {

	private final byte[] ciphertextBodyList;
	private int ciphertextBodyListOffset;
	private int remainingPlaintext;

	/**
	 * Construct new compact list encryption.
	 *
	 * @param engine engine to encrypt with
	 * @param pkMask mask of public key to encrypt with
	 * @param pkMaskOff offset, within {@code pkMask}, to begin loading from
	 * @param pkBody body of public key to encrypt with
	 * @param pkBodyOff offset, within {@code pkBody}, to begin loading from
	 * @param ctMaskList buffer to store ciphertext masks into
	 * @param ctMaskListOff offset, within {@code ctMaskList}, to begin storing into
	 * @param ctBodyList buffer to store ciphertext bodies into
	 * @param ctBodyListOff offset, within {@code ctBodyList}, to begin storing into
	 * @param remPt number of plaintexts that will be encrypted
	 */
	FheCompactListByteArrayEncryption(
		TorusFhe engine,
		@ScalarVector byte[] pkMask, int pkMaskOff,
		@ScalarVector byte[] pkBody, int pkBodyOff,
		@ScalarVector byte[] ctMaskList, int ctMaskListOff,
		byte[] ctBodyList, int ctBodyListOff,
		int remPt
	) {
		super(engine, pkMask, pkMaskOff, pkBody, pkBodyOff);

		this.ciphertextBodyList = ctBodyList;
		this.ciphertextBodyListOffset = ctBodyListOff;
		this.remainingPlaintext = remPt;
		super.setCurrentCiphertextMask(ctMaskList, ctMaskListOff);
	}

	@Override
	protected void beginEncryptingBin() {
	}

	@Override
	protected void writeCiphertextBody(int i, long ctBody) {
		Bits.storeLongLe(this.ciphertextBodyList, this.ciphertextBodyListOffset + i * 8, ctBody);
	}

	@Override
	protected void writeCiphertextMask(@ScalarVector byte[] ctMask, int ctMaskOff, int ptLen) {
		Preconditions.checkState(this.remainingPlaintext >= ptLen);
		this.remainingPlaintext -= ptLen;
		this.ciphertextBodyListOffset += ptLen * 8;
		if (this.remainingPlaintext > 0)
			super.setCurrentCiphertextMask(ctMask, ctMaskOff + super.engine().sizeOfScalarVector());
	}
}
