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
 * {@link Ad} tests.
 */
@RunWith(AndroidJUnit4.class)
public class AdTest {

	private static final List<AdEventTracker> EXPECT_EVENT_TRACKERS = Arrays.asList(
		AdEventTracker.ofBuilder()
			.event(AdComEnums.AdEventActivated)
			.type(AdComEnums.AdEventTrackerJavaScript)
			.url("https://tracker-1.com")
			.build(),
		AdEventTracker.ofBuilder()
			.event(AdComEnums.AdEventError)
			.type(AdComEnums.AdEventTrackerPixel)
			.url("https://tracker-2.com")
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

	private static void assertEventTrackers(Ad ad) {
		assertEquals(2, ad.eventTrackerCount());
		assertEquals(AdComEnums.AdEventActivated, ad.eventTracker(0).event());
		assertEquals(AdComEnums.AdEventTrackerJavaScript, ad.eventTracker(0).type());
		assertEquals("https://tracker-1.com", ad.eventTracker(0).url());
		assertEquals(AdComEnums.AdEventError, ad.eventTracker(1).event());
		assertEquals(AdComEnums.AdEventTrackerPixel, ad.eventTracker(1).type());
		assertEquals("https://tracker-2.com", ad.eventTracker(1).url());
	}

	private static void assertCreatives(PlaybackAd got) {
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
	public void testAudioAd() throws InvalidProtocolBufferException {
		PlaybackAd exp = PlaybackAd.ofAudioAdBuilder()
			.id("ad-id")
			.serveId("serve-id")
			.secure(true)
			.eventTrackers(EXPECT_EVENT_TRACKERS)
			.titleText("title")
			.descriptionText("description")
			.creatives(EXPECT_CREATIVES)
			.build();
		Ad got = Ad.ofProtobuf(new ProtobufReader(
			AdcomMedia.Ad.parseFrom(ProtobufWriter.serialize(exp))
				.toByteString()
				.asReadOnlyByteBuffer()
		));

		assertTrue(got instanceof PlaybackAd);

		PlaybackAd gotPlayback = (PlaybackAd) got;

		assertTrue(gotPlayback.isAudioAd());
		assertEquals("ad-id", gotPlayback.id());
		assertEquals("serve-id", gotPlayback.serveId());
		assertTrue(gotPlayback.secure());
		assertEventTrackers(gotPlayback);
		assertCreatives(gotPlayback);
	}

	@Test
	public void testDisplayAd() throws InvalidProtocolBufferException {
		DisplayAd exp = DisplayAd.ofDisplayAdBuilder()
			.id("ad-id")
			.serveId("serve-id")
			.secure(true)
			.requiredAdApis(AdComEnums.AdApiMraid10, AdComEnums.AdApiMraid30)
			.widthDp(123)
			.heightDp(456)
			.widthRatio(12)
			.heightRatio(21)
			.buyerPrivacyPolicyUrl("https://privacy.com")
			.bannerHtmlCreativeUrl("https://html.com/")
			.eventTrackers(EXPECT_EVENT_TRACKERS)
			.minShowDurationSeconds(789)
			.build();
		Ad got = Ad.ofProtobuf(new ProtobufReader(
			AdcomMedia.Ad.parseFrom(ProtobufWriter.serialize(exp))
				.toByteString()
				.asReadOnlyByteBuffer()
		));

		assertTrue(got instanceof DisplayAd);

		DisplayAd gotDisplay = (DisplayAd) got;

		assertEquals("ad-id", gotDisplay.id());
		assertEquals("serve-id", gotDisplay.serveId());
		assertTrue(gotDisplay.secure());
		assertTrue(gotDisplay.isAdApiRequired(AdComEnums.AdApiMraid10));
		assertTrue(gotDisplay.isAdApiRequired(AdComEnums.AdApiMraid30));
		assertEquals(123, gotDisplay.widthDp());
		assertEquals(456, gotDisplay.heightDp());
		assertEquals(12, gotDisplay.widthRatio());
		assertEquals(21, gotDisplay.heightRatio());
		assertEquals("https://privacy.com", gotDisplay.buyerPrivacyPolicyUrl());
		assertEquals(AdComEnums.DisplayCreativeHtml, gotDisplay.creativeType());
		assertEquals("https://html.com/", gotDisplay.bannerHtmlUrl());
		assertEventTrackers(gotDisplay);
		assertEquals(789, gotDisplay.minShowDurationSeconds());
	}

	@Test
	public void testVideoAd() throws InvalidProtocolBufferException {
		PlaybackAd exp = PlaybackAd.ofVideoAdBuilder()
			.id("ad-id")
			.serveId("serve-id")
			.secure(true)
			.eventTrackers(EXPECT_EVENT_TRACKERS)
			.titleText("title")
			.descriptionText("description")
			.creatives(EXPECT_CREATIVES)
			.build();
		Ad got = Ad.ofProtobuf(new ProtobufReader(
			AdcomMedia.Ad.parseFrom(ProtobufWriter.serialize(exp))
				.toByteString()
				.asReadOnlyByteBuffer()
		));

		assertTrue(got instanceof PlaybackAd);

		PlaybackAd gotPlayback = (PlaybackAd) got;

		assertTrue(gotPlayback.isVideoAd());
		assertEquals("ad-id", gotPlayback.id());
		assertEquals("serve-id", gotPlayback.serveId());
		assertTrue(gotPlayback.secure());
		assertEventTrackers(gotPlayback);
		assertCreatives(gotPlayback);
	}
}
