// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.gppstring;

import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;

import androidx.annotation.Nullable;
import androidx.core.util.Supplier;

import org.polygamma.android.origin.util.Bits;
import org.polygamma.android.origin.util.Preconditions;
import org.polygamma.android.origin.util.Strings;

import java.util.Arrays;
import java.util.Date;

/**
 * Signals segment of a {@linkplain Section section}.
 * <p>Segments are immutable value objects, mapping signal fields to their respective values.
 * A segment may be either a {@linkplain #isCore() core} or non-core segment. Core segments define
 * the <i>core</i> field-value mapping of a {@linkplain Section section}, while non-core segments
 * are often optional field-value mappings of a section. All segments are identified by a unique
 * {@linkplain #id() id}. These unique ids can be translated {@linkplain GppIds#toGppSegmentId(int)
 * to} and {@linkplain GppIds#ofGppSegmentId(int, int) from} a Global Privacy Platform (GPP)
 * assigned id.
 * <p>To build a segment decoded from a <i>discreet segment</i> of a GPP string, use {@link
 * #of(int, String)}. Segments can be constructed manually using a {@linkplain #ofBuilder(int)
 * builder}. To encode a segment into a <i>discrete segment</i> of a GPP string, {@link
 * #toString()} can be used.
 *
 * @since 0.2
 * @see Section#core()
 * @see Section#segmentOf(int)
 */
public final class Segment {

	private static final Object[] EMPTY_OBJECTS = new Object[0];
	private static final int[] EMPTY_INTS = new int[0];
	private static final boolean[] EMPTY_BOOLEANS = new boolean[0];

	/**
	 * Validate field and retrieve value index of a field.
	 *
	 * @param field field to retrieve value index of
	 * @param seg expected segment id
	 * @param type expected decoded value type
	 * @return value index
	 * @throws IllegalArgumentException {@code field} is not defined by segment or does not
	 * evaluate to a value of type {@code type}
	 */
	private static int valueIndexOf(@FieldId long field, @SegmentId int seg, Class<?> type) {
		int meta = GppIds.toFieldMetadata(field);

		Preconditions.checkArgument(
			GppIds.segmentIdOfField(field) == seg &&
			FieldTypes.decodedTypeOf(GppIds.toFieldType(meta)).equals(type)
		);
		return GppIds.toFieldValueIndex(meta);
	}

	/**
	 * {@linkplain Segment Segment} builder.
	 *
	 * @since 0.2
	 * @see #ofBuilder(int)
	 */
	public static final class Builder {

		private final @SegmentId int id;
		private @Nullable SparseArray<Object> objects;
		private @Nullable SparseIntArray ints;
		private @Nullable SparseBooleanArray booleans;
		private final @Nullable Segment segment;

		private Builder(@SegmentId int id) {
			this.id = id;
			this.segment = null;
		}

		/**
		 * Retrieve object value table.
		 *
		 * @return value table
		 */
		private SparseArray<Object> objects() {
			return Preconditions.checkNotNullElseGet(this.objects, () -> {
				SparseArray<Object> rv = new SparseArray<>(1);

				if (this.segment != null) {
					for (int i = 0; i < this.segment.objects.length; i++)
						rv.append(i, this.segment.objects[i]);
				}
				this.objects = rv;
				return rv;
			});
		}

		/**
		 * Retrieve {@code int} value table.
		 *
		 * @return value table
		 */
		private SparseIntArray ints() {
			return Preconditions.checkNotNullElseGet(this.ints, () -> {
				SparseIntArray rv = new SparseIntArray(1);

				if (this.segment != null) {
					for (int i = 0; i < this.segment.ints.length; i++)
						rv.append(i, this.segment.ints[i]);
				}
				this.ints = rv;
				return rv;
			});
		}

		/**
		 * Retrieve {@code boolean} value table.
		 *
		 * @return value table
		 */
		private SparseBooleanArray booleans() {
			return Preconditions.checkNotNullElseGet(this.booleans, () -> {
				SparseBooleanArray rv = new SparseBooleanArray(1);

				if (this.segment != null) {
					for (int i = 0; i < this.segment.booleans.length; i++)
						rv.append(i, this.segment.booleans[i]);
				}
				this.booleans = rv;
				return rv;
			});
		}

		/**
		 * Set value of an object field.
		 *
		 * @param field field to set value of
		 * @param val value to set to
		 * @return {@code this}
		 * @throws IllegalArgumentException {@code field} is not defined by segment or does not
		 * evaluate to {@code val.getClass()}
		 */
		private Builder setObject(@FieldId long field, Object val) {
			this.objects().put(valueIndexOf(field, this.id, val.getClass()), val);
			return this;
		}

