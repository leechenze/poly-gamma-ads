// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.util;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.view.Display;

import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Function;

/**
 * Utility {@linkplain Context context} definitions.
 *
 * @since 0.1
 */
public class AndroidContexts {

	private static final String TAG = AndroidContexts.class.getSimpleName();

	/**
	 * Test whether a permission has been granted.
	 * <p>Shorthand for:
	 * {@snippet lang="java" :
	 * ctxt.checkCallingOrSelfPermission(perm) == PackageManager.PERMISSION_GRANTED
	 * }
	 *
	 * @param ctxt context to test permission on
	 * @param perm permission to test for
	 * @return {@code true} if, and only if, {@code perm} has been granted
	 * @since 0.1
	 * @see Context#checkCallingOrSelfPermission(String)
	 */
	@SuppressWarnings("JavadocDeclaration")
	public static @CheckResult boolean hasPermission(Context ctxt, String perm) {
		return ctxt.checkCallingOrSelfPermission(perm) == PackageManager.PERMISSION_GRANTED;
	}

	/**
	 * Test whether zero or more permissions have been granted.
	 * <p>This test whether {@code perms} is empty, or all permissions within {@code perms} have
	 * been {@linkplain #hasPermission(Context, String) granted}.
	 *
	 * @param ctxt context to test permission on
	 * @param perms permissions to test for
	 * @return {@code true} if, and only if, {@code perms} is empty <i>or</i> all permissions
	 * within {@code perms} have been granted
	 * @since 0.1
	 * @see #hasPermission(Context, String)
	 */
	public static @CheckResult boolean hasAllPermissions(Context ctxt, String... perms) {
		for (String perm : perms) {
			if (!hasPermission(ctxt, perm))
				return false;
		}
		return true;
	}

	/**
	 * Test whether any permission has been granted.
	 * <p>This tests whether {@code perms} is empty, or any permission within {@code perms} have
	 * been {@linkplain #hasPermission(Context, String) granted}.
	 *
	 * @param ctxt context to test permission on
	 * @param perms permissions to test for
	 * @return {@code true} if, and only if, {@code perms} is empty <i>or</i> any permission within
	 * {@code perms} has been granted
	 * @since 0.1
	 * @see #hasPermission(Context, String)
	 */
	public static @CheckResult boolean hasAnyPermission(Context ctxt, String... perms) {
		for (String perm : perms) {
			if (hasPermission(ctxt, perm))
				return true;
		}
		return perms.length == 0;
	}

	/**
	 * Test whether a system feature is present.
	 * <p>Shorthand for:
	 * {@snippet lang="java" :
	 * PackageManager manager = ctxt.getPackageManager();
	 *
	 * return manager != null && manager.hasSystemFeature(feat);
	 * }
	 *
	 * @param ctxt context to test feature on
	 * @param feat feature to test for
	 * @return {@code true} if, and only if, {@code feat} is present
	 * @since 0.1
	 * @see Context#getPackageManager()
	 * @see PackageManager#hasSystemFeature(String)
	 */
	@SuppressWarnings("JavadocDeclaration")
	public static @CheckResult boolean hasSystemFeature(Context ctxt, String feat) {
		PackageManager pman = ctxt.getPackageManager();

		return pman != null && pman.hasSystemFeature(feat);
	}

	/**
	 * Retrieve system service.
	 * <p>This returns the system service, identified by {@code name}, of type {@code type} from
	 * {@code ctxt}. If the system service {@linkplain Context#getSystemService(String) returned}
	 * by {@code ctxt} is non-{@code null} <i>and</i> not an instance of {@code type}, this returns
	 * {@code null}.
	 *
	 * @param <T> service type
	 * @param ctxt context to retrieve service from
	 * @param type type class of service to return
	 * @param name service name
	 * @return service instance or {@code null} if not found
	 * @since 0.1
	 * @see Context#getSystemService(String)
	 */
	@SuppressWarnings("unchecked")
	public static <T> @Nullable T systemServiceOf(Context ctxt, Class<T> type, String name) {
		Object svc = ctxt.getSystemService(name);

		if (svc == null)
			return null;
		if (!type.isInstance(svc)) {
			Logger.warn(TAG, "system service %s is not an instance of %s", name, type);
			return null;
		}
		return (T) svc;
	}

	/**
	 * Perform operation on a system service, if available.
	 * <p>Shorthand for:
	 * {@snippet lang="java" :
	 * T service = AndroidContexts.systemServiceOf(ctxt, type, name); // @link substring="systemServiceOf" target="#systemServiceOf(Context, Class, String)"
	 *
	 * service != null ? op.apply(service) : null
	 * }
	 *
	 * @param <T> service type
	 * @param <R> operation result type
	 * @param ctxt context to retrieve service from
	 * @param type type class of service to return
	 * @param name service name
	 * @param op operation to apply
	 * @return operation result or {@code null}
	 * @since 0.1
	 * @see #systemServiceOf(Context, Class, String)
	 */
	@SuppressWarnings("JavadocDeclaration")
	public static <T, R> @Nullable R withSystemService(
		Context ctxt,
		Class<T> type,
		String name,
		Function<T, R> op
	) {
		T svc = systemServiceOf(ctxt, type, name);

		return svc != null ? op.apply(svc) : null;
	}

	/**
	 * Perform operation on a system service, if available, or return default value.
	 * <p>Like {@link #withSystemService(Context, Class, String, Function)}; however, this returns
	 * {@code dfl} if service does not exist <i>or</i> {@code op} returns a {@code null} value.
	 *
	 * @param <T> service type
	 * @param <R> operation result type
	 * @param ctxt context to retrieve service from
	 * @param type type class of service to return
	 * @param name service name
	 * @param op operation to apply
	 * @param dfl default value to return
	 * @return operation result or {@code dfl}
	 * @since 0.1
	 * @see #withSystemService(Context, Class, String, Function)
	 */
	public static <T, R> R withSystemServiceElse(
		Context ctxt,
		Class<T> type,
		String name,
		Function<T, R> op,
		@NonNull R dfl
	) {
		return Preconditions.checkNotNullElse(withSystemService(ctxt, type, name, op), dfl);
	}

	/**
	 * Retrieve display a context is associated with.
	 *
	 * @param ctxt context to retrieve display of
	 * @return display associated with {@code ctxt} or, {@code null} if {@code ctxt} is not
	 * associated with a display or display could not be resolved
	 * @since 1.2
	 * @see Context#getDisplay()
	 */
	public static @Nullable Display displayOf(Context ctxt) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R)
			return null;

		try {
			return ctxt.getDisplay();
		} catch (UnsupportedOperationException ignored) {
			return null;
		}
	}

	private AndroidContexts() {
	}
}
