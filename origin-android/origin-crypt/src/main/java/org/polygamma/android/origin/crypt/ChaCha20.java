// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.crypt;

import static org.polygamma.android.origin.crypt.UnsignedMath.rotateLeft;
import static org.polygamma.android.origin.util.Bits.loadIntLe;
import static org.polygamma.android.origin.util.Bits.loadLongLe;
import static org.polygamma.android.origin.util.Bits.storeIntLe;
import static org.polygamma.android.origin.util.Bits.storeLongLe;

import androidx.annotation.VisibleForTesting;

import org.polygamma.android.origin.util.Preconditions;


/**
 * ChaCha20 cipher definitions.
 * <p>The encipher and decipher operations for ChaCha are implemented using {@link
 * #xor(byte[], int, byte[], int, int)}, where invoking {@code xor} with a enciphered or deciphered
 * source buffer will result in deciphering or enciphering, respectively. The cipher state must be
 * initialized with a {@linkplain #setKey(byte[], int) key}, {@linkplain #setCounter(int) counter},
 * and {@linkplain #setNonce(byte[], int) nonce}, respectively. Transformations will modify the
 * counter, and may modify the nonce; however, the key is guaranteed to remain consistent.
 * Assigning key, counter, or nonce will result in the cipher's state being reset.
 * {@snippet :
 * SecureRandom rand = new SecureRandom(); // @link substring="SecureRandom" target="java.security.SecureRandom"
 * byte[] key = new byte[ChaCha20.KEY_SIZE]; // @link substring="KEY_SIZE" target="#KEY_SIZE"
 * byte[] nonce = new byte[ChaCha20.NONCE_SIZE]; // @link substring="NONCE_SIZE" target="#NONCE_SIZE"
 *
 * rand.nextBytes(key);
 *
 * ChaCha20 cipher = ChaCha20.ofKey(key, 0); // @link substring="ofKey" target="#ofKey(byte[], int)"
 *
 * for (String plaintext : new String[] { "Hello", "World" }) {
 * 	   byte[] plaintextBuffer = plaintext.getBytes(StandardCharsets.UTF_8);
 *     byte[] ciphertextBuffer = new byte[plaintextBuffer.length];
 * 	   int counter = rand.nextInt(32767);
 *
 * 	   rand.nextBytes(nonce);
 *     cipher.setNonce(nonce, 0); // @link substring="setNonce" target="#setNonce(byte[], int)"
 *     cipher.setCounter(counter); // @link substring="setCounter" target="#setCounter(int)"
 *	   // encipher `plaintext`:
 * 	   cipher.xor(ciphertextBuffer, 0, plaintextBuffer, 0, plaintextBuffer.length); // @link substring="xor" target="#xor(byte[], int, byte[], int, int)"
 *     // clear out `plaintextBuffer` so we can decipher it:
 *     for (int i = 0; i < plaintextBuffer.length; i++)
 * 	       plaintextBuffer[i] = (byte) 0;
 * 	   // reset nonce and counter, because transform above may have modified it:
 *	   cipher.setCounter(counter);
 *	   cipher.setNonce(nonce, 0);
 *     cipher.xor(plaintextBuffer, 0, ciphertextBuffer, 0, ciphertextBuffer.length);
 *
 * 	   assert plaintext.equals(new String(plaintextBuffer, StandardCharsets.UTF_8));
 * }
 *}
 *
 * @since 1.2
 */
@SuppressWarnings("JavadocDeclaration")
public class ChaCha20 {

	@VisibleForTesting
	static final int CONSTANT_0 = 0x61707865 /* expa */;
	@VisibleForTesting
	static final int CONSTANT_1 = 0x3320646e /* nd 3 */;
	@VisibleForTesting
	static final int CONSTANT_2 = 0x79622d32 /* 2-by */;
	@VisibleForTesting
	static final int CONSTANT_3 = 0x6b206574 /* te k */;

	/**
	 * Size, in bytes, of key.
	 *
	 * @since 1.2
	 */
	public static final int KEY_SIZE = 32;

	/**
	 * Size, in bytes, of nonce.
	 *
	 * @since 1.2
	 */
	public static final int NONCE_SIZE = 12;

	// Size, in bytes, of cipher keystream.
	static final int KEYSTREAM_SIZE = 64;

