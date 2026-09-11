// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.core;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.polygamma.android.origin.core.ConnectivityNetwork.CapabilityInternet;
import static org.polygamma.android.origin.core.ConnectivityNetwork.CapabilityMms;
import static org.polygamma.android.origin.core.ConnectivityNetwork.CapabilityNotRoaming;
import static org.polygamma.android.origin.core.ConnectivityNetwork.CapabilityUnmetered;
import static org.polygamma.android.origin.core.ConnectivityNetwork.CapabilityValidated;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.polygamma.android.origin.adcom.enums.AdComEnums;
import org.polygamma.android.origin.protobuf.ProtobufDecoder;
import org.polygamma.android.origin.protobuf.ProtobufEncoder;

import java.util.Arrays;
import java.util.Random;
import java.util.UUID;

/**
 * {@link Connectivity} tests.
 */
@RunWith(AndroidJUnit4.class)
public class ConnectivityTest {

	private static final int[] CONNECTION_TYPES = {
		AdComEnums.ConnectionBluetooth,
		AdComEnums.ConnectionCell,
		AdComEnums.ConnectionCell2G,
		AdComEnums.ConnectionCell3G,
		AdComEnums.ConnectionCell4G,
		AdComEnums.ConnectionCell5G,
		AdComEnums.ConnectionUnknown,
		AdComEnums.ConnectionVpn,
		AdComEnums.ConnectionWiMax,
		AdComEnums.ConnectionWifi,
		AdComEnums.ConnectionWired
	};

	private static final int[] NETWORK_CAPABILITIES = {
		CapabilityInternet,
		CapabilityMms,
		CapabilityNotRoaming,
		CapabilityUnmetered,
		CapabilityValidated
	};

	private static String nextBssid(Random rand) {
		if (rand.nextBoolean())
			return "";

		byte[] addr = new byte[6];

		rand.nextBytes(addr);
		addr[0] = (byte) ((addr[0] & 0xfe) | 0x02);

		StringBuilder rv = new StringBuilder(17);

		for (int i = 0; i < addr.length; i++) {
			rv.append(String.format("%02X", addr[i]));
			if (i < (addr.length - 1))
				rv.append(":");
		}
		return rv.toString();
	}

	private static ConnectivityNetwork nextNetwork(Random rand) {
		int[] connTypes = new int[rand.nextInt(CONNECTION_TYPES.length)];
		int[] caps = new int[rand.nextInt(NETWORK_CAPABILITIES.length)];

		System.arraycopy(CONNECTION_TYPES, 0, connTypes, 0, connTypes.length);
		System.arraycopy(NETWORK_CAPABILITIES, 0, caps, 0, caps.length);

		return ConnectivityNetwork.ofBuilder()
			.id(rand.nextLong())
			.connectionTypes(connTypes)
			.capabilities(caps)
			.downstreamKbps(rand.nextInt())
			.upstreamKbps(rand.nextInt())
			.proxyHost(rand.nextBoolean() ? "" : UUID.randomUUID().toString())
			.wifiAddress(nextBssid(rand))
			.interfaceName(rand.nextBoolean() ? "" : UUID.randomUUID().toString())
			.interfaceAddress(nextBssid(rand))
			.subscriptionId(rand.nextLong())
			.build();
	}

	private static ConnectivitySubscription nextSubscription(Random rand) {
		return ConnectivitySubscription.ofBuilder()
			.id(rand.nextInt())
			.connectionType(CONNECTION_TYPES[rand.nextInt(CONNECTION_TYPES.length)])
			.carrierId(rand.nextInt())
			.carrierName(rand.nextBoolean() ? "" : UUID.randomUUID().toString())
			.operatorName(rand.nextBoolean() ? "" : UUID.randomUUID().toString())
			.operatorMcc(rand.nextInt(100))
			.operatorMnc(rand.nextInt(100))
			.networkOperatorName(rand.nextBoolean() ? "" : UUID.randomUUID().toString())
			.networkOperatorMcc(rand.nextInt(100))
			.networkOperatorMnc(rand.nextInt(100))
			.build();
	}

	private static Connectivity nextConnectivity(Random rand) {
		int numNet = rand.nextInt(32);
		int numSub = rand.nextInt(32);
		ConnectivityNetwork[] nets = new ConnectivityNetwork[numNet];
		ConnectivitySubscription[] subs = new ConnectivitySubscription[numSub];

		for (int i = 0; i < nets.length; i++)
			nets[i] = nextNetwork(rand);
		for (int i = 0; i < subs.length; i++)
			subs[i] = nextSubscription(rand);
		return Connectivity.of()
			.withNetworks(Arrays.asList(nets), rand.nextInt(nets.length + 1))
			.withSubscriptions(Arrays.asList(subs), rand.nextInt(subs.length + 1));
	}

	@Test
	public void testSerde() {
		Random rand = new Random(44);
		ProtobufEncoder enc = ProtobufEncoder.of();

		for (int i = 0; i < 10000; i++) {
			Connectivity exp = nextConnectivity(rand);

			exp.toProtobuf(enc.reset());

			Connectivity got = Connectivity.ofProtobuf(ProtobufDecoder.ofBuffer(enc.asBuffer()));

			assertArrayEquals(exp.networks, got.networks);
			assertArrayEquals(exp.subscriptions, got.subscriptions);
			assertEquals(exp.activeNetwork(), got.activeNetwork());
			assertEquals(exp.activeSubscription(), got.activeSubscription());
		}
	}
}
