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

import java.util.Arrays;
import java.util.List;

/**
 * {@link PlaybackAd} tests.
 */
@RunWith(AndroidJUnit4.class)
public class PlaybackAdTest {

	private static final List<AdEventTracker> EXPECT_EVENT_TRACKERS = Arrays.asList(
		AdEventTracker.ofBuilder()
			.event(AdComEnums.AdEventLoaded)
			.type(AdComEnums.AdEventTrackerPixel)
			.url("https://loaded-track.com")
			.build(),
		AdEventTracker.ofBuilder()
			.event(AdComEnums.AdEventImpression)
			.type(AdComEnums.AdEventTrackerPixel)
			.url("https://impression-track.com")
			.build()
	);

	private static final List<PlaybackCreative> EXPECT_CREATIVES = Arrays.asList(
		PlaybackCreative.ofOverlayBuilder()
			.id("overlay")
			.sequence(123)
			.overlayDisplay(
				DisplayAd.ofDisplayAdBuilder()
					.requiredAdApis(AdComEnums.AdApiMraid10, AdComEnums.AdApiMraid30)
					.widthDp(556)
					.heightDp(655)
					.bannerHtmlCreativeMarkup("<html>overlay</html>")
					.build()
			)
			.build(),
		PlaybackCreative.ofLinearBuilder()
			.id("linear")
			.sequence(321)
			.linearPlaybackDurationSeconds(1234567)
			.linearSkipOffsetSeconds(23456)
			.linearAssets(Arrays.asList(
				LinearAsset.ofClosedCaptionAsset("https://linear.com/cc", "application/xml", "en"),
				LinearAsset.ofMediaAsset(
					"https://linear.com/video.mp4",
					"video/mp4",
					"linear-media-1-id",
					"linear-media-1-codec-name",
					123, 456, 789,
					AdComEnums.PlaybackDeliveryStreaming,
					456, 654,
					true, false
				)
			))
			.build()
	);

	private static void assertCommon(PlaybackAd got) {
		assertEquals(2, got.eventTrackerCount());
		assertEquals(AdComEnums.AdEventLoaded, got.eventTracker(0).event());
		assertEquals(AdComEnums.AdEventTrackerPixel, got.eventTracker(0).type());
		assertEquals("https://loaded-track.com", got.eventTracker(0).url());
		assertEquals(AdComEnums.AdEventImpression, got.eventTracker(1).event());
		assertEquals(AdComEnums.AdEventTrackerPixel, got.eventTracker(1).type());
		assertEquals("https://impression-track.com", got.eventTracker(1).url());

		assertEquals(2, got.creativeCount());

		PlaybackCreative gotCreative = got.creative(0);

		assertTrue(gotCreative.isOverlay());
		assertEquals("overlay", gotCreative.id());
		assertEquals(123, gotCreative.sequence());

		DisplayAd gotDisplay = gotCreative.overlayDisplay();

		assertTrue(gotDisplay.isAdApiRequired(AdComEnums.AdApiMraid10));
		assertTrue(gotDisplay.isAdApiRequired(AdComEnums.AdApiMraid30));
		assertEquals(556, gotDisplay.widthDp());
		assertEquals(655, gotDisplay.heightDp());
		assertEquals(AdComEnums.DisplayCreativeHtml, gotDisplay.creativeType());
		assertEquals("<html>overlay</html>", gotDisplay.bannerHtmlMarkup());

		gotCreative = got.creative(1);
		assertTrue(gotCreative.isLinear());
		assertEquals("linear", gotCreative.id());
		assertEquals(321, gotCreative.sequence());
		assertEquals(1234567, gotCreative.linearPlaybackDurationSeconds());
		assertEquals(23456, gotCreative.linearSkipOffsetSeconds());
		assertEquals(2, gotCreative.linearAssetCount());

		LinearAsset gotAsset = gotCreative.linearAsset(0);

		assertTrue(gotAsset.isClosedCaptionAsset());
		assertEquals("https://linear.com/cc", gotAsset.url());
		assertEquals("application/xml", gotAsset.mime());
		assertEquals("en", gotAsset.closedCaptionLanguageCode());

		gotAsset = gotCreative.linearAsset(1);
		assertEquals("https://linear.com/video.mp4", gotAsset.url());
		assertEquals("video/mp4", gotAsset.mime());
		assertEquals("linear-media-1-id", gotAsset.playbackId());
		assertEquals("linear-media-1-codec-name", gotAsset.playbackCodec());
		assertEquals(123, gotAsset.playbackAverageBitRateKbps());
		assertEquals(456, gotAsset.playbackMinBitRateKbps());
		assertEquals(789, gotAsset.playbackMaxBitRateKbps());
		assertEquals(AdComEnums.PlaybackDeliveryStreaming, gotAsset.playbackSupportedDelivery());
		assertEquals(456, gotAsset.playbackWidthPx());
		assertEquals(654, gotAsset.playbackHeightPx());
		assertTrue(gotAsset.playbackCanScale());
		assertFalse(gotAsset.playbackMaintainAspectRatio());
	}

	@Test
	public void testSerdeVideoAd() throws InvalidProtocolBufferException {
		PlaybackAd exp = PlaybackAd.ofVideoAdBuilder()
			.titleText("title")
			.descriptionText("description")
			.eventTrackers(EXPECT_EVENT_TRACKERS)
			.creatives(EXPECT_CREATIVES)
			.build();
		PlaybackAd got = PlaybackAd.ofVideoAdProtobuf(new ProtobufReader(
			AdcomMedia.VideoAd.parseFrom(ProtobufWriter.serialize(exp::toAudioOrVideoAdProtobuf))
				.toByteString()
				.asReadOnlyByteBuffer()
		));

		assertFalse(got.isAudioAd());
		assertTrue(got.isVideoAd());
		assertEquals("title", got.titleText());
		assertEquals("description", got.descriptionText());
		assertCommon(got);
	}

	@Test
	public void testSerdeAudioAd() throws InvalidProtocolBufferException {
		PlaybackAd exp = PlaybackAd.ofAudioAdBuilder()
			.titleText("title")
			.descriptionText("description")
			.eventTrackers(EXPECT_EVENT_TRACKERS)
			.creatives(EXPECT_CREATIVES)
			.build();
		PlaybackAd got = PlaybackAd.ofAudioAdProtobuf(new ProtobufReader(
			AdcomMedia.AudioAd.parseFrom(ProtobufWriter.serialize(exp::toAudioOrVideoAdProtobuf))
				.toByteString()
				.asReadOnlyByteBuffer()
		));

		assertTrue(got.isAudioAd());
		assertFalse(got.isVideoAd());
		assertEquals("title", got.titleText());
		assertEquals("description", got.descriptionText());
		assertCommon(got);
	}
}
