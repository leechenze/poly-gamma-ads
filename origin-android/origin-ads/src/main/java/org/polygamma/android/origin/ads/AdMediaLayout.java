// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.ads;

import static androidx.annotation.Dimension.DP;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Insets;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroupOverlay;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.WindowMetrics;

import androidx.annotation.AnyThread;
import androidx.annotation.CallSuper;
import androidx.annotation.Dimension;
import androidx.annotation.IntRange;
import androidx.annotation.Nullable;
import androidx.annotation.Px;
import androidx.annotation.RestrictTo;
import androidx.annotation.UiContext;
import androidx.annotation.UiThread;

import org.polygamma.android.origin.util.AndroidContexts;
import org.polygamma.android.origin.util.Logger;
import org.polygamma.android.origin.util.Preconditions;

import java.util.Locale;

/**
 * View which applies layout to nested ad media.
 * <p>These views permit at most <i>2</i> children, ad media and ad media overlay. Any {@link
 * ViewGroup} operation which modifies contents are unsupported. These include, but are not
 * limited to, {@link #addView(View)} and friends.
 * <p>Nested ad media and ad media overlay is laid out based on size constraints of ad media.
 * There are two sets of constraints supported, {@linkplain #requestedAdMediaSize() requested} and
 * {@linkplain #supportedAdMediaSize() supported} ad media size constraints. Requested ad media
 * size constraints are constraints <i>requested</i> by ad media rendered within a layout.
 * Supported ad media size constraints are constraints <i>{@linkplain #measure(int, int)
 * measured}</i> or set <i>{@linkplain #setSupportedAdMediaSize(AdSize) explicitly}</i>, which
 * define constraints any media rendered within a layout must satisfy in order to be eligble to
 * be rendered within the layout.
 * <p>Layouts, which are {@linkplain #isRenderingAdMedia() rendering} ad media, whose supported
 * constraints are updated, either through re-measurements or explicitly being set, <i>may</i>
 * be {@linkplain #isAdMediaOverflow() smaller} than the ad media size requested for the ad media.
 * In this case, the media cannot be considered viewable.
 *
 * @since 1.2
 */
@UiThread
public abstract class AdMediaLayout extends ViewGroup {

	private static final String TAG = AdMediaLayout.class.getSimpleName();

	// Ad media overflows along width dimension.
	private static final int AD_MEDIA_WIDTH_OVERFLOW			= 1 << 0;
	// Ad media overflows along height dimension.
	private static final int AD_MEDIA_HEIGHT_OVERFLOW			= 1 << 1;

	/**
	 * Fail unconditionally with {@link UnsupportedOperationException}.
	 */
	private static void throwUnsupported() {
		throw new UnsupportedOperationException();
	}

	private int flags;
	// requested size of ad media
	private AdSize requestedAdMediaSize;
	// explicit supported ad media size, or empty if none specified
	private AdSize explicitSupportedAdMediaSize;
	// maximum possible ad media size, `null` if view hasn't been measured, or empty if unavailable
	private @Nullable AdSize supportedAdMediaSize;
	// pixel density or `0` if unknown
	private float pixelDensity;

	/**
	 * Construct new ad media layout.
	 *
	 * @param ctxt owning context
	 * @param attrs attributes to initialize with
	 */
	AdMediaLayout(@UiContext Context ctxt, @Nullable AttributeSet attrs) {
		super(ctxt, attrs);
		this.requestedAdMediaSize = AdSize.EMPTY;
		this.explicitSupportedAdMediaSize = AdSize.EMPTY;
		this.pixelDensity = super.getResources().getDisplayMetrics().density;
	}

	/**
	 * Ensure calling context is not within a {@linkplain #isInLayout() layout pass}.
	 *
	 * @throws IllegalStateException calling context is within a layout pass
	 */
	@RestrictTo(RestrictTo.Scope.SUBCLASSES)
	final void checkNotInLayout() {
		Preconditions.checkState(!super.isInLayout(), "cannot be invoked within a layout pass");
	}

