// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.gppstring;

import android.util.Pair;
import android.util.SparseArray;

import androidx.annotation.Nullable;
import androidx.core.util.Consumer;

import org.polygamma.android.origin.util.Preconditions;
import org.polygamma.android.origin.util.Strings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Global Privacy Platform (GPP) string model.
 * <p>GPP strings are immutable value objects, containing zero or more {@linkplain Section
 * sections}. There may be a <i>single</i> section per {@linkplain Section#id() id}. Sections can
 * be iterated over using {@link #sectionAt(int)}, or retrieved by id using {@link #sectionOf(int)}.
 * <p>The {@link #of(String, Consumer)} method can be used to parse a GPP string from an encoded
 * text string, while {@link #ofBuilder()} can be used to construct a GPP string manually. The
 * {@link #toString()} method can be used to encode a GPP string into an encoded text string.
 *
 * @since 0.2
 */
public final class GppString {

	/**
	 * GPP {@linkplain GppString string} builder.
	 *
	 * @since 0.2
	 * @see #ofBuilder()
	 */
	public static final class Builder {

		private @Nullable SparseArray<Section> sections;
		private final @Nullable GppString string;

		private Builder() {
			this.string = null;
		}

		private Builder(GppString src) {
			this.string = src;
		}

		/**
		 * Add or replace section.
		 * <p>If another section with the same {@linkplain Section#id() id} as {@code sect} already
		 * exists, it is replaced with {@code sect}; otherwise, {@code sect} is added.
		 *
		 * @param sect section to add
		 * @return {@code this}
		 * @since 0.2
		 */
		public Builder section(Section sect) {
			if (this.sections == null) {
				this.sections = new SparseArray<>();
				if (this.string != null) {
					for (Section currSect : this.string.sections)
						this.sections.append(GppIds.toGppSectionId(currSect.id()), currSect);
				}
			}
			this.sections.put(GppIds.toGppSectionId(sect.id()), sect);
			return this;
		}

		/**
		 * Build resulting string.
		 *
		 * @return resulting string
		 * @since 0.2
		 */
		public GppString build() {
			if (this.sections == null && this.string != null)
				return this.string;
			if (this.sections == null)
				return new GppString(new Section[0]);

			Section[] sects = new Section[this.sections.size()];

			for (int i = 0; i < sects.length; i++)
				sects[i] = this.sections.valueAt(i);
			return new GppString(sects);
		}
	}

	/**
	 * Construct new empty {@linkplain Builder builder}.
	 *
	 * @return builder instance
	 * @since 0.2
	 */
	public static Builder ofBuilder() {
		return new Builder();
	}

	/**
	 * Decode GPP string from encoded text string.
	 *
	 * @param enc text string to decode from
	 * @param onError consumer to invoke with tuple of section id and text string, and error cause
	 * on decode error
	 * @return decoded string instance
	 * @throws IllegalArgumentException {@code sect} has a malformed header or does not have a
	 * required section
	 * @since 0.2
	 */
	public static GppString
	of(String enc, Consumer<Pair<Pair<Integer, String>, Throwable>> onError) {
		if (enc.isEmpty())
			return new GppString(new Section[0]);
		if (enc.startsWith("C")) {
			return new GppString(new Section[] { Section.of(
				GppIds.TcfEuV2.ID,
				enc,
				err -> onError.accept(new Pair<>(
					new Pair<>(GppIds.TcfEuV2.ID, err.first),
					err.second
				))
			) });
		}

		Iterator<String> sectStrs = Strings.split(enc, '~');

		Preconditions.checkArgument(sectStrs.hasNext(), "expected header section");

		Segment hdr = Section.of(GppIds.Header.ID, sectStrs.next()).core();
		List<Section> sects = new ArrayList<>();

		Preconditions.checkArgument(
			hdr.getInt(GppIds.Header.Core.Version) == 1,
			"unknown header version"
		);

		for (int sectId : hdr.getIntArray(GppIds.Header.Core.Sections)) {
			Preconditions.checkArgument(sectStrs.hasNext(), "expected section %s", sectId);

			String sectStr = sectStrs.next();

			try {
				sects.add(Section.of(
					GppIds.ofGppSectionId(sectId),
					sectStr,
					err -> onError.accept(new Pair<>(new Pair<>(sectId, err.first), err.second))
				));
			} catch (Exception e) {
				onError.accept(new Pair<>(new Pair<>(sectId, sectStr), e));
			}
		}
		return new GppString(sects.toArray(new Section[0]));
	}

	/**
	 * Decode GPP string from encoded text string, failing on any error.
	 * <p>Like {@link #of(String, Consumer)}; however, this fails with {@link
	 * IllegalArgumentException} if <b>any</b> section fails to decode.
	 *
	 * @param enc string to decode
	 * @return decoded string
	 * @throws IllegalArgumentException {@code enc} could not be fully decoded
	 * @since 0.2
	 * @see #of(String, Consumer)
	 */
	public static GppString of(String enc) {
		return of(enc, why -> {
			throw new IllegalArgumentException(
				"malformed segment: ".concat(why.first.second),
				why.second
			);
		});
	}

	private final Section[] sections;

	private GppString(Section[] sects) {
		this.sections = Preconditions.checkNotNull(sects);
	}

	/**
	 * Number of sections.
	 *
	 * @return sections
	 * @since 0.2
	 */
	public int sectionCount() {
		return this.sections.length;
	}

	/**
	 * Retrieve nested section by index.
	 *
	 * @param idx index to retrieve section at
	 * @return section
	 * @throws IndexOutOfBoundsException {@code idx} is negative or, greater than or equal to
	 * section {@linkplain #sectionCount() count}
	 */
	public Section sectionAt(int idx) {
		return this.sections[idx];
	}

	/**
	 * Find section by {@linkplain Section#id() id}.
	 *
	 * @param id id of section to find
	 * @return section or {@code null} if no section with {@code id} was found
	 * @since 0.2
	 */
	public @Nullable Section sectionOf(@SectionId int id) {
		for (Section sect : this.sections) {
			if (sect.id() == id)
				return sect;
		}
		return null;
	}

	/**
	 * Construct new {@linkplain Builder builder} initialized with {@code this}.
	 *
	 * @return builder instance
	 * @since 0.2
	 */
	public Builder toBuilder() {
		return new Builder(this);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(this.sections);
	}

	@Override
	public boolean equals(@Nullable Object that) {
		return (
			that instanceof GppString &&
			Arrays.equals(this.sections, ((GppString) that).sections)
		);
	}

	@Override
	public String toString() {
		StringBuilder rv = new StringBuilder();
		int[] sectIds = new int[this.sections.length];

		for (int i = 0; i < this.sections.length; i++)
			sectIds[i] = GppIds.toGppSectionId(this.sections[i].id());

		rv.append(new Section(
			GppIds.Header.ID,
			new Segment(
				GppIds.Header.Core.ID,
				new Object[] { sectIds },
				new int[] { 3, 1 },
				new boolean[0]
			),
			new Segment[0]
		));
		for (Section sect : this.sections) {
			rv.append('~')
				.append(sect);
		}
		return rv.toString();
	}
}
