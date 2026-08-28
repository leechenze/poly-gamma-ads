// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.gppstring;

import static org.polygamma.android.origin.gppstring.GppIds.*;

import android.util.Pair;
import android.util.SparseArray;

import androidx.annotation.Nullable;
import androidx.core.util.Consumer;

import org.polygamma.android.origin.util.Preconditions;

import java.util.Arrays;

/**
 * Privacy signals section.
 * <p>Sections are immutable value objects, containing a {@linkplain #core() core} fields segment,
 * and zero or more {@linkplain #segmentOf(int) additional} fields segments. The core fields
 * segment maps core fields to values, while additional fields segments are optional fields
 * segments, mapping optional fields to values. Sections are identified by a unique {@linkplain
 * #id() id}, which can be translated {@linkplain GppIds#toGppSectionId(int) to} and {@linkplain
 * GppIds#ofGppSectionId(int) from} a Global Privacy Platform (GPP) assigned id.
 * <p>To build a section decoded from a <i>discrete section</i> of a GPP string, use {@link
 * #of(int, String)} or {@link #of(int, String, Consumer)}. Sections can be constructed manually
 * using a {@linkplain #ofBuilder(int) builder}. Note that when constructing a section manually,
 * at minimum the core segment must be specified. To encode a section into a <i>discrete
 * section</i> of a GPP string, {@link #toString()} can be used.
 *
 * @since 0.2
 * @see GppString#sectionOf(int)
 */
public final class Section {

	private static final Segment[] EMPTY_SEGMENTS = new Segment[0];

	/**
	 * {@linkplain Section} builder.
	 * <p>Before {@linkplain #build() building}, at minimum the {@linkplain #core(Segment) core}
	 * segment must be specified.
	 *
	 * @since 0.2
	 * @see #ofBuilder(int)
	 */
	public static final class Builder {

		private final @SectionId int id;
		private @Nullable Segment core;
		private @Nullable SparseArray<Segment> segments;
		private final @Nullable Section section;

		private Builder(@SectionId int id) {
			ofGppSectionId(toGppSectionId(id));
			this.id = id;
			this.section = null;
		}

		private Builder(Section sect) {
			this.id = sect.id;
			this.section = sect;
		}

		/**
		 * Set {@linkplain Segment#isCore() core} segment.
		 *
		 * @param core core segment
		 * @return {@code this}
		 * @throws IllegalArgumentException {@code core} is not a {@linkplain Segment#isCore()
		 * core} segment or is not defined for the section
		 * @since 0.2
		 * @see Section#core()
		 */
		public Builder core(Segment core) {
			Preconditions.checkArgument(
				core.isCore() &&
				sectionIdOfSegment(core.id()) == this.id
			);
			this.core = core;
			return this;
		}

		/**
		 * Add or replace non-{@linkplain Segment#isCore() core} segment.
		 * <p>If another segment with the same {@linkplain Segment#id() id} as {@code seg} already
		 * exists, it is replaced with {@code seg}; otherwise, {@code seg} is added.
		 *
		 * @param seg segment to add or replace
		 * @return {@code this}
		 * @throws IllegalArgumentException {@code seg} is a {@linkplain Segment#isCore() core}
		 * segment, or {@code seg} is not defined for the section
		 * @since 0.2
		 * @see Section#segmentOf(int)
		 */
		public Builder segment(Segment seg) {
			Preconditions.checkArgument(
				!seg.isCore() &&
				sectionIdOfSegment(seg.id()) == this.id
			);
			if (this.segments == null) {
				this.segments = new SparseArray<>(1);
				if (this.section != null) {
					for (Segment currSeg : this.section.segments)
						this.segments.put(currSeg.id(), currSeg);
				}
			}

			this.segments.put(seg.id(), seg);
			return this;
		}

		/**
		 * Build array of segments, sorted by {@linkplain Segment#id() id}.
		 *
		 * @return segment array
		 */
		private Segment[] buildSegments() {
			if (this.segments == null || this.segments.size() == 0)
				return this.section == null ? EMPTY_SEGMENTS : this.section.segments;

			Segment[] segs = new Segment[this.segments.size()];

			for (int i = 0; i < segs.length; i++)
				segs[i] = this.segments.valueAt(i);
			return segs;
		}

		/**
		 * Build resulting section.
		 *
		 * @return section instance
		 * @throws IllegalStateException {@linkplain Segment#isCore() core} segment has not been
		 * {@linkplain #core(Segment) specified}
		 * @since 0.2
		 */
		public Section build() {
			Section src = this.section;
			Segment core = this.core;

			if (src != null) {
				if (core == null && this.segments == null)
					return src;
				if (core == null)
					core = src.core;
			} else {
				Preconditions.checkState(core != null);
			}
			return new Section(this.id, core, this.buildSegments());
		}
	}

