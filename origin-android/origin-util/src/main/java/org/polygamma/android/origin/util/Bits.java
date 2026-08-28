// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.util;

import androidx.annotation.IntRange;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Utility definitions for bitwise operations.
 *
 * @since 0.1
 */
public class Bits {

	/**
	 * Size, in bits, of {@code byte}.
	 *
	 * @since 0.2
	 */
	public static final int SIZE_OF_BYTE = 8;

	/**
	 * Size, in bits, of {@code int}.
	 *
	 * @since 0.1
	 */
	public static final int SIZE_OF_INT = 32;

	/**
	 * Size, in bits, of {@code long}.
	 *
	 * @since 0.1
	 */
	public static final int SIZE_OF_LONG = 64;

	/**
	 * Native byte-order is little-endian.
	 */
	public static final boolean LITTLE_ENDIAN = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;

	/**
	 * Construct {@code int} mask with single bit set.
	 *
	 * @param i position of bit to set
	 * @return resulting mask
	 * @throws IndexOutOfBoundsException {@code i} is negative or, greater than or equal to {@link
	 * #SIZE_OF_INT}
	 * @since 0.1
	 */
	public static int intMaskOf(int i) {
		return 1 << Preconditions.checkIndex(i, SIZE_OF_INT);
	}

	/**
	 * Construct {@code long} mask with single bit set.
	 *
	 * @param i position of bit to set
	 * @return resulting mask
	 * @throws IndexOutOfBoundsException {@code i} is negative or, greater than or equal to {@link
	 * #SIZE_OF_LONG}
	 * @since 0.1
	 */
	public static long longMaskOf(int i) {
		return 1L << Preconditions.checkIndex(i, SIZE_OF_LONG);
	}

	/**
	 * Construct {@code int} mask with range of bits set.
	 *
	 * @param i position of first bit to set (inclusive)
	 * @param j position of last bit to set (inclusive)
	 * @return resulting mask
	 * @throws IndexOutOfBoundsException {@code i} is greater than {@code j}, {@code j} is negative,
	 * or, {@code j} is greater than or equal to {@link #SIZE_OF_INT}
	 * @since 0.1
	 */
	public static int intMaskOfRange(int i, int j) {
		Preconditions.checkFromToIndex(i, j, SIZE_OF_INT - 1);
		return (~0 - intMaskOf(i) + 1) & (~0 >>> (SIZE_OF_INT - 1 - j));
	}

	/**
	 * Construct {@code long} mask with range of bits set.
	 *
	 * @param i position of first bit to set (inclusive)
	 * @param j position of last bit to set (inclusive)
	 * @return resulting mask
	 * @throws IndexOutOfBoundsException {@code i} is greater than {@code j}, {@code j} is negative,
	 * or, {@code j} is greater than or equal to {@link #SIZE_OF_LONG}
	 * @since 0.1
	 */
	public static long longMaskOfRange(int i, int j) {
		Preconditions.checkFromToIndex(i, j, SIZE_OF_LONG - 1);
		return (~0L - longMaskOf(i) + 1) & (~0L >>> (SIZE_OF_LONG - 1 - j));
	}

	/**
	 * Find position of first set bit within an {@code int}.
	 *
	 * @param a {@code int} to search in
	 * @return position of first set bit in {@code a}, or {@link #SIZE_OF_INT} if {@code a} is
	 * {@code 0}
	 * @since 0.2
	 */
	public static @IntRange(from = 0, to = SIZE_OF_INT) int firstSetBitOf(int a) {
		return Integer.numberOfTrailingZeros(a);
	}

	/**
	 * Find position of next set bit within an {@code int}.
	 *
	 * @param a {@code int} to search in
	 * @param bit position to search from (inclusive)
	 * @return position greater than or equal to {@code pos} of set bit in {@code a}, or {@link
	 * #SIZE_OF_INT} if {@code bit} is greater than or equal to {@link #SIZE_OF_INT} or there is
	 * no bit set at or above {@code bit}
	 * @since 0.2
	 */
	public static @IntRange(from = 0, to = SIZE_OF_INT) int nextSetBitOf(int a, int bit) {
		return (
			bit >= SIZE_OF_INT ? SIZE_OF_INT :
			firstSetBitOf(a & intMaskOfRange(bit, SIZE_OF_INT - 1))
		);
	}

