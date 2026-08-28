// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.core;

import static org.polygamma.android.origin.protobuf.ProtobufField.*;

import android.annotation.SuppressLint;
import android.net.LinkAddress;
import android.os.Build;
import android.util.SparseBooleanArray;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.ReturnThis;

import org.polygamma.android.origin.adcom.enums.ConnectionType;
import org.polygamma.android.origin.protobuf.ProtobufReader;
import org.polygamma.android.origin.protobuf.ProtobufSerializable;
import org.polygamma.android.origin.protobuf.ProtobufWriter;
import org.polygamma.android.origin.util.CollectionsCompat;
import org.polygamma.android.origin.util.Preconditions;
import org.polygamma.android.origin.util.Strings;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Connectivity network description.
 *
 * @since 0.3
 * @see Connectivity#network(int)
 */
public final class ConnectivityNetwork implements ProtobufSerializable {

	private static final @Tag int ID					= ofFixed64(     1);
	private static final @Tag int CONNTYPE				= ofPackedInt32( 2);
	private static final @Tag int CAPABILITY			= ofPackedInt32( 3);
	private static final @Tag int DOWNKBPS				= ofInt32(       4);
	private static final @Tag int UPKBPS				= ofInt32(       5);
	private static final @Tag int LINKNAME				= ofBytes(       6);
	private static final @Tag int PROXYHOST				= ofString(      7);
	private static final @Tag int WIFIADDR				= ofFixed64(     8);
	private static final @Tag int IFNAME				= ofString(      9);
	private static final @Tag int IFADDR				= ofFixed64(    10);
	private static final @Tag int SUBID					= ofFixed64(    11);

	/**
	 * Network connectivity has been validated.
	 *
	 * @since 0.3
	 */
	public static final @ConnectivityNetworkCapability int CapabilityValidated		= 0;

	/**
	 * Network is capable of reaching the internet.
	 *
	 * @since 0.3
	 */
	public static final @ConnectivityNetworkCapability int CapabilityInternet		= 1;

	/**
	 * Network is capable of sending and receiving MMS messages.
	 *
	 * @since 0.3
	 */
	public static final @ConnectivityNetworkCapability int CapabilityMms			= 2;

	/**
	 * Network is not in a roaming state.
	 *
	 * @since 0.3
	 */
	public static final @ConnectivityNetworkCapability int CapabilityNotRoaming		= 3;

	/**
	 * Network traffic is unmetered.
	 *
	 * @since 0.3
	 */
	public static final @ConnectivityNetworkCapability int CapabilityUnmetered		= 4;

	private static final @ConnectivityNetworkCapability int MAX_CAPABILITY = CapabilityUnmetered;

	/**
	 * Default empty descriptor.
	 */
	private static final ConnectivityNetwork DEFAULT = new ConnectivityNetwork();

	/**
	 * Encode EUI-48 into big-endian word.
	 *
	 * @param addr address to encode
	 * @return encoded word
	 * @throws IllegalArgumentException {@code addr} is not {@linkplain String#isEmpty() empty} and
	 * malformed
	 */
	private static long encodeEui48(String addr) {
		if (addr.isEmpty())
			return 0;

		ByteBuffer rv = ByteBuffer.allocate(8);
		Iterator<String> parts = Strings.split(addr, ':');

		for (int i = 0; i < 6; i++) {
			Preconditions.checkArgument(parts.hasNext());
			rv.put((byte) (Integer.parseInt(parts.next(), 16) & 0xff));
		}
		Preconditions.checkArgument(!parts.hasNext());
		return rv.getLong(0);
	}

	/**
	 * Decode EUI-48 from big-endian word.
	 *
	 * @param addr word to decode
	 * @return decoded address
	 */
	private static String decodeEui48(long addr) {
		if (addr == 0L)
			return "";

		ByteBuffer parts = ByteBuffer.allocate(8)
			.putLong(0, addr);
		StringBuilder str = new StringBuilder();

		for (int i = 0; i < 6; i++) {
			String part = Integer.toString(parts.get(i) & 0xff, 16);

			if (part.length() < 2)
				str.append('0');
			str.append(part);
			if (i < 5)
				str.append(':');
		}
		return str.toString();
	}

