// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.antifraud;

import android.os.Build;
import android.util.Pair;

import androidx.annotation.Nullable;
import androidx.core.util.Supplier;

import org.polygamma.android.origin.protobuf.ProtobufField;
import org.polygamma.android.origin.protobuf.ProtobufSerializable;
import org.polygamma.android.origin.protobuf.ProtobufWriter;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility for generating entropy from source values.
 */
class Entropy {

	/*
	 * Golden ratios to use for propagating random hash to MSB. These constants are derived from
	 * Knuth vol 3, section 6.4, exercise 9.: (sqrt(5) - 1) / 2
	 */
	private static final int GOLDEN_RATIO = 0x61c88647;
	private static final long GOLDEN_RATIO_LONG = 0x61c8864680b583ebL;

	/**
	 * Generate entropy from {@code byte} {@linkplain ByteBuffer buffer}.
	 *
	 * @param src buffer to generate entropy from
	 * @return resulting entropy
	 */
	static ByteBuffer of(ByteBuffer src) {
		if (!src.hasRemaining())
			return ByteBuffer.allocate(0);
		if (src.order() != ByteOrder.LITTLE_ENDIAN)
			src = src.duplicate().order(ByteOrder.LITTLE_ENDIAN);

		ByteBuffer dst = ByteBuffer.allocate((((src.remaining() + 7) / 8) * 8) + 8)
			.order(ByteOrder.LITTLE_ENDIAN)
			.putLong(Double.doubleToLongBits(Math.random()) * GOLDEN_RATIO_LONG);

		do {
			int pos = dst.position();
			int a, b;

			if (src.remaining() >= 8) {
				a = src.getInt();
				b = src.getInt();
			} else {
				dst.put(src)
					.put((byte) 0)
					.position(pos);
				a = dst.getInt(pos + 0);
				b = dst.getInt(pos + 4);
			}

			pos -= 8;

			int c = (dst.getInt(pos) * GOLDEN_RATIO) ^ (dst.getInt(pos + 4) * GOLDEN_RATIO);
			int d = c + dst.getInt(pos + ((c & 1) * 4));
			int e = c + dst.getInt(pos + (((c >>> 11) & 1) * 4));

			a += (((b << 4) ^ (b >>> 5)) + b) ^ d;
			b += (((a << 4) ^ (a >>> 5)) + a) ^ e;
			dst.putInt(a)
				.putInt(b);
		} while (src.hasRemaining());
		return (ByteBuffer) dst.flip();
	}

	/**
	 * Generate entropy from an arbitrary value.
	 *
	 * @param value value to generate entropy from
	 * @return resulting entropy
	 */
	static ByteBuffer ofValue(@Nullable Object value) {
		List<Object> worklist = new ArrayList<>(1);
		IdentityHashMap<Object, Object> visited = new IdentityHashMap<>();
		ProtobufWriter writer = new ProtobufWriter();
		int num = 0;

		worklist.add(value);
		while (!worklist.isEmpty()) {
			Object val = worklist.remove(0);
			int field = ++num;

			if (val == null || visited.put(val, val) == val)
				continue;
			if (val instanceof Supplier) {
				--num;
				try {
					worklist.add(0, ((Supplier<?>) val).get());
				} catch (Throwable cause) {
					worklist.add(0, cause);
				}
			} else if (
				Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
				val instanceof java.util.function.Supplier
			) {
				--num;
				try {
					worklist.add(0, ((java.util.function.Supplier<?>) val).get());
				} catch (Throwable cause) {
					worklist.add(0, cause);
				}
			} else if (val instanceof Throwable) {
				try (
					StringWriter str = new StringWriter();
					PrintWriter printer = new PrintWriter(str)
				) {
					((Throwable) val).printStackTrace(printer);
					printer.flush();
					writer.writeString(ProtobufField.ofString(field), str.toString());
				} catch (IOException ignored) {
				}
			} else if (val instanceof Float || val instanceof Double) {
				writer.writeDouble(ProtobufField.ofDouble(field), ((Number) val).doubleValue());
			} else if (val instanceof Number) {
				writer.writeInt64(ProtobufField.ofInt64(field), ((Number) val).longValue());
			} else if (val instanceof Boolean) {
				writer.writeBool(ProtobufField.ofBool(field), (boolean) val);
			} else if (val instanceof byte[]) {
				writer.writeBytes(ProtobufField.ofBytes(field), (byte[]) val);
			} else if (val instanceof ByteBuffer) {
				writer.writeBytes(ProtobufField.ofBytes(field), ((ByteBuffer) val).duplicate());
			} else if (val instanceof float[]) {
				writer.writePackedFloat(ProtobufField.ofPackedFloat(field), (float[]) val);
			} else if (val instanceof double[]) {
				writer.writePackedDouble(ProtobufField.ofPackedDouble(field), (double[]) val);
			} else if (val instanceof int[]) {
				writer.writePackedInt32(ProtobufField.ofPackedInt32(field), (int[]) val);
			} else if (val instanceof long[]) {
				writer.writePackedInt64(ProtobufField.ofPackedInt64(field), (long[]) val);
			} else if (val instanceof boolean[]) {
				writer.writePackedBool(ProtobufField.ofPackedBool(field), (boolean[]) val);
			} else if (val instanceof ProtobufSerializable) {
				writer.writeLen(ProtobufField.ofMessage(field), (ProtobufSerializable) val);
			} else if (val instanceof Pair) {
				writer.writeInt32(ProtobufField.ofInt32(field), 2);
				worklist.add(0, ((Pair<?, ?>) val).first);
				worklist.add(1, ((Pair<?, ?>) val).second);
			} else if (val instanceof Map) {
				--num;
				worklist.add(0, ((Map<?, ?>) val).entrySet());
			} else if (val instanceof Iterable) {
				int i = 0;

				for (Object elem : (Iterable<?>) val)
					worklist.add(i++, elem);
				writer.writeInt32(ProtobufField.ofInt32(field), i);
			} else if (val.getClass().isArray()) {
				for (int i = 0, n = Array.getLength(val); i < n; i++)
					worklist.add(i, Array.get(val, i));
				writer.writeInt32(ProtobufField.ofInt32(field), Array.getLength(val));
			} else {
				writer.writeString(ProtobufField.ofString(field), val.toString());
			}
		}
		return of(writer.finish());
	}

	private Entropy() {
	}
}
