// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.util.Pair;

import androidx.annotation.CallSuper;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;
import androidx.core.util.Function;

import org.polygamma.android.origin.util.Sync;

import java.io.File;

/**
 * Test module implementation.
 */
public class TestModule extends OriginModule {

	/**
	 * Test module provider.
	 *
	 * @param <M> module type
	 */
	public static final class TestProvider<M extends TestModule> extends Provider<M> {

		private final Function<Pair<Origin, Context>, M> onLoad;
		private final @Nullable Consumer<Pair<M, Context>> onReload;

		/**
		 * Construct new module provider.
		 *
		 * @param type module type
		 * @param onLoad function to load module with
		 * @param onReload optional function to reload module with
		 */
		TestProvider(
			Class<M> type,
			Function<Pair<Origin, Context>, M> onLoad,
			@SuppressWarnings("SameParameterValue") @Nullable Consumer<Pair<M, Context>> onReload
		) {
			super(type);
			this.onLoad = onLoad;
			this.onReload = onReload;
		}

		@Override
		protected M load(Origin sdk, Context ctxt) {
			assertTrue(Sync.isMainThread());

			M module = this.onLoad.apply(new Pair<>(sdk, ctxt));

			module.provider = this;
			return module;
		}

		@Override
		protected void reload(M module, Context ctxt) {
			assertTrue(Sync.isMainThread());
			module.provider = this;
			if (this.onReload != null)
				this.onReload.accept(new Pair<>(module, ctxt));
		}
	}

	/**
	 * Provider which loaded or reloaded module.
	 */
	@Nullable TestProvider<?> provider;

	/**
	 * Millisecond timestamp of when module was {@linkplain #destroy() destroyed}.
	 */
	long destroyedOn;

	/**
	 * Construct new module.
	 *
	 * @param name module name
	 * @param sdk owning SDK
	 */
	public TestModule(String name, Origin sdk) {
		super(name, sdk);
	}

	File cacheDirectory() {
		return super.resolveCacheDirectory();
	}

	String persistentId() {
		return super.resolvePersistentId();
	}

	@Override
	@CallSuper
	protected void destroy() {
		assertFalse(Sync.isMainThread());
		this.destroyedOn = System.nanoTime();
	}
}
