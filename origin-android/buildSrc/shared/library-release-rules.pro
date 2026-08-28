# SPDX-License-Identifier: MIT OR Apache-2.0

-classobfuscationdictionary ./obfuscationdictionary.txt
-obfuscationdictionary ./obfuscationdictionary.txt

-keepnames public class org.polygamma.android.origin.**
-keepnames public enum org.polygamma.android.origin.**
-keepnames public interface org.polygamma.android.origin.**
-renamesourcefileattribute SourceFile
-keepattributes Exceptions,EnclosingMethod,InnerClasses,Signature
-keepattributes *Annotation*

-keep,allowoptimization public class !org.polygamma.android.origin.**.BuildConfig,
	org.polygamma.android.origin.** { public protected *; }
-keep,allowoptimization public enum org.polygamma.android.origin.** { public protected *; }
-keep,allowoptimization public interface org.polygamma.android.origin.** { public protected *; }
