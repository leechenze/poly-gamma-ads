// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.net.http.HttpException;
import android.net.http.UrlRequest;
import android.net.http.UrlResponseInfo;
import android.os.Build;
import android.os.SystemClock;
import android.os.ext.SdkExtensions;

import androidx.annotation.Nullable;
import androidx.core.util.Supplier;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.polygamma.android.origin.util.Strings;

import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;

/**
 * {@link HttpModule} tests.
 */
@RunWith(AndroidJUnit4.class)
public class HttpModuleTest extends TestWithSdk {

	/**
	 * Test request lifecycle event.
	 */
	private static final class TestLifecycleEvent {
		// request event is for
		final HttpRequest request;
		// event name
		final String name;
		// event arguments
		final Object[] arguments;
		// `true` if, and only if, request was done when event was fired
		final boolean done;

		TestLifecycleEvent(HttpRequest req, String name, Object... args) {
			this.request = req;
			this.name = name;
			this.arguments = args;
			this.done = req.isDone();
		}

		@Override
		public String toString() {
			return String.format(
				"TestLifecycleEvent{request=%s,name=%s,arguments=%s,done=%s}",
				this.request,
				this.name,
				Arrays.toString(this.arguments),
				this.done
			);
		}
	}

	/**
	 * Test request lifecycle {@linkplain TestLifecycleEvent event} listener.
	 */
	private static class TestLifecycleEventListener implements HttpRequest.Listener {
		final BlockingQueue<TestLifecycleEvent> events;

		TestLifecycleEventListener() {
			this.events = new LinkedBlockingQueue<>();
		}

		private void pushEvent(HttpRequest req, String what, Object... args) {
			this.events.add(new TestLifecycleEvent(req, what, args));
		}

		private void done(HttpRequest req) {
			TestExchange xchg = (TestExchange) req.attachment();

			if (xchg != null)
				xchg.receivedResponse.countDown();
		}

		@Override
		public void onFailed(HttpRequest req, Throwable cause, boolean recoverable) {
			this.pushEvent(req, "onFailed", cause, recoverable);
			this.done(req);
		}

		@Override
		public void onCancel(HttpRequest req) {
			this.pushEvent(req, "onCancel");
			this.done(req);
		}

		@Override
		public boolean onRedirect(
			HttpRequest req,
			URL url,
			int statusCode,
			String statusMsg,
			Supplier<Map<String, List<String>>> resolveHdrs
		) {
			this.pushEvent(
				req,
				"onRedirect",
				url,
				statusCode,
				statusMsg,
				new HashMap<>(resolveHdrs.get())
			);
			return true;
		}

		@Override
		public void onResponseStart(
			HttpRequest req,
			int statusCode,
			String statusMsg,
			Supplier<Map<String, List<String>>> resolveHdrs
		) {
			this.pushEvent(
				req,
				"onResponseStart",
				statusCode,
				statusMsg,
				new HashMap<>(resolveHdrs.get())
			);
		}

		@Override
		public void onResponseBodyPart(HttpRequest req, ByteBuffer body, boolean last) {
			this.pushEvent(
				req,
				"onResponseBodyPart",
				ByteBuffer.allocate(body.remaining())
					.put(body)
					.flip(),
				last
			);
		}

		@Override
		public void onSuccess(HttpRequest req) {
			this.pushEvent(req, "onSuccess");
			this.done(req);
		}
	}

	/**
	 * Test HTTP exchange.
	 */
	private static final class TestExchange {
		// request method
		final @HttpMethod String method;
		// path being requested
		final String path;
		// expected request body, if any
		final @Nullable ByteBuffer requestBody;
		// expected response body, if any
		final @Nullable ByteBuffer responseBody;
		// number of times request will be redirected before it is responded to with >399
		final int redirectCount;
		// status code of final response
		final int responseStatusCode;
		// duration, in milliseconds, before response body is sent
		long responseBodyDelayMillis;
		// rate, in bytes per second, to write body at
		long responseBodyBytesPerSecond;
		// `true` or `false` if response must be non-cached or may be cached
		boolean disableCache;

		// latch unlocked when request is received by server
		final CountDownLatch receivedRequest;
		// latch unlocked when response is received by client
		final CountDownLatch receivedResponse;