	/**
	 * Extract keys for all {@code true} mappings.
	 *
	 * @param arr array to extract from
	 * @return keys
	 */
	private static int[] keysOf(SparseBooleanArray arr) {
		int[] keys = new int[arr.size()];
		int n = 0;

		for (int i = 0; i < arr.size(); i++) {
			if (arr.valueAt(i))
				keys[n++] = arr.keyAt(i);
		}
		return Arrays.copyOf(keys, n);
	}

	/**
	 * Connectivity network {@linkplain ConnectivityNetwork descriptor} builder.
	 *
	 * @see #ofBuilder()
	 */
	static final class Builder {

		private ConnectivityNetwork network;
		private boolean needClone;

		private Builder(ConnectivityNetwork net) {
			this.network = net;
			this.needClone = true;
		}

		private ConnectivityNetwork target() {
			if (this.needClone) {
				this.network = new ConnectivityNetwork(this.network);
				this.needClone = false;
			}
			return this.network;
		}

		/**
		 * Set id of network.
		 *
		 * @param id network id or {@code -1} if unknown
		 * @return network id
		 * @see ConnectivityNetwork#id()
		 */
		@ReturnThis
		Builder id(long id) {
			this.target().id = id;
			return this;
		}

		/**
		 * Set connection types supported by network.
		 *
		 * @param types supported connection types
		 * @return {@code this}
		 * @see ConnectivityNetwork#supportsConnection(int)
		 */
		@ReturnThis
		Builder connectionTypes(@ConnectionType int... types) {
			this.target().connectionTypes = Arrays.copyOf(types, types.length);
			return this;
		}

		/**
		 * Set connection types supported by network, from sparse array.
		 *
		 * @param caps supported connection types array
		 * @return {@code this}
		 * @see #connectionTypes(int...)
		 */
		@SuppressLint("ReturnThis")
		@ReturnThis
		Builder connectionTypes(SparseBooleanArray caps) {
			return this.connectionTypes(keysOf(caps));
		}

		/**
		 * Set capabilities supported by network.
		 *
		 * @param caps supported capabilities
		 * @return {@code this}
		 * @see ConnectivityNetwork#hasCapability(int)
		 */
		@ReturnThis
		Builder capabilities(@ConnectivityNetworkCapability int... caps) {
			int mask = 0;

			for (int cap : caps) {
				if (cap >= 0 && cap <= MAX_CAPABILITY)
					mask |= (1 << cap);
			}
			this.target().capabilitiesMask = mask;
			return this;
		}

		/**
		 * Set capabilities supported by network, from sparse array.
		 *
		 * @param caps supported capabilities array
		 * @return {@code this}
		 * @see #capabilities(int...)
		 */
		@ReturnThis
		Builder capabilities(SparseBooleanArray caps) {
			int mask = 0;

			for (int i = 0; i < caps.size(); i++) {
				if (!caps.valueAt(i))
					continue;

				int cap = caps.keyAt(i);

				if (cap >= 0 && cap <= MAX_CAPABILITY)
					mask |= (1 << cap);
			}
			this.target().capabilitiesMask = mask;
			return this;
		}

		/**
		 * Set downstream bandwidth of network, in Kbps.
		 *
		 * @param kbps bandwidth
		 * @return {@code this}
		 * @see ConnectivityNetwork#downstreamKbps()
		 */
		@ReturnThis
		Builder downstreamKbps(int kbps) {
			this.target().downstreamKbps = kbps;
			return this;
		}

		/**
		 * Set upstream bandwidth of network, in Kbps.
		 *
		 * @param kbps bandwidth
		 * @return {@code this}
		 * @see ConnectivityNetwork#upstreamKbps()
		 */
		@ReturnThis
		Builder upstreamKbps(int kbps) {
			this.target().upstreamKbps = kbps;
			return this;
		}

