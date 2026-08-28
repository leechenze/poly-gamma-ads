// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.ads;

import android.annotation.SuppressLint;
import android.net.Uri;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.core.util.Consumer;

import org.polygamma.android.origin.util.Preconditions;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Locale;

/**
 * Description of an event associated with a placement.
 * <p>This describes an event associated with a placement. Events signal the state of a placement,
 * where the associated placement id and {@linkplain PlacementRenderer renderer} can be accessed
 * using {@link #placementId()} and {@link #renderer()}, respectively. Certain events may be
 * associated with an ad {@linkplain AdInstance instance}, allocated for the respective placement,
 * which may be retrieved using {@link #adInstance()}.
 * <p>Events for a placement are always dispatched through the placement's renderer. A listener for
 * events can be set for a renderer using {@link
 * PlacementRenderer#setPlacementEventListener(Consumer)}. Events are dispatched on either a worker
 * or UI thread, see {@link PlacementRenderer#setPlacementEventListener(Consumer)} for more
 * information. Note, however, events dispatched through the ads {@linkplain AdsModule} are
 * <i>always</i> dispatched on a worker thread.
 * <h2>Event Types</h2>
 * <p>Different types of events have different data associated with them. See the table below
 * describing the various event types.
 * <table>
 *     <caption>Event Types</caption>
 *     <thead>
 *         <tr>
 *             <th>Type</th>
 *             <th>Data</th>
 *             <th>Description</th>
 *         </tr>
 *     </thead>
 *     <tbody>
 *         <tr>
 *             <td>{@linkplain #EVENT_ERROR Error}</td>
 *             <td>
 *                 <ul>
 *                     <li>{@link #errorCause()}</li>
 *                 </ul>
 *             </td>
 *             <td>
 *                 Error encountered while loading or rendering an ad, or servicing a placement.
 *                 If an error is associated with an ad instance, {@link #adInstance()} is
 *                 guaranteed to return non-{@code null}.
 *             </td>
 *         </tr>
 *         <tr>
 *             <td>{@linkplain #EVENT_AD_AVAILABLE Ad Available}</td>
 *             <td>
 *                 <ul>
 *                     <li>{@link #adInstance()}</li>
 *                 </ul>
 *             </td>
 *             <td>
 *                 Ad is available for placement. This may be dispatched multiple times, even while
 *                 a placement is in an erroneous state, is rendering an ad, or cannot render an ad.
 *             </td>
 *         </tr>
 *         <tr>
 *             <td>{@linkplain #EVENT_AD_SELECTED Ad Selected}</td>
 *             <td>
 *                 <ul>
 *                     <li>{@link #adInstance()}</li>
 *                 </ul>
 *             </td>
 *             <td>
 *                 Ad has been selected to be rendered within the placement. This is dispatched
 *                 <i>immediately</i> before, on a UI thread, the ad is rendered. When dispatched,
 *                 the placement is guaranteed to {@linkplain PlacementRenderer#currentAdInstance()
 *                 currently} not be rendering an ad. Placement re-configuration may be performed
 *                 when this event is dispatched, apriori rendering.
 *             </td>
 *         </tr>
 *         <tr>
 *             <td>{@linkplain #EVENT_AD_RENDERED Ad Rendered}</td>
 *             <td>
 *                 <ul>
 *                     <li>{@link #adInstance()}</li>
 *                 </ul>
 *             </td>
 *             <td>
 *                 Previously selected ad is rendered within the placement. This is dispatched
 *                 <i>immediately</i> after, on a UI thread, the ad is rendered.
 *             </td>
 *         </tr>
 *         <tr>
 *             <td>{@linkplain #EVENT_AD_IMPRESSION Ad Impression}</td>
 *             <td>
 *                 <ul>
 *                     <li>{@link #adImpressionPlaybackPercent()}</li>
 *                     <li>{@link #adImpressionPossiblyBillable()}</li>
 *                     <li>{@link #adImpressionViewablePercent()}</li>
 *                     <li>{@link #adInstance()}</li>
 *                 </ul>
 *             </td>
 *             <td>
 *
 *             </td>
 *         </tr>
 *     </tbody>
 * </table>
 *
 * @since 1.2
 * @see PlacementRenderer#setPlacementEventListener(Consumer)
 */
public final class PlacementEvent {

	/**
	 * Error encountered while loading an ad, rendering an ad, or servicing a placement.
	 * <p>This event is dispatched whenever a placement or its {@linkplain #renderer() renderer}
	 * encounters an error. The error cause may be retrieved using {@link #errorCause()}. If an
	 * error is associated with an ad instance, then {@link #adInstance()} will return a non-{@code
	 * null} error.
	 *
	 * @since 1.2
	 * @see #type()
	 */
	public static final @Type int EVENT_ERROR			= 0;

