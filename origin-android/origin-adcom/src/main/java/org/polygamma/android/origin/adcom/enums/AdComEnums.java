// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.adcom.enums;

/**
 * AdCOM enumerations.
 *
 * @since 1.2
 * @see <a href="https://github.com/InteractiveAdvertisingBureau/AdCOM/blob/main/AdCOM%20v1.0%20FINAL.md#enumerations">AdCOM, version 1.0 - Enumerations</a>
 */
public interface AdComEnums {

	/**
	 * Ad media cannot be activated.
	 *
	 * @since 1.2
	 */
	@ActivationBehavior int ActivationNone				= 0;

	/**
	 * Ad media can be activated and behavior is unknown.
	 *
	 * @since 1.2
	 */
	@ActivationBehavior int ActivationUnknown			= 1;

	/**
	 * Activating ad media results in embedded browser being opened.
	 *
	 * @since 1.2
	 */
	@ActivationBehavior int ActivationEmbeddedBrowser	= 2;

	/**
	 * Activating ad media results in native browser being opened.
	 *
	 * @since 1.2
	 */
	@ActivationBehavior int ActivationNativeBrowser		= 3;

	/**
	 * Highest activation behavior discriminant.
	 *
	 * @since 1.2
	 */
	@ActivationBehavior int MAX_ACTIVATION_BEHAVIOR		= ActivationNativeBrowser;

	/**
	 * Unknown advertising API.
	 *
	 * @since 1.2
	 */
	@AdApiCode int AdApiUnknown		= 0;

	/**
	 * VPAID, version 1.0.
	 *
	 * @since 1.2
	 */
	@AdApiCode int AdApiVpaid10		= 1;

	/**
	 * VPAID, version 2.0.
	 *
	 * @since 1.2
	 */
	@AdApiCode int AdApiVpaid20		= 2;

	/**
	 * MRAID, version 1.0.
	 *
	 * @since 1.2
	 */
	@AdApiCode int AdApiMraid10		= 3;

	/**
	 * ORMMA.
	 *
	 * @since 1.2
	 */
	@AdApiCode int AdApiOrmma		= 4;

	/**
	 * MRAID, version 2.0.
	 *
	 * @since 1.2
	 */
	@AdApiCode int AdApiMraid20		= 5;

	/**
	 * MRAID, version 3.0.
	 *
	 * @since 1.2
	 */
	@AdApiCode int AdApiMraid30		= 6;

	/**
	 * OMID, version 1.0.
	 *
	 * @since 1.2
	 */
	@AdApiCode int AdApiOmid10		= 7;

	/**
	 * SIMID, version 1.0.
	 *
	 * @since 1.2
	 */
	@AdApiCode int AdApiSimid10		= 8;

	/**
	 * SIMID, version 1.1.
	 *
	 * @since 1.2
	 */
	@AdApiCode int AdApiSimid11		= 9;

	/**
	 * Highest advertising API code discriminant.
	 *
	 * @since 1.2
	 */
	@AdApiCode int MAX_AD_API_CODE	= AdApiSimid11;

	/**
	 * No ad error.
	 *
	 * @since 1.2
	 */
	@AdErrorCode int AdErrorNone						= 0;

	/**
	 * Unsupported ad type.
	 *
	 * @since 1.2
	 */
	@AdErrorCode int AdErrorUnsupportedType				= 200;

	/**
	 * Unsupported ad linearity.
	 *
	 * @since 1.2
	 */
	@AdErrorCode int AdErrorUnsupportedLinearity		= 201;

	/**
	 * Unsupported playback media duration.
	 *
	 * @since 1.2
	 */
	@AdErrorCode int AdErrorUnsupportedDuration			= 202;

	/**
	 * Unsupported media size.
	 *
	 * @since 1.2
	 */
	@AdErrorCode int AdErrorUnsupportedSize				= 203;

	/**
	 * Categories, describing ad media, missing.
	 *
	 * @since 1.2
	 */
	@AdErrorCode int AdErrorCatMissing					= 204;

	/**
	 * Ad media ignored.
	 *
	 * @since 1.2
	 */
	@AdErrorCode int AdErrorIgnored						= 206;

	/**
	 * Timeout while rendering ad media.
	 *
	 * @since 1.2
	 */
	@AdErrorCode int AdErrorRenderTimeout				= 304;

	/**
	 * General playback media error.
	 *
	 * @since 1.2
	 */
	@AdErrorCode int AdErrorPlayback					= 400;

	/**
	 * Playback media file not found.
	 *
	 * @since 1.2
	 */
	@AdErrorCode int AdErrorPlaybackMediaNotFound		= 401;

	/**
	 * Timeout while loading playback media file.
	 *
	 * @since 1.2
	 */
	@AdErrorCode int AdErrorPlaybackMediaTimeout		= 402;

	/**
	 * Unsupported playback media file.
	 *
	 * @since 1.2
	 */
	@AdErrorCode int AdErrorUnsupportedPlaybackMedia	= 403;

	/**
	 * Failed to play playback media file.
	 *
	 * @since 1.2
	 */
	@AdErrorCode int AdErrorPlaybackMediaError			= 405;

	/**
	 * Interactive asset of playback ad was not executed.
	 *
	 * @since 1.2
	 */
	@AdErrorCode int AdErrorPlaybackInteractiveIgnored	= 409;

	/**
	 * General playback overlay creative error.
	 *
	 * @since 1.2
	 */
	@AdErrorCode int AdErrorPlaybackOverlay				= 500;

