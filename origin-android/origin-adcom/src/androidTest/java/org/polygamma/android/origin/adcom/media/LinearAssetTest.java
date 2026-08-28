// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.adcom.media;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.protobuf.InvalidProtocolBufferException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.polygamma.android.origin.adcom.enums.AdComEnums;
import org.polygamma.android.origin.protobuf.ProtobufReader;
import org.polygamma.android.origin.protobuf.ProtobufWriter;
import org.polygamma.origin.adcom.AdcomMedia;

/**
 * {@link LinearAsset} tests.
 */
@RunWith(AndroidJUnit4.class)
public class LinearAssetTest {
	@Test
	public void testSerdeClosedCaption() throws InvalidProtocolBufferException {
		LinearAsset exp =
			LinearAsset.ofClosedCaptionAsset("https://foo.com", "application/xml", "en");
		LinearAsset got = LinearAsset.ofProtobuf(new ProtobufReader(
			AdcomMedia.LinearAsset.parseFrom(ProtobufWriter.serialize(exp))
				.toByteString()
				.asReadOnlyByteBuffer()
		));

		assertTrue(got.isClosedCaptionAsset());
		assertFalse(got.isInteractiveAsset());
		assertFalse(got.isMediaAsset());
		assertFalse(got.isMezzanineAsset());
		assertEquals("application/xml", got.mime());
		assertEquals("https://foo.com", got.url());
		assertEquals("en", got.closedCaptionLanguageCode());
	}

	@Test
	public void testSerdeInteractiveAsset() throws InvalidProtocolBufferException {
		LinearAsset exp = LinearAsset.ofInteractiveAsset(
			"https://foo.com",
			"text/html",
			AdComEnums.AdApiOmid10,
			true
		);
		LinearAsset got = LinearAsset.ofProtobuf(new ProtobufReader(
			AdcomMedia.LinearAsset.parseFrom(ProtobufWriter.serialize(exp))
				.toByteString()
				.asReadOnlyByteBuffer()
		));

		assertFalse(got.isClosedCaptionAsset());
		assertTrue(got.isInteractiveAsset());
		assertFalse(got.isMediaAsset());
		assertFalse(got.isMezzanineAsset());
		assertEquals("https://foo.com", got.url());
		assertEquals("text/html", got.mime());
		assertEquals(AdComEnums.AdApiOmid10, got.interactiveRequiredAdApi());
		assertTrue(got.interactiveCanExtendPlaybackDuration());
	}

	@Test
	public void testSerdeMediaAsset() throws InvalidProtocolBufferException {
		LinearAsset exp = LinearAsset.ofMediaAsset(
			"https://foo.com",
			"video/mp4",
			"media-id",
			"codec-name",
			123,
			456,
			789,
			AdComEnums.PlaybackDeliveryStreaming,
			234,
			432,
			true,
			true
		);
		LinearAsset got = LinearAsset.ofProtobuf(new ProtobufReader(
			AdcomMedia.LinearAsset.parseFrom(ProtobufWriter.serialize(exp))
				.toByteString()
				.asReadOnlyByteBuffer()
		));

		assertFalse(got.isClosedCaptionAsset());
		assertFalse(got.isInteractiveAsset());
		assertTrue(got.isMediaAsset());
		assertFalse(got.isMezzanineAsset());
		assertEquals("https://foo.com", got.url());
		assertEquals("video/mp4", got.mime());
		assertEquals("media-id", got.playbackId());
		assertEquals("codec-name", got.playbackCodec());
		assertEquals(123, got.playbackAverageBitRateKbps());
		assertEquals(456, got.playbackMinBitRateKbps());
		assertEquals(789, got.playbackMaxBitRateKbps());
		assertEquals(AdComEnums.PlaybackDeliveryStreaming, got.playbackSupportedDelivery());
		assertEquals(234, got.playbackWidthPx());
		assertEquals(432, got.playbackHeightPx());
		assertTrue(got.playbackCanScale());
		assertTrue(got.playbackMaintainAspectRatio());
	}

	@Test
	public void testSerdeMezzanineAsset() throws InvalidProtocolBufferException {
		LinearAsset exp = LinearAsset.ofMezzanineAsset(
			"https://foo.com",
			"video/mp4",
			"media-id",
			"codec-name",
			123,
			456,
			789,
			AdComEnums.PlaybackDeliveryStreaming,
			234,
			432,
			true,
			true
		);
		LinearAsset got = LinearAsset.ofProtobuf(new ProtobufReader(
			AdcomMedia.LinearAsset.parseFrom(ProtobufWriter.serialize(exp))
				.toByteString()
				.asReadOnlyByteBuffer()
		));

		assertFalse(got.isClosedCaptionAsset());
		assertFalse(got.isInteractiveAsset());
		assertFalse(got.isMediaAsset());
		assertTrue(got.isMezzanineAsset());
		assertEquals("https://foo.com", got.url());
		assertEquals("video/mp4", got.mime());
		assertEquals("media-id", got.playbackId());
		assertEquals("codec-name", got.playbackCodec());
		assertEquals(123, got.playbackAverageBitRateKbps());
		assertEquals(456, got.playbackMinBitRateKbps());
		assertEquals(789, got.playbackMaxBitRateKbps());
		assertEquals(AdComEnums.PlaybackDeliveryStreaming, got.playbackSupportedDelivery());
		assertEquals(234, got.playbackWidthPx());
		assertEquals(432, got.playbackHeightPx());
		assertTrue(got.playbackCanScale());
		assertTrue(got.playbackMaintainAspectRatio());
	}
}
