// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.antifraud;

import androidx.annotation.VisibleForTesting;

import org.polygamma.android.origin.protobuf.ProtobufField;
import org.polygamma.android.origin.protobuf.ProtobufReader;
import org.polygamma.android.origin.util.Futures;
import org.polygamma.android.origin.util.Reflection;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

/**
 * Machine used to dynamically generate entropy when a device tamper is detected.
 */
final class EntropyMachine {

	/**
	 * Pop top value placing it into the entropy pool.
	 */
	@VisibleForTesting
	static final int Pop				=  1;

	/**
	 * Pop top value and discard it.
	 */
	@VisibleForTesting
	static final int PopDiscard			=  2;

	/**
	 * Push value onto top.
	 */
	@VisibleForTesting
	static final int Push				= 50;

	/**
	 * Push background asynchronous value onto top.
	 */
	@VisibleForTesting
	static final int PushBackAsync		= 51;

	/**
	 * Push foreground asynchronous value onto top.
	 */
	@VisibleForTesting
	static final int PushFrontAsync		= 52;

	/**
	 * Push application onto top.
	 */
	@VisibleForTesting
	static final int PushApp			= 53;

	/**
	 * Push current activity onto top.
	 */
	@VisibleForTesting
	static final int PushActivity		= 54;

	/**
	 * Duplicate {@code i}-th value onto top.
	 */
	@VisibleForTesting
	static final int Dup				= 100;

	/**
	 * Invoke subroutine.
	 */
	@VisibleForTesting
	static final int Call				= 150;

	/**
	 * Invoke subroutine asynchronously.
	 */
	@VisibleForTesting
	static final int CallAsync			= 151;

	/**
	 * Read property.
	 */
	@VisibleForTesting
	static final int Read				= 200;

	/**
	 * Write property.
	 */
	@VisibleForTesting
	static final int Write				= 201;

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
	 * @param module module to generate entropy for
	 * @param ops machine operations to perform
	 * @return resulting entropy values
	 */
	static List<Object> generate(AntifraudModule module, ByteBuffer ops) {
		if (!ops.hasRemaining())
			return Collections.emptyList();

		EntropyMachine machine = new EntropyMachine(module);
		ProtobufReader reader = new ProtobufReader(ops);

		try {
			while (reader.hasRemaining()) {
				int tag = reader.readTag();

				Object val =
					reader.isCurrentVarint() ? reader.readInt64() :
					reader.isCurrentFixed32() ? reader.readFixed32() :
					reader.isCurrentFixed64() ? reader.readFixed64() :
					reader.readBytes();

				machine.evaluate(ProtobufField.numberOf(tag), val);
			}
		} catch (Exception err) {
			try (
				ByteArrayOutputStream bytes = new ByteArrayOutputStream();
				PrintStream msg = new PrintStream(bytes)
			) {
				err.printStackTrace(msg);
				machine.entropy.add(bytes.toByteArray());
			} catch (IOException ignored) {
			}
		}
		return machine.entropy;
	}

	private final List<Object> entropy;
	private final AntifraudModule module;
	private final ArrayList<Object> stack;

	private EntropyMachine(AntifraudModule module) {
		this.entropy = new ArrayList<>(1);
		this.module = module;
		this.stack = new ArrayList<>();
	}

	/**
	 * Pop from top.
	 *
	 * @return top value
	 */
	private Object pop() {
		return this.stack.remove(this.stack.size() - 1);
	}

	/**
	 * Push onto top.
	 *
	 * @param val new top value
	 */
	private void push(Object val) {
		this.stack.add(val);
	}

	/**
	 * Resolve to a tamper member of this class.
	 *
	 * @param value tamper member name
	 * @return tamper member signature
	 */
	private static String toSignature(Object value) {
		return String.format(
			Locale.ROOT,
			"L%s.%s",
			EntropyMachine.class.getName().replace('.', '/'),
			new String((byte[]) value, StandardCharsets.UTF_8)
		);
	}

	private void evaluate(int tag, Object value) {
		switch (tag) {
		case Pop:
			this.entropy.add(this.pop());
			break;
		case PopDiscard:
			this.pop();
			break;
		case Push:
			this.push(value);
			break;
		case PushBackAsync:
			this.push(this.module.sdk().backgroundIoExecutor());
			break;
		case PushFrontAsync:
			this.push(this.module.sdk().foregroundExecutor());
			break;
		case PushApp:
			this.push(this.module.sdk().context());
			break;
		case PushActivity:
			this.push(this.module.currentActivity());
			break;
		case Dup:
			this.push(this.stack.get(this.stack.size() - ((Number) value).intValue()));
			break;
		case Call:
			this.push(Reflection.invoke(toSignature(value), this::pop));
			break;
		case CallAsync:
			this.push(Futures.await(
				((ExecutorService) this.pop())
					.submit(Reflection.invokerOf(toSignature(value), this::pop)::get)
			));
			break;
		case Read:
			this.push(Reflection.read(toSignature(value), this::pop));
			break;
		case Write:
			Reflection.write(toSignature(value), this::pop);
			break;
		default:
			throw new UnsupportedOperationException(String.format(
				Locale.ROOT,
				"unknown tag: %s",
				tag
			));
		}
	}
}
