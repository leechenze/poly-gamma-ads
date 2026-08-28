// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.util;

import android.os.Build;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Pair;

import androidx.annotation.Nullable;
import androidx.core.util.Function;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * Compatibility methods for {@link android.util} and {@link java.util}.
 *
 * @since 0.1
 */
public class CollectionsCompat {

	private static final String[] EMPTY_STRING_ARRAY = new String[0];

	/**
	 * Collect elements into an array or return an empty array.
	 *
	 * @param <T> element type
	 * @param vals collection to collect elements from
	 * @param empty empty array
	 * @return array of elements collected from {@code vals} or {@code empty} if {@code vals} is
	 * {@linkplain Collection#isEmpty() empty}
	 * @since 1.2
	 */
	public static <T> T[] toArrayOrEmpty(Collection<T> vals, T[] empty) {
		return vals.isEmpty() ? empty : vals.toArray(empty);
	}

	/**
	 * Collect {@link String string} elements into an array or return a shared empty array.
	 *
	 * @param vals collection to collect elements from
	 * @return array of elements collected from {@code vals} or shared empty array if {@code vals}
	 * is {@linkplain Collection#isEmpty() empty}
	 * @since 1.2
	 */
	public static String[] toStringArrayOrEmpty(Collection<String> vals) {
		return toArrayOrEmpty(vals, EMPTY_STRING_ARRAY);
	}

	/**
	 * Construct a new {@link ArraySet} when available, otherwise, {@link HashSet}, both initialized
	 * from an existing collection.
	 *
	 * @param <T> element type
	 * @param vals collection to initialize from
	 * @return resulting set
	 * @since 1.2
	 */
	public static <T> Set<T> newArraySet(Collection<T> vals) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
			return new ArraySet<>(vals);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			ArraySet<T> rv = new ArraySet<>(vals.size());

