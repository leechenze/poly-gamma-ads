// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.ads;

import static androidx.annotation.Dimension.DP;

import androidx.annotation.Dimension;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import org.polygamma.android.origin.util.Preconditions;

/**
 * Ad size specification.
 * <p>Ad sizes are defined in terms of constraints along the width and height dimensions.
 * Constraints may apply to a single or both dimensions. Depending on constraints, a dimension
 * may be flexible or exact. Exact dimensions are dimensions which <i>must</i> be equal. Flexible
 * dimensions are dimensions which <i>may</i> be equal.
 * <p>A dimension is exact if its maximum constraint is non-zero and, its minimum and maximum
 * constraints are equal. For example, if {@link #minWidthDp()} and {@link #maxWidthDp()} are
 * equal, and {@link #maxWidthDp()} is non-zero, then the width dimension is exact.
 *
 * @since 1.2
 */
public final class AdSize {

	/**
	 * Empty flexible ad size.
	 */
	static final AdSize EMPTY = new AdSize(0, 0, 0, 0, 0, 0);

	/**
	 * Calculate greatest common divisor of two integers.
	 *
	 * @param u first integer
	 * @param v second integer
	 * @return greatest common divisor or, {@code 0} if both {@code u} and {@code v} are {@code 0}
	 */
	@VisibleForTesting
	static int gcd(int u, int v) {
		// Stein's algorithm
		if (u == 0)
			return v;
		if (v == 0)
			return u;

		// 2,3) gcd(2^iu,2^jv) = 2^k gcd(u,v)
		int i = Integer.numberOfTrailingZeros(u);
		int j = Integer.numberOfTrailingZeros(v);
		int k = Math.min(i, j);

		u >>>= i;
		v >>>= j;
		while (true) {
			assert (u % 2) == 1 && (v % 2) == 1;

			if (u > v) {
				int pu = u;

				u = v;
				v = pu;
			}

			// 4) gcd(u,v) = gcd(u,v-u) as u <= v and u,v are odd
			v -= u;
			if (v == 0) {
				// 1) gcd(u,0) = u
				return u << k;
			}
			// 3) gcd(u,2^jv) = gcd(u,v) as u odd
			v >>>= Integer.numberOfTrailingZeros(v);
		}
	}

	/**
	 * Calculate a dimension given an aspect ratio and an opposite dimension.
	 *
	 * @param dRatio relative size of dimension to calculate
	 * @param oRatio relative size of opposite dimension
	 * @param o opposite dimension measurement
	 * @return calculated measurement of dimension
	 */
	private static int dimensionOfAspectRatio(int dRatio, int oRatio, int o) {
		return oRatio == 0 ? 0 : (o * dRatio) / oRatio;
	}

	/**
	 * Calculate width given an aspect ratio and height.
	 *
	 * @param wRatio relative width
	 * @param hRatio relative height
	 * @param h height
	 * @return width
	 */
	@VisibleForTesting
	static int widthOfAspectRatio(int wRatio, int hRatio, int h) {
		return dimensionOfAspectRatio(wRatio, hRatio, h);
	}

	/**
	 * Calculate height given an aspect ratio and width.
	 *
	 * @param wRatio relative width
	 * @param hRatio relative height
	 * @param w width
	 * @return height
	 */
	@VisibleForTesting
	static int heightOfAspectRatio(int wRatio, int hRatio, int w) {
		return dimensionOfAspectRatio(hRatio, wRatio, w);
	}

	/**
	 * Construct ad size given explicit constraints.
	 *
	 * @param wRatio relative width, expressed as a ratio, or {@code 0}
	 * @param hRatio relative height, expressed as a ratio, or {@code 0}
	 * @param minW minimum width, or {@code 0}
	 * @param minH minimum height, or {@code 0}
	 * @param maxW maximum width, or {@code 0}
	 * @param maxH maximum height, or {@code 0}
	 * @return resulting size
	 */
	static AdSize of(
		int wRatio,
		int hRatio,
		@Dimension(unit = DP) int minW,
		@Dimension(unit = DP) int minH,
		@Dimension(unit = DP) int maxW,
		@Dimension(unit = DP) int maxH
	) {
		return (wRatio | hRatio | minW | minH | maxW | maxH) == 0 ? EMPTY : new AdSize(
			wRatio, hRatio,
			minW, minH,
			maxW, maxH
		);
	}

