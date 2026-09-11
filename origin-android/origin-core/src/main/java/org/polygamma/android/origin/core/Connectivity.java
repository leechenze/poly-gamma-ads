// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.core;

import static org.polygamma.android.origin.protobuf.Protobuf.WIRE_LEN;
import static org.polygamma.android.origin.protobuf.Protobuf.WIRE_VARINT;
import static org.polygamma.android.origin.protobuf.Protobuf.fieldTagOf;

import androidx.annotation.CheckResult;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import org.polygamma.android.origin.protobuf.Protobuf.FieldTag;
import org.polygamma.android.origin.protobuf.ProtobufDecoder;
import org.polygamma.android.origin.protobuf.ProtobufEncoder;
import org.polygamma.android.origin.protobuf.ProtobufSerializable;
import org.polygamma.android.origin.util.CollectionsCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Network connectivity description.
 * <p>Network connectivity is described by available {@linkplain #network(int) networks} and
 * cellular network {@linkplain #subscription(int) subscriptions}. The active network and
 * subscription, if any, can be determined using {@link #activeNetwork()} and {@link
 * #activeSubscription()}, respectively.
 *
 * @since 0.3
 */
public final class Connectivity implements ProtobufSerializable {

	private static final @FieldTag int NET			= fieldTagOf(1, WIRE_LEN);
	private static final @FieldTag int ACTNETIDX	= fieldTagOf(2, WIRE_VARINT);
	private static final @FieldTag int SUB			= fieldTagOf(3, WIRE_LEN);
	private static final @FieldTag int ACTSUBIDX	= fieldTagOf(4, WIRE_VARINT);

	private static final Connectivity DEFAULT =
		new Connectivity(new ConnectivityNetwork[0], 0, new ConnectivitySubscription[0], 0);

	/**
	 * Default empty connectivity description instance.
	 *
	 * @return empty description instance
	 * @since 1.2
	 */
	public static Connectivity of() {
		return DEFAULT;
	}

	/**
	 * Deserialize description from Protobuf message.
	 *
	 * @param dec decoder to deserialize from
	 * @return deserialized description
	 * @throws RuntimeException coding is malformed
	 * @since 1.2
	 */
	public static Connectivity ofProtobuf(ProtobufDecoder dec) {
		List<ConnectivityNetwork> nets = new ArrayList<>(0);
		List<ConnectivitySubscription> subs = new ArrayList<>(0);
		int actNetIdx = 0;
		int actSubIdx = 0;

		while (dec.hasRemaining()) {
			int tag = dec.decodeFieldTag();

			if (tag == NET)
				nets.add(dec.decodeLen(ConnectivityNetwork::ofProtobuf));
			else if (tag == ACTNETIDX)
				actNetIdx = dec.decodeUint32();
			else if (tag == SUB)
				subs.add(dec.decodeLen(ConnectivitySubscription::ofProtobuf));
			else if (tag == ACTSUBIDX)
				actSubIdx = dec.decodeUint32();
			else
				dec.skipFieldValue(tag);
		}
		return nets.isEmpty() && subs.isEmpty() ? DEFAULT : new Connectivity(
			CollectionsCompat.toArrayOrEmpty(nets, DEFAULT.networks),
			actNetIdx,
			CollectionsCompat.toArrayOrEmpty(subs, DEFAULT.subscriptions),
			actSubIdx
		);
	}

	@VisibleForTesting
	final ConnectivityNetwork[] networks;
	@VisibleForTesting
	final ConnectivitySubscription[] subscriptions;
	private final int activeNetworkIndex;
	private final int activeSubscriptionIndex;

	private Connectivity(
		ConnectivityNetwork[] nets,
		int actNetIdx,
		ConnectivitySubscription[] subs,
		int actSubIdx
	) {
		this.networks = nets;
		this.activeNetworkIndex = actNetIdx;
		this.subscriptions = subs;
		this.activeSubscriptionIndex = actSubIdx;
	}

	/**
	 * Available network count.
	 *
	 * @return network count
	 * @since 1.2
	 * @see #network(int)
	 */
	public int networkCount() {
		return this.networks.length;
	}

	/**
	 * Available network, at index.
	 *
	 * @param i index to retrieve network at
	 * @return network at index {@code i}
	 * @throws IndexOutOfBoundsException {@code i} is negative or, greater than or equal to network
	 * {@linkplain #networkCount() count}
	 * @since 1.2
	 * @see #networkCount()
	 * @see #activeNetwork()
	 */
	public ConnectivityNetwork network(int i) {
		return this.networks[i];
	}

	/**
	 * Current active network.
	 *
	 * @return active network or {@code null} if no active network
	 * @since 0.3
	 */
	public @Nullable ConnectivityNetwork activeNetwork() {
		int i = this.activeNetworkIndex;

		return i == this.networks.length ? null : this.networks[i];
	}

	/**
	 * Construct copy of {@code this} with new network descriptions.
	 *
	 * @param nets new network descriptions
	 * @param actIdx index of description describing active network or {@code nets.size()}
	 * @return resulting description
	 */
	@CheckResult
	Connectivity withNetworks(Collection<ConnectivityNetwork> nets, int actIdx) {
		return new Connectivity(
			CollectionsCompat.toArrayOrEmpty(nets, DEFAULT.networks),
			actIdx,
			this.subscriptions,
			this.activeSubscriptionIndex
		);
	}

	/**
	 * Available subscription count.
	 *
	 * @return subscription count
	 * @since 1.2
	 * @see #subscription(int)
	 */
	public int subscriptionCount() {
		return this.subscriptions.length;
	}

	/**
	 * Available subscription, at index.
	 *
	 * @param i index to retrieve subscription at
	 * @return subscription at index {@code i}
	 * @throws IndexOutOfBoundsException {@code i} is negative or, greater than or equal to
	 * subscription {@linkplain #subscriptionCount() count}
	 * @since 1.2
	 * @see #subscriptionCount()
	 * @see #activeSubscription()
	 */
	public ConnectivitySubscription subscription(int i) {
		return this.subscriptions[i];
	}

	/**
	 * Current active subscription.
	 *
	 * @return active subscription or {@code null} if no active subscription
	 * @since 0.3
	 */
	public @Nullable ConnectivitySubscription activeSubscription() {
		int i = this.activeSubscriptionIndex;

		return i == this.subscriptions.length ? null : this.subscriptions[i];
	}

	/**
	 * Construct copy of {@code this} with new subscription descriptions.
	 *
	 * @param subs new subscription descriptions
	 * @param actIdx index of description describing active subscription or {@code subs.size()}
	 * @return resulting description
	 */
	@CheckResult
	Connectivity withSubscriptions(Collection<ConnectivitySubscription> subs, int actIdx) {
		return new Connectivity(
			this.networks,
			this.activeNetworkIndex,
			CollectionsCompat.toArrayOrEmpty(subs, DEFAULT.subscriptions),
			actIdx
		);
	}

	@Override
	public void toProtobuf(ProtobufEncoder enc) {
		for (ConnectivityNetwork net : this.networks)
			enc.encodeMessageField(NET, net);
		for (ConnectivitySubscription sub : this.subscriptions)
			enc.encodeMessageField(SUB, sub);
		enc.encodeUnsignedIntField(ACTNETIDX, this.activeNetworkIndex)
			.encodeUnsignedIntField(ACTSUBIDX, this.activeSubscriptionIndex);
	}

	@Override
	public int hashCode() {
		return Integer.hashCode(this.activeNetworkIndex) ^
			Integer.hashCode(this.activeSubscriptionIndex);
	}

	@Override
	public boolean equals(@Nullable Object other) {
		if (!(other instanceof Connectivity))
			return false;

		Connectivity that = (Connectivity) other;

		return this == that || (
			Arrays.equals(this.networks, that.networks) &&
			this.activeNetworkIndex == that.activeNetworkIndex &&
			Arrays.equals(this.subscriptions, that.subscriptions) &&
			this.activeSubscriptionIndex == that.activeSubscriptionIndex
		);
	}

	@Override
	public String toString() {
		if (!BuildConfig.DEBUG)
			return "";
		return String.format(
			Locale.ROOT,
			"Connectivity{" +
				"networks=%s," +
				"activeNetworkIndex=%s," +
				"subscriptions=%s," +
				"activeSubscriptionIndex=%s" +
			"}",
			Arrays.toString(this.networks),
			this.activeNetworkIndex,
			Arrays.toString(this.subscriptions),
			this.activeSubscriptionIndex
		);
	}
}