	/**
	 * Find position of first set bit within a {@code long}.
	 *
	 * @param a {@code long} to search in
	 * @return position of first set bit in {@code a}, or {@link #SIZE_OF_LONG} if {@code a} is
	 * {@code 0}
	 * @since 0.2
	 */
	public static @IntRange(from = 0, to = SIZE_OF_LONG) int firstSetBitOf(long a) {
		return Long.numberOfTrailingZeros(a);
	}

	/**
	 * Find position of next set bit within a {@code long}.
	 *
	 * @param a {@code int} to search in
	 * @param bit position to search from (inclusive)
	 * @return position greater than or equal to {@code pos} of set bit in {@code a}, or {@link
	 * #SIZE_OF_LONG} if {@code bit} is greater than or equal to {@link #SIZE_OF_LONG} or there is
	 * no bit set at or above {@code bit}
	 * @since 0.2
	 */
	public static @IntRange(from = 0, to = SIZE_OF_LONG) int nextSetBitOf(long a, int bit) {
		return (
			bit >= SIZE_OF_LONG ? SIZE_OF_LONG :
			firstSetBitOf(a & longMaskOfRange(bit, SIZE_OF_LONG - 1))
		);
	}

	/**
	 * Calculate size, in bits, of an unsigned value.
	 *
	 * @param v unsigned value to calculate bit size of
	 * @return number of bits required to represent {@code v}
	 * @since 0.1
	 */
	public static int sizeOfUnsigned(long v) {
		return (
			v == 0L ? 1 :
			// ceil(log2(v))
			(SIZE_OF_LONG - Long.numberOfLeadingZeros(v))
		);
	}

	/**
	 * Ensure buffer has a big-endian byte order.
	 *
	 * @param buffer buffer to ensure byte order of
	 * @throws IllegalArgumentException {@code buff} has a little-endian byte order
	 * @since 1.2
	 */
	public static void checkByteBufferOrderBe(ByteBuffer buffer) {
		Preconditions.checkArgument(buffer.order() == ByteOrder.BIG_ENDIAN);
	}

	/**
	 * Ensure buffer has a little-endian byte order.
	 *
	 * @param buffer buffer to ensure byte order of
	 * @throws IllegalArgumentException {@code buff} has a big-endian byte order
	 * @since 1.2
	 */
	public static void checkByteBufferOrderLe(ByteBuffer buffer) {
		Preconditions.checkArgument(buffer.order() == ByteOrder.LITTLE_ENDIAN);
	}

	/**
	 * Ensure buffer has a native byte order.
	 *
	 * @param buff buffer to ensure byte order of
	 * @throws IllegalArgumentException {@code buff} has a non-native byte order
	 * @since 1.2
	 */
	public static void checkByteBufferOrderNe(ByteBuffer buff) {
		Preconditions.checkArgument(buff.order() == ByteOrder.nativeOrder());
	}

	/**
	 * Ensure byte buffer is valid for access within a {@code byte} range.
	 *
	 * @param buff buffer to validate
	 * @param size number of bytes to be accessed
	 * @return {@link ByteBuffer#position() buff.position()}
	 * @throws IndexOutOfBoundsException {@code buff} has fewer than {@code size} bytes {@linkplain
	 * ByteBuffer#remaining() remaining}
	 * @since 1.2
	 */
	public static int checkByteBufferAccess(ByteBuffer buff, int size) {
		int pos = buff.position();

		Preconditions.checkFromIndexSize(pos, size, buff.limit());
		return pos;
	}

	/**
	 * Ensure buffer is valid for access within a {@code byte} range and has a little-endian
	 * byte-order.
	 *
	 * @param buff buffer to validate
	 * @param size number of bytes to be accessed
	 * @return {@link ByteBuffer#position() buff.position()}
	 * @throws IllegalArgumentException {@code buff} has a big-endian byte-order
	 * @throws IndexOutOfBoundsException {@code buff} has fewer than {@code size} bytes {@linkplain
	 * ByteBuffer#remaining() remaining}
	 * @since 1.2
	 */
	public static int checkByteBufferAccessLe(ByteBuffer buff, int size) {
		checkByteBufferOrderLe(buff);
		return checkByteBufferAccess(buff, size);
	}

