// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.crypt;

import static org.polygamma.android.origin.crypt.UnsignedMath.rotateRight;
import static org.polygamma.android.origin.util.Bits.loadLongLe;
import static org.polygamma.android.origin.util.Bits.storeLongLe;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import org.polygamma.android.origin.util.Bits;
import org.polygamma.android.origin.util.Preconditions;

/**
 * Ascon state.
 *
 * @since 1.2
 */
public class Ascon {

	/**
	 * Size, in bytes, of Ascon 256-bit hash.
	 *
	 * @since 1.2
	 */
	public static final int HASH_SIZE = 32;

	/**
	 * Size, in bytes, of Ascon state.
	 *
	 * @since 1.2
	 */
	public static final int STATE_SIZE = 40;

	/**
	 * Size, in bytes, of an Ascon block.
	 *
	 * @since 1.2
	 */
	public static final int BLOCK_SIZE = 8;

	/**
	 * Construct new empty Ascon state.
	 *
	 * @return empty state
	 * @since 1.2
	 */
	public static Ascon ofEmpty() {
		return new Ascon(0, 0, 0, 0, 0);
	}

	/**
	 * Construct Ascon state initialized from initialization vector.
	 *
	 * @param iv initialization vector to initialize from
	 * @param off offset, within {@code iv}, to begin loading from
	 * @return resulting state
	 * @throws IndexOutOfBoundsException {@code off} is negative or {@link #STATE_SIZE
	 * off + STATE_SIZE} is greater than {@code iv.length}
	 * @since 1.2
	 */
	public static Ascon of(byte[] iv, int off) {
		return ofEmpty()
			.reset(iv, off);
	}

	/**
	 * Construct new Ascon state initialized for 256-bit hash.
	 * <p>The resulting state is initialized with the NIST defined hash initialization vector and
	 * initial 12-round initialization permutation.
	 *
	 * @return hash state
	 * @since 1.2
	 * @see #resetHash()
	 */
	public static Ascon ofHash() {
		return ofEmpty()
			.resetHash();
	}

	@VisibleForTesting
	long x0;
	@VisibleForTesting
	long x1;
	@VisibleForTesting
	long x2;
	@VisibleForTesting
	long x3;
	@VisibleForTesting
	long x4;
	private long lastPartialBlock;

	private Ascon(long x0, long x1, long x2, long x3, long x4) {
		this.x0 = x0;
		this.x1 = x1;
		this.x2 = x2;
		this.x3 = x3;
		this.x4 = x4;
	}

	/**
	 * Construct copy of {@code this} at current state.
	 *
	 * @return copy
	 * @since 1.2
	 */
	public Ascon split() {
		Ascon that = ofEmpty();

		that.x0 = this.x0;
		that.x1 = this.x1;
		that.x2 = this.x2;
		that.x3 = this.x3;
		that.x4 = this.x4;
		that.lastPartialBlock = this.lastPartialBlock;
		return that;
	}

	/**
	 * Serialize state to buffer.
	 *
	 * @param dst buffer to serialize state to
	 * @param off offset, within {@code dst}, to begin storing from
	 * @throws IndexOutOfBoundsException {@code off} is negative or {@link #STATE_SIZE
	 * off + STATE_SIZE} is greater than {@code dst.length}
	 * @since 1.2
	 * @see #of(byte[], int)
	 */
	public void serialize(byte[] dst, int off) {
		Preconditions.checkFromIndexSize(off, STATE_SIZE, dst.length);
		storeLongLe(dst, off +  0, this.x0);
		storeLongLe(dst, off +  8, this.x1);
		storeLongLe(dst, off + 16, this.x2);
		storeLongLe(dst, off + 24, this.x3);
		storeLongLe(dst, off + 32, this.x4);
	}

	/**
	 * Reset state with an initialization vector.
	 *
	 * @param iv buffer to load from
	 * @param off offset, within {@code iv}, to begin loading from
	 * @return {@code this}
	 * @throws IndexOutOfBoundsException {@code off} is negative or {@link #STATE_SIZE
	 * off + STATE_SIZE} is greater than {@code iv.length}
	 * @since 1.2
	 */
	public Ascon reset(byte[] iv, int off) {
		Preconditions.checkFromIndexSize(off, STATE_SIZE, iv.length);
		this.x0 = loadLongLe(iv, off +  0);
		this.x1 = loadLongLe(iv, off +  8);
		this.x2 = loadLongLe(iv, off + 16);
		this.x3 = loadLongLe(iv, off + 24);
		this.x4 = loadLongLe(iv, off + 32);
		this.lastPartialBlock = 0L;
		return this;
	}

	/**
	 * Reset state for computing 256-bit hash.
	 *
	 * @return {@code this}
	 * @since 1.2
	 */
	public Ascon resetHash() {
		this.x0 = 0x9b1e5494e934d681L;
		this.x1 = 0x4bc3a01e333751d2L;
		this.x2 = 0xae65396c6b34b81aL;
		this.x3 = 0x3c7fd4a4d56a4db3L;
		this.x4 = 0x1a5c464906c5976dL;
		this.lastPartialBlock = 0L;
		return this;
	}

