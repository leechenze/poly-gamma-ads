// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.ads;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.Window;

import androidx.annotation.AnyThread;
import androidx.annotation.FloatRange;
import androidx.annotation.IntDef;
import androidx.annotation.Nullable;
import androidx.annotation.UiContext;
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
 * Placement {@linkplain PlacementRenderer renderer} capable of rendering outstream ad media.
 *
 * @since 1.2
 */
@UiThread
public final class DisplayPlacementView
extends AdMediaLayout
implements PlacementRenderer, PlacementRendererInternal {

	/**
	 * Renderer is never modal.
	 *
	 * @since 1.2
	 */
	public static final @ModalityMode int MODALITY_NONE			= 0;

	/**
	 * Renderer is always an interstitial modal.
	 *
	 * @since 1.2
	 */
	public static final @ModalityMode int MODALITY_INTERSTITIAL	= 1;

	/**
	 * Renderer can expand into an interstitial modal.
	 *
	 * @since 1.2
	 */
	public static final @ModalityMode int MODALITY_EXPANDABLE	= 2;

	/**
	 * Renderer modality mode enumeration value marker.
	 *
	 * @since 1.2
	 */
	@Documented
	@Retention(RetentionPolicy.SOURCE)
	@Target(ElementType.TYPE_USE)
	@IntDef({ MODALITY_EXPANDABLE, MODALITY_INTERSTITIAL, MODALITY_NONE })
	public @interface ModalityMode {
	}

	/**
	 * Construct new empty renderer {@linkplain DisplayPlacementViewBuilder builder}.
	 *
	 * @param ctxt owning context
	 * @return resulting builder
	 * @since 1.2
	 */
	@AnyThread
	public static DisplayPlacementViewBuilder ofBuilder(@UiContext Context ctxt) {
		return new DisplayPlacementViewBuilder(ctxt);
	}

	/**
	 * Construct new empty renderer {@linkplain DisplayPlacementViewBuilder builder} with
	 * placement id.
	 * <p>Shorthand for:
	 * {@snippet lang="java" :
	 * ofBuilder(ctxt) // @link substring="ofBuilder" target="#ofBuilder(Context)"
	 *     .placementId(plcmtId); // @link substring="placementId" target="DisplayPlacementViewBuilder#placementId(String)"
	 * }
	 *
	 * @param ctxt owning context
	 * @param plcmtId id of placement to bind renderer to
	 * @return resulting builder
	 * @throws IllegalArgumentException {@code plcmtId} is invalid
	 * @since 1.2
	 * @see #ofBuilder(Context)
	 */
	@AnyThread
	public static DisplayPlacementViewBuilder ofBuilder(@UiContext Context ctxt, String plcmtId) {
		return ofBuilder(ctxt).placementId(plcmtId);
	}

	/**
	 * Construct new renderer with a context, {@linkplain #bindToPlacement(String) bound} to a
	 * placement.
	 * <p>Shorthand for:
	 * {@snippet lang="java" :
	 * ofBuilder(ctxt, plcmtId) // @link substring="ofBuilder" target="#ofBuilder(Context, String)"
	 *     .build(); // @link substring="build" target="DisplayPlacementViewBuilder#build()"
	 * }
	 *
	 * @param ctxt owning context
	 * @param plcmtId id of placement to bind renderer to
	 * @return resulting view
	 * @since 1.2
	 */
	public static DisplayPlacementView ofPlacementId(@UiContext Context ctxt, String plcmtId) {
		return ofBuilder(ctxt, plcmtId).build();
	}

	private @Nullable Window window;
	private final @ModalityMode int modality;

	/**
	 * Construct new renderer from builder, with attributes.
	 *
	 * @param builder builder to construct from
	 * @param attrs renderer attributes
	 */
	DisplayPlacementView(DisplayPlacementViewBuilder builder, @Nullable AttributeSet attrs) {
		super(builder.context, attrs);
		this.modality = builder.modality;
	}

	/**
	 * Construct new renderer with owning context and attributes.
	 *
	 * @param ctxt owning context
	 * @param attrs renderer attributes
	 * @throws IllegalArgumentException {@code attrs} is malformed:
	 * <ul>
	 *     <li>{@code origin_displayPlacement_id} is defined to an invalid value</li>
	 *     <li>
	 *         {@code origin_displayPlacement_modality} is not {@linkplain #MODALITY_NONE none}
	 *         and {@code ctxt} is not an {@linkplain Activity activity}
	 *     </li>
	 * </ul>
	 * @throws IllegalStateException {@linkplain AdsModule module} has not been loaded
	 * @since 1.2
	 */
	public DisplayPlacementView(@UiContext Context ctxt, AttributeSet attrs) {
		this(DisplayPlacementViewBuilder.ofAttributes(ctxt, attrs), attrs);
	}

	public void pauseAdMediaPlayback() {
	}

	public void resumeAdMediaPlayback() {
	}

	public void setPlaybackAdMediaVolume(
		@FloatRange(from = 0, to = 1) float left,
		@FloatRange(from = 0, to = 1) float right
	) {
	}

	@Override
	void onSupportedAdMediaSize(AdSize size) {
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();

		this.window = ViewUtils.windowOf(this);
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		this.window = null;
		super.clearAdMediaView();
	}

	@Override
	public @Nullable String placementId() {
		return null;
	}

	@Override
	public void setPlacementEventListener(@Nullable Consumer<PlacementEvent> listener) {
	}

	@Override
	public void bindToPlacement(String id) {
		Preconditions.checkArgument(!id.isEmpty());
	}

	/**
	 * {@inheritDoc}
	 *
	 * @throws IllegalStateException renderer has not been measured <i>and</i> {@linkplain
	 * #setSupportedAdMediaSize(AdSize) supported} ad media size not specified, or, {@inheritDoc}
	 */
	@Override
	public void beginRequestingAds() {
	}

	@Override
	public boolean isRequestingAds() {
		return false;
	}

	@Override
	public boolean canRenderAds() {
		return this.isRequestingAds() && super.supportedAdMediaSize() != null;
	}

	@Override
	public @Nullable AdInstance currentAdInstance() {
		return super.isRenderingAdMedia() ? new AdInstance() : null;
	}

	@Override
	public String toString() {
		return String.format(
			Locale.ROOT,
			"DisplayPlacementView{placementId=%s}",
			this.placementId()
		);
	}
}
