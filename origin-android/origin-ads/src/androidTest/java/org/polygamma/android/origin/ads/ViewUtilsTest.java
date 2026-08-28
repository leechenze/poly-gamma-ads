// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.ads;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * {@link ViewUtils} tests.
 */
@RunWith(AndroidJUnit4.class)
public class ViewUtilsTest {

	public static final class TestActivity extends Activity {
		TextView content;
		FrameLayout layout;
		Dialog dialog;
		TextView dialogContent;

		@Override
		public void onCreate(@Nullable Bundle bnd) {
			super.onCreate(bnd);

			this.content = new TextView(this);
			this.content.setText("Test");
			this.layout = new FrameLayout(this);
			this.layout.addView(
				this.content,
				FrameLayout.LayoutParams.MATCH_PARENT,
				FrameLayout.LayoutParams.MATCH_PARENT
			);
			super.setContentView(this.layout);
		}

		void createDialog() {
			this.dialog = new Dialog(this);
			this.dialogContent = new TextView(this);

			Window wnd = this.dialog.getWindow();
			WindowManager.LayoutParams layout = wnd.getAttributes();

			this.dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
			this.dialog.setContentView(this.dialogContent);
			layout.gravity = Gravity.CENTER;
			wnd.setAttributes(layout);
			wnd.setLayout(
				WindowManager.LayoutParams.MATCH_PARENT,
				WindowManager.LayoutParams.MATCH_PARENT
			);
		}
	}

	@Test
	public void testWindowOf() {
		TestUtils.withActivity(TestActivity.class, (test) -> {
			assertSame(test.getWindow(), ViewUtils.windowOf(test.content));

			TestUtils.runOnMainSync(test::createDialog);
			TestUtils.runOnMainSync(test.dialog::show);
			assertSame(test.dialog.getWindow(), ViewUtils.windowOf(test.dialogContent));

			TestUtils.runOnMainSync(test.layout::removeAllViews);
			assertNull(ViewUtils.windowOf(test.content));
		});
	}

	@Test
	public void testPaddingOf() {
		TestUtils.withActivity(TestActivity.class, (test) -> {
			assertEquals(0, ViewUtils.inlinePaddingOf(test.content));
			assertEquals(0, ViewUtils.blockPaddingOf(test.content));

			TestUtils.runOnMainSync(() -> test.content.setPadding(1, 2, 3, 4));
			assertEquals(4, ViewUtils.inlinePaddingOf(test.content));
			assertEquals(6, ViewUtils.blockPaddingOf(test.content));
		});
	}
}
