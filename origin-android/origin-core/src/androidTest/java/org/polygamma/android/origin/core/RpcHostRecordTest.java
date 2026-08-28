// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.os.Build;
import android.os.SystemClock;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.polygamma.android.origin.protobuf.ProtobufReader;
import org.polygamma.android.origin.protobuf.ProtobufWriter;
import org.polygamma.android.origin.util.Time;

import java.util.Collection;
import java.util.concurrent.Executor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@link RpcHostRecord} tests.
 */
@RunWith(AndroidJUnit4.class)
public class RpcHostRecordTest {

	@Test
	public void testOfProto() {
		ProtobufWriter writer = new ProtobufWriter();

		writer.writeFixed64(RpcHostRecord.EXPTS, 123);
		writer.writeInt32(RpcHostRecord.PRIO, 456);
		writer.writeString(RpcHostRecord.HOST, "local.host");
		writer.writeInt32(RpcHostRecord.PORT, 8443);

		RpcHostRecord got = RpcHostRecord.ofProtobuf(new ProtobufReader(writer.finish()));

		assertEquals(123, got.expiryTimestampSeconds);
		assertTrue(got.isExpired());
		assertEquals(456, got.priority);
		assertEquals("local.host", got.host);
		assertEquals(8443, got.port);
	}

	@Test
	public void testOfQuery() {
		long now = Time.nowUtcSeconds();
		Executor exec = Runnable::run;

		assertTrue(RpcHostRecord.ofQuery("nonexistant.domain", exec, 5000).isEmpty());

		Collection<RpcHostRecord> got =
			RpcHostRecord.ofQuery("oghdrtest.pgoriginad.com", exec, 5000);

		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
			assertTrue(got.isEmpty());
		} else {
			Pattern pat = Pattern.compile("^oghdrtest-(\\d\\d)\\.pgoriginad\\.com$");

			assertFalse(got.isEmpty());
			for (RpcHostRecord rec : got) {
				assertTrue(rec.priority > 0);
				assertTrue(rec.expiryTimestampSeconds > now);
				assertFalse(rec.isExpired());
				assertEquals(Long.MAX_VALUE, rec.lastFailureDurationSeconds());
				assertFalse(rec.host.isEmpty());

				Matcher match = pat.matcher(rec.host);

				assertTrue(match.matches());
				assertEquals(8080 + Integer.parseInt(match.group(1), 10), rec.port);
			}
		}
	}

	@Test
	public void testIsExpired() {
		long now = Time.nowUtcSeconds();
		RpcHostRecord rec = new RpcHostRecord(now + 1, 1, "test.com", 0);

		assertFalse(rec.isExpired());
		SystemClock.sleep(1100);
		assertTrue(rec.isExpired());
	}

	@Test
	public void testLastFailure() {
		RpcHostRecord rec = RpcHostRecord.ofHost("test.com", 0);

		assertEquals(Long.MAX_VALUE, rec.lastFailureDurationSeconds());
		rec.updateLastFailureTimestamp();

		long dur = rec.lastFailureDurationSeconds();

		assertTrue(dur >= 0 && dur <= 1);

		SystemClock.sleep(1100);

		dur = rec.lastFailureDurationSeconds();
		assertTrue(String.format("invalid duration: %s", dur), dur >= 1 && dur <= 2);
	}
}
