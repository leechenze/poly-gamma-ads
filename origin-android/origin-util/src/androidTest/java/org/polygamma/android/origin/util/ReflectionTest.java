// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.graphics.Point;
import android.os.Build;
import android.util.Pair;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;

/**
 * {@link Reflection} tests.
 */
@RunWith(AndroidJUnit4.class)
public class ReflectionTest {

	@Test
	public void testResolveMember() throws Exception {
		assertEquals(
			String.class.getMethod("indexOf", int.class),
			Reflection.resolveMember("Ljava/lang/String.indexOf(I)I")
		);
		assertEquals(
			String.class.getMethod("indexOf", String.class),
			Reflection.resolveMember("Ljava/lang/String.indexOf(Ljava/lang/String;)I")
		);
		assertEquals(
			String.class.getConstructor(byte[].class),
			Reflection.resolveMember("Ljava/lang/String.<init>([B)V")
		);
		assertEquals(
			Arrays.class.getMethod("equals", int[].class, int[].class),
			Reflection.resolveMember("Ljava/util/Arrays.equals([I[I)Z")
		);
		assertEquals(
			Arrays.class.getMethod("copyOfRange", int[].class, int.class, int.class),
			Reflection.resolveMember("Ljava/util/Arrays.copyOfRange([III)[I")
		);
		assertEquals(
			Arrays.class.getMethod(
				"copyOfRange",
				Object[].class,
				int.class,
				int.class,
				Class.class
			),
			Reflection.resolveMember("Ljava/util/Arrays.copyOfRange([Ljava/lang/Object;IILjava/lang/Class;)[Ljava/lang/Object;")
		);
		assertEquals(
			Pair.class.getField("first"),
			Reflection.resolveMember("Landroid/util/Pair.first")
		);
		assertEquals(
			Pair.class.getField("second"),
			Reflection.resolveMember("Landroid/util/Pair.second")
		);
	}

	@Test
	public void testResolveExecutable() throws Exception {
		assertEquals(
			String.class.getMethod("indexOf", int.class),
			Reflection.resolveExecutable("Ljava/lang/String.indexOf(I)I")
		);
		assertEquals(
			String.class.getMethod("indexOf", String.class),
			Reflection.resolveExecutable("Ljava/lang/String.indexOf(Ljava/lang/String;)I")
		);
		assertEquals(
			String.class.getConstructor(byte[].class),
			Reflection.resolveExecutable("Ljava/lang/String.<init>([B)V")
		);
		assertThrows(
			IllegalArgumentException.class,
			() -> Reflection.resolveExecutable("Landroid/util/Pair.first")
		);
	}

	@Test
	public void testResolveField() throws Exception {
		assertEquals(
			Pair.class.getField("first"),
			Reflection.resolveField("Landroid/util/Pair.first")
		);
		assertEquals(
			Pair.class.getField("second"),
			Reflection.resolveField("Landroid/util/Pair.second")
		);
		assertThrows(
			IllegalArgumentException.class,
			() -> Reflection.resolveField("Ljava/lang/String.indexOf(I)I")
		);
		assertThrows(
			IllegalArgumentException.class,
			() -> Reflection.resolveField("Ljava/lang/String.indexOf(Ljava/lang/String;)I")
		);
		assertThrows(
			IllegalArgumentException.class,
			() -> Reflection.resolveField("Ljava/lang/String.<init>([B)V")
		);
	}

	@Test
	public void testIsInstanceMember() {
		assertTrue(Reflection.isInstanceMember(Reflection.resolveMember(
			"Landroid/util/Pair.first"
		)));
		assertTrue(Reflection.isInstanceMember(Reflection.resolveMember(
			"Ljava/lang/String.indexOf(I)I"
		)));
		assertFalse(Reflection.isInstanceMember(Reflection.resolveMember(
			"Ljava/util/Arrays.copyOfRange([III)[I"
		)));
		assertFalse(Reflection.isInstanceMember(Reflection.resolveMember(
			"Landroid/os/Build$VERSION.SDK_INT"
		)));
	}

	@Test
	public void testInvoke() {
		assertEquals(
			"foobar",
			Reflection.invoke(
				"Ljava/lang/String.concat(Ljava/lang/String;)Ljava/lang/String;",
				Arrays.asList("bar", "foo").iterator()::next
			)
		);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			assertEquals(
				Long.hashCode(34L),
				Reflection.invoke(
					"Ljava/lang/Long.hashCode(J)I",
					Collections.singletonList(34).iterator()::next
				)
			);
		}
		assertEquals(
			"foo",
			Reflection.invoke(
				"Ljava/lang/String.<init>([B)V",
				Collections.singletonList("foo".getBytes(StandardCharsets.UTF_8)).iterator()::next
			)
		);
	}

	@Test
	public void testRead() {
		assertEquals(
			Build.VERSION.SDK_INT,
			Reflection.read("Landroid/os/Build$VERSION.SDK_INT", null)
		);
		assertEquals(
			"foo",
			Reflection.read(
				"Landroid/util/Pair.first",
				Collections.singletonList(new Pair<>("foo", "bar")).iterator()::next
			)
		);
		assertEquals(
			"bar",
			Reflection.read(
				"Landroid/util/Pair.second",
				Collections.singletonList(new Pair<>("foo", "bar")).iterator()::next
			)
		);
	}

	@Test
	public void testWrite() {
		Point point = new Point();

		Reflection.write(
			"Landroid/graphics/Point.x",
			Arrays.asList(point, 123).iterator()::next
		);
		assertEquals(123, point.x);
		assertEquals(0, point.y);

		Reflection.write(
			"Landroid/graphics/Point.y",
			Arrays.asList(point, 456).iterator()::next
		);
		assertEquals(123, point.x);
		assertEquals(456, point.y);
	}
}
