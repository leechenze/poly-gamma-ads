// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.antifraud;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Pair;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.protobuf.ByteString;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.InvalidProtocolBufferException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.polygamma.android.origin.adcom.context.Device;
import org.polygamma.android.origin.core.Origin;
import org.polygamma.android.origin.protobuf.ProtobufEncoder;
import org.polygamma.android.origin.util.Supplier;
import org.polygamma.origin.antifraud.IvtCheck;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * {@link EntropyCoding} tests.
 */
@RunWith(AndroidJUnit4.class)
public class EntropyCodingTest {

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

	@Test
	public void testEncodeAndroid() throws InvalidProtocolBufferException {
		Iterator<PlainEntropyTestCase> gen = PlainEntropyTestCase.newGenerator(sdk);
		ProtobufEncoder enc = ProtobufEncoder.of();

		for (int i = 0; i < 100; i++) {
			PlainEntropyTestCase test = gen.next();

			EntropyCoding.encodeAndroid(enc.reset(), test.context, test.sensors);
			test.assertAndroidEntropyEquals(IvtCheck.AndroidEntropy.parseFrom(enc.asBuffer()));
		}
	}

	@Test
	public void testEncodePlain() throws InvalidProtocolBufferException {
		Iterator<PlainEntropyTestCase> gen = PlainEntropyTestCase.newGenerator(sdk);
		ProtobufEncoder enc = ProtobufEncoder.of();

		for (int i = 0; i < 100; i++) {
			PlainEntropyTestCase test = gen.next();

			EntropyCoding.encodePlain(
				enc.reset(),
				test.context,
				test.app, test.regs, test.device,
				test.sensors
			);
			test.assertPlainEntropyEquals(IvtCheck.PlainEntropy.parseFrom(enc.asBuffer()));
		}
	}

	private static final class TestDynamicEntropyGenerator
	implements Iterator<Pair<Object, IvtCheck.DynamicEntropy>> {

		private final Random random = new Random(44);

		@Override
		public boolean hasNext() {
			return true;
		}

		private Object nextSimpleValue0(IvtCheck.DynamicEntropy.Builder ent) throws Exception {
			ByteArrayOutputStream protoBytes;
			CodedOutputStream proto;

			switch (this.random.nextInt(17)) {
			case 1:
				boolean flag = this.random.nextBoolean();

				ent.addFlag(flag);
				return flag;
			case 2:
				int u32 = this.random.nextInt();

				ent.addU32(u32);
				return u32;
			case 3:
				long u64 = this.random.nextLong();

				ent.addU64(u64);
				return u64;
			case 4:
				float f32 = this.random.nextFloat();

				ent.addF32(f32);
				return f32;
			case 5:
				double f64 = this.random.nextDouble();

				ent.addF64(f64);
				return f64;
			case 6:
				boolean[] pflag = new boolean[this.random.nextInt(32)];

				protoBytes = new ByteArrayOutputStream();
				proto = CodedOutputStream.newInstance(protoBytes);
				for (int i = 0; i < pflag.length; i++) {
					pflag[i] = this.random.nextBoolean();
					proto.writeBoolNoTag(pflag[i]);
				}
				proto.flush();
				ent.addPflag(ByteString.copyFrom(protoBytes.toByteArray()));
				return pflag;
			case 7:
				int[] pu32 = new int[this.random.nextInt(32)];

				protoBytes = new ByteArrayOutputStream();
				proto = CodedOutputStream.newInstance(protoBytes);
				for (int i = 0; i < pu32.length; i++) {
					pu32[i] = this.random.nextInt();
					proto.writeUInt32NoTag(pu32[i]);
				}
				proto.flush();
				ent.addPu32(ByteString.copyFrom(protoBytes.toByteArray()));
				return pu32;
			case 8:
				long[] pu64 = new long[this.random.nextInt(32)];

				protoBytes = new ByteArrayOutputStream();
				proto = CodedOutputStream.newInstance(protoBytes);
				for (int i = 0; i < pu64.length; i++) {
					pu64[i] = this.random.nextLong();
					proto.writeUInt64NoTag(pu64[i]);
				}
				proto.flush();
				ent.addPu64(ByteString.copyFrom(protoBytes.toByteArray()));
				return pu64;
			case 9:
				float[] pf32 = new float[this.random.nextInt(32)];

				protoBytes = new ByteArrayOutputStream();
				proto = CodedOutputStream.newInstance(protoBytes);
				for (int i = 0; i < pf32.length; i++) {
					pf32[i] = this.random.nextFloat();
					proto.writeFloatNoTag(pf32[i]);
				}
				proto.flush();
				ent.addPf32(ByteString.copyFrom(protoBytes.toByteArray()));
				return pf32;
			case 10:
				double[] pf64 = new double[this.random.nextInt(32)];

				protoBytes = new ByteArrayOutputStream();
				proto = CodedOutputStream.newInstance(protoBytes);
				for (int i = 0; i < pf64.length; i++) {
					pf64[i] = this.random.nextDouble();
					proto.writeDoubleNoTag(pf64[i]);
				}
				proto.flush();
				ent.addPf64(ByteString.copyFrom(protoBytes.toByteArray()));
				return pf64;
			case 11:
				byte[] blob = new byte[this.random.nextInt(32)];

				this.random.nextBytes(blob);
				ent.addBlob(ByteString.copyFrom(blob));
				return blob;
			case 12:
				String str = UUID.randomUUID().toString();

				ent.addStr(str);
				return str;
			case 13:
				Device msg = (PlainEntropyTestCase.newGenerator(sdk)).next().device;
				ProtobufEncoder msgEnc = ProtobufEncoder.of();

				msg.toProtobuf(msgEnc);
				ent.addMsg(ByteString.copyFrom(msgEnc.asBuffer()));
				return msg;
			case 14:
				ent.addListseq(2);
				return new Pair<>(this.nextSimpleValue(ent), this.nextSimpleValue(ent));
			default:
				ent.addNil(true);
				return null;
			}
		}

		private Object nextSimpleValue(IvtCheck.DynamicEntropy.Builder ent) {
			Object value;

			try {
				value = this.nextSimpleValue0(ent);
			} catch (Exception cause) {
				throw new AssertionError(cause);
			}
			return this.random.nextBoolean() ? ((Supplier<Object>) () -> value) : value;
		}

		@Override
		public Pair<Object, IvtCheck.DynamicEntropy> next() {
			int type = this.random.nextInt(3);
			IvtCheck.DynamicEntropy.Builder ent = IvtCheck.DynamicEntropy.newBuilder();
			Object value;

			if (type == 0) {
				value = this.nextSimpleValue(ent);
			} else if (type == 1) {
				Object[] vals = new Object[this.random.nextInt(32)];

				ent.addListseq(vals.length);
				for (int i = 0; i < vals.length; i++)
					vals[i] = this.nextSimpleValue(ent);
				value = this.random.nextBoolean() ? Arrays.asList(vals) : vals;
			} else {
				int n = this.random.nextInt(32);
				ArrayMap<Integer, Object> map = new ArrayMap<>(n);

				ent.addMapseq(n);
				for (int i = 0; i < n; i++) {
					ent.addU32(i);
					map.put(i, this.nextSimpleValue(ent));
				}
				value = map;
			}
			return new Pair<>(value, ent.build());
		}
	}

