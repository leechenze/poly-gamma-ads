// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.antifraud;

import static android.provider.Settings.Global.ADB_ENABLED;
import static android.provider.Settings.Global.AIRPLANE_MODE_ON;
import static android.provider.Settings.Global.AUTO_TIME_ZONE;
import static android.provider.Settings.Global.BOOT_COUNT;
import static android.provider.Settings.Secure.ACCESSIBILITY_ENABLED;
import static org.polygamma.android.origin.antifraud.CheckWire.AndroidEntropy_ACCESSIBENABLED;
import static org.polygamma.android.origin.antifraud.CheckWire.AndroidEntropy_ADBENABLED;
import static org.polygamma.android.origin.antifraud.CheckWire.AndroidEntropy_ADVRAMBYTES;
import static org.polygamma.android.origin.antifraud.CheckWire.AndroidEntropy_AIRMODEENABLED;
import static org.polygamma.android.origin.antifraud.CheckWire.AndroidEntropy_APPRUNTIMESEC;
import static org.polygamma.android.origin.antifraud.CheckWire.AndroidEntropy_APPSLEEPTIMESEC;
import static org.polygamma.android.origin.antifraud.CheckWire.AndroidEntropy_AUTOTZENABLED;
import static org.polygamma.android.origin.antifraud.CheckWire.AndroidEntropy_AVAILRAMBYTES;
import static org.polygamma.android.origin.antifraud.CheckWire.AndroidEntropy_BATTCHRGING;
import static org.polygamma.android.origin.antifraud.CheckWire.AndroidEntropy_BOOTCNT;
import static org.polygamma.android.origin.antifraud.CheckWire.AndroidEntropy_BOOTID;
import static org.polygamma.android.origin.antifraud.CheckWire.AndroidEntropy_BUILDDISP;
import static org.polygamma.android.origin.antifraud.CheckWire.AndroidEntropy_BUILDFP;
import static org.polygamma.android.origin.antifraud.CheckWire.AndroidEntropy_BUILDHW;
import static org.polygamma.android.origin.antifraud.CheckWire.AndroidEntropy_BUILDPROD;
import static org.polygamma.android.origin.antifraud.CheckWire.AndroidEntropy_BUILDRADIO;
import static org.polygamma.android.origin.antifraud.CheckWire.AndroidEntropy_BUILDSOCMAN;
import static org.polygamma.android.origin.antifraud.CheckWire.AndroidEntropy_BUILDSOCMODEL;
import static org.polygamma.android.origin.antifraud.CheckWire.AndroidEntropy_BUILDSUPPABI;
import static org.polygamma.android.origin.antifraud.CheckWire.AndroidEntropy_BUILDTAGS;
import static org.polygamma.android.origin.antifraud.CheckWire.AndroidEntropy_DEVREALTIMESEC;
import static org.polygamma.android.origin.antifraud.CheckWire.AndroidEntropy_DEVSLEEPTIMESEC;
import static org.polygamma.android.origin.antifraud.CheckWire.AndroidEntropy_FREERAMBYTES;
import static org.polygamma.android.origin.antifraud.CheckWire.AndroidEntropy_NCPU;
import static org.polygamma.android.origin.antifraud.CheckWire.AndroidEntropy_PID;
import static org.polygamma.android.origin.antifraud.CheckWire.AndroidEntropy_TOTALRAMBYTES;
import static org.polygamma.android.origin.antifraud.CheckWire.AndroidEntropy_UID;
import static org.polygamma.android.origin.antifraud.CheckWire.CipherEntropy_CTCONTENT;
import static org.polygamma.android.origin.antifraud.CheckWire.CipherEntropy_ID;
import static org.polygamma.android.origin.antifraud.CheckWire.CipherEntropy_SCHEMA;
import static org.polygamma.android.origin.antifraud.CheckWire.DynamicEntropy_BLOB;
import static org.polygamma.android.origin.antifraud.CheckWire.DynamicEntropy_F32;
import static org.polygamma.android.origin.antifraud.CheckWire.DynamicEntropy_F64;
import static org.polygamma.android.origin.antifraud.CheckWire.DynamicEntropy_FLAG;
import static org.polygamma.android.origin.antifraud.CheckWire.DynamicEntropy_LISTSEQ;
import static org.polygamma.android.origin.antifraud.CheckWire.DynamicEntropy_MAPSEQ;
import static org.polygamma.android.origin.antifraud.CheckWire.DynamicEntropy_MSG;
import static org.polygamma.android.origin.antifraud.CheckWire.DynamicEntropy_NIL;
import static org.polygamma.android.origin.antifraud.CheckWire.DynamicEntropy_PF32;
import static org.polygamma.android.origin.antifraud.CheckWire.DynamicEntropy_PF64;
import static org.polygamma.android.origin.antifraud.CheckWire.DynamicEntropy_PFLAG;
import static org.polygamma.android.origin.antifraud.CheckWire.DynamicEntropy_PU32;
import static org.polygamma.android.origin.antifraud.CheckWire.DynamicEntropy_PU64;
import static org.polygamma.android.origin.antifraud.CheckWire.DynamicEntropy_STR;
import static org.polygamma.android.origin.antifraud.CheckWire.DynamicEntropy_U32;
import static org.polygamma.android.origin.antifraud.CheckWire.DynamicEntropy_U64;
import static org.polygamma.android.origin.antifraud.CheckWire.PlainEntropy_ANDROID;
import static org.polygamma.android.origin.antifraud.CheckWire.PlainEntropy_CANATTESTDEVICE;
import static org.polygamma.android.origin.antifraud.CheckWire.PlainEntropy_CHANNEL;
import static org.polygamma.android.origin.antifraud.CheckWire.PlainEntropy_DEVICE;
import static org.polygamma.android.origin.antifraud.CheckWire.PlainEntropy_GPSCLOCKDRIFTSEC;
import static org.polygamma.android.origin.antifraud.CheckWire.PlainEntropy_NETCLOCKDRIFTSEC;
import static org.polygamma.android.origin.antifraud.CheckWire.PlainEntropy_REGS;
import static org.polygamma.android.origin.util.AndroidSettings.getGlobalBoolean;
import static org.polygamma.android.origin.util.AndroidSettings.getGlobalInt;
import static org.polygamma.android.origin.util.AndroidSettings.getSecureBoolean;

