// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.ads;

import static org.polygamma.android.origin.protobuf.Protobuf.WIRE_FIXED32;
import static org.polygamma.android.origin.protobuf.Protobuf.WIRE_LEN;
import static org.polygamma.android.origin.protobuf.Protobuf.WIRE_VARINT;
import static org.polygamma.android.origin.protobuf.Protobuf.fieldTagOf;

import androidx.annotation.IntDef;

import org.polygamma.android.origin.protobuf.Protobuf;
import org.polygamma.android.origin.protobuf.Protobuf.FieldTag;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Ads service remote procedure call (RPC) constants.
 */
interface Rpc {

	/**
	 * {@code get-ads} RPC constants.
	 */
	interface GetAds {
		/**
		 * Unknown error encountered.
		 */
		int ERROR_UNKNOWN				= 0;

		/**
		 * Ad is not available for placement.
		 */
		int ERROR_NO_AD					= 1;

		/**
		 * System error encountered.
		 */
		int ERROR_SYSTEM				= 2;

		/**
		 * Invalid placement.
		 */
		int ERROR_INVALID_PLACEMENT		= 3;

		/**
		 * Placement has been disabled.
		 */
		int ERROR_PLACEMENT_DISABLED	= 4;

		/**
		 * Enumeration of RPC error codes.
		 * <p>{@snippet lang="protobuf" :
		 * enum ErrorCode {
		 *     ErrorUnknown				= 0;
		 *     ErrorNoAd				= 1;
		 *     ErrorSystem				= 2;
		 *     ErrorInvalidPlacement	= 3;
		 *     ErrorPlacementDisabled	= 4;
		 * }
		 * }
		 */
		@Documented
		@Retention(RetentionPolicy.SOURCE)
		@Target(ElementType.TYPE_USE)
		@IntDef({
			ERROR_INVALID_PLACEMENT,
			ERROR_NO_AD,
			ERROR_PLACEMENT_DISABLED,
			ERROR_SYSTEM,
			ERROR_UNKNOWN
		})
		@interface ErrorCode {
		}

		/**
		 * {@code get-ads} RPC argument constants.
		 * <p>{@snippet lang="protobuf" :
		 * message GetAdsArguments {
		 *     bytes cookie				= 1;
		 * 	   string ivtdigest			= 2;
		 * 	   string adcomver			= 3;
		 * 	   App app					= 4;
		 * 	   Device device			= 5;
		 * 	   Regs regs				= 6;
		 * 	   repeated Placement plcmt	= 7;
		 * }
		 * }
		 */
		interface Arguments {
			/**
			 * Backend persistent settings cookie.
			 */
			@FieldTag int COOKIE		= fieldTagOf(1, WIRE_LEN);

			/**
			 * Antifraud {@linkplain
			 * org.polygamma.android.origin.antifraud.AntifraudStatus#digest() digest}.
			 */
			@FieldTag int IVTDIGEST		= fieldTagOf(2, WIRE_LEN);

			/**
			 * AdCOM model {@linkplain org.polygamma.android.origin.adcom.AdCom#DOMAIN_VERSION
			 * version}.
			 */
			@FieldTag int ADCOMVER		= fieldTagOf(3, WIRE_LEN);

			/**
			 * {@linkplain org.polygamma.android.origin.core.Origin#app() Distribution channel}
			 * ad media is requested for.
			 */
			@FieldTag int CHANNEL		= fieldTagOf(4, WIRE_LEN);

			/**
			 * {@linkplain org.polygamma.android.origin.core.DeviceModule#device() Device} on
			 * which ad media will be executed.
			 */
			@FieldTag int DEVICE		= fieldTagOf(5, WIRE_LEN);

			/**
			 * {@linkplain org.polygamma.android.origin.core.RegulationsModule#regs() Regulations}
			 * applicable to device.
			 */
			@FieldTag int REGS			= fieldTagOf(6, WIRE_LEN);

			/**
			 * {@linkplain org.polygamma.android.origin.adcom.placement.Placement Placements} for
			 * which ad media is being requested.
			 */
			@FieldTag int PLCMT			= fieldTagOf(7, WIRE_LEN);
		}

