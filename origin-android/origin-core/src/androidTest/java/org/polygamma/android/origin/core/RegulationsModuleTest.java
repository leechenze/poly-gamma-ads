// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.os.SystemClock;
import android.util.Pair;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.polygamma.android.origin.adcom.context.Regs;
import org.polygamma.android.origin.util.ExecutingService;

import java.util.ArrayList;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;

/**
 * {@link RegulationsModule} tests.
 */
@RunWith(AndroidJUnit4.class)
public class RegulationsModuleTest extends TestWithSdk {

	private static RegulationsModule module;

	@BeforeClass
	public static void setup() {
		module = sdk.loadModule(RegulationsModule.class);
	}

	@AfterClass
	public static void destroy() {
		module = null;
	}

	private static Regs currentRegs() {
		while (true) {
			int state = module.updater.state();

			if (
				state != ExecutingService.STATE_SCHEDULED &&
				state != ExecutingService.STATE_RUNNING
			) {
				return module.regs();
			}
			SystemClock.sleep(15);
		}
	}

	private static void assertRegsEqual(Regs exp, Regs got) {
		assertEquals(exp.gpp(), got.gpp());
		assertEquals(exp.applicableGppSectionIdCount(), got.applicableGppSectionIdCount());
		for (int i = 0; i < got.applicableGppSectionIdCount(); i++)
			assertEquals(exp.applicableGppSectionId(i), got.applicableGppSectionId(i));
		assertEquals(exp.coppa(), got.coppa());
		assertEquals(exp.gdpr(), got.gdpr());
		assertEquals(exp.pipl(), got.pipl());
	}

	private static boolean areRegsEqual(Regs a, Regs b) {
		if (!a.gpp().equals(b.gpp()))
			return false;
		if (a.applicableGppSectionIdCount() != b.applicableGppSectionIdCount())
			return false;
		for (int i = 0; i < a.applicableGppSectionIdCount(); i++) {
			if (a.applicableGppSectionId(i) != b.applicableGppSectionId(i))
				return false;
		}
		return a.coppa() == b.coppa() &&
			a.gdpr() == b.gdpr() &&
			a.pipl() == b.pipl();
	}

	@Before
	public void clearPreferences() {
		module.preferences
			.edit()
			.remove(RegulationsModule.GPP_SID_KEY)
			.remove(RegulationsModule.GPP_STRING_KEY)
			.remove(RegulationsModule.TCF_GDPR_APPLIES_KEY)
			.remove(RegulationsModule.TCF_PURPOSE_CONSENTS_KEY)
			.remove(RegulationsModule.TCF_TC_STRING_KEY)
			.remove(RegulationsModule.US_PRIVACY_STRING)
			.apply();

		for (int i = 0; i < 5; i++) {
			if (areRegsEqual(currentRegs(), Regs.of()))
				break;
			SystemClock.sleep(50);
		}
	}

	@Test
	public void testRegs() {
		assertRegsEqual(Regs.of(), currentRegs());

		module.preferences
			.edit()
			.putInt(RegulationsModule.TCF_GDPR_APPLIES_KEY, 1)
			.apply();
		SystemClock.sleep(250);
		assertRegsEqual(
			Regs.ofBuilder()
				.gdpr(true)
				.build(),
			currentRegs()
		);

		module.preferences
			.edit()
			.putString(RegulationsModule.US_PRIVACY_STRING, "1YNY")
			.apply();
		SystemClock.sleep(250);
		assertRegsEqual(
			Regs.ofBuilder()
				.applicableGppSectionIds(6)
				.gpp("DBABTA~1YNY")
				.gdpr(true)
				.build(),
			currentRegs()
		);

		module.preferences
			.edit()
			.putString(RegulationsModule.GPP_STRING_KEY, "DBACNYA~CQS_1MAQS_1MAPoABABGDgCAAAAAAAAAAAAAAAAAAAAA.QAAA.IAAA~1NYN")
			.apply();
		SystemClock.sleep(250);
		assertRegsEqual(
			Regs.ofBuilder()
				.applicableGppSectionIds(2, 6)
				.gpp("DBACNYA~CQS_1MAQS_1MAPoABABGDgCAAAAAAAAAAAAAAAAAAAAA.QAAA.IAAA~1NYN")
				.gdpr(true)
				.build(),
			currentRegs()
		);

		module.preferences
			.edit()
			.putString(RegulationsModule.GPP_STRING_KEY, "DBABAKY~BAAAAA")
			.apply();
		SystemClock.sleep(250);
		assertRegsEqual(
			Regs.ofBuilder()
				.applicableGppSectionIds(500)
				.gdpr(true)
				.gpp("DBABAKY~BAAAAA")
				.pipl(true)
				.build(),
			currentRegs()
		);
	}

	@Test
	public void testRegsUpdate() throws InterruptedException {
		assertRegsEqual(Regs.of(), currentRegs());

		ArrayList<Regs> expect = new ArrayList<>();
		LinkedTransferQueue<Regs> updates = new LinkedTransferQueue<>();
		OriginModuleEventCallback callback =
			(_mod, _name, data, when) -> updates.add((Regs) data);

		sdk.registerModuleEventCallback(callback, new Pair<>(
			module,
			RegulationsModule.REGS_UPDATE_EVENT
		));

		module.preferences
			.edit()
			.putInt(RegulationsModule.TCF_GDPR_APPLIES_KEY, 1)
			.apply();
		expect.add(
			Regs.ofBuilder()
				.gdpr(true)
				.build()
		);
		SystemClock.sleep(250);

		module.preferences
			.edit()
			.putString(RegulationsModule.US_PRIVACY_STRING, "1YNY")
			.apply();
		expect.add(
			Regs.ofBuilder()
				.applicableGppSectionIds(6)
				.gpp("DBABTA~1YNY")
				.gdpr(true)
				.build()
		);
		SystemClock.sleep(250);

		module.preferences
			.edit()
			.putString(RegulationsModule.GPP_STRING_KEY, "DBACNYA~CQS_1MAQS_1MAPoABABGDgCAAAAAAAAAAAAAAAAAAAAA.QAAA.IAAA~1NYN")
			.apply();
		expect.add(
			Regs.ofBuilder()
				.applicableGppSectionIds(2, 6)
				.gpp("DBACNYA~CQS_1MAQS_1MAPoABABGDgCAAAAAAAAAAAAAAAAAAAAA.QAAA.IAAA~1NYN")
				.gdpr(true)
				.build()
		);
		SystemClock.sleep(250);

		module.preferences
			.edit()
			.putString(RegulationsModule.GPP_STRING_KEY, "DBABAKY~BAAAAA")
			.apply();
		expect.add(
			Regs.ofBuilder()
				.applicableGppSectionIds(500)
				.gdpr(true)
				.gpp("DBABAKY~BAAAAA")
				.pipl(true)
				.build()
		);
		SystemClock.sleep(250);

		while (!expect.isEmpty()) {
			Regs exp = expect.remove(0);
			Regs got = updates.poll(1, TimeUnit.SECONDS);

			assertRegsEqual(exp, got);
		}
		assertNull(updates.poll(1, TimeUnit.SECONDS));
	}
}
