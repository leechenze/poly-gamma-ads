// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.antifraud;

import static org.junit.Assert.assertEquals;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.protobuf.InvalidProtocolBufferException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.polygamma.android.origin.protobuf.ProtobufDecoder;
import org.polygamma.android.origin.protobuf.ProtobufEncoder;
import org.polygamma.origin.antifraud.IvtCheck;

import java.util.Random;

/**
 * {@link CheckSessionResult} tests.
 */
@RunWith(AndroidJUnit4.class)
public class CheckSessionResultTest {
	@Test
	public void testNextCheckDelaySeconds() {
		CheckSessionResult res = new CheckSessionResult(15, new AntifraudStatus(null, 0, 0));

		assertEquals(15, res.nextCheckDelaySeconds(), 1);
	}

	@Test
	public void testWithNextCheckDelaySeconds() {
		CheckSessionResult res = new CheckSessionResult(15, new AntifraudStatus(null, 0, 0));

		assertEquals(30, res.withNextCheckDelaySeconds(30).nextCheckDelaySeconds(), 1);
	}

	@Test
	public void testSerde() throws InvalidProtocolBufferException {
		Random rand = new Random(44);
		ProtobufEncoder enc = ProtobufEncoder.of();

		for (int i = 0; i < 10000; i++) {
			int delay = rand.nextInt(100);
			CheckSessionResult exp = new CheckSessionResult(delay, new AntifraudStatus(
				new byte[rand.nextInt(32)],
				rand.nextInt(3),
				rand.nextInt(100)
			));

			if (exp.status.digest != null)
				rand.nextBytes(exp.status.digest);

			exp.toProtobuf(enc.reset());

			byte[] gotEnc = IvtCheck.SessionResult.parseFrom(enc.asBuffer())
				.toByteArray();
			CheckSessionResult got = CheckSessionResult.ofProtobuf(ProtobufDecoder.of(gotEnc));

			assertEquals(exp.status, got.status);
			assertEquals(exp.recheckTimestampSeconds, got.recheckTimestampSeconds, 1);
		}
	}
}
