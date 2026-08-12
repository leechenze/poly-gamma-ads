package com.polygamma.adsdemo;

import android.app.Application;
import android.content.Context;
import android.util.Log;
import android.util.Pair;


import com.polygamma.adsdemo.constants.SDKInitConfig;
import com.polygamma.adsdemo.utils.DeviceIdGenerator;

import org.polygamma.android.origin.Origin;
import org.polygamma.android.origin.OriginOptions;

public class MyApplication extends Application {

    private static final String TAG = "MyApplication";
    private boolean sdkInitialized = false;

    private boolean deviceIdLimited = false;
    private String originDeviceId;

    @Override
    public void onCreate() {

        super.onCreate();

        originDeviceId = DeviceIdGenerator.getDeviceId(this);

    }

    public boolean isSdkInitialized() {
        return sdkInitialized;
    }

    public boolean initSDK(SDKInitConfig sdkInitConfig) {
        try {
            Origin.initializeWithOptions(this,
                    new OriginOptions()
                            .addCapability(Origin.CAPABILITY_ANTIFRAUD)
                            .addCapability(Origin.CAPABILITY_ADS)
                            .addDynamicDeviceId("CUSTOMID", ctxt -> {
                                Log.d(TAG, "Dynamic CUSTOMID: " + originDeviceId);
                                return new Pair<>(originDeviceId, deviceIdLimited);
                            }));

            /**
             * To do
             * Improve of Origin SDK methods
             */
            if (sdkInitConfig.isAdult) {
            }
            if (sdkInitConfig.isPersonalized) {
            }
            if (sdkInitConfig.isProgrammatic) {
            }
            if (sdkInitConfig.canUseLocation) {
            }
            if (sdkInitConfig.canUsePhoneState) {
            }
            if (sdkInitConfig.canUseOaid) {
            }
            if (sdkInitConfig.canUseAndroidId) {
            }
            if (sdkInitConfig.canUseAppList) {
            }
            if (sdkInitConfig.canUseSimOperator) {
            }
            if (sdkInitConfig.canUseSpaceSize) {
            }

            Log.d(
                    TAG,
                    String.format(
                            "saveSettings: isAdult=%s, isPersonalized=%s, isProgrammatic=%s, location=%s, phoneState=%s, oaid=%s, androidId=%s, appList=%s, simOperator=%s, spaceSize=%s",
                            sdkInitConfig.isAdult,
                            sdkInitConfig.isPersonalized,
                            sdkInitConfig.isProgrammatic,
                            sdkInitConfig.canUseLocation,
                            sdkInitConfig.canUsePhoneState,
                            sdkInitConfig.canUseOaid,
                            sdkInitConfig.canUseAndroidId,
                            sdkInitConfig.canUseAppList,
                            sdkInitConfig.canUseSimOperator,
                            sdkInitConfig.canUseSpaceSize
                    )
            );
            Log.d(TAG, "Origin SDK launched");
            sdkInitialized = true;
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Origin SDK init failed", e);
            sdkInitialized = false;
            return false;
        }
    }


    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
    }

}
