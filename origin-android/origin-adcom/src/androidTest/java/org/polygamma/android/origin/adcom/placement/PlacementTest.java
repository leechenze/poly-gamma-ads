// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.adcom.placement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.protobuf.InvalidProtocolBufferException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.polygamma.android.origin.protobuf.ProtobufReader;
import org.polygamma.android.origin.protobuf.ProtobufWriter;
import org.polygamma.origin.adcom.AdcomPlacement;

/**
 * {@link Placement} tests.
 */
@RunWith(AndroidJUnit4.class)
public class PlacementTest {
	@Test
	public void testSerde() throws InvalidProtocolBufferException {
		Placement exp = Placement.ofBuilder()
			.id("placement-id")
			.secure(false)
			.supportsInlineMarkup(true)
			.supportsMarkupUrl(false)
			.display(
				DisplayAdFormat.ofBuilder()
					.interstitial(true)
					.widthDp(123)
					.heightDp(456)
					.build()
			)
			.video(
				PlaybackAdFormat.ofVideoAdBuilder()
					.skippable(true)
					.minBitRateKbps(455)
					.maxBitRateKbps(544)
					.videoPlayerWidthDp(321)
					.videoPlayerHeightDp(654)
					.build()
			)
			.audio(
				PlaybackAdFormat.ofAudioAdBuilder()
					.skippable(false)
					.minBitRateKbps(554)
					.maxBitRateKbps(445)
					.build()
			)
			.build();
		Placement got = Placement.ofProtobuf(new ProtobufReader(
			AdcomPlacement.Placement.parseFrom(ProtobufWriter.serialize(exp))
				.toByteString()
				.asReadOnlyByteBuffer()
		));

		assertEquals("placement-id", got.id());
		assertFalse(got.secure());
		assertTrue(got.supportsInlineMarkup());
		assertFalse(got.supportsMarkupUrl());
		assertTrue(got.display().interstitial());
		assertEquals(123, got.display().widthDp());
		assertEquals(456, got.display().heightDp());
		assertTrue(got.video().isVideoAd());
		assertTrue(got.video().skippable());
		assertEquals(455, got.video().minBitRateKbps());
		assertEquals(544, got.video().maxBitRateKbps());
		assertEquals(321, got.video().videoPlayerWidthDp());
		assertEquals(654, got.video().videoPlayerHeightDp());
		assertTrue(got.audio().isAudioAd());
		assertFalse(got.audio().skippable());
		assertEquals(554, got.audio().minBitRateKbps());
		assertEquals(445, got.audio().maxBitRateKbps());

		got = Placement.of();
		assertEquals("", got.id());
		assertSame(DisplayAdFormat.of(), got.display());
		assertSame(PlaybackAdFormat.ofVideoAd(), got.video());
		assertSame(PlaybackAdFormat.ofAudioAd(), got.audio());
		assertFalse(got.secure());
		assertFalse(got.supportsInlineMarkup());
		assertFalse(got.supportsMarkupUrl());
	}
}
