// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.adcom.media;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.polygamma.android.origin.adcom.TestUtil;
import org.polygamma.origin.adcom.AdcomMedia;

/**
 * {@link IconDisplayAsset} tests.
 */
@RunWith(AndroidJUnit4.class)
public class IconDisplayAssetTest {
	@Test
	public void testSerdeHtmlMakrup() {
		IconDisplayAsset exp = IconDisplayAsset.ofHtmlMarkupAsset("<html></html>");
		IconDisplayAsset got = TestUtil.encodeAndDecode(
			exp,
			IconDisplayAsset::toProtobuf,
			IconDisplayAsset::ofProtobuf,
			AdcomMedia.IconDisplayAsset::parseFrom
		);

		assertTrue(got.isHtmlMarkupAsset());
		assertFalse(got.isIframeUrlAsset());
		assertFalse(got.isImageUrlAsset());
		assertEquals("", got.mime());
		assertEquals("<html></html>", got.htmlMarkup());
	}

	@Test
	public void testSerdeIframeUrl() {
		IconDisplayAsset exp = IconDisplayAsset.ofIframeUrlAsset("https://foo.com");
		IconDisplayAsset got = TestUtil.encodeAndDecode(
			exp,
			IconDisplayAsset::toProtobuf,
			IconDisplayAsset::ofProtobuf,
			AdcomMedia.IconDisplayAsset::parseFrom
		);

		assertFalse(got.isHtmlMarkupAsset());
		assertTrue(got.isIframeUrlAsset());
		assertFalse(got.isImageUrlAsset());
		assertEquals("", got.mime());
		assertEquals("https://foo.com", got.iframeUrl());
	}

	@Test
	public void testSerdeImageUrl() {
		IconDisplayAsset exp = IconDisplayAsset.ofImageUrlAsset("image/jpeg", "https://foo.com");
		IconDisplayAsset got = TestUtil.encodeAndDecode(
			exp,
			IconDisplayAsset::toProtobuf,
			IconDisplayAsset::ofProtobuf,
			AdcomMedia.IconDisplayAsset::parseFrom
		);

		assertFalse(got.isHtmlMarkupAsset());
		assertFalse(got.isIframeUrlAsset());
		assertTrue(got.isImageUrlAsset());
		assertEquals("image/jpeg", got.mime());
		assertEquals("https://foo.com", got.imageUrl());
	}
}
