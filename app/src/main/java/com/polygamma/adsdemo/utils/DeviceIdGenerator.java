package com.polygamma.adsdemo.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.polygamma.adsdemo.constants.Constants;

import java.util.UUID;

public class DeviceIdGenerator {

    private DeviceIdGenerator() {
    }


    public static String getDeviceId(Context context) {

        SharedPreferences sp = context.getSharedPreferences(
                Constants.PREFS_NAME_ORIGIN_DEVICE_INFO, Context.MODE_PRIVATE
        );

        String deviceId = sp.getString(Constants.PREFS_KEY_DEVICE_ID, null);


        if (deviceId == null) {
            deviceId = UUID.randomUUID().toString();
            sp.edit().putString(Constants.PREFS_KEY_DEVICE_ID, deviceId).apply();
        }

        return deviceId;
    }
}