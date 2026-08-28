// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.gppstring;

import static org.polygamma.android.origin.gppstring.GppIds.*;

import android.graphics.Point;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Pair;
import android.util.SparseArray;

import androidx.core.util.Consumer;

import org.polygamma.android.origin.util.Preconditions;
import org.polygamma.android.origin.util.Strings;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * GPP {@linkplain Section section} coding definitions.
 */
final class SectionCoding {

	private static final int[] HEADER_CODING =
		{
			-toGppSegmentId(Header.Core.ID) - 1, 3,
			toFieldMetadata(Header.Core.Type),
			toFieldMetadata(Header.Core.Version),
			toFieldMetadata(Header.Core.Sections)
		};

	private static final int[] TCF_EU_V2 =
		{
			-toGppSegmentId(TcfEuV2.Core.ID) - 1, 19,
			toFieldMetadata(TcfEuV2.Core.Version),
			toFieldMetadata(TcfEuV2.Core.Created),
			toFieldMetadata(TcfEuV2.Core.LastUpdated),
			toFieldMetadata(TcfEuV2.Core.CmpId),
			toFieldMetadata(TcfEuV2.Core.CmpVersion),
			toFieldMetadata(TcfEuV2.Core.ConsentScreen),
			toFieldMetadata(TcfEuV2.Core.ConsentLanguage),
			toFieldMetadata(TcfEuV2.Core.VendorListVersion),
			toFieldMetadata(TcfEuV2.Core.TcfPolicyVersion),
			toFieldMetadata(TcfEuV2.Core.IsServiceSpecific),
			toFieldMetadata(TcfEuV2.Core.UseNonStandardTexts),
			toFieldMetadata(TcfEuV2.Core.SpecialFeatureOptIns),
			toFieldMetadata(TcfEuV2.Core.PurposeConsent),
			toFieldMetadata(TcfEuV2.Core.PurposesLiTransparency),
			toFieldMetadata(TcfEuV2.Core.PurposeOneTreatment),
			toFieldMetadata(TcfEuV2.Core.PublisherCc),
			toFieldMetadata(TcfEuV2.Core.VendorConsent),
			toFieldMetadata(TcfEuV2.Core.VendorLegitimateInterest),
			toFieldMetadata(TcfEuV2.Core.PubRestrictions),

			toGppSegmentId(TcfEuV2.DisclosedVendors.ID), 1,
			toFieldMetadata(TcfEuV2.DisclosedVendors.DisclosedVendors),

			toGppSegmentId(TcfEuV2.PublisherPurposes.ID), 5,
			toFieldMetadata(TcfEuV2.PublisherPurposes.PubPurposesConsent),
			toFieldMetadata(TcfEuV2.PublisherPurposes.PubPurposesLiTransparency),
			toFieldMetadata(TcfEuV2.PublisherPurposes.NumCustomPurposes),
			toFieldMetadata(TcfEuV2.PublisherPurposes.CustomPurposesConsent),
			toFieldMetadata(TcfEuV2.PublisherPurposes.CustomPurposesLiTransparency)
		};

	private static final int[] TCF_CA_V1 =
		{
			-toGppSegmentId(TcfCaV1.Core.ID) - 1, 16,
			toFieldMetadata(TcfCaV1.Core.Version),
			toFieldMetadata(TcfCaV1.Core.Created),
			toFieldMetadata(TcfCaV1.Core.LastUpdated),
			toFieldMetadata(TcfCaV1.Core.CmpId),
			toFieldMetadata(TcfCaV1.Core.CmpVersion),
			toFieldMetadata(TcfCaV1.Core.ConsentScreen),
			toFieldMetadata(TcfCaV1.Core.ConsentLanguage),
			toFieldMetadata(TcfCaV1.Core.VendorListVersion),
			toFieldMetadata(TcfCaV1.Core.TcfPolicyVersion),
			toFieldMetadata(TcfCaV1.Core.UseNonStandardStacks),
			toFieldMetadata(TcfCaV1.Core.SpecialFeatureExpressConsent),
			toFieldMetadata(TcfCaV1.Core.PurposesExpressConsent),
			toFieldMetadata(TcfCaV1.Core.PurposesImpliedConsent),
			toFieldMetadata(TcfCaV1.Core.VendorExpressConsent),
			toFieldMetadata(TcfCaV1.Core.VendorImpliedConsent),
			toFieldMetadata(TcfCaV1.Core.PubRestrictions),

			toGppSegmentId(TcfCaV1.DisclosedVendors.ID), 1,
			toFieldMetadata(TcfCaV1.DisclosedVendors.DisclosedVendors),

			toGppSegmentId(TcfCaV1.PublisherPurposes.ID), 5,
			toFieldMetadata(TcfCaV1.PublisherPurposes.PubPurposesExpressConsent),
			toFieldMetadata(TcfCaV1.PublisherPurposes.PubPurposesImpliedConsent),
			toFieldMetadata(TcfCaV1.PublisherPurposes.NumCustomPurposes),
			toFieldMetadata(TcfCaV1.PublisherPurposes.CustomPurposesExpressConsent),
			toFieldMetadata(TcfCaV1.PublisherPurposes.CustomPurposesImpliedConsent)
		};

	private static final int[] US_NATIONAL =
		{
			-toGppSegmentId(UsNational.Core.ID) - 1, 16,
			toFieldMetadata(UsNational.Core.Version),
			toFieldMetadata(UsNational.Core.SharingNotice),
			toFieldMetadata(UsNational.Core.SaleOptOutNotice),
			toFieldMetadata(UsNational.Core.SharingOptOutNotice),
			toFieldMetadata(UsNational.Core.TargetedAdvertisingOptOutNotice),
			toFieldMetadata(UsNational.Core.SensitiveDataProcessingOptOutNotice),
			toFieldMetadata(UsNational.Core.SensitiveDataLimitUseNotice),
			toFieldMetadata(UsNational.Core.SaleOptOut),
			toFieldMetadata(UsNational.Core.SharingOptOut),
			toFieldMetadata(UsNational.Core.TargetedAdvertisingOptOut),
			toFieldMetadata(UsNational.Core.SensitiveDataProcessing),
			toFieldMetadata(UsNational.Core.KnownChildSensitiveDataConsents),
			toFieldMetadata(UsNational.Core.PersonalDataConsents),
			toFieldMetadata(UsNational.Core.MspaCoveredTransaction),
			toFieldMetadata(UsNational.Core.MspaOptOutOptionMode),
			toFieldMetadata(UsNational.Core.MspaServiceProviderMode),

			toGppSegmentId(UsNational.Gpc.ID), 1,
			toFieldMetadata(UsNational.Gpc.Gpc)
		};

