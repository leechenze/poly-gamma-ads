// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.adcom.context;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.polygamma.android.origin.adcom.TestUtil;
import org.polygamma.origin.adcom.AdcomContext;

/**
 * {@link Regs} tests.
 */
@RunWith(AndroidJUnit4.class)
public class RegsTest {
	@Test
	public void testSerde() {
		Regs exp = Regs.ofBuilder()
			.coppa(true)
			.gdpr(false)
			.gpp("12345")
			.applicableGppSectionIds(1, 2, 3, 4)
			.pipl(true)
			.build();
		Regs got = TestUtil.encodeAndDecode(
			exp,
			Regs::toProtobuf,
			Regs::ofProtobuf,
			AdcomContext.Regs::parseFrom
		);

		assertTrue(got.coppa());
		assertFalse(got.gdpr());
		assertEquals("12345", got.gpp());
		assertEquals(4, got.applicableGppSectionIdCount());
		assertEquals(1, got.applicableGppSectionId(0));
		assertEquals(2, got.applicableGppSectionId(1));
		assertEquals(3, got.applicableGppSectionId(2));
		assertEquals(4, got.applicableGppSectionId(3));
		assertTrue(got.pipl());

		got = Regs.of();
		assertEquals("", got.gpp());
		assertEquals(0, got.applicableGppSectionIdCount());
		assertFalse(got.coppa());
		assertFalse(got.gdpr());
		assertFalse(got.pipl());
	}
}
