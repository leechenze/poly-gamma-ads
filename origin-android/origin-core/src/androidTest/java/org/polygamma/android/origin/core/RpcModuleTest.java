// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.polygamma.android.origin.protobuf.Protobuf.WIRE_LEN;

import android.os.SystemClock;
import android.util.ArrayMap;
import android.util.Base64;

import androidx.annotation.Nullable;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.polygamma.android.origin.protobuf.Protobuf;
import org.polygamma.android.origin.protobuf.Protobuf.FieldTag;
import org.polygamma.android.origin.protobuf.ProtobufDecoder;
import org.polygamma.android.origin.protobuf.ProtobufEncoder;
import org.polygamma.android.origin.protobuf.ProtobufSerializable;
import org.polygamma.android.origin.util.CollectionsCompat;
import org.polygamma.android.origin.util.ExecutingService;
import org.polygamma.android.origin.util.Flate;
import org.polygamma.android.origin.util.Time;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpRetryException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.zip.GZIPOutputStream;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.QueueDispatcher;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;

/**
 * {@link RpcModule} tests.
 */
@SuppressWarnings({"DataFlowIssue", "resource"})
@RunWith(AndroidJUnit4.class)
public class RpcModuleTest extends TestWithSdk {

	private static final String SERVICE = "test";

	/**
	 * Test message.
	 */
	private static final class TestMessage implements ProtobufSerializable {

		private static final @FieldTag int DATA = Protobuf.fieldTagOf(1, WIRE_LEN);

		/**
		 * Construct new test message with random data of specific length.
		 *
		 * @param len message data length, in bytes
		 * @return resulting message
		 */
		static TestMessage ofRandom(int len) {
			Random rand = new Random();
			ByteBuffer word = ByteBuffer.allocate(4);
			ByteBuffer data = ByteBuffer.allocate(len);

			while (data.hasRemaining()) {
				word.clear();
				word.putInt(0, rand.nextInt());

				while (word.hasRemaining() && data.hasRemaining())
					data.put(word.get());
			}
			return new TestMessage((ByteBuffer) data.flip());
		}

		static TestMessage ofProtobuf(ProtobufDecoder dec) {
			ByteBuffer data = ByteBuffer.allocate(0);

			while (dec.hasRemaining()) {
				int tag = dec.decodeFieldTag();

				if (tag == DATA)
					data = ByteBuffer.wrap(dec.decodeByteArray());
				else
					dec.skipFieldValue(tag);
			}
			return new TestMessage(data);
		}

		final ByteBuffer data;

		TestMessage(ByteBuffer data) {
			this.data = data;
		}

		@Override
		public void toProtobuf(ProtobufEncoder enc) {
			byte[] data = new byte[this.data.remaining()];

			this.data.duplicate()
				.get(data);
			enc.encodeByteArrayField(DATA, data);
		}

		@Override
		public boolean equals(@Nullable Object that) {
			return that instanceof TestMessage && ((TestMessage) that).data.equals(this.data);
		}
	}

	/**
	 * Call request exchange test.
	 */
	private static final class TestCallCase {
		final long timeoutMillis;
		final @Nullable String serviceVersion;
		final String procedure;
		final @HttpMethod String method;
		final @Nullable ByteBuffer arguments;
		final @Nullable ByteBuffer response;
		final boolean dropResponse;

		TestCallCase(
			@Nullable String svcVer,
			String proc,
			int argsSizeBytes,
			int resSizeBytes,
			boolean dropResp,
			long timeoutMillis
		) {
			ProtobufEncoder enc = ProtobufEncoder.of();

			this.timeoutMillis = timeoutMillis;
			this.serviceVersion = svcVer;
			this.procedure = proc;
			if (argsSizeBytes == 0) {
				this.arguments = null;
			} else {
				TestMessage.ofRandom(argsSizeBytes)
					.toProtobuf(enc.reset());
				this.arguments = ByteBuffer.wrap(enc.intoArray());
			}
			if (resSizeBytes == 0) {
				this.response = null;
			} else {
				TestMessage.ofRandom(resSizeBytes)
					.toProtobuf(enc.reset());
				this.response = ByteBuffer.wrap(enc.intoArray());
			}

			// /<svc>/<proc>/<args>
			long pathLen =
				RpcModule.idOfProcedure(SERVICE, proc, svcVer).length() + 1 +
				RpcModule.estimateBase64CodingOf(argsSizeBytes);

			this.method =
				pathLen <= RpcModule.HTTP_GET_CALL_PATH_THRESHOLD ? "GET" :
				"POST";
			this.dropResponse = dropResp;
		}

