// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.core;

import static android.Manifest.permission.READ_BASIC_PHONE_STATE;
import static android.Manifest.permission.READ_PHONE_STATE;
import static android.content.Context.TELEPHONY_SUBSCRIPTION_SERVICE;
import static android.content.pm.PackageManager.FEATURE_TELEPHONY_SUBSCRIPTION;
import static android.telephony.TelephonyManager.NETWORK_TYPE_UNKNOWN;
import static android.telephony.TelephonyManager.SIM_STATE_READY;

import static org.polygamma.android.origin.util.AndroidContexts.hasAnyPermission;
import static org.polygamma.android.origin.util.AndroidContexts.hasPermission;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;
import android.util.Pair;

import androidx.annotation.MainThread;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;

import org.polygamma.android.origin.adcom.enums.AdComEnums;
import org.polygamma.android.origin.util.AndroidContexts;
import org.polygamma.android.origin.util.Futures;
import org.polygamma.android.origin.util.Logger;
import org.polygamma.android.origin.util.Preconditions;
import org.polygamma.android.origin.util.Strings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Subscription {@linkplain Connectivity#subscription(int) descriptions} updater.
 */
@SuppressWarnings("deprecation")
class ConnectivitySubscriptionUpdater extends ConnectivityUpdater {

	private static final String TAG = ConnectivitySubscriptionUpdater.class.getSimpleName();

	/**
	 * Build subscription descriptor from telephony subscription.
	 *
	 * @param tele telephony manager to build from
	 * @param ctxt context to build for
	 * @return resulting descriptor
	 */
	@SuppressLint("MissingPermission")
	private static ConnectivitySubscription descriptionOf(TelephonyManager tele, Context ctxt) {
		ConnectivitySubscription.Builder desc = ConnectivitySubscription.ofBuilder();
		int netType = NETWORK_TYPE_UNKNOWN;

		try {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
				desc.id(tele.getSubscriptionId());
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
				desc.carrierId(tele.getSimCarrierId())
					.carrierName(
						Preconditions.checkNotNullElse(tele.getSimCarrierIdName(), "")
							.toString()
					);
			}
			desc.operatorCountryCode(tele.getSimCountryIso());
			if (tele.getSimState() == SIM_STATE_READY) {
				desc.operatorCode(Strings.nullToEmpty(tele.getSimOperator()))
					.operatorName(Strings.nullToEmpty(tele.getSimOperatorName()));
			}
		} catch (Exception err) {
			Logger.debug(TAG, "failed to query subscription from telephony", err);
		}

		try {
			if (hasAnyPermission(ctxt, READ_PHONE_STATE, READ_BASIC_PHONE_STATE)) {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
					netType = tele.getDataNetworkType();
				if (netType == NETWORK_TYPE_UNKNOWN)
					netType = tele.getNetworkType();
			}

			desc.networkOperatorCode(Strings.nullToEmpty(tele.getNetworkOperator()))
				.networkOperatorName(Strings.nullToEmpty(tele.getNetworkOperatorName()))
				.networkOperatorCountryCode(Strings.nullToEmpty(tele.getNetworkCountryIso()));
		} catch (Exception err) {
			Logger.debug(TAG, "failed to query radio from telephony", err);
		}

		if (netType != NETWORK_TYPE_UNKNOWN) {
			int connType = ConnectivityModule.connectionTypeOfNetwork(netType);

			desc.connectionType(connType == 0 ? AdComEnums.ConnectionCell : connType);
		}
		return desc.build();
	}

	/**
	 * Build subscription descriptor from subscription info.
	 *
	 * @param info subscription info to build from
	 * @return resulting descriptor
	 */
	@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
	private static ConnectivitySubscription descriptionOf(SubscriptionInfo info) {
		ConnectivitySubscription.Builder desc = ConnectivitySubscription.ofBuilder();

		desc.id(info.getSubscriptionId())
			.connectionType(AdComEnums.ConnectionCell)
			.operatorCountryCode(Strings.nullToEmpty(info.getCountryIso()));
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			String mcc = Strings.nullToEmpty(info.getMccString());
			String mnc = Strings.nullToEmpty(info.getMncString());

			if (!mcc.isEmpty())
				desc.operatorMcc(Integer.parseInt(mcc, 10));
			if (!mnc.isEmpty())
				desc.operatorMnc(Integer.parseInt(mnc, 10));
			desc.carrierId(info.getCarrierId())
				.carrierName(Preconditions.checkNotNullElse(info.getCarrierName(), "").toString());
		} else {
			int mcc = Math.max(info.getMcc(), 0);
			int mnc = Math.max(info.getMnc(), 0);

			if (mcc > 0) {
				desc.operatorMcc(mcc)
					.operatorMnc(mnc);
			}
		}
		return desc.build();
	}

	/**
	 * Build subscription descriptor from telephony manager or subscription info based on platform
	 * capabilities.
	 *
	 * @param tele telephony manager to build from
	 * @param info subscription info to build from
	 * @param ctxt context to build for
	 * @param active {@code true} if, and only if, subscription is active subscription
	 * @return resulting descriptor
	 */
	@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
	private static ConnectivitySubscription
	descriptionOf(TelephonyManager tele, SubscriptionInfo info, Context ctxt, boolean active) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			try {
				return descriptionOf(tele.createForSubscriptionId(info.getSubscriptionId()), ctxt);
			} catch (Throwable err) {
				Logger.debug(TAG, "failed to probe scription from bound telephony", err);
			}
		}
		return active ? descriptionOf(tele, ctxt) : descriptionOf(info);
	}

	/**
	 * Active subscription change listener, for Android Q and above.
	 */
	@RequiresApi(api = Build.VERSION_CODES.Q)
	@SuppressWarnings({ "RedundantSuppression", "deprecation" })
	private static final class OnActiveSubscriptionChanged
	extends android.telephony.PhoneStateListener {

		private final ImplLollipopMr1 updater;

		OnActiveSubscriptionChanged(ImplLollipopMr1 updater) {
			super(updater.backgroundExecutor);
			this.updater = updater;
		}

		@Override
		public void onActiveDataSubscriptionIdChanged(int subId) {
			this.updater.onUpdateAvailable();
		}
	}

	/**
	 * Active subscription change listener, for Android S and above.
	 */
	@RequiresApi(api = Build.VERSION_CODES.S)
	private static final class OnActiveSubscriptionChangedS
	extends TelephonyCallback
	implements TelephonyCallback.ActiveDataSubscriptionIdListener {

		private final ImplLollipopMr1 updater;

		OnActiveSubscriptionChangedS(ImplLollipopMr1 updater) {
			this.updater = updater;
		}

		@Override
		public void onActiveDataSubscriptionIdChanged(int subId) {
			this.updater.onUpdateAvailable();
		}
	}

	/**
	 * Subscription change listener.
	 */
	@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
	private static final class OnSubscriptionChanged
	extends SubscriptionManager.OnSubscriptionsChangedListener {

		private final ImplLollipopMr1 updater;

		OnSubscriptionChanged(ImplLollipopMr1 updater) {
			this.updater = updater;
		}

		@Override
		public void onSubscriptionsChanged() {
			this.updater.onUpdateAvailable();
		}
	}

	/**
	 * Android Lollipop MR1 and above specialization.
	 */
	@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
	@VisibleForTesting
	static class ImplLollipopMr1 extends ConnectivitySubscriptionUpdater {

		private final ExecutorService backgroundExecutor;
		private final ExecutorService foregroundExecutor;
		@VisibleForTesting
		final @Nullable SubscriptionManager subscriptions;
		@VisibleForTesting
		@Nullable OnSubscriptionChanged onSubscriptionChanged;
		@VisibleForTesting
		@Nullable Object onActiveSubscriptionChanged;

		private ImplLollipopMr1(
			Context ctxt,
			TelephonyManager teleMan,
			ExecutorService bgExec,
			ExecutorService fgExec
		) {
			super(teleMan);
			this.backgroundExecutor = bgExec;
			this.foregroundExecutor = fgExec;
			this.subscriptions =
				Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
				!AndroidContexts.hasSystemFeature(ctxt, FEATURE_TELEPHONY_SUBSCRIPTION) ? null :
				AndroidContexts.systemServiceOf(
					ctxt,
					SubscriptionManager.class,
					TELEPHONY_SUBSCRIPTION_SERVICE
				);
		}

		private void installSubscriptionChangeListener() {
			assert this.subscriptions != null && this.onSubscriptionChanged == null;

			OnSubscriptionChanged onChanged =
				Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM ?
				new OnSubscriptionChanged(this) :
				Futures.await(this.foregroundExecutor.submit(
					() -> new OnSubscriptionChanged(this)
				));

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
				this.subscriptions.addOnSubscriptionsChangedListener(
					this.backgroundExecutor,
					onChanged
				);
			} else {
				this.subscriptions.addOnSubscriptionsChangedListener(onChanged);
			}
			this.onSubscriptionChanged = onChanged;
		}

		private void installActiveSubscriptionListener() {
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
				return;
			assert this.onActiveSubscriptionChanged == null;

			try {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
					OnActiveSubscriptionChangedS listener =
						new OnActiveSubscriptionChangedS(this);

					super.telephonyManager.registerTelephonyCallback(
						this.backgroundExecutor,
						listener
					);
					this.onActiveSubscriptionChanged = listener;
					return;
				}

				OnActiveSubscriptionChanged listener =
					new OnActiveSubscriptionChanged(this);

				super.telephonyManager.listen(
					listener,
					OnActiveSubscriptionChanged.LISTEN_ACTIVE_DATA_SUBSCRIPTION_ID_CHANGE
				);
				this.onActiveSubscriptionChanged = listener;
			} catch (Exception err) {
				Logger.debug(TAG, "failed to register active subscription listener", err);
			}
		}

		@SuppressLint("MissingPermission")
		private List<SubscriptionInfo> probeAvailable(Context ctxt) {
			if (this.subscriptions == null || (
				!hasPermission(ctxt, READ_PHONE_STATE) &&
				!super.telephonyManager.hasCarrierPrivileges()
			)) {
				return Collections.emptyList();
			} else if (this.onSubscriptionChanged == null) {
				this.installSubscriptionChangeListener();
			}

			return (
				Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ?
				this.subscriptions.getAllSubscriptionInfoList() :
				this.subscriptions.getActiveSubscriptionInfoList()
			);
		}

		@SuppressLint("MissingPermission")
		@Override
		public Pair<Connectivity, Boolean> update(Connectivity dst, Context ctxt) {
			if (this.onActiveSubscriptionChanged == null)
				this.installActiveSubscriptionListener();

			List<SubscriptionInfo> subs = this.probeAvailable(ctxt);

			if (subs.isEmpty()) {
				return new Pair<>(
					super.update(dst, ctxt).first,
					this.onActiveSubscriptionChanged != null
				);
			}

			ArrayList<ConnectivitySubscription> descs = new ArrayList<>(subs.size());
			int activeId =
				Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ?
				SubscriptionManager.getActiveDataSubscriptionId() : -1;
			int activeIdx = -1;

			if (activeId == -1 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
				activeId = SubscriptionManager.getDefaultDataSubscriptionId();

			for (SubscriptionInfo sub : subs) {
				boolean active = activeId != -1 && activeId == sub.getSubscriptionId();

				if (active)
					activeIdx = descs.size();
				descs.add(descriptionOf(super.telephonyManager, sub, ctxt, active));
			}

			if (activeIdx == -1 && activeId != -1 && this.subscriptions != null) {
				SubscriptionInfo active = null;
				try {
					active = this.subscriptions.getActiveSubscriptionInfo(activeId);
				} catch (Throwable err) {
					Logger.debug(TAG, "failed to query active subscription info", err);
				}
				if (active != null) {
					activeIdx = descs.size();
					descs.add(descriptionOf(super.telephonyManager, active, ctxt, true));
				}
			}
			return new Pair<>(
				dst.withSubscriptions(descs, activeIdx == -1 ? descs.size() : activeIdx),
				this.onActiveSubscriptionChanged != null && this.onSubscriptionChanged != null
			);
		}

		@Override
		public void close() {
			if (this.onActiveSubscriptionChanged != null) {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
					super.telephonyManager.unregisterTelephonyCallback(
						(OnActiveSubscriptionChangedS) this.onActiveSubscriptionChanged
					);
				} else {
					super.telephonyManager.listen(
						(OnActiveSubscriptionChanged) this.onActiveSubscriptionChanged,
						OnActiveSubscriptionChanged.LISTEN_NONE
					);
				}
				this.onActiveSubscriptionChanged = null;
			}
			if (this.onSubscriptionChanged != null) {
				assert this.subscriptions != null;
				this.subscriptions.removeOnSubscriptionsChangedListener(this.onSubscriptionChanged);
				this.onSubscriptionChanged = null;
			}
		}
	}

	/**
	 * Construct updater for a module.
	 *
	 * @param ctxt context to construct updater for
	 * @param bgExec executor to use for scheduling background tasks
	 * @param fgExec executor to use for scheduling foreground tasks
	 * @return updater instance or {@code null} if unavailable
	 */
	static @Nullable ConnectivitySubscriptionUpdater
	of(Context ctxt, ExecutorService bgExec, ExecutorService fgExec) {
		TelephonyManager teleMan =
			AndroidContexts.systemServiceOf(
				ctxt,
				TelephonyManager.class,
				Context.TELEPHONY_SERVICE
			);

		return (
			teleMan == null ? null :
			Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1 ?
			new ImplLollipopMr1(ctxt, teleMan, bgExec, fgExec) :
			new ConnectivitySubscriptionUpdater(teleMan)
		);
	}

	/**
	 * Telephony manager to probe subscriptions from.
	 */
	final TelephonyManager telephonyManager;

	private ConnectivitySubscriptionUpdater(TelephonyManager teleMan) {
		this.telephonyManager = teleMan;
	}

	@Override
	public Pair<Connectivity, Boolean> update(Connectivity dst, Context ctxt) {
		return new Pair<>(
			dst.withSubscriptions(
				Collections.singletonList(descriptionOf(this.telephonyManager, ctxt)),
				0
			),
			false
		);
	}
}
