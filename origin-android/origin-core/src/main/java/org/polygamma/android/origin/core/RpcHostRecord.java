package org.polygamma.android.origin.core;

import static org.polygamma.android.origin.protobuf.ProtobufField.*;

import android.annotation.SuppressLint;
import android.net.DnsResolver;
import android.os.Build;
import android.os.CancellationSignal;
import android.os.SystemClock;
import android.text.TextUtils;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;

import org.polygamma.android.origin.protobuf.ProtobufReader;
import org.polygamma.android.origin.protobuf.ProtobufWriter;
import org.polygamma.android.origin.util.Logger;
import org.polygamma.android.origin.util.Preconditions;
import org.polygamma.android.origin.util.Strings;
import org.polygamma.android.origin.util.Time;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Origin Remote Procedure Call (RPC) host DNS record.
 */
final class RpcHostRecord {

	private static final String TAG = RpcHostRecord.class.getSimpleName();

	private static final int MAX_EXPIRY_SECONDS = Math.toIntExact(TimeUnit.DAYS.toSeconds(7));

	/*
	 * If we have DNS queries available to us, then prefer a lower default expiry, because
	 * this will be used for records only when normal DNS queries fail.
	 */
	private static final int DEFAULT_EXPIRY_SECONDS =
		Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ? MAX_EXPIRY_SECONDS :
		Math.toIntExact(TimeUnit.MINUTES.toSeconds(30));

	@VisibleForTesting
	static final @Tag int EXPTS		= ofFixed64(1);
	@VisibleForTesting
	static final @Tag int PRIO		= ofInt32(  2);
	@VisibleForTesting
	static final @Tag int HOST		= ofString( 3);
	@VisibleForTesting
	static final @Tag int PORT		= ofInt32(  4);

	/**
	 * Construct host record with default {@linkplain #expiryTimestampSeconds expiry} and
	 * {@linkplain #priority priority}.
	 *
	 * @param host target hostname
	 * @param port target port
	 * @return resulting record
	 * @throws IllegalArgumentException {@code host} is {@linkplain String#isEmpty() empty}
	 */
	static RpcHostRecord ofHost(String host, int port) {
		return new RpcHostRecord(
			Time.nowUtcSeconds() + DEFAULT_EXPIRY_SECONDS,
			0,
			host,
			port
		);
	}

	/**
	 * Deserialize record from Protobuf message.
	 *
	 * @param reader reader to deserialize from
	 * @return deserialized record
	 * @throws RuntimeException {@code buff} is malformed
	 */
	static RpcHostRecord ofProtobuf(ProtobufReader reader) {
		long exp = Time.nowUtcSeconds() + MAX_EXPIRY_SECONDS;
		int prio = 0;
		String host = null;
		int port = 0;

		while (reader.hasRemaining()) {
			int tag = reader.readTag();

			if (tag == EXPTS)
				exp = reader.readFixed64();
			else if (tag == PRIO)
				prio = reader.readInt32();
			else if (tag == HOST)
				host = reader.readString();
			else if (tag == PORT)
				port = reader.readInt32();
		}
		return new RpcHostRecord(exp, prio, host, port);
	}

	/**
	 * Retrieve root host of a hostname.
	 *
	 * @param host hostname to retrieve root host of
	 * @return root host
	 */
	private static String rootHostOf(String host) {
		// XXX: This is really basic, we assume the root host is 2 part `<name>.<tld>`.
		Iterator<String> parts = Strings.split(host, '.');
		ArrayList<String> labels = new ArrayList<>(2);

		while (parts.hasNext())
			labels.add(parts.next());
		return (
			labels.size() > 2 ? host :
			TextUtils.join(".", labels.subList(labels.size() - 2, labels.size()))
		);
	}

