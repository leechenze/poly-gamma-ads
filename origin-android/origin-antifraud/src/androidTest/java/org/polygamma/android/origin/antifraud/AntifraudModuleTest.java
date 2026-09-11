// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.antifraud;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.polygamma.android.origin.antifraud.AntifraudModule.NULL;
import static org.polygamma.android.origin.antifraud.CheckWire.DynamicEntropy_F64;
import static org.polygamma.android.origin.antifraud.CheckWire.DynamicEntropy_NIL;
import static org.polygamma.android.origin.antifraud.CheckWire.DynamicEntropy_STR;
import static org.polygamma.android.origin.antifraud.CheckWire.DynamicEntropy_U32;
import static org.polygamma.android.origin.antifraud.CheckWire.IvtRatingHuman;
import static org.polygamma.android.origin.antifraud.CheckWire.IvtRatingUnknown;
import static org.polygamma.android.origin.antifraud.CheckWire.SessionAttestDevice;
import static org.polygamma.android.origin.antifraud.CheckWire.SessionBegin;
import static org.polygamma.android.origin.antifraud.CheckWire.SessionTamperMachine;

import android.os.SystemClock;
import android.util.Base64;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.Nullable;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageLite;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.polygamma.android.origin.adcom.AdCom;
import org.polygamma.android.origin.adcom.context.App;
import org.polygamma.android.origin.adcom.context.Device;
import org.polygamma.android.origin.antifraud.CheckWire.SessionOpcode;
import org.polygamma.android.origin.core.DeviceModule;
import org.polygamma.android.origin.core.Origin;
import org.polygamma.android.origin.core.RpcModule;
import org.polygamma.android.origin.crypt.ChaCha20;
import org.polygamma.android.origin.crypt.Csprng;
import org.polygamma.android.origin.crypt.Xtea;
import org.polygamma.android.origin.protobuf.ProtobufDecoder;
import org.polygamma.android.origin.util.Flate;
import org.polygamma.android.origin.util.Futures;
import org.polygamma.android.origin.util.Supplier;
import org.polygamma.android.origin.util.Time;
import org.polygamma.origin.antifraud.IvtCheck;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;

/**
 * {@link AntifraudModule} tests.
 */
@RunWith(AndroidJUnit4.class)
public class AntifraudModuleTest {

	private static final String TAG = AntifraudModuleTest.class.getSimpleName();
	private static final long TEST_DISPATCHER_EXCHANGE_TIMEOUT_MILLIS =
		TimeUnit.SECONDS.toMillis(30);

	private static final class TestDispatcher extends Dispatcher {
		final SynchronousQueue<Object> exchange;
		final Lock lock;
		@Nullable Throwable lastError;

		TestDispatcher() {
			this.exchange = new SynchronousQueue<>();
			this.lock = new ReentrantLock();
		}

		// Poll from exchange queue
		@SuppressWarnings("unchecked")
		<T> T poll(Class<T> type) throws InterruptedException {
			Object rv =
				this.exchange.poll(TEST_DISPATCHER_EXCHANGE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);

			assertTrue(type.isInstance(rv));
			return (T) rv;
		}

		// Push value onto exchange queue
		void push(Object v) throws InterruptedException {
			assertTrue(this.exchange.offer(
				v,
				TEST_DISPATCHER_EXCHANGE_TIMEOUT_MILLIS,
				TimeUnit.MILLISECONDS
			));
		}

		@Override
		public MockResponse dispatch(RecordedRequest req) {
			Log.w(TAG, "received request");
			this.lock.lock();
			try {
				this.push(req);
				return this.poll(MockResponse.class);
			} catch (Exception cause) {
				Log.w(TAG, "dispatch failed", cause);
				this.lastError = cause;
				return (new MockResponse())
					.setResponseCode(500);
			} finally {
				Log.w(TAG, "sent response");
				this.lock.unlock();
			}
		}
	}

	private static Origin sdk;
	private static AntifraudModule module;
	private static MockWebServer server;
	private static TestDispatcher dispatcher;
	private static Random random;

