// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.adcom.context;

import static org.polygamma.android.origin.protobuf.Protobuf.*;

import android.location.Location;
import android.os.Build;

import androidx.annotation.FloatRange;
import androidx.annotation.ReturnThis;

import org.polygamma.android.origin.adcom.enums.AdComEnums;
import org.polygamma.android.origin.adcom.enums.GeoSourceType;
import org.polygamma.android.origin.protobuf.ProtobufDecoder;
import org.polygamma.android.origin.protobuf.ProtobufEncoder;
import org.polygamma.android.origin.protobuf.ProtobufSerializable;

import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Geographic location context.
 *
 * @since 0.1
 * @see <a href="https://github.com/InteractiveAdvertisingBureau/AdCOM/blob/main/AdCOM%20v1.0%20FINAL.md#object--geo-">AdCOM, version 1.0 - Object: Geo</a>
 */
public final class Geo implements ProtobufSerializable {

	private static final @FieldTag int TYPE				= fieldTagOf(  1, WIRE_VARINT);
	private static final @FieldTag int LAT				= fieldTagOf(  2, WIRE_FIXED64);
	private static final @FieldTag int LON				= fieldTagOf(  3, WIRE_FIXED64);
	/*private static final @FieldTag int ACCUR			= fieldTagOf(  4, WIRE_VARINT);*/
	/*private static final @FieldTag int LASTFIX		= fieldTagOf(  5, WIRE_VARINT);*/
	/*private static final @FieldTag int IPSERV			= fieldTagOf(  6, WIRE_VARINT);*/
	private static final @FieldTag int COUNTRY			= fieldTagOf(  7, WIRE_LEN);
	/*private static final @FieldTag int REGION			= fieldTagOf(  8, WIRE_LEN);*/
	/*private static final @FieldTag int METRO			= fieldTagOf(  9, WIRE_LEN);*/
	/*private static final @FieldTag int CITY			= fieldTagOf( 10, WIRE_LEN);*/
	/*private static final @FieldTag int ZIP			= fieldTagOf( 11, WIRE_LEN);*/
	private static final @FieldTag int UTCOFFSET		= fieldTagOf( 12, WIRE_VARINT);
	private static final @FieldTag int TIMESTAMPSEC		= fieldTagOf(500, WIRE_FIXED64);
	private static final @FieldTag int PROVIDER			= fieldTagOf(501, WIRE_LEN);
	private static final @FieldTag int HORZACCUR		= fieldTagOf(502, WIRE_FIXED32);
	private static final @FieldTag int BEARING			= fieldTagOf(503, WIRE_FIXED64);
	private static final @FieldTag int BEARINGACCUR		= fieldTagOf(504, WIRE_FIXED32);
	private static final @FieldTag int SPEED			= fieldTagOf(505, WIRE_FIXED64);
	private static final @FieldTag int SPEEDACCUR		= fieldTagOf(506, WIRE_FIXED32);
	private static final @FieldTag int ALTWGS84			= fieldTagOf(507, WIRE_FIXED64);
	private static final @FieldTag int ALTWGS84ACCUR	= fieldTagOf(508, WIRE_FIXED32);
	private static final @FieldTag int ALTMSL			= fieldTagOf(509, WIRE_FIXED64);
	private static final @FieldTag int ALTMSLACCUR		= fieldTagOf(510, WIRE_FIXED32);

	/**
	 * Empty geographic location context.
	 */
	private static final Geo DEFAULT = new Geo();

	/**
	 * Geographic location {@linkplain Geo context} builder.
	 *
	 * @since 0.1
	 * @see #ofBuilder()
	 */
	public static final class Builder {

		private Geo geo;
		private boolean needClone;

		private Builder(Geo geo) {
			this.geo = geo;
			this.needClone = true;
		}

		private Geo target() {
			if (this.needClone) {
				this.geo = new Geo(this.geo);
				this.needClone = false;
			}
			return this.geo;
		}

