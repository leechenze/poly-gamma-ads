# SPDX-License-Identifier: MIT OR Apache-2.0

-classobfuscationdictionary ../buildSrc/shared/obfuscationdictionary.txt
-obfuscationdictionary ../buildSrc/shared/obfuscationdictionary.txt

-renamesourcefileattribute SourceFile
-keepattributes Exceptions,EnclosingMethod,InnerClasses,Signature
-keepattributes *Annotation*

-repackageclasses org.polygamma.android.origin.internal
# Keep our internal `package-info` to ensure IDEs take into consideration it's `@RestrictTo`.
-keep class org.polygamma.android.origin.internal.package-info

-keep,allowoptimization class org.polygamma.android.origin.Origin,
	org.polygamma.android.origin.OriginOptions { public *; }

# `origin-core` exported members:
-keep interface org.polygamma.android.origin.core.OriginModuleEventCallback
{ *** onOriginModuleEvent(...); }

-keep,allowoptimization class org.polygamma.android.origin.core.OriginModule {
	*** registerEventCallback(...);
	*** unregisterEventCallback(...);
}

# `origin-ads` exported members:
-keep,allowoptimization interface org.polygamma.android.origin.ads.PlacementRenderer { public *; }

-keep,allowoptimization class org.polygamma.android.origin.ads.AdInstance,
	org.polygamma.android.origin.ads.AdMediaLayout,
	org.polygamma.android.origin.ads.AdSize,
	org.polygamma.android.origin.ads.DisplayPlacementView,
	org.polygamma.android.origin.ads.DisplayPlacementViewBuilder,
	org.polygamma.android.origin.ads.PlacementEvent,
	org.polygamma.android.origin.ads.PlacementException { public *; }

-keep,allowoptimization class org.polygamma.android.origin.ads.AdsModule { *** PLACEMENT_EVENT; }

# `origin-antifraud` exported members:

-keep,allowoptimization class org.polygamma.android.origin.antifraud.AntifraudStatus { public *; }

-keep,allowoptimization class org.polygamma.android.origin.antifraud.AntifraudModule {
	*** STATUS_UPDATE_EVENT;
	*** addEntropyData(...);
	*** status();
}

# Remove logs below `WARN`.
-assumenosideeffects interface org.polygamma.android.origin.util.Logger {
	static *** debug(...);
	static *** info(...);
}

# See `../origin-antifraud/release-rules.pro`
-keepnames class org.polygamma.android.origin.antifraud.EntropyMachine

-include consumer-rules.pro
