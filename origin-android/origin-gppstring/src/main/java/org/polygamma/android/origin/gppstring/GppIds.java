// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.gppstring;

import android.annotation.SuppressLint;
import android.graphics.Point;

import org.polygamma.android.origin.util.Preconditions;

/**
 * Global Privacy Platform (GPP) section, segment, and field id constants.
 * <p>Note that the id constants defined do <b>not</b> equal their respective values, as defined by
 * GPP. Instead, they are unique to {@link org.polygamma.android.origin.gppstring}. The id
 * constants defined, however, can be translated to and from their GPP equivalent ids using {@code
 * toGpp} and {@code ofGpp} methods in the respective inner classes.
 *
 * @since 0.2
 */
@SuppressLint("WrongConstant")
public class GppIds {

	/*
	 * ID word:
	 *
	 * +---------+---------+-------+
	 * | Section | Segment | Field |
	 * +---------+---------+-------+
	 * |         |         |       |
	 * |         |         |       +---->  0
	 * |         |         +------------> 17
	 * |         +----------------------> 21
	 * +--------------------------------> 31
	 *
	 * Field metadata word:
	 *
	 * +-------+----------+------+
	 * | Index | Argument | Type |
	 * +-------+----------+------+
	 * |       |          |      |
	 * |       |          |      +---->  0
	 * |       |          +-----------> 23
	 * |       +-----------------------> 26
	 * +-------------------------------> 31
	 */
	private static final int METADATA_WORD_SHIFT	= 32;
	private static final long ID_WORD_MASK			= ~0L >>> (64 - METADATA_WORD_SHIFT);

	private static final int BITS_PER_SECTION		= 10;
	private static final int BITS_PER_SEGMENT		=  4;
	private static final int BITS_PER_FIELD			= 18;
	private static final int BITS_PER_FIELD_ARG		= 23;
	private static final int BITS_PER_FIELD_INDEX	=  5;
	private static final int BITS_PER_FIELD_TYPE	=  4;

	private static final int SECTION_SHIFT		= BITS_PER_SEGMENT + BITS_PER_FIELD;
	private static final int SEGMENT_SHIFT		= BITS_PER_FIELD;
	private static final int FIELD_ARG_SHIFT	= BITS_PER_FIELD_TYPE;
	private static final int FIELD_INDEX_SHIFT	= BITS_PER_FIELD_ARG + BITS_PER_FIELD_TYPE;

	private static final int SEGMENT_MASK		= ~0 >>> (32 - BITS_PER_SEGMENT);
	private static final int SECTION_MASK		= ~0 >>> (32 - BITS_PER_SECTION);
	private static final int FIELD_MASK			= ~0 >>> (32 - BITS_PER_FIELD);
	private static final int FIELD_ARG_MASK		= ~0 >>> (32 - BITS_PER_FIELD_ARG);
	private static final int FIELD_INDEX_MASK	= ~0 >>> (32 - BITS_PER_FIELD_INDEX);
	private static final int FIELD_TYPE_MASK	= ~0 >>> (32 - BITS_PER_FIELD_TYPE);

	/**
	 * Pseudo id used for core segments.
	 */
	static final int CORE_SEGMENT_ID = SEGMENT_MASK;

	/**
	 * Convert a {@linkplain Section section} id to a GPP defined section id.
	 *
	 * @param id id of section
	 * @return GPP id of section
	 * @since 0.2
	 * @see #ofGppSectionId(int)
	 */
	public static int toGppSectionId(@SectionId int id) {
		return (id >>> SECTION_SHIFT) & SECTION_MASK;
	}

	/**
	 * Convert a GPP defined section id to a {@linkplain Section section} id.
	 *
	 * @param id GPP id of a section
	 * @return id of section
	 * @throws IllegalArgumentException {@code id} is not a valid GPP section id
	 * @since 0.2
	 * @see #toGppSectionId(int)
	 */
	public static @SectionId int ofGppSectionId(int id) {
		Preconditions.checkArgument((id >= 1 && id <= 22) || id == 500);
		return (id << SECTION_SHIFT);
	}

	/**
	 * Convert a {@linkplain Segment segment} id to a GPP defined segment id.
	 *
	 * @param id id of segment
	 * @return GPP id of segment, or undefined if, and only if, {@code id} is a {@linkplain
	 * Segment#isCore() core} segment id
	 * @since 0.2
	 * @see #ofGppSegmentId(int, int)
	 */
	public static int toGppSegmentId(@SegmentId int id) {
		return (id >>> SEGMENT_SHIFT) & SEGMENT_MASK;
	}

	/**
	 * Convert a GPP defined segment id to a {@linkplain Segment segment} id.
	 *
	 * @param sectId GPP id of a section
	 * @param id GPP id of a segment
	 * @return id of segment
	 * @throws IllegalArgumentException {@code sectId} or {@code id} is not a valid GPP section or
	 * segment id, respectively
	 * @since 0.2
	 * @see #toGppSegmentId(int)
	 */
	public static @SegmentId int ofGppSegmentId(int sectId, int id) {
		Preconditions.checkArgument((id & SEGMENT_MASK) == id);
		return (id << SEGMENT_SHIFT) | ofGppSectionId(sectId);
	}

	/**
	 * Convert a field id to the index of the field within its segment.
	 *
	 * @param id id of field
	 * @return GPP index of field within segment
	 */
	@SuppressWarnings("unused")
	static int toGppFieldIndex(@FieldId long id) {
		return (int) (id & FIELD_MASK);
	}

	/**
	 * Convert a segment id to its section id.
	 *
	 * @param id id of segment
	 * @return defining section id
	 */
	static @SectionId int sectionIdOfSegment(@SegmentId int id) {
		return id & (SECTION_MASK << SECTION_SHIFT);
	}

	/**
	 * Convert a field id to its segment id.
	 *
	 * @param id id of field
	 * @return defining segment id
	 */
	static @SegmentId int segmentIdOfField(@FieldId long id) {
		return ((int) (id & ID_WORD_MASK)) & ~FIELD_MASK;
	}

	/**
	 * Extract metadata word from a field id.
	 *
	 * @param id id field
	 * @return field metadata word
	 */
	static int toFieldMetadata(@FieldId long id) {
		return (int) (id >>> METADATA_WORD_SHIFT);
	}

	/**
	 * Retrieve field type from a metadata word.
	 *
	 * @param word metadata word
	 * @return field type
	 */
	static @FieldType int toFieldType(int word) {
		return word & FIELD_TYPE_MASK;
	}

	/**
	 * Retrieve field type argument from a metadata word.
	 *
	 * @param word metadata word
	 * @return field type argument
	 */
	static int toFieldTypeArg(int word) {
		return (word >>> FIELD_ARG_SHIFT) & FIELD_ARG_MASK;
	}

	/**
	 * Retrieve field type {@linkplain #toFieldTypeArg(int) argument}, as a single fixed bit
	 * length, from a metadata word.
	 *
	 * @param word metadata word
	 * @return fixed bit length argument
	 */
	static int toFieldFixedBitLength(int word) {
		return toFieldTypeArg(word) & FieldTypes.FIXED_BIT_LENGTH_MASK;
	}

	/**
	 * Retrieve field type {@linkplain #toFieldTypeArg(int) argument}, as a tuple of fixed bit
	 * lengths, from a metadata word.
	 *
	 * @param word metadata word
	 * @return tuple of first and second fixed bit length arguments, respectively
	 */
	static Point toFieldFixedBitLength2(int word) {
		int arg = toFieldTypeArg(word);
		int x = arg & FieldTypes.FIXED_BIT_LENGTH_MASK;
		int y = (arg >>> FieldTypes.BITS_PER_FIXED_BIT_LENGTH) & FieldTypes.FIXED_BIT_LENGTH_MASK;

		return new Point(x, y);
	}

	/**
	 * Retrieve field <i>value</i> index from a metadata word.
	 * <p>The value index of a field is the index of its <i>value</i> within the decoded type value
	 * array. For example, fields which are decoded as {@code int}, the index would be the index of
	 * the field's value within {@code Segment.ints}.
	 *
	 * @param word metadata word
	 * @return value index
	 */
	static int toFieldValueIndex(int word) {
		return (word >>> FIELD_INDEX_SHIFT) & FIELD_INDEX_MASK;
	}

	/**
	 * Europe Transparency and Consent Framework (TCF), version 1.
	 *
	 * @since 0.2
	 * @deprecated
	 */
	@Deprecated
	@SuppressWarnings("DeprecatedIsStillUsed")
	public interface TcfEuV1 {
		/** {@link Section#id()} */
		@SectionId int ID = 1 << SECTION_SHIFT;
	}

	/**
	 * Europe Transparency and Consent Framework (TCF), version 2, section id.
	 *
	 * @since 0.2
	 * @see <a href="https://github.com/InteractiveAdvertisingBureau/Global-Privacy-Platform/blob/main/Sections/EEA/GPPExtension%3A%20IAB%20Europe%20TCF.md">IAB TCF Europe Technical Specification</a>
	 */
	public interface TcfEuV2 {
		/** {@link Section#id()} */
		@SectionId int ID = 2 << SECTION_SHIFT;

		/**
		 * Core segment.
		 *
		 * @since 0.2
		 */
		interface Core {
			/** {@link Segment#id()} */
			@SegmentId int ID = TcfEuV2.ID | (CORE_SEGMENT_ID << SEGMENT_SHIFT);

			// BOOLEANS

