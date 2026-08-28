// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.crypt;

import androidx.annotation.VisibleForTesting;

import org.polygamma.android.origin.util.Bits;
import org.polygamma.android.origin.util.Preconditions;

/**
 * XTEA cipher definitions.
 * <p>This XTEA encipher and decipher operations are implemented using {@link
 * #encipher(byte[], int, byte[], int, int)} and {@link
 * #decipher(byte[], int, byte[], int, int)}, respectively. Both encipher and decipher
 * operations expect the ciphertext and plaintext buffers to be aligned to the 64-bit XTEA
 * block-size, {@link #BLOCK_SIZE}.
 * <p>Unlike the Bouncycastle implementation of XTEA, this implements XTEA in terms of a
 * little-endian byte-order, since majority, if not all, modern phones have a native little-endian
 * byte-order.
 *
 * @since 1.2
 */
public class Xtea {

	@VisibleForTesting
	static final int NUM_ROUNDS = 32;
	@VisibleForTesting
	static final int DELTA = 0x9e3779b9;

	/**
	 * Size, in bytes, of an XTEA secret key.
	 *
	 * @since 1.2
	 */
	public static final int KEY_SIZE = 16;

	/**
	 * Size, in bytes, of an XTEA block.
	 *
	 * @since 1.2
	 */
	public static final int BLOCK_SIZE = 8;

	/**
	 * Construct new XTEA cipher without a key.
	 * <p>The cipher returned must be initialized with a {@linkplain #setKey(byte[], int) key}
	 * before it can be used.
	 *
	 * @return cipher instance
	 * @since 1.2
	 */
	public static Xtea ofEmpty() {
		return new Xtea();
	}

	/**
	 * Construct new XTEA cipher with a key.
	 *
	 * @param key key to initialize with
	 * @param off offset, within {@code key}, to begin loading from
	 * @return cipher instance
	 * @throws IndexOutOfBoundsException {@code off} is negative or {@link #KEY_SIZE off +
	 * KEY_SIZE} is greater than {@code key.length}
	 * @since 1.2
	 */
	public static Xtea ofKey(byte[] key, int off) {
		return ofEmpty()
			.setKey(key, off);
	}

	@VisibleForTesting
	final int[] key;

	private Xtea() {
		this.key = new int[KEY_SIZE / 4];
	}

	/**
	 * Set cipher key.
	 *
	 * @param key new cipher key
	 * @param off offset, within {@code key}, to begin loading from
	 * @return {@code this}
	 * @throws IndexOutOfBoundsException {@code off} is negative or {@link #KEY_SIZE off +
	 * KEY_SIZE} is greater than {@code key.length}
	 * @since 1.2
	 */
	public Xtea setKey(byte[] key, int off) {
		Preconditions.checkFromIndexSize(off, KEY_SIZE, key.length);
		for (int i = 0; i < this.key.length; i++)
			this.key[i] = Bits.loadIntLe(key, off + i * 4);
		return this;
	}

	/**
	 * Encipher buffer.
	 * <p>This enciphers {@code len} bytes of {@code src}, starting at position {@code srcOff}
	 * (inclusive), into {@code dst}, starting at position {@code dstOff} (inclusive). The cipher
	 * length {@code len} must be a multiple of the cipher {@linkplain #BLOCK_SIZE block size}.
	 *
	 * @param dst buffer to store ciphertext into
	 * @param dstOff offset, within {@code dst}, to begin storing from
	 * @param src buffer to load plaintext from
	 * @param srcOff offset, within {@code src}, to begin loading from
	 * @param len number of bytes to encipher
	 * @throws IllegalArgumentException {@code len} is not aligned to block size
	 * @throws IndexOutOfBoundsException {@code dstOff}, {@code srcOff}, or {@code len} is
	 * negative, or, {@code dstOff + len} or {@code srcOff + len} is greater than {@code
	 * dst.length} or {@code src.length}, respectively
	 * @since 1.2
	 */
	public void encipher(byte[] dst, int dstOff, byte[] src, int srcOff, int len) {
		Preconditions.checkArgument((len % BLOCK_SIZE) == 0);
		Preconditions.checkFromIndexSize(dstOff, len, dst.length);
		Preconditions.checkFromIndexSize(srcOff, len, src.length);
		for (int i = 0; i < len; i += 8) {
			int v0 = Bits.loadIntLe(src, srcOff + i + 0);
			int v1 = Bits.loadIntLe(src, srcOff + i + 4);

			for (int j = 0, s = 0; j < NUM_ROUNDS; j++) {
				v0 += (((v1 << 4) ^ (v1 >>> 5)) + v1) ^ (s + this.key[s & 3]);
				s += DELTA;
				v1 += (((v0 << 4) ^ (v0 >>> 5)) + v0) ^ (s + this.key[(s >>> 11) & 3]);
			}
			Bits.storeIntLe(dst, dstOff + i + 0, v0);
			Bits.storeIntLe(dst, dstOff + i + 4, v1);
		}
	}

	/**
	 * Decipher buffer.
	 * <p>This deciphers {@code len} bytes of {@code src}, starting at position {@code srcOff}
	 * (inclusive), into {@code dst}, starting at position {@code dstOff} (inclusive). The cipher
	 * length {@code len} must be a multiple of the cipher {@linkplain #BLOCK_SIZE block size}.
	 *
	 * @param dst buffer to store plaintext into
	 * @param dstOff offset, within {@code dst}, to begin storing from
	 * @param src buffer to load ciphertext from
	 * @param srcOff offset, within {@code src}, to begin loading from
	 * @param len number of bytes to decipher
	 * @throws IllegalArgumentException {@code len} is not aligned to block size
	 * @throws IndexOutOfBoundsException {@code dstOff}, {@code srcOff}, or {@code len} is
	 * negative, or, {@code dstOff + len} or {@code srcOff + len} is greater than {@code
	 * dst.length} or {@code src.length}, respectively
	 * @since 1.2
	 */
	public void decipher(byte[] dst, int dstOff, byte[] src, int srcOff, int len) {
		Preconditions.checkArgument((len % BLOCK_SIZE) == 0);
		Preconditions.checkFromIndexSize(dstOff, len, dst.length);
		Preconditions.checkFromIndexSize(srcOff, len, src.length);
		for (int i = 0; i < len; i += 8) {
			int v0 = Bits.loadIntLe(src, srcOff + i + 0);
			int v1 = Bits.loadIntLe(src, srcOff + i + 4);

			for (int j = 0, s = DELTA * NUM_ROUNDS; j < NUM_ROUNDS; j++) {
				v1 -= (((v0 << 4) ^ (v0 >>> 5)) + v0) ^ (s + this.key[(s >>> 11) & 3]);
				s -= DELTA;
				v0 -= (((v1 << 4) ^ (v1 >>> 5)) + v1) ^ (s + this.key[s & 3]);
			}
			Bits.storeIntLe(dst, dstOff + i + 0, v0);
			Bits.storeIntLe(dst, dstOff + i + 4, v1);
		}
	}
}
