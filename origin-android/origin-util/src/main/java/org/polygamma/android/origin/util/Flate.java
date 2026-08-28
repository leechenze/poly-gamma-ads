// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.util;

import android.os.Build;

import java.nio.ByteBuffer;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * Definitions for compressing and decompressing memory using the flate family of compression
 * methods.
 *
 * @since 0.1
 */
public class Flate {

	private static Deflater deflaterOf(ByteBuffer src, int level, boolean nowrap) {
		Deflater deflater = new Deflater(level, nowrap);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
			deflater.setInput(src);
		} else if (src.hasArray()) {
			deflater.setInput(src.array(), src.arrayOffset() + src.position(), src.remaining());
		} else {
			byte[] tmp = new byte[src.remaining()];

			src.get(tmp);
			deflater.setInput(tmp);
		}
		deflater.finish();
		return deflater;
	}

	private static Inflater inflaterOf(ByteBuffer src, boolean nowrap) {
		Inflater inflater = new Inflater(nowrap);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
			inflater.setInput(src);
		} else if (src.hasArray()) {
			inflater.setInput(src.array(), src.arrayOffset() + src.position(), src.remaining());
		} else {
			byte[] tmp = new byte[src.remaining()];

			src.get(tmp);
			inflater.setInput(tmp);
		}
		return inflater;
	}

	/**
	 * Compress {@linkplain ByteBuffer buffer} using ZLIB.
	 * <p>The buffer returned contains the compressed result of {@code src}, using {@link
	 * Deflater}. The buffer returned is guaranteed to be a {@linkplain ByteBuffer#isReadOnly()
	 * writable} {@linkplain ByteBuffer#hasArray() heap} buffer. Input buffer, {@code src}, is
	 * compressed at the level specified in {@code level}. See {@link
	 * Deflater#Deflater(int, boolean)} for possible compression levels. Additionally, when {@code
	 * nowrap} is {@code true}, the resulting buffer will not contain any ZLIB specific headers.
	 *
	 * @param src buffer to compress
	 * @param level level to compress at
	 * @param nowrap {@code true} if, and only if, ZLIB headers should be omitted
	 * @return compressed buffer
	 * @since 0.1
	 * @see Deflater
	 */
	public static ByteBuffer compressZlib(ByteBuffer src, int level, boolean nowrap) {
		ByteBuffer dst = ByteBuffer.allocate(Math.min(512, src.remaining() / 2));
		Deflater deflater = deflaterOf(src, level, nowrap);

		while (true) {
			int rem = dst.remaining();
			int nb;

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
				nb = deflater.deflate(dst);
			} else {
				int pos = dst.position();

				nb = deflater.deflate(dst.array(), dst.arrayOffset() + pos, rem);
				dst.position(pos + nb);
			}

			if (nb != rem && deflater.finished())
				return (ByteBuffer) dst.flip();
			dst = ByteBuffer.allocate(dst.capacity() + 1024)
				.put((ByteBuffer) dst.flip());
		}
	}

	/**
	 * Compress {@linkplain ByteBuffer buffer} using ZLIB with {@linkplain
	 * Deflater#DEFAULT_COMPRESSION default} compression level.
	 * <p>Shorthand for:
	 * {@snippet lang="java" :
	 * compressZlib(src, Deflater.DEFAULT_COMPRESSION, false); // @link substring="compressZlib" target="#compressZlib(ByteBuffer, int, boolean)"
	 * }
	 *
	 * @param src buffer to compress
	 * @return compressed buffer
	 * @since 0.1
	 * @see #compressZlib(ByteBuffer, int, boolean)
	 */
	@SuppressWarnings("JavadocDeclaration")
	public static ByteBuffer compressZlib(ByteBuffer src) {
		return compressZlib(src, Deflater.DEFAULT_COMPRESSION, false);
	}

	/**
	 * Compress subregion of {@code byte} array, using ZLIB, with {@linkplain
	 * Deflater#DEFAULT_COMPRESSION default} compression level.
	 * <p>Shorthand for:
	 * {@snippet lang="java" :
	 * compressZlib(ByteBuffer.wrap(src, off, len), Deflater.DEFAULT_COMPRESSION, false); // @link substring="compressZlib" target="#compressZlib(ByteBuffer, int, boolean)"
	 * }
	 *
	 * @param src array to compress contents of
	 * @param off offset, within {@code src}, to begin compressing from (inclusive)
	 * @param len number of bytes to compress
	 * @return compressed buffer
	 * @throws IndexOutOfBoundsException {@code off} or {@code len} is negative, or {@code off +
	 * len} is greater than {@code src.length}
	 * @since 0.1
	 * @see ByteBuffer#wrap(byte[], int, int)
	 * @see #compressZlib(ByteBuffer, int, boolean)
	 */
	@SuppressWarnings("JavadocDeclaration")
	public static ByteBuffer compressZlib(byte[] src, int off, int len) {
		return compressZlib(ByteBuffer.wrap(src, off, len));
	}

	/**
	 * Compress {@code byte} array, using ZLIB, with {@linkplain Deflater#DEFAULT_COMPRESSION
	 * default} compression level.
	 * <p>Shorthand for:
	 * {@snippet lang="java" :
	 * compressZlib(src, 0, src.length); // @link substring="compressZlib" target="#compressZlib(byte[], int, int)"
	 * }
	 *
	 * @param src array to compress contents of
	 * @return compressed buffer
	 * @since 0.1
	 * @see #compressZlib(byte[], int, int)
	 */
	@SuppressWarnings("JavadocDeclaration")
	public static ByteBuffer compressZlib(byte[] src) {
		return compressZlib(src, 0, src.length);
	}

	/**
	 * Decompress ZLIB compressed {@linkplain ByteBuffer buffer}.
	 * <p>The buffer returned contains the decompressed result of {@code src}, using {@link
	 * Inflater}. The buffer returned is guaranteed to be a {@linkplain ByteBuffer#isReadOnly()
	 * writable} {@linkplain ByteBuffer#hasArray() heap} buffer. When {@code nowrap} is {@code
	 * true}, {@code src} does not contain any ZLIB headers.
	 *
	 * @param src buffer to decompress
	 * @param nowrap {@code true} if, and only if, {@code src} does not have ZLIB headers
	 * @return decompressed buffer
	 * @throws IllegalArgumentException {@code src} is not a valid compressed buffer
	 * @since 0.1
	 * @see Inflater
	 */
	public static ByteBuffer decompressZlib(ByteBuffer src, boolean nowrap) {
		ByteBuffer dst = ByteBuffer.allocate(Math.max(512, src.remaining() * 2));
		Inflater inflater = inflaterOf(src, nowrap);

		while (true) {
			int nb;

			try {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
					nb = inflater.inflate(dst);
				}  else {
					int pos = dst.position();

					nb = inflater.inflate(dst.array(), dst.arrayOffset() + pos, dst.remaining());
					dst.position(pos + nb);
				}
			} catch (DataFormatException err) {
				throw new IllegalArgumentException(err);
			}
			if (dst.hasRemaining() && nb == 0)
				return (ByteBuffer) dst.flip();
			dst = ByteBuffer.allocate(dst.capacity() + 1024)
				.put((ByteBuffer) dst.flip());
		}
	}

	/**
	 * Decompress ZLIB compressed {@linkplain ByteBuffer buffer}, with ZLIB headers.
	 * <p>Shorthand for:
	 * {@snippet lang="java" :
	 * decompressZlib(src, false); // @link substring="decompressZlib" target="#decompressZlib(ByteBuffer, boolean)"
	 * }
	 *
	 * @param src buffer to decompress
	 * @return decompressed buffer
	 * @throws IllegalArgumentException {@code src} is not a valid compressed buffer
	 * @since 0.1
	 * @see #decompressZlib(ByteBuffer, boolean)
	 */
	@SuppressWarnings("JavadocDeclaration")
	public static ByteBuffer decompressZlib(ByteBuffer src) {
		return decompressZlib(src, false);
	}

	/**
	 * Decompress subregion of ZLIB compressed {@code byte} array, with ZLIB headers.
	 * <p>Shorthand for:
	 * {@snippet lang="java" :
	 * decompressZlib(ByteBuffer.wrap(src, off, len), false); // @link substring="decompressZlib" target="#decompressZlib(ByteBuffer, boolean)"
	 * }
	 *
	 * @param src array to decompress from
	 * @param off offset, within {@code src}, to begin decompressing from
	 * @param len number of bytes to decompress
	 * @return decompressed buffer
	 * @throws IndexOutOfBoundsException {@code off} or {@code len} is negative, or {@code off +
	 * len} is greater than {@code src.length}
	 * @throws IllegalArgumentException {@code src} is not a valid compressed buffer
	 * @since 0.1
	 * @see #decompressZlib(ByteBuffer, boolean)
	 */
	@SuppressWarnings("JavadocDeclaration")
	public static ByteBuffer decompressZlib(byte[] src, int off, int len) {
		return decompressZlib(ByteBuffer.wrap(src, off, len));
	}

	/**
	 * Decompress of ZLIB compressed {@code byte} array, with ZLIB headers.
	 * <p>Shorthand for:
	 * {@snippet lang="java" :
	 * decompressZlib(src, 0, src.length); // @link substring="decompressZlib" target="#decompressZlib(byte[], int, int)"
	 * }
	 *
	 * @param src array to decompress from
	 * @return decompressed buffer
	 * @throws IndexOutOfBoundsException {@code off} or {@code len} is negative, or {@code off +
	 * len} is greater than {@code src.length}
	 * @since 0.1
	 * @see #decompressZlib(byte[], int, int)
	 */
	@SuppressWarnings("JavadocDeclaration")
	public static ByteBuffer decompressZlib(byte[] src) {
		return decompressZlib(src, 0, src.length);
	}

	private Flate() {
	}
}
