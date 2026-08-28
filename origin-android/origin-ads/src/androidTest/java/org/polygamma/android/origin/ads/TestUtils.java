// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.ads;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.graphics.Insets;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.WindowMetrics;

import androidx.annotation.WorkerThread;
import androidx.core.util.Consumer;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Assert;

import java.util.concurrent.CountDownLatch;

/**
 * Test utility definitions.
 */
public class TestUtils {

	/**
	 * Execute command on main thread.
	 *
	 * @param cmd command to execute
	 */
	public static void runOnMainSync(Runnable cmd) {
		InstrumentationRegistry.getInstrumentation().runOnMainSync(cmd);
	}

	/**
	 * Launch an activity.
	 *
	 * @param <T> activity type
	 * @param klass class of activity to launch
	 * @return resulting activity
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Activity> T launchActivity(Class<T> klass) {
		Instrumentation instr = InstrumentationRegistry.getInstrumentation();
		Activity rv = instr.startActivitySync(
			(new Intent(instr.getContext(), klass))
				.setAction(Intent.ACTION_MAIN)
				.addCategory(Intent.CATEGORY_LAUNCHER)
				.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
		);

		if (!klass.isInstance(rv)) {
			instr.runOnMainSync(rv::finish);
			throw new AssertionError(String.format(
				"failed to launch activity, expected %s got %s",
				klass,
				rv
			));
		}
		return (T) rv;
	}

	/**
	 * Launch and perform action on activity.
	 *
	 * @param <T> activity type
	 * @param klass class of activity to launch
	 * @param cons action to perform on activity
	 */
	public static <T extends Activity> void withActivity(Class<T> klass, Consumer<T> cons) {
		T activity = launchActivity(klass);

		try {
			cons.accept(activity);
		} finally {
			runOnMainSync(activity::finish);
		}
	}

	/**
	 * Wait until layout pass completes for a view.
	 * <p>If {@code view} has no layout pass scheduled, this simply returns.
	 *
	 * @param view view to wait for layout pass completion for
	 */
	@WorkerThread
	public static void awaitLayout(View view) {
		CountDownLatch latch = new CountDownLatch(1);
		ViewTreeObserver.OnGlobalLayoutListener listener = latch::countDown;

		runOnMainSync(() -> {
			view.getViewTreeObserver().addOnGlobalLayoutListener(listener);
			if (!view.isLayoutRequested())
				latch.countDown();
		});

		try {
			latch.await();
		} catch (InterruptedException cause) {
			throw new AssertionError(cause);
		} finally {
			runOnMainSync(() -> {
				ViewTreeObserver vto = view.getViewTreeObserver();

				if (vto.isAlive())
					vto.removeOnGlobalLayoutListener(listener);
			});
		}
	}

	/**
	 * Assert a view is centered within its parent.
	 *
	 * @param view view to assert position of
	 */
	public static void assertViewCentered(View view) {
		Assert.assertTrue(view.getParent() instanceof View);

		View parent = (View) view.getParent();
		Rect bounds = new Rect();

		view.getHitRect(bounds);

		int viewW = view.getWidth();
		int viewH = view.getHeight();
		int expectLeft = parent.getPaddingLeft() + (
			parent.getWidth() -
			ViewUtils.inlinePaddingOf(parent) -
			viewW
		) / 2;
		int expectTop = parent.getPaddingTop() + (
			parent.getHeight() -
			ViewUtils.blockPaddingOf(parent) -
			viewH
		) / 2;

		Assert.assertEquals(expectLeft, bounds.left);
		Assert.assertEquals(expectTop, bounds.top);
		Assert.assertEquals(expectLeft + viewW, bounds.right);
		Assert.assertEquals(expectTop + viewH, bounds.bottom);
	}

	private TestUtils() {
	}
}