		TestCallCase(
			@Nullable String svcVer, String proc,
			int argsSizeBytes, int resSizeBytes, boolean dropResp
		) {
			this(svcVer, proc, argsSizeBytes, resSizeBytes, dropResp, 0L);
		}
	}

	private static RpcModule client;
	private static ArrayMap<RpcHostRecord, MockWebServer> servers;

	/**
	 * Wait until client is done {@linkplain RpcModule#processor processing}.
	 */
	private static void awaitProcessDone() {
		for (
			int state = client.processor.state();
			state == ExecutingService.STATE_RUNNING ||
			state == ExecutingService.STATE_SCHEDULED;
			state = client.processor.state()
		) {
			SystemClock.sleep(15);
			// processing is done, we're waiting for something like timeouts
			if (client.processor.nextExecutionDelayMillis() >= 10)
				return;
		}
	}

	@BeforeClass
	public static void setupRpc() throws Exception {
		client = sdk.loadModule(
			RpcModule.ofProvider()
				.port(8080)
				.insecure(true)
		);

		// our settings are cleared, no service host records should have been loaded
		awaitProcessDone();

		client.lock.readLock().lock();
		try {
			assertEquals(ExecutingService.STATE_IDLE, client.processor.state());
			assertTrue(client.servicesHostRecords.isEmpty());
		} finally {
			client.lock.readLock().unlock();
		}

		String host = client.hostOfService(SERVICE);
		Collection<RpcHostRecord> recs =
			RpcHostRecord.ofQuery(TestUtil.context(), Runnable::run, host, 5000);

		if (recs.isEmpty())
			recs = Collections.singleton(RpcHostRecord.ofHost(host, 8080));
		servers = new ArrayMap<>(recs.size());
		for (RpcHostRecord rec : recs) {
			MockWebServer server = new MockWebServer();

			server.start(InetAddress.getByName(rec.host), rec.port);
			server.setDispatcher(new QueueDispatcher());
			servers.put(rec, server);
		}
	}

	@AfterClass
	public static void destroyRpc() throws Exception {
		for (int i = 0; i < servers.size(); i++)
			servers.valueAt(i).close();
		servers.clear();

		// make sure records are saved properly
		ArrayMap<String, Set<RpcHostRecord>> exp;

		client.lock.readLock().lock();
		try {
			exp = new ArrayMap<>(client.servicesHostRecords.size());
			for (int i = 0; i < client.servicesHostRecords.size(); i++) {
				List<RpcHostRecord> svcRecs = client.servicesHostRecords.valueAt(i).toRecords();

				if (svcRecs.isEmpty())
					continue;
				exp.put(
					client.servicesHostRecords.keyAt(i),
					CollectionsCompat.newArraySet(svcRecs)
				);
			}
		} finally {
			client.lock.readLock().unlock();
		}

		sdk.shutdown();
		assertTrue(sdk.awaitShutdown(1, TimeUnit.MINUTES));

		sdk = TestUtil.callOnMainSync(() -> Origin.initialize(TestUtil.context()));
		client = sdk.loadModule(RpcModule.ofProvider().insecure(true));
		awaitProcessDone();

		ArrayMap<String, RpcServiceHostRecords> got;

		client.lock.readLock().lock();
		try {
			assertEquals(ExecutingService.STATE_IDLE, client.processor.state());
			got = client.servicesHostRecords;
			assertEquals(exp.size(), got.size());
			for (int i = 0; i < got.size(); i++) {
				assertEquals(
					exp.get(got.keyAt(i)),
					CollectionsCompat.newArraySet(got.valueAt(i).toRecords())
				);
			}
		} finally {
			client.lock.readLock().unlock();
		}
	}

	/**
	 * Construct response for a test call case.
	 *
	 * @param test test case describing response to build
	 * @return resulting response
	 * @throws IOException I/O error was encountered
	 */
	private static MockResponse httpResponseOf(TestCallCase test) throws IOException {
		MockResponse rv = new MockResponse();

		if (test.response == null) {
			rv.setResponseCode(204);
			return rv;
		}

		try (Buffer body = new Buffer()) {
			try (OutputStream gzip = new GZIPOutputStream(body.outputStream())) {
				gzip.write(
					test.response.array(),
					test.response.arrayOffset() + test.response.position(),
					test.response.remaining()
				);
			}
			rv.setResponseCode(200)
				.addHeader("Content-Encoding", "gzip")
				.setBody(body);
		}
		return rv;
	}

