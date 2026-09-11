// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.antifraud;

import static org.polygamma.android.origin.antifraud.CheckWire.MIN_SENSOR_TYPE;
import static org.polygamma.android.origin.antifraud.CheckWire.PlainEntropy_SENSOR;
import static org.polygamma.android.origin.antifraud.CheckWire.SENSOR_TYPE_RANGE;
import static org.polygamma.android.origin.antifraud.CheckWire.SensorAccelMag;
import static org.polygamma.android.origin.antifraud.CheckWire.SensorBattCurrMua;
import static org.polygamma.android.origin.antifraud.CheckWire.SensorBattLevelPct;
import static org.polygamma.android.origin.antifraud.CheckWire.SensorBattTempC;
import static org.polygamma.android.origin.antifraud.CheckWire.SensorBattVoltMv;
import static org.polygamma.android.origin.antifraud.CheckWire.SensorEntropy_COUNT;
import static org.polygamma.android.origin.antifraud.CheckWire.SensorEntropy_D2;
import static org.polygamma.android.origin.antifraud.CheckWire.SensorEntropy_MU;
import static org.polygamma.android.origin.antifraud.CheckWire.SensorEntropy_TYPE;
import static org.polygamma.android.origin.antifraud.CheckWire.SensorLightIllumSilux;
import static org.polygamma.android.origin.antifraud.CheckWire.SensorScreenProxCm;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.BatteryManager;
import android.os.Build;
import android.util.Pair;

import androidx.annotation.GuardedBy;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import org.polygamma.android.origin.antifraud.CheckWire.SensorType;
import org.polygamma.android.origin.core.Origin;
import org.polygamma.android.origin.protobuf.ProtobufEncoder;
import org.polygamma.android.origin.util.AndroidContexts;
import org.polygamma.android.origin.util.ExecutingService;
import org.polygamma.android.origin.util.Logger;
import org.polygamma.android.origin.util.Preconditions;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Service collecting entropy from various low-power sensors.
 */
final class SensorEntropyService extends ExecutingService implements SensorEventListener {

	private static final String TAG = SensorEntropyService.class.getSimpleName();

	/**
	 * Create service entropy service.
	 *
	 * @param sdk owning SDK
	 * @param samplePeriodUsec period, in micro seconds, between capturing samples
	 * @return resulting service
	 * @throws IllegalArgumentException {@code samplePeriodSecs} is negative
	 * @throws IllegalStateException application context has been garbage collected
	 */
	static SensorEntropyService create(Origin sdk, int samplePeriodUsec) {
		Preconditions.checkArgument(samplePeriodUsec >= 0);
		return new SensorEntropyService(sdk, samplePeriodUsec);
	}

	// Retrieve mean for `type` sensor.
	@VisibleForTesting
	static double getMean(@SensorType int type, double[] meansAndD2) {
		return meansAndD2[(type - MIN_SENSOR_TYPE) * 2 + 0];
	}

	// Retrieve d^2 for `type` sensor.
	@VisibleForTesting
	static double getD2(@SensorType int type, double[] meansAndD2) {
		return meansAndD2[(type - MIN_SENSOR_TYPE) * 2 + 1];
	}

	// Set mean and d^2 for `type` sensor.
	private static void
	setMeanAndD2(@SensorType int type, double[] meansAndD2, double mu, double d2) {
		meansAndD2[(type - MIN_SENSOR_TYPE) * 2 + 0] = mu;
		meansAndD2[(type - MIN_SENSOR_TYPE) * 2 + 1] = d2;
	}

	// a * b + c
	private static double fma(double a, double b, double c) {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ? Math.fma(a, b, c) :
			(a * b) + c;
	}

	private final Origin sdk;
	@VisibleForTesting
	final @Nullable BatteryManager battery;
	@VisibleForTesting
	final @Nullable SensorManager sensors;
	private final ReadWriteLock updateLock;
	@GuardedBy("this.updateLock")
	@VisibleForTesting
	final long[] counts;
	@GuardedBy("this.updateLock")
	@VisibleForTesting
	final double[] meansAndD2;
	private final int samplePeriodMicroseconds;
	@VisibleForTesting
	boolean batteryCharging;
	private boolean paused;