			/**
			 * {@link Segment#getBoolean(long) Boolean} value, always {@code true}.
			 *
			 * @since 0.2
			 */
			@FieldId long IsServiceSpecific =
				((ID | 9) & ID_WORD_MASK) | ((long) (
					FieldTypes.Boolean |
					(0 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * {@link Segment#getBoolean(long) Boolean} value indicating that a CMP has modified
			 * standard Stack descriptions, their translations, and/or, that a CMP has modified or
			 * supplemented standard illustrations and/or their translations as allowed by the
			 * <a href="https://iabeurope.eu/iab-europe-transparency-consent-framework-policies/">policy</a>.
			 *
			 * @since 0.2
			 */
			@FieldId long UseNonStandardTexts =
				((ID | 10) & ID_WORD_MASK) | ((long) (
					FieldTypes.Boolean |
					(1 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * {@link Segment#getBoolean(long) Boolean} value indicating whether purpose 1 was not
			 * disclosed.
			 *
			 * @since 0.2
			 */
			@FieldId long PurposeOneTreatment =
				((ID | 14) & ID_WORD_MASK) | ((long) (
					FieldTypes.Boolean |
					(2 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			// INTEGERS

			/**
			 * Section version {@link Segment#getInt(long) int}, always {@code 2}.
			 *
			 * @since 0.2
			 */
			@FieldId long Version =
				((ID | 0) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (6 << FIELD_ARG_SHIFT) |
					(0 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Id {@link Segment#getInt(long) int} of consent management platform (CMP) which last
			 * updated the section.
			 *
			 * @since 0.2
			 */
			@FieldId long CmpId =
				((ID | 3) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (12 << FIELD_ARG_SHIFT) |
					(1 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Version {@link Segment#getInt(long) int} of consent management platform (CMP) which
			 * last updated the section.
			 *
			 * @since 0.2
			 */
			@FieldId long CmpVersion =
				((ID | 4) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (12 << FIELD_ARG_SHIFT) |
					(2 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Screen number {@link Segment#getInt(long) int} at which consent was given for a user
			 * with the CMP that last updated the section.
			 *
			 * @since 0.2
			 */
			@FieldId long ConsentScreen =
				((ID | 5) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (6 << FIELD_ARG_SHIFT) |
					(3 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Version {@link Segment#getInt(long) int} of GVL used to create section.
			 *
			 * @since 0.2
			 */
			@FieldId long VendorListVersion =
				((ID | 7) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (12 << FIELD_ARG_SHIFT) |
					(4 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Version {@link Segment#getInt(long) int} of policy used within GVL.
			 *
			 * @since 0.2
			 */
			@FieldId long TcfPolicyVersion =
				((ID | 8) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (6 << FIELD_ARG_SHIFT) |
					(5 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			// OBJECTS

			/**
			 * {@link Segment#getDate(long) Date} when section was created.
			 *
			 * @since 0.2
			 */
			@FieldId long Created =
				((ID | 1) & ID_WORD_MASK) | ((long) (
					FieldTypes.Datetime |
					(0 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * {@link Segment#getDate(long) Date} when section was last updated.
			 *
			 * @since 0.2
			 */
			@FieldId long LastUpdated =
				((ID | 2) & ID_WORD_MASK) | ((long) (
					FieldTypes.Datetime |
					(1 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Two-letter ISO 639-1 language code {@link Segment#getString(long) string} in which
			 * the CMP UI was presented.
			 *
			 * @since 0.2
			 */
			@FieldId long ConsentLanguage =
				((ID | 6) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedString | (2 << FIELD_ARG_SHIFT) |
					(2 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * {@link Segment#getBitfield(long) Bitfield} mapping special feature to whether it
			 * is opted in or not.
			 *
			 * @since 0.2
			 */
			@FieldId long SpecialFeatureOptIns =
				((ID | 11) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedBitfield | (12 << FIELD_ARG_SHIFT) |
					(3 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * {@link Segment#getBitfield(long) Bitfield} mapping purpose to whether consent has
			 * been given or not.
			 *
			 * @since 0.2
			 */
			@FieldId long PurposeConsent =
				((ID | 12) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedBitfield | (24 << FIELD_ARG_SHIFT) |
					(4 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * {@link Segment#getBitfield(long) Bitfield} mapping purpose to whether legitimate
			 * interest has been established or not.
			 *
			 * @since 0.2
			 */
			@FieldId long PurposesLiTransparency =
				((ID | 13 ) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedBitfield | (24 << FIELD_ARG_SHIFT) |
					(5 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Two-letter ISO 3166-1 alpha-2 code {@link Segment#getString(long) string} of the
			 * country in which the publisher's business entity is established.
			 *
			 * @since 0.2
			 */
			@FieldId long PublisherCc =
				((ID | 15) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedString | (2 << FIELD_ARG_SHIFT) |
					(6 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Id {@link Segment#getIntArray(long) int array} of vendors for which consent is
			 * given.
			 *
			 * @since 0.2
			 */
			@FieldId long VendorConsent =
				((ID | 16) & ID_WORD_MASK) | ((long) (
					FieldTypes.OptimizedIntRange2 |
					(7 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Id {@link Segment#getIntArray(long) int array} of vendors for which legitimate
			 * interest is established and user did not exercise <i>Right to Object</i>.
			 *
			 * @since 0.2
			 */
			@FieldId long VendorLegitimateInterest =
				((ID | 17) & ID_WORD_MASK) | ((long) (
					FieldTypes.OptimizedIntRange2 |
					(8 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Tagged id {@link Segment#getTaggedIdsArray(long) array} of vendors and their
			 * respective ublisher side restrictions.
			 *
			 * @since 0.2
			 */
			@FieldId long PubRestrictions =
				((ID | 18) & ID_WORD_MASK) | ((long) (
					FieldTypes.ArrayOfIntRanges |
					(9 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);
		}

		/**
		 * Disclosed vendors segment.
		 *
		 * @since 0.2
		 */
		interface DisclosedVendors {
			/** {@link Segment#id()} */
			@SegmentId int ID = TcfEuV2.ID | (1 << SEGMENT_SHIFT);

			/**
			 * Id {@link Segment#getIntArray(long) int array} of vendors which were disclosed in
			 * a CMP UI to the user.
			 *
			 * @since 0.2
			 */
			@FieldId long DisclosedVendors =
				((ID | 0) & ID_WORD_MASK) | ((long) (
					FieldTypes.OptimizedIntRange2 |
					(0 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);
		}

		/**
		 * Publisher purposes segment.
		 *
		 * @since 0.2
		 */
		interface PublisherPurposes {
			/** {@link Segment#id()} */
			@SegmentId int ID = TcfEuV2.ID | (3 << SEGMENT_SHIFT);

			// INTEGERS

			/**
			 * Custom purpose count {@link Segment#getInt(long) int}.
			 *
			 * @since 0.2
			 */
			@FieldId long NumCustomPurposes =
				((ID | 2) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (6 << FIELD_ARG_SHIFT) |
					(0 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			// OBJECTS

			/**
			 * {@link Segment#getBitfield(long) Bitfield} mapping purpose to whether consent has
			 * been given or not.
			 *
			 * @since 0.2
			 */
			@FieldId long PubPurposesConsent =
				((ID | 0) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedBitfield | (24 << FIELD_ARG_SHIFT) |
					(0 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * {@link Segment#getBitfield(long) Bitfield} mapping purpose to whether legitimate
			 * interest has been established or not.
			 *
			 * @since 0.2
			 */
			@FieldId long PubPurposesLiTransparency =
				((ID | 1) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedBitfield | (24 << FIELD_ARG_SHIFT) |
					(1 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * {@link Segment#getBitfield(long) Bitfield} mapping custom purpose to whether
			 * consent has been given or not.
			 *
			 * @since 0.2
			 */
			@FieldId long CustomPurposesConsent =
				((ID | 3) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedBitfield | (/* NumCustomPurposes */ 0 << FIELD_ARG_SHIFT) |
					(2 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * {@link Segment#getBitfield(long) Bitfield} mapping custom purpose to whether
			 * legitimate interest has been established or not.
			 *
			 * @since 0.2
			 */
			@FieldId long CustomPurposesLiTransparency =
				((ID | 4) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedBitfield | (/* NumCustomPurposes */ 0 << FIELD_ARG_SHIFT) |
					(3 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);
		}
	}

	/**
	 * Header section.
	 *
	 * @see <a href="https://github.com/InteractiveAdvertisingBureau/Global-Privacy-Platform/blob/main/Core/Consent%20String%20Specification.md#header">Global Privacy Platform String - Header</a>
	 */
	interface Header {
		/** {@link Section#id()} */
		@SectionId int ID = 3 << SECTION_SHIFT;

		/**
		 * Core segment.
		 */
		interface Core {
			/** {@link Segment#id()} */
			@SegmentId int ID = Header.ID | (CORE_SEGMENT_ID << SEGMENT_SHIFT);

			/**
			 * Header type {@link Segment#getInt(long) int}, fixed to {@code 3}.
			 */
			@FieldId long Type =
				((ID | 0) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (6 << FIELD_ARG_SHIFT) |
					(0 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * GPP specification version {@link Segment#getInt(long) int}.
			 */
			@FieldId long Version =
				((ID | 1) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (6 << FIELD_ARG_SHIFT) |
					(1 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Range of nested positive section id {@link Segment#getIntArray(long) array}.
			 */
			@FieldId long Sections =
				((ID | 2) & ID_WORD_MASK) | ((long) (
					FieldTypes.FibonacciIntRange |
					(0 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);
		}
	}

	/**
	 * Signal integrity section.
	 *
	 * @see <a href="https://github.com/InteractiveAdvertisingBureau/Global-Privacy-Platform/blob/main/Core/Consent%20String%20Specification.md#signal-integrity">Global Privacy Platform String - Signal Integrity</a>
	 */
	interface SignalIntegrity {
		/** {@link Section#id()} */
		@SectionId int ID = 4 << SECTION_SHIFT;
	}

	/**
	 * Canada Transparency and Consent Framework (TCF), version 1, section id.
	 *
	 * @see <a href="https://github.com/InteractiveAdvertisingBureau/Global-Privacy-Platform/blob/main/Sections/Canada/GPPExtension%3A%20IAB%20Canada%20TCF.md">IAB Canada TCF Technical Specification</a>
	 * @since 0.2
	 */
	public interface TcfCaV1 {
		/** {@link Section#id()} */
		@SectionId int ID = 5 << SECTION_SHIFT;

		/**
		 * Core segment.
		 *
		 * @since 0.2
		 */
		interface Core {
			/** {@link Segment#id()} */
			@SegmentId int ID = TcfCaV1.ID | (CORE_SEGMENT_ID << SEGMENT_SHIFT);

			// BOOLEANS

			/**
			 * {@link Segment#getBoolean(long) Boolean} value indicating that a publisher-run CMP
			 * is using customized stack descriptions and not the standard stack descriptions
			 * defined in the <a href="https://iabcanada.com/tcf-policies/">policies</a>.
			 *
			 * @since 0.2
			 */
			@FieldId long UseNonStandardStacks =
				((ID | 9) & ID_WORD_MASK) | ((long) (
					FieldTypes.Boolean |
					(0 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			// INTEGERS

			/**
			 * Section version {@link Segment#getInt(long) int}, always {@code 1}.
			 *
			 * @since 0.2
			 */
			@FieldId long Version =
				((ID | 0) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (6 << FIELD_ARG_SHIFT) |
					(0 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Id {@link Segment#getInt(long) int} of consent management platform that last updated
			 * the section.
			 *
			 * @since 0.2
			 */
			@FieldId long CmpId =
				((ID | 3) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (12 << FIELD_ARG_SHIFT) |
					(1 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Version {@link Segment#getInt(long) int} of consent management platform that last
			 * updated the section.
			 *
			 * @since 0.2
			 */
			@FieldId long CmpVersion =
				((ID | 4) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (12 << FIELD_ARG_SHIFT) |
					(2 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Screen number {@link Segment#getInt(long) int} at which consent was given for a user
			 * with the CPM that last updated the section.
			 *
			 * @since 0.2
			 */
			@FieldId long ConsentScreen =
				((ID | 5) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (6 << FIELD_ARG_SHIFT) |
					(3 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Version {@link Segment#getInt(long) int} of the GVL used to create the section.
			 *
			 * @since 0.2
			 */
			@FieldId long VendorListVersion =
				((ID | 7) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (12 << FIELD_ARG_SHIFT) |
					(4 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Version {@link Segment#getInt(long) int} of the policy used within GVL.
			 *
			 * @since 0.2
			 */
			@FieldId long TcfPolicyVersion =
				((ID | 8) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (6 << FIELD_ARG_SHIFT) |
					(5 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			// OBJECTS

			/**
			 * {@link Segment#getDate(long) Date} when section was created.
			 *
			 * @since 0.2
			 */
			@FieldId long Created =
				((ID | 1) & ID_WORD_MASK) | ((long) (
					FieldTypes.Datetime |
					(0 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * {@link Segment#getDate(long) Date} when section was last updated.
			 *
			 * @since 0.2
			 */
			@FieldId long LastUpdated =
				((ID | 2) & ID_WORD_MASK) | ((long) (
					FieldTypes.Datetime |
					(1 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Two-letter ISO 639-1 language code {@link Segment#getString(long) string} in which
			 * the CMP UI was presented.
			 *
			 * @since 0.2
			 */
			@FieldId long ConsentLanguage =
				((ID | 6) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedString | (2 << FIELD_ARG_SHIFT) |
					(2 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * {@link Segment#getBitfield(long) Bitfield} mapping special feature to whether it is
			 * opted in or not.
			 *
			 * @since 0.2
			 */
			@FieldId long SpecialFeatureExpressConsent =
				((ID | 10) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedBitfield | (12 << FIELD_ARG_SHIFT) |
					(3 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * {@link Segment#getBitfield(long) Bitfield} mapping purpose to whether it has express
			 * consent or not.
			 *
			 * @since 0.2
			 */
			@FieldId long PurposesExpressConsent =
				((ID | 11) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedBitfield | (24 << FIELD_ARG_SHIFT) |
					(4 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * {@link Segment#getBitfield(long) Bitfield} mapping purpose to whether implied
			 * consent has been established or not.
			 *
			 * @since 0.2
			 */
			@FieldId long PurposesImpliedConsent =
				((ID | 12) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedBitfield | (24 << FIELD_ARG_SHIFT) |
					(5 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Id {@link Segment#getIntArray(long) int array} of vendors which have consent.
			 *
			 * @since 0.2
			 */
			@FieldId long VendorExpressConsent =
				((ID | 13) & ID_WORD_MASK) | ((long) (
					FieldTypes.OptimizedIntRange2 |
					(6 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Id {@link Segment#getIntArray(long) int array} of vendors which have implied
			 * consent.
			 *
			 * @since 0.2
			 */
			@FieldId long VendorImpliedConsent =
				((ID | 14) & ID_WORD_MASK) | ((long) (
					FieldTypes.OptimizedIntRange2 |
					(7 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Tagged id {@link Segment#getTaggedIdsArray(long) array} of vendors and their
			 * respective ublisher side restrictions.
			 *
			 * @since 0.2
			 */
			@FieldId long PubRestrictions =
				((ID | 15) & ID_WORD_MASK) | ((long) (
					FieldTypes.ArrayOfIntRanges |
					(8 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);
		}

		/**
		 * Disclosed vendors segment.
		 *
		 * @since 0.2
		 */
		interface DisclosedVendors {
			/** {@link Segment#id()} */
			@SegmentId int ID = TcfCaV1.ID | (1 << SEGMENT_SHIFT);

			/**
			 * Id {@link Segment#getIntArray(long) int array} of vendors which were disclosed in
			 * a CMP UI to the user.
			 *
			 * @since 0.2
			 */
			@FieldId long DisclosedVendors =
				((ID | 0) & ID_WORD_MASK) | ((long) (
					FieldTypes.OptimizedIntRange2 |
					(0 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);
		}

		/**
		 * Publisher purposes segment.
		 *
		 * @since 0.2
		 */
		interface PublisherPurposes {
			/** {@link Segment#id()} */
			@SegmentId int ID = TcfCaV1.ID | (3 << SEGMENT_SHIFT);

			// INTEGERS

			/**
			 * Custom purpose count {@link Segment#getInt(long) int}.
			 *
			 * @since 0.2
			 */
			@FieldId long NumCustomPurposes =
				((ID | 2) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (6 << FIELD_ARG_SHIFT) |
					(0 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			// OBJECTS

			/**
			 * {@link Segment#getBitfield(long) Bitfield} mapping purpose to whether express
			 * consent has been given or not.
			 *
			 * @since 0.2
			 */
			@FieldId long PubPurposesExpressConsent =
				((ID | 0) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedBitfield | (24 << FIELD_ARG_SHIFT) |
					(0 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * {@link Segment#getBitfield(long) Bitfield} mapping purpose to whether implied
			 * consent has been established or not.
			 *
			 * @since 0.2
			 */
			@FieldId long PubPurposesImpliedConsent =
				((ID | 1) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedBitfield | (24 << FIELD_ARG_SHIFT) |
					(1 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * {@link Segment#getBitfield(long) Bitfield} mapping custom purpose to whether express
			 * consent has been given or not.
			 *
			 * @since 0.2
			 */
			@FieldId long CustomPurposesExpressConsent =
				((ID | 3) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedBitfield | (/* NumCustomPurposes */ 0 << FIELD_ARG_SHIFT) |
					(2 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * {@link Segment#getBitfield(long) Bitfield} mapping custom purpose to whether implied
			 * consent has been established or not.
			 *
			 * @since 0.2
			 */
			@FieldId long CustomPurposesImpliedConsent =
				((ID | 4) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedBitfield | (/* NumCustomPurposes */ 0 << FIELD_ARG_SHIFT) |
					(3 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);
		}
	}

	/**
	 * United States Privacy String, version 1, section id.
	 *
	 * @since 0.2
	 * @see <a href="https://github.com/InteractiveAdvertisingBureau/USPrivacy/blob/master/CCPA/US%20Privacy%20String.md">US Privacy String</a>
	 */
	public interface UsPrivacyV1 {
		/** {@link Section#id()} */
		@SectionId int ID = 6 << SECTION_SHIFT;

		/**
		 * Core segment.
		 *
		 * @since 0.2
		 */
		interface Core {
			/** {@link Segment#id()} */
			@SegmentId int ID = UsPrivacyV1.ID | (CORE_SEGMENT_ID << SEGMENT_SHIFT);

			/**
			 * Section version {@link Segment#getInt(long) int}.
			 *
			 * @since 0.2
			 */
			@FieldId long Version =
				((ID | 0) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (6 << FIELD_ARG_SHIFT) |
					(0 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} for whether notice has been provided where
			 * {@code 0}, {@code 1} or {@code 2} for {@code '-'}, {@code 'Y'} or {@code 'N'},
			 * respectively.
			 *
			 * @since 0.2
			 */
			@FieldId long OptOutNotice =
				((ID | 1) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(1 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} for whether consumer has opted out {@code
			 * 0}, {@code 1} or {@code 2} for {@code '-'}, {@code 'Y'} or {@code 'N'}, respectively.
			 *
			 * @since 0.2
			 */
			@FieldId long OptOutSale =
				((ID | 2) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(2 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} for whether transaction is covered by LSPA
			 * for {@code 0}, {@code 1} or {@code 2} for {@code '-'}, {@code 'Y'} or {@code 'N'},
			 * respectively.
			 *
			 * @since 0.2
			 */
			@FieldId long LspaCoveredTransaction =
				((ID | 3) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(3 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);
		}
	}

	/**
	 * United States National Multi-State Privacy Agreement (MSPA) section id.
	 *
	 * @since 0.2
	 * @see <a href="https://github.com/InteractiveAdvertisingBureau/Global-Privacy-Platform/blob/main/Sections/US-National/IAB%20Privacy%E2%80%99s%20Multi-State%20Privacy%20Agreement%20(MSPA)%20US%20National%20Technical%20Specification.md">GPP Extension: IAB Privacy's MSPA US National Section Technical Specification</a>
	 */
	public interface UsNational {
		/** {@link Section#id()} */
		@SectionId int ID = 7 << SECTION_SHIFT;

		/**
		 * Core sergment.
		 *
		 * @since 0.2
		 */
		interface Core {
			/** {@link Segment#id()} */
			@SegmentId int ID = UsNational.ID | (CORE_SEGMENT_ID << SEGMENT_SHIFT);

			// INTEGERS

			/**
			 * Version {@link Segment#getInt(long) int} of section.
			 *
			 * @since 0.2
			 */
			@FieldId long Version =
				((ID | 0) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (6 << FIELD_ARG_SHIFT) |
					(0 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Notice {@link Segment#getInt(long) int} describing <i>Processing of Personal
			 * Information</i> pursuant to <i>National Approach</i> as defined in Section 1.81(a)
			 * of the MSPA.
			 *
			 * @since 0.2
			 */
			@FieldId long SharingNotice =
				((ID | 1) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(1 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Notice {@link Segment#getInt(long) int} describing <i>Opportunity to Opt Out</i> of
			 * the <i>Sale of the Consumer's Personal Information</i> pursuant to the <i>National
			 * Approach</i>, as defined in Section 1.81(b) of the MSPA.
			 *
			 * @since 0.2
			 */
			@FieldId long SaleOptOutNotice =
				((ID | 2) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(2 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Notice {@link Segment#getInt(long) int} describing <i>Opportunity to Opt Out</i> of
			 * the <i>Sharing of the Consumer's Personal Information</i> pursuant to the
			 * <i>National Approach</i>, as defined in Section 1.81(b) of the MSPA.
			 *
			 * @since 0.2
			 */
			@FieldId long SharingOptOutNotice =
				((ID | 3) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(3 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Notice {@link Segment#getInt(long) int} describing <i>Opportunity to Opt Out</i> of
			 * the <i>Consumer's Personal Information for Targeted Advertising</i> pursuant to the
			 * <i>National Approach</i>, as defined in section 1.81(b) of the MSPA.
			 *
			 * @since 0.2
			 */
			@FieldId long TargetedAdvertisingOptOutNotice =
				((ID | 4) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(4 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Notice {@link Segment#getInt(long) int} describing the <i>Consumer of the
			 * Consumer's</i> right to opt out of the <i>Processing of the Consumer's Sensitive
			 * Personal Information</i> that adheres to the requirements of an optional addendum
			 * to the MSPA.
			 *
			 * @since 0.2
			 */
			@FieldId long SensitiveDataProcessingOptOutNotice =
				((ID | 5) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(5 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Notice {@link Segment#getInt(long) int} describing whether consumer, or a person
			 * authorized by the consumer, has limited the Business's use or disclosure of the
			 * consumer's sensitive personal information.
			 *
			 * @since 0.2
			 */
			@FieldId long SensitiveDataLimitUseNotice =
				((ID | 6) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(6 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether consumer has submitted
			 * a request to opt out of the <i>Sale of Personal Information</i>, pursuant to the
			 * <i>National Approach</i>, as defined in section 1.81(c) of the MSPA.
			 *
			 * @since 0.2
			 */
			@FieldId long SaleOptOut =
				((ID | 7) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(7 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether consumer submitted a
			 * request to opt-out of the <i>Sharing of the Consumer's Personal Information</i>
			 * pursuant to the <i>National Approach</i>, as described in section 1.81(c) of the
			 * MSPA.
			 *
			 * @since 0.2
			 */
			@FieldId long SharingOptOut =
				((ID | 8) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(8 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether consumer submitted a
			 * request to opt out of the <i>Processing of Personal Information for Targeted
			 * Advertising</i> pursuant to the <i>National Approach</i>, as described in section
			 * 1.81(c) of the MSPA.
			 *
			 * @since 0.2
			 */
			@FieldId long TargetedAdvertisingOptOut =
				((ID | 9) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(9 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether consumer consented to
			 * the <i>Processing of the Consumer's Personal Information for Digital Advertising
			 * Activities</i> that would, but providing consent, not otherwise meet the
			 * <i>Secondary Use Limitations</i>.
			 *
			 * @since 0.2
			 */
			@FieldId long PersonalDataConsents =
				((ID | 12) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(10 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether publisher or advertiser,
			 * as applicable, is a signatory to the IAB Multistate Service Provider Agreement
			 * (MSPA), as may be amended from time to time, and declares that the transaction is a
			 * <i>Covered Transaction</i>, as defined in the MSPA.
			 *
			 * @since 0.2
			 */
			@FieldId long MspaCoveredTransaction =
				((ID | 13) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(11 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether publisher or advertiser,
			 * as applicable, has enabled <i>Opt-Out Option Mode</i> for the <i>Covered
			 * Transaction</i>, as such terms are defined in the MSPA.
			 *
			 * @since 0.2
			 */
			@FieldId long MspaOptOutOptionMode =
				((ID | 14) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(12 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether publisher or advertiser,
			 * as applicable, has enabled <i>Service Provider Mode</i> for the <i>Covered
			 * Transaction</i>, as such terms are defined in the MSPA.
			 *
			 * @since 0.2
			 */
			@FieldId long MspaServiceProviderMode =
				((ID | 15) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(13 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			// OBJECTS

			/**
			 * Data activity {@link Segment#getIntArray(long) int array} consent signals.
			 *
			 * @since 0.2
			 */
			@FieldId long SensitiveDataProcessing =
				((ID | 10) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedIntList | (
						(2 | (16 << FieldTypes.BITS_PER_FIXED_BIT_LENGTH)) <<
						FIELD_ARG_SHIFT
					) |
					(0 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Data activity {@link Segment#getIntArray(long) int array} child sensitive consent
			 * signals.
			 *
			 * @since 0.2
			 */
			@FieldId long KnownChildSensitiveDataConsents =
				((ID | 11) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedIntList | (
						(2 | (3 << FieldTypes.BITS_PER_FIXED_BIT_LENGTH)) <<
						FIELD_ARG_SHIFT
					) |
					(1 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);
		}

		/**
		 * GPC segment.
		 *
		 * @since 0.2
		 */
		interface Gpc {
			/** {@link Segment#id()} */
			@SegmentId int ID = UsNational.ID | (1 << SEGMENT_SHIFT);

			/**
			 * {@link Segment#getBoolean(long) Boolean} flag indicating whether consumer indicates
			 * they prefer information not be shared with, or sold to third parties.
			 *
			 * @since 0.2
			 */
			@FieldId long Gpc =
				((ID | 0) & ID_WORD_MASK) | ((long) (
					FieldTypes.Boolean |
					(0 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);
		}
	}

	/**
	 * California, United States section id.
	 *
	 * @since 0.2
	 * @see <a href="https://github.com/InteractiveAdvertisingBureau/Global-Privacy-Platform/blob/main/Sections/US-States/CA/GPP%20Extension%3A%20California%20Privacy%20Technical%20Specification.md">GPP Extension: California Privacy Technical Specification</a>
	 */
	public interface UsStateCa {
		/** {@link Section#id()} */
		@SectionId int ID = 8 << SECTION_SHIFT;

		/**
		 * Core segment.
		 *
		 * @since 0.2
		 */
		interface Core {
			/** {@link Segment#id()} */
			@SegmentId int ID = UsStateCa.ID | (CORE_SEGMENT_ID << SEGMENT_SHIFT);

			// INTEGERS

			/**
			 * Section version {@link Segment#getInt(long) int}.
			 *
			 * @since 0.2
			 */
			@FieldId long Version =
				((ID | 0) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (6 << FIELD_ARG_SHIFT) |
					(0 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Notice {@link Segment#getInt(long) int} describing whether notice of the
			 * <i>Opportunity to Opt Out of the Sale of the Consumer's Personal Information</i>
			 * was provided or not.
			 *
			 * @since 0.2
			 */
			@FieldId long SaleOptOutNotice =
				((ID | 1) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(1 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Notice {@link Segment#getInt(long) int} describing whether notice of the
			 * <i>Opportunity to Opt Out of the Sharing of the Consumer's Personal Information</i>
			 * was provided or not.
			 *
			 * @since 0.2
			 */
			@FieldId long SharingOptOutNotice =
				((ID | 2) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(2 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Notice {@link Segment#getInt(long) int} describing whether notice of the
			 * <i>Opportunity to Limit use or Disclosure of the Consumer's Sensitive Personal
			 * Information</i> was provided or not.
			 *
			 * @since 0.2
			 */
			@FieldId long SensitiveDataLimitUseNotice =
				((ID | 3) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(3 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether consumer has opted out
			 * of the <i>Sale of the Consumer's Personal Information</i>.
			 *
			 * @since 0.2
			 */
			@FieldId long SaleOptOut =
				((ID | 4) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(4 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether consumer has opted out
			 * of the <i>Sharing of the Consumer's Personal Information</i>.
			 *
			 * @since 0.2
			 */
			@FieldId long SharingOptOut =
				((ID | 5) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(5 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether consumer has consented
			 * to collection, use, retention, sale, and/or sharing of the <i>Consumer's Personal
			 * Data</i> that is <i>Unrelated</i> or <i>Incompatible</i> with the purpsoes for
			 * which the <i>Consumer's Personal Data</i> was collected or processed.
			 *
			 * @since 0.2
			 */
			@FieldId long PersonalDataConsents =
				((ID | 8) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(6 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether publisher or advertiser,
			 * as applicable, is a signatory to the IAB Multistate Service Provider Agreement
			 * (MSPA), as may be amended from time to time, and declares that the transaction is a
			 * <i>Covered Transaction</i>, as defined in the MSPA.
			 *
			 * @since 0.2
			 */
			@FieldId long MspaCoveredTransaction =
				((ID | 9) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(7 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether publisher or advertiser,
			 * as applicable, has enabled <i>Opt-Out Option Mode</i> for the <i>Covered
			 * Transaction</i>, as such terms are defined in the MSPA.
			 *
			 * @since 0.2
			 */
			@FieldId long MspaOptOutOptionMode =
				((ID | 10) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(8 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether publisher or advertiser,
			 * as applicable, has enabled <i>Service Provider Mode</i> for the <i>Covered
			 * Transaction</i>, as such terms are defined in the MSPA.
			 *
			 * @since 0.2
			 */
			@FieldId long MspaServiceProviderMode =
				((ID | 11) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(9 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			// OBJECTS

			/**
			 * Data activity {@link Segment#getIntArray(long) int array} consent signals.
			 *
			 * @since 0.2
			 */
			@FieldId long SensitiveDataProcessing =
				((ID | 6) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedIntList | (
						(2 | (9 << FieldTypes.BITS_PER_FIXED_BIT_LENGTH)) <<
						FIELD_ARG_SHIFT
					) |
					(0 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Data activity {@link Segment#getIntArray(long) int array} child sensitive consent
			 * signals.
			 *
			 * @since 0.2
			 */
			@FieldId long KnownChildSensitiveDataConsents =
				((ID | 7) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedIntList | (
						(2 | (2 << FieldTypes.BITS_PER_FIXED_BIT_LENGTH)) <<
						FIELD_ARG_SHIFT
					) |
					(1 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);
		}

		/**
		 * GPC segment.
		 *
		 * @since 0.2
		 */
		interface Gpc {
			/** {@link Segment#id()} */
			@SegmentId int ID = UsStateCa.ID | (1 << SEGMENT_SHIFT);

			/**
			 * {@link Segment#getBoolean(long) Boolean} flag indicating whether consumer indicates
			 * they prefer information not be shared with, or sold to third parties.
			 *
			 * @since 0.2
			 */
			@FieldId long Gpc =
				((ID | 0) & ID_WORD_MASK) | ((long) (
					FieldTypes.Boolean |
					(0 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);
		}
	}

	/**
	 * Virginia, United States section id.
	 *
	 * @since 0.2
	 * @see <a href="https://github.com/InteractiveAdvertisingBureau/Global-Privacy-Platform/blob/main/Sections/US-States/VA/GPP%20Extension%3A%20Virginia%20Privacy%20Technical%20Specification.md">GPP Extension: Virginia Privacy Technical Specification</a>
	 */
	public interface UsStateVa {
		/** {@link Section#id()} */
		@SectionId int ID = 9 << SECTION_SHIFT;

		/**
		 * Core segment.
		 *
		 * @since 0.2
		 */
		interface Core {
			/** {@link Segment#id()} */
			@SegmentId int ID = UsStateVa.ID | (CORE_SEGMENT_ID << SEGMENT_SHIFT);

			// INTEGERS

			/**
			 * Section version {@link Segment#getInt(long) int}.
			 *
			 * @since 0.2
			 */
			@FieldId long Version =
				((ID | 0) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (6 << FIELD_ARG_SHIFT) |
					(0 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Notice {@link Segment#getInt(long) int} describing whether notice of the <i>Sharing
			 * of Personal Data</i> with third parties was presented.
			 *
			 * @since 0.2
			 */
			@FieldId long SharingNotice =
				((ID | 1) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(1 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Notice {@link Segment#getInt(long) int} describing whether notice of the
			 * <i>Opportunity to Opt Out of the Sale of the Consumer's Personal Data</i> was
			 * presented.
			 *
			 * @since 0.2
			 */
			@FieldId long SaleOptOutNotice =
				((ID | 2) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(2 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Notice {@link Segment#getInt(long) int} describing whether notice of the
			 * <i>Opportunity to Opt Out of Processing of the Consumer's Personal Data for
			 * Targeted Advertising</i> was presented.
			 *
			 * @since 0.2
			 */
			@FieldId long TargetedAdvertisingOptOutNotice =
				((ID | 3) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(3 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether consumer has opted out
			 * of the sale of the consumer's personal data.
			 *
			 * @since 0.2
			 */
			@FieldId long SaleOptOut =
				((ID | 4) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(4 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether consumer has opted out
			 * of the processing of the consumer's personal data for targeted advertising.
			 *
			 * @since 0.2
			 */
			@FieldId long TargetedAdvertisingOptOut =
				((ID | 5) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(5 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether consumer has consented
			 * to the processing sensitive data from a known child.
			 *
			 * @since 0.2
			 */
			@FieldId long KnownChildSensitiveDataConsents =
				((ID | 7) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(6 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether publisher or advertiser,
			 * as applicable, is a signatory to the IAB Multistate Service Provider Agreement
			 * (MSPA), as may be amended from time to time, and declares that the transaction is a
			 * <i>Covered Transaction</i>, as defined in the MSPA.
			 *
			 * @since 0.2
			 */
			@FieldId long MspaCoveredTransaction =
				((ID | 8) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(7 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether publisher or advertiser,
			 * as applicable, has enabled <i>Opt-Out Option Mode</i> for the <i>Covered
			 * Transaction</i>, as such terms are defined in the MSPA.
			 *
			 * @since 0.2
			 */
			@FieldId long MspaOptOutOptionMode =
				((ID | 9) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(8 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether publisher or advertiser,
			 * as applicable, has enabled <i>Service Provider Mode</i> for the <i>Covered
			 * Transaction</i>, as such terms are defined in the MSPA.
			 *
			 * @since 0.2
			 */
			@FieldId long MspaServiceProviderMode =
				((ID | 10) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(9 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			// OBJECTS

			/**
			 * Data activity {@link Segment#getIntArray(long) int array} consent signals.
			 *
			 * @since 0.2
			 */
			@FieldId long SensitiveDataProcessing =
				((ID | 6) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedIntList | (
						(2 | (8 << FieldTypes.BITS_PER_FIXED_BIT_LENGTH)) <<
						FIELD_ARG_SHIFT
					) |
					(0 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);
		}
	}

	/**
	 * Colorado, United States section id.
	 *
	 * @since 0.2
	 * @see <a href="https://github.com/InteractiveAdvertisingBureau/Global-Privacy-Platform/blob/main/Sections/US-States/CO/GPP%20Extension%3A%20Colorado%20Privacy%20Technical%20Specification.md">GPP Extension: Colorado Privacy Technical Specification</a>
	 */
	public interface UsStateCo {
		/** {@link Section#id()} */
		@SectionId int ID = 10 << SECTION_SHIFT;

		/**
		 * Core segment.
		 *
		 * @since 0.2
		 */
		interface Core {
			/** {@link Segment#id()} */
			@SegmentId int ID = UsStateCo.ID | (CORE_SEGMENT_ID << SEGMENT_SHIFT);

			// INTEGERS

			/**
			 * Section version {@link Segment#getInt(long) int}.
			 *
			 * @since 0.2
			 */
			@FieldId long Version =
				((ID | 0) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (6 << FIELD_ARG_SHIFT) |
					(0 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Notice {@link Segment#getInt(long) int} describing whether notice of the <i>Sharing
			 * of Personal Data</i> with third parties was presented.
			 *
			 * @since 0.2
			 */
			@FieldId long SharingNotice =
				((ID | 1) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(1 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Notice {@link Segment#getInt(long) int} describing whether notice of the
			 * <i>Opportunity to Opt Out of the Sale of the Consumer's Personal Data</i> was
			 * presented.
			 *
			 * @since 0.2
			 */
			@FieldId long SaleOptOutNotice =
				((ID | 2) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(2 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Notice {@link Segment#getInt(long) int} describing whether notice of the
			 * <i>Opportunity to Opt Out of Processing of the Consumer's Personal Data for
			 * Targeted Advertising</i> was presented.
			 *
			 * @since 0.2
			 */
			@FieldId long TargetedAdvertisingOptOutNotice =
				((ID | 3) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(3 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether consumer has opted out
			 * of the sale of the consumer's personal data.
			 *
			 * @since 0.2
			 */
			@FieldId long SaleOptOut =
				((ID | 4) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(4 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether consumer has opted out
			 * of the processing of the consumer's personal data for targeted advertising.
			 *
			 * @since 0.2
			 */
			@FieldId long TargetedAdvertisingOptOut =
				((ID | 5) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(5 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether consumer has consented
			 * to the processing sensitive data from a known child.
			 *
			 * @since 0.2
			 */
			@FieldId long KnownChildSensitiveDataConsents =
				((ID | 7) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(6 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether publisher or advertiser,
			 * as applicable, is a signatory to the IAB Multistate Service Provider Agreement
			 * (MSPA), as may be amended from time to time, and declares that the transaction is a
			 * <i>Covered Transaction</i>, as defined in the MSPA.
			 *
			 * @since 0.2
			 */
			@FieldId long MspaCoveredTransaction =
				((ID | 8) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(7 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether publisher or advertiser,
			 * as applicable, has enabled <i>Opt-Out Option Mode</i> for the <i>Covered
			 * Transaction</i>, as such terms are defined in the MSPA.
			 *
			 * @since 0.2
			 */
			@FieldId long MspaOptOutOptionMode =
				((ID | 9) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(8 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether publisher or advertiser,
			 * as applicable, has enabled <i>Service Provider Mode</i> for the <i>Covered
			 * Transaction</i>, as such terms are defined in the MSPA.
			 *
			 * @since 0.2
			 */
			@FieldId long MspaServiceProviderMode =
				((ID | 10) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(9 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			// OBJECTS

			/**
			 * Data activity {@link Segment#getIntArray(long) int array} consent signals.
			 *
			 * @since 0.2
			 */
			@FieldId long SensitiveDataProcessing =
				((ID | 6) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedIntList | (
						(2 | (7 << FieldTypes.BITS_PER_FIXED_BIT_LENGTH)) <<
						FIELD_ARG_SHIFT
					) |
					(0 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);
		}

		/**
		 * GPC segment.
		 *
		 * @since 0.2
		 */
		interface Gpc {
			/** {@link Segment#id()} */
			@SegmentId int ID = UsStateCo.ID | (1 << SEGMENT_SHIFT);

			/**
			 * {@link Segment#getBoolean(long) Boolean} flag indicating whether consumer indicates
			 * they prefer information not be shared with, or sold to third parties.
			 *
			 * @since 0.2
			 */
			@FieldId long Gpc =
				((ID | 0) & ID_WORD_MASK) | ((long) (
					FieldTypes.Boolean |
					(0 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);
		}
	}

	/**
	 * Utah, United States section id.
	 *
	 * @since 0.2
	 * @see <a href="https://github.com/InteractiveAdvertisingBureau/Global-Privacy-Platform/blob/main/Sections/US-States/UT/GPP%20Extension%3A%20Utah%20Privacy%20Technical%20Specification.md">GPP Extension: Utah Privacy Technical Specification</a>
	 */
	public interface UsStateUt {
		/** {@link Section#id()} */
		@SectionId int ID = 11 << SECTION_SHIFT;

		/**
		 * Core segment.
		 *
		 * @since 0.2
		 */
		interface Core {
			/** {@link Segment#id()} */
			@SegmentId int ID = UsStateUt.ID | (CORE_SEGMENT_ID << SEGMENT_SHIFT);

			// INTEGERS

			/**
			 * Section version {@link Segment#getInt(long) int}.
			 *
			 * @since 0.2
			 */
			@FieldId long Version =
				((ID | 0) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (6 << FIELD_ARG_SHIFT) |
					(0 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Notice {@link Segment#getInt(long) int} describing whether notice of the <i>Sharing
			 * of Personal Data</i> with third parties was presented.
			 *
			 * @since 0.2
			 */
			@FieldId long SharingNotice =
				((ID | 1) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(1 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Notice {@link Segment#getInt(long) int} describing whether notice of the
			 * <i>Opportunity to Opt Out of the Sale of the Consumer's Personal Data</i> was
			 * presented.
			 *
			 * @since 0.2
			 */
			@FieldId long SaleOptOutNotice =
				((ID | 2) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(2 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Notice {@link Segment#getInt(long) int} describing whether notice of the
			 * <i>Opportunity to Opt Out of Processing of the Consumer's Personal Data for
			 * Targeted Advertising</i> was presented.
			 *
			 * @since 0.2
			 */
			@FieldId long TargetedAdvertisingOptOutNotice =
				((ID | 3) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(3 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Notice {@link Segment#getInt(long) int} describing whether notice of the
			 * <i>Opportunity to Opt Out of the Processing of the Consumer's Sensitive Data</i>
			 * was presented.
			 *
			 * @since 0.2
			 */
			@FieldId long SensitiveDataProcessingOptOutNotice =
				((ID | 4) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(4 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether consumer has opted out
			 * of the sale of the consumer's personal data.
			 *
			 * @since 0.2
			 */
			@FieldId long SaleOptOut =
				((ID | 5) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(5 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether consumer has opted out
			 * of the processing of the consumer's personal data for targeted advertising.
			 *
			 * @since 0.2
			 */
			@FieldId long TargetedAdvertisingOptOut =
				((ID | 6) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(6 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether consumer has consented
			 * to the processing sensitive data from a known child.
			 *
			 * @since 0.2
			 */
			@FieldId long KnownChildSensitiveDataConsents =
				((ID | 8) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(7 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether publisher or advertiser,
			 * as applicable, is a signatory to the IAB Multistate Service Provider Agreement
			 * (MSPA), as may be amended from time to time, and declares that the transaction is a
			 * <i>Covered Transaction</i>, as defined in the MSPA.
			 *
			 * @since 0.2
			 */
			@FieldId long MspaCoveredTransaction =
				((ID | 9) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(8 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether publisher or advertiser,
			 * as applicable, has enabled <i>Opt-Out Option Mode</i> for the <i>Covered
			 * Transaction</i>, as such terms are defined in the MSPA.
			 *
			 * @since 0.2
			 */
			@FieldId long MspaOptOutOptionMode =
				((ID | 10) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(9 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether publisher or advertiser,
			 * as applicable, has enabled <i>Service Provider Mode</i> for the <i>Covered
			 * Transaction</i>, as such terms are defined in the MSPA.
			 *
			 * @since 0.2
			 */
			@FieldId long MspaServiceProviderMode =
				((ID | 11) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(10 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			// OBJECTS

			/**
			 * Data activity {@link Segment#getIntArray(long) int array} consent signals.
			 *
			 * @since 0.2
			 */
			@FieldId long SensitiveDataProcessing =
				((ID | 7) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedIntList | (
						(2 | (8 << FieldTypes.BITS_PER_FIXED_BIT_LENGTH)) <<
						FIELD_ARG_SHIFT
					) |
					(0 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);
		}
	}

	/**
	 * Connecticut, United States section id.
	 *
	 * @since 0.2
	 * @see <a href="https://github.com/InteractiveAdvertisingBureau/Global-Privacy-Platform/blob/main/Sections/US-States/CT/GPP%20Extension%3A%20Connecticut%20Privacy%20Technical%20Specification.md">GPP Extension: Connecticut Privacy Technical Specification</a>
	 */
	public interface UsStateCt {
		/** {@link Section#id()} */
		@SectionId int ID = 12 << SECTION_SHIFT;

		/**
		 * Core segment.
		 *
		 * @since 0.2
		 */
		interface Core {
			/** {@link Segment#id()} */
			@SegmentId int ID = UsStateCt.ID | (CORE_SEGMENT_ID << SEGMENT_SHIFT);

			// INTEGERS

			/**
			 * Section version {@link Segment#getInt(long) int}.
			 *
			 * @since 0.2
			 */
			@FieldId long Version =
				((ID | 0) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (6 << FIELD_ARG_SHIFT) |
					(0 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Notice {@link Segment#getInt(long) int} describing whether notice of the <i>Sharing
			 * of Personal Data</i> with third parties was presented.
			 *
			 * @since 0.2
			 */
			@FieldId long SharingNotice =
				((ID | 1) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(1 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Notice {@link Segment#getInt(long) int} describing whether notice of the
			 * <i>Opportunity to Opt Out of the Sale of the Consumer's Personal Data</i> was
			 * presented.
			 *
			 * @since 0.2
			 */
			@FieldId long SaleOptOutNotice =
				((ID | 2) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(2 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Notice {@link Segment#getInt(long) int} describing whether notice of the
			 * <i>Opportunity to Opt Out of Processing of the Consumer's Personal Data for
			 * Targeted Advertising</i> was presented.
			 *
			 * @since 0.2
			 */
			@FieldId long TargetedAdvertisingOptOutNotice =
				((ID | 3) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(3 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether consumer has opted out
			 * of the sale of the consumer's personal data.
			 *
			 * @since 0.2
			 */
			@FieldId long SaleOptOut =
				((ID | 4) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(4 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether consumer has opted out
			 * of the processing of the consumer's personal data for targeted advertising.
			 *
			 * @since 0.2
			 */
			@FieldId long TargetedAdvertisingOptOut =
				((ID | 5) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(5 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether publisher or advertiser,
			 * as applicable, is a signatory to the IAB Multistate Service Provider Agreement
			 * (MSPA), as may be amended from time to time, and declares that the transaction is a
			 * <i>Covered Transaction</i>, as defined in the MSPA.
			 *
			 * @since 0.2
			 */
			@FieldId long MspaCoveredTransaction =
				((ID | 8) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(6 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether publisher or advertiser,
			 * as applicable, has enabled <i>Opt-Out Option Mode</i> for the <i>Covered
			 * Transaction</i>, as such terms are defined in the MSPA.
			 *
			 * @since 0.2
			 */
			@FieldId long MspaOptOutOptionMode =
				((ID | 9) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(7 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether publisher or advertiser,
			 * as applicable, has enabled <i>Service Provider Mode</i> for the <i>Covered
			 * Transaction</i>, as such terms are defined in the MSPA.
			 *
			 * @since 0.2
			 */
			@FieldId long MspaServiceProviderMode =
				((ID | 10) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(8 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			// OBJECTS

			/**
			 * Data activity {@link Segment#getIntArray(long) int array} consent signals.
			 *
			 * @since 0.2
			 */
			@FieldId long SensitiveDataProcessing =
				((ID | 6) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedIntList | (
						(2 | (8 << FieldTypes.BITS_PER_FIXED_BIT_LENGTH)) <<
						FIELD_ARG_SHIFT
					) |
					(0 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Data activity {@link Segment#getIntArray(long) int array} child sensitive consent
			 * signals.
			 *
			 * @since 0.2
			 */
			@FieldId long KnownChildSensitiveDataConsents =
				((ID | 7) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedIntList | (
						(2 | (3 << FieldTypes.BITS_PER_FIXED_BIT_LENGTH)) <<
						FIELD_ARG_SHIFT
					) |
					(1 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);
		}

		/**
		 * GPC segment.
		 *
		 * @since 0.2
		 */
		interface Gpc {
			/** {@link Segment#id()} */
			@SegmentId int ID = UsStateCt.ID | (1 << SEGMENT_SHIFT);

			/**
			 * {@link Segment#getBoolean(long) Boolean} flag indicating whether consumer indicates
			 * they prefer information not be shared with, or sold to third parties.
			 *
			 * @since 0.2
			 */
			@FieldId long Gpc =
				((ID | 0) & ID_WORD_MASK) | ((long) (
					FieldTypes.Boolean |
					(0 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);
		}
	}

	/**
	 * Florida, United States section id.
	 *
	 * @since 0.2
	 * @see <a href="https://github.com/InteractiveAdvertisingBureau/Global-Privacy-Platform/blob/main/Sections/US-States/FL/GPP%20Extension%3A%20IAB%20Privacy%E2%80%99s%20Florida%20Privacy%20Technical%20Specification.md">GPP Extension: IAB Privacy's Florida Privacy Technical Specification</a>
	 */
	public interface UsStateFl {
		/** {@link Section#id()} */
		int ID = 13 << SECTION_SHIFT;

		/**
		 * Core segment.
		 *
		 * @since 0.2
		 */
		interface Core {
			/** {@link Segment#id()} */
			@SegmentId int ID = UsStateFl.ID | (CORE_SEGMENT_ID << SEGMENT_SHIFT);

			// INTEGERS

			/**
			 * Section version {@link Segment#getInt(long) int}.
			 *
			 * @since 0.2
			 */
			@FieldId long Version =
				((ID | 0) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (6 << FIELD_ARG_SHIFT) |
					(0 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Notice {@link Segment#getInt(long) int} describing whether notice of the
			 * <i>Processing of Personal Data</i> was presented.
			 *
			 * @since 0.2
			 */
			@FieldId long ProcessingNotice =
				((ID | 1) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(1 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Notice {@link Segment#getInt(long) int} describing whether notice of the
			 * <i>Opportunity to Opt Out of the Sale of the Consumer's Personal Data</i> was
			 * presented.
			 *
			 * @since 0.2
			 */
			@FieldId long SaleOptOutNotice =
				((ID | 2) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(2 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Notice {@link Segment#getInt(long) int} describing whether notice of the
			 * <i>Opportunity to Opt Out of Processing of the Consumer's Personal Data for
			 * Targeted Advertising</i> was presented.
			 *
			 * @since 0.2
			 */
			@FieldId long TargetedAdvertisingOptOutNotice =
				((ID | 3) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(3 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether consumer has opted out
			 * of the sale of the consumer's personal data.
			 *
			 * @since 0.2
			 */
			@FieldId long SaleOptOut =
				((ID | 4) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(4 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether consumer has opted out
			 * of the processing of the consumer's personal data for targeted advertising.
			 *
			 * @since 0.2
			 */
			@FieldId long TargetedAdvertisingOptOut =
				((ID | 5) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(5 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether consumer has consented
			 * to <i>Processing of the Consumer's Personal Data that Is Not Reasonably Necessary
			 * for nor Compatible with the Disclosed Purpose(s) for which the Consumer's Personal
			 * Data Was Processed</i>.
			 *
			 * @since 0.2
			 */
			@FieldId long AdditionalDataProcessingConsent =
				((ID | 8) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(8 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether publisher or advertiser,
			 * as applicable, is a signatory to the IAB Multistate Service Provider Agreement
			 * (MSPA), as may be amended from time to time, and declares that the transaction is a
			 * <i>Covered Transaction</i>, as defined in the MSPA.
			 *
			 * @since 0.2
			 */
			@FieldId long MspaCoveredTransaction =
				((ID | 9) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(9 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether publisher or advertiser,
			 * as applicable, has enabled <i>Opt-Out Option Mode</i> for the <i>Covered
			 * Transaction</i>, as such terms are defined in the MSPA.
			 *
			 * @since 0.2
			 */
			@FieldId long MspaOptOutOptionMode =
				((ID | 10) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(10 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether publisher or advertiser,
			 * as applicable, has enabled <i>Service Provider Mode</i> for the <i>Covered
			 * Transaction</i>, as such terms are defined in the MSPA.
			 *
			 * @since 0.2
			 */
			@FieldId long MspaServiceProviderMode =
				((ID | 11) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(11 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			// OBJECTS

			/**
			 * Data activity {@link Segment#getIntArray(long) int array} consent signals.
			 *
			 * @since 0.2
			 */
			@FieldId long SensitiveDataProcessing =
				((ID | 6) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedIntList | (
						(2 | (8 << FieldTypes.BITS_PER_FIXED_BIT_LENGTH)) <<
						FIELD_ARG_SHIFT
					) |
					(0 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Data activity {@link Segment#getIntArray(long) int array} child sensitive consent
			 * signals.
			 *
			 * @since 0.2
			 */
			@FieldId long KnownChildSensitiveDataConsents =
				((ID | 7) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedIntList | (
						(2 | (3 << FieldTypes.BITS_PER_FIXED_BIT_LENGTH)) <<
						FIELD_ARG_SHIFT
					) |
					(1 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);
		}
	}

	/**
	 * Montana, United States section id.
	 *
	 * @since 0.2
	 * @see <a href="https://github.com/InteractiveAdvertisingBureau/Global-Privacy-Platform/blob/main/Sections/US-States/MT/GPP%20Extension%3A%20IAB%20Privacy%E2%80%99s%20Montana%20Privacy%20Technical%20Specification.md">GPP Extension: IAB Privacy's Montana Privacy Technical Specification</a>
	 */
	public interface UsStateMt {
		/** {@link Section#id()} */
		int ID = 14 << SECTION_SHIFT;

		/**
		 * Core segment.
		 *
		 * @since 0.2
		 */
		interface Core {
			/** {@link Segment#id()} */
			@SegmentId int ID = UsStateMt.ID | (CORE_SEGMENT_ID << SEGMENT_SHIFT);

			// INTEGERS

			/**
			 * Section version {@link Segment#getInt(long) int}.
			 *
			 * @since 0.2
			 */
			@FieldId long Version =
				((ID | 0) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (6 << FIELD_ARG_SHIFT) |
					(0 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Notice {@link Segment#getInt(long) int} describing whether notice of the <i>Sharing
			 * of Personal Data</i> with third parties was presented.
			 *
			 * @since 0.2
			 */
			@FieldId long SharingNotice =
				((ID | 1) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(1 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Notice {@link Segment#getInt(long) int} describing whether notice of the
			 * <i>Opportunity to Opt Out of the Sale of the Consumer's Personal Data</i> was
			 * presented.
			 *
			 * @since 0.2
			 */
			@FieldId long SaleOptOutNotice =
				((ID | 2) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(2 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Notice {@link Segment#getInt(long) int} describing whether notice of the
			 * <i>Opportunity to Opt Out of Processing of the Consumer's Personal Data for
			 * Targeted Advertising</i> was presented.
			 *
			 * @since 0.2
			 */
			@FieldId long TargetedAdvertisingOptOutNotice =
				((ID | 3) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(3 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether consumer has opted out
			 * of the sale of the consumer's personal data.
			 *
			 * @since 0.2
			 */
			@FieldId long SaleOptOut =
				((ID | 4) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(4 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether consumer has opted out
			 * of the processing of the consumer's personal data for targeted advertising.
			 *
			 * @since 0.2
			 */
			@FieldId long TargetedAdvertisingOptOut =
				((ID | 5) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(5 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether consumer has consented
			 * to <i>Processing of the Consumer's Personal Data that Is Not Reasonably Necessary
			 * for nor Compatible with the Disclosed Purpose(s) for which the Consumer's Personal
			 * Data Was Processed</i>.
			 *
			 * @since 0.2
			 */
			@FieldId long AdditionalDataProcessingConsent =
				((ID | 8) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(6 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether publisher or advertiser,
			 * as applicable, is a signatory to the IAB Multistate Service Provider Agreement
			 * (MSPA), as may be amended from time to time, and declares that the transaction is a
			 * <i>Covered Transaction</i>, as defined in the MSPA.
			 *
			 * @since 0.2
			 */
			@FieldId long MspaCoveredTransaction =
				((ID | 9) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(7 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether publisher or advertiser,
			 * as applicable, has enabled <i>Opt-Out Option Mode</i> for the <i>Covered
			 * Transaction</i>, as such terms are defined in the MSPA.
			 *
			 * @since 0.2
			 */
			@FieldId long MspaOptOutOptionMode =
				((ID | 10) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(8 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether publisher or advertiser,
			 * as applicable, has enabled <i>Service Provider Mode</i> for the <i>Covered
			 * Transaction</i>, as such terms are defined in the MSPA.
			 *
			 * @since 0.2
			 */
			@FieldId long MspaServiceProviderMode =
				((ID | 11) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(9 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			// OBJECTS

			/**
			 * Data activity {@link Segment#getIntArray(long) int array} consent signals.
			 *
			 * @since 0.2
			 */
			@FieldId long SensitiveDataProcessing =
				((ID | 6) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedIntList | (
						(2 | (8 << FieldTypes.BITS_PER_FIXED_BIT_LENGTH)) <<
						FIELD_ARG_SHIFT
					) |
					(0 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Data activity {@link Segment#getIntArray(long) int array} child sensitive consent
			 * signals.
			 *
			 * @since 0.2
			 */
			@FieldId long KnownChildSensitiveDataProcessing =
				((ID | 7) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedIntList | (
						(2 | (3 << FieldTypes.BITS_PER_FIXED_BIT_LENGTH)) <<
						FIELD_ARG_SHIFT
					) |
					(1 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);
		}

		/**
		 * GPC segment.
		 *
		 * @since 0.2
		 */
		interface Gpc {
			/** {@link Segment#id()} */
			@SegmentId int ID = UsStateMt.ID | (1 << SEGMENT_SHIFT);

			/**
			 * {@link Segment#getBoolean(long) Boolean} flag indicating whether consumer indicates
			 * they prefer information not be shared with, or sold to third parties.
			 *
			 * @since 0.2
			 */
			@FieldId long Gpc =
				((ID | 0) & ID_WORD_MASK) | ((long) (
					FieldTypes.Boolean |
					(0 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);
		}
	}

	/**
	 * Oregon, United States section id.
	 *
	 * @since 0.2
	 * @see <a href="https://github.com/InteractiveAdvertisingBureau/Global-Privacy-Platform/blob/main/Sections/US-States/OR/GPP%20Extension%3A%20IAB%20Privacy%E2%80%99s%20Oregon%20Privacy%20Technical%20Specification.md">GPP Extension: IAB Privacy's Oregon Privacy Technical Specification</a>
	 */
	public interface UsStateOr {
		/** {@link Section#id()} */
		int ID = 15 << SECTION_SHIFT;

		/**
		 * Core segment.
		 *
		 * @since 0.2
		 */
		interface Core {
			/** {@link Segment#id()} */
			@SegmentId int ID = UsStateOr.ID | (CORE_SEGMENT_ID << SEGMENT_SHIFT);

			// INTEGERS

			/**
			 * Section version {@link Segment#getInt(long) int}.
			 *
			 * @since 0.2
			 */
			@FieldId long Version =
				((ID | 0) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (6 << FIELD_ARG_SHIFT) |
					(0 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Notice {@link Segment#getInt(long) int} describing whether notice of the
			 * <i>Processing of Personal Data</i> was presented.
			 *
			 * @since 0.2
			 */
			@FieldId long ProcessingNotice =
				((ID | 1) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(1 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Notice {@link Segment#getInt(long) int} describing whether notice of the
			 * <i>Opportunity to Opt Out of the Sale of the Consumer's Personal Data</i> was
			 * presented.
			 *
			 * @since 0.2
			 */
			@FieldId long SaleOptOutNotice =
				((ID | 2) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(2 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Notice {@link Segment#getInt(long) int} describing whether notice of the
			 * <i>Opportunity to Opt Out of Processing of the Consumer's Personal Data for
			 * Targeted Advertising</i> was presented.
			 *
			 * @since 0.2
			 */
			@FieldId long TargetedAdvertisingOptOutNotice =
				((ID | 3) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(3 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether consumer has opted out
			 * of the sale of the consumer's personal data.
			 *
			 * @since 0.2
			 */
			@FieldId long SaleOptOut =
				((ID | 4) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(4 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether consumer has opted out
			 * of the processing of the consumer's personal data for targeted advertising.
			 *
			 * @since 0.2
			 */
			@FieldId long TargetedAdvertisingOptOut =
				((ID | 5) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(5 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether consumer has consented
			 * to <i>Processing of the Consumer's Personal Data that Is Not Reasonably Necessary
			 * for nor Compatible with the Disclosed Purpose(s) for which the Consumer's Personal
			 * Data Was Processed</i>.
			 *
			 * @since 0.2
			 */
			@FieldId long AdditionalDataProcessingConsent =
				((ID | 8) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(6 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether publisher or advertiser,
			 * as applicable, is a signatory to the IAB Multistate Service Provider Agreement
			 * (MSPA), as may be amended from time to time, and declares that the transaction is a
			 * <i>Covered Transaction</i>, as defined in the MSPA.
			 *
			 * @since 0.2
			 */
			@FieldId long MspaCoveredTransaction =
				((ID | 9) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(7 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether publisher or advertiser,
			 * as applicable, has enabled <i>Opt-Out Option Mode</i> for the <i>Covered
			 * Transaction</i>, as such terms are defined in the MSPA.
			 *
			 * @since 0.2
			 */
			@FieldId long MspaOptOutOptionMode =
				((ID | 10) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(8 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether publisher or advertiser,
			 * as applicable, has enabled <i>Service Provider Mode</i> for the <i>Covered
			 * Transaction</i>, as such terms are defined in the MSPA.
			 *
			 * @since 0.2
			 */
			@FieldId long MspaServiceProviderMode =
				((ID | 11) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(9 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			// OBJECTS

			/**
			 * Data activity {@link Segment#getIntArray(long) int array} consent signals.
			 *
			 * @since 0.2
			 */
			@FieldId long SensitiveDataProcessing =
				((ID | 6) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedIntList | (
						(2 | (11 << FieldTypes.BITS_PER_FIXED_BIT_LENGTH)) <<
						FIELD_ARG_SHIFT
					) |
					(0 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Data activity {@link Segment#getIntArray(long) int array} child sensitive consent
			 * signals.
			 *
			 * @since 0.2
			 */
			@FieldId long KnownChildSensitiveDataConsents =
				((ID | 7) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedIntList | (
						(2 | (3 << FieldTypes.BITS_PER_FIXED_BIT_LENGTH)) <<
						FIELD_ARG_SHIFT
					) |
					(1 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);
		}

		/**
		 * GPC segment.
		 *
		 * @since 0.2
		 */
		interface Gpc {
			/** {@link Segment#id()} */
			@SegmentId int ID = UsStateOr.ID | (1 << SEGMENT_SHIFT);

			/**
			 * {@link Segment#getBoolean(long) Boolean} flag indicating whether consumer indicates
			 * they prefer information not be shared with, or sold to third parties.
			 *
			 * @since 0.2
			 */
			@FieldId long Gpc =
				((ID | 0) & ID_WORD_MASK) | ((long) (
					FieldTypes.Boolean |
					(0 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);
		}
	}

	/**
	 * Texas, United States section id.
	 *
	 * @since 0.2
	 * @see <a href="https://github.com/InteractiveAdvertisingBureau/Global-Privacy-Platform/blob/main/Sections/US-States/TX/GPP%20Extension%3A%20IAB%20Privacy%E2%80%99s%20Texas%20Privacy%20Technical%20Specification.md">GPP Extension: IAB Privacy's Texas Privacy Technical Specification</a>
	 */
	public interface UsStateTx {
		/** {@link Section#id()} */
		int ID = 16 << SECTION_SHIFT;

		/**
		 * Core segment.
		 *
		 * @since 0.2
		 */
		interface Core {
			/** {@link Segment#id()} */
			@SegmentId int ID = UsStateTx.ID | (CORE_SEGMENT_ID << SEGMENT_SHIFT);

			// INTEGERS

			/**
			 * Section version {@link Segment#getInt(long) int}.
			 *
			 * @since 0.2
			 */
			@FieldId long Version =
				((ID | 0) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (6 << FIELD_ARG_SHIFT) |
					(0 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Notice {@link Segment#getInt(long) int} describing whether notice of the
			 * <i>Processing of Personal Data</i> was presented.
			 *
			 * @since 0.2
			 */
			@FieldId long ProcessingNotice =
				((ID | 1) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(1 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Notice {@link Segment#getInt(long) int} describing whether notice of the
			 * <i>Opportunity to Opt Out of the Sale of the Consumer's Personal Data</i> was
			 * presented.
			 *
			 * @since 0.2
			 */
			@FieldId long SaleOptOutNotice =
				((ID | 2) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(2 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Notice {@link Segment#getInt(long) int} describing whether notice of the
			 * <i>Opportunity to Opt Out of Processing of the Consumer's Personal Data for
			 * Targeted Advertising</i> was presented.
			 *
			 * @since 0.2
			 */
			@FieldId long TargetedAdvertisingOptOutNotice =
				((ID | 3) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(3 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether consumer has opted out
			 * of the sale of the consumer's personal data.
			 *
			 * @since 0.2
			 */
			@FieldId long SaleOptOut =
				((ID | 4) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(4 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether consumer has opted out
			 * of the processing of the consumer's personal data for targeted advertising.
			 *
			 * @since 0.2
			 */
			@FieldId long TargetedAdvertisingOptOut =
				((ID | 5) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(5 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether consumer has consented
			 * to the processing sensitive data from a known child.
			 *
			 * @since 0.2
			 */
			@FieldId long KnownChildSensitiveDataConsents =
				((ID | 7) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(6 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether consumer has consented
			 * to <i>Processing of the Consumer's Personal Data that Is Not Reasonably Necessary
			 * for nor Compatible with the Disclosed Purpose(s) for which the Consumer's Personal
			 * Data Was Processed</i>.
			 *
			 * @since 0.2
			 */
			@FieldId long AdditionalDataProcessingConsent =
				((ID | 8) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(7 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether publisher or advertiser,
			 * as applicable, is a signatory to the IAB Multistate Service Provider Agreement
			 * (MSPA), as may be amended from time to time, and declares that the transaction is a
			 * <i>Covered Transaction</i>, as defined in the MSPA.
			 *
			 * @since 0.2
			 */
			@FieldId long MspaCoveredTransaction =
				((ID | 9) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(8 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether publisher or advertiser,
			 * as applicable, has enabled <i>Opt-Out Option Mode</i> for the <i>Covered
			 * Transaction</i>, as such terms are defined in the MSPA.
			 *
			 * @since 0.2
			 */
			@FieldId long MspaOptOutOptionMode =
				((ID | 10) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(9 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether publisher or advertiser,
			 * as applicable, has enabled <i>Service Provider Mode</i> for the <i>Covered
			 * Transaction</i>, as such terms are defined in the MSPA.
			 *
			 * @since 0.2
			 */
			@FieldId long MspaServiceProviderMode =
				((ID | 11) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(10 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			// OBJECTS

			/**
			 * Data activity {@link Segment#getIntArray(long) int array} consent signals.
			 *
			 * @since 0.2
			 */
			@FieldId long SensitiveDataProcessing =
				((ID | 6) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedIntList | (
						(2 | (8 << FieldTypes.BITS_PER_FIXED_BIT_LENGTH)) <<
						FIELD_ARG_SHIFT
					) |
					(0 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);
		}

		/**
		 * GPC segment.
		 *
		 * @since 0.2
		 */
		interface Gpc {
			/** {@link Segment#id()} */
			@SegmentId int ID = UsStateTx.ID | (1 << SEGMENT_SHIFT);

			/**
			 * {@link Segment#getBoolean(long) Boolean} flag indicating whether consumer indicates
			 * they prefer information not be shared with, or sold to third parties.
			 *
			 * @since 0.2
			 */
			@FieldId long Gpc =
				((ID | 0) & ID_WORD_MASK) | ((long) (
					FieldTypes.Boolean |
					(0 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);
		}
	}

	/**
	 * Delaware, United States section id.
	 *
	 * @since 0.2
	 * @see <a href="https://github.com/InteractiveAdvertisingBureau/Global-Privacy-Platform/blob/main/Sections/US-States/DE/GPP%20Extension%3A%20IAB%20Privacy%E2%80%99s%20Delaware%20Privacy%20Technical%20Specification.md">GPP Extension: IAB Privacy's Delaware Privacy Technical Specification</a>
	 */
	public interface UsStateDe {
		/** {@link Section#id()} */
		int ID = 17 << SECTION_SHIFT;

		/**
		 * Core segment.
		 *
		 * @since 0.2
		 */
		interface Core {
			/** {@link Segment#id()} */
			@SegmentId int ID = UsStateDe.ID | (CORE_SEGMENT_ID << SEGMENT_SHIFT);

			// INTEGERS

			/**
			 * Section version {@link Segment#getInt(long) int}.
			 *
			 * @since 0.2
			 */
			@FieldId long Version =
				((ID | 0) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (6 << FIELD_ARG_SHIFT) |
					(0 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Notice {@link Segment#getInt(long) int} describing whether notice of the
			 * <i>Processing of Personal Data</i> was presented.
			 *
			 * @since 0.2
			 */
			@FieldId long ProcessingNotice =
				((ID | 1) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(1 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Notice {@link Segment#getInt(long) int} describing whether notice of the
			 * <i>Opportunity to Opt Out of the Sale of the Consumer's Personal Data</i> was
			 * presented.
			 *
			 * @since 0.2
			 */
			@FieldId long SaleOptOutNotice =
				((ID | 2) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(2 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Notice {@link Segment#getInt(long) int} describing whether notice of the
			 * <i>Opportunity to Opt Out of Processing of the Consumer's Personal Data for
			 * Targeted Advertising</i> was presented.
			 *
			 * @since 0.2
			 */
			@FieldId long TargetedAdvertisingOptOutNotice =
				((ID | 3) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(3 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether consumer has opted out
			 * of the sale of the consumer's personal data.
			 *
			 * @since 0.2
			 */
			@FieldId long SaleOptOut =
				((ID | 4) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(4 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether consumer has opted out
			 * of the processing of the consumer's personal data for targeted advertising.
			 *
			 * @since 0.2
			 */
			@FieldId long TargetedAdvertisingOptOut =
				((ID | 5) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(5 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether consumer has consented
			 * to <i>Processing of the Consumer's Personal Data that Is Not Reasonably Necessary
			 * for nor Compatible with the Disclosed Purpose(s) for which the Consumer's Personal
			 * Data Was Processed</i>.
			 *
			 * @since 0.2
			 */
			@FieldId long AdditionalDataProcessingConsent =
				((ID | 8) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(6 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether publisher or advertiser,
			 * as applicable, is a signatory to the IAB Multistate Service Provider Agreement
			 * (MSPA), as may be amended from time to time, and declares that the transaction is a
			 * <i>Covered Transaction</i>, as defined in the MSPA.
			 *
			 * @since 0.2
			 */
			@FieldId long MspaCoveredTransaction =
				((ID | 9) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(7 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether publisher or advertiser,
			 * as applicable, has enabled <i>Opt-Out Option Mode</i> for the <i>Covered
			 * Transaction</i>, as such terms are defined in the MSPA.
			 *
			 * @since 0.2
			 */
			@FieldId long MspaOptOutOptionMode =
				((ID | 10) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(8 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether publisher or advertiser,
			 * as applicable, has enabled <i>Service Provider Mode</i> for the <i>Covered
			 * Transaction</i>, as such terms are defined in the MSPA.
			 *
			 * @since 0.2
			 */
			@FieldId long MspaServiceProviderMode =
				((ID | 11) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(9 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			// OBJECTS

			/**
			 * Data activity {@link Segment#getIntArray(long) int array} consent signals.
			 *
			 * @since 0.2
			 */
			@FieldId long SensitiveDataProcessing =
				((ID | 6) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedIntList | (
						(2 | (9 << FieldTypes.BITS_PER_FIXED_BIT_LENGTH)) <<
						FIELD_ARG_SHIFT
					) |
					(0 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Data activity {@link Segment#getIntArray(long) int array} child sensitive consent
			 * signals.
			 *
			 * @since 0.2
			 */
			@FieldId long KnownChildSensitiveDataConsents =
				((ID | 7) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedIntList | (
						(2 | (5 << FieldTypes.BITS_PER_FIXED_BIT_LENGTH)) <<
						FIELD_ARG_SHIFT
					) |
					(1 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);
		}

		/**
		 * GPC segment.
		 *
		 * @since 0.2
		 */
		interface Gpc {
			/** {@link Segment#id()} */
			@SegmentId int ID = UsStateDe.ID | (1 << SEGMENT_SHIFT);

			/**
			 * {@link Segment#getBoolean(long) Boolean} flag indicating whether consumer indicates
			 * they prefer information not be shared with, or sold to third parties.
			 *
			 * @since 0.2
			 */
			@FieldId long Gpc =
				((ID | 0) & ID_WORD_MASK) | ((long) (
					FieldTypes.Boolean |
					(0 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);
		}
	}

	/**
	 * Iowa, United States section id.
	 *
	 * @since 0.2
	 * @see <a href="https://github.com/InteractiveAdvertisingBureau/Global-Privacy-Platform/blob/main/Sections/US-States/IA/GPP%20Extension%3A%20IAB%20Privacy%E2%80%99s%20Iowa%20Privacy%20Technical%20Specification.md">GPP Extension: IAB Privacy's Iowa Privacy Technical Specification</a>
	 */
	public interface UsStateIa {
		/** {@link Section#id()} */
		int ID = 18 << SECTION_SHIFT;

		/**
		 * Core segment.
		 *
		 * @since 0.2
		 */
		interface Core {
			/** {@link Segment#id()} */
			@SegmentId int ID = UsStateIa.ID | (CORE_SEGMENT_ID << SEGMENT_SHIFT);

			// INTEGERS

			/**
			 * Section version {@link Segment#getInt(long) int}.
			 *
			 * @since 0.2
			 */
			@FieldId long Version =
				((ID | 0) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (6 << FIELD_ARG_SHIFT) |
					(0 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Notice {@link Segment#getInt(long) int} describing whether notice of the
			 * <i>Processing of Personal Data</i> was presented.
			 *
			 * @since 0.2
			 */
			@FieldId long ProcessingNotice =
				((ID | 1) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(1 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Notice {@link Segment#getInt(long) int} describing whether notice of the
			 * <i>Opportunity to Opt Out of the Sale of the Consumer's Personal Data</i> was
			 * presented.
			 *
			 * @since 0.2
			 */
			@FieldId long SaleOptOutNotice =
				((ID | 2) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(2 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Notice {@link Segment#getInt(long) int} describing whether notice of the
			 * <i>Opportunity to Opt Out of Processing of the Consumer's Personal Data for
			 * Targeted Advertising</i> was presented.
			 *
			 * @since 0.2
			 */
			@FieldId long TargetedAdvertisingOptOutNotice =
				((ID | 3) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(3 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Notice {@link Segment#getInt(long) int} describing whether notice of the
			 * <i>Opportunity to Opt Out of the Processing of the Consumer's Sensitive Data</i>.
			 *
			 * @since 0.2
			 */
			@FieldId long SensitiveDataOptOutNotice =
				((ID | 4) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(4 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether consumer has opted out
			 * of the sale of the consumer's personal data.
			 *
			 * @since 0.2
			 */
			@FieldId long SaleOptOut =
				((ID | 5) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(5 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether consumer has opted out
			 * of the processing of the consumer's personal data for targeted advertising.
			 *
			 * @since 0.2
			 */
			@FieldId long TargetedAdvertisingOptOut =
				((ID | 6) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(6 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether consumer has consented
			 * to the processing sensitive data from a known child.
			 *
			 * @since 0.2
			 */
			@FieldId long KnownChildSensitiveDataConsents =
				((ID | 8) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(7 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether publisher or advertiser,
			 * as applicable, is a signatory to the IAB Multistate Service Provider Agreement
			 * (MSPA), as may be amended from time to time, and declares that the transaction is a
			 * <i>Covered Transaction</i>, as defined in the MSPA.
			 *
			 * @since 0.2
			 */
			@FieldId long MspaCoveredTransaction =
				((ID | 9) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(8 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether publisher or advertiser,
			 * as applicable, has enabled <i>Opt-Out Option Mode</i> for the <i>Covered
			 * Transaction</i>, as such terms are defined in the MSPA.
			 *
			 * @since 0.2
			 */
			@FieldId long MspaOptOutOptionMode =
				((ID | 10) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(9 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether publisher or advertiser,
			 * as applicable, has enabled <i>Service Provider Mode</i> for the <i>Covered
			 * Transaction</i>, as such terms are defined in the MSPA.
			 *
			 * @since 0.2
			 */
			@FieldId long MspaServiceProviderMode =
				((ID | 11) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(10 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			// OBJECTS

			/**
			 * Data activity {@link Segment#getIntArray(long) int array} consent signals.
			 *
			 * @since 0.2
			 */
			@FieldId long SensitiveDataProcessing =
				((ID | 7) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedIntList | (
						(2 | (8 << FieldTypes.BITS_PER_FIXED_BIT_LENGTH)) <<
						FIELD_ARG_SHIFT
					) |
					(0 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);
		}

		/**
		 * GPC segment.
		 *
		 * @since 0.2
		 */
		interface Gpc {
			/** {@link Segment#id()} */
			@SegmentId int ID = UsStateIa.ID | (1 << SEGMENT_SHIFT);

			/**
			 * {@link Segment#getBoolean(long) Boolean} flag indicating whether consumer indicates
			 * they prefer information not be shared with, or sold to third parties.
			 *
			 * @since 0.2
			 */
			@FieldId long Gpc =
				((ID | 0) & ID_WORD_MASK) | ((long) (
					FieldTypes.Boolean |
					(0 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);
		}
	}

	/**
	 * Nebraska, United States section id.
	 *
	 * @since 0.2
	 * @see <a href="https://github.com/InteractiveAdvertisingBureau/Global-Privacy-Platform/blob/main/Sections/US-States/NE/GPP%20Extension%3A%20IAB%20Privacy%E2%80%99s%20Nebraska%20Privacy%20Technical%20Specification.md">GPP Extension: IAB Privacy's Nebraska Privacy Technical Specification</a>
	 */
	public interface UsStateNe {
		/** {@link Section#id()} */
		int ID = 19 << SECTION_SHIFT;

		/**
		 * Core segment.
		 *
		 * @since 0.2
		 */
		interface Core {
			/** {@link Segment#id()} */
			@SegmentId int ID = UsStateNe.ID | (CORE_SEGMENT_ID << SEGMENT_SHIFT);

			// INTEGERS

			/**
			 * Section version {@link Segment#getInt(long) int}.
			 *
			 * @since 0.2
			 */
			@FieldId long Version =
				((ID | 0) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (6 << FIELD_ARG_SHIFT) |
					(0 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Notice {@link Segment#getInt(long) int} describing whether notice of the
			 * <i>Processing of Personal Data</i> was presented.
			 *
			 * @since 0.2
			 */
			@FieldId long ProcessingNotice =
				((ID | 1) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(1 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Notice {@link Segment#getInt(long) int} describing whether notice of the
			 * <i>Opportunity to Opt Out of the Sale of the Consumer's Personal Data</i> was
			 * presented.
			 *
			 * @since 0.2
			 */
			@FieldId long SaleOptOutNotice =
				((ID | 2) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(2 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Notice {@link Segment#getInt(long) int} describing whether notice of the
			 * <i>Opportunity to Opt Out of Processing of the Consumer's Personal Data for
			 * Targeted Advertising</i> was presented.
			 *
			 * @since 0.2
			 */
			@FieldId long TargetedAdvertisingOptOutNotice =
				((ID | 3) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(3 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether consumer has opted out
			 * of the sale of the consumer's personal data.
			 *
			 * @since 0.2
			 */
			@FieldId long SaleOptOut =
				((ID | 4) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(4 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether consumer has opted out
			 * of the processing of the consumer's personal data for targeted advertising.
			 *
			 * @since 0.2
			 */
			@FieldId long TargetedAdvertisingOptOut =
				((ID | 5) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(5 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether consumer has consented
			 * to the processing sensitive data from a known child.
			 *
			 * @since 0.2
			 */
			@FieldId long KnownChildSensitiveDataConsents =
				((ID | 7) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(6 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether consumer has consented
			 * to <i>Processing of the Consumer's Personal Data that Is Not Reasonably Necessary
			 * for nor Compatible with the Disclosed Purpose(s) for which the Consumer's Personal
			 * Data Was Processed</i>.
			 *
			 * @since 0.2
			 */
			@FieldId long AdditionalDataProcessingConsent =
				((ID | 8) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(7 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether publisher or advertiser,
			 * as applicable, is a signatory to the IAB Multistate Service Provider Agreement
			 * (MSPA), as may be amended from time to time, and declares that the transaction is a
			 * <i>Covered Transaction</i>, as defined in the MSPA.
			 *
			 * @since 0.2
			 */
			@FieldId long MspaCoveredTransaction =
				((ID | 9) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(8 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether publisher or advertiser,
			 * as applicable, has enabled <i>Opt-Out Option Mode</i> for the <i>Covered
			 * Transaction</i>, as such terms are defined in the MSPA.
			 *
			 * @since 0.2
			 */
			@FieldId long MspaOptOutOptionMode =
				((ID | 10) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(9 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether publisher or advertiser,
			 * as applicable, has enabled <i>Service Provider Mode</i> for the <i>Covered
			 * Transaction</i>, as such terms are defined in the MSPA.
			 *
			 * @since 0.2
			 */
			@FieldId long MspaServiceProviderMode =
				((ID | 11) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(10 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			// OBJECTS

			/**
			 * Data activity {@link Segment#getIntArray(long) int array} consent signals.
			 *
			 * @since 0.2
			 */
			@FieldId long SensitiveDataProcessing =
				((ID | 6) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedIntList | (
						(2 | (8 << FieldTypes.BITS_PER_FIXED_BIT_LENGTH)) <<
						FIELD_ARG_SHIFT
					) |
					(0 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);
		}

		/**
		 * GPC segment.
		 *
		 * @since 0.2
		 */
		interface Gpc {
			/** {@link Segment#id()} */
			@SegmentId int ID = UsStateNe.ID | (1 << SEGMENT_SHIFT);

			/**
			 * {@link Segment#getBoolean(long) Boolean} flag indicating whether consumer indicates
			 * they prefer information not be shared with, or sold to third parties.
			 *
			 * @since 0.2
			 */
			@FieldId long Gpc =
				((ID | 0) & ID_WORD_MASK) | ((long) (
					FieldTypes.Boolean |
					(0 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);
		}
	}

	/**
	 * New Hampshire, United States section id.
	 *
	 * @since 0.2
	 * @see <a href="https://github.com/InteractiveAdvertisingBureau/Global-Privacy-Platform/blob/main/Sections/US-States/NH/GPP%20Extension%3A%20IAB%20Privacy%E2%80%99s%20New%20Hampshire%20Privacy%20Technical%20Specification.md">GPP Extension: IAB Privacy's New Hampshire Privacy Technical Specification</a>
	 */
	public interface UsStateNh {
		/** {@link Section#id()} */
		int ID = 20 << SECTION_SHIFT;

		/**
		 * Core segment.
		 *
		 * @since 0.2
		 */
		interface Core {
			/** {@link Segment#id()} */
			@SegmentId int ID = UsStateNh.ID | (CORE_SEGMENT_ID << SEGMENT_SHIFT);

			// INTEGERS

			/**
			 * Section version {@link Segment#getInt(long) int}.
			 *
			 * @since 0.2
			 */
			@FieldId long Version =
				((ID | 0) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (6 << FIELD_ARG_SHIFT) |
					(0 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Notice {@link Segment#getInt(long) int} describing whether notice of the
			 * <i>Processing of Personal Data</i> was presented.
			 *
			 * @since 0.2
			 */
			@FieldId long ProcessingNotice =
				((ID | 1) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(1 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Notice {@link Segment#getInt(long) int} describing whether notice of the
			 * <i>Opportunity to Opt Out of the Sale of the Consumer's Personal Data</i> was
			 * presented.
			 *
			 * @since 0.2
			 */
			@FieldId long SaleOptOutNotice =
				((ID | 2) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(2 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Notice {@link Segment#getInt(long) int} describing whether notice of the
			 * <i>Opportunity to Opt Out of Processing of the Consumer's Personal Data for
			 * Targeted Advertising</i> was presented.
			 *
			 * @since 0.2
			 */
			@FieldId long TargetedAdvertisingOptOutNotice =
				((ID | 3) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(3 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether consumer has opted out
			 * of the sale of the consumer's personal data.
			 *
			 * @since 0.2
			 */
			@FieldId long SaleOptOut =
				((ID | 4) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(4 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether consumer has opted out
			 * of the processing of the consumer's personal data for targeted advertising.
			 *
			 * @since 0.2
			 */
			@FieldId long TargetedAdvertisingOptOut =
				((ID | 5) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(5 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether consumer has consented
			 * to <i>Processing of the Consumer's Personal Data that Is Not Reasonably Necessary
			 * for nor Compatible with the Disclosed Purpose(s) for which the Consumer's Personal
			 * Data Was Processed</i>.
			 *
			 * @since 0.2
			 */
			@FieldId long AdditionalDataProcessingConsent =
				((ID | 8) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(6 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether publisher or advertiser,
			 * as applicable, is a signatory to the IAB Multistate Service Provider Agreement
			 * (MSPA), as may be amended from time to time, and declares that the transaction is a
			 * <i>Covered Transaction</i>, as defined in the MSPA.
			 *
			 * @since 0.2
			 */
			@FieldId long MspaCoveredTransaction =
				((ID | 9) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(7 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether publisher or advertiser,
			 * as applicable, has enabled <i>Opt-Out Option Mode</i> for the <i>Covered
			 * Transaction</i>, as such terms are defined in the MSPA.
			 *
			 * @since 0.2
			 */
			@FieldId long MspaOptOutOptionMode =
				((ID | 10) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(8 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether publisher or advertiser,
			 * as applicable, has enabled <i>Service Provider Mode</i> for the <i>Covered
			 * Transaction</i>, as such terms are defined in the MSPA.
			 *
			 * @since 0.2
			 */
			@FieldId long MspaServiceProviderMode =
				((ID | 11) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(9 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			// OBJECTS

			/**
			 * Data activity {@link Segment#getIntArray(long) int array} consent signals.
			 *
			 * @since 0.2
			 */
			@FieldId long SensitiveDataProcessing =
				((ID | 6) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedIntList | (
						(2 | (8 << FieldTypes.BITS_PER_FIXED_BIT_LENGTH)) <<
						FIELD_ARG_SHIFT
					) |
					(0 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Data activity {@link Segment#getIntArray(long) int array} child sensitive consent
			 * signals.
			 *
			 * @since 0.2
			 */
			@FieldId long KnownChildSensitiveDataConsents =
				((ID | 7) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedIntList | (
						(2 | (3 << FieldTypes.BITS_PER_FIXED_BIT_LENGTH)) <<
						FIELD_ARG_SHIFT
					) |
					(1 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);
		}

		/**
		 * GPC segment.
		 *
		 * @since 0.2
		 */
		interface Gpc {
			/** {@link Segment#id()} */
			@SegmentId int ID = UsStateNh.ID | (1 << SEGMENT_SHIFT);

			/**
			 * {@link Segment#getBoolean(long) Boolean} flag indicating whether consumer indicates
			 * they prefer information not be shared with, or sold to third parties.
			 *
			 * @since 0.2
			 */
			@FieldId long Gpc =
				((ID | 0) & ID_WORD_MASK) | ((long) (
					FieldTypes.Boolean |
					(0 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);
		}
	}

	/**
	 * New Jersey, United States section id.
	 *
	 * @since 0.2
	 * @see <a href="https://github.com/InteractiveAdvertisingBureau/Global-Privacy-Platform/blob/main/Sections/US-States/NJ/GPP%20Extension%3A%20IAB%20Privacy%E2%80%99s%20New%20Jersey%20Privacy%20Technical%20Specification.md">GPP Extension: IAB Privacy's New Jersey Privacy Technical Specification</a>
	 */
	public interface UsStateNj {
		/** {@link Section#id()} */
		int ID = 21 << SECTION_SHIFT;

		/**
		 * Core segment.
		 *
		 * @since 0.2
		 */
		interface Core {
			/**
			 * {@link Segment#id()}
			 */
			@SegmentId int ID = UsStateNj.ID | (CORE_SEGMENT_ID << SEGMENT_SHIFT);

			// INTEGERS

			/**
			 * Section version {@link Segment#getInt(long) int}.
			 *
			 * @since 0.2
			 */
			@FieldId long Version =
				((ID | 0) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (6 << FIELD_ARG_SHIFT) |
					(0 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Notice {@link Segment#getInt(long) int} describing whether notice of the
			 * <i>Processing of Personal Data</i> was presented.
			 *
			 * @since 0.2
			 */
			@FieldId long ProcessingNotice =
				((ID | 1) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(1 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Notice {@link Segment#getInt(long) int} describing whether notice of the
			 * <i>Opportunity to Opt Out of the Sale of the Consumer's Personal Data</i> was
			 * presented.
			 *
			 * @since 0.2
			 */
			@FieldId long SaleOptOutNotice =
				((ID | 2) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(2 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Notice {@link Segment#getInt(long) int} describing whether notice of the
			 * <i>Opportunity to Opt Out of Processing of the Consumer's Personal Data for
			 * Targeted Advertising</i> was presented.
			 *
			 * @since 0.2
			 */
			@FieldId long TargetedAdvertisingOptOutNotice =
				((ID | 3) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(3 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether consumer has opted out
			 * of the sale of the consumer's personal data.
			 *
			 * @since 0.2
			 */
			@FieldId long SaleOptOut =
				((ID | 4) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(4 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether consumer has opted out
			 * of the processing of the consumer's personal data for targeted advertising.
			 *
			 * @since 0.2
			 */
			@FieldId long TargetedAdvertisingOptOut =
				((ID | 5) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(5 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether consumer has consented
			 * to <i>Processing of the Consumer's Personal Data that Is Not Reasonably Necessary
			 * for nor Compatible with the Disclosed Purpose(s) for which the Consumer's Personal
			 * Data Was Processed</i>.
			 *
			 * @since 0.2
			 */
			@FieldId long AdditionalDataProcessingConsent =
				((ID | 8) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(6 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether publisher or advertiser,
			 * as applicable, is a signatory to the IAB Multistate Service Provider Agreement
			 * (MSPA), as may be amended from time to time, and declares that the transaction is a
			 * <i>Covered Transaction</i>, as defined in the MSPA.
			 *
			 * @since 0.2
			 */
			@FieldId long MspaCoveredTransaction =
				((ID | 9) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(7 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether publisher or advertiser,
			 * as applicable, has enabled <i>Opt-Out Option Mode</i> for the <i>Covered
			 * Transaction</i>, as such terms are defined in the MSPA.
			 *
			 * @since 0.2
			 */
			@FieldId long MspaOptOutOptionMode =
				((ID | 10) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(8 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether publisher or advertiser,
			 * as applicable, has enabled <i>Service Provider Mode</i> for the <i>Covered
			 * Transaction</i>, as such terms are defined in the MSPA.
			 *
			 * @since 0.2
			 */
			@FieldId long MspaServiceProviderMode =
				((ID | 11) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(9 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			// OBJECTS

			/**
			 * Data activity {@link Segment#getIntArray(long) int array} consent signals.
			 *
			 * @since 0.2
			 */
			@FieldId long SensitiveDataProcessing =
				((ID | 6) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedIntList | (
						(2 | (10 << FieldTypes.BITS_PER_FIXED_BIT_LENGTH)) <<
						FIELD_ARG_SHIFT
					) |
					(0 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Data activity {@link Segment#getIntArray(long) int array} child sensitive consent
			 * signals.
			 *
			 * @since 0.2
			 */
			@FieldId long KnownChildSensitiveDataConsents =
				((ID | 7) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedIntList | (
						(2 | (5 << FieldTypes.BITS_PER_FIXED_BIT_LENGTH)) <<
						FIELD_ARG_SHIFT
					) |
					(1 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);
		}

		/**
		 * GPC segment.
		 *
		 * @since 0.2
		 */
		interface Gpc {
			/** {@link Segment#id()} */
			@SegmentId int ID = UsStateNj.ID | (1 << SEGMENT_SHIFT);

			/**
			 * {@link Segment#getBoolean(long) Boolean} flag indicating whether consumer indicates
			 * they prefer information not be shared with, or sold to third parties.
			 *
			 * @since 0.2
			 */
			@FieldId long Gpc =
				((ID | 0) & ID_WORD_MASK) | ((long) (
					FieldTypes.Boolean |
					(0 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);
		}
	}

	/**
	 * Tennessee, United States section id.
	 *
	 * @since 0.2
	 * @see <a href="https://github.com/InteractiveAdvertisingBureau/Global-Privacy-Platform/blob/main/Sections/US-States/TN/GPP%20Extension%3A%20IAB%20Privacy%E2%80%99s%20Tennessee%20Privacy%20Technical%20Specification.md">GPP Extension: IAB Privacy's Tennessee Privacy Technical Specification</a>
	 */
	public interface UsStateTn {
		/** {@link Section#id()} */
		int ID = 22 << SECTION_SHIFT;

		/**
		 * Core segment.
		 *
		 * @since 0.2
		 */
		interface Core {
			/** {@link Segment#id()} */
			@SegmentId int ID = UsStateTn.ID | (CORE_SEGMENT_ID << SEGMENT_SHIFT);

			// INTEGERS

			/**
			 * Section version {@link Segment#getInt(long) int}.
			 *
			 * @since 0.2
			 */
			@FieldId long Version =
				((ID | 0) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (6 << FIELD_ARG_SHIFT) |
					(0 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Notice {@link Segment#getInt(long) int} describing whether notice of the
			 * <i>Processing of Personal Data</i> was presented.
			 *
			 * @since 0.2
			 */
			@FieldId long ProcessingNotice =
				((ID | 1) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(1 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Notice {@link Segment#getInt(long) int} describing whether notice of the
			 * <i>Opportunity to Opt Out of the Sale of the Consumer's Personal Data</i> was
			 * presented.
			 *
			 * @since 0.2
			 */
			@FieldId long SaleOptOutNotice =
				((ID | 2) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(2 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Notice {@link Segment#getInt(long) int} describing whether notice of the
			 * <i>Opportunity to Opt Out of Processing of the Consumer's Personal Data for
			 * Targeted Advertising</i> was presented.
			 *
			 * @since 0.2
			 */
			@FieldId long TargetedAdvertisingOptOutNotice =
				((ID | 3) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(3 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether consumer has opted out
			 * of the sale of the consumer's personal data.
			 *
			 * @since 0.2
			 */
			@FieldId long SaleOptOut =
				((ID | 4) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(4 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether consumer has opted out
			 * of the processing of the consumer's personal data for targeted advertising.
			 *
			 * @since 0.2
			 */
			@FieldId long TargetedAdvertisingOptOut =
				((ID | 5) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(5 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether consumer has consented
			 * to the processing sensitive data from a known child.
			 *
			 * @since 0.2
			 */
			@FieldId long KnownChildSensitiveDataConsent =
				((ID | 7) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(6 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether consumer has consented
			 * to <i>Processing of the Consumer's Personal Data that Is Not Reasonably Necessary
			 * for nor Compatible with the Disclosed Purpose(s) for which the Consumer's Personal
			 * Data Was Processed</i>.
			 *
			 * @since 0.2
			 */
			@FieldId long AdditionalDataProcessingConsent =
				((ID | 8) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(7 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether publisher or advertiser,
			 * as applicable, is a signatory to the IAB Multistate Service Provider Agreement
			 * (MSPA), as may be amended from time to time, and declares that the transaction is a
			 * <i>Covered Transaction</i>, as defined in the MSPA.
			 *
			 * @since 0.2
			 */
			@FieldId long MspaCoveredTransaction =
				((ID | 9) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(8 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether publisher or advertiser,
			 * as applicable, has enabled <i>Opt-Out Option Mode</i> for the <i>Covered
			 * Transaction</i>, as such terms are defined in the MSPA.
			 *
			 * @since 0.2
			 */
			@FieldId long MspaOptOutOptionMode =
				((ID | 10) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(9 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether publisher or advertiser,
			 * as applicable, has enabled <i>Service Provider Mode</i> for the <i>Covered
			 * Transaction</i>, as such terms are defined in the MSPA.
			 *
			 * @since 0.2
			 */
			@FieldId long MspaServiceProviderMode =
				((ID | 11) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(10 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			// OBJECTS

			/**
			 * Data activity {@link Segment#getIntArray(long) int array} consent signals.
			 *
			 * @since 0.2
			 */
			@FieldId long SensitiveDataProcessing =
				((ID | 6) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedIntList | (
						(2 | (8 << FieldTypes.BITS_PER_FIXED_BIT_LENGTH)) <<
						FIELD_ARG_SHIFT
					) |
					(0 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);
		}

		/**
		 * GPC segment.
		 *
		 * @since 0.2
		 */
		interface Gpc {
			/** {@link Segment#id()} */
			@SegmentId int ID = UsStateTn.ID | (1 << SEGMENT_SHIFT);

			/**
			 * {@link Segment#getBoolean(long) Boolean} flag indicating whether consumer indicates
			 * they prefer information not be shared with, or sold to third parties.
			 *
			 * @since 0.2
			 */
			@FieldId long Gpc =
				((ID | 0) & ID_WORD_MASK) | ((long) (
					FieldTypes.Boolean |
					(0 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);
		}
	}

	/**
	 * China 中华人民共和国个人信息保护法 (Personal Information Protection Law of the People's Republic
	 * of China) section id.
	 * <p>This is a non-standard section containing consents the user has granted with respect to
	 * the China's data protection laws.
	 *
	 * @since 0.2
	 * @see <a href="https://www.gov.cn/xinwen/2021-08/20/content_5632486.htm">中华人民共和国个人信息保护法</a>
	 */
	public interface CnPrivacyV1 {
		/** {@link Section#id()} */
		int ID = 500 << SECTION_SHIFT;

		/**
		 * Core segment.
		 *
		 * @since 0.2
		 */
		interface Core {
			/** {@link Segment#id()} */
			@SegmentId int ID = CnPrivacyV1.ID | (CORE_SEGMENT_ID << SEGMENT_SHIFT);

			// INTEGERS

			/**
			 * Section version {@link Segment#getInt(long) int}.
			 *
			 * @since 0.2
			 */
			@FieldId long Version =
				((ID | 0) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (6 << FIELD_ARG_SHIFT) |
					(0 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Notice {@link Segment#getInt(long) int} describing whether notice of the
			 * <i>Processing of Personal Data</i> was presented.
			 *
			 * @since 0.2
			 */
			@FieldId long ProcessingNotice =
				((ID | 1) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(1 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Notice {@link Segment#getInt(long) int} describing whether notice of the
			 * <i>Opportunity to Opt Out of the Sale of the Consumer's Personal Data</i> was
			 * presented.
			 *
			 * @since 0.2
			 */
			@FieldId long SaleOptOutNotice =
				((ID | 2) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(2 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Notice {@link Segment#getInt(long) int} describing whether notice of the
			 * <i>Opportunity to Opt Out of Processing of the Consumer's Personal Data for
			 * Targeted Advertising</i> was presented.
			 *
			 * @since 0.2
			 */
			@FieldId long TargetedAdvertisingOptOutNotice =
				((ID | 3) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(3 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether consumer has opted out
			 * of the sale of the consumer's personal data.
			 *
			 * @since 0.2
			 */
			@FieldId long SaleOptOut =
				((ID | 4) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(4 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			/**
			 * Signal {@link Segment#getInt(long) int} describing whether consumer has opted out
			 * of the processing of the consumer's personal data for targeted advertising.
			 *
			 * @since 0.2
			 */
			@FieldId long TargetedAdvertisingOptOut =
				((ID | 5) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedInt | (2 << FIELD_ARG_SHIFT) |
					(5 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);

			// OBJECTS

			/**
			 * Data activity {@link Segment#getIntArray(long) int array} consent signals.
			 * <p>Each consent signal can have 3 possible values, {@code 0}, {@code 1}, or {@code
			 * 2} for not applicable, consent provided, consent not provided.
			 * <table>
			 *   <caption>Consent Mapping</caption>
			 *   <tr>
			 *     <th>Array Index</th>
			 *     <th>Description</th>
			 *   </tr>
			 *   <tr>
			 *     <td>0</td>
			 *     <td>
			 *       第二十三条　个人信息处理者向其他个人信息处理者提供其处理的个人信息的，
			 *       应当向个人告知接收方的名称或者姓名、联系方式、处理目的、处理方式和个人信息的种类，
			 *       并取得个人的单独同意。接收方应当在上述处理目的、处理方式和个人信息的种类等范围内处理个人信息。
			 *       接收方变更原先的处理目的、处理方式的，应当依照本法规定重新取得个人同意。
			 *     </td>
			 *   </tr>
			 *   <tr>
			 *     <td>1</td>
			 *     <td>
			 *       第二十五条　个人信息处理者不得公开其处理的个人信息，取得个人单独同意的除外。
			 *     </td>
			 *   </tr>
			 *   <tr>
			 *     <td>2</td>
			 *     <td>
			 *       第二十六条　在公共场所安装图像采集、个人身份识别设备，应当为维护公共安全所必需，
			 *       遵守国家有关规定，并设置显著的提示标识。所收集的个人图像、
			 *       身份识别信息只能用于维护公共安全的目的，不得用于其他目的；取得个人单独同意的除外。
			 *     </td>
			 *   </tr>
			 *   <tr>
			 *     <td>3</td>
			 *     <td>
			 *       第二十九条　处理敏感个人信息应当取得个人的单独同意；法律、
			 *       行政法规规定处理敏感个人信息应当取得书面同意的，从其规定。
			 *     </td>
			 *   </tr>
			 *   <tr>
			 *     <td>4</td>
			 *     <td>
			 *       第三十九条　个人信息处理者向中华人民共和国境外提供个人信息的，
			 *       应当向个人告知境外接收方的名称或者姓名、联系方式、处理目的、处理方式、
			 *       个人信息的种类以及个人向境外接收方行使本法规定权利的方式和程序等事项，并取得个人的单独同意。
			 *     </td>
			 *   </tr>
			 * </table>
			 *
			 * @since 0.2
			 */
			@FieldId long SensitiveDataProcessing =
				((ID | 6) & ID_WORD_MASK) | ((long) (
					FieldTypes.FixedIntList | (
						(2 | (5 << FieldTypes.BITS_PER_FIXED_BIT_LENGTH)) <<
						FIELD_ARG_SHIFT
					) |
					(0 << FIELD_INDEX_SHIFT)
				) << METADATA_WORD_SHIFT);
		}
	}

	private GppIds() {
	}
}
