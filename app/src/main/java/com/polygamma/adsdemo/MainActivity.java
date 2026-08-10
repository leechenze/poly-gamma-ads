package com.polygamma.adsdemo;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private Button sdkInitBtn;
    private Button privacySettingBtn;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sdkInitBtn = findViewById(R.id.btn_sdk_init);
        privacySettingBtn = findViewById(R.id.bt_privacy_setting);

        // click listener group
        sdkInitBtn.setOnClickListener(view -> initSDK());
        privacySettingBtn.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, PrivacySettingActivity.class)));

    }

    private void initSDK() {
        sdkInitBtn.setEnabled(false);
        sdkInitBtn.setText("SDK 初始化中...");

        MyApplication app = (MyApplication) getApplication();
        boolean success = app.initSDK();
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

        sdkInitBtn.setEnabled(true);
        sdkInitBtn.setText("SDK 初始化");
    }
}