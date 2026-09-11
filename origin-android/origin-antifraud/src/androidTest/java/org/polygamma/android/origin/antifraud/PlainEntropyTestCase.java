// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.antifraud;

import static android.provider.Settings.Global.ADB_ENABLED;
import static android.provider.Settings.Global.AIRPLANE_MODE_ON;
import static android.provider.Settings.Global.AUTO_TIME_ZONE;
import static android.provider.Settings.Global.BOOT_COUNT;
import static android.provider.Settings.Secure.ACCESSIBILITY_ENABLED;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.polygamma.android.origin.util.AndroidSettings.getGlobalBoolean;
import static org.polygamma.android.origin.util.AndroidSettings.getGlobalInt;
import static org.polygamma.android.origin.util.AndroidSettings.getSecureBoolean;

import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Build;
import android.os.SystemClock;

import androidx.annotation.Nullable;
import androidx.test.platform.app.InstrumentationRegistry;

import org.polygamma.android.origin.adcom.context.App;
import org.polygamma.android.origin.adcom.context.Device;
import org.polygamma.android.origin.adcom.context.Regs;
import org.polygamma.android.origin.adcom.enums.AdComEnums;
import org.polygamma.android.origin.core.DeviceModule;
import org.polygamma.android.origin.core.Origin;
import org.polygamma.android.origin.core.RegulationsModule;
import org.polygamma.android.origin.protobuf.ProtobufEncoder;
import org.polygamma.android.origin.util.AndroidContexts;
import org.polygamma.android.origin.util.Bits;
import org.polygamma.android.origin.util.Time;
import org.polygamma.origin.antifraud.IvtCheck;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Plain entropy test case.
 */
final class PlainEntropyTestCase {

	private static final class Generator implements Iterator<PlainEntropyTestCase> {

		private final Origin sdk;
		private final @Nullable DeviceModule deviceModule;
		private final @Nullable RegulationsModule regulationsModule;
		private final @Nullable AntifraudModule antifraudModule;
		private final Random random;
		private final boolean session;

		private Generator(Origin sdk, Random rand, boolean sess) {
			this.sdk = sdk;
			this.deviceModule = sdk.findModule(DeviceModule.class);
			this.regulationsModule = sdk.findModule(RegulationsModule.class);
			this.antifraudModule = sdk.findModule(AntifraudModule.class);
			this.random = rand;
			this.session = this.antifraudModule != null || sess;
		}

		@Override
		public boolean hasNext() {
			return true;
		}

		@Override
		public PlainEntropyTestCase next() {
			PlainEntropyTestCase test = new PlainEntropyTestCase();

			if (this.random.nextBoolean() || this.session)
				test.context = this.sdk.context();
			if (this.antifraudModule != null) {
				test.sensors = this.antifraudModule.sensors;
			} else if (this.random.nextBoolean()) {
				test.sensors = new SensorEntropyService(this.sdk, 350000);
				for (int i = 0; i < test.sensors.counts.length; i++)
					test.sensors.counts[i] = this.random.nextLong();
				for (int i = 0; i < test.sensors.meansAndD2.length; i++)
					test.sensors.meansAndD2[i] = this.random.nextDouble();
				test.sensors.batteryCharging = this.random.nextBoolean();
			}
			if (this.regulationsModule != null) {
				test.regs = this.regulationsModule.regs();
			} else if (this.random.nextBoolean()) {
				int[] gppSids = new int[8];

				for (int i = 0; i < gppSids.length; i++)
					gppSids[i] = this.random.nextInt();
				test.regs = Regs.ofBuilder()
					.coppa(this.random.nextBoolean())
					.gdpr(this.random.nextBoolean())
					.gpp(UUID.randomUUID().toString())
					.applicableGppSectionIds(gppSids)
					.pipl(this.random.nextBoolean())
					.build();
			} else {
				test.regs = Regs.of();
			}
			if (this.deviceModule != null) {
				test.device = this.deviceModule.device();
			} else {
				test.device = Device.ofBuilder()
					.type(this.random.nextInt())
					.userAgent(UUID.randomUUID().toString())
					.limitAdTracking(this.random.nextBoolean())
					.manufacturerName(UUID.randomUUID().toString())
					.modelName(UUID.randomUUID().toString())
					.operatingSystem(AdComEnums.OsGoogleAndroid)
					.operatingSystemVersion(Build.VERSION.RELEASE)
					.modelVersion(UUID.randomUUID().toString())
					.screenWidthPx(this.random.nextInt())
					.screenHeightPx(this.random.nextInt())
					.screenPixelRatio(this.random.nextFloat())
					.languageCode("en")
					.carrierName(UUID.randomUUID().toString())
					.build();
			}
			if (this.session) {
				test.app = this.sdk.app();
			} else {
				test.app = App.ofBuilder()
					.id(UUID.randomUUID().toString())
					.name(UUID.randomUUID().toString())
					.publisherId(UUID.randomUUID().toString())
					.storeId(UUID.randomUUID().toString())
					.version(UUID.randomUUID().toString())
					.paid(this.random.nextBoolean())
					.debuggable(this.random.nextBoolean())
					.system(this.random.nextBoolean())
					.build();
			}
			return test;
		}
	}

