// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.antifraud;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.polygamma.android.origin.antifraud.EntropyMachine.*;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.polygamma.android.origin.protobuf.ProtobufField;
import org.polygamma.android.origin.protobuf.ProtobufWriter;
import org.polygamma.android.origin.util.Time;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

/**
 * {@link EntropyMachine} tests.
 */
@RunWith(AndroidJUnit4.class)
public class EntropyMachineTest extends TestWithModule {

	@BeforeClass
	public static void sendInitialCheckResponse() {
		long rechk = Time.nowRealtimeSeconds() + TimeUnit.HOURS.toSeconds(1);

		pollRequest();
		pushResponse(new CheckResult(
			rechk,
			new AntifraudStatus("", AntifraudStatus.RatingUnknown, 0),
			null
		));
	}

	@Test
	public void testGenerate() {
		assertTrue(generate(module, ByteBuffer.allocate(0)).isEmpty());

		ProtobufWriter ops = new ProtobufWriter();

		ops.writeString(ProtobufField.ofString(Push), "test-1");
		ops.writeString(ProtobufField.ofString(Push), "test-2");
		ops.writeInt32(ProtobufField.ofInt32(Push), 123);
		ops.writeBool(ProtobufField.ofBool(Pop), true);
		ops.writeBool(ProtobufField.ofBool(Pop), true);
		ops.writeBool(ProtobufField.ofBool(PopDiscard), true);

		ops.writeBool(ProtobufField.ofBool(Push), true);
		ops.writeString(ProtobufField.ofString(Write), "Z");
		ops.writeString(ProtobufField.ofString(Read), "Z");
		ops.writeBool(ProtobufField.ofBool(Pop), true);

		ops.writeInt32(ProtobufField.ofInt32(Push), 456);
		ops.writeString(ProtobufField.ofString(Write), "I");
		ops.writeString(ProtobufField.ofString(Read), "I");
		ops.writeBool(ProtobufField.ofBool(Pop), true);

		ops.writeInt64(ProtobufField.ofInt64(Push), 789);
		ops.writeString(ProtobufField.ofString(Write), "J");
		ops.writeString(ProtobufField.ofString(Read), "J");
		ops.writeBool(ProtobufField.ofBool(Pop), true);

		ops.writeString(ProtobufField.ofString(Push), "test-3");
		ops.writeString(ProtobufField.ofString(Write), "L");
		ops.writeString(ProtobufField.ofString(Read), "L");
		ops.writeBool(ProtobufField.ofBool(Pop), true);

		ops.writeInt64(ProtobufField.ofInt64(Push), 123456789);
		ops.writeInt32(ProtobufField.ofInt32(Push), 987654321);
		ops.writeString(ProtobufField.ofString(Call), "IJ(IJ)I");
		ops.writeBool(ProtobufField.ofBool(Pop), true);

		Iterator<Object> res = generate(module, ops.finish()).iterator();

		assertEquals(123L, res.next());
		assertEquals("test-2", new String((byte[]) res.next()));
		assertTrue(Z);
		assertTrue((Boolean) res.next());
		assertEquals(456, I);
		assertEquals(456, res.next());
		assertEquals(789, J);
		assertEquals(789L, res.next());
		assertEquals("test-3", new String((byte[]) L));
		assertEquals("test-3", new String((byte[]) res.next()));
		assertEquals(IJ(987654321, 123456789), res.next());
		assertFalse(res.hasNext());
	}
}
