// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.util;

import android.util.SparseBooleanArray;

import androidx.annotation.Nullable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.LongBuffer;
import java.util.Arrays;

/**
 * Dense mapping of bit positions to {@code boolean} value.
 *
 * @since 0.2
 */
@SuppressWarnings("UnusedReturnValue")
public final class Bitmap {

	private static final Bitmap EMPTY = new Bitmap(new long[0], 0, true);
	private static final Bitmap EMPTY_WRITABLE = new Bitmap(EMPTY.words, 0, false);

	/**
	 * Calculate number of words required to represent bits.
	 *
	 * @param bit number of bits to represent
	 * @param len size of a word, in bits
	 * @return number of {@code len} words required to represent {@code bit} bits
	 */
	private static int wordSizeOf(int bit, int len) {
		return (bit + len - 1) / len;
	}

	/**
	 * Calculate word index for a bit position.
	 *
	 * @param bit bit position
	 * @return word index
	 */
	private static int wordIndexOf(int bit) {
		return bit / Bits.SIZE_OF_LONG;
	}

	/**
	 * Calculate bit offset within word for a bit position.
	 *
	 * @param bit bit position
	 * @return word offset
	 */
	private static int wordOffsetOf(int bit) {
		return bit % Bits.SIZE_OF_LONG;
	}

	/**
	 * Create word mask for a bit position.
	 *
	 * @param bit bit position
	 * @return word mask containing only a single bit set for {@code bit}
	 */
	private static long wordMaskOf(int bit) {
		return Bits.longMaskOf(wordOffsetOf(bit));
	}

	/**
	 * Create word mask for a head bit position.
	 *
	 * @param bit head bit position
	 * @return word mask containing bits set starting at head bit {@code bit}
	 */
	private static long wordMaskOfHead(int bit) {
		return ~0L << (bit & (Bits.SIZE_OF_LONG - 1));
	}

	/**
	 * Construct word mask for a tail bit position.
	 *
	 * @param size tail bit position
	 * @return word mask containing bits set starting at {@code 0} and ending at tail bit {@code
	 * size}
	 */
	private static long wordMaskOfTail(int size) {
		return ~0L >>> (-size & (Bits.SIZE_OF_LONG - 1));
	}

	/**
	 * Empty bitmap with a {@linkplain #size() size} of {@code 0}.
	 *
	 * @return empty bitmap instance
	 * @since 1.2
	 */
	public static Bitmap of() {
		return EMPTY;
	}

	/**
	 * Construct a new empty bitmap.
	 *
	 * @param size bitmap size
	 * @return resulting bitmap
	 * @throws IllegalArgumentException {@code size} is negative
	 * @since 0.2
	 */
	public static Bitmap of(int size) {
		return size == 0 ? EMPTY_WRITABLE : new Bitmap(
			new long[wordSizeOf(size, Bits.SIZE_OF_LONG)],
			size,
			false
		);
	}

	/**
	 * Construct a new bitmap initialized from a {@code boolean} array.
	 * <p>The bitmap returned has a {@linkplain #size() size} equal to {@code bools.length}. Each
	 * bit {@code i} of the bitmap will be equal to the {@code i}-th value of {@code bools}. This
	 * is a shorthand for:
	 * {@snippet lang="java" :
	 * Bitmap bits = Bitmap.of(bools.length); // @link substring="Bitmap.of" target="#of(int)"
	 *
	 * for (int i = 0; i < bools.length; i++)
	 *     bits.put(i, bools[i]);
	 * return bits;
	 * }
	 *
	 * @param bools array to construct bitmap from
	 * @return resulting bitmap
	 * @since 1.1
	 */
	@SuppressWarnings("JavadocDeclaration")
	public static Bitmap of(boolean[] bools) {
		Bitmap rv = of(bools.length);

		for (int i = 0; i < bools.length; i++)
			rv.put(i, bools[i]);
		return rv;
	}

