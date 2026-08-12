package com.polygamma.adsdemo;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

public class AdEntryActivity extends AppCompatActivity {

    private Button backBtn;
    private Button splashAdBtn;
    private Button rewardedAdBtn;
    private Button interstitialAdBtn;
    private Button nativeAdBtn;
    private Button deviceInfoBtn;
    private MaterialToolbar toolbar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ad_entry);

        backBtn = findViewById(R.id.btn_back);
        toolbar = findViewById(R.id.toolbar);
        splashAdBtn = findViewById(R.id.splash_ad_btn);
        rewardedAdBtn = findViewById(R.id.rewarded_ad_btn);
        interstitialAdBtn = findViewById(R.id.interstitial_ad_btn);
        nativeAdBtn = findViewById(R.id.native_ad_btn);
        deviceInfoBtn = findViewById(R.id.device_info_btn);

        // click listener group
        backBtn.setOnClickListener(v -> finish());
        toolbar.setNavigationOnClickListener(v -> finish());


        splashAdBtn.setOnClickListener(v ->
                startActivity(new Intent(this, SplashAdActivity.class)));
        rewardedAdBtn.setOnClickListener(v ->
                startActivity(new Intent(this, RewardedAdActivity.class)));
        interstitialAdBtn.setOnClickListener(v ->
                startActivity(new Intent(this, InterstitialAdActivity.class)));
        nativeAdBtn.setOnClickListener(v ->
                startActivity(new Intent(this, NativeAdActivity.class)));
        deviceInfoBtn.setOnClickListener(v ->
                startActivity(new Intent(this, DeviceInfoActivity.class)));

    }
}