	/**
	 * Playback overlay creative size does not fit placement.
	 *
	 * @since 1.2
	 */
	@AdErrorCode int AdErrorPlaybackOverlaySize			= 501;

	/**
	 * Failed to fetch playback overlay creative assets.
	 *
	 * @since 1.2
	 */
	@AdErrorCode int AdErrorPlaybackOverlayFetch		= 502;

	/**
	 * Unsupported playback overlay creative.
	 *
	 * @since 1.2
	 */
	@AdErrorCode int AdErrorUnsupportedPlaybackOverlay	= 503;

	/**
	 * General companion ad error.
	 *
	 * @since 1.2
	 */
	@AdErrorCode int AdErrorCompanion					= 600;

	/**
	 * Companion ad size does not fit placement.
	 *
	 * @since 1.2
	 */
	@AdErrorCode int AdErrorCompanionSize				= 601;

	/**
	 * Failed to render companion ad.
	 *
	 * @since 1.2
	 */
	@AdErrorCode int AdErrorCompanionRender				= 602;

	/**
	 * Failed to fetch companion ad assets.
	 *
	 * @since 1.2
	 */
	@AdErrorCode int AdErrorCompanionFetch				= 603;

	/**
	 * Companion ad is not supported.
	 *
	 * @since 1.2
	 */
	@AdErrorCode int AdErrorUnsupportedCompanion		= 604;

	/**
	 * Undefined advertising media error.
	 *
	 * @since 1.2
	 */
	@AdErrorCode int AdErrorUndefined					= 900;

	/**
	 * General playback creative interactive asset error.
	 *
	 * @since 1.2
	 */
	@AdErrorCode int AdErrorPlaybackInteractive			= 902;

	/**
	 * General display ad media error.
	 *
	 * @since 1.2
	 */
	@AdErrorCode int AdErrorDisplay						= 10000;

	/**
	 * Failed to fetch display ad media assets.
	 *
	 * @since 1.2
	 */
	@AdErrorCode int AdErrorDisplayFetch				= 10001;

	/**
	 * Failed to load display ad media.
	 *
	 * @since 1.2
	 */
	@AdErrorCode int AdErrorDisplayLoad					= 10002;

	/**
	 * Highest advertising error code discriminant.
	 *
	 * @since 1.2
	 */
	@AdErrorCode int MAX_AD_ERROR_CODE					= AdErrorDisplayLoad;

	/**
	 * No ad event tracker error.
	 *
	 * @since 1.2
	 */
	@AdEventTrackerErrorCode int AdEventTrackerErrorNone			= 0;

	/**
	 * Event tracker URL rejected.
	 *
	 * @since 1.2
	 */
	@AdEventTrackerErrorCode int AdEventTrackerErrorRejected		= 1;

	/**
	 * Event tracker type is not supported.
	 *
	 * @since 1.2
	 */
	@AdEventTrackerErrorCode int AdEventTrackerErrorUnsupported		= 2;

	/**
	 * Failed to load event tracker.
	 *
	 * @since 1.2
	 */
	@AdEventTrackerErrorCode int AdEventTrackerErrorLoad			= 3;

	/**
	 * Highest advertising event tracker error code discriminant.
	 *
	 * @since 1.2
	 */
	@AdEventTrackerErrorCode int MAX_AD_EVENT_TRACKER_ERROR_CODE = AdEventTrackerErrorLoad;

	/**
	 * Unknown advertising media event.
	 *
	 * @since 1.2
	 */
	@AdEventType int AdEventUnknown						= 0;

	/**
	 * Ad media has been loaded.
	 *
	 * @since 1.2
	 */
	@AdEventType int AdEventLoaded						= 1;

	/**
	 * Ad media has received an impression.
	 *
	 * @since 1.2
	 */
	@AdEventType int AdEventImpression					= 2;

	/**
	 * Ad media was 50% in view for 1 continuous second.
	 *
	 * @since 1.2
	 */
	@AdEventType int AdEventViewableMrc50				= 3;

	/**
	 * Ad media was 100% in view for 1 continuous second.
	 *
	 * @since 1.2
	 */
	@AdEventType int AdEventViewableMrc100				= 4;

	/**
	 * Ad media was 50% in view for 2 continuous seconds.
	 *
	 * @since 1.2
	 */
	@AdEventType int AdEventViewableVideo50				= 5;

	/**
	 * Error encountered while executing ad media.
	 *
	 * @since 1.2
	 */
	@AdEventType int AdEventError						= 500;

	/**
	 * Ad media was not selected for execution.
	 *
	 * @since 1.2
	 */
	@AdEventType int AdEventIgnored						= 501;

	/**
	 * Ad media received an impression but viewability was undetermined for the lifetime of its
	 * execution.
	 *
	 * @since 1.2
	 */
	@AdEventType int AdEventViewUndetermined			= 502;

	/**
	 * Ad media received an impression but was not viewable for the lifetime of its execution.
	 *
	 * @since 1.2
	 */
	@AdEventType int AdEventNotViewable					= 503;

	/**
	 * Non-navigable asset of ad media was activated (i.e. clicked).
	 *
	 * @since 1.2
	 */
	@AdEventType int AdEventActivated					= 504;

	/**
	 * Navigable asset of ad media was activated (i.e. clicked).
	 *
	 * @since 1.2
	 */
	@AdEventType int AdEventActivatedLink				= 505;