	/**
	 * Construct a normalized ad size given constraints and actual media size.
	 * <p>If {@code wRatio} and {@code hRatio} are {@code 0}, then the aspect ratio of actual
	 * media size, maximum size, or minimum size will be used, in that order; otherwise, if either
	 * {@code wRatio} or {@code hRatio} is less than {@code 1}, it is set to {@code 1}. The
	 * resulting ad size will be normalized to the resulting aspect ratio if, and only if, the
	 * resulting aspect ratio is non-zero.
	 *
	 * @param wRatio relative width or {@code 0}
	 * @param hRatio relative height or {@code 0}
	 * @param minW minimum possible width or {@code 0} if no constraint
	 * @param minH minimum possible height or {@code 0} if no constraint
	 * @param maxW maximum possible width or {@code 0} if no constraint
	 * @param maxH maximum possible height or {@code 0} if no constraint
	 * @param w actual media width or {@code 0} if flexible
	 * @param h actual media height or {@code 0} if flexible
	 * @return normalized size
	 */
	static AdSize ofNormalized(
		int wRatio,
		int hRatio,
		@Dimension(unit = DP) int minW,
		@Dimension(unit = DP) int minH,
		@Dimension(unit = DP) int maxW,
		@Dimension(unit = DP) int maxH,
		@Dimension(unit = DP) int w,
		@Dimension(unit = DP) int h
	) {
		// set aspect ratio if not present
		if (wRatio != 0 || hRatio != 0) {
			wRatio = Math.max(wRatio, 1);
			hRatio = Math.max(hRatio, 1);
		} else {
			int dW, dH;

			if (
				((dW = w) != 0 && (dH = h) != 0) ||
				((dW = maxW) != 0 && (dH = maxH) != 0) ||
				((dW = minW) != 0 && (dH = minH) != 0)
			) {
				int d = gcd(dW, dH);

				wRatio = dW / d;
				hRatio = dH / d;
			}
		}

		// if aspect ratio could not be calculated, return as is
		if (wRatio == 0 || hRatio == 0)
			return of(0, 0, minW, minH, maxW == 0 ? w : maxW, maxH == 0 ? h : maxH);

		// normalize minimum bounds to aspect ratio
		if (minW != 0 && minH != 0) {
			int newW = widthOfAspectRatio(wRatio, hRatio, minH);
			int newH = heightOfAspectRatio(wRatio, hRatio, minW);

			if (newW <= minW)
				minH = newH;
			else
				minW = newW;
		} else if (minW == 0) {
			minW = widthOfAspectRatio(wRatio, hRatio, minH);
		} else {
			minH = heightOfAspectRatio(wRatio, hRatio, minW);
		}

		// normalize maximum bounds to aspect ratio
		if (maxW == 0 && maxH == 0) {
			maxW = w;
			maxH = h;
		}
		if (maxW != 0 && maxH != 0) {
			int newW = widthOfAspectRatio(wRatio, hRatio, maxH);
			int newH = heightOfAspectRatio(wRatio, hRatio, maxW);

			if (newW > maxW)
				maxH = newH;
			else
				maxW = newW;
		} else if (maxW == 0) {
			maxW = widthOfAspectRatio(wRatio, hRatio, maxH);
		} else {
			maxH = heightOfAspectRatio(wRatio, hRatio, maxW);
		}
		return of(wRatio, hRatio, minW, minH, maxW, maxH);
	}

	/**
	 * Construct a size with an exact width and height constraint.
	 * <p>The resulting size is <i>exact</i>, in other words, its minimum and maximum constraints
	 * are equal, and its relative {@linkplain #widthRatio() width} and {@linkplain #heightRatio()
	 * height} will be equal to the aspect ratio of {@code w} and {@code h}.
	 *
	 * @param w exact width, in device independent pixels
	 * @param h exact height, in device independent pixels
	 * @return resulting size
	 * @throws IllegalArgumentException {@code w} or {@code h} is less than {@code 0}
	 * @since 1.2
	 * @see #isExact()
	 */
	public static AdSize ofExact(@Dimension(unit = DP) int w, @Dimension(unit = DP) int h) {
		Preconditions.checkArgument(w > 0 && h > 0);

		int d = gcd(w, h);

		return of(w / d, h / d, w, h, w, h);
	}

	/**
	 * Construct a size with an exact width constraint and an optional minimum height constraint.
	 * <p>The resulting size is <i>exact</i> and <i>relative</i> along the width and height
	 * dimensions, respectively. In other words, the minimum and maximum width constraints are
	 * equal, there is no maximum height constraint, and a minimum height constraint is set if,
	 * and only if, {@code minH} is greater than {@code 0}. The resulting size will not have a
	 * relative size constraint.
	 *
	 * @param w exact width, in device independent pixels
	 * @param minH minimum height, in device independent pixels, or {@code 0} if no constraint
	 * @return resulting size
	 * @throws IllegalArgumentException {@code w} is less than {@code 1} or {@code minH} is
	 * negative
	 * @since 1.2
	 */
	public static AdSize
	ofExactWidth(@Dimension(unit = DP) int w, @Dimension(unit = DP) int minH) {
		Preconditions.checkArgument(w > 0);
		return of(0, 0, w, minH, w, 0);
	}

