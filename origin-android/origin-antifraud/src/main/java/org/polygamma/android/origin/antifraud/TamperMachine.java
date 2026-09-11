// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.antifraud;

import static org.polygamma.android.origin.antifraud.CheckWire.TamperMachineCall;
import static org.polygamma.android.origin.antifraud.CheckWire.TamperMachineCallAsync;
import static org.polygamma.android.origin.antifraud.CheckWire.TamperMachineDup;
import static org.polygamma.android.origin.antifraud.CheckWire.TamperMachineOperation_BLOB;
import static org.polygamma.android.origin.antifraud.CheckWire.TamperMachineOperation_CODE;
import static org.polygamma.android.origin.antifraud.CheckWire.TamperMachineOperation_F32;
import static org.polygamma.android.origin.antifraud.CheckWire.TamperMachineOperation_F64;
import static org.polygamma.android.origin.antifraud.CheckWire.TamperMachineOperation_S32;
import static org.polygamma.android.origin.antifraud.CheckWire.TamperMachineOperation_S64;
import static org.polygamma.android.origin.antifraud.CheckWire.TamperMachineOperation_STR;
import static org.polygamma.android.origin.antifraud.CheckWire.TamperMachineOperation_U32;
import static org.polygamma.android.origin.antifraud.CheckWire.TamperMachineOperation_U64;
import static org.polygamma.android.origin.antifraud.CheckWire.TamperMachinePop;
import static org.polygamma.android.origin.antifraud.CheckWire.TamperMachinePopDiscard;
import static org.polygamma.android.origin.antifraud.CheckWire.TamperMachinePush;
import static org.polygamma.android.origin.antifraud.CheckWire.TamperMachinePushActivity;
import static org.polygamma.android.origin.antifraud.CheckWire.TamperMachinePushApp;
import static org.polygamma.android.origin.antifraud.CheckWire.TamperMachinePushBackAsync;
import static org.polygamma.android.origin.antifraud.CheckWire.TamperMachinePushFrontAsync;
import static org.polygamma.android.origin.antifraud.CheckWire.TamperMachineRead;
import static org.polygamma.android.origin.antifraud.CheckWire.TamperMachineRequest_OP;
import static org.polygamma.android.origin.antifraud.CheckWire.TamperMachineResponse_STACK;
import static org.polygamma.android.origin.antifraud.CheckWire.TamperMachineWrite;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import org.polygamma.android.origin.antifraud.CheckWire.TamperMachineOpcode;
import org.polygamma.android.origin.core.Origin;
import org.polygamma.android.origin.protobuf.ProtobufDecoder;
import org.polygamma.android.origin.protobuf.ProtobufEncoder;
import org.polygamma.android.origin.util.Futures;
import org.polygamma.android.origin.util.Logger;
import org.polygamma.android.origin.util.Preconditions;
import org.polygamma.android.origin.util.Reflection;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

/**
 * Machine used to dynamically generate entropy when a device tamper is detected.
 */
final class TamperMachine {

	private static final String TAG = TamperMachine.class.getSimpleName();

	// TAMPER MEMBERS
	@VisibleForTesting
	static boolean Z;
	@VisibleForTesting
	static int I;
	@VisibleForTesting
	static long J;
	@VisibleForTesting
	static Object L;

	@VisibleForTesting
	static int ZI(boolean z, int i) {
		return Boolean.hashCode(z) ^ Integer.hashCode(i);
	}

	@VisibleForTesting
	@SuppressWarnings("SameParameterValue")
	static int IJ(int i, long j) {
		return Integer.hashCode(i) ^ Long.hashCode(j);
	}

	@VisibleForTesting
	static int JL(long j, Object l) {
		return Long.hashCode(j) ^ Objects.hashCode(l);
	}

