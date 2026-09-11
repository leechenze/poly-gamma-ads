// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.antifraud;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.polygamma.android.origin.antifraud.CheckWire.DynamicEntropy_BLOB;
import static org.polygamma.android.origin.antifraud.CheckWire.FheEncapsulateRequest_PARAMS;
import static org.polygamma.android.origin.antifraud.CheckWire.IvtRatingUnknown;
import static org.polygamma.android.origin.antifraud.CheckWire.SessionAttestDevice;
import static org.polygamma.android.origin.antifraud.CheckWire.SessionBegin;
import static org.polygamma.android.origin.antifraud.CheckWire.SessionEnd;
import static org.polygamma.android.origin.antifraud.CheckWire.SessionFheDecrypt;
import static org.polygamma.android.origin.antifraud.CheckWire.SessionFheEncapsulate;
import static org.polygamma.android.origin.antifraud.CheckWire.SessionTamperMachine;

import android.content.Context;
import android.os.Build;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.Nullable;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageLite;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Sequence;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.polygamma.android.origin.adcom.AdCom;
import org.polygamma.android.origin.core.Origin;
import org.polygamma.android.origin.crypt.ChaCha20;
import org.polygamma.android.origin.crypt.Csprng;
import org.polygamma.android.origin.crypt.TorusFhe;
import org.polygamma.android.origin.crypt.Xtea;
import org.polygamma.android.origin.protobuf.ProtobufDecoder;
import org.polygamma.android.origin.util.Bits;
import org.polygamma.android.origin.util.Flate;
import org.polygamma.android.origin.util.Logger;
import org.polygamma.android.origin.util.Time;
import org.polygamma.origin.antifraud.IvtCheck;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * {@link CheckSession} tests.
 */
@RunWith(AndroidJUnit4.class)
public class CheckSessionTest {

	private static @Nullable CheckSessionResult nextCheckSessionResult(Random rand) {
		if (rand.nextBoolean())
			return null;

		CheckSessionResult rv = new CheckSessionResult(rand.nextInt(5000), new AntifraudStatus(
			new byte[rand.nextInt(32)],
			rand.nextInt(3),
			rand.nextInt(100)
		));

		if (rv.status.digest != null)
			rand.nextBytes(rv.status.digest);
		return rv;
	}

	private static ArrayMap<String, byte[]> nextSecretEntropies(Random rand) {
		int n = rand.nextInt(32);
		ArrayMap<String, byte[]> rv = new ArrayMap<>(n);

		for (int i = 0; i < n; i++) {
			String id = Integer.toString(i, 16);
			byte[] ent = new byte[rand.nextInt(32)];

			rand.nextBytes(ent);
			rv.put(id, ent);
		}
		return rv;
	}

	private static final class TestCase {

		final PlainEntropyTestCase plainTestCase;
		final @Nullable CheckSessionResult lastResult;
		final ArrayMap<String, byte[]> secretEntropies;
		final byte[] sessionId;

		TestCase(Random rand, Iterator<PlainEntropyTestCase> gen) {
			this.plainTestCase = gen.next();
			this.lastResult = nextCheckSessionResult(rand);
			this.secretEntropies = nextSecretEntropies(rand);
			this.sessionId = new byte[rand.nextInt(32)];
			rand.nextBytes(this.sessionId);
		}
	}

	private static Origin sdk;

	@BeforeClass
	public static void setupSdk() {
		Context ctxt = InstrumentationRegistry.getInstrumentation()
			.getTargetContext()
			.getApplicationContext();

		sdk = Origin.initialize(ctxt);
	}

	@AfterClass
	public static void destroySdk() throws InterruptedException {
		if (sdk != null) {
			sdk.shutdown();
			while (!sdk.awaitShutdown(10, TimeUnit.SECONDS))
				Log.w(EntropyCodingTest.class.getSimpleName(), "sdk shutdown taking longer than 10 seconds");
			assertTrue(sdk.isShutdown());
		}
		sdk = null;
	}

	private static CheckSession openSession(TestCase test) {
		CheckSession sess = CheckSession.open(
			sdk, null,
			test.lastResult,
			test.plainTestCase.regs, test.plainTestCase.device, test.plainTestCase.sensors,
			test.secretEntropies
		);

		// Session just began! It shouldn't have ended yet.
		assertFalse(sess.hasEnded());
		// We don't have a result yet.
		assertThrows(IllegalStateException.class, sess::result);
		assertNull(sess.result);
		// We haven't consumed arguments yet.
		assertThrows(IllegalStateException.class, () -> sess.apply(null));
		// Last result should be same as what we provided.
		assertSame(test.lastResult, sess.lastResult);
		// We don'thave an id yet, engine hasn't consumed any result yet.
		assertNull(sess.id);
		assertNull(sess.headOperation);
		// No decryption operation has been performed, secret entropy key should still be present.
		assertNotNull(sess.secretEntropyKey);
		// FHE encapsulation hasn't been performed yet.
		assertNull(sess.fhe);
		assertNull(sess.fheSecretKey);
		return sess;
	}

	private static IvtCheck.CheckArguments
	nextArguments(TestCase test, CheckSession sess, int expOps, boolean expEnd)
	throws InvalidProtocolBufferException {
		ByteBuffer args = sess.nextCheckArguments();

		// if we don't expect any operations then we don't expect any arguments either
		if (expOps == 0)
			assertNull(args);
		else
			assertNotNull(args);
		// subsequent arguments should also not be present since we haven't pushed a result yet
		assertNull(sess.nextCheckArguments());
		// session end should match
		assertEquals(expEnd, sess.hasEnded());
		if (expEnd)
			assertNotNull(sess.result);
		else
			assertThrows(IllegalStateException.class, sess::result);
		// last result should never change
		assertSame(test.lastResult, sess.lastResult);

		if (args == null)
			return null;

		IvtCheck.CheckArguments got = IvtCheck.CheckArguments.parseFrom(args);

		// request timestamp should be <1 second, but emulator introduces some noise
		assertEquals(Time.nowUtcSeconds(), got.getTimestampsec(), 5.);
		assertEquals(expOps, got.getOpCount());
		return got;
	}

