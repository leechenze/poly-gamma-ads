package com.polygamma.adsdemo;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.polygamma.adsdemo.constants.Constants;
import com.polygamma.adsdemo.constants.SDKInitConfig;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private Button sdkInitBtn;
    private Button privacySettingBtn;
    private Button deviceSettingBtn;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sdkInitBtn = findViewById(R.id.btn_sdk_init);
        privacySettingBtn = findViewById(R.id.btn_privacy_setting);
        deviceSettingBtn = findViewById(R.id.btn_device_setting);

        // click listener group
        sdkInitBtn.setOnClickListener(view -> initSDK());
        privacySettingBtn.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, PrivacySettingActivity.class)));
        deviceSettingBtn.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, DeviceSettingActivity.class));
        });

    }

    private void initSDK() {
        sdkInitBtn.setEnabled(false);
        sdkInitBtn.setText("SDK 初始化中...");
        // ==========================================================================================
        MyApplication app = (MyApplication) getApplication();
        SharedPreferences privacySp = getSharedPreferences(Constants.PREFS_PRIVACY_SETTING, 0);
        final boolean cbAdult = privacySp.getBoolean(Constants.PREFS_KEY_ADULT, true);
        final boolean cbPersonalized = privacySp.getBoolean(Constants.PREFS_KEY_PERSONALIZED, true);
        final boolean cbProgrammatic = privacySp.getBoolean(Constants.PREFS_KEY_PROGRAMMATIC, true);
        SharedPreferences deviceSp = getSharedPreferences(Constants.PREFS_DEVICE_SETTING, 0);
        final boolean canUseLocation = deviceSp.getBoolean(Constants.DEV_CAN_USE_LOCATION, true);
        final boolean canUsePhoneState = deviceSp.getBoolean(Constants.DEV_CAN_USE_PHONE_STATE, true);
        final boolean canUseOaid = deviceSp.getBoolean(Constants.DEV_CAN_USE_OAID, true);
        final boolean canUseAndroidId = deviceSp.getBoolean(Constants.DEV_CAN_USE_ANDROID_ID, true);
        final boolean canUseAppList = deviceSp.getBoolean(Constants.DEV_CAN_USE_APP_LIST, true);
        final boolean canUseSimOperator = deviceSp.getBoolean(Constants.DEV_CAN_USE_SIM_OPERATOR, true);
        final boolean canUseSpaceSize = deviceSp.getBoolean(Constants.DEV_CAN_USE_SPACE_SIZE, true);

        // build sdkConf param obj
        SDKInitConfig sdkInitConfig = new SDKInitConfig.Builder()
                .setIsAdult(cbAdult)
                .setIsPersonalized(cbPersonalized)
                .setIsProgrammatic(cbProgrammatic)
                .setCanUseLocation(canUseLocation)
                .setCanUsePhoneState(canUsePhoneState)
                .setCanUseOaid(canUseOaid)
                .setCanUseAndroidId(canUseAndroidId)
                .setCanUseAppList(canUseAppList)
                .setCanUseSimOperator(canUseSimOperator)
                .setCanUseSpaceSize(canUseSpaceSize).build();

        boolean success = app.initSDK(sdkInitConfig);

        if (success) {
            Toast.makeText(
                    this,
                    "初始化成功!",
                    Toast.LENGTH_SHORT
            ).show();
        } else {
            Toast.makeText(
                    this,
                    "初始化失败!",
                    Toast.LENGTH_SHORT
            ).show();
        }

        // ==========================================================================================
        sdkInitBtn.setEnabled(true);
        sdkInitBtn.setText("SDK 初始化");
    }
}