// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.adcom.context;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.util.Pair;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.protobuf.InvalidProtocolBufferException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.polygamma.android.origin.adcom.enums.AdComEnums;
import org.polygamma.android.origin.protobuf.ProtobufReader;
import org.polygamma.android.origin.protobuf.ProtobufWriter;
import org.polygamma.origin.adcom.AdcomContext;

import java.util.Arrays;

/**
 * {@link Device} tests.
 */
@RunWith(AndroidJUnit4.class)
public class DeviceTest {
	@Test
	public void testSerde() throws InvalidProtocolBufferException {
		Device exp = Device.ofBuilder()
			.type(AdComEnums.DevicePhone)
			.limitAdTracking(true)
			.manufacturerName("MAKE")
			.modelName("MODEL")
			.operatingSystem(AdComEnums.OsGoogleAndroid)
			.operatingSystemVersion("1.2.3")
			.modelVersion("4.5.6")
			.screenHeightPx(100)
			.screenWidthPx(200)
			.screenPixelsPerInch(10)
			.screenPixelRatio(1.2f)
			.languageCode("en")
			.carrierName("CARRIER")
			.carrierMccMnc("400-400")
			.simCarrierMccMnc("500-500")
			.connectionType(AdComEnums.ConnectionCell3G)
			.geos(Arrays.asList(
				Geo.ofBuilder()
					.countryCode("cn")
					.build(),
				Geo.ofBuilder()
					.countryCode("us")
					.build()
			))
			.advertisingIds(Arrays.asList(
				new Pair<>("a", "A"),
				new Pair<>("b", "B")
			))
			.extraLanguageCodes(Arrays.asList("jp", "de"))
			.nightMode(true)
			.landscape(true)
			.build();
		Device got = Device.ofProtobuf(new ProtobufReader(
			AdcomContext.Device.parseFrom(ProtobufWriter.serialize(exp))
				.toByteString()
				.asReadOnlyByteBuffer()
		));

		assertEquals(AdComEnums.DevicePhone, got.type());
		assertTrue(got.limitAdTracking());
		assertEquals("MAKE", got.manufacturerName());
		assertEquals("MODEL", got.modelName());
		assertEquals(AdComEnums.OsGoogleAndroid, got.operatingSystem());
		assertEquals("1.2.3", got.operatingSystemVersion());
		assertEquals("4.5.6", got.modelVersion());
		assertEquals(100, got.screenHeightPx());
		assertEquals(200, got.screenWidthPx());
		assertEquals(10, got.screenPixelsPerInch());
		assertEquals(1.2f, got.screenPixelRatio(), 0);
		assertEquals("en", got.languageCode());
		assertEquals("CARRIER", got.carrierName());
		assertEquals("400-400", got.carrierMccMnc());
		assertEquals("500-500", got.simCarrierMccMnc());
		assertEquals(AdComEnums.ConnectionCell3G, got.connectionType());
		assertEquals(2, got.geoCount());
		assertEquals("cn", got.geo(0).countryCode());
		assertEquals("us", got.geo(1).countryCode());
		assertEquals(2, got.advertisingIdCount());
		assertEquals(new Pair<>("a", "A"), got.advertisingId(0));
		assertEquals(new Pair<>("b", "B"), got.advertisingId(1));
		assertEquals(2, got.extraLanguageCodeCount());
		assertEquals("jp", got.extraLanguageCode(0));
		assertEquals("de", got.extraLanguageCode(1));
		assertTrue(got.nightMode());
		assertTrue(got.landscape());

		got = Device.of();
		assertEquals("", got.userAgent());
		assertEquals("", got.manufacturerName());
		assertEquals("", got.modelName());
		assertEquals(AdComEnums.DeviceUnknown, got.type());
		assertEquals(AdComEnums.OsUnknown, got.operatingSystem());
		assertEquals("", got.operatingSystemVersion());
		assertEquals(0.0f, got.screenPixelRatio(), 0);
		assertEquals(0, got.screenPixelsPerInch());
		assertEquals(0, got.screenWidthPx());
		assertEquals(0, got.screenHeightPx());
		assertEquals(AdComEnums.ConnectionUnknown, got.connectionType());
		assertEquals("", got.carrierName());
		assertEquals("", got.carrierMccMnc());
		assertEquals("", got.simCarrierMccMnc());
		assertEquals("", got.languageCode());
		assertEquals(0, got.extraLanguageCodeCount());
		assertEquals(0, got.advertisingIdCount());
		assertFalse(got.limitAdTracking());
		assertFalse(got.nightMode());
		assertFalse(got.landscape());
		assertFalse(got.supportsJavaScriptSandbox());
	}
}
