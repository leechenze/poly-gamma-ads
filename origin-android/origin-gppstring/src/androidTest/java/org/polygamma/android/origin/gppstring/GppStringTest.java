// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.gppstring;

import static org.junit.Assert.assertEquals;
import static org.polygamma.android.origin.gppstring.GppIds.*;

import android.util.Pair;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * {@link GppString} tests.
 */
@RunWith(AndroidJUnit4.class)
public class GppStringTest {

	private static Section sectionOf(Segment core, Segment... rest) {
		Section.Builder rv = Section.ofBuilder(sectionIdOfSegment(core.id()))
			.core(core);

		for (Segment seg : rest)
			rv.segment(seg);
		return rv.build();
	}

	private static GppString of(Section... sects) {
		GppString.Builder rv = GppString.ofBuilder();

		for (Section sect : sects)
			rv.section(sect);
		return rv.build();
	}

	private static final List<Pair<String, GppString>> TEST_CASES =
		Arrays.asList(
			new Pair<>("DBAA", GppString.ofBuilder().build()),
			new Pair<>(
				"DBACObA~" +
				"CPSG_8APSG_8AAAAAAENAACAAAAAAAAAAAAAAAAAAAAA.IAAA~" +
				"BPSG_8APSG_8AAAAAAENAACAAAAAAAAAAAAAAAAAAA.YAAAAAAAAAA~" +
				"1---~" +
				"BAAAAAAAAABA.QA",
				of(
					sectionOf(
						Segment.ofBuilder(TcfEuV2.Core.ID)
							.setInt(TcfEuV2.Core.Version, 2)
							.setDate(TcfEuV2.Core.Created, new Date(1640995200000L))
							.setDate(TcfEuV2.Core.LastUpdated, new Date(1640995200000L))
							.setString(TcfEuV2.Core.ConsentLanguage, "EN")
							.setInt(TcfEuV2.Core.TcfPolicyVersion, 2)
							.setString(TcfEuV2.Core.PublisherCc, "AA")
							.build(),
						Segment.ofBuilder(TcfEuV2.DisclosedVendors.ID)
							.build()
					),
					sectionOf(
						Segment.ofBuilder(TcfCaV1.Core.ID)
							.setInt(TcfCaV1.Core.Version, 1)
							.setDate(TcfCaV1.Core.Created, new Date(1640995200000L))
							.setDate(TcfCaV1.Core.LastUpdated, new Date(1640995200000L))
							.setString(TcfCaV1.Core.ConsentLanguage, "EN")
							.setInt(TcfCaV1.Core.TcfPolicyVersion, 2)
							.build(),
						Segment.ofBuilder(TcfCaV1.PublisherPurposes.ID)
							.build()
					),
					sectionOf(
						Segment.ofBuilder(UsPrivacyV1.Core.ID)
							.setInt(UsPrivacyV1.Core.Version, 1)
							.build()
					),
					sectionOf(
						Segment.ofBuilder(UsNational.Core.ID)
							.setInt(UsNational.Core.Version, 1)
							.setIntArray(UsNational.Core.SensitiveDataProcessing,
								0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
							)
							.setIntArray(UsNational.Core.KnownChildSensitiveDataConsents, 0, 0, 0)
							.setInt(UsNational.Core.MspaCoveredTransaction, 1)
							.build(),
						Segment.ofBuilder(UsNational.Gpc.ID)
							.build()
					)
				)
			)
		);

	@Test
	public void testOf() {
		for (Pair<String, GppString> test : TEST_CASES)
			assertEquals(test.second, GppString.of(test.first));
	}

	@Test
	public void testToString() {
		for (Pair<String, GppString> test : TEST_CASES)
			assertEquals(test.first, test.second.toString());
	}
}
