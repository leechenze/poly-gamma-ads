// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.core;

import static org.polygamma.android.origin.protobuf.ProtobufField.*;

import androidx.annotation.Nullable;
import androidx.annotation.ReturnThis;

import org.polygamma.android.origin.adcom.enums.AdComEnums;
import org.polygamma.android.origin.adcom.enums.ConnectionType;
import org.polygamma.android.origin.protobuf.ProtobufReader;
import org.polygamma.android.origin.protobuf.ProtobufSerializable;
import org.polygamma.android.origin.protobuf.ProtobufWriter;

import java.util.Locale;

/**
 * Connectivity network subscription description.
 *
 * @since 0.3
 * @see Connectivity#subscription(int)
 */
public final class ConnectivitySubscription implements ProtobufSerializable {

	private static final @Tag int ID				= ofFixed64(  1);
	private static final @Tag int CONNTYPE			= ofInt32(    2);
	private static final @Tag int CARRNAME			= ofString(   4);
	private static final @Tag int OPERNAME			= ofString(   5);
	private static final @Tag int OPERMCC			= ofInt32(    6);
	private static final @Tag int OPERMNC			= ofInt32(    7);
	private static final @Tag int OPERCOUNTRY		= ofString(   8);
	private static final @Tag int NETOPERNAME		= ofString(   9);
	private static final @Tag int NETOPERMCC		= ofInt32(   10);
	private static final @Tag int NETOPERMNC		= ofInt32(   11);
	private static final @Tag int NETOPERCOUNTRY	= ofString(  12);
	private static final @Tag int AOSPCARRID		= ofSint32( 500);

	/**
	 * Default descriptor.
	 */
	private static final ConnectivitySubscription DEFAULT = new ConnectivitySubscription();

	/**
	 * Connectivity network subscription {@linkplain ConnectivitySubscription descriptor} builder.
	 *
	 * @see #ofBuilder()
	 */
	static final class Builder {

		private ConnectivitySubscription subscription;
		private boolean needClone;

		private Builder(ConnectivitySubscription sub) {
			this.subscription = sub;
			this.needClone = true;
		}

		private ConnectivitySubscription target() {
			if (this.needClone) {
				this.subscription = new ConnectivitySubscription(this.subscription);
				this.needClone = false;
			}
			return this.subscription;
		}

		/**
		 * Set id of subscription.
		 *
		 * @param id subscription id or {@code -1} if unknown
		 * @return {@code this}
		 * @see ConnectivitySubscription#id()
		 */
		@ReturnThis
		Builder id(int id) {
			this.target().id = id;
			return this;
		}

		/**
		 * Set cellular network connection type of subscription.
		 *
		 * @param type connection type
		 * @return {@code this}
		 * @see ConnectivitySubscription#connectionType()
		 */
		@ReturnThis
		Builder connectionType(@ConnectionType int type) {
			this.target().connectionType = type;
			return this;
		}

		/**
		 * Set AOSP assigned id of carrier subscription is registered with.
		 *
		 * @param id carrier id or {@code -1} if unknown
		 * @return {@code this}
		 * @see ConnectivitySubscription#carrierId()
		 */
		@ReturnThis
		Builder carrierId(int id) {
			this.target().carrierId = id;
			return this;
		}

		/**
		 * Set human-readable name of carrier subscription is registered with.
		 *
		 * @param name carrier name or {@linkplain String#isEmpty() empty} if unknown
		 * @return {@code this}
		 * @see ConnectivitySubscription#carrierName()
		 */
		@ReturnThis
		Builder carrierName(String name) {
			this.target().carrierName = name;
			return this;
		}

		/**
		 * Set human-readable name of operator subscription is registered with.
		 *
		 * @param name operator name or {@linkplain String#isEmpty() empty} if unknown
		 * @return {@code this}
		 * @see ConnectivitySubscription#operatorName()
		 */
		@ReturnThis
		Builder operatorName(String name) {
			this.target().operatorName = name;
			return this;
		}

		/**
		 * Set mobile country code (MCC) of operator subscription is registered with.
		 *
		 * @param mcc code or {@code 0} if unknown
		 * @return {@code this}
		 * @see ConnectivitySubscription#operatorMcc()
		 */
		@ReturnThis
		Builder operatorMcc(int mcc) {
			this.target().operatorMcc = mcc;
			return this;
		}

