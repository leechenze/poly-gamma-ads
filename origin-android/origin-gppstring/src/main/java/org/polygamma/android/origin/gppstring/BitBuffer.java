// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.gppstring;

import android.graphics.Point;
import android.util.SparseBooleanArray;

import org.polygamma.android.origin.util.Bitmap;
import org.polygamma.android.origin.util.Bits;
import org.polygamma.android.origin.util.Preconditions;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

/**
 * Buffer of bits backed with a dense {@linkplain Bitmap bitmap}.
 */
final class BitBuffer {

	/**
	 * Table of Fibonacci numbers for up to 45 bits (covers a full 31-bit word).
	 */
	private static final int[] FIBONACCI_TABLE =
		{
			0x1, 0x2, 0x3, 0x5, 0x8, 0xd,
			0x15, 0x22, 0x37, 0x59, 0x90, 0xe9,
			0x179, 0x262, 0x3db, 0x63d, 0xa18,
			0x1055, 0x1a6d, 0x2ac2, 0x452f, 0x6ff1, 0xb520,
			0x12511, 0x1da31, 0x2ff42, 0x4d973, 0x7d8b5, 0xcb228,
			0x148add, 0x213d05, 0x35c7e2, 0x5704e7, 0x8cccc9, 0xe3d1b0,
			0x1709e79, 0x2547029, 0x3c50ea2, 0x6197ecb, 0x9de8d6d, 0xff80c38,
			0x19d699a5, 0x29cea5dd, 0x43a53f82, 0x6d73e55f
		};

	private static void reverseBits(ByteBuffer src) {
		// GPP strings are coded left to right...
		for (int i = 0; i < src.limit(); i++)
			src.put(i, (byte) (Integer.reverseBytes(Integer.reverse(src.get(i) & 0xff)) & 0xff));
	}

	private Bitmap bits;
	private int position;

	/**
	 * Construct new buffer with existing bits.
	 *
	 * @param src existing bits
	 */
	BitBuffer(byte[] src) {
		ByteBuffer dec = ByteBuffer.wrap(src);

		reverseBits(dec);
		this.bits = Bitmap.ofBuffer(dec);
	}

	/**
	 * Construct new empty buffer.
	 */
	BitBuffer() {
		this.bits = Bitmap.of(512);
	}

	/**
	 * Remaining capacity.
	 *
	 * @return capacity, in bits
	 */
	private int remaining() {
		return this.bits.size() - this.position;
	}

	/**
	 * Ensure underlying bitmap has sufficient capacity.
	 *
	 * @param rem minimum remaining capacity, in bits, to ensure
	 * @return {@link #bits}
	 */
	private Bitmap ensureCapacity(int rem) {
		int diff = rem - this.remaining();

		if (diff > 0) {
			// align up to word boundary
			this.bits =
				this.bits.withSize(
					((this.bits.size() + diff) + (Bits.SIZE_OF_LONG - 1)) &
					~(Bits.SIZE_OF_LONG - 1)
				);
		}
		return this.bits;
	}

	/**
	 * Advance position, returning previous position.
	 *
	 * @param len length, in bits, to advance position by
	 * @return previous position
	 */
	private int advance(int len) {
		int curr = this.position;

		this.position = curr + len;
		return curr;
	}

	/**
	 * Constructe {@code byte} array representation of buffer.
	 *
	 * @return resulting representation
	 */
	byte[] toByteArray() {
		ByteBuffer src = ByteBuffer.wrap(this.bits.toByteArray(this.position));

		reverseBits(src);
		return src.array();
	}

	/**
	 * Read fixed length {@link FieldTypes#FixedInt int}.
	 *
	 * @param len length, in bits, of {@code int}
	 * @return read value
	 * @throws IllegalArgumentException {@code len} is less than {@code 0} or greater than {@code
	 * int} {@linkplain Bits#SIZE_OF_INT length}
	 * @throws IndexOutOfBoundsException read overflows underlying buffer
	 */
	int readFixedInt(int len) {
		Preconditions.checkArgument(len >= 0 && len <= Bits.SIZE_OF_INT);
		return Integer.reverse((int) this.bits.extractWord(this.advance(len), len)) >>> (32 - len);
	}

