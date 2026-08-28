// SPDX-License-Identifier: MIT OR Apache-2.0

/**
 * Origin Ads SDK classes.
 * <p>This package defines {@link org.polygamma.android.origin.ads.AdsModule}, which is used to
 * access the Origin ads service. This service allocates ad media to render within placements,
 * tracks ad events, and manage lifecycle of available ads.
 * <p>The {@link org.polygamma.android.origin.ads.PlacementRenderer} class can be used to both
 * request <i>and</i> render ad media. Scheduling, lifecycle, and loading of ad media is handled
 * automatically by renderers, using the active ads module. See {@link
 * org.polygamma.android.origin.ads.PlacementRenderer} for more information.
 *
 * @since 1.2
 */
@NonNull
package org.polygamma.android.origin.ads;

import androidx.annotation.NonNull;
