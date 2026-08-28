# SPDX-License-Identifier: MIT OR Apache-2.0

-repackageclasses org.polygamma.android.origin.core

-include consumer-rules.pro

# Remove logs below `WARN`.
-assumenosideeffects interface org.polygamma.android.origin.util.Logger {
	static *** debug(...);
	static *** info(...);
}
