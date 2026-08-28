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
import java.util.Collections;

/**
 * {@link NativeAsset} tests.
 */
@RunWith(AndroidJUnit4.class)
public class NativeAssetTest {

	private static final LinkAsset EXPECT_LINK = LinkAsset.of(
		"https://primary.com",
		"https://fallback.com",
		Arrays.asList("https://click-track-1.com", "https://click-track-2.com")
	);

	private static void assertLink(NativeAsset got) {
		assertEquals("https://primary.com", got.link().primaryUrl());
		assertEquals("https://fallback.com", got.link().fallbackUrl());
		assertEquals(2, got.link().trackerUrlCount());
		assertEquals("https://click-track-1.com", got.link().trackerUrl(0));
		assertEquals("https://click-track-2.com", got.link().trackerUrl(1));
	}

	@Test
	public void testSerdeDataAsset() throws InvalidProtocolBufferException {
		NativeAsset exp = NativeAsset.ofDataAsset(
			123,
			true,
			EXPECT_LINK,
			AdComEnums.NativeDataAssetCtaText,
			"cta"
		);
		NativeAsset got = NativeAsset.ofProtobuf(new ProtobufReader(
			AdcomMedia.NativeAsset.parseFrom(ProtobufWriter.serialize(exp))
				.toByteString()
				.asReadOnlyByteBuffer()
		));

		assertTrue(got.isDataAsset());
		assertFalse(got.isTitleAsset());
		assertFalse(got.isImageAsset());
		assertFalse(got.isVideoAsset());
		assertEquals(123, got.id());
		assertTrue(got.required());
		assertEquals(AdComEnums.NativeDataAssetCtaText, got.dataAssetType());
		assertEquals("cta", got.dataValue());
		assertLink(got);
	}

	@Test
	public void testSerdeTitleAsset() throws InvalidProtocolBufferException {
		NativeAsset exp = NativeAsset.ofTitleAsset(123, true, EXPECT_LINK, "title");
		NativeAsset got = NativeAsset.ofProtobuf(new ProtobufReader(
			AdcomMedia.NativeAsset.parseFrom(ProtobufWriter.serialize(exp))
				.toByteString()
				.asReadOnlyByteBuffer()
		));

		assertFalse(got.isDataAsset());
		assertTrue(got.isTitleAsset());
		assertFalse(got.isImageAsset());
		assertFalse(got.isVideoAsset());
		assertEquals(123, got.id());
		assertTrue(got.required());
		assertEquals("title", got.titleText());
		assertLink(got);
	}

	@Test
	public void testSerdeImageAsset() throws InvalidProtocolBufferException {
		NativeAsset exp = NativeAsset.ofImageAsset(
			123,
			true,
			EXPECT_LINK,
			AdComEnums.NativeImageAssetMain,
			456,
			789,
			"https://image.com"
		);
		NativeAsset got = NativeAsset.ofProtobuf(new ProtobufReader(
			AdcomMedia.NativeAsset.parseFrom(ProtobufWriter.serialize(exp))
				.toByteString()
				.asReadOnlyByteBuffer()
		));

		assertFalse(got.isDataAsset());
		assertFalse(got.isTitleAsset());
		assertTrue(got.isImageAsset());
		assertFalse(got.isVideoAsset());
		assertEquals(123, got.id());
		assertTrue(got.required());
		assertEquals(AdComEnums.NativeImageAssetMain, got.imageAssetType());
		assertEquals(456, got.imageWidthDp());
		assertEquals(789, got.imageHeightDp());
		assertEquals("https://image.com", got.imageUrl());
		assertLink(got);
	}