	/**
	 * Find server for a host record.
	 *
	 * @param rec record to find server for
	 * @return resulting server
	 * @throws AssertionError {@code rec} does not have a server
	 */
	private static MockWebServer serverOf(RpcHostRecord rec) {
		MockWebServer server = servers.get(rec);

		assertNotNull("server not found for: " + rec, server);
		return server;
	}

	/**
	 * Ensure call request is well formed.
	 *
	 * @param rec record of host request is expected to be received on
	 * @param test test case describing request which should be received
	 * @throws InterruptedException interrupted while waiting for request to be received
	 */
	private static void assertCallRequest(RpcHostRecord rec, TestCallCase test)
	throws InterruptedException {
		MockWebServer server = serverOf(rec);
		RecordedRequest req = server.takeRequest(5, TimeUnit.SECONDS);

		assertNotNull(req);
		if (test.serviceVersion != null) {
			assertTrue(req.getPath().startsWith(String.format(
				"/%s/%s/%s",
				SERVICE, test.serviceVersion, test.procedure
			)));
		} else {
			assertTrue(req.getPath().startsWith(String.format("/%s/%s", SERVICE, test.procedure)));
		}
		assertEquals(test.method, req.getMethod());

		assertNotNull(req.getHeader("Host"));
		assertEquals(
			Origin.VENDOR + '.' + Origin.NAME + '/' + Origin.VERSION + "-android",
			req.getHeader("origin-build")
		);
		assertEquals(sdk.app().storeId(), req.getHeader("x-requested-with"));

		if ("GET".equals(req.getMethod())) {
			String argsBase64 = req.getPath()
				.substring(RpcModule.idOfProcedure(
					SERVICE,
					test.procedure,
					test.serviceVersion
				).length());

			if (test.arguments == null) {
				assertEquals("", argsBase64);
			} else {
				boolean deflate = "deflate".equals(req.getHeader("origin-encoding"));

				assertTrue(argsBase64.startsWith("/"));

				ByteBuffer body = ByteBuffer.wrap(Base64.decode(
					argsBase64.substring(1).getBytes(StandardCharsets.UTF_8),
					Base64.NO_PADDING | Base64.NO_WRAP | Base64.URL_SAFE
				));

				assertEquals(test.arguments, deflate ? Flate.decompressZlib(body) : body);
			}
		} else {
			boolean deflate = "deflate".equals(req.getHeader("Content-Encoding"));
			ByteBuffer body = ByteBuffer.wrap(req.getBody().readByteArray());

			assertEquals("application/octet-stream", req.getHeader("Content-Type"));
			assertNotNull(test.arguments);
			assertEquals(test.arguments, deflate ? Flate.decompressZlib(body) : body);
		}
	}

	private static void assertCallResponse(RpcModule.CallRequest call, TestCallCase test)
	throws ExecutionException, InterruptedException {
		Object res = call.get();

		if (test.response == null || test.dropResponse) {
			assertNull(res);
		} else {
			ProtobufEncoder enc = ProtobufEncoder.of();

			((TestMessage) res).toProtobuf(enc);
			assertEquals(test.response, enc.asBuffer());
		}
	}

	/**
	 * Send a call a request.
	 *
	 * @param test test case of call to request
	 * @return resulting request
	 */
	private static RpcModule.CallRequest sendCallRequest(TestCallCase test) {
		ListenableFuture<?> fut;

		if (test.arguments == null) {
			fut = test.response == null || test.dropResponse ?
				client.callVoidWithoutArguments(
					RpcModule.idOfProcedure(SERVICE, test.procedure, test.serviceVersion),
					test.timeoutMillis,
					TimeUnit.MILLISECONDS
				) :
				client.callWithoutArguments(
					RpcModule.idOfProcedure(SERVICE, test.procedure, test.serviceVersion),
					TestMessage::ofProtobuf,
					test.timeoutMillis,
					TimeUnit.MILLISECONDS
				);
		} else {
			TestMessage args =
				TestMessage.ofProtobuf(ProtobufDecoder.ofBuffer(test.arguments.duplicate()));

			fut = test.response == null || test.dropResponse ?
				client.callVoid(
					RpcModule.idOfProcedure(SERVICE, test.procedure, test.serviceVersion),
					args,
					test.timeoutMillis,
					TimeUnit.MILLISECONDS
				) :
				client.call(
					RpcModule.idOfProcedure(SERVICE, test.procedure, test.serviceVersion),
					args,
					TestMessage::ofProtobuf,
					test.timeoutMillis,
					TimeUnit.MILLISECONDS
				);
		}
		return (RpcModule.CallRequest) fut;
	}