	/**
	 * Ad media was interacted with.
	 * <p>This event is fired when no other interaction event fits the user interaction.
	 *
	 * @since 1.2
	 */
	@AdEventType int AdEventInteracted					= 506;

	/**
	 * User activated ad skip button and ad was skipped.
	 *
	 * @since 1.2
	 */
	@AdEventType int AdEventSkipped						= 507;

	/**
	 * User activated mute button on media player, used to playback ad media, and creative was
	 * muted.
	 *
	 * @since 1.2
	 */
	@AdEventType int AdEventMediaPlayerMuted			= 508;

	/**
	 * User activated unmute button on media player, used to playback ad media, and creative was
	 * unmuted.
	 *
	 * @since 1.2
	 */
	@AdEventType int AdEventMediaPlayerUnmuted			= 509;

	/**
	 * User activated pause button on media player, used to playback ad media, and creative was
	 * paused.
	 *
	 * @since 1.2
	 */
	@AdEventType int AdEventMediaPlayerPaused			= 510;

	/**
	 * User activated unpause button on media player, used to playback ad media, and creative was
	 * unpaused.
	 *
	 * @since 1.2
	 */
	@AdEventType int AdEventMediaPlayerUnpaused			= 511;

	/**
	 * User activated rewind controls on media player, used to playback ad media, and creative
	 * playhead was moved to an earlier point.
	 *
	 * @since 1.2
	 */
	@AdEventType int AdEventMediaPlayerRewind			= 512;

	/**
	 * User activated expand controls on media player, used to playback ad media.
	 *
	 * @since 1.2
	 */
	@AdEventType int AdEventMediaPlayerExpand			= 513;

	/**
	 * User activated collapse controls on media player, used to playback ad media.
	 *
	 * @since 1.2
	 */
	@AdEventType int AdEventMediaPlayerCollapse			= 514;

	/**
	 * Playback ad media progress.
	 *
	 * @since 1.2
	 */
	@AdEventType int AdEventPlaybackProgress			= 515;

	/**
	 * User activated control to terminate ad media playback.
	 *
	 * @since 1.2
	 */
	@AdEventType int AdEventPlaybackTerminated			= 516;

	/**
	 * Interactive asset of playback ad was started.
	 *
	 * @since 1.2
	 */
	@AdEventType int AdEventPlaybackInteractiveStart	= 517;

	/**
	 * User activated control used to pause streaming content, which either expands the ad overlay
	 * within media player's viewable area or takes over streaming content area by launching an
	 * additional portion of the overlay creative.
	 *
	 * @since 1.2
	 */
	@AdEventType int AdEventPlaybackOverlayAccept		= 518;

	/**
	 * Time that initial ad is displayed.
	 *
	 * @since 1.2
	 */
	@AdEventType int AdEventPlaybackOverlayViewDur		= 519;

	/**
	 * User activated control to expand display or playback overlay ad.
	 *
	 * @since 1.2
	 */
	@AdEventType int AdEventExpanded					= 520;

	/**
	 * User activated control to collapse display or playback overlay ad.
	 *
	 * @since 1.2
	 */
	@AdEventType int AdEventCollapsed					= 521;

	/**
	 * User activated control to minimize display or playback overlay ad.
	 *
	 * @since 1.2
	 */
	@AdEventType int AdEventMinimized					= 522;

	/**
	 * User activated control to close display or playback overlay ad.
	 *
	 * @since 1.2
	 */
	@AdEventType int AdEventClosed						= 523;

	/**
	 * Highest advertising media event type discriminant.
	 *
	 * @since 1.2
	 */
	@AdEventType int MAX_AD_EVENT_TYPE					= AdEventClosed;

	/**
	 * Unknown advertising media event tracker type.
	 *
	 * @since 1.2
	 */
	@AdEventTrackerType int AdEventTrackerUnknown			= 0;

	/**
	 * Image pixel tracker.
	 *
	 * @since 1.2
	 */
	@AdEventTrackerType int AdEventTrackerPixel				= 1;

	/**
	 * JavaScript tracker.
	 *
	 * @since 1.2
	 */
	@AdEventTrackerType int AdEventTrackerJavaScript		= 2;

	/**
	 * JavaScript tracker, which may be executed within a restricted sandbox.
	 *
	 * @since 1.2
	 */
	@AdEventTrackerType int AdEventTrackerJavaScriptSandbox	= 500;

	/**
	 * Highest advertising media event tracker type discriminant.
	 *
	 * @since 1.2
	 */
	@AdEventTrackerType int MAX_AD_EVENT_TRACKER_TYPE		= AdEventTrackerJavaScriptSandbox;

	/**
	 * Both linear and non-linear.
	 *
	 * @since 1.2
	 */
	@AdLinearityMode int AdLinearityBoth		= 0;

	/**
	 * Linear ad.
	 *
	 * @since 1.2
	 */
	@AdLinearityMode int AdLinearityLinear		= 1;

	/**
	 * Non-linear (overlay) ad.
	 *
	 * @since 1.2
	 */
	@AdLinearityMode int AdLinearityNonlinear	= 2;

	/**
	 * Highest ad linearity mode discriminant.
	 *
	 * @since 1.2
	 */
	@AdLinearityMode int MAX_AD_LINEARITY_MODE	= AdLinearityNonlinear;

	/**
	 * Unknown resize type.
	 *
	 * @since 1.2
	 */
	@AdResizeType int AdResizeUnknown			= 0;