	private static final int[] US_STATE_CA =
		{
			-toGppSegmentId(UsStateCa.Core.ID) - 1, 12,
			toFieldMetadata(UsStateCa.Core.Version),
			toFieldMetadata(UsStateCa.Core.SaleOptOutNotice),
			toFieldMetadata(UsStateCa.Core.SharingOptOutNotice),
			toFieldMetadata(UsStateCa.Core.SensitiveDataLimitUseNotice),
			toFieldMetadata(UsStateCa.Core.SaleOptOut),
			toFieldMetadata(UsStateCa.Core.SharingOptOut),
			toFieldMetadata(UsStateCa.Core.SensitiveDataProcessing),
			toFieldMetadata(UsStateCa.Core.KnownChildSensitiveDataConsents),
			toFieldMetadata(UsStateCa.Core.PersonalDataConsents),
			toFieldMetadata(UsStateCa.Core.MspaCoveredTransaction),
			toFieldMetadata(UsStateCa.Core.MspaOptOutOptionMode),
			toFieldMetadata(UsStateCa.Core.MspaServiceProviderMode),

			toGppSegmentId(UsStateCa.Gpc.ID), 1,
			toFieldMetadata(UsStateCa.Gpc.Gpc)
		};

	private static final int[] US_STATE_VA =
		{
			-toGppSegmentId(UsStateVa.Core.ID) - 1, 11,
			toFieldMetadata(UsStateVa.Core.Version),
			toFieldMetadata(UsStateVa.Core.SharingNotice),
			toFieldMetadata(UsStateVa.Core.SaleOptOutNotice),
			toFieldMetadata(UsStateVa.Core.TargetedAdvertisingOptOutNotice),
			toFieldMetadata(UsStateVa.Core.SaleOptOut),
			toFieldMetadata(UsStateVa.Core.TargetedAdvertisingOptOut),
			toFieldMetadata(UsStateVa.Core.SensitiveDataProcessing),
			toFieldMetadata(UsStateVa.Core.KnownChildSensitiveDataConsents),
			toFieldMetadata(UsStateVa.Core.MspaCoveredTransaction),
			toFieldMetadata(UsStateVa.Core.MspaOptOutOptionMode),
			toFieldMetadata(UsStateVa.Core.MspaServiceProviderMode)
		};

	private static final int[] US_STATE_CO =
		{
			-toGppSegmentId(UsStateCo.Core.ID) - 1, 11,
			toFieldMetadata(UsStateCo.Core.Version),
			toFieldMetadata(UsStateCo.Core.SharingNotice),
			toFieldMetadata(UsStateCo.Core.SaleOptOutNotice),
			toFieldMetadata(UsStateCo.Core.TargetedAdvertisingOptOutNotice),
			toFieldMetadata(UsStateCo.Core.SaleOptOut),
			toFieldMetadata(UsStateCo.Core.TargetedAdvertisingOptOut),
			toFieldMetadata(UsStateCo.Core.SensitiveDataProcessing),
			toFieldMetadata(UsStateCo.Core.KnownChildSensitiveDataConsents),
			toFieldMetadata(UsStateCo.Core.MspaCoveredTransaction),
			toFieldMetadata(UsStateCo.Core.MspaOptOutOptionMode),
			toFieldMetadata(UsStateCo.Core.MspaServiceProviderMode),

			toGppSegmentId(UsStateCo.Gpc.ID), 1,
			toFieldMetadata(UsStateCo.Gpc.Gpc)
		};

	private static final int[] US_STATE_UT =
		{
			-toGppSegmentId(UsStateUt.Core.ID) - 1, 12,
			toFieldMetadata(UsStateUt.Core.Version),
			toFieldMetadata(UsStateUt.Core.SharingNotice),
			toFieldMetadata(UsStateUt.Core.SaleOptOutNotice),
			toFieldMetadata(UsStateUt.Core.TargetedAdvertisingOptOutNotice),
			toFieldMetadata(UsStateUt.Core.SensitiveDataProcessingOptOutNotice),
			toFieldMetadata(UsStateUt.Core.SaleOptOut),
			toFieldMetadata(UsStateUt.Core.TargetedAdvertisingOptOut),
			toFieldMetadata(UsStateUt.Core.SensitiveDataProcessing),
			toFieldMetadata(UsStateUt.Core.KnownChildSensitiveDataConsents),
			toFieldMetadata(UsStateUt.Core.MspaCoveredTransaction),
			toFieldMetadata(UsStateUt.Core.MspaOptOutOptionMode),
			toFieldMetadata(UsStateUt.Core.MspaServiceProviderMode)
		};

	private static final int[] US_STATE_CT =
		{
			-toGppSegmentId(UsStateCt.Core.ID) - 1, 11,
			toFieldMetadata(UsStateCt.Core.Version),
			toFieldMetadata(UsStateCt.Core.SharingNotice),
			toFieldMetadata(UsStateCt.Core.SaleOptOutNotice),
			toFieldMetadata(UsStateCt.Core.TargetedAdvertisingOptOutNotice),
			toFieldMetadata(UsStateCt.Core.SaleOptOut),
			toFieldMetadata(UsStateCt.Core.TargetedAdvertisingOptOut),
			toFieldMetadata(UsStateCt.Core.SensitiveDataProcessing),
			toFieldMetadata(UsStateCt.Core.KnownChildSensitiveDataConsents),
			toFieldMetadata(UsStateCt.Core.MspaCoveredTransaction),
			toFieldMetadata(UsStateCt.Core.MspaOptOutOptionMode),
			toFieldMetadata(UsStateCt.Core.MspaServiceProviderMode),

			toGppSegmentId(UsStateCt.Gpc.ID), 1,
			toFieldMetadata(UsStateCt.Gpc.Gpc)
		};

