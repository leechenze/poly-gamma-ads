// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.antifraud;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Current {@linkplain Activity activity} reference.
 */
final class CurrentActivityReference implements Application.ActivityLifecycleCallbacks {

	private final AtomicReference<Activity> reference;

	/**
	 * Construct new activity reference with no initial activity.
	 */
	CurrentActivityReference() {
		this.reference = new AtomicReference<>();
	}

	/**
	 * Retrieve current activity, if any.
	 *
	 * @return current activity
	 */
	@Nullable Activity get() {
		return this.reference.get();
	}

	@Override
	@RestrictTo(RestrictTo.Scope.SUBCLASSES)
	public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
	}

	@Override
	@RestrictTo(RestrictTo.Scope.SUBCLASSES)
	public void onActivityStarted(@NonNull Activity activity) {
		this.reference.set(activity);
	}

	@Override
	@RestrictTo(RestrictTo.Scope.SUBCLASSES)
	public void onActivityResumed(@NonNull Activity activity) {
		this.onActivityStarted(activity);
	}

	@Override
	@RestrictTo(RestrictTo.Scope.SUBCLASSES)
	public void onActivityPaused(@NonNull Activity activity) {
		this.onActivityDestroyed(activity);
	}

	@Override
	@RestrictTo(RestrictTo.Scope.SUBCLASSES)
	public void onActivityStopped(@NonNull Activity activity) {
		this.onActivityDestroyed(activity);
	}

	@Override
	@RestrictTo(RestrictTo.Scope.SUBCLASSES)
	public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
	}

	@Override
	@RestrictTo(RestrictTo.Scope.SUBCLASSES)
	public void onActivityDestroyed(@NonNull Activity activity) {
		this.reference.compareAndSet(activity, null);
	}
}
