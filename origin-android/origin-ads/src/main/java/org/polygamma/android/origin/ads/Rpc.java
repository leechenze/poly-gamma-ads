// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.ads;

import static org.polygamma.android.origin.protobuf.ProtobufField.*;

import androidx.annotation.IntDef;

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
			@Tag int COOKIE			= ofBytes(  1);

			/**
			 * Antifraud {@linkplain
			 * org.polygamma.android.origin.antifraud.AntifraudStatus#digest() digest}.
			 */
			@Tag int IVTDIGEST		= ofString( 2);

			/**
			 * AdCOM model {@linkplain org.polygamma.android.origin.adcom.AdCom#DOMAIN_VERSION
			 * version}.
			 */
			@Tag int ADCOMVER		= ofString( 3);

			/**
			 * {@linkplain org.polygamma.android.origin.core.Origin#app() Distribution channel}
			 * ad media is requested for.
			 */
			@Tag int CHANNEL		= ofMessage(4);

			/**
			 * {@linkplain org.polygamma.android.origin.core.DeviceModule#device() Device} on
			 * which ad media will be executed.
			 */
			@Tag int DEVICE			= ofMessage(5);

			/**
			 * {@linkplain org.polygamma.android.origin.core.RegulationsModule#regs() Regulations}
			 * applicable to device.
			 */
			@Tag int REGS			= ofMessage(6);

			/**
			 * {@linkplain org.polygamma.android.origin.adcom.placement.Placement Placements} for
			 * which ad media is being requested.
			 */
			@Tag int PLCMT			= ofMessage(7);
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
			@Tag int COOKIE		= ofBytes(  1);

			/**
			 * Result items.
			 */
			@Tag int ITEM		= ofMessage(2);

			/**
			 * Ad result item.
			 */
			interface AdItem {
				/**
				 * Item identifier.
				 */
				@Tag int ID				= ofString( 1);

				/**
				 * {@linkplain org.polygamma.android.origin.adcom.media.Ad Ad} result.
				 */
				@Tag int AD				= ofMessage(2);

				/**
				 * Count of item user is rewarded with for viewing ad media.
				 * <p>If this is {@code 0} or omitted, user is not rewarded for ad media.
				 */
				@Tag int RWDITEMCOUNT	= ofInt64(  3);

				/**
				 * Type of item user is rewarded with for viewing ad media.
				 */
				@Tag int RWDITEMTYPE	= ofString( 4);

				/**
				 * Scale factor to apply when generating preview image(s) of ad media.
				 * <p>If this is {@code 0}, preview image of ad media is not required.
				 */
				@Tag int PREVIEWSCALE	= ofFloat(  5);

				/**
				 * Price, in one-thousandths, buyer is willing to pay for ad execution.
				 */
				@Tag int PRICEMILLI		= ofInt64(  6);

				/**
				 * ISO 4217 code of currency {@linkplain #PRICEMILLI price} is specified in.
				 */
				@Tag int PRICECUR		= ofString( 7);
			}

			/**
			 * Erroneous result item.
			 */
			interface ErrorItem {
				/**
				 * Error {@linkplain ErrorCode code}.
				 */
				@Tag int CODE	= ofInt32( 1);

				/**
				 * Human-readable error message.
				 */
				@Tag int MSG	= ofString(2);
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
				@Tag int PLCMTID	= ofString( 1);

				/**
				 * {@link ErrorItem Erroneous} result.
				 */
				@Tag int ERR		= ofMessage(2);

				/**
				 * {@link AdItem Successful ad} result.
				 */
				@Tag int AD			= ofMessage(3);
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
			@Tag int EVENT		= ofMessage(1);
		}
	}
}
