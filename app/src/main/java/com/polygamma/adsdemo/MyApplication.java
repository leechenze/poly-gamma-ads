package com.polygamma.adsdemo;

import android.app.Application;
import android.content.Context;
import android.util.Log;
import android.util.Pair;


import com.polygamma.adsdemo.utils.DeviceIdGenerator;

import org.polygamma.android.origin.Origin;
import org.polygamma.android.origin.OriginOptions;

public class MyApplication extends Application {

    private static final String TAG = "MyApplication";

    // GAID缓存
    private volatile String gaid = "";

    private boolean gaidLimited = false;
    private String originDeviceId;


    @Override
    public void onCreate() {

        super.onCreate();

        originDeviceId = DeviceIdGenerator.getDeviceId(this);

    }


    public boolean initSDK() {
        try {
            Origin.initializeWithOptions(this,
                    new OriginOptions()
                            .addCapability(Origin.CAPABILITY_ANTIFRAUD)
                            .addCapability(Origin.CAPABILITY_ADS)
                            .addDynamicDeviceId("GAID", ctxt -> {
                                Log.d(TAG, "Dynamic GAID: " + originDeviceId);
                                return new Pair<>(originDeviceId, gaidLimited);
                            }));
            Log.d(TAG, "Origin SDK launched");
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Origin SDK init failed", e);
            return false;
        }
    }


    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
    }

}
