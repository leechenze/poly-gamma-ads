# SPDX-License-Identifier: MIT OR Apache-2.0

-dontwarn com.bun.miitmdid.**
-dontwarn com.google.android.gms.**

# Reflection is used to access `ofProvider()`.
-keepclassmembers class * extends org.polygamma.android.origin.core.OriginModule {
	public static *** ofProvider();
}
