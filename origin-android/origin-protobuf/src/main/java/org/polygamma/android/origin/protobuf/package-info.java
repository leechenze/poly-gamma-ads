// SPDX-License-Identifier: MIT OR Apache-2.0

/**
 * Lightweight Protocol Buffers (Protobuf) implementation classes.
 * <p>Protobuf messages can be serialized and deserialized using {@link
 * org.polygamma.android.origin.protobuf.ProtobufWriter} and {@link
 * org.polygamma.android.origin.protobuf.ProtobufReader}, respectively. Protobuf message field
 * tags can be generated using the definitions in {@link
 * org.polygamma.android.origin.protobuf.ProtobufField}.
 * <p>Objects which can be serialized into a Protobuf message should implement {@link
 * org.polygamma.android.origin.protobuf.ProtobufSerializable}. The {@link
 * org.polygamma.android.origin.protobuf.ProtobufDeserializer} can be implemented for messages
 * which can be deserialized from a Protobuf message.
 *
 * @since 1.2
 */
package org.polygamma.android.origin.protobuf;
