// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.util.Pair;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.polygamma.android.origin.protobuf.ProtobufField;
import org.polygamma.android.origin.protobuf.ProtobufReader;
import org.polygamma.android.origin.protobuf.ProtobufSerializable;
import org.polygamma.android.origin.protobuf.ProtobufWriter;

import java.io.File;

/**
 * {@link OriginModule} tests.
 */
@RunWith(AndroidJUnit4.class)
public class OriginModuleTest extends TestWithSdk {

	public static class TestModuleA extends TestModule {
		private TestModuleA(Origin sdk) {
			super("A", sdk);
		}
	}

	public static class TestModuleB extends TestModule {

		@SuppressWarnings({"raw", "unchecked", "rawtypes"})
		public static Provider<TestModuleB> ofProvider() {
			return new TestProvider(
				TestModuleA.class,
				pair -> new TestModuleB(((Pair<Origin, Context>) pair).first),
				null
			);
		}

		private TestModuleB(Origin sdk) {
			super("B", sdk);
		}
	}

	public static class TestModuleC extends TestModule {
		public static Provider<TestModuleC> ofProvider() {
			return new TestProvider<>(TestModuleC.class, pair -> new TestModuleC(pair.first), null);
		}

		private TestModuleC(Origin sdk) {
			super("C", sdk);
		}
	}

	public static class TestModuleD extends TestModule {
		public static Provider<TestModuleD> ofProvider() {
			return new TestProvider<>(TestModuleD.class, pair -> new TestModuleD(pair.first), null);
		}

		private TestModuleD(Origin sdk) {
			super("D", sdk);
		}
	}

	private static class TestSettings implements ProtobufSerializable {
		private static final @ProtobufField.Tag int VALUE = ProtobufField.ofString(1);

		static final TestSettings EMPTY = new TestSettings("");

		static TestSettings ofProtobuf(ProtobufReader reader) {
			String val = "";

			while (reader.hasRemaining()) {
				if (reader.readTag() == VALUE)
					val = reader.readString();
			}
			return new TestSettings(val);
		}

		final String value;

		TestSettings(String val) {
			this.value = val;
		}

		@Override
		public void toProtobuf(ProtobufWriter writer) {
			writer.writeString(VALUE, this.value);
		}
	}

	@Test
	public void testProviderOf() {
		// Module must define a static `ofProvider()` method.
		assertThrows(
			IllegalArgumentException.class,
			() -> OriginModule.providerOf(TestModuleA.class)
		);
		// `ofProvider()` method must return a provider which provides the same type module.
		assertThrows(
			IllegalArgumentException.class,
			() -> OriginModule.providerOf(TestModuleB.class)
		);

		assertTrue(OriginModule.providerOf(TestModuleC.class) instanceof TestModule.TestProvider);
	}

	@Test
	public void testSettings() {
		TestModuleC mod = sdk.loadModule(TestModuleC.class);

		assertNull(mod.loadSettings(TestSettings::ofProtobuf));

		mod.storeSettings(new TestSettings("1234"));
		assertEquals("1234", mod.loadSettings(TestSettings::ofProtobuf).value);

		mod.storeSettings(new TestSettings("12345"));
		assertEquals("12345", mod.loadSettings(TestSettings::ofProtobuf).value);
	}

	@Test
	public void testCacheDirectory() throws InterruptedException {
		TestModuleC c = sdk.loadModule(TestModuleC.class);
		TestModuleD d = sdk.loadModule(TestModuleD.class);

		File cDir = c.cacheDirectory();
		File dDir = d.cacheDirectory();

		assertNotEquals(cDir.getAbsolutePath(), dDir.getAbsolutePath());

		destroySdk();
		setupSdk();

		c = sdk.loadModule(TestModuleC.class);
		d = sdk.loadModule(TestModuleD.class);

		assertEquals(cDir.getAbsolutePath(), c.cacheDirectory().getAbsolutePath());
		assertEquals(dDir.getAbsolutePath(), d.cacheDirectory().getAbsolutePath());
	}

	@Test
	public void testPersistentId() throws InterruptedException {
		TestModuleC c = sdk.loadModule(TestModuleC.class);
		TestModuleD d = sdk.loadModule(TestModuleD.class);

		String cId = c.persistentId();
		String dId = d.persistentId();

		assertNotEquals(cId, dId);

		destroySdk();
		setupSdk();

		c = sdk.loadModule(TestModuleC.class);
		d = sdk.loadModule(TestModuleD.class);

		assertEquals(cId, c.persistentId());
		assertEquals(dId, d.persistentId());
	}
}
