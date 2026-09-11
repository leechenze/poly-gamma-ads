// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.antifraud;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.polygamma.android.origin.antifraud.CheckWire.MIN_SENSOR_TYPE;
import static org.polygamma.android.origin.antifraud.CheckWire.SENSOR_TYPE_RANGE;
import static org.polygamma.android.origin.antifraud.CheckWire.SensorAccelMag;
import static org.polygamma.android.origin.antifraud.CheckWire.SensorBattCurrMua;
import static org.polygamma.android.origin.antifraud.CheckWire.SensorBattLevelPct;
import static org.polygamma.android.origin.antifraud.CheckWire.SensorBattTempC;
import static org.polygamma.android.origin.antifraud.CheckWire.SensorBattVoltMv;
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
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.polygamma.android.origin.core.Origin;
import org.polygamma.android.origin.util.AndroidContexts;

import java.util.concurrent.TimeUnit;

/**
 * {@link SensorEntropyService} tests.
 */
@RunWith(AndroidJUnit4.class)
public class SensorEntropyServiceTest {

	private static final class TestMeasurement {

		long count;
		double mean;
		double d2;

		void add(double value) {
			long n = ++this.count;
			double mu = this.mean + ((value - this.mean) / n);
			double d2 = (value - mu) * (value - this.mean) + this.d2;

			this.mean = mu;
			this.d2 = d2;
		}
	}

	private static final class TestSensorEventListener implements SensorEventListener {

		private final TestMeasurement[] measurements;

		TestSensorEventListener(TestMeasurement[] measurements) {
			this.measurements = measurements;
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

				value = Math.sqrt(x * x + y * y + z * z);
				mtype = SensorAccelMag;
			} else if (type == Sensor.TYPE_LIGHT || type == Sensor.TYPE_PROXIMITY) {
				if (evt.values == null || evt.values.length < 1)
					return;

				value = evt.values[0];
				mtype = type == Sensor.TYPE_LIGHT ? SensorLightIllumSilux : SensorScreenProxCm;
			} else {
				return;
			}
			measurements[mtype - MIN_SENSOR_TYPE].add(value);
		}
	}

	private static Origin sdk;

	@BeforeClass
	public static void setup() {
		Context ctxt = InstrumentationRegistry.getInstrumentation()
			.getTargetContext()
			.getApplicationContext();

		sdk = Origin.initialize(ctxt);
	}

	@AfterClass
	public static void destroy() throws InterruptedException {
		if (sdk != null) {
			sdk.shutdown();
			while (!sdk.awaitShutdown(10, TimeUnit.SECONDS))
				Log.w(EntropyCodingTest.class.getSimpleName(), "sdk shutdown taking longer than 10 seconds");
			assertTrue(sdk.isShutdown());
		}
		sdk = null;
	}

	private static boolean pollBattery(TestMeasurement[] dst, @Nullable BatteryManager man) {
		Intent status = sdk.context()
			.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
		boolean chg = false;

		if (status != null) {
			int charging = status.getIntExtra(
				BatteryManager.EXTRA_STATUS,
				BatteryManager.BATTERY_STATUS_UNKNOWN
			);
			int lvl = status.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
			int scale = status.getIntExtra(BatteryManager.EXTRA_SCALE, lvl);
			int temp = status.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Integer.MIN_VALUE);
			int volt = status.getIntExtra(BatteryManager.EXTRA_VOLTAGE, Integer.MIN_VALUE);

			chg = charging == BatteryManager.BATTERY_STATUS_CHARGING ||
				charging == BatteryManager.BATTERY_STATUS_FULL;
			if (lvl != -1 && scale != 0)
				dst[SensorBattLevelPct - MIN_SENSOR_TYPE].add(lvl / ((double) scale) * 100);
			if (temp != Integer.MIN_VALUE)
				dst[SensorBattTempC - MIN_SENSOR_TYPE].add(temp / 10.);
			if (volt != Integer.MIN_VALUE)
				dst[SensorBattVoltMv - MIN_SENSOR_TYPE].add(volt);
		}
		if (man != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			dst[SensorBattCurrMua - MIN_SENSOR_TYPE].add(
				man.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
			);
		}
		return chg;
	}

	@Test
	public void test() throws InterruptedException {
		final int SAMPLE_PERIOD_MSEC = 1000;

		TestMeasurement[] exp = new TestMeasurement[SENSOR_TYPE_RANGE];

		for (int i = 0; i < exp.length; i++)
			exp[i] = new TestMeasurement();

		TestSensorEventListener listener = new TestSensorEventListener(exp);
		SensorEntropyService svc = SensorEntropyService.create(sdk, SAMPLE_PERIOD_MSEC * 1000);

		assertTrue(svc.isPaused());
		assertFalse(svc.isBatteryCharging());
		assertEquals(SensorEntropyService.STATE_IDLE, svc.state());
		svc.unpause();
		assertFalse(svc.isPaused());
		assertTrue(
			svc.state() == SensorEntropyService.STATE_SCHEDULED ||
			svc.state() == SensorEntropyService.STATE_RUNNING
		);

		if (svc.sensors != null) {
			for (int type : new int[] {
				Sensor.TYPE_ACCELEROMETER,
				Sensor.TYPE_LIGHT,
				Sensor.TYPE_PROXIMITY
			}) {
				Sensor sensor =
					Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP ?
					svc.sensors.getDefaultSensor(type) :
					svc.sensors.getDefaultSensor(type, false);

				if (sensor != null)
					svc.sensors.registerListener(listener, sensor, SAMPLE_PERIOD_MSEC * 1000);
			}
		}

		boolean expChg = pollBattery(exp, svc.battery);

		for (int i = 0; i < 5; i++) {
			SystemClock.sleep(SAMPLE_PERIOD_MSEC + 15);
			assertEquals(expChg, svc.isBatteryCharging());
			assertEquals(
				SAMPLE_PERIOD_MSEC,
				svc.nextExecutionDelayMillis(),
				SAMPLE_PERIOD_MSEC / 2.
			);
			expChg = pollBattery(exp, svc.battery);
		}
		SystemClock.sleep(SAMPLE_PERIOD_MSEC);
		assertEquals(expChg, svc.isBatteryCharging());

		svc.pause();
		assertTrue(svc.isPaused());
		SystemClock.sleep(SAMPLE_PERIOD_MSEC);
		assertEquals(SensorEntropyService.STATE_IDLE, svc.state());
		assertEquals(-1, svc.nextExecutionDelayMillis());

		SystemClock.sleep(SAMPLE_PERIOD_MSEC);
		assertEquals(expChg, svc.isBatteryCharging());

		for (int i = 0; i < exp.length; i++) {
			double expMu = exp[i].mean;
			double expVar = exp[i].count == 0L ? 0 : exp[i].d2 / exp[i].count;
			double gotMu = SensorEntropyService.getMean(MIN_SENSOR_TYPE + i, svc.meansAndD2);
			double gotD2 = SensorEntropyService.getD2(MIN_SENSOR_TYPE + i, svc.meansAndD2);
			double gotVar = svc.counts[i] == 0L ? 0 : gotD2 / svc.counts[i];

			assertEquals(i+"", expMu, gotMu, 0.01);
			assertEquals(i+"", expVar, gotVar, 0.01);
		}

		svc.shutdown();
		assertTrue(svc.awaitShutdown(SAMPLE_PERIOD_MSEC, TimeUnit.MILLISECONDS));
	}
}
