// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.adcom.context;

import static org.polygamma.android.origin.protobuf.ProtobufField.*;

import android.location.Location;
import android.os.Build;

import androidx.annotation.FloatRange;
import androidx.annotation.ReturnThis;

import org.polygamma.android.origin.adcom.enums.AdComEnums;
import org.polygamma.android.origin.adcom.enums.GeoSourceType;
import org.polygamma.android.origin.protobuf.ProtobufReader;
import org.polygamma.android.origin.protobuf.ProtobufSerializable;
import org.polygamma.android.origin.protobuf.ProtobufWriter;

import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Geographic location context.
 *
 * @since 0.1
 * @see <a href="https://github.com/InteractiveAdvertisingBureau/AdCOM/blob/main/AdCOM%20v1.0%20FINAL.md#object--geo-">AdCOM, version 1.0 - Object: Geo</a>
 */
public final class Geo implements ProtobufSerializable {

	private static final @Tag int TYPE				= ofInt32(    1);
	private static final @Tag int LAT				= ofDouble(   2);
	private static final @Tag int LON				= ofDouble(   3);
	/*private static final @Tag int ACCUR			= ofInt32(    4);*/
	/*private static final @Tag int LASTFIX			= ofInt32(    5);*/
	/*private static final @Tag int IPSERV			= ofInt32(    6);*/
	private static final @Tag int COUNTRY			= ofString(   7);
	/*private static final @Tag int REGION			= ofString(   8);*/
	/*private static final @Tag int METRO			= ofString(   9);*/
	/*private static final @Tag int CITY			= ofString(  10);*/
	/*private static final @Tag int ZIP				= ofString(  11);*/
	private static final @Tag int UTCOFFSET			= ofSint32(  12);
	private static final @Tag int TIMESTAMPSEC		= ofFixed64(500);
	private static final @Tag int PROVIDER			= ofString( 501);
	private static final @Tag int HORZACCUR			= ofFloat(  502);
	private static final @Tag int BEARING			= ofDouble( 503);
	private static final @Tag int BEARINGACCUR		= ofFloat(  504);
	private static final @Tag int SPEED				= ofDouble( 505);
	private static final @Tag int SPEEDACCUR		= ofFloat(  506);
	private static final @Tag int ALTWGS84			= ofDouble( 507);
	private static final @Tag int ALTWGS84ACCUR		= ofFloat(  508);
	private static final @Tag int ALTMSL			= ofDouble( 509);
	private static final @Tag int ALTMSLACCUR		= ofFloat(  510);

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
	 * @param reader reader to deserialize from
	 * @return deserialized context
	 * @throws RuntimeException coding is malformed
	 * @since 1.2
	 */
	public static Geo ofProtobuf(ProtobufReader reader) {
		Geo rv = new Geo();

		while (reader.hasRemaining()) {
			int tag = reader.readTag();

			if (tag == LAT) {
				rv.latitudeDegrees = reader.readDouble();
			} else if (tag == LON) {
				rv.longitudeDegrees = reader.readDouble();
			} else if (tag == BEARING) {
				rv.bearingDegrees = reader.readDouble();
			} else if (tag == SPEED) {
				rv.speedMetersPerSecond = reader.readDouble();
			} else if (tag == ALTWGS84) {
				rv.altitudeWgs84Meters = reader.readDouble();
			} else if (tag == ALTMSL) {
				rv.altitudeMslMeters = reader.readDouble();
			} else if (tag == HORZACCUR) {
				rv.horizontalAccuracyMeters = reader.readFloat();
			} else if (tag == BEARINGACCUR) {
				rv.bearingAccuracyDegrees = reader.readFloat();
			} else if (tag == SPEEDACCUR) {
				rv.speedAccuracyMetersPerSecond = reader.readFloat();
			} else if (tag == ALTWGS84ACCUR) {
				rv.altitudeWgs84AccuracyMeters = reader.readFloat();
			} else if (tag == ALTMSLACCUR) {
				rv.altitudeMslAccuracyMeters = reader.readFloat();
			} else if (tag == TIMESTAMPSEC) {
				rv.timestampSeconds = reader.readFixed64();
			} else if (tag == UTCOFFSET) {
				rv.utcOffsetMinutes = reader.readSint32();
			} else if (tag == TYPE) {
				rv.type = reader.readInt32();
			} else if (tag == COUNTRY) {
				rv.countryCode = reader.readString();
			} else if (tag == PROVIDER) {
				rv.providerName = reader.readString();
			}
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
	public void toProtobuf(ProtobufWriter writer) {
		writer.writeDouble(LAT, this.latitudeDegrees);
		writer.writeDouble(LON, this.longitudeDegrees);
		writer.writeDouble(BEARING, this.bearingDegrees);
		writer.writeDouble(SPEED, this.speedMetersPerSecond);
		writer.writeDouble(ALTWGS84, this.altitudeWgs84Meters);
		writer.writeDouble(ALTMSL, this.altitudeMslMeters);
		writer.writeFloat(HORZACCUR, this.horizontalAccuracyMeters);
		writer.writeFloat(BEARINGACCUR, this.bearingAccuracyDegrees);
		writer.writeFloat(SPEEDACCUR, this.speedAccuracyMetersPerSecond);
		writer.writeFloat(ALTWGS84ACCUR, this.altitudeWgs84AccuracyMeters);
		writer.writeFloat(ALTMSLACCUR, this.altitudeMslAccuracyMeters);
		writer.writeFixed64(TIMESTAMPSEC, this.timestampSeconds);
		writer.writeSint32(UTCOFFSET, this.utcOffsetMinutes);
		writer.writeInt32(TYPE, this.type);
		writer.writeString(COUNTRY, this.countryCode);
		writer.writeString(PROVIDER, this.providerName);
	}
}
