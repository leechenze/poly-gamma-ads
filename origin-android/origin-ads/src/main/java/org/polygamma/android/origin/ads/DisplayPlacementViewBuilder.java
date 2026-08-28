// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.ads;

import static org.polygamma.android.origin.ads.R.styleable
	.DisplayPlacementView_origin_displayPlacement_id;
import static org.polygamma.android.origin.ads.R.styleable
	.DisplayPlacementView_origin_displayPlacement_modality;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.text.TextUtils;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.annotation.UiContext;
import androidx.annotation.UiThread;

import org.polygamma.android.origin.util.Preconditions;

/**
 * Display placement {@linkplain DisplayPlacementView renderer} builder.
 *
 * @since 1.2
 * @see DisplayPlacementView#ofBuilder(Context)
 */
public final class DisplayPlacementViewBuilder {

	/**
	 * Construct builder initialized from attributes.
	 *
	 * @param ctxt owning context
	 * @param attrs attributes to initialize from
	 * @return resulting builder instance
	 * @throws IllegalArgumentException {@code attrs} is malformed:
	 * <ul>
	 *     <li>{@code origin_displayPlacement_id} is defined to an invalid value</li>
	 *     <li>
	 *         {@code origin_displayPlacement_modality} is defined and {@code ctxt} is not an
	 *         {@linkplain Activity activity}
	 *     </li>
	 * </ul>
	 */
	static DisplayPlacementViewBuilder ofAttributes(@UiContext Context ctxt, AttributeSet attrs) {
		DisplayPlacementViewBuilder rv = new DisplayPlacementViewBuilder(ctxt);
		@SuppressWarnings("resource")
		TypedArray ar = ctxt.obtainStyledAttributes(attrs, R.styleable.DisplayPlacementView);

		try {
			if (ar.hasValue(DisplayPlacementView_origin_displayPlacement_id))
				rv.placementId(ar.getString(DisplayPlacementView_origin_displayPlacement_id));
			if (ar.hasValue(DisplayPlacementView_origin_displayPlacement_modality)) {
				rv.modality(ar.getInteger(
					DisplayPlacementView_origin_displayPlacement_modality,
					DisplayPlacementView.MODALITY_NONE
				));
			}
		} finally {
			ar.recycle();
		}
		return rv;
	}

	final @UiContext Context context;
	@Nullable String placementId;
	@DisplayPlacementView.ModalityMode int modality;

	/**
	 * Construct a new empty renderer builder.
	 *
	 * @param ctxt owning context
	 */
	DisplayPlacementViewBuilder(@UiContext Context ctxt) {
		this.context = Preconditions.checkNotNull(ctxt);
		this.modality = DisplayPlacementView.MODALITY_NONE;
	}

	/**
	 * Set renderer's placement id.
	 * <p>When invoked, the resulting renderer will be {@linkplain
	 * DisplayPlacementView#bindToPlacement(String) bound} to the placement identified by {@code
	 * id}.
	 *
	 * @param id id of placement to bind renderer to
	 * @return {@code this}
	 * @throws IllegalArgumentException {@code id} is invalid
	 * @since 1.2
	 */
	public DisplayPlacementViewBuilder placementId(String id) {
		Preconditions.checkArgument(!TextUtils.isEmpty(id), "placement id cannot be empty");
		this.placementId = id;
		return this;
	}

	/**
	 * Set modality mode of renderer.
	 *
	 * @param mode modality mode
	 * @return {@code this}
	 * @throws IllegalArgumentException {@code mode} is not {@linkplain
	 * DisplayPlacementView#MODALITY_NONE none} and context is not an {@linkplain Activity
	 * activity}
	 * @since 1.2
	 */
	public DisplayPlacementViewBuilder modality(@DisplayPlacementView.ModalityMode int mode) {
		Preconditions.checkState(
			mode== DisplayPlacementView.MODALITY_NONE ||
			this.context instanceof Activity,
			"non-none modality unsupported with non-activity context"
		);
		this.modality = mode;
		return this;
	}

	/**
	 * Build resulting renderer.
	 *
	 * @return renderer instance
	 * @since 1.2
	 */
	@UiThread
	public DisplayPlacementView build() {
		return new DisplayPlacementView(this, null);
	}
}
