// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.adcom.media;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.util.ArrayMap;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.protobuf.InvalidProtocolBufferException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.polygamma.android.origin.adcom.enums.AdComEnums;
import org.polygamma.android.origin.protobuf.ProtobufReader;
import org.polygamma.android.origin.protobuf.ProtobufWriter;
import org.polygamma.origin.adcom.AdcomMedia;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * {@link PlaybackCreative} tests.
 */
@RunWith(AndroidJUnit4.class)
public class PlaybackCreativeTest {

	private static final List<CompanionAd> EXPECT_COMPANIONS = Arrays.asList(
		CompanionAd.of(
			"comp-1",
			DisplayAd.ofDisplayAdBuilder()
				.widthDp(123)
				.heightDp(456)
				.bannerHtmlCreativeMarkup("<html>comp-1</html>")
				.build(),
			true
		),
		CompanionAd.of(
			"comp-2",
			DisplayAd.ofDisplayAdBuilder()
				.widthDp(321)
				.heightDp(654)
				.bannerHtmlCreativeMarkup("<html>comp-2</html>")
				.build(),
			false
		)
	);

	private static void assertCompanion(PlaybackCreative got) {
		assertEquals(2, got.companionCount());

		DisplayAd gotDisplay = got.companion(0).display();

		assertEquals("comp-1", got.companion(0).placementId());
		assertTrue(got.companion(0).endCard());
		assertEquals(123, gotDisplay.widthDp());
		assertEquals(456, gotDisplay.heightDp());
		assertEquals(AdComEnums.DisplayCreativeHtml, gotDisplay.creativeType());
		assertEquals("<html>comp-1</html>", gotDisplay.bannerHtmlMarkup());

		gotDisplay = got.companion(1).display();
		assertEquals("comp-2", got.companion(1).placementId());
		assertFalse(got.companion(1).endCard());
		assertEquals(321, gotDisplay.widthDp());
		assertEquals(654, gotDisplay.heightDp());
		assertEquals(AdComEnums.DisplayCreativeHtml, gotDisplay.creativeType());
		assertEquals("<html>comp-2</html>", gotDisplay.bannerHtmlMarkup());
	}

