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
 * {@link DisplayAdFormat} tests.
 */
@RunWith(AndroidJUnit4.class)
public class DisplayAdFormatTest {
	@Test
	public void testSerde() throws InvalidProtocolBufferException {
		DisplayAdFormat exp = DisplayAdFormat.ofBuilder()
			.supportedMimes(Arrays.asList("text/html", "application/javascript", "text/css"))
			.supportedAdApis(AdComEnums.AdApiMraid30, AdComEnums.AdApiOmid10)
			.activationBehavior(AdComEnums.ActivationEmbeddedBrowser)
			.widthDp(123)
			.heightDp(456)
			.nativeAssets(Arrays.asList(
				NativeAssetFormat.ofTitleAsset(1, true, 321),
				NativeAssetFormat.ofDataAsset(2, false, AdComEnums.NativeDataAssetCtaText, 80),
				NativeAssetFormat.ofImageAsset(
					3,
					true,
					AdComEnums.NativeImageAssetMain,
					32,
					64,
					Arrays.asList("image/png", "image/jpeg")
				)
			))
			.interstitial(true)
			.build();
		DisplayAdFormat got = DisplayAdFormat.ofProtobuf(new ProtobufReader(
			AdcomPlacement.DisplayAdFormat.parseFrom(ProtobufWriter.serialize(exp))
				.toByteString()
				.asReadOnlyByteBuffer()
		));

		assertEquals(3, exp.supportedMimeCount());
		assertEquals("text/html", exp.supportedMime(0));
		assertEquals("application/javascript", exp.supportedMime(1));
		assertEquals("text/css", exp.supportedMime(2));
		assertFalse(got.isAdApiSupported(AdComEnums.AdApiMraid10));
		assertFalse(got.isAdApiSupported(AdComEnums.AdApiMraid20));
		assertTrue(got.isAdApiSupported(AdComEnums.AdApiMraid30));
		assertTrue(got.isAdApiSupported(AdComEnums.AdApiOmid10));
		assertFalse(got.isAdApiSupported(AdComEnums.AdApiOrmma));
		assertFalse(got.isAdApiSupported(AdComEnums.AdApiSimid10));
		assertFalse(got.isAdApiSupported(AdComEnums.AdApiSimid11));
		assertFalse(got.isAdApiSupported(AdComEnums.AdApiUnknown));
		assertFalse(got.isAdApiSupported(AdComEnums.AdApiVpaid10));
		assertFalse(got.isAdApiSupported(AdComEnums.AdApiVpaid20));
		assertEquals(AdComEnums.ActivationEmbeddedBrowser, got.activationBehavior());
		assertEquals(123, got.widthDp());
		assertEquals(456, got.heightDp());
		assertEquals(3, got.nativeAssetCount());

		NativeAssetFormat gotAsset = got.nativeAsset(0);

		assertTrue(gotAsset.isTitleAsset());
		assertEquals(1, gotAsset.id());
		assertTrue(gotAsset.required());
		assertEquals(321, gotAsset.maxTitleTextLength());

		gotAsset = got.nativeAsset(1);
		assertTrue(gotAsset.isDataAsset());
		assertEquals(2, gotAsset.id());
		assertFalse(gotAsset.required());
		assertEquals(AdComEnums.NativeDataAssetCtaText, gotAsset.dataAssetType());
		assertEquals(80, gotAsset.maxDataValueLength());

		gotAsset = got.nativeAsset(2);
		assertTrue(gotAsset.isImageAsset());
		assertEquals(3, gotAsset.id());
		assertTrue(gotAsset.required());
		assertEquals(AdComEnums.NativeImageAssetMain, gotAsset.imageAssetType());
		assertEquals(32, gotAsset.imageWidthDp());
		assertEquals(64, gotAsset.imageHeightDp());
		assertEquals(2, gotAsset.supportedImageMimeCount());
		assertEquals("image/png", gotAsset.supportedImageMime(0));
		assertEquals("image/jpeg", gotAsset.supportedImageMime(1));

		assertTrue(got.interstitial());

		got = DisplayAdFormat.of();
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
		assertEquals(AdComEnums.ActivationNone, got.activationBehavior());
		assertEquals(0, got.widthDp());
		assertEquals(0, got.heightDp());
		assertEquals(0, got.nativeAssetCount());
		assertFalse(got.interstitial());
	}
}
