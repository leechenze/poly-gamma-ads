// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.antifraud;

import static android.security.keystore.KeyProperties.PURPOSE_SIGN;
import static android.security.keystore.KeyProperties.PURPOSE_VERIFY;
import static org.polygamma.android.origin.antifraud.CheckWire.AttestDeviceRequest_CHALLENGE;
import static org.polygamma.android.origin.antifraud.CheckWire.AttestDeviceResponse_CERT;
import static org.polygamma.android.origin.antifraud.CheckWire.CheckArguments_ERROR;
import static org.polygamma.android.origin.antifraud.CheckWire.CheckArguments_OP;
import static org.polygamma.android.origin.antifraud.CheckWire.CheckArguments_SESSID;
import static org.polygamma.android.origin.antifraud.CheckWire.CheckArguments_SESSKEYSEED;
import static org.polygamma.android.origin.antifraud.CheckWire.CheckArguments_TIMESTAMPSEC;
import static org.polygamma.android.origin.antifraud.CheckWire.CheckResult_OP;
import static org.polygamma.android.origin.antifraud.CheckWire.CheckResult_SESSID;
import static org.polygamma.android.origin.antifraud.CheckWire.FheDecryptRequest_CTBODY;
import static org.polygamma.android.origin.antifraud.CheckWire.FheDecryptRequest_CTMASK;
import static org.polygamma.android.origin.antifraud.CheckWire.FheDecryptResponse_PT;
import static org.polygamma.android.origin.antifraud.CheckWire.FheEncapsulateRequest_PARAMS;
import static org.polygamma.android.origin.antifraud.CheckWire.FheEncapsulateResponse_CTENTKEYBODY;
import static org.polygamma.android.origin.antifraud.CheckWire.FheEncapsulateResponse_CTENTKEYMASK;
import static org.polygamma.android.origin.antifraud.CheckWire.FheEncapsulateResponse_PARAMS;
import static org.polygamma.android.origin.antifraud.CheckWire.FheEncapsulateResponse_PKBODY;
import static org.polygamma.android.origin.antifraud.CheckWire.FheEncapsulateResponse_PKMASKSEED;
import static org.polygamma.android.origin.antifraud.CheckWire.FheParameters_CARRYMOD;
import static org.polygamma.android.origin.antifraud.CheckWire.FheParameters_LOGNOISEB;
import static org.polygamma.android.origin.antifraud.CheckWire.FheParameters_LWEDIM;
import static org.polygamma.android.origin.antifraud.CheckWire.FheParameters_MSGMOD;
import static org.polygamma.android.origin.antifraud.CheckWire.IvtRatingUnknown;
import static org.polygamma.android.origin.antifraud.CheckWire.MAX_RECHECK_DELAY_SECONDS;
import static org.polygamma.android.origin.antifraud.CheckWire.SessionAttestDevice;
import static org.polygamma.android.origin.antifraud.CheckWire.SessionBegin;
import static org.polygamma.android.origin.antifraud.CheckWire.SessionEnd;
import static org.polygamma.android.origin.antifraud.CheckWire.SessionFheDecrypt;
import static org.polygamma.android.origin.antifraud.CheckWire.SessionFheEncapsulate;
import static org.polygamma.android.origin.antifraud.CheckWire.SessionRequest_ADCOMVER;
import static org.polygamma.android.origin.antifraud.CheckWire.SessionRequest_CTENT;
import static org.polygamma.android.origin.antifraud.CheckWire.SessionRequest_LASTDIGEST;
import static org.polygamma.android.origin.antifraud.CheckWire.SessionRequest_PTENTFLATE;
import static org.polygamma.android.origin.antifraud.CheckWire.SessionTamperMachine;

import android.content.Context;
import android.os.Build;
import android.os.PerformanceHintManager;
import android.os.Process;
import android.os.SystemClock;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;

import org.polygamma.android.origin.adcom.AdCom;
import org.polygamma.android.origin.adcom.context.Device;
import org.polygamma.android.origin.adcom.context.Regs;
import org.polygamma.android.origin.antifraud.CheckWire.SessionOpcode;
import org.polygamma.android.origin.core.Origin;
import org.polygamma.android.origin.crypt.Ascon;
import org.polygamma.android.origin.crypt.ChaCha20;
import org.polygamma.android.origin.crypt.Csprng;
import org.polygamma.android.origin.crypt.TorusFhe;
import org.polygamma.android.origin.crypt.Xtea;
import org.polygamma.android.origin.protobuf.ProtobufDecoder;
import org.polygamma.android.origin.protobuf.ProtobufEncoder;
import org.polygamma.android.origin.util.AndroidContexts;
import org.polygamma.android.origin.util.Bits;
import org.polygamma.android.origin.util.Flate;
import org.polygamma.android.origin.util.Function;
import org.polygamma.android.origin.util.Logger;
import org.polygamma.android.origin.util.Preconditions;
import org.polygamma.android.origin.util.Supplier;
import org.polygamma.android.origin.util.Time;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.Deflater;