	/**
	 * Ad is available for placement.
	 * <p>This event is dispatched whenever an ad becomes available for a placement, even if the
	 * placement is currently {@linkplain PlacementRenderer#currentAdInstance() rendering} an ad
	 * or if it cannot {@linkplain PlacementRenderer#canRenderAds() render} an ad. The instance
	 * describing the available ad can be retrieved through {@link #adInstance()}.
	 *
	 * @since 1.2
	 * @see #type()
	 * @see #adInstance()
	 */
	public static final @Type int EVENT_AD_AVAILABLE	= 1;

	/**
	 * Ad has been selected for rendering within a placement.
	 * <p>This event is dispatched whenever an <i>{@linkplain #EVENT_AD_AVAILABLE available}</i>
	 * ad is selected for rendering, immediately <i>before</i> the ad is {@linkplain
	 * #EVENT_AD_RENDERED rendered}.
	 *
	 * @since 1.2
	 * @see #type()
	 * @see #adInstance()
	 */
	public static final @Type int EVENT_AD_SELECTED		= 2;

	/**
	 * Selected ad has been rendered within a placement.
	 * <p>This event is dispatched whenever a <i>{@linkplain #EVENT_AD_SELECTED selected}</i> ad
	 * is rendered.
	 *
	 * @since 1.2
	 * @see #type()
	 * @see #adInstance()
	 */
	public static final @Type int EVENT_AD_RENDERED		= 3;

	/**
	 * Rendered ad has received an impression.
	 * <p>This event is dispatched whenever a <i>{@linkplain #EVENT_AD_RENDERED rendered}</i> ad
	 * receives a <i>possibly</i> billable impression. An ad impression is considered possibly
	 * billable based on ad media type, {@linkplain #adImpressionViewablePercent() viewability},
	 * and {@linkplain #adImpressionPlaybackPercent() playback duration}. To determine if an ad
	 * impression is <i>possibly</i> billable, use {@link #adImpressionPossiblyBillable()}.
	 * <p>This event may be dispatched more than once, until a final possibly billable impression
	 * is resolved.
	 *
	 * @since 1.2
	 * @see #type()
	 * @see #adInstance()
	 */
	public static final @Type int EVENT_AD_IMPRESSION	= 4;

	/**
	 * Rendered ad has been activated (i.e. clicked).
	 * <p>This event is dispatched whenever a <i>{@linkplain #EVENT_AD_RENDERED rendered}</i> ad
	 * is activated, i.e. clicked, <i>and</i> the ad has a navigation destination. The navigation
	 * destination can be retrieved using {@link #adActivatedTarget()}.
	 *
	 * @since 1.2
	 * @see #type()
	 * @see #adInstance()
	 */
	public static final @Type int EVENT_AD_ACTIVATED	= 5;

	/**
	 * Rendered ad has been removed.
	 * <p>This event is dispatched whenever a <i>{@linkplain #EVENT_AD_RENDERED rendered}</i> ad
	 * is removed from the placement.
	 *
	 * @since 1.2
	 * @see #type()
	 * @see #adInstance()
	 */
	public static final @Type int EVENT_AD_REMOVED		= 6;

	/**
	 * Rendered ad has resized.
	 * <p>This event is dispatched whenever a <i>{@linkplain #EVENT_AD_RENDERED rendered}</i> ad
	 * has resized the placement.
	 *
	 * @since 1.2
	 * @see #type()
	 * @see #adInstance()
	 */
	public static final @Type int EVENT_AD_RESIZED		= 7;

	/**
	 * User has completed requirements to receive reward.
	 * <p>This event is dispatched whenever a currently or previously <i>{@linkplain
	 * #EVENT_AD_RENDERED rendered}</i> ad is viewed for a rewarded placement.
	 *
	 * @since 1.2
	 * @see #type()
	 * @see #adInstance()
	 */
	public static final @Type int EVENT_USER_REWARD		= 8;

	/**
	 * Event type enumeration value marker.
	 *
	 * @since 1.2
	 * @see #type()
	 */
	@Documented
	@Retention(RetentionPolicy.SOURCE)
	@Target(ElementType.TYPE_USE)
	@IntDef({
		EVENT_AD_ACTIVATED,
		EVENT_AD_AVAILABLE,
		EVENT_AD_IMPRESSION,
		EVENT_AD_REMOVED,
		EVENT_AD_RENDERED,
		EVENT_AD_RESIZED,
		EVENT_AD_SELECTED,
		EVENT_ERROR,
		EVENT_USER_REWARD
	})
	public @interface Type {
	}