			rv.addAll(vals);
			return rv;
		}
		return new HashSet<>(vals);
	}

	/**
	 * Construct a new {@link ArraySet} when available, otherwise, {@link HashSet}.
	 *
	 * @param <T> element type
	 * @param initialCap initial capacity
	 * @return resulting set
	 * @since 0.1
	 */
	public static <T> Set<T> newArraySet(int initialCap) {
		return (
			Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? new ArraySet<>(initialCap) :
			new HashSet<>(initialCap)
		);
	}

	/**
	 * Construct a new zero capacity {@link ArraySet} when available, otherwise, {@link HashSet}.
	 * <p>Shorthand for:
	 * {@snippet lang="java" :
	 * newArraySet(0) // @link substring="newArraySet" target="#newArraySet(int)"
	 * }
	 *
	 * @param <T> element type
	 * @return resulting set
	 * @since 0.1
	 * @see #newArraySet(int)
	 */
	@SuppressWarnings("JavadocDeclaration")
	public static <T> Set<T> newArraySet() {
		return newArraySet(0);
	}

	/**
	 * Construct new {@link ArraySet} or {@link HashSet} prepopulated with elements.
	 *
	 * @param <T> element type
	 * @param vals elements to prepopulate set with
	 * @return resulting set
	 * @since 1.2
	 * @see #newArraySet(int)
	 */
	@SafeVarargs
	@SuppressWarnings("varargs")
	public static <T> Set<T> newArraySetOf(T... vals) {
		Set<T> rv = newArraySet(vals.length);

		Collections.addAll(rv, vals);
		return rv;
	}

	/**
	 * Construct new immutable set with elements copied from an existing collection.
	 *
	 * @param <T> element type
	 * @param src existing collection to copy elements from
	 * @return resulting set
	 * @since 1.2
	 */
	public static <T> Set<T> unmodifiableSetCopyOf(Collection<T> src) {
		return
			Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? Set.copyOf(src) :
			Collections.unmodifiableSet(newArraySet(src));
	}

	/**
	 * Construct new immutable set with zero or more elements.
	 *
	 * @param <T> element type
	 * @param vals elements to include within set
	 * @return resulting set
	 * @since 1.2
	 */
	@SafeVarargs
	@SuppressWarnings("varargs")
	public static <T> Set<T> unmodifiableSetOf(T... vals) {
		return
			Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ? Set.of(vals) :
			unmodifiableSetCopyOf(newArraySetOf(vals));
	}

	/**
	 * Construct new immutable set with three elements.
	 *
	 * @param <T> element type
	 * @param a first element
	 * @param b second element
	 * @param c third element
	 * @return resulting set
	 * @since 1.2
	 */
	public static <T> Set<T> unmodifiableSetOf(T a, T b, T c) {
		return
			Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ? Set.of(a, b, c) :
			unmodifiableSetCopyOf(newArraySetOf(a, b, c));
	}

	/**
	 * Construct new immutable set with two elements.
	 *
	 * @param <T> element type
	 * @param a first element
	 * @param b second element
	 * @return resulting set
	 * @since 1.2
	 */
	public static <T> Set<T> unmodifiableSetOf(T a, T b) {
		return
			Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ? Set.of(a, b) :
			unmodifiableSetCopyOf(newArraySetOf(a, b));
	}

	/**
	 * Construct new immutable set with one element.
	 *
	 * @param <T> element type
	 * @param a first element
	 * @return resulting set
	 * @since 1.2
	 */
	public static <T> Set<T> unmodifiableSetOf(T a) {
		return
			Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ? Set.of(a) :
			Collections.singleton(a);
	}

	/**
	 * Retrieve {@linkplain Set#isEmpty() empty} immutable set.
	 *
	 * @param <T> element type
	 * @return empty set instance, possibly immutable
	 * @since 1.2
	 */
	public static <T> Set<T> unmodifiableSetOf() {
		return
			Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ? Set.of() :
			Collections.emptySet();
	}

	/**
	 * Construct new array map with mappings copied from an existing map.
	 *
	 * @param <K> key type
	 * @param <V> value type
	 * @param src map to copy mappings from
	 * @return resulting map
	 * @since 1.2
	 */
	public static <K, V> ArrayMap<K, V> arrayMapCopyOf(Map<K, V> src) {
		ArrayMap<K, V> rv = new ArrayMap<>(src.size());

		rv.putAll(src);
		return rv;
	}

	/**
	 * Invoke {@link Map#putIfAbsent(Object, Object)}.
	 *
	 * @param <K> key type
	 * @param <V> value type
	 * @param dst map to operate on
	 * @param key key to associate value with
	 * @param value value to associate
	 * @return {@code null} if {@code key} was not {@linkplain Map#containsKey(Object) present} in
	 * {@code map} or was mapped to {@code null}
	 * @since 0.1
	 */
	public static <K, V> @Nullable V putIfAbsent(Map<K, V> dst, K key, V value) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
			return dst.putIfAbsent(key, value);

		V curr = dst.get(key);

		if (curr == null)
			dst.put(key, value);
		return curr;
	}

	/**
	 * Invoke {@link Map#computeIfAbsent(Object, java.util.function.Function)}.
	 *
	 * @param <K> key type
	 * @param <V> value type
	 * @param dst map to operate on
	 * @param key key to associate value with
	 * @param compute function to compute value with
	 * @return current value mapped to {@code key}, newly mapped value of {@code key}, or {@code
	 * null} if {@code compute} returned {@code null}
	 * @since 0.1
	 */
	public static <K, V> V
	computeIfAbsent(Map<K, V> dst, K key, Function<? super K, ? extends V> compute) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
			return dst.computeIfAbsent(key, compute::apply);

		V curr = dst.get(key);

		if (curr == null) {
			curr = compute.apply(key);
			if (curr != null)
				dst.put(key, curr);
		}
		return curr;
	}

	/**
	 * Invoke {@link Map#merge(Object, Object, BiFunction)}.
	 *
	 * @param <K> key type
	 * @param <V> value type
	 * @param dst map to operate on
	 * @param key key to remap value of
	 * @param val value to remap with
	 * @param remap function to remap old and new values
	 * @return updated value
	 * @since 0.1
	 */
	public static <K, V> V
	merge(Map<K, V> dst, K key, V val, Function<? super Pair<V, V>, ? extends V> remap) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
			return dst.merge(key, val, (oldVal, newVal) -> remap.apply(new Pair<>(oldVal, newVal)));

		//noinspection ResultOfMethodCallIgnored
		Preconditions.checkNotNull(val);

		V curr = dst.get(key);
		V upd = curr == null ? val : remap.apply(new Pair<>(curr, val));

		if (upd == null)
			dst.remove(key);
		else
			dst.put(key, upd);
		return upd;
	}

	private CollectionsCompat() {
	}
}
