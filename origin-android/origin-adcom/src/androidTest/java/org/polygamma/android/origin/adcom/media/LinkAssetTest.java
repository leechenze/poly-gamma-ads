// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.adcom.media;

import static org.junit.Assert.assertEquals;

import android.util.SparseArray;

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
 * {@link LinkAsset} tests.
 */
@RunWith(AndroidJUnit4.class)
public class LinkAssetTest {
	@Test
	public void testSerde() throws InvalidProtocolBufferException {
		LinkAsset exp = LinkAsset.of("https://primary.com", "https://fallback.com", Arrays.asList(
			"https://click-track-1.com/\00019\0\00010\0foo",
			"https://click-track-2.com/bar\00040\0"
		));
		LinkAsset got = LinkAsset.ofProtobuf(new ProtobufReader(
			AdcomMedia.LinkAsset.parseFrom(ProtobufWriter.serialize(exp))
				.toByteString()
				.asReadOnlyByteBuffer()
		));
		SparseArray<String> macros = new SparseArray<>();

		macros.put(AdComEnums.AdTrackerUrlMacroTransactionId, "123");
		macros.put(AdComEnums.AdTrackerUrlMacroAppBundle, "com.foo.bar");
		macros.put(AdComEnums.AdTrackerUrlMacroAdEventTrackerErrorCode, "100");

		assertEquals("https://primary.com", got.primaryUrl());
		assertEquals("https://fallback.com", got.fallbackUrl());
		assertEquals(2, got.trackerUrlCount());
		assertEquals("https://click-track-1.com/\00019\0\00010\0foo", got.trackerUrl(0));
		assertEquals(
			"https://click-track-1.com/123com.foo.barfoo",
			got.resolveTrackerUrl(0, macros)
		);
		assertEquals("https://click-track-2.com/bar\00040\0", got.trackerUrl(1));
		assertEquals(
			"https://click-track-2.com/bar100",
			got.resolveTrackerUrl(1, macros)
		);
	}
}