	/**
	 * Construct a new empty ChaCha20 cipher state.
	 *
	 * @return empty state
	 * @since 1.2
	 */
	public static ChaCha20 ofEmpty() {
		return new ChaCha20();
	}

	/**
	 * Construct a new ChaCha20 cipher state initialized with a key.
	 *
	 * @param key key to initialize with
	 * @param off offset, within {@code key}, to begin loading from
	 * @return initialized state
	 * @throws IndexOutOfBoundsException {@code off} is negative or {@link #KEY_SIZE
	 * off + KEY_SIZE} is greater than {@code key.length}
	 * @since 1.2
	 */
	public static ChaCha20 ofKey(byte[] key, int off) {
		return ofEmpty()
			.setKey(key, off);
	}

	@VisibleForTesting
	long key0;
	@VisibleForTesting
	long key64;
	@VisibleForTesting
	long key128;
	@VisibleForTesting
	long key192;

	/**
	 * Cipher keystream.
	 */
	final byte[] keystream;

	/**
	 * Number of bytes remaining in keystream.
	 */
	int keystreamRemaining;

	@VisibleForTesting
	int counter;
	@VisibleForTesting
	int nonce0;
	@VisibleForTesting
	int nonce32;
	@VisibleForTesting
	int nonce64;

	private ChaCha20() {
		this.keystream = new byte[KEYSTREAM_SIZE];
	}

	/**
	 * Construct copy of {@code this} at current state.
	 * <p>The copy returned will be initialized with the same state as {@code this}, including the
	 * underlying keystream state.
	 *
	 * @return copy
	 * @since 1.2
	 */
	public ChaCha20 split() {
		ChaCha20 that = ofEmpty();

		that.key0 = this.key0;
		that.key64 = this.key64;
		that.key128 = this.key128;
		that.key192 = this.key192;
		that.counter = this.counter;
		that.nonce0 = this.nonce0;
		that.nonce32 = this.nonce32;
		that.nonce64 = this.nonce64;
		that.keystreamRemaining = this.keystreamRemaining;
		System.arraycopy(this.keystream, 0, that.keystream, 0, KEYSTREAM_SIZE);
		return that;
	}

	/**
	 * Reset cipher key.
	 *
	 * @param key buffer to load key from
	 * @param off offset, within {@code key}, to begin loading from
	 * @return {@code this}
	 * @throws IndexOutOfBoundsException {@code off} is negative or {@link #KEY_SIZE
	 * off + KEY_SIZE} is greater than {@code key.length}
	 * @since 1.2
	 */
	public ChaCha20 setKey(byte[] key, int off) {
		Preconditions.checkFromIndexSize(off, KEY_SIZE, key.length);
		this.key0 = loadLongLe(key, off +  0);
		this.key64 = loadLongLe(key, off + 8);
		this.key128 = loadLongLe(key, off + 16);
		this.key192 = loadLongLe(key, off + 24);
		this.keystreamRemaining = 0;
		return this;
	}

	/**
	 * Set cipher counter.
	 *
	 * @param c counter to set to
	 * @return {@code this}
	 * @since 1.2
	 */
	public ChaCha20 setCounter(int c) {
		this.counter = c;
		this.keystreamRemaining = 0;
		return this;
	}

	/**
	 * Set cipher nonce.
	 *
	 * @param nonce buffer to load nonce from
	 * @param off offset, within {@code nonce}, to begin loading from
	 * @return {@code this}
	 * @throws IndexOutOfBoundsException {@code off} is negative or {@link #NONCE_SIZE
	 * off + NONCE_SIZE} is greater than {@code nonce.length}
	 * @since 1.2
	 */
	public ChaCha20 setNonce(byte[] nonce, int off) {
		Preconditions.checkFromIndexSize(off, NONCE_SIZE, nonce.length);
		this.nonce0 = loadIntLe(nonce, off + 0);
		this.nonce32 = loadIntLe(nonce, off + 4);
		this.nonce64 = loadIntLe(nonce, off + 8);
		this.keystreamRemaining = 0;
		return this;
	}