	private static void endSessionEmpty(TestCase test, CheckSession sess)
	throws InvalidProtocolBufferException {
		sess.apply(null);
		assertTrue(sess.hasEnded());
		nextArguments(test, sess, 0, true);

		CheckSessionResult res = sess.result();

		// Ending session with empty result implies no change in fraud status
		assertEquals(CheckWire.MAX_RECHECK_DELAY_SECONDS, res.nextCheckDelaySeconds(), 5);
		// Digest for implicitly ended sessions is always non-null
		assertNotNull(res.status.digest);
		// If we have a previous result, status shouldmatch
		if (test.lastResult != null) {
			assertEquals(test.lastResult.status.confidence, res.status.confidence);
			assertEquals(test.lastResult.status.rating, res.status.rating);
			if (test.lastResult.status.digest != null)
				assertArrayEquals(test.lastResult.status.digest, res.status.digest);
		} else {
			assertEquals(0, res.status.confidence);
			assertEquals(IvtRatingUnknown, res.status.rating);
		}

		// calling apply again must fail
		assertThrows(IllegalStateException.class, () -> sess.apply(null));
	}

	private static void decryptSessionPayload(
		IvtCheck.SessionOperation op,
		MessageLite.Builder msg,
		Csprng sessCsprng
	) throws InvalidProtocolBufferException {
		byte[] key = new byte[Xtea.KEY_SIZE];
		byte[] payload = op.getPayload().toByteArray();

		// only successful operations have a payload that could be decrypted
		assertTrue(op.hasPayload());
		assertFalse(op.hasError());
		// payload should always be aligned to block size boundary
		assertEquals(0, payload.length % Xtea.BLOCK_SIZE);
		// length must include padding
		assertTrue(payload.length >= op.getPayloadpad());
		// if payload is empty session CSPRNG does not move forward
		if (payload.length > 0)
			sessCsprng.nextBytes(key, 0, Xtea.KEY_SIZE);
		Xtea.ofKey(key, 0)
			.decipher(payload, 0, payload, 0, payload.length);
		msg.mergeFrom(ByteString.copyFrom(
			ByteBuffer.wrap(payload, 0, payload.length - op.getPayloadpad())
		));
	}

	private static IvtCheck.SessionOperation
	sessionOperationOf(IvtCheck.SessionOpcode code, Object payload, Csprng sessCsprng) {
		byte[] data =
			payload instanceof MessageLite ? ((MessageLite) payload).toByteArray() :
			(byte[]) payload;
		int pad = 0;

		if (data.length > 0) {
			int blockRem = data.length % Xtea.BLOCK_SIZE;

			if (blockRem > 0) {
				pad = Xtea.BLOCK_SIZE - blockRem;
				data = Arrays.copyOfRange(data, 0, data.length + pad);
			}

			byte[] key = new byte[Xtea.KEY_SIZE];

			sessCsprng.nextBytes(key, 0, Xtea.KEY_SIZE);
			Xtea.ofKey(key, 0)
				.encipher(data, 0, data, 0, data.length);
		}
		return IvtCheck.SessionOperation.newBuilder()
			.setCode(code)
			.setPayload(ByteString.copyFrom(data))
			.setPayloadpad(pad)
			.build();
	}

	private static void checkSessionRequest(
		TestCase test,
		CheckSession sess,
		Csprng sessCsprng,
		IvtCheck.SessionOperation op
	) throws InvalidProtocolBufferException {
		assertEquals(SessionBegin, op.getCodeValue());

		IvtCheck.SessionRequest.Builder reqBuilder = IvtCheck.SessionRequest.newBuilder();

		decryptSessionPayload(op, reqBuilder, sessCsprng);

		IvtCheck.SessionRequest req = reqBuilder.build();

		assertEquals(AdCom.DOMAIN_VERSION, req.getAdcomver());
		assertArrayEquals(
			test.lastResult == null || test.lastResult.status.digest == null ? new byte[0] :
			test.lastResult.status.digest,
			req.getLastdigest().toByteArray()
		);
		assertFalse(req.hasPtent());
		assertTrue(req.hasPtentflate());
		test.plainTestCase.assertPlainEntropyEquals(IvtCheck.PlainEntropy.parseFrom(
			Flate.decompressZlib(req.getPtentflate().asReadOnlyByteBuffer(), true)
		));

		assertEquals(req.getCtentCount(), test.secretEntropies.size());

		ChaCha20 secretEntCipher = ChaCha20.ofKey(sess.secretEntropyKey, 0);

		for (int j = 0; j < test.secretEntropies.size(); j++) {
			IvtCheck.CipherEntropy ctEnt = req.getCtent(j);
			byte[] ctSchema = ctEnt.getSchema().toByteArray();
			byte[] ctContent = ctEnt.getCtcontent().toByteArray();

			assertEquals(test.secretEntropies.keyAt(j), ctEnt.getId());
			secretEntCipher.setCounter(0)
				.setNonce(j + 1)
				.xor(ctContent, 0, ctContent, 0, ctContent.length);

			ProtobufDecoder ctDec = ProtobufDecoder.ofSplit(ctSchema, ctContent);

			assertTrue(ctDec.hasRemaining());
			assertEquals(DynamicEntropy_BLOB, ctDec.decodeFieldTag());
			assertArrayEquals(test.secretEntropies.valueAt(j), ctDec.decodeByteArray());
			assertFalse(ctDec.hasRemaining());
		}
	}

	private static void
	pushCheckResult(TestCase test, CheckSession sess, IvtCheck.SessionOperation... ops) {
		sess.apply(ProtobufDecoder.of(
			IvtCheck.CheckResult.newBuilder()
				.setSessid(ByteString.copyFrom(test.sessionId))
				.addAllOp(Arrays.asList(ops))
				.build()
				.toByteArray()
		));
		// make sure session id matches
		assertArrayEquals(test.sessionId.length == 0 ? null : test.sessionId, sess.id);
	}

	// Opening a new session, and responding with an empty result should end the session.
	@Test
	public void testOpen() throws InvalidProtocolBufferException {
		Random rand = new Random(44);
		Csprng sessCsprng = Csprng.ofUnseeded();
		Iterator<PlainEntropyTestCase> gen = PlainEntropyTestCase.newGenerator(sdk, rand, true);

		for (int i = 0; i < 100; i++) {
			TestCase test = new TestCase(rand, gen);
			CheckSession sess = openSession(test);
			IvtCheck.CheckArguments args = nextArguments(test, sess, 1, false);

			assertNotNull(args);
			assertNull(sess.id);
			assertNotNull(sess.secretEntropyKey);
			assertNull(sess.fhe);
			assertNull(sess.fheSecretKey);
			assertNull(sess.headOperation);

			assertFalse(args.hasSessid());
			assertTrue(args.hasSesskeyseed());
			sessCsprng.reset()
				.mixEntropy(args.getSesskeyseed().toByteArray(), 0, args.getSesskeyseed().size())
				.reseed();

			checkSessionRequest(test, sess, sessCsprng, args.getOp(0));

			// An empty response should end the session.
			endSessionEmpty(test, sess);
		}
	}