	private static final int[] US_STATE_FL =
		{
			-toGppSegmentId(UsStateFl.Core.ID) - 1, 12,
			toFieldMetadata(UsStateFl.Core.Version),
			toFieldMetadata(UsStateFl.Core.ProcessingNotice),
			toFieldMetadata(UsStateFl.Core.SaleOptOutNotice),
			toFieldMetadata(UsStateFl.Core.TargetedAdvertisingOptOutNotice),
			toFieldMetadata(UsStateFl.Core.SaleOptOut),
			toFieldMetadata(UsStateFl.Core.TargetedAdvertisingOptOut),
			toFieldMetadata(UsStateFl.Core.SensitiveDataProcessing),
			toFieldMetadata(UsStateFl.Core.KnownChildSensitiveDataConsents),
			toFieldMetadata(UsStateFl.Core.AdditionalDataProcessingConsent),
			toFieldMetadata(UsStateFl.Core.MspaCoveredTransaction),
			toFieldMetadata(UsStateFl.Core.MspaOptOutOptionMode),
			toFieldMetadata(UsStateFl.Core.MspaServiceProviderMode)
		};

	private static final int[] US_STATE_MT =
		{
			-toGppSegmentId(UsStateMt.Core.ID) - 1, 12,
			toFieldMetadata(UsStateMt.Core.Version),
			toFieldMetadata(UsStateMt.Core.SharingNotice),
			toFieldMetadata(UsStateMt.Core.SaleOptOutNotice),
			toFieldMetadata(UsStateMt.Core.TargetedAdvertisingOptOutNotice),
			toFieldMetadata(UsStateMt.Core.SaleOptOut),
			toFieldMetadata(UsStateMt.Core.TargetedAdvertisingOptOut),
			toFieldMetadata(UsStateMt.Core.SensitiveDataProcessing),
			toFieldMetadata(UsStateMt.Core.KnownChildSensitiveDataProcessing),
			toFieldMetadata(UsStateMt.Core.AdditionalDataProcessingConsent),
			toFieldMetadata(UsStateMt.Core.MspaCoveredTransaction),
			toFieldMetadata(UsStateMt.Core.MspaOptOutOptionMode),
			toFieldMetadata(UsStateMt.Core.MspaServiceProviderMode),

			toGppSegmentId(UsStateMt.Gpc.ID), 1,
			toFieldMetadata(UsStateMt.Gpc.Gpc)
		};

	private static final int[] US_STATE_OR =
		{
			-toGppSegmentId(UsStateOr.Core.ID) - 1, 12,
			toFieldMetadata(UsStateOr.Core.Version),
			toFieldMetadata(UsStateOr.Core.ProcessingNotice),
			toFieldMetadata(UsStateOr.Core.SaleOptOutNotice),
			toFieldMetadata(UsStateOr.Core.TargetedAdvertisingOptOutNotice),
			toFieldMetadata(UsStateOr.Core.SaleOptOut),
			toFieldMetadata(UsStateOr.Core.TargetedAdvertisingOptOut),
			toFieldMetadata(UsStateOr.Core.SensitiveDataProcessing),
			toFieldMetadata(UsStateOr.Core.KnownChildSensitiveDataConsents),
			toFieldMetadata(UsStateOr.Core.AdditionalDataProcessingConsent),
			toFieldMetadata(UsStateOr.Core.MspaCoveredTransaction),
			toFieldMetadata(UsStateOr.Core.MspaOptOutOptionMode),
			toFieldMetadata(UsStateOr.Core.MspaServiceProviderMode),

			toGppSegmentId(UsStateOr.Gpc.ID), 1,
			toFieldMetadata(UsStateOr.Gpc.Gpc)
		};

	private static final int[] US_STATE_TX =
		{
			-toGppSegmentId(UsStateTx.Core.ID) - 1, 12,
			toFieldMetadata(UsStateTx.Core.Version),
			toFieldMetadata(UsStateTx.Core.ProcessingNotice),
			toFieldMetadata(UsStateTx.Core.SaleOptOutNotice),
			toFieldMetadata(UsStateTx.Core.TargetedAdvertisingOptOutNotice),
			toFieldMetadata(UsStateTx.Core.SaleOptOut),
			toFieldMetadata(UsStateTx.Core.TargetedAdvertisingOptOut),
			toFieldMetadata(UsStateTx.Core.SensitiveDataProcessing),
			toFieldMetadata(UsStateTx.Core.KnownChildSensitiveDataConsents),
			toFieldMetadata(UsStateTx.Core.AdditionalDataProcessingConsent),
			toFieldMetadata(UsStateTx.Core.MspaCoveredTransaction),
			toFieldMetadata(UsStateTx.Core.MspaOptOutOptionMode),
			toFieldMetadata(UsStateTx.Core.MspaServiceProviderMode),

			toGppSegmentId(UsStateTx.Gpc.ID), 1,
			toFieldMetadata(UsStateTx.Gpc.Gpc)
		};

	private static final int[] US_STATE_DE =
		{
			-toGppSegmentId(UsStateDe.Core.ID) - 1, 12,
			toFieldMetadata(UsStateDe.Core.Version),
			toFieldMetadata(UsStateDe.Core.ProcessingNotice),
			toFieldMetadata(UsStateDe.Core.SaleOptOutNotice),
			toFieldMetadata(UsStateDe.Core.TargetedAdvertisingOptOutNotice),
			toFieldMetadata(UsStateDe.Core.SaleOptOut),
			toFieldMetadata(UsStateDe.Core.TargetedAdvertisingOptOut),
			toFieldMetadata(UsStateDe.Core.SensitiveDataProcessing),
			toFieldMetadata(UsStateDe.Core.KnownChildSensitiveDataConsents),
			toFieldMetadata(UsStateDe.Core.AdditionalDataProcessingConsent),
			toFieldMetadata(UsStateDe.Core.MspaCoveredTransaction),
			toFieldMetadata(UsStateDe.Core.MspaOptOutOptionMode),
			toFieldMetadata(UsStateDe.Core.MspaServiceProviderMode),

			toGppSegmentId(UsStateDe.Gpc.ID), 1,
			toFieldMetadata(UsStateDe.Gpc.Gpc)
		};

