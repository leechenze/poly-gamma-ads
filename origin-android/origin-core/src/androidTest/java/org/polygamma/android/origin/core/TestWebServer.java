// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.core;

import static org.junit.Assert.assertTrue;

import android.util.Log;

import androidx.annotation.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

/**
 * Test HTTP web server.
 */
public final class TestWebServer implements Closeable {

	private static final class DispatcherImpl extends Dispatcher {

		private static final long EXCHANGE_TIMEOUT_MILLIS =
			TimeUnit.SECONDS.toMillis(30);

		final SynchronousQueue<Object> exchange;
		final Lock lock;
		@Nullable Throwable lastError;

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
				Log.w(TestWebServer.class.getSimpleName(), "dispatch failed", err);
				this.lastError = err;
				return (new MockResponse())
					.setResponseCode(500);
			} finally {
				this.lock.unlock();
			}
		}
	}

	private final String host;
	private final int port;
	private final MockWebServer server;
	private final DispatcherImpl dispatcher;

	/**
	 * Construct new server.
	 */
	TestWebServer(String host, int port) {
		this.server = new MockWebServer();
		this.dispatcher = new DispatcherImpl();

		try {
			this.server.start(InetAddress.getByName(host), port);
			this.server.setDispatcher(this.dispatcher);
		} catch (Exception err) {
			throw new IllegalStateException("failed to start server", err);
		}
		this.host = host;
		this.port = this.server.getPort();
	}

	/**
	 * Retrieve server hostname.
	 *
	 * @return hostname
	 */
	public String host() {
		return this.host;
	}

	/**
	 * Retrieve port on which server accepts connections.
	 *
	 * @return port
	 */
	public int port() {
		return this.port;
	}

	private void checkError() {
		Throwable err = this.dispatcher.lastError;

		if (err != null)
			throw new AssertionError("dispatch error", err);
	}

	/**
	 * Poll next request.
	 *
	 * @return next request
	 */
	public RecordedRequest pollRequest() {
		RecordedRequest req;

		this.checkError();
		try {
			req = this.dispatcher.poll(RecordedRequest.class);
		} catch (InterruptedException err) {
			throw new AssertionError(err);
		}
		this.checkError();
		return req;
	}

	/**
	 * Respond to last {@linkplain #pollRequest() polled} request.
	 *
	 * @param resp response to send
	 */
	public void pushResponse(MockResponse resp) {
		this.checkError();
		try {
			this.dispatcher.push(resp);
		} catch (InterruptedException err) {
			throw new AssertionError(err);
		}
	}

	@Override
	public void close() throws IOException {
		this.server.close();
	}
}
