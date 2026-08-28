// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.ads;

import androidx.annotation.GuardedBy;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.core.util.Consumer;

import org.polygamma.android.origin.adcom.placement.AdFormat;
import org.polygamma.android.origin.adcom.placement.DisplayAdFormat;
import org.polygamma.android.origin.adcom.placement.Placement;
import org.polygamma.android.origin.adcom.placement.PlaybackAdFormat;
import org.polygamma.android.origin.util.Preconditions;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Subscription of a {@linkplain PlacementRenderer renderer} to a placement.
 */
final class PlacementSubscription extends WeakReference<PlacementRenderer> {

	private final AdsModule ads;
	private final ReadWriteLock lock;
	@GuardedBy("this.lock")
	private Placement placement;
	private @Nullable Consumer<PlacementEvent> eventListener;

	/**
	 * Construct a new placement subscription.
	 *
	 * @param ads owning module
	 * @param renderer renderer subscription is for
	 * @param refs subscription reference queue
	 */
	PlacementSubscription(
		AdsModule ads,
		PlacementRendererInternal renderer,
		ReferenceQueue<PlacementRenderer> refs
	) {
		super((PlacementRenderer) renderer, refs);
		this.ads = ads;
		this.lock = new ReentrantReadWriteLock();
		this.placement = Placement.of();
	}

	/**
	 * Set placement {@linkplain PlacementEvent event} listener.
	 *
	 * @param listener event listener or {@code null}
	 */
	void setEventListener(@Nullable Consumer<PlacementEvent> listener) {
		this.eventListener = listener;
	}

	/**
	 * Test whether subscription is bound to a placement.
	 *
	 * @return {@code true} if, and only if, {@code this} is bound to a placement
	 * @see #bind(String)
	 */
	boolean isBound() {
		return !this.placement.id().isEmpty();
	}

	/**
	 * Bind subscription to placement.
	 *
	 * @param plcmtId id of placement to bind to
	 * @throws IllegalArgumentException {@code plcmtId} is invalid
	 * @throws IllegalStateException already bound to a placement
	 */
	@UiThread
	void bind(String plcmtId) {
		Preconditions.checkArgument(!plcmtId.isEmpty(), "placement id cannot be empty");
		Preconditions.checkState(!this.isBound(), "already bound to placement");

		/*
		 * no need to acquire `this.lock` here, we're not bound and we can only be invoked on
		 * the UI thread
		 */
		this.placement = this.placement.toBuilder()
			.id(plcmtId)
			.build();
	}

	@UiThread
	void setSupportedAdFormats(
		DisplayAdFormat display,
		PlaybackAdFormat audio,
		PlaybackAdFormat video
	) {
	}

	@UiThread
	void setSupportedAdFormat(AdFormat format) {
	}
}
