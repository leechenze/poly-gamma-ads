# SPDX-License-Identifier: MIT OR Apache-2.0

# Ignore warnings for optional dependencies.
-dontwarn androidx.annotation.**
-dontwarn com.bun.miitmdid.**
-dontwarn com.google.android.gms.**
-dontwarn com.huawei.hms.**

# See `../origin-antifraud/consumer-rules.pro`
-keepclassmembers class org.polygamma.android.origin.antifraud.EntropyMachine {
	*** Z;
	*** ZI(...);
	*** I;
	*** IJ(...);
	*** J;
	*** JL(...);
	*** L;
}

-keepclassmembers class org.polygamma.android.origin.** extends android.net.http.UploadDataProvider {
	public <methods>;
}

-keepclassmembers class org.polygamma.android.origin.** implements android.net.http.UrlRequest$Callback {
	public <methods>;
}
