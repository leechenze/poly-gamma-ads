// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.adcom.context;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.polygamma.android.origin.adcom.enums.AdComEnums;
import org.polygamma.android.origin.protobuf.ProtobufReader;
import org.polygamma.android.origin.protobuf.ProtobufWriter;
import org.polygamma.origin.adcom.AdcomContext;

import java.io.IOException;
import java.util.Arrays;

/**
 * {@link App} tests.
 */
@RunWith(AndroidJUnit4.class)
public class AppTest {
	@Test
	public void testSerde() throws IOException {
		App exp = App.ofBuilder()
			.categoryTaxonomy(AdComEnums.CategoryTaxonomyIabAdProduct10)
			.pageCategories(Arrays.asList("a", "b", "c"))
			.sectionCategories(Arrays.asList("d", "e", "f"))
			.paid(true)
			.storeId("123456")
			.version("1.2.3")
			.id("654321")
			.name("test.app")
			.publisherId("89101112")
			.build();
		App got = App.ofProtobuf(new ProtobufReader(
			AdcomContext.DistributionChannel.parseFrom(ProtobufWriter.serialize(exp))
				.toByteString()
				.asReadOnlyByteBuffer()
		));

		assertEquals(AdComEnums.CategoryTaxonomyIabAdProduct10, got.categoryTaxonomy());
		assertEquals(3, got.pageCategoryCount());
		assertEquals("a", got.pageCategory(0));
		assertEquals("b", got.pageCategory(1));
		assertEquals("c", got.pageCategory(2));
		assertEquals(3, got.sectionCategoryCount());
		assertEquals("d", got.sectionCategory(0));
		assertEquals("e", got.sectionCategory(1));
		assertEquals("f", got.sectionCategory(2));
		assertTrue(got.paid());
		assertEquals("123456", got.storeId());
		assertEquals("1.2.3", got.version());
		assertEquals("654321", got.id());
		assertEquals("test.app", got.name());
		assertEquals("89101112", got.publisherId());

		got = App.of();
		assertEquals("", got.id());
		assertEquals("", got.name());
		assertEquals("", got.publisherId());
		assertEquals("", got.storeId());
		assertEquals("", got.version());
		assertEquals(0, got.sectionCategoryCount());
		assertEquals(0, got.pageCategoryCount());
		assertEquals(AdComEnums.CategoryTaxonomyUnknown, got.categoryTaxonomy());
		assertFalse(got.paid());
	}
}
