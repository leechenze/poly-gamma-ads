// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.adcom;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageLite;

import org.polygamma.android.origin.protobuf.ProtobufDecoder;
import org.polygamma.android.origin.protobuf.ProtobufEncoder;
import org.polygamma.android.origin.util.BiConsumer;
import org.polygamma.android.origin.util.Function;

import java.nio.ByteBuffer;

public class TestUtil {

	@FunctionalInterface
	public interface GoogleProtobufDecode {
		MessageLite decode(ByteBuffer src) throws InvalidProtocolBufferException;
	}

	public static <T> T encodeAndDecode(
		T msg,
		BiConsumer<T, ProtobufEncoder> encode,
		Function<ProtobufDecoder, T> decode,
		GoogleProtobufDecode decodeExp
	) {
		ProtobufEncoder enc = ProtobufEncoder.of();

		encode.accept(msg, enc);

		MessageLite got;

		try {
			got = decodeExp.decode(enc.asBuffer());
		} catch (InvalidProtocolBufferException cause) {
			throw new AssertionError(cause);
		}
		return decode.apply(ProtobufDecoder.of(got.toByteArray()));
	}
}