	/**
	 * Construct a new bitmap initialized from a buffer.
	 * <p>The bitmap returned has a {@linkplain #size() size} equal to the {@linkplain
	 * ByteBuffer#remaining() remaining} capacity of {@code buff} multiplied by the size of a
	 * {@link Bits#SIZE_OF_BYTE byte}, in bits. Bits within the bitmap are initialized from
	 * {@code buff}. Upon return, the {@linkplain ByteBuffer#position() position} of {@code buff}
	 * will equal its {@linkplain ByteBuffer#limit() limit}. This is an efficient equivalent of:
	 * {@snippet lang="java" :
	 * Bitmap bits = Bitmap.of(buff.remaining() / Bits.SIZE_OF_BYTE); // @link substring="Bitmap.of" target="#of(int)"
	 * int bit = 0;
	 *
	 * while (buff.hasRemaining()) {
	 *     int val = buff.get() & 0xff;
	 *
	 *     for (int i = 0; i < Bits.SIZE_OF_BYTE; i++, bit++) {
	 *         if ((val & Bits.intMaskOf(i)) != 0)
	 *             bits.set(bit);
	 *     }
	 * }
	 * return bits;
	 * }
	 *
	 * @param buff buffer to initialize from
	 * @return resulting bitmap
	 * @since 0.2
	 */
	@SuppressWarnings("JavadocDeclaration")
	public static Bitmap ofBuffer(ByteBuffer buff) {
		buff = buff.slice()
			.order(ByteOrder.LITTLE_ENDIAN);

		LongBuffer words = buff.asLongBuffer();
		Bitmap bits = of(buff.remaining() * Bits.SIZE_OF_BYTE);
		int pos = words.remaining() * (Bits.SIZE_OF_LONG / Bits.SIZE_OF_BYTE);
		int rem = buff.position(pos).remaining();

		words.get(bits.words, 0, words.remaining());

		if (rem > 0) {
			long word = 0;

			for (int i = rem; --i >= 0;)
				word = (word << Bits.SIZE_OF_BYTE) | (buff.get(pos + i) & 0xffL);
			bits.words[bits.words.length - 1] = word;
			buff.position(pos + rem);
		}
		return bits;
	}

	private final long[] words;
	private final int size;
	private final boolean readOnly;

	private Bitmap(long[] words, int size, boolean readOnly) {
		this.words = words;
		this.size = size;
		this.readOnly = readOnly;
	}

	/**
	 * Ensure bit access is valid.
	 *
	 * @param bit bit position to check
	 * @return {@code bit}
	 * @throws IndexOutOfBoundsException {@code bit} is negative or, greater than or equal to
	 * bitmap {@linkplain #size size}
	 */
	private int checkBit(int bit) {
		return Preconditions.checkIndex(bit, this.size);
	}

	/**
	 * Ensure word access is valid.
	 *
	 * @param bit bit position access begins from (inclusive)
	 * @param len length, in bits, of each accessed word
	 * @param num number of words accessed
	 * @return {@code len * num}
	 * @throws IllegalArgumentException {@code len} is less than {@code 1} or greater than the
	 * word {@linkplain Bits#SIZE_OF_LONG size}
	 * @throws IndexOutOfBoundsException {@code bit}, {@code len}, or {@code num} is negative or,
	 * {@code bit + (len * num)} is greater than bitmap {@linkplain #size size}
	 */
	private int checkWord(int bit, int len, int num) {
		int size = len * num;

		Preconditions.checkArgument(len > 0 && len <= Bits.SIZE_OF_LONG);
		Preconditions.checkFromIndexSize(bit, size, this.size);
		return size;
	}

	/**
	 * Ensure bitmap is writable.
	 *
	 * @throws IllegalStateException bitmap is read-only
	 */
	private void checkWritable() {
		Preconditions.checkState(!this.readOnly);
	}