	/**
	 * Respond to a call request.
	 *
	 * @param rec record of host to send response on
	 * @param test test case of call to respond to
	 */
	private static void sendCallResponse(RpcHostRecord rec, TestCallCase test) throws IOException {
		serverOf(rec)
			.enqueue(httpResponseOf(test));
	}

	@Test
	public void testCallInvalid() {
		assertThrows(
			IllegalArgumentException.class,
			() -> client.callVoidWithoutArguments("invalid/name")
		);
		assertThrows(
			IllegalArgumentException.class,
			() -> client.callVoid("invalid/name", TestMessage.ofRandom(8))
		);
	}

	@Test
	public void testCall() throws Exception {
		RpcHostRecord rec = null;

		// test normal calls which should succeed just fine
		for (TestCallCase test : new TestCallCase[] {
			// void call with no arguments and response: should be a GET
			new TestCallCase(null, "void_no_args", 0, 0, false),
			new TestCallCase("1.0", "void_no_args_ver", 0, 0, false),
			// void call with small arguments: should be a GET
			new TestCallCase(null, "void_small_args", 128, 0, false),
			new TestCallCase("1.0", "void_small_args_ver", 128, 0, false),
			// void call with a response should drop response: should be a GET
			new TestCallCase(null, "void_small_args_drop_body", 128, 128, true),
			new TestCallCase("1.0", "void_small_args_drop_body_ver", 128, 128, true),
			// void call with large arguments: should be a POST
			new TestCallCase(
				null, "void_large_args",
				RpcModule.HTTP_GET_CALL_PATH_THRESHOLD, 0, false
			),
			new TestCallCase(
				"1.0", "void_large_args_ver",
				RpcModule.HTTP_GET_CALL_PATH_THRESHOLD, 0, false
			),
			// non-void calls have a response: should be a GET
			new TestCallCase(null, "test_no_args", 0, 128, false),
			new TestCallCase("1.0", "test_no_args_ver", 0, 128, false),
			// non-void calls with small arguments have a response: should be a GET
			new TestCallCase(null, "test_small_args", 128, 128, false),
			new TestCallCase("1.0", "test_small_args_ver", 128, 128, false),
			// non-void calls with large arguments have a response: should be a POST
			new TestCallCase(
				null, "test_large_args",
				RpcModule.HTTP_GET_CALL_PATH_THRESHOLD, 128, true
			),
			new TestCallCase(
				"1.0", "test_large_args",
				RpcModule.HTTP_GET_CALL_PATH_THRESHOLD, 128, true
			)
		}) {
			RpcModule.CallRequest call = sendCallRequest(test);

			if (call.hostRecord == null) {
				// service host records not resolved yet
				awaitProcessDone();
				assertNotNull(call.hostRecord);
			}
			// same host should be receiving the request since nothing has failed
			if (rec == null)
				rec = call.hostRecord;
			else
				assertSame(rec, call.hostRecord);

			assertTrue(client.activeCalls.contains(call));
			sendCallResponse(rec, test);
			assertCallRequest(rec, test);
			assertCallResponse(call, test);
			assertTrue(call.isDone());
			assertSame(rec, call.hostRecord);
			assertEquals(Long.MAX_VALUE, rec.lastFailureDurationSeconds());
			// wait a little bit for call future to notify the client it's done
			SystemClock.sleep(15);
			assertFalse(client.activeCalls.contains(call));
		}

		assertNotNull(rec);

		// now test failures
		TestCallCase test;
		RpcModule.CallRequest call;
		Throwable cause;
		long now;

		// call should fail, 404 isn't recoverable
		serverOf(rec).enqueue((new MockResponse()).setResponseCode(404));

		test = new TestCallCase(null, "void_error_404", 128, 0, false);
		call = sendCallRequest(test);

		assertTrue(client.activeCalls.contains(call));
		assertCallRequest(rec, test);

		cause = assertThrows(ExecutionException.class, call::get);
		assertTrue(cause.getCause() instanceof IllegalStateException);
		assertTrue(cause.getCause().getMessage().startsWith("invalid status 404"));
		assertTrue(call.isDone());
		assertFalse(call.isCancelled());
		SystemClock.sleep(15);
		assertFalse(client.activeCalls.contains(call));

		// record should not be marked erroneous
		assertSame(rec, call.hostRecord);
		assertEquals(0L, rec.lastFailureTimestampSeconds());

		// call should retry
		serverOf(rec).enqueue((new MockResponse()).setResponseCode(502));

		now = Time.nowUptimeSeconds();
		test = new TestCallCase(null, "void_retry_502", 128, 128, false);
		call = sendCallRequest(test);
		assertCallRequest(rec, test);

		/*
		 * call should have moved to another host (if available), we wait because record failure
		 * timestamps are second based.
		 */
		SystemClock.sleep(1000);
		assertTrue(rec.lastFailureTimestampSeconds() >= now);
		assertTrue(client.activeCalls.contains(call));

		rec = client.servicesHostRecords.get(SERVICE).next();
		sendCallResponse(rec, test);

		assertCallRequest(rec, test);
		assertCallResponse(call, test);
		assertTrue(call.isDone());
		assertSame(rec, call.hostRecord);
		assertEquals(0, rec.lastFailureTimestampSeconds());
		SystemClock.sleep(15);
		assertFalse(client.activeCalls.contains(call));

		// call should retry only a fixed number of times before bailing
		test = new TestCallCase(null, "void_retry_502_max", 128, 128, false);
		call = sendCallRequest(test);
		for (int i = 0; i <= RpcModule.MAX_CALL_RETRY_COUNT; i++) {
			now = Time.nowUptimeSeconds();
			assertSame(rec, call.hostRecord);
			assertFalse(call.isDone());
			assertTrue(client.activeCalls.contains(call));

			serverOf(rec).enqueue((new MockResponse()).setResponseCode(502));
			assertCallRequest(rec, test);

			SystemClock.sleep(1000);
			assertTrue(rec.lastFailureTimestampSeconds() >= now);
			rec = client.servicesHostRecords.get(SERVICE).next();
		}
		cause = assertThrows(ExecutionException.class, call::get);
		assertTrue(cause.getCause() instanceof HttpRetryException);
		assertEquals(502, ((HttpRetryException) cause.getCause()).responseCode());
		assertTrue(call.isDone());
		SystemClock.sleep(15);
		assertFalse(client.activeCalls.contains(call));
	}

