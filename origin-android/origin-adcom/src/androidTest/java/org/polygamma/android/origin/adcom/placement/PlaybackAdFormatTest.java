// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.adcom.placement;

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
import org.polygamma.origin.adcom.AdcomPlacement;

import java.util.Arrays;

/**
 * {@link PlaybackAdFormat} tests.
 */
@RunWith(AndroidJUnit4.class)
public class PlaybackAdFormatTest {
	@Test
	public void testSerdeAudioAd() throws InvalidProtocolBufferException {
		PlaybackAdFormat exp = PlaybackAdFormat.ofAudioAdBuilder()
			.supportedMimes(Arrays.asList("text/html", "video/mp4", "video/ogg"))
			.supportedAdApis(AdComEnums.AdApiMraid30, AdComEnums.AdApiSimid11)
			.minBitRateKbps(123)
			.maxBitRateKbps(456)
			.skippable(true)
			.build();
		PlaybackAdFormat got = PlaybackAdFormat.ofAudioAdProtobuf(new ProtobufReader(
			AdcomPlacement.AudioAdFormat.parseFrom(ProtobufWriter.serialize(exp))
				.toByteString()
				.asReadOnlyByteBuffer()
		));

		assertTrue(got.isAudioAd());
		assertFalse(got.isVideoAd());
		assertEquals(3, got.supportedMimeCount());
		assertEquals("text/html", got.supportedMime(0));
		assertEquals("video/mp4", got.supportedMime(1));
		assertEquals("video/ogg", got.supportedMime(2));
		assertFalse(got.isAdApiSupported(AdComEnums.AdApiMraid10));
		assertFalse(got.isAdApiSupported(AdComEnums.AdApiMraid20));
		assertTrue(got.isAdApiSupported(AdComEnums.AdApiMraid30));
		assertFalse(got.isAdApiSupported(AdComEnums.AdApiOmid10));
		assertFalse(got.isAdApiSupported(AdComEnums.AdApiOrmma));
		assertFalse(got.isAdApiSupported(AdComEnums.AdApiSimid10));
		assertTrue(got.isAdApiSupported(AdComEnums.AdApiSimid11));
		assertFalse(got.isAdApiSupported(AdComEnums.AdApiUnknown));
		assertFalse(got.isAdApiSupported(AdComEnums.AdApiVpaid10));
		assertFalse(got.isAdApiSupported(AdComEnums.AdApiVpaid20));
		assertEquals(123, got.minBitRateKbps());
		assertEquals(456, got.maxBitRateKbps());
		assertTrue(got.skippable());

		got = PlaybackAdFormat.ofAudioAd();
		assertTrue(got.isAudioAd());
		assertFalse(got.isVideoAd());
		assertEquals(0, got.supportedMimeCount());
		assertFalse(got.isAdApiSupported(AdComEnums.AdApiMraid10));
		assertFalse(got.isAdApiSupported(AdComEnums.AdApiMraid20));
		assertFalse(got.isAdApiSupported(AdComEnums.AdApiMraid30));
		assertFalse(got.isAdApiSupported(AdComEnums.AdApiOmid10));
		assertFalse(got.isAdApiSupported(AdComEnums.AdApiOrmma));
		assertFalse(got.isAdApiSupported(AdComEnums.AdApiSimid10));
		assertFalse(got.isAdApiSupported(AdComEnums.AdApiSimid11));
		assertFalse(got.isAdApiSupported(AdComEnums.AdApiUnknown));
		assertFalse(got.isAdApiSupported(AdComEnums.AdApiVpaid10));
		assertFalse(got.isAdApiSupported(AdComEnums.AdApiVpaid20));
		assertEquals(0, got.minBitRateKbps());
		assertEquals(0, got.maxBitRateKbps());
		assertFalse(got.skippable());
	}

