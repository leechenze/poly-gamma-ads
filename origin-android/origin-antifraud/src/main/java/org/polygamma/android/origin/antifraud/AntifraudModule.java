// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.antifraud;

import android.app.Activity;
import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Build;
import android.os.Process;
import android.provider.Settings;
import android.util.Pair;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;
import androidx.core.util.Supplier;

import com.google.common.util.concurrent.ListenableFuture;

import org.polygamma.android.origin.adcom.AdCom;
import org.polygamma.android.origin.core.DeviceModule;
import org.polygamma.android.origin.core.Origin;
import org.polygamma.android.origin.core.OriginModule;
import org.polygamma.android.origin.core.OriginModuleEventBus;
import org.polygamma.android.origin.core.OriginModuleEventCallback;
import org.polygamma.android.origin.core.OriginModuleEventName;
import org.polygamma.android.origin.core.RpcModule;
import org.polygamma.android.origin.protobuf.ProtobufField;
import org.polygamma.android.origin.protobuf.ProtobufWriter;
import org.polygamma.android.origin.util.AndroidSettings;
import org.polygamma.android.origin.util.Futures;
import org.polygamma.android.origin.util.ListenableScheduledFuture;
import org.polygamma.android.origin.util.Logger;
import org.polygamma.android.origin.util.Preconditions;
import org.polygamma.android.origin.util.Time;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Module tracking invalid traffic (IVT) status of underlying device.
 * <p>The IVT status of a device is described through a {@linkplain AntifraudStatus descriptor},
 * accessible using {@link #status()}. As the status materially changes, an {@linkplain
 * #STATUS_UPDATE_EVENT update event} is issued, whose data is equal exactly to the return value
 * of {@link #status()}.
 *
 * @since 1.1
 */
public final class AntifraudModule extends OriginModule {

	private static final String TAG = AntifraudModule.class.getSimpleName();

	/**
	 * Antifraud module name.
	 *
	 * @since 1.1
	 */
	public static final String NAME = "origin.antifraud";

	/**
	 * Name of {@linkplain Origin#registerModuleEventCallback(OriginModuleEventCallback, Pair)
	 * event} fired when {@link #status() status} descriptor has materially changed.
	 * <p>This event is non-sticky and fired only when the anti-fraud {@linkplain AntifraudStatus
	 * status} has materially changed. The data associated with the event is the new descriptor.
	 *
	 * @since 1.1
	 */
	public static final @OriginModuleEventName String STATUS_UPDATE_EVENT = "status-update";

	/**
	 * Constant used to represent {@code null} data.
	 */
	private static final Object NULL = new Object();

	/**
	 * Maximum delay, in seconds, between two anti-fraud checks.
	 */
	private static final long MAX_RECHECK_DELAY_SECS = TimeUnit.DAYS.toSeconds(5);

	/**
	 * Minimum delay, in seconds, between two anti-fraud checks.
	 */
	private static final long MIN_RECHECK_DELAY_SECS = 1;

	/**
	 * Default delay, in seconds, to wait before issuing a recheck.
	 */
	private static final long RECHECK_DELAY_SECS = TimeUnit.MINUTES.toSeconds(15);

	/**
	 * Read boot identifier of device.
	 *
	 * @return boot id
	 */
	@WorkerThread
	private static String readBootId() {
		try {
			File file = new File("/proc/sys/kernel/random/boot_id");
			List<String> lines;

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				lines = java.nio.file.Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
			} else {
				lines = new ArrayList<>(1);
				try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
					String ln;

					while ((ln = reader.readLine()) != null)
						lines.add(ln);
				}
			}
			return String.join("", lines);
		} catch (Throwable err) {
			Logger.debug(TAG, "failed to read boot id", err);
			return "";
		}
	}

	/**
	 * Construct a new module {@linkplain Provider provider}, optionally clearing settings.
	 *
	 * @param clearSettings {@code true} if, and only if, module settings should be cleared
	 * @return provider instance
	 */
	@VisibleForTesting
	static Provider<AntifraudModule> ofProvider(boolean clearSettings) {
		return new Provider<AntifraudModule>(AntifraudModule.class) {
			@Override
			protected AntifraudModule load(Origin sdk, Context ctxt) {
				AntifraudModule module = new AntifraudModule(sdk);

				sdk.runInBackground(() -> {
					if (clearSettings)
						module.storeSettings(new CheckResult());
					module.init(ctxt);
				});
				return module;
			}
		};
	}

	/**
	 * Construct a new module {@linkplain Provider provider}.
	 *
	 * @return provider instance
	 * @since 1.1
	 */
	public static Provider<AntifraudModule> ofProvider() {
		return ofProvider(false);
	}

	private final DeviceModule deviceModule;
	private final RpcModule rpcModule;
	private final OriginModuleEventBus statusUpdateEvent;
	private final ConcurrentMap<String, Object> entropyData;
	@VisibleForTesting
	CheckResult lastResult;
	@VisibleForTesting
	@Nullable Pair<Cipher, SecretKeySpec> entropyMachineCrypto;
	private @Nullable OriginModuleEventCallback onModuleEvent;
	@VisibleForTesting
	@Nullable ListenableFuture<CheckResult> checkFuture;
	@VisibleForTesting
	@Nullable ListenableScheduledFuture<?> callCheckFuture;
	private @Nullable ActivityListener activityListener;
	@VisibleForTesting
	boolean destroyed;

	private AntifraudModule(Origin sdk) {
		super(NAME, sdk);
		this.deviceModule = sdk.loadModule(DeviceModule.class);
		this.rpcModule = sdk.loadModule(RpcModule.class);
		this.statusUpdateEvent = super.registerEvent(STATUS_UPDATE_EVENT, false);
		this.entropyData = new ConcurrentHashMap<>();
		this.lastResult = new CheckResult();
	}

	/**
	 * Antifraud status description of underlying device.
	 *
	 * @return status description
	 * @since 1.1
	 */
	public AntifraudStatus status() {
		return this.lastResult.status;
	}

	/**
	 * Add custom entropy data.
	 * <p>If {@code val} is a {@linkplain Supplier supplier}, it is invoked to retrieve the actual
	 * entropy data; otherwise, {@code val} is used as-is to generate entropy.
	 *
	 * @param name datapoint name
	 * @param val datapoint value, possibly {@code null}
	 * @since 1.1
	 */
	public void addEntropyData(String name, @Nullable Object val) {
		this.entropyData.put(name, val);
	}

	/**
	 * Current app activity, if any.
	 *
	 * @return app activity
	 */
	@Nullable Activity currentActivity() {
		ActivityListener listener = this.activityListener;

		return listener == null ? null : listener.current();
	}

	/**
	 * Decrypt machine entropy operations of a {@linkplain CheckResult result}, if required.
	 *
	 * @param res result to decrypt machine entropy operations of
	 * @return result with decrypted machine operations or {@code res} if decryption was not
	 * required
	 */
	private CheckResult decryptMachineEntropyOperations(CheckResult res) {
		if (
			this.entropyMachineCrypto == null ||
			res.entropyMachineOperations == null ||
			res.entropyMachineOperations.length == 0
		) {
			return res;
		}

		Cipher cipher = this.entropyMachineCrypto.first;
		SecretKeySpec key = this.entropyMachineCrypto.second;
		byte[] entMachineOps = null;

		try {
			cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(Arrays.copyOf(
				res.status.digest().getBytes(StandardCharsets.UTF_8),
				16
			)));
			entMachineOps = cipher.doFinal(res.entropyMachineOperations);
		} catch (
			BadPaddingException |
			IllegalBlockSizeException |
			InvalidAlgorithmParameterException |
			InvalidKeyException cause
		) {
			Logger.info(TAG, "failed to decrypt machine entropy operations", cause);
		}
		return new CheckResult(res.recheckTimestampSeconds,res.status, entMachineOps);
	}

	/**
	 * Handle IVT check result.
	 */
	private void onCheckComplete() {
		long now = Time.nowRealtimeSeconds();
		ListenableFuture<CheckResult> fut = this.checkFuture;
		CheckResult res;

		try {
			res = fut == null ? null : fut.get();
			if (res == null) {
				Logger.debug(TAG, "previous status is consistent");
				res = new CheckResult(now + RECHECK_DELAY_SECS, this.lastResult.status, null);
			} else {
				res = this.decryptMachineEntropyOperations(res);
				if (!res.status.equals(this.lastResult.status)) {
					this.statusUpdateEvent.submit(res.status);
					Logger.info(TAG, "updated status");
				}
			}
		} catch (Exception err) {
			Logger.info(TAG, "check call failed, rescheduling", err);
			res = this.lastResult.withRecheckTimestampSeconds(now + RECHECK_DELAY_SECS);
		}

		synchronized (this) {
			if (this.checkFuture != fut)
				return;
			this.checkFuture = null;
		}

		long delay = res.recheckDelaySeconds();

		if (Long.compareUnsigned(delay, MIN_RECHECK_DELAY_SECS) < 0)
			res = res.withRecheckTimestampSeconds(now + MIN_RECHECK_DELAY_SECS);
		else if (Long.compareUnsigned(delay, MAX_RECHECK_DELAY_SECS) > 0)
			res = res.withRecheckTimestampSeconds(now + MAX_RECHECK_DELAY_SECS);
		this.lastResult = res;
		super.storeSettings(res);
		this.scheduleCheckCall(false);
		Logger.debug(TAG, "antifraud check result: %s", res);
	}

	private void prepareCheckArguments(ProtobufWriter writer) {
		Context ctxt = super.tryContext();
		ContentResolver contRes = ctxt == null ? null : ctxt.getContentResolver();

		writer.writeLen(CheckArgumentTags.APP, super.sdk().app());
		writer.writeLen(CheckArgumentTags.DEVICE, this.deviceModule.device());
		writer.writeString(CheckArgumentTags.ADCOMVER, AdCom.DOMAIN_VERSION);
		writer.writeString(CheckArgumentTags.DIGEST, this.lastResult.status.digest());
		writer.writeFixed64(CheckArgumentTags.TIMESTAMPSEC, Time.nowUtcSeconds());
		writer.writeString(CheckArgumentTags.BOOTID, readBootId());
		writer.writeInt32(CheckArgumentTags.PID, Process.myPid());
		writer.writeInt32(CheckArgumentTags.UID, Process.myUid());
		writer.writeString(CheckArgumentTags.BUILDTAGS, Build.TAGS);
		writer.writeString(CheckArgumentTags.BUILDFP, Build.FINGERPRINT);
		writer.writeString(CheckArgumentTags.BUILDPROD, Build.PRODUCT);
		writer.writeString(CheckArgumentTags.BUILDHW, Build.HARDWARE);
		writer.writeString(CheckArgumentTags.BUILDDISP, Build.DISPLAY);
		writer.writeString(CheckArgumentTags.BUILDRADIO, Build.getRadioVersion());

		if (this.entropyMachineCrypto != null) {
			writer.writeInt32(
				CheckArgumentTags.ENTMACHINEENC,
				CheckArgumentTags.ENCRYPTION_AES
			);
			writer.writeBytes(
				CheckArgumentTags.ENTMACHINEKEY,
				this.entropyMachineCrypto.second.getEncoded()
			);
		}
		if (contRes != null) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
				writer.writeInt32(
					CheckArgumentTags.BOOTCNT,
					AndroidSettings.getGlobalInt(contRes, Settings.Global.BOOT_COUNT)
				);
			}
			writer.writeBool(CheckArgumentTags.ADB, AndroidSettings.getGlobalBoolean(
				contRes,
				Settings.Global.ADB_ENABLED
			));
			writer.writeBool(CheckArgumentTags.AIRMODE, AndroidSettings.getGlobalBoolean(
				contRes,
				Settings.Global.AIRPLANE_MODE_ON
			));
			writer.writeBool(CheckArgumentTags.AUTOTZ, AndroidSettings.getGlobalBoolean(
				contRes,
				Settings.Global.AUTO_TIME_ZONE
			));
			writer.writeBool(CheckArgumentTags.ACCESSIB, AndroidSettings.getSecureBoolean(
				contRes,
				Settings.Secure.ACCESSIBILITY_ENABLED
			));
		}

		long cookie = writer.beginWriteLen(CheckArgumentTags.ENTROPY);

		for (Map.Entry<String, Object> ent : this.entropyData.entrySet()) {
			writer.writeString(
				ProtobufField.ofString(1),
				ent.getKey().isEmpty() ? "XW" : ent.getKey()
			);
			writer.writeBytes(
				ProtobufField.ofBytes(2),
				Entropy.ofValue(ent.getValue() == NULL ? null : ent.getValue())
			);
		}

		if (this.lastResult.entropyMachineOperations != null) {
			writer.writeString(ProtobufField.ofString(1), "machine");
			writer.writeBytes(ProtobufField.ofBytes(2), Entropy.ofValue(EntropyMachine.generate(
				this,
				ByteBuffer.wrap(this.lastResult.entropyMachineOperations)
			)));
		}
		writer.endWriteLen(cookie);
	}

	/**
	 * Call IVT check procedure.
	 */
	private void callCheck() {
		this.callCheckFuture = null;

		if (this.deviceModule.device() == null || (
			this.entropyData.isEmpty() &&
			this.lastResult.entropyMachineOperations == null
		)) {
			Logger.debug(TAG, "environment not ready for check, rescheduling");
			this.scheduleCheckCall(true);
		} else {
			this.checkFuture = this.rpcModule.call(
				"ivt",
				"check",
				this::prepareCheckArguments,
				CheckResult::ofProtobuf
			);
			Futures.addDirectListener(this.checkFuture, this::onCheckComplete);
			Logger.debug(TAG, "check called");
		}
	}

	/**
	 * Schedule next check call.
	 *
	 * @param imm {@code true} if, and only if, call should be scheduled immediately
	 */
	private void scheduleCheckCall(boolean imm) {
		long delay =
			imm ? MIN_RECHECK_DELAY_SECS :
			Math.max(MIN_RECHECK_DELAY_SECS, Math.min(
				MAX_RECHECK_DELAY_SECS,
				this.lastResult.recheckDelaySeconds()
			));

		synchronized (this) {
			if (this.destroyed || this.callCheckFuture != null || this.checkFuture != null)
				return;

			if (this.lastResult.recheckTimestampSeconds == 0L) {
				this.lastResult = this.lastResult
					.withRecheckTimestampSeconds(Time.nowRealtimeSeconds());
			}
			//noinspection resource
			this.callCheckFuture =
				super.sdk().backgroundExecutor()
					.schedule(this::callCheck, delay, TimeUnit.SECONDS);
			Logger.debug(TAG, "scheduled check call in %ss", delay);
		}
	}

	/**
	 * Initialize module.
	 *
	 * @param ctxt context to initialize with
	 */
	@WorkerThread
	private void init(Context ctxt) {
		try {
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
			KeyGenerator keyGen = KeyGenerator.getInstance("AES");

			keyGen.init(256);
			this.entropyMachineCrypto = new Pair<>(cipher, new SecretKeySpec(
				keyGen.generateKey().getEncoded(),
				"AES"
			));
			Logger.debug(TAG, "Using AES encrypted machine entropy operations");
		} catch (NoSuchPaddingException | NoSuchAlgorithmException cause) {
			Logger.info(TAG, "AES not supported", cause);
		}

		synchronized (this) {
			if (this.destroyed)
				return;

			CheckResult lastRes = super.loadSettings(CheckResult::ofProtobuf);

			if (lastRes != null) {
				this.lastResult =
					lastRes.recheckDelaySeconds() <= MAX_RECHECK_DELAY_SECS ? lastRes :
					lastRes.withRecheckTimestampSeconds(Time.nowRealtimeSeconds());
			}
			this.onModuleEvent = (mod, name, data, _when) -> {
				if (mod != this && mod != this.deviceModule) {
					this.entropyData.put(
						String.format(Locale.ROOT, "%s/%s", mod.name(), name),
						Preconditions.checkNotNullElse(data, NULL)
					);
				}
			};
			super.sdk().registerModuleEventCallback(this.onModuleEvent, null);
		}
		super.sdk().runInForeground(() -> {
			synchronized (this) {
				if (!this.destroyed) {
					this.activityListener = new ActivityListener();
					((Application) ctxt).registerActivityLifecycleCallbacks(this.activityListener);
				}
			}
		});
		this.scheduleCheckCall(false);
	}

	@Override
	protected void destroy() {
		ArrayList<Future<?>> futs = new ArrayList<>(2);

		synchronized (this) {
			if (this.destroyed)
				return;

			futs.add(this.checkFuture);
			futs.add(this.callCheckFuture);
			this.checkFuture = null;
			this.callCheckFuture = null;
			this.destroyed = true;
		}

		for (Future<?> fut : futs)
			Futures.cancel(fut, false);

		if (this.onModuleEvent != null) {
			super.sdk().unregisterModuleEventCallback(this.onModuleEvent, null);
			this.onModuleEvent = null;
		}

		if (this.activityListener != null) {
			super.acceptContext(
				ctxt ->
					((Application) ctxt)
						.unregisterActivityLifecycleCallbacks(this.activityListener)
			);
			this.activityListener = null;
		}
	}
}