		/**
		 * Set value of a date field.
		 *
		 * @param field field to set value of
		 * @param date value to set to
		 * @return {@code this}
		 * @throws IllegalArgumentException {@code field} is not defined by segment or does not
		 * evaluate to a date
		 * @since 0.2
		 * @see Segment#getDate(long)
		 */
		public Builder setDate(@FieldId long field, Date date) {
			return this.setObject(field, date);
		}

		/**
		 * Set value of a string field.
		 *
		 * @param field field to set value of
		 * @param str value to set to
		 * @return {@code this}
		 * @throws IllegalArgumentException {@code field} is not defined by segment, does not
		 * evaluate to a string, {@linkplain String#length() length} of {@code str} is not equal
		 * to expected string length, or {@code str} contains unsupported characters
		 * @since 0.2
		 * @see Segment#getString(long)
		 */
		public Builder setString(@FieldId long field, String str) {
			int idx = valueIndexOf(field, this.id, String.class);

			Preconditions.checkArgument(
				str.length() ==
				GppIds.toFieldTypeArg(GppIds.toFieldMetadata(field))
			);
			for (int i = 0; i < str.length(); i++) {
				int c = str.charAt(i) & 0xffff;

				Preconditions.checkArgument(c >= 65 && c <= 127);
			}
			this.objects().put(idx, str);
			return this;
		}

		/**
		 * Set value of a bitfield field.
		 *
		 * @param field field to set value of
		 * @param bits value to set to
		 * @return {@code this}
		 * @throws IllegalArgumentException {@code field} is not defined by segment or does not
		 * evaluate to a {@code bitfield}
		 * @since 0.2
		 * @see Segment#getBitfield(long)
		 */
		public Builder setBitfield(@FieldId long field, SparseBooleanArray bits) {
			return this.setObject(field, bits);
		}

		/**
		 * Set value of an {@code int} array field.
		 *
		 * @param field field to set value of
		 * @param ints value to set to
		 * @return {@code this}
		 * @throws IllegalArgumentException {@code field} is not defined by segment, does not
		 * evaluate to an {@code int} array, field is non-list and {@code ints} is not in sort
		 * order or contains duplicates, or an {@code int} value within {@code ints} is not
		 * positive
		 * @since 0.2
		 * @see Segment#getIntArray(long)
		 */
		public Builder setIntArray(@FieldId long field, int... ints) {
			int meta = GppIds.toFieldMetadata(field);

			if (GppIds.toFieldType(meta) != FieldTypes.FixedIntList) {
				for (int i = 0; i < ints.length; i++) {
					int val = ints[i];

					Preconditions.checkArgument(val > 0 && (i == 0 || ints[i - 1] < val));
				}
			} else {
				Preconditions.checkArgument(
					ints.length ==
					GppIds.toFieldFixedBitLength2(meta).y
				);
			}
			return this.setObject(field, ints);
		}

		/**
		 * Set value of a {@linkplain TaggedIds tagged ids} array field.
		 *
		 * @param field field to set value of
		 * @param ids value to set to
		 * @return {@code this}
		 * @throws IllegalArgumentException {@code field} is not defined by segment or does not
		 * evaluate to a tagged ids array
		 * @since 0.2
		 * @see Segment#getTaggedIdsArray(long)
		 */
		public Builder setTaggedIdsArray(@FieldId long field, TaggedIds... ids) {
			return this.setObject(field, ids);
		}

		/**
		 * Set value of a {@code int} field.
		 *
		 * @param field field to set value of
		 * @param val value to set to
		 * @return {@code this}
		 * @throws IllegalArgumentException {@code field} is not defined by segment, does not
		 * evaluate to an {@code int}, or {@code val} does not satisfy the constraints of {@code
		 * field} (for example, {@code field} requires a non-negative value)
		 * @since 0.2
		 * @see Segment#getInt(long)
		 */
		public Builder setInt(@FieldId long field, int val) {
			int idx = valueIndexOf(field, this.id, int.class);
			int meta = GppIds.toFieldMetadata(field);
			int type = GppIds.toFieldType(meta);

			Preconditions.checkArgument(
				// make sure val fits in the bits allocated for fixed length ints:
				(
					type == FieldTypes.FixedInt &&
					(val & Bits.intMaskOfRange(0, GppIds.toFieldFixedBitLength(meta) - 1)) == val
				) ||
				// fib ints cannot encode `0`s or negatives
				(type == FieldTypes.FibonacciInt && val > 0)
			);
			this.ints().put(idx, val);
			return this;
		}

