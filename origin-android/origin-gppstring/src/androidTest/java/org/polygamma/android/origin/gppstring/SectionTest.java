// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.gppstring;

import static org.polygamma.android.origin.gppstring.GppIds.*;
import static org.junit.Assert.assertEquals;

import android.util.Pair;
import android.util.SparseArray;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link Section} tests.
 */
@RunWith(AndroidJUnit4.class)
public class SectionTest {

	private static final List<Pair<String, Section>> TEST_CASES;

	static {
		SparseArray<List<Pair<String, Segment>>> coreTests = new SparseArray<>();
		SparseArray<SparseArray<List<Pair<String, Segment>>>> optTests = new SparseArray<>();

		for (Pair<String, Segment> test : SegmentTest.TEST_CASES) {
			Segment seg = test.second;
			int id = toGppSectionId(sectionIdOfSegment(seg.id()));
			SparseArray<List<Pair<String, Segment>>> tests;
			int idx;

			if (seg.isCore()) {
				tests = coreTests;
			} else {
				idx = optTests.indexOfKey(id);

				if (idx < 0) {
					tests = new SparseArray<>(1);
					optTests.put(id, tests);
				} else {
					tests = optTests.valueAt(idx);
				}
				id = toGppSegmentId(seg.id());
			}

			List<Pair<String, Segment>> sectTests;

			idx = tests.indexOfKey(id);
			if (idx < 0) {
				sectTests = new ArrayList<>(1);
				tests.put(id, sectTests);
			} else {
				sectTests = tests.valueAt(idx);
			}
			sectTests.add(test);
		}

		TEST_CASES = new ArrayList<>();

		for (int i = 0; i < coreTests.size(); i++) {
			for (Pair<String, Segment> coreTest : coreTests.valueAt(i)) {
				TEST_CASES.add(new Pair<>(
					coreTest.first,
					Section.ofBuilder(sectionIdOfSegment(coreTest.second.id()))
						.core(coreTest.second)
						.build()
				));

				SparseArray<List<Pair<String, Segment>>> restTests =
					optTests.get(coreTests.keyAt(i));

				if (restTests == null)
					continue;

				for (int j = 0; j < restTests.size(); j++) {
					for (Pair<String, Segment> rootTest : restTests.valueAt(j)) {
						SparseArray<Pair<String, Segment>> segTests = new SparseArray<>(1);

						segTests.put(toGppSegmentId(rootTest.second.id()), rootTest);
						for (int k = 0; k < restTests.size(); k++) {
							if (j != k) {
								Pair<String, Segment> otherTest = restTests.valueAt(k).get(0);

								segTests.put(toGppSegmentId(otherTest.second.id()), otherTest);
							}
						}

						Section.Builder sect =
							Section.ofBuilder(sectionIdOfSegment(coreTest.second.id()))
								.core(coreTest.second);
						StringBuilder encoded = new StringBuilder(coreTest.first);

						for (int k = 0; k < segTests.size(); k++) {
							Pair<String, Segment> segTest = segTests.valueAt(k);

							encoded.append('.')
								.append(segTest.first);
							sect.segment(segTest.second);
						}
						TEST_CASES.add(new Pair<>(encoded.toString(), sect.build()));
					}
				}
			}
		}
	}

	@Test
	public void testOf() {
		for (Pair<String, Section> test : TEST_CASES)
			assertEquals(test.second, Section.of(test.second.id(), test.first));
	}

	@Test
	public void testToString() {
		for (Pair<String, Section> test : TEST_CASES)
			assertEquals(test.first, test.second.toString());
	}
}