		/**
		 * Set names identifying network links.
		 *
		 * @param names link names
		 * @return {@code this}
		 * @see ConnectivityNetwork#linkName(int)
		 */
		@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
		@ReturnThis
		Builder linkNames(List<LinkAddress> names) {
			if (names.isEmpty()) {
				this.target().linkNames = DEFAULT.linkNames;
				return this;
			}

			InetAddress[] addrs = new InetAddress[names.size()];

			for (int i = 0; i < addrs.length; i++)
				addrs[i] = names.get(i).getAddress();
			this.target().linkNames = addrs;
			return this;
		}

		/**
		 * Set host of proxy through which requests should be proxied.
		 *
		 * @param host proxy host or {@linkplain String#isEmpty() empty} if requests should not
		 * be proxied
		 * @return {@code this}
		 * @see ConnectivityNetwork#proxyHost()
		 */
		@ReturnThis
		Builder proxyHost(String host) {
			this.target().proxyHost = host;
			return this;
		}

		/**
		 * Set basic service set identifier (BSSID) of current WiFi access point.
		 *
		 * @param addr BSSID or, {@linkplain String#isEmpty() empty} if unknown or not applicable
		 * @return {@code this}
		 * @throws IllegalArgumentException {@code addr} is not {@linkplain String#isEmpty() empty}
		 * and malformed
		 * @see ConnectivityNetwork#wifiAddress()
		 */
		@ReturnThis
		Builder wifiAddress(String addr) {
			this.target().wifiAddress = encodeEui48(addr);
			return this;
		}

		/**
		 * Set network interface name.
		 *
		 * @param name interface name
		 * @return {@code this}
		 * @see ConnectivityNetwork#interfaceName()
		 */
		@ReturnThis
		Builder interfaceName(String name) {
			this.target().interfaceName = name;
			return this;
		}

		/**
		 * Set network interface address.
		 *
		 * @param addr interface address
		 * @return {@code this}
		 * @throws IllegalArgumentException {@code addr} is not {@linkplain String#isEmpty() empty}
		 * and malformed
		 * @see ConnectivityNetwork#interfaceAddress()
		 */
		@ReturnThis
		Builder interfaceAddress(String addr) {
			this.target().interfaceAddress = encodeEui48(addr);
			return this;
		}

		/**
		 * Set id of subscription used by network, if any.
		 *
		 * @param id subscription id or, {@code -1} if unknown or network does not use cellular
		 * network data
		 * @return {@code this}
		 * @see ConnectivityNetwork#subscriptionId()
		 */
		@ReturnThis
		Builder subscriptionId(long id) {
			this.target().subscriptionId = id;
			return this;
		}

		/**
		 * Build resulting network.
		 *
		 * @return network instance
		 */
		ConnectivityNetwork build() {
			this.needClone = true;
			return this.network;
		}
	}

	/**
	 * Construct new empty {@linkplain Builder builder}.
	 *
	 * @return empty builder instance
	 */
	static Builder ofBuilder() {
		return DEFAULT.toBuilder();
	}

	/**
	 * Deserialize network description from Protobuf message.
	 *
	 * @param reader reader to deserialize from
	 * @return deserialized description
	 * @throws RuntimeException coding is malformed
	 * @since 1.2
	 */
	public static ConnectivityNetwork ofProtobuf(ProtobufReader reader) {
		ConnectivityNetwork rv = new ConnectivityNetwork(DEFAULT);
		List<InetAddress> linkNames = new ArrayList<>(0);

		while (reader.hasRemaining()) {
			int tag = reader.readTag();

			if (tag == ID) {
				rv.id = reader.readFixed64();
			} else if (tag == CONNTYPE) {
				rv.connectionTypes = reader.readPackedInt32();
			} else if (tag == CAPABILITY) {
				rv.capabilitiesMask = (int) (reader.readWordBitmap(0) & 0xffffffffL);
			} else if (tag == DOWNKBPS) {
				rv.downstreamKbps = reader.readInt32();
			} else if (tag == UPKBPS) {
				rv.upstreamKbps = reader.readInt32();
			} else if (tag == LINKNAME) {
				try {
					linkNames.add(InetAddress.getByAddress(reader.readBytes()));
				} catch (UnknownHostException cause) {
					throw new RuntimeException("failed to deserialize link name", cause);
				}
			} else if (tag == PROXYHOST) {
				rv.proxyHost = reader.readString();
			} else if (tag == WIFIADDR) {
				rv.wifiAddress = reader.readFixed64();
			} else if (tag == IFNAME) {
				rv.interfaceName = reader.readString();
			} else if (tag == IFADDR) {
				rv.interfaceAddress = reader.readFixed64();
			} else if (tag == SUBID) {
				rv.subscriptionId = reader.readFixed64();
			}
		}
		rv.linkNames = CollectionsCompat.toArrayOrEmpty(linkNames, DEFAULT.linkNames);
		return rv;
	}