	/**
	 * Write fixed length {@link FieldTypes#FixedInt int}.
	 *
	 * @param len length, in bits, of {@code x}
	 * @param x value to write
	 */
	void writeFixedInt(int len, int x) {
		this.ensureCapacity(len)
			.insertWord(
				this.advance(len),
				Integer.toUnsignedLong(Integer.reverse(x) >>> (32 - len)),
				len
			);
	}

	/**
	 * Read {@link FieldTypes#Boolean boolean}.
	 *
	 * @return read value
	 * @throws IndexOutOfBoundsException read overflows underlying buffer
	 */
	boolean readBoolean() {
		return this.bits.test(this.advance(1));
	}

	/**
	 * Write {@link FieldTypes#Boolean boolean}.
	 *
	 * @param val value to write
	 */
	void writeBoolean(boolean val) {
		this.ensureCapacity(1).put(this.advance(1), val);
	}

	/**
	 * Read {@link FieldTypes#Datetime datetime}.
	 *
	 * @return read value
	 * @throws IndexOutOfBoundsException read overflows underlying buffer
	 */
	Date readDatetime() {
		long ts = Long.reverse(this.bits.extractWord(this.advance(36), 36)) >>> (64 - 36);

		return new Date(ts * 100);
	}

	/**
	 * Write {@link FieldTypes#Datetime datetime}.
	 *
	 * @param date value to write
	 */
	void writeDatetime(Date date) {
		this.ensureCapacity(36)
			.insertWord(
				this.advance(36),
				Long.reverse(date.getTime() / 100) >>> (64 - 36),
				36
			);
	}

	/**
	 * Read variable length {@link FieldTypes#FibonacciInt int}.
	 *
	 * @return read value
	 * @throws IllegalStateException coding is malformed
	 * @throws IndexOutOfBoundsException read overflows underlying buffer
	 */
	int readFibonacciInt() {
		int acc = 0;
		int len;
		// We can have only `FIBONACCI_TABLE.length` int bits, and a trailing terminator bit
		long word =
			this.bits.extractWord(
				this.position,
				Math.min(this.remaining(), FIBONACCI_TABLE.length + 1)
			);
		int off = Bits.firstSetBitOf(word);

		while (true) {
			Preconditions.checkState(off != Bits.SIZE_OF_LONG);

			acc += FIBONACCI_TABLE[off];
			len = off + 1;

			int nextOff = Bits.nextSetBitOf(word, len);

			if (nextOff == (off + 1)) {
				// found the terminator, all done now
				len++;
				break;
			}
			off = nextOff;
		}
		this.advance(len);
		return acc;
	}

	/**
	 * Write variable length {@link FieldTypes#FibonacciInt int}.
	 *
	 * @param x value to write
	 * @throws IllegalArgumentException {@code x} is less than {@code 1}
	 */
	void writeFibonacciInt(int x) {
		Preconditions.checkArgument(x > 0);

		int fibLen = Arrays.binarySearch(FIBONACCI_TABLE, x);

		if (fibLen < 0)
			fibLen = -fibLen - 2;

		Bitmap bits = this.ensureCapacity(fibLen + 2);
		int pos = this.position + (fibLen + 1);

		bits.set(pos);
		for (int i = fibLen; i >= 0; i--) {
			int fib = FIBONACCI_TABLE[i];

			pos--;
			if (fib <= x) {
				x = x - fib;
				bits.set(pos);
			}
		}
		this.position += fibLen + 2;
	}

	/**
	 * Read fixed length {@link FieldTypes#FixedString string}.
	 *
	 * @param len string length, in ASCII characters
	 * @return read value
	 * @throws IndexOutOfBoundsException read overflows underlying buffer
	 */
	String readFixedString(int len) {
		byte[] chars = new byte[len];

		for (int i = 0; i < len; i++)
			chars[i] = (byte) (this.readFixedInt(6) + 65);
		return new String(chars, StandardCharsets.US_ASCII);
	}

	/**
	 * Write fixed length {@link FieldTypes#FixedString string}.
	 *
	 * @param str string to write
	 * @throws IllegalArgumentException {@code str} is malformed
	 */
	void writeFixedString(String str) {
		for (byte chr : str.getBytes(StandardCharsets.US_ASCII)) {
			Preconditions.checkArgument(chr >= 65);
			this.writeFixedInt(6, (chr & 0xff) - 65);
		}
	}

