package com.polygamma.adsdemo;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import androidx.annotation.Nullable;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.appbar.MaterialToolbar;

import org.polygamma.android.origin.ads.DisplayPlacementView;

public class SplashAdActivity extends Activity {
    private FrameLayout splashAdContainer;

    ImageButton closeTopIcon;
    Button closeBtn;
    Button landingPageBtn;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideStatusBarAndGestureNavHandle();
        setContentView(R.layout.activity_splash_ad);

        splashAdContainer = findViewById(R.id.splash_ad_container);
        closeBtn = findViewById(R.id.btn_close);
        closeTopIcon = findViewById(R.id.btn_close_icon);
        landingPageBtn = findViewById(R.id.btn_landing_page);

        DisplayPlacementView adView = DisplayPlacementView.ofPlacementId(this, "FgUtQqop18uf1I2fwDie");

        splashAdContainer.addView(adView);

        closeBtn.setOnClickListener(v -> finish());
        closeTopIcon.setOnClickListener(v -> finish());
        landingPageBtn.setOnClickListener(v -> {
            Intent intent = new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.yunyou.space")
            );
            startActivity(intent);
        });
    }

    private void hideStatusBarAndGestureNavHandle() {
        // 全屏隐藏状态栏和手势导航条.
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(
                        getWindow(),
                        getWindow().getDecorView()
                );
        controller.hide(WindowInsetsCompat.Type.statusBars() | WindowInsetsCompat.Type.navigationBars());
        controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);

    }
}
