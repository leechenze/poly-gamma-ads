// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.adcom.context;

import static org.junit.Assert.assertEquals;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.protobuf.InvalidProtocolBufferException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.polygamma.android.origin.adcom.enums.AdComEnums;
import org.polygamma.android.origin.protobuf.ProtobufReader;
import org.polygamma.android.origin.protobuf.ProtobufWriter;
import org.polygamma.origin.adcom.AdcomContext;

/**
 * {@link Geo} tests.
 */
@RunWith(AndroidJUnit4.class)
public class GeoTest {
	@Test
	public void testSerde() throws InvalidProtocolBufferException {
		Geo exp = Geo.ofBuilder()
			.type(AdComEnums.GeoSourceDevice)
			.latitudeDegrees(4.5)
			.longitudeDegrees(5.4)
			.countryCode("us")
			.utcOffsetMinutes(10)
			.timestampSeconds(123456789L)
			.providerName("PROVIDER")
			.horizontalAccuracyMeters(1.2f)
			.bearingDegrees(2.3)
			.bearingAccuracyDegrees(2.1f)
			.speedMetersPerSecond(3.2)
			.speedAccuracyMetersPerSecond(3.1f)
			.altitudeWgs84Meters(8.9)
			.altitudeWgs84AccuracyMeters(9.8f)
			.altitudeMslMeters(10.1)
			.altitudeMslAccuracyMeters(1.10f)
			.build();
		Geo got = Geo.ofProtobuf(new ProtobufReader(
			AdcomContext.Geo.parseFrom(ProtobufWriter.serialize(exp))
				.toByteString()
				.asReadOnlyByteBuffer()
		));

		assertEquals(AdComEnums.GeoSourceDevice, got.type());
		assertEquals(4.5, got.latitudeDegrees(), 0);
		assertEquals(5.4, got.longitudeDegrees(), 0);
		assertEquals("us", got.countryCode());
		assertEquals(10, got.utcOffsetMinutes());
		assertEquals(123456789L, got.timestampSeconds());
		assertEquals("PROVIDER", got.providerName());
		assertEquals(1.2f, got.horizontalAccuracyMeters(), 0);
		assertEquals(2.3, got.bearingDegrees(), 0);
		assertEquals(2.1f, got.bearingAccuracyDegrees(), 0);
		assertEquals(3.2, got.speedMetersPerSecond(), 0);
		assertEquals(3.1f, got.speedAccuracyMetersPerSecond(), 0);
		assertEquals(8.9, got.altitudeWgs84Meters(), 0);
		assertEquals(9.8f, got.altitudeWgs84AccuracyMeters(), 0);
		assertEquals(10.1, got.altitudeMslMeters(), 0);
		assertEquals(1.10f, got.altitudeMslAccuracyMeters(), 0);

		got = Geo.of();
		assertEquals(0.0d, got.latitudeDegrees(), 0);
		assertEquals(0.0d, got.longitudeDegrees(), 0);
		assertEquals(0.0d, got.bearingDegrees(), 0);
		assertEquals(0.0d, got.speedMetersPerSecond(), 0);
		assertEquals(0.0d, got.altitudeWgs84Meters(), 0);
		assertEquals(0.0d, got.altitudeMslMeters(), 0);
		assertEquals(0.0f, got.horizontalAccuracyMeters(), 0);
		assertEquals(0.0f, got.bearingAccuracyDegrees(), 0);
		assertEquals(0.0f, got.speedAccuracyMetersPerSecond(), 0);
		assertEquals(0.0f, got.altitudeWgs84AccuracyMeters(), 0);
		assertEquals(0.0f, got.altitudeMslAccuracyMeters(), 0);
		assertEquals(0, got.timestampSeconds());
		assertEquals(0, got.utcOffsetMinutes());
		assertEquals(AdComEnums.GeoSourceUnknown, got.type());
		assertEquals("", got.countryCode());
		assertEquals("", got.providerName());
	}
}
