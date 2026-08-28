// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.core;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.ArrayMap;
import android.util.Base64;
import android.util.Pair;

import androidx.annotation.GuardedBy;
import androidx.annotation.Nullable;
import androidx.core.os.ExecutorCompat;
import androidx.core.util.Consumer;

import com.google.common.util.concurrent.ListenableFuture;

import org.polygamma.android.origin.adcom.ContentCategories;
import org.polygamma.android.origin.adcom.context.App;
import org.polygamma.android.origin.protobuf.ProtobufDeserializer;
import org.polygamma.android.origin.protobuf.ProtobufSerializable;
import org.polygamma.android.origin.util.CollectionsCompat;
import org.polygamma.android.origin.util.Finalizer;
import org.polygamma.android.origin.util.Futures;
import org.polygamma.android.origin.util.ListeningExecutor;
import org.polygamma.android.origin.util.Logger;
import org.polygamma.android.origin.util.Preconditions;
import org.polygamma.android.origin.util.Strings;
import org.polygamma.android.origin.util.Sync;

import java.lang.ref.SoftReference;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Origin Android software development kit (SDK) entry-point.
 * <p>There may be a <i>single</i> SDK instance active at any given point in time. An instance
 * can be initialized using {@link #initialize(Context, OriginModule.Provider[])}, and extended
 * with {@linkplain OriginModule modules}. The <i>active</i> instance can be retrieved using
 * {@link #current()} or {@link #tryCurrent()}. An instance can be destroyed using {@link
 * #shutdown()}, if forceful reinitialization is required.
 * <h2>Modules</h2>
 * <p>The primary purpose of an SDK instance is to provide a facility for managing modules, context,
 * and work scheduling. As such, functionality useful for interacting with the Origin Ad Platform
 * is provided by {@linkplain OriginModule modules}.
 * <p>Modules may be loaded during {@linkplain #initialize(Context, OriginModule.Provider[])
 * initialization} <i>or</i> after initialization, using {@link #loadModule(Class)} and friends.
 * While a module is being loaded, it may recursively load other modules. This builds a dependency
 * tree for modules, which is enforced during {@linkplain #shutdown() shutdown}. Loaded modules can
 * be retrieved using {@link #findModule(Class)}.
 * <p>All modules support reloading. Invoking {@link #loadModule(OriginModule.Provider)} with a
 * provider configured materially different than the current module configuration will force the
 * module to be reloaded. The module instance itself is not changed, only the internal state of the
 * module is modified.
 * <p>Modules additionally may export <i>events</i>. These events can be listened for using {@link
 * #registerModuleEventCallback(OriginModuleEventCallback, Pair)}. See {@link OriginModule} for
 * more information.
 *
 * @since 0.1
 */
public final class Origin {

	private static final String TAG = Origin.class.getSimpleName();

	/**
	 * SDK version number.
	 *
	 * @since 0.1
	 */
	public static final String VERSION = BuildConfig.ORIGIN_SDK_VERSION;

	/**
	 * SDK name.
	 *
	 * @since 1.2
	 */
	public static final String NAME = BuildConfig.ORIGIN_SDK_NAME;

	/**
	 * SDK vendor.
	 *
	 * @since 1.2
	 */
	public static final String VENDOR = BuildConfig.ORIGIN_SDK_VENDOR;

	private static @Nullable Origin instance;

	/**
	 * Retrieve current SDK instance, if available.
	 * <p>Unlike {@link #current()}, this returns {@code null} if an SDK has not yet been
	 * {@linkplain #initialize(Context, OriginModule.Provider[]) initialized} or has been
	 * {@linkplain #shutdown() shutdown}.
	 *
	 * @return SDK instance or {@code null} if not available
	 * @since 1.1
	 * @see #current()
	 */
	public static @Nullable Origin tryCurrent() {
		return instance;
	}

	/**
	 * Retrieve current SDK instance.
	 *
	 * @return SDK instance
	 * @throws IllegalStateException SDK has not yet been {@linkplain
	 * #initialize(Context, OriginModule.Provider[]) initialized}
	 * @since 0.1
	 * @see #tryCurrent()
	 */
	public static Origin current() {
		Origin curr = tryCurrent();

		Preconditions.checkState(curr != null, "SDK not initialized");
		return curr;
	}

	/**
	 * Initialize SDK, if not already initialized.
	 * <p>If another SDK instance has already been initialized, it is used; otherwise, a new SDK
	 * instance is initialized. Modules are loaded or reloaded based on {@code providers}.
	 *
	 * @param ctxt context to initialize with
	 * @param providers zero or more providers to pre-initialize modules with
	 * @return SDK instance
	 * @since 0.1
	 * @see #loadModule(OriginModule.Provider)
	 */
	public static Origin initialize(Context ctxt, OriginModule.Provider<?>... providers) {
		Origin sdk;

		synchronized (Origin.class) {
			sdk = instance;

			if (sdk == null || sdk.isShutdown()) {
				sdk = new Origin(ctxt);
				instance = sdk;
			}
		}

		if (!Sync.isMainThread()) {
			Origin curr = sdk;

			Futures.await(sdk.runInForeground(() -> {
				for (OriginModule.Provider<?> provider : providers)
					curr.loadModule(provider);
			}));
		} else {
			for (OriginModule.Provider<?> provider : providers)
				sdk.loadModule(provider);
		}
		return sdk;
	}

	/*
	 * Shutdown works in 2 ways:
	 *
	 * 1) Reference underlying `contextReference` gets garbage collected (unlikely).
	 *
	 * 2) `shutdown` is set to `true` by `shutdown()`.
	 *
	 * When either happens, `listenShutdown()` wakes up and begins the shutdown process.
	 */
	private final SoftReference<Context> contextReference;
	private final Runnable shutdown;
	private final ListeningExecutor foregroundExecutor;
	private final ListeningExecutor backgroundIoExecutor;
	private final ListeningExecutor backgroundExecutor;
	/*
	 * Mapping of `OriginModule.Provider::type()` to module, ordered by load-order of modules.
	 * The mapped value may be either a `OriginModule`, if it has been loaded, or `LOADING_MODULE`
	 * if the module is currently being loaded. The `LOADING_MODULE` constant is used to track
	 * circular module dependencies.
	 */
	private final LinkedHashMap<Class<? extends OriginModule>, OriginModule> modules;
	private final ReentrantReadWriteLock moduleEventLock;
	@GuardedBy("moduleEventLock")
	private final ArrayMap<@OriginModuleEventName String, OriginModuleEventBus> moduleEventBuses;
	private final CountDownLatch shutdownLatch;
	private @Nullable App app;
	private @Nullable SharedPreferences settings;

	@SuppressWarnings("this-escape")
	private Origin(Context ctxt) {
		if (!(ctxt instanceof Application))
			ctxt = Preconditions.checkNotNull(ctxt).getApplicationContext();

		Handler handler = new Handler(ctxt.getMainLooper());

		this.contextReference = new SoftReference<>(ctxt);
		this.foregroundExecutor = new ListeningExecutor(handler, ExecutorCompat.create(handler));
		this.backgroundIoExecutor =
			new ListeningExecutor(handler, Executors.newCachedThreadPool());
		this.backgroundExecutor =
			Build.VERSION.SDK_INT < Build.VERSION_CODES.N ? this.backgroundIoExecutor :
			new ListeningExecutor(
				handler,
				// When debugging, we want to stress this pool to make sure there are no deadlocks.
				BuildConfig.DEBUG ? new ForkJoinPool(1) :
				ForkJoinPool.commonPool()
			);
		this.modules = new LinkedHashMap<>();
		this.moduleEventLock = new ReentrantReadWriteLock();
		this.moduleEventBuses = new ArrayMap<>();
		this.shutdownLatch = new CountDownLatch(1);
		this.shutdown = Finalizer.register(ctxt, this::doShutdown);
	}

	/**
	 * Owning application context.
	 *
	 * @return application context
	 * @throws IllegalStateException application context has been garbage collected
	 * @since 0.1
	 * @see #tryContext()
	 */
	public Context context() {
		Context ctxt = this.contextReference.get();

		Preconditions.checkState(ctxt != null);
		return ctxt;
	}

	/**
	 * Try and retrieve owning application context.
	 *
	 * @return application context or {@code null} if context has been garbage collected
	 * @since 1.2
	 * @see #context()
	 */
	public @Nullable Context tryContext() {
		return this.contextReference.get();
	}

	/**
	 * Test whether SDK has been shut down.
	 *
	 * @return {@code true} if, and only if, SDK has shut down
	 * @since 0.1
	 * @see #shutdown()
	 */
	public boolean isShutdown() {
		return this.contextReference.get() == null;
	}

	/**
	 * Query application for Origin metadata.
	 * <p>The resulting mapping is guaranteed to have an entry for each id specified in {@code
	 * ids}. Any field id for which metadata was not found, the mapped value is guaranteed to be
	 * an empty string. Ids are required to be alphanumeric, with optional hyphens.
	 *
	 * @param ids metadata field ids to query
	 * @return resulting mapping
	 */
	Map<String, String> queryAppMetadata(String... ids) {
		ArrayMap<String, String> rv = new ArrayMap<>(ids.length);
		Bundle metadata = null;

		try {
			Context ctxt = this.context();
			PackageManager pman = ctxt.getPackageManager();

			metadata = pman.getApplicationInfo(ctxt.getPackageName(), PackageManager.GET_META_DATA)
				.metaData;
		} catch (Throwable cause) {
			Logger.warn(TAG, "failed to query metadata", cause);
		}

		for (String id : ids) {
			String val = "";

			if (metadata != null) {
				String key = VENDOR.replace('_', '-') + '.' + NAME.replace('_', '-') + '.' + id;

				val = metadata.getString(key, "");
				if (val.isEmpty()) {
					val = metadata.getString(key.replace('-', '_'), "");
					if (val.isEmpty()) {
						// XXX: included for compatibility with pre-1.2
						val = metadata.getString("poly-gamma.origin." + id, "");
					}
				}
			}
			rv.put(id, val);
		}
		return rv;
	}

	/**
	 * Probe executing application.
	 * <p>Upon return, the application {@linkplain #app descriptor} is updated to the returned
	 * value.
	 *
	 * @return app descriptor
	 */
	private App probeApp() {
		Context ctxt = this.context();
		PackageManager pm = ctxt.getPackageManager();
		String pkg = ctxt.getPackageName();
		ApplicationInfo info = null;
		App.Builder rv = App.ofBuilder()
			.storeId(pkg)
			.categoryTaxonomy(ContentCategories.TAXONOMY);

		try {
			rv.name(pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString())
				.version(Strings.nullToEmpty(pm.getPackageInfo(pkg, 0).versionName));
			info = ctxt.getApplicationInfo();
		} catch (PackageManager.NameNotFoundException err) {
			Logger.warn(TAG, "application info unavailable for %s", pkg, err);
		}

		if (info != null) {
			rv.debuggable((info.flags & (
				ApplicationInfo.FLAG_DEBUGGABLE |
				ApplicationInfo.FLAG_TEST_ONLY
			)) != 0)
				.system((info.flags & (
					ApplicationInfo.FLAG_SYSTEM |
					ApplicationInfo.FLAG_UPDATED_SYSTEM_APP
				)) != 0);
		}

		Map<String, String> meta = this.queryAppMetadata("application-id", "publisher-id");

		this.app = rv.id(meta.get("application-id"))
			.publisherId(meta.get("publisher-id"))
			.build();
		return this.app;
	}

	/**
	 * Underlying application descriptor.
	 * <p>If an explicit descriptor has not been {@linkplain #app(App) specified}, the underlying
	 * application is probed and its context is returned. The {@link App#id() application} and
	 * {@link App#publisherId() publisher} identifiers, by default, are initialized from the
	 * {@code poly-gamma.origin.application-id} and {@code poly-gamma.origin.publisher-id}
	 * meta fields.
	 *
	 * @return descriptor
	 * @since 0.1
	 * @see #app(App)
	 */
	public App app() {
		return Preconditions.checkNotNullElseGet(this.app, this::probeApp);
	}

	/**
	 * Set underlying application descriptor.
	 *
	 * @param app descriptor or {@code null} to probe application
	 * @return {@code this}
	 * @since 0.1
	 * @see #app()
	 */
	public Origin app(@Nullable App app) {
		this.app = Preconditions.checkNotNullElseGet(app, this::probeApp);
		return this;
	}

	/**
	 * Retrieve settings directory name.
	 *
	 * @param name base settings name
	 * @return settings directory
	 */
	private String settingsName(String name) {
		String pkgName = this.app().storeId();
		String procName = Sync.currentProcessName();

		if (pkgName.equals(procName) || procName.isEmpty())
			return name;
		if (procName.startsWith(pkgName))
			procName = procName.substring(pkgName.length());

		String descr =
			Base64.encodeToString(
				procName.getBytes(StandardCharsets.UTF_8),
				Base64.NO_PADDING |
				Base64.NO_WRAP |
				Base64.URL_SAFE
			);

		return String.format(Locale.ROOT, "%s.%s", name, descr);
	}

	/**
	 * Initialize settings from previous settings store, if any.
	 */
	@SuppressWarnings("unchecked")
	private void initializeSettings() {
		if (this.settings == null)
			return;

		SharedPreferences.Editor dst = this.settings.edit();

		try {
			dst.putBoolean("__INITIALIZED__", true);

			for (String srcName : new String[] {
				this.settingsName("poly-gamma.origin"),
				BuildConfig.LIBRARY_PACKAGE_NAME
			}) {
				SharedPreferences src =
					this.context().getSharedPreferences(srcName, Context.MODE_PRIVATE);
				Map<String, ?> all;

				try {
					all = new HashMap<>(src.getAll());
					src.edit().clear().apply();
				} catch (Throwable cause) {
					Logger.debug(TAG, "failed to retrieve settings %s", srcName, cause);
					continue;
				}

				if (all.isEmpty())
					continue;
				for (Map.Entry<String, ?> entry : all.entrySet()) {
					String name = entry.getKey();
					Object val = entry.getValue();

					if (val instanceof Double || val instanceof Float)
						dst.putFloat(name, ((Number) val).floatValue());
					else if (val instanceof Long)
						dst.putLong(name, (Long) val);
					else if (val instanceof Number)
						dst.putInt(name, ((Number) val).intValue());
					else if (val instanceof Boolean)
						dst.putBoolean(name, (Boolean) val);
					else if (val instanceof String)
						dst.putString(name, (String) val);
					else if (val instanceof Set)
						dst.putStringSet(name, (Set<String>) val);
					else
						Logger.warn(TAG, "unexpected preference value: %s", val);
				}
			}
		} finally {
			dst.apply();
		}
	}

	/**
	 * Retrieve SDK settings directory.
	 *
	 * @return settings directory
	 * @see OriginModule#loadSettings(ProtobufDeserializer)
	 * @see OriginModule#storeSettings(ProtobufSerializable)
	 */
	SharedPreferences settings() {
		return Preconditions.checkNotNullElseGet(this.settings, () -> {
			this.settings = this.context().getSharedPreferences(
				this.settingsName(VENDOR + '-' + NAME),
				Context.MODE_PRIVATE
			);

			if (this.settings.contains("__INITIALIZED__"))
				return this.settings;

			try {
				this.initializeSettings();
			} catch (Exception err) {
				Logger.warn(TAG, "failed to initialize settings", err);
			}
			return this.settings;
		});
	}

	/**
	 * Foreground task executor.
	 * <p>The executor returned always schedules work on the main (UI) thread.
	 *
	 * @return task executor
	 * @since 0.1
	 */
	public ListeningExecutor foregroundExecutor() {
		return this.foregroundExecutor;
	}

	/**
	 * Background I/O task executor.
	 * <p>The executor returned always schedules work on a non-main (UI) thread. Use the returned
	 * executor, in place of the {@linkplain #backgroundExecutor() compute} task executor when the
	 * submitted tasks may blocking I/O.
	 *
	 * @return task executor
	 * @since 1.2
	 */
	public ListeningExecutor backgroundIoExecutor() {
		return this.backgroundIoExecutor;
	}

	/**
	 * Background task executor.
	 * <p>The executor returned always schedules work on a non-main (UI) thread. Tasks submitted
	 * to this pool <i>should</i> not perform any blocking operation. If part of a task <i>may</i>
	 * block, the respective part should be executed within a {@linkplain
	 * #callIoInBackground(Callable) blocking} context.
	 *
	 * @return task executor
	 * @since 0.1
	 */
	public ListeningExecutor backgroundExecutor() {
		return this.backgroundExecutor;
	}

	private static <V> ListenableFuture<V>
	callNowOrSubmit(ListeningExecutor exec, Callable<V> cmd, boolean now) {
		if (!now)
			return exec.submit(cmd);
		try {
			return Futures.of(cmd.call());
		} catch (Exception err) {
			return Futures.ofError(err);
		}
	}

	private static ListenableFuture<?>
	runNowOrSubmit(ListeningExecutor exec, Runnable cmd, boolean now) {
		return callNowOrSubmit(exec, Executors.callable(cmd), now);
	}

	/**
	 * Execute task on main (UI) thread.
	 * <p>If {@linkplain Thread#currentThread() current} thread is main thread, then {@code cmd}
	 * is {@linkplain Runnable#run() executed} inline, and an {@linkplain
	 * ListenableFuture#isDone() immediate} future is returned; otherwise, {@code cmd} is
	 * {@linkplain ListeningExecutor#submit(Runnable) submitted} for execution on the {@linkplain
	 * #foregroundExecutor() foreground} executor.
	 *
	 * @param cmd task to execute
	 * @return completion future
	 * @throws java.util.concurrent.RejectedExecutionException current thread is not main thread
	 * and foreground executor has {@linkplain ListeningExecutor#isShutdown() shut down}
	 * @since 0.1
	 * @see #foregroundExecutor()
	 * @see #callInForeground(Callable)
	 * @see #runInBackground(Runnable)
	 */
	@SuppressWarnings("UnusedReturnValue")
	public ListenableFuture<?> runInForeground(Runnable cmd) {
		return runNowOrSubmit(this.foregroundExecutor, cmd, Sync.isMainThread());
	}

	/**
	 * Execute value computing task on main (UI) thread.
	 * <p>Like {@link #runInForeground(Runnable)}; however, upon success the future returned
	 * completes with the value returned by {@code cmd}.
	 *
	 * @param <V> computed value type
	 * @param cmd task to execute
	 * @return completion future
	 * @throws java.util.concurrent.RejectedExecutionException current thread is not main thread
	 * and foreground executor has {@linkplain ListeningExecutor#isShutdown() shut down}
	 * @since 0.1
	 * @see #foregroundExecutor()
	 * @see #runInForeground(Runnable)
	 * @see #callInBackground(Callable)
	 */
	public <V> ListenableFuture<V> callInForeground(Callable<V> cmd) {
		return callNowOrSubmit(this.foregroundExecutor, cmd, Sync.isMainThread());
	}

	/**
	 * Execute task on non-main (UI) thread.
	 * <p>If {@linkplain Thread#currentThread() current} thread is not main thread, then {@code
	 * cmd} is {@linkplain Runnable#run() executed} inline, and an {@linkplain
	 * ListenableFuture#isDone() immediate} future is returned; otherwise, {@code cmd} is
	 * {@linkplain ListeningExecutor#submit(Runnable) submitted} for execution on the {@linkplain
	 * #backgroundExecutor() background} executor.
	 *
	 * @param cmd task to execute
	 * @return completion future
	 * @throws java.util.concurrent.RejectedExecutionException current thread is main thread
	 * and background executor has {@linkplain ListeningExecutor#isShutdown() shut down}
	 * @since 0.1
	 * @see #backgroundExecutor()
	 * @see #callInBackground(Callable)
	 * @see #runInForeground(Runnable)
	 */
	@SuppressWarnings("UnusedReturnValue")
	public ListenableFuture<?> runInBackground(Runnable cmd) {
		return runNowOrSubmit(this.backgroundExecutor, cmd, !Sync.isMainThread());
	}

	/**
	 * Execute value computing task on non-main (UI) thread.
	 * <p>Like {@link #runInBackground(Runnable)}; however, upon success the future returned
	 * completes with the value returned by {@code cmd}.
	 *
	 * @param <V> computed value type
	 * @param cmd task to execute
	 * @return completion future
	 * @throws java.util.concurrent.RejectedExecutionException current thread is main thread
	 * and background executor has {@linkplain ListeningExecutor#isShutdown() shut down}
	 * @since 0.1
	 * @see #backgroundExecutor()
	 * @see #runInBackground(Runnable)
	 * @see #callInForeground(Callable)
	 */
	public <V> ListenableFuture<V> callInBackground(Callable<V> cmd) {
		return callNowOrSubmit(this.backgroundExecutor, cmd, !Sync.isMainThread());
	}

	/**
	 * Execute I/O value computing task on non-main (UI) thread.
	 * <p>Like {@link #runIoInBackground(Runnable)}; however, upon success the future returned
	 * completes with the value returned by {@code cmd}.
	 *
	 * @param <V> computed value type
	 * @param cmd task to execute
	 * @return completion future
	 * @throws java.util.concurrent.RejectedExecutionException current thread is main thread
	 * and background executor has {@linkplain ListeningExecutor#isShutdown() shut down}
	 * @since 1.2
	 * @see #backgroundIoExecutor()
	 * @see #runIoInBackground(Runnable)
	 * @see #callInForeground(Callable)
	 */
	public <V> ListenableFuture<V> callIoInBackground(Callable<V> cmd) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
			return this.callInBackground(cmd);
		if (ForkJoinTask.getPool() != this.backgroundExecutor.executor())
			return this.backgroundIoExecutor.submit(cmd);
		return ManagedCallableBlocker.managedBlock(cmd);
	}

	/**
	 * Execute I/O task on non-main (UI) thread.
	 * <p>This schedules execution of {@code cmd} on the background {@linkplain
	 * #backgroundIoExecutor() I/O} executor. If {@linkplain Thread#currentThread() current} thread
	 * is a compute thread, this attempts to execute {@code cmd} inline, if possible; otherwise,
	 * deferring to the I/O executor.
	 *
	 * @param cmd task to execute
	 * @return completion future
	 * @throws java.util.concurrent.RejectedExecutionException current thread is main thread
	 * and background executor has {@linkplain ListeningExecutor#isShutdown() shut down}
	 * @since 1.2
	 * @see #backgroundExecutor()
	 * @see #callInBackground(Callable)
	 * @see #runInForeground(Runnable)
	 */
	public ListenableFuture<?> runIoInBackground(Runnable cmd) {
		return this.callIoInBackground(Executors.callable(cmd));
	}

	/**
	 * Find module by type.
	 * <p>This should be invoked on the {@linkplain Sync#isMainThread() main}. When invoked on a
	 * non-main thread, this is equivalent to:
	 * {@snippet lang="java" :
	 * callInForeground(() -> findModule(type)) // @link substring="callInForeground" target="#callInForeground(Callable)"
	 *     .get();
	 * }
	 *
	 * @param <T> module type
	 * @param type module type class
	 * @return loaded module, or {@code null} if, and only if, module has not yet been {@linkplain
	 * #loadModule(Class) loaded} <i>or</i> SDK has {@linkplain #isShutdown() shutdown}
	 * @throws IllegalStateException SDK has {@linkplain #isShutdown() shutdown}
	 * @since 0.1
	 */
	@SuppressWarnings({ "JavadocDeclaration", "unchecked" })
	public <T extends OriginModule> @Nullable T findModule(Class<T> type) {
		if (!Sync.isMainThread())
			return Futures.await(this.callInForeground(() -> this.findModule(type)));

		OriginModule module = this.modules.get(type);

		return module == null || module == OriginModule.LOADING_MODULE ? null : (T) module;
	}

	/**
	 * Load or reload a module using an existing provider.
	 * <p>If a module has already been loaded for {@code provider}, it is reloaded; otherwise,
	 * a new module is loaded for {@code provider}.
	 *
	 * @param <T> module type
	 * @param provider module provider
	 * @return resulting module
	 * @throws IllegalArgumentException {@code provider} does not provide a module of type {@code
	 * T}
	 * @throws IllegalStateException SDK has {@linkplain #isShutdown() shutdown}, there is a
	 * recursive dependency on the module, or another module with the same {@linkplain
	 * OriginModule#name() name} already exists
	 * @since 0.1
	 */
	@SuppressWarnings("unchecked")
	public <T extends OriginModule> T loadModule(OriginModule.Provider<T> provider) {
		if (!Sync.isMainThread())
			return Futures.await(this.callInForeground(() -> this.loadModule(provider)));

		Context ctxt = this.context();
		OriginModule module = this.modules.get(provider.type());

		Preconditions.checkState(
			module != OriginModule.LOADING_MODULE,
			"recursive dependency on module %s",
			provider.type()
		);
		if (module == null) {
			// protect against recursive dependency on the module we're loading
			this.modules.put(provider.type(), OriginModule.LOADING_MODULE);

			module = provider.load(this, ctxt);
			Preconditions.checkArgument(provider.type().isInstance(module));

			for (OriginModule curr : this.modules.values()) {
				Preconditions.checkState(
					!curr.name().equals(module.name()),
					"module with same name already exists"
				);
			}
			// preserve load order
			this.modules.remove(provider.type());
			this.modules.put(provider.type(), module);

			Logger.debug(TAG, "loaded module %s", module.name());
		} else {
			provider.reload((T) module, ctxt);
			Logger.debug(TAG, "reloaded module %s", module.name());
		}
		return (T) module;
	}

	/**
	 * Find or load a module by type.
	 * <p>If a module has already been loaded for {@code type}, it is returned; otherwise, a new
	 * module is loaded.
	 *
	 * @param <T> module type
	 * @param type module type class
	 * @return resulting module
	 * @throws IllegalArgumentException {@code type} is not a valid module
	 * @throws IllegalStateException SDK has {@linkplain #isShutdown() shutdown}, there is a
	 * recursive dependency on the module, or another module with the same {@linkplain
	 * OriginModule#name() name} already exists
	 * @since 0.1
	 */
	public <T extends OriginModule> T loadModule(Class<T> type) {
		return Preconditions.checkNotNullElseGet(
			this.findModule(type),
			() -> this.loadModule(OriginModule.providerOf(type))
		);
	}

	/**
	 * Generate event name for a module.
	 *
	 * @param event tuple of module and corresponding event, or {@code null}
	 * @return {@code "*"} if, and only if, {@code event} is {@code null}; otherwise, unique event
	 * name
	 */
	private static String
	moduleEventNameOf(@Nullable Pair<OriginModule, @OriginModuleEventName String> event) {
		return event == null ? "*" : String.format(
			Locale.ROOT,
			"%s.%s",
			event.first.name(),
			Preconditions.checkNotNull(event.second)
		).intern();
	}

	/**
	 * Register an event for a module.
	 *
	 * @param module module to register event for
	 * @param event event to register
	 * @param sticky {@code true} if, and only if, event is sticky
	 * @return bus with which event can be fired to consumers
	 * @throws IllegalArgumentException {@code event} has already been registered for {@code
	 * module}
	 */
	OriginModuleEventBus registerModuleEvent(
		OriginModule module,
		@OriginModuleEventName String event,
		boolean sticky
	) {
		String name = moduleEventNameOf(new Pair<>(module, event));
		OriginModuleEventBus bus = new OriginModuleEventBus(module, event, sticky);
		Lock write = this.moduleEventLock.writeLock();

		write.lock();
		try {
			Preconditions.checkArgument(
				!this.moduleEventBuses.containsKey(name),
				"event already registered"
			);
			this.moduleEventBuses.put(name, bus);
		} finally {
			write.unlock();
		}

		OriginModuleEventBus wildcardBus = this.moduleEventBuses.get("*");

		if (wildcardBus != null)
			wildcardBus.forEachCallback(bus::registerCallback);
		return bus;
	}

	private void withWildcardBus(Consumer<OriginModuleEventBus> cons) {
		Lock write = this.moduleEventLock.writeLock();

		write.lock();
		try {
			cons.accept(CollectionsCompat.computeIfAbsent(
				this.moduleEventBuses,
				"*",
				_key -> new OriginModuleEventBus(OriginModule.LOADING_MODULE, "*", false)
			));
		} finally {
			write.unlock();
		}
	}

	private static void
	registerModuleEventCallbackWith(OriginModuleEventBus bus, OriginModuleEventCallback callback) {
		Preconditions.checkArgument(
			bus.registerCallback(callback),
			"callback was already registered"
		);
	}

	/**
	 * Register a {@linkplain OriginModuleEventCallback callback} to invoke when a {@linkplain
	 * OriginModule module} fires an event.
	 * <p>If {@code event} is non-{@code null}, then {@code callback} is invoked whenever the
	 * module and event specified in {@link Pair#first event.first} and {@link Pair#second
	 * event.second}, respectively, is fired. When {@code event} is {@code null}, {@code callback}
	 * is invoked when any module fires an event.
	 * <p>Note that some events are <i>sticky</i>. When {@code event} is {@code null} or {@code
	 * event.second} targets such an event, {@code callback} is scheduled for immediate invocation
	 * if, and only if, the sticky event had previously fired.
	 * <p>The specified callback can be unregistered, after this returns, using {@link
	 * #unregisterModuleEventCallback(OriginModuleEventCallback, Pair)}.
	 *
	 * @param callback callback to invoke
	 * @param event event filter to invoke callback for or {@code null} to invoke callback for all
	 * module events
	 * @throws IllegalArgumentException {@code event} was not recognized or {@code callback} has
	 * already been registered with {@code event}
	 * @since 0.1
	 * @see #unregisterModuleEventCallback(OriginModuleEventCallback, Pair)
	 */
	public void registerModuleEventCallback(
		OriginModuleEventCallback callback,
		@Nullable Pair<OriginModule, @OriginModuleEventName String> event
	) {
		String name = moduleEventNameOf(event);
		OriginModuleEventBus bus;
		Lock read = this.moduleEventLock.readLock();

		read.lock();
		try {
			bus = this.moduleEventBuses.get(name);
		} finally {
			read.unlock();
		}

		if (bus != null) {
			registerModuleEventCallbackWith(bus, callback);
		} else {
			Preconditions.checkArgument("*".equals(name), "unrecognized module event");
			this.withWildcardBus(wildcardBus -> {
				registerModuleEventCallbackWith(wildcardBus, callback);
				for (int i = 0; i < this.moduleEventBuses.size(); i++) {
					OriginModuleEventBus evBus = this.moduleEventBuses.valueAt(i);

					if (evBus != wildcardBus)
						evBus.registerCallback(callback);
				}
			});
		}
	}

	/**
	 * Unregister a {@linkplain OriginModuleEventCallback callback} invoked when a {@linkplain
	 * OriginModule module} fires an event.
	 * <p>Upon return, {@code callback} will not be invoked again for the specified event.
	 *
	 * @param callback callback to unregister
	 * @param event event filter to unregister from or {@code null} to unregister wildcard filter
	 * @throws IllegalArgumentException {@code callback} has not yet been {@linkplain
	 * #registerModuleEventCallback(OriginModuleEventCallback, Pair) registered}
	 * @since 0.1
	 * @see #registerModuleEventCallback(OriginModuleEventCallback, Pair)
	 */
	public void unregisterModuleEventCallback(
		OriginModuleEventCallback callback,
		@Nullable Pair<OriginModule, @OriginModuleEventName String> event
	) {
		String name = moduleEventNameOf(event);
		Lock write = this.moduleEventLock.writeLock();

		write.lock();
		try {
			OriginModuleEventBus bus = this.moduleEventBuses.get(name);

			Preconditions.checkArgument(
				bus != null && bus.unregisterCallback(callback),
				"callback not registered"
			);
			if ("*".equals(name)) {
				for (int i = 0; i < this.moduleEventBuses.size(); i++) {
					OriginModuleEventBus evBus = this.moduleEventBuses.valueAt(i);

					if (evBus != bus)
						evBus.unregisterCallback(callback);
				}
			}
		} finally {
			write.unlock();
		}
	}

	/**
	 * Initiate SDK shutdown.
	 * <p>If a shutdown was previously initiated, this simply returns; otherwise, this initiates
	 * the SDK shutdown. Shutdown is asynchronous; however, upon return, invocations of {@link
	 * #current()} will fail <i>and</i> invocations of {@link
	 * #initialize(Context, OriginModule.Provider[])} will initialize a new SDK instance.
	 *
	 * @since 0.1
	 */
	public void shutdown() {
		this.shutdown.run();
	}

	/**
	 * Await until SDK shutdown is complete, thread is {@linkplain Thread#interrupt() interrupted},
	 * or timeout occurs.
	 * <p>If {@linkplain #shutdown() shutdown} has already completed, this simply returns with
	 * {@code true}; otherwise, this will wait for at most a {@code unit} duration of {@code
	 * timeout} for shutdown to complete, returning {@code false} if timeout occurred before
	 * shutdown was complete. If the current thread is interrupted, this fails with {@link
	 * InterruptedException}.
	 *
	 * @param timeout maximum time to wait
	 * @param unit unit {@code timeout} is measured in
	 * @return {@code true} if, and only if, shutdown completed; otherwise, {@code false}
	 * @throws InterruptedException thread was interrupted while waiting
	 * @since 1.1
	 * @see #awaitShutdown()
	 */
	public boolean awaitShutdown(long timeout, TimeUnit unit) throws InterruptedException {
		return this.shutdownLatch.await(timeout, unit);
	}

	/**
	 * Await until SDK shutdown is complete or thread is {@linkplain Thread#interrupt()
	 * interrupted}.
	 * <p>Like {@link #awaitShutdown(long, TimeUnit)}; however, this waits indefinitely.
	 *
	 * @throws InterruptedException thread was interrupted while waiting
	 * @since 1.1
	 * @see #awaitShutdown(long, TimeUnit)
	 */
	public void awaitShutdown() throws InterruptedException {
		this.shutdownLatch.await();
	}

	/**
	 * Perform shutdown.
	 * <p>Upon return, SDK {@link #instance}, if equal to {@code this}, is set to {@code null}. All
	 * modules are {@linkplain OriginModule#destroy() destroyed}, and executors are shutdown.
	 */
	private void doShutdown0() {
		Logger.debug(TAG, "initiating shutdown");

		synchronized (Origin.class) {
			if (instance == this)
				instance = null;
		}

		try {
			Future<OriginModule[]> modulesFut =
				this.callInForeground(() -> {
					OriginModule[] rv = this.modules.values().toArray(new OriginModule[0]);

					this.modules.clear();
					return rv;
				});
			OriginModule[] modules = null;

			for (int i = 0; i < 5; i++) {
				try {
					modules = modulesFut.get(30, TimeUnit.SECONDS);
				} catch (InterruptedException err) {
					Logger.warn(TAG, "interrupted while awaiting modules clear");
				} catch (Exception err) {
					Logger.warn(TAG, "failed to clear modules", err);
					break;
				}
			}
			if (modules == null) {
				// try our best
				modules = this.modules.values().toArray(new OriginModule[0]);
				this.modules.clear();
			}

			for (int i = modules.length - 1; i >= 0; i--) {
				OriginModule module = modules[i];

				try {
					module.destroy();
					Logger.debug(TAG, "destroyed module %s", module.name());
				} catch (Exception err) {
					Logger.debug(TAG, "failed to unload module %s", module.name(), err);
				}
			}
		} catch (Exception err) {
			Logger.debug(TAG, "failed to unload modules", err);
		}

		for (ListeningExecutor exec : new ListeningExecutor[] {
			this.foregroundExecutor,
			this.backgroundIoExecutor
		}) {
			exec.shutdown();

			for (int i = 0; i < 2; i++) {
				try {
					if (exec.awaitTermination(30, TimeUnit.SECONDS))
						break;
					Logger.debug(TAG, "executor taking longer than 30 seconds to terminate");
					exec.shutdownNow();
				} catch (Exception err) {
					Logger.debug(TAG, "executor failed to shutdown", err);
				}
			}
		}

		((ExecutorService) this.backgroundIoExecutor).shutdownNow();
	}

	/**
	 * Perform shutdown.
	 */
	private void doShutdown() {
		try {
			this.doShutdown0();
		} catch (Exception err) {
			Logger.debug(TAG, "shutdown failed", err);
		} finally {
			this.contextReference.clear();
			this.shutdownLatch.countDown();
		}
	}
}