	private static final int[] US_STATE_IA =
		{
			-toGppSegmentId(UsStateIa.Core.ID) - 1, 12,
			toFieldMetadata(UsStateIa.Core.Version),
			toFieldMetadata(UsStateIa.Core.ProcessingNotice),
			toFieldMetadata(UsStateIa.Core.SaleOptOutNotice),
			toFieldMetadata(UsStateIa.Core.TargetedAdvertisingOptOutNotice),
			toFieldMetadata(UsStateIa.Core.SensitiveDataOptOutNotice),
			toFieldMetadata(UsStateIa.Core.SaleOptOut),
			toFieldMetadata(UsStateIa.Core.TargetedAdvertisingOptOut),
			toFieldMetadata(UsStateIa.Core.SensitiveDataProcessing),
			toFieldMetadata(UsStateIa.Core.KnownChildSensitiveDataConsents),
			toFieldMetadata(UsStateIa.Core.MspaCoveredTransaction),
			toFieldMetadata(UsStateIa.Core.MspaOptOutOptionMode),
			toFieldMetadata(UsStateIa.Core.MspaServiceProviderMode),

			toGppSegmentId(UsStateIa.Gpc.ID), 1,
			toFieldMetadata(UsStateIa.Gpc.Gpc)
		};

	private static final int[] US_STATE_NE =
		{
			-toGppSegmentId(UsStateNe.Core.ID) - 1, 12,
			toFieldMetadata(UsStateNe.Core.Version),
			toFieldMetadata(UsStateNe.Core.ProcessingNotice),
			toFieldMetadata(UsStateNe.Core.SaleOptOutNotice),
			toFieldMetadata(UsStateNe.Core.TargetedAdvertisingOptOutNotice),
			toFieldMetadata(UsStateNe.Core.SaleOptOut),
			toFieldMetadata(UsStateNe.Core.TargetedAdvertisingOptOut),
			toFieldMetadata(UsStateNe.Core.SensitiveDataProcessing),
			toFieldMetadata(UsStateNe.Core.KnownChildSensitiveDataConsents),
			toFieldMetadata(UsStateNe.Core.AdditionalDataProcessingConsent),
			toFieldMetadata(UsStateNe.Core.MspaCoveredTransaction),
			toFieldMetadata(UsStateNe.Core.MspaOptOutOptionMode),
			toFieldMetadata(UsStateNe.Core.MspaServiceProviderMode),

			toGppSegmentId(UsStateNe.Gpc.ID), 1,
			toFieldMetadata(UsStateNe.Gpc.Gpc)
		};

	private static final int[] US_STATE_NH =
		{
			-toGppSegmentId(UsStateNh.Core.ID) - 1, 12,
			toFieldMetadata(UsStateNh.Core.Version),
			toFieldMetadata(UsStateNh.Core.ProcessingNotice),
			toFieldMetadata(UsStateNh.Core.SaleOptOutNotice),
			toFieldMetadata(UsStateNh.Core.TargetedAdvertisingOptOutNotice),
			toFieldMetadata(UsStateNh.Core.SaleOptOut),
			toFieldMetadata(UsStateNh.Core.TargetedAdvertisingOptOut),
			toFieldMetadata(UsStateNh.Core.SensitiveDataProcessing),
			toFieldMetadata(UsStateNh.Core.KnownChildSensitiveDataConsents),
			toFieldMetadata(UsStateNh.Core.AdditionalDataProcessingConsent),
			toFieldMetadata(UsStateNh.Core.MspaCoveredTransaction),
			toFieldMetadata(UsStateNh.Core.MspaOptOutOptionMode),
			toFieldMetadata(UsStateNh.Core.MspaServiceProviderMode),

			toGppSegmentId(UsStateNh.Gpc.ID), 1,
			toFieldMetadata(UsStateNh.Gpc.Gpc)
		};

	private static final int[] US_STATE_NJ =
		{
			-toGppSegmentId(UsStateNj.Core.ID) - 1, 12,
			toFieldMetadata(UsStateNj.Core.Version),
			toFieldMetadata(UsStateNj.Core.ProcessingNotice),
			toFieldMetadata(UsStateNj.Core.SaleOptOutNotice),
			toFieldMetadata(UsStateNj.Core.TargetedAdvertisingOptOutNotice),
			toFieldMetadata(UsStateNj.Core.SaleOptOut),
			toFieldMetadata(UsStateNj.Core.TargetedAdvertisingOptOut),
			toFieldMetadata(UsStateNj.Core.SensitiveDataProcessing),
			toFieldMetadata(UsStateNj.Core.KnownChildSensitiveDataConsents),
			toFieldMetadata(UsStateNj.Core.AdditionalDataProcessingConsent),
			toFieldMetadata(UsStateNj.Core.MspaCoveredTransaction),
			toFieldMetadata(UsStateNj.Core.MspaOptOutOptionMode),
			toFieldMetadata(UsStateNj.Core.MspaServiceProviderMode),

			toGppSegmentId(UsStateNj.Gpc.ID), 1,
			toFieldMetadata(UsStateNj.Gpc.Gpc)
		};

