// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.adcom.media;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.util.ArrayMap;
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
import java.util.Locale;

/**
 * {@link AdEventTracker} tests.
 */
@RunWith(AndroidJUnit4.class)
public class AdEventTrackerTest {
	@Test
	public void testSerde() throws InvalidProtocolBufferException {
		ArrayMap<String, String> expVendorData = new ArrayMap<>();

		expVendorData.put("A", "B");
		expVendorData.put("C", "D");

		AdEventTracker exp = AdEventTracker.ofBuilder()
			.event(AdComEnums.AdEventActivated)
			.type(AdComEnums.AdEventTrackerJavaScript)
			.playbackOffsetSeconds(456)
			.url(String.format(
				Locale.ROOT,
				"https://tracker.com/\0%s\0",
				AdComEnums.AdTrackerUrlMacroTransactionId
			))
			.vendorData(expVendorData)
			.errorUrls(Arrays.asList(
				String.format(
					Locale.ROOT,
					"https://error1.com/\0%s\0",
					AdComEnums.AdTrackerUrlMacroAppBundle
				),
				String.format(
					Locale.ROOT,
					"https://error2.com/\0%s\0",
					AdComEnums.AdTrackerUrlMacroAdEventTrackerErrorCode
				)
			))
			.requiredAdApis(AdComEnums.AdApiOmid10, AdComEnums.AdApiOrmma)
			.build();
		AdEventTracker got = AdEventTracker.ofProtobuf(new ProtobufReader(
			AdcomMedia.AdEventTracker.parseFrom(ProtobufWriter.serialize(exp))
				.toByteString()
				.asReadOnlyByteBuffer()
		));
		SparseArray<String> macros = new SparseArray<>();

		macros.put(AdComEnums.AdTrackerUrlMacroTransactionId, "123");
		macros.put(AdComEnums.AdTrackerUrlMacroAppBundle, "com.foo.bar");
		macros.put(AdComEnums.AdTrackerUrlMacroAdEventTrackerErrorCode, "100");

		assertEquals(AdComEnums.AdEventActivated, got.event());
		assertEquals(AdComEnums.AdEventTrackerJavaScript, got.type());
		assertEquals(456, got.playbackOffsetSeconds());
		assertEquals("https://tracker.com/\00019\0", got.url());
		assertEquals("https://tracker.com/123", got.resolveUrl(macros));
		assertEquals(2, got.vendorDataCount());
		assertEquals("A", got.vendorDataKey(0));
		assertEquals("B", got.vendorDataValue(0));
		assertEquals("C", got.vendorDataKey(1));
		assertEquals("D", got.vendorDataValue(1));
		assertEquals(2, got.errorUrlCount());
		assertEquals("https://error1.com/\00010\0", got.errorUrl(0));
		assertEquals("https://error1.com/com.foo.bar", got.resolveErrorUrl(0, macros));
		assertEquals("https://error2.com/\00040\0", got.errorUrl(1));
		assertEquals("https://error2.com/100", got.resolveErrorUrl(1, macros));
		assertFalse(got.isAdApiRequired(AdComEnums.AdApiMraid10));
		assertFalse(got.isAdApiRequired(AdComEnums.AdApiMraid20));
		assertFalse(got.isAdApiRequired(AdComEnums.AdApiMraid30));
		assertTrue(got.isAdApiRequired(AdComEnums.AdApiOmid10));
		assertTrue(got.isAdApiRequired(AdComEnums.AdApiOrmma));
		assertFalse(got.isAdApiRequired(AdComEnums.AdApiSimid10));
		assertFalse(got.isAdApiRequired(AdComEnums.AdApiSimid11));
		assertFalse(got.isAdApiRequired(AdComEnums.AdApiUnknown));
		assertFalse(got.isAdApiRequired(AdComEnums.AdApiVpaid10));
		assertFalse(got.isAdApiRequired(AdComEnums.AdApiVpaid20));

		got = AdEventTracker.of();
		assertEquals(AdComEnums.AdEventUnknown, got.event());
		assertEquals(AdComEnums.AdEventTrackerUnknown, got.type());
		assertEquals(-1, got.playbackOffsetSeconds());
		assertEquals(-1, got.playbackOffsetPercent());
		assertEquals("", got.url());
		assertEquals("", got.resolveUrl(macros));
		assertEquals(0, got.vendorDataCount());
		assertEquals(0, got.errorUrlCount());
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
	}
}