	/**
	 * Test whether bitmap is read-only.
	 * <p>If this returns {@code true}, then the contents of the bitmap cannot be modified.
	 *
	 * @return {@code true} if, and only if, bitmap is read-only
	 * @since 1.1
	 */
	public boolean isReadOnly() {
		return this.readOnly;
	}

	/**
	 * Construct read-only view of bitmap.
	 * <p>The view returned shares the same underlying storage as {@code this}, but does not allow
	 * any modifications. If {@code this} is alread {@linkplain #isReadOnly() read-only}, then
	 * {@code this} is returned.
	 *
	 * @return bitmap view
	 * @since 1.1
	 * @see #isReadOnly()
	 */
	public Bitmap asReadOnly() {
		return this.readOnly ? this : new Bitmap(this.words, this.size, true);
	}

	/**
	 * Construct a new bitmap with contents copied from this bitmap.
	 * <p>The bitmap returned will have a size of {@code newSize}, and its bits {@code 0}
	 * (inclusive) to {@code n} (exclusive), where {@code n} is the smaller of {@code newSize} and
	 * the {@linkplain #size() size} of this bitmap, will be equal to the bits of this bitmap, all
	 * remaining bits, if any, will be cleared.
	 *
	 * @param newSize new size
	 * @return resulting bitmap
	 * @throws IllegalArgumentException {@code newSize} is negative
	 * @since 0.2
	 */
	public Bitmap withSize(int newSize) {
		Bitmap that = of(newSize);

		System.arraycopy(
			this.words, 0,
			that.words, 0,
			Math.min(this.words.length, that.words.length)
		);
		return that;
	}

	/**
	 * Construct duplicate of {@code this}.
	 * <p>The duplicate returned will have the same contents as {@code this}, but will be backed
	 * with independent storage.
	 *
	 * @return duplicate instance
	 * @since 1.1
	 */
	public Bitmap duplicate() {
		return new Bitmap(this.words.clone(), this.size, this.readOnly);
	}

	/**
	 * Construct {@code byte} array of all bit values, in little-endian order.
	 * <p>The array returned will contain bits {@code 0} (inclusive) to {@code len} (exclusive)
	 * of this bitmap.
	 *
	 * @param len number of bits to include
	 * @return resulting array
	 * @throws IllegalArgumentException {@code len} is negative or greater than bitmap {@linkplain
	 * #size() size}
	 * @since 0.2
	 */
	public byte[] toByteArray(int len) {
		int size = (this.checkWord(0, 1, len) + (Bits.SIZE_OF_BYTE - 1)) / Bits.SIZE_OF_BYTE;
		ByteBuffer rv = ByteBuffer.allocate(size)
			.order(ByteOrder.LITTLE_ENDIAN);
		int idx = 0;

		while (rv.remaining() >= (Bits.SIZE_OF_LONG / Bits.SIZE_OF_BYTE))
			rv.putLong(this.words[idx++]);
		if (rv.hasRemaining()) {
			long word = this.words[idx] & wordMaskOfTail(len);

			do {
				rv.put((byte) (word & 0xff));
				word >>>= 8;
			} while (rv.hasRemaining());
		}
		return rv.array();
	}

	/**
	 * Construct {@code boolean} array of all bit values.
	 * <p>The array returned will have a length equal to the {@linkplain #size() size} of this
	 * bitmap, with each element {@code i} equal to the value of the {@code i}-th bit of this
	 * bitmap.
	 *
	 * @return resulting array
	 * @since 1.1
	 */
	public boolean[] toBooleanArray() {
		boolean[] rv = new boolean[this.size];

		for (int i = 0; i < rv.length; i++)
			rv[i] = this.test(i);
		return rv;
	}

	/**
	 * Size of bitmap.
	 *
	 * @return bitmap size
	 * @since 0.2
	 */
	public int size() {
		return this.size;
	}

