// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.ads;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.UiContext;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * {@link AdMediaLayout} tests.
 */
@RunWith(AndroidJUnit4.class)
public class AdMediaLayoutTest {

	private static final class TestAdLayout extends AdMediaLayout {
		final LinkedBlockingQueue<AdSize> supportedAdMediaSizes;
		TextView expectAdMediaView;

		TestAdLayout(@UiContext Context ctxt, @Nullable AttributeSet attrs) {
			super(ctxt, attrs);
			this.supportedAdMediaSizes = new LinkedBlockingQueue<>();
		}

		void setTestAdMediaView(AdSize size) {
			if (this.expectAdMediaView == null) {
				this.expectAdMediaView = new TextView(super.getContext());
				this.expectAdMediaView.setIncludeFontPadding(false);
				this.expectAdMediaView.setLineSpacing(1, 1);
				this.expectAdMediaView.setSingleLine();
				this.expectAdMediaView.setTypeface(Typeface.MONOSPACE);
				this.expectAdMediaView.setText("Test Ad Media View");
			} else {
				super.clearAdMediaView();
			}
			super.setAdMediaView(null, this.expectAdMediaView, size);
		}

		@Override
		void onSupportedAdMediaSize(AdSize size) {
			this.supportedAdMediaSizes.add(size);
		}
	}

	public static final class TestActivity extends Activity {
		LinearLayout container;