	/**
	 * Set cipher nonce to {@code 0}.
	 * <p>Efficient equivalent of:
	 * {@snippet :
	 * setNonce(new byte[NONCE_SIZE], 0); // @link substring="setNonce" target="#setNonce(byte[], int)"
	 * }
	 *
	 * @return {@code this}
	 * @since 1.2
	 */
	public ChaCha20 clearNonce() {
		this.nonce0 = 0;
		this.nonce32 = 0;
		this.nonce64 = 0;
		this.keystreamRemaining = 0;
		return this;
	}

	/**
	 * Apply 20 rounds of ChaCha20, writing result to keystream.
	 * <p>Upon return, {@linkplain #setCounter(int) counter} is incremented, overflowing into
	 * {@linkplain #setNonce(byte[], int) nonce} if rquired, and {@linkplain #keystreamRemaining}
	 * is set to {@link #KEYSTREAM_SIZE}.
	 */
	void applyBlock() {
		int s00 = CONSTANT_0;
		int s01 = CONSTANT_1;
		int s02 = CONSTANT_2;
		int s03 = CONSTANT_3;
		int s04 = (int) (this.key0 & 0xffffffffL);
		int s05 = (int) (this.key0 >>> 32);
		int s06 = (int) (this.key64 & 0xffffffffL);
		int s07 = (int) (this.key64 >>> 32);
		int s08 = (int) (this.key128 & 0xffffffffL);
		int s09 = (int) (this.key128 >>> 32);
		int s10 = (int) (this.key192 & 0xffffffffL);
		int s11 = (int) (this.key192 >>> 32);
		int s12 = this.counter;
		int s13 = this.nonce0;
		int s14 = this.nonce32;
		int s15 = this.nonce64;

		for (int i = 0; i < 10; i++) {
			s00 += s04; s12 ^= s00; s12 = rotateLeft(s12, 16);
			s08 += s12; s04 ^= s08; s04 = rotateLeft(s04, 12);
			s00 += s04; s12 ^= s00; s12 = rotateLeft(s12,  8);
			s08 += s12; s04 ^= s08; s04 = rotateLeft(s04,  7);

			s01 += s05; s13 ^= s01; s13 = rotateLeft(s13, 16);
			s09 += s13; s05 ^= s09; s05 = rotateLeft(s05, 12);
			s01 += s05; s13 ^= s01; s13 = rotateLeft(s13,  8);
			s09 += s13; s05 ^= s09; s05 = rotateLeft(s05,  7);

			s02 += s06; s14 ^= s02; s14 = rotateLeft(s14, 16);
			s10 += s14; s06 ^= s10; s06 = rotateLeft(s06, 12);
			s02 += s06; s14 ^= s02; s14 = rotateLeft(s14,  8);
			s10 += s14; s06 ^= s10; s06 = rotateLeft(s06,  7);

			s03 += s07; s15 ^= s03; s15 = rotateLeft(s15, 16);
			s11 += s15; s07 ^= s11; s07 = rotateLeft(s07, 12);
			s03 += s07; s15 ^= s03; s15 = rotateLeft(s15,  8);
			s11 += s15; s07 ^= s11; s07 = rotateLeft(s07,  7);

			s00 += s05; s15 ^= s00; s15 = rotateLeft(s15, 16);
			s10 += s15; s05 ^= s10; s05 = rotateLeft(s05, 12);
			s00 += s05; s15 ^= s00; s15 = rotateLeft(s15,  8);
			s10 += s15; s05 ^= s10; s05 = rotateLeft(s05,  7);

			s01 += s06; s12 ^= s01; s12 = rotateLeft(s12, 16);
			s11 += s12; s06 ^= s11; s06 = rotateLeft(s06, 12);
			s01 += s06; s12 ^= s01; s12 = rotateLeft(s12,  8);
			s11 += s12; s06 ^= s11; s06 = rotateLeft(s06,  7);

			s02 += s07; s13 ^= s02; s13 = rotateLeft(s13, 16);
			s08 += s13; s07 ^= s08; s07 = rotateLeft(s07, 12);
			s02 += s07; s13 ^= s02; s13 = rotateLeft(s13,  8);
			s08 += s13; s07 ^= s08; s07 = rotateLeft(s07,  7);

			s03 += s04; s14 ^= s03; s14 = rotateLeft(s14, 16);
			s09 += s14; s04 ^= s09; s04 = rotateLeft(s04, 12);
			s03 += s04; s14 ^= s03; s14 = rotateLeft(s14,  8);
			s09 += s14; s04 ^= s09; s04 = rotateLeft(s04,  7);
		}
		storeIntLe(this.keystream,  0 * 4, s00 + CONSTANT_0);
		storeIntLe(this.keystream,  1 * 4, s01 + CONSTANT_1);
		storeIntLe(this.keystream,  2 * 4, s02 + CONSTANT_2);
		storeIntLe(this.keystream,  3 * 4, s03 + CONSTANT_3);
		storeIntLe(this.keystream,  4 * 4, s04 + (int) (this.key0 & 0xffffffffL));
		storeIntLe(this.keystream,  5 * 4, s05 + (int) (this.key0 >>> 32));
		storeIntLe(this.keystream,  6 * 4, s06 + (int) (this.key64 & 0xffffffffL));
		storeIntLe(this.keystream,  7 * 4, s07 + (int) (this.key64 >>> 32));
		storeIntLe(this.keystream,  8 * 4, s08 + (int) (this.key128 & 0xffffffffL));
		storeIntLe(this.keystream,  9 * 4, s09 + (int) (this.key128 >>> 32));
		storeIntLe(this.keystream, 10 * 4, s10 + (int) (this.key192 & 0xffffffffL));
		storeIntLe(this.keystream, 11 * 4, s11 + (int) (this.key192 >>> 32));
		storeIntLe(this.keystream, 12 * 4, s12 + this.counter);
		storeIntLe(this.keystream, 13 * 4, s13 + this.nonce0);
		storeIntLe(this.keystream, 14 * 4, s14 + this.nonce32);
		storeIntLe(this.keystream, 15 * 4, s15 + this.nonce64);
		this.keystreamRemaining = KEYSTREAM_SIZE;
		if (++this.counter == 0)
			this.nonce0++;
	}