	/**
	 * Convert measurement in pixels to device independent pixels.
	 *
	 * @param px measurement to convert
	 * @return converted measurement
	 */
	final @Dimension(unit = DP) int dpOfPx(@Px int px) {
		return this.pixelDensity == 0.f ? 0 : Math.round(px / this.pixelDensity);
	}

	/**
	 * Convert measurement in device independent pixels to pixels.
	 *
	 * @param dp measurement to convert
	 * @return converted measurement
	 */
	final @Px int pxOfDp(@Dimension(unit = DP) int dp) {
		return Math.round(dp * this.pixelDensity);
	}

	/**
	 * Test whether ad media is rendered.
	 *
	 * @return {@code true} if, and only if, ad media is rendered
	 */
	public final boolean isRenderingAdMedia() {
		return super.getChildCount() > 0;
	}

	/**
	 * Test whether rendered ad media overflows size of {@code this}.
	 *
	 * @return {@code true} if, and only, ad media overflows {@code this}
	 * @since 1.2
	 */
	public final boolean isAdMediaOverflow() {
		return (this.flags & (AD_MEDIA_WIDTH_OVERFLOW | AD_MEDIA_HEIGHT_OVERFLOW)) != 0;
	}

	/**
	 * Size requested by rendered ad media.
	 * <p>If no ad media is currently {@linkplain #isRenderingAdMedia() rendered}, this returns
	 * an {@linkplain AdSize#isEmpty() empty} size unconditionally.
	 *
	 * @return requested size
	 * @since 1.2
	 * @see #isRenderingAdMedia()
	 */
	@AnyThread
	public final AdSize requestedAdMediaSize() {
		return this.requestedAdMediaSize;
	}

	/**
	 * Supported ad media size.
	 *
	 * @return {@code null}, {@linkplain AdSize#isEmpty() empty}, or supported size if layout has
	 * not yet been {@linkplain #measure(int, int) measured}, no space is available for ad media,
	 * or supported size, respectively
	 * @since 1.2
	 * @see #setSupportedAdMediaSize(AdSize)
	 */
	public final @Nullable AdSize supportedAdMediaSize() {
		AdSize supp = this.supportedAdMediaSize;
		AdSize expl = this.explicitSupportedAdMediaSize;

		return supp != null ? supp : expl.isEmpty() ? null : expl;
	}

	/**
	 * Handle {@linkplain #supportedAdMediaSize() supported} ad media size update.
	 *
	 * @param size updated ad media size, possibly {@linkplain AdSize#isEmpty() empty} if no space
	 * is available to render ad media
	 */
	@RestrictTo(RestrictTo.Scope.SUBCLASSES)
	abstract void onSupportedAdMediaSize(AdSize size);