	static Iterator<PlainEntropyTestCase> newGenerator(Origin sdk, Random rand, boolean sess) {
		return new Generator(sdk, rand, sess);
	}

	static Iterator<PlainEntropyTestCase> newGenerator(Origin sdk) {
		return newGenerator(sdk, new Random(44), false);
	}

	@Nullable Context context;
	@Nullable SensorEntropyService sensors;
	Regs regs;
	Device device;
	App app;

	void assertAndroidEntropyEquals(IvtCheck.AndroidEntropy got) {
		try {
			String line = "";

			try (BufferedReader reader = new BufferedReader(
				new FileReader("/proc/sys/kernel/random/boot_id")
			)) {
				for (String ln; (ln = reader.readLine()) != null;)
					line = line.concat(ln);
			}

			UUID bootUuid = UUID.fromString(line);
			byte[] bootId = new byte[16];

			Bits.storeLongLe(bootId, 0, bootUuid.getLeastSignificantBits());
			Bits.storeLongLe(bootId, 8, bootUuid.getMostSignificantBits());
			assertArrayEquals(got.getBootid().toByteArray(), bootId);
		} catch (IOException cause) {
			throw new AssertionError(cause);
		}

		assertEquals(
			Build.VERSION.SDK_INT < Build.VERSION_CODES.N || this.context == null ? 0 :
			getGlobalInt(this.context.getContentResolver(), BOOT_COUNT),
			got.getBootcnt()
		);
		assertEquals(android.os.Process.myPid(), got.getPid());
		assertEquals(android.os.Process.myUid(), got.getUid());
		assertEquals(Time.nowRealtimeSeconds(), got.getDevrealtimesec(), 10.);
		assertEquals(
			Time.nowRealtimeSeconds() - Time.nowUptimeSeconds(),
			got.getDevsleeptimesec(),
			10.
		);
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
			assertEquals(0, got.getAppruntimesec());
			assertEquals(0, got.getAppsleeptimesec());
		} else {
			assertEquals(
				Time.nowRealtimeSeconds() -
				TimeUnit.MILLISECONDS.toSeconds(android.os.Process.getStartElapsedRealtime()),
				got.getAppruntimesec(),
				10.
			);
			assertEquals(
				Time.nowUptimeSeconds() -
				TimeUnit.MILLISECONDS.toSeconds(android.os.Process.getStartUptimeMillis()),
				got.getAppsleeptimesec(),
				10.
			);
		}
		assertEquals(Runtime.getRuntime().availableProcessors(), got.getNcpu());

		ActivityManager activity =
			this.context == null ? null :
			AndroidContexts.systemServiceOf(
				this.context,
				ActivityManager.class,
				Context.ACTIVITY_SERVICE
			);

		if (activity == null) {
			assertEquals(0, got.getAdvrambytes());
			assertEquals(0, got.getFreerambytes());
			assertEquals(0, got.getAvailrambytes());
			assertEquals(0, got.getTotalrambytes());
		} else {
			ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo();

			activity.getMemoryInfo(info);
			assertEquals(
				Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ? 0 :
				info.advertisedMem,
				got.getAdvrambytes()
			);
			assertEquals(
				Build.VERSION.SDK_INT < Build.VERSION_CODES.CINNAMON_BUN ? 0 :
				info.freeMem,
				got.getFreerambytes(),
				/*
				 * Give a delta of 128 mb since free bytes is dependent on execution at that point
				 * in time.
				 */
				128L * 1024 * 1024
			);
			assertEquals(info.availMem, got.getAvailrambytes(), 128.d * 1024 * 1024);
			assertEquals(info.totalMem, got.getTotalrambytes());
		}