	private static final int[] US_STATE_TN =
		{
			-toGppSegmentId(UsStateTn.Core.ID) - 1, 12,
			toFieldMetadata(UsStateTn.Core.Version),
			toFieldMetadata(UsStateTn.Core.ProcessingNotice),
			toFieldMetadata(UsStateTn.Core.SaleOptOutNotice),
			toFieldMetadata(UsStateTn.Core.TargetedAdvertisingOptOutNotice),
			toFieldMetadata(UsStateTn.Core.SaleOptOut),
			toFieldMetadata(UsStateTn.Core.TargetedAdvertisingOptOut),
			toFieldMetadata(UsStateTn.Core.SensitiveDataProcessing),
			toFieldMetadata(UsStateTn.Core.KnownChildSensitiveDataConsent),
			toFieldMetadata(UsStateTn.Core.AdditionalDataProcessingConsent),
			toFieldMetadata(UsStateTn.Core.MspaCoveredTransaction),
			toFieldMetadata(UsStateTn.Core.MspaOptOutOptionMode),
			toFieldMetadata(UsStateTn.Core.MspaServiceProviderMode),

			toGppSegmentId(UsStateTn.Gpc.ID), 1,
			toFieldMetadata(UsStateTn.Gpc.Gpc)
		};

	private static final int[] CN_PRIVACY_V1 =
		{
			-toGppSegmentId(CnPrivacyV1.Core.ID) - 1, 7,
			toFieldMetadata(CnPrivacyV1.Core.Version),
			toFieldMetadata(CnPrivacyV1.Core.ProcessingNotice),
			toFieldMetadata(CnPrivacyV1.Core.SaleOptOutNotice),
			toFieldMetadata(CnPrivacyV1.Core.TargetedAdvertisingOptOutNotice),
			toFieldMetadata(CnPrivacyV1.Core.SaleOptOut),
			toFieldMetadata(CnPrivacyV1.Core.TargetedAdvertisingOptOut),
			toFieldMetadata(CnPrivacyV1.Core.SensitiveDataProcessing)
		};

	/**
	 * Array of section coding specification.
	 * <p>The first dimension of this array is indexed by the section id minus {@code 1} for
	 * standard sections, and for non-standard sections, indexed by section id minus {@code 500}.
	 * Non-standard sections start counting from the end of the first dimension. For example, a
	 * non-standard section with id {@code 501} has its definition stored at {@code
	 * CODING.length - 1 - (501 - 500)}.
	 * <p>The second dimension of this array contains, for each nested segment, the segment id
	 * ({@code -id - 1} if the segment is required), segment field count, followed by, for each
	 * field, the field specification.
	 */
	private static final int[][] CODING =
		{
			/*   1: TcfEuV1         */ null,
			/*   2: TcfEuV2         */ TCF_EU_V2,
			/*   3: Header          */ HEADER_CODING,
			/*   4: SignalIntegrity */ null,
			/*   5: TcfCaV1         */ TCF_CA_V1,
			/*   6: UsPrivacyV1     */ null,
			/*   7: Us              */ US_NATIONAL,
			/*   8: UsCa            */ US_STATE_CA,
			/*   9: UsVa            */ US_STATE_VA,
			/*  10: UsCo            */ US_STATE_CO,
			/*  11: UsUt            */ US_STATE_UT,
			/*  12: UsCt            */ US_STATE_CT,
			/*  13: UsFl            */ US_STATE_FL,
			/*  14: UsMt            */ US_STATE_MT,
			/*  15: UsOr            */ US_STATE_OR,
			/*  16: UsTx            */ US_STATE_TX,
			/*  17: UsDe            */ US_STATE_DE,
			/*  18: UsIa            */ US_STATE_IA,
			/*  19: UsNe            */ US_STATE_NE,
			/*  20: UsNh            */ US_STATE_NH,
			/*  21: UsNj            */ US_STATE_NJ,
			/*  22: UsTn            */ US_STATE_TN,
			/* 500: CnPrivacyV1     */ CN_PRIVACY_V1
		};

	/**
	 * Find coding for a section by id.
	 *
	 * @param id id of section to find coding of
	 * @return section coding
	 * @throws IllegalArgumentException {@code id} is not {@linkplain GppIds#toGppSectionId(int)
	 * valid}
	 * @throws IllegalStateException decoding {@code id} is not yet supported
	 */
	private static int[] codingOf(@SectionId int id) {
		int idx = GppIds.toGppSectionId(id);
		int[] coding = CODING[idx >= 500 ? (CODING.length - 1 - (500 - idx)) : (idx - 1)];

		Preconditions.checkState(coding != null);
		return coding;
	}

	/**
	 * Find offset of a segments coding within a section coding array.
	 *
	 * @param id id of segment to find coding of
	 * @param coding coding array to search in
	 * @param codingOff offset within {@code coding} to search from
	 * @return offset of segment within {@code coding}
	 * @throws IllegalArgumentException {@code id} does not exist within {@code coding}
	 */
	private static int codingOffsetOfSegment(@SegmentId int id, int[] coding, int codingOff) {
		int segId = toGppSegmentId(id);

		while (codingOff < coding.length) {
			int currSegId = coding[codingOff];

			if (currSegId < 0)
				currSegId = -currSegId - 1;
			if (segId == currSegId)
				return codingOff;
			codingOff += 2 + coding[codingOff + 1];
		}
		throw new IllegalArgumentException(String.format(
			Locale.ROOT,
			"segment %s.%s not found",
			toGppSectionId(sectionIdOfSegment(id)),
			segId
		));
	}

	/**
	 * Determine the fixed length of a segment id of a section.
	 *
	 * @param id id of section
	 * @return fixed length of segment id
	 * @throws IllegalArgumentException {@code id} does not define any non-core optional segments
	 */
	private static int segmentIdBitSizeOf(@SectionId int id) {
		switch (id) {
		case TcfCaV1.ID:
		case TcfEuV2.ID:
			return 3;
		case UsNational.ID:
		case UsStateCa.ID:
		case UsStateCo.ID:
		case UsStateCt.ID:
		case UsStateDe.ID:
		case UsStateIa.ID:
		case UsStateMt.ID:
		case UsStateNe.ID:
		case UsStateNh.ID:
		case UsStateNj.ID:
		case UsStateOr.ID:
		case UsStateTn.ID:
		case UsStateTx.ID:
			return 2;
		default:
			return 0;
		}
	}

