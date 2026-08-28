// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.ads;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * {@link AdSize} tests.
 */
@RunWith(AndroidJUnit4.class)
public class AdSizeTest {
	@Test
	public void testGcd() {
		for (int odd : new int[] { Integer.MAX_VALUE, 65535, 32767, 255, 127 }) {
			assertEquals(odd, AdSize.gcd(odd, 0));
			assertEquals(odd, AdSize.gcd(0, odd));
			assertEquals(odd, AdSize.gcd(odd, odd));
			assertEquals(1, AdSize.gcd(odd, odd - 1));
			assertEquals(odd - 1, AdSize.gcd(odd - 1, odd - 1));
			assertEquals((odd - 1) / 2, AdSize.gcd(odd - 1, (odd - 1) / 2));
			assertEquals((odd - 1) / 2, AdSize.gcd((odd - 1) / 2, odd - 1));
		}

		for (int[] test : new int[][] {
			{        140,        136,  4 },
			{          1,        123,  1 },
			{        140,        203,  7 },
			{         33,        252,  3 },
			{        225,        153,  9 },
			{      53144,      41105,  1 },
			{      44062,       5088,  2 },
			{      65054,      35332, 22 },
			{      60568,      19184,  8 },
			{      11932,      54004,  4 },
			{ 1353048788, 1969135932,  4 },
			{ 1491301950, 1356732645, 15 }
		}) {
			assertEquals(test[2], AdSize.gcd(test[1], test[2]));
			assertEquals(test[2], AdSize.gcd(test[2], test[1]));
		}
	}

	@Test
	public void testDimensionOfAspectRatio() {
		for (int[] test : new int[][] {
			{ 16,  9, 1280,  720, 1920, 1080, 3840, 2160 },
			{  4,  3,  640,  480,  800,  600, 2880, 2160 },
			{  9, 16, 1080, 1920, 1440, 2560, 2160, 3840 },
			{ 43, 18, 3440, 1440, 5160, 2160, 6880, 2880 }
		}) {
			int wRatio = test[0];
			int hRatio = test[1];

			for (int i = 2; i < test.length; i += 2) {
				int w = test[i + 0];
				int h = test[i + 1];

				assertEquals(w, AdSize.widthOfAspectRatio(wRatio, hRatio, h));
				assertEquals(h, AdSize.heightOfAspectRatio(wRatio, hRatio, w));
			}
		}
	}

	@Test
	public void testOf() {
		assertSame(AdSize.EMPTY, AdSize.of(0, 0, 0, 0, 0, 0));

		// negative dimensions should always fail
		assertThrows(IllegalArgumentException.class, () -> AdSize.of(0, 0, -1, 0, 0, 0));
		assertThrows(IllegalArgumentException.class, () -> AdSize.of(0, 0, 0, -1, 0, 0));
		assertThrows(IllegalArgumentException.class, () -> AdSize.of(0, 0, 0, 0, -1, 0));
		assertThrows(IllegalArgumentException.class, () -> AdSize.of(0, 0, 0, 0, 0, -1));
		// relative size should be set or not
		assertThrows(IllegalArgumentException.class, () -> AdSize.of(1, 0, 0, 0, 0, 0));
		assertThrows(IllegalArgumentException.class, () -> AdSize.of(0, 1, 0, 0, 0, 0));

		AdSize got = AdSize.of(1, 2, 3, 4, 5, 6);

		assertEquals(1, got.widthRatio());
		assertEquals(2, got.heightRatio());
		assertEquals(3, got.minWidthDp());
		assertEquals(4, got.minHeightDp());
		assertEquals(5, got.maxWidthDp());
		assertEquals(6, got.maxHeightDp());
	}

	@Test
	public void testOfNormalized() {
		AdSize got;

		// aspect ratio should be calculated when not set
		got = AdSize.ofNormalized(0, 0, 1280, 720, 1920, 0, 0, 0);
		assertEquals(16, got.widthRatio());
		assertEquals(9, got.heightRatio());
		assertEquals(1280, got.minWidthDp());
		assertEquals(720, got.minHeightDp());
		assertEquals(1920, got.maxWidthDp());
		assertEquals(1080, got.maxHeightDp());

		got = AdSize.ofNormalized(0, 0, 640, 0, 0, 600, 2880, 2160);
		assertEquals(4, got.widthRatio());
		assertEquals(3, got.heightRatio());
		assertEquals(640, got.minWidthDp());
		assertEquals(480, got.minHeightDp());
		assertEquals(800, got.maxWidthDp());
		assertEquals(600, got.maxHeightDp());

		got = AdSize.ofNormalized(0, 0, 3440, 0, 5160, 2160, 0, 0);
		assertEquals(43, got.widthRatio());
		assertEquals(18, got.heightRatio());
		assertEquals(3440, got.minWidthDp());
		assertEquals(1440, got.minHeightDp());
		assertEquals(5160, got.maxWidthDp());
		assertEquals(2160, got.maxHeightDp());

		// when aspect ratio is set non-relative constraints should be normalized
		got = AdSize.ofNormalized(9, 16, 1080, 1920, 1440, 2560, 2160, 3840);
		assertEquals(9, got.widthRatio());
		assertEquals(16, got.heightRatio());
		assertEquals(1080, got.minWidthDp());
		assertEquals(1920, got.minHeightDp());
		assertEquals(1440, got.maxWidthDp());
		assertEquals(2560, got.maxHeightDp());

		got = AdSize.ofNormalized(9, 16, 52, 70, 563, 100, 0, 0);
		assertEquals(9, got.widthRatio());
		assertEquals(16, got.heightRatio());
		assertEquals(52, got.minWidthDp());
		assertEquals(92, got.minHeightDp());
		assertEquals(56, got.maxWidthDp());
		assertEquals(100, got.maxHeightDp());

		// when one part of the aspect ratio is set, the other part should be set to 1
		got = AdSize.ofNormalized(0, 4, 0, 0, 0, 0, 0, 0);
		assertEquals(1, got.widthRatio());
		assertEquals(4, got.heightRatio());

		got = AdSize.ofNormalized(4, 0, 0, 0, 0, 0, 0, 0);
		assertEquals(4, got.widthRatio());
		assertEquals(1, got.heightRatio());

		// if just relative size is set, we should get a normal result
		got = AdSize.ofNormalized(1, 2, 0, 0, 0, 0, 0, 0);
		assertEquals(1, got.widthRatio());
		assertEquals(2, got.heightRatio());
		assertEquals(0, got.minWidthDp());
		assertEquals(0, got.minHeightDp());
		assertEquals(0, got.maxWidthDp());
		assertEquals(0, got.maxHeightDp());

		// when everything is unset, it should be empty
		assertSame(AdSize.EMPTY, AdSize.ofNormalized(0, 0, 0, 0, 0, 0, 0, 0));
	}