		/**
		 * Set mobile network code (MNC) of operator subscription is registered with.
		 *
		 * @param mnc code
		 * @return {@code this}
		 * @see ConnectivitySubscription#operatorMcc()
		 */
		@ReturnThis
		Builder operatorMnc(int mnc) {
			this.target().operatorMnc = mnc;
			return this;
		}

		/**
		 * Set mobile country code and mobile network code (MCC-MNC) of operator subscription is
		 * registered with.
		 * <p>If {@code code} is empty, the {@linkplain #operatorMcc(int) MCC} and {@linkplain
		 * #operatorMnc(int) MNC} are set to {@code 0}; otherwise, {@code code} must have the
		 * MCC code integer, with optional leading {@code 0} padding, and an optional MNC code
		 * integer, with optional leading {@code 0} padding.
		 *
		 * @param code code or {@linkplain String#isEmpty() empty} if unknown
		 * @return {@code this}
		 * @throws IllegalArgumentException {@code code} is not empty and malformed
		 * @see #operatorMcc(int)
		 * @see #operatorMnc(int)
		 * @see ConnectivitySubscription#operatorCode()
		 */
		@ReturnThis
		Builder operatorCode(String code) {
			int[] mccmnc = MccMnc.parse(code);
			ConnectivitySubscription dst = this.target();

			dst.operatorMcc = Math.max(mccmnc[0], 0);
			dst.operatorMnc = Math.max(mccmnc[1], 0);
			return this;
		}

		/**
		 * Set ISO 3166-1 alpha-2 country code of operator subscription is registered with.
		 *
		 * @param cc country code or {@linkplain String#isEmpty() empty} if unknown
		 * @return {@code this}
		 * @see ConnectivitySubscription#operatorCountryCode()
		 */
		@ReturnThis
		Builder operatorCountryCode(String cc) {
			this.target().operatorCountryCode = cc;
			return this;
		}

		/**
		 * Set human-readable name of operator subscription is registered with for network
		 * connectivity.
		 *
		 * @param name operator name or {@linkplain String#isEmpty() empty} if unknown
		 * @return {@code this}
		 * @see ConnectivitySubscription#networkOperatorName()
		 */
		@ReturnThis
		Builder networkOperatorName(String name) {
			this.target().networkOperatorName = name;
			return this;
		}

		/**
		 * Set mobile country code (MCC) of operator subscription is registered with for network
		 * connectivity.
		 *
		 * @param mcc code or {@code 0} if unknown
		 * @return {@code this}
		 * @see ConnectivitySubscription#networkOperatorMcc()
		 */
		@ReturnThis
		Builder networkOperatorMcc(int mcc) {
			this.target().networkOperatorMcc = mcc;
			return this;
		}

		/**
		 * Set mobile network code (MNC) of operator subscription is registered with for network
		 * connectivity.
		 *
		 * @param mnc code
		 * @return {@code this}
		 * @see ConnectivitySubscription#networkOperatorMnc()
		 */
		@ReturnThis
		Builder networkOperatorMnc(int mnc) {
			this.target().networkOperatorMnc = mnc;
			return this;
		}

		/**
		 * Set mobile country code and mobile network code (MCC-MNC) of operator subscription is
		 * registered with for network connectivity.
		 * <p>If {@code code} is empty, the {@linkplain #networkOperatorMcc(int) MCC} and
		 * {@linkplain #networkOperatorMnc(int) MNC} are set to {@code 0}; otherwise, {@code code}
		 * must have the MCC code integer, with optional leading {@code 0} padding, and an optional
		 * MNC code integer, with optional leading {@code 0} padding.
		 *
		 * @param code code or {@linkplain String#isEmpty() empty} if unknown
		 * @return {@code this}
		 * @throws IllegalArgumentException {@code code} is not empty and malformed
		 * @see #networkOperatorMcc(int)
		 * @see #networkOperatorMnc(int)
		 * @see ConnectivitySubscription#networkOperatorCode()
		 */
		@ReturnThis
		Builder networkOperatorCode(String code) {
			int[] mccmnc = MccMnc.parse(code);
			ConnectivitySubscription dst = this.target();

			dst.networkOperatorMcc = Math.max(mccmnc[0], 0);
			dst.networkOperatorMnc = Math.max(mccmnc[1], 0);
			return this;
		}

