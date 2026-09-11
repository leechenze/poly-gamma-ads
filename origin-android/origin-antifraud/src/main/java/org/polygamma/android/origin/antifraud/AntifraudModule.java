// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.antifraud;

import static org.polygamma.android.origin.antifraud.CheckWire.ERROR_RECHECK_DELAY_SECONDS;
import static org.polygamma.android.origin.antifraud.CheckWire.RPC_PROCEDURE_ID;

import android.app.Application;
import android.content.Context;
import android.util.ArrayMap;
import android.util.Pair;

import androidx.annotation.GuardedBy;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;

import com.google.common.util.concurrent.ListenableFuture;

import org.polygamma.android.origin.adcom.context.Device;
import org.polygamma.android.origin.core.DeviceModule;
import org.polygamma.android.origin.core.Origin;
import org.polygamma.android.origin.core.OriginModule;
import org.polygamma.android.origin.core.OriginModuleEventBus;
import org.polygamma.android.origin.core.OriginModuleEventCallback;
import org.polygamma.android.origin.core.OriginModuleEventName;
import org.polygamma.android.origin.core.RegulationsModule;
import org.polygamma.android.origin.core.RpcModule;
import org.polygamma.android.origin.protobuf.ProtobufEncoder;
import org.polygamma.android.origin.util.Futures;
import org.polygamma.android.origin.util.Logger;
import org.polygamma.android.origin.util.Preconditions;
import org.polygamma.android.origin.util.Supplier;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

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

	// Constant used to represent `null` data.
	static final Object NULL = new Object();

	// Minimum number of sensor samples we want to measure before invoking check procedure.
	private static final int MIN_SENSOR_SAMPLE_COUNT = 15;
	// Period at which we measure from samples, in microseconds. (350msec)
	private static final int SENSOR_SAMPLE_PERIOD_MICROSECONDS = 350000;

	// Delta at which we should begin measuring from sensors *BEFORE* invoking check procedure.
	@VisibleForTesting
	static final long SENSORS_CHECK_DELTA_SECONDS =
		Math.max(1L, TimeUnit.MICROSECONDS.toSeconds(
			((long) SENSOR_SAMPLE_PERIOD_MICROSECONDS) *
			MIN_SENSOR_SAMPLE_COUNT
		));

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

				if (clearSettings)
					module.storeSettings(ByteBuffer.allocate(0));
				module.init(ctxt);
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
	private final RegulationsModule regulationsModule;
	private final RpcModule rpcModule;
	private final OriginModuleEventBus statusUpdateEvent;

	// Mapping of sensitive entropy name to entropy value.
	private final ConcurrentMap<String, Object> sensitiveEntropies;

	// Current check result.
	@VisibleForTesting
	CheckSessionResult checkResult;

	/*
	 * Callback which adds module descriptors to `sensitiveEntropies`, this is `null` when we're
	 * destroyed.
	 */
	@VisibleForTesting
	@Nullable OriginModuleEventCallback onModuleEvent;

	/*
	 * Reference to current activity, this is `null` if we've been destroyed or were unable to
	 * attach activity lifecycle callbacks to owning application context.
	 */
	private @Nullable CurrentActivityReference currentActivityReference;

	/*
	 * Service we poll sensor entropies from. This is `null` when we don't need to capture sensor
	 * measurements.
	 */
	@GuardedBy("this")
	@VisibleForTesting
	@Nullable SensorEntropyService sensors;

	// Current check session. This is `null` when we don't have a check active.
	@GuardedBy("this")
	@VisibleForTesting
	@Nullable CheckSession checkSession;

	/*
	 * Call check future. This will be `null` if no check is pending, `NULL` if check process is
	 * executing inline, or `ListenableFuture` if check process is awaiting some I/O.
	 */
	@VisibleForTesting
	@Nullable Object callCheckFuture;

	// Module has been destroyed.
	@GuardedBy("this")
	@VisibleForTesting
	boolean destroyed;

	private AntifraudModule(Origin sdk) {
		super(NAME, sdk);
		this.deviceModule = sdk.loadModule(DeviceModule.class);
		this.regulationsModule = sdk.loadModule(RegulationsModule.class);
		this.rpcModule = sdk.loadModule(RpcModule.class);
		this.statusUpdateEvent = super.registerEvent(STATUS_UPDATE_EVENT, false);
		this.sensitiveEntropies = new ConcurrentHashMap<>();
		this.checkResult =
			new CheckSessionResult(0, new AntifraudStatus(null, CheckWire.IvtRatingUnknown, 0));
	}

	/**
	 * Antifraud status description of underlying device.
	 *
	 * @return status description
	 * @since 1.1
	 */
	public AntifraudStatus status() {
		return this.checkResult.status;
	}

	/**
	 * Add custom sensitive entropy data.
	 * <p>If {@code val} is a {@linkplain Supplier supplier}, it is invoked to retrieve the actual
	 * entropy data; otherwise, {@code val} is used as-is to generate entropy.
	 *
	 * @param name data point name
	 * @param val data point value, possibly {@code null}
	 * @since 1.1
	 */
	public void addEntropyData(String name, @Nullable Object val) {
		this.sensitiveEntropies.put(name, Preconditions.checkNotNullElse(val, NULL));
	}

	/*
	 * Update our check result to `curr`. If `curr.status` is not equal to current anti-fraud
	 * status, `curr.status` is published. In all cases, `curr` is serialized in settings.
	 * If next check delay is greater than `SENSORS_CHECK_DELTA_SECONDS*2`, sensor measurement
	 * service is paused. In all cases, `scheduleCheckCall()` is invoked.
	 */
	private void updateCheckResult(CheckSessionResult curr) {
		CheckSessionResult prev = this.checkResult;
		ProtobufEncoder enc = ProtobufEncoder.of();

		this.checkResult = curr;
		curr.toProtobuf(enc);
		super.storeSettings(enc.asBuffer());
		synchronized (this) {
			if (this.destroyed)
				return;
			// Inline process invoking this implies inline execution is complete.
			if (this.callCheckFuture == NULL)
				this.callCheckFuture = null;
			if (!curr.status.equals(prev.status))
				this.statusUpdateEvent.submit(curr.status);
			if (
				curr.nextCheckDelaySeconds() >= (SENSORS_CHECK_DELTA_SECONDS * 2) &&
				this.sensors != null
			) {
				this.sensors.pause();
			}
		}
		this.scheduleCheckCall();
	}

	/*
	 * Handle completion of check call. This will push the check session forward, initiate any
	 * subsequent invocations as needed, and if session is complete, update our check result.
	 */
	private void onCheckCallResult() {
		CheckSession sess;
		Object fut;

		synchronized (this) {
			sess = this.checkSession;
			fut = this.callCheckFuture;
			this.checkSession = null;
			if (sess == null || !(fut instanceof ListenableFuture<?>) || this.destroyed)
				return;
			this.callCheckFuture = NULL;
		}

		try {
			Object res = ((ListenableFuture<?>) fut).get();

			if (res == null) {
				/*
				 * `sess::apply()` doesn't get called when service returns 204, manually let
				 * session know we received empty response
				 */
				sess.apply(null);
			} else {
				/*
				 * `sess::apply()` returns itself, so when RPC module decodes the remote response,
				 * the future should've completed with `CheckSession` if this is the correct
				 * future.
				 */
				Preconditions.checkState(res instanceof CheckSession);
			}

			ByteBuffer args = sess.nextCheckArguments();

			if (sess.hasEnded()) {
				this.updateCheckResult(sess.result());
				if (args != null)
					this.rpcModule.callVoidWithBytes(RPC_PROCEDURE_ID, args);
				Logger.debug(TAG, "check session ended with %s", this.checkResult);
			} else {
				synchronized (this) {
					this.doCallCheck(sess, args);
				}
			}
		} catch (Exception cause) {
			Logger.info(TAG, "check call failed", cause);
			this.updateCheckResult(
				this.checkResult.withNextCheckDelaySeconds(ERROR_RECHECK_DELAY_SECONDS)
			);
		}
	}

	// Invoke remote check procedure with session `sess`.
	@GuardedBy("this")
	private void doCallCheck(CheckSession sess, @Nullable ByteBuffer args) {
		Preconditions.checkState(!this.destroyed && this.checkSession == null);

		ListenableFuture<?> fut =
			args == null ? this.rpcModule.callWithoutArguments(RPC_PROCEDURE_ID, sess) :
			this.rpcModule.callWithBytes(RPC_PROCEDURE_ID, args, sess);

		this.checkSession = sess;
		this.callCheckFuture = fut;
		fut.addListener(this::onCheckCallResult, super.sdk().backgroundExecutor());
		Logger.debug(TAG, "check called %s arguments", args == null ? "without" : "with");
	}

	// Invoke remote check procedure if possible.
	@WorkerThread
	private void callCheck() {
		Device dev = this.deviceModule.device();

		synchronized (this) {
			if (this.destroyed) {
				// we're destroyed, clear out device so we punt
				dev = null;
			} else {
				this.callCheckFuture = NULL;
			}
		}
		if (dev == null) {
			this.scheduleCheckCall();
			return;
		}

		ArrayMap<String, Object> senEnt = new ArrayMap<>(this.sensitiveEntropies.size());

		senEnt.putAll(this.sensitiveEntropies);
		for (int i = 0; i < senEnt.size(); i++) {
			if (senEnt.valueAt(i) == NULL)
				senEnt.setValueAt(i, null);
		}

		CheckSession sess;
		ByteBuffer args;

		try {
			sess = CheckSession.open(
				super.sdk(),
				this.currentActivityReference,
				this.checkResult,
				this.regulationsModule.regs(), dev, this.sensors,
				senEnt
			);
			args = sess.nextCheckArguments();
		} catch (Exception cause) {
			Logger.warn(TAG, "failed to open check session", cause);
			this.updateCheckResult(
				this.checkResult.withNextCheckDelaySeconds(ERROR_RECHECK_DELAY_SECONDS)
			);
			return;
		}
		Logger.debug(TAG, "check session opened, invoking check RPC");
		try {
			synchronized (this) {
				this.doCallCheck(sess, args);
			}
		} catch (Exception cause) {
			Logger.debug(TAG, "failed to call check", cause);
			this.updateCheckResult(
				this.checkResult
					.withNextCheckDelaySeconds(ERROR_RECHECK_DELAY_SECONDS)
			);
		}
	}

	/*
	 * Start measuring entropy from sensors, creating service if one does not already exist, and
	 * schedule check call after `SENSORS_CHECK_DELTA_SECONDS`.
	 */
	private void startSensorsAndScheduleCallCheck() {
		synchronized (this) {
			if (this.destroyed)
				return;

			SensorEntropyService svc = this.sensors;

			try {
				if (svc == null) {
					svc = SensorEntropyService.create(
						super.sdk(),
						SENSOR_SAMPLE_PERIOD_MICROSECONDS
					);
					this.sensors = svc;
				}
				svc.unpause();
			} catch (Exception cause) {
				Logger.debug(TAG, "failed to start sensor entropy service", cause);
			} finally {
				Logger.debug(
					TAG,
					"started sensor entropy service, check call scheduled delay=%ds",
					SENSORS_CHECK_DELTA_SECONDS
				);
				//noinspection resource
				this.callCheckFuture = super.sdk()
					.backgroundIoExecutor()
					.schedule(this::callCheck, SENSORS_CHECK_DELTA_SECONDS, TimeUnit.SECONDS);
			}
		}
	}

	// Schedule check call invocation.
	private void scheduleCheckCall() {
		synchronized (this) {
			if (this.destroyed || this.callCheckFuture != null)
				return;

			long delay = this.checkResult.nextCheckDelaySeconds();
			SensorEntropyService sensors = this.sensors;

			/*
			 * If we haven't created the sensor entropy service, or if it's paused, we need to
			 * adjust the delay such that we create and start the service, collect enough sensor
			 * measurements, then make the check call.
			 */
			if (sensors == null || sensors.isPaused()) {
				Logger.debug(TAG, "insufficient sensor entropy to schedule check call");
				delay -= SENSORS_CHECK_DELTA_SECONDS;
				if (delay < 0) {
					// Check delay is too soon, so just start it immediately.
					this.startSensorsAndScheduleCallCheck();
				} else {
					//noinspection resource
					this.callCheckFuture = super.sdk()
						.backgroundExecutor()
						.schedule(this::startSensorsAndScheduleCallCheck, delay, TimeUnit.SECONDS);
				}
			} else {
				Logger.debug(TAG, "check call scheduled, delay=%ds", delay);
				//noinspection resource
				this.callCheckFuture = super.sdk()
					.backgroundIoExecutor()
					.schedule(this::callCheck, Math.max(delay, 1L), TimeUnit.SECONDS);
			}
		}
	}

	// Initialize module.
	private void init(Context ctxt) {
		CheckSessionResult lastRes = super.loadSettings(CheckSessionResult::ofProtobuf);

		if (lastRes != null) {
			this.checkResult =
				Long.compareUnsigned(
					lastRes.nextCheckDelaySeconds(),
					CheckWire.MAX_RECHECK_DELAY_SECONDS
				) <= 0 ? lastRes :
				new CheckSessionResult(0, lastRes.status);
		}

		this.onModuleEvent = (mod, name, data, _when) -> {
			if (mod != this && mod != this.deviceModule && mod != this.regulationsModule) {
				data = Preconditions.checkNotNullElse(data, NULL);
				this.sensitiveEntropies.put(mod.name() + '/' + name, data);
			}
		};
		super.sdk().registerModuleEventCallback(this.onModuleEvent, null);

		try {
			CurrentActivityReference actRef = new CurrentActivityReference();

			((Application) ctxt).registerActivityLifecycleCallbacks(actRef);
			this.currentActivityReference = actRef;
		} catch (Throwable cause) {
			Logger.debug(TAG, "failed to register activity lifecycle callbacks", cause);
		}

		this.scheduleCheckCall();
	}

	@Override
	protected void destroy() {
		Object callCheckFut;

		synchronized (this) {
			if (this.destroyed)
				return;
			callCheckFut = this.callCheckFuture;
			this.callCheckFuture = null;
			this.destroyed = true;
		}

		if (callCheckFut instanceof ListenableFuture<?>) {
			Futures.cancel((ListenableFuture<?>) callCheckFut, false);
			Futures.awaitUnchecked((ListenableFuture<?>) callCheckFut);
		}

		this.checkSession = null;
		if (this.sensors != null) {
			this.sensors.shutdown();
			this.sensors = null;
		}

		if (this.onModuleEvent != null) {
			super.sdk().unregisterModuleEventCallback(this.onModuleEvent, null);
			this.onModuleEvent = null;
		}

		if (this.currentActivityReference != null) {
			super.acceptContext(
				ctxt ->
					((Application) ctxt)
						.unregisterActivityLifecycleCallbacks(this.currentActivityReference)
			);
			this.currentActivityReference = null;
		}
	}
}