		/**
		 * Set value of a {@code boolean} field.
		 *
		 * @param field field to set value of
		 * @param val value to set to
		 * @return {@code this}
		 * @throws IllegalArgumentException {@code field} is not defined by segment or does not
		 * evaluate to a {@code boolean}
		 * @since 0.2
		 * @see Segment#getBoolean(long)
		 */
		public Builder setBoolean(@FieldId long field, boolean val) {
			int idx = valueIndexOf(field, this.id, boolean.class);

			this.booleans().put(idx, val);
			return this;
		}

		/**
		 * Build object value table.
		 *
		 * @return value table
		 */
		private Object[] buildObjects() {
			if (this.objects == null || this.objects.size() == 0)
				return this.segment == null ? EMPTY_OBJECTS : this.segment.objects;

			Object[] objs = new Object[this.objects.keyAt(this.objects.size() - 1) + 1];

			for (int i = 0; i < this.objects.size(); i++)
				objs[this.objects.keyAt(i)] = this.objects.valueAt(i);
			return objs;
		}

		/**
		 * Build {@code int} value table.
		 *
		 * @return value table
		 */
		private int[] buildInts() {
			if (this.ints == null || this.ints.size() == 0)
				return this.segment == null ? EMPTY_INTS : this.segment.ints;

			int[] ints = new int[this.ints.keyAt(this.ints.size() - 1) + 1];

			for (int i = 0; i < this.ints.size(); i++)
				ints[this.ints.keyAt(i)] = this.ints.valueAt(i);
			return ints;
		}

		/**
		 * Build {@code boolean} value table.
		 *
		 * @return value table
		 */
		private boolean[] buildBooleans() {
			if (this.booleans == null || this.booleans.size() == 0)
				return this.segment == null ? EMPTY_BOOLEANS : this.segment.booleans;

			boolean[] bools = new boolean[this.booleans.keyAt(this.booleans.size() - 1) + 1];

			for (int i = 0; i < this.booleans.size(); i++)
				bools[this.booleans.keyAt(i)] = this.booleans.valueAt(i);
			return bools;
		}

		/**
		 * Build resulting segment.
		 *
		 * @return segment instance
		 * @since 0.2
		 */
		public Segment build() {
			Segment seg = this.segment;

			return (
				seg != null &&
				this.objects == null &&
				this.ints == null &&
				this.booleans == null ? seg :
				new Segment(this.id, this.buildObjects(), this.buildInts(), this.buildBooleans())
			);
		}
	}

	/**
	 * Construct new empty builder.
	 *
	 * @param id id of segment to construct builder for
	 * @return builder instance
	 * @throws IllegalArgumentException {@code id} is not a valid segment id
	 * @since 0.2
	 */
	public static Builder ofBuilder(@SegmentId int id) {
		return new Builder(id);
	}

	/**
	 * Decode segment from encoded string.
	 *
	 * @param id id of segment to decode
	 * @param str string to decode
	 * @return decoded segment
	 * @throws IllegalArgumentException {@code id} is not a valid segment id or {@code str} is
	 * malformed
	 * @since 0.2
	 */
	public static Segment of(@SegmentId int id, String str) {
		return SectionCoding.decodeSegment(id, str);
	}

	private final @SegmentId int id;
	private final Object[] objects;
	private final int[] ints;
	private final boolean[] booleans;

	/**
	 * Construct a new segment.
	 *
	 * @param id id of segment
	 * @param objs object value table
	 * @param ints integer value table
	 * @param bools {@code boolean} value table
	 */
	Segment(@SegmentId int id, Object[] objs, int[] ints, boolean[] bools) {
		this.id = id;
		this.objects = objs.length == 0 ? EMPTY_OBJECTS : objs;
		this.ints = ints.length == 0 ? EMPTY_INTS : ints;
		this.booleans = bools.length == 0 ? EMPTY_BOOLEANS : bools;
	}

	/**
	 * Id of segment.
	 *
	 * @return segment id
	 * @since 0.2
	 */
	public @SegmentId int id() {
		return this.id;
	}

	/**
	 * Test whether segment is a {@linkplain Section#core() core} segment.
	 *
	 * @return {@code true} if, and only if, segment is core
	 * @since 0.2
	 * @see Section#core()
	 */
	public boolean isCore() {
		return GppIds.toGppSegmentId(this.id) == GppIds.CORE_SEGMENT_ID;
	}

