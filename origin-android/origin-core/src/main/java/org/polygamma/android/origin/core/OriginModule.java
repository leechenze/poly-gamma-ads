// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Pair;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.WorkerThread;
import androidx.core.util.Consumer;
import androidx.core.util.Function;

import org.polygamma.android.origin.protobuf.ProtobufDeserializer;
import org.polygamma.android.origin.protobuf.ProtobufReader;
import org.polygamma.android.origin.protobuf.ProtobufSerializable;
import org.polygamma.android.origin.protobuf.ProtobufWriter;
import org.polygamma.android.origin.util.Flate;
import org.polygamma.android.origin.util.Logger;
import org.polygamma.android.origin.util.Preconditions;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.zip.Deflater;

/**
 * Origin {@linkplain Origin SDK} module.
 * <p>Modules provide a method for integrating into the core SDK and providing extended
 * capabilities. Implementations must be declared {@code public}, and must provide at minimum a
 * {@code public static} method named {@code ofProvider}, which accepts no arguments and constructs
 * an instance of the module's {@linkplain Provider provider}, <b>and</b> a {@code public static}
 * {@link String name string} named {@code NAME}:
 * {@snippet lang="java" :
 * public class MyModule extends OriginModule {
 *
 *     public static final String NAME = "my-company.mymodule";
 *
 *     public static final class Provider extends OriginModule.Provider<MyModule> {
 *         private Provider() {
 *             super(MyModule.class);
 *         }
 *
 *         @Override
 *         public MyModule load(Origin sdk, Context ctxt) {
 *             return new MyModule(sdk);
 *         }
 *     }
 *
 *     public static Provider ofProvider() {
 *         return new Provider();
 *     }
 *
 *     private MyModule(Origin sdk) {
 *         super(NAME, sdk);
 *     }
 *
 *     @Override
 *     public void destroy() {
 *     }
 * }
 * }
 * <h2>Events</h2>
 * <p>Modules support named events, registered using {@linkplain #registerEvent(String, boolean)}.
 * An {@linkplain OriginModuleEventBus event bus} is used to {@linkplain
 * OriginModuleEventBus#submit(Object) deliver} events to listeners {@linkplain
 * Origin#registerModuleEventCallback(OriginModuleEventCallback, Pair) registered} with the owning
 * SDK.
 * <p>Events can fire with optional data, and may be sticky or non-sticky. Sticky events are
 * collapsable, and are fired to <i>newly</i> registered listeners, while non-sticky events are
 * not collapsable and are fired only when an event is delivered through its bus.
 * <p>Publicly exposed events of a module should have their names defined in the module as
 * {@code public static}, annotated with {@link OriginModuleEventName}.
 * {@snippet lang="java" :
 * public class MyModule extends OriginModule {
 *
 *     public static final String NAME = "my-company.mymodule";
 *     public static final @OriginModuleEventName String STICKY_EVENT = "sticky-event";
 *     public static final @OriginModuleEventName String NON_STICKY_EVENT = "non-sticky-event";
 *
 *     private final OriginModuleEventBus stickyEventBus;
 *     private final OriginModuleEventBus nonStickyEventBus;
 *
 *     public MyModule(Origin sdk) {
 *         super(NAME, sdk);
 *         this.stickyEventBus = super.registerEvent(STICKY_EVENT, true);
 *         this.nonStickyEventBus = super.registerEvent(NON_STICKY_EVENT, false);
 *
 *         sdk.backgroundExecutor()
 *             .submitWithFixedDelay(() -> {
 *                 this.stickyEventBus.submit("sticky: " + SystemClock.uptimeMillis());
 *                 this.nonStickyEventBus.submit("non-sticky: " + SystemClock.uptimeMillis());
 *             }, 0, 30, TimeUnit.SECONDS);
 *     }
 * }
 * }
 * <p>In the example above, after {@code sticky-event} is fired for the first time, any subsequent
 * listener registration will be invoked with the <i>last</i> value the event was fired with. The
 * {@code non-sticky-event}, however, will be delivered to listeners <i>only</i> when the
 * delayed task runs. Additionally, if the system is under stress and cannot deliver {@code
 * sticky-event} before it is scheduled for delivery again, the existing delivery will collapse
 * into the new delivery and only <i>one</i> {@code sticky-event} will be fired.
 *
 * @since 0.1
 */
