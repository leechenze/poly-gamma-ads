// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.antifraud;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.protobuf.InvalidProtocolBufferException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.polygamma.android.origin.core.Origin;
import org.polygamma.android.origin.protobuf.ProtobufDecoder;
import org.polygamma.android.origin.protobuf.ProtobufEncoder;
import org.polygamma.origin.antifraud.IvtCheck;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * {@link TamperMachine} tests.
 */
@RunWith(AndroidJUnit4.class)
public class TamperMachineTest {

	private static Origin sdk;

	@BeforeClass
	public static void setupSdk() {
		Context ctxt = InstrumentationRegistry.getInstrumentation()
			.getTargetContext()
			.getApplicationContext();

		sdk = Origin.initialize(ctxt);
	}

	@AfterClass
	public static void destroySdk() throws InterruptedException {
		if (sdk != null) {
			sdk.shutdown();
			while (!sdk.awaitShutdown(10, TimeUnit.SECONDS))
				Log.w(EntropyCodingTest.class.getSimpleName(), "sdk shutdown taking longer than 10 seconds");
			assertTrue(sdk.isShutdown());
		}
		sdk = null;
	}

	@Test
	public void testGenerate() throws InvalidProtocolBufferException {
		IvtCheck.TamperMachineRequest req = IvtCheck.TamperMachineRequest.newBuilder()
			.addOp(
				IvtCheck.TamperMachineOperation.newBuilder()
					.setCode(IvtCheck.TamperMachineOpcode.TamperMachinePush)
					.setStr("test-1")
					.build()
			)
			.addOp(
				IvtCheck.TamperMachineOperation.newBuilder()
					.setCode(IvtCheck.TamperMachineOpcode.TamperMachinePush)
					.setStr("test-2")
					.build()
			)
			.addOp(
				IvtCheck.TamperMachineOperation.newBuilder()
					.setCode(IvtCheck.TamperMachineOpcode.TamperMachinePush)
					.setU32(123)
					.build()
			)
			.addOp(
				IvtCheck.TamperMachineOperation.newBuilder()
					.setCode(IvtCheck.TamperMachineOpcode.TamperMachinePop)
					.build()
			)
			.addOp(
				IvtCheck.TamperMachineOperation.newBuilder()
					.setCode(IvtCheck.TamperMachineOpcode.TamperMachinePop)
					.build()
			)
			.addOp(
				IvtCheck.TamperMachineOperation.newBuilder()
					.setCode(IvtCheck.TamperMachineOpcode.TamperMachinePopDiscard)
					.build()
			)
			.addOp(
				IvtCheck.TamperMachineOperation.newBuilder()
					.setCode(IvtCheck.TamperMachineOpcode.TamperMachinePush)
					.setU32(1)
					.build()
			)
			.addOp(
				IvtCheck.TamperMachineOperation.newBuilder()
					.setCode(IvtCheck.TamperMachineOpcode.TamperMachineWrite)
					.setStr("Z")
					.build()
			)
			.addOp(
				IvtCheck.TamperMachineOperation.newBuilder()
					.setCode(IvtCheck.TamperMachineOpcode.TamperMachineRead)
					.setStr("Z")
					.build()
			)
			.addOp(
				IvtCheck.TamperMachineOperation.newBuilder()
					.setCode(IvtCheck.TamperMachineOpcode.TamperMachinePop)
					.build()
			)
			.addOp(
				IvtCheck.TamperMachineOperation.newBuilder()
					.setCode(IvtCheck.TamperMachineOpcode.TamperMachinePush)
					.setU32(456)
					.build()
			)
			.addOp(
				IvtCheck.TamperMachineOperation.newBuilder()
					.setCode(IvtCheck.TamperMachineOpcode.TamperMachineWrite)
					.setStr("I")
					.build()
			)
			.addOp(
				IvtCheck.TamperMachineOperation.newBuilder()
					.setCode(IvtCheck.TamperMachineOpcode.TamperMachineRead)
					.setStr("I")
					.build()
			)
			.addOp(
				IvtCheck.TamperMachineOperation.newBuilder()
					.setCode(IvtCheck.TamperMachineOpcode.TamperMachinePop)
					.build()
			)
			.addOp(
				IvtCheck.TamperMachineOperation.newBuilder()
					.setCode(IvtCheck.TamperMachineOpcode.TamperMachinePush)
					.setU64(789)
					.build()
			)
			.addOp(
				IvtCheck.TamperMachineOperation.newBuilder()
					.setCode(IvtCheck.TamperMachineOpcode.TamperMachineWrite)
					.setStr("J")
					.build()
			)
			.addOp(
				IvtCheck.TamperMachineOperation.newBuilder()
					.setCode(IvtCheck.TamperMachineOpcode.TamperMachineRead)
					.setStr("J")
					.build()
			)
			.addOp(
				IvtCheck.TamperMachineOperation.newBuilder()
					.setCode(IvtCheck.TamperMachineOpcode.TamperMachinePop)
					.build()
			)
			.addOp(
				IvtCheck.TamperMachineOperation.newBuilder()
					.setCode(IvtCheck.TamperMachineOpcode.TamperMachinePush)
					.setStr("test-3")
					.build()
			)
			.addOp(
				IvtCheck.TamperMachineOperation.newBuilder()
					.setCode(IvtCheck.TamperMachineOpcode.TamperMachineWrite)
					.setStr("L")
					.build()
			)
			.addOp(
				IvtCheck.TamperMachineOperation.newBuilder()
					.setCode(IvtCheck.TamperMachineOpcode.TamperMachineRead)
					.setStr("L")
					.build()
			)
			.addOp(
				IvtCheck.TamperMachineOperation.newBuilder()
					.setCode(IvtCheck.TamperMachineOpcode.TamperMachinePop)
					.build()
			)
			.addOp(
				IvtCheck.TamperMachineOperation.newBuilder()
					.setCode(IvtCheck.TamperMachineOpcode.TamperMachinePush)
					.setU64(123456789L)
					.build()
			)
			.addOp(
				IvtCheck.TamperMachineOperation.newBuilder()
					.setCode(IvtCheck.TamperMachineOpcode.TamperMachinePush)
					.setU32(987654321)
					.build()
			)
			.addOp(
				IvtCheck.TamperMachineOperation.newBuilder()
					.setCode(IvtCheck.TamperMachineOpcode.TamperMachineCall)
					.setStr("IJ(IJ)I")
					.build()
			)
			.addOp(
				IvtCheck.TamperMachineOperation.newBuilder()
					.setCode(IvtCheck.TamperMachineOpcode.TamperMachinePop)
					.build()
			)
			.addOp(
				IvtCheck.TamperMachineOperation.newBuilder()
					.setCode(IvtCheck.TamperMachineOpcode.TamperMachinePush)
					.setU64(32)
					.build()
			)
			.addOp(
				IvtCheck.TamperMachineOperation.newBuilder()
					.setCode(IvtCheck.TamperMachineOpcode.TamperMachinePushApp)
					.build()
			)
			.addOp(
				IvtCheck.TamperMachineOperation.newBuilder()
					.setCode(IvtCheck.TamperMachineOpcode.TamperMachinePushBackAsync)
					.build()
			)
			.addOp(
				IvtCheck.TamperMachineOperation.newBuilder()
					.setCode(IvtCheck.TamperMachineOpcode.TamperMachineCallAsync)
					.setStr("JL(JLjava/lang/Object;)I")
					.build()
			)
			.addOp(
				IvtCheck.TamperMachineOperation.newBuilder()
					.setCode(IvtCheck.TamperMachineOpcode.TamperMachinePop)
					.build()
			)
			.build();
		ProtobufEncoder resEnc = ProtobufEncoder.of();

		TamperMachine.generate(resEnc, ProtobufDecoder.of(req.toByteArray()), sdk, null);

		IvtCheck.TamperMachineResponse resp =
			IvtCheck.TamperMachineResponse.parseFrom(resEnc.asBuffer());
		IvtCheck.DynamicEntropy stack = resp.getStack();

		assertEquals(true, TamperMachine.Z);
		assertEquals(456, TamperMachine.I);
		assertEquals(789, TamperMachine.J);
		assertEquals("test-3", TamperMachine.L);
		assertEquals(1, stack.getFlagCount());
		assertEquals(true, stack.getFlag(0));
		assertEquals(
			Arrays.asList(
				123,
				456,
				TamperMachine.IJ(987654321, 123456789),
				TamperMachine.JL(32, sdk.context())
			),
			stack.getU32List()
		);
		assertEquals(1, stack.getU64Count());
		assertEquals(789, stack.getU64(0));
		assertEquals(Arrays.asList("test-2", "test-3"), stack.getStrList());
	}
}