		/**
		 * Set location data source.
		 *
		 * @param type location source
		 * @return {@code this}
		 * @since 0.1
		 * @see Geo#type()
		 */
		@ReturnThis
		public Builder type(@GeoSourceType int type) {
			this.target().type = type;
			return this;
		}

		/**
		 * Set latitude, in degrees.
		 *
		 * @param lat latitude between {@code -90} and {@code 90}, where negative is south
		 * @return {@code this}
		 * @since 1.2
		 * @see Geo#latitudeDegrees()
		 */
		@ReturnThis
		public Builder latitudeDegrees(@FloatRange(from = -90, to = 90) double lat) {
			this.target().latitudeDegrees = lat;
			return this;
		}

		/**
		 * Set longitude, in degrees.
		 *
		 * @param lon longitude between {@code -180} and {@code 180}, where negative is west
		 * @return {@code this}
		 * @since 1.2
		 * @see Geo#longitudeDegrees()
		 */
		@ReturnThis
		public Builder longitudeDegrees(@FloatRange(from = -180, to = 180) double lon) {
			this.target().longitudeDegrees = lon;
			return this;
		}

		/**
		 * Set ISO-3166-1-alpha-2 or ISO-3166-1-alpha-3 code identifying country.
		 *
		 * @param cc country code or {@linkplain String#isEmpty() empty} if unknown
		 * @return {@code this}
		 * @since 0.1
		 * @see Geo#countryCode()
		 */
		@ReturnThis
		public Builder countryCode(String cc) {
			this.target().countryCode = cc;
			return this;
		}

		/**
		 * Set local time delta, in minutes, from UTC.
		 *
		 * @param off delta
		 * @return {@code this}
		 * @since 1.2
		 * @see Geo#utcOffsetMinutes()
		 */
		@ReturnThis
		public Builder utcOffsetMinutes(int off) {
			this.target().utcOffsetMinutes = off;
			return this;
		}

		/**
		 * Set timestamp, in seconds since UNIX epoch, of when location fix was established.
		 *
		 * @param ts timestamp or {@code 0} if unknown
		 * @return {@code this}
		 * @since 1.2
		 * @see Geo#timestampSeconds()
		 */
		@ReturnThis
		public Builder timestampSeconds(long ts) {
			this.target().timestampSeconds = ts;
			return this;
		}

		/**
		 * Set name of provider location was sourced from.
		 *
		 * @param prov provider name or {@linkplain String#isEmpty() empty} if unknown
		 * @return {@code this}
		 * @since 1.2
		 * @see Geo#providerName()
		 * @see Location#getProvider()
		 */
		@ReturnThis
		public Builder providerName(String prov) {
			this.target().providerName = prov;
			return this;
		}

		/**
		 * Set accuracy radius, in meters, of {@linkplain #latitudeDegrees(double) latitude} and
		 * {@linkplain #longitudeDegrees(double) longitude}.
		 *
		 * @param acc accuracy or {@code 0} if unknown
		 * @return {@code this}
		 * @since 1.2
		 * @see Geo#horizontalAccuracyMeters()
		 */
		@ReturnThis
		public Builder horizontalAccuracyMeters(float acc) {
			this.target().horizontalAccuracyMeters = acc;
			return this;
		}

		/**
		 * Set bearing, in degrees.
		 *
		 * @param b bearing or {@code 0} if unknown
		 * @return {@code this}
		 * @since 1.2
		 * @see Geo#bearingDegrees()
		 */
		@ReturnThis
		public Builder bearingDegrees(double b) {
			this.target().bearingDegrees = b;
			return this;
		}

		/**
		 * Set {@linkplain #bearingDegrees(double) bearing} accuracy, in degrees.
		 *
		 * @param b accuracy or {@code 0} if unknown
		 * @return {@code this}
		 * @since 1.2
		 * @see Geo#bearingAccuracyDegrees()
		 */
		@ReturnThis
		public Builder bearingAccuracyDegrees(float b) {
			this.target().bearingAccuracyDegrees = b;
			return this;
		}