import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Build;
import android.os.SystemClock;
import android.util.Pair;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;

import org.polygamma.android.origin.adcom.context.App;
import org.polygamma.android.origin.adcom.context.Device;
import org.polygamma.android.origin.adcom.context.Regs;
import org.polygamma.android.origin.protobuf.ProtobufEncoder;
import org.polygamma.android.origin.protobuf.ProtobufSerializable;
import org.polygamma.android.origin.util.AndroidContexts;
import org.polygamma.android.origin.util.Logger;
import org.polygamma.android.origin.util.Supplier;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Clock;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Various entropy coding definitions.
 */
class EntropyCoding {

	private static final String TAG = EntropyCoding.class.getSimpleName();

	private static final @Nullable Class<?> ANDROIDX_CORE_SUPPLIER;
	private static final @Nullable Method ANDROIDX_CORE_SUPPLIER_GET;

	static {
		Class<?> klass;
		Method meth;

		try {
			klass = Class.forName("androidx.core.util.Supplier");
			meth = klass.getDeclaredMethod("get");
		} catch (ClassNotFoundException | NoSuchMethodException ignored) {
			klass = null;
			meth = null;
		}
		ANDROIDX_CORE_SUPPLIER = klass;
		ANDROIDX_CORE_SUPPLIER_GET = meth;
	}