	/**
	 * Calculate segment value table sizes of a segment.
	 *
	 * @param coding section coding
	 * @param codingOff offset, within {@code coding} of segment coding
	 * @return object, {@code int}, and {@code boolean} table sizes, respectively
	 */
	private static int[] segmentValueTableSizes(int[] coding, int codingOff) {
		int[] sizes = new int[3];

		for (int i = 0; i < coding[codingOff + 1]; i++) {
			int meta = coding[codingOff + 2 + i];
			int j;

			switch (toFieldType(meta)) {
			case FieldTypes.Boolean:
				j = 2;
				break;
			case FieldTypes.FibonacciInt:
			case FieldTypes.FixedInt:
				j = 1;
				break;
			default:
				j = 0;
				break;
			}
			sizes[j] = Math.max(sizes[j], toFieldValueIndex(meta) + 1);
		}
		return sizes;
	}

	/**
	 * Decode segment from a string using a coding.
	 *
	 * @param id id of segment to decode
	 * @param src buffer to decode
	 * @param coding section coding
	 * @param codingOff offset of segment coding within {@code coding}
	 * @return decoded segment
	 * @throws IllegalArgumentException {@code src} is malformed
	 */
	private static Segment
	decodeSegment(@SegmentId int id, BitBuffer src, int[] coding, int codingOff) {
		int[] tableSizes = segmentValueTableSizes(coding, codingOff);
		Object[] objs = new Object[tableSizes[0]];
		int[] ints = new int[tableSizes[1]];
		boolean[] bools = new boolean[tableSizes[2]];
		int lastInt = 0;

		for (int i = 0; i < coding[codingOff + 1]; i++) {
			int meta = coding[codingOff + 2 + i];
			int idx = toFieldValueIndex(meta);
			int fieldArg;
			Object val;

			switch (toFieldType(meta)) {
			case FieldTypes.Boolean:
				bools[idx] = src.readBoolean();
				continue;
			case FieldTypes.FibonacciInt:
				lastInt = ints[idx] = src.readFibonacciInt();
				continue;
			case FieldTypes.FixedInt:
				lastInt = ints[idx] = src.readFixedInt(toFieldFixedBitLength(meta));
				continue;
			case FieldTypes.FixedIntList:
				Point fixedListSize = toFieldFixedBitLength2(meta);

				val = src.readFixedIntList(fixedListSize.x, fixedListSize.y);
				break;
			case FieldTypes.FixedString:
				fieldArg = toFieldTypeArg(meta);
				val = src.readFixedString(fieldArg);
				break;
			case FieldTypes.Datetime:
				val = src.readDatetime();
				break;
			case FieldTypes.FixedBitfield:
				// an arg of 0 indicates we need to use the value of the last `int` field.
				fieldArg = toFieldTypeArg(meta);
				val = src.readFixedBitfield(fieldArg == 0 ? lastInt : fieldArg);
				break;
			case FieldTypes.Bitfield:
				val = src.readBitfield();
				break;
			case FieldTypes.FixedInt16Range:
				val = src.readFixedInt16Range();
				break;
			case FieldTypes.FibonacciIntRange:
				val = src.readFibonacciIntRange();
				break;
			case FieldTypes.OptimizedIntRange:
				val = src.readOptimizedIntRange();
				break;
			case FieldTypes.OptimizedIntRange2:
				val = src.readOptimizedIntRange2();
				break;
			case FieldTypes.ArrayOfIntRanges:
				val = src.readArrayOfIntRanges();
				break;
			case FieldTypes.ArrayOfFixedIntRanges:
				Point fixedRangeSize = toFieldFixedBitLength2(meta);

				val = src.readArrayOfFixedIntRanges(fixedRangeSize.x, fixedRangeSize.y);
				break;
			default:
				throw new AssertionError();
			}
			objs[idx] = val;
		}
		return new Segment(id, objs, ints, bools);
	}

	/**
	 * Encode segment into a buffer using a coding.
	 *
	 * @param dst buffer to encode into
	 * @param src segment to encode
	 * @param coding section coding
	 * @param codingOff offset of segment coding within {@code coding}
	 */
	private static void encodeSegment(BitBuffer dst, Segment src, int[] coding, int codingOff) {
		int lastInt = 0;

		for (int i = 0; i < coding[codingOff + 1]; i++) {
			int meta = coding[codingOff + 2 + i];
			int idx = toFieldValueIndex(meta);
			int fieldArg;

			switch (toFieldType(meta)) {
			case FieldTypes.Boolean:
				dst.writeBoolean(src.getBooleanAt(idx));
				break;
			case FieldTypes.FibonacciInt:
				lastInt = src.getIntAt(idx);
				dst.writeFibonacciInt(lastInt);
				break;
			case FieldTypes.FixedInt:
				lastInt = src.getIntAt(idx);
				dst.writeFixedInt(toFieldFixedBitLength(meta), lastInt);
				break;
			case FieldTypes.FixedIntList:
				dst.writeFixedIntList(toFieldFixedBitLength2(meta).x, src.getIntArrayAt(idx));
				break;
			case FieldTypes.FixedString:
				dst.writeFixedString(src.getStringAt(idx));
				break;
			case FieldTypes.Datetime:
				dst.writeDatetime(src.getDateAt(idx));
				break;
			case FieldTypes.FixedBitfield:
				// an arg of 0 indicates we need to use the value of the last `int` field.
				fieldArg = toFieldTypeArg(meta);
				dst.writeFixedBitfield(fieldArg == 0 ? lastInt : fieldArg, src.getBitfieldAt(idx));
				break;
			case FieldTypes.Bitfield:
				dst.writeBitfield(src.getBitfieldAt(idx));
				break;
			case FieldTypes.FixedInt16Range:
				dst.writeFixedInt16Range(src.getIntArrayAt(idx));
				break;
			case FieldTypes.FibonacciIntRange:
				dst.writeFibonacciIntRange(src.getIntArrayAt(idx));
				break;
			case FieldTypes.OptimizedIntRange:
				dst.writeOptimizedIntRange(src.getIntArrayAt(idx));
				break;
			case FieldTypes.OptimizedIntRange2:
				dst.writeOptimizedIntRange2(src.getIntArrayAt(idx));
				break;
			case FieldTypes.ArrayOfIntRanges:
				dst.writeArrayOfIntRanges(src.getTaggedIdsArrayAt(idx));
				break;
			case FieldTypes.ArrayOfFixedIntRanges:
				Point fixedRangeSize = toFieldFixedBitLength2(meta);

				dst.writeArrayOfFixedIntRanges(
					fixedRangeSize.x,
					fixedRangeSize.y,
					src.getTaggedIdsArrayAt(idx)
				);
				break;
			default:
				throw new AssertionError();
			}
		}
	}

