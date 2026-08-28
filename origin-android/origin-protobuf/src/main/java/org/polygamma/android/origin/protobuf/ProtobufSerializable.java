// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.protobuf;

/**
 * Payload which can be serialized into a Protocol buffer message.
 *
 * @since 1.2
 */
public interface ProtobufSerializable {
	/**
	 * Serialize payload into Protocol buffer.
	 *
	 * @param writer writer to serialize with
	 * @since 1.2
	 */
	void toProtobuf(ProtobufWriter writer);
}
