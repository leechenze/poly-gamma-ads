// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.util.SparseBooleanArray;

import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.BitSet;

/**
 * {@link Bitmap} tests.
 */
public class BitmapTest {

	private static final byte _1 = 0b01001011;
	private static final byte _2 = (byte) (~(_1 & 0xff) & 0xff);
	private static final byte _3 = (byte) 0b11010010;
	private static final byte _4 = (byte) (~(_3 & 0xff) & 0xff);

	private static final BitSet WORDS =
		BitSet.valueOf(new long[] {
			((_1 & 0xffL) <<  0) | ((_2 & 0xffL) <<  8) |
			((_3 & 0xffL) << 16) | ((_4 & 0xffL) << 24) |
			((_2 & 0xffL) << 32) | ((_1 & 0xffL) << 40) |
			((_4 & 0xffL) << 48) | ((_3 & 0xffL) << 56),

			((_4 & 0xffL) <<  0) | ((_3 & 0xffL) <<  8) |
			((_2 & 0xffL) << 16) | ((_1 & 0xffL) << 24) |
			((_3 & 0xffL) << 32) | ((_4 & 0xffL) << 40) |
			((_1 & 0xffL) << 48) | ((_2 & 0xffL) << 56),

			((_1 & 0xffL) <<  0) | (0xffL <<  8) |
			((_2 & 0xffL) << 16) | (0xffL << 24) |
			((_3 & 0xffL) << 32) | (0xffL << 40) |
			((_4 & 0xffL) << 48) | (0xffL << 56),

			((_1 & 0xffL) <<  0) | (0xffL        <<  8) |
			(0xffL        << 16) | ((_2 & 0xffL) << 24) |
			((_3 & 0xffL) << 32) | (0xffL        << 40) |
			(0xffL        << 48) | ((_4 & 0xffL) << 56),
		});

	@Test
	public void testOf() {
		assertEquals(0, Bitmap.of(0).size());
		assertEquals(1, Bitmap.of(1).size());
		assertEquals(Bits.SIZE_OF_LONG, Bitmap.of(Bits.SIZE_OF_LONG).size());
	}

	@Test
	public void testBitwise() {
		for (int n = 0; n < WORDS.size(); n += 4) {
			BitSet exp = WORDS.get(0, n);
			Bitmap got = Bitmap.of(n);

			for (int i = 0; i < n; i++) {
				if (exp.get(i))
					got.set(i);
			}
			assertThrows(IndexOutOfBoundsException.class, () -> got.set(-1));
			assertThrows(IndexOutOfBoundsException.class, () -> got.set(got.size()));
			for (int i = 0; i < n; i++)
				assertEquals(exp.get(i), got.test(i));

			for (int i = 0; i < n; i++) {
				if (exp.get(i))
					got.clear(i);
				else
					got.set(i);
			}
			assertThrows(IndexOutOfBoundsException.class, () -> got.clear(-1));
			assertThrows(IndexOutOfBoundsException.class, () -> got.clear(got.size()));
			for (int i = 0; i < n; i++)
				assertEquals(!exp.get(i), got.test(i));

			for (int i = 0; i < n; i++)
				got.toggle(i);
			assertThrows(IndexOutOfBoundsException.class, () -> got.toggle(-1));
			assertThrows(IndexOutOfBoundsException.class, () -> got.toggle(got.size()));
			for (int i = 0; i < n; i++)
				assertEquals(exp.get(i), got.test(i));
		}
	}

	@Test
	public void testOfBuffer() {
		for (int n = 0; n < WORDS.size(); n += 8) {
			BitSet exp = WORDS.get(0, n);
			Bitmap got = Bitmap.ofBuffer(ByteBuffer.wrap(exp.toByteArray()));

			assertEquals(n, got.size());
			for (int i = 0; i < n; i++)
				assertEquals(exp.get(i), got.test(i));
		}
	}

	@Test
	public void testExtractWord() {
		for (int n = 0; n < WORDS.size(); n += 2) {
			BitSet exp = WORDS.get(0, n);
			Bitmap got = Bitmap.of(n);

			for (int i = 0; i < n; i++)
				got.put(i, exp.get(i));
			for (int step = 1; step <= Bits.SIZE_OF_LONG; step++) {
				for (int i = 0; i < (n / step); i++) {
					int bi = i * step;
					long[] wexp = exp.get(bi, bi + step).toLongArray();
					long wgot = got.extractWord(bi, step);

					assertEquals(wexp.length == 0 ? 0 : wexp[0], wgot);
				}
			}
		}
	}