		/**
		 * {@code get-ads} RPC result constants.
		 * <p>{@snippet lang="protobuf" :
		 * message GetAdsResult {
		 *     message AdItem {
		 *         string id			= 1;
		 *         Ad ad				= 2;
		 *         uint64 rwditemcount	= 3;
		 *         string rwditemtype	= 4;
		 *         float previewscale	= 5;
		 *         uint64 pricemilli	= 6;
		 *         string pricecur		= 7;
		 *     }
		 *
		 *     message ErrorItem {
		 *         ErrorCode code		= 1;
		 *         string msg			= 2;
		 *     }
		 *
		 *     message Item {
		 *         string plcmtid		= 1;
		 *         oneof inner {
		 *             ErrorItem err	= 2;
		 *             AdItem ad        = 3;
		 *         }
		 *     }
		 *
		 *     bytes cookie				= 1;
		 * 	   repeated Item item		= 2;
		 * }
		 * }
		 */
		interface Result {
			/**
			 * Backend persistent settings cookie.
			 */
			@FieldTag int COOKIE	= fieldTagOf(1, WIRE_LEN);

			/**
			 * Result items.
			 */
			@FieldTag int ITEM		= fieldTagOf(2, WIRE_LEN);

			/**
			 * Ad result item.
			 */
			interface AdItem {
				/**
				 * Item identifier.
				 */
				@FieldTag int ID			= fieldTagOf(1, WIRE_LEN);

				/**
				 * {@linkplain org.polygamma.android.origin.adcom.media.Ad Ad} result.
				 */
				@FieldTag int AD			= fieldTagOf(2, WIRE_LEN);

				/**
				 * Count of item user is rewarded with for viewing ad media.
				 * <p>If this is {@code 0} or omitted, user is not rewarded for ad media.
				 */
				@FieldTag int RWDITEMCOUNT	= fieldTagOf(3, WIRE_VARINT);

				/**
				 * Type of item user is rewarded with for viewing ad media.
				 */
				@FieldTag int RWDITEMTYPE	= fieldTagOf(4, WIRE_LEN);

				/**
				 * Scale factor to apply when generating preview image(s) of ad media.
				 * <p>If this is {@code 0}, preview image of ad media is not required.
				 */
				@FieldTag int PREVIEWSCALE	= fieldTagOf(5, WIRE_FIXED32);

				/**
				 * Price, in one-thousandths, buyer is willing to pay for ad execution.
				 */
				@FieldTag int PRICEMILLI	= fieldTagOf(6, WIRE_VARINT);

				/**
				 * ISO 4217 code of currency {@linkplain #PRICEMILLI price} is specified in.
				 */
				@FieldTag int PRICECUR		= fieldTagOf(7, WIRE_LEN);
			}

			/**
			 * Erroneous result item.
			 */
			interface ErrorItem {
				/**
				 * Error {@linkplain ErrorCode code}.
				 */
				@FieldTag int CODE	= fieldTagOf(1, WIRE_VARINT);

				/**
				 * Human-readable error message.
				 */
				@FieldTag int MSG	= fieldTagOf(2, WIRE_LEN);
			}

			/**
			 * Result item.
			 */
			interface Item {
				/**
				 * Identifier of placement item is for.
				 * <p>This may be omitted or an empty string, in which case the item is the result
				 * for all placements for which ads were requested but for which there are no
				 * result items.
				 */
				@FieldTag int PLCMTID	= fieldTagOf(1, WIRE_LEN);

				/**
				 * {@link ErrorItem Erroneous} result.
				 */
				@FieldTag int ERR		= fieldTagOf(2, WIRE_LEN);

				/**
				 * {@link AdItem Successful ad} result.
				 */
				@FieldTag int AD		= fieldTagOf(3, WIRE_LEN);
			}
		}
	}

	/**
	 * {@code report-ad-events} RPC constants.
	 */
	interface ReportAdEvents {
		/**
		 * Single ad event being reported.
		 */
		interface ReportAdEvent {

		}

		/**
		 * {@code report-ad-events} RPC argument constants.
		 * <p>{@snippet lang="protobuf" :
		 * message ReportAdEventsArguments {
		 *     repeated ReportAdEvent event	= 1;
		 * }
		 * }
		 */
		interface Arguments {
			/**
			 * Ad {@linkplain ReportAdEvent events} being reported.
			 */
			@FieldTag int EVENT		= fieldTagOf(1, WIRE_LEN);
		}
	}
}
