// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.ads;

import androidx.annotation.AnyThread;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.core.util.Consumer;

import org.polygamma.android.origin.core.OriginModuleEventCallback;

/**
 * Ad media renderer of a placement.
 * <p>Implementations of this implement ad media rendering logic for a placement. Renderers are
 * always {@linkplain #bindToPlacement(String) bound} to a <i>single</i> placement. There may be
 * only a single renderer for a placement at any given point in time. Thus if more than one
 * renderer exists for a placement at any given point in time, only a single renderer will receive
 * ads for the respective placement.
 * <p>Events generated for a placement, including ad media events, can be listened for on either
 * a per-renderer basis or through the active ads module, using {@link
 * #setPlacementEventListener(Consumer)} or {@linkplain
 * AdsModule#registerEventCallback(OriginModuleEventCallback, String) registering} a callback
 * to the {@linkplain AdsModule#PLACEMENT_EVENT module placement event}, respectively.
 * <h2>Requesting Ads</h2>
 * <p>Renderer implementations will usually request ads automatically, based on the respective
 * implementation's viewability logic <i>and</i> when the renderer is bound to a placement, see
 * implementation specific documentation for information on implementation specific viewability
 * logic. All renderers, however, support manually loading ads using {@link #beginRequestingAds()}.
 * <p>While ads may be requested for the renderer, ads will not, however, be rendered until
 * the renderer {@linkplain #canRenderAds() can} render ad media. For a renderer to be capable of
 * rendering ad media, the viewability criteria of the renderer must be met. See renderer
 * implementation documentation for more information.
 *
 * @since 1.2
 * @see DisplayPlacementView
 */
@UiThread
public interface PlacementRenderer {
	/**
	 * Id of placement ad media is rendered for.
	 *
	 * @return placement id or {@code null} if a placement has not been {@linkplain
	 * #bindToPlacement(String) bound}
	 * @since 1.2
	 */
	@AnyThread
	@Nullable String placementId();

	/**
	 * Set listener to notify of any events generated for underlying placement.
	 * <p>The listener specified, if any, is invoked on either a worker or UI thread, depending on
	 * event type, for each event generated for the underlying placement. See the table below for
	 * thread type an event is dispatched on.
	 * <table>
	 *     <caption>Event Thread Type</caption>
	 *     <thead>
	 *         <tr>
	 *             <th>Event</th>
	 *             <th>Worker Thread</th>
	 *             <th>UI Thread</th>
	 *         </tr>
	 *     </thead>
	 *     <tbody>
	 *         <tr>
	 *             <td>{@linkplain PlacementEvent#EVENT_ERROR Error}</td>
	 *             <td>&#x2611;</td>
	 *             <td>&#x2611;</td>
	 *         </tr>
	 *         <tr>
	 *             <td>{@linkplain PlacementEvent#EVENT_AD_AVAILABLE Ad Available}</td>
	 *             <td>&#x2611;</td>
	 *             <td>&#9744;</td>
	 *         </tr>
	 *         <tr>
	 *             <td>{@linkplain PlacementEvent#EVENT_AD_SELECTED Ad Selected}</td>
	 *             <td>&#9744;</td>
	 *             <td>&#x2611;</td>
	 *         </tr>
	 *         <tr>
	 *             <td>{@linkplain PlacementEvent#EVENT_AD_RENDERED Ad Rendered}</td>
	 *             <td>&#9744;</td>
	 *             <td>&#x2611;</td>
	 *         </tr>
	 *         <tr>
	 *             <td>{@linkplain PlacementEvent#EVENT_AD_IMPRESSION Ad Impression}</td>
	 *             <td>&#x2611;</td>
	 *             <td>&#9744;</td>
	 *         </tr>
	 *         <tr>
	 *             <td>{@linkplain PlacementEvent#EVENT_AD_ACTIVATED Ad Activated}</td>
	 *             <td>&#9744;</td>
	 *             <td>&#x2611;</td>
	 *         </tr>
	 *         <tr>
	 *             <td>{@linkplain PlacementEvent#EVENT_AD_REMOVED Ad Removed}</td>
	 *             <td>&#9744;</td>
	 *             <td>&#x2611;</td>
	 *         </tr>
	 *         <tr>
	 *             <td>{@linkplain PlacementEvent#EVENT_AD_RESIZED Ad Resized}</td>
	 *             <td>&#9744;</td>
	 *             <td>&#x2611;</td>
	 *         </tr>
	 *         <tr>
	 *             <td>{@linkplain PlacementEvent#EVENT_USER_REWARD User Reward}</td>
	 *             <td>&#9744;</td>
	 *             <td>&#x2611;</td>
	 *         </tr>
	 *     </tbody>
	 * </table>
	 *
	 * @param listener listener or {@code null}
	 * @since 1.2
	 */
	void setPlacementEventListener(@Nullable Consumer<PlacementEvent> listener);

	/**
	 * Bind renderer to a placement.
	 * <p>Upon return, this renderer will be bound to the placement specified by {@code id}, and
	 * {@link #placementId()} will return {@code id}. If this renderer has already been bound to
	 * a placement, this fails unconditionally.
	 * <p>This may be invoked <i>only</i> from the UI thread.
	 *
	 * @param id id of placement to bind to
	 * @throws IllegalArgumentException {@code id} is invalid
	 * @throws IllegalStateException already bound to a placement
	 * @since 1.2
	 * @see #placementId()
	 */
	void bindToPlacement(String id);

	/**
	 * Begin requesting ads for underlying placement.
	 * <p>Placement renderers will <i>usually</i> begin requesting ads automatically, based on
	 * renderer type. This may be invoked to explicitly begin requesting ads for the underlying
	 * placement. If ads are already being {@linkplain #isRequestingAds() requested}, this simply
	 * returns.
	 * <p>Note that while this begins the ad request process, as ads become available for the
	 * placement, they still will not be rendered until this renderer is attached to its respective
	 * user-facing peripheral, either audio or display, depending on placement type.
	 *
	 * @throws IllegalStateException renderer is not {@linkplain #bindToPlacement(String) bound}
	 * to a placement
	 * @since 1.2
	 * @see #isRequestingAds()
	 */
	void beginRequestingAds();

	/**
	 * Test whether ads are being requested for underlying placement.
	 *
	 * @return {@code true} if, and only if, ads are being requested
	 * @since 1.2
	 * @see #beginRequestingAds()
	 */
	@AnyThread
	boolean isRequestingAds();

	/**
	 * Test whether renderer is currently capable of rendering ads.
	 * <p>A renderer is capable of rendering ad media when it is attached to its respective
	 * user-facing peripheral, such as an audio or display peripheral.
	 *
	 * @return {@code true} if, and only if, renderer is capable of rendering ads
	 * @since 1.2
	 */
	boolean canRenderAds();

	/**
	 * Current rendered ad instance, if any.
	 *
	 * @return ad instance or {@code null} if no ad instance is being rendered
	 * @since 1.2
	 */
	@Nullable AdInstance currentAdInstance();
}