	/**
	 * Apply cipher transform.
	 * <p>Upon return, the first {@code len} bytes of {@code dst}, starting at position {@code
	 * dstOff} (inclusive), will contain the ChaCha20 transform applied to the first {@code len}
	 * bytes of {@code src}, starting at position {@code srcOff} (inclusive). The source and
	 * destination buffers may point to the same memory region if, and only if, {@code dstOff} and
	 * {@code srcOff} are equal.
	 *
	 * @param dst buffer to store transformation into
	 * @param dstOff offset, within {@code dst}, to begin storing from
	 * @param src buffer to transform contents of
	 * @param srcOff offset, within {@code src}, to begin loading from
	 * @param len number of bytes to transform
	 * @throws IndexOutOfBoundsException {@code dstOff}, {@code srcOff}, or {@code len} is
	 * negative, or, {@code dstOff + len} or {@code srcOff + len} is greater than {@code
	 * dst.length} or {@code src.length}, respectively
	 * @since 1.2
	 */
	public void xor(byte[] dst, int dstOff, byte[] src, int srcOff, int len) {
		int ksRem = this.keystreamRemaining;

		Preconditions.checkFromIndexSize(dstOff, len, dst.length);
		Preconditions.checkFromIndexSize(srcOff, len, src.length);
		while (len > 0) {
			if (ksRem <= 0) {
				ksRem = KEYSTREAM_SIZE;
				this.applyBlock();
			}

			int lim = Math.min(len, ksRem);
			int ksOff = KEYSTREAM_SIZE - ksRem;
			int wlen = lim / 8;
			int blen = lim % 8;

			for (int i = 0; i < wlen; i++) {
				long s = loadLongLe(src, srcOff + (i * 8));
				long k = loadLongLe(this.keystream, ksOff + (i * 8));

				storeLongLe(dst, dstOff + (i * 8), s ^ k);
			}
			for (int i = 0; i < blen; i++) {
				int s = src[srcOff + (wlen * 8) + i] & 0xff;
				int k = this.keystream[ksOff + (wlen * 8) + i] & 0xff;

				dst[dstOff + (wlen * 8) + i] = (byte) (s ^ k);
			}

			len -= lim;
			ksRem -= lim;
			srcOff += lim;
			dstOff += lim;
		}
		this.keystreamRemaining = ksRem;
	}
}