/**
 * IVT check session engine.
 * <p>Sessions can be constructed using
 * {@link #open(Origin, CurrentActivityReference, CheckSessionResult, Regs, Device, SensorEntropyService, Map)}.
 * An open session will produce {@linkplain #nextCheckArguments() arguments} to invoke the IVT
 * check {@linkplain CheckWire#RPC_PROCEDURE_ID procedure} with. Results, empty or
 * otherwise, from the procedure must be used to invoke {@link #apply(ProtobufDecoder)}. The
 * engine will continue producing results until a session {@linkplain #hasEnded() ends}, and a
 * check {@linkplain #result() result} generated.
 * {@snippet :
 * CheckSession sess = CheckSession.open(sdk, actRef, lastRes, regs, dev, sensors, secretsEnc);
 *
 * for (ByteBuffer args; (args = sess.nextCheckArguments()) != null;) { // @link substring="nextCheckArguments" target="#nextCheckArguments()"
 *     ByteBuffer result = invokeRemoteCheckProcedure(args);
 *
 * 	   // Session may have ended, in which case, there is no point in applying anything.
 * 	   if (sess.hasEnded()) // @link substring="hasEnded" target="#hasEnded()"
 *         continue;
 * 	   if (result == null || !result.hasRemaining()) {
 *         sess.apply(null); // @link substring="apply" target="#apply(ProtobufDecoder)"
 *     } else {
 *         sess.apply(ProtobufDecoder.ofBuffer(result));
 *     }
 * }
 *
 * CheckSessionResult res = sess.result(); // @link substring="result" target="#result()"
 * }
 */
@SuppressWarnings("JavadocDeclaration")
final class CheckSession implements Function<ProtobufDecoder, CheckSession> {

	private static final String TAG = CheckSession.class.getSimpleName();

	// Generate a shared CSPRNG seed, given some 256-bit input `x`. See `beginSession()`.
	@VisibleForTesting
	static void generateSharedCsprngSeed(byte[] x) {
		/*
		 *                0-63 (t0)       64-127 (t1)
		 *              +---------------+---------------+
		 *  0- 63 (t0') | Identity      | Diagonal + 13 |
		 *              |               | Shifted + 17  |
		 *              +---------------+---------------+
		 * 64-127 (t1') | Diagonal + 37 | Identity      |
		 *              | Shifted + 23  |               |
		 *              +---------------+---------------+
		 *
		 * The identity diagonals are important so that all input bits flow directly through the
		 * mixer, ensuring nothing from the input is lost or muted. Off-diagonal shift blocks are
		 * used to enforce cross-half communication (iow change in right half flashes into left
		 * half via 13-bit cyclic rotation and viseversa via the 37-bit rotation).
		 */
		long s0 = Bits.loadLongLe(x,  0);
		long s1 = Bits.loadLongLe(x,  8);
		long s2 = Bits.loadLongLe(x, 16);
		long s3 = Bits.loadLongLe(x, 24);
		long i0, i1, i2, i3;
		Ascon ascon = Ascon.ofHash();

		for (String str : new String[] {
			Build.DISPLAY,
			Build.FINGERPRINT,
			Build.HARDWARE,
			Build.PRODUCT,
			Build.TAGS
		}) {
			ascon.updateHash(str.getBytes(StandardCharsets.UTF_8));
		}

		ascon.finishHash(x, 0);
		i0 = Bits.loadLongLe(x,  0);
		i1 = Bits.loadLongLe(x,  8);
		i2 = Bits.loadLongLe(x, 16);
		i3 = Bits.loadLongLe(x, 24);

		long x0 = i1 ^ s0;
		long x1 = i2 ^ s1;
		long y0, y1;
		long t0, t1;

		y0 = x0 ^ ((x1 << 13) | (x1 >>> 51));
		y1 = x1 ^ ((y0 << 37) | (y0 >>> 27));
		y0 += y1 * 0xff51afd7ed558ccdL;
		y1 += y0 * 0xc4ceb9fe1a85ec53L;

		t0 = i0 ^ ((i3 << 13) | (i3 >>> 51));
		t1 = i3 ^ ((t0 << 37) | (t0 >>> 27));
		t0 += t1 * 0xff51afd7ed558ccdL;
		t1 += t0 * 0xc4ceb9fe1a85ec53L;

		y0 ^= s2 ^ t0;
		y1 ^= s3 ^ t1;

		t0 = y0 ^ ((y1 << 13) | (y1 >>> 51));
		t1 = y1 ^ ((t0 << 37) | (t0 >>> 27));
		t0 += t1 * 0xff51afd7ed558ccdL;
		t1 += t0 * 0xc4ceb9fe1a85ec53L;

		long h0 = x0 ^ t0;
		long h1 = x1 ^ t1;

		t0 = h0 ^ ((h1 << 13) | (h1 >>> 51));
		t1 = h1 ^ ((t0 << 37) | (t0 >>> 27));
		t0 += t1 * 0xff51afd7ed558ccdL;
		t1 += t0 * 0xc4ceb9fe1a85ec53L;

		long h2 = y0 ^ t0;
		long h3 = y1 ^ t1;

		Bits.storeLongLe(x,  0, h0);
		Bits.storeLongLe(x,  8, h1);
		Bits.storeLongLe(x, 16, h2);
		Bits.storeLongLe(x, 24, h3);
	}