	@Test
	public void testCallTimeout() throws Exception {
		TestCallCase test = new TestCallCase("1.0", "slow_response", 128, 4096, false, 5000L);
		RpcModule.CallRequest call = sendCallRequest(test);
		RpcHostRecord rec = call.hostRecord;

		if (rec == null) {
			awaitProcessDone();

			rec = call.hostRecord;
			assertNotNull(rec);
		}

		serverOf(rec).enqueue(httpResponseOf(test).setBodyDelay(800, TimeUnit.MILLISECONDS));
		assertTrue(client.activeCalls.contains(call));
		assertCallRequest(rec, test);
		assertCallResponse(call, test);
		assertTrue(call.isDone());
		assertSame(rec, call.hostRecord);
		assertEquals(0, rec.lastFailureTimestampSeconds());
		SystemClock.sleep(15);
		assertFalse(client.activeCalls.contains(call));

		// now enforce a timeout
		test = new TestCallCase("1.0", "timeout_response", 128, 4096, false, 1000);
		call = sendCallRequest(test);

		assertSame(rec, call.hostRecord);
		serverOf(rec).enqueue(httpResponseOf(test).setBodyDelay(1015, TimeUnit.MILLISECONDS));
		assertCallRequest(rec, test);
		assertTrue(client.activeCalls.contains(call));
		assertFalse(call.isDone());

		assertTrue(
			assertThrows(ExecutionException.class, call::get).getCause() instanceof
			TimeoutException
		);
		assertTrue(call.isDone());
		assertSame(rec, call.hostRecord);
		assertEquals(0, rec.lastFailureTimestampSeconds());
		SystemClock.sleep(50);
		assertFalse(client.activeCalls.contains(call));
	}
}