	/**
	 * Expand left-ward.
	 *
	 * @since 1.2
	 */
	@AdResizeType int AdResizeExpandLeft		= 1;

	/**
	 * Expand right-ward.
	 *
	 * @since 1.2
	 */
	@AdResizeType int AdResizeExpandRight		= 2;

	/**
	 * Expand upward.
	 *
	 * @since 1.2
	 */
	@AdResizeType int AdResizeExpandUp			= 3;

	/**
	 * Expand downward.
	 *
	 * @since 1.2
	 */
	@AdResizeType int AdResizeExpandDown		= 4;

	/**
	 * Expand to fullscreen.
	 *
	 * @since 1.2
	 */
	@AdResizeType int AdResizeExpandFullscreen	= 5;

	/**
	 * Resize down.
	 *
	 * @since 1.2
	 */
	@AdResizeType int AdResizeCollapse			= 6;

	/**
	 * Highest ad resize type discriminant.
	 *
	 * @since 1.2
	 */
	@AdResizeType int MAX_AD_RESIZE_TYPE		= AdResizeCollapse;

	/**
	 * Unknown tracker URL macro.
	 *
	 * @since 1.2
	 */
	@AdTrackerUrlMacroType int AdTrackerUrlMacroUnknown					=  0;

	/**
	 * Random 8-digit integer.
	 *
	 * @since 1.2
	 */
	@AdTrackerUrlMacroType int AdTrackerUrlMacroCacheBust				=  1;

	/**
	 * ISO 8601 formatted timestamp of when tracker was fired.
	 *
	 * @since 1.2
	 */
	@AdTrackerUrlMacroType int AdTrackerUrlMacroTimestamp				=  2;

	/**
	 * Limit ad tracking has been requested.
	 *
	 * @since 1.2
	 */
	@AdTrackerUrlMacroType int AdTrackerUrlMacroLmt						=  3;

	/**
	 * Comma delimited list of regulations applicable to device.
	 *
	 * @since 1.2
	 */
	@AdTrackerUrlMacroType int AdTrackerUrlMacroRegs					=  4;

	/**
	 * Base-64 encoded GDPR consent string.
	 *
	 * @since 1.2
	 */
	@AdTrackerUrlMacroType int AdTrackerUrlMacroGdprConsent				=  5;

	/**
	 * GPP consent string.
	 *
	 * @since 1.2
	 */
	@AdTrackerUrlMacroType int AdTrackerUrlMacroGppString				=  6;

	/**
	 * Comma delimited list of applicable GPP section identifiers.
	 *
	 * @since 1.2
	 */
	@AdTrackerUrlMacroType int AdTrackerUrlMacroGppSectionIds			=  7;

	/**
	 * App store assigned identifier of app for which tracker is fired.
	 *
	 * @since 1.2
	 */
	@AdTrackerUrlMacroType int AdTrackerUrlMacroAppStoreId				=  8;

	/**
	 * App store URL of app for which tracker is being fired.
	 *
	 * @since 1.2
	 */
	@AdTrackerUrlMacroType int AdTrackerUrlMacroAppStoreUrl				=  9;

	/**
	 * Bundle identifier of app for which tracker is being fired.
	 *
	 * @since 1.2
	 */
	@AdTrackerUrlMacroType int AdTrackerUrlMacroAppBundle				= 10;

	/**
	 * Top-level domain name of website for which tracker is being fired.
	 *
	 * @since 1.2
	 */
	@AdTrackerUrlMacroType int AdTrackerUrlMacroSiteDomain				= 11;

	/**
	 * URL of website page for which tracker is being fired.
	 *
	 * @since 1.2
	 */
	@AdTrackerUrlMacroType int AdTrackerUrlMacroSitePageUrl				= 12;

	/**
	 * Number of ads that have been executed, including current ad, by placement for an ad break.
	 *
	 * @since 1.2
	 */
	@AdTrackerUrlMacroType int AdTrackerUrlMacroExecutedAdCount			= 13;

	/**
	 * Type of ad tracker is being fired for.
	 *
	 * @since 1.2
	 */
	@AdTrackerUrlMacroType int AdTrackerUrlMacroAdType					= 14;

	/**
	 * Duration, in seconds, of ad break.
	 *
	 * @since 1.2
	 */
	@AdTrackerUrlMacroType int AdTrackerUrlMacroAdBreakDuration			= 15;

	/**
	 * Position of ad break within underlying content.
	 *
	 * @since 1.2
	 */
	@AdTrackerUrlMacroType int AdTrackerUrlMacroAdBreakPosition			= 16;

	/**
	 * Playhead position, formatted as {@code HH:MM:SS.mmm}, of underlying playback content.
	 *
	 * @since 1.2
	 */
	@AdTrackerUrlMacroType int AdTrackerUrlMacroContentPlayhead			= 17;

	/**
	 * Placement type.
	 *
	 * @since 1.2
	 */
	@AdTrackerUrlMacroType int AdTrackerUrlMacroPlacementType			= 18;

	/**
	 * Identifier of transaction from which ad was sourced.
	 *
	 * @since 1.2
	 */
	@AdTrackerUrlMacroType int AdTrackerUrlMacroTransactionId			= 19;

	/**
	 * Comma delimited list of universal ad identifiers.
	 *
	 * @since 1.2
	 */
	@AdTrackerUrlMacroType int AdTrackerUrlMacroUniversalAdId			= 20;