	/**
	 * Apply a single round of Ascon.
	 *
	 * @param c constant to apply round with
	 * @since 1.2
	 */
	public void applyRound(long c) {
		long x0 = this.x0;
		long x1 = this.x1;
		long x2 = this.x2 ^ c;
		long x3 = this.x3;
		long x4 = this.x4;
		long t0 = x3 ^ (x1 | x2) ^ x0 ^ (x1 & (x0 ^ x4));
		long t1 = x0 ^ x4 ^ (x1 | x2 | x3) ^ (x1 & x2 & x3);
		long t2 = x1 ^ x2 ^ (x4 & ~x3);
		long t3 = (x0 | (x3 ^ x4)) ^ x1 ^ x2;
		long t4 = x3 ^ (x1 | x4) ^ (x0 & x1);

		this.x0 = t0   ^ rotateRight(t0, 19)  ^ rotateRight(t0, 28);
		this.x1 = t1   ^ rotateRight(t1, 39)  ^ rotateRight(t1, 61);
		this.x2 = ~(t2 ^ rotateRight(t2,  1)  ^ rotateRight(t2,  6));
		this.x3 = t3   ^ rotateRight(t3, 10)  ^ rotateRight(t3, 17);
		this.x4 = t4   ^ rotateRight(t4,  7)  ^ rotateRight(t4, 41);
	}

	/**
	 * Apply a 12-round permutation of Ascon.
	 *
	 * @since 1.2
	 */
	public void applyPermute12() {
		int p1 = 0xc3d2e1f0;
		int p2 = 0x8796a5b4;
		int p3 = 0x4b5a6978;

		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 4; j++)
				this.applyRound((p1 >>> (j * 8)) & 0xffL);
			p1 = p2;
			p2 = p3;
		}
	}

	// Buffer `src` from `[off,off+len)` into `lastPartialBlock` with `len < BLOCK_SIZE`.
	private void bufferPartialBlock(byte[] src, int off, int len) {
		Preconditions.checkArgument(len > 0 && len < BLOCK_SIZE);

		long w = 0;

		for (int i = 0; i < len; i++)
			w |= (src[off + i] & 0xffL) << (i * 8);
		this.lastPartialBlock = w | (1L << (len * 8));
	}

	/*
	 * Absorb the last partial block. When `src` is `null`, this pads the last partial block and
	 * absorbs it.
	 */
	private int absorbLastPartialHashBlock(@Nullable byte[] src, int off, int len) {
		Preconditions.checkArgument((len == 0 && src == null) || (len != 0 && src != null));

		long w = this.lastPartialBlock;
		int tlen = (64 - Long.numberOfLeadingZeros(w)) / 8;
		int tshift = tlen * 8;

		// remove the trailing length marker
		w &= ~(1L << tshift);
		this.lastPartialBlock = 0L;
		if (src == null) {
			Preconditions.checkState(tlen != BLOCK_SIZE);
			this.x0 ^= w ^ (1L << tshift);
			this.applyPermute12();
			return 0;
		}

		// merge in new bits and see if we get a full block
		int plen = Math.min(BLOCK_SIZE - tlen, len);

		for (int i = 0; i < plen; i++)
			w |= (src[off + i] & 0xffL) << (tshift + i * 8);
		tlen += plen;
		if (tlen == BLOCK_SIZE) {
			// we got a full block, absorb it
			this.x0 ^= w;
			this.applyPermute12();
		} else {
			// not a full block, keep buffering
			this.lastPartialBlock = tlen == 0 ? 0L : (w | (1L << (tlen * 8)));
		}
		return plen;
	}

	/**
	 * Absorb input for computing hash.
	 * <p>Upon return, the state is updated with the first {@code N} bytes absorbed from
	 * {@code src}, starting at position {@code off} (inclusive). The number of bytes absorbed
	 * {@code N} is guaranteed to be less than or equal to {@code len}. When {@code len} is aligned
	 * to the {@linkplain #BLOCK_SIZE block-size}, it is guaranteed {@code N} will equal {@code
	 * len}; otherwise, {@code N} will equal {@code len} aligned <i>down</i> to the block-size
	 * boundary, with the tail bytes buffered for the next absorbtion or {@linkplain
	 * #finishHash(byte[], int) squeezing}.
	 *
	 * @param src buffer to hash contents of
	 * @param off offset, within {@code src}, to begin hashing from
	 * @param len number of bytes to hash
	 * @throws IndexOutOfBoundsException {@code off} or {@code len} is negative, or, {@code
	 * off + len} is greater than {@code src.length}
	 * @since 1.2
	 */
	public void updateHash(byte[] src, int off, int len) {
		if (this.lastPartialBlock != 0L) {
			int n = this.absorbLastPartialHashBlock(src, off, len);

			off += n;
			len -= n;
		}
		int wlen = len / BLOCK_SIZE;
		int blen = len % BLOCK_SIZE;

		Preconditions.checkFromIndexSize(off, len, src.length);
		for (int i = 0; i < wlen; i++) {
			this.x0 ^= loadLongLe(src, off + i * BLOCK_SIZE);
			this.applyPermute12();
		}
		if (blen != 0)
			this.bufferPartialBlock(src, off + (wlen * BLOCK_SIZE), blen);
	}

	/**
	 * Generate 256-bit hash.
	 *
	 * @param dst buffer to store resulting hash into
	 * @param off offset, within {@code dst}, to begin storing from
	 * @throws IndexOutOfBoundsException {@code off} is negative or {@link #HASH_SIZE
	 * off + HASH_SIZE} is greater than {@code dst.length}
	 * @since 1.2
	 */
	public void finishHash(byte[] dst, int off) {
		Preconditions.checkFromIndexSize(off, HASH_SIZE, dst.length);
		if (this.lastPartialBlock != 0L) {
			this.absorbLastPartialHashBlock(null, 0, 0);
		} else {
			this.x0 ^= 1L;
			this.applyPermute12();
		}

		for (int i = 0; i < 3; i++) {
			Bits.storeLongLe(dst, off + i * BLOCK_SIZE, this.x0);
			this.applyPermute12();
		}
		Bits.storeLongLe(dst, off + 3 * BLOCK_SIZE, this.x0);
	}
}