	private long id;
	private long subscriptionId;
	private long wifiAddress;
	private long interfaceAddress;
	private String interfaceName;
	private InetAddress[] linkNames;
	private String proxyHost;
	private @ConnectionType int[] connectionTypes;
	private int capabilitiesMask;
	private int downstreamKbps;
	private int upstreamKbps;

	private ConnectivityNetwork() {
		this.id = -1;
		this.subscriptionId = -1;
		this.interfaceName = "";
		this.linkNames = new InetAddress[0];
		this.proxyHost = "";
		this.connectionTypes = new int[0];
	}

	private ConnectivityNetwork(ConnectivityNetwork that) {
		this.id = that.id;
		this.subscriptionId = that.subscriptionId;
		this.wifiAddress = that.wifiAddress;
		this.interfaceAddress = that.interfaceAddress;
		this.interfaceName = that.interfaceName;
		this.linkNames = that.linkNames;
		this.proxyHost = that.proxyHost;
		this.connectionTypes = that.connectionTypes;
		this.capabilitiesMask = that.capabilitiesMask;
		this.downstreamKbps = that.downstreamKbps;
		this.upstreamKbps = that.upstreamKbps;
	}

	/**
	 * Network id.
	 *
	 * @return id
	 * @since 1.1
	 */
	public long id() {
		return this.id;
	}

	/**
	 * Test whether a connection type is supported by network.
	 *
	 * @param conn connection type to test
	 * @return {@code true} if, and only if, {@code conn} is supported
	 * @since 0.3
	 */
	public boolean supportsConnection(@ConnectionType int conn) {
		return Arrays.binarySearch(this.connectionTypes, conn) >= 0;
	}

	/**
	 * Test whether network has a capability.
	 *
	 * @param cap capability to test for
	 * @return {@code true} if, and only if, {@code cap} is supported
	 * @since 0.3
	 */
	public boolean hasCapability(@ConnectivityNetworkCapability int cap) {
		return (this.capabilitiesMask & (1 << cap)) != 0;
	}

	/**
	 * Downstream bandwidth of network, in Kbps.
	 *
	 * @return downstream bandwidth, or {@code 0} if unknown
	 * @since 0.3
	 */
	public int downstreamKbps() {
		return this.downstreamKbps;
	}

	/**
	 * Upstream bandwidth of network, in Kbps.
	 *
	 * @return upstream bandwidth, or {@code 0} if unknown
	 * @since 0.3
	 */
	public int upstreamKbps() {
		return this.upstreamKbps;
	}

	/**
	 * Count of names identifying network link.
	 *
	 * @return link name count
	 * @since 1.2
	 * @see #linkName(int)
	 */
	public int linkNameCount() {
		return this.linkNames.length;
	}

	/**
	 * Name identifying network link, at index.
	 *
	 * @param i index to retrieve link name at
	 * @return link name at index {@code i}
	 * @throws IndexOutOfBoundsException {@code i} is negative or, greater than or equal to link
	 * name {@linkplain #linkNameCount() count}
	 * @since 1.2
	 * @see #linkNameCount()
	 */
	public InetAddress linkName(int i) {
		return this.linkNames[i];
	}

	/**
	 * Host of proxy through which HTTP requests should be proxied.
	 *
	 * @return proxy host, or {@linkplain String#isEmpty() empty} if requests should not be proxied
	 * @since 0.3
	 */
	public String proxyHost() {
		return this.proxyHost;
	}

