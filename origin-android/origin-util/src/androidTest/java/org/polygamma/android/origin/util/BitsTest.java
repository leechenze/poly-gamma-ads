// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.polygamma.android.origin.util.Bits.*;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.Arrays;
import java.util.Random;

/**
 * {@link Bits} tests.
 */
@RunWith(AndroidJUnit4.class)
public class BitsTest {

	@Test
	public void testConstants() {
		assertEquals(Integer.SIZE, SIZE_OF_INT);
		assertEquals(Long.SIZE, SIZE_OF_LONG);
	}

	@Test
	public void testIntMaskOf() {
		for (int i = 0; i < SIZE_OF_INT; i++)
			assertEquals(1 << i, intMaskOf(i));
		assertThrows(IndexOutOfBoundsException.class, () -> intMaskOf(-1));
		assertThrows(IndexOutOfBoundsException.class, () -> intMaskOf(SIZE_OF_INT));
	}

	@Test
	public void testLongMaskOf() {
		for (int i = 0; i < SIZE_OF_LONG; i++)
			assertEquals(1L << i, longMaskOf(i));
		assertThrows(IndexOutOfBoundsException.class, () -> longMaskOf(-1));
		assertThrows(IndexOutOfBoundsException.class, () -> longMaskOf(SIZE_OF_LONG));
	}

	@Test
	public void testIntMaskOfRange() {
		for (int i = 0; i < SIZE_OF_INT; i++) {
			for (int j = i; j < SIZE_OF_INT; j++) {
				int exp = 0;

				for (int k = i; k <= j; k++)
					exp |= intMaskOf(k);
				assertEquals(exp, intMaskOfRange(i, j));
			}
		}
		assertThrows(IndexOutOfBoundsException.class, () -> intMaskOfRange(-1, 0));
		assertThrows(IndexOutOfBoundsException.class, () -> intMaskOfRange(0, -1));
		assertThrows(IndexOutOfBoundsException.class, () -> intMaskOfRange(0, SIZE_OF_INT));
		assertThrows(IndexOutOfBoundsException.class, () -> intMaskOfRange(SIZE_OF_INT, 0));
		assertThrows(IndexOutOfBoundsException.class, () -> intMaskOfRange(1, 0));
	}

	@Test
	public void testLongMaskOfRange() {
		for (int i = 0; i < SIZE_OF_LONG; i++) {
			for (int j = i; j < SIZE_OF_LONG; j++) {
				long exp = 0;

				for (int k = i; k <= j; k++)
					exp |= longMaskOf(k);
				assertEquals(exp, longMaskOfRange(i, j));
			}
		}
		assertThrows(IndexOutOfBoundsException.class, () -> longMaskOfRange(-1, 0));
		assertThrows(IndexOutOfBoundsException.class, () -> longMaskOfRange(0, -1));
		assertThrows(IndexOutOfBoundsException.class, () -> longMaskOfRange(0, SIZE_OF_LONG));
		assertThrows(IndexOutOfBoundsException.class, () -> longMaskOfRange(SIZE_OF_LONG, 0));
		assertThrows(IndexOutOfBoundsException.class, () -> longMaskOfRange(1, 0));
	}

	@Test
	public void testFirstSetBitOf() {
		assertEquals(SIZE_OF_INT, firstSetBitOf(0));
		assertEquals(SIZE_OF_LONG, firstSetBitOf(0L));

		for (int i = 0; i < SIZE_OF_INT; i++) {
			assertEquals(i, firstSetBitOf(intMaskOf(i)));
			assertEquals(i, firstSetBitOf(intMaskOfRange(i, SIZE_OF_INT - 1)));
			assertEquals(i, firstSetBitOf(longMaskOf(i)));
			assertEquals(i, firstSetBitOf(longMaskOfRange(i, SIZE_OF_LONG - 1)));
			assertEquals(SIZE_OF_INT + i, firstSetBitOf(longMaskOf(SIZE_OF_INT + i)));
			assertEquals(
				SIZE_OF_INT + i,
				firstSetBitOf(longMaskOfRange(SIZE_OF_INT + i, SIZE_OF_LONG - 1))
			);
		}
	}

