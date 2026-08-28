// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.util;

import android.content.ContentResolver;
import android.provider.Settings;

import androidx.annotation.Nullable;

import java.lang.reflect.Method;

/**
 * Android {@linkplain Settings settings} utility definitions.
 *
 * @since 1.1
 */
public class AndroidSettings {

	private static final String TAG = AndroidSettings.class.getSimpleName();

	private static final @Nullable Method SYSTEM_PROPERTIES_GET;

	static {
		Method get = null;

		try {
			get =
				(Method) Reflection.resolveExecutable(
					"Landroid/os/SystemProperties.get(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;"
				);
		} catch (Throwable err) {
			Logger.debug(TAG, "failed to resolve `android.os.SystemProperties::get()`", err);
		}
		SYSTEM_PROPERTIES_GET = get;
	}

	/**
	 * Retrieve {@code android.os.SystemProperties} setting value as a string.
	 *
	 * @param name setting name
	 * @param dfl default value
	 * @return setting value or {@code dfl} if setting was not found
	 * @since 1.1
	 */
	public static String getSystemString(String name, String dfl) {
		if (SYSTEM_PROPERTIES_GET == null)
			return dfl;
		try {
			return Preconditions.checkNotNullElse(
				(String) SYSTEM_PROPERTIES_GET.invoke(null, name, dfl),
				dfl
			);
		} catch (Throwable err) {
			Logger.debug(TAG, "failed to get system string %s", name, err);
			return dfl;
		}
	}

	/**
	 * Retrieve {@code android.os.SystemProperties} setting value as a string, or return
	 * {@linkplain String#isEmpty() empty} string.
	 * <p>Shorthand for:
	 * {@snippet lang="java" :
	 * getSystemString(name, ""); // @link substring="getSystemString" target="#getSystemString(String, String)"
	 * }
	 *
	 * @param name setting name
	 * @return setting value or empty string if setting was not found
	 * @since 1.1
	 * @see #getSystemString(String, String)
	 */
	@SuppressWarnings("JavadocDeclaration")
	public static String getSystemString(String name) {
		return getSystemString(name, "");
	}

	/**
	 * Retrieve {@linkplain Settings.Secure secure} setting value as an {@code int}.
	 *
	 * @param cr resolver to access settings with
	 * @param name setting name
	 * @param dfl default value
	 * @return setting value or, {@code dfl} if setting was not found or could not be translated to
	 * an {@code int}
	 * @since 1.1
	 */
	public static int getSecureInt(ContentResolver cr, String name, int dfl) {
		try {
			return Settings.Secure.getInt(cr, name, dfl);
		} catch (Throwable err) {
			Logger.debug(TAG, "failed to get secure int %s", name, err);
			return dfl;
		}
	}

	/**
	 * Retrieve {@linkplain Settings.Secure secure} setting value as an {@code int}, or return
	 * {@code 0}.
	 * <p>Shorthand for:
	 * {@snippet lang="java" :
	 * getSecureInt(cr, name, 0); // @link substring="getSecureInt" target="#getSecureInt(ContentResolver, String, int)"
	 * }
	 *
	 * @param cr resolver to access settings with
	 * @param name setting name
	 * @return setting value or, {@code 0} if setting was not found or could not be translated to
	 * an {@code int}
	 * @since 1.1
	 * @see #getSecureInt(ContentResolver, String, int)
	 */
	@SuppressWarnings("JavadocDeclaration")
	public static int getSecureInt(ContentResolver cr, String name) {
		return getSecureInt(cr, name, 0);
	}

	/**
	 * Retrieve {@linkplain Settings.Secure secure} setting value as a {@code boolean}.
	 * <p>Shorthand for:
	 * {@snippet lang="java" :
	 * return getSecureInt(cr, name, dfl ? 1 : 0) != 0; // @link substring="getSecureInt" target="#getSecureInt(ContentResolver, String, int)"
	 * }
	 *
	 * @param cr resolver to access settings with
	 * @param name setting name
	 * @param dfl default value
	 * @return setting value or, {@code dfl} if setting was not found or could not be translated to
	 * an {@code int}
	 * @since 1.1
	 */
	@SuppressWarnings("JavadocDeclaration")
	public static boolean getSecureBoolean(ContentResolver cr, String name, boolean dfl) {
		return getSecureInt(cr, name, dfl ? 1 : 0) != 0;
	}

	/**
	 * Retrieve {@linkplain Settings.Secure secure} setting value as a {@code boolean}, or return
	 * {@code false}.
	 * <p>Shorthand for:
	 * {@snippet lang="java" :
	 * getSecureBoolean(cr, name, false); // @link substring="getSecureBoolean" target="#getSecureBoolean(ContentResolver, String, boolean)"
	 * }
	 *
	 * @param cr resolver to access settings with
	 * @param name setting name
	 * @return setting value or, {@code false} if setting was not found or could not be translated
	 * to an {@code int}
	 * @since 1.1
	 * @see #getSecureBoolean(ContentResolver, String, boolean)
	 */
	@SuppressWarnings("JavadocDeclaration")
	public static boolean getSecureBoolean(ContentResolver cr, String name) {
		return getSecureBoolean(cr, name, false);
	}

	/**
	 * Retrieve {@linkplain Settings.Secure secure} setting value as string.
	 *
	 * @param cr resolver to access settings with
	 * @param name setting name
	 * @param dfl default value
	 * @return setting value or {@code dfl} if setting was not found
	 * @since 1.1
	 */
	public static String getSecureString(ContentResolver cr, String name, String dfl) {
		try {
			return Preconditions.checkNotNullElse(Settings.Secure.getString(cr, name), dfl);
		} catch (Throwable err) {
			Logger.debug(TAG, "failed to get secure string %s", name, err);
			return dfl;
		}
	}