	/**
	 * Validate field and retrieve value index of a field.
	 *
	 * @param field field to retrieve value index of
	 * @param type expected decoded value type
	 * @return value index
	 * @throws IllegalArgumentException {@code field} is not defined by segment or does not
	 * evaluate to a value of type {@code type}
	 */
	private int valueIndexOf(@FieldId long field, Class<?> type) {
		return valueIndexOf(field, this.id, type);
	}

	/**
	 * Retrieve object value of a field by index.
	 *
	 * @param <T> value type
	 * @param idx index of object field value
	 * @param defaultVal default value supplier
	 * @return object value
	 */
	@SuppressWarnings("unchecked")
	private <T> T getObjectAt(int idx, Supplier<T> defaultVal) {
		return Preconditions.checkNotNullElseGet(
			idx < this.objects.length ? (T) this.objects[idx] : null,
			defaultVal
		);
	}

	/**
	 * Retrieve value of a date field by index.
	 *
	 * @param idx index of date field value
	 * @return date value
	 */
	Date getDateAt(int idx) {
		return this.getObjectAt(idx, () -> new Date(0));
	}

	/**
	 * Retrieve value of a date field.
	 *
	 * @param field field to retrieve value of
	 * @return date value
	 * @throws IllegalArgumentException {@code field} is not defined by segment or does not
	 * evaluate to a date value
	 * @since 0.2
	 */
	public Date getDate(@FieldId long field) {
		return this.getDateAt(this.valueIndexOf(field, Date.class));
	}

	/**
	 * Retrieve value of a string field by index.
	 *
	 * @param idx index of string field value
	 * @return string value
	 */
	String getStringAt(int idx) {
		return this.getObjectAt(idx, () -> "");
	}

	/**
	 * Retrieve value of a string field.
	 *
	 * @param field field to retrieve value of
	 * @return string value
	 * @throws IllegalArgumentException {@code field} is not defined by segment or does not
	 * evaluate to a string value
	 * @since 0.2
	 */
	public String getString(@FieldId long field) {
		return this.getStringAt(this.valueIndexOf(field, String.class));
	}

	/**
	 * Retrieve value of a bitfield field by index.
	 *
	 * @param idx index of bitfield field value
	 * @return bitfield value
	 */
	SparseBooleanArray getBitfieldAt(int idx) {
		return this.getObjectAt(idx, SparseBooleanArray::new);
	}

	/**
	 * Retrieve value of a bitfield field.
	 * <p>The value returned must <b>not</b> be modified. Modifying the returned array will result
	 * in undefined behaviour.
	 *
	 * @param field field to retrieve value of
	 * @return bitfield value
	 * @throws IllegalArgumentException {@code field} is not defined by segment or does not
	 * evaluate to a bitfield value
	 * @since 0.2
	 */
	public SparseBooleanArray getBitfield(@FieldId long field) {
		return this.getBitfieldAt(this.valueIndexOf(field, SparseBooleanArray.class));
	}

	/**
	 * Retrieve value of an {@code int} array field by index.
	 *
	 * @param idx index of {@code int} array field value
	 * @return {@code int} array value
	 */
	int[] getIntArrayAt(int idx) {
		return this.getObjectAt(idx, () -> EMPTY_INTS);
	}

	/**
	 * Retrieve value of an {@code int} array field.
	 * <p>The value returned must <b>not</b> be modified. Modifying the returned array will result
	 * in undefined behaviour.
	 *
	 * @param field field to retrieve value of
	 * @return {@code int} array value
	 * @throws IllegalArgumentException {@code field} is not defined by segment or does not
	 * evaluate to an {@code int} array
	 * @since 0.2
	 */
	public int[] getIntArray(@FieldId long field) {
		return this.getIntArrayAt(this.valueIndexOf(field, int[].class));
	}

	/**
	 * Retrieve value of a {@linkplain TaggedIds tagged ids} array field by index.
	 *
	 * @param idx index of tagged ids array field value
	 * @return tagged ids array value
	 */
	TaggedIds[] getTaggedIdsArrayAt(int idx) {
		return this.getObjectAt(idx, () -> new TaggedIds[0]);
	}

	/**
	 * Retrieve value of a {@linkplain TaggedIds tagged ids} array field.
	 * <p>The value returned must <b>not</b> be modified. Modifying the returned array will result
	 * in undefined behaviour.
	 *
	 * @param field field to retrieve value of
	 * @return tagged ids array value
	 * @throws IllegalArgumentException {@code field} is not defined by segment or does not
	 * evaluate to a tagged ids array
	 * @since 0.2
	 */
	public TaggedIds[] getTaggedIdsArray(@FieldId long field) {
		return this.getTaggedIdsArrayAt(this.valueIndexOf(field, TaggedIds[].class));
	}

