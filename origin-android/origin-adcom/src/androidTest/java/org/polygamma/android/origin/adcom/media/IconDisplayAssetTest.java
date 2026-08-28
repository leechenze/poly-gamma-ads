// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.adcom.media;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.protobuf.InvalidProtocolBufferException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.polygamma.android.origin.protobuf.ProtobufReader;
import org.polygamma.android.origin.protobuf.ProtobufWriter;
import org.polygamma.origin.adcom.AdcomMedia;

/**
 * {@link IconDisplayAsset} tests.
 */
@RunWith(AndroidJUnit4.class)
public class IconDisplayAssetTest {
	@Test
	public void testSerdeHtmlMakrup() throws InvalidProtocolBufferException {
		IconDisplayAsset exp = IconDisplayAsset.ofHtmlMarkupAsset("<html></html>");
		IconDisplayAsset got = IconDisplayAsset.ofProtobuf(new ProtobufReader(
			AdcomMedia.IconDisplayAsset.parseFrom(ProtobufWriter.serialize(exp))
				.toByteString()
				.asReadOnlyByteBuffer()
		));

		assertTrue(got.isHtmlMarkupAsset());
		assertFalse(got.isIframeUrlAsset());
		assertFalse(got.isImageUrlAsset());
		assertEquals("", got.mime());
		assertEquals("<html></html>", got.htmlMarkup());
	}

	@Test
	public void testSerdeIframeUrl() throws InvalidProtocolBufferException {
		IconDisplayAsset exp = IconDisplayAsset.ofIframeUrlAsset("https://foo.com");
		IconDisplayAsset got = IconDisplayAsset.ofProtobuf(new ProtobufReader(
			AdcomMedia.IconDisplayAsset.parseFrom(ProtobufWriter.serialize(exp))
				.toByteString()
				.asReadOnlyByteBuffer()
		));

		assertFalse(got.isHtmlMarkupAsset());
		assertTrue(got.isIframeUrlAsset());
		assertFalse(got.isImageUrlAsset());
		assertEquals("", got.mime());
		assertEquals("https://foo.com", got.iframeUrl());
	}

	@Test
	public void testSerdeImageUrl() throws InvalidProtocolBufferException {
		IconDisplayAsset exp = IconDisplayAsset.ofImageUrlAsset("image/jpeg", "https://foo.com");
		IconDisplayAsset got = IconDisplayAsset.ofProtobuf(new ProtobufReader(
			AdcomMedia.IconDisplayAsset.parseFrom(ProtobufWriter.serialize(exp))
				.toByteString()
				.asReadOnlyByteBuffer()
		));

		assertFalse(got.isHtmlMarkupAsset());
		assertFalse(got.isIframeUrlAsset());
		assertTrue(got.isImageUrlAsset());
		assertEquals("image/jpeg", got.mime());
		assertEquals("https://foo.com", got.imageUrl());
	}
}
