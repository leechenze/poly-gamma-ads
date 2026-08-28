// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.adcom.enums;

import androidx.annotation.IntDef;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Advertising tracker URL macro type enumeration value marker.
 *
 * @since 1.2
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE_USE)
@IntDef({
	AdComEnums.AdTrackerUrlMacroAdAssetUrl,
	AdComEnums.AdTrackerUrlMacroAdBreakDuration,
	AdComEnums.AdTrackerUrlMacroAdBreakPosition,
	AdComEnums.AdTrackerUrlMacroAdErrorCode,
	AdComEnums.AdTrackerUrlMacroAdEventTrackerErrorCode,
	AdComEnums.AdTrackerUrlMacroAdPlayhead,
	AdComEnums.AdTrackerUrlMacroAdServeId,
	AdComEnums.AdTrackerUrlMacroAdSize,
	AdComEnums.AdTrackerUrlMacroAdState,
	AdComEnums.AdTrackerUrlMacroAdType,
	AdComEnums.AdTrackerUrlMacroAppBundle,
	AdComEnums.AdTrackerUrlMacroAppStoreId,
	AdComEnums.AdTrackerUrlMacroAppStoreUrl,
	AdComEnums.AdTrackerUrlMacroCacheBust,
	AdComEnums.AdTrackerUrlMacroClickPosition,
	AdComEnums.AdTrackerUrlMacroClickType,
	AdComEnums.AdTrackerUrlMacroContentPlayhead,
	AdComEnums.AdTrackerUrlMacroDeviceIfa,
	AdComEnums.AdTrackerUrlMacroDeviceIfaType,
	AdComEnums.AdTrackerUrlMacroDeviceIp,
	AdComEnums.AdTrackerUrlMacroDeviceUa,
	AdComEnums.AdTrackerUrlMacroExecutedAdCount,
	AdComEnums.AdTrackerUrlMacroGdprConsent,
	AdComEnums.AdTrackerUrlMacroGeoLatLon,
	AdComEnums.AdTrackerUrlMacroGppSectionIds,
	AdComEnums.AdTrackerUrlMacroGppString,
	AdComEnums.AdTrackerUrlMacroLmt,
	AdComEnums.AdTrackerUrlMacroMediaPlayerCaps,
	AdComEnums.AdTrackerUrlMacroMediaPlayerState,
	AdComEnums.AdTrackerUrlMacroMediaPlayerUa,
	AdComEnums.AdTrackerUrlMacroOmidPartner,
	AdComEnums.AdTrackerUrlMacroPlacementType,
	AdComEnums.AdTrackerUrlMacroRegs,
	AdComEnums.AdTrackerUrlMacroServerSide,
	AdComEnums.AdTrackerUrlMacroServerUa,
	AdComEnums.AdTrackerUrlMacroSiteDomain,
	AdComEnums.AdTrackerUrlMacroSitePageUrl,
	AdComEnums.AdTrackerUrlMacroTimestamp,
	AdComEnums.AdTrackerUrlMacroTransactionId,
	AdComEnums.AdTrackerUrlMacroUniversalAdId,
	AdComEnums.AdTrackerUrlMacroUnknown
})
public @interface AdTrackerUrlMacroType {
}