	/**
	 * Retrieve {@linkplain Settings.Secure secure} setting value as string, or return {@linkplain
	 * String#isEmpty() empty} string.
	 * <p>Shorthand for:
	 * {@snippet lang="java" :
	 * getSecureString(cr, name, ""); // @link substring="getSecureString" target="#getSecureString(ContentResolver, String, String)"
	 * }
	 *
	 * @param cr resolver to access settings with
	 * @param name setting name
	 * @return setting value or empty string if setting was not found
	 * @since 1.1
	 * @see #getSecureString(ContentResolver, String, String)
	 */
	@SuppressWarnings("JavadocDeclaration")
	public static String getSecureString(ContentResolver cr, String name) {
		return getSecureString(cr, name, "");
	}

	/**
	 * Retrieve {@linkplain Settings.Global global} setting value as an {@code int}.
	 *
	 * @param cr resolver to access settings with
	 * @param name setting name
	 * @param dfl default value
	 * @return setting value or, {@code dfl} if setting was not found or could not be translated to
	 * an {@code int}
	 * @since 1.1
	 */
	public static int getGlobalInt(ContentResolver cr, String name, int dfl) {
		try {
			return Settings.Global.getInt(cr, name, dfl);
		} catch (Throwable err) {
			Logger.debug(TAG, "failed to get global int %s", name, err);
			return dfl;
		}
	}

	/**
	 * Retrieve {@linkplain Settings.Global global} setting value as an {@code int}, or return
	 * {@code 0}.
	 * <p>Shorthand for:
	 * {@snippet lang="java" :
	 * getGlobalInt(cr, name, 0); // @link substring="getGlobalInt" target="#getGlobalInt(ContentResolver, String, int)"
	 * }
	 *
	 * @param cr resolver to access settings with
	 * @param name setting name
	 * @return setting value or, {@code 0} if setting was not found or could not be translated to
	 * an {@code int}
	 * @since 1.1
	 * @see #getGlobalInt(ContentResolver, String, int)
	 */
	@SuppressWarnings("JavadocDeclaration")
	public static int getGlobalInt(ContentResolver cr, String name) {
		return getGlobalInt(cr, name, 0);
	}

	/**
	 * Retrieve {@linkplain Settings.Global global} setting value as a {@code boolean}.
	 * <p>Shorthand for:
	 * {@snippet lang="java" :
	 * return getGlobalInt(cr, name, dfl ? 1 : 0) != 0; // @link substring="getGlobalInt" target="#getGlobalInt(ContentResolver, String, boolean)"
	 * }
	 *
	 * @param cr resolver to access settings with
	 * @param name setting name
	 * @param dfl default value
	 * @return setting value or, {@code dfl} if setting was not found or could not be translated to
	 * an {@code int}
	 * @since 1.1
	 */
	@SuppressWarnings("JavadocDeclaration")
	public static boolean getGlobalBoolean(ContentResolver cr, String name, boolean dfl) {
		return getGlobalInt(cr, name, dfl ? 1 : 0) != 0;
	}

	/**
	 * Retrieve {@linkplain Settings.Global global} setting value as a {@code boolean}, or return
	 * {@code false}.
	 * <p>Shorthand for:
	 * {@snippet lang="java" :
	 * getGlobalBoolean(cr, name, false); // @link substring="getGlobalBoolean" target="#getGlobalBoolean(ContentResolver, String, boolean)"
	 * }
	 *
	 * @param cr resolver to access settings with
	 * @param name setting name
	 * @return setting value or, {@code false} if setting was not found or could not be translated
	 * to an {@code int}
	 * @since 1.1
	 * @see #getGlobalBoolean(ContentResolver, String, boolean)
	 */
	public static boolean getGlobalBoolean(ContentResolver cr, String name) {
		return getGlobalBoolean(cr, name, false);
	}

	/**
	 * Retrieve {@linkplain Settings.Global global} setting value as string.
	 *
	 * @param cr resolver to access settings with
	 * @param name setting name
	 * @param dfl default value
	 * @return setting value or {@code dfl} if setting was not found
	 * @since 1.1
	 */
	public static String getGlobalString(ContentResolver cr, String name, String dfl) {
		try {
			return Preconditions.checkNotNullElse(Settings.Global.getString(cr, name), dfl);
		} catch (Throwable err) {
			Logger.debug(TAG, "failed to get global string %s", name, err);
			return dfl;
		}
	}

	/**
	 * Retrieve {@linkplain Settings.Global global} setting value as string, or return {@linkplain
	 * String#isEmpty() empty} string.
	 * <p>Shorthand for:
	 * {@snippet lang="java" :
	 * getGlobalString(cr, name, ""); // @link substring="getGlobalString" target="#getGlobalString(ContentResolver, String, String)"
	 * }
	 *
	 * @param cr resolver to access settings with
	 * @param name setting name
	 * @return setting value or empty string if setting was not found
	 * @since 1.1
	 * @see #getGlobalString(ContentResolver, String, String)
	 */
	@SuppressWarnings("JavadocDeclaration")
	public static String getGlobalString(ContentResolver cr, String name) {
		return getGlobalString(cr, name, "");
	}

	private AndroidSettings() {
	}
}