	/**
	 * Read fixed length {@link FieldTypes#FixedBitfield bitfield}.
	 *
	 * @param len length, in bits, of bitfield
	 * @return read value
	 * @throws IndexOutOfBoundsException read overflows underlying buffer
	 */
	SparseBooleanArray readFixedBitfield(int len) {
		return this.bits.extractSparse(this.advance(len), len);
	}

	/**
	 * Write fixed length {@link FieldTypes#FixedBitfield bitfield}.
	 *
	 * @param len length, in bits, of bitfield
	 * @param val value to write
	 */
	void writeFixedBitfield(int len, SparseBooleanArray val) {
		if (len > 0) {
			this.ensureCapacity(len)
				.insertSparse(this.advance(len), val, len);
		}
	}

	private SparseBooleanArray readBitfield(int len) {
		return this.bits.extractSparse(this.advance(len), len);
	}

	/**
	 * Read variable length {@link FieldTypes#Bitfield bitfield}.
	 *
	 * @return read value
	 * @throws IndexOutOfBoundsException read overflows underlying buffer
	 */
	SparseBooleanArray readBitfield() {
		return this.readBitfield(this.readFixedInt(16));
	}

	/**
	 * Find highest set bit within a sparse {@code boolean} array.
	 *
	 * @param val array to search
	 * @return highest set bit or {@code -1} if all bits are clear
	 */
	private static int highestSetBitOf(SparseBooleanArray val) {
		int size = val.size();

		for (int i = size - 1; i >= 0; i--) {
			if (val.valueAt(i))
				return val.keyAt(i);
		}
		return -1;
	}

	/**
	 * Write variable length {@link FieldTypes#Bitfield bitfield}.
	 *
	 * @param val value to write
	 */
	void writeBitfield(SparseBooleanArray val) {
		int len = highestSetBitOf(val);

		this.ensureCapacity(len + 16);
		this.writeFixedInt(16, len);
		this.writeFixedBitfield(len, val);
	}

	/**
	 * Read an {@code int} range.
	 *
	 * @param fib {@code true} if, and only if, {@code int} values are Fibonacci coded
	 * @return resulting range
	 */
	private int[] readIntRange(boolean fib) {
		int num = this.readFixedInt(12);
		IntBuffer rv = IntBuffer.allocate(num);
		int last = 0;

		for (int i = 0; i < num; i++) {
			boolean group = this.readBoolean();
			int start = fib ? this.readFibonacciInt() + last : this.readFixedInt(16);
			int end =
				group ? (fib ? this.readFibonacciInt() + start : this.readFixedInt(16)) :
					start;

			int len = (end - start) + 1;

			Preconditions.checkState(end >= start);
			if (rv.remaining() < len) {
				rv = IntBuffer.allocate(rv.capacity() + len)
					.put((IntBuffer) rv.flip());
			}
			for (int j = start; j <= end; j++)
				rv.put(j);
			last = end;
		}
		return Arrays.copyOfRange(rv.array(), 0, rv.position());
	}

	/**
	 * Read range of fixed 16-bit {@link FieldTypes#FixedInt int} values.
	 *
	 * @return read value
	 * @throws IllegalStateException range is malformed
	 * @throws IndexOutOfBoundsException read overflows underlying buffer
	 */
	int[] readFixedInt16Range() {
		return this.readIntRange(false);
	}

	/**
	 * Read range of variable length {@link FieldTypes#FibonacciInt int} values.
	 *
	 * @return read value
	 * @throws IllegalStateException range is malformed
	 * @throws IndexOutOfBoundsException read overflows underlying buffer
	 */
	int[] readFibonacciIntRange() {
		return this.readIntRange(true);
	}

	/**
	 * Construct an {@code int} range, if possible.
	 *
	 * @param vals {@code int} values to build range of
	 * @return array of {@linkplain Point points} if a range is possible; otherwise, {@code vals}
	 * @throws IllegalArgumentException {@code vals} is not in sort order
	 */
	private static Object intRangeOf(int[] vals) {
		if (vals.length == 0)
			return vals;

		ArrayList<Point> ranges = new ArrayList<>();

		ranges.add(new Point(vals[0], vals[0]));
		for (int i = 1; i < vals.length; i++) {
			Point range = ranges.get(ranges.size() - 1);
			int y = vals[i];

			if (range.y == (y - 1)) {
				range.y = y;
			} else {
				Preconditions.checkArgument(y > range.y);
				//noinspection SuspiciousNameCombination
				ranges.add(new Point(y, y));
			}
		}
		return ranges.size() == vals.length ? vals : ranges.toArray(new Point[0]);
	}