		/**
		 * Set speed, in meters per second.
		 *
		 * @param s speed or, {@code 0} if unknown or not moving
		 * @return {@code this}
		 * @since 1.2
		 * @see Geo#speedMetersPerSecond()
		 */
		@ReturnThis
		public Builder speedMetersPerSecond(double s) {
			this.target().speedMetersPerSecond = s;
			return this;
		}

		/**
		 * Set {@linkplain #speedMetersPerSecond(double) speed} accuracy, in meters per second.
		 *
		 * @param s accuracy or {@code 0} if unknown
		 * @return {@code this}
		 * @since 1.2
		 * @see Geo#speedAccuracyMetersPerSecond()
		 */
		@ReturnThis
		public Builder speedAccuracyMetersPerSecond(float s) {
			this.target().speedAccuracyMetersPerSecond = s;
			return this;
		}

		/**
		 * Set altitude, in meters above WGS84 reference ellipsoid.
		 *
		 * @param a altitude or {@code 0} if unknown
		 * @return {@code this}
		 * @since 1.2
		 * @see Geo#altitudeWgs84Meters()
		 */
		@ReturnThis
		public Builder altitudeWgs84Meters(double a) {
			this.target().altitudeWgs84Meters = a;
			return this;
		}

		/**
		 * Set accuracy, in meters, of {@linkplain #altitudeWgs84Meters(double) altitude}, in
		 * meters above WGS84 reference ellipsoid.
		 *
		 * @param a accuracy or {@code 0} if unknown
		 * @return {@code this}
		 * @since 1.2
		 * @see Geo#altitudeWgs84AccuracyMeters()
		 */
		@ReturnThis
		public Builder altitudeWgs84AccuracyMeters(float a) {
			this.target().altitudeWgs84AccuracyMeters = a;
			return this;
		}

		/**
		 * Set Mean Sea Level altitude, in meters.
		 *
		 * @param a altitude or {@code 0} if unknown
		 * @return {@code this}
		 * @since 1.2
		 * @see Geo#altitudeMslMeters()
		 */
		@ReturnThis
		public Builder altitudeMslMeters(double a) {
			this.target().altitudeMslMeters = a;
			return this;
		}

		/**
		 * Set accuracy, in meters, of Mean Sea Level {@linkplain #altitudeMslMeters(double)
		 * altitiude}.
		 *
		 * @param a accuracy or {@code 0} if unknown
		 * @return {@code this}
		 * @since 1.2
		 * @see Geo#altitudeMslAccuracyMeters()
		 */
		@ReturnThis
		public Builder altitudeMslAccuracyMeters(float a) {
			this.target().altitudeMslAccuracyMeters = a;
			return this;
		}

		/**
		 * Build resulting geolocation.
		 *
		 * @return resulting geolocation instance
		 * @since 1.2
		 */
		public Geo build() {
			this.needClone = true;
			return this.geo;
		}
	}

	/**
	 * Empty geographic location context instance.
	 *
	 * @return context instance
	 * @since 1.2
	 */
	public static Geo of() {
		return DEFAULT;
	}

	/**
	 * Construct new empty {@linkplain Builder builder}.
	 *
	 * @return empty builder
	 * @since 0.1
	 */
	public static Builder ofBuilder() {
		return DEFAULT.toBuilder();
	}

