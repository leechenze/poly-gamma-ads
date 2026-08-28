// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.antifraud;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.os.Build;
import android.os.Process;
import android.os.SystemClock;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.polygamma.android.origin.adcom.context.App;
import org.polygamma.android.origin.adcom.context.Device;
import org.polygamma.android.origin.core.DeviceModule;
import org.polygamma.android.origin.protobuf.ProtobufField;
import org.polygamma.android.origin.protobuf.ProtobufReader;
import org.polygamma.android.origin.protobuf.ProtobufWriter;
import org.polygamma.android.origin.util.Futures;
import org.polygamma.android.origin.util.ListenableScheduledFuture;
import org.polygamma.android.origin.util.Time;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * {@link AntifraudModule} tests.
 */
@RunWith(AndroidJUnit4.class)
public class AntifraudModuleTest extends TestWithModule {

	private static void assertCheckArguments(TestCheckArguments got) {
		App expApp = sdk.app();
		Device expDev = sdk.loadModule(DeviceModule.class).device();

		assertNotNull(expDev);
		assertEquals(expApp.storeId(), got.app.storeId());
		assertEquals(expApp.name(), got.app.name());
		assertEquals(expApp.version(), got.app.version());
		assertEquals(expDev.advertisingIdCount(), got.device.advertisingIdCount());
		for (int i = 0; i < expDev.advertisingIdCount(); i++)
			assertEquals(expDev.advertisingId(i), got.device.advertisingId(i));
		assertEquals(expDev.screenWidthPx(), got.device.screenWidthPx());
		assertEquals(expDev.screenHeightPx(), got.device.screenHeightPx());
		assertEquals(expDev.landscape(), got.device.landscape());
		assertEquals(expDev.languageCode(), got.device.languageCode());
		assertEquals(expDev.extraLanguageCodeCount(), got.device.extraLanguageCodeCount());
		for (int i = 0; i < expDev.extraLanguageCodeCount(); i++)
			assertEquals(expDev.extraLanguageCode(i), got.device.extraLanguageCode(i));
		assertEquals(expDev.simCarrierMccMnc(), got.device.simCarrierMccMnc());
		assertTrue(got.timestampSeconds > 0 && got.timestampSeconds <= Time.nowUtcSeconds());
		assertEquals(Process.myPid(), got.processId);
		assertEquals(Process.myUid(), got.userId);
		assertEquals(Build.TAGS, got.buildTags);
		assertEquals(Build.FINGERPRINT, got.buildFingerprint);
		assertEquals(Build.PRODUCT, got.buildProduct);
		assertEquals(Build.HARDWARE, got.buildHardware);
		assertEquals(Build.DISPLAY, got.buildDisplay);

		if (module.entropyMachineCrypto == null) {
			assertEquals(CheckArgumentTags.ENCRYPTION_NONE, got.entropyMachineEncryptionMode);
			assertArrayEquals(new byte[0], got.entropyMachineKey);
		} else {
			assertEquals(CheckArgumentTags.ENCRYPTION_AES, got.entropyMachineEncryptionMode);
			assertArrayEquals(
				module.entropyMachineCrypto.second.getEncoded(),
				got.entropyMachineKey
			);
		}
	}

	@Test
	public void testCheck() throws Exception {
		ListenableFuture<?> fut = module.checkFuture;

		while (fut == null) {
			SystemClock.sleep(10);
			fut = module.checkFuture;
		}
		assertNull(module.callCheckFuture);

		TestCheckArguments args = pollRequest();

		assertSame(fut, module.checkFuture);
		assertCheckArguments(args);
		assertNotEquals(0, args.entropy.length);
		assertEquals("", args.digest);

		CheckResult expRes = new CheckResult(
			Time.nowRealtimeSeconds() + 2,
			new AntifraudStatus(UUID.randomUUID().toString(), AntifraudStatus.RatingHuman, 93),
			null
		);

		pushResponse(expRes);

		CheckResult gotRes = (CheckResult) Futures.await(fut);

		assertEquals(expRes.status, gotRes.status);

		do {
			SystemClock.sleep(50);
		} while (module.checkFuture != null);

		fut = module.callCheckFuture;
		assertNotNull(fut);

		long delay = ((ListenableScheduledFuture<?>) fut).getDelay(TimeUnit.SECONDS);

		assertTrue(delay > 0 && delay <= 5);

		assertEquals(gotRes.status, module.status());

		// next request should have a digest, but be otherwise the same

		args = pollRequest();
		fut = module.checkFuture;

		assertNotNull(fut);
		assertCheckArguments(args);
		assertNotEquals(0, args.entropy.length);
		assertEquals(expRes.status.digest(), args.digest);

		String digest = UUID.randomUUID().toString();
		ProtobufWriter entWriter = new ProtobufWriter();

		entWriter.writeString(
			ProtobufField.ofString(EntropyMachine.Push),
			AntifraudModuleTest.class.getName()
		);
		entWriter.writeString(ProtobufField.ofString(EntropyMachine.Write), "L");
		entWriter.writeString(ProtobufField.ofString(EntropyMachine.Read), "L");
		entWriter.writeBool(ProtobufField.ofBool(EntropyMachine.Pop), true);

		ByteBuffer ent = entWriter.finish();

		if (module.entropyMachineCrypto != null) {
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");

			cipher.init(
				Cipher.ENCRYPT_MODE,
				new SecretKeySpec(args.entropyMachineKey, "AES"),
				new IvParameterSpec(Arrays.copyOf(digest.getBytes(StandardCharsets.UTF_8), 16))
			);
			ent = ByteBuffer.wrap(cipher.doFinal(ent.array(), ent.position(), ent.remaining()));
		}

		expRes = new CheckResult(
			Time.nowRealtimeSeconds() + 2,
			new AntifraudStatus(digest, AntifraudStatus.RatingHuman, 93),
			Arrays.copyOf(ent.array(), ent.remaining())
		);

		pushResponse(expRes);

		gotRes = (CheckResult) Futures.await(fut);

		assertEquals(expRes.status, gotRes.status);

		// next request should have entropy with machine entropy in there
		args = pollRequest();
		pushResponse(new CheckResult(
			Time.nowRealtimeSeconds() + 10,
			new AntifraudStatus(
				UUID.randomUUID().toString(),
				AntifraudStatus.RatingHuman,
				93
			),
			null
		));

		ProtobufReader reader = new ProtobufReader(ByteBuffer.wrap(args.entropy));
		boolean hasMachine = false;

		for (int tag = 0; reader.hasRemaining();) {
			assertEquals(ProtobufField.ofString(1), tag == 0 ? reader.readTag() : tag);

			String name = reader.readString();

			tag = reader.readTag();
			if (tag == ProtobufField.ofString(1))
				continue;

			assertEquals(ProtobufField.ofBytes(2), tag);
			assertNotEquals(0, reader.readBytes().length);
			tag = 0;
			hasMachine |= name.equals("machine");
		}
		assertTrue(hasMachine);
		assertEquals(
			AntifraudModuleTest.class.getName(),
			new String((byte[]) EntropyMachine.L)
		);
	}
}