	/**
	 * Determine current window size.
	 * <p>If this layout is attached to a {@linkplain #isAttachedToWindow() window}, then the size
	 * of the window, less any system insets, is returned; otherwise, the size of the device screen
	 * is returned.
	 *
	 * @return tuple of window width and height, in pixels
	 */
	@SuppressWarnings({ "RedundantSuppression", "deprecation" })
	private Point currentWindowSize() {
		Point rv = new Point();
		Context ctxt = super.getContext();
		Display display = super.getDisplay();
		WindowManager wman = AndroidContexts.systemServiceOf(
			ctxt,
			WindowManager.class,
			Context.WINDOW_SERVICE
		);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && (
			display == AndroidContexts.displayOf(ctxt) &&
			wman != null
		)) {
			WindowMetrics met = wman.getCurrentWindowMetrics();
			WindowInsets wins = met.getWindowInsets();
			Insets ins = wins.getInsetsIgnoringVisibility(
				WindowInsets.Type.displayCutout() |
				WindowInsets.Type.navigationBars()
			);
			Rect bounds = met.getBounds();

			rv.set(
				bounds.width() - (ins.left + ins.right),
				bounds.height() - (ins.top + ins.bottom)
			);
		} else if (
			display != null ||
			(display = AndroidContexts.displayOf(ctxt)) != null ||
			(wman != null && (display = wman.getDefaultDisplay()) != null)
		) {
			display.getRealSize(rv);
		} else {
			Configuration cfg = super.getResources().getConfiguration();

			if (cfg != null)
				rv.set(this.pxOfDp(cfg.screenWidthDp), this.pxOfDp(cfg.screenHeightDp));
		}
		return rv;
	}

	/**
	 * Try and update {@linkplain #supportedAdMediaSize supported} ad media size.
	 *
	 * @param size size to update with
	 * @return {@code true} if {@code size} was materially different and was set as the new
	 * supported size; otherwise, {@code false}
	 */
	private boolean tryUpdateSupportedAdMediaSize(AdSize size) {
		if (this.supportedAdMediaSize != null && this.supportedAdMediaSize.equals(size))
			return false;
		Logger.debug(
			TAG,
			"%s - updated supported ad media size: %sx%s,%sx%s,%s:%s",
			this,
			size.minWidthDp(), size.minHeightDp(),
			size.maxWidthDp(), size.maxHeightDp(),
			size.widthRatio(), size.heightRatio()
		);
		this.supportedAdMediaSize = size;
		this.onSupportedAdMediaSize(size);

		/*
		 * if we're rendering ad media, make sure to request layout, this is important because
		 * if we're invoked outside of `onMeasure`, `onMeasure` *may* not re-measure the media
		 * if its invocation of `updateSupportedAdMediaSize` does not update the supported size,
		 * in this case, we'll rely on the call below to ensure the media is still re-measured.
		 */
		if (!super.isInLayout()) {
			View media = this.adMediaView();
			View overlay = this.adMediaOverlayView();

			if (media != null)
				media.requestLayout();
			if (overlay != null)
				overlay.requestLayout();
		}
		return true;
	}

	/**
	 * Update {@linkplain #supportedAdMediaSize supported} ad media size based on layout size.
	 * <p>If {@code w} and {@code h} cause a material change in supported ad media size, this
	 * updates the supported ad media size, {@linkplain #onSupportedAdMediaSize(AdSize) notifies}
	 * implementation, and returns {@code true}; otherwise, this simply returns {@code false}.
	 *
	 * @param w {@code -1}, {@code 0}, or positive value if layout width is unconstrained, layout
	 * has no space along width dimension, or maximum width, in device independent pixels, of
	 * layout, respectively
	 * @param h {@code -1}, {@code 0}, or positive value if layout height is unconstrained, layout
	 * has no space along height dimension, or maximum height, in device independent pixels, of
	 * layout, respectively
	 * @return {@code true} if, and only if, supported size was updated
	 */
	private boolean updateSupportedAdMediaSize(
		@Dimension(unit = DP) @IntRange(from = -1) int w,
		@Dimension(unit = DP) @IntRange(from = -1) int h
	) {
		AdSize update = this.explicitSupportedAdMediaSize;
		int maxW = update.maxWidthDp();
		int maxH = update.maxHeightDp();

		Logger.debug(
			TAG,
			"%s - updating supported ad media size with max %sx%s, explicit=%sx%s,%sx%s,%s:%s current=%s",
			this,
			w, h,
			update.minWidthDp(), update.minHeightDp(),
			update.maxWidthDp(), update.maxHeightDp(),
			update.widthRatio(), update.heightRatio(),
			this.supportedAdMediaSize == null ? "N/A" : String.format(
				Locale.ROOT,
				"%sx%s,%sx%s,%s:%s",
				this.supportedAdMediaSize.minWidthDp(), this.supportedAdMediaSize.minHeightDp(),
				this.supportedAdMediaSize.maxWidthDp(), this.supportedAdMediaSize.maxHeightDp(),
				this.supportedAdMediaSize.widthRatio(), this.supportedAdMediaSize.heightRatio()
			)
		);
		if (w == -1 && maxW != 0)
			w = maxW;
		if (h == -1 && maxH != 0)
			h = maxH;
		if (this.requestedAdMediaSize != null) {
			int maxReqW = this.requestedAdMediaSize.maxWidthDp();
			int maxReqH = this.requestedAdMediaSize.maxHeightDp();

			if (w == -1 && maxReqW != 0)
				w = Math.max(maxReqW, update.minWidthDp());
			if (h == -1 && maxReqH != 0)
				h = Math.max(maxReqH, update.minHeightDp());
		}
		if (w == -1 || h == -1) {
			Point size = this.currentWindowSize();

			if (w == -1) {
				w = Math.max(
					this.dpOfPx(Math.max(size.x - ViewUtils.inlinePaddingOf(this), 0)),
					update.minWidthDp()
				);
			}
			if (h == -1) {
				h = Math.max(
					this.dpOfPx(Math.max(size.y - ViewUtils.blockPaddingOf(this), 0)),
					update.minHeightDp()
				);
			}
		}

		if (w == 0 || h == 0) {
			// we can't possibly render anything
			return this.tryUpdateSupportedAdMediaSize(AdSize.EMPTY);
		}

		w = maxW == 0 ? w : Math.min(w, maxW);
		h = maxH == 0 ? h : Math.min(h, maxH);
		if (update.hasRelative()) {
			int newW = update.resolveRelativeWidth(h);
			int newH = update.resolveRelativeHeight(w);

			if (newW > w)
				h = newH;
			else
				w = newW;
		}
		return this.tryUpdateSupportedAdMediaSize(AdSize.of(
			update.widthRatio(), update.heightRatio(),
			update.minWidthDp(), update.minHeightDp(),
			w, h
		));
	}

	/**
	 * Set supported ad media size.
	 * <p>If {@code size} is empty, supported ad media size is determined based on the measured
	 * size of {@code this}. Otherwise, {@code size} specifies the maximum supported ad media
	 * size. Note that when {@code size} is non-empty, the size returned by {@link
	 * #supportedAdMediaSize()} may not equal {@code size}
	 *
	 * @param size supported size or {@linkplain AdSize#isEmpty() empty} to determine based on
	 * measurement
	 * @throws IllegalStateException currently in a {@linkplain #isInLayout() layout pass}
	 * @since 1.2
	 * @see #supportedAdMediaSize()
	 */
	public final void setSupportedAdMediaSize(AdSize size) {
		int w = -2;
		int h = -2;

		this.checkNotInLayout();
		if (!size.isEmpty()) {
			this.explicitSupportedAdMediaSize = size;
			/*
			 * if we're not attached to a display *or* we haven't been measured, use window or
			 * screen bounds. this may not be the right final dimension, however, if we're
			 * requesting ads before measurement, better to set some upper bound than
			 * unconstrained.
			 */
			if (
				this.supportedAdMediaSize == null ||
				super.getDisplay() == null ||
				!super.getDisplay().isValid()
			) {
				w = -1;
				h = -1;
			}
		} else if (this.explicitSupportedAdMediaSize.isEmpty()) {
			// already not explicit, bail
			return;
		} else {
			this.explicitSupportedAdMediaSize = AdSize.EMPTY;
		}
		if (this.supportedAdMediaSize != null && w == -2) {
			w = this.supportedAdMediaSize.maxWidthDp();
			h = this.supportedAdMediaSize.maxHeightDp();
		}
		if (w == -2 || this.updateSupportedAdMediaSize(w, h))
			super.requestLayout();
	}

	/**
	 * Rendered ad media, if any.
	 *
	 * @return ad media or {@code null} if ad media has not yet been {@linkplain
	 * #setAdMediaView(View, View, AdSize) set}
	 */
	final @Nullable View adMediaView() {
		return super.getChildAt(0);
	}

	/**
	 * Require rendered ad media.
	 *
	 * @return ad media
	 * @throws NullPointerException ad media has not yet been {@linkplain
	 * #setAdMediaView(View, View, AdSize) set}
	 */
	final View requireAdMediaView() {
		return Preconditions.checkNotNull(this.adMediaView(), "ad media not rendered");
	}

	/**
	 * Rendered ad media overlay, if any.
	 *
	 * @return ad media overlay or {@code null} if ad media has not yet been {@linkplain
	 * #setAdMediaView(View, View, AdSize) set}
	 */
	final @Nullable View adMediaOverlayView() {
		return super.getChildAt(1);
	}

	/**
	 * Remove rendered ad media and overlay views, if any.
	 *
	 * @throws IllegalStateException currently in a {@linkplain #isInLayout() layout pass}
	 */
	@CallSuper
	@RestrictTo(RestrictTo.Scope.SUBCLASSES)
	void clearAdMediaView() {
		this.checkNotInLayout();
		this.flags = 0;
		this.requestedAdMediaSize = AdSize.EMPTY;
		super.removeAllViewsInLayout();
		super.requestLayout();
		super.invalidate();
	}

	/**
	 * Set ad media and overlay views.
	 *
	 * @param overlay overlay view to render, if any
	 * @param media ad media view to render
	 * @param size requested ad media size
	 * @throws IllegalArgumentException {@code overlay} is non-{@code null} and already has a
	 * {@linkplain View#getParent() parent}, or {@code media} already has a parent
	 * @throws IllegalStateException currently in a {@linkplain #isInLayout() layout pass}
	 */
	@CallSuper
	@RestrictTo(RestrictTo.Scope.SUBCLASSES)
	void setAdMediaView(@Nullable View overlay, View media, AdSize size) {
		Preconditions.checkArgument(
			overlay == null || overlay.getParent() == null,
			"overlay already attached to parent"
		);
		Preconditions.checkArgument(media.getParent() == null, "media already attached to parent");
		this.checkNotInLayout();
		this.clearAdMediaView();

		this.requestedAdMediaSize = size;
		super.addViewInLayout(media, -1, super.generateDefaultLayoutParams());
		if (overlay != null)
			super.addViewInLayout(overlay, -1, super.generateDefaultLayoutParams());
		super.requestLayout();
		super.invalidate();
		Logger.debug(
			TAG,
			"%s - rendering ad media %s, max-size=%sx%s, min-size=%sx%s, aspect-ratio=%s:%s",
			this,
			media,
			size.maxWidthDp(),
			size.maxHeightDp(),
			size.minWidthDp(),
			size.minHeightDp(),
			size.widthRatio(),
			size.heightRatio()
		);
	}

	/**
	 * Calculate the preferred render size of ad media.
	 * <p>The preferred render size of ad media is the render size of the ad media without
	 * considering layout constraints.
	 *
	 * @return tuple of preferred width and height, in device independent pixels
	 */
	private Point calculatePreferredAdMediaRenderSize() {
		AdSize req = this.requestedAdMediaSize;
		Point rv = new Point(req.exactWidthDp(), req.exactHeightDp());

		// if requested size is exact, then just return it
		if (rv.x != 0 && rv.y != 0)
			return rv;

		// any inexact dimension needs to be replaced with whatever is viable for us
		if ((rv.x != 0 || rv.y != 0) && req.hasRelative()) {
			/*
			 * we have an exact dimension and a ratio, figure out inexact dimension using exact
			 * dimension and ratio
			 */
			if (rv.x == 0)
				rv.x = req.resolveRelativeWidth(rv.y);
			else
				rv.y = req.resolveRelativeHeight(rv.x);
			return rv;
		}
		if (rv.x == 0) {
			//noinspection DataFlowIssue
			rv.x = Math.max(req.minWidthDp(), Math.max(
				this.supportedAdMediaSize.maxWidthDp(),
				req.maxWidthDp()
			));
		}
		if (rv.y == 0) {
			//noinspection DataFlowIssue
			rv.y = Math.max(req.minHeightDp(), Math.max(
				this.supportedAdMediaSize.maxHeightDp(),
				req.maxHeightDp()
			));
		}
		// enforce ratio, if required
		if (this.requestedAdMediaSize.hasRelative()) {
			int newW = this.requestedAdMediaSize.resolveRelativeWidth(rv.y);
			int newH = this.requestedAdMediaSize.resolveRelativeHeight(rv.x);

			if (rv.x < newW)
				rv.y = newH;
			else
				rv.x = newW;
		}
		return rv;
	}

	/**
	 * Calculate render size of ad media based on {@linkplain #supportedAdMediaSize supported}
	 * ad media size.
	 * <p>Upon return overflow {@linkplain #flags flags} are updated to reflect whether the
	 * supported ad media size is sufficient for ad media. The size returned is guaranteed to not
	 * overflow the maximum size constraint of the supported ad media size.
	 *
	 * @return tuple of render width and height, in device independent pixels
	 */
	private Point calculateAdMediaRenderSize() {
		Point size = this.calculatePreferredAdMediaRenderSize();
		View media = this.requireAdMediaView();
		int padX = this.dpOfPx(ViewUtils.inlinePaddingOf(media));
		int padY = this.dpOfPx(ViewUtils.blockPaddingOf(media));
		@SuppressWarnings("DataFlowIssue")
		int maxW = this.supportedAdMediaSize.maxWidthDp();
		int maxH = this.supportedAdMediaSize.maxHeightDp();

		Logger.debug(
			TAG,
			"%s - preferred ad media render size: (%s + %s)x(%s + %s)",
			this,
			size.x, padX,
			size.y, padY
		);

		this.flags &= ~(AD_MEDIA_HEIGHT_OVERFLOW | AD_MEDIA_WIDTH_OVERFLOW);
		if ((size.x + padX) <= maxW && (size.y + padY) <= maxH) {
			// we can fit in the available space
			size.x += padX;
			size.y += padY;
		} else if (size.x != 0 && size.y != 0) {
			// we don't fit in the available space, resize
			float scale = Math.min(((float) maxW) / size.x, ((float) maxH) / size.y);

			size.x = Math.min(Math.round(size.x * scale) + padX, maxW);
			size.y = Math.min(Math.round(size.y * scale) + padY, maxH);
		}

		int minReqW = Math.max(this.requestedAdMediaSize.minWidthDp(), 1) + padX;
		int minReqH = Math.max(this.requestedAdMediaSize.minHeightDp(), 1) + padY;

		if (size.x < minReqW)
			this.flags |= AD_MEDIA_WIDTH_OVERFLOW;
		if (size.y < minReqH)
			this.flags |= AD_MEDIA_HEIGHT_OVERFLOW;
		Logger.debug(
			TAG,
			"%s - resolved ad media render size: %sx%s, min-req-size=%sx%s, overflow=%s",
			this,
			size.x,
			size.y,
			minReqW,
			minReqH,
			this.isAdMediaOverflow()
		);
		return size;
	}

	/**
	 * Measure ad media and overlay.
	 * <p>Upon return overflow {@linkplain #flags flags} are updated to reflect whether the
	 * supported ad media size is sufficient for ad media.
	 */
	private void measureViews() {
		View media = this.requireAdMediaView();
		View overlay = this.adMediaOverlayView();
		Point size = this.calculateAdMediaRenderSize();

		/*
		 * if an exact measurement was provided along a dimension *or* relative sizing was
		 * supplied, then the media must use our measurement exactly; otherwise, it's up to
		 * the media to determine what it wants
		 */
		media.measure(
			MeasureSpec.makeMeasureSpec(
				this.pxOfDp(size.x),
				this.requestedAdMediaSize.exactWidthDp() == 0 &&
				!this.requestedAdMediaSize.hasRelative() ? MeasureSpec.AT_MOST :
				MeasureSpec.EXACTLY
			),
			MeasureSpec.makeMeasureSpec(
				this.pxOfDp(size.y),
				this.requestedAdMediaSize.exactHeightDp() == 0 &&
				!this.requestedAdMediaSize.hasRelative() ? MeasureSpec.AT_MOST :
				MeasureSpec.EXACTLY
			)
		);
		if (overlay != null) {
			//noinspection DataFlowIssue
			overlay.measure(
				MeasureSpec.makeMeasureSpec(
					this.pxOfDp(this.supportedAdMediaSize.maxWidthDp()),
					MeasureSpec.AT_MOST
				),
				MeasureSpec.makeMeasureSpec(
					this.pxOfDp(this.supportedAdMediaSize.maxHeightDp()),
					MeasureSpec.AT_MOST
				)
			);
		}
	}

	@Override
	protected final void onMeasure(int wSpec, int hSpec) {
		int padX = ViewUtils.inlinePaddingOf(this);
		int padY = ViewUtils.blockPaddingOf(this);
		int w = MeasureSpec.getMode(wSpec) == MeasureSpec.UNSPECIFIED ? -1 :
			this.dpOfPx(Math.max(MeasureSpec.getSize(wSpec) - padX, 0));
		int h = MeasureSpec.getMode(hSpec) == MeasureSpec.UNSPECIFIED ? -1 :
			this.dpOfPx(Math.max(MeasureSpec.getSize(hSpec) - padY, 0));
		View media = this.adMediaView();
		View overlay = this.adMediaOverlayView();

		Logger.debug(TAG, "%s - onMeasure(%s, %s), w=%s h=%s", this, wSpec, hSpec, w, h);
		if ((this.updateSupportedAdMediaSize(w, h) && media != null) || (
			(media != null && media.isLayoutRequested()) ||
			(overlay != null && overlay.isLayoutRequested())
		)) {
			this.measureViews();
		}

		int wState =
			(this.flags & AD_MEDIA_WIDTH_OVERFLOW) != 0 ? View.MEASURED_STATE_TOO_SMALL : 0;
		int hState =
			(this.flags & AD_MEDIA_HEIGHT_OVERFLOW) != 0 ? View.MEASURED_STATE_TOO_SMALL : 0;

		//noinspection DataFlowIssue
		w = this.supportedAdMediaSize.maxWidthDp();
		h = this.supportedAdMediaSize.maxHeightDp();
		if (w < this.explicitSupportedAdMediaSize.minWidthDp())
			wState |= View.MEASURED_STATE_TOO_SMALL;
		if (h < this.explicitSupportedAdMediaSize.minHeightDp())
			hState |= View.MEASURED_STATE_TOO_SMALL;
		super.setMeasuredDimension(
			View.resolveSizeAndState(
				Math.max(this.pxOfDp(w) + padX, super.getSuggestedMinimumWidth()),
				wSpec,
				wState
			),
			View.resolveSizeAndState(
				Math.max(this.pxOfDp(h) + padY, super.getSuggestedMinimumHeight()),
				hSpec,
				hState
			)
		);
	}

	@Override
	protected final void onLayout(boolean changed, int left, int top, int right, int bottom) {
		View media = this.adMediaView();
		View overlay = this.adMediaOverlayView();
		int w = Math.max(right - left, 0);
		int h = Math.max(bottom - top, 0);
		int mediaW, mediaH;

		Logger.debug(
			TAG,
			"%s - onLayout(%s, %s, %s, %s, %s), w=%s h=%s",
			this,
			changed,
			left,
			top,
			right,
			bottom,
			w,
			h
		);
		// this is our actual size, so update the supported ad media size with it
		if (
			this.updateSupportedAdMediaSize(
				this.dpOfPx(Math.max(w - ViewUtils.inlinePaddingOf(this), 0)),
				this.dpOfPx(Math.max(h - ViewUtils.blockPaddingOf(this), 0))
			) &&
			media != null
		) {
			Point mediaSize = this.calculateAdMediaRenderSize();

			mediaW = this.pxOfDp(mediaSize.x);
			mediaH = this.pxOfDp(mediaSize.y);
		} else if (media == null) {
			return;
		} else {
			mediaW = media.getMeasuredWidth();
			mediaH = media.getMeasuredHeight();
		}

		int layoutLeft = super.getPaddingLeft();
		int layoutRight = w - super.getPaddingRight();
		int layoutTop = super.getPaddingTop();
		int layoutBottom = h - super.getPaddingBottom();

		// media is centered
		int mediaLeft = layoutLeft + (layoutRight - layoutLeft - mediaW) / 2;
		int mediaTop = layoutTop + (layoutBottom - layoutTop - mediaH) / 2;

		media.layout(mediaLeft, mediaTop, mediaLeft + mediaW, mediaTop + mediaH);

		if (overlay == null)
			return;

		// overlay sticks to the top and, is left or right aligned based on layout direction
		int overlayW = overlay.getMeasuredWidth();
		int overlayH = overlay.getMeasuredHeight();
		int overlayLeft =
			super.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL ?
			layoutRight - overlayW : layoutLeft;

		overlay.layout(overlayLeft, layoutTop, overlayLeft + overlayW, layoutTop + overlayH);
	}

	/**
	 * Update pixel density, if required.
	 */
	@SuppressWarnings({ "RedundantSuppression", "deprecation" })
	private void updatePixelDensity() {
		Display display = super.getDisplay();
		float density = 0.f;

		if (
			Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
			display == AndroidContexts.displayOf(super.getContext())
		) {
			// context is referencing the same display, use `WindowManager` since its most accurate
			WindowManager wman = AndroidContexts.systemServiceOf(
				super.getContext(),
				WindowManager.class,
				Context.WINDOW_SERVICE
			);

			if (wman != null)
				density = wman.getCurrentWindowMetrics().getDensity();
		}
		if (display != null && density == 0.f) {
			DisplayMetrics metrics = new DisplayMetrics();

			display.getRealMetrics(metrics);
			density = metrics.density;
		}
		if (density == 0.f)
			density = super.getResources().getDisplayMetrics().density;
		if (density != this.pixelDensity) {
			this.pixelDensity = density;
			if (!super.isInLayout())
				super.requestLayout();
		}
	}

	@Override
	@CallSuper
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		// dips may have changed
		this.updatePixelDensity();
	}

	@Override
	@CallSuper
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		this.supportedAdMediaSize = null;
	}

	@Override
	@CallSuper
	public void onConfigurationChanged(Configuration cfg) {
		super.onConfigurationChanged(cfg);
		// dips or orientation may have changed
		this.updatePixelDensity();
		super.requestLayout();
	}

	@Override
	public final void setFilterTouchesWhenObscured(boolean enabled) {
		super.setFilterTouchesWhenObscured(true);
	}

	@Override
	public final ViewGroupOverlay getOverlay() {
		throwUnsupported();
		return super.getOverlay();
	}

	@Override
	public void addView(View child, int idx, LayoutParams params) {
		throwUnsupported();
	}

	@Override
	public void addView(View child, int idx) {
		throwUnsupported();
	}

	@Override
	public void addView(View child, LayoutParams params) {
		throwUnsupported();
	}

	@Override
	public void addView(View child, int w, int h) {
		throwUnsupported();
	}

	@Override
	public void addView(View child) {
		throwUnsupported();
	}

	@Override
	public void removeViewInLayout(View child) {
		throwUnsupported();
	}

	@Override
	public void removeView(View child) {
		throwUnsupported();
	}

	@Override
	public void removeViewAt(int idx) {
		throwUnsupported();
	}

	@Override
	public void removeViewsInLayout(int i, int j) {
		throwUnsupported();
	}

	@Override
	public void removeViews(int i, int j) {
		throwUnsupported();
	}

	@Override
	public void removeAllViews() {
		throwUnsupported();
	}

	@Override
	public void removeAllViewsInLayout() {
		throwUnsupported();
	}
}
