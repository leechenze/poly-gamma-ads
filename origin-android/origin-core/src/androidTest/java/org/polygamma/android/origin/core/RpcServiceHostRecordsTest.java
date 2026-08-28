// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.os.SystemClock;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.polygamma.android.origin.util.CollectionsCompat;
import org.polygamma.android.origin.util.Time;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

/**
 * {@link RpcServiceHostRecords} tests.
 */
@RunWith(AndroidJUnit4.class)
public class RpcServiceHostRecordsTest {

	@Test
	public void testCtor() {
		RpcServiceHostRecords got =
			new RpcServiceHostRecords("foo", "foo.com", Collections.emptyList());

		assertEquals("foo", got.service());
		assertEquals("foo.com", got.host());
		assertEquals(0, got.size());
		assertTrue(got.toRecords().isEmpty());
		assertNull(got.next());

		RpcHostRecord a = RpcHostRecord.ofHost("foo.com", 0);
		RpcHostRecord b = RpcHostRecord.ofHost("bar.com", 0);

		got = new RpcServiceHostRecords("foo", "foo.com", Arrays.asList(a, b));

		assertEquals("foo", got.service());
		assertEquals("foo.com", got.host());
		assertEquals(2, got.size());
		assertEquals(new HashSet<>(Arrays.asList(a, b)), new HashSet<>(got.toRecords()));

		assertThrows(
			IllegalArgumentException.class,
			() -> new RpcServiceHostRecords("", "foo.com", Collections.emptyList())
		);
		assertThrows(
			IllegalArgumentException.class,
			() -> new RpcServiceHostRecords("foo", "", Collections.emptyList())
		);
	}

	@Test
	public void testToRecords() {
		long now = Time.nowUtcSeconds();
		RpcHostRecord a = new RpcHostRecord(now + 1, 1, "foo.com", 0);
		RpcHostRecord b = new RpcHostRecord(now + 10, 1, "bar.com", 0);
		RpcHostRecord c = new RpcHostRecord(now + 20, 1, "baz.com", 0);
		RpcHostRecord d = new RpcHostRecord(now, 1, "whiz.com", 0);
		RpcServiceHostRecords recs =
			new RpcServiceHostRecords("foo", "foo.com", Arrays.asList(a, b, c, d));

		assertEquals(new HashSet<>(Arrays.asList(a, b, c)), new HashSet<>(recs.toRecords()));
		SystemClock.sleep(1100);
		assertEquals(new HashSet<>(Arrays.asList(b, c)), new HashSet<>(recs.toRecords()));
	}

	@Test
	public void testNext() {
		long now = Time.nowUtcSeconds();
		RpcHostRecord a = new RpcHostRecord(now + 1, 2, "foo.com", 0);
		RpcHostRecord b = new RpcHostRecord(now + 10, 3, "bar.com", 0);
		RpcHostRecord c = new RpcHostRecord(now + 20, 3, "baz.com", 0);
		RpcHostRecord d = new RpcHostRecord(now, 1, "whiz.com", 0);
		RpcServiceHostRecords recs =
			new RpcServiceHostRecords("foo", "foo.com", Arrays.asList(a, b, c, d));
		HashSet<RpcHostRecord> visited = new HashSet<>();

		assertEquals(4, recs.size());
		for (int i = 0; i < 4; i++)
			visited.add(recs.next());
		assertEquals(3, recs.size()); // `d` should've been expired
		assertEquals(2, visited.size());
		assertTrue(CollectionsCompat.unmodifiableSetOf(a, d).containsAll(visited));

		visited.clear();
		SystemClock.sleep(1100);
		for (int i = 0; i < 4; i++)
			visited.add(recs.next());
		assertEquals(2, recs.size()); // `a` should've been expired
		assertEquals(2, visited.size());
		assertTrue(CollectionsCompat.unmodifiableSetOf(a, b, c).containsAll(visited));

		visited.clear();
		for (int i = 0; i < 4; i++)
			visited.add(recs.next());
		assertEquals(2, recs.size());
		assertEquals(1, visited.size());
		assertTrue(CollectionsCompat.unmodifiableSetOf(b, c).containsAll(visited));

		b.updateLastFailureTimestamp();
		for (int i = 0; i < 4; i++)
			assertSame(c, recs.next());

		// should return record with earliest failure
		SystemClock.sleep(1000);
		c.updateLastFailureTimestamp();
		for (int i = 0; i < 4; i++)
			assertSame(b, recs.next());

		SystemClock.sleep(3100);
		visited.clear();
		for (int i = 0; i < 4; i++)
			visited.add(recs.next());
		assertEquals(2, recs.size());
		assertEquals(1, visited.size());
		assertTrue(CollectionsCompat.unmodifiableSetOf(b, c).containsAll(visited));
	}
}