	/**
	 * Number of underlying {@link Bits#SIZE_OF_LONG long} words.
	 *
	 * @return underlying word count
	 * @since 0.2
	 */
	public int wordCount() {
		return this.words.length;
	}

	/**
	 * Retrieve underlying word at index.
	 *
	 * @param i word index
	 * @return word value
	 * @throws IndexOutOfBoundsException {@code i} is negative or, greater than or equal to
	 * word {@linkplain #wordCount() count}
	 * @since 0.2
	 */
	public long word(int i) {
		return this.words[i];
	}

	/**
	 * Set or clear bit.
	 *
	 * @param bit position of bit to set or clear
	 * @param val {@code true} or {@code false} if bit should be set or cleared, respectively
	 * @throws IllegalStateException bitmap is {@linkplain #isReadOnly() read-only}
	 * @throws IndexOutOfBoundsException {@code bit} is negative or, greater than or equal to
	 * bitmap {@linkplain #size() size}
	 * @since 0.2
	 * @see #clear(int)
	 * @see #set(int)
	 */
	public void put(int bit, boolean val) {
		this.checkWritable();

		int idx = wordIndexOf(this.checkBit(bit));
		long word = this.words[idx];
		long mask = wordMaskOf(bit);

		this.words[idx] = val ? (word | mask) : (word & ~mask);
	}

	/**
	 * Set bit.
	 *
	 * @param bit position of bit to set
	 * @throws IllegalStateException bitmap is {@linkplain #isReadOnly() read-only}
	 * @throws IndexOutOfBoundsException {@code bit} is negative or, greater than or equal to
	 * bitmap {@linkplain #size() size}
	 * @since 0.2
	 * @see #put(int, boolean)
	 */
	public void set(int bit) {
		this.put(bit, true);
	}

	/**
	 * Clear bit.
	 *
	 * @param bit position of bit to clear
	 * @throws IllegalStateException bitmap is {@linkplain #isReadOnly() read-only}
	 * @throws IndexOutOfBoundsException {@code bit} is negative or, greater than or equal to
	 * bitmap {@linkplain #size() size}
	 * @since 0.2
	 * @see #put(int, boolean)
	 */
	public void clear(int bit) {
		this.put(bit, false);
	}

	/**
	 * Toggle bit.
	 * <p>Efficient equivalent of:
	 * {@snippet lang="java" :
	 * put( // @link substring="put" target="#put(int, boolean)"
	 *     bit,
	 *     !test(bit) // @link substring="test" target="#test(int)"
	 * );
	 * }
	 *
	 * @param bit position of bit to clear
	 * @return previous value of bit
	 * @throws IllegalStateException bitmap is {@linkplain #isReadOnly() read-only}
	 * @throws IndexOutOfBoundsException {@code bit} is negative or, greater than or equal to
	 * bitmap {@linkplain #size() size}
	 * @since 0.2
	 */
	@SuppressWarnings("JavadocDeclaration")
	public boolean toggle(int bit) {
		this.checkWritable();

		int idx = wordIndexOf(this.checkBit(bit));
		long mask = wordMaskOf(bit);
		long word = this.words[idx] ^ mask;

		this.words[idx] = word;
		return (word & mask) == 0L;
	}

	/**
	 * Test whether bit is {@linkplain #set(int) set} or {@linkplain #clear(int) clear}.
	 *
	 * @param bit position of bit to test
	 * @return {@code true} or {@code false} if bit at position {@code bit} is set or clear,
	 * respectively
	 * @throws IndexOutOfBoundsException {@code bit} is negative or, greater than or equal to
	 * bitmap {@linkplain #size() size}
	 * @since 0.2
	 */
	public boolean test(int bit) {
		this.checkBit(bit);
		return (this.words[wordIndexOf(bit)] & wordMaskOf(bit)) != 0L;
	}

