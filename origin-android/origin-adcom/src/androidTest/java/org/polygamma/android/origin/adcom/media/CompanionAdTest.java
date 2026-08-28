// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.adcom.media;

import static org.junit.Assert.assertEquals;
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

/**
 * {@link CompanionAd} tests.
 */
@RunWith(AndroidJUnit4.class)
public class CompanionAdTest {
	@Test
	public void testSerde() throws InvalidProtocolBufferException {
		ArrayMap<String, String> expUnivIds = new ArrayMap<>();

		expUnivIds.put("registry-1.com", "id-1");
		expUnivIds.put("registry-2.com", "id-2");

		CompanionAd exp = CompanionAd.of(
			"test-placement-id",
			DisplayAd.ofDisplayAdBuilder()
				.requiredAdApis(AdComEnums.AdApiMraid30)
				.widthDp(123)
				.heightDp(456)
				.widthRatio(12)
				.heightRatio(21)
				.buyerPrivacyPolicyUrl("https://privacy.com")
				.bannerImageCreative(
					LinkAsset.of(
						"https://primary.com",
						"https://secondary.com",
						Arrays.asList(
							"https://click-tracker-1.com/",
							"https://click-tracker-2.com/"
						)
					),
					"https://banner.com/image.png"
				)
				.eventTrackers(Arrays.asList(
					AdEventTracker.ofBuilder()
						.event(AdComEnums.AdEventActivated)
						.type(AdComEnums.AdEventTrackerPixel)
						.url("https://click-tracker-1.com")
						.build(),
					AdEventTracker.ofBuilder()
						.event(AdComEnums.AdEventError)
						.type(AdComEnums.AdEventTrackerPixel)
						.url("https://error-tracker-1.com")
						.build()
				))
				.minShowDurationSeconds(789)
				.universalAdIds(expUnivIds)
				.build(),
			true
		);
		CompanionAd got = CompanionAd.ofProtobuf(new ProtobufReader(
			AdcomMedia.CompanionAd.parseFrom(ProtobufWriter.serialize(exp))
				.toByteString()
				.asReadOnlyByteBuffer()
		));

		assertEquals("test-placement-id", got.placementId());
		assertTrue(got.endCard());

		DisplayAd gotDisplay = got.display();

		assertTrue(gotDisplay.isAdApiRequired(AdComEnums.AdApiMraid30));
		assertEquals(123, gotDisplay.widthDp());
		assertEquals(456, gotDisplay.heightDp());
		assertEquals(12, gotDisplay.widthRatio());
		assertEquals(21, gotDisplay.heightRatio());
		assertEquals("https://privacy.com", gotDisplay.buyerPrivacyPolicyUrl());
		assertEquals(AdComEnums.DisplayCreativeImage, gotDisplay.creativeType());
		assertEquals("https://banner.com/image.png", gotDisplay.bannerImageUrl());
		assertEquals("https://primary.com", gotDisplay.link().primaryUrl());
		assertEquals("https://secondary.com", gotDisplay.link().fallbackUrl());
		assertEquals(2, gotDisplay.link().trackerUrlCount());
		assertEquals("https://click-tracker-1.com/", gotDisplay.link().trackerUrl(0));
		assertEquals("https://click-tracker-2.com/", gotDisplay.link().trackerUrl(1));
		assertEquals(2, gotDisplay.eventTrackerCount());
		assertEquals(AdComEnums.AdEventActivated, gotDisplay.eventTracker(0).event());
		assertEquals(AdComEnums.AdEventTrackerPixel, gotDisplay.eventTracker(0).type());
		assertEquals("https://click-tracker-1.com", gotDisplay.eventTracker(0).url());
		assertEquals(AdComEnums.AdEventError, gotDisplay.eventTracker(1).event());
		assertEquals(AdComEnums.AdEventTrackerPixel, gotDisplay.eventTracker(1).type());
		assertEquals("https://error-tracker-1.com", gotDisplay.eventTracker(1).url());
		assertEquals(789, gotDisplay.minShowDurationSeconds());
		assertEquals(2, gotDisplay.universalAdIdCount());
		assertEquals("registry-1.com", gotDisplay.universalAdIdRegistry(0));
		assertEquals("id-1", gotDisplay.universalAdIdValue(0));
		assertEquals("registry-2.com", gotDisplay.universalAdIdRegistry(1));
		assertEquals("id-2", gotDisplay.universalAdIdValue(1));
	}
}
