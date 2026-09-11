// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.antifraud;

import static org.polygamma.android.origin.antifraud.CheckWire.SessionOperation_CODE;
import static org.polygamma.android.origin.antifraud.CheckWire.SessionOperation_ERROR;
import static org.polygamma.android.origin.antifraud.CheckWire.SessionOperation_PAYLOAD;
import static org.polygamma.android.origin.antifraud.CheckWire.SessionOperation_PAYLOADPAD;

import androidx.annotation.Nullable;

import org.polygamma.android.origin.antifraud.CheckWire.SessionOpcode;
import org.polygamma.android.origin.protobuf.ProtobufDecoder;
import org.polygamma.android.origin.protobuf.ProtobufEncoder;
import org.polygamma.android.origin.util.Preconditions;

import java.nio.ByteBuffer;

/**
 * IVT check session operation.
 * <p>This encapsulates a session operation decoded for an IVT check session. The payload for the
 * operation <i>may</i> be encrypted or decrypted based on context.
 */
final class CheckSessionOperation {

	/**
	 * Construct new operation with payload from subsequence of {@code byte} array.
	 *
	 * @param code operation code
	 * @param arr payload array
	 * @param off offset, within {@code arr}, payload begins at
	 * @param len number of payload bytes
	 * @param padSize number of trailing payload bytes
	 * @return resulting operation
	 * @throws IndexOutOfBoundsException {@code off}, {@code len}, or {@code padSize} is negative,
	 * or, {@code off + len} or {@code padSize} is greater than {@code arr.length} or {@code len},
	 * respectively
	 */
	static CheckSessionOperation
	ofPayloadArray(@SessionOpcode int code, byte[] arr, int off, int len, int padSize) {
		return new CheckSessionOperation(code, arr, off, len, padSize);
	}

	/**
	 * Construct new operation with payload from {@code byte} array.
	 *
	 * @param code operation code
	 * @param arr payload array
	 * @param padSize number of trailing payload bytes
	 * @return resulting operation
	 * @throws IndexOutOfBoundsException {@code padSize} is negative or greater than {@code
	 * arr.length}
	 */
	static CheckSessionOperation ofPayloadArray(@SessionOpcode int code, byte[] arr, int padSize) {
		return new CheckSessionOperation(code, arr, 0, arr.length, padSize);
	}

	/**
	 * Construct new operation with payload from {@code byte} buffer.
	 * <p>If {@code buff} is backed by an {@linkplain ByteBuffer#hasArray() array}, resulting
	 * operation will reference the array directly; otherwise, the resulting operation will
	 * reference the {@linkplain ByteBuffer#remaining() remaining} bytes of {@code buff} copied
	 * into a newly allocated array.
	 *
	 * @param code operation code
	 * @param buff payload buffer
	 * @param padSize number of trailing payload bytes
	 * @return resulting operation
	 * @throws IndexOutOfBoundsException {@code padSize} is negative or greater than number of
	 * bytes remaining in {@code buff}
	 */
	static CheckSessionOperation
	ofPayloadBuffer(@SessionOpcode int code, ByteBuffer buff, int padSize) {
		if (buff.hasArray()) {
			return ofPayloadArray(
				code,
				buff.array(), buff.arrayOffset() + buff.position(), buff.remaining(), padSize
			);
		}

		byte[] arr = new byte[buff.remaining()];

		buff.duplicate()
			.get(arr);
		return ofPayloadArray(code, arr, padSize);
	}

	/**
	 * Construct erroneous operation with string cause.
	 *
	 * @param code operation code
	 * @param cause error cause
	 * @return resulting operation
	 */
	static CheckSessionOperation ofError(@SessionOpcode int code, @Nullable String cause) {
		return new CheckSessionOperation(code, cause == null ? "unknown" : cause);
	}

	/**
	 * Construct erroneous operation with throwable cause.
	 *
	 * @param code operation code
	 * @param cause error cause
	 * @return resulting operation
	 */
	static CheckSessionOperation ofError(@SessionOpcode int code, @Nullable Throwable cause) {
		return new CheckSessionOperation(code, cause == null ? "unknown" : cause);
	}