	/**
	 * Extract up to {@linkplain Bits#SIZE_OF_LONG word} bits.
	 * <p>The word returned will have its bit {@code i}, counting from {@code 0} (inclusive) up to
	 * {@code len} (exclusive), set to bit {@code bit + i} of this bitmap. Remaining bits of the
	 * returned word will equal {@code 0}. This is an efficient equivalent of:
	 * {@snippet lang="java" :
	 * long word = 0;
	 *
	 * for (int i = 0; i < len; i++) {
	 *     if (test(bit + i)) // @link substring="test" target="#test(int)"
	 *         word |= Bits.longMaskOf(i);
	 * }
	 * return word;
	 * }
	 *
	 * @param bit position to begin extracting from (inclusive)
	 * @param len number of bits to extract
	 * @return resulting word or {@code 0} if {@code len} is {@code 0}
	 * @throws IllegalArgumentException {@code len} is less than {@code 1} or greater than the
	 * word {@linkplain Bits#SIZE_OF_LONG size}
	 * @throws IndexOutOfBoundsException {@code bit} is negative or, {@code bit + len} is greater
	 * than bitmap {@linkplain #size() size}
	 * @since 0.2
	 * @see #insertWord(int, long, int)
	 */
	@SuppressWarnings("JavadocDeclaration")
	public long extractWord(int bit, int len) {
		if (this.checkWord(bit, len, 1) == 0)
			return 0;

		int idx = wordIndexOf(bit);
		int off = wordOffsetOf(bit);
		int rem = Bits.SIZE_OF_LONG - off;

		if (rem >= len) {
			// Extracting from a single word.
			return (this.words[idx] >>> off) & wordMaskOfTail(len);
		}

		long head = this.words[idx] & wordMaskOfHead(bit);
		long tail = this.words[idx + 1] & wordMaskOfTail(bit + len);

		return (head >>> off) | (tail << rem);
	}

	/**
	 * Insert up to {@linkplain Bits#SIZE_OF_LONG word} bits.
	 * <p>Upon return, bit {@code i} of {@code word}, counting from {@code 0} (inclusive) up to
	 * {@code len} (exclusive), will be stored at position {@code bit + i} within this bitmap.
	 * This is an efficient equivalent of:
	 * {@snippet lang="java" :
	 * for (int i = 0; i < len; i++) {
	 *     put(bit + i, (word & Bits.longMaskOf(i)) != 0); // @link substring="put" target="#put(int, boolean)"
	 * }
	 * }
	 *
	 * @param bit position to begin inserting from (inclusive)
	 * @param word word to insert bits of
	 * @param len number of bits to insert
	 * @throws IllegalStateException bitmap is {@linkplain #isReadOnly() read-only}
	 * @throws IllegalArgumentException {@code len} is less than {@code 1} or greater than the
	 * word {@linkplain Bits#SIZE_OF_LONG size}
	 * @throws IndexOutOfBoundsException {@code bit} is negative or, {@code bit + len} is greater
	 * than bitmap {@linkplain #size() size}
	 * @since 0.2
	 * @see #extractWord(int, int)
	 */
	@SuppressWarnings("JavadocDeclaration")
	public void insertWord(int bit, long word, int len) {
		this.checkWritable();
		if (this.checkWord(bit, len, 1) == 0)
			return;

		int idx = wordIndexOf(bit);
		int off = wordOffsetOf(bit);
		int rem = Bits.SIZE_OF_LONG - off;
		long wmask = wordMaskOfTail(len);

		word &= wmask;
		if (rem >= len) {
			// Inserting into a single word.
			this.words[idx] = (this.words[idx] & ~(wmask << off)) | (word << off);
		} else {
			this.words[idx] = (this.words[idx] & ~wordMaskOfHead(bit)) | (word << off);
			this.words[idx + 1] =
				(this.words[idx + 1] & wordMaskOfHead(bit + len)) |
				(word >>> rem);
		}
	}

