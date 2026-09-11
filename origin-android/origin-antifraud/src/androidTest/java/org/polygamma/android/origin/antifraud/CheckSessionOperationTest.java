// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.antifraud;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.polygamma.android.origin.antifraud.CheckWire.SessionBegin;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.protobuf.InvalidProtocolBufferException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.polygamma.android.origin.crypt.Xtea;
import org.polygamma.android.origin.protobuf.ProtobufDecoder;
import org.polygamma.android.origin.protobuf.ProtobufEncoder;
import org.polygamma.origin.antifraud.IvtCheck;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;
import java.util.UUID;

/**
 * {@link CheckSessionOperation} tests.
 */
@RunWith(AndroidJUnit4.class)
public class CheckSessionOperationTest {
	@Test
	public void testOfPayload() {
		Random rand = new Random(44);

		assertThrows(
			IndexOutOfBoundsException.class,
			() -> CheckSessionOperation.ofPayloadArray(SessionBegin, new byte[32], 1, 32, 0)
		);
		assertThrows(
			IndexOutOfBoundsException.class,
			() -> CheckSessionOperation.ofPayloadArray(SessionBegin, new byte[32], 0, 32, 33)
		);
		for (int i = 0; i < 10000; i++) {
			int len = rand.nextInt(5000);
			int numBlockRem = len % Xtea.BLOCK_SIZE;
			int pad = 0;

			if (numBlockRem != 0) {
				pad = Xtea.BLOCK_SIZE - numBlockRem;
				len += pad;
			}
			assertEquals(0, len % 8);

			byte[] payload = new byte[3 + len];

			rand.nextBytes(payload);
			for (CheckSessionOperation op : new CheckSessionOperation[] {
				CheckSessionOperation.ofPayloadArray(SessionBegin, payload, 3, len, pad),
				CheckSessionOperation.ofPayloadBuffer(
					SessionBegin,
					ByteBuffer.wrap(payload, 3, len),
					pad
				)
			}) {
				assertFalse(op.isError());
				assertSame(payload, op.payload());
				assertThrows(IllegalStateException.class, op::error);

				assertEquals(SessionBegin, op.code);
				assertEquals(3, op.payloadOffset);
				assertEquals(len, op.payloadLength);
				assertEquals(pad, op.payloadPadSize);
				assertNull(op.next);
			}

			ByteBuffer directPayload = ByteBuffer.allocateDirect(len);

			directPayload.put(payload, 3, len);
			directPayload.flip();

			CheckSessionOperation op =
				CheckSessionOperation.ofPayloadBuffer(SessionBegin, directPayload, pad);

			assertFalse(op.isError());
			assertArrayEquals(Arrays.copyOfRange(payload, 3, payload.length), Arrays.copyOfRange(
				op.payload(),
				op.payloadOffset,
				op.payloadOffset + op.payloadLength
			));
			assertThrows(IllegalStateException.class, op::error);

			assertEquals(SessionBegin, op.code);
			assertEquals(len, op.payloadLength);
			assertEquals(pad, op.payloadPadSize);
			assertNull(op.next);
		}
	}

	@Test
	public void testOfError() {
		CheckSessionOperation op = CheckSessionOperation.ofError(SessionBegin, "error");

		assertTrue(op.isError());
		assertEquals("error", op.error());
		assertThrows(IllegalStateException.class, op::payload);

		assertEquals(SessionBegin, op.code);
		assertEquals(0, op.payloadOffset);
		assertEquals(0, op.payloadLength);
		assertEquals(0, op.payloadPadSize);
		assertNull(op.next);

		Throwable cause = (new RuntimeException())
			.fillInStackTrace();

		op = CheckSessionOperation.ofError(SessionBegin, cause);

		assertTrue(op.isError());
		assertEquals(EntropyCoding.throwableToString(cause), op.error());
		assertThrows(IllegalStateException.class, op::payload);

		assertEquals(SessionBegin, op.code);
		assertEquals(0, op.payloadOffset);
		assertEquals(0, op.payloadLength);
		assertEquals(0, op.payloadPadSize);
		assertNull(op.next);
	}

	@Test
	public void testSerde() throws InvalidProtocolBufferException {
		Random rand = new Random(44);
		ProtobufEncoder enc = ProtobufEncoder.of();

		for (int i = 0; i < 10000; i++) {
			int code = 1 + rand.nextInt(100);
			CheckSessionOperation exp;

			if (rand.nextBoolean()) {
				int len = rand.nextInt(5000);
				int pad = ((len + 7) & -7) - len;

				len += pad;
				exp = CheckSessionOperation.ofPayloadArray(code, new byte[len], pad);
				rand.nextBytes(exp.payload());
			} else if (rand.nextBoolean()) {
				exp = CheckSessionOperation.ofError(code, UUID.randomUUID().toString());
			} else {
				exp = CheckSessionOperation.ofError(
					code,
					(new RuntimeException())
						.fillInStackTrace()
				);
			}

			exp.toProtobuf(enc.reset());

			byte[] gotEnc = IvtCheck.SessionOperation.parseFrom(enc.asBuffer())
				.toByteArray();
			CheckSessionOperation got =
				CheckSessionOperation.ofProtobuf(ProtobufDecoder.of(gotEnc));

			assertNull(got.next);
			assertEquals(exp.code, got.code);
			assertEquals(exp.payloadLength, got.payloadLength);
			assertEquals(exp.payloadPadSize, got.payloadPadSize);
			if (exp.isError()) {
				assertTrue(got.isError());
				assertEquals(exp.error(), got.error());
			} else {
				assertFalse(got.isError());
				assertArrayEquals(exp.payload(), Arrays.copyOfRange(
					got.payload(),
					got.payloadOffset,
					got.payloadOffset + got.payloadLength
				));
			}
		}
	}
}