	/**
	 * Construct host records from {@code HTTPS} DNS records.
	 *
	 * @param host queried hostname
	 * @param src DNS query answer
	 * @return resulting records
	 */
	private static ArrayList<RpcHostRecord> ofHttpsAnswers(String host, Dns.Message src) {
		ArrayList<RpcHostRecord> recs = new ArrayList<>(src.answers.length);

		for (int i = 0; i < src.answers.length; i++) {
			Dns.Record answer = src.answers[i];

			if (answer.type != Dns.HttpsRecordType)
				continue;

			Dns.Svcb https;

			try {
				https = Dns.decodeSvcb(ByteBuffer.wrap(answer.data));
			} catch (Throwable err) {
				Logger.warn(TAG, "failed to decode HTTPS for %s", host, err);
				continue;
			}

			String name = https.targetName;

			if (name.endsWith(".")) {
				name = name.substring(0, name.length() - 1);
				if (name.isEmpty())
					name = rootHostOf(host);
			} else {
				// relative name, append to root host
				name = String.format(Locale.ROOT, "%s.%s", name, rootHostOf(host));
			}

			recs.add(new RpcHostRecord(
				Time.nowUtcSeconds() + (
					answer.timeToLive <= 0 ? MAX_EXPIRY_SECONDS :
					Math.min(answer.timeToLive, MAX_EXPIRY_SECONDS)
				),
				https.priority,
				name,
				(Integer) https.parameters.get(Dns.PortSvcParamKey, 0)
			));
		}
		return recs;
	}

	/**
	 * Post query of {@code HTTPS} DNS records.
	 *
	 * @param host host to post query for
	 * @param exec executor to use for executing background tasks
	 * @param timeoutMs maximum time, in milliseconds, to wait for DNS response
	 * @return query result
	 * @throws Exception error was encountered
	 */
	@SuppressLint("WrongConstant")
	@RequiresApi(api = Build.VERSION_CODES.Q)
	private static Dns.Message postHttpsQuery(String host, Executor exec, long timeoutMs)
	throws Exception {
		LinkedTransferQueue<Object> res = new LinkedTransferQueue<>();
		CancellationSignal cancel = new CancellationSignal();
		Object val = null;

		DnsResolver.getInstance()
			.rawQuery(
				null,
				host,
				DnsResolver.CLASS_IN,
				Dns.HttpsRecordType,
				DnsResolver.FLAG_NO_CACHE_STORE | DnsResolver.FLAG_NO_CACHE_LOOKUP,
				exec,
				cancel,
				new DnsResolver.Callback<byte[]>() {
					@Override
					public void onAnswer(@NonNull byte[] answer, int rcode) {
						res.add(rcode == 0 ? answer : new IllegalStateException(String.format(
							Locale.ROOT,
							"query failed: %s",
							rcode
						)));
					}

					@Override
					public void onError(@NonNull DnsResolver.DnsException err) {
						res.add(err);
					}
				}
			);

		while (true) {
			long start = SystemClock.uptimeMillis();

			try {
				val = res.poll(timeoutMs, TimeUnit.MILLISECONDS);
				break;
			} catch (InterruptedException err) {
				Logger.info(TAG, "interrupted while awaiting query result", err);

				long end = SystemClock.uptimeMillis();

				timeoutMs -= (end - start);
				if (timeoutMs <= 0)
					break;
			}
		}

		cancel.cancel();

		if (val instanceof byte[])
			return Dns.decodeMessage(ByteBuffer.wrap((byte[]) val));
		if (val instanceof Throwable)
			throw new IllegalStateException("query failed", (Throwable) val);
		throw new TimeoutException("query timed out");
	}

	/**
	 * Query {@code HTTPS} records of a host.
	 *
	 * @param host host to query records of
	 * @param exec executor to use for executing background tasks
	 * @param timeoutMs maximum time, in milliseconds, to wait for DNS response
	 * @return queried records or {@linkplain Collection#isEmpty() empty}
	 */
	@RequiresApi(api = Build.VERSION_CODES.Q)
	private static Collection<RpcHostRecord>
	ofQueryHttps(String host, Executor exec, long timeoutMs) {
		HashSet<RpcHostRecord> recs = new HashSet<>();
		HashSet<String> queried = new HashSet<>();
		ArrayList<String> worklist = new ArrayList<>(1);

		worklist.add(host);
		do {
			String queryHost = worklist.remove(worklist.size() - 1);

			if (!queried.add(queryHost))
				continue;

			List<RpcHostRecord> answers;

			try {
				answers = ofHttpsAnswers(queryHost, postHttpsQuery(queryHost, exec, timeoutMs));
			} catch (Throwable err) {
				Logger.warn(TAG, "query failed", err);
				continue;
			}

			for (RpcHostRecord rec : answers) {
				// if it's an alias record, recurse; otherwise, push
				if (rec.priority == 0)
					worklist.add(rec.host);
				else
					recs.add(rec);
			}
		} while (!worklist.isEmpty());
		return recs;
	}