	@BeforeClass
	public static void setup() throws Exception {
		random = new Random(44);
		server = new MockWebServer();
		dispatcher = new TestDispatcher();
		server.start();
		server.setDispatcher(dispatcher);

		String host = server.getHostName();
		int port = server.getPort();

		sdk = Origin.initialize(
			InstrumentationRegistry.getInstrumentation()
				.getTargetContext().
				getApplicationContext(),
			RpcModule.ofProvider()
				.host(host)
				.port(port)
				.insecure(true)
		);
		module = sdk.loadModule(AntifraudModule.ofProvider(true));
		module.addEntropyData("a", "foo");
		module.addEntropyData("b", 123);
		module.addEntropyData("c", null);
		module.addEntropyData("d", (Supplier<Double>) () -> 456.);
	}

	@AfterClass
	public static void destroy() throws Exception {
		if (sdk != null) {
			sdk.shutdown();
			while (!sdk.awaitShutdown(10, TimeUnit.SECONDS))
				Log.w(TAG, "sdk shutdown taking longer than 10 seconds");
			assertTrue(sdk.isShutdown());
			sdk = null;
		}
		if (module != null) {
			assertTrue(module.destroyed);
			assertNull(module.callCheckFuture);
			assertNull(module.checkSession);
			assertNull(module.sensors);
			module = null;
		}
		if (server != null) {
			server.shutdown();
			server = null;
			dispatcher = null;
		}
	}

	private static void checkDispatcherError() {
		Throwable cause = dispatcher.lastError;

		if (cause != null)
			throw new AssertionError("dispatcher error", cause);
	}

	// Poll next check arguments.
	private static IvtCheck.CheckArguments pollCheckArguments() {
		checkDispatcherError();

		RecordedRequest http;

		try {
			http = dispatcher.poll(RecordedRequest.class);
		} catch (InterruptedException cause) {
			throw new AssertionError(cause);
		}

		checkDispatcherError();

		String path = http.getPath();
		String method = http.getMethod();

		assertNotNull(path);
		assertTrue(path.startsWith(CheckWire.RPC_PROCEDURE_ID));

		byte[] body;

		if ("GET".equals(method)) {
			String args = path.substring(CheckWire.RPC_PROCEDURE_ID.length());

			if (args.isEmpty() || "/".equals(args)) {
				body = new byte[0];
			} else {
				assertTrue(args.startsWith("/"));
				body = Base64.decode(
					args.substring(1)
						.getBytes(StandardCharsets.UTF_8),
					Base64.NO_PADDING | Base64.NO_WRAP | Base64.URL_SAFE
				);
			}
		} else {
			body = http.getBody()
				.readByteArray();
			if ("deflate".equals(http.getHeader("Content-Encoding"))) {
				ByteBuffer deflate = Flate.decompressZlib(body);

				body = Arrays.copyOfRange(
					deflate.array(),
					deflate.arrayOffset() + deflate.position(),
					deflate.arrayOffset() + deflate.position() + deflate.remaining()
				);
			}
		}

		IvtCheck.CheckArguments args;

		try {
			args = IvtCheck.CheckArguments.parseFrom(body);
		} catch (InvalidProtocolBufferException cause) {
			throw new AssertionError(cause);
		}
		assertEquals("", args.getError());
		return args;
	}

	// Respond to polled arguments.
	private static void pushCheckResult(@Nullable IvtCheck.CheckResult res) {
		checkDispatcherError();

		MockResponse http = new MockResponse();

		if (res == null) {
			http.setResponseCode(204);
		} else {
			http.setResponseCode(200)
				.setBody((new Buffer()).write(res.toByteArray()));
		}
		try {
			dispatcher.push(http);
		} catch (InterruptedException cause) {
			throw new AssertionError(cause);
		}
	}

	private static byte[]
	pushSessionOperations(Csprng sessCsprng, IvtCheck.SessionOperation... ops) {
		byte[] sessId = new byte[1 + random.nextInt(32)];

		random.nextBytes(sessId);

		IvtCheck.CheckResult.Builder res = IvtCheck.CheckResult.newBuilder()
			.setSessid(ByteString.copyFrom(sessId));
		byte[] key = new byte[Xtea.KEY_SIZE];
		Xtea cipher = Xtea.ofEmpty();

		for (int i = 0; i < ops.length; i++) {
			IvtCheck.SessionOperation op = ops[i];

			if (!op.hasPayload() || op.getPayload().isEmpty()) {
				res.addOp(op);
				continue;
			}

			byte[] payload = op.getPayload().toByteArray();
			int blockRem = payload.length % Xtea.BLOCK_SIZE;
			int pad = 0;

			if (blockRem != 0) {
				pad = Xtea.BLOCK_SIZE - blockRem;
				payload = Arrays.copyOfRange(payload, 0, payload.length + pad);
			}
			sessCsprng.nextBytes(key, 0, Xtea.KEY_SIZE);
			cipher.setKey(key, 0)
				.encipher(payload, 0, payload, 0, payload.length);
			res.addOp(
				IvtCheck.SessionOperation.newBuilder()
					.setCode(op.getCode())
					.setPayload(ByteString.copyFrom(payload))
					.setPayloadpad(pad)
					.build()
			);
		}
		pushCheckResult(res.build());
		return sessId;
	}