		// client-side request
		@Nullable HttpRequest request;
		// client-side request lifecycle event listener
		@Nullable TestLifecycleEventListener requestListener;

		// method received by server
		@Nullable String receivedRequestMethod;
		// headers received by server
		@Nullable Map<String, List<String>> receivedRequestHeaders;
		// body received by server
		@Nullable ByteBuffer receivedRequestBody;

		TestExchange(
			int resStatusCode,
			@HttpMethod String meth,
			String path,
			@Nullable ByteBuffer reqBody,
			@Nullable ByteBuffer resBody,
			int redirCount
		) {
			this.method = meth;
			this.path = path;
			this.requestBody = reqBody;
			this.responseBody = resBody;
			this.redirectCount = redirCount;
			this.responseStatusCode = resStatusCode;
			this.receivedRequest = new CountDownLatch(1);
			this.receivedResponse = new CountDownLatch(1);
		}

		TestExchange throttleResponseBody(long bps) {
			this.responseBodyBytesPerSecond = bps;
			return this;
		}

		TestExchange responseBodyDelay(long delay, TimeUnit unit) {
			this.responseBodyDelayMillis = unit.toMillis(delay);
			return this;
		}

		TestExchange disableCache() {
			this.disableCache = true;
			return this;
		}
	}

	private static final class TestResponseDispatcher extends Dispatcher {
		private final ConcurrentHashMap<String, TestExchange> pendingExchanges;
		final LinkedBlockingQueue<Throwable> errors;

		TestResponseDispatcher() {
			this.pendingExchanges = new ConcurrentHashMap<>();
			this.errors = new LinkedBlockingQueue<>();
		}

		TestExchange putExchange(TestExchange xchg) {
			assertNull(this.pendingExchanges.put(xchg.path, xchg));
			return xchg;
		}

		@Override
		public MockResponse dispatch(RecordedRequest req) {
			String path = Strings.nullToEmpty(req.getPath());
			int redirIdx = path.indexOf("/__redir__");
			int redirCount = 0;
			TestExchange xchg = null;

			if (redirIdx != -1) {
				redirCount = Integer.parseInt(path.substring(redirIdx + "/__redir__".length()));
				path = path.substring(0, redirIdx);
			}

			try {
				MockResponse res = new MockResponse();

				xchg = this.pendingExchanges.get(path);
				if (xchg.redirectCount != redirCount) {
					String loc = path + "/__redir__" + (redirCount + 1);

					return res.setResponseCode(307)
						.setHeader(
							"Location",
							(redirCount % 2) == 0 ? loc :
							req.getRequestUrl().resolve(loc).toString()
						);
				}
				xchg.receivedRequestMethod = req.getMethod();
				xchg.receivedRequestHeaders = req.getHeaders().toMultimap();
				if (req.getBodySize() > 0) {
					xchg.receivedRequestBody =
						ByteBuffer.wrap(req.getBody().readByteArray());
				}
				if (xchg.responseBody != null) {
					Buffer buff = new Buffer();

					buff.write(xchg.responseBody.duplicate());
					res.setBody(buff)
						.setHeader("Content-Type", "application/octet-stream");
					if (xchg.method.equals("GET")) {
						res.setHeader(
							"Cache-Control",
							xchg.disableCache ? "no-store" : "max-age=800"
						);
					}
					if (xchg.responseBodyBytesPerSecond != 0)
						res.throttleBody(xchg.responseBodyBytesPerSecond, 1, TimeUnit.SECONDS);
					if (xchg.responseBodyDelayMillis != 0)
						res.setBodyDelay(xchg.responseBodyDelayMillis, TimeUnit.MILLISECONDS);
				}
				xchg.receivedRequest.countDown();
				this.pendingExchanges.remove(path);
				return res.setResponseCode(xchg.responseStatusCode)
					.addHeader("test-response-header", "1");
			} catch (Exception cause) {
				this.errors.add(cause);
				if (xchg != null) {
					xchg.receivedRequest.countDown();
					this.pendingExchanges.remove(path);
				}
				return (new MockResponse()).setResponseCode(500);
			}
		}
	}

	private static MockWebServer server;
	private static  HttpModule client;
	private static String baseUrl;

	private static void setupServer() throws Exception {
		server = new MockWebServer();
		server.start();
		server.setDispatcher(new TestResponseDispatcher());
		baseUrl = "http://" + server.getHostName() + ':' + server.getPort();
	}