	private static void
	assertDynamicEntropyEquals(IvtCheck.DynamicEntropy exp, IvtCheck.DynamicEntropy got) {
		assertEquals(exp.getFlagList(), got.getFlagList());
		assertEquals(exp.getU32List(), got.getU32List());
		assertEquals(exp.getU64List(), got.getU64List());
		assertEquals(exp.getF32List(), got.getF32List());
		assertEquals(exp.getF64List(), got.getF64List());
		assertEquals(exp.getPflagList(), got.getPflagList());
		assertEquals(exp.getPu32List(), got.getPu32List());
		assertEquals(exp.getPu64List(), got.getPu64List());
		assertEquals(exp.getPf32List(), got.getPf32List());
		assertEquals(exp.getPf64List(), got.getPf64List());
		assertEquals(exp.getBlobList(), got.getBlobList());
		assertEquals(exp.getStrList(), got.getStrList());
		assertEquals(exp.getMsgList(), got.getMsgList());
		assertEquals(exp.getListseqList(), got.getListseqList());
		assertEquals(exp.getMapseqList(), got.getMapseqList());
		assertEquals(exp.getNilList(), got.getNilList());
	}

	@Test
	public void testEncodeDynamic() throws InvalidProtocolBufferException {
		TestDynamicEntropyGenerator gen = new TestDynamicEntropyGenerator();
		ProtobufEncoder enc = ProtobufEncoder.of();

		for (int i = 0; i < 100; i++) {
			Pair<Object, IvtCheck.DynamicEntropy> valAndEnt = gen.next();

			EntropyCoding.encodeDynamic(enc.reset(), valAndEnt.first);

			assertDynamicEntropyEquals(
				valAndEnt.second,
				IvtCheck.DynamicEntropy.parseFrom(enc.asBuffer())
			);
		}
	}

	@Test
	public void testEncodeCipher() throws InvalidProtocolBufferException {
		TestDynamicEntropyGenerator gen = new TestDynamicEntropyGenerator();
		ProtobufEncoder cipherEnc = ProtobufEncoder.of();
		ProtobufEncoder contentEnc = ProtobufEncoder.ofSplit();

		for (int i = 0; i < 100; i++) {
			String id = Integer.toString(i, 16);

			EntropyCoding.encodeDynamic(contentEnc.reset(), gen.next().first);
			EntropyCoding.encodeCipher(cipherEnc.reset(), id, contentEnc);

			IvtCheck.CipherEntropy got = IvtCheck.CipherEntropy.parseFrom(cipherEnc.asBuffer());

			assertEquals(id, got.getId());
			assertEquals(contentEnc.asSchemaBuffer(), got.getSchema().asReadOnlyByteBuffer());
			assertEquals(contentEnc.asContentBuffer(), got.getCtcontent().asReadOnlyByteBuffer());
		}
	}
}