	private static MessageLite decipherSessionPayload(
		@SessionOpcode int code,
		IvtCheck.SessionOperation op,
		MessageLite.Builder builder,
		Csprng sessCsprng
	) {
		assertEquals(code, op.getCodeValue());
		assertFalse(op.hasError());
		assertTrue(op.hasPayload());

		byte[] payload = op.getPayload().toByteArray();

		if (payload.length == 0)
			return builder.build();

		byte[] key = new byte[Xtea.KEY_SIZE];

		sessCsprng.nextBytes(key, 0, Xtea.KEY_SIZE);
		Xtea.ofKey(key, 0)
			.decipher(payload, 0, payload, 0, payload.length);
		try {
			builder.mergeFrom(payload, 0, payload.length - op.getPayloadpad());
		} catch (InvalidProtocolBufferException cause) {
			throw new AssertionError(cause);
		}
		return builder.build();
	}

	private static Pair<Csprng, IvtCheck.PlainEntropy> checkSessionRequest() {
		IvtCheck.CheckArguments args = pollCheckArguments();

		assertTrue(args.hasSesskeyseed());
		assertFalse(args.hasSessid());
		assertEquals("", args.getError());
		assertEquals(Time.nowUtcSeconds(), args.getTimestampsec(), 10);
		assertEquals(1, args.getOpCount());

		Csprng sessCsprng = Csprng.ofSeed(args.getSesskeyseed().toByteArray());
		IvtCheck.SessionOperation op = args.getOp(0);

		assertEquals(SessionBegin, op.getCodeValue());
		assertFalse(op.hasError());

		IvtCheck.SessionRequest sessReq = (IvtCheck.SessionRequest) decipherSessionPayload(
			SessionBegin, op,
			IvtCheck.SessionRequest.newBuilder(),
			sessCsprng
		);

		assertEquals(AdCom.DOMAIN_VERSION, sessReq.getAdcomver());
		assertArrayEquals(
			module.checkResult.status.digest == null ? new byte[0] :
			module.checkResult.status.digest,
			sessReq.getLastdigest().toByteArray()
		);

		assertFalse(sessReq.hasPtent());
		assertTrue(sessReq.hasPtentflate());

		IvtCheck.PlainEntropy plainEnt;

		try {
			plainEnt = IvtCheck.PlainEntropy.parseFrom(Flate.decompressZlib(
				sessReq.getPtentflate()
					.asReadOnlyByteBuffer(),
				true
			));
		} catch (InvalidProtocolBufferException cause) {
			throw new AssertionError(cause);
		}

		App gotApp = App.ofProtobuf(ProtobufDecoder.of(plainEnt.getChannel().toByteArray()));
		App expApp = sdk.app();

		assertEquals(expApp.debuggable(), gotApp.debuggable());
		assertEquals(expApp.storeId(), gotApp.storeId());
		assertEquals(expApp.system(), gotApp.system());
		assertEquals(expApp.version(), gotApp.version());
		assertEquals(expApp.id(), gotApp.id());
		assertEquals(expApp.name(), gotApp.name());
		assertEquals(expApp.publisherId(), gotApp.publisherId());

		Device gotDev = Device.ofProtobuf(ProtobufDecoder.of(plainEnt.getDevice().toByteArray()));
		Device expDev = sdk.loadModule(DeviceModule.class).device();

		assertNotNull(expDev);
		assertEquals(expDev.advertisingIdCount(), gotDev.advertisingIdCount());
		for (int i = 0; i < expDev.advertisingIdCount(); i++) {
			Pair<String, String> expDevId = expDev.advertisingId(i);
			Pair<String, String> gotDevId = gotDev.advertisingId(i);

			assertEquals(expDevId.first, gotDevId.first);
			assertEquals(expDevId.second, gotDevId.second);
		}
		assertEquals(expDev.carrierMccMnc(), gotDev.carrierMccMnc());
		assertEquals(expDev.carrierName(), gotDev.carrierName());
		assertEquals(expDev.languageCode(), gotDev.languageCode());
		assertEquals(expDev.manufacturerName(), gotDev.manufacturerName());
		assertEquals(expDev.modelName(), gotDev.modelName());
		assertEquals(expDev.modelVersion(), gotDev.modelVersion());
		assertEquals(expDev.operatingSystem(), gotDev.operatingSystem());
		assertEquals(expDev.operatingSystemVersion(), gotDev.operatingSystemVersion());
		assertEquals(expDev.screenHeightPx(), gotDev.screenHeightPx());
		assertEquals(expDev.screenWidthPx(), gotDev.screenWidthPx());

		IvtCheck.AndroidEntropy andEnt = plainEnt.getAndroid();

		assertEquals(android.os.Process.myPid(), andEnt.getPid());
		assertEquals(android.os.Process.myUid(), andEnt.getUid());

		CheckSession sess = module.checkSession;
		ChaCha20 ctEntCipher = ChaCha20.ofEmpty();

		assertNotNull(sess);
		assertTrue(sessReq.getCtentCount() >= 4);
		assertNotNull(sess.secretEntropyKey);
		ctEntCipher.setKey(sess.secretEntropyKey, 0);

		HashSet<String> gotCtEnt = new HashSet<>();

		for (int i = 0; i < sessReq.getCtentCount(); i++) {
			IvtCheck.CipherEntropy ctEnt = sessReq.getCtent(i);
			String ctEntId = ctEnt.getId();

			if (
				!ctEntId.equals("a") &&
				!ctEntId.equals("b") &&
				!ctEntId.equals("c") &&
				!ctEntId.equals("d")
			) {
				continue;
			}

			assertTrue(gotCtEnt.add(ctEntId));

			byte[] schema = ctEnt.getSchema().toByteArray();
			byte[] content = ctEnt.getCtcontent().toByteArray();

			ctEntCipher.setCounter(0)
				.setNonce(i + 1)
				.xor(content, 0, content, 0, content.length);

			ProtobufDecoder ctEntDec = ProtobufDecoder.ofSplit(schema, content);

			assertTrue(ctEntDec.hasRemaining());

			int tag = ctEntDec.decodeFieldTag();

			if (ctEntId.equals("a")) {
				assertEquals(DynamicEntropy_STR, tag);
				assertEquals("foo", ctEntDec.decodeString());
			} else if (ctEntId.equals("b")) {
				assertEquals(DynamicEntropy_U32, tag);
				assertEquals(123, ctEntDec.decodeUint32());
			} else if (ctEntId.equals("c")) {
				assertEquals(DynamicEntropy_NIL, tag);
				assertTrue(ctEntDec.decodeBool());
			} else {
				assertEquals(DynamicEntropy_F64, tag);
				assertEquals(456., ctEntDec.decodeDouble(), 0.);
			}
		}
		assertTrue(gotCtEnt.contains("a"));
		assertTrue(gotCtEnt.contains("b"));
		assertTrue(gotCtEnt.contains("c"));
		assertTrue(gotCtEnt.contains("d"));
		return new Pair<>(sessCsprng, plainEnt);
	}