	@VisibleForTesting
	SensorEntropyService(Origin sdk, int samplePeriodUsec) {
		super(sdk.backgroundExecutor());

		Context ctxt = sdk.context();

		this.sdk = sdk;
		this.battery =
			Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP ? null :
			AndroidContexts.systemServiceOf(ctxt, BatteryManager.class, Context.BATTERY_SERVICE);
		this.sensors =
			AndroidContexts.systemServiceOf(ctxt, SensorManager.class, Context.SENSOR_SERVICE);
		this.updateLock = new ReentrantReadWriteLock();
		this.samplePeriodMicroseconds = samplePeriodUsec;
		this.counts = new long[SENSOR_TYPE_RANGE];
		this.meansAndD2 = new double[SENSOR_TYPE_RANGE * 2];
		this.paused = true;
	}

	// Register ourself as a listener for the sensors we're interested in.
	private void registerSensors() {
		if (this.sensors == null)
			return;

		int samplePeriodUs =
			this.samplePeriodMicroseconds == 0 ? SensorManager.SENSOR_DELAY_NORMAL :
			this.samplePeriodMicroseconds;

		for (int type : new int[] {
			Sensor.TYPE_ACCELEROMETER,
			Sensor.TYPE_LIGHT,
			Sensor.TYPE_PROXIMITY
		}) {
			Sensor sensor =
				Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP ?
				this.sensors.getDefaultSensor(type) :
				this.sensors.getDefaultSensor(type, false);

			if (sensor != null && this.sensors.registerListener(this, sensor, samplePeriodUs))
				Logger.debug(TAG, "registered listener to sensor %d", type);
			else
				Logger.debug(TAG, "failed to register listener to sensor %d", type);
		}
	}

	/**
	 * Test whether sensor measurement is paused.
	 *
	 * @return {@code true} if, and only if, sensors are not being measured from
	 */
	boolean isPaused() {
		return this.paused;
	}

	/**
	 * Pause measuring sensors.
	 */
	void pause() {
		Lock write = this.updateLock.writeLock();

		write.lock();
		try {
			if (this.paused)
				return;
			this.paused = true;
			if (this.sensors != null)
				this.sensors.unregisterListener(this);
		} finally {
			write.unlock();
		}
	}

	/**
	 * Unpause measuring sensors.
	 */
	void unpause() {
		Lock write = this.updateLock.writeLock();

		write.lock();
		try {
			if (!this.paused)
				return;
			this.paused = false;
			if (super.schedule())
				this.registerSensors();
		} finally {
			write.unlock();
		}
	}

	// Snapshot current measurements.
	private Pair<long[], double[]> snapshot() {
		long[] counts = new long[SENSOR_TYPE_RANGE];
		double[] meansAndD2 = new double[SENSOR_TYPE_RANGE * 2];
		Lock write = this.updateLock.writeLock();

		write.lock();
		try {
			System.arraycopy(this.counts, 0, counts, 0, SENSOR_TYPE_RANGE);
			System.arraycopy(this.meansAndD2, 0, meansAndD2, 0, SENSOR_TYPE_RANGE * 2);
		} finally {
			write.unlock();
		}
		return new Pair<>(counts, meansAndD2);
	}

	/**
	 * Test whether battery is charging according to last measurement, if any.
	 *
	 * @return {@code true} if, and only if, battery is charging
	 */
	boolean isBatteryCharging() {
		return this.batteryCharging;
	}

	/**
	 * Encode current sensor measurements into {@code SensorEntropy} messages of {@code
	 * PlainEntropy.sensor} field.
	 *
	 * @param enc encoder to encode into
	 */
	void encodePlainEntropySensors(ProtobufEncoder enc) {
		ProtobufEncoder sensorEnc = ProtobufEncoder.of();
		Pair<long[], double[]> snapshot = this.snapshot();
		long[] counts = snapshot.first;
		double[] muAndD2 = snapshot.second;

		for (int i = 0; i < SENSOR_TYPE_RANGE; i++) {
			if (counts[i] == 0L)
				continue;

			int type = MIN_SENSOR_TYPE + i;

			sensorEnc.reset()
				.encodeUnsignedIntField(SensorEntropy_TYPE, type)
				.encodeUnsignedLongField(SensorEntropy_COUNT, counts[i])
				.encodeFloatField(SensorEntropy_MU, (float) getMean(type, muAndD2))
				.encodeFloatField(SensorEntropy_D2, (float) getD2(type, muAndD2));
			enc.encodeByteArrayField(
				PlainEntropy_SENSOR,
				sensorEnc.array(), 0, sensorEnc.arrayOffset()
			);
		}
	}