	/**
	 * Decode operation from Protobuf wire format.
	 * <p>The payload, if any, of the resulting operation will reference the array {@linkplain
	 * ProtobufDecoder#array() array} underlying {@code dec}.
	 *
	 * @param dec decoder to decode from
	 * @return resulting operation
	 */
	static CheckSessionOperation ofProtobuf(ProtobufDecoder dec) {
		int code = 0;
		int payloadPad = 0;
		ByteBuffer payload = null;
		String err = null;

		while (dec.hasRemaining()) {
			int tag = dec.decodeFieldTag();

			if (tag == SessionOperation_CODE)
				code = dec.decodeUint32();
			else if (tag == SessionOperation_PAYLOAD)
				payload = dec.decodeByteBufferView();
			else if (tag == SessionOperation_PAYLOADPAD)
				payloadPad = dec.decodeUint32();
			else if (tag == SessionOperation_ERROR)
				err = dec.decodeString();
			else
				dec.skipFieldValue(tag);
		}

		Preconditions.checkArgument(
			code != 0 && (payload == null || err == null),
			"malformed coding"
		);
		if (err != null)
			return ofError(code, err);
		if (payload != null)
			return ofPayloadBuffer(code, payload, payloadPad);
		return ofPayloadArray(code, new byte[0], payloadPad);
	}

	/**
	 * Operation code.
	 */
	final @SessionOpcode int code;

	/**
	 * Offset, within {@linkplain #payload payload}, to decode operation payload from.
	 */
	final int payloadOffset;

	/**
	 * Total length, including {@linkplain #payloadPadSize padding}, of payload.
	 */
	final int payloadLength;

	/**
	 * Number of trailing bytes, within payload, used for padding.
	 */
	final int payloadPadSize;

	// `byte[]` if successful; otherwise, `String` or `Throwable` error cause.
	private final Object payload;

	/**
	 * Next operation in chain, if any.
 	 */
	@Nullable CheckSessionOperation next;

	private CheckSessionOperation(
		@SessionOpcode int code,
		byte[] payload, int payloadOff, int payloadLen, int payloadPadSize
	) {
		Preconditions.checkFromIndexSize(payloadOff, payloadLen, payload.length);
		Preconditions.checkFromIndexSize(0, payloadPadSize, payloadLen);
		this.code = code;
		this.payload = payload;
		this.payloadOffset = payloadOff;
		this.payloadLength = payloadLen;
		this.payloadPadSize = payloadPadSize;
	}

	private CheckSessionOperation(@SessionOpcode int code, Object cause) {
		Preconditions.checkArgument(cause instanceof String || cause instanceof Throwable);
		this.code = code;
		this.payload = cause;
		this.payloadOffset = 0;
		this.payloadLength = 0;
		this.payloadPadSize = 0;
	}

	/**
	 * Test whether operation completed erroneously.
	 *
	 * @return {@code true} if, and only if, operation failed
	 */
	boolean isError() {
		return !(this.payload instanceof byte[]);
	}

	/**
	 * Payload to execute operation with or result of operation.
	 *
	 * @return operation payload
	 * @throws IllegalStateException operation has {@linkplain #isError() failed}
	 */
	byte[] payload() {
		Preconditions.checkState(this.payload instanceof byte[]);
		//noinspection DataFlowIssue
		return (byte[]) this.payload;
	}

	/**
	 * Description of error which caused operation to fail.
	 *
	 * @return error cause
	 * @throws IllegalStateException operation has not {@linkplain #isError() failed}
	 */
	String error() {
		if (this.payload instanceof String)
			return (String) this.payload;
		Preconditions.checkState(this.payload instanceof Throwable);
		//noinspection DataFlowIssue
		return EntropyCoding.throwableToString((Throwable) this.payload);
	}

	/**
	 * Encode operation into Protobuf wire format.
	 *
	 * @param enc encoder to encode into
	 */
	void toProtobuf(ProtobufEncoder enc) {
		if (this.payload instanceof byte[]) {
			/*
			 * Our `payload` maybe a subsequence of array underlying `enc`, if we encode `payload`
			 * first, we can ensure we don't overwrite the array corrupting `payload`. If payload
			 * is empty, then fallback to ordered.
			 */
			if (this.payloadLength == 0) {
				enc.encodeFieldTag(SessionOperation_PAYLOAD)
					.encodeByteArray((byte[]) this.payload, 0, 0);
			} else {
				enc.encodeByteArrayField(
					SessionOperation_PAYLOAD,
					(byte[]) this.payload, this.payloadOffset, this.payloadLength
				)
					.encodeUnsignedIntField(SessionOperation_PAYLOADPAD, this.payloadPadSize);
			}
		} else {
			enc.encodeFieldTag(SessionOperation_ERROR)
				.encodeString(
					this.payload instanceof String ? (String) this.payload :
					EntropyCoding.throwableToString((Throwable) this.payload)
				);
		}
		enc.encodeUnsignedIntField(SessionOperation_CODE, this.code);
	}
}