	/**
	 * Generate string stack trace from throwable.
	 *
	 * @param cause throwable to generate string stack trace of
	 * @return string stack trace of {@code cause}
	 */
	static String throwableToString(Throwable cause) {
		try (
			StringWriter str = new StringWriter();
			PrintWriter print = new PrintWriter(str)
		) {
			cause.printStackTrace(print);
			print.flush();
			return str.toString();
		} catch (IOException ignored) {
			return "";
		}
	}

	// Measure drift between wall clock and `clock`.
	@RequiresApi(api = Build.VERSION_CODES.O)
	@WorkerThread
	@VisibleForTesting
	static long measureClockDriftSeconds(Clock clock) {
		long drift = 0;
		long latency = Long.MAX_VALUE;

		for (int i = 0; i < 100; i++) {
			long wallBefore = System.currentTimeMillis();
			long now = clock.millis();
			long wallAfter = System.currentTimeMillis();
			long currLatency = wallAfter - wallBefore;

			if (currLatency < latency) {
				long wallMidSec = Long.divideUnsigned(
					TimeUnit.MILLISECONDS.toSeconds(wallBefore) +
					TimeUnit.MILLISECONDS.toSeconds(wallAfter),
					2
				);

				latency = currLatency;
				drift = wallMidSec - TimeUnit.MILLISECONDS.toSeconds(now);
			}
			SystemClock.sleep(1);
		}
		return drift;
	}