	@BeforeClass
	public static void setupHttp() throws Exception {
		setupServer();
		client = sdk.loadModule(HttpModule.class);
	}

	@AfterClass
	public static void destroyHttp() throws Exception {
		if (server != null) {
			server.close();
			assertEquals(Collections.emptyList(), new ArrayList<>(responses().errors));
		}
		if (client != null) {
			TestWithSdk.destroySdk();
			assertTrue(client.isDestroyed());
		}
	}

	/**
	 * Server-side response dispatcher.
	 *
	 * @return response dispatcher
	 */
	private static TestResponseDispatcher responses() {
		assertNotNull(server);
		return (TestResponseDispatcher) server.getDispatcher();
	}

	/**
	 * Construct random body of a size.
	 *
	 * @param sizeBytes size, in bytes, of body to construct
	 * @return resulting body or, {@code null} if {@code size} is less than {@code 1}
	 */
	private static @Nullable ByteBuffer randomBodyOf(int sizeBytes) {
		if (sizeBytes <= 0)
			return null;

		Random rand = new Random();
		ByteBuffer rv = ByteBuffer.allocate(sizeBytes);
		ByteBuffer word = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);

		while (rv.hasRemaining()) {
			word.clear();
			word.putLong(0, rand.nextLong())
				.limit(Math.min(8, rv.remaining()));
			rv.put(word);
			word.clear();
		}
		rv.flip();
		return rv;
	}

	/**
	 * Construct a new HTTP request and response exchange.
	 *
	 * @param resStatusCode status code response will send
	 * @param meth method request will be made with
	 * @param path path to request
	 * @param reqBodySizeBytes request body size, or {@code 0} if no body
	 * @param resBodySizeBytes response body size, or {@code 0} if no body
	 * @param redirCount number of times server will redirect request before responding with final
	 * response
	 * @return resulting exchange
	 */
	private static TestExchange newExchange(
		int resStatusCode,
		@HttpMethod String meth,
		String path,
		int reqBodySizeBytes,
		int resBodySizeBytes,
		int redirCount
	) {
		return responses().putExchange(new TestExchange(
			resStatusCode,
			meth,
			path,
			randomBodyOf(reqBodySizeBytes),
			randomBodyOf(resBodySizeBytes),
			redirCount
		));
	}

	/**
	 * Construct a new HTTP request and response exchange without redirects.
	 *
	 * @param resStatusCode status code response will send
	 * @param meth method request will be made with
	 * @param path path to request
	 * @param reqBodySizeBytes request body size, or {@code 0} if no body
	 * @param resBodySizeBytes response body size, or {@code 0} if no body
	 * @return resulting exchange
	 */
	private static TestExchange newExchange(
		int resStatusCode,
		@HttpMethod String meth,
		String path,
		int reqBodySizeBytes,
		int resBodySizeBytes
	) {
		return newExchange(resStatusCode, meth, path, reqBodySizeBytes, resBodySizeBytes, 0);
	}

	/**
	 * Send request side of exchange.
	 *
	 * @param xchg exchange to send request side of
	 * @param prio priority to send request at
	 */
	static void sendRequest(TestExchange xchg, @HttpRequest.Priority int prio) {
		TestLifecycleEventListener listener = new TestLifecycleEventListener();

		assertNotNull(client);
		assertFalse(client.isDestroyed());

		HttpRequest.Builder req =
			client.newRequestBuilder(baseUrl + xchg.path, xchg.method, listener)
				.attachment(xchg)
				.priority(prio)
				.addHeader("test-request-header", "a");

		if (xchg.requestBody != null) {
			req.body(xchg.requestBody)
				.addHeader("Content-Type", "application/octet-stream");
		}
		if (xchg.method.equals("GET")) {
			req.addHeader(
				"Cache-Control",
				xchg.disableCache ? "no-cache,no-store" :
				"max-age=800,max-stale=3600"
			);
		}
		xchg.requestListener = listener;
		xchg.request = req.send();
	}

	/**
	 * Ensure request lifecycle is valid.
	 *
	 * @param listener listener to check lifecycle events of for validity
	 * @return terminating event
	 * @throws AssertionError lifecycle events are malformed
	 */
	private static TestLifecycleEvent assertLifecycleValid(TestLifecycleEventListener listener) {
		TestLifecycleEvent prev = null;

		for (TestLifecycleEvent curr : listener.events) {
			switch (curr.name) {
			case "onCancel":
			case "onFailed":
				assertTrue("invalid previous event: " + prev, prev == null || (
					!prev.name.equals("onCancel") &&
					!prev.name.equals("onFailed") &&
					!prev.name.equals("onSuccess")
				));
				break;
			case "onRedirect":
			case "onResponseStart":
				assertTrue(
					"expected redirect event or nothing: " + prev,
					prev == null || prev.name.equals("onRedirect")
				);
				break;
			case "onResponseBodyPart":
				assertTrue(
					"expected response start or non-last body part event: " + prev,
					prev != null && (prev.name.equals("onResponseStart") || (
						prev.name.equals("onResponseBodyPart") &&
						!((boolean) prev.arguments[1])
					))
				);
				break;
			default:
				assertEquals("expected success event: " + curr, "onSuccess", curr.name);
				assertTrue(
					"expected response start or last body part event: " + prev,
					prev != null && (prev.name.equals("onResponseStart") || (
						prev.name.equals("onResponseBodyPart") &&
							((boolean) prev.arguments[1])
					))
				);
				break;
			}
			prev = curr;
		}
		assertNotNull(prev);
		assertTrue("invalid done event: " + prev.name, prev.done && (
			"onCancel".equals(prev.name) ||
			"onFailed".equals(prev.name) ||
			"onSuccess".equals(prev.name)
		));
		return prev;
	}

	/**
	 * Ensure request part of exchange is valid.
	 *
	 * @param xchg exchange to test request part of
	 * @throws InterruptedException interrupted while waiting for server to receive request
	 * @throws AssertionError request side of exchange is malformed
	 */
	private static void assertRequestValid(TestExchange xchg) throws InterruptedException {
		assertTrue(xchg.receivedRequest.await(1, TimeUnit.MINUTES));
		assertEquals(xchg.method, xchg.receivedRequestMethod);
		assertNotNull(xchg.receivedRequestHeaders);
		assertEquals(
			Collections.singletonList("a"),
			xchg.receivedRequestHeaders.get("test-request-header")
		);

		if (xchg.requestBody != null) {
			assertEquals(
				Collections.singletonList("application/octet-stream"),
				xchg.receivedRequestHeaders.get("Content-Type")
			);
		}
		assertEquals(xchg.requestBody, xchg.receivedRequestBody);
	}

	/**
	 * Ensure response part of exchange is valid.
	 *
	 * @param xchg exchange to test response part of
	 * @throws InterruptedException interrupted while waiting for client to receive response
	 * @throws AssertionError response side of exchange is malformed
	 */
	private static void assertResponseValid(TestExchange xchg) throws InterruptedException {
		assertTrue(xchg.receivedResponse.await(1, TimeUnit.MINUTES));
		assertNotNull(xchg.request);
		assertNotNull(xchg.requestListener);
		assertEquals("onSuccess", assertLifecycleValid(xchg.requestListener).name);

		List<String> redirPaths = new ArrayList<>(xchg.redirectCount);
		@Nullable ByteBuffer resBody =
			xchg.responseBody == null ? null :
			ByteBuffer.allocate(xchg.responseBody.remaining());

		for (TestLifecycleEvent evt : xchg.requestListener.events) {
			assertSame(xchg.request, evt.request);
			assertSame(xchg, evt.request.attachment());
			if ("onRedirect".equals(evt.name)) {
				redirPaths.add(((URL) evt.arguments[0]).getPath());
				assertEquals(307, evt.arguments[1]);
			} else if ("onResponseBodyPart".equals(evt.name)) {
				ByteBuffer part = (ByteBuffer) evt.arguments[0];

				assertNotNull(resBody);
				assertTrue(resBody.remaining() >= part.remaining());
				resBody.put(part);
				if ((boolean) evt.arguments[1])
					resBody.flip();
			}
		}

		assertEquals(xchg.responseBody, resBody);
		assertEquals("unexpected redirects: " + redirPaths, xchg.redirectCount, redirPaths.size());
		for (int i = 0; i < redirPaths.size(); i++)
			assertEquals(xchg.path + "/__redir__" + (i + 1), redirPaths.get(i));
	}

	/**
	 * Ensure request and response sides of exchange are valid.
	 *
	 * @param xchg exchange to test sides of
	 * @throws InterruptedException interrupted while waiting for client or server to receive
	 * response or request, respectively
	 * @throws AssertionError response or request -side of exchange is malformed
	 */
	private static void assertExchangeValid(TestExchange xchg) throws InterruptedException {
		assertRequestValid(xchg);
		assertResponseValid(xchg);
	}

	@Test
	public void testNewRequestBuilderInvalid() {
		TestLifecycleEventListener listener = new TestLifecycleEventListener();

		assertEquals(
			"invalid request method",
			assertThrows(
				IllegalArgumentException.class,
				() -> client.newRequestBuilder("http://foo.com", "foo", listener)
			).getMessage()
		);
		assertEquals(
			"invalid request URL",
			assertThrows(
				IllegalArgumentException.class,
				() -> client.newRequestBuilder("1234", "GET", listener)
			).getMessage()
		);
		assertThrows(
			NullPointerException.class,
			() -> client.newRequestBuilder("http://foo.com", "GET", null)
		);
		assertTrue(listener.events.isEmpty());
	}

	@Test
	public void testRequestNetwork() throws Exception {
		Lock read = client.lock.readLock();
		TestExchange[] xchgs = {
			// GET without a body
			newExchange(204, "GET", "/get-no-body", 0, 0),
			// GET with a body
			newExchange(200, "GET", "/get-body", 0, 128),
			// GET with a slow body
			newExchange(200, "GET", "/get-body-slow", 0, 32768)
				.throttleResponseBody(28671),
			// GET with 1 redirect and a body
			newExchange(200, "GET", "/get-body-1-redirect", 0, 16384, 1),
			// GET with 5 redirect and a body
			newExchange(200, "GET", "/get-body-5-redirects", 0, 128, 5),

			// GET error without a body
			newExchange(404, "GET", "/get-error-no-body", 0, 0),
			// GET error with a body
			newExchange(400, "GET", "/get-error-body", 0, 128),

			// POST without a body
			newExchange(204, "POST", "/post-no-body", 0, 0),
			// POST with a request body and without a response body
			newExchange(204, "POST", "/post-no-request-body", 128, 0),
			// POST with a body
			newExchange(200, "POST", "/post-body", 128, 128),
			// POST with a slow body
			newExchange(200, "POST", "/post-body-slow", 32768, 32768)
				.throttleResponseBody(28671),
			// POST with 1 redirect and a body
			newExchange(200, "POST", "/post-body-1-redirect", 16384, 16384, 1),
			// POST with 5 redirect and a body
			newExchange(200, "POST", "/post-body-5-redirects", 128, 128, 5),

			// POST error without a body
			newExchange(404, "POST", "/post-error-no-body", 0, 0),
			// POST error with a body
			newExchange(400, "POST", "/post-error-body", 128, 128)
		};

		assertTrue(client.liveRequests.isEmpty());
		assertEquals(0, client.liveRequestCount.get());

		for (int i = 0; i < xchgs.length;) {
			// ensure we don't hit backlog
			int n = Math.min(client.resolveImpl().maxLiveRequestCount, xchgs.length - i);

			for (int j = 0; j < n; j++) {
				TestExchange xchg = xchgs[i + j].disableCache();

				sendRequest(xchg, HttpRequest.PRIORITY_NORMAL);
				read.lock();
				try {
					if (xchg.request.isDone()) {
						/*
						 * Request is already done: it shouldn't be in the live set. This usually
						 * wont happen unless the request errors out early.
						 */
						assertNull(client.liveRequests.get(xchg.request));
					} else if (client.state != HttpModule.STATE_INTERNET_INACCESSIBLE) {
						/*
						 * Internet is accessible: request should be in the live set, since we
						 * haven't exhausted the live request limit.
						 */
						assertSame(xchg.request, client.liveRequests.get(xchg.request));
						assertTrue(client.liveRequestCount.get() >= 1);
					} else {
						/*
						 * Internet isn't accessible: request is going to get backlogged and will
						 * be sent when internet connectivity is restored.
						 */
						assertNull(client.liveRequests.get(xchg.request));
						assertTrue(
							client.priorityBacklogs[HttpRequest.PRIORITY_NORMAL]
								.contains(xchg.request)
						);
						continue;
					}
					/*
					 * if we're here, request is done or is live, so nothing should be in the
					 * backlog
					 */
					for (Queue<HttpRequest> req : client.priorityBacklogs)
						assertTrue(req.isEmpty());
				} finally {
					read.unlock();
				}
			}
			for (int j = 0; j < n; j++) {
				TestExchange xchg = xchgs[i + j];

				assertExchangeValid(xchg);
				assertNull(client.liveRequests.get(xchg.request));
			}
			// requests are complete, we shouldn't have anything live *or* in the backlog
			assertTrue(client.liveRequests.isEmpty());
			assertEquals(0, client.liveRequestCount.get());
			for (Queue<HttpRequest> req : client.priorityBacklogs)
				assertTrue(req.isEmpty());
			i += n;
		}
	}

	@Test
	@SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP_MR1)
	public void testRequestCache() throws Exception {
		TestExchange xchg =
			newExchange(200, "GET", "/test-request-cache-" + UUID.randomUUID(), 0, 8192);

		// make the network request first to populate the cache
		sendRequest(xchg, HttpRequest.PRIORITY_NORMAL);
		assertExchangeValid(xchg);
		assertTrue(client.liveRequests.isEmpty());
		assertEquals(0, client.liveRequestCount.get());

		// now disable internet access, and make the request
		server.close();
		server = null;
		TestUtil.toggleInternetAccess(false);
		try {
			do {
				SystemClock.sleep(1000);
			} while (client.state != HttpModule.STATE_INTERNET_INACCESSIBLE);

			xchg = new TestExchange(200, "GET", xchg.path, null, xchg.responseBody, 0);
			sendRequest(xchg, HttpRequest.PRIORITY_NORMAL);
			assertResponseValid(xchg);
			assertTrue(client.liveRequests.isEmpty());
			assertEquals(0, client.liveRequestCount.get());

			// now send a non-cache request, it should get backlogged
			xchg = (new TestExchange(200, "GET", xchg.path, null, xchg.responseBody, 0))
				.disableCache();
			setupServer();
			responses().putExchange(xchg);
			sendRequest(xchg, HttpRequest.PRIORITY_NORMAL);

			client.lock.readLock().lock();
			try {
				assertFalse(xchg.request.isDone());
				assertNull(client.liveRequests.get(xchg.request));
				assertEquals(0, client.liveRequestCount.get());
				assertSame(
					xchg.request,
					client.priorityBacklogs[HttpRequest.PRIORITY_NORMAL].peek()
				);
			} finally {
				client.lock.readLock().unlock();
			}
			/*
			 * start up a new server on the same address as before so we can service the request,
			 * do this before we enable internet
			 */
			URL currBaseUrl = new URL(baseUrl);
		} finally {
			TestUtil.toggleInternetAccess(true);
		}

		// the pending exchange should start when connectivity updates
		assertExchangeValid(xchg);
		for (Queue<HttpRequest> req : client.priorityBacklogs)
			assertTrue(req.isEmpty());
		assertTrue(client.liveRequests.isEmpty());
		assertEquals(0, client.liveRequestCount.get());
	}

	@Test
	public void testRequestBacklog() throws Exception {
		TestExchange[] highPrio = new TestExchange[client.resolveImpl().maxLiveRequestCount];
		List<TestExchange> lowPrios = new ArrayList<>();

		for (int i = HttpRequest.PRIORITY_CRITICAL; i > HttpRequest.PRIORITY_IDLE; i--) {
			// fill live set with high priority requests up to the live request limit
			for (int j = 0; j < highPrio.length; j++) {
				highPrio[j] = newExchange(200, "GET", "/get-" + i + "-" + j, 0, 64)
					.disableCache()
					.responseBodyDelay(20, TimeUnit.MILLISECONDS);
			}
			// lower priority requests should go into their backlog
			for (int j = i - 1; j >= HttpRequest.PRIORITY_IDLE; j--) {
				lowPrios.add(
					newExchange(200, "GET", "/get-" + i + "-b" + j, 0, 64)
						.disableCache()
				);
			}

			/*
			 * we hold the write-side of the done lock to ensure live set and backlog isn't
			 * modified
			 */
			client.doneLock.writeLock().lock();
			try {
				for (int j = 0; j < highPrio.length; j++) {
					TestExchange xchg = highPrio[j];

					sendRequest(xchg, i);
					assertSame(xchg.request, client.liveRequests.get(xchg.request));
					assertEquals(j + 1, client.liveRequestCount.get());
					assertTrue(client.priorityBacklogs[i].isEmpty());
				}
				for (int j = 0; j < lowPrios.size(); j++) {
					TestExchange xchg = lowPrios.get(j);
					int prio = i - (j + 1);

					sendRequest(xchg, prio);
					assertNull(client.liveRequests.get(xchg.request));
					assertEquals(highPrio.length, client.liveRequestCount.get());
					assertTrue(client.priorityBacklogs[prio].contains(xchg.request));
				}
			} finally {
				client.doneLock.writeLock().unlock();
			}

			for (TestExchange xchg : highPrio)
				assertExchangeValid(xchg);
			for (TestExchange xchg : lowPrios)
				assertExchangeValid(xchg);
			for (Queue<HttpRequest> req : client.priorityBacklogs)
				assertTrue(req.isEmpty());
			SystemClock.sleep(50);
			assertTrue(client.liveRequests.isEmpty());
			assertEquals(0, client.liveRequestCount.get());
			lowPrios.clear();
		}
	}

	@Test
	public void testRequestCancel() throws Exception {
		TestExchange xchg = newExchange(200, "GET", "/get-cancel", 0, 64)
			.disableCache()
			.responseBodyDelay(1, TimeUnit.SECONDS);

		sendRequest(xchg, HttpRequest.PRIORITY_NORMAL);
		xchg.receivedRequest.await();
		assertTrue(xchg.request.cancel());
		assertTrue(client.liveRequests.isEmpty());
		assertEquals(0, client.liveRequestCount.get());
		assertEquals("onCancel", assertLifecycleValid(xchg.requestListener).name);
		assertFalse(xchg.request.cancel());
	}

	private void doTestDestroy() throws Exception {
		HttpModule.Impl impl = client.resolveImpl();
		TestExchange[] reqs = new TestExchange[impl.maxLiveRequestCount * 2];

		for (int i = 0; i < reqs.length; i++) {
			reqs[i] = newExchange(200, "GET", "/get-destroy-" + i, 0, 64)
				.disableCache()
				.responseBodyDelay(1, TimeUnit.SECONDS);
			sendRequest(reqs[i], HttpRequest.PRIORITY_NORMAL);
		}

		sdk.shutdown();
		assertTrue(sdk.awaitShutdown(1, TimeUnit.SECONDS));
		for (TestExchange req : reqs)
			assertLifecycleValid(req.requestListener);
		assertTrue(client.liveRequests.isEmpty());
		assertEquals(0, client.liveRequestCount.get());
		for (Queue<HttpRequest> q : client.priorityBacklogs)
			assertTrue(q.isEmpty());

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE || (
			Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
			SdkExtensions.getExtensionVersion(Build.VERSION_CODES.S) >= 7
		)) {
			assertTrue(impl instanceof HttpModuleCronetImpl);
			assertThrows(
				IllegalStateException.class,
				() -> ((HttpModuleCronetImpl) impl).cronet.newUrlRequestBuilder(
					"http://test.com",
					Runnable::run,
					new UrlRequest.Callback() {
						@Override
						public void onRedirectReceived(UrlRequest r, UrlResponseInfo i, String l) {
						}

						@Override
						public void onResponseStarted(UrlRequest r, UrlResponseInfo i) {

						}

						@Override
						public void onReadCompleted(UrlRequest r, UrlResponseInfo i, ByteBuffer b) {

						}

						@Override
						public void onSucceeded(UrlRequest r, UrlResponseInfo i) {

						}

						@Override
						public void onFailed(UrlRequest r, UrlResponseInfo i, HttpException e) {

						}

						@Override
						public void onCanceled(UrlRequest r, UrlResponseInfo i) {
						}
					}
				).build());
		} else {
			assertTrue(((HttpModuleJdkImpl) impl).threadRequestSenderTasks.isEmpty());
		}
	}

	@Test
	public void testDestroy() throws Exception {
		try {
			this.doTestDestroy();
		} finally {
			sdk = TestUtil.callOnMainSync(() -> Origin.initialize(TestUtil.context()));
			client = sdk.loadModule(HttpModule.class);
		}
	}
}