	/*
	 * Update measurement with `value` for sensor `type`. This assumes read-side of `updateLock`
	 * is held.
	 */
	@GuardedBy("this.updateLock")
	private void update(@SensorType int type, double value) {
		long n = ++this.counts[type - MIN_SENSOR_TYPE];

		if (n == 0) {
			// overflow, clear out all the stats
			this.counts[type - MIN_SENSOR_TYPE] = 1;
			setMeanAndD2(type, this.meansAndD2, 0, 0);
			n = 1;
		}

		// `n` is unsigned, turn it into a well formed double
		double nd = fma((double) (n >>> 1), 2, n & 1);
		double curMu = getMean(type, this.meansAndD2);
		double newMu = curMu + ((value - curMu) / nd);
		double newD2 = fma(value - newMu, value - curMu, getD2(type, this.meansAndD2));

		setMeanAndD2(type, this.meansAndD2, newMu, newD2);
		Logger.debug(TAG, "sensor=%d, mu=%f, d2=%f, n=%f, value=%f", type, newMu, newD2, nd, value);
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int i) {
	}

	@Override
	public void onSensorChanged(SensorEvent evt) {
		int type = evt.sensor.getType();
		double value;
		int mtype;

		if (type == Sensor.TYPE_ACCELEROMETER) {
			if (evt.values == null || evt.values.length < 3)
				return;

			double x = evt.values[0];
			double y = evt.values[1];
			double z = evt.values[2];

			value = Math.sqrt(fma(x, x, fma(y, y, z * z)));
			mtype = SensorAccelMag;
		} else if (type == Sensor.TYPE_LIGHT || type == Sensor.TYPE_PROXIMITY) {
			if (evt.values == null || evt.values.length < 1)
				return;

			value = evt.values[0];
			mtype = type == Sensor.TYPE_LIGHT ? SensorLightIllumSilux : SensorScreenProxCm;
		} else {
			return;
		}

		Lock read = this.updateLock.readLock();

		read.lock();
		try {
			this.update(mtype, value);
		} finally {
			read.unlock();
		}
	}

	// Update battery status measurements.
	private void updateBatteryStatus(Intent status) {
		int charging =
			status.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN);
		int lvl = status.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
		int scale = status.getIntExtra(BatteryManager.EXTRA_SCALE, lvl);
		int temp = status.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Integer.MIN_VALUE);
		int volt = status.getIntExtra(BatteryManager.EXTRA_VOLTAGE, Integer.MIN_VALUE);
		Lock read = this.updateLock.readLock();

		read.lock();
		try {
			this.batteryCharging =
				charging == BatteryManager.BATTERY_STATUS_CHARGING ||
				charging == BatteryManager.BATTERY_STATUS_FULL;
			if (lvl != -1 && scale != 0)
				this.update(SensorBattLevelPct, (lvl / ((double) scale)) * 100);
			if (temp != Integer.MIN_VALUE)
				this.update(SensorBattTempC, temp / 10.);
			if (volt != Integer.MIN_VALUE)
				this.update(SensorBattVoltMv, volt);
		} finally {
			read.unlock();
		}
	}

	// Update battery related measurements.
	private void updateBattery() {
		try {
			Intent status = this.sdk.context()
				.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

			if (status != null)
				this.updateBatteryStatus(status);
		} catch (Exception cause) {
			Logger.debug(TAG, "failed to retrieve battery status", cause);
		}

		if (this.battery == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
			return;

		int currMua;
		Lock read = this.updateLock.readLock();

		try {
			currMua = this.battery.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
		} catch (Exception cause) {
			Logger.debug(TAG, "failed to retrieve battery current", cause);
			return;
		}

		read.lock();
		try {
			this.update(SensorBattCurrMua, currMua);
		} finally {
			read.unlock();
		}
	}

	@Override
	protected void run() {
		Lock read = this.updateLock.readLock();

		try {
			this.updateBattery();
		} finally {
			read.lock();
			try {
				if (!this.paused)
					super.schedule(this.samplePeriodMicroseconds, TimeUnit.MICROSECONDS);
			} finally {
				read.unlock();
			}
		}
	}

	@Override
	public void shutdown() {
		this.pause();
		super.shutdown();
	}
}