		if (this.context == null) {
			assertFalse(got.getAdbenabled());
			assertFalse(got.getAirmodeenabled());
			assertFalse(got.getAutotzenabled());
			assertFalse(got.getAccessibenabled());
		} else {
			ContentResolver content = this.context.getContentResolver();

			assertEquals(getGlobalBoolean(content, ADB_ENABLED), got.getAdbenabled());
			assertEquals(getGlobalBoolean(content, AIRPLANE_MODE_ON), got.getAirmodeenabled());
			assertEquals(getGlobalBoolean(content, AUTO_TIME_ZONE), got.getAutotzenabled());
			assertEquals(
				getSecureBoolean(content, ACCESSIBILITY_ENABLED),
				got.getAccessibenabled()
			);
		}

		if (this.sensors == null)
			assertFalse(got.getBattchrging());
		else
			assertEquals(this.sensors.isBatteryCharging(), got.getBattchrging());

		assertEquals(Build.TAGS, got.getBuildtags());
		assertEquals(Build.FINGERPRINT, got.getBuildfp());
		assertEquals(Build.PRODUCT, got.getBuildprod());
		assertEquals(Build.HARDWARE, got.getBuildhw());
		assertEquals(Build.DISPLAY, got.getBuilddisp());
		assertEquals(Build.getRadioVersion(), got.getBuildradio());
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
			assertEquals(Build.SOC_MANUFACTURER, got.getBuildsocman());
			assertEquals(Build.SOC_MODEL, got.getBuildsocmodel());
		} else {
			assertEquals("", got.getBuildsocman());
			assertEquals("", got.getBuildsocmodel());
		}
		assertEquals(
			Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP ? Collections.emptyList() :
				Arrays.asList(Build.SUPPORTED_ABIS),
			got.getBuildsuppabiList()
		);
	}

	void assertPlainEntropyEquals(IvtCheck.PlainEntropy got) {
		ProtobufEncoder expEnc = ProtobufEncoder.of();

		this.app.toProtobuf(expEnc.reset());
		assertEquals(expEnc.asBuffer(), got.getChannel().asReadOnlyByteBuffer());

		this.regs.toProtobuf(expEnc.reset());
		assertEquals(expEnc.asBuffer(), got.getRegs().asReadOnlyByteBuffer());

		this.device.toProtobuf(expEnc.reset());
		assertEquals(expEnc.asBuffer(), got.getDevice().asReadOnlyByteBuffer());

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			try {
				assertEquals(
					EntropyCoding.measureClockDriftSeconds(SystemClock.currentGnssTimeClock()),
					got.getGpsclockdriftsec(),
					10.
				);
			} catch (Exception ignored) {
				assertEquals(0, got.getGpsclockdriftsec());
			}
		} else {
			assertEquals(0, got.getGpsclockdriftsec());
		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			try {
				assertEquals(
					EntropyCoding.measureClockDriftSeconds(SystemClock.currentNetworkTimeClock()),
					got.getNetclockdriftsec(),
					10.
				);
			} catch (Exception ignored) {
				assertEquals(0, got.getNetclockdriftsec());
			}
		} else {
			assertEquals(0, got.getNetclockdriftsec());
		}

		if (this.sensors == null) {
			assertEquals(0, got.getSensorCount());
		} else {
			assertEquals(this.sensors.counts.length, got.getSensorCount());
			for (int i = 0; i < this.sensors.counts.length; i++) {
				IvtCheck.SensorEntropy sensor = got.getSensor(i);
				int type = i + CheckWire.MIN_SENSOR_TYPE;

				assertEquals(type, sensor.getTypeValue());
				assertEquals(
					SensorEntropyService.getMean(type, this.sensors.meansAndD2),
					sensor.getMu(),
					1.
				);
				assertEquals(
					SensorEntropyService.getD2(type, this.sensors.meansAndD2),
					sensor.getD2(),
					1.
				);
			}
		}

		assertEquals(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N, got.getCanattestdevice());
		this.assertAndroidEntropyEquals(got.getAndroid());
	}
}
