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
 * {@link DisplayAd} tests.
 */
@RunWith(AndroidJUnit4.class)
public class DisplayAdTest {

	private static final ArrayMap<String, String> EXPECT_UNIVERSAL_AD_IDS = new ArrayMap<>();
	private static final LinkAsset EXPECT_LINK = LinkAsset.of(
		"https://primary.com",
		"https://fallback.com",
		Arrays.asList(
			"https://link-tracker-1.com",
			"https://link-tracker-2.com"
		)
	);
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
	private static final List<IconAsset> EXPECT_ICON_ASSETS = Arrays.asList(
		IconAsset.ofBuilder()
			.programName("icon-1")
			.display(Collections.singleton(
				IconDisplayAsset.ofHtmlMarkupAsset("<html>icon-1</html>")
			))
			.build(),
		IconAsset.ofBuilder()
			.programName("icon-2")
			.display(Collections.singleton(
				IconDisplayAsset.ofHtmlMarkupAsset("<html>icon-2</html>")
			))
			.build()
	);

	static {
		EXPECT_UNIVERSAL_AD_IDS.put("registry-1.com", "id-1");
		EXPECT_UNIVERSAL_AD_IDS.put("registry-2.com", "id-2");
	}

	private static void assertCommonExpect(DisplayAd got) {
		if (
			got.creativeType() == AdComEnums.DisplayCreativeImage ||
			got.creativeType() == AdComEnums.DisplayCreativeNative
		) {
			assertEquals("https://primary.com", got.link().primaryUrl());
			assertEquals("https://fallback.com", got.link().fallbackUrl());
			assertEquals(2, got.link().trackerUrlCount());
			assertEquals("https://link-tracker-1.com", got.link().trackerUrl(0));
			assertEquals("https://link-tracker-2.com", got.link().trackerUrl(1));
		}

		assertEquals(2, got.eventTrackerCount());
		assertEquals(AdComEnums.AdEventActivated, got.eventTracker(0).event());
		assertEquals(AdComEnums.AdEventTrackerJavaScript, got.eventTracker(0).type());
		assertEquals("https://tracker-1.com", got.eventTracker(0).url());
		assertEquals(AdComEnums.AdEventError, got.eventTracker(1).event());
		assertEquals(AdComEnums.AdEventTrackerPixel, got.eventTracker(1).type());
		assertEquals("https://tracker-2.com", got.eventTracker(1).url());

		assertEquals(2, got.universalAdIdCount());
		assertEquals("registry-1.com", got.universalAdIdRegistry(0));
		assertEquals("id-1", got.universalAdIdValue(0));
		assertEquals("registry-2.com", got.universalAdIdRegistry(1));
		assertEquals("id-2", got.universalAdIdValue(1));

		assertEquals(2, got.iconCount());
		assertEquals("icon-1", got.icon(0).programName());
		assertEquals(1, got.icon(0).displayCount());
		assertEquals("<html>icon-1</html>", got.icon(0).display(0).htmlMarkup());
		assertEquals("icon-2", got.icon(1).programName());
		assertEquals(1, got.icon(1).displayCount());
		assertEquals("<html>icon-2</html>", got.icon(1).display(0).htmlMarkup());

		got = DisplayAd.ofDisplayAd();
		assertEquals("", got.id());
		assertEquals("", got.serveId());
		assertEquals(0, got.advertiserDomainCount());
		assertEquals(0, got.advertiserAppBundleCount());
		assertEquals(0, got.eventTrackerCount());
		assertFalse(got.secure());
		assertFalse(got.isAdApiRequired(AdComEnums.AdApiMraid10));
		assertFalse(got.isAdApiRequired(AdComEnums.AdApiMraid20));
		assertFalse(got.isAdApiRequired(AdComEnums.AdApiMraid30));
		assertFalse(got.isAdApiRequired(AdComEnums.AdApiOmid10));
		assertFalse(got.isAdApiRequired(AdComEnums.AdApiOrmma));
		assertFalse(got.isAdApiRequired(AdComEnums.AdApiSimid10));
		assertFalse(got.isAdApiRequired(AdComEnums.AdApiSimid11));
		assertFalse(got.isAdApiRequired(AdComEnums.AdApiUnknown));
		assertFalse(got.isAdApiRequired(AdComEnums.AdApiVpaid10));
		assertFalse(got.isAdApiRequired(AdComEnums.AdApiVpaid20));
		assertEquals(0, got.minShowDurationSeconds());
		assertEquals("", got.buyerPrivacyPolicyUrl());
		assertEquals(0, got.universalAdIdCount());
		assertEquals(0, got.iconCount());
		assertEquals(AdComEnums.DisplayCreativeUnknown, got.creativeType());
		assertEquals(0, got.widthDp());
		assertEquals(0, got.heightDp());
		assertEquals(0, got.widthRatio());
		assertEquals(0, got.heightRatio());
	}