	@Test
	public void testCheck() throws Exception {
		// Check result initially should be empty.
		assertEquals(0L, module.checkResult.nextCheckDelaySeconds());
		assertEquals(IvtRatingUnknown, module.checkResult.status.rating);
		assertEquals(0, module.checkResult.status.confidence);
		assertNull(module.checkResult.status.digest);

		// We should have a module event listener
		assertNotNull(module.onModuleEvent);

		// We should not be destroyed.
		assertFalse(module.destroyed);

		// We should have a sensor service that's running.
		assertNotNull(module.sensors);
		assertFalse(module.sensors.isPaused());

		// We should have a call check pending.
		Object fut = module.callCheckFuture;

		assertNotNull(fut);
		assertTrue(fut instanceof Future<?> || fut == NULL);

		// We should be expecting a new request.
		Pair<Csprng, IvtCheck.PlainEntropy> sessCsprngAndPlainEnt = checkSessionRequest();
		Csprng sessCsprng = sessCsprngAndPlainEnt.first;
		IvtCheck.PlainEntropy plainEnt = sessCsprngAndPlainEnt.second;
		CheckSession sess = module.checkSession;
		SensorEntropyService sensors = module.sensors;

		// Future now should be a `Future` for the RPC call.
		fut = module.callCheckFuture;
		assertTrue(fut instanceof Future<?>);
		assertNotNull(sess);

		byte[] sessId = pushSessionOperations(
			sessCsprng,
			IvtCheck.SessionOperation.newBuilder()
				.setCode(IvtCheck.SessionOpcode.SessionAttestDevice)
				.setPayload(
					IvtCheck.AttestDeviceRequest.newBuilder()
						.setChallenge(ByteString.copyFrom("challenge", StandardCharsets.UTF_8))
						.build()
						.toByteString()
				)
				.build()
		);

		assertSame(sess, Futures.await((Future<?>) fut));

		// We should expect a result for the attestation challenge.
		IvtCheck.CheckArguments args = pollCheckArguments();

		assertArrayEquals(sessId, args.getSessid().toByteArray());
		assertEquals(Time.nowUtcSeconds(), args.getTimestampsec(), 5);
		assertEquals(1, args.getOpCount());

		IvtCheck.SessionOperation op = args.getOp(0);

		assertEquals(SessionAttestDevice, op.getCodeValue());
		if (!plainEnt.getCanattestdevice()) {
			assertTrue(op.hasError());
			assertFalse(op.hasPayload());
			assertTrue(op.getError().contains("java.lang.UnsupportedOperationException"));
		} else {
			CheckSessionTest.checkAttestDeviceResponse(
				"challenge".getBytes(StandardCharsets.UTF_8),
				(IvtCheck.AttestDeviceResponse) decipherSessionPayload(
					SessionAttestDevice,
					op,
					IvtCheck.AttestDeviceResponse.newBuilder(),
					sessCsprng
				)
			);
		}

		// Make sure session hasn't changed.
		assertSame(sess, module.checkSession);
		assertFalse(sess.hasEnded());
		assertSame(sensors, module.sensors);
		if (sensors != null)
			assertFalse(sensors.isPaused());

		// we should have a new future
		assertNotSame(fut, module.callCheckFuture);
		fut = module.callCheckFuture;
		assertTrue(fut instanceof Future<?>);

		// Respond with some tamper ops and session end.
		int conf = random.nextInt(100);
		byte[] digest = new byte[1 + random.nextInt(32)];

		random.nextBytes(digest);

		sessId = pushSessionOperations(
			sessCsprng,
			IvtCheck.SessionOperation.newBuilder()
				.setCode(IvtCheck.SessionOpcode.SessionEnd)
				.setPayload(
					IvtCheck.SessionResult.newBuilder()
						.setRating(IvtCheck.IvtRating.IvtRatingHuman)
						.setConf(conf)
						.setDigest(ByteString.copyFrom(digest))
						.setRecktimestampsec(
							Time.nowUtcSeconds() +
							((AntifraudModule.SENSORS_CHECK_DELTA_SECONDS * 2) - 1)
						)
						.build()
						.toByteString()
				)
				.build(),
			IvtCheck.SessionOperation.newBuilder()
				.setCode(IvtCheck.SessionOpcode.SessionTamperMachine)
				.setPayload(
					IvtCheck.TamperMachineRequest.newBuilder()
						.addOp(
							IvtCheck.TamperMachineOperation.newBuilder()
								.setCode(IvtCheck.TamperMachineOpcode.TamperMachinePush)
								.setStr("123")
								.build()
						)
						.addOp(
							IvtCheck.TamperMachineOperation.newBuilder()
								.setCode(IvtCheck.TamperMachineOpcode.TamperMachinePop)
								.build()
						)
						.build()
						.toByteString()
				)
				.build()
		);

		assertSame(sess, Futures.await((Future<?>) fut));

		/*
		 * We should expect session ended, antifraud module with new result, sensors service
		 * still active, and next check scheduled.
		 */
		args = pollCheckArguments();

		assertTrue(sess.hasEnded());
		assertNull(module.checkSession);
		assertSame(sensors, module.sensors);
		if (sensors != null)
			assertFalse(sensors.isPaused());

		assertNotSame(fut, module.callCheckFuture);
		fut = module.callCheckFuture;
		assertTrue(fut instanceof Future<?>);

		assertEquals(IvtRatingHuman, module.checkResult.status.rating);
		assertEquals(conf, module.checkResult.status.confidence);
		assertArrayEquals(digest, module.checkResult.status.digest);
		assertEquals(
			(AntifraudModule.SENSORS_CHECK_DELTA_SECONDS * 2) - 1,
			module.checkResult.nextCheckDelaySeconds(),
			5
		);

		assertArrayEquals(sessId, args.getSessid().toByteArray());
		assertEquals(Time.nowUtcSeconds(), args.getTimestampsec(), 5);
		assertEquals(1, args.getOpCount());

		IvtCheck.DynamicEntropy tamperStack =
			((IvtCheck.TamperMachineResponse) decipherSessionPayload(
				SessionTamperMachine, args.getOp(0),
				IvtCheck.TamperMachineResponse.newBuilder(),
				sessCsprng
			)).getStack();

		assertEquals(1, tamperStack.getStrCount());
		assertEquals("123", tamperStack.getStr(0));

		pushCheckResult(null);

		// We should be get a new session.
		sessCsprngAndPlainEnt = checkSessionRequest();
		sessCsprng = sessCsprngAndPlainEnt.first;
		plainEnt = sessCsprngAndPlainEnt.second;
		sess = module.checkSession;

		// Future now should be a `Future` for the RPC call.
		fut = module.callCheckFuture;
		assertTrue(fut instanceof Future<?>);
		assertNotNull(sess);

		// sensor service should be the same as it was
		assertFalse(sess.hasEnded());
		assertSame(sensors, module.sensors);
		if (sensors != null)
			assertFalse(sensors.isPaused());

		sessId = pushSessionOperations(
			sessCsprng,
			IvtCheck.SessionOperation.newBuilder()
				.setCode(IvtCheck.SessionOpcode.SessionAttestDevice)
				.setPayload(
					IvtCheck.AttestDeviceRequest.newBuilder()
						.setChallenge(ByteString.copyFrom("challenge2", StandardCharsets.UTF_8))
						.build()
						.toByteString()
				)
				.build()
		);

		assertSame(sess, Futures.await((Future<?>) fut));

		// We should expect a result for the attestation challenge.
		args = pollCheckArguments();

		assertArrayEquals(sessId, args.getSessid().toByteArray());
		assertEquals(Time.nowUtcSeconds(), args.getTimestampsec(), 5);
		assertEquals(1, args.getOpCount());

		op = args.getOp(0);
		assertEquals(SessionAttestDevice, op.getCodeValue());
		if (!plainEnt.getCanattestdevice()) {
			assertTrue(op.hasError());
			assertFalse(op.hasPayload());
			assertTrue(op.getError().contains("java.lang.UnsupportedOperationException"));
		} else {
			CheckSessionTest.checkAttestDeviceResponse(
				"challenge2".getBytes(StandardCharsets.UTF_8),
				(IvtCheck.AttestDeviceResponse) decipherSessionPayload(
					SessionAttestDevice,
					op,
					IvtCheck.AttestDeviceResponse.newBuilder(),
					sessCsprng
				)
			);
		}

		// Make sure session hasn't changed.
		assertSame(sess, module.checkSession);
		assertFalse(sess.hasEnded());
		assertSame(sensors, module.sensors);
		if (sensors != null)
			assertFalse(sensors.isPaused());

		// we should have a new future
		assertNotSame(fut, module.callCheckFuture);
		fut = module.callCheckFuture;
		assertTrue(fut instanceof Future<?>);

		pushCheckResult(null);
		assertNull(Futures.await((Future<?>) fut));

		// Small delay for listener to execute.
		SystemClock.sleep(1000);

		// Session should have ended, and sensor service should be paused.
		assertTrue(sess.hasEnded());
		assertNull(module.checkSession);
		assertSame(sensors, module.sensors);
		if (sensors != null)
			assertTrue(sensors.isPaused());

		// Status shouldn't have changed save delay.
		assertEquals(IvtRatingHuman, module.checkResult.status.rating);
		assertEquals(conf, module.checkResult.status.confidence);
		assertArrayEquals(digest, module.checkResult.status.digest);
		assertEquals(
			CheckWire.MAX_RECHECK_DELAY_SECONDS,
			module.checkResult.nextCheckDelaySeconds(),
			5
		);

		// We should have next check at maximum delay now.
		assertNotSame(fut, module.callCheckFuture);
		fut = module.callCheckFuture;
		assertTrue(fut instanceof ScheduledFuture<?>);
		assertEquals(
			CheckWire.MAX_RECHECK_DELAY_SECONDS,
			((ScheduledFuture<?>) fut).getDelay(TimeUnit.SECONDS),
			10.
		);
	}
}
