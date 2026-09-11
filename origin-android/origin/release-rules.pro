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

# `origin-util` exported members:

-keep,allowoptimization interface org.polygamma.android.origin.util.BiConsumer,
	org.polygamma.android.origin.util.Consumer,
	org.polygamma.android.origin.util.Function,
	org.polygamma.android.origin.util.IntFunction,
	org.polygamma.android.origin.util.Supplier { public *; }

# Remove logs below `WARN`.
-assumenosideeffects interface org.polygamma.android.origin.util.Logger {
	static *** debug(...);
	static *** info(...);
}

# See `../origin-antifraud/release-rules.pro`
-keepnames class org.polygamma.android.origin.antifraud.TamperMachine

# See `../origin-protobuf/release-rules.pro`
-assumevalues public class org.polygamma.android.origin.protobuf.Protobuf {
	static int MAX_VARINT32_SIZE return 5;
	static int MAX_VARINT64_SIZE return 10;
	static int varintSizeOfBits(int) return 0..10;

	public static int wireTypeOfFieldTag(int) return 0..5;
}

-assumevalues public class org.polygamma.android.origin.protobuf.ProtobufEncoder {
	private static int sizeOfVarint32(int) return 1..5;
	private static int sizeOfVarint64(long) return 1..10;
}

# See `../origin-util/consumer-rules.pro`
-convertchecknotnull public class org.polygamma.android.origin.util.Preconditions {
	** checkNotNull(...);
}

-include consumer-rules.pro
