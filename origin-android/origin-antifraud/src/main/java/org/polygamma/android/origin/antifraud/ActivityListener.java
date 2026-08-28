// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.antifraud;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Current {@linkplain Activity activity} listener.
 */
final class ActivityListener implements Application.ActivityLifecycleCallbacks {

	private final AtomicReference<Activity> current;

	/**
	 * Construct a new activity listener with no initial activity.
	 */
	ActivityListener() {
		this.current = new AtomicReference<>();
	}

	/**
	 * Current active activity, if any.
	 *
	 * @return current activity
	 */
	@Nullable Activity current() {
		return this.current.get();
	}

	@Override
	public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
	}

	@Override
	public void onActivityStarted(@NonNull Activity activity) {
		this.current.set(activity);
	}

	@Override
	public void onActivityResumed(@NonNull Activity activity) {
		this.onActivityStarted(activity);
	}

	@Override
	public void onActivityPaused(@NonNull Activity activity) {
		this.onActivityDestroyed(activity);
	}

	@Override
	public void onActivityStopped(@NonNull Activity activity) {
		this.onActivityDestroyed(activity);
	}

	@Override
	public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
	}

	@Override
	public void onActivityDestroyed(@NonNull Activity activity) {
		this.current.compareAndSet(activity, null);
	}
}