		/**
		 * Set ISO 3166-1 alpha-2 country code of operator subscription is registered with for
		 * network connectivity.
		 *
		 * @param cc country code or {@linkplain String#isEmpty() empty} if unknown
		 * @return {@code this}
		 * @see ConnectivitySubscription#networkOperatorCountryCode()
		 */
		@ReturnThis
		Builder networkOperatorCountryCode(String cc) {
			this.target().networkOperatorCountryCode = cc;
			return this;
		}

		/**
		 * Build resulting subscription.
		 *
		 * @return subscription instance
		 */
		ConnectivitySubscription build() {
			this.needClone = true;
			return this.subscription;
		}
	}

	/**
	 * Construct new empty {@linkplain Builder builder}.
	 *
	 * @return empty builder instance
	 * @since 0.3
	 */
	static Builder ofBuilder() {
		return DEFAULT.toBuilder();
	}

	/**
	 * Deserialize subscription description from Protobuf message.
	 *
	 * @param reader reader to deserialize from
	 * @return deserialized subscription description
	 * @throws RuntimeException coding is malformed
	 * @since 1.2
	 */
	public static ConnectivitySubscription ofProtobuf(ProtobufReader reader) {
		ConnectivitySubscription rv = new ConnectivitySubscription(DEFAULT);

		while (reader.hasRemaining()) {
			int tag = reader.readTag();

			if (tag == ID)
				rv.id = (int) reader.readFixed64();
			else if (tag == CONNTYPE)
				rv.connectionType = reader.readInt32();
			else if (tag == CARRNAME)
				rv.carrierName = reader.readString();
			else if (tag == OPERNAME)
				rv.operatorName = reader.readString();
			else if (tag == OPERMCC)
				rv.operatorMcc = reader.readInt32();
			else if (tag == OPERMNC)
				rv.operatorMnc = reader.readInt32();
			else if (tag == OPERCOUNTRY)
				rv.operatorCountryCode = reader.readString();
			else if (tag == NETOPERNAME)
				rv.networkOperatorName = reader.readString();
			else if (tag == NETOPERMCC)
				rv.networkOperatorMcc = reader.readInt32();
			else if (tag == NETOPERMNC)
				rv.networkOperatorMnc = reader.readInt32();
			else if (tag == NETOPERCOUNTRY)
				rv.networkOperatorCountryCode = reader.readString();
			else if (tag == AOSPCARRID)
				rv.carrierId = reader.readSint32();
		}
		return rv;
	}

	private int id;
	private @ConnectionType int connectionType;
	private int carrierId;
	private int operatorMcc;
	private int operatorMnc;
	private int networkOperatorMcc;
	private int networkOperatorMnc;
	private String carrierName;
	private String operatorName;
	private String operatorCountryCode;
	private String networkOperatorName;
	private String networkOperatorCountryCode;

	private ConnectivitySubscription() {
		this.id = -1;
		this.connectionType = AdComEnums.ConnectionUnknown;
		this.carrierId = -1;
		this.carrierName = "";
		this.operatorName = "";
		this.operatorCountryCode = "";
		this.networkOperatorName = "";
		this.networkOperatorCountryCode = "";
	}

	private ConnectivitySubscription(ConnectivitySubscription that) {
		this.id = that.id;
		this.connectionType = that.connectionType;
		this.carrierId = that.carrierId;
		this.operatorMcc = that.operatorMcc;
		this.operatorMnc = that.operatorMnc;
		this.networkOperatorMcc = that.networkOperatorMcc;
		this.networkOperatorMnc = that.networkOperatorMnc;
		this.carrierName = that.carrierName;
		this.operatorName = that.operatorName;
		this.operatorCountryCode = that.operatorCountryCode;
		this.networkOperatorName = that.networkOperatorName;
		this.networkOperatorCountryCode = that.networkOperatorCountryCode;
	}

	/**
	 * Subscription id.
	 *
	 * @return id or {@code -1} if unknown
	 * @since 0.3
	 */
	public long id() {
		return this.id;
	}