	@Test
	public void testExtractWords() {
		for (int n = 0; n < WORDS.size(); n += 2) {
			BitSet exp = WORDS.get(0, n);
			Bitmap got = Bitmap.of(n);

			for (int i = 0; i < n; i++)
				got.put(i, exp.get(i));
			for (int step = 1; step <= Bits.SIZE_OF_LONG; step++) {
				for (int i = 0; i < (n / step); i++) {
					int bi = i * step;
					int bn = (n / step) - i;
					int nb = (step + 8 - 1) / 8;
					ByteBuffer wgot = ByteBuffer.allocate(nb * bn);

					got.extractWords(bi, wgot, step);
					assertFalse(wgot.hasRemaining());
					wgot.flip();
					assertEquals(wgot, got.extractWords(bi, bn, step));
					for (int j = 0; j < bn; j++) {
						byte[] bexp =
							Arrays.copyOf(
								exp.get(bi + j * step, bi + j * step + step).toByteArray(),
								nb
							);

						for (int k = 0; k < nb; k++)
							assertEquals(bexp[k], wgot.get());
					}
				}
			}
		}
	}

	@Test
	public void testInsertWord() {
		for (int n = 0; n < WORDS.size(); n += 2) {
			BitSet exp = WORDS.get(0, n);

			for (int step = 1; step <= Bits.SIZE_OF_LONG; step++) {
				if (step > n)
					break;

				Bitmap got = Bitmap.of(n);

				for (int i = 0; i < (n / step); i++) {
					int bi = i * step;
					long[] wexp = exp.get(bi, bi + step).toLongArray();
					long mask =
						step == Bits.SIZE_OF_LONG ? 0 :
						Bits.longMaskOfRange(step, Bits.SIZE_OF_LONG - 1);

					got.insertWord(bi, mask | (wexp.length == 0 ? 0 : wexp[0]), step);
				}

				for (int i = 0; i < n; i++)
					assertEquals(i < ((n / step) * step) && exp.get(i), got.test(i));
			}
		}
	}

	@Test
	public void testInsertWords() {
		for (int n = 0; n < WORDS.size(); n += 2) {
			BitSet exp = WORDS.get(0, n);

			for (int step = 1; step <= Bits.SIZE_OF_LONG; step++) {
				if (step > n)
					break;

				Bitmap got = Bitmap.of(n);

				for (int i = 0; i < (n / step); i++) {
					int bi = i * step;
					int bn = (n / step) - i;
					int nb = (step + 8 - 1) / 8;
					ByteBuffer wexp = ByteBuffer.allocate(nb * bn);

					for (int j = 0; j < bn; j++) {
						wexp.put(Arrays.copyOf(
							exp.get(bi + j * step, bi + j * step + step).toByteArray(),
							nb
						));
					}
					got.insertWords(bi, (ByteBuffer) wexp.flip(), step);
					assertFalse(wexp.hasRemaining());
				}

				for (int i = 0; i < n; i++)
					assertEquals(i < ((n / step) * step) && exp.get(i), got.test(i));
			}
		}
	}

	@Test
	public void testExtractSparse() {
		for (int n = 0; n < WORDS.size(); n += 1) {
			BitSet exp = WORDS.get(0, n);
			Bitmap got = Bitmap.of(n);

			for (int i = 0; i < n; i++)
				got.put(i, exp.get(i));
			for (int i = 0; i < n; i++) {
				SparseBooleanArray head = got.extractSparse(0, i);
				SparseBooleanArray tail = got.extractSparse(i, n - i);

				assertTrue(head.size() <= i);
				assertTrue(tail.size() <= (n - i));

				for (int j = 0; j < i; j++)
					assertEquals(exp.get(j), head.get(j));
				for (int j = 0; j < (n - i); j++)
					assertEquals(exp.get(i + j), tail.get(j));
			}
		}
	}

	@Test
	public void testInsertSparse() {
		for (int n = 0; n < WORDS.size(); n += 1) {
			BitSet exp = WORDS.get(0, n);

			for (int i = 0; i < n; i++) {
				SparseBooleanArray head = new SparseBooleanArray();
				SparseBooleanArray tail = new SparseBooleanArray();

				for (int j = 0; j < i; j++) {
					if (exp.get(j))
						head.put(j, true);
				}
				for (int j = 0; j < (n - i); j++) {
					if (exp.get(i + j))
						tail.put(j, true);
				}

				Bitmap gotHead = Bitmap.of(n);
				Bitmap gotTail = Bitmap.of(n);

				gotHead.insertSparse(0, head, i);
				System.err.println(head);
				gotTail.insertSparse(i, tail, n - i);
				System.err.println(tail);

				for (int j = 0; j < n; j++) {
					assertEquals(j < i && exp.get(j), gotHead.test(j));
					assertEquals(j >= i && exp.get(j), gotTail.test(j));
				}
			}
		}
	}
}