	/**
	 * Construct geographic location context from {@linkplain Location location}.
	 *
	 * @param src location to construct from
	 * @return resulting context
	 * @since 0.1
	 */
	public static Geo ofLocation(Location src) {
		Geo rv = new Geo();

		rv.type = AdComEnums.GeoSourceDevice;
		rv.latitudeDegrees = src.getLatitude();
		rv.longitudeDegrees = src.getLongitude();
		rv.timestampSeconds = TimeUnit.MILLISECONDS.toSeconds(src.getTime());
		rv.providerName = src.getProvider();

		TimeZone tz = TimeZone.getDefault();

		if (tz != null) {
			rv.utcOffsetMinutes = (int) TimeUnit.MILLISECONDS
				.toMinutes(tz.getOffset(src.getTime()));
		}
		if (src.hasAccuracy())
			rv.horizontalAccuracyMeters = src.getAccuracy();
		if (src.hasBearing()) {
			rv.bearingDegrees = src.getBearing();
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && src.hasBearingAccuracy())
				rv.bearingAccuracyDegrees = src.getBearingAccuracyDegrees();
		}
		if (src.hasSpeed()) {
			rv.speedMetersPerSecond = src.getSpeed();
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && src.hasSpeedAccuracy())
				rv.speedAccuracyMetersPerSecond = src.getSpeedAccuracyMetersPerSecond();
		}
		if (src.hasAltitude()) {
			rv.altitudeWgs84Meters = src.getAltitude();
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && src.hasVerticalAccuracy())
				rv.altitudeWgs84AccuracyMeters = src.getVerticalAccuracyMeters();
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && src.hasMslAltitude()) {
			rv.altitudeMslMeters = src.getMslAltitudeMeters();
			if (src.hasMslAltitudeAccuracy())
				rv.altitudeMslAccuracyMeters = src.getMslAltitudeAccuracyMeters();
		}
		return rv;
	}

	/**
	 * Deserialize context from Protobuf message.
	 *
	 * @param dec decoder to deserialize from
	 * @return deserialized context
	 * @throws RuntimeException coding is malformed
	 * @since 1.2
	 */
	public static Geo ofProtobuf(ProtobufDecoder dec) {
		Geo rv = new Geo();

		while (dec.hasRemaining()) {
			int tag = dec.decodeFieldTag();

			if (tag == LAT)
				rv.latitudeDegrees = dec.decodeDouble();
			else if (tag == LON)
				rv.longitudeDegrees = dec.decodeDouble();
			else if (tag == BEARING)
				rv.bearingDegrees = dec.decodeDouble();
			else if (tag == SPEED)
				rv.speedMetersPerSecond = dec.decodeDouble();
			else if (tag == ALTWGS84)
				rv.altitudeWgs84Meters = dec.decodeDouble();
			else if (tag == ALTMSL)
				rv.altitudeMslMeters = dec.decodeDouble();
			else if (tag == HORZACCUR)
				rv.horizontalAccuracyMeters = dec.decodeFloat();
			else if (tag == BEARINGACCUR)
				rv.bearingAccuracyDegrees = dec.decodeFloat();
			else if (tag == SPEEDACCUR)
				rv.speedAccuracyMetersPerSecond = dec.decodeFloat();
			else if (tag == ALTWGS84ACCUR)
				rv.altitudeWgs84AccuracyMeters = dec.decodeFloat();
			else if (tag == ALTMSLACCUR)
				rv.altitudeMslAccuracyMeters = dec.decodeFloat();
			else if (tag == TIMESTAMPSEC)
				rv.timestampSeconds = dec.decodeFixed64();
			else if (tag == UTCOFFSET)
				rv.utcOffsetMinutes = dec.decodeSint32();
			else if (tag == TYPE)
				rv.type = dec.decodeUint32();
			else if (tag == COUNTRY)
				rv.countryCode = dec.decodeString();
			else if (tag == PROVIDER)
				rv.providerName = dec.decodeString();
			else
				dec.skipFieldValue(tag);
		}
		return rv;
	}

	private double latitudeDegrees;
	private double longitudeDegrees;
	private double bearingDegrees;
	private double speedMetersPerSecond;
	private double altitudeWgs84Meters;
	private double altitudeMslMeters;
	private float horizontalAccuracyMeters;
	private float bearingAccuracyDegrees;
	private float speedAccuracyMetersPerSecond;
	private float altitudeWgs84AccuracyMeters;
	private float altitudeMslAccuracyMeters;
	private long timestampSeconds;
	private int utcOffsetMinutes;
	private @GeoSourceType int type;
	private String countryCode;
	private String providerName;

	private Geo() {
		this.type = AdComEnums.GeoSourceUnknown;
		this.countryCode = "";
		this.providerName = "";
	}

	private Geo(Geo that) {
		this.latitudeDegrees = that.latitudeDegrees;
		this.longitudeDegrees = that.longitudeDegrees;
		this.bearingDegrees = that.bearingDegrees;
		this.speedMetersPerSecond = that.speedMetersPerSecond;
		this.altitudeWgs84Meters = that.altitudeWgs84Meters;
		this.altitudeMslMeters = that.altitudeMslMeters;
		this.horizontalAccuracyMeters = that.horizontalAccuracyMeters;
		this.bearingAccuracyDegrees = that.bearingAccuracyDegrees;
		this.speedAccuracyMetersPerSecond = that.speedAccuracyMetersPerSecond;
		this.altitudeWgs84AccuracyMeters = that.altitudeWgs84AccuracyMeters;
		this.altitudeMslAccuracyMeters = that.altitudeMslAccuracyMeters;
		this.timestampSeconds = that.timestampSeconds;
		this.utcOffsetMinutes = that.utcOffsetMinutes;
		this.type = that.type;
		this.countryCode = that.countryCode;
		this.providerName = that.providerName;
	}

	/**
	 * Location data source.
	 *
	 * @return source
	 * @since 0.1
	 * @see Builder#type(int)
	 */
	public @GeoSourceType int type() {
		return this.type;
	}

	/**
	 * Location latitude, in degrees.
	 *
	 * @return latitude between {@code -90} and {@code 90}, where negative is south
	 * @since 1.2
	 * @see Builder#latitudeDegrees(double)
	 */
	public double latitudeDegrees() {
		return this.latitudeDegrees;
	}

	/**
	 * Location longitude degrees.
	 *
	 * @return longitude between {@code -180} and {@code 180}, where negative is west
	 * @since 1.2
	 * @see Builder#longitudeDegrees(double)
	 */
	public double longitudeDegrees() {
		return this.longitudeDegrees;
	}

	/**
	 * ISO-3166-1-alpha-2 or ISO-3166-1-alpha-3 country code.
	 *
	 * @return country code or {@linkplain String#isEmpty() empty} if unknown
	 * @since 0.1
	 * @see Builder#countryCode(String)
	 */
	public String countryCode() {
		return this.countryCode;
	}

	/**
	 * Local time delta, in minutes, from UTC.
	 *
	 * @return local time delta
	 * @since 1.2
	 * @see Builder#utcOffsetMinutes(int)
	 */
	public int utcOffsetMinutes() {
		return this.utcOffsetMinutes;
	}

	/**
	 * Timestamp, in seconds since UNIX epoch, of when location fix was established.
	 *
	 * @return location fix timestamp or {@code 0} if unknown
	 * @since 1.2
	 * @see Builder#timestampSeconds(long)
	 */
	public long timestampSeconds() {
		return this.timestampSeconds;
	}

	/**
	 * Name of provider location was sourced from.
	 *
	 * @return provider name or {@linkplain String#isEmpty() empty} if unknown
	 * @since 1.2
	 * @see Builder#providerName(String)
	 */
	public String providerName() {
		return this.providerName;
	}

	/**
	 * Set accuracy radius, in meters, of {@linkplain #latitudeDegrees() latitude} and {@linkplain
	 * #longitudeDegrees() longitude}.
	 *
	 * @return accuracy or {@code 0} if unknown
	 * @since 0.1
	 * @see Builder#horizontalAccuracyMeters(float)
	 */
	public float horizontalAccuracyMeters() {
		return this.horizontalAccuracyMeters;
	}

	/**
	 * Bearing, in degrees.
	 *
	 * @return bearing or {@code 0} if unknown
	 * @since 0.1
	 * @see Builder#bearingDegrees(double)
	 */
	public double bearingDegrees() {
		return this.bearingDegrees;
	}

	/**
	 * {@linkplain #bearingDegrees() Bearing} accuracy, in degrees.
	 *
	 * @return accuracy or {@code 0} if unknown
	 * @since 0.1
	 * @see Builder#bearingAccuracyDegrees(float)
	 */
	public float bearingAccuracyDegrees() {
		return this.bearingAccuracyDegrees;
	}

	/**
	 * Speed, in meters per second.
	 *
	 * @return speed or, {@code 0} if unknown or not moving
	 * @since 0.1
	 * @see Builder#speedMetersPerSecond(double)
	 */
	public double speedMetersPerSecond() {
		return this.speedMetersPerSecond;
	}

	/**
	 * Set {@linkplain #speedMetersPerSecond() speed} accuracy, in meters per second.
	 *
	 * @return accuracy or {@code 0} if unknown
	 * @since 0.1
	 * @see Builder#speedAccuracyMetersPerSecond(float)
	 */
	public float speedAccuracyMetersPerSecond() {
		return this.speedAccuracyMetersPerSecond;
	}

	/**
	 * Altitude, in meters above WGS84 reference ellipsoid.
	 *
	 * @return altitude or {@code 0} if unknown
	 * @since 0.1
	 * @see Builder#altitudeWgs84Meters(double)
	 */
	public double altitudeWgs84Meters() {
		return this.altitudeWgs84Meters;
	}

	/**
	 * Accuracy, in meters, of {@linkplain #altitudeWgs84Meters() altitude}, in meters above WGS84
	 * reference ellipsoid.
	 *
	 * @return accuracy or {@code 0} if unknown
	 * @since 0.1
	 * @see Builder#altitudeWgs84AccuracyMeters(float)
	 */
	public float altitudeWgs84AccuracyMeters() {
		return this.altitudeWgs84AccuracyMeters;
	}

	/**
	 * Mean Sea Level altitude, in meters.
	 *
	 * @return altitude or {@code 0} if unknown
	 * @since 0.1
	 * @see Builder#altitudeMslMeters(double)
	 */
	public double altitudeMslMeters() {
		return this.altitudeMslMeters;
	}

	/**
	 * Accuracy, in meters, of Mean Sea Level {@linkplain #altitudeMslMeters() altitude}.
	 *
	 * @return accuracy or {@code 0} if unknown
	 * @since 0.1
	 * @see Builder#altitudeMslAccuracyMeters(float)
	 */
	public float altitudeMslAccuracyMeters() {
		return this.altitudeMslAccuracyMeters;
	}

	/**
	 * Construct new builder initialized from {@code this}.
	 *
	 * @return builder instance
	 * @since 0.1
	 */
	public Builder toBuilder() {
		return new Builder(this);
	}

	@Override
	public void toProtobuf(ProtobufEncoder enc) {
		enc.encodeDoubleField(LAT, this.latitudeDegrees)
			.encodeDoubleField(LON, this.longitudeDegrees)
			.encodeDoubleField(BEARING, this.bearingDegrees)
			.encodeDoubleField(SPEED, this.speedMetersPerSecond)
			.encodeDoubleField(ALTWGS84, this.altitudeWgs84Meters)
			.encodeDoubleField(ALTMSL, this.altitudeMslMeters)
			.encodeFloatField(HORZACCUR, this.horizontalAccuracyMeters)
			.encodeFloatField(BEARINGACCUR, this.bearingAccuracyDegrees)
			.encodeFloatField(SPEEDACCUR, this.speedAccuracyMetersPerSecond)
			.encodeFloatField(ALTWGS84ACCUR, this.altitudeWgs84AccuracyMeters)
			.encodeFloatField(ALTMSLACCUR, this.altitudeMslAccuracyMeters)
			.encodeUnsignedLongField(TIMESTAMPSEC, this.timestampSeconds)
			.encodeSignedIntField(UTCOFFSET, this.utcOffsetMinutes)
			.encodeUnsignedIntField(TYPE, this.type)
			.encodeStringField(COUNTRY, this.countryCode)
			.encodeStringField(PROVIDER, this.providerName);
	}
}
