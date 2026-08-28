// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.ads;

import android.content.Context;
import android.util.Pair;

import androidx.annotation.NonNull;

import org.polygamma.android.origin.core.Origin;
import org.polygamma.android.origin.core.OriginModule;
import org.polygamma.android.origin.core.OriginModuleEventBus;
import org.polygamma.android.origin.core.OriginModuleEventCallback;
import org.polygamma.android.origin.core.OriginModuleEventName;
import org.polygamma.android.origin.util.Preconditions;

/**
 * Module providing advertising media.
 *
 * @since 1.2
 */
public final class AdsModule extends OriginModule {

	private static final String TAG = AdsModule.class.getSimpleName();

	/**
	 * Ads module name.
	 *
	 * @since 1.2
	 */
	public static final String NAME = "origin.ads";

	/**
	 * Name of {@linkplain Origin#registerModuleEventCallback(OriginModuleEventCallback, Pair)
	 * event} fired for each placement {@linkplain PlacementEvent event}.
	 *
	 * @since 1.2
	 */
	public static final @OriginModuleEventName String PLACEMENT_EVENT = "placement-event";

	/**
	 * Construct new module provider.
	 *
	 * @return new provider instance
	 * @since 1.2
	 */
	public static OriginModule.Provider<AdsModule> ofProvider() {
		return new Provider<AdsModule>(AdsModule.class) {
			@Override
			protected AdsModule load(Origin sdk, Context ctxt) {
				return new AdsModule(sdk, ctxt);
			}
		};
	}

	/**
	 * Current ads module.
	 *
	 * @return ads module
	 * @throws IllegalStateException SDK or module not loaded
	 * @since 1.2
	 */
	public static @NonNull AdsModule current() {
		AdsModule rv = Origin.current().findModule(AdsModule.class);

		Preconditions.checkState(rv != null, "ads module not loaded");
		//noinspection DataFlowIssue
		return rv;
	}

	private final AdDatabase database;
	private final OriginModuleEventBus placementEvents;

	private AdsModule(Origin sdk, Context ctxt) {
		super(NAME, sdk);
		this.placementEvents = super.registerEvent(PLACEMENT_EVENT, false);
		this.database = AdDatabase.open(ctxt, super.resolvePersistentId() + "-addb");
	}
}