	// Opening a new session, and responding with an erroneous result should end the session.
	@Test
	public void testOpenFail() throws InvalidProtocolBufferException {
		Random rand = new Random(44);
		Csprng sessCsprng = Csprng.ofUnseeded();
		Iterator<PlainEntropyTestCase> gen = PlainEntropyTestCase.newGenerator(sdk, rand, true);

		for (int i = 0; i < 100; i++) {
			TestCase test = new TestCase(rand, gen);
			CheckSession sess = openSession(test);
			IvtCheck.CheckArguments args = nextArguments(test, sess, 1, false);

			assertNotNull(args);
			assertNull(sess.id);
			assertNotNull(sess.secretEntropyKey);
			assertNull(sess.fhe);
			assertNull(sess.fheSecretKey);
			assertNull(sess.headOperation);

			assertFalse(args.hasSessid());
			assertTrue(args.hasSesskeyseed());
			sessCsprng.reset()
				.mixEntropy(args.getSesskeyseed().toByteArray(), 0, args.getSesskeyseed().size())
				.reseed();

			checkSessionRequest(test, sess, sessCsprng, args.getOp(0));

			// An empty response should end the session.
			pushCheckResult(
				test, sess,
				IvtCheck.SessionOperation.newBuilder()
					.setCode(IvtCheck.SessionOpcode.SessionBegin)
					.setError("oops")
					.build()
			);
			assertTrue(sess.hasEnded());
			assertNull(sess.nextCheckArguments());

			CheckSessionResult res = sess.result();

			assertEquals(CheckWire.ERROR_RECHECK_DELAY_SECONDS, res.nextCheckDelaySeconds(), 5);
			assertNotNull(res.status.digest);
			if (test.lastResult != null) {
				assertEquals(test.lastResult.status.confidence, res.status.confidence);
				assertEquals(test.lastResult.status.rating, res.status.rating);
				if (test.lastResult.status.digest != null)
					assertArrayEquals(test.lastResult.status.digest, res.status.digest);
			} else {
				assertEquals(0, res.status.confidence);
				assertEquals(IvtRatingUnknown, res.status.rating);
			}

			// calling apply again must fail
			assertThrows(IllegalStateException.class, () -> sess.apply(null));
		}
	}

	static void
	checkAttestDeviceResponse(byte[] challenge, IvtCheck.AttestDeviceResponse got)
	throws Exception {
		assertTrue(got.getCertCount() > 0);

		CertificateFactory factory = CertificateFactory.getInstance("X.509");
		List<X509Certificate> certs = new ArrayList<>();

		for (int i = 0; i < got.getCertCount(); i++) {
			try (InputStream in = got.getCert(i).newInput()) {
				certs.add((X509Certificate) factory.generateCertificate(in));
			}
		}

		if (challenge == null || challenge.length == 0)
			return;

		String oid = "1.3.6.1.4.1.11129.2.1.17";
		byte[] ext = certs.get(0).getExtensionValue(oid);

		assertNotNull(ext);

		ASN1OctetString envelope;
		ASN1Sequence keyDesc;

		try(ASN1InputStream asn1 = new ASN1InputStream(ext)) {
			envelope = (ASN1OctetString) asn1.readObject();
		}
		try (ASN1InputStream asn1 = new ASN1InputStream(envelope.getOctetStream())) {
			keyDesc = (ASN1Sequence) asn1.readObject();
		}

		assertArrayEquals(challenge, ((ASN1OctetString) keyDesc.getObjectAt(4)).getOctets());
	}