		@Override
		public void onCreate(@Nullable Bundle bnd) {
			super.onCreate(bnd);

			this.container = new LinearLayout(this);
			this.container.setOrientation(LinearLayout.VERTICAL);
			this.container.setLayoutParams(new ViewGroup.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT
			));
			super.setContentView(this.container);
		}
	}

	private TestActivity activity;

	@Before
	public void init() {
		activity = TestUtils.launchActivity(TestActivity.class);
	}

	@After
	public void destroy() {
		if (activity != null) {
			TestUtils.runOnMainSync(activity::finish);
			activity = null;
		}
	}

	/**
	 * Create a new test ad layout for {@linkplain #activity current} activity.
	 *
	 * @return resulting layout
	 */
	private TestAdLayout newTestAdLayout() {
		ArrayBlockingQueue<TestAdLayout> queue = new ArrayBlockingQueue<>(1);

		TestUtils.runOnMainSync(() -> {
			assertNotNull(activity);
			queue.offer(new TestAdLayout(activity, null));
		});

		TestAdLayout rv;

		try {
			rv = queue.take();
		} catch (InterruptedException cause) {
			throw new AssertionError(cause);
		}
		assertNotNull(rv);
		return rv;
	}

	/**
	 * Create and add a new test ad layout to {@linkplain #activity current} activity.
	 *
	 * @return resulting layout
	 */
	private TestAdLayout addNewTestAdLayout() {
		TestAdLayout rv = newTestAdLayout();

		TestUtils.runOnMainSync(() -> {
			assertNotNull(activity);
			activity.container.addView(rv);
		});
		return rv;
	}

	@Test
	public void testDpOfPxAndPxOfDp() {
		TestAdLayout layout = this.newTestAdLayout();
		DisplayMetrics metrics = activity.getResources().getDisplayMetrics();

		/*
		 * 1) not attached to a window - context display metrics should be used
		 * 2) attached to a window - display metrics should be used
		 */
		for (int i = 0; true; i++) {
			for (int dp : new int[]{0, 1, 64, 512, 8192, 65536, 524288}) {
				int px =
					Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, metrics));

				assertEquals(px, layout.pxOfDp(dp));
				assertEquals(dp, layout.dpOfPx(px));
			}
			if (i == 1)
				break;

			// when attached to a window, display metrics should be used
			TestUtils.runOnMainSync(() -> activity.container.addView(layout));
			TestUtils.awaitLayout(activity.container);

			metrics = new DisplayMetrics();
			assertNotNull(layout.getDisplay());
			layout.getDisplay().getRealMetrics(metrics);
		}
	}

	@Test
	public void testSetSupportedAdMediaSize() {
		AdSize got;
		TestAdLayout layout = this.newTestAdLayout();

		// until we're attached to a window, `setSupportedAdMediaSize()` should use screen size
		assertNull(layout.supportedAdMediaSize());
		// setting an empty size should do nothing
		TestUtils.runOnMainSync(() -> layout.setSupportedAdMediaSize(AdSize.EMPTY));
		assertNull(layout.supportedAdMediaSizes.poll());
		assertNull(layout.supportedAdMediaSize());

		// setting an exact size should use the exact size
		TestUtils.runOnMainSync(() -> layout.setSupportedAdMediaSize(AdSize.ofExact(300, 50)));

		got = layout.supportedAdMediaSizes.poll();
		assertNotNull(got);
		assertSame(layout.supportedAdMediaSize(), got);
		assertEquals(AdSize.ofExact(300, 50), got);

		/*
		 * setting a single exact dimension should use the exact measurement along that
		 * dimension and screen size along the opposite dimension
		 */
		TestUtils.runOnMainSync(() -> layout.setSupportedAdMediaSize(AdSize.ofExactWidth(300, 50)));

		got = layout.supportedAdMediaSizes.poll();
		assertNotNull(got);
		assertSame(layout.supportedAdMediaSize(), got);
		assertFalse(got.hasRelative());
		assertEquals(300, got.exactWidthDp());
		assertEquals(layout.dpOfPx(activity.container.getHeight()), got.maxHeightDp());
		assertEquals(50, got.minHeightDp());

		// now attach to a window
		TestUtils.runOnMainSync(() -> activity.container.addView(
			layout,
			new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.WRAP_CONTENT,
				LinearLayout.LayoutParams.WRAP_CONTENT
			)
		));
		TestUtils.awaitLayout(layout);

		assertNull(layout.supportedAdMediaSizes.poll());
		assertSame(layout.supportedAdMediaSize(), got);
		assertEquals(300, layout.dpOfPx(layout.getWidth()));
		assertEquals(activity.container.getHeight(), layout.getHeight());
		assertFalse(layout.isAdMediaOverflow());
		assertEquals(0, layout.getMeasuredState() & (
			ViewGroup.MEASURED_STATE_TOO_SMALL |
			(ViewGroup.MEASURED_STATE_MASK >> ViewGroup.MEASURED_HEIGHT_STATE_SHIFT)
		));

		// set an exact size, the actual size of the view should be what we set
		TestUtils.runOnMainSync(() -> layout.setSupportedAdMediaSize(AdSize.ofExact(250, 100)));
		TestUtils.awaitLayout(layout);

		got = layout.supportedAdMediaSizes.poll();
		assertNotNull(got);
		assertSame(layout.supportedAdMediaSize(), got);
		assertEquals(250, got.exactWidthDp());
		assertEquals(100, got.exactHeightDp());
		assertEquals(250, layout.dpOfPx(layout.getWidth()));
		assertEquals(100, layout.dpOfPx(layout.getHeight()));
		assertFalse(layout.isAdMediaOverflow());
		assertEquals(0, layout.getMeasuredState() & (
			ViewGroup.MEASURED_STATE_TOO_SMALL |
			(ViewGroup.MEASURED_STATE_MASK >> ViewGroup.MEASURED_HEIGHT_STATE_SHIFT)
		));

		// get rid of exact size, we should fill the container entirely
		TestUtils.runOnMainSync(() -> layout.setSupportedAdMediaSize(AdSize.EMPTY));
		TestUtils.awaitLayout(layout);

		assertNotNull(layout.supportedAdMediaSizes.poll());
		got = layout.supportedAdMediaSizes.poll();
		assertNotNull(got);
		assertSame(layout.supportedAdMediaSize(), got);
		assertEquals(layout.dpOfPx(activity.container.getWidth()), got.maxWidthDp());
		assertEquals(layout.dpOfPx(activity.container.getHeight()), got.maxHeightDp());
		assertEquals(got.maxWidthDp(), layout.dpOfPx(layout.getWidth()));
		assertEquals(got.maxHeightDp(), layout.dpOfPx(layout.getHeight()));
		assertFalse(layout.isAdMediaOverflow());
		assertEquals(0, layout.getMeasuredState() & (
			ViewGroup.MEASURED_STATE_TOO_SMALL |
			(ViewGroup.MEASURED_STATE_MASK >> ViewGroup.MEASURED_HEIGHT_STATE_SHIFT)
		));
	}

	@Test
	public void testSetAdMediaView() {
		AdSize size;
		TextView media;
		Rect mediaBounds = new Rect();
		TestAdLayout layout = this.addNewTestAdLayout();

		assertNull(layout.adMediaView());
		assertNull(layout.adMediaOverlayView());
		// ad media should figure out its own size, the full layout is available to it
		TestUtils.runOnMainSync(() -> layout.setTestAdMediaView(AdSize.EMPTY));

		media = layout.expectAdMediaView;
		assertNotNull(media);
		assertSame(media, layout.adMediaView());
		assertNull(layout.adMediaOverlayView());
		assertFalse(layout.isAdMediaOverflow());
		assertTrue(layout.isRenderingAdMedia());

		TestUtils.awaitLayout(media);
		media.getPaint()
			.getTextBounds(media.getText().toString(), 0, media.getText().length(), mediaBounds);
		/*
		 * there's always some delta between calculated bounds and the size textview resolves,
		 * 20 seems to be the sweet spot
		 */
		assertEquals(mediaBounds.width(), media.getWidth(), 20);
		assertEquals(mediaBounds.height(), media.getHeight(), 20);
		TestUtils.assertViewCentered(media);

		size = layout.supportedAdMediaSize();
		assertNotNull(size);
		assertEquals(0, size.minWidthDp());
		assertEquals(0, size.minHeightDp());
		assertEquals(layout.dpOfPx(activity.container.getWidth()), size.maxWidthDp());
		assertEquals(layout.dpOfPx(activity.container.getHeight()), size.maxHeightDp());

		// updating the supported size should apply to the rendered media
		TestUtils.runOnMainSync(() -> layout.setSupportedAdMediaSize(AdSize.ofExact(10, 10)));
		TestUtils.awaitLayout(layout);

		assertTrue(layout.dpOfPx(media.getWidth()) <= 10);
		assertTrue(layout.dpOfPx(media.getHeight()) <= 10);

		size = layout.supportedAdMediaSize();
		assertNotNull(size);
		assertEquals(10, size.maxWidthDp());
		assertEquals(10, size.maxHeightDp());
		assertFalse(layout.isAdMediaOverflow());

		// media should render correctly w.r.t requested constraints
		TestUtils.runOnMainSync(() -> {
			layout.setSupportedAdMediaSize(AdSize.EMPTY);
			layout.setTestAdMediaView(AdSize.ofExact(50, 50));
		});
		TestUtils.awaitLayout(layout);

		assertEquals(50, layout.dpOfPx(media.getWidth()));
		assertEquals(50, layout.dpOfPx(media.getHeight()));
		assertFalse(layout.isAdMediaOverflow());

		size = layout.supportedAdMediaSize();
		assertNotNull(size);
		assertEquals(0, size.minWidthDp());
		assertEquals(0, size.minHeightDp());
		assertEquals(layout.dpOfPx(activity.container.getWidth()), size.maxWidthDp());
		assertEquals(layout.dpOfPx(activity.container.getHeight()), size.maxHeightDp());

		// media should report underflow correctly
		TestUtils.runOnMainSync(() -> layout.setSupportedAdMediaSize(AdSize.ofExact(10, 10)));
		TestUtils.awaitLayout(layout);

		assertEquals(10, layout.dpOfPx(media.getWidth()));
		assertEquals(10, layout.dpOfPx(media.getHeight()));
		assertTrue(layout.isAdMediaOverflow());

		// flexible sizes should be rendered correctly w.r.t layout constraints
		TestUtils.runOnMainSync(() -> {
			layout.setSupportedAdMediaSize(AdSize.EMPTY);
			layout.setTestAdMediaView(AdSize.ofFlexible(1, 3, 15, 50, 300, 900));
		});
		TestUtils.awaitLayout(layout);

		int mediaW = layout.dpOfPx(media.getWidth());
		int mediaH = layout.dpOfPx(media.getHeight());

		assertTrue(mediaW >= 15 && mediaW <= 300);
		assertTrue(mediaH >= 50 && mediaH <= 900);
		assertEquals(1.f / 3, (float) mediaW / mediaH, 0.005f);
		assertFalse(layout.isAdMediaOverflow());

		TestUtils.runOnMainSync(() -> layout.setSupportedAdMediaSize(AdSize.ofExact(83, 300)));
		TestUtils.awaitLayout(layout);

		assertEquals(83, layout.dpOfPx(media.getWidth()));
		assertEquals(250, layout.dpOfPx(media.getHeight()), 1);

		TestUtils.runOnMainSync(() -> layout.setSupportedAdMediaSize(AdSize.ofExact(14, 49)));
		TestUtils.awaitLayout(layout);

		assertEquals(14, layout.dpOfPx(media.getWidth()));
		assertEquals(42, layout.dpOfPx(media.getHeight()), 1);
		assertTrue(layout.isAdMediaOverflow());
	}

	@Test
	public void testClearAdMediaView() {
		TestAdLayout layout = this.addNewTestAdLayout();

		assertNull(layout.adMediaView());
		assertNull(layout.adMediaOverlayView());
		assertFalse(layout.isRenderingAdMedia());

		TestUtils.runOnMainSync(() -> layout.setTestAdMediaView(AdSize.EMPTY));

		assertSame(layout.expectAdMediaView, layout.adMediaView());
		assertTrue(layout.isRenderingAdMedia());

		TestUtils.runOnMainSync(layout::clearAdMediaView);
		assertNull(layout.adMediaView());
		assertNull(layout.adMediaOverlayView());
		assertFalse(layout.isRenderingAdMedia());
	}
}