	@Test
	public void testSerdeLinear() throws InvalidProtocolBufferException {
		ArrayMap<String, String> expUnivIds = new ArrayMap<>();

		expUnivIds.put("id-registry-1.com", "id-1");
		expUnivIds.put("id-registry-2.com", "id-2");

		PlaybackCreative exp = PlaybackCreative.ofLinearBuilder()
			.id("linear-id")
			.sequence(123)
			.companionRequirement(AdComEnums.CompanionRequirementAll)
			.companions(EXPECT_COMPANIONS)
			.linearLink(LinkAsset.of("https://primary.com", "https://fallback.com", Arrays.asList(
				"https://click-track-1.com",
				"https://click-track-2.com"
			)))
			.linearPlaybackDurationSeconds(456)
			.linearSkipOffsetSeconds(789)
			.linearAssets(Arrays.asList(
				LinearAsset.ofClosedCaptionAsset("https://linear.com/cc", "application/xml", "en"),
				LinearAsset.ofMediaAsset(
					"https://linear.com/video.mp4",
					"video/mp4",
					"linear-asset-1",
					"linear-codec-1",
					123, 456, 789,
					AdComEnums.PlaybackDeliveryStreaming,
					556, 655,
					true, false
				)
			))
			.linearIcons(Arrays.asList(
				IconAsset.ofBuilder()
					.programName("linear-icon-1")
					.display(Collections.singleton(
						IconDisplayAsset.ofHtmlMarkupAsset("<html>linear-icon-1</html>")
					))
					.build(),
				IconAsset.ofBuilder()
					.programName("linear-icon-2")
					.display(Collections.singleton(
						IconDisplayAsset.ofHtmlMarkupAsset("<html>linear-icon-2</html>")
					))
					.build()
			))
			.linearEventTrackers(Arrays.asList(
				AdEventTracker.ofBuilder()
					.event(AdComEnums.AdEventLoaded)
					.type(AdComEnums.AdEventTrackerPixel)
					.url("https://linear.com/track-loaded")
					.build(),
				AdEventTracker.ofBuilder()
					.event(AdComEnums.AdEventImpression)
					.type(AdComEnums.AdEventTrackerPixel)
					.url("https://linear.com/track-impression")
					.build()
			))
			.linearUniversalAdIds(expUnivIds)
			.build();
		PlaybackCreative got = PlaybackCreative.ofProtobuf(new ProtobufReader(
			AdcomMedia.PlaybackCreative.parseFrom(ProtobufWriter.serialize(exp))
				.toByteString()
				.asReadOnlyByteBuffer()
		));

		assertTrue(got.isLinear());
		assertFalse(got.isOverlay());
		assertEquals("linear-id", got.id());
		assertEquals(123, got.sequence());
		assertEquals(AdComEnums.CompanionRequirementAll, got.companionRequirement());
		assertCompanion(got);

		assertEquals("https://primary.com", got.link().primaryUrl());
		assertEquals("https://fallback.com", got.link().fallbackUrl());
		assertEquals(2, got.link().trackerUrlCount());
		assertEquals("https://click-track-1.com", got.link().trackerUrl(0));
		assertEquals("https://click-track-2.com", got.link().trackerUrl(1));

		assertEquals(456, got.linearPlaybackDurationSeconds());
		assertEquals(789, got.linearSkipOffsetSeconds());

		assertEquals(2, got.linearAssetCount());
		assertTrue(got.linearAsset(0).isClosedCaptionAsset());
		assertEquals("https://linear.com/cc", got.linearAsset(0).url());
		assertEquals("application/xml", got.linearAsset(0).mime());
		assertEquals("en", got.linearAsset(0).closedCaptionLanguageCode());

		assertTrue(got.linearAsset(1).isMediaAsset());
		assertEquals("https://linear.com/video.mp4", got.linearAsset(1).url());
		assertEquals("video/mp4", got.linearAsset(1).mime());
		assertEquals("linear-asset-1", got.linearAsset(1).playbackId());
		assertEquals("linear-codec-1", got.linearAsset(1).playbackCodec());
		assertEquals(123, got.linearAsset(1).playbackAverageBitRateKbps());
		assertEquals(456, got.linearAsset(1).playbackMinBitRateKbps());
		assertEquals(789, got.linearAsset(1).playbackMaxBitRateKbps());
		assertEquals(
			AdComEnums.PlaybackDeliveryStreaming,
			got.linearAsset(1).playbackSupportedDelivery()
		);
		assertEquals(556, got.linearAsset(1).playbackWidthPx());
		assertEquals(655, got.linearAsset(1).playbackHeightPx());
		assertTrue(got.linearAsset(1).playbackCanScale());
		assertFalse(got.linearAsset(1).playbackMaintainAspectRatio());

		assertEquals(2, got.linearIconCount());
		assertEquals("linear-icon-1", got.linearIcon(0).programName());
		assertEquals(1, got.linearIcon(0).displayCount());
		assertEquals("<html>linear-icon-1</html>", got.linearIcon(0).display(0).htmlMarkup());
		assertEquals("linear-icon-2", got.linearIcon(1).programName());
		assertEquals(1, got.linearIcon(1).displayCount());
		assertEquals("<html>linear-icon-2</html>", got.linearIcon(1).display(0).htmlMarkup());

		assertEquals(2, got.eventTrackerCount());
		assertEquals(AdComEnums.AdEventLoaded, got.eventTracker(0).event());
		assertEquals(AdComEnums.AdEventTrackerPixel, got.eventTracker(0).type());
		assertEquals("https://linear.com/track-loaded", got.eventTracker(0).url());
		assertEquals(AdComEnums.AdEventImpression, got.eventTracker(1).event());
		assertEquals(AdComEnums.AdEventTrackerPixel, got.eventTracker(1).type());
		assertEquals("https://linear.com/track-impression", got.eventTracker(1).url());

		assertEquals(2, got.universalAdIdCount());
		assertEquals("id-registry-1.com", got.universalAdIdRegistry(0));
		assertEquals("id-1", got.universalAdIdValue(0));
		assertEquals("id-registry-2.com", got.universalAdIdRegistry(1));
		assertEquals("id-2", got.universalAdIdValue(1));

		got = PlaybackCreative.ofLinear();
		assertEquals("", got.id());
		assertEquals(0, got.sequence());
		assertEquals(AdComEnums.CompanionRequirementNone, got.companionRequirement());
		assertEquals(0, got.companionCount());
		assertSame(LinkAsset.of(), got.link());
		assertEquals(0, got.linearPlaybackDurationSeconds());
		assertEquals(0, got.linearSkipOffsetSeconds());
		assertEquals(0, got.linearAssetCount());
		assertEquals(0, got.linearIconCount());
		assertEquals(0, got.eventTrackerCount());
		assertEquals(0, got.universalAdIdCount());
	}

	@Test
	public void testSerdeOverlay() throws InvalidProtocolBufferException {
		PlaybackCreative exp = PlaybackCreative.ofOverlayBuilder()
			.id("overlay-id")
			.sequence(123)
			.companionRequirement(AdComEnums.CompanionRequirementAll)
			.companions(EXPECT_COMPANIONS)
			.overlayDisplay(
				DisplayAd.ofDisplayAdBuilder()
					.widthDp(231)
					.heightDp(564)
					.bannerHtmlCreativeMarkup("<html>overlay</html>")
					.build()
			)
			.build();
		PlaybackCreative got = PlaybackCreative.ofProtobuf(new ProtobufReader(
			AdcomMedia.PlaybackCreative.parseFrom(ProtobufWriter.serialize(exp))
				.toByteString()
				.asReadOnlyByteBuffer()
		));

		assertFalse(got.isLinear());
		assertTrue(got.isOverlay());
		assertEquals("overlay-id", got.id());
		assertEquals(123, got.sequence());
		assertEquals(AdComEnums.CompanionRequirementAll, got.companionRequirement());
		assertCompanion(got);

		DisplayAd gotDisplay = got.overlayDisplay();

		assertEquals(AdComEnums.DisplayCreativeHtml, gotDisplay.creativeType());
		assertEquals(231, gotDisplay.widthDp());
		assertEquals(564, gotDisplay.heightDp());
		assertEquals("<html>overlay</html>", gotDisplay.bannerHtmlMarkup());

		got = PlaybackCreative.ofOverlay();
		assertEquals("", got.id());
		assertEquals(0, got.sequence());
		assertEquals(AdComEnums.CompanionRequirementNone, got.companionRequirement());
		assertEquals(0, got.companionCount());
		assertSame(LinkAsset.of(), got.link());
		assertSame(DisplayAd.ofDisplayAd(), got.overlayDisplay());
	}
}