	/**
	 * Construct a {@linkplain BitBuffer bit buffer} of a URL safe base64 encoded string.
	 *
	 * @param str base64 encoded string to construct buffer of
	 * @return resulting bit buffer
	 */
	private static BitBuffer bitBufferOfBase64(String str) {
		if (!str.endsWith("=")) {
			int rem = str.length() % 4;

			if (rem > 0) {
				byte[] pad = new byte[4 - rem];

				Arrays.fill(pad, (byte) 'A');
				str = str.concat(new String(pad, StandardCharsets.US_ASCII));
			}
		}
		return new BitBuffer(Base64.decode(str, Base64.URL_SAFE));
	}

	/**
	 * Encode {@linkplain BitBuffer bit buffer} contents to a URL safe base64 string.
	 *
	 * @param buff buffer to encode contents of
	 * @return base64 encoded string
	 */
	private static String base64OfBitBuffer(BitBuffer buff) {
		return Base64.encodeToString(
			buff.toByteArray(),
			Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP
		);
	}

	private static Segment decodeUsPrivacy1(String str) {
		Preconditions.checkArgument(str.length() == 4);

		int[] vals = new int[4];

		char chr = str.charAt(0);

		Preconditions.checkArgument(chr == '1');
		vals[0] = 1;

		for (int i = 1; i < str.length(); i++) {
			chr = str.charAt(i);

			if (chr == 'Y')
				vals[i] = 1;
			else if (chr == 'N')
				vals[i] = 2;
			else
				Preconditions.checkArgument(chr == '-');
		}
		return new Segment(UsPrivacyV1.Core.ID, new Object[0], vals, new boolean[0]);
	}

	/**
	 * Decode segment from a string.
	 *
	 * @param id id of segment to decode
	 * @param str encoded string
	 * @return decoded segment
	 * @throws IllegalArgumentException {@code id} is not a valid section id or {@code str} is
	 * malformed
	 * @throws IllegalStateException decoding {@code id} is not yet supported
	 */
	static Segment decodeSegment(@SegmentId int id, String str) {
		if (id == UsPrivacyV1.Core.ID)
			return decodeUsPrivacy1(str);

		int[] coding = codingOf(sectionIdOfSegment(id));
		int codingOff = codingOffsetOfSegment(id, coding, 0);
		BitBuffer buff = bitBufferOfBase64(str);

		if (coding[codingOff] >= 0) {
			Preconditions.checkArgument(
				toGppSegmentId(id) ==
				buff.readFixedInt(segmentIdBitSizeOf(sectionIdOfSegment(id)))
			);
		}
		return decodeSegment(id, buff, coding, codingOff);
	}

	/**
	 * Decode required segments of a section.
	 *
	 * @param segs array to store decoded segments into
	 * @param id id of section to decode required sections of
	 * @param coding section coding
	 * @param str iterator of segment sources to decode
	 * @return offset, within {@code coding}, of first non-core optional segment coding, possibly
	 * {@code coding.length}
	 */
	private static int decodeRequiredSegments(
		SparseArray<Segment> segs,
		@SectionId int id,
		int[] coding,
		Iterator<String> str
	) {
		int gppId = toGppSectionId(id);
		int i;

		for (i = 0; i < coding.length; i += 2 + coding[i + 1]) {
			int gppSegId = coding[i];

			if (gppSegId >= 0)
				break;

			gppSegId = -gppSegId - 1;
			Preconditions.checkArgument(
				str.hasNext(),
				"expected segment %s.%s", gppId, gppSegId
			);

			@SegmentId int segId = ofGppSegmentId(gppId, gppSegId);
			BitBuffer segBuff = bitBufferOfBase64(str.next());

			segs.put(gppSegId, decodeSegment(segId, segBuff, coding, i));
		}
		return i;
	}

	/**
	 * Decode segments of a section from a string, returning decoded segments array.
	 *
	 * @param id id of section to decode segments of
	 * @param str string to decode
	 * @param onError consumer to invoke with malformed sections and error cause tuple
	 * @return decoded segments
	 * @throws IllegalArgumentException {@code id} is not {@linkplain GppIds#toGppSectionId(int)
	 * valid}
	 * @throws IllegalStateException decoding {@code id} is not yet supported
	 */
	private static SparseArray<Segment>
	decodeSegments(@SectionId int id, String str, Consumer<Pair<String, Throwable>> onError) {
		Iterator<String> segStrs = Strings.split(str, '.');
		SparseArray<Segment> segs = new SparseArray<>(1);
		int[] coding = codingOf(id);
		int codingOff = decodeRequiredSegments(segs, id, coding, segStrs);

		while (segStrs.hasNext()) {
			String segStr = segStrs.next();

			try {
				BitBuffer segBuff = bitBufferOfBase64(segStr);
				int gppSegId = segBuff.readFixedInt(segmentIdBitSizeOf(id));
				@SegmentId int segId = ofGppSegmentId(toGppSectionId(id), gppSegId);
				int segCodingOff = codingOffsetOfSegment(segId, coding, codingOff);

				segs.put(gppSegId, decodeSegment(segId, segBuff, coding, segCodingOff));
			} catch (Exception e) {
				onError.accept(new Pair<>(segStr, e));
			}
		}
		return segs;
	}