	/**
	 * Query host records from DNS.
	 *
	 * @param host host to query records of
	 * @param exec executor to use for executing background tasks
	 * @param timeoutMs maximum time, in milliseconds, to wait for DNS response
	 * @return possibly {@linkplain Collection#isEmpty() empty} collection of queried records
	 */
	@WorkerThread
	static Collection<RpcHostRecord> ofQuery(String host, Executor exec, long timeoutMs) {
		return (
			Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ?
			ofQueryHttps(host, exec, timeoutMs) :
			Collections.emptyList()
		);
	}

	private volatile long lastFailureTimestampSeconds;

	/**
	 * Timestamp, in seconds since UNIX epoch, of when record expires.
	 */
	final long expiryTimestampSeconds;

	/**
	 * Non-negative priority of service host.
	 */
	final @IntRange(from = 0) int priority;

	/**
	 * Target hostname.
	 */
	final String host;

	/**
	 * Target port, or {@code 0} for default.
	 */
	final int port;

	/**
	 * Construct new host record.
	 *
	 * @param expTs expiry timestamp, in seconds since UNIX epoch
	 * @param prio non-negative priority of target
	 * @param host target hostname
	 * @param port target port
	 * @throws IllegalArgumentException {@code prio} is negative, or {@code host} is {@linkplain
	 * String#isEmpty() empty}
	 */
	@VisibleForTesting
	RpcHostRecord(long expTs, @IntRange(from = 0) int prio, String host, int port) {
		Preconditions.checkArgument(prio >= 0 && !host.isEmpty());
		this.expiryTimestampSeconds = expTs;
		this.priority = prio;
		this.host = host;
		this.port = port;
	}

	/**
	 * Serialize record to Protobuf message.
	 *
	 * @param writer writer to serialize record to
	 */
	void toProtobuf(ProtobufWriter writer) {
		writer.writeFixed64(EXPTS, this.expiryTimestampSeconds);
		writer.writeInt32(PRIO, this.priority);
		writer.writeString(HOST, this.host);
		writer.writeInt32(PORT, this.port);
	}

	/**
	 * Test whether record is expired.
	 *
	 * @return {@code true} if, and only if, record is expired
	 */
	boolean isExpired() {
		return Time.durationBetween(Time.nowUtcSeconds(), this.expiryTimestampSeconds) == 0;
	}

	/**
	 * Duration, in seconds, since last failure.
	 *
	 * @return duration or {@link Long#MAX_VALUE} if no failure
	 */
	long lastFailureDurationSeconds() {
		long when = this.lastFailureTimestampSeconds;

		return when == 0 ? Long.MAX_VALUE : Time.durationBetween(when, Time.nowUptimeSeconds());
	}

	/**
	 * Monotonic timestamp of last failure.
	 *
	 * @return timestamp or {@code 0} if there has been no failure
	 */
	long lastFailureTimestampSeconds() {
		return this.lastFailureTimestampSeconds;
	}

	/**
	 * Update last failure timestamp.
	 */
	void updateLastFailureTimestamp() {
		this.lastFailureTimestampSeconds = Time.nowUptimeSeconds();
	}

	/**
	 * Clear last failure timestamp.
	 */
	void clearLastFailureTimestamp() {
		this.lastFailureTimestampSeconds = 0;
	}

	@Override
	public int hashCode() {
		return this.host.hashCode();
	}

	@Override
	public boolean equals(@Nullable Object that) {
		return (
			that instanceof RpcHostRecord &&
			this.host.equals(((RpcHostRecord) that).host) &&
			this.port == ((RpcHostRecord) that).port
		);
	}

	@Override
	public String toString() {
		if (!BuildConfig.DEBUG)
			return super.toString();
		return String.format(
			Locale.ROOT,
			"RpcHostRecord{" +
				"expiryTimestampSeconds=%s," +
				"priority=%s," +
				"host=%s," +
				"port=%s" +
			"}",
			this.expiryTimestampSeconds,
			this.priority,
			this.host,
			this.port
		);
	}
}