	/**
	 * Construct a size with an exact height constraint and an optional minimum width constraint.
	 * <p>The resulting size is <i>exact</i> and <i>relative</i> along the height and width
	 * dimensions, respectively. In other words, the minimum and maximum height constraints are
	 * equal, there is no maximum width constraint, and a minimum width constraint is set if,
	 * and only if, {@code minW} is greater than {@code 0}. The resulting size will not have a
	 * relative size constraint.
	 *
	 * @param h exact height, in device independent pixels
	 * @param minW minimum width, in device independent pixels, or {@code 0} if no constraint
	 * @return resulting size
	 * @throws IllegalArgumentException {@code h} is less than {@code 1} or {@code minW} is
	 * negative
	 * @since 1.2
	 */
	public static AdSize
	ofExactHeight(@Dimension(unit = DP) int h, @Dimension(unit = DP) int minW) {
		Preconditions.checkArgument(h > 0);
		return of(0, 0, minW, h, 0, h);
	}

	/**
	 * Construct a flexible size.
	 *
	 * @param wRatio relative width, expressed as a ratio, or {@code 0} if no constraint
	 * @param hRatio relative height, expressed as a ratio, or {@code 0} if no constraint
	 * @param minW minimum width, in device independent pixels, or {@code 0} if no constraint
	 * @param minH minimum height, in device independent pixels, or {@code 0} if no constraint
	 * @param maxW maximum width, in device independent pixels, or {@code 0} if no constraint
	 * @param maxH maximum height, in device independent pixels, or {@code 0} if no constraint
	 * @return resulting size
	 * @throws IllegalArgumentException {@code wRatio}, {@code hRatio}, {@code minW}, {@code
	 * minH}, {@code maxW}, or {@code maxH} is negative, {@code maxW} and {@code maxH} are
	 * non-zero and equal to {@code minW} and {@code minH}, respectively, or, {@code wRatio}
	 * or {@code hRatio} is non-zero and {@code hRatio} or {@code wRatio} is zero, respectively
	 */
	public static AdSize ofFlexible(
		int wRatio,
		int hRatio,
		@Dimension(unit = DP) int minW,
		@Dimension(unit = DP) int minH,
		@Dimension(unit = DP) int maxW,
		@Dimension(unit = DP) int maxH
	) {
		Preconditions.checkArgument((maxW == 0 || maxW != minW) && (maxH == 0 || maxH != minH));
		return of(wRatio, hRatio, minW, minH, maxW, maxH);
	}

	private final int widthRatio;
	private final int heightRatio;
	private final @Dimension(unit = DP) int minWidthDp;
	private final @Dimension(unit = DP) int minHeightDp;
	private final @Dimension(unit = DP) int maxWidthDp;
	private final @Dimension(unit = DP) int maxHeightDp;

	private AdSize(
		int wRatio,
		int hRatio,
		@Dimension(unit = DP) int minW,
		@Dimension(unit = DP) int minH,
		@Dimension(unit = DP) int maxW,
		@Dimension(unit = DP) int maxH
	) {
		Preconditions.checkArgument(
			((wRatio == 0 && hRatio == 0) || (wRatio > 0 && hRatio > 0)) &&
			minW >= 0 && minH >= 0 &&
			maxW >= 0 && maxH >= 0
		);
		this.widthRatio = wRatio;
		this.heightRatio = hRatio;
		this.minWidthDp = minW;
		this.minHeightDp = minH;
		this.maxWidthDp = maxW;
		this.maxHeightDp = maxH;
	}

	/**
	 * Test whether size is exact along the width <i>or</i> height dimensions.
	 * <p>A size is exact when maximum {@linkplain #maxWidthDp() width} <i>or</i> {@linkplain
	 * #maxHeightDp() height} are non-zero and equal to minimum width or height, respectively.
	 *
	 * @return {@code true} if, and only if, size is exact; otherwise, {@code false} if size is
	 * flexible
	 * @since 1.2
	 */
	public boolean isExact() {
		return (
			(this.minWidthDp == this.maxWidthDp && this.maxWidthDp > 0) ||
			(this.minHeightDp == this.maxHeightDp && this.maxHeightDp > 0)
		);
	}

	/**
	 * Test whether size is empty.
	 * <p>A size is empty when it has no constraints.
	 *
	 * @return {@code true} if, and only if, size has no constraints
	 * @since 1.2
	 */
	public boolean isEmpty() {
		return this == EMPTY;
	}