	/**
	 * User-agent string of media player used to play playback media.
	 *
	 * @since 1.2
	 */
	@AdTrackerUrlMacroType int AdTrackerUrlMacroMediaPlayerUa			= 21;

	/**
	 * IP address of device on which tracker is fired.
	 *
	 * @since 1.2
	 */
	@AdTrackerUrlMacroType int AdTrackerUrlMacroDeviceIp				= 22;

	/**
	 * User-agent string of device on which tracker is fired.
	 *
	 * @since 1.2
	 */
	@AdTrackerUrlMacroType int AdTrackerUrlMacroDeviceUa				= 23;

	/**
	 * Advertising sanctioned identifier of device on which tracker is fired.
	 *
	 * @since 1.2
	 */
	@AdTrackerUrlMacroType int AdTrackerUrlMacroDeviceIfa				= 24;

	/**
	 * Advertising sanctioned identifier type of device on which tracker is fired.
	 *
	 * @since 1.2
	 */
	@AdTrackerUrlMacroType int AdTrackerUrlMacroDeviceIfaType			= 25;

	/**
	 * Latitude and longitude, delimited by comma, of end user.
	 *
	 * @since 1.2
	 */
	@AdTrackerUrlMacroType int AdTrackerUrlMacroGeoLatLon				= 26;

	/**
	 * Tracker is fired on client or server -side.
	 *
	 * @since 1.2
	 */
	@AdTrackerUrlMacroType int AdTrackerUrlMacroServerSide				= 27;

	/**
	 * User-agent string of server on which tracker is fired.
	 *
	 * @since 1.2
	 */
	@AdTrackerUrlMacroType int AdTrackerUrlMacroServerUa				= 28;

	/**
	 * Supported clickthrough mode.
	 *
	 * @since 1.2
	 */
	@AdTrackerUrlMacroType int AdTrackerUrlMacroClickType				= 29;

	/**
	 * Supported OMID partner identifier.
	 *
	 * @since 1.2
	 */
	@AdTrackerUrlMacroType int AdTrackerUrlMacroOmidPartner				= 30;

	/**
	 * Comma delimited list of media player, used for playback ad media, capabilities.
	 *
	 * @since 1.2
	 */
	@AdTrackerUrlMacroType int AdTrackerUrlMacroMediaPlayerCaps			= 31;

	/**
	 * Playback ad media playhead position, formatted as {@code HH:MM:SS.mmm}.
	 *
	 * @since 1.2
	 */
	@AdTrackerUrlMacroType int AdTrackerUrlMacroAdPlayhead				= 32;

	/**
	 * Serving identifier of currently executing ad.
	 *
	 * @since 1.2
	 */
	@AdTrackerUrlMacroType int AdTrackerUrlMacroAdServeId				= 33;

	/**
	 * URL of ad asset currently being executed.
	 *
	 * @since 1.2
	 */
	@AdTrackerUrlMacroType int AdTrackerUrlMacroAdAssetUrl				= 34;

	/**
	 * Comma delimited list of ad states.
	 *
	 * @since 1.2
	 */
	@AdTrackerUrlMacroType int AdTrackerUrlMacroAdState					= 35;

	/**
	 * Width and height, delimited by comma, of currently executing ad.
	 *
	 * @since 1.2
	 */
	@AdTrackerUrlMacroType int AdTrackerUrlMacroAdSize					= 36;

	/**
	 * Comma delimited states of media player, rendering currently executing ad.
	 *
	 * @since 1.2
	 */
	@AdTrackerUrlMacroType int AdTrackerUrlMacroMediaPlayerState		= 37;

	/**
	 * Screen {@code x} and {@code y} coordinates, delimited by comma, of where ad was activated.
	 *
	 * @since 1.2
	 */
	@AdTrackerUrlMacroType int AdTrackerUrlMacroClickPosition			= 38;

	/**
	 * Code describing error encountered while executing ad.
	 *
	 * @since 1.2
	 */
	@AdTrackerUrlMacroType int AdTrackerUrlMacroAdErrorCode				= 39;

	/**
	 * Code describing error encountered while executing ad event tracker.
	 *
	 * @since 1.2
	 */
	@AdTrackerUrlMacroType int AdTrackerUrlMacroAdEventTrackerErrorCode	= 40;

	/**
	 * Highest advertising tracker URL macro type discriminant.
	 *
	 * @since 1.2
	 */
	@AdTrackerUrlMacroType int MAX_AD_TRACKER_URL_MACRO_TYPE =
		AdTrackerUrlMacroAdEventTrackerErrorCode;

	/**
	 * Unknown category taxonomy.
	 *
	 * @since 1.2
	 */
	@CategoryTaxonomyCode int CategoryTaxonomyUnknown			= 0;

	/**
	 * IAB Content Taxonomy, version 1.0.
	 *
	 * @since 1.2
	 */
	@CategoryTaxonomyCode int CategoryTaxonomyIabContent10		= 1;

	/**
	 * IAB Content Taxonomy, version 2.0.
	 *
	 * @since 1.2
	 */
	@CategoryTaxonomyCode int CategoryTaxonomyIabContent20		= 2;

	/**
	 * IAB Ad Product Taxonomy, version 1.0.
	 *
	 * @since 1.2
	 */
	@CategoryTaxonomyCode int CategoryTaxonomyIabAdProduct10	= 3;

	/**
	 * IAB Audience Taxonomy, version 1.1.
	 *
	 * @since 1.2
	 */
	@CategoryTaxonomyCode int CategoryTaxonomyIabAudience11		= 4;

