// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.antifraud;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.util.Base64;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.polygamma.android.origin.core.Origin;
import org.polygamma.android.origin.core.RpcModule;
import org.polygamma.android.origin.protobuf.ProtobufReader;
import org.polygamma.android.origin.protobuf.ProtobufWriter;
import org.polygamma.android.origin.util.Flate;
import org.polygamma.android.origin.util.Futures;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
 * Initialize SDK and module before all tests and destroy after all tests.
 */
public class TestWithModule {

	private static final String TAG = TestWithModule.class.getSimpleName();

	private static final class DispatcherImpl extends Dispatcher {

		private static final long EXCHANGE_TIMEOUT_MILLIS =
			TimeUnit.SECONDS.toMillis(30);

		final SynchronousQueue<Object> exchange;
		final Lock lock;
		@Nullable
		Throwable lastError;

		DispatcherImpl() {
			this.exchange = new SynchronousQueue<>();
			this.lock = new ReentrantLock();
		}

		/**
		 * Poll from exchange queue.
		 *
		 * @param <T> value type
		 * @param type expected type
		 * @return polled value
		 * @throws InterruptedException poll was interrupted
		 */
		@SuppressWarnings("unchecked")
		<T> T poll(Class<T> type) throws InterruptedException {
			Object res = this.exchange.poll(EXCHANGE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);

			assertTrue(type.isInstance(res));
			return (T) res;
		}

		/**
		 * Push value into exchange queue.
		 *
		 * @param val value to push
		 * @throws InterruptedException push was interrupted
		 */
		void push(Object val) throws InterruptedException {
			assertTrue(this.exchange.offer(val, EXCHANGE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS));
		}

		@Override
		public MockResponse dispatch(RecordedRequest req) {
			this.lock.lock();
			try {
				this.push(req);
				return this.poll(MockResponse.class);
			} catch (Throwable err) {
				Log.w(TAG, "dispatch failed", err);
				this.lastError = err;
				return (new MockResponse())
					.setResponseCode(500);
			} finally {
				this.lock.unlock();
			}
		}
	}

	/**
	 * Current SDK instance.
	 */
	protected static Origin sdk;

	/**
	 * Current module instance.
	 */
	protected static AntifraudModule module;

	/**
	 * RPC web server.
	 */
	private static MockWebServer server;

	/**
	 * RPC dispatcher.
	 */
	private static DispatcherImpl dispatcher;

	@BeforeClass
	public static void setupModule() throws IOException {
		server = new MockWebServer();
		dispatcher = new DispatcherImpl();
		server.start();
		server.setDispatcher(dispatcher);

		String host = server.getHostName();
		int port = server.getPort();

		ListenableFuture<Boolean> fut =
			CallbackToFutureAdapter.getFuture(completer -> {
				InstrumentationRegistry.getInstrumentation()
					.runOnMainSync(() -> {
						try {
							sdk = Origin.initialize(
								InstrumentationRegistry.getInstrumentation()
									.getTargetContext()
									.getApplicationContext(),
								RpcModule.ofProvider()
									.host(host)
									.port(port)
									.insecure(true)
							);
							module = sdk.loadModule(AntifraudModule.ofProvider(true));
							completer.set(true);
						} catch (Throwable err) {
							completer.setException(err);
						}
					});
				return "load-sdk";
			});

		Futures.await(fut);
	}

	@AfterClass
	public static void destroyModule() throws InterruptedException, IOException {
		if (sdk != null) {
			sdk.shutdown();
			while (!sdk.awaitShutdown(10, TimeUnit.SECONDS))
				Log.w(TAG, "shutdown taking longer than 10 seconds...");
			assertTrue(sdk.isShutdown());
			sdk = null;
		}
		if (module != null) {
			assertTrue(module.destroyed);
			assertNull(module.checkFuture);
			assertNull(module.callCheckFuture);
			module = null;
		}
		if (server != null) {
			server.shutdown();
			server = null;
		}
	}

	private static void checkError() {
		Throwable err = dispatcher.lastError;

		if (err != null)
			throw new AssertionError("dispatch error", err);
	}

	/**
	 * Poll next check arguments.
	 *
	 * @return check arguments
	 */
	static TestCheckArguments pollRequest() {
		checkError();

		RecordedRequest http;

		try {
			http = dispatcher.poll(RecordedRequest.class);
		} catch (InterruptedException err) {
			throw new AssertionError(err);
		}
		checkError();

		String path = http.getPath();
		String meth = http.getMethod();

		assertNotNull(path);
		assertTrue(path.startsWith("/ivt/check"));

		byte[] body;

		if ("GET".equals(meth)) {
			String argB64 = path.substring("/ivt/check".length());

			if ("/".equals(argB64)) {
				body = new byte[0];
			} else {
				assertTrue(argB64.startsWith("/"));

				argB64 = argB64.substring(1);
				body = Base64.decode(
					argB64.getBytes(StandardCharsets.UTF_8),
					Base64.NO_PADDING | Base64.NO_WRAP | Base64.URL_SAFE
				);
			}
		} else {
			body = http.getBody().readByteArray();
		}
		try {
			return TestCheckArguments.ofProtobuf(new ProtobufReader(Flate.decompressZlib(body)));
		} catch (Throwable err) {
			throw new AssertionError(err);
		}
	}

	/**
	 * Respond to last {@linkplain #pollRequest() polled} check.
	 *
	 * @param res response to send
	 */
	static void pushResponse(@Nullable CheckResult res) {
		checkError();
		try {
			MockResponse resp = new MockResponse();

			if (res == null) {
				resp.setResponseCode(204);
			} else {
				Buffer body = new Buffer();

				body.write(ProtobufWriter.serialize(res));
				resp.setResponseCode(200)
					.setBody(body);
			}
			dispatcher.push(resp);
		} catch (IOException | InterruptedException err) {
			throw new AssertionError(err);
		}
	}

	protected TestWithModule() {
	}
}