	/**
	 * Write grouped {@code int} range.
	 *
	 * @param fib {@code true} if, and only if, {@code int} values are Fibonacci coded
	 * @param ranges ranges to write
	 * @throws IllegalArgumentException {@code fib} is {@code true} and a value in {@code ranges}
	 * is less than {@code 1}
	 */
	private void writeGroupedIntRange(boolean fib, Point[] ranges) {
		this.ensureCapacity(
			12 +
			(ranges.length * 1) + // booleans
			(ranges.length * 2 * (fib ? FIBONACCI_TABLE.length + 1 : 16)) // ints
		);

		int last = 0;

		for (Point range : ranges) {
			boolean unit = range.x == range.y;

			this.writeBoolean(!unit);
			if (fib) {
				this.writeFibonacciInt(range.x - last);
				last = range.x;
			} else {
				this.writeFixedInt(16, range.x);
			}
			if (!unit) {
				if (fib) {
					this.writeFibonacciInt(range.y - last);
					last = range.y;
				} else {
					//noinspection SuspiciousNameCombination
					this.writeFixedInt(16, range.y);
				}
			}
		}
	}

	/**
	 * Write unit {@code int} range.
	 *
	 * @param fib {@code true} if, and only if, {@code int} values are Fibonacci coded
	 * @param vals values to write
	 * @throws IllegalArgumentException {@code fib} is {@code true} and a value in {@code vals}
	 * is less than {@code 1}
	 */
	private void writeUnitIntRange(boolean fib, int[] vals) {
		this.ensureCapacity(
			12 +
			(vals.length * 1) + // booleans
			(vals.length * (fib ? FIBONACCI_TABLE.length + 1 : 16)) // ints
		);

		int last = 0;

		for (int val : vals) {
			this.writeBoolean(false);
			if (fib) {
				this.writeFibonacciInt(val - last);
				last = val;
			} else {
				this.writeFixedInt(16, val);
			}
		}
	}

	/**
	 * Write {@linkplain #writeGroupedIntRange(boolean, Point[]) grouped} or {@linkplain
	 * #writeUnitIntRange(boolean, int[]) unit} {@code int} range.
	 *
	 * @param fib {@code true} if, and only if, {@code int} values are Fibonacci coded
	 * @param vals values to write
	 * @throws IllegalArgumentException {@code fib} is {@code true} and a value in {@code vals}
	 * is less than {@code 1}, or {@code vals} is not in sort order
	 */
	private void writeIntRange(boolean fib, int[] vals) {
		Object ranges = intRangeOf(vals);

		if (ranges instanceof Point[]) {
			this.writeFixedInt(12, ((Point[]) ranges).length);
			this.writeGroupedIntRange(fib, (Point[]) ranges);
		} else {
			this.writeFixedInt(12, vals.length);
			this.writeUnitIntRange(fib, vals);
		}
	}

	/**
	 * Write range of fixed 16-bit {@link FieldTypes#FixedInt int} values.
	 *
	 * @param vals values to write
	 * @throws IllegalArgumentException {@code vals} is not in sort order
	 */
	void writeFixedInt16Range(int[] vals) {
		this.writeIntRange(false, vals);
	}

	/**
	 * Write range of variable length {@link FieldTypes#FibonacciInt int} values.
	 *
	 * @param vals values to write
	 * @throws IllegalArgumentException value in {@code vals} is less than {@code 1}, or {@code
	 * vals} is not in sort order
	 */
	void writeFibonacciIntRange(int[] vals) {
		this.writeIntRange(true, vals);
	}