	@Test
	public void testSerdeVideoAsset() throws InvalidProtocolBufferException {
		NativeAsset exp = NativeAsset.ofVideoAsset(
			123,
			true,
			EXPECT_LINK,
			PlaybackAd.ofVideoAdBuilder()
				.titleText("title")
				.descriptionText("description")
				.eventTrackers(Arrays.asList(
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
				))
				.creatives(Arrays.asList(
					PlaybackCreative.ofLinearBuilder()
						.id("linear-1")
						.sequence(123)
						.linearPlaybackDurationSeconds(5456)
						.linearSkipOffsetSeconds(6545)
						.linearAssets(Arrays.asList(
							LinearAsset.ofMediaAsset(
								"https://foo.com/video.mp4",
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
							),
							LinearAsset.ofClosedCaptionAsset(
								"https://foo.com/cc",
								"application/xml",
								"en"
							)
						))
						.build(),
					PlaybackCreative.ofOverlayBuilder()
						.id("overlay-1")
						.sequence(456)
						.overlayDisplay(
							DisplayAd.ofDisplayAdBuilder()
								.widthDp(123)
								.heightDp(456)
								.bannerHtmlCreativeUrl("https://html.com/")
								.minShowDurationSeconds(789)
								.build()
						)
						.build()
				))
				.build()
		);
		NativeAsset got = NativeAsset.ofProtobuf(new ProtobufReader(
			AdcomMedia.NativeAsset.parseFrom(ProtobufWriter.serialize(exp))
				.toByteString()
				.asReadOnlyByteBuffer()
		));

		assertFalse(got.isDataAsset());
		assertFalse(got.isTitleAsset());
		assertFalse(got.isImageAsset());
		assertTrue(got.isVideoAsset());
		assertEquals(123, got.id());
		assertTrue(got.required());
		assertLink(got);

		PlaybackAd gotVideo = got.video();

		assertEquals("title", gotVideo.titleText());
		assertEquals("description", gotVideo.descriptionText());
		assertEquals(2, gotVideo.eventTrackerCount());
		assertEquals(AdComEnums.AdEventLoaded, gotVideo.eventTracker(0).event());
		assertEquals(AdComEnums.AdEventTrackerPixel, gotVideo.eventTracker(0).type());
		assertEquals("https://loaded-track.com", gotVideo.eventTracker(0).url());
		assertEquals(AdComEnums.AdEventImpression, gotVideo.eventTracker(1).event());
		assertEquals(AdComEnums.AdEventTrackerPixel, gotVideo.eventTracker(1).type());
		assertEquals("https://impression-track.com", gotVideo.eventTracker(1).url());

		assertEquals(2, gotVideo.creativeCount());
		assertTrue(gotVideo.creative(0).isLinear());
		assertEquals("linear-1", gotVideo.creative(0).id());
		assertEquals(123, gotVideo.creative(0).sequence());
		assertEquals(5456, gotVideo.creative(0).linearPlaybackDurationSeconds());
		assertEquals(6545, gotVideo.creative(0).linearSkipOffsetSeconds());

		assertEquals(2, gotVideo.creative(0).linearAssetCount());

		LinearAsset gotLinearAsset = gotVideo.creative(0).linearAsset(0);

		assertTrue(gotLinearAsset.isMediaAsset());
		assertEquals("https://foo.com/video.mp4", gotLinearAsset.url());
		assertEquals("video/mp4", gotLinearAsset.mime());
		assertEquals("media-id", gotLinearAsset.playbackId());
		assertEquals("codec-name", gotLinearAsset.playbackCodec());
		assertEquals(123, gotLinearAsset.playbackAverageBitRateKbps());
		assertEquals(456, gotLinearAsset.playbackMinBitRateKbps());
		assertEquals(789, gotLinearAsset.playbackMaxBitRateKbps());
		assertEquals(
			AdComEnums.PlaybackDeliveryStreaming,
			gotLinearAsset.playbackSupportedDelivery()
		);
		assertEquals(234, gotLinearAsset.playbackWidthPx());
		assertEquals(432, gotLinearAsset.playbackHeightPx());
		assertTrue(gotLinearAsset.playbackCanScale());
		assertTrue(gotLinearAsset.playbackMaintainAspectRatio());

		gotLinearAsset = gotVideo.creative(0).linearAsset(1);
		assertTrue(gotLinearAsset.isClosedCaptionAsset());
		assertEquals("application/xml", gotLinearAsset.mime());
		assertEquals("https://foo.com/cc", gotLinearAsset.url());
		assertEquals("en", gotLinearAsset.closedCaptionLanguageCode());

		assertTrue(gotVideo.creative(1).isOverlay());
		assertEquals("overlay-1", gotVideo.creative(1).id());

		DisplayAd gotOverlay = gotVideo.creative(1).overlayDisplay();

		assertEquals(123, gotOverlay.widthDp());
		assertEquals(456, gotOverlay.heightDp());
		assertEquals(AdComEnums.DisplayCreativeHtml, gotOverlay.creativeType());
		assertEquals("https://html.com/", gotOverlay.bannerHtmlUrl());
		assertEquals(789, gotOverlay.minShowDurationSeconds());
	}
}
