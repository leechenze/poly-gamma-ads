// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.util;

import static org.junit.Assert.assertEquals;
import static org.polygamma.android.origin.util.Flate.*;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

/**
 * {@link Flate} tests.
 */
@RunWith(AndroidJUnit4.class)
public class FlateTest {

	private static ByteBuffer compress(ByteBuffer src, int level, boolean nowrap) {
		try (
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			DeflaterOutputStream zlib =
				new DeflaterOutputStream(bytes, new Deflater(level, nowrap))
		) {
			while (src.hasRemaining())
				zlib.write(src.get() & 0xff);
			zlib.finish();
			zlib.flush();
			return ByteBuffer.wrap(bytes.toByteArray());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Test
	public void testCompressZlib() {
		ByteBuffer src = ByteBuffer.wrap("test".getBytes(StandardCharsets.UTF_8));

		for (
			int lvl : new int[] {
				Deflater.DEFAULT_COMPRESSION,
				Deflater.BEST_COMPRESSION,
				Deflater.BEST_SPEED,
				Deflater.NO_COMPRESSION
			}
		) {
			ByteBuffer wrap = compress(src.duplicate(), lvl, false);
			ByteBuffer nowrap = compress(src.duplicate(), lvl, true);

			assertEquals(wrap, compressZlib(src.duplicate(), lvl, false));
			assertEquals(nowrap, compressZlib(src.duplicate(), lvl, true));
		}
	}

	@Test
	public void testDecompressZlib() {
		ByteBuffer src = ByteBuffer.wrap("test".getBytes(StandardCharsets.UTF_8));

		for (
			int lvl : new int[] {
			Deflater.DEFAULT_COMPRESSION,
			Deflater.BEST_COMPRESSION,
			Deflater.BEST_SPEED,
			Deflater.NO_COMPRESSION
		}
		) {
			ByteBuffer wrap = compress(src.duplicate(), lvl, false);
			ByteBuffer nowrap = compress(src.duplicate(), lvl, true);

			assertEquals(src, decompressZlib(wrap, false));
			assertEquals(src, decompressZlib(nowrap, true));
		}
	}
}