	/**
	 * Range of {@code int}s using {@link FieldTypes#FibonacciIntRange}, {@link
	 * FieldTypes#FixedInt16Range} or {@link FieldTypes#Bitfield}.
	 *
	 * @param fib {@code true} if, and only if, range is {@link FieldTypes#FibonacciIntRange}
	 * @return read value
	 * @throws IllegalStateException range is malformed
	 * @throws IndexOutOfBoundsException read overflows underlying buffer
	 */
	private int[] readOptimizedIntRange(boolean fib) {
		int max = fib ? -1 : this.readFixedInt(16);

		if (this.readBoolean())
			return this.readIntRange(fib);

		SparseBooleanArray bits = fib ? this.readBitfield() : this.readBitfield(max);
		IntBuffer rv = IntBuffer.allocate(bits.size());

		for (int i = 0; i < bits.size(); i++) {
			if (bits.valueAt(i))
				rv.put(bits.keyAt(i) + 1);
		}
		return Arrays.copyOfRange(rv.array(), 0, rv.position());
	}

	/**
	 * Range of {@code int}s using either {@link FieldTypes#FibonacciIntRange} or {@link
	 * FieldTypes#Bitfield}.
	 *
	 * @return read value
	 * @throws IllegalStateException range is malformed
	 * @throws IndexOutOfBoundsException read overflows underlying buffer
	 */
	int[] readOptimizedIntRange() {
		return this.readOptimizedIntRange(true);
	}

	/**
	 * Range of {@code int}s using either {@link FieldTypes#FixedInt16Range} or {@link
	 * FieldTypes#Bitfield}.
	 *
	 * @return read value
	 * @throws IllegalStateException range is malformed
	 * @throws IndexOutOfBoundsException read overflows underlying buffer
	 */
	int[] readOptimizedIntRange2() {
		return this.readOptimizedIntRange(false);
	}

	/**
	 * Write range of {@code ints} using {@link FieldTypes#FibonacciIntRange}, {@link
	 * FieldTypes#FixedInt16Range}, or {@link FieldTypes#Bitfield}.
	 *
	 * @param fib {@code true} if, and only if, Fibonacci coding can be used
	 * @param vals values to write
	 * @throws IllegalArgumentException {@code int} value in {@code vals} is less than {@code 1}
	 */
	private void writeOptimizedIntRange(boolean fib, int[] vals) {
		int numGroups = 0;
		int numUnits = 1;
		int lastGroupEnd = -1;

		for (int i = 0; i < vals.length; i++) {
			int val = vals[i];

			Preconditions.checkArgument(val > 0 && (i == 0 || val > vals[i - 1]));
			if (lastGroupEnd == 0) {
				if (val == (lastGroupEnd + 1)) {
					++numGroups;
				} else {
					++numUnits;
					lastGroupEnd = -1;
				}
			}
			if (i == 0 || val == (lastGroupEnd + 1)) {
				lastGroupEnd = val;
			} else if (val == (vals[i - 1] + 1)) {
				numGroups++;
				lastGroupEnd = vals[i - 1];
			} else {
				++numUnits;
			}
		}

		int max = vals.length == 0 ? 0 : vals[vals.length - 1];
		int bitsPerUnit = fib ? 2 : 16;
		int rangeSize =
			12 +
			(numGroups * 2 * bitsPerUnit) +
			(numUnits * bitsPerUnit) +
			numGroups +
			numUnits;
		boolean useBits = max <= rangeSize;

		if (!fib)
			this.writeFixedInt(16, max);
		if (!useBits) {
			this.writeBoolean(true);
			this.writeIntRange(fib, vals);
		} else {
			SparseBooleanArray arr = new SparseBooleanArray(max);

			for (int val : vals)
				arr.append(val - 1, true);
			this.writeBoolean(false);
			if (!fib)
				this.writeFixedBitfield(max, arr);
			else
				this.writeBitfield(arr);
		}
	}

	/**
	 * Write range of {@code ints} using {@link FieldTypes#FibonacciIntRange} or {@link
	 * FieldTypes#Bitfield}.
	 *
	 * @param vals values to write
	 * @throws IllegalArgumentException {@code int} value in {@code vals} is less than {@code 1}
	 */
	void writeOptimizedIntRange(int[] vals) {
		this.writeOptimizedIntRange(true, vals);
	}

	/**
	 * Write range of {@code ints} using {@link FieldTypes#FixedInt16Range} or {@link
	 * FieldTypes#Bitfield}.
	 *
	 * @param vals values to write
	 * @throws IllegalArgumentException {@code int} value in {@code vals} is less than {@code 1}
	 */
	void writeOptimizedIntRange2(int[] vals) {
		this.writeOptimizedIntRange(false, vals);
	}