	@Test
	public void testSerdeBannerImage() throws InvalidProtocolBufferException {
		DisplayAd exp = DisplayAd.ofDisplayAdBuilder()
			.requiredAdApis(AdComEnums.AdApiMraid10, AdComEnums.AdApiMraid30)
			.widthDp(123)
			.heightDp(456)
			.widthRatio(12)
			.heightRatio(21)
			.buyerPrivacyPolicyUrl("https://privacy.com")
			.bannerImageCreative(EXPECT_LINK, "https://banner.com/image.png")
			.eventTrackers(EXPECT_EVENT_TRACKERS)
			.minShowDurationSeconds(789)
			.universalAdIds(EXPECT_UNIVERSAL_AD_IDS)
			.build();
		DisplayAd got = DisplayAd.ofDisplayAdProtobuf(new ProtobufReader(
			AdcomMedia.DisplayAd.parseFrom(ProtobufWriter.serialize(exp::toDisplayAdProtobuf))
				.toByteString()
				.asReadOnlyByteBuffer()
		));

		assertTrue(got.isAdApiRequired(AdComEnums.AdApiMraid10));
		assertFalse(got.isAdApiRequired(AdComEnums.AdApiMraid20));
		assertTrue(got.isAdApiRequired(AdComEnums.AdApiMraid30));
		assertFalse(got.isAdApiRequired(AdComEnums.AdApiOmid10));
		assertFalse(got.isAdApiRequired(AdComEnums.AdApiOrmma));
		assertFalse(got.isAdApiRequired(AdComEnums.AdApiSimid10));
		assertFalse(got.isAdApiRequired(AdComEnums.AdApiSimid11));
		assertFalse(got.isAdApiRequired(AdComEnums.AdApiUnknown));
		assertFalse(got.isAdApiRequired(AdComEnums.AdApiVpaid10));
		assertFalse(got.isAdApiRequired(AdComEnums.AdApiVpaid20));
		assertEquals(123, got.widthDp());
		assertEquals(456, got.heightDp());
		assertEquals(12, got.widthRatio());
		assertEquals(21, got.heightRatio());
		assertEquals("https://privacy.com", got.buyerPrivacyPolicyUrl());
		assertEquals(AdComEnums.DisplayCreativeImage, got.creativeType());
		assertEquals("https://banner.com/image.png", got.bannerImageUrl());
		assertEquals(789, got.minShowDurationSeconds());
		assertCommonExpect(got);
	}