	@Test
	public void testNextSetBitOf() {
		int ival = ~0;
		long lval = ~0L;

		for (int i = 0; i < SIZE_OF_INT; i++) {
			assertEquals(SIZE_OF_INT, nextSetBitOf(0, i));
			assertEquals(SIZE_OF_INT, nextSetBitOf(ival, SIZE_OF_INT + i));
			assertEquals(SIZE_OF_LONG, nextSetBitOf(0L, i));
			assertEquals(SIZE_OF_LONG, nextSetBitOf(lval, SIZE_OF_LONG + i));

			assertEquals(i, nextSetBitOf(ival, i));
			assertEquals(i, nextSetBitOf(lval, i));
			assertEquals(SIZE_OF_INT + i, nextSetBitOf(lval, SIZE_OF_INT + i));

			assertEquals(SIZE_OF_INT, nextSetBitOf(ival & ~intMaskOfRange(i, SIZE_OF_INT - 1), i));
			assertEquals(
				SIZE_OF_LONG,
				nextSetBitOf(lval & ~longMaskOfRange(i, SIZE_OF_LONG - 1), i)
			);
			assertEquals(
				SIZE_OF_LONG,
				nextSetBitOf(lval &
				~longMaskOfRange(SIZE_OF_INT + i, SIZE_OF_LONG - 1), SIZE_OF_INT + i)
			);

			assertEquals(i + 1, nextSetBitOf(ival & ~intMaskOf(i), i));
			assertEquals(i + 1, nextSetBitOf(lval & ~longMaskOf(i), i));
			assertEquals(
				SIZE_OF_INT + i + 1,
				nextSetBitOf(lval & ~longMaskOf(SIZE_OF_INT + i), SIZE_OF_INT + i)
			);
		}
	}

	@Test
	public void testSizeOfUnsinged() {
		assertEquals(1, sizeOfUnsigned(0));
		assertEquals(1, sizeOfUnsigned(1));
		assertEquals(2, sizeOfUnsigned(2));
		assertEquals(2, sizeOfUnsigned(3));
		assertEquals(3, sizeOfUnsigned(4));
		assertEquals(3, sizeOfUnsigned(5));
		assertEquals(3, sizeOfUnsigned(6));
		assertEquals(3, sizeOfUnsigned(7));
		assertEquals(4, sizeOfUnsigned(8));
		assertEquals(8, sizeOfUnsigned(0xff));
		assertEquals(16, sizeOfUnsigned(0xffff));
		assertEquals(32, sizeOfUnsigned(Integer.toUnsignedLong(~0)));
		assertEquals(64, sizeOfUnsigned(~0L));
	}

	private void testLoadAndStoreInt(ByteOrder order) {
		final int N = 1024;

		Random rand = new Random(44);
		IntBuffer exp = ByteBuffer.allocate(N * 4)
			.order(order)
			.asIntBuffer();
		byte[] pad = new byte[3];
		int off = pad.length;
		byte[] got = new byte[pad.length + N * 4 + pad.length];

		rand.nextBytes(pad);
		System.arraycopy(pad, 0, got, 0, pad.length);
		System.arraycopy(pad, 0, got, off + N * 4, pad.length);

		for (int i = 0; i < N; i++) {
			int x = rand.nextInt();

			exp.put(i, x);
			if (order == ByteOrder.LITTLE_ENDIAN)
				Bits.storeIntLe(got, off + i * 4, x);
			else
				Bits.storeIntBe(got, off + i * 4, x);
		}
		for (int i = 0; i < N; i++) {
			assertEquals(
				exp.get(i),
				order == ByteOrder.LITTLE_ENDIAN ? Bits.loadIntLe(got, off + i * 4) :
				Bits.loadIntBe(got, off + i * 4)
			);
		}
		assertArrayEquals(pad, Arrays.copyOfRange(got, 0, pad.length));
		assertArrayEquals(pad, Arrays.copyOfRange(got, off + N * 4, off + N * 4 + pad.length));
	}

	@Test
	public void testLoadAndStoreIntLe() {
		assertThrows(ArrayIndexOutOfBoundsException.class, () -> Bits.loadIntLe(new byte[0], 0));
		assertThrows(ArrayIndexOutOfBoundsException.class, () -> Bits.loadIntLe(new byte[4], 1));
		assertThrows(
			ArrayIndexOutOfBoundsException.class,
			() -> Bits.storeIntLe(new byte[0], 0, 0)
		);
		assertThrows(
			ArrayIndexOutOfBoundsException.class,
			() -> Bits.storeIntLe(new byte[4], 1, 0)
		);
		this.testLoadAndStoreInt(ByteOrder.LITTLE_ENDIAN);
	}

	@Test
	public void testLoadAndStoreIntBe() {
		assertThrows(ArrayIndexOutOfBoundsException.class, () -> Bits.loadIntBe(new byte[0], 0));
		assertThrows(ArrayIndexOutOfBoundsException.class, () -> Bits.loadIntBe(new byte[4], 1));
		assertThrows(
			ArrayIndexOutOfBoundsException.class,
			() -> Bits.storeIntBe(new byte[0], 0, 0)
		);
		assertThrows(
			ArrayIndexOutOfBoundsException.class,
			() -> Bits.storeIntBe(new byte[4], 1, 0)
		);
		this.testLoadAndStoreInt(ByteOrder.BIG_ENDIAN);
	}