	/**
	 * Retrieve value of an {@code int} field by index.
	 *
	 * @param idx index of {@code int} field value
	 * @return {@code int} value
	 */
	int getIntAt(int idx) {
		return idx < this.ints.length ? this.ints[idx] : 0;
	}

	/**
	 * Retrieve value of an {@code int} field.
	 *
	 * @param field field to retrieve value of
	 * @return {@code int} value
	 * @throws IllegalArgumentException {@code field} is not defined by segment or does not
	 * evaluate to an {@code int}
	 * @since 0.2
	 */
	public int getInt(@FieldId long field) {
		return this.getIntAt(this.valueIndexOf(field, int.class));
	}

	/**
	 * Retrieve value of a {@code boolean} field by index.
	 *
	 * @param idx index of {@code boolean} field value
	 * @return {@code boolean} value
	 */
	boolean getBooleanAt(int idx) {
		return idx < this.booleans.length && this.booleans[idx];
	}

	/**
	 * Retrieve value of a {@code boolean} field.
	 *
	 * @param field field to retrieve value of
	 * @return {@code boolean} value
	 * @throws IllegalArgumentException {@code field} is not defined by segment or does not
	 * evaluate to a {@code boolean}
	 * @since 0.2
	 */
	public boolean getBoolean(@FieldId long field) {
		return this.getBooleanAt(this.valueIndexOf(field, boolean.class));
	}

	@Override
	public int hashCode() {
		return Long.hashCode(this.id);
	}

	@Override
	public boolean equals(@Nullable Object other) {
		if (!(other instanceof Segment))
			return false;

		Segment that = (Segment) other;

		if (this.id != that.id)
			return false;
		for (int i = 0; i < Math.max(this.ints.length, that.ints.length); i++) {
			if (this.getIntAt(i) != that.getIntAt(i))
				return false;
		}
		for (int i = 0; i < Math.max(this.booleans.length, that.booleans.length); i++) {
			if (this.getBooleanAt(i) != that.getBooleanAt(i))
				return false;
		}
		for (int i = 0; i < Math.max(this.objects.length, that.objects.length); i++) {
			Object thisObj = i < this.objects.length ? this.objects[i] : null;
			Object thatObj = i < that.objects.length ? that.objects[i] : null;

			if (thisObj == thatObj)
				continue;

			Class<?> type = (thisObj == null ? thatObj : thisObj).getClass();

			if (int[].class.equals(type)) {
				int[] a = Preconditions.checkNotNullElse((int[]) thisObj, EMPTY_INTS);
				int[] b = Preconditions.checkNotNullElse((int[]) thatObj, EMPTY_INTS);

				if (!Arrays.equals(a, b))
					return false;
			} else if (TaggedIds[].class.equals(type)) {
				TaggedIds[] a =
					Preconditions.checkNotNullElse((TaggedIds[]) thisObj, new TaggedIds[0]);
				TaggedIds[] b =
					Preconditions.checkNotNullElse((TaggedIds[]) thatObj, new TaggedIds[0]);

				if (!Arrays.equals(a, b))
					return false;
			} else if (SparseBooleanArray.class.equals(type)) {
				SparseBooleanArray a =
					Preconditions.checkNotNullElse(
						(SparseBooleanArray) thisObj,
						new SparseBooleanArray()
					);
				SparseBooleanArray b =
					Preconditions.checkNotNullElse(
						(SparseBooleanArray) thatObj,
						new SparseBooleanArray()
					);

				for (int j = 0; j < a.size(); j++) {
					if (b.get(a.keyAt(j)) != a.valueAt(j))
						return false;
				}
				for (int j = 0; j < b.size(); j++) {
					if (a.get(b.keyAt(j)) != b.valueAt(j))
						return false;
				}
			} else if (String.class.equals(type)) {
				if (!Strings.nullToEmpty((String) thisObj).equals(thatObj))
					return false;
			} else if (Date.class.equals(type)) {
				Date a = Preconditions.checkNotNullElse((Date) thisObj, new Date(0));
				Date b = Preconditions.checkNotNullElse((Date) thatObj, new Date(0));

				if (!a.equals(b))
					return false;
			} else if (thisObj == null || !thisObj.equals(thatObj)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public String toString() {
		return SectionCoding.encodeSegment(this);
	}
}
