package com.polygamma.adsdemo;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.android.material.appbar.MaterialToolbar;
import com.polygamma.adsdemo.constants.Constants;

public class DeviceSettingActivity extends Activity {

    private static final String TAG = "DeviceSettingActivity";

    private Button backBtn;
    private Button saveBtn;
    private MaterialToolbar toolbar;
    private CheckBox cbCanUseLocation;
    private CheckBox cbCanUsePhoneState;
    private CheckBox cbCanUseOaid;
    private CheckBox cbCanUseAndroidId;
    private CheckBox cbCanUseAppList;
    private CheckBox cbCanUseSimOperator;
    private CheckBox cbCanUseSpaceSize;

    private SharedPreferences sp;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_device_setting);

        sp = getSharedPreferences(Constants.PREFS_DEVICE_SETTING, 0);

        initView();
        loadSettings();
    }

    private void initView() {
        backBtn = findViewById(R.id.btn_back);
        saveBtn = findViewById(R.id.btn_save);
        toolbar = findViewById(R.id.toolbar);
        cbCanUseLocation = findViewById(R.id.cb_can_use_location);
        cbCanUsePhoneState = findViewById(R.id.cb_can_use_phone_state);
        cbCanUseOaid = findViewById(R.id.cb_can_use_oaid);
        cbCanUseAndroidId = findViewById(R.id.cb_can_use_android_id);
        cbCanUseAppList = findViewById(R.id.cb_can_use_app_list);
        cbCanUseSimOperator = findViewById(R.id.cb_can_use_sim_operator);
        cbCanUseSpaceSize = findViewById(R.id.cb_can_use_space_size);

        // click listener group
        backBtn.setOnClickListener(v -> finish());
        toolbar.setNavigationOnClickListener(v -> finish());
        saveBtn.setOnClickListener(v -> saveSettings());

    }

    private void loadSettings() {
        cbCanUseLocation.setChecked(sp.getBoolean(Constants.DEV_CAN_USE_LOCATION, true));
        cbCanUsePhoneState.setChecked(sp.getBoolean(Constants.DEV_CAN_USE_PHONE_STATE, true));
        cbCanUseOaid.setChecked(sp.getBoolean(Constants.DEV_CAN_USE_OAID, true));
        cbCanUseAndroidId.setChecked(sp.getBoolean(Constants.DEV_CAN_USE_ANDROID_ID, true));
        cbCanUseAppList.setChecked(sp.getBoolean(Constants.DEV_CAN_USE_APP_LIST, true));
        cbCanUseSimOperator.setChecked(sp.getBoolean(Constants.DEV_CAN_USE_SIM_OPERATOR, true));
        cbCanUseSpaceSize.setChecked(sp.getBoolean(Constants.DEV_CAN_USE_SPACE_SIZE, true));
    }

    private void saveSettings() {
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(Constants.DEV_CAN_USE_LOCATION, cbCanUseLocation.isChecked());
        editor.putBoolean(Constants.DEV_CAN_USE_PHONE_STATE, cbCanUsePhoneState.isChecked());
        editor.putBoolean(Constants.DEV_CAN_USE_OAID, cbCanUseOaid.isChecked());
        editor.putBoolean(Constants.DEV_CAN_USE_ANDROID_ID, cbCanUseAndroidId.isChecked());
        editor.putBoolean(Constants.DEV_CAN_USE_APP_LIST, cbCanUseAppList.isChecked());
        editor.putBoolean(Constants.DEV_CAN_USE_SIM_OPERATOR, cbCanUseSimOperator.isChecked());
        editor.putBoolean(Constants.DEV_CAN_USE_SPACE_SIZE, cbCanUseSpaceSize.isChecked());
        editor.apply();


        /**
         * print value of:
         * cbCanUseLocation
         * cbCanUsePhoneState
         * cbCanUseOaid
         * cbCanUseAndroidId
         * cbCanUseAppList
         * cbCanUseSimOperator
         * cbCanUseSpaceSize
         */
        Log.d(
                TAG,
                String.format(
                        "saveSettings: location=%s, phoneState=%s, oaid=%s, androidId=%s, appList=%s, simOperator=%s, spaceSize=%s",
                        cbCanUseLocation.isChecked(),
                        cbCanUsePhoneState.isChecked(),
                        cbCanUseOaid.isChecked(),
                        cbCanUseAndroidId.isChecked(),
                        cbCanUseAppList.isChecked(),
                        cbCanUseSimOperator.isChecked(),
                        cbCanUseSpaceSize.isChecked()
                )
        );

        Toast.makeText(this, "设置已保存, 将在下次初始化时生效", Toast.LENGTH_SHORT).show();
        finish();
    }

}