	@Test
	public void testLoadAndStoreInt() {
		assertThrows(ArrayIndexOutOfBoundsException.class, () -> Bits.loadInt(new byte[0], 0));
		assertThrows(ArrayIndexOutOfBoundsException.class, () -> Bits.loadInt(new byte[4], 1));
		assertThrows(
			ArrayIndexOutOfBoundsException.class,
			() -> Bits.storeInt(new byte[0], 0, 0)
		);
		assertThrows(
			ArrayIndexOutOfBoundsException.class,
			() -> Bits.storeInt(new byte[4], 1, 0)
		);
		this.testLoadAndStoreInt(ByteOrder.nativeOrder());
	}

	private void testLoadAndStoreLong(ByteOrder order) {
		final int N = 1024;

		Random rand = new Random(44);
		LongBuffer exp = ByteBuffer.allocate(N * 8)
			.order(order)
			.asLongBuffer();
		byte[] pad = new byte[3];
		int off = pad.length;
		byte[] got = new byte[pad.length + N * 8 + pad.length];

		rand.nextBytes(pad);
		System.arraycopy(pad, 0, got, 0, pad.length);
		System.arraycopy(pad, 0, got, off + N * 8, pad.length);

		for (int i = 0; i < N; i++) {
			long x = rand.nextLong();

			exp.put(i, x);
			if (order == ByteOrder.LITTLE_ENDIAN)
				Bits.storeLongLe(got, off + i * 8, x);
			else
				Bits.storeLongBe(got, off + i * 8, x);
		}
		for (int i = 0; i < N; i++) {
			assertEquals(
				exp.get(i),
				order == ByteOrder.LITTLE_ENDIAN ? Bits.loadLongLe(got, off + i * 8) :
				Bits.loadLongBe(got, off + i * 8)
			);
		}
		assertArrayEquals(pad, Arrays.copyOfRange(got, 0, pad.length));
		assertArrayEquals(pad, Arrays.copyOfRange(got, off + N * 8, off + N * 8 + pad.length));
	}

	@Test
	public void testLoadAndStoreLongLe() {
		assertThrows(ArrayIndexOutOfBoundsException.class, () -> Bits.loadLongLe(new byte[0], 0));
		assertThrows(ArrayIndexOutOfBoundsException.class, () -> Bits.loadLongLe(new byte[8], 1));
		assertThrows(
			ArrayIndexOutOfBoundsException.class,
			() -> Bits.storeLongLe(new byte[0], 0, 0)
		);
		assertThrows(
			ArrayIndexOutOfBoundsException.class,
			() -> Bits.storeLongLe(new byte[8], 1, 0)
		);
		this.testLoadAndStoreLong(ByteOrder.LITTLE_ENDIAN);
	}

	@Test
	public void testLoadAndStoreLongBe() {
		assertThrows(ArrayIndexOutOfBoundsException.class, () -> Bits.loadLongBe(new byte[0], 0));
		assertThrows(ArrayIndexOutOfBoundsException.class, () -> Bits.loadLongBe(new byte[8], 1));
		assertThrows(
			ArrayIndexOutOfBoundsException.class,
			() -> Bits.storeLongBe(new byte[0], 0, 0)
		);
		assertThrows(
			ArrayIndexOutOfBoundsException.class,
			() -> Bits.storeLongBe(new byte[8], 1, 0)
		);
		this.testLoadAndStoreLong(ByteOrder.BIG_ENDIAN);
	}

	@Test
	public void testLoadAndStoreLong() {
		assertThrows(ArrayIndexOutOfBoundsException.class, () -> Bits.loadLong(new byte[0], 0));
		assertThrows(ArrayIndexOutOfBoundsException.class, () -> Bits.loadLong(new byte[8], 1));
		assertThrows(
			ArrayIndexOutOfBoundsException.class,
			() -> Bits.storeLong(new byte[0], 0, 0)
		);
		assertThrows(
			ArrayIndexOutOfBoundsException.class,
			() -> Bits.storeLong(new byte[8], 1, 0)
		);
		this.testLoadAndStoreLong(ByteOrder.nativeOrder());
	}