	/**
	 * IAB Content Taxonomy, version 2.1.
	 *
	 * @since 1.2
	 */
	@CategoryTaxonomyCode int CategoryTaxonomyIabContent21		= 5;

	/**
	 * IAB Content Taxonomy, version 2.2.
	 *
	 * @since 1.2
	 */
	@CategoryTaxonomyCode int CategoryTaxonomyIabContent22		= 6;

	/**
	 * IAB Content Taxonomy, version 3.0.
	 *
	 * @since 1.2
	 */
	@CategoryTaxonomyCode int CategoryTaxonomyIabContent30		= 7;

	/**
	 * IAB Ad Product Taxonomy, version 2.0.
	 *
	 * @since 1.2
	 */
	@CategoryTaxonomyCode int CategoryTaxonomyIabAdProduct20	= 8;

	/**
	 * IAB Content Taxonomy, version 3.1.
	 *
	 * @since 1.2
	 */
	@CategoryTaxonomyCode int CategoryTaxonomyIabContent31		= 9;

	/**
	 * Highest category taxonomy code discriminant.
	 *
	 * @since 1.2
	 */
	@CategoryTaxonomyCode int MAX_CATEGORY_TAXONOMY_CODE		= CategoryTaxonomyIabContent31;

	/**
	 * No requirement on companion execution.
	 *
	 * @since 1.2
	 */
	@CompanionRequirementType int CompanionRequirementNone			= 0;

	/**
	 * All companion ad media must be executed.
	 *
	 * @since 1.2
	 */
	@CompanionRequirementType int CompanionRequirementAll			= 1;

	/**
	 * Any companion ad media must be executed.
	 *
	 * @since 1.2
	 */
	@CompanionRequirementType int CompanionRequirementAny			= 2;

	/**
	 * Highest companion requirement type discriminant.
	 *
	 * @since 1.2
	 */
	@CompanionRequirementType int MAX_COMPANION_REQUIREMENT_TYPE	= CompanionRequirementAny;

	/**
	 * Unknown connection type.
	 *
	 * @since 1.2
	 */
	@ConnectionType int ConnectionUnknown	= 0;

	/**
	 * Wired (i.e. ethernet) connection.
	 *
	 * @since 1.2
	 */
	@ConnectionType int ConnectionWired		= 1;

	/**
	 * WiFi connection.
	 *
	 * @since 1.2
	 */
	@ConnectionType int ConnectionWifi		= 2;

	/**
	 * Cellular, unknown generation, connection.
	 *
	 * @since 1.2
	 */
	@ConnectionType int ConnectionCell		= 3;

	/**
	 * Cellular, 2nd generation, connection.
	 *
	 * @since 1.2
	 */
	@ConnectionType int ConnectionCell2G	= 4;

	/**
	 * Cellular, 3rd generation, connection.
	 *
	 * @since 1.2
	 */
	@ConnectionType int ConnectionCell3G	= 5;

	/**
	 * Cellular, 4th generation, connection.
	 *
	 * @since 1.2
	 */
	@ConnectionType int ConnectionCell4G	= 6;

	/**
	 * Cellular, 5th generation, connection.
	 *
	 * @since 1.2
	 */
	@ConnectionType int ConnectionCell5G	= 7;

	/**
	 * VPN connection.
	 *
	 * @since 1.2
	 */
	@ConnectionType int ConnectionVpn		= 500;

	/**
	 * WiMAX connection.
	 *
	 * @since 1.2
	 */
	@ConnectionType int ConnectionWiMax		= 501;

	/**
	 * Bluetooth connection.
	 *
	 * @since 1.2
	 */
	@ConnectionType int ConnectionBluetooth	= 502;

	/**
	 * Highest connection type discriminant.
	 *
	 * @since 1.2
	 */
	@ConnectionType int MAX_CONNECTION_TYPE	= ConnectionBluetooth;

	/**
	 * Unknown device type.
	 *
	 * @since 1.2
	 */
	@DeviceType int DeviceUnknown	= 0;

	/**
	 * Mobile phone or tablet device.
	 *
	 * @since 1.2
	 */
	@DeviceType int DeviceMobile	= 1;

	/**
	 * Personal computer.
	 *
	 * @since 1.2
	 */
	@DeviceType int DevicePc		= 2;

	/**
	 * Connected TV.
	 *
	 * @since 1.2
	 */
	@DeviceType int DeviceCtv		= 3;

	/**
	 * Mobile phone.
	 *
	 * @since 1.2
	 */
	@DeviceType int DevicePhone		= 4;

	/**
	 * Tablet device.
	 *
	 * @since 1.2
	 */
	@DeviceType int DeviceTablet	= 5;

	/**
	 * Connected device.
	 *
	 * @since 1.2
	 */
	@DeviceType int DeviceCd		= 6;

	/**
	 * Set-top-box device.
	 *
	 * @since 1.2
	 */
	@DeviceType int DeviceStb		= 7;

	/**
	 * Out-of-home device.
	 *
	 * @since 1.2
	 */
	@DeviceType int DeviceOoh		= 8;

	/**
	 * Highest device type discriminant.
	 *
	 * @since 1.2
	 */
	@DeviceType int MAX_DEVICE_TYPE	= DeviceOoh;

	/**
	 * Unknown dimension measurement unit.
	 *
	 * @since 1.2
	 */
	@DimensionUnit int DimensionUnknown		= 0;