	/**
	 * Open new check session.
	 * <p>This constructs a new session, initialized for invoking the check procedure with
	 * initial arguments to begin the session. The initial check arguments can be accessed using
	 * {@link #nextCheckArguments()}. Each result from an invocation of the check procedure should
	 * be used to {@linkplain #apply(ProtobufDecoder) update} the returned check session. Every
	 * invocation of update should be followed by {@link #nextCheckArguments()}. If {@code
	 * nextCheckArguments}, returns a non-{@code null} value, then the check procedure must be
	 * invoked again, with the arguments returned by {@code nextCheckArguments}; otherwise, the
	 * session can be considered complete, and its result may be accessed using {@link
	 * #result()}.
	 *
	 * @param sdk owning SDK
	 * @param actRef  reference to current activity, if any
	 * @param lastRes last check result, if any
	 * @param regs description of laws and regulations applicable to device
	 * @param dev description of device for which check is being performed
	 * @param sensors service to capture sensor entropy from, if any
	 * @param secretEnts mapping of secret entropy name to respective entropy value
	 * @return resulting session
	 */
	@WorkerThread
	static CheckSession open(
		Origin sdk,
		@Nullable CurrentActivityReference actRef,
		@Nullable CheckSessionResult lastRes,
		Regs regs, Device dev, @Nullable SensorEntropyService sensors,
		Map<String, ?> secretEnts
	) {
		/*
		 * `SecureRandom`, `/dev/urandom`, and `/dev/random` are all broken in some way on older
		 * devices [1]. We're going to assume we can only get a small amount of high-quality
		 * entropy from the os, during testing that seems to be about 16-bits. We'll use the
		 * original high-quality entropy to seed our secret CSPRNG. The shared CSPRNG we'll seed
		 * with high-quality entropy frobbed with some low-quality entropy, since some versions of
		 * the IVT service expect 256-bit+ seed. At this point, the secret CSPRNG will have been
		 * seeded with 128-bits of high-quality entropy, before we use it to encipher secure check
		 * entropy, we'll mix in at least an additional 128-bits of high quality entropy from
		 * things like sensors. After that, we're good to use the secret CSPRNG.
		 *
		 * [1] https://android-developers.googleblog.com/2013/08/some-securerandom-thoughts.html
		 */
		byte[] secret = SecureRandom.getSeed(16);
		byte[] shared = new byte[32];

		Bits.storeIntLe(shared, 0, Process.myPid());
		Bits.storeIntLe(shared, 4, Process.myUid());
		Bits.storeLongLe(shared, 8, SystemClock.elapsedRealtimeNanos());
		System.arraycopy(secret, 0, shared, 16, 16);
		generateSharedCsprngSeed(shared);

		CheckSession sess = new CheckSession(sdk, actRef, lastRes);

		EntropyCoding.encodePlain(
			sess.argumentsEncoder,
			sdk.tryContext(),
			sdk.app(), regs, dev, sensors
		);
		sess.begin(secretEnts, secret, shared);
		return sess;
	}

	// Owning SDK.
	private final Origin sdk;

	// Reference to current activity.
	private final @Nullable CurrentActivityReference currentActivityReference;

	// CSPRNG we use for secret random generation. This is not shared with remote service.
	private final Csprng secretCsprng;

	// CSPRNG we keep in sync with remote service.
	private final Csprng sharedCsprng;

	// Last check result, if any.
	@VisibleForTesting
	final @Nullable CheckSessionResult lastResult;

	// Current check result. This is set only when session has ended.
	@VisibleForTesting
	@Nullable CheckSessionResult result;

	// Service assigned session id. This is `null` until service responds back with a session id.
	@VisibleForTesting
	@Nullable byte[] id;

	// Key used to encipher secret entropy. This is `null` after FHE decryption is performed.
	@VisibleForTesting
	@Nullable byte[] secretEntropyKey;

	/*
	 * When non-`null`, `secretEntropyKey` has been encapsulated within homomorphic space, and
	 * FHE decryption has not yet been performed.
	 */
	@VisibleForTesting
	@Nullable TorusFhe fhe;
	@VisibleForTesting
	@Nullable byte[] fheSecretKey;

	// Encoder used to encode check arguments.
	private ProtobufEncoder argumentsEncoder;

	// Chain of operations, from service, that we're meant to execute.
	@VisibleForTesting
	@Nullable CheckSessionOperation headOperation;

	private CheckSession(
		Origin sdk,
		@Nullable CurrentActivityReference actRef,
		@Nullable CheckSessionResult lastRes
	) {
		this.sdk = Preconditions.checkNotNull(sdk);
		this.currentActivityReference = actRef;
		this.secretCsprng = Csprng.ofUnseeded();
		this.sharedCsprng = Csprng.ofUnseeded();
		this.lastResult = lastRes;
		this.argumentsEncoder = ProtobufEncoder.of();
	}

