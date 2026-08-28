// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.ads;

import android.os.Build;
import android.util.ArrayMap;
import android.util.Pair;

import androidx.annotation.GuardedBy;

import com.google.common.util.concurrent.ListenableFuture;

import org.polygamma.android.origin.adcom.placement.Placement;
import org.polygamma.android.origin.core.RpcModule;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Ads service remote procedure call (RPC) client.
 */
final class RpcClient {

	private final RpcModule rpc;
	private final Lock requestAdsLock;

	/*
	 * Mapping of placement ids to:
	 *
	 * 1. `Pair<Placement, ListenableFuture<AdInstance>>`
	 *
	 *     Tuple of placement description and ad request future, respectively. Set when ads are
	 *     requested for placement, but a call has not yet been made.
	 *
	 * 2. `ListenableFuture<AdInstance>`
	 *
	 *     Ad request future. Set when ads are requested for placement and a call has been made.
	 */
	@GuardedBy("this.requestAdsLock")
	private ArrayMap<String, Pair<Placement, ListenableFuture<AdInstance>>> pendingRequestAds;

	/**
	 * Construct a new ads RPC client.
	 *
	 * @param rpc underlying RPC module
	 */
	RpcClient(RpcModule rpc) {
		this.rpc = rpc;
		this.requestAdsLock = new ReentrantLock();
		this.pendingRequestAds = new ArrayMap<>();
	}

	ListenableFuture<AdInstance> requestAds(Placement plcmt) {
		this.requestAdsLock.lock();
		try {
			/*
			 * If we have a request pending for `plcmt.id()` already, then replace it with `plcmt`;
			 * otherwise, we'll create a new one.
			 */
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				int idx = this.pendingRequestAds.indexOfKey(plcmt.id());

				if (idx >= 0) {
					Pair<Placement, ListenableFuture<AdInstance>> curr =
						this.pendingRequestAds.valueAt(idx);

					this.pendingRequestAds.setValueAt(idx, new Pair<>(
						plcmt,
						curr.second
					));
					return curr.second;
				}
			} else {
				Pair<Placement, ListenableFuture<AdInstance>> curr =
					this.pendingRequestAds.get(plcmt.id());

				if (curr != null) {
					this.pendingRequestAds.put(plcmt.id(), new Pair<>(
						plcmt,
						curr.second
					));
					return curr.second;
				}
			}
			throw new UnsupportedOperationException();
		} finally {
			this.requestAdsLock.unlock();
		}
	}
}
