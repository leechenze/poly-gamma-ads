// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.antifraud;

import static org.polygamma.android.origin.antifraud.CheckWire.IvtRatingUnknown;
import static org.polygamma.android.origin.antifraud.CheckWire.SessionResult_CONF;
import static org.polygamma.android.origin.antifraud.CheckWire.SessionResult_DIGEST;
import static org.polygamma.android.origin.antifraud.CheckWire.SessionResult_RATING;
import static org.polygamma.android.origin.antifraud.CheckWire.SessionResult_RECKTIMESTAMPSEC;

import androidx.annotation.IntRange;

import org.polygamma.android.origin.protobuf.ProtobufDecoder;
import org.polygamma.android.origin.protobuf.ProtobufEncoder;
import org.polygamma.android.origin.util.Preconditions;
import org.polygamma.android.origin.util.Time;

/**
 * IVT check session result.
 * <p>This describes the result of an IVT check performed for the executing environment. While
 * the session result, sent over the wire, specifies the next recheck timestamp as a UNIX
 * timestamp, instances of this define next recheck timestamp as time since boot.
 */
final class CheckSessionResult {

	/**
	 * Decode result from Protobuf wire format.
	 *
	 * @param dec decoder to decode from
	 * @return decoded result
	 */
	static CheckSessionResult ofProtobuf(ProtobufDecoder dec) {
		byte[] digest = null;
		int rating = IvtRatingUnknown;
		int conf = 0;
		long reck = 0L;

		while (dec.hasRemaining()) {
			int tag = dec.decodeFieldTag();

			if (tag == SessionResult_RATING)
				rating = dec.decodeUint32();
			else if (tag == SessionResult_CONF)
				conf = dec.decodeUint32();
			else if (tag == SessionResult_DIGEST)
				digest = dec.decodeByteArray();
			else if (tag == SessionResult_RECKTIMESTAMPSEC)
				reck = Time.durationBetween(Time.nowUtcSeconds(), dec.decodeUint64());
			else
				dec.skipFieldValue(tag);
		}
		return new CheckSessionResult(reck, new AntifraudStatus(digest, rating, conf));
	}

	/**
	 * Timestamp, in seconds since boot, to perform recheck at.
	 */
	final long recheckTimestampSeconds;

	/**
	 * Anti-fraud status.
	 */
	final AntifraudStatus status;

	/**
	 * Construct new session result.
	 *
	 * @param reckDelaySecs delay, in seconds, to perform recheck at
	 * @param status anti-fraud status
	 */
	CheckSessionResult(long reckDelaySecs, AntifraudStatus status) {
		this.recheckTimestampSeconds = Time.nowRealtimeSeconds() + reckDelaySecs;
		this.status = Preconditions.checkNotNull(status);
	}

	/**
	 * Construct copy with new recheck delay.
	 *
	 * @param delaySecs new delay, in seconds
	 * @return copy instance
	 */
	CheckSessionResult withNextCheckDelaySeconds(long delaySecs) {
		return new CheckSessionResult(delaySecs, this.status);
	}

	/**
	 * Delay, in seconds, to perform next check at.
	 *
	 * @return next check delay
	 */
	@IntRange(from = 0) long nextCheckDelaySeconds() {
		return Time.durationBetween(Time.nowRealtimeSeconds(), this.recheckTimestampSeconds);
	}

	/**
	 * Encode into Protobuf wire format.
	 *
	 * @param enc encoder to encode into
	 */
	void toProtobuf(ProtobufEncoder enc) {
		enc.encodeUnsignedIntField(SessionResult_RATING, this.status.rating)
			.encodeUnsignedIntField(SessionResult_CONF, this.status.confidence)
			.encodeByteArrayField(SessionResult_DIGEST, this.status.digest)
			.encodeUnsignedLongField(
				SessionResult_RECKTIMESTAMPSEC,
				Time.nowUtcSeconds() + this.nextCheckDelaySeconds()
			);
	}
}