	/**
	 * Construct new empty builder.
	 *
	 * @param id id of section to construct builder for
	 * @return builder instance
	 * @since 0.2
	 */
	public static Builder ofBuilder(@SectionId int id) {
		return new Builder(id);
	}

	/**
	 * Decode section from encoded string.
	 * <p>This attempts to decode an {@code id} section from {@code str}. At minimum, the core
	 * segment of the section must decode successfully for this to return successfully. If any
	 * non-core segments which cannot be decoded, {@code onError} is invoked with a tuple of the
	 * subsequence for the segment and an error cause, respectively.
	 *
	 * @param id id of section to decode
	 * @param str string to decode
	 * @param onError consumer to invoke with malformed section and error cause
	 * @return decoded section
	 * @throws IllegalArgumentException {@code id} is not a valid section id or {@linkplain
	 * #core() core} segment could not be decoded
	 * @throws IllegalStateException {@code id} is not currently supported
	 * @since 0.2
	 */
	public static Section
	of(@SectionId int id, String str, Consumer<Pair<String, Throwable>> onError) {
		return SectionCoding.decode(id, str, onError);
	}

	/**
	 * Decode section from encoded string, failing on any error.
	 * <p>Like {@link #of(int, String, Consumer)}; however, this fails with {@link
	 * IllegalArgumentException} if <b>any</b> segment fails to decode.
	 *
	 * @param id id of section to decode
	 * @param str string to decode
	 * @return decoded section
	 * @throws IllegalArgumentException {@code id} is not a valid section id or any segment could
	 * not be decoded
	 * @since 0.2
	 * @see #of(int, String, Consumer)
	 */
	public static Section of(@SectionId int id, String str) {
		return of(id, str, why -> {
			throw new IllegalArgumentException(
				"malformed segment: ".concat(why.first),
				why.second
			);
		});
	}

	private final @SectionId int id;
	private final Segment core;
	private final Segment[] segments;

	/**
	 * Construct new section.
	 *
	 * @param id section id
	 * @param core core segment
	 * @param segs additional segments, sorted by {@linkplain Segment#id() id}
	 */
	Section(@SectionId int id, Segment core, Segment[] segs) {
		this.id = id;
		this.core = Preconditions.checkNotNull(core);
		this.segments = segs.length == 0 ? EMPTY_SEGMENTS : segs;
	}

	/**
	 * Id of section.
	 *
	 * @return section id
	 * @since 0.2
	 */
	public @SectionId int id() {
		return this.id;
	}

	/**
	 * Core fields segment.
	 * <p>A core segment defines the core field-value mapping of a section.
	 *
	 * @return core segment
	 * @since 0.2
	 * @see Segment#isCore()
	 */
	public Segment core() {
		return this.core;
	}

	/**
	 * Number of non-{@linkplain #core() core} fields segments.
	 * <p>The value returned, when non-zero, minus {@code 1} is the highest non-{@linkplain
	 * Segment#isCore() core} {@linkplain #segmentAt(int) indexable} segment of the section.
	 *
	 * @return non-core fields section count
	 * @since 0.2
	 * @see #segmentAt(int)
	 */
	public int segmentCount() {
		return this.segments.length;
	}

	/**
	 * Retrieve non-{@linkplain #core() core} fields segment at index.
	 *
	 * @param idx segment index
	 * @return segment value
	 * @throws IndexOutOfBoundsException {@code idx} is negative or, greater than or equal to
	 * {@linkplain #segmentCount() segment count}
	 * @since 0.2
	 * @see #segmentCount()
	 */
	public Segment segmentAt(int idx) {
		return this.segments[idx];
	}

	/**
	 * Retrieve non-{@linkplain #core() core} fields segment by {@linkplain Segment#id() id}.
	 *
	 * @param id id to retrieve segment for
	 * @return segment for {@code id}, or {@code null} if no such segment exists
	 * @since 0.2
	 */
	public @Nullable Segment segmentOf(@SegmentId int id) {
		for (Segment seg : this.segments) {
			if (seg.id() == id)
				return seg;
			if (seg.id() > id)
				break;
		}
		return null;
	}

	/**
	 * Construct new builder initialized with {@code this}.
	 *
	 * @return builder instance
	 * @since 0.2
	 */
	public Builder toBuilder() {
		return new Builder(this);
	}

	@Override
	public int hashCode() {
		return this.core.hashCode();
	}

	@Override
	public boolean equals(@Nullable Object other) {
		if (!(other instanceof Section))
			return false;

		Section that = (Section) other;

		return (
			this.id == that.id &&
			this.core.equals(that.core) &&
			Arrays.equals(this.segments, that.segments)
		);
	}

	@Override
	public String toString() {
		return SectionCoding.encode(this);
	}
}