	@Test
	public void testExpandBits() {
		final int N = 1024;
		Random rand = new Random(44);
		byte[] padPacked = new byte[3];
		int[] padExpanded = new int[3];
		byte[] packed = new byte[(N + 8 - 1) / 8];
		int[] expanded = new int[N];

		rand.nextBytes(padPacked);
		for (int i = 0; i < padExpanded.length; i++)
			padExpanded[i] = rand.nextInt();
		for (int i = 0; i < N; i++) {
			int v = rand.nextBoolean() ? 1 : 0;

			expanded[i] = v;
			packed[i / 8] |= (byte) (v << (i % 8));
		}
		for (int i = 0; i < N; i++) {
			int nBytes = (i + 8 - 1) / 8;
			int[] got = new int[padExpanded.length + N + padExpanded.length];
			byte[] bits = new byte[padPacked.length + nBytes + padPacked.length];

			System.arraycopy(padExpanded, 0, got, 0, padExpanded.length);
			System.arraycopy(
				padExpanded, 0,
				got, got.length - padExpanded.length,
				padExpanded.length
			);

			System.arraycopy(padPacked, 0, bits, 0, padPacked.length);
			System.arraycopy(padPacked, 0, bits, bits.length - padPacked.length, padPacked.length);
			System.arraycopy(packed, 0, bits, padPacked.length, nBytes);

			assertThrows(IndexOutOfBoundsException.class, () -> Bits.expandBits(
				got, padExpanded.length,
				bits, padPacked.length * 2 + 1,
				got.length
			));
			assertThrows(IndexOutOfBoundsException.class, () -> Bits.expandBits(
				got, padExpanded.length * 2 + 1,
				bits, padPacked.length,
				got.length
			));
			Bits.expandBits(got, padExpanded.length, bits, padPacked.length, i);

			assertArrayEquals(padExpanded, Arrays.copyOfRange(got, 0, padExpanded.length));
			assertArrayEquals(
				Arrays.copyOfRange(expanded, 0, i),
				Arrays.copyOfRange(got, padExpanded.length, padExpanded.length + i)
			);
			assertArrayEquals(padExpanded, Arrays.copyOfRange(
				got,
				got.length - padExpanded.length, got.length
			));

			assertArrayEquals(padPacked, Arrays.copyOfRange(bits, 0, padPacked.length));
			assertArrayEquals(
				Arrays.copyOfRange(packed, 0, nBytes),
				Arrays.copyOfRange(bits, padPacked.length, padPacked.length + nBytes)
			);
			assertArrayEquals(padPacked, Arrays.copyOfRange(
				bits,
				bits.length - padPacked.length, bits.length
			));
		}
	}

	@Test
	public void testExpandBitsReverse() {
		final int N = 1024;
		Random rand = new Random(44);
		byte[] padPacked = new byte[3];
		int[] padExpanded = new int[3];
		byte[] packed = new byte[(N + 8 - 1) / 8];
		int[] expanded = new int[N];

		rand.nextBytes(padPacked);
		for (int i = 0; i < padExpanded.length; i++)
			padExpanded[i] = rand.nextInt();
		for (int i = 0; i < N; i++) {
			int v = rand.nextBoolean() ? 1 : 0;

			expanded[i] = v;
			packed[i / 8] |= (byte) (v << (i % 8));
		}
		for (int i = 0; i < N; i++) {
			int nBytes = (i + 8 - 1) / 8;
			int[] got = new int[padExpanded.length + N + padExpanded.length];
			byte[] bits = new byte[padPacked.length + nBytes + padPacked.length];

			System.arraycopy(padExpanded, 0, got, 0, padExpanded.length);
			System.arraycopy(
				padExpanded, 0,
				got, got.length - padExpanded.length,
				padExpanded.length
			);

			System.arraycopy(padPacked, 0, bits, 0, padPacked.length);
			System.arraycopy(padPacked, 0, bits, bits.length - padPacked.length, padPacked.length);
			System.arraycopy(packed, 0, bits, padPacked.length, nBytes);

			assertThrows(IndexOutOfBoundsException.class, () -> Bits.expandBitsReverse(
				got, padExpanded.length,
				bits, padPacked.length * 2 + 1,
				got.length
			));
			assertThrows(IndexOutOfBoundsException.class, () -> Bits.expandBitsReverse(
				got, padExpanded.length * 2 + 1,
				bits, padPacked.length,
				got.length
			));
			Bits.expandBitsReverse(got, padExpanded.length, bits, padPacked.length, i);

			assertArrayEquals(padExpanded, Arrays.copyOfRange(got, 0, padExpanded.length));
			for (int j = 0; j < i; j++)
				assertEquals(expanded[i - 1 - j], got[padExpanded.length + j]);
			assertArrayEquals(padExpanded, Arrays.copyOfRange(
				got,
				got.length - padExpanded.length, got.length
			));

			assertArrayEquals(padPacked, Arrays.copyOfRange(bits, 0, padPacked.length));
			assertArrayEquals(
				Arrays.copyOfRange(packed, 0, nBytes),
				Arrays.copyOfRange(bits, padPacked.length, padPacked.length + nBytes)
			);
			assertArrayEquals(padPacked, Arrays.copyOfRange(
				bits,
				bits.length - padPacked.length, bits.length
			));
		}
	}
}