	/*
	 * Layout of `typeAndBits`:
	 *
	 * +------------+------------+
	 * | Event Bits | Event Type |
	 * +------------+------------+
	 * ^            ^            ^
	 * 31           4            0
	 */
	private static final int BITS_PER_TYPE		= 4;
	private static final int TYPE_MASK			= 0xf;

	private static final int BITS_PER_PERCENT	= 7;
	private static final int PERCENT_MASK		= 0x7f;

	/*
	 * Layout of `bits()` for ad impression events:
	 *
	 * +----------+------------------+------------------+
	 * | Billable | Playback Percent | Viewable Percent |
	 * +----------+------------------+------------------+
	 * ^          ^                  ^                  ^
	 * 15         14                 7                  0
	 */
	private static final int AD_IMPRESSION_VIEWABLE_PERCENT_SHIFT	= 0;
	private static final int AD_IMPRESSION_PLAYBACK_PERCENT_SHIFT	=
		AD_IMPRESSION_VIEWABLE_PERCENT_SHIFT + BITS_PER_PERCENT;
	private static final int AD_IMPRESSION_FLAGS_SHIFT				=
		AD_IMPRESSION_PLAYBACK_PERCENT_SHIFT + BITS_PER_PERCENT;

	private static final int AD_IMPRESSION_BILLABLE_MASK = 1 << (AD_IMPRESSION_FLAGS_SHIFT + 0);

	/**
	 * Construct error event.
	 *
	 * @param renderer renderer on which error was encountered
	 * @param cause error cause
	 * @param adIns ad instance associated with event, if any
	 * @return resulting error event
	 */
	static PlacementEvent
	ofError(PlacementRenderer renderer, PlacementException cause, @Nullable AdInstance adIns) {
		return new PlacementEvent(
			EVENT_ERROR,
			renderer,
			adIns,
			Preconditions.checkNotNull(cause),
			0
		);
	}

	/**
	 * Construct error event with newly constructed {@linkplain PlacementException exception}.
	 *
	 * @param renderer renderer on which error was encountered
	 * @param errCode code describing error
	 * @param msg human-readable error message
	 * @param cause error cause, if any
	 * @param adIns ad instance associated with event, if any
	 * @return resulting error event
	 */
	static PlacementEvent ofError(
		PlacementRenderer renderer,
		@PlacementException.ErrorCode int errCode,
		String msg,
		@Nullable Throwable cause,
		@Nullable AdInstance adIns
	) {
		PlacementException exc = new PlacementException(errCode, msg, cause);

		exc.fillInStackTrace();
		return ofError(renderer, exc, adIns);
	}

	/**
	 * Construct a new ad event.
	 *
	 * @param type ad event type
	 * @param renderer renderer for which to construct event
	 * @param adIns ad instance associated with event
	 * @param data additional event data
	 * @param bits additional event bits
	 * @return resulting event
	 */
	private static PlacementEvent ofAd(
		@Type int type,
		PlacementRenderer renderer,
		AdInstance adIns,
		@Nullable Object data,
		int bits
	) {
		return new PlacementEvent(type, renderer, Preconditions.checkNotNull(adIns), data, bits);
	}

	/**
	 * Construct new ad available event.
	 *
	 * @param renderer renderer for which ad is available
	 * @param adIns instance of available ad
	 * @return resulting ad available event
	 */
	static PlacementEvent ofAdAvailable(PlacementRenderer renderer, AdInstance adIns) {
		return ofAd(EVENT_AD_AVAILABLE, renderer, adIns, null, 0);
	}

	/**
	 * Construct new ad selected event.
	 *
	 * @param renderer renderer for which ad has been selected
	 * @param adIns instance of selected ad
	 * @return resulting ad selected event
	 */
	@UiThread
	static PlacementEvent ofAdSelected(PlacementRenderer renderer, AdInstance adIns) {
		return ofAd(EVENT_AD_SELECTED, renderer, adIns, null, 0);
	}

	/**
	 * Construct new ad rendered event.
	 * <p>The rendered ad must be the {@linkplain PlacementRenderer#currentAdInstance() current}
	 * ad of {@code renderer}.
	 *
	 * @param renderer renderer in which ad has been rendered
	 * @return resulting ad rendered event
	 */
	@UiThread
	static PlacementEvent ofAdRendered(PlacementRenderer renderer) {
		return ofAd(EVENT_AD_RENDERED, renderer, renderer.currentAdInstance(), null, 0);
	}