@SuppressWarnings("JavadocDeclaration")
public class OriginModule {

	private static final String TAG = OriginModule.class.getSimpleName();
	private static final int SETTINGS_BASE64_FLAGS = Base64.URL_SAFE | Base64.NO_WRAP;

	/**
	 * Pseudo module used to denote a module is being loaded.
	 */
	static final OriginModule LOADING_MODULE = new OriginModule();

	/**
	 * Origin SDK {@linkplain OriginModule module} provider.
	 * <p>Module providers provide an interface for {@linkplain #load(Origin, Context) loading} or
	 * {@linkplain #reload(OriginModule, Context) reloading} a module.
	 *
	 * @param <M> module type
	 * @since 0.1
	 */
	public static abstract class Provider<M extends OriginModule> {

		private final Class<? extends M> type;

		/**
		 * Construct a new provider.
		 *
		 * @param type module type
		 * @since 0.1
		 */
		protected Provider(Class<? extends M> type) {
			this.type = Preconditions.checkNotNull(type);
		}

		/**
		 * Module type.
		 *
		 * @return module type class
		 */
		final Class<? extends M> type() {
			return this.type;
		}

		/**
		 * Load new module.
		 * <p>Implementations should ensure this returns quickly.
		 *
		 * @param sdk SDK requesting module
		 * @param ctxt owning application context
		 * @return module instance
		 * @since 0.1
		 */
		@MainThread
		protected abstract M load(Origin sdk, Context ctxt);

		/**
		 * Reload existing module.
		 * <p>Implementation must reload {@code module} with new configuration, ensuring to return
		 * quickly.
		 *
		 * @param module module to reload
		 * @param ctxt owning application context
		 * @since 0.1
		 */
		@MainThread
		protected void reload(M module, Context ctxt) {
		}
	}

	/**
	 * Construct provider for a module.
	 *
	 * @param <T> module type
	 * @param type module type class
	 * @return new provider instance
	 * @throws IllegalArgumentException {@code type} does not define a {@code ofProvider} method
	 */
	@SuppressWarnings("unchecked")
	static <T extends OriginModule> Provider<T> providerOf(Class<T> type) {
		Provider<T> res;

		try {
			res = (Provider<T>) type.getDeclaredMethod("ofProvider")
				.invoke(null);
		} catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException err) {
			throw new IllegalArgumentException(String.format(
				Locale.ROOT,
				"unable to access %s::ofProvider()",
				type.getSimpleName()
			), err);
		}