	@Test
	public void testOfExact() {
		// both width and height must be specified
		assertThrows(IllegalArgumentException.class, () -> AdSize.ofExact(0, 0));
		assertThrows(IllegalArgumentException.class, () -> AdSize.ofExact(1, 0));
		assertThrows(IllegalArgumentException.class, () -> AdSize.ofExact(0, 1));

		AdSize got = AdSize.ofExact(1280, 720);

		assertEquals(16, got.widthRatio());
		assertEquals(9, got.heightRatio());
		assertEquals(1280, got.minWidthDp());
		assertEquals(720, got.minHeightDp());
		assertEquals(1280, got.maxWidthDp());
		assertEquals(720, got.maxHeightDp());
		assertEquals(1280, got.exactWidthDp());
		assertEquals(720, got.exactHeightDp());
		assertTrue(got.isExact());
	}

	@Test
	public void testOfExactWidth() {
		assertThrows(IllegalArgumentException.class, () -> AdSize.ofExactWidth(0, 0));

		AdSize got = AdSize.ofExactWidth(1280, 720);

		assertEquals(0, got.widthRatio());
		assertEquals(0, got.heightRatio());
		assertEquals(1280, got.minWidthDp());
		assertEquals(720, got.minHeightDp());
		assertEquals(1280, got.maxWidthDp());
		assertEquals(0, got.maxHeightDp());
		assertEquals(1280, got.exactWidthDp());
		assertEquals(0, got.exactHeightDp());
		assertTrue(got.isExact());

		got = AdSize.ofExactWidth(1280, 0);
		assertEquals(0, got.widthRatio());
		assertEquals(0, got.heightRatio());
		assertEquals(1280, got.minWidthDp());
		assertEquals(0, got.minHeightDp());
		assertEquals(1280, got.maxWidthDp());
		assertEquals(0, got.maxHeightDp());
		assertEquals(1280, got.exactWidthDp());
		assertEquals(0, got.exactHeightDp());
		assertTrue(got.isExact());
	}

	@Test
	public void testOfExactHeight() {
		assertThrows(IllegalArgumentException.class, () -> AdSize.ofExactHeight(0, 0));

		AdSize got = AdSize.ofExactHeight(720, 1280);

		assertEquals(0, got.widthRatio());
		assertEquals(0, got.heightRatio());
		assertEquals(1280, got.minWidthDp());
		assertEquals(720, got.minHeightDp());
		assertEquals(0, got.maxWidthDp());
		assertEquals(720, got.maxHeightDp());
		assertEquals(0, got.exactWidthDp());
		assertEquals(720, got.exactHeightDp());
		assertTrue(got.isExact());

		got = AdSize.ofExactHeight(720, 0);
		assertEquals(0, got.widthRatio());
		assertEquals(0, got.heightRatio());
		assertEquals(0, got.minWidthDp());
		assertEquals(720, got.minHeightDp());
		assertEquals(0, got.maxWidthDp());
		assertEquals(720, got.maxHeightDp());
		assertEquals(0, got.exactWidthDp());
		assertEquals(720, got.exactHeightDp());
		assertTrue(got.isExact());
	}

	@Test
	public void testOfFlexible() {
		// exact size should fail
		assertThrows(IllegalArgumentException.class, () -> AdSize.ofFlexible(0, 0, 0, 34, 0, 34));
		assertThrows(IllegalArgumentException.class, () -> AdSize.ofFlexible(0, 0, 12, 0, 12, 0));
		assertThrows(IllegalArgumentException.class, () -> AdSize.ofFlexible(0, 0, 12, 34, 12, 34));

		// no constraints should be empty
		assertSame(AdSize.EMPTY, AdSize.ofFlexible(0, 0, 0, 0, 0, 0));

		AdSize got = AdSize.ofFlexible(1, 2, 3, 4, 5, 6);

		assertEquals(1, got.widthRatio());
		assertEquals(2, got.heightRatio());
		assertEquals(3, got.minWidthDp());
		assertEquals(4, got.minHeightDp());
		assertEquals(5, got.maxWidthDp());
		assertEquals(6, got.maxHeightDp());
	}
}