	/**
	 * Extract zero or more words, of up to {@linkplain Bits#SIZE_OF_LONG word} bits.
	 * <p>This extracts {@code len}-bit words into {@code words}, where each {@code len}-bit word
	 * is aligned to a {@code byte} boundary. At most {@code n} words are extracted, where {@code
	 * n} is equal to {@linkplain ByteBuffer#remaining() remaining} capacity of {@code words}
	 * divided by {@code len} aligned to a {@code byte} boundary. Upon return, the {@linkplain
	 * ByteBuffer#position() position} of {@code words} is updated to reflect the first
	 * <i>unwritten</i> {@code byte}. This is an efficient equivalent of:
	 * {@snippet lang="java" :
	 * // Number of bytes to represent a `len`-bit word:
	 * int numBytes = (len + Bits.SIZE_OF_BYTE - 1) / Bits.SIZE_OF_BYTE;
	 * int numWords = words.remaining() / numBytes;
	 *
	 * for (int i = 0; i < numWords; i++) {
	 *     for (int j = 0, rem = len; j < numBytes; j++, rem -= Bits.SIZE_OF_BYTE) {
	 *         int val = 0;
	 *
	 *         for (int k = 0; k < Math.min(rem, Bits.SIZE_OF_BYTE); k++, bit++) {
	 *             if (test(bit)) // @link substring="test" target="#test(int)"
	 *                 val |= Bits.intMaskOf(k);
	 *         }
	 *         words.put((byte) (val & 0xff));
	 *     }
	 * }
	 * }
	 *
	 * @param bit position to begin extracting from (inclusive)
	 * @param words buffer to extract words into
	 * @param len length of a word, in bits
	 * @throws IllegalArgumentException {@code len} is less than {@code 1} or greater than the
	 * word {@linkplain Bits#SIZE_OF_LONG size}
	 * @throws IndexOutOfBoundsException {@code bit} is negative or, {@code bit + len * num}, where
	 * {@code num} is equal to the number of words that can be stord in {@code words}, is greater
	 * than bitmap {@linkplain #size() size}
	 * @since 0.2
	 * @see #insertWords(int, ByteBuffer, int)
	 */
	@SuppressWarnings("JavadocDeclaration")
	public void extractWords(int bit, ByteBuffer words, int len) {
		int bytesPerWord = wordSizeOf(len, Bits.SIZE_OF_BYTE);
		int numWords = words.remaining() / bytesPerWord;
		int size = this.checkWord(bit, len, numWords);

		if (size == 0)
			return;

		for (int i = 0; i < numWords; i++) {
			long word = this.extractWord(bit + (len * i), len);

			for (int j = 0; j < bytesPerWord; j++) {
				words.put((byte) (word & 0xffL));
				word >>>= Bits.SIZE_OF_BYTE;
			}
		}
	}

	/**
	 * Extract zero or more words, of up to {@linkplain Bits#SIZE_OF_LONG word} bits, into a newly
	 * allocated buffer.
	 * <p>Shorthand for:
	 * {@snippet lang="java" :
	 * // Number of bytes to represent a `len`-bit word:
	 * int numBytes = (len + Bits.SIZE_OF_BYTE - 1) / Bits.SIZE_OF_BYTE;
	 * ByteBuffer words = ByteBuffer.allocate(num * numBytes);
	 *
	 * extractWords(bit, words, len); // @link substring="extractWords" target="#extractWords(int, ByteBuffer, int)"
	 * words.flip();
	 * return words;
	 * }
	 *
	 * @param bit position to begin extracting from (inclusive)
	 * @param num number of {@code len}-bit words to extract
	 * @param len length of a word, in bits
	 * @return word buffer
	 * @throws IllegalArgumentException {@code len} is less than {@code 1} or greater than the
	 * word {@linkplain Bits#SIZE_OF_LONG size}
	 * @throws IndexOutOfBoundsException {@code bit} or {@code num} is negative, or {@code bit +
	 * len * num} is greater than bitmap {@linkplain #size() size}
	 * @since 0.2
	 * @see #extractWords(int, ByteBuffer, int)
	 */
	@SuppressWarnings("JavadocDeclaration")
	public ByteBuffer extractWords(int bit, int num, int len) {
		ByteBuffer words = ByteBuffer.allocate(wordSizeOf(len, Bits.SIZE_OF_BYTE) * num);

		this.extractWords(bit, words, len);
		words.flip();
		return words;
	}