		//noinspection DataFlowIssue
		Preconditions.checkArgument(
			type.isAssignableFrom(res.type),
			"%s provides module %s, expected %s",
			res.getClass(),
			res.type,
			type
		);
		return res;
	}

	private final String name;
	private final Origin sdk;

	/**
	 * Construct new module.
	 * <p>The module name, {@code name}, specified should be a unique non-{@linkplain
	 * String#isEmpty() empty} identifier for the module.
	 *
	 * @param name consistent module name
	 * @param sdk owning SDK
	 * @since 0.1
	 */
	protected OriginModule(String name, Origin sdk) {
		Preconditions.checkArgument(!TextUtils.isEmpty(name), "name must be specified");
		this.name = name;
		this.sdk = Preconditions.checkNotNull(sdk);
	}

	private OriginModule() {
		this.name = "";
		this.sdk = null;
	}

	/**
	 * Unique consistent module name.
	 *
	 * @return module name
	 * @since 0.1
	 */
	public final String name() {
		return this.name;
	}

	/**
	 * Owning SDK instance.
	 *
	 * @return SDK instance
	 * @since 0.1
	 */
	public final @NonNull Origin sdk() {
		return this.sdk;
	}

	/**
	 * Retrieve underlying application context.
	 * <p>Shorthand for:
	 * {@snippet lang="java" :
	 * sdk() // @link substring="sdk" target="#sdk()"
	 *     .context(); // @link substring="context" target="Origin#context()"
	 * }
	 *
	 * @return application context
	 * @throws IllegalStateException application context has been garbage collected
	 * @since 0.1
	 * @see #sdk()
	 * @see Origin#context()
	 */
	@RestrictTo(RestrictTo.Scope.SUBCLASSES)
	protected final Context context() {
		return this.sdk.context();
	}

	/**
	 * Try and retrieve underlying application context.
	 * <p>Shorthand for:
	 * {@snippet lang="java" :
	 * sdk() // @link substring="sdk" target="#sdk()"
	 *     .tryContext(); // @link substring="tryContext" target="Origin#tryContext()"
	 * }
	 *
	 * @return application context or {@code null} if context has been garbage collected
	 * @since 1.2
	 * @see #sdk()
	 * @see Origin#tryContext()
	 */
	@RestrictTo(RestrictTo.Scope.SUBCLASSES)
	protected final @Nullable Context tryContext() {
		return this.sdk().tryContext();
	}

	/**
	 * Perform operation with underlying application context, if available.
	 * <p>This invokes {@code fn} with the underlying application {@linkplain #context() context},
	 * returning the resulting value, if, and only if, there is an underlying context. When a
	 * context is not present, this simply returns {@code null}.
	 *
	 * @param <T> operation return value
	 * @param fn operation to perform
	 * @return operation result or {@code null}
	 * @since 0.1
	 * @see #context()
	 */
	@RestrictTo(RestrictTo.Scope.SUBCLASSES)
	protected final <T> @Nullable T applyContext(Function<Context, T> fn) {
		Context ctxt = this.tryContext();

		return ctxt != null ? fn.apply(ctxt) : null;
	}

	/**
	 * Perform non-value returning operation with underlying application context, if available.
	 * <p>Like {@link #applyContext(Function)}, however, {@code fn} need not return a value.
	 *
	 * @param fn operation to perform
	 * @return {@code true} if, and only if, operation was performed
	 * @since 0.1
	 * @see #applyContext(Function)
	 */
	@RestrictTo(RestrictTo.Scope.SUBCLASSES)
	@SuppressWarnings("UnusedReturnValue")
	protected final boolean acceptContext(Consumer<Context> fn) {
		return Preconditions.checkNotNullElse(
			applyContext(ctxt -> { fn.accept(ctxt); return true; }),
			Boolean.FALSE
		);
	}

	/**
	 * Register a {@linkplain OriginModuleEventCallback callback} to invoke when module fires an
	 * event.
	 * <p>Efficient equivalent of:
	 * {@snippet lang="java" :
	 *     this.sdk().registerModuleEventCallback(callback, new Pair<>(this, evt)); // @link substring="registerModuleEventCallback" target="Origin#registerModuleEventCallback(OriginModuleEventCallback, Pair)"
	 * }
	 *
	 * @param callback callback to register
	 * @param evt name of event to register to
	 * @throws IllegalArgumentException {@code evt} was not recognized or {@code callback} has
	 * already been registered with {@code evt}
	 * @since 1.2
	 * @see #unregisterEventCallback(OriginModuleEventCallback, String)
	 * @see Origin#registerModuleEventCallback(OriginModuleEventCallback, Pair)
	 */
	public final void
	registerEventCallback(OriginModuleEventCallback callback, @OriginModuleEventName String evt) {
		this.sdk.registerModuleEventCallback(callback, new Pair<>(this, evt));
	}

	/**
	 * Unregister a {@linkplain OriginModuleEventCallback callback} invoked when module fires
	 * an event.
	 * <p>Efficient equivalent of:
	 * {@snippet lang="java" :
	 *     this.sdk().unregisterModuleEventCallback(callback, new Pair<>(this, evt)); // @link substring="unregisterModuleEventCallback" target="Origin#unregisterModuleEventCallback(OriginModuleEventCallback, Pair)"
	 * }
	 *
	 * @param callback callback to unregister
	 * @param evt name of event to unregister from
	 * @throws IllegalArgumentException {@code callback} has not yet been {@linkplain
	 * #registerEventCallback(OriginModuleEventCallback, String) registered}
	 * @since 1.2
	 * @see #registerEventCallback(OriginModuleEventCallback, String)
	 * @see Origin#unregisterModuleEventCallback(OriginModuleEventCallback, Pair)
	 */
	public final void
	unregisterEventCallback(OriginModuleEventCallback callback, @OriginModuleEventName String evt) {
		this.sdk.unregisterModuleEventCallback(callback, new Pair<>(this, evt));
	}

	private String settingsKey() {
		return String.format(Locale.ROOT, "settings-%s", this.name);
	}

	/**
	 * Load persistent module settings {@code byte} array.
	 * <p>Variant of {@link #loadSettings(ProtobufDeserializer)}; however, the raw settings value
	 * is returned. If settings are present, a non-{@code null} buffer is returned, containing the
	 * settings value; otherwise, this returns {@code null}.
	 *
	 * @return settings value buffer or {@code null} if settings were not found
	 * @since 1.2
	 * @see #loadSettings(ProtobufDeserializer)
	 */
	@RestrictTo(RestrictTo.Scope.SUBCLASSES)
	protected final @Nullable ByteBuffer loadSettings() {
		SharedPreferences settings = this.sdk.settings();
		String val = settings.getString(this.settingsKey(), "");

		if (val.isEmpty())
			return null;

		try {
			return Flate.decompressZlib(
				ByteBuffer.wrap(Base64.decode(val, SETTINGS_BASE64_FLAGS)),
				true
			);
		} catch (RuntimeException err) {
			Logger.debug(TAG, "failed to load settings for module %s", this.name, err);
			settings.edit()
				.remove(this.settingsKey())
				.apply();
			return null;
		}
	}

	/**
	 * Store persistent module settings {@code byte} array.
	 * <p>Like {@link #storeSettings(ProtobufSerializable)}; however, {@code buff} is stored as-is
	 * for later retrieval using {@link #loadSettings()}.
	 *
	 * @param buff settings to store
	 * @since 1.1
	 * @see #storeSettings(ProtobufSerializable)
	 */
	@RestrictTo(RestrictTo.Scope.SUBCLASSES)
	protected final void storeSettings(ByteBuffer buff) {
		SharedPreferences.Editor edit = this.sdk.settings().edit();
		String key = this.settingsKey();

		if (!buff.hasRemaining()) {
			edit.remove(key);
		} else {
			ByteBuffer compSer = Flate.compressZlib(buff, Deflater.DEFAULT_COMPRESSION, true);
			String settVal =
				Base64.encodeToString(
					compSer.array(),
					compSer.arrayOffset() + compSer.position(),
					compSer.remaining(),
					SETTINGS_BASE64_FLAGS
				);

			edit.putString(key, settVal);
		}
		edit.apply();
	}

	/**
	 * Load persistent module settings.
	 * <p>If settings for {@code this} exist and are valid, then the persisted settings are loaded
	 * and deserialized; otherwise, {@code null} is returned. While this may be invoked on any
	 * thread, it is recommended to invoke this only on non-{@linkplain
	 * org.polygamma.android.origin.util.Sync#isMainThread() UI} threads.
	 *
	 * @param <T> settings type
	 * @param deser deserializer to deserialize settings with
	 * @return loaded settings value or {@code null}
	 * @since 0.1
	 * @see #storeSettings(ProtobufSerializable)
	 */
	@RestrictTo(RestrictTo.Scope.SUBCLASSES)
	@SuppressWarnings("overloads")
	protected final <T> @Nullable T loadSettings(ProtobufDeserializer<T> deser) {
		ByteBuffer rv = this.loadSettings();

		if (rv == null)
			return null;
		try {
			return deser.ofProtobuf(new ProtobufReader(rv));
		} catch (RuntimeException cause) {
			Logger.debug(TAG, "failed to load settings for module %s", this.name, cause);
			return null;
		}
	}

	/**
	 * Store persistent module settings.
	 * <p>If settings for {@code this} already exist, they are replaced with {@code val};
	 * otherwise, {@code val} is persisted as settings for {@code this}. While this may be
	 * invoked on any thread, it is recommended to invoke this only on non-{@linkplain
	 * org.polygamma.android.origin.util.Sync#isMainThread() UI} threads.
	 *
	 * @param val settings to store
	 * @since 1.2
	 * @see #loadSettings(ProtobufDeserializer)
	 */
	@RestrictTo(RestrictTo.Scope.SUBCLASSES)
	protected final void storeSettings(ProtobufSerializable val) {
		this.storeSettings(ProtobufWriter.serialize(val));
	}

	/**
	 * Resolve an identifier which can be used for persistent storage.
	 * <p>The identifier returned is guaranteed to be stable for the module with respect to SDK
	 * package and module {@linkplain #name() name}, containing only file-system safe characters.
	 *
	 * @return resolved identifier
	 * @since 1.2
	 */
	@RestrictTo(RestrictTo.Scope.SUBCLASSES)
	protected final String resolvePersistentId() {
		return String.format(
			Locale.ROOT,
			"%s-%s-%s",
			Origin.VENDOR,
			Origin.NAME,
			this.name.replace('.', '_')
		);
	}

	/**
	 * Resolve the cache directory of module.
	 * <p>The cache directory returned is unique to {@code this} module. The directory, including
	 * any parent directory, is <b>not</b> guaranteed to {@linkplain File#exists() exist}, and
	 * should be {@linkplain File#mkdirs() created} if required immediately.
	 *
	 * @return cache directory
	 * @throws IllegalStateException application context has been garbage collected
	 * @since 1.2
	 */
	@RestrictTo(RestrictTo.Scope.SUBCLASSES)
	protected final File resolveCacheDirectory() {
		return new File(
			this.context().getCacheDir(),
			String.format(
				Locale.ROOT,
				"%s-%s%s%s",
				Origin.VENDOR,
				Origin.NAME,
				File.separator,
				this.name.replace('.', '_')
			)
		);
	}

	/**
	 * Register an event.
	 * <p>This registers a new event, with the name {@code name}, for this module. The resulting
	 * {@linkplain OriginModuleEventBus bus} can be used to {@linkplain
	 * OriginModuleEventBus#submit(Object) fire} the event to {@linkplain
	 * Origin#registerModuleEventCallback(OriginModuleEventCallback, Pair) registered} listeners.
	 * <p>An event may be either <i>sticky</i> or non-<i>sticky</i>. Sticky events are collapsable
	 * and fire to newly registered listeners. Non-sticky events, however, are not collapsable and
	 * are not fired to newly registered listeners.
	 *
	 * @param name event name
	 * @param sticky {@code true} if, and only if, event is sticky
	 * @return bus with which event can be fired to consumers
	 * @throws IllegalArgumentException {@code name} has already been registered
	 * @since 0.1
	 * @see Origin#registerModuleEventCallback(OriginModuleEventCallback, Pair)
	 * @see Origin#unregisterModuleEventCallback(OriginModuleEventCallback, Pair)
	 */
	@RestrictTo(RestrictTo.Scope.SUBCLASSES)
	protected final OriginModuleEventBus
	registerEvent(@OriginModuleEventName String name, boolean sticky) {
		return this.sdk.registerModuleEvent(this, name, sticky);
	}

	/**
	 * Destroy module.
	 * <p>This is called at most <i>once</i> by the owning {@linkplain #sdk() SDK}, when the SDK
	 * is undergoing termination. This should return <i>only</i> when the module is destroyed.
	 *
	 * @since 0.1
	 */
	@WorkerThread
	protected void destroy() {
	}

	@Override
	public @NonNull String toString() {
		return this.name;
	}
}