	/**
	 * Cellular network connection type of subscription.
	 *
	 * @return connection type
	 * @since 0.3
	 */
	public @ConnectionType int connectionType() {
		return this.connectionType;
	}

	/**
	 * AOSP assigned id of carrier subscription is registered with.
	 *
	 * @return carrier id or {@code -1} if unknown
	 * @since 0.3
	 * @see #carrierName()
	 */
	public int carrierId() {
		return this.carrierId;
	}

	/**
	 * Human-readable name of carrier subscription is registered with.
	 *
	 * @return carrier name or {@linkplain String#isEmpty() empty} if unknown
	 * @since 0.3
	 * @see #carrierId()
	 */
	public String carrierName() {
		return this.carrierName;
	}

	/**
	 * Human-readable name of operator subscription is registered with.
	 *
	 * @return operator name or {@linkplain String#isEmpty() empty} if unknown
	 * @since 0.3
	 * @see #operatorCode()
	 * @see #operatorCountryCode()
	 */
	public String operatorName() {
		return this.operatorName;
	}

	/**
	 * Mobile country code (MCC) of operator subscription is registered with.
	 *
	 * @return code or {@code 0} if unknown
	 * @since 0.3
	 * @see #operatorMnc()
	 * @see #operatorCode()
	 * @see #operatorName()
	 * @see #operatorCountryCode()
	 */
	public int operatorMcc() {
		return this.operatorMcc;
	}

	/**
	 * Mobile network code (MNC) of operator subscription is registered with.
	 *
	 * @return code
	 * @since 0.3
	 * @see #operatorMcc()
	 * @see #operatorCode()
	 * @see #operatorName()
	 * @see #operatorCountryCode()
	 */
	public int operatorMnc() {
		return this.operatorMnc;
	}

	/**
	 * Mobile country code and mobile network code (MCC-MNC) of operator subscription is
	 * registered with.
	 * <p>If the operator {@linkplain #operatorMcc() MCC} code is {@code 0}, this returns an empty
	 * string; otherwise, this returns the MCC and {@linkplain #operatorMnc() MNC} codes, padded
	 * with leading {@code 0}.
	 *
	 * @return code or {@linkplain String#isEmpty() empty} if unknown
	 * @since 0.3
	 * @see #operatorMcc()
	 * @see #operatorMnc()
	 */
	public String operatorCode() {
		return MccMnc.serialize(
			this.operatorMcc == 0 ? -1 : this.operatorMcc,
			this.operatorMnc == 0 ? -1 : this.operatorMnc
		);
	}

	/**
	 * ISO 3166-1 alpha-2 country code of operator subscription is registered with.
	 *
	 * @return code or {@linkplain String#isEmpty() empty} if unknown
	 * @since 0.3
	 * @see #operatorName()
	 * @see #operatorCode()
	 */
	public String operatorCountryCode() {
		return this.operatorCountryCode;
	}

	/**
	 * Human-readable name of operator subscription is registered with for network connectivity.
	 *
	 * @return operator name or {@linkplain String#isEmpty() empty} if unknown
	 * @since 0.3
	 * @see #networkOperatorCode()
	 * @see #networkOperatorCountryCode()
	 */
	public String networkOperatorName() {
		return this.networkOperatorName;
	}

	/**
	 * MCC of operator subscription is registered with for network connectivity.
	 *
	 * @return code or {@code 0} if unknown
	 * @since 0.3
	 * @see #networkOperatorMnc()
	 * @see #networkOperatorCode()
	 * @see #networkOperatorName()
	 * @see #networkOperatorCountryCode()
	 */
	public int networkOperatorMcc() {
		return this.networkOperatorMcc;
	}

	/**
	 * MNC of operator subscription is registered with for network connectivity.
	 *
	 * @return code
	 * @since 0.3
	 * @see #networkOperatorMcc()
	 * @see #networkOperatorCode()
	 * @see #networkOperatorName()
	 * @see #networkOperatorCountryCode()
	 */
	public int networkOperatorMnc() {
		return this.networkOperatorMnc;
	}