	/**
	 * Construct new ad impression event.
	 *
	 * @param renderer renderer in which ad was (or is) rendered
	 * @param adIns instance of ad which received impression
	 * @param viewPct percent of ad which was viewable during impression
	 * @param playPct percent of playback ad duration elapsed during impression
	 * @param billable {@code true} if, and only if, impression is possibly billable
	 * @return resulting ad impression event
	 */
	static PlacementEvent ofAdImpression(
		PlacementRenderer renderer,
		AdInstance adIns,
		int viewPct,
		int playPct,
		boolean billable
	) {
		return ofAd(EVENT_AD_IMPRESSION, renderer, adIns, null, (
			((viewPct & PERCENT_MASK) << AD_IMPRESSION_VIEWABLE_PERCENT_SHIFT) |
			((playPct & PERCENT_MASK) << AD_IMPRESSION_PLAYBACK_PERCENT_SHIFT) |
			(billable ? AD_IMPRESSION_BILLABLE_MASK : 0)
		));
	}

	/**
	 * Construct new ad activated event.
	 * <p>The activated ad must be the {@linkplain PlacementRenderer#currentAdInstance() current}
	 * ad of {@code renderer}.
	 *
	 * @param renderer renderer in which ad is rendered
	 * @param uri URI user will be navigated to
	 * @return resulting ad activated event
	 */
	@UiThread
	static PlacementEvent ofAdActivated(PlacementRenderer renderer, Uri uri) {
		return ofAd(EVENT_AD_ACTIVATED, renderer, renderer.currentAdInstance(), uri, 0);
	}

	/**
	 * Construct new ad removed event.
	 *
	 * @param renderer renderer in which ad was rendered
	 * @param adIns instance of removed ad
	 * @return resulting ad removed event
	 */
	@UiThread
	static PlacementEvent ofAdRemoved(PlacementRenderer renderer, AdInstance adIns) {
		return ofAd(EVENT_AD_REMOVED, renderer, adIns, null, 0);
	}

	/**
	 * Construct new ad resized event.
	 * <p>The resized ad must be the {@linkplain PlacementRenderer#currentAdInstance() current}
	 * ad of {@code renderer}.
	 *
	 * @param renderer renderer in which ad is rendered
	 * @return resulting ad resized event
	 */
	@UiThread
	static PlacementEvent ofAdResized(PlacementRenderer renderer) {
		return ofAd(EVENT_AD_RESIZED, renderer, renderer.currentAdInstance(), null, 0);
	}

	/**
	 * Construct new user reward event.
	 *
	 * @param renderer renderer in which ad was (or is) rendered
	 * @param adIns instance of ad for which user is rewarded
	 * @return resulting user reward event
	 */
	static PlacementEvent ofUserReward(PlacementRenderer renderer, AdInstance adIns) {
		return ofAd(EVENT_AD_RESIZED, renderer, adIns, null, 0);
	}

	private final PlacementRenderer renderer;
	private final @Nullable AdInstance adInstance;
	private final @Nullable Object data;
	private final int typeAndBits;

	private PlacementEvent(
		@Type int type,
		PlacementRenderer renderer,
		@Nullable AdInstance adIns,
		@Nullable Object data,
		int bits
	) {
		Preconditions.checkArgument(
			renderer != null &&
			(type & TYPE_MASK) == type &&
			(bits & (~0 >>> BITS_PER_TYPE)) == bits
		);

		this.renderer = renderer;
		this.adInstance = adIns;
		this.data = data;
		this.typeAndBits = type | (bits << BITS_PER_TYPE);
	}

	/**
	 * Event type.
	 *
	 * @return event type
	 * @since 1.2
	 */
	@SuppressLint("WrongConstant")
	public @Type int type() {
		return this.typeAndBits & TYPE_MASK;
	}

	/**
	 * Event bits data.
	 *
	 * @return bits data
	 */
	private int bits() {
		return this.typeAndBits >>> BITS_PER_TYPE;
	}

	/**
	 * Renderer of placement for which event was generated for.
	 *
	 * @return placement renderer
	 * @since 1.2
	 */
	public PlacementRenderer renderer() {
		return this.renderer;
	}

	/**
	 * Id of placement event was generated for.
	 *
	 * @return placement id
	 * @since 1.2
	 * @see PlacementRenderer#placementId()
	 */
	public @NonNull String placementId() {
		//noinspection DataFlowIssue
		return this.renderer.placementId();
	}