	/**
	 * Relative width, expressed as a ratio.
	 *
	 * @return width ratio or {@code 0} if no relative size constraint
	 * @since 1.2
	 */
	public int widthRatio() {
		return this.widthRatio;
	}

	/**
	 * Relative height, expressed as a ratio.
	 *
	 * @return height ratio or {@code 0} if no relative size constraint
	 * @since 1.2
	 */
	public int heightRatio() {
		return this.heightRatio;
	}

	/**
	 * Test whether size has relative size constraint.
	 *
	 * @return {@code true} if, and only if, size has relative size constraint
	 */
	boolean hasRelative() {
		return this.widthRatio != 0 && this.heightRatio != 0;
	}

	/**
	 * Resolve width measurement, according to relative size constraint, given a height
	 * measurement.
	 *
	 * @param h height measurement
	 * @return resulting width measurement
	 */
	int resolveRelativeWidth(int h) {
		return widthOfAspectRatio(this.widthRatio, this.heightRatio, h);
	}

	/**
	 * Resolve height measurement, according to relative size constraint, given a width
	 * measurement.
	 *
	 * @param w width measurement
	 * @return resulting height measurement
	 */
	int resolveRelativeHeight(int w) {
		return heightOfAspectRatio(this.widthRatio, this.heightRatio, w);
	}

	/**
	 * Minimum width, in device independent pixels.
	 *
	 * @return width or {@code 0} if no minimum width constraint
	 * @since 1.2
	 */
	public @Dimension(unit = DP) int minWidthDp() {
		return this.minWidthDp;
	}

	/**
	 * Minimum height, in device independent pixels.
	 *
	 * @return height or {@code 0} if no minimum height constraint
	 * @since 1.2
	 */
	public @Dimension(unit = DP) int minHeightDp() {
		return this.minHeightDp;
	}

	/**
	 * Maximum width, in device independent pixels.
	 *
	 * @return width or {@code 0} if no maximum width constraint
	 * @since 1.2
	 */
	public @Dimension(unit = DP) int maxWidthDp() {
		return this.maxWidthDp;
	}

	/**
	 * Maximum height, in device independent pixels.
	 *
	 * @return height or {@code 0} if no maximum height constraint
	 * @since 1.2
	 */
	public @Dimension(unit = DP) int maxHeightDp() {
		return this.maxHeightDp;
	}

	/**
	 * Exact width, in device independent pixels.
	 *
	 * @return {@code 0} if size has no exact width constraint; otherwise, exact width
	 * @since 1.2
	 * @see #isExact()
	 */
	public @Dimension(unit = DP) int exactWidthDp() {
		return this.maxWidthDp == this.minWidthDp ? this.maxWidthDp : 0;
	}

	/**
	 * Exact height, in device independent pixels.
	 *
	 * @return {@code 0} if size has no exact height constraint; otherwise, exact height
	 * @since 1.2
	 * @see #isExact()
	 */
	public @Dimension(unit = DP) int exactHeightDp() {
		return this.maxHeightDp == this.minHeightDp ? this.maxHeightDp : 0;
	}

	/**
	 * Construct new {@linkplain #ofNormalized(int, int, int, int, int, int, int, int) normalized}
	 * size with {@code this} and actual size dimensions.
	 *
	 * @param w actual width
	 * @param h actual height
	 * @return normalized size
	 */
	AdSize withActual(@Dimension(unit = DP) int w, @Dimension(unit = DP) int h) {
		return ofNormalized(
			this.widthRatio, this.heightRatio,
			this.minWidthDp, this.minHeightDp,
			this.maxWidthDp, this.maxHeightDp,
			w, h
		);
	}

	@Override
	public int hashCode() {
		return
			Integer.hashCode(this.maxWidthDp) ^
			Integer.hashCode(this.maxHeightDp) ^
			Integer.hashCode(this.minWidthDp) ^
			Integer.hashCode(this.minHeightDp) ^
			Integer.hashCode(this.widthRatio) ^
			Integer.hashCode(this.heightRatio);
	}

	@Override
	public boolean equals(@Nullable Object other) {
		if (!(other instanceof AdSize))
			return false;

		AdSize that = (AdSize) other;

		return (
			this.maxWidthDp == that.maxWidthDp &&
			this.maxHeightDp == that.maxHeightDp &&
			this.minWidthDp == that.minWidthDp &&
			this.minHeightDp == that.minHeightDp &&
			this.widthRatio == that.widthRatio &&
			this.heightRatio == that.heightRatio
		);
	}
}