	/**
	 * MCC-MNC of operator subscription is registered with for network connectivity.
	 * <p>If the network operator {@linkplain #networkOperatorMcc() MCC} code is {@code 0}, this
	 * returns an empty string; otherwise, this returns the MCC and {@linkplain
	 * #networkOperatorMnc() MNC} codes, padded with leading {@code 0}.
	 *
	 * @return code or {@linkplain String#isEmpty() empty} if unknown
	 * @since 0.3
	 * @see #networkOperatorMcc()
	 * @see #networkOperatorMnc()
	 * @see #networkOperatorName()
	 * @see #networkOperatorCountryCode()
	 */
	public String networkOperatorCode() {
		return MccMnc.serialize(
			this.networkOperatorMcc == 0 ? -1 : this.networkOperatorMcc,
			this.networkOperatorMnc == 0 ? -1 : this.networkOperatorMnc
		);
	}

	/**
	 * ISO 3166-1 alpha-2 country code of operator subscription is registered with for network
	 * connectivity.
	 *
	 * @return code or {@linkplain String#isEmpty() empty} if unknown
	 * @since 0.3
	 * @see #networkOperatorName()
	 * @see #networkOperatorCode()
	 */
	public String networkOperatorCountryCode() {
		return this.networkOperatorCountryCode;
	}

	/**
	 * Construct new {@linkplain Builder builder} initialized from {@code this}.
	 *
	 * @return initialized builder
	 */
	Builder toBuilder() {
		return new Builder(this);
	}

	@Override
	public void toProtobuf(ProtobufWriter writer) {
		if (this.id != -1)
			writer.writeFixed64(ID, this.id);
		writer.writeInt32(CONNTYPE, this.connectionType);
		writer.writeString(CARRNAME, this.carrierName);
		writer.writeString(OPERNAME, this.operatorName);
		writer.writeInt32(OPERMCC, this.operatorMcc);
		writer.writeInt32(OPERMNC, this.operatorMnc);
		writer.writeString(OPERCOUNTRY, this.operatorCountryCode);
		writer.writeString(NETOPERNAME, this.networkOperatorName);
		writer.writeInt32(NETOPERMCC, this.networkOperatorMcc);
		writer.writeInt32(NETOPERMNC, this.networkOperatorMnc);
		writer.writeString(NETOPERCOUNTRY, this.networkOperatorCountryCode);
		if (this.carrierId != -1)
			writer.writeSint32(AOSPCARRID, this.carrierId);
	}

	@Override
	public int hashCode() {
		return Integer.hashCode(this.id);
	}

	@Override
	public boolean equals(@Nullable Object other) {
		if (!(other instanceof ConnectivitySubscription))
			return false;

		ConnectivitySubscription that = (ConnectivitySubscription) other;

		return this == that || (
			this.id == that.id &&
			this.connectionType == that.connectionType &&
			this.operatorMcc == that.operatorMcc &&
			this.operatorMnc == that.operatorMnc &&
			this.networkOperatorMcc == that.networkOperatorMcc &&
			this.networkOperatorMnc == that.networkOperatorMnc &&
			this.carrierName.equals(that.carrierName) &&
			this.operatorName.equals(that.operatorName) &&
			this.operatorCountryCode.equals(that.operatorCountryCode) &&
			this.networkOperatorName.equals(that.networkOperatorName) &&
			this.networkOperatorCountryCode.equals(that.networkOperatorCountryCode)
		);
	}

	@Override
	public String toString() {
		if (!BuildConfig.DEBUG)
			return "";
		return String.format(
			Locale.ROOT,
			"ConnectivitySubscription{" +
				"id=%s," +
				"connectionType=%s," +
				"carrierId=%s," +
				"operatorMcc=%s," +
				"operatorMnc=%s," +
				"networkOperatorMcc=%s," +
				"networkOperatorMnc=%s," +
				"carrierName=%s," +
				"operatorName=%s," +
				"operatorCountryCode=%s," +
				"networkOperatorName=%s," +
				"networkOperatorCountryCode=%s" +
			"}",
			this.id,
			this.connectionType,
			this.carrierId,
			this.operatorMcc,
			this.operatorMnc,
			this.networkOperatorMcc,
			this.networkOperatorMnc,
			this.carrierName,
			this.operatorName,
			this.operatorCountryCode,
			this.networkOperatorName,
			this.networkOperatorCountryCode
		);
	}
}
