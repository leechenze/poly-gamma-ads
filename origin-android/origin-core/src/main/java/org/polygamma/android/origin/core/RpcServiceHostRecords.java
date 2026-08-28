// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.core;

import android.os.Build;
import android.util.SparseArray;

import androidx.annotation.GuardedBy;
import androidx.annotation.Nullable;

import org.polygamma.android.origin.util.Preconditions;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * Collection of {@linkplain RpcHostRecord host} records for a service.
 */
final class RpcServiceHostRecords {

	private final String service;
	private final String host;
	private final AtomicReferenceArray<RpcHostRecord>[] priorityRecords;
	@GuardedBy("this")
	private int counter;
	@GuardedBy("this")
	private int size;

	/**
	 * Construct new host record collection.
	 *
	 * @param svc service to construct record collection for
	 * @param host host for which to construct record collection for
	 * @param recs corresponding records
	 * @throws IllegalArgumentException {@code svc} or {@code host} is {@linkplain
	 * String#isEmpty() empty}
	 */
	@SuppressWarnings({ "RedundantSuppression", "rawtypes", "unchecked" })
	RpcServiceHostRecords(String svc, String host, Iterable<RpcHostRecord> recs) {
		Preconditions.checkArgument(!svc.isEmpty() && !host.isEmpty());

		this.service = svc;
		this.host = host;

		SparseArray<ArrayList<RpcHostRecord>> prioRecs = new SparseArray<>(1);

		for (RpcHostRecord rec : recs) {
			ArrayList<RpcHostRecord> dst = prioRecs.get(rec.priority);

			if (dst == null) {
				dst = new ArrayList<>(1);
				prioRecs.put(rec.priority, dst);
			}
			dst.add(rec);
			//noinspection NonAtomicOperationOnVolatileField
			this.size++;
		}

		this.priorityRecords = new AtomicReferenceArray[prioRecs.size()];
		this.counter =
			this.size == 0 ? 0 :
			Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ?
			ThreadLocalRandom.current().nextInt(this.size) :
			(new Random()).nextInt(this.size);

		for (int i = 0; i < prioRecs.size(); i++) {
			this.priorityRecords[i] =
				new AtomicReferenceArray<>(prioRecs.valueAt(i).toArray(new RpcHostRecord[0]));
		}
	}

	/**
	 * Service records are for.
	 *
	 * @return service
	 */
	String service() {
		return this.service;
	}

	/**
	 * Host records were queried for.
	 *
	 * @return host
	 */
	String host() {
		return this.host;
	}

	/**
	 * Number of record entries available.
	 *
	 * @return record count
	 */
	int size() {
		return this.size;
	}

	/**
	 * Retrieve non-{@linkplain RpcHostRecord#isExpired() expired} records.
	 *
	 * @return non-expired records
	 */
	List<RpcHostRecord> toRecords() {
		ArrayList<RpcHostRecord> rv = new ArrayList<>(this.size);

		for (AtomicReferenceArray<RpcHostRecord> recs : this.priorityRecords) {
			if (recs != null) {
				for (int i = 0; i < recs.length(); i++) {
					RpcHostRecord rec = recs.get(i);

					if (rec != null && !rec.isExpired())
						rv.add(rec);
				}
			}
		}
		return rv;
	}

	/**
	 * Retrieve next record of host to execute {@linkplain #service() service} procedure on.
	 *
	 * @return next record or {@code null} if unavailable
	 */
	@Nullable RpcHostRecord next() {
		RpcHostRecord rv = null;
		int count = this.counter;

		search:
		for (int i = 0; i < this.priorityRecords.length; i++) {
			AtomicReferenceArray<RpcHostRecord> recs = this.priorityRecords[i];
			int num = 0;

			if (recs == null)
				continue;
			for (int j = 0; j < recs.length(); j++) {
				int idx = (count + j) % recs.length();
				RpcHostRecord rec = recs.get(idx);

				if (rec == null)
					continue;

				long lastFail = rec.lastFailureTimestampSeconds();

				num++;
				// if we have another record already with a failure earlier than us, keep it
				if (
					rv != null &&
					Long.compareUnsigned(rv.lastFailureTimestampSeconds(), lastFail) <= 0
				) {
					continue;
				}

				// If record is expired, remove it, but still return it in case it is still valid
				if (rec.isExpired() && recs.compareAndSet(idx, rec, null)) {
					synchronized (this) {
						this.size--;
						if (this.counter == count)
							this.counter++;
					}
				} else if (lastFail == 0L && rv != null) {
					// we're a better fit, move the counter towards us
					synchronized (this) {
						if (this.counter == count)
							this.counter++;
					}
				}
				rv = rec;
				if (lastFail == 0L)
					break search;
			}
			if (num == 0)
				this.priorityRecords[i] = null;
		}
		return rv;
	}
}