	/**
	 * Insert zero or more words, of up to {@linkplain Bits#SIZE_OF_LONG word} bits.
	 * <p>This inserts {@code len}-bit words from {@code words}, where each {@code len}-bit word
	 * is aligned to a {@code byte} boundary. At most {@code n} words are inserted, where {@code n}
	 * is equal to {@linkplain ByteBuffer#remaining() remaining} capacity of {@code words} divided
	 * by {@code len} aligned to a {@code byte} boundary. Upon return, the {@linkplain
	 * ByteBuffer#position() position} of {@code words} is updated to reflect the first
	 * <i>unread</i> {@code byte}. This is an efficient equivalent of:
	 * {@snippet lang="java" :
	 * // Number of bytes to represent a `len`-bit word:
	 * int numBytes = (len + Bits.SIZE_OF_BYTE - 1) / Bits.SIZE_OF_BYTE;
	 * int numWords = words.remaining() / numBytes;
	 *
	 * for (int i = 0; i < numWords; i++) {
	 *     for (int j = 0, rem = len; j < numBytes; j++, rem -= Bits.SIZE_OF_BYTE) {
	 *         int val = words.get() & 0xff;
	 *
	 *         for (int k = 0; k < Math.min(rem, Bits.SIZE_OF_BYTE); k++, bit++) {
	 *             put(bit, (val & Bits.intMaskOf(k)) != 0); // @link substring="put" target="#put(int, boolean)"
	 *         }
	 *     }
	 * }
	 * }
	 *
	 * @param bit position to begin inserting from (inclusive)
	 * @param words buffer to insert words from
	 * @param len length of a word, in bits
	 * @throws IllegalStateException bitmap is {@linkplain #isReadOnly() read-only}
	 * @throws IllegalArgumentException {@code len} is less than {@code 1} or greater than the
	 * word {@linkplain Bits#SIZE_OF_LONG size}
	 * @throws IndexOutOfBoundsException {@code bit} is negative or, {@code bit + len * num}, where
	 * {@code num} is equal to the number of words that can be stord in {@code words}, is greater
	 * than bitmap {@linkplain #size() size}
	 * @since 0.2
	 * @see #extractWords(int, ByteBuffer, int)
	 */
	@SuppressWarnings("JavadocDeclaration")
	public void insertWords(int bit, ByteBuffer words, int len) {
		int bytesPerWord = wordSizeOf(len, Bits.SIZE_OF_BYTE);
		int numWords = words.remaining() / bytesPerWord;
		int size = this.checkWord(bit, len, numWords);

		if (size == 0)
			return;

		int pos = words.position();

		for (int i = 0; i < numWords; i++, pos += bytesPerWord) {
			long word = 0;

			for (int j = bytesPerWord; --j >= 0;)
				word = (word << Bits.SIZE_OF_BYTE) | (words.get(pos + j) & 0xffL);
			this.insertWord(bit + (len * i), word, len);
		}
		words.position(pos);
	}