	/**
	 * Ad instance associated with event.
	 * <p>This is returned to return a non-{@code null} value for the following events:
	 * <ol>
	 *     <li>{@linkplain #EVENT_AD_ACTIVATED Ad Activated}</li>
	 *     <li>{@linkplain #EVENT_AD_AVAILABLE Ad Available}</li>
	 *     <li>{@linkplain #EVENT_AD_IMPRESSION Ad Impression}</li>
	 *     <li>{@linkplain #EVENT_AD_REMOVED Ad Removed}</li>
	 *     <li>{@linkplain #EVENT_AD_RENDERED Ad Rendered}</li>
	 *     <li>{@linkplain #EVENT_AD_RESIZED Ad Resized}</li>
	 *     <li>{@linkplain #EVENT_AD_SELECTED Ad Selected}</li>
	 *     <li>{@linkplain #EVENT_USER_REWARD User Reward}</li>
	 * </ol>
	 *
	 * @return ad instance, if any
	 * @since 1.2
	 */
	public @Nullable AdInstance adInstance() {
		return this.adInstance;
	}

	/**
	 * Ensure event is of an expected type.
	 *
	 * @param exp expected event type
	 * @param what expected event type name
	 * @throws IllegalStateException event type is not {@code exp}
	 */
	private void checkType(@Type int exp, String what) {
		Preconditions.checkState(this.type() == exp, "not %s event", what);
	}

	/**
	 * Retrieve ad impression event data bits.
	 *
	 * @return data bits
	 * @throws IllegalStateException not an {@linkplain #EVENT_AD_IMPRESSION ad impression} event
	 */
	private int adImpressionBits() {
		this.checkType(EVENT_AD_IMPRESSION, "ad impression");
		return this.bits();
	}

	/**
	 * Percent of ad media viewable during {@linkplain #EVENT_AD_IMPRESSION ad impression}.
	 *
	 * @return viewable ad media percent
	 * @throws IllegalStateException not an ad impression event
	 * @since 1.2
	 */
	public int adImpressionViewablePercent() {
		return (this.adImpressionBits() >>> AD_IMPRESSION_VIEWABLE_PERCENT_SHIFT) & PERCENT_MASK;
	}

	/**
	 * Percent of playback ad media duration elapsed during {@linkplain #EVENT_AD_IMPRESSION ad
	 * impression}.
	 *
	 * @return playback ad media duration elapse percent
	 * @throws IllegalStateException not an ad impression event
	 * @since 1.2
	 */
	public int adImpressionPlaybackPercent() {
		return (this.adImpressionBits() >>> AD_IMPRESSION_PLAYBACK_PERCENT_SHIFT) & PERCENT_MASK;
	}

	/**
	 * Possibly billable {@linkplain #EVENT_AD_IMPRESSION ad impression}.
	 *
	 * @return {@code true} if, and only if, ad impression is possibly billable
	 * @throws IllegalStateException not an ad impression event
	 * @since 1.2
	 */
	public boolean adImpressionPossiblyBillable() {
		return (this.adImpressionBits() & AD_IMPRESSION_BILLABLE_MASK) != 0;
	}

	/**
	 * Retrieve type-specific event data.
	 *
	 * @param <T> event data type
	 * @param exp expected event type
	 * @param what expected event type name
	 * @return event data
	 * @throws IllegalStateException event type is not {@code exp}
	 */
	@SuppressWarnings("unchecked")
	private <T> @NonNull T data(@Type int exp, String what) {
		this.checkType(exp, what);
		//noinspection DataFlowIssue
		return (T) this.data;
	}

	/**
	 * Ad {@linkplain #EVENT_AD_ACTIVATED activated} target URL.
	 * <p>The URL returned is what the user will be navigated to in response to the ad activation.
	 *
	 * @return activation target URL
	 * @throws IllegalStateException not an ad activated event
	 * @since 1.2
	 */
	public Uri adActivatedTarget() {
		return this.data(EVENT_AD_ACTIVATED, "ad activated");
	}

	/**
	 * Cause of {@linkplain #EVENT_ERROR error} event.
	 *
	 * @return error cause
	 * @throws IllegalStateException not an error event
	 * @since 1.2
	 */
	public PlacementException errorCause() {
		return this.data(EVENT_ERROR, "error");
	}

	@Override
	public String toString() {
		return String.format(
			Locale.ROOT,
			"PlacementEvent{" +
				"renderer=%s," +
				"adInstance=%s," +
				"data=%s," +
				"type=%s," +
				"bits=%X" +
			"}",
			this.renderer,
			this.adInstance,
			this.data,
			this.type(),
			this.bits()
		);
	}
}
