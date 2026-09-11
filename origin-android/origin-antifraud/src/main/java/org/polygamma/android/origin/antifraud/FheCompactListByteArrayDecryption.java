// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.antifraud;

import org.polygamma.android.origin.crypt.TorusFhe;
import org.polygamma.android.origin.crypt.TorusFhe.BinaryVector;
import org.polygamma.android.origin.crypt.TorusFhe.ScalarVector;
import org.polygamma.android.origin.util.Bits;
import org.polygamma.android.origin.util.Preconditions;

/**
 * Compact FHE list decryption from {@code byte} array.
 */
final class FheCompactListByteArrayDecryption extends TorusFhe.CompactListDecryption {

	private final byte[] ciphertextBodyList;
	private int ciphertextBodyListOffset;
	private int remainingCiphertext;
	private int nextCiphertextMaskOffset;

	/**
	 * Construct new compact list decryption.
	 *
	 * @param engine engine to decrypt with
	 * @param sk secret key coefficients to decrypt with
	 * @param skOff offset, within {@code sk}, to begin loading from
	 * @param ctMaskList buffer to load ciphertext masks from
	 * @param ctMaskListOff offset, within {@code ctMaskList}, to begin loading from
	 * @param ctBodyList buffer to load ciphertext bodies from
	 * @param ctBodyListOff offset, within {@code ctBodyList}, to begin loading from
	 * @param remCt number of ciphertexts that will be decrypted
	 */
	FheCompactListByteArrayDecryption(
		TorusFhe engine,
		@BinaryVector byte[] sk, int skOff,
		@ScalarVector byte[] ctMaskList, int ctMaskListOff,
		byte[] ctBodyList, int ctBodyListOff,
		int remCt
	) {
		super(engine, sk, skOff);

		this.ciphertextBodyList = ctBodyList;
		this.ciphertextBodyListOffset = ctBodyListOff;
		this.remainingCiphertext = remCt;
		this.nextCiphertextMaskOffset = ctMaskListOff;
		super.setCurrentCiphertextMask(ctMaskList, ctMaskListOff);
	}

	@Override
	protected void beginDecryptingBin() {
		super.setCurrentCiphertextMask(
			super.currentCiphertextMask(),
			this.nextCiphertextMaskOffset
		);
		this.nextCiphertextMaskOffset += super.engine().sizeOfScalarVector();
	}

	@Override
	protected long readCiphertextBody(int i) {
		Preconditions.checkState(this.remainingCiphertext-- > 0);

		long ct = Bits.loadLongLe(this.ciphertextBodyList, this.ciphertextBodyListOffset);

		this.ciphertextBodyListOffset += 8;
		return ct;
	}
}