	/**
	 * Device independent pixels.
	 *
	 * @since 1.2
	 */
	@DimensionUnit int DimensionDp			= 1;

	/**
	 * Inches.
	 *
	 * @since 1.2
	 */
	@DimensionUnit int DimensionIn			= 2;

	/**
	 * Centimeters.
	 *
	 * @since 1.2
	 */
	@DimensionUnit int DimensionCm			= 3;

	/**
	 * Highest dimension measurement unit discriminant.
	 *
	 * @since 1.2
	 */
	@DimensionUnit int MAX_DIMENSION_UNIT	= DimensionCm;

	/**
	 * Unknown display creative type.
	 *
	 * @since 1.2
	 */
	@DisplayCreativeType int DisplayCreativeUnknown		= 0;

	/**
	 * HTML display creative.
	 *
	 * @since 1.2
	 */
	@DisplayCreativeType int DisplayCreativeHtml		= 1;

	/**
	 * AMP HTML display creative.
	 *
	 * @since 1.2
	 */
	@DisplayCreativeType int DisplayCreativeAmpHtml		= 2;

	/**
	 * Static image display creative.
	 *
	 * @since 1.2
	 */
	@DisplayCreativeType int DisplayCreativeImage		= 3;

	/**
	 * Native display creative.
	 *
	 * @since 1.2
	 */
	@DisplayCreativeType int DisplayCreativeNative		= 4;

	/**
	 * Highest display creative type discriminant.
	 *
	 * @since 1.2
	 */
	@DisplayCreativeType int MAX_DISPLAY_CREATIVE_TYPE	= DisplayCreativeNative;

	/**
	 * Unknown geolocation source.
	 *
	 * @since 1.2
	 */
	@GeoSourceType int GeoSourceUnknown		= 0;

	/**
	 * Geolocation sourced from device location services.
	 *
	 * @since 1.2
	 */
	@GeoSourceType int GeoSourceDevice		= 1;

	/**
	 * Geolocation sourced from IP address.
	 *
	 * @since 1.2
	 */
	@GeoSourceType int GeoSourceIp			= 2;

	/**
	 * Geolocation provided by user.
	 *
	 * @since 1.2
	 */
	@GeoSourceType int GeoSourceUser		= 3;

	/**
	 * Highest geolocation source type discriminant.
	 *
	 * @since 1.2
	 */
	@GeoSourceType int MAX_GEO_SOURCE_TYPE	= GeoSourceUser;

	/**
	 * Unknown native data asset type.
	 *
	 * @since 1.2
	 */
	@NativeDataAssetType int NativeDataAssetUnknown		=  0;

	/**
	 * Brand name of ad sponsor.
	 *
	 * @since 1.2
	 */
	@NativeDataAssetType int NativeDataAssetSponsored	=  1;

	/**
	 * Description of advertised product or service.
	 *
	 * @since 1.2
	 */
	@NativeDataAssetType int NativeDataAssetDesc		=  2;

	/**
	 * Numerical rating of advertised product or service.
	 *
	 * @since 1.2
	 */
	@NativeDataAssetType int NativeDataAssetRating		=  3;

	/**
	 * Social rating of advertised product or service.
	 *
	 * @since 1.2
	 */
	@NativeDataAssetType int NativeDataAssetLikes		=  4;

	/**
	 * Number of times advertised product has been downloaded or installed.
	 *
	 * @since 1.2
	 */
	@NativeDataAssetType int NativeDataAssetDownloads	=  5;

	/**
	 * Price, in localized format with currency symbol, of advertised product or service.
	 *
	 * @since 1.2
	 */
	@NativeDataAssetType int NativeDataAssetPrice		=  6;

	/**
	 * Sale price, in localized format with currency symbol, of advertised product or service.
	 *
	 * @since 1.2
	 */
	@NativeDataAssetType int NativeDataAssetSalePrice	=  7;

	/**
	 * Localized phone number.
	 *
	 * @since 1.2
	 */
	@NativeDataAssetType int NativeDataAssetPhone		=  8;

	/**
	 * Localized physical address.
	 *
	 * @since 1.2
	 */
	@NativeDataAssetType int NativeDataAssetAddress		=  9;

	/**
	 * Additional description of advertised product or service.
	 *
	 * @since 1.2
	 */
	@NativeDataAssetType int NativeDataAssetDesc2		= 10;

	/**
	 * Displayable URL of product or service.
	 *
	 * @since 1.2
	 */
	@NativeDataAssetType int NativeDataAssetDisplayUrl	= 11;

	/**
	 * Call-to-action text.
	 *
	 * @since 1.2
	 */
	@NativeDataAssetType int NativeDataAssetCtaText		= 12;

	/**
	 * Highest native data asset type discriminant.
	 *
	 * @since 1.2
	 */
	@NativeDataAssetType int MAX_NATIVE_DATA_ASSET_TYPE	= NativeDataAssetCtaText;

	/**
	 * Unknown native image asset type.
	 *
	 * @since 1.2
	 */
	@NativeImageAssetType int NativeImageAssetUnknown		= 0;

	/**
	 * Icon image asset type.
	 *
	 * @since 1.2
	 */
	@NativeImageAssetType int NativeImageAssetIcon			= 1;

	/**
	 * Main image asset type.
	 *
	 * @since 1.2
	 */
	@NativeImageAssetType int NativeImageAssetMain			= 3;