	/**
	 * Ensure buffer is valid for access within a {@code byte} range and has a big-endian
	 * byte-order.
	 *
	 * @param buff buffer to validate
	 * @param size number of bytes to be accessed
	 * @return {@link ByteBuffer#position() buff.position()}
	 * @throws IllegalArgumentException {@code buff} has a little-endian byte-order
	 * @throws IndexOutOfBoundsException {@code buff} has fewer than {@code size} bytes {@linkplain
	 * ByteBuffer#remaining() remaining}
	 * @since 1.2
	 */
	public static int checkByteBufferAccessBe(ByteBuffer buff, int size) {
		checkByteBufferOrderBe(buff);
		return checkByteBufferAccess(buff, size);
	}

	/**
	 * Ensure buffer is valid for access within a {@code byte} range and has a native byte-order.
	 *
	 * @param buff buffer to validate
	 * @param size number of bytes to be accessed
	 * @return {@link ByteBuffer#position() buff.position()}
	 * @throws IllegalArgumentException {@code buff} has a non-native byte-order
	 * @throws IndexOutOfBoundsException {@code buff} has fewer than {@code size} bytes {@linkplain
	 * ByteBuffer#remaining() remaining}
	 * @since 1.2
	 */
	public static int checkByteBufferAccessNe(ByteBuffer buff, int size) {
		checkByteBufferOrderNe(buff);
		return checkByteBufferAccess(buff, size);
	}

	/**
	 * Load {@code int}, in little-endian byte-order, from byte array.
	 *
	 * @param x array to load from
	 * @param off offset, within {@code x}, to begin loading from
	 * @return loaded value
	 * @throws ArrayIndexOutOfBoundsException {@code off} is negative, or {@code off + 4} is
	 * greater than {@code x.length}
	 * @since 1.2
	 */
	public static int loadIntLe(byte[] x, int off) {
		return
			( x[off + 0] & 0xff) |
			((x[off + 1] & 0xff) <<  8) |
			((x[off + 2] & 0xff) << 16) |
			((x[off + 3] & 0xff) << 24);
	}

	/**
	 * Load {@code int}, in big-endian byte-order, from byte array.
	 *
	 * @param x array to load from
	 * @param off offset, within {@code x}, to begin loading from
	 * @return loaded value
	 * @throws ArrayIndexOutOfBoundsException {@code off} is negative, or {@code off + 4} is
	 * greater than {@code x.length}
	 * @since 1.2
	 */
	public static int loadIntBe(byte[] x, int off) {
		return
			((x[off + 0] & 0xff) << 24) |
			((x[off + 1] & 0xff) << 16) |
			((x[off + 2] & 0xff) <<  8) |
			( x[off + 3] & 0xff);
	}

	/**
	 * Load {@code int}, in native byte-order, from byte array.
	 *
	 * @param x array to load from
	 * @param off offset, within {@code x}, to begin loading from
	 * @return loaded value
	 * @throws ArrayIndexOutOfBoundsException {@code off} is negative, or {@code off + 4} is
	 * greater than {@code x.length}
	 * @since 1.2
	 */
	public static int loadInt(byte[] x, int off) {
		return LITTLE_ENDIAN ? loadIntLe(x, off) : loadIntBe(x, off);
	}

	/**
	 * Store {@code int} into byte array, in little-endian byte-order.
	 *
	 * @param x array to store into
	 * @param off offset, within {@code x}, to begin storing into
	 * @param v value to store
	 * @throws ArrayIndexOutOfBoundsException {@code off} is negative, or {@code off + 4} is
	 * greater than {@code x.length}
	 * @since 1.2
	 */
	public static void storeIntLe(byte[] x, int off, int v) {
		x[off + 0] = (byte) (v & 0xff);
		x[off + 1] = (byte) ((v >>>  8) & 0xff);
		x[off + 2] = (byte) ((v >>> 16) & 0xff);
		x[off + 3] = (byte) ((v >>> 24) & 0xff);
	}