	/**
	 * Encode plain entropy drift between wall clock and, network and GNSS clocks.
	 * <p>This uses {@link SystemClock#sleep(long)} during measurement, and should not be invoked
	 * on a UI thread.
	 *
	 * @param enc encoder to encode into
	 */
	@WorkerThread
	static void encodePlainClockDrifts(ProtobufEncoder enc) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			try {
				enc.encodeSignedLongField(
					PlainEntropy_GPSCLOCKDRIFTSEC,
					measureClockDriftSeconds(SystemClock.currentGnssTimeClock())
				);
			} catch (Exception cause) {
				Logger.debug(TAG, "failed to measure GNSS clock drift", cause);
			}
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			try {
				enc.encodeSignedLongField(
					PlainEntropy_NETCLOCKDRIFTSEC,
					measureClockDriftSeconds(SystemClock.currentNetworkTimeClock())
				);
			} catch (Exception cause) {
				Logger.debug(TAG, "failed to measure GNSS clock drift", cause);
			}
		}
	}

	// Read `/proc/sys/kernel/random/boot_id` and encode it into `bootid` field.
	@WorkerThread
	private static void encodeAndroidBootId(ProtobufEncoder enc) {
		String line = "";
		UUID id = null;

		try {
			File file = new File("/proc/sys/kernel/random/boot_id");

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				line = String.join("", Files.readAllLines(file.toPath(), StandardCharsets.UTF_8));
			} else {
				try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
					for (String ln; (ln = reader.readLine()) != null;)
						line = line.concat(ln);
				}
			}
			id = UUID.fromString(line);
		} catch (Exception cause) {
			Logger.debug(TAG, "failed to read boot id", cause);
		}

		if (id != null) {
			enc.encodeLenExactField(AndroidEntropy_BOOTID, 16)
				.order(ByteOrder.LITTLE_ENDIAN)
				.putLong(id.getLeastSignificantBits())
				.putLong(id.getMostSignificantBits());
		} else {
			enc.encodeStringField(AndroidEntropy_BOOTID, line);
		}
	}

	// Encode device memory information.
	private static void encodeAndroidMemoryInfo(ProtobufEncoder enc, @Nullable Context ctxt) {
		ActivityManager activity =
			ctxt == null ? null :
			AndroidContexts.systemServiceOf(ctxt, ActivityManager.class, Context.ACTIVITY_SERVICE);

		if (activity == null)
			return;

		ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo();

		try {
			activity.getMemoryInfo(info);
		} catch (Exception cause) {
			Logger.debug(TAG, "failed to retrieve memory info", cause);
			return;
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
			enc.encodeUnsignedLongField(AndroidEntropy_ADVRAMBYTES, info.advertisedMem);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN)
			enc.encodeUnsignedLongField(AndroidEntropy_FREERAMBYTES, info.freeMem);
		enc.encodeUnsignedLongField(AndroidEntropy_AVAILRAMBYTES, info.availMem)
			.encodeUnsignedLongField(AndroidEntropy_TOTALRAMBYTES, info.totalMem);
	}

	/**
	 * Encode Android-specific entropy.
	 * <p>This performs reads from the file system and should not be invoked on a UI thread.
	 *
	 * @param enc encoder to encode into
	 * @param ctxt context to resolve settings and services from, if any
	 * @param sensor service to query sensor entropy from, if any
	 */
	@WorkerThread
	static void encodeAndroid(
		ProtobufEncoder enc,
		@Nullable Context ctxt,
		@Nullable SensorEntropyService sensor
	) {
		ContentResolver content = ctxt == null ? null : ctxt.getContentResolver();
		long devRealMs = SystemClock.elapsedRealtime();
		long devUpMs = SystemClock.uptimeMillis();
		long appRunMs = 0L;
		long appSleepMs = 0L;

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			long appRealMs = android.os.Process.getStartElapsedRealtime();
			long appUpMs = android.os.Process.getStartUptimeMillis();

			appRunMs = devRealMs - appRealMs;
			appSleepMs = devUpMs - appUpMs;
		}
		encodeAndroidBootId(enc);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && content != null)
			enc.encodeUnsignedIntField(AndroidEntropy_BOOTCNT, getGlobalInt(content, BOOT_COUNT));
		enc.encodeSignedIntField(AndroidEntropy_PID, android.os.Process.myPid())
			.encodeSignedIntField(AndroidEntropy_UID, android.os.Process.myUid())
			.encodeUnsignedLongField(
				AndroidEntropy_DEVREALTIMESEC,
				TimeUnit.MILLISECONDS.toSeconds(devRealMs)
			)
			.encodeUnsignedLongField(
				AndroidEntropy_DEVSLEEPTIMESEC,
				TimeUnit.MILLISECONDS.toSeconds(devRealMs - devUpMs)
			)
			.encodeUnsignedLongField(
				AndroidEntropy_APPRUNTIMESEC,
				TimeUnit.MILLISECONDS.toSeconds(appRunMs)
			)
			.encodeUnsignedLongField(
				AndroidEntropy_APPSLEEPTIMESEC,
				TimeUnit.MILLISECONDS.toSeconds(appSleepMs)
			)
			.encodeUnsignedIntField(
				AndroidEntropy_NCPU,
				Runtime.getRuntime().availableProcessors()
			);

		encodeAndroidMemoryInfo(enc, ctxt);
		if (content != null) {
			enc.encodeBoolField(AndroidEntropy_ADBENABLED, getGlobalBoolean(content, ADB_ENABLED))
				.encodeBoolField(
					AndroidEntropy_AIRMODEENABLED,
					getGlobalBoolean(content, AIRPLANE_MODE_ON)
				)
				.encodeBoolField(
					AndroidEntropy_AUTOTZENABLED,
					getGlobalBoolean(content, AUTO_TIME_ZONE)
				)
				.encodeBoolField(
					AndroidEntropy_ACCESSIBENABLED,
					getSecureBoolean(content, ACCESSIBILITY_ENABLED)
				);
		}
		if (sensor != null)
			enc.encodeBoolField(AndroidEntropy_BATTCHRGING, sensor.isBatteryCharging());
		enc.encodeStringField(AndroidEntropy_BUILDTAGS, Build.TAGS)
			.encodeStringField(AndroidEntropy_BUILDFP, Build.FINGERPRINT)
			.encodeStringField(AndroidEntropy_BUILDPROD, Build.PRODUCT)
			.encodeStringField(AndroidEntropy_BUILDHW, Build.HARDWARE)
			.encodeStringField(AndroidEntropy_BUILDDISP, Build.DISPLAY)
			.encodeStringField(AndroidEntropy_BUILDRADIO, Build.getRadioVersion());
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
			enc.encodeStringField(AndroidEntropy_BUILDSOCMAN, Build.SOC_MANUFACTURER)
				.encodeStringField(AndroidEntropy_BUILDSOCMODEL, Build.SOC_MODEL);
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			for (String abi : Build.SUPPORTED_ABIS)
				enc.encodeStringField(AndroidEntropy_BUILDSUPPABI, abi);
		}
	}

	/**
	 * Encode plain executing environment entropy.
	 *
	 * @param enc encoder to encode into
	 * @param ctxt context to resolve settings and services from, if any
	 * @param app description of executing app
	 * @param regs description of laws and regulations applicable to device
	 * @param dev description of executing device
	 * @param sensor service to query sensor entropy from, if any
	 */
	static void encodePlain(
		ProtobufEncoder enc,
		@Nullable Context ctxt,
		App app, Regs regs, Device dev,
		@Nullable SensorEntropyService sensor
	) {
		enc.encodeMessageField(PlainEntropy_CHANNEL, app)
			.encodeMessageField(PlainEntropy_REGS, regs)
			.encodeMessageField(PlainEntropy_DEVICE, dev);
		encodePlainClockDrifts(enc);
		if (sensor != null)
			sensor.encodePlainEntropySensors(enc);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
			enc.encodeBoolField(PlainEntropy_CANATTESTDEVICE, true);
		enc.encodeLenField(
			PlainEntropy_ANDROID, new Pair<>(ctxt, sensor),
			(ctxtAndSensor, andEnc) -> encodeAndroid(
				andEnc,
				ctxtAndSensor.first, ctxtAndSensor.second
			)
		);
	}

	/**
	 * Encode dynamically typed entropy.
	 *
	 * @param enc encoder to encode into
	 * @param val value to encode
	 */
	static void encodeDynamic(ProtobufEncoder enc, @Nullable Object val) {
		List<Object> worklist = new ArrayList<>();
		IdentityHashMap<Object, Object> visited = new IdentityHashMap<>();

		while (true) {
			if (val == null) {
				enc.encodeBoolField(DynamicEntropy_NIL, true);
			} else if (val instanceof Float) {
				enc.encodeFieldTag(DynamicEntropy_F32)
					.encodeFloat((float) val);
			} else if (val instanceof Double) {
				enc.encodeFieldTag(DynamicEntropy_F64)
					.encodeDouble((double) val);
			} else if (val instanceof Byte) {
				enc.encodeFieldTag(DynamicEntropy_U32)
					.encodeUint32(((byte) val) & 0xff);
			} else if (val instanceof Short) {
				enc.encodeFieldTag(DynamicEntropy_U32)
					.encodeUint32(((short) val) & 0xffff);
			} else if (val instanceof Integer) {
				enc.encodeFieldTag(DynamicEntropy_U32)
					.encodeUint32((int) val);
			} else if (val instanceof Number) {
				enc.encodeFieldTag(DynamicEntropy_U64)
					.encodeUint64(((Number) val).longValue());
			} else if (val instanceof Boolean) {
				enc.encodeFieldTag(DynamicEntropy_FLAG)
					.encodeBool((boolean) val);
			} else if (val instanceof byte[]) {
				enc.encodeFieldTag(DynamicEntropy_BLOB)
					.encodeByteArray((byte[]) val);
			} else if (val instanceof ByteBuffer) {
				enc.encodeFieldTag(DynamicEntropy_BLOB)
					.encodeByteBuffer(((ByteBuffer) val).duplicate());
			} else if (val instanceof String) {
				enc.encodeFieldTag(DynamicEntropy_STR)
					.encodeString((String) val);
			} else if (val instanceof boolean[]) {
				enc.encodeFieldTag(DynamicEntropy_PFLAG)
					.encodePackedBoolArray((boolean[]) val);
			} else if (val instanceof int[]) {
				enc.encodeFieldTag(DynamicEntropy_PU32)
					.encodePackedUint32Array((int[]) val);
			} else if (val instanceof long[]) {
				enc.encodeFieldTag(DynamicEntropy_PU64)
					.encodePackedUint64Array((long[]) val);
			} else if (val instanceof float[]) {
				enc.encodeFieldTag(DynamicEntropy_PF32)
					.encodePackedFloatArray((float[]) val);
			} else if (val instanceof double[]) {
				enc.encodeFieldTag(DynamicEntropy_PF64)
					.encodePackedDoubleArray((double[]) val);
			} else if (visited.put(val, val) != val) {
				if (val instanceof ProtobufSerializable) {
					enc.encodeFieldTag(DynamicEntropy_MSG)
						.encodeMessage((ProtobufSerializable) val);
				} else if (val instanceof Pair) {
					enc.encodeUnsignedIntField(DynamicEntropy_LISTSEQ, 2);
					worklist.add(0, ((Pair<?, ?>) val).first);
					worklist.add(1, ((Pair<?, ?>) val).second);
				} else if (val instanceof Map) {
					Map<?, ?> map = (Map<?, ?>) val;
					int i = 0;

					for (Map.Entry<?, ?> ent : map.entrySet()) {
						worklist.add(i * 2 + 0, ent.getKey());
						worklist.add(i * 2 + 1, ent.getValue());
						i++;
					}
					enc.encodeFieldTag(DynamicEntropy_MAPSEQ)
						.encodeUint32(i);
				} else if (val instanceof Iterable) {
					int i = 0;

					for (Object a : ((Iterable<?>) val))
						worklist.add(i++, a);
					enc.encodeFieldTag(DynamicEntropy_LISTSEQ)
						.encodeUint32(i);
				} else if (val instanceof Supplier<?>) {
					try {
						worklist.add(0, ((Supplier<?>) val).get());
					} catch (Exception cause) {
						worklist.add(0, cause);
					}
				} else if (
					Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
					val instanceof java.util.function.Supplier
				) {
					try {
						worklist.add(0, ((java.util.function.Supplier<?>) val).get());
					} catch (Exception cause) {
						worklist.add(0, cause);
					}
				} else if (
					ANDROIDX_CORE_SUPPLIER != null &&
					ANDROIDX_CORE_SUPPLIER.isInstance(val)
				) {
					try {
						//noinspection DataFlowIssue
						worklist.add(0, ANDROIDX_CORE_SUPPLIER_GET.invoke(val));
					} catch (Exception cause) {
						worklist.add(0, cause);
					}
				} else if (val instanceof Throwable) {
					enc.encodeFieldTag(DynamicEntropy_STR)
						.encodeString(throwableToString((Throwable) val));
				} else if (val.getClass().isArray()) {
					int n = Array.getLength(val);

					for (int i = 0; i < n; i++)
						worklist.add(i, Array.get(val, i));
					enc.encodeFieldTag(DynamicEntropy_LISTSEQ)
						.encodeUint32(n);
				} else {
					enc.encodeFieldTag(DynamicEntropy_STR)
						.encodeString(val.toString());
				}
			}
			if (worklist.isEmpty())
				break;
			val = worklist.remove(0);
		}
	}

	/**
	 * Encode ciphered entropy.
	 *
	 * @param enc encoder to encode into
	 * @param id entropy id
	 * @param content {@linkplain ProtobufEncoder#isSplit() split} coding of entropy
	 * coding
	 */
	static void encodeCipher(ProtobufEncoder enc, String id, ProtobufEncoder content) {
		enc.encodeStringField(CipherEntropy_ID, id)
			.encodeByteArrayField(
				CipherEntropy_SCHEMA,
				content.schemaArray(), 0, content.schemaArrayOffset()
			)
			.encodeByteArrayField(
				CipherEntropy_CTCONTENT,
				content.contentArray(), 0, content.contentArrayOffset()
			);
	}

	private EntropyCoding() {
	}
}