	/**
	 * Extract zero or more bits into a {@linkplain SparseBooleanArray sparse} array.
	 * <p>This extracts up to {@code len} bits, starting at {@code bit} (inclusive), from bitmap,
	 * into a sparse array, starting at {@code 0} (inclusive). Note that the {@linkplain
	 * SparseBooleanArray#size() size} may be less than {@code len} as <i>only</i> set bits are
	 * copied. This is an efficient equivalent of:
	 * {@snippet lang="java" :
	 * SparseBooleanArray sparse = new SparseBooleanArray();
	 *
	 * for (int i = 0; i < len; i++) {
	 *     if (test(bit + i)) // @link substring="test" target="#test(int)"
	 *         sparse.put(i, true);
	 * }
	 * }
	 *
	 * @param bit position to begin extracting from (inclusive)
	 * @param len number of bits to extract
	 * @return sparse array of extracted bits
	 * @throws IndexOutOfBoundsException {@code bit} or {@code len} is negative, or {@code bit +
	 * len} is greater than bitmap {@linkplain #size() size}
	 * @since 0.2
	 * @see #insertSparse(int, SparseBooleanArray, int)
	 */
	@SuppressWarnings("JavadocDeclaration")
	public SparseBooleanArray extractSparse(int bit, int len) {
		SparseBooleanArray rv = new SparseBooleanArray();

		if (this.checkWord(bit, 1, len) == 0)
			return rv;

		int off = 0;

		do {
			int nb = Math.min(len - off, Bits.SIZE_OF_LONG);
			long word = this.extractWord(bit + off, nb);

			for (int i = Bits.firstSetBitOf(word); i < nb; i = Bits.nextSetBitOf(word, i + 1))
				rv.append(off + i, true);
			off += nb;
		} while (off < len);
		return rv;
	}

	/**
	 * Insert zero or more bits from a {@linkplain SparseBooleanArray sparse} array.
	 * <p>This inserts up to {@code len} bits, starting at {@code 0} (inclusive), from {@code src},
	 * into bitmap, starting at {@code bit} (inclusive). If the {@linkplain
	 * SparseBooleanArray#size()} of {@code src} is greater than {@code len}, the only the first
	 * {@code len} values of {@code src} are copied. If {@code src} has a size less than {@code
	 * len}, then all values of {@code src} are copied, and remaining bits are cleared. This is an
	 * efficient equivalent of:
	 * {@snippet lang="java" :
	 * for (int i = 0; i < len; i++) {
	 *     put(bit + i, i < src.size() && src.get(i)); // @link substring="put" target="#put(int, boolean)"
	 * }
	 * }
	 *
	 * @param bit position to begin inserting from (inclusive)
	 * @param src sparse array to insert from
	 * @param len number of bits to insert
	 * @since 0.2
	 * @throws IllegalStateException bitmap is {@linkplain #isReadOnly() read-only}
	 * @throws IndexOutOfBoundsException {@code bit} or {@code len} is negative, or {@code bit +
	 * len} is greater than bitmap {@linkplain #size() size}
	 * @since 0.2
	 * @see #extractSparse(int, int)
	 */
	@SuppressWarnings("JavadocDeclaration")
	public void insertSparse(int bit, SparseBooleanArray src, int len) {
		if (this.checkWord(bit, 1, len) == 0)
			return;

		int off = 0;

		for (int i = 0; i < src.size() && src.keyAt(i) < len;) {
			long word = 0L;
			int startPos = src.keyAt(i);
			int pos = startPos;

			while (true) {
				if (src.valueAt(i))
					word |= Bits.longMaskOf(pos - startPos);

				int nextPos;

				if (
					++i >= src.size() ||
					(nextPos = src.keyAt(i)) >= len ||
					(nextPos - startPos) >= Bits.SIZE_OF_LONG
				) {
					break;
				}
				pos = nextPos;
			}

			this.insertWord(bit + startPos, word, (pos - startPos) + 1);
			off = pos + 1;
		}

		while (off < len) {
			int nb = Math.min(len - off, Bits.SIZE_OF_LONG);

			this.insertWord(bit + off, 0L, nb);
			off += nb;
		}
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(this.words);
	}

	@Override
	public boolean equals(@Nullable Object that) {
		return (
			that instanceof Bitmap &&
			((Bitmap) that).size == this.size &&
			Arrays.equals(((Bitmap) that).words, this.words)
		);
	}
}
