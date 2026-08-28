# SPDX-License-Identifier: MIT OR Apache-2.0

# Keep tamper members in `EntropyMachine`.
-keepclassmembers class org.polygamma.android.origin.antifraud.EntropyMachine {
	*** Z;
	*** ZI(...);
	*** I;
	*** IJ(...);
	*** J;
	*** JL(...);
	*** L;
}
