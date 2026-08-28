// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.protobuf;

/**
 * Function which deserializes a payload from a Protocol buffer message.
 *
 * @param <T> deserialized payload type
 * @since 1.2
 */
@FunctionalInterface
public interface ProtobufDeserializer<T> {
	/**
	 * Deserialize payload from Protocol buffer.
	 *
	 * @param reader reader to deserialize from
	 * @return deserialized payload
	 * @throws java.nio.BufferUnderflowException insufficient data available to deserialize payload
	 * @throws IllegalStateException coding is malformed
	 * @since 1.2
	 */
	T ofProtobuf(ProtobufReader reader);
}