	@Test
	public void testSerdeHtmlMarkup() throws InvalidProtocolBufferException {
		DisplayAd exp = DisplayAd.ofDisplayAdBuilder()
			.requiredAdApis(AdComEnums.AdApiMraid10, AdComEnums.AdApiMraid30)
			.widthDp(123)
			.heightDp(456)
			.widthRatio(12)
			.heightRatio(21)
			.buyerPrivacyPolicyUrl("https://privacy.com")
			.bannerHtmlCreativeMarkup("<html></html>")
			.eventTrackers(EXPECT_EVENT_TRACKERS)
			.minShowDurationSeconds(789)
			.universalAdIds(EXPECT_UNIVERSAL_AD_IDS)
			.build();
		DisplayAd got = DisplayAd.ofDisplayAdProtobuf(new ProtobufReader(
			AdcomMedia.DisplayAd.parseFrom(ProtobufWriter.serialize(exp::toDisplayAdProtobuf))
				.toByteString()
				.asReadOnlyByteBuffer()
		));

		assertTrue(got.isAdApiRequired(AdComEnums.AdApiMraid10));
		assertFalse(got.isAdApiRequired(AdComEnums.AdApiMraid20));
		assertTrue(got.isAdApiRequired(AdComEnums.AdApiMraid30));
		assertFalse(got.isAdApiRequired(AdComEnums.AdApiOmid10));
		assertFalse(got.isAdApiRequired(AdComEnums.AdApiOrmma));
		assertFalse(got.isAdApiRequired(AdComEnums.AdApiSimid10));
		assertFalse(got.isAdApiRequired(AdComEnums.AdApiSimid11));
		assertFalse(got.isAdApiRequired(AdComEnums.AdApiUnknown));
		assertFalse(got.isAdApiRequired(AdComEnums.AdApiVpaid10));
		assertFalse(got.isAdApiRequired(AdComEnums.AdApiVpaid20));
		assertEquals(123, got.widthDp());
		assertEquals(456, got.heightDp());
		assertEquals(12, got.widthRatio());
		assertEquals(21, got.heightRatio());
		assertEquals("https://privacy.com", got.buyerPrivacyPolicyUrl());
		assertEquals(AdComEnums.DisplayCreativeHtml, got.creativeType());
		assertEquals("<html></html>", got.bannerHtmlMarkup());
		assertEquals(789, got.minShowDurationSeconds());
		assertCommonExpect(got);
	}

	@Test
	public void testSerdeHtmlUrl() throws InvalidProtocolBufferException {
		DisplayAd exp = DisplayAd.ofDisplayAdBuilder()
			.requiredAdApis(AdComEnums.AdApiMraid10, AdComEnums.AdApiMraid30)
			.widthDp(123)
			.heightDp(456)
			.widthRatio(12)
			.heightRatio(21)
			.buyerPrivacyPolicyUrl("https://privacy.com")
			.bannerHtmlCreativeUrl("https://html.com/")
			.eventTrackers(EXPECT_EVENT_TRACKERS)
			.minShowDurationSeconds(789)
			.universalAdIds(EXPECT_UNIVERSAL_AD_IDS)
			.build();
		DisplayAd got = DisplayAd.ofDisplayAdProtobuf(new ProtobufReader(
			AdcomMedia.DisplayAd.parseFrom(ProtobufWriter.serialize(exp::toDisplayAdProtobuf))
				.toByteString()
				.asReadOnlyByteBuffer()
		));

		assertTrue(got.isAdApiRequired(AdComEnums.AdApiMraid10));
		assertFalse(got.isAdApiRequired(AdComEnums.AdApiMraid20));
		assertTrue(got.isAdApiRequired(AdComEnums.AdApiMraid30));
		assertFalse(got.isAdApiRequired(AdComEnums.AdApiOmid10));
		assertFalse(got.isAdApiRequired(AdComEnums.AdApiOrmma));
		assertFalse(got.isAdApiRequired(AdComEnums.AdApiSimid10));
		assertFalse(got.isAdApiRequired(AdComEnums.AdApiSimid11));
		assertFalse(got.isAdApiRequired(AdComEnums.AdApiUnknown));
		assertFalse(got.isAdApiRequired(AdComEnums.AdApiVpaid10));
		assertFalse(got.isAdApiRequired(AdComEnums.AdApiVpaid20));
		assertEquals(123, got.widthDp());
		assertEquals(456, got.heightDp());
		assertEquals(12, got.widthRatio());
		assertEquals(21, got.heightRatio());
		assertEquals("https://privacy.com", got.buyerPrivacyPolicyUrl());
		assertEquals(AdComEnums.DisplayCreativeHtml, got.creativeType());
		assertEquals("https://html.com/", got.bannerHtmlUrl());
		assertEquals(789, got.minShowDurationSeconds());
		assertCommonExpect(got);
	}

