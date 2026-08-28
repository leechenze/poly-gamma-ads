// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.ads;

import android.content.Context;
import android.database.DefaultDatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;

import androidx.annotation.AnyThread;
import androidx.annotation.RestrictTo;
import androidx.annotation.WorkerThread;

/**
 * Database of cached ads, placements, and ad event trackers.
 */
@WorkerThread
final class AdDatabase extends SQLiteOpenHelper {

	/**
	 * Database version.
	 */
	private static final int VERSION	= 1;

	/**
	 * Open ad database.
	 *
	 * @param ctxt owning context
	 * @param name database name
	 * @return resulting database
	 */
	@AnyThread
	static AdDatabase open(Context ctxt, String name) {
		return new AdDatabase(ctxt, name);
	}

	@AnyThread
	private AdDatabase(Context ctxt, String name) {
		super(ctxt, name, null, VERSION, new DefaultDatabaseErrorHandler());

		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
			super.setWriteAheadLoggingEnabled(true);
		} else {
			super.setOpenParams(
				(new SQLiteDatabase.OpenParams.Builder())
					.setErrorHandler(new DefaultDatabaseErrorHandler())
					.addOpenFlags(
						SQLiteDatabase.CREATE_IF_NECESSARY |
						SQLiteDatabase.ENABLE_WRITE_AHEAD_LOGGING |
						SQLiteDatabase.OPEN_READWRITE
					)
					//noinspection WrongConstant
					.setSynchronousMode("NORMAL")
					.build()
			);
		}
	}

	@Override
	@RestrictTo(RestrictTo.Scope.SUBCLASSES)
	public void onCreate(SQLiteDatabase db) {
	}

	@Override
	@RestrictTo(RestrictTo.Scope.SUBCLASSES)
	public void onDowngrade(SQLiteDatabase db, int oldVer, int newVer) {
		this.onCreate(db);
	}

	@Override
	@RestrictTo(RestrictTo.Scope.SUBCLASSES)
	public void onUpgrade(SQLiteDatabase db, int oldVer, int newVer) {
		this.onDowngrade(db, oldVer, newVer);
	}
}
