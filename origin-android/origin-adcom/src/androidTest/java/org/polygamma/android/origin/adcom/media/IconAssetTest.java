// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.adcom.media;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
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

/**
 * {@link IconAsset} tests.
 */
@RunWith(AndroidJUnit4.class)
public class IconAssetTest {
	@Test
	public void testSerde() throws InvalidProtocolBufferException {
		IconAsset exp = IconAsset.ofBuilder()
			.programName("program-name")
			.alternativeText("alternative-text")
			.tooltipText("tooltip-text")
			.showDurationSeconds(123)
			.offsetDurationSeconds(456)
			.pixelRatio(1.2f)
			.widthPx(789)
			.heightPx(987)
			.xOffsetDp(232)
			.yOffsetDp(-323)
			.requiredAdApi(AdComEnums.AdApiOmid10)
			.display(Arrays.asList(
				IconDisplayAsset.ofImageUrlAsset("image/jpeg", "https://image.com"),
				IconDisplayAsset.ofHtmlMarkupAsset("<html></html>"),
				IconDisplayAsset.ofIframeUrlAsset("https://iframe.com")
			))
			.link(LinkAsset.of(
				"https://primary-link.com",
				"https://fallback-link.com",
				Arrays.asList("https://click-track-1.com", "https://click-track-2.com")
			))
			.eventTrackers(Arrays.asList(
				AdEventTracker.ofBuilder()
					.event(AdComEnums.AdEventActivated)
					.type(AdComEnums.AdEventTrackerJavaScript)
					.url("https://activated-track.com")
					.build(),
				AdEventTracker.ofBuilder()
					.event(AdComEnums.AdEventLoaded)
					.type(AdComEnums.AdEventTrackerPixel)
					.url("https://loaded-track.com")
					.build()
			))
			.build();
		IconAsset got = IconAsset.ofProtobuf(new ProtobufReader(
			AdcomMedia.IconAsset.parseFrom(ProtobufWriter.serialize(exp))
				.toByteString()
				.asReadOnlyByteBuffer()
		));

		assertEquals("program-name", got.programName());
		assertEquals("alternative-text", got.alternativeText());
		assertEquals("tooltip-text", got.tooltipText());
		assertEquals(123, got.showDurationSeconds());
		assertEquals(456, got.offsetDurationSeconds());
		assertEquals(1.2f, got.pixelRatio(), 0);
		assertEquals(789, got.widthPx());
		assertEquals(987, got.heightPx());
		assertEquals(232, got.xOffsetDp());
		assertEquals(-323, got.yOffsetDp());
		assertEquals(AdComEnums.AdApiOmid10, got.requiredAdApi());
		assertEquals(3, got.displayCount());
		assertTrue(got.display(0).isImageUrlAsset());
		assertEquals("image/jpeg", got.display(0).mime());
		assertEquals("https://image.com", got.display(0).imageUrl());
		assertTrue(got.display(1).isHtmlMarkupAsset());
		assertEquals("", got.display(1).mime());
		assertEquals("<html></html>", got.display(1).htmlMarkup());
		assertTrue(got.display(2).isIframeUrlAsset());
		assertEquals("", got.display(2).mime());
		assertEquals("https://iframe.com", got.display(2).iframeUrl());
		assertEquals("https://primary-link.com", got.link().primaryUrl());
		assertEquals("https://fallback-link.com", got.link().fallbackUrl());
		assertEquals(2, got.link().trackerUrlCount());
		assertEquals("https://click-track-1.com", got.link().trackerUrl(0));
		assertEquals("https://click-track-2.com", got.link().trackerUrl(1));
		assertEquals(2, got.eventTrackerCount());
		assertEquals(AdComEnums.AdEventActivated, got.eventTracker(0).event());
		assertEquals(AdComEnums.AdEventTrackerJavaScript, got.eventTracker(0).type());
		assertEquals("https://activated-track.com", got.eventTracker(0).url());
		assertEquals(AdComEnums.AdEventLoaded, got.eventTracker(1).event());
		assertEquals(AdComEnums.AdEventTrackerPixel, got.eventTracker(1).type());
		assertEquals("https://loaded-track.com", got.eventTracker(1).url());

		got = IconAsset.of();
		assertEquals("", got.programName());
		assertEquals("", got.alternativeText());
		assertEquals("", got.tooltipText());
		assertEquals(0, got.showDurationSeconds());
		assertEquals(0, got.offsetDurationSeconds());
		assertEquals(0, got.pixelRatio(), 0);
		assertEquals(0, got.widthPx());
		assertEquals(0, got.heightPx());
		assertEquals(0, got.xOffsetDp());
		assertEquals(0, got.yOffsetDp());
		assertEquals(AdComEnums.AdApiUnknown, got.requiredAdApi());
		assertEquals(0, got.displayCount());
		assertSame(LinkAsset.of(), got.link());
		assertEquals(0, got.eventTrackerCount());
	}
}