	@Test
	public void testSerdeNative() throws InvalidProtocolBufferException {
		DisplayAd exp = DisplayAd.ofDisplayAdBuilder()
			.requiredAdApis(AdComEnums.AdApiMraid10, AdComEnums.AdApiMraid30)
			.widthDp(123)
			.heightDp(456)
			.widthRatio(12)
			.heightRatio(21)
			.buyerPrivacyPolicyUrl("https://privacy.com")
			.nativeCreative(EXPECT_LINK, Arrays.asList(
				NativeAsset.ofTitleAsset(1, true, LinkAsset.of(), "Title"),
				NativeAsset.ofDataAsset(
					2,
					false,
					LinkAsset.of(
						"https://cta-primary.com",
						"https://cta-fallback.com",
						Collections.emptyList()
					),
					AdComEnums.NativeDataAssetCtaText,
					"Click"
				)
			))
			.eventTrackers(EXPECT_EVENT_TRACKERS)
			.minShowDurationSeconds(789)
			.universalAdIds(EXPECT_UNIVERSAL_AD_IDS)
			.build();
		DisplayAd got = DisplayAd.ofDisplayAdProtobuf(new ProtobufReader(
			AdcomMedia.DisplayAd.parseFrom(ProtobufWriter.serialize(exp::toDisplayAdProtobuf))
				.toByteString()
				.asReadOnlyByteBuffer()
		));

		assertTrue(got.isAdApiRequired(AdComEnums.AdApiMraid10));
		assertFalse(got.isAdApiRequired(AdComEnums.AdApiMraid20));
		assertTrue(got.isAdApiRequired(AdComEnums.AdApiMraid30));
		assertFalse(got.isAdApiRequired(AdComEnums.AdApiOmid10));
		assertFalse(got.isAdApiRequired(AdComEnums.AdApiOrmma));
		assertFalse(got.isAdApiRequired(AdComEnums.AdApiSimid10));
		assertFalse(got.isAdApiRequired(AdComEnums.AdApiSimid11));
		assertFalse(got.isAdApiRequired(AdComEnums.AdApiUnknown));
		assertFalse(got.isAdApiRequired(AdComEnums.AdApiVpaid10));
		assertFalse(got.isAdApiRequired(AdComEnums.AdApiVpaid20));
		assertEquals(123, got.widthDp());
		assertEquals(456, got.heightDp());
		assertEquals(12, got.widthRatio());
		assertEquals(21, got.heightRatio());
		assertEquals("https://privacy.com", got.buyerPrivacyPolicyUrl());
		assertEquals(AdComEnums.DisplayCreativeNative, got.creativeType());
		assertEquals(2, got.nativeAssetCount());
		assertTrue(got.nativeAsset(0).isTitleAsset());
		assertEquals("Title", got.nativeAsset(0).titleText());
		assertSame(LinkAsset.of(), got.nativeAsset(0).link());
		assertTrue(got.nativeAsset(1).isDataAsset());
		assertEquals("Click", got.nativeAsset(1).dataValue());
		assertEquals(AdComEnums.NativeDataAssetCtaText, got.nativeAsset(1).dataAssetType());
		assertEquals("https://cta-primary.com", got.nativeAsset(1).link().primaryUrl());
		assertEquals("https://cta-fallback.com", got.nativeAsset(1).link().fallbackUrl());
		assertEquals(0, got.nativeAsset(1).link().trackerUrlCount());
		assertEquals(789, got.minShowDurationSeconds());
		assertCommonExpect(got);
	}
}