	/**
	 * Highest native image asset type discriminant.
	 *
	 * @since 1.2
	 */
	@NativeImageAssetType int MAX_NATIVE_IMAGE_ASSET_TYPE	= NativeImageAssetMain;

	/**
	 * Unknown operating-system.
	 *
	 * @since 1.2
	 */
	@OsCode int OsUnknown			= 0;

	/**
	 * Google's Android operating-system.
	 *
	 * @since 1.2
	 */
	@OsCode int OsGoogleAndroid		= 2;

	/**
	 * Google's ChromeOS operating-system.
	 *
	 * @since 1.2
	 */
	@OsCode int OsGoogleChromeOs	= 8;

	/**
	 * Amazon's FireOS operating-system.
	 *
	 * @since 1.2
	 */
	@OsCode int OsAmazonFireOs		= 10;

	/**
	 * Highest operating system code discriminant.
	 *
	 * @since 1.2
	 */
	@OsCode int MAX_OS_CODE			= OsAmazonFireOs;

	/**
	 * Unknown playback creative type.
	 *
	 * @since 1.2
	 */
	@PlaybackCreativeType int PlaybackCreativeUnknown			=  0;

	/**
	 * VAST, version 1.0.
	 *
	 * @since 1.2
	 */
	@PlaybackCreativeType int PlaybackCreativeVast10			=  1;

	/**
	 * VAST, version 2.0.
	 *
	 * @since 1.2
	 */
	@PlaybackCreativeType int PlaybackCreativeVast20			=  2;

	/**
	 * VAST, version 3.0.
	 *
	 * @since 1.2
	 */
	@PlaybackCreativeType int PlaybackCreativeVast30			=  3;

	/**
	 * VAST, version 1.0, with wrapper support.
	 *
	 * @since 1.2
	 */
	@PlaybackCreativeType int PlaybackCreativeVast10Wrapper		=  4;

	/**
	 * VAST, version 2.0, with wrapper support.
	 *
	 * @since 1.2
	 */
	@PlaybackCreativeType int PlaybackCreativeVast20Wrapper		=  5;

	/**
	 * VAST, version 3.0, with wrapper support.
	 *
	 * @since 1.2
	 */
	@PlaybackCreativeType int PlaybackCreativeVast30Wrapper		=  6;

	/**
	 * VAST, version 4.0.
	 *
	 * @since 1.2
	 */
	@PlaybackCreativeType int PlaybackCreativeVast40			=  7;

	/**
	 * VAST, version 4.0, with wrapper support.
	 *
	 * @since 1.2
	 */
	@PlaybackCreativeType int PlaybackCreativeVast40Wrapper		=  8;

	/**
	 * DAAST, version 1.0.
	 *
	 * @since 1.2
	 */
	@PlaybackCreativeType int PlaybackCreativeDaast10			=  9;

	/**
	 * DAAST, version 1.0, with wrapper support.
	 *
	 * @since 1.2
	 */
	@PlaybackCreativeType int PlaybackCreativeDaast10Wrapper	= 10;

	/**
	 * VAST, version 4.1.
	 *
	 * @since 1.2
	 */
	@PlaybackCreativeType int PlaybackCreativeVast41			= 11;

	/**
	 * VAST, version 4.1, with wrapper support.
	 *
	 * @since 1.2
	 */
	@PlaybackCreativeType int PlaybackCreativeVast41Wrapper		= 12;

	/**
	 * VAST, version 4.2.
	 *
	 * @since 1.2
	 */
	@PlaybackCreativeType int PlaybackCreativeVast42			= 13;

	/**
	 * VAST, version 4.2, with wrapper support.
	 *
	 * @since 1.2
	 */
	@PlaybackCreativeType int PlaybackCreativeVast42Wrapper		= 14;

	/**
	 * VAST, version 4.3.
	 *
	 * @since 1.2
	 */
	@PlaybackCreativeType int PlaybackCreativeVast43			= 15;

	/**
	 * VAST, version 4.3, with wrapper support.
	 *
	 * @since 1.2
	 */
	@PlaybackCreativeType int PlaybackCreativeVast43Wrapper		= 16;

	/**
	 * Structured playback creative.
	 *
	 * @since 1.2
	 */
	@PlaybackCreativeType int PlaybackCreativeStructured		= 500;

	/**
	 * Highest playback creative type discriminant.
	 *
	 * @since 1.2
	 */
	@PlaybackCreativeType int MAX_PLAYBACK_CREATIVE_TYPE		= PlaybackCreativeStructured;

	/**
	 * Unknown playback content delivery method.
	 *
	 * @since 1.2
	 */
	@PlaybackDeliveryMethod int PlaybackDeliveryUnknown			= 0;

	/**
	 * Streaming playback delivery.
	 *
	 * @since 1.2
	 */
	@PlaybackDeliveryMethod int PlaybackDeliveryStreaming		= 1;

	/**
	 * Progressive playback delivery.
	 *
	 * @since 1.2
	 */
	@PlaybackDeliveryMethod int PlaybackDeliveryProgressive		= 2;

	/**
	 * Download playback delivery.
	 *
	 * @since 1.2
	 */
	@PlaybackDeliveryMethod int PlaybackDeliveryDownload		= 3;

	/**
	 * Highest playback delivery method discriminant.
	 *
	 * @since 1.2
	 */
	@PlaybackDeliveryMethod int MAX_PLAYBACK_DELIVERY_METHOD	= PlaybackDeliveryDownload;
}
