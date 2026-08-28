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
 * {@link NativeAssetFormat} tests.
 */
@RunWith(AndroidJUnit4.class)
public class NativeAssetFormatTest {
	@Test
	public void testSerdeData() throws InvalidProtocolBufferException {
		NativeAssetFormat exp =
			NativeAssetFormat.ofDataAsset(123, true, AdComEnums.NativeDataAssetAddress, 456);
		NativeAssetFormat got = NativeAssetFormat.ofProtobuf(new ProtobufReader(
			AdcomPlacement.NativeAssetFormat.parseFrom(ProtobufWriter.serialize(exp))
				.toByteString()
				.asReadOnlyByteBuffer()
		));

		assertTrue(got.isDataAsset());
		assertFalse(got.isImageAsset());
		assertFalse(got.isTitleAsset());
		assertFalse(got.isVideoAsset());

		assertEquals(123, got.id());
		assertTrue(got.required());
		assertEquals(AdComEnums.NativeDataAssetAddress, exp.dataAssetType());
		assertEquals(456, got.maxDataValueLength());
	}

	@Test
	public void testSerdeImage() throws InvalidProtocolBufferException {
		NativeAssetFormat exp = NativeAssetFormat.ofImageAsset(
			123,
			true,
			AdComEnums.NativeImageAssetMain,
			456,
			789,
			Arrays.asList("image/png", "image/jpeg")
		);
		NativeAssetFormat got = NativeAssetFormat.ofProtobuf(new ProtobufReader(
			AdcomPlacement.NativeAssetFormat.parseFrom(ProtobufWriter.serialize(exp))
				.toByteString()
				.asReadOnlyByteBuffer()
		));

		assertFalse(got.isDataAsset());
		assertTrue(got.isImageAsset());
		assertFalse(got.isTitleAsset());
		assertFalse(got.isVideoAsset());

		assertEquals(123, got.id());
		assertTrue(got.required());
		assertEquals(AdComEnums.NativeImageAssetMain, got.imageAssetType());
		assertEquals(456, got.imageWidthDp());
		assertEquals(789, got.imageHeightDp());
		assertEquals(2, got.supportedImageMimeCount());
		assertEquals("image/png", got.supportedImageMime(0));
		assertEquals("image/jpeg", got.supportedImageMime(1));
	}

	@Test
	public void testSerdeTitle() throws InvalidProtocolBufferException {
		NativeAssetFormat exp = NativeAssetFormat.ofTitleAsset(123, true, 456);
		NativeAssetFormat got = NativeAssetFormat.ofProtobuf(new ProtobufReader(
			AdcomPlacement.NativeAssetFormat.parseFrom(ProtobufWriter.serialize(exp))
				.toByteString()
				.asReadOnlyByteBuffer()
		));

		assertFalse(got.isDataAsset());
		assertFalse(got.isImageAsset());
		assertTrue(got.isTitleAsset());
		assertFalse(got.isVideoAsset());

		assertEquals(123, got.id());
		assertTrue(got.required());
		assertEquals(456, got.maxTitleTextLength());
	}

	@Test
	public void testSerdeVideo() throws InvalidProtocolBufferException {
		NativeAssetFormat exp = NativeAssetFormat.ofVideoAsset(
			123,
			true,
			PlaybackAdFormat.ofVideoAdBuilder()
				.supportedMimes(Arrays.asList("text/html", "video/mp4", "video/ogg"))
				.supportedAdApis(AdComEnums.AdApiMraid30, AdComEnums.AdApiSimid11)
				.minBitRateKbps(123)
				.maxBitRateKbps(456)
				.videoPlayerWidthDp(789)
				.videoPlayerHeightDp(987)
				.videoActivationBehavior(AdComEnums.ActivationEmbeddedBrowser)
				.skippable(true)
				.build()
		);
		NativeAssetFormat got = NativeAssetFormat.ofProtobuf(new ProtobufReader(
			AdcomPlacement.NativeAssetFormat.parseFrom(ProtobufWriter.serialize(exp))
				.toByteString()
				.asReadOnlyByteBuffer()
		));

		assertFalse(got.isDataAsset());
		assertFalse(got.isImageAsset());
		assertFalse(got.isTitleAsset());
		assertTrue(got.isVideoAsset());

		assertEquals(123, got.id());
		assertTrue(got.required());

		PlaybackAdFormat gotVideo = got.video();

		assertFalse(gotVideo.isAudioAd());
		assertTrue(gotVideo.isVideoAd());
		assertEquals(3, gotVideo.supportedMimeCount());
		assertEquals("text/html", gotVideo.supportedMime(0));
		assertEquals("video/mp4", gotVideo.supportedMime(1));
		assertEquals("video/ogg", gotVideo.supportedMime(2));
		assertFalse(gotVideo.isAdApiSupported(AdComEnums.AdApiMraid10));
		assertFalse(gotVideo.isAdApiSupported(AdComEnums.AdApiMraid20));
		assertTrue(gotVideo.isAdApiSupported(AdComEnums.AdApiMraid30));
		assertFalse(gotVideo.isAdApiSupported(AdComEnums.AdApiOmid10));
		assertFalse(gotVideo.isAdApiSupported(AdComEnums.AdApiOrmma));
		assertFalse(gotVideo.isAdApiSupported(AdComEnums.AdApiSimid10));
		assertTrue(gotVideo.isAdApiSupported(AdComEnums.AdApiSimid11));
		assertFalse(gotVideo.isAdApiSupported(AdComEnums.AdApiUnknown));
		assertFalse(gotVideo.isAdApiSupported(AdComEnums.AdApiVpaid10));
		assertFalse(gotVideo.isAdApiSupported(AdComEnums.AdApiVpaid20));
		assertEquals(123, gotVideo.minBitRateKbps());
		assertEquals(456, gotVideo.maxBitRateKbps());
		assertEquals(789, gotVideo.videoPlayerWidthDp());
		assertEquals(987, gotVideo.videoPlayerHeightDp());
		assertEquals(AdComEnums.ActivationEmbeddedBrowser, gotVideo.videoActivationBehavior());
		assertTrue(gotVideo.skippable());
	}
}
