// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.ads;

import android.os.Build;
import android.util.Pair;
import android.view.View;
import android.view.Window;

import androidx.annotation.Nullable;
import androidx.annotation.Px;

import org.polygamma.android.origin.util.Logger;
import org.polygamma.android.origin.util.Preconditions;

import java.lang.reflect.Field;

/**
 * {@link View} utility definitions.
 */
@SuppressWarnings("unchecked")
class ViewUtils {

	private static final String TAG = ViewUtils.class.getSimpleName();

	private static final @Nullable Field DECOR_VIEW_WINDOW_FIELD;

	static {
		Field decorViewWndField = null;

		for (Pair<String, String> spec : (Pair<String, String>[]) new Pair<?, ?>[] {
			// https://android.googlesource.com/platform/frameworks/base/+/android-7.0.0_r1/core/java/com/android/internal/policy/DecorView.java
			Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ?
				new Pair<>("com.android.internal.policy.DecorView", "mWindow") : null,
			// https://android.googlesource.com/platform/frameworks/base/+/android-6.0.0_r1/core/java/com/android/internal/policy/PhoneWindow.java
			Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ?
				new Pair<>("com.android.internal.policy.PhoneWindow$DecorView", "this$0") :
				null,
			// https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-4.4_r0.7/policy/src/com/android/internal/policy/impl/PhoneWindow.java
			new Pair<>("com.android.internal.policy.impl.PhoneWindow$DecorView", "this$0")
		}) {
			if (spec == null)
				continue;
			try {
				Field field = Class.forName(spec.first).getDeclaredField(spec.second);

				Preconditions.checkState(
					Window.class.isAssignableFrom(field.getType()),
					"field is not `%s`",
					Window.class
				);
				field.setAccessible(true);
				decorViewWndField = field;
				break;
			} catch (Throwable cause) {
				Logger.warn(TAG, "failed to resolve `%s::%s`", spec.first, spec.second, cause);
			}
		}

		if (decorViewWndField == null)
			Logger.warn(TAG, "failed to resolve `DecorView::window` field");
		else
			Logger.debug(TAG, "resolved `DecorView::window`: %s", decorViewWndField);
		DECOR_VIEW_WINDOW_FIELD = decorViewWndField;
	}

	/**
	 * Resolve window a view is attached to.
	 *
	 * @param view view to resolve window of
	 * @return window in which {@code view} is rendered, if any
	 */
	static @Nullable Window windowOf(View view) {
		if (DECOR_VIEW_WINDOW_FIELD == null)
			return null;

		View root = Preconditions.checkNotNullElse(view.getRootView(), view);

		if (!DECOR_VIEW_WINDOW_FIELD.getDeclaringClass().isInstance(root))
			return null;
		try {
			return (Window) DECOR_VIEW_WINDOW_FIELD.get(root);
		} catch (IllegalAccessException cause) {
			Logger.warn(TAG, "failed to retrieve window of %s, root=%s", view, root, cause);
			return null;
		}
	}

	/**
	 * View padding along {@code x} axis.
	 *
	 * @param view view to calculate padding of
	 * @return padding
	 */
	static @Px int inlinePaddingOf(View view) {
		return view.getPaddingLeft() + view.getPaddingRight();
	}

	/**
	 * View padding along {@code y} axis.
	 *
	 * @param view view to calculate padding of
	 * @return padding
	 */
	static @Px int blockPaddingOf(View view) {
		return view.getPaddingTop() + view.getPaddingBottom();
	}

	private ViewUtils() {
	}
}
