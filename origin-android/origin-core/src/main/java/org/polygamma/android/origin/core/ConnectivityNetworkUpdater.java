// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.core;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.NetworkSpecifier;
import android.net.ProxyInfo;
import android.net.TelephonyNetworkSpecifier;
import android.net.TransportInfo;
import android.net.wifi.WifiInfo;
import android.os.Build;
import android.os.ext.SdkExtensions;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Pair;
import android.util.SparseBooleanArray;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.core.util.Consumer;

import org.polygamma.android.origin.adcom.enums.AdComEnums;
import org.polygamma.android.origin.util.AndroidContexts;
import org.polygamma.android.origin.util.Logger;
import org.polygamma.android.origin.util.Strings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Network {@linkplain Connectivity#network(int) descriptions} updater.
 */
@SuppressLint("MissingPermission")
@SuppressWarnings("deprecation")
class ConnectivityNetworkUpdater extends ConnectivityUpdater {

	private static final String TAG = ConnectivityNetworkUpdater.class.getSimpleName();

	/**
	 * Update network capabilities description.
	 *
	 * @param dst descriptor to update
	 * @param src capabilities to update from
	 */
	@SuppressLint("HardwareIds")
	@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
	private static void
	probeCapabilities(ConnectivityNetwork.Builder dst, NetworkCapabilities src) {
		SparseBooleanArray caps = new SparseBooleanArray();
		SparseBooleanArray conns = new SparseBooleanArray();

		caps.put(
			ConnectivityNetwork.CapabilityInternet,
			src.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
		);
		caps.put(
			ConnectivityNetwork.CapabilityMms,
			src.hasCapability(NetworkCapabilities.NET_CAPABILITY_MMS)
		);
		caps.put(
			ConnectivityNetwork.CapabilityNotRoaming,
			Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
			src.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING)
		);
		caps.put(
			ConnectivityNetwork.CapabilityValidated,
			Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
			src.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
		);
		caps.put(
			ConnectivityNetwork.CapabilityUnmetered,
			src.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
		);

		conns.put(
			AdComEnums.ConnectionBluetooth,
			src.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH)
		);
		conns.put(
			AdComEnums.ConnectionCell,
			src.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) || ((
				Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM || (
					Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
					SdkExtensions.getExtensionVersion(Build.VERSION_CODES.UPSIDE_DOWN_CAKE) >= 12
				)
			) && src.hasTransport(NetworkCapabilities.TRANSPORT_SATELLITE))
		);
		conns.put(
			AdComEnums.ConnectionWired,
			src.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) || (
				Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
				src.hasTransport(NetworkCapabilities.TRANSPORT_USB)
			)
		);
		conns.put(
			AdComEnums.ConnectionWifi,
			src.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || (
				Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
				src.hasTransport(NetworkCapabilities.TRANSPORT_WIFI_AWARE)
			) || (
				Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1 &&
				src.hasTransport(NetworkCapabilities.TRANSPORT_LOWPAN)
			)
		);
		conns.put(AdComEnums.ConnectionVpn, src.hasTransport(NetworkCapabilities.TRANSPORT_VPN));

		dst.capabilities(caps)
			.connectionTypes(conns)
			.downstreamKbps(src.getLinkDownstreamBandwidthKbps())
			.upstreamKbps(src.getLinkUpstreamBandwidthKbps());

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			TransportInfo info = src.getTransportInfo();

			if (info instanceof WifiInfo) {
				WifiInfo wifi = (WifiInfo) info;

				dst.wifiAddress(Strings.nullToEmpty(wifi.getBSSID()));
				dst.interfaceAddress(Strings.nullToEmpty(wifi.getMacAddress()));
			}
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			NetworkSpecifier spec = src.getNetworkSpecifier();

			if (spec instanceof TelephonyNetworkSpecifier)
				dst.subscriptionId(((TelephonyNetworkSpecifier) spec).getSubscriptionId());
		}
	}

	/**
	 * Update link properties description.
	 *
	 * @param dst descriptor to update
	 * @param src properties to update from
	 */
	@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
	private static void probeLink(ConnectivityNetwork.Builder dst, LinkProperties src) {
		ProxyInfo proxy = src.getHttpProxy();

		dst.linkNames(src.getLinkAddresses())
			.interfaceName(Strings.nullToEmpty(src.getInterfaceName()))
			.proxyHost(
				proxy == null || Strings.nullToEmpty(proxy.getHost()).isEmpty() ? "" :
				proxy.getHost()
			);
	}

	/**
	 * Batched network updates.
	 */
	private static final class UpdateBatch {

		private final ArrayMap<Network, LinkProperties> links;
		private final ArrayMap<Network, NetworkCapabilities> capabilities;
		private final ArrayMap<Network, Network> lost;

		private UpdateBatch() {
			this.links = new ArrayMap<>();
			this.capabilities = new ArrayMap<>();
			this.lost = new ArrayMap<>();
		}
	}

	/**
	 * Network update callback.
	 */
	@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
	@VisibleForTesting
	static final class OnUpdate
	extends ConnectivityManager.NetworkCallback
	implements ConnectivityManager.OnNetworkActiveListener {

		private final ImplLollipop updater;
		private final ReadWriteLock lock;
		private UpdateBatch batch;

		@RequiresApi(api = Build.VERSION_CODES.S)
		@VisibleForTesting
		OnUpdate(ImplLollipop updater, @SuppressWarnings("SameParameterValue") int flags) {
			super(flags);
			this.updater = updater;
			this.lock = new ReentrantReadWriteLock();
			this.batch = new UpdateBatch();
		}

		@VisibleForTesting
		OnUpdate(ImplLollipop updater) {
			this.updater = updater;
			this.lock = new ReentrantReadWriteLock();
			this.batch = new UpdateBatch();
		}

		/**
		 * Poll batched updates.
		 *
		 * @return batched updates
		 */
		UpdateBatch poll() {
			UpdateBatch prev;
			UpdateBatch next = new UpdateBatch();
			Lock read = this.lock.readLock();

			read.lock();
			try {
				prev = this.batch;
				this.batch = next;
			} finally {
				read.unlock();
			}
			return prev;
		}

		private void appendUpdate(Consumer<UpdateBatch> with) {
			Lock write = this.lock.writeLock();

			write.lock();
			try {
				with.accept(this.batch);
			} finally {
				write.unlock();
			}
			this.updater.onUpdateAvailable();
		}

		@Override
		public void onAvailable(Network net) {
			this.appendUpdate(batch -> {
				ConnectivityManager man = this.updater.connectivityManager;

				batch.capabilities.put(net, man.getNetworkCapabilities(net));
				batch.links.put(net, man.getLinkProperties(net));
				batch.lost.remove(net);
			});
			Logger.debug(TAG, "network available: %s", net);
		}

		@Override
		public void onCapabilitiesChanged(Network net, NetworkCapabilities caps) {
			this.appendUpdate(batch -> batch.capabilities.put(net, caps));
			Logger.debug(TAG, "network capabilities changed: %s", net);
		}

		@Override
		public void onLinkPropertiesChanged(Network net, LinkProperties link) {
			this.appendUpdate(batch -> batch.links.put(net, link));
			Logger.debug(TAG, "network link properties changed: %s", net);
		}

		@Override
		public void onLost(Network net) {
			this.appendUpdate(batch -> {
				batch.lost.put(net, net);
				batch.capabilities.remove(net);
				batch.links.remove(net);
			});
			Logger.debug(TAG, "network lost: %s", net);
		}

		@Override
		public void onNetworkActive() {
			this.updater.onUpdateAvailable();
		}
	}

	/**
	 * Android Lollipop and above specialization.
	 */
	@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
	@VisibleForTesting
	static final class ImplLollipop extends ConnectivityNetworkUpdater {

		@VisibleForTesting
		final ArrayMap<Network, ConnectivityNetwork> descriptors;
		@VisibleForTesting
		@Nullable OnUpdate onUpdate;

		@VisibleForTesting
		ImplLollipop(ConnectivityManager connMan) {
			super(connMan);
			this.descriptors = new ArrayMap<>();
		}

		/**
		 * Update descriptors of available networks.
		 */
		private void updateAvailable() {
			UpdateBatch batch = this.onUpdate == null ? new UpdateBatch() : this.onUpdate.poll();

			if (this.onUpdate == null) {
				for (Network net : super.connectivityManager.getAllNetworks()) {
					batch.links.put(net, super.connectivityManager.getLinkProperties(net));
					batch.capabilities.put(
						net,
						super.connectivityManager.getNetworkCapabilities(net)
					);
				}
			}

			ArrayMap<Network, ConnectivityNetwork> descs = this.descriptors;
			Set<Network> nets =
				Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? new ArraySet<>() :
				new HashSet<>();

			nets.addAll(batch.links.keySet());
			nets.addAll(batch.capabilities.keySet());

			for (Network lost : batch.lost.keySet())
				descs.remove(lost);
			for (Network net : nets) {
				ConnectivityNetwork prev = descs.get(net);
				ConnectivityNetwork.Builder curr =
					prev == null ?
					ConnectivityNetwork.ofBuilder()
						.id(
							Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ?
							net.getNetworkHandle() : -1
						) :
					prev.toBuilder();
				LinkProperties link = batch.links.get(net);
				NetworkCapabilities caps = batch.capabilities.get(net);

				if (link != null)
					probeLink(curr, link);
				if (caps != null)
					probeCapabilities(curr, caps);
				descs.put(net, curr.build());
			}
		}

		/**
		 * Poll queued updates.
		 *
		 * @return tuple of updated active and available descriptions
		 */
		private Pair<ConnectivityNetwork, List<ConnectivityNetwork>> pollUpdate() {
			this.updateAvailable();

			Network active =
				Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ?
				super.connectivityManager.getActiveNetwork() : null;
			ConnectivityNetwork activeDesc = null;
			List<ConnectivityNetwork> restDescs = new ArrayList<>(this.descriptors.size());

			for (int i = 0; i < this.descriptors.size(); i++) {
				ConnectivityNetwork desc = this.descriptors.valueAt(i);

				if (this.descriptors.keyAt(i).equals(active))
					activeDesc = desc;
				else
					restDescs.add(desc);
			}
			return new Pair<>(activeDesc, restDescs);
		}

		/**
		 * Attempt to register network callbacks.
		 * <p>Upon success, {@link #onUpdate} will be non-{@code null}.
		 *
		 * @param ctxt context to register for
		 */
		private void tryRegisterCallbacks(Context ctxt) {
			assert this.onUpdate == null;

			OnUpdate onUpdate =
				Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
				AndroidContexts.hasPermission(ctxt, Manifest.permission.ACCESS_FINE_LOCATION) ?
				new OnUpdate(this, OnUpdate.FLAG_INCLUDE_LOCATION_INFO) :
				new OnUpdate(this);

			try {
				NetworkRequest.Builder req = new NetworkRequest.Builder();

				req.addTransportType(NetworkCapabilities.TRANSPORT_BLUETOOTH)
					.addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
					.addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
					.addTransportType(NetworkCapabilities.TRANSPORT_VPN)
					.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
					req.addTransportType(NetworkCapabilities.TRANSPORT_WIFI_AWARE);
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1)
					req.addTransportType(NetworkCapabilities.TRANSPORT_LOWPAN);
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
					req.addTransportType(NetworkCapabilities.TRANSPORT_USB);
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE || (
					Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
					SdkExtensions.getExtensionVersion(Build.VERSION_CODES.UPSIDE_DOWN_CAKE) >= 7
				)) {
					req.addTransportType(NetworkCapabilities.TRANSPORT_THREAD);
				}
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM || (
					Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
					SdkExtensions.getExtensionVersion(Build.VERSION_CODES.UPSIDE_DOWN_CAKE) >= 12
				)) {
					req.addTransportType(NetworkCapabilities.TRANSPORT_SATELLITE);
				}
				super.connectivityManager.registerNetworkCallback(req.build(), onUpdate);
			} catch (Throwable err) {
				Logger.debug(TAG, "failed to register network callback", err);
				return;
			}
			super.connectivityManager.addDefaultNetworkActiveListener(onUpdate);
			this.onUpdate = onUpdate;
		}

		@Override
		Pair<Boolean, Pair<ConnectivityNetwork, List<ConnectivityNetwork>>>
		probeNetworks(Context ctxt) {
			Pair<ConnectivityNetwork, List<ConnectivityNetwork>> update = this.pollUpdate();

			if (this.onUpdate == null)
				this.tryRegisterCallbacks(ctxt);
			return new Pair<>(this.onUpdate != null, update);
		}

		@Override
		public void close() {
			if (this.onUpdate != null) {
				super.connectivityManager.removeDefaultNetworkActiveListener(this.onUpdate);
				super.connectivityManager.unregisterNetworkCallback(this.onUpdate);
				this.onUpdate = null;
			}
		}
	}

	/**
	 * Construct a new network descriptions updater.
	 *
	 * @param ctxt context to construct updater for
	 * @return updater instance or {@code null} if unavailable
	 */
	static @Nullable ConnectivityNetworkUpdater of(Context ctxt) {
		ConnectivityManager connMan =
			AndroidContexts.systemServiceOf(
				ctxt,
				ConnectivityManager.class,
				Context.CONNECTIVITY_SERVICE
			);

		return (
			connMan == null ? null :
			Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ? new ImplLollipop(connMan) :
			new ConnectivityNetworkUpdater(connMan)
		);
	}

	/**
	 * Connectivity manager to probe networks from.
	 */
	final ConnectivityManager connectivityManager;

	@VisibleForTesting
	ConnectivityNetworkUpdater(ConnectivityManager connMan) {
		this.connectivityManager = connMan;
	}

	/**
	 * Probe network descriptions.
	 *
	 * @param ctxt context to update from
	 * @return tuple of {@code boolean} flag indicating whether updates are asynchronous or not,
	 * and, active network, possibly {@code null} if no active network is available, and available
	 * networks tuple, respectively
	 */
	@SuppressWarnings("deprecation")
	@RestrictTo(RestrictTo.Scope.SUBCLASSES)
	Pair<Boolean, Pair<ConnectivityNetwork, List<ConnectivityNetwork>>>
	probeNetworks(Context ctxt) {
		android.net.NetworkInfo info = this.connectivityManager.getActiveNetworkInfo();

		if (info == null)
			return new Pair<>(false, new Pair<>(null, Collections.emptyList()));

		SparseBooleanArray caps = new SparseBooleanArray();
		int type = ConnectivityModule.connectionTypeOfNetwork(info.getSubtype());

		if (type == 0)
			type = ConnectivityModule.connectionTypeOfConnectivity(info.getType());

		caps.put(ConnectivityNetwork.CapabilityInternet, info.isAvailable());
		caps.put(ConnectivityNetwork.CapabilityValidated, info.isConnected());
		return new Pair<>(false, new Pair<>(
			ConnectivityNetwork.ofBuilder()
				.connectionTypes(type)
				.capabilities(caps)
				.build(),
			Collections.emptyList()
		));
	}

	@Override
	public final Pair<Connectivity, Boolean> update(Connectivity dst, Context ctxt) {
		if (!AndroidContexts.hasPermission(ctxt, Manifest.permission.ACCESS_NETWORK_STATE)) {
			return new Pair<>(
				dst.withNetworks(Collections.emptyList(), 0),
				false
			);
		}

		Pair<Boolean, Pair<ConnectivityNetwork, List<ConnectivityNetwork>>> res =
			this.probeNetworks(ctxt);
		ConnectivityNetwork active = res.second.first;
		List<ConnectivityNetwork> rest = res.second.second;

		if (active != null) {
			rest = new ArrayList<>(rest);
			rest.add(active);
			dst = dst.withNetworks(rest, rest.size() - 1);
		} else {
			dst = dst.withNetworks(rest, rest.size());
		}
		return new Pair<>(dst, res.first);
	}
}
