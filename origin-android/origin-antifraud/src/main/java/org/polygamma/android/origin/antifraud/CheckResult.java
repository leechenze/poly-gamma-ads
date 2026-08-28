// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.antifraud;

import static org.polygamma.android.origin.protobuf.ProtobufField.*;

import androidx.annotation.Nullable;

import org.polygamma.android.origin.protobuf.ProtobufReader;
import org.polygamma.android.origin.protobuf.ProtobufSerializable;
import org.polygamma.android.origin.protobuf.ProtobufWriter;
import org.polygamma.android.origin.util.Time;

/**
 * IVT check remote procedure call (RPC) result.
 */
final class CheckResult implements ProtobufSerializable {

	private static final @Tag int DIGEST			= ofString(   1);
	private static final @Tag int RATING			= ofInt32(    2);
	private static final @Tag int CONFIDENCE		= ofInt32(    3);
	private static final @Tag int RECKTIMESTAMPSEC	= ofFixed64(  4);
	private static final @Tag int ENTMACHINEOP		= ofBytes(  100);

	/**
	 * Deserialize check result from Protobuf message.
	 *
	 * @param reader reader to deserialize from
	 * @return deserialized result
	 * @throws RuntimeException coding is malformed
	 */
	static CheckResult ofProtobuf(ProtobufReader reader) {
		String digest = "";
		int rating = AntifraudStatus.RatingUnknown;
		int conf = 0;
		long reckWhen = 0L;
		byte[] entOps = null;

		while (reader.hasRemaining()) {
			int tag = reader.readTag();

			if (tag == DIGEST) {
				digest = reader.readString();
			} else if (tag == RATING) {
				rating = reader.readInt32();
			} else if (tag == CONFIDENCE) {
				conf = reader.readInt32();
			} else if (tag == RECKTIMESTAMPSEC) {
				long when = reader.readFixed64();
				long nowBoot = Time.nowRealtimeSeconds();
				long nowUtc = Time.nowUtcSeconds();

				reckWhen = nowBoot + Time.durationBetween(nowUtc, when);
			} else if (tag == ENTMACHINEOP) {
				entOps = reader.readBytes();
			}
		}
		return new CheckResult(reckWhen, new AntifraudStatus(digest, rating, conf), entOps);
	}

	final long recheckTimestampSeconds;
	final AntifraudStatus status;
	final @Nullable byte[] entropyMachineOperations;

	/**
	 * Construct new empty check result.
	 */
	CheckResult() {
		this.recheckTimestampSeconds = 0;
		this.status = new AntifraudStatus("", AntifraudStatus.RatingUnknown, 0);
		this.entropyMachineOperations = null;
	}

	/**
	 * Construct new check result.
	 *
	 * @param rechkSecs timestamp, in seconds since boot, of when next recheck must be performed
	 * @param status antifraud status
	 * @param entMachineOps entropy machine operations to execute, if any
	 */
	CheckResult(long rechkSecs, AntifraudStatus status, @Nullable byte[] entMachineOps) {
		this.recheckTimestampSeconds = rechkSecs;
		this.status = status;
		this.entropyMachineOperations =
			entMachineOps == null || entMachineOps.length == 0 ? null : entMachineOps;
	}

	/**
	 * Duration, in seconds, until next recheck.
	 *
	 * @return recheck delay
	 */
	long recheckDelaySeconds() {
		return Time.durationBetween(Time.nowRealtimeSeconds(), this.recheckTimestampSeconds);
	}

	/**
	 * Construct copy of {@code this} with new recheck {@linkplain #recheckTimestampSeconds
	 * timestamp}.
	 *
	 * @param when new recheck timestamp
	 * @return resulting check result
	 */
	CheckResult withRecheckTimestampSeconds(long when) {
		return new CheckResult(when, this.status, this.entropyMachineOperations);
	}

	@Override
	public void toProtobuf(ProtobufWriter writer) {
		writer.writeString(DIGEST, this.status.digest());
		writer.writeInt32(RATING, this.status.rating());
		writer.writeInt32(CONFIDENCE, this.status.confidence());
		writer.writeFixed64(RECKTIMESTAMPSEC, Time.nowUtcSeconds() + this.recheckDelaySeconds());
		writer.writeBytes(ENTMACHINEOP, this.entropyMachineOperations);
	}
}