	/**
	 * Basic service set identifier (BSSID) of current WiFi access point.
	 *
	 * @return BSSID or, {@linkplain String#isEmpty() empty} if unknown or not applicable
	 * @since 0.3
	 */
	public String wifiAddress() {
		return decodeEui48(this.wifiAddress);
	}

	/**
	 * Name of network interface.
	 *
	 * @return interface name
	 * @since 0.3
	 */
	public String interfaceName() {
		return this.interfaceName;
	}

	/**
	 * Address of network interface.
	 *
	 * @return interface address or {@linkplain String#isEmpty() empty} if unknown
	 * @since 0.3
	 */
	public String interfaceAddress() {
		return decodeEui48(this.interfaceAddress);
	}

	/**
	 * Id of subscription used by network, if any.
	 *
	 * @return subscription id or, {@code -1} if unknown or network does not use cellular network
	 * @since 0.3
	 * @see ConnectivitySubscription#id()
	 */
	public long subscriptionId() {
		return this.subscriptionId;
	}

	/**
	 * Construct new {@linkplain Builder builder} initialized from {@code this}.
	 *
	 * @return initialized builder instance
	 */
	Builder toBuilder() {
		return new Builder(this);
	}

	@Override
	public void toProtobuf(ProtobufWriter writer) {
		if (this.id != -1)
			writer.writeFixed64(ID, this.id);
		writer.writePackedInt32(CONNTYPE, this.connectionTypes);
		writer.writeWordBitmap(CAPABILITY, Integer.toUnsignedLong(this.capabilitiesMask), 0);
		writer.writeInt32(DOWNKBPS, this.downstreamKbps);
		writer.writeInt32(UPKBPS, this.upstreamKbps);
		writer.writeString(PROXYHOST, this.proxyHost);
		writer.writeFixed64(WIFIADDR, this.wifiAddress);
		writer.writeString(IFNAME, this.interfaceName);
		writer.writeFixed64(IFADDR, this.interfaceAddress);
		if (this.subscriptionId != -1)
			writer.writeFixed64(SUBID, this.subscriptionId);

		for (InetAddress name : this.linkNames)
			writer.writeBytes(LINKNAME, name.getAddress());
	}

	@Override
	public int hashCode() {
		return Long.hashCode(this.id);
	}

	@Override
	public boolean equals(@Nullable Object other) {
		if (!(other instanceof ConnectivityNetwork))
			return false;

		ConnectivityNetwork that = (ConnectivityNetwork) other;

		return this == that || (
			this.id == that.id &&
			this.subscriptionId == that.subscriptionId &&
			this.wifiAddress == that.wifiAddress &&
			this.interfaceAddress == that.interfaceAddress &&
			this.interfaceName.equals(that.interfaceName) &&
			Arrays.equals(this.linkNames, that.linkNames) &&
			this.proxyHost.equals(that.proxyHost) &&
			Arrays.equals(this.connectionTypes, that.connectionTypes) &&
			this.capabilitiesMask == that.capabilitiesMask &&
			this.downstreamKbps == that.downstreamKbps &&
			this.upstreamKbps == that.upstreamKbps
		);
	}

	@Override
	public String toString() {
		if (!BuildConfig.DEBUG)
			return "";
		return String.format(
			Locale.ROOT,
			"ConnectivityNetwork{" +
				"id=%s," +
				"subscriptionId=%s," +
				"wifiAddress=%s" +
				"interfaceAddress=%s," +
				"interfaceName=%s," +
				"linkNames=%s," +
				"proxyHost=%s," +
				"connectionTypes=%s," +
				"capabilitiesMask=%s," +
				"downstreamKbps=%s," +
				"upstreamKbps=%s" +
			"}",
			this.id,
			this.subscriptionId,
			this.wifiAddress,
			this.interfaceAddress,
			this.interfaceName,
			Arrays.toString(this.linkNames),
			this.proxyHost,
			Arrays.toString(this.connectionTypes),
			this.capabilitiesMask,
			this.downstreamKbps,
			this.upstreamKbps
		);
	}
}