	/**
	 * Store {@code int} into byte array, in big-endian byte-order.
	 *
	 * @param x array to store into
	 * @param off offset, within {@code x}, to begin storing into
	 * @param v value to store
	 * @throws ArrayIndexOutOfBoundsException {@code off} is negative, or {@code off + 4} is
	 * greater than {@code x.length}
	 * @since 1.2
	 */
	public static void storeIntBe(byte[] x, int off, int v) {
		x[off + 0] = (byte) ((v >>> 24) & 0xff);
		x[off + 1] = (byte) ((v >>> 16) & 0xff);
		x[off + 2] = (byte) ((v >>>  8) & 0xff);
		x[off + 3] = (byte) (v & 0xff);
	}

	/**
	 * Store {@code int} into byte array, in native byte-order.
	 *
	 * @param x array to store into
	 * @param off offset, within {@code x}, to begin storing into
	 * @param v value to store
	 * @throws ArrayIndexOutOfBoundsException {@code off} is negative, or {@code off + 4} is
	 * greater than {@code x.length}
	 * @since 1.2
	 */
	public static void storeInt(byte[] x, int off, int v) {
		if (LITTLE_ENDIAN)
			storeIntLe(x, off, v);
		else
			storeIntBe(x, off, v);
	}

	/**
	 * Load {@code long}, in little-endian byte-order, from byte array.
	 *
	 * @param x array to load from
	 * @param off offset, within {@code x}, to begin loading from
	 * @return loaded value
	 * @throws ArrayIndexOutOfBoundsException {@code off} is negative, or {@code off + 8} is
	 * greater than {@code x.length}
	 * @since 1.2
	 */
	public static long loadLongLe(byte[] x, int off) {
		return
			(loadIntLe(x, off + 0) & 0xffffffffL) |
			((loadIntLe(x, off + 4) & 0xffffffffL) << 32);
	}

	/**
	 * Load {@code long}, in big-endian byte-order, from byte array.
	 *
	 * @param x array to load from
	 * @param off offset, within {@code x}, to begin loading from
	 * @return loaded value
	 * @throws ArrayIndexOutOfBoundsException {@code off} is negative, or {@code off + 8} is
	 * greater than {@code x.length}
	 * @since 1.2
	 */
	public static long loadLongBe(byte[] x, int off) {
		return
			((loadIntBe(x, off + 0) & 0xffffffffL) << 32) |
			(loadIntBe(x, off + 4) & 0xffffffffL);
	}

	/**
	 * Load {@code long}, in native byte-order, from byte array.
	 *
	 * @param x array to load from
	 * @param off offset, within {@code x}, to begin loading from
	 * @return loaded value
	 * @throws ArrayIndexOutOfBoundsException {@code off} is negative, or {@code off + 8} is
	 * greater than {@code x.length}
	 * @since 1.2
	 */
	public static long loadLong(byte[] x, int off) {
		return LITTLE_ENDIAN ? loadLongLe(x, off) : loadLongBe(x, off);
	}

	/**
	 * Store {@code long} into byte array, in little-endian byte-order.
	 *
	 * @param x array to store into
	 * @param off offset, within {@code x}, to begin storing into
	 * @param v value to store
	 * @throws ArrayIndexOutOfBoundsException {@code off} is negative, or {@code off + 8} is
	 * greater than {@code x.length}
	 * @since 1.2
	 */
	public static void storeLongLe(byte[] x, int off, long v) {
		storeIntLe(x, off + 0, (int) (v & 0xffffffffL));
		storeIntLe(x, off + 4, (int) (v >>> 32));
	}

	/**
	 * Store {@code long} into byte array, in big-endian byte-order.
	 *
	 * @param x array to store into
	 * @param off offset, within {@code x}, to begin storing into
	 * @param v value to store
	 * @throws ArrayIndexOutOfBoundsException {@code off} is negative, or {@code off + 8} is
	 * greater than {@code x.length}
	 * @since 1.2
	 */
	public static void storeLongBe(byte[] x, int off, long v) {
		storeIntBe(x, off + 0, (int) (v >>> 32));
		storeIntBe(x, off + 4, (int) (v & 0xffffffffL));
	}