	/**
	 * Decode segments of a section from a string.
	 *
	 * @param id id of section to decode
	 * @param str encoded string
	 * @param onError consumer to invoke with malformed sections and error cause tuple
	 * @return decoded section
	 * @throws IllegalArgumentException {@code id} is not {@linkplain GppIds#toGppSectionId(int)
	 * valid}
	 * @throws IllegalStateException decoding {@code id} is not yet supported
	 */
	static Section
	decode(@SectionId int id, String str, Consumer<Pair<String, Throwable>> onError) {
		if (id == UsPrivacyV1.ID)
			return new Section(id, decodeSegment(UsPrivacyV1.Core.ID, str), new Segment[0]);

		SparseArray<Segment> segs = decodeSegments(id, str, onError);
		int coreIdx = segs.indexOfKey(CORE_SEGMENT_ID);

		Preconditions.checkArgument(coreIdx >= 0, "core segment not found");

		Segment core = segs.valueAt(coreIdx);
		Segment[] rest = new Segment[segs.size() - 1];

		segs.removeAt(coreIdx);
		for (int i = 0; i < segs.size(); i++)
			rest[i] = segs.valueAt(i);
		return new Section(id, core, rest);
	}

	private static String encodeUsPrivacy1(Segment seg) {
		char[] enc = new char[4];

		enc[0] = (char) ((('0' & 0xff) + seg.getIntAt(0)));
		for (int i = 1; i < 4; i++) {
			int val = seg.getIntAt(i);

			enc[i] =
				val == 0 ? '-' :
				val == 1 ? 'Y' : 'N';
		}
		return new String(enc);
	}

	/**
	 * Encode segment into a string.
	 *
	 * @param seg segment to encode
	 * @return encoded segment
	 * @throws IllegalArgumentException {@code seg} is malformed
	 */
	static String encodeSegment(Segment seg) {
		if (seg.id() == UsPrivacyV1.Core.ID)
			return encodeUsPrivacy1(seg);

		int[] coding = codingOf(sectionIdOfSegment(seg.id()));
		int codingOff = codingOffsetOfSegment(seg.id(), coding, 0);
		BitBuffer buff = new BitBuffer();

		if (coding[codingOff] >= 0) {
			buff.writeFixedInt(
				segmentIdBitSizeOf(sectionIdOfSegment(seg.id())),
				toGppSegmentId(seg.id())
			);
		}
		encodeSegment(buff, seg, coding, codingOff);
		return base64OfBitBuffer(buff);
	}

	/**
	 * Find segment for an id, remove it, and return its reference.
	 *
	 * @param segs list of segments to operate on
	 * @param id id of segment to find
	 * @return segment reference
	 * @throws IllegalArgumentException no segment with {@code id} exists in {@code segs}
	 */
	private static Segment findAndRemoveSegment(List<Segment> segs, @SegmentId int id) {
		for (int i = 0; i < segs.size(); i++) {
			Segment seg = segs.get(i);

			if (seg.id() == id) {
				segs.remove(i);
				return seg;
			}
		}
		throw new IllegalArgumentException(String.format(
			Locale.ROOT,
			"segment %s.%s not found",
			toGppSectionId(sectionIdOfSegment(id)),
			toGppSegmentId(id)
		));
	}

	/**
	 * Encode required segments of a section.
	 * <p>Upon return, all core and required segments are removed from {@code segs}.
	 *
	 * @param strs list to add encoded segments to
	 * @param id id of section to encode required sections of
	 * @param coding section coding
	 * @param segs list of segments to encode required segments from
	 * @return offset, within {@code coding}, of first non-core optional segment coding, possibly
	 * {@code coding.length}
	 */
	private static int encodeRequiredSegments(
		List<String> strs,
		@SectionId int id,
		int[] coding,
		List<Segment> segs
	) {
		int gppId = toGppSectionId(id);
		int i;

		for (i = 0; i < coding.length; i += 2 + coding[i + 1]) {
			int gppSegId = coding[i];

			if (gppSegId >= 0)
				break;

			Segment seg = findAndRemoveSegment(segs, ofGppSegmentId(gppId, -gppSegId - 1));
			BitBuffer segBuff = new BitBuffer();

			encodeSegment(segBuff, seg, coding, i);
			strs.add(base64OfBitBuffer(segBuff));
		}
		return i;
	}

	/**
	 * Encode segments of a section into strings.
	 * <p>Upon return, {@code segs} will be {@linkplain List#isEmpty() empty}.
	 *
	 * @param id id of section to encode segments of
	 * @param segs segments to encode
	 * @return encoded segments
	 * @throws IllegalArgumentException {@code segs} does not contain a core or a required segment
	 */
	private static List<String> encodeSegments(@SectionId int id, List<Segment> segs) {
		ArrayList<String> segStrs = new ArrayList<>(segs.size());
		int[] coding = codingOf(id);
		int codingOff = encodeRequiredSegments(segStrs, id, coding, segs);

		while (!segs.isEmpty()) {
			Segment seg = segs.remove(0);
			BitBuffer segBuff = new BitBuffer();

			segBuff.writeFixedInt(segmentIdBitSizeOf(id), toGppSegmentId(seg.id()));
			encodeSegment(segBuff, seg, coding, codingOffsetOfSegment(seg.id(), coding, codingOff));
			segStrs.add(base64OfBitBuffer(segBuff));
		}
		return segStrs;
	}

	/**
	 * Encode section into string.
	 *
	 * @param sect section to encode
	 * @return encoded representation of {@code sect}
	 * @throws IllegalArgumentException {@code sect} is malformed
	 */
	static String encode(Section sect) {
		if (sect.id() == UsPrivacyV1.ID)
			return encodeSegment(sect.core());

		ArrayList<Segment> segs = new ArrayList<>(1 + sect.segmentCount());

		segs.add(sect.core());
		for (int i = 0; i < sect.segmentCount(); i++)
			segs.add(sect.segmentAt(i));
		return TextUtils.join(".", encodeSegments(sect.id(), segs));
	}

	private SectionCoding() {
	}
}