	/*
	 * Execute `fn` within a performance session with expected runtime duration, in milliseconds,
	 * `expDurMs`.
	 */
	private <T> T withinPerformanceSession(long expDurMs, Supplier<T> fn) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S)
			return fn.get();

		Context ctxt = this.sdk.tryContext();
		PerformanceHintManager mgr = ctxt == null ? null : AndroidContexts.systemServiceOf(
			ctxt,
			PerformanceHintManager.class,
			Context.PERFORMANCE_HINT_SERVICE
		);

		if (mgr == null)
			return fn.get();

		PerformanceHintManager.Session sess = null;

		try {
			sess = mgr.createHintSession(
				new int[] { android.os.Process.myTid() },
				Math.max(TimeUnit.MILLISECONDS.toNanos(10), TimeUnit.MILLISECONDS.toNanos(expDurMs))
			);
		} catch (Exception cause) {
			Logger.debug(TAG, "failed to create performance session", cause);
		}

		if (sess == null)
			return fn.get();
		try {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM)
				sess.setPreferPowerEfficiency(true);

			long start = SystemClock.elapsedRealtimeNanos();
			T rv = fn.get();
			long end = SystemClock.elapsedRealtimeNanos();

			sess.reportActualWorkDuration(end - start);
			return rv;
		} finally {
			sess.close();
		}
	}

	// End session.
	private void end(CheckSessionResult res) {
		this.result = res;
	}

	// End session implicitly, due to either an error or empty response.
	private void endImplicit(long nextCkDelay) {
		byte[] digest = null;
		int rating = IvtRatingUnknown;
		int conf = 0;

		if (this.lastResult != null) {
			digest = this.lastResult.status.digest;
			rating = this.lastResult.status.rating;
			conf = this.lastResult.status.confidence;
		}

		if (digest == null) {
			// We don't have a digest, make one from the shared CSPRNG
			digest = new byte[Xtea.KEY_SIZE];
			this.sharedCsprng.nextBytes(digest, 0, Xtea.KEY_SIZE);
		}
		this.end(new CheckSessionResult(nextCkDelay, new AntifraudStatus(digest, rating, conf)));
	}

	// Decode `challenge` from `AttestDeviceRequest`.
	private static @Nullable byte[] decodeAttestDeviceRequestChallenge(ProtobufDecoder dec) {
		byte[] challenge = null;

		while (dec.hasRemaining()) {
			int tag = dec.decodeFieldTag();

			if (tag == AttestDeviceRequest_CHALLENGE)
				challenge = dec.decodeByteArray();
			else
				dec.skipFieldValue(tag);
		}
		return challenge;
	}

	// Perform device attestation.
	private void executeAttestDevice(ProtobufEncoder enc, ProtobufDecoder dec) throws Exception {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N)
			throw new UnsupportedOperationException();

		byte[] challenge = decodeAttestDeviceRequestChallenge(dec);
		Calendar expiryBuilder = Calendar.getInstance();

		expiryBuilder.add(Calendar.MINUTE, 15);

		Date expiry = expiryBuilder.getTime();
		String alias =
			Origin.VENDOR + '-' + Origin.NAME + "-antifraud-" + Time.nowRealtimeSeconds();
		KeyPairGenerator gen =
			KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore");
		KeyGenParameterSpec.Builder specBuilder =
			(new KeyGenParameterSpec.Builder(alias, PURPOSE_SIGN | PURPOSE_VERIFY))
				.setDigests(KeyProperties.DIGEST_SHA256)
				.setKeyValidityEnd(expiry)
				.setCertificateNotAfter(expiry);

		if (challenge != null && challenge.length > 0)
			specBuilder.setAttestationChallenge(challenge);
		gen.initialize(specBuilder.build());
		this.withinPerformanceSession(10, gen::generateKeyPair);

		KeyStore store = KeyStore.getInstance("AndroidKeyStore");

		store.load(null);
		try {
			for (Certificate cert : store.getCertificateChain(alias))
				enc.encodeByteArrayField(AttestDeviceResponse_CERT, cert.getEncoded());
		} finally {
			if (store.containsAlias(alias))
				store.deleteEntry(alias);
		}
	}

	// Initialize FHE engine and secret key given `FheEncapsulateRequest.params` decoded from `dec`.
	private TorusFhe initFhe(ProtobufDecoder dec) {
		int lweDim = 1024;
		int logNoiseB = 42;
		int msgMod = 4;
		int carryMod = 4;

		while (dec.hasRemaining()) {
			int tag = dec.decodeFieldTag();

			if (tag != FheEncapsulateRequest_PARAMS) {
				dec.skipFieldValue(tag);
				continue;
			}

			ProtobufDecoder params = ProtobufDecoder.ofBuffer(dec.decodeByteBufferView());

			while (params.hasRemaining()) {
				tag = params.decodeFieldTag();
				if (tag == FheParameters_LWEDIM)
					lweDim = params.decodeUint32();
				else if (tag == FheParameters_LOGNOISEB)
					logNoiseB = params.decodeUint32();
				else if (tag == FheParameters_MSGMOD)
					msgMod = params.decodeUint32();
				else if (tag == FheParameters_CARRYMOD)
					carryMod = params.decodeUint32();
				else
					params.skipFieldValue(tag);
			}
		}

		lweDim =
			Math.max(Math.min(lweDim & -lweDim, TorusFhe.MAX_DIMENSION), TorusFhe.MIN_DIMENSION);
		if (lweDim <= 1024) {
			/*
			 * LWE=1024; log2(b)=43
			 *
			 * usvp                 :: rop: ≈2^138.2, red: ≈2^138.2, δ: 1.004118, β: 381, d: 1930, tag: usvp
			 * bdd                  :: rop: ≈2^136.3, red: ≈2^136.0, svp: ≈2^134.0, β: 373, η: 403, d: 1892, tag: bdd
			 * dual                 :: rop: ≈2^141.9, mem: ≈2^91.4, m: 932, β: 390, d: 1956, ↻: 1, tag: dual
			 * dual_hybrid          :: rop: ≈2^132.8, red: ≈2^132.7, guess: ≈2^128.0, β: 357, p: 2, ζ: 0, t: 110, β': 368, N: ≈2^75.8, m: 1024
			 */
			lweDim = 1024;
			logNoiseB = Math.max(logNoiseB, 42);
		} else if (lweDim <= 2048) {
			/*
			 * LWE=2048; log2(b)=17
			 *
			 * usvp                 :: rop: ≈2^137.5, red: ≈2^137.5, δ: 1.004171, β: 374, d: 3901, tag: usvp
			 * bdd                  :: rop: ≈2^136.5, red: ≈2^136.1, svp: ≈2^134.3, β: 369, η: 404, d: 3952, tag: bdd
			 * dual                 :: rop: ≈2^139.6, mem: ≈2^89.7, m: 1962, β: 378, d: 4010, ↻: 1, tag: dual
			 * dual_hybrid          :: rop: ≈2^134.8, red: ≈2^134.7, guess: ≈2^128.0, β: 360, p: 2, ζ: 0, t: 110, β': 375, N: ≈2^77.3, m: ≈2^11.0
			 */
			lweDim = 2048;
			logNoiseB = Math.max(logNoiseB, 17);
		} else {
			/*
			 * LWE=4096; log2(b)=3
			 *
			 * usvp                 :: rop: ≈2^224.8, red: ≈2^224.8, δ: 1.002713, β: 684, d: 7791, tag: usvp
			 * bdd                  :: rop: ≈2^223.8, red: ≈2^223.7, svp: ≈2^219.6, β: 680, η: 710, d: 7898, tag: bdd
			 * dual                 :: rop: ≈2^227.8, mem: ≈2^152.4, m: ≈2^11.9, β: 691, d: 8011, ↻: 1, tag: dual
			 * dual_hybrid          :: rop: ≈2^219.3, red: ≈2^219.2, guess: ≈2^214.2, β: 660, p: 2, ζ: 10, t: 180, β': 660, N: ≈2^134.6, m: ≈2^12.0
			 */
			lweDim = Integer.highestOneBit(lweDim - 1) << 1;

			if (Integer.compareUnsigned(lweDim, TorusFhe.MAX_DIMENSION) > 0) {
				lweDim = TorusFhe.MAX_DIMENSION;
				logNoiseB = 3;
			} else {
				logNoiseB = Math.max(logNoiseB, 3);
			}
		}

		int fLweDim = lweDim;
		int fMsgMod = msgMod;
		int fCarryMod = carryMod;
		int fLogNoiseB = logNoiseB;

		this.fhe = this.withinPerformanceSession(50, () -> TorusFhe.ofDimension(
			fLweDim,
			fMsgMod, fCarryMod, fLogNoiseB,
			this.secretCsprng
		));
		this.fheSecretKey = new byte[this.fhe.sizeOfBinaryVector()];
		this.fhe.generateSecretKey(this.fheSecretKey, 0);
		return this.fhe;
	}

	/*
	 * Perform FHE encapsulation,
	 *
	 * 1. Initialize the FHE engine, given `FheEncapsulateRequest.params`, decoded from `dec`.
	 * 2. Generate FHE secret key, `fheSecretKey`.
	 * 3. Generate FHE public key.
	 * 4. Encapsulate `secretEntropyKey` using public key.
	 */
	private void executeFheEncapsulate(ProtobufEncoder enc, ProtobufDecoder dec) {
		//noinspection DataFlowIssue
		byte[] ctEntKey = Preconditions.checkNotNull(this.secretEntropyKey);
		TorusFhe fhe = this.initFhe(dec);

		enc.encodeLenField(
			FheEncapsulateResponse_PARAMS, fhe,
			(params, paramsEnc) ->
				paramsEnc.encodeUnsignedIntField(FheParameters_LWEDIM, params.dimension())
					.encodeUnsignedIntField(FheParameters_LOGNOISEB, params.logNoiseBound())
					.encodeUnsignedIntField(FheParameters_MSGMOD, params.messageModulus())
					.encodeUnsignedIntField(FheParameters_CARRYMOD, params.carryModulus())
		);

		/*
		 * We're going to encode `pkmaskseed`, `pkbody`, `ctentkeybody`, and `ctentkeymask`
		 * into `enc`. We want to reduce memory usage as much as possible, so we're not going to
		 * use any temporary arrays. We'll reserve space in `enc` for each field, and then we'll
		 * actually begin generating them directly into their respective space.
		 */
		int n = fhe.dimension();
		int nS = fhe.sizeOfScalarVector();
		int numPt = fhe.plaintextCountOfDecomposedInt() * (ChaCha20.KEY_SIZE / 4);
		int numCtMask = fhe.ciphertextMaskListCountOf(numPt);
		int pkMaskSeedOff, pkBodyOff, ctEntKeyBodyOff, ctEntKeyMaskOff;

		enc.encodeLenExactField(FheEncapsulateResponse_PKMASKSEED, Csprng.INPUT_ENTROPY_SIZE);
		pkMaskSeedOff = enc.arrayOffset() - Csprng.INPUT_ENTROPY_SIZE;

		enc.encodeLenExactField(FheEncapsulateResponse_PKBODY, nS);
		pkBodyOff = enc.arrayOffset() - nS;

		enc.encodeLenExactField(FheEncapsulateResponse_CTENTKEYBODY, numPt * 8);
		ctEntKeyBodyOff = enc.arrayOffset() - (numPt * 8);

		enc.encodeLenExactField(FheEncapsulateResponse_CTENTKEYMASK, numCtMask * nS);
		ctEntKeyMaskOff = enc.arrayOffset() - (numCtMask * nS);

		// Now the array underlying `enc` is stable, so begin generating.
		byte[] dst = enc.array();
		byte[] pkMask = new byte[nS];

		this.secretCsprng.nextBytes(dst, pkMaskSeedOff, Csprng.INPUT_ENTROPY_SIZE);
		fhe.generatePublicKeyMask(
			pkMask, 0,
			Csprng.ofSeed(dst, pkMaskSeedOff, Csprng.INPUT_ENTROPY_SIZE)
		);
		this.withinPerformanceSession(50, () -> {
			fhe.generatePublicKeyBody(dst, pkBodyOff, pkMask, 0, this.fheSecretKey, 0);
			return null;
		});

		FheCompactListByteArrayEncryption ctEntKeyEnc = new FheCompactListByteArrayEncryption(
			fhe, pkMask, 0, dst, pkBodyOff,
			dst, ctEntKeyMaskOff, dst, ctEntKeyBodyOff, numPt
		);

		for (int i = 0; i < ChaCha20.KEY_SIZE; i += 4)
			ctEntKeyEnc.decomposeAndEncryptUnsignedInt(Bits.loadIntLe(ctEntKey, i));
		this.withinPerformanceSession(50, () -> { ctEntKeyEnc.flush(); return null; });

		/*
		 * If we're not LE, we need to swap PK body samples and ciphertext mask coefficients, since
		 * Protobuf requires LE. We don't need to touch ciphertext bodies because they're already
		 * stored in LE.
		 */
		if (!Bits.LITTLE_ENDIAN) {
			Bits.copyLongsSwab(dst, pkBodyOff, dst, pkBodyOff, n);
			Bits.copyLongsSwab(dst, ctEntKeyBodyOff, dst, ctEntKeyBodyOff, n);
			Bits.copyLongsSwab(dst, ctEntKeyMaskOff, dst, ctEntKeyMaskOff, n * numCtMask);
		}
	}

	/*
	 * Perform FHE decryption,
	 *
	 * 1. Up to 64-bits of plaintext is decoded from `FheDecrypt`.
	 * 2. FHE is destroyed along with key used to encrypt `CipherEntropy`.
	 */
	private void executeFheDecrypt(ProtobufEncoder enc, ProtobufDecoder dec) {
		TorusFhe fhe = this.fhe;
		byte[] sk = this.fheSecretKey;
		int ctMaskOff = -1;
		int ctMaskLen = -1;
		int ctBodyOff = -1;
		int ctBodyLen = -1;

		Preconditions.checkState(fhe != null && sk != null);
		while (dec.hasRemaining()) {
			int tag = dec.decodeFieldTag();

			if (tag != FheDecryptRequest_CTMASK && tag != FheDecryptRequest_CTBODY) {
				dec.skipFieldValue(tag);
				continue;
			}

			ByteBuffer buff = dec.decodeByteBufferView();
			int len = buff.remaining();
			int off = buff.arrayOffset() + buff.position();

			if (tag == FheDecryptRequest_CTMASK) {
				ctMaskOff = off;
				ctMaskLen = len;
			} else {
				ctBodyOff = off;
				ctBodyLen = len;
			}
		}

		int numCt = ctBodyLen / 8;

		Preconditions.checkState((ctMaskLen % 8) == 0 && (ctBodyLen % 8) == 0);
		//noinspection DataFlowIssue
		Preconditions.checkFromIndexSize(
			0,
			fhe.sizeOfScalarVector() * fhe.ciphertextMaskListCountOf(numCt),
			ctMaskLen
		);

		// Decryption is limited to 64 bits.
		numCt = Math.min(numCt, fhe.plaintextCountOfDecomposedLong());
		if (numCt == 0)
			return; // nothing to decrypt

		FheCompactListByteArrayDecryption decryptList = new FheCompactListByteArrayDecryption(
			fhe, sk, 0,
			dec.array(), ctMaskOff,
			dec.array(), ctBodyOff,
			numCt
		);
		long[] list = new long[numCt];

		this.withinPerformanceSession(50, () -> {
			for (int i = 0; i < list.length; i++)
				list[i] = decryptList.decrypt();
			return list;
		});
		enc.encodePackedUint64ArrayField(FheDecryptResponse_PT, list);
		this.fhe = null;
		this.fheSecretKey = null;
		this.secretEntropyKey = null;
	}

	/*
	 * Execute an operation. When `worker` is `true`, this will execute any operation and return
	 * `true`; otherwise, this will only execute `op` if it will not block execution, returning
	 * `true` or `false` if `op` was executed or not, respectively.
	 *
	 * If this returns `true`, the payload for the operation will be encoded into `enc`.
	 */
	private boolean
	executeOperation(CheckSessionOperation op, ProtobufEncoder enc, boolean worker)
	throws Exception {
		ProtobufDecoder dec = ProtobufDecoder.of(
			op.payload(),
			op.payloadOffset,
			op.payloadLength - op.payloadPadSize
		);

		switch (op.code) {
		case SessionAttestDevice:
			this.executeAttestDevice(enc, dec);
			break;
		case SessionTamperMachine:
			if (!worker)
				return false;
			TamperMachine.generate(enc, dec, this.sdk, this.currentActivityReference);
			break;
		case SessionFheEncapsulate:
			if (!worker)
				return false;
			this.executeFheEncapsulate(enc, dec);
			break;
		case SessionFheDecrypt:
			if (!worker)
				return false;
			this.executeFheDecrypt(enc, dec);
			break;
		default:
			Preconditions.checkArgument(op.code == SessionEnd, "unsupported");
			if (!dec.hasRemaining()) {
				// No change since last status.
				this.endImplicit(MAX_RECHECK_DELAY_SECONDS);
			} else {
				CheckSessionResult res = CheckSessionResult.ofProtobuf(dec);

				// If result has a delay longer than what we support, clamp it.
				if (Long.compareUnsigned(
					res.nextCheckDelaySeconds(),
					MAX_RECHECK_DELAY_SECONDS
				) > 0) {
					res = res.withNextCheckDelaySeconds(MAX_RECHECK_DELAY_SECONDS);
				}
				this.end(res);
			}
			break;
		}
		return true;
	}

	// Consume next 128-bit cipher key for next session operation, returning cipher.
	private Xtea nextOperationCipher() {
		byte[] key = new byte[Xtea.KEY_SIZE];

		this.sharedCsprng.nextBytes(key, 0, Xtea.KEY_SIZE);
		return Xtea.ofKey(key, 0);
	}

	/*
	 * Encrypt `payload` and encode into `CheckArguments::op`. Upon return, `payload` *may* have
	 * been modified.
	 */
	private void encryptAndEncodeNextPayloadOperation(
		@SessionOpcode int code,
		byte[] payload, int off, int len
	) {
		Preconditions.checkFromIndexSize(off, len, payload.length);
		int numPad = 0;

		if (len > 0) {
			int numBlockRem = len % Xtea.BLOCK_SIZE;

			if (numBlockRem != 0) {
				numPad = Xtea.BLOCK_SIZE - numBlockRem;
				len += numPad;
				if ((off + len) > payload.length) {
					Logger.debug(TAG, "COPYING PAYLOAD");
					payload = Arrays.copyOfRange(payload, off, off + len);
					off = 0;
				}
			}
			this.nextOperationCipher()
				.encipher(payload, off, payload, off, len);
		}
		this.argumentsEncoder.encodeLenField(
			CheckArguments_OP,
			CheckSessionOperation.ofPayloadArray(code, payload, off, len, numPad),
			CheckSessionOperation::toProtobuf
		);
	}

	/*
	 * Execute any pending operations. When `worker` is `true`, *all* pending operations are
	 * executed; otherwise, only operations which can be invoked without blocking are executed.
	 */
	private void executeOperations(boolean worker) {
		CheckSessionOperation op = this.headOperation;

		if (op == null)
			return;

		ProtobufEncoder enc = ProtobufEncoder.of();

		do {
			try {
				if (op.isError()) {
					Logger.info(TAG, "operation %d failed: %s", op.code, op.error());
					if (op.code == SessionBegin || op.code == SessionEnd) {
						// If session begin or end fails then we need to retry later
						this.endImplicit(CheckWire.ERROR_RECHECK_DELAY_SECONDS);
					}
				} else {
					if (!this.executeOperation(op, enc.reset(), worker))
						break;
					if (op.code != SessionEnd) {
						this.encryptAndEncodeNextPayloadOperation(
							op.code,
							enc.array(), 0, enc.arrayOffset()
						);
					}
				}
			} catch (Exception cause) {
				if (op.code == SessionEnd)
					this.endImplicit(CheckWire.ERROR_RECHECK_DELAY_SECONDS);
				this.argumentsEncoder.encodeLenField(
					CheckArguments_OP,
					CheckSessionOperation.ofError(op.code, cause),
					CheckSessionOperation::toProtobuf
				);
				Logger.info(TAG, "failed to execute operation %s", op.code, cause);
			}
			op = op.next;
		} while (op != null);
		this.headOperation = op;
		if (op == null && this.argumentsEncoder.arrayOffset() > 0) {
			// we've consumed all of the operations, write out the remaining argument fields now
			this.argumentsEncoder.encodeByteArrayField(CheckArguments_SESSID, this.id)
				.encodeUnsignedLongField(CheckArguments_TIMESTAMPSEC, Time.nowUtcSeconds());
		}
		if (op == null && this.result != null) {
			// we're done, wipe out secrets
			this.secretEntropyKey = null;
			this.fheSecretKey = null;
			this.fhe = null;
		}
	}

	/*
	 * Generate new key for encrypting secret entropy, encrypt entropies from `ents` using the
	 * new key, and encode as `CipherEntropy` into `reqEnc`.
	 */
	private void encryptAndEncodeSecretEntropy(ProtobufEncoder reqEnc, Map<String, ?> ents) {
		byte[] key = new byte[ChaCha20.KEY_SIZE];

		this.secretEntropyKey = key;
		this.secretCsprng.nextBytes(key, 0, ChaCha20.KEY_SIZE);

		ChaCha20 cipher = ChaCha20.ofKey(key, 0);
		ProtobufEncoder entEnc = ProtobufEncoder.ofSplit();
		int numEnt = 0;

		for (Map.Entry<String, ?> ent : ents.entrySet()) {
			// encode the entropy value
			EntropyCoding.encodeDynamic(entEnc.reset(), ent.getValue());
			// mix the encoded value into our secret generator, it should be some juicy entropy
			this.secretCsprng.mixEntropy(entEnc.contentArray(), 0, entEnc.contentArrayOffset());
			// encipher the encoded value
			cipher.setCounter(0)
				.setNonce(++numEnt)
				.xor(
					entEnc.contentArray(), 0,
					entEnc.contentArray(), 0,
					entEnc.contentArrayOffset()
				);
			// now push it into the `SessionRequest`
			reqEnc.encodeLenField(
				SessionRequest_CTENT,
				new Pair<>(ent.getKey(), entEnc),
				(entNameAndEnc, dstEnc) -> EntropyCoding.encodeCipher(
					dstEnc,
					entNameAndEnc.first,
					entNameAndEnc.second
				),
				entEnc.schemaArrayOffset() + entEnc.contentArrayOffset()
			);
		}
		// If we encoded anything, then we mixed something into secret generator, so reseed it.
		if (numEnt != 0)
			this.secretCsprng.reseed();
	}

	/*
	 * Prepare `CheckArguments`, into `argumentsEncoder`, for beginning session. This allocates a
	 * new 256-bit ChaCha20 key to encipher `secretEnt`. Upon return, `{secret,shared}Csprng` are
	 * initialized with `{secret,shared}CsprngSeed`, respectively.
	 *
	 * NOTE: This assumes `argumentsEncoder` contains `PlainEntropy` upon entry!
	 */
	private void begin(
		Map<String, ?> secretEnts,
		byte[] secretCsprngSeed,
		byte[] sharedCsprngSeed
	) {
		ProtobufEncoder enc = this.argumentsEncoder;
		int plainLen = enc.arrayOffset();

		/*
		 * We don't need to reset the secret generator, since we're not sharing its seed. We
		 * do, however, want to mix in `plainEntFlate`, since it'll likely contain some good
		 * entropy from sensors/hardware.
		 */
		this.secretCsprng.mixEntropy(secretCsprngSeed, 0, secretCsprngSeed.length)
			.mixEntropy(enc.array(), 0, plainLen)
			.reseed();
		// we *do* need to reset the shared generator, since the service needs to match
		this.sharedCsprng.reset()
			.mixEntropy(sharedCsprngSeed, 0, sharedCsprngSeed.length)
			.reseed();

		/*
		 * Encode `SessionRequest` first, then we can encode the operation+arguments. Since
		 * `enc` contains `PlainEntropy` right now, encode `ptentflate` first so that we don't
		 * overwrite array underlying `enc` with `SessionRequest` before we deflate `ptent`.
		 */
		enc.reset()
			.encodeByteBufferField(
				SessionRequest_PTENTFLATE,
				Flate.compressZlib(
					ByteBuffer.wrap(enc.array(), 0, plainLen),
					Deflater.BEST_COMPRESSION,
					true
				)
			)
			.encodeStringField(SessionRequest_ADCOMVER, AdCom.DOMAIN_VERSION);
		if (this.lastResult != null)
			enc.encodeByteArrayField(SessionRequest_LASTDIGEST, this.lastResult.status.digest);
		this.encryptAndEncodeSecretEntropy(enc, secretEnts);

		/*
		 * `enc` now contains a `SessionRequest`, turn it into a `SessionOperation`, then encode
		 * `CheckArguments` into `enc`. We encode the session request first so that we can push
		 * remainder arguments into `enc` without overwriting session request.
		 */
		int sessReqLen = enc.arrayOffset();

		enc.reset();
		this.encryptAndEncodeNextPayloadOperation(SessionBegin, enc.array(), 0, sessReqLen);
		enc.encodeByteArrayField(CheckArguments_SESSKEYSEED, sharedCsprngSeed)
			.encodeUnsignedLongField(CheckArguments_TIMESTAMPSEC, Time.nowUtcSeconds());
	}

	/**
	 * Test whether session has ended.
	 *
	 * @return {@code true} if, and only if, session has ended
	 */
	boolean hasEnded() {
		return this.result != null;
	}

	/**
	 * Session result.
	 *
	 * @return result
	 * @throws IllegalStateException session has not yet {@linkplain #hasEnded() ended}
	 */
	@NonNull CheckSessionResult result() {
		Preconditions.checkState(this.result != null);
		return this.result;
	}

	/**
	 * Produce arguments for next check invocation.
	 *
	 * @return arguments to invoke check with or {@code null} no additional invocations are
	 * required
	 */
	@WorkerThread
	@Nullable ByteBuffer nextCheckArguments() {
		this.executeOperations(true);

		ByteBuffer rv = this.argumentsEncoder.asBuffer();

		if (!rv.hasRemaining())
			return null;
		this.argumentsEncoder = ProtobufEncoder.of();
		return rv;
	}

	// Decode `CheckResult` from `dec`.
	private void decodeCheckResult(ProtobufDecoder dec) {
		CheckSessionOperation headOp = null;
		CheckSessionOperation tailOp = null;

		while (dec.hasRemaining()) {
			int tag = dec.decodeFieldTag();

			if (tag == CheckResult_SESSID) {
				this.id = dec.decodeByteArray(this.id);
				Logger.debug(TAG, "new session id %s", Arrays.toString(this.id));
			} else if (tag == CheckResult_OP) {
				CheckSessionOperation op = dec.decodeLen(CheckSessionOperation::ofProtobuf);
				int len = op.payloadLength;

				if (headOp == null)
					headOp = op;
				if (tailOp != null)
					tailOp.next = op;
				tailOp = op;
				Logger.debug(TAG, "received operation %d", op.code);
				if (len == 0)
					continue;

				byte[] payload = op.payload();

				this.nextOperationCipher()
					.decipher(payload, op.payloadOffset, payload, op.payloadOffset, len);
			} else {
				dec.skipFieldValue(tag);
			}
		}
		this.headOperation = headOp;
	}

	@Override
	public CheckSession apply(@Nullable ProtobufDecoder dec) {
		Preconditions.checkState(
			this.result == null &&
			this.argumentsEncoder.arrayOffset() == 0 &&
			this.headOperation == null
		);

		if (dec == null || !dec.hasRemaining()) {
			// service responded with an empty result, meaning we can use our last result
			this.endImplicit(CheckWire.MAX_RECHECK_DELAY_SECONDS);
			return this;
		}
		try {
			this.decodeCheckResult(dec);
		} catch (Exception cause) {
			this.argumentsEncoder.encodeByteArrayField(CheckArguments_SESSID, this.id)
				.encodeUnsignedLongField(CheckArguments_TIMESTAMPSEC, Time.nowUtcSeconds())
				.encodeStringField(CheckArguments_ERROR, EntropyCoding.throwableToString(cause));
			this.headOperation = null;
			this.endImplicit(CheckWire.ERROR_RECHECK_DELAY_SECONDS);
			Logger.debug(TAG, "failed to decode check result", cause);
			return this;
		}

		this.executeOperations(false);

		/*
		 * if we have any operations remaining, we need to re-allocate them since the operations
		 * right now reference array underlying `dec`.
		 */
		CheckSessionOperation op = this.headOperation;
		CheckSessionOperation headOp = null;
		CheckSessionOperation tailOp = null;

		while (op != null) {
			CheckSessionOperation newTail;

			if (op.isError()) {
				newTail = op;
			} else {
				newTail = CheckSessionOperation.ofPayloadArray(
					op.code,
					Arrays.copyOfRange(
						op.payload(),
						op.payloadOffset,
						op.payloadOffset + op.payloadLength
					),
					op.payloadPadSize
				);
			}
			op = op.next;
			if (headOp == null)
				headOp = newTail;
			if (tailOp != null)
				tailOp.next = newTail;
			tailOp = newTail;
			tailOp.next = null;
		}
		this.headOperation = headOp;
		return this;
	}
}