	/**
	 * Generate entropy values.
	 *
	 * @param enc encoder to encode tamper machine response into
	 * @param dec decoder to decode tamper machine request from
	 * @param sdk owning SDK
	 * @param actRef reference to current activity, if any
	 */
	static void generate(
		ProtobufEncoder enc, ProtobufDecoder dec,
		Origin sdk, @Nullable CurrentActivityReference actRef
	) {
		TamperMachine machine = new TamperMachine(sdk, actRef);

		try {
			while (dec.hasRemaining()) {
				int tag = dec.decodeFieldTag();

				if (tag == TamperMachineRequest_OP)
					dec.decodeLen(machine, TamperMachine::decodeAndExecute);
				else
					dec.skipFieldValue(tag);
			}
		} catch (Exception cause) {
			machine.entropy.add(cause);
			Logger.debug(TAG, "execution failed", cause);
		}
		enc.encodeLenField(
			TamperMachineResponse_STACK, machine.entropy,
			(ent, entEnc) -> EntropyCoding.encodeDynamic(entEnc, ent)
		);
	}

	private final Origin sdk;
	private final @Nullable CurrentActivityReference currentActivityReference;
	private final List<Object> entropy;
	private final ArrayList<Object> stack;

	private TamperMachine(Origin sdk, @Nullable CurrentActivityReference actRef) {
		this.sdk = Preconditions.checkNotNull(sdk);
		this.currentActivityReference = actRef;
		this.entropy = new ArrayList<>(1);
		this.stack = new ArrayList<>();
	}

	// Pop from top.
	private Object pop() {
		return this.stack.remove(this.stack.size() - 1);
	}

	// Push onto top.
	private void push(Object val) {
		this.stack.add(val);
	}

	// Resolve to a tamper member of this class.
	private static String toSignature(Object value) {
		return String.format(
			Locale.ROOT,
			"L%s.%s",
			TamperMachine.class.getName().replace('.', '/'),
			value instanceof byte[] ? new String((byte[]) value, StandardCharsets.UTF_8) :
			value.toString()
		);
	}

	private void execute(@TamperMachineOpcode int code, Object value) {
		switch (code) {
		case TamperMachinePop:
			this.entropy.add(this.pop());
			break;
		case TamperMachinePopDiscard:
			this.pop();
			break;
		case TamperMachinePush:
			this.push(value);
			break;
		case TamperMachinePushBackAsync:
			this.push(this.sdk.backgroundIoExecutor());
			break;
		case TamperMachinePushFrontAsync:
			this.push(this.sdk.foregroundExecutor());
			break;
		case TamperMachinePushApp:
			this.push(this.sdk.context());
			break;
		case TamperMachinePushActivity:
			this.push(
				this.currentActivityReference == null ? null :
				this.currentActivityReference.get()
			);
			break;
		case TamperMachineDup:
			this.push(this.stack.get(this.stack.size() - ((Number) value).intValue()));
			break;
		case TamperMachineCall:
			this.push(Reflection.invoke(toSignature(value), this::pop));
			break;
		case TamperMachineCallAsync:
			this.push(Futures.await(
				((ExecutorService) this.pop())
					.submit(Reflection.invokerOf(toSignature(value), this::pop)::get)
			));
			break;
		case TamperMachineRead:
			this.push(Reflection.read(toSignature(value), this::pop));
			break;
		case TamperMachineWrite:
			Reflection.write(toSignature(value), this::pop);
			break;
		default:
			throw new UnsupportedOperationException(String.format(
				Locale.ROOT,
				"unknown tag: %s",
				code
			));
		}
	}

	private TamperMachine decodeAndExecute(ProtobufDecoder dec) {
		int code = 0;
		Object value = null;

		while (dec.hasRemaining()) {
			int tag = dec.decodeFieldTag();

			if (tag == TamperMachineOperation_CODE)
				code = dec.decodeUint32();
			else if (tag == TamperMachineOperation_U32)
				value = dec.decodeUint32();
			else if (tag == TamperMachineOperation_S32)
				value = dec.decodeSint32();
			else if (tag == TamperMachineOperation_U64)
				value = dec.decodeUint64();
			else if (tag == TamperMachineOperation_S64)
				value = dec.decodeSint64();
			else if (tag == TamperMachineOperation_F32)
				value = dec.decodeFloat();
			else if (tag == TamperMachineOperation_F64)
				value = dec.decodeDouble();
			else if (tag == TamperMachineOperation_BLOB)
				value = dec.decodeByteArray();
			else if (tag == TamperMachineOperation_STR)
				value = dec.decodeString();
			else
				dec.skipFieldValue(tag);
		}
		this.execute(code, value);
		return this;
	}
}