	/**
	 * Read fixed or variable {@link TaggedIds} array.
	 *
	 * @param fixed {@code true} if, and only if, Fibonacci coding of id ranges is permitted
	 * @param keyLen length, in bits, of key tags
	 * @param typeLen length, in bits, of type tags
	 * @return read value
	 * @throws IllegalStateException range is malformed
	 * @throws IndexOutOfBoundsException read overflows underlying buffer
	 */
	private TaggedIds[] readArrayOfIntRanges(boolean fixed, int keyLen, int typeLen) {
		int num = this.readFixedInt(12);
		TaggedIds[] table = new TaggedIds[num];

		for (int i = 0; i < num; i++) {
			table[i] =
				new TaggedIds(
					this.readFixedInt(keyLen),
					this.readFixedInt(typeLen),
					fixed ? this.readFibonacciIntRange() : this.readFixedInt16Range()
				);
		}
		return table;
	}

	/**
	 * Read array of {@link FieldTypes#ArrayOfIntRanges tagged int ranges}.
	 *
	 * @return read value
	 * @throws IllegalStateException range is malformed
	 * @throws IndexOutOfBoundsException read overflows underlying buffer
	 */
	TaggedIds[] readArrayOfIntRanges() {
		return this.readArrayOfIntRanges(false, 6, 2);
	}

	/**
	 * Read array of {@link FieldTypes#ArrayOfFixedIntRanges fixed tagged int ranges}.
	 *
	 * @param keyLen length, in bits, of key tags
	 * @param typeLen length, in bits, of type tags
	 * @return read value
	 * @throws IllegalStateException range is malformed
	 * @throws IndexOutOfBoundsException read overflows underlying buffer
	 */
	TaggedIds[] readArrayOfFixedIntRanges(int keyLen, int typeLen) {
		return this.readArrayOfIntRanges(true, keyLen, typeLen);
	}

	/**
	 * Write fixed or variable length {@link TaggedIds} array.
	 *
	 * @param fixed {@code true} if, and only if, Fibonacci coding of id ranges is permitted
	 * @param keyLen length, in bits, of key tags
	 * @param typeLen length, in bits, of type tags
	 * @param vals values to write
	 */
	private void writeArrayOfIntRanges(boolean fixed, int keyLen, int typeLen, TaggedIds[] vals) {
		this.writeFixedInt(12, vals.length);

		for (TaggedIds val : vals) {
			this.writeFixedInt(keyLen, val.key());
			this.writeFixedInt(typeLen, val.type());
			if (fixed)
				this.writeFibonacciIntRange(val.ids());
			else
				this.writeFixedInt16Range(val.ids());
		}
	}

	/**
	 * Write array of {@link FieldTypes#ArrayOfIntRanges tagged int ranges}.
	 *
	 * @param vals values to write
	 */
	void writeArrayOfIntRanges(TaggedIds[] vals) {
		this.writeArrayOfIntRanges(false, 6, 2, vals);
	}

	/**
	 * Write array of {@link FieldTypes#ArrayOfFixedIntRanges fixed tagged int ranges}.
	 *
	 * @param vals values to write
	 */
	void writeArrayOfFixedIntRanges(int keyLen, int typeLen, TaggedIds[] vals) {
		this.writeArrayOfIntRanges(true, keyLen, typeLen, vals);
	}

	/**
	 * Read an array of fixed-length {@code int} values.
	 *
	 * @param len length, in bits, of each {@code int} value
	 * @param num number of values to read
	 * @return read value
	 * @throws IndexOutOfBoundsException read overflows underlying buffer
	 */
	int[] readFixedIntList(int len, int num) {
		int[] rv = new int[num];

		for (int i = 0; i < num; i++)
			rv[i] = this.readFixedInt(len);
		return rv;
	}

	/**
	 * Write an array of {@code int} values as an array of fixed-length values.
	 *
	 * @param len length, in bits, of each {@code int} value
	 * @param vals value to write
	 */
	void writeFixedIntList(int len, int[] vals) {
		for (int val : vals)
			this.writeFixedInt(len, val);
	}
}
