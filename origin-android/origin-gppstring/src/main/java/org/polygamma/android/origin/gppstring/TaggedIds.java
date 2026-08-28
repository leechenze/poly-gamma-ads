// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.gppstring;

import androidx.annotation.Nullable;

import org.polygamma.android.origin.util.Preconditions;

import java.util.Arrays;
import java.util.Locale;

/**
 * Array of {@code int} ids tagged with an {@code int} key and type.
 *
 * @since 0.2
 * @see Segment#getTaggedIdsArray(long)
 */
public final class TaggedIds {

	/**
	 * Construct new tagged id range.
	 *
	 * @param key key tag
	 * @param type type tag
	 * @param ids id range
	 * @return resulting instance
	 * @throws IllegalArgumentException {@code key} or {@code type} is negative, {@code ids} is
	 * not in sort order, or an id within {@code ids} is less than {@code 1}
	 * @since 0.2
	 */
	public static TaggedIds of(int key, int type, int... ids) {
		Preconditions.checkArgument(key >= 0 && type >= 0);

		for (int i = 0; i < ids.length; i++)
			Preconditions.checkArgument(ids[i] > 0 && (i == 0 || ids[i - 1] < ids[i]));
		return new TaggedIds(key, type, ids);
	}

	private final int key;
	private final int type;
	private final int[] ids;

	/**
	 * Construct new tagged id range.
	 *
	 * @param key key tag
	 * @param type type tag
	 * @param ids id range
	 */
	TaggedIds(int key, int type, int[] ids) {
		this.key = key;
		this.type = type;
		this.ids = ids;
	}

	/**
	 * Key tag.
	 *
	 * @return key
	 * @since 0.2
	 */
	public int key() {
		return this.key;
	}

	/**
	 * Type tag.
	 *
	 * @return type
	 * @since 0.2
	 */
	public int type() {
		return this.type;
	}

	/**
	 * Id range.
	 *
	 * @return range
	 * @since 0.2
	 */
	public int[] ids() {
		return this.ids;
	}

	@Override
	public int hashCode() {
		return Integer.hashCode(this.key) ^ Integer.hashCode(this.type) ^ Arrays.hashCode(this.ids);
	}

	@Override
	public boolean equals(@Nullable Object other) {
		if (!(other instanceof TaggedIds))
			return false;

		TaggedIds that = (TaggedIds) other;

		return this.key == that.key && this.type == that.type && Arrays.equals(this.ids, that.ids);
	}

	@Override
	public String toString() {
		return String.format(
			Locale.ROOT,
			"key=%s, type=%s, ids=%s",
			this.key,
			this.type,
			Arrays.toString(this.ids)
		);
	}
}