	private static void checkAttestDeviceResponse(
		byte[] challenge,
		IvtCheck.SessionOperation got,
		Csprng sessCsprng
	) throws Exception {
		assertEquals(SessionAttestDevice, got.getCodeValue());
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
			assertTrue(got.hasError());
			assertFalse(got.hasPayload());
			assertTrue(got.getError().contains("java.lang.UnsupportedOperationException"));
		} else {
			IvtCheck.AttestDeviceResponse.Builder respBuilder =
				IvtCheck.AttestDeviceResponse.newBuilder();

			decryptSessionPayload(got, respBuilder, sessCsprng);
			checkAttestDeviceResponse(challenge, respBuilder.build());
		}
	}

	// Device attestation should be fine >= N
	@Test
	public void testAttestDevice() throws Exception {
		Random rand = new Random(44);
		Csprng sessCsprng = Csprng.ofUnseeded();
		Iterator<PlainEntropyTestCase> gen = PlainEntropyTestCase.newGenerator(sdk, rand, true);

		for (int i = 0; i < 100; i++) {
			TestCase test = new TestCase(rand, gen);
			CheckSession sess = openSession(test);
			IvtCheck.CheckArguments args = nextArguments(test, sess, 1, false);

			assertNotNull(args);
			sessCsprng.reset()
				.mixEntropy(args.getSesskeyseed().toByteArray(), 0, args.getSesskeyseed().size())
				.reseed();

			checkSessionRequest(test, sess, sessCsprng, args.getOp(0));

			byte[] challenge = new byte[rand.nextInt(32)];

			rand.nextBytes(challenge);
			pushCheckResult(test, sess, sessionOperationOf(
				IvtCheck.SessionOpcode.SessionAttestDevice,
				IvtCheck.AttestDeviceRequest.newBuilder()
					.setChallenge(ByteString.copyFrom(challenge))
					.build(),
				sessCsprng
			));
			assertFalse(sess.hasEnded());

			args = nextArguments(test, sess, 1, false);
			assertNotNull(args);
			checkAttestDeviceResponse(challenge, args.getOp(0), sessCsprng);

			endSessionEmpty(test, sess);
			assertThrows(IllegalStateException.class, () -> pushCheckResult(
				test, sess,
				sessionOperationOf(
					IvtCheck.SessionOpcode.SessionAttestDevice,
					IvtCheck.AttestDeviceRequest.newBuilder()
						.setChallenge(ByteString.copyFrom(challenge))
						.build(),
					sessCsprng
				)
			));
		}
	}

	@Test
	public void testTamperMachine() throws Exception {
		Random rand = new Random(44);
		Csprng sessCsprng = Csprng.ofUnseeded();
		Iterator<PlainEntropyTestCase> gen = PlainEntropyTestCase.newGenerator(sdk, rand, true);

		for (int i = 0; i < 100; i++) {
			TestCase test = new TestCase(rand, gen);
			CheckSession sess = openSession(test);
			IvtCheck.CheckArguments args = nextArguments(test, sess, 1, false);

			assertNotNull(args);
			sessCsprng.reset()
				.mixEntropy(args.getSesskeyseed().toByteArray(), 0, args.getSesskeyseed().size())
				.reseed();

			checkSessionRequest(test, sess, sessCsprng, args.getOp(0));

			String expStr = UUID.randomUUID().toString();
			boolean expZ = rand.nextBoolean();
			int expI = rand.nextInt();
			long expJ = rand.nextLong();

			IvtCheck.TamperMachineRequest req =
				IvtCheck.TamperMachineRequest.newBuilder()
					.addOp(
						IvtCheck.TamperMachineOperation.newBuilder()
							.setCode(IvtCheck.TamperMachineOpcode.TamperMachinePush)
							.setStr(expStr)
							.build()
					)
					.addOp(
						IvtCheck.TamperMachineOperation.newBuilder()
							.setCode(IvtCheck.TamperMachineOpcode.TamperMachinePush)
							.setU32(expZ ? 1 : 0)
							.build()
					)
					.addOp(
						IvtCheck.TamperMachineOperation.newBuilder()
							.setCode(IvtCheck.TamperMachineOpcode.TamperMachinePush)
							.setU32(expI)
							.build()
					)
					.addOp(
						IvtCheck.TamperMachineOperation.newBuilder()
							.setCode(IvtCheck.TamperMachineOpcode.TamperMachinePush)
							.setU64(expJ)
							.build()
					)
					.addOp(
						IvtCheck.TamperMachineOperation.newBuilder()
							.setCode(IvtCheck.TamperMachineOpcode.TamperMachinePushApp)
							.build()
					)
					.addOp(
						IvtCheck.TamperMachineOperation.newBuilder()
							.setCode(IvtCheck.TamperMachineOpcode.TamperMachineWrite)
							.setStr("L")
							.build()
					)
					.addOp(
						IvtCheck.TamperMachineOperation.newBuilder()
							.setCode(IvtCheck.TamperMachineOpcode.TamperMachineWrite)
							.setStr("J")
							.build()
					)
					.addOp(
						IvtCheck.TamperMachineOperation.newBuilder()
							.setCode(IvtCheck.TamperMachineOpcode.TamperMachineWrite)
							.setStr("I")
							.build()
					)
					.addOp(
						IvtCheck.TamperMachineOperation.newBuilder()
							.setCode(IvtCheck.TamperMachineOpcode.TamperMachineWrite)
							.setStr("Z")
							.build()
					)
					.addOp(
						IvtCheck.TamperMachineOperation.newBuilder()
							.setCode(IvtCheck.TamperMachineOpcode.TamperMachinePop)
							.build()
					)
					.build();

			pushCheckResult(
				test, sess,
				sessionOperationOf(IvtCheck.SessionOpcode.SessionTamperMachine, req, sessCsprng)
			);
			assertFalse(sess.hasEnded());

			args = nextArguments(test, sess, 1, false);
			assertNotNull(args);

			IvtCheck.SessionOperation op = args.getOp(0);

			assertEquals(SessionTamperMachine, op.getCodeValue());
			assertFalse(op.hasError());
			assertTrue(op.hasPayload());

			IvtCheck.TamperMachineResponse.Builder respBuilder =
				IvtCheck.TamperMachineResponse.newBuilder();

			decryptSessionPayload(op, respBuilder, sessCsprng);

			IvtCheck.DynamicEntropy stack = respBuilder.build()
				.getStack();

			assertEquals(expZ, TamperMachine.Z);
			assertEquals(expI, TamperMachine.I);
			assertEquals(expJ, TamperMachine.J);
			assertEquals(test.plainTestCase.context, TamperMachine.L);
			assertEquals(Collections.singletonList(expStr), stack.getStrList());
			assertEquals(1, stack.getListseqCount());
			assertEquals(1, stack.getListseq(0));
			assertEquals(0, stack.getFlagCount());
			assertEquals(0, stack.getU32Count());
			assertEquals(0, stack.getU64Count());
			assertEquals(0, stack.getF32Count());
			assertEquals(0, stack.getF64Count());
			assertEquals(0, stack.getPflagCount());
			assertEquals(0, stack.getPu32Count());
			assertEquals(0, stack.getPu64Count());
			assertEquals(0, stack.getPf32Count());
			assertEquals(0, stack.getPf64Count());
			assertEquals(0, stack.getBlobCount());
			assertEquals(0, stack.getMsgCount());
			assertEquals(0, stack.getMapseqCount());
			assertEquals(0, stack.getNilCount());
			endSessionEmpty(test, sess);
			assertThrows(IllegalStateException.class, () -> pushCheckResult(
				test, sess,
				sessionOperationOf(IvtCheck.SessionOpcode.SessionTamperMachine, req, sessCsprng)
			));

			TamperMachine.Z = rand.nextBoolean();
			TamperMachine.I = rand.nextInt();
			TamperMachine.J = rand.nextLong();
			TamperMachine.L = expStr;
		}
	}

	private static byte[][] checkFheEncapsulateResponse(
		CheckSession sess,
		IvtCheck.SessionOperation got,
		Csprng sessCsprng
	) throws InvalidProtocolBufferException {
		assertEquals(SessionFheEncapsulate, got.getCodeValue());

		IvtCheck.FheEncapsulateResponse.Builder respBuilder =
			IvtCheck.FheEncapsulateResponse.newBuilder();

		decryptSessionPayload(got, respBuilder, sessCsprng);

		IvtCheck.FheEncapsulateResponse resp = respBuilder.build();
		TorusFhe fhe = sess.fhe;
		byte[] sk = sess.fheSecretKey;
		byte[] entKey = sess.secretEntropyKey;

		assertNotNull(fhe);
		assertNotNull(sk);
		// encapsulation should not have deleted entropy key
		assertNotNull(entKey);

		// Make sure parameters are valid
		assertEquals(fhe.dimension(), resp.getParams().getLwedim());
		assertEquals(fhe.logNoiseBound(), resp.getParams().getLognoiseb());
		assertEquals(fhe.messageModulus(), resp.getParams().getMsgmod());
		assertEquals(fhe.carryModulus(), resp.getParams().getCarrymod());

		// Make sure public key is well-formed
		byte[] pkMask = new byte[fhe.sizeOfScalarVector()];
		byte[] pkBody = new byte[fhe.sizeOfScalarVector()];

		assertEquals(fhe.dimension(), resp.getPkbodyCount());
		Csprng.ofSeed(resp.getPkmaskseed().toByteArray())
			.nextBytes(pkMask, 0, pkMask.length);
		for (int j = 0; j < resp.getPkbodyCount(); j++)
			Bits.storeLong(pkBody, j * 8, resp.getPkbody(j));

		// Make sure entropy key ciphertext is well-formed
		int numCt = fhe.plaintextCountOfDecomposedInt() * (ChaCha20.KEY_SIZE / 4);
		int numCtMask = fhe.ciphertextMaskListCountOf(numCt);
		byte[] ctEntKeyMask = new byte[numCtMask * fhe.sizeOfScalarVector()];
		byte[] ctEntKeyBody = new byte[numCt * 8];

		assertEquals(ctEntKeyMask.length, resp.getCtentkeymaskCount() * 8);
		assertEquals(ctEntKeyBody.length, resp.getCtentkeybodyCount() * 8);
		for (int j = 0; j < resp.getCtentkeymaskCount(); j++)
			Bits.storeLong(ctEntKeyMask, j * 8, resp.getCtentkeymask(j));
		for (int j = 0; j < resp.getCtentkeybodyCount(); j++)
			Bits.storeLong(ctEntKeyBody, j * 8, resp.getCtentkeybody(j));

		// Decrypt the encapsulated key to make sure it matches what we expect
		FheCompactListByteArrayDecryption decrypt =
			new FheCompactListByteArrayDecryption(
				fhe,
				sk, 0,
				ctEntKeyMask, 0,
				ctEntKeyBody, 0,
				numCt
			);

		for (int j = 0; j < (ChaCha20.KEY_SIZE / 4); j++) {
			assertEquals(
				Bits.loadIntLe(entKey, j * 4),
				decrypt.decryptAndRecomposeUnsignedInt()
			);
		}

		// Encapsulate the key again to make sure public key is for sk
		FheCompactListByteArrayEncryption encrypt =
			new FheCompactListByteArrayEncryption(
				fhe,
				pkMask, 0,
				pkBody, 0,
				ctEntKeyMask, 0,
				ctEntKeyBody, 0,
				numCt
			);

		for (int j = 0; j < (ChaCha20.KEY_SIZE / 4); j++) {
			encrypt.decomposeAndEncryptUnsignedInt(Bits.loadIntLe(entKey, j * 4));
		}
		encrypt.flush();

		decrypt = new FheCompactListByteArrayDecryption(
			fhe,
			sk, 0,
			ctEntKeyMask, 0,
			ctEntKeyBody, 0,
			numCt
		);
		for (int j = 0; j < (ChaCha20.KEY_SIZE / 4); j++) {
			assertEquals(
				Bits.loadIntLe(entKey, j * 4),
				decrypt.decryptAndRecomposeUnsignedInt()
			);
		}
		return new byte[][] { pkBody, pkMask };
	}

	@Test
	public void testFheEncapsulate() throws InvalidProtocolBufferException {
		Random rand = new Random(44);
		Csprng sessCsprng = Csprng.ofUnseeded();
		Iterator<PlainEntropyTestCase> gen = PlainEntropyTestCase.newGenerator(sdk, rand, true);

		for (int i = 0; i < 100; i++) {
			TestCase test = new TestCase(rand, gen);
			CheckSession sess = openSession(test);
			IvtCheck.CheckArguments args = nextArguments(test, sess, 1, false);

			assertNotNull(args);
			sessCsprng.reset()
				.mixEntropy(args.getSesskeyseed().toByteArray(), 0, args.getSesskeyseed().size())
				.reseed();

			checkSessionRequest(test, sess, sessCsprng, args.getOp(0));

			IvtCheck.FheParameters params = IvtCheck.FheParameters.newBuilder()
				.setLwedim(rand.nextBoolean() ? 4095 : 0)
				.setLognoiseb(rand.nextInt(17))
				.setMsgmod(2 + rand.nextInt(7))
				.setCarrymod(1 + rand.nextInt(8))
				.build();

			assertNull(sess.fhe);
			assertNull(sess.fheSecretKey);
			pushCheckResult(test, sess, sessionOperationOf(
				IvtCheck.SessionOpcode.SessionFheEncapsulate,
				IvtCheck.FheEncapsulateRequest.newBuilder()
					.setParams(params)
					.build(),
				sessCsprng
			));
			assertFalse(sess.hasEnded());
			// encapsulation is a moderate process and should only be executed on worker
			assertNotNull(sess.headOperation);
			assertNull(sess.headOperation.next);
			assertEquals(SessionFheEncapsulate, sess.headOperation.code);

			args = nextArguments(test, sess, 1, false);
			assertNotNull(args);
			assertNull(sess.headOperation);
			checkFheEncapsulateResponse(sess, args.getOp(0), sessCsprng);

			endSessionEmpty(test, sess);

			// This should fail since session has already ended.
			assertThrows(IllegalStateException.class, () -> pushCheckResult(
				test, sess,
				sessionOperationOf(
					IvtCheck.SessionOpcode.SessionFheEncapsulate,
					IvtCheck.FheEncapsulateRequest.newBuilder()
						.setParams(params)
						.build(),
					sessCsprng
				)
			));
		}
	}

	@Test
	public void testFheDecrypt() throws InvalidProtocolBufferException {
		Random rand = new Random(44);
		Csprng sessCsprng = Csprng.ofUnseeded();
		Iterator<PlainEntropyTestCase> gen = PlainEntropyTestCase.newGenerator(sdk, rand, true);

		for (int i = 0; i < 100; i++) {
			TestCase test = new TestCase(rand, gen);
			CheckSession sess = openSession(test);
			IvtCheck.CheckArguments args = nextArguments(test, sess, 1, false);

			assertNotNull(args);
			sessCsprng.reset()
				.mixEntropy(args.getSesskeyseed().toByteArray(), 0, args.getSesskeyseed().size())
				.reseed();

			checkSessionRequest(test, sess, sessCsprng, args.getOp(0));

			assertNull(sess.fhe);
			assertNull(sess.fheSecretKey);

			// We haven't performed encapsulation, so this should fail
			pushCheckResult(test, sess, sessionOperationOf(
				IvtCheck.SessionOpcode.SessionFheDecrypt,
				IvtCheck.FheDecryptRequest.newBuilder()
					.addCtbody(1)
					.addCtmask(1)
					.build(),
				sessCsprng
			));
			assertFalse(sess.hasEnded());

			args = nextArguments(test, sess, 1, false);
			assertNotNull(args);

			IvtCheck.SessionOperation op = args.getOp(0);

			assertEquals(SessionFheDecrypt, op.getCodeValue());
			assertTrue(op.hasError());
			assertFalse(op.hasPayload());
			assertTrue(op.getError().contains("java.lang.IllegalStateException"));

			pushCheckResult(test, sess, sessionOperationOf(
				IvtCheck.SessionOpcode.SessionFheEncapsulate,
				IvtCheck.FheEncapsulateRequest.getDefaultInstance(),
				sessCsprng
			));
			assertFalse(sess.hasEnded());

			args = nextArguments(test, sess, 1, false);
			assertNotNull(args);

			IvtCheck.FheEncapsulateResponse.Builder encapRespBuilder =
				IvtCheck.FheEncapsulateResponse.newBuilder();

			op = args.getOp(0);
			assertEquals(SessionFheEncapsulate, op.getCodeValue());
			decryptSessionPayload(op, encapRespBuilder, sessCsprng);

			IvtCheck.FheEncapsulateResponse encapResp = encapRespBuilder.build();

			TorusFhe fhe = sess.fhe;
			byte[] sk = sess.fheSecretKey;
			byte[] entKey = sess.secretEntropyKey;

			assertNotNull(fhe);
			assertNotNull(sk);
			// encapsulation should not have deleted entropy key
			assertNotNull(entKey);

			// Make sure parameters are valid
			assertEquals(fhe.dimension(), encapResp.getParams().getLwedim());
			assertEquals(fhe.logNoiseBound(), encapResp.getParams().getLognoiseb());
			assertEquals(fhe.messageModulus(), encapResp.getParams().getMsgmod());
			assertEquals(fhe.carryModulus(), encapResp.getParams().getCarrymod());

			// Make sure public key is well-formed
			byte[] pkMask = new byte[fhe.sizeOfScalarVector()];
			byte[] pkBody = new byte[fhe.sizeOfScalarVector()];

			assertEquals(fhe.dimension(), encapResp.getPkbodyCount());
			Csprng.ofSeed(encapResp.getPkmaskseed().toByteArray())
				.nextBytes(pkMask, 0, pkMask.length);
			for (int j = 0; j < encapResp.getPkbodyCount(); j++)
				Bits.storeLong(pkBody, j * 8, encapResp.getPkbody(j));

			long clear1 = rand.nextLong();
			long clear2 = rand.nextLong();
			int numPt = fhe.plaintextCountOfDecomposedLong() * 2;
			int numCtMask = fhe.ciphertextMaskListCountOf(numPt);
			byte[] ctMaskList = new byte[numCtMask * fhe.sizeOfScalarVector()];
			byte[] ctBodyList = new byte[numPt * 8];
			FheCompactListByteArrayEncryption encrypt = new FheCompactListByteArrayEncryption(
				fhe,
				pkMask, 0,
				pkBody, 0,
				ctMaskList, 0,
				ctBodyList, 0,
				numPt
			);

			encrypt.decomposeAndEncryptUnsignedLong(clear1)
				.decomposeAndEncryptUnsignedLong(clear2)
				.flush();

			IvtCheck.FheDecryptRequest.Builder reqBuilder =
				IvtCheck.FheDecryptRequest.newBuilder();

			for (int j = 0; j < ctMaskList.length; j += 8)
				reqBuilder.addCtmask(Bits.loadLong(ctMaskList, j));
			for (int j = 0; j < ctBodyList.length; j += 8)
				reqBuilder.addCtbody(Bits.loadLong(ctBodyList, j));

			IvtCheck.FheDecryptRequest req = reqBuilder.build();

			pushCheckResult(test, sess, sessionOperationOf(
				IvtCheck.SessionOpcode.SessionFheDecrypt,
				req,
				sessCsprng
			));
			assertFalse(sess.hasEnded());
			// decryption is a moderate process and should only be executed on worker
			assertNotNull(sess.headOperation);
			assertNull(sess.headOperation.next);
			assertEquals(SessionFheDecrypt, sess.headOperation.code);

			args = nextArguments(test, sess, 1, false);
			assertNotNull(args);
			assertNull(sess.headOperation);
			// everything should have been wiped
			assertNull(sess.secretEntropyKey);
			assertNull(sess.fheSecretKey);
			assertNull(sess.fhe);

			op = args.getOp(0);
			assertEquals(SessionFheDecrypt, op.getCodeValue());

			IvtCheck.FheDecryptResponse.Builder respBuilder =
				IvtCheck.FheDecryptResponse.newBuilder();

			decryptSessionPayload(op, respBuilder, sessCsprng);

			IvtCheck.FheDecryptResponse resp = respBuilder.build();
			int bitsPerMsg = 31 - Integer.numberOfLeadingZeros(fhe.messageModulus());
			int ptPerClear = fhe.plaintextCountOfDecomposedLong();
			long got = 0L;

			assertEquals(ptPerClear, resp.getPtCount());
			for (int j = 0; j < resp.getPtCount(); j++)
				got += resp.getPt(j) << (bitsPerMsg * j);
			assertEquals(clear1, got);

			// should not be able to decrypt again
			pushCheckResult(test, sess, sessionOperationOf(
				IvtCheck.SessionOpcode.SessionFheDecrypt,
				req,
				sessCsprng
			));
			assertFalse(sess.hasEnded());

			args = nextArguments(test, sess, 1, false);
			assertNotNull(args);

			op = args.getOp(0);
			assertEquals(IvtCheck.SessionOpcode.SessionFheDecrypt, op.getCode());
			assertTrue(op.hasError());
			assertFalse(op.hasPayload());
			assertTrue(op.getError().contains("java.lang.IllegalStateException"));

			// should not be able to encapsulate again
			pushCheckResult(test, sess, sessionOperationOf(
				IvtCheck.SessionOpcode.SessionFheEncapsulate,
				IvtCheck.FheEncapsulateRequest.getDefaultInstance(),
				sessCsprng
			));
			assertFalse(sess.hasEnded());

			args = nextArguments(test, sess, 1, false);
			assertNotNull(args);

			op = args.getOp(0);
			assertEquals(IvtCheck.SessionOpcode.SessionFheEncapsulate, op.getCode());
			assertTrue(op.hasError());
			assertFalse(op.hasPayload());
			assertTrue(op.getError().contains("java.lang.NullPointerException"));

			endSessionEmpty(test, sess);
			assertThrows(IllegalStateException.class, () -> pushCheckResult(
				test, sess,
				sessionOperationOf(IvtCheck.SessionOpcode.SessionFheDecrypt, req, sessCsprng)
			));
		}
	}

	@Test
	public void testEnd() throws InvalidProtocolBufferException {
		Random rand = new Random(44);
		Csprng sessCsprng = Csprng.ofUnseeded();
		Iterator<PlainEntropyTestCase> gen = PlainEntropyTestCase.newGenerator(sdk, rand, true);

		for (int i = 0; i < 100; i++) {
			TestCase test = new TestCase(rand, gen);
			CheckSession sess = openSession(test);
			IvtCheck.CheckArguments args = nextArguments(test, sess, 1, false);

			assertNotNull(args);

			assertFalse(args.hasSessid());
			assertTrue(args.hasSesskeyseed());
			sessCsprng.reset()
				.mixEntropy(args.getSesskeyseed().toByteArray(), 0, args.getSesskeyseed().size())
				.reseed();

			checkSessionRequest(test, sess, sessCsprng, args.getOp(0));

			int rating = rand.nextInt(3);
			int confidence = rand.nextInt(100);
			byte[] digest = new byte[rand.nextInt(32)];
			int nextCheckDelaySecs = rand.nextInt(864000);

			rand.nextBytes(digest);
			pushCheckResult(test, sess, sessionOperationOf(
				IvtCheck.SessionOpcode.SessionEnd,
				IvtCheck.SessionResult.newBuilder()
					.setRatingValue(rating)
					.setConf(confidence)
					.setDigest(ByteString.copyFrom(digest))
					.setRecktimestampsec(Time.nowUtcSeconds() + nextCheckDelaySecs)
					.build(),
				sessCsprng
			));
			nextArguments(test, sess, 0, true);

			CheckSessionResult res = sess.result();

			assertEquals(rating, res.status.rating);
			assertEquals(confidence, res.status.confidence);
			if (digest.length == 0)
				assertNull(res.status.digest);
			else
				assertArrayEquals(digest, res.status.digest);
			assertEquals(
				nextCheckDelaySecs < CheckWire.MAX_RECHECK_DELAY_SECONDS ?
				nextCheckDelaySecs :
				CheckWire.MAX_RECHECK_DELAY_SECONDS,
				res.nextCheckDelaySeconds(),
				5
			);
		}
	}

	@Test
	public void testFull() throws Exception {
		Random rand = new Random(44);
		Csprng sessCsprng = Csprng.ofUnseeded();
		Iterator<PlainEntropyTestCase> gen = PlainEntropyTestCase.newGenerator(sdk, rand, true);

		for (int i = 0; i < 100; i++) {
			TestCase test = new TestCase(rand, gen);
			CheckSession sess = openSession(test);
			IvtCheck.CheckArguments args = nextArguments(test, sess, 1, false);

			assertNotNull(args);
			sessCsprng.reset()
				.mixEntropy(args.getSesskeyseed().toByteArray(), 0, args.getSesskeyseed().size())
				.reseed();

			checkSessionRequest(test, sess, sessCsprng, args.getOp(0));

			byte[] challenge = new byte[rand.nextInt(32)];
			byte[] digest = new byte[rand.nextInt(32)];
			int rating = rand.nextInt(3);
			int confidence = rand.nextInt(100);
			int nextCheckDelaySecs = rand.nextInt(864000);
			long J = rand.nextLong();
			int I = rand.nextInt();
			List<IvtCheck.SessionOperation> ops = new ArrayList<>();

			rand.nextBytes(challenge);
			rand.nextBytes(digest);
			TamperMachine.J = J;
			TamperMachine.I = I;

			boolean endInline = rand.nextBoolean();
			IvtCheck.SessionResult end = IvtCheck.SessionResult.newBuilder()
				.setDigest(ByteString.copyFrom(digest))
				.setRatingValue(rating)
				.setConf(confidence)
				.setRecktimestampsec(Time.nowUtcSeconds() + nextCheckDelaySecs)
				.build();

			ops.add(
				IvtCheck.SessionOperation.newBuilder()
					.setCode(IvtCheck.SessionOpcode.SessionAttestDevice)
					.setPayload(
						IvtCheck.AttestDeviceRequest.newBuilder()
							.setChallenge(ByteString.copyFrom(challenge))
							.build()
							.toByteString()
					)
					.build()
			);
			ops.add(
				IvtCheck.SessionOperation.newBuilder()
					.setCode(IvtCheck.SessionOpcode.SessionTamperMachine)
					.setPayload(
						IvtCheck.TamperMachineRequest.newBuilder()
							.addOp(
								IvtCheck.TamperMachineOperation.newBuilder()
									.setCode(IvtCheck.TamperMachineOpcode.TamperMachineRead)
									.setStr("I")
									.build()
							)
							.addOp(
								IvtCheck.TamperMachineOperation.newBuilder()
									.setCode(IvtCheck.TamperMachineOpcode.TamperMachineRead)
									.setStr("J")
									.build()
							)
							.addOp(
								IvtCheck.TamperMachineOperation.newBuilder()
									.setCode(IvtCheck.TamperMachineOpcode.TamperMachineCall)
									.setStr("IJ(IJ)I")
									.build()
							)
							.addOp(
								IvtCheck.TamperMachineOperation.newBuilder()
									.setCode(IvtCheck.TamperMachineOpcode.TamperMachinePop)
									.build()
							)
							.build()
							.toByteString()
					)
					.build()
			);
			if (endInline) {
				ops.add(
					IvtCheck.SessionOperation.newBuilder()
						.setCode(IvtCheck.SessionOpcode.SessionEnd)
						.setPayload(end.toByteString())
						.build()
				);
			} else {
				ops.add(
					IvtCheck.SessionOperation.newBuilder()
						.setCode(IvtCheck.SessionOpcode.SessionFheEncapsulate)
						.setPayload(IvtCheck.FheEncapsulateRequest.getDefaultInstance().toByteString())
						.build()
				);
			}
			Collections.shuffle(ops, rand);
			for (int j = 0; j < ops.size(); j++) {
				ops.set(j, sessionOperationOf(
					ops.get(j).getCode(),
					ops.get(j).getPayload().toByteArray(),
					sessCsprng
				));
			}

			pushCheckResult(test, sess, ops.toArray(new IvtCheck.SessionOperation[0]));

			args = nextArguments(test, sess, ops.size() - (endInline ? 1 : 0), endInline);
			assertNotNull(args);

			TorusFhe fhe = sess.fhe;
			byte[] pkBody = null, pkMask = null;

			if (fhe == null) {
				assertTrue(endInline);
				// use default params
				fhe = TorusFhe.ofDimension(1024, 4, 4, 42, Csprng.ofSeed(SecureRandom.getSeed(16)));
			}
			for (int j = 0, k = 0; j < ops.size(); j++) {
				IvtCheck.SessionOperation exp = ops.get(j);

				if (exp.getCodeValue() == SessionEnd)
					continue;

				IvtCheck.SessionOperation got = args.getOp(k++);

				assertEquals(exp.getCode(), got.getCode());
				if (exp.getCodeValue() == SessionAttestDevice) {
					checkAttestDeviceResponse(challenge, got, sessCsprng);
				} else if (exp.getCodeValue() == SessionFheEncapsulate) {
					byte[][] pkBodyAndMask = checkFheEncapsulateResponse(sess, got, sessCsprng);

					pkBody = pkBodyAndMask[0];
					pkMask = pkBodyAndMask[1];
				} else {
					assertEquals(SessionTamperMachine, got.getCodeValue());
					assertEquals(J, TamperMachine.J);
					assertEquals(I, TamperMachine.I);

					IvtCheck.TamperMachineResponse.Builder respBuilder =
						IvtCheck.TamperMachineResponse.newBuilder();

					decryptSessionPayload(got, respBuilder, sessCsprng);

					IvtCheck.DynamicEntropy stack = respBuilder.build()
						.getStack();

					assertEquals(1, stack.getU32Count());
					assertEquals(TamperMachine.IJ(I, J), stack.getU32(0));
					assertEquals(1, stack.getListseqCount());
					assertEquals(1, stack.getListseq(0));
					assertEquals(0, stack.getFlagCount());
					assertEquals(0, stack.getU64Count());
					assertEquals(0, stack.getF32Count());
					assertEquals(0, stack.getF64Count());
					assertEquals(0, stack.getPflagCount());
					assertEquals(0, stack.getPu32Count());
					assertEquals(0, stack.getPu64Count());
					assertEquals(0, stack.getPf32Count());
					assertEquals(0, stack.getPf64Count());
					assertEquals(0, stack.getBlobCount());
					assertEquals(0, stack.getStrCount());
					assertEquals(0, stack.getMsgCount());
					assertEquals(0, stack.getMapseqCount());
					assertEquals(0, stack.getNilCount());
				}
			}


			if (!endInline) {
				assertNotNull(pkBody);
				assertNotNull(pkMask);

				long clear = rand.nextLong();
				int ptPerLong = fhe.plaintextCountOfDecomposedLong();
				int numPt = ptPerLong * 2;
				int numCtMask = fhe.ciphertextMaskListCountOf(numPt);
				byte[] ctMaskList = new byte[numCtMask * fhe.sizeOfScalarVector()];
				byte[] ctBodyList = new byte[numPt * 8];
				FheCompactListByteArrayEncryption encrypt = new FheCompactListByteArrayEncryption(
					fhe,
					pkMask, 0,
					pkBody, 0,
					ctMaskList, 0,
					ctBodyList, 0,
					numPt
				);

				encrypt.decomposeAndEncryptUnsignedLong(clear)
					.decomposeAndEncryptUnsignedLong(rand.nextLong())
					.flush();

				IvtCheck.FheDecryptRequest.Builder reqBuilder =
					IvtCheck.FheDecryptRequest.newBuilder();

				for (int j = 0; j < ctMaskList.length; j += 8)
					reqBuilder.addCtmask(Bits.loadLong(ctMaskList, j));
				for (int j = 0; j < ctBodyList.length; j += 8)
					reqBuilder.addCtbody(Bits.loadLong(ctBodyList, j));

				pushCheckResult(
					test, sess,
					sessionOperationOf(
						IvtCheck.SessionOpcode.SessionEnd,
						end,
						sessCsprng
					),
					sessionOperationOf(
						IvtCheck.SessionOpcode.SessionFheDecrypt,
						reqBuilder.build(),
						sessCsprng
					)
				);
				args = nextArguments(test, sess, 1, true);
				assertNotNull(args);

				IvtCheck.SessionOperation dop = args.getOp(0);
				IvtCheck.FheDecryptResponse.Builder respBuilder =
					IvtCheck.FheDecryptResponse.newBuilder();

				assertEquals(SessionFheDecrypt, dop.getCodeValue());
				decryptSessionPayload(dop, respBuilder, sessCsprng);

				IvtCheck.FheDecryptResponse resp = respBuilder.build();
				int bitsPerMsg = 31 - Integer.numberOfLeadingZeros(fhe.messageModulus());
				int ptPerClear = fhe.plaintextCountOfDecomposedLong();
				long got = 0L;

				assertEquals(ptPerClear, resp.getPtCount());
				for (int j = 0; j < resp.getPtCount(); j++)
					got += resp.getPt(j) << (bitsPerMsg * j);
				assertEquals(clear, got);
			}

			nextArguments(test, sess, 0, true);
			assertNull(sess.secretEntropyKey);
			assertNull(sess.fheSecretKey);
			assertNull(sess.fhe);

			CheckSessionResult res = sess.result();

			assertEquals(rating, res.status.rating);
			assertEquals(confidence, res.status.confidence);
			if (digest.length == 0)
				assertNull(res.status.digest);
			else
				assertArrayEquals(digest, res.status.digest);
			assertEquals(
				nextCheckDelaySecs < CheckWire.MAX_RECHECK_DELAY_SECONDS ?
				nextCheckDelaySecs :
				CheckWire.MAX_RECHECK_DELAY_SECONDS,
				res.nextCheckDelaySeconds(),
				5
			);
		}
	}
}
