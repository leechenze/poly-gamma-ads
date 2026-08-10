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

import org.polygamma.android.origin.Origin;
import org.polygamma.android.origin.ads.AdsModule;

public class PrivacySettingActivity extends Activity {

    private static final String TAG = "PrivacySettingActivity";
    private Button backBtn;
    private Button saveBtn;
    private MaterialToolbar toolbar;
    private CheckBox cbAdult;
    private CheckBox cbPersonalized;
    private CheckBox cbProgrammatic;
    private SharedPreferences sp;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_privacy_setting);

        sp = getSharedPreferences(Constants.PREFS_PRIVACY_SETTING, 0);

        initView();
        loadSettings();
    }


    private void initView() {
        backBtn = findViewById(R.id.btn_back);
        saveBtn = findViewById(R.id.btn_save);
        toolbar = findViewById(R.id.toolbar);
        cbAdult = findViewById(R.id.cb_adult);
        cbPersonalized = findViewById(R.id.cb_personalized);
        cbProgrammatic = findViewById(R.id.cb_programmatic);

        // click listener group
        backBtn.setOnClickListener(v -> finish());
        toolbar.setNavigationOnClickListener(v -> finish());
        saveBtn.setOnClickListener(v -> saveSettings());
    }

    private void loadSettings() {
        cbAdult.setChecked(sp.getBoolean(Constants.PREFS_KEY_ADULT, true));
        cbPersonalized.setChecked(sp.getBoolean(Constants.PREFS_KEY_PERSONALIZED, true));
        cbProgrammatic.setChecked(sp.getBoolean(Constants.PREFS_KEY_PROGRAMMATIC, true));
    }

    private void saveSettings() {
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(Constants.PREFS_KEY_ADULT, cbAdult.isChecked());
        editor.putBoolean(Constants.PREFS_KEY_PERSONALIZED, cbPersonalized.isChecked());
        editor.putBoolean(Constants.PREFS_KEY_PROGRAMMATIC, cbProgrammatic.isChecked());
        editor.apply();

        // Synchronize settings to Origin SDK
        applySettingsToSDK();

        // print value of cbAdult, cbPersonalized, cbprogrammatic.
        Log.d(
                TAG,
                String.format(
                        "saveSettings: adult=%s, personalized=%s, programmatic=%s",
                        cbAdult.isChecked(),
                        cbPersonalized.isChecked(),
                        cbProgrammatic.isChecked()
                )
        );

        Toast.makeText(this, "设置已保存", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void applySettingsToSDK() {
        /** TO DO:
         * Add support for methods regarding whether the user is an adult,
         * whether personalized recommendations are enabled,
         * whether programmatic recommendations are enabled.
         */

        /** Similar to the implementation method of sigmob SDK */


        // AdsModule ads = Origin.ads();

        // if (!ads.isInit()) {
        //     return;
        // }
        // ads.setAdult(cbAdult.isChecked());
        // ads.setPersonalizedAdvertisingOn(cbPersonalized.isChecked());
        // ads.setProgrammaticRecommend(cbProgrammatic.isChecked());
    }
}