	@Test
	public void testSerdeVideoAd() throws InvalidProtocolBufferException {
		PlaybackAdFormat exp = PlaybackAdFormat.ofVideoAdBuilder()
			.supportedMimes(Arrays.asList("text/html", "video/mp4", "video/ogg"))
			.supportedAdApis(AdComEnums.AdApiMraid30, AdComEnums.AdApiSimid11)
			.minBitRateKbps(123)
			.maxBitRateKbps(456)
			.videoPlayerWidthDp(789)
			.videoPlayerHeightDp(987)
			.videoActivationBehavior(AdComEnums.ActivationEmbeddedBrowser)
			.skippable(true)
			.build();
		PlaybackAdFormat got = PlaybackAdFormat.ofVideoAdProtobuf(new ProtobufReader(
			AdcomPlacement.VideoAdFormat.parseFrom(ProtobufWriter.serialize(exp))
				.toByteString()
				.asReadOnlyByteBuffer()
		));

		assertFalse(got.isAudioAd());
		assertTrue(got.isVideoAd());
		assertEquals(3, got.supportedMimeCount());
		assertEquals("text/html", got.supportedMime(0));
		assertEquals("video/mp4", got.supportedMime(1));
		assertEquals("video/ogg", got.supportedMime(2));
		assertFalse(got.isAdApiSupported(AdComEnums.AdApiMraid10));
		assertFalse(got.isAdApiSupported(AdComEnums.AdApiMraid20));
		assertTrue(got.isAdApiSupported(AdComEnums.AdApiMraid30));
		assertFalse(got.isAdApiSupported(AdComEnums.AdApiOmid10));
		assertFalse(got.isAdApiSupported(AdComEnums.AdApiOrmma));
		assertFalse(got.isAdApiSupported(AdComEnums.AdApiSimid10));
		assertTrue(got.isAdApiSupported(AdComEnums.AdApiSimid11));
		assertFalse(got.isAdApiSupported(AdComEnums.AdApiUnknown));
		assertFalse(got.isAdApiSupported(AdComEnums.AdApiVpaid10));
		assertFalse(got.isAdApiSupported(AdComEnums.AdApiVpaid20));
		assertEquals(123, got.minBitRateKbps());
		assertEquals(456, got.maxBitRateKbps());
		assertEquals(789, got.videoPlayerWidthDp());
		assertEquals(987, got.videoPlayerHeightDp());
		assertEquals(AdComEnums.ActivationEmbeddedBrowser, got.videoActivationBehavior());
		assertTrue(got.skippable());

		got = PlaybackAdFormat.ofVideoAd();
		assertFalse(got.isAudioAd());
		assertTrue(got.isVideoAd());
		assertEquals(0, got.supportedMimeCount());
		assertFalse(got.isAdApiSupported(AdComEnums.AdApiMraid10));
		assertFalse(got.isAdApiSupported(AdComEnums.AdApiMraid20));
		assertFalse(got.isAdApiSupported(AdComEnums.AdApiMraid30));
		assertFalse(got.isAdApiSupported(AdComEnums.AdApiOmid10));
		assertFalse(got.isAdApiSupported(AdComEnums.AdApiOrmma));
		assertFalse(got.isAdApiSupported(AdComEnums.AdApiSimid10));
		assertFalse(got.isAdApiSupported(AdComEnums.AdApiSimid11));
		assertFalse(got.isAdApiSupported(AdComEnums.AdApiUnknown));
		assertFalse(got.isAdApiSupported(AdComEnums.AdApiVpaid10));
		assertFalse(got.isAdApiSupported(AdComEnums.AdApiVpaid20));
		assertEquals(0, got.minBitRateKbps());
		assertEquals(0, got.maxBitRateKbps());
		assertEquals(0, got.videoPlayerWidthDp());
		assertEquals(0, got.videoPlayerHeightDp());
		assertEquals(AdComEnums.ActivationNone, got.videoActivationBehavior());
		assertFalse(got.skippable());
	}
}
