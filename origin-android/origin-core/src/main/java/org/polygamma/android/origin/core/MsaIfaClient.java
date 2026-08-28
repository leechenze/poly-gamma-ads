// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.core;

import android.content.Context;
import android.util.Pair;

import com.bun.miitmdid.core.MdidSdkHelper;
import com.bun.miitmdid.interfaces.IIdentifierListener;
import com.bun.miitmdid.interfaces.IdSupplier;

import org.polygamma.android.origin.util.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Locale;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Mobile Security Alliance (MSA) Open Advertising Id client implementation, for MSA SDK version
 * 2.0 and above.
 *
 * @see IfaClient#ofMsa()
 */
final class MsaIfaClient implements IfaClient {

	private static final String TAG = MsaIfaClient.class.getSimpleName();

	private static final class OaidListener implements IIdentifierListener {
		private final LinkedBlockingQueue<Pair<String, Boolean>> ifa;

		OaidListener() {
			this.ifa = new LinkedBlockingQueue<>();
		}

		@Override
		public void onSupport(IdSupplier supplier) {
			try {
				if (supplier == null || supplier.getOAID() == null)
					this.ifa.add(new Pair<>("", Boolean.FALSE));
				else
					this.ifa.add(new Pair<>(supplier.getOAID(), supplier.isLimited()));
			} catch (Throwable cause) {
				this.ifa.add(new Pair<>("", Boolean.FALSE));
			}
		}

		@Override
		public void OnSupport(boolean exists, IdSupplier supplier) {
			this.onSupport(supplier);
		}
	}

	private boolean initialized;

	/**
	 * Construct new GMS advertising id client.
	 */
	public MsaIfaClient() {
	}

	@Override
	public String type() {
		return "oaid";
	}

	private static String readCert(Context ctxt) throws IOException {
		String certFile = String.format(Locale.ROOT, "%s.cert.pem", ctxt.getPackageName());

		try (
			BufferedReader reader =
				new BufferedReader(new InputStreamReader(ctxt.getAssets().open(certFile)))
		) {
			StringBuilder cert = new StringBuilder();
			String line;

			while ((line = reader.readLine()) != null) {
				cert.append(line)
					.append('\n');
			}
			return cert.toString();
		}
	}

	private void initialize(Context ctxt) throws Exception {
		System.loadLibrary("msaoaidsec");

		MdidSdkHelper.setGlobalTimeout(10000);
		this.initialized = MdidSdkHelper.InitCert(ctxt, readCert(ctxt));
		if (!this.initialized)
			Logger.info(TAG, "MSA SDK not available");
	}

	@Override
	public Pair<String, Boolean> sendRequest(Origin sdk, Context ctxt) throws Exception {
		if (!this.initialized) {
			try {
				initialize(ctxt);
			} catch (Throwable cause) {
				Logger.info(TAG, "failed to initialize MSA SDK", cause);
			}
		}

		OaidListener listener = new OaidListener();

		MdidSdkHelper.InitSdk(ctxt, BuildConfig.DEBUG, listener);
		return listener.ifa.poll(1, TimeUnit.SECONDS);
	}
}