	/**
	 * Store {@code long} into byte array, in native byte-order.
	 *
	 * @param x array to store into
	 * @param off offset, within {@code x}, to begin storing into
	 * @param v value to store
	 * @throws ArrayIndexOutOfBoundsException {@code off} is negative, or {@code off + 8} is
	 * greater than {@code x.length}
	 * @since 1.2
	 */
	public static void storeLong(byte[] x, int off, long v) {
		if (LITTLE_ENDIAN)
			storeLongLe(x, off, v);
		else
			storeLongBe(x, off, v);
	}

	/**
	 * Expand bits from a {@code byte} array into {@code int} array.
	 * <p>Upon return, for each bit {@code i}, from {@code 0} (inclusive) to {@code n} (exclusive),
	 * of {@code src}, starting at position {@code srcOff} (inclusive), will be expanded into an
	 * {@code int} into {@code dst}, starting at position {@code dstOff} (inclusive).
	 *
	 * @param dst array to expand into
	 * @param dstOff position, within {@code dst}, to begin expanding into
	 * @param src array to expand
	 * @param srcOff position, within {@code src}, to begin expanding from
	 * @param n number of bits to expand
	 * @throws IndexOutOfBoundsException {@code dstOff}, {@code srcOff}, or {@code n} is negative,
	 * or, {@code dstOff + n} or {@code srcOff + ((n + 8 - 1) / 8)} is greater than {@code
	 * dst.length} or {@code src.length}, respectively
	 * @since 1.2
	 */
	public static void expandBits(int[] dst, int dstOff, byte[] src, int srcOff, int n) {
		int nFull = n / 8;
		int nPart = n % 8;

		Preconditions.checkFromIndexSize(dstOff, n, dst.length);
		Preconditions.checkFromIndexSize(srcOff, nFull + (nPart > 0 ? 1 : 0), src.length);
		for (int i = 0; i < nFull; i++) {
			int packed = src[srcOff + i] & 0xff;

			for (int j = 0; j < 8; j++)
				dst[dstOff + (i * 8) + j] = (packed >>> j) & 1;
		}
		if (nPart > 0) {
			for (int i = 0; i < nPart; i++)
				dst[dstOff + (nFull * 8) + i] = ((src[srcOff + nFull] & 0xff) >>> i) & 1;
		}
	}

	/**
	 * Expand bits, in reverse order, from a {@code byte} array into {@code int} array.
	 * <p>Upon return, for each bit {@code i}, from {@code 0} (inclusive) to {@code n} (exclusive),
	 * of {@code src}, starting at position {@code srcOff} (inclusive), will be expanded into an
	 * {@code int} into {@code dst[dstOff + n - 1 - i]}.
	 *
	 * @param dst array to expand into
	 * @param dstOff position, within {@code dst}, to begin expanding into
	 * @param src array to expand
	 * @param srcOff position, within {@code src}, to begin expanding from
	 * @param n number of bits to expand
	 * @throws IndexOutOfBoundsException {@code dstOff}, {@code srcOff}, or {@code n} is negative,
	 * or, {@code dstOff + n} or {@code srcOff + ((n + 8 - 1) / 8)} is greater than {@code
	 * dst.length} or {@code src.length}, respectively
	 * @since 1.2
	 */
	public static void expandBitsReverse(int[] dst, int dstOff, byte[] src, int srcOff, int n) {
		int nFull = n / 8;
		int nPart = n % 8;

		Preconditions.checkFromIndexSize(dstOff, n, dst.length);
		Preconditions.checkFromIndexSize(srcOff, nFull + (nPart > 0 ? 1 : 0), src.length);
		if (nPart > 0) {
			int packed = src[srcOff + nFull] & 0xff;

			for (int i = 0; i < nPart; i++)
				dst[dstOff + i] = (packed >>> (nPart - 1 - i)) & 1;
		}
		for (int i = 0; i < nFull; i++) {
			int packed = src[srcOff + (nFull - 1 - i)] & 0xff;

			for (int j = 0; j < 8; j++)
				dst[dstOff + nPart + (i * 8) + j] = (packed >>> (7 - j)) & 1;
		}
	}

	private Bits() {
	}
}
