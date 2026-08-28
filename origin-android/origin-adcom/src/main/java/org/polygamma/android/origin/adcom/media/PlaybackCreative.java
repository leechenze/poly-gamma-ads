// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.adcom.media;

import static org.polygamma.android.origin.protobuf.ProtobufField.*;

import android.util.ArrayMap;
import android.util.Pair;

import androidx.annotation.IntRange;
import androidx.annotation.ReturnThis;

import org.polygamma.android.origin.adcom.enums.AdComEnums;
import org.polygamma.android.origin.adcom.enums.CompanionRequirementType;
import org.polygamma.android.origin.protobuf.ProtobufReader;
import org.polygamma.android.origin.protobuf.ProtobufSerializable;
import org.polygamma.android.origin.protobuf.ProtobufWriter;
import org.polygamma.android.origin.util.CollectionsCompat;
import org.polygamma.android.origin.util.Preconditions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Linear or overlay playback creative media.
 *
 * @since 1.2
 */
public class PlaybackCreative implements ProtobufSerializable {

	private static final @Tag int ID				= ofString(    1);
	private static final @Tag int SEQ				= ofInt32(     2);
	private static final @Tag int LINEAR			= ofMessage(   3);
	private static final @Tag int OVERLAY			= ofMessage(   4);
	private static final @Tag int COMPREQ			= ofInt32(     5);
	private static final @Tag int COMP				= ofMessage(   6);

	// `LinearCreative`
	private static final @Tag int LINEAR_LINK		= ofMessage(   1);
	private static final @Tag int LINEAR_DUR		= ofInt64(     2);
	private static final @Tag int LINEAR_SKIPOFF	= ofInt64(     3);
	private static final @Tag int LINEAR_ASSET		= ofMessage(   4);
	private static final @Tag int LINEAR_ICON		= ofMessage(   5);
	private static final @Tag int LINEAR_EVENT		= ofMessage(   6);
	private static final @Tag int LINEAR_UNIVID		= ofStringPair(7);

	/**
	 * Creative media is overlay.
	 */
	private static final int FLAG_OVERLAY	= 1 << 31;

	private static final PlaybackCreative DEFAULT_LINEAR	= new PlaybackCreative();
	private static final PlaybackCreative DEFAULT_OVERLAY	=
		new PlaybackCreative(DEFAULT_LINEAR);

	static {
		DEFAULT_OVERLAY.sequenceAndFlags |= FLAG_OVERLAY;
		DEFAULT_OVERLAY.data = DisplayAd.ofDisplayAd();
	}

	/**
	 * Playback creative {@linkplain PlaybackCreative media} builder.
	 *
	 * @since 1.2
	 * @see #ofLinearBuilder()
	 * @see #ofOverlayBuilder()
	 */
	public static final class Builder {

		private PlaybackCreative creative;
		private boolean needClone;

		private Builder(PlaybackCreative creative) {
			this.creative = creative;
			this.needClone = true;
		}

		private PlaybackCreative target() {
			if (this.needClone) {
				this.creative = new PlaybackCreative(this.creative);
				this.needClone = false;
			}
			return this.creative;
		}

		private PlaybackCreative targetOverlay() {
			PlaybackCreative rv = this.target();

			rv.checkOverlay();
			return rv;
		}

		private PlaybackCreative targetLinear() {
			PlaybackCreative rv = this.target();

			rv.checkLinear();
			return rv;
		}

		/**
		 * Set creative identifier, unique to ad server.
		 *
		 * @param id creative identifier
		 * @return {@code this}
		 * @since 1.2
		 * @see PlaybackCreative#id()
		 */
		@ReturnThis
		public Builder id(String id) {
			this.target().id = id;
			return this;
		}

		/**
		 * Set sequence at which creative must be executed.
		 *
		 * @param seq sequence number
		 * @return {@code this}
		 * @throws IllegalArgumentException {@code seq} is negative
		 * @since 1.2
		 * @see PlaybackCreative#id()
		 */
		@ReturnThis
		public Builder sequence(@IntRange(from = 0) int seq) {
			Preconditions.checkArgument(seq >= 0);

			PlaybackCreative dst = this.target();

			dst.sequenceAndFlags = (dst.sequenceAndFlags & FLAG_OVERLAY) | seq;
			return this;
		}

		/**
		 * Set companion media execution requirement.
		 *
		 * @param req comanion execution requirement
		 * @return {@code this}
		 * @since 1.2
		 * @see PlaybackCreative#companionRequirement()
		 */
		@ReturnThis
		public Builder companionRequirement(@CompanionRequirementType int req) {
			this.target().companionRequirement = req;
			return this;
		}

		/**
		 * Set companion media to be executed alongside creative.
		 *
		 * @param comp companions media
		 * @return {@code this}
		 * @since 1.2
		 * @see PlaybackCreative#companion(int)
		 */
		@ReturnThis
		public Builder companions(Collection<CompanionAd> comp) {
			this.target().companions =
				CollectionsCompat.toArrayOrEmpty(comp, DEFAULT_LINEAR.companions);
			return this;
		}

		/**
		 * Set {@linkplain PlaybackCreative#isOverlay() overlay} display media.
		 *
		 * @param display overlay display media
		 * @return {@code this}
		 * @throws IllegalStateException builder is not building {@linkplain
		 * PlaybackCreative#ofOverlayBuilder() overlay} media
		 * @since 1.2
		 * @see PlaybackCreative#overlayDisplay()
		 */
		@ReturnThis
		public Builder overlayDisplay(DisplayAd display) {
			this.targetOverlay().configureOverlay(display);
			return this;
		}

		/**
		 * Set universal ad identifier registry to identifier value mapping for {@linkplain
		 * PlaybackCreative#isLinear() linear} creative media.
		 *
		 * @param ids mapping of identifier registry to identifier value
		 * @return {@code this}
		 * @throws IllegalStateException builder is not building {@linkplain
		 * PlaybackCreative#ofLinearBuilder() linear} media
		 * @since 1.2
		 * @see PlaybackCreative#universalAdIdRegistry(int)
		 * @see PlaybackCreative#universalAdIdValue(int)
		 */
		@ReturnThis
		public Builder linearUniversalAdIds(Map<String, String> ids) {
			this.targetLinear().universalAdIds =
				ids.isEmpty() ? DEFAULT_LINEAR.universalAdIds :
				CollectionsCompat.arrayMapCopyOf(ids);
			return this;
		}

		/**
		 * Set link to which user is navigated to when {@linkplain PlaybackCreative#isLinear()
		 * linear} creative is activated (i.e. clicked).
		 *
		 * @param link navigation link
		 * @return {@code this}
		 * @throws IllegalStateException builder is not building {@linkplain
		 * PlaybackCreative#ofLinearBuilder() linear} media
		 * @since 1.2
		 * @see PlaybackCreative#link()
		 */
		@ReturnThis
		public Builder linearLink(LinkAsset link) {
			this.targetLinear().link = link;
			return this;
		}

		/**
		 * Set total {@linkplain PlaybackCreative#isLinear() linear} media playback duration, in
		 * seconds.
		 *
		 * @param secs playback duration
		 * @return {@code this}
		 * @throws IllegalStateException builder is not building {@linkplain
		 * PlaybackCreative#ofLinearBuilder() linear} media
		 * @since 1.2
		 * @see PlaybackCreative#linearPlaybackDurationSeconds()
		 */
		@ReturnThis
		public Builder linearPlaybackDurationSeconds(long secs) {
			this.targetLinear().linearPlaybackDurationSeconds = secs;
			return this;
		}

		/**
		 * Set {@linkplain PlaybackCreative#isLinear() linear} media playback duration, in seconds,
		 * after which skip controls may be displayed.
		 *
		 * @param secs playback duration after which skip controls should be displayed
		 * @return {@code this}
		 * @throws IllegalStateException builder is not building {@linkplain
		 * PlaybackCreative#ofLinearBuilder() linear} media
		 * @since 1.2
		 * @see PlaybackCreative#linearSkipOffsetSeconds()
		 */
		@ReturnThis
		public Builder linearSkipOffsetSeconds(long secs) {
			this.targetLinear().linearSkipOffsetSeconds = secs;
			return this;
		}

		/**
		 * Set {@linkplain PlaybackCreative#isLinear() linear} media assets.
		 *
		 * @param assets media assets
		 * @return {@code this}
		 * @throws IllegalStateException builder is not building {@linkplain
		 * PlaybackCreative#ofLinearBuilder() linear} media
		 * @since 1.2
		 * @see PlaybackCreative#linearAsset(int)
		 */
		@ReturnThis
		public Builder linearAssets(Collection<LinearAsset> assets) {
			this.targetLinear().data =
				CollectionsCompat.toArrayOrEmpty(assets, DEFAULT_LINEAR.linearAssets());
			return this;
		}

		/**
		 * Set {@linkplain PlaybackCreative#isLinear() linear} media event trackers.
		 *
		 * @param trkr event trackers
		 * @return {@code this}
		 * @throws IllegalStateException builder is not building {@linkplain
		 * PlaybackCreative#ofLinearBuilder() linear} media
		 * @since 1.2
		 * @see PlaybackCreative#eventTracker(int)
		 */
		@ReturnThis
		public Builder linearEventTrackers(Collection<AdEventTracker> trkr) {
			this.targetLinear().eventTrackers =
				CollectionsCompat.toArrayOrEmpty(trkr, DEFAULT_LINEAR.eventTrackers);
			return this;
		}

		/**
		 * Set {@linkplain PlaybackCreative#isLinear() linear} media icon overlay assets.
		 *
		 * @param icons icon overlay assets
		 * @return {@code this}
		 * @throws IllegalStateException builder is not building {@linkplain
		 * PlaybackCreative#ofLinearBuilder() linear} media
		 * @since 1.2
		 * @see PlaybackCreative#linearIcon(int)
		 */
		@ReturnThis
		public Builder linearIcons(Collection<IconAsset> icons) {
			this.targetLinear().icons =
				CollectionsCompat.toArrayOrEmpty(icons, DEFAULT_LINEAR.icons);
			return this;
		}

		/**
		 * Build resulting creative.
		 *
		 * @return creative instance
		 * @since 1.2
		 */
		public PlaybackCreative build() {
			this.needClone = true;
			return this.creative;
		}
	}

	/**
	 * Default empty {@linkplain #isLinear() linear} creative instance.
	 *
	 * @return empty linear creative instance
	 * @since 1.2
	 * @see #isLinear()
	 */
	public static PlaybackCreative ofLinear() {
		return DEFAULT_LINEAR;
	}

	/**
	 * Default empty {@linkplain #isOverlay() overlay} creative instance.
	 *
	 * @return empty overlay creative instance
	 * @since 1.2
	 * @see #isOverlay()
	 */
	public static PlaybackCreative ofOverlay() {
		return DEFAULT_OVERLAY;
	}

	/**
	 * Construct new empty {@linkplain #isLinear() linear} creative {@linkplain Builder builder}.
	 *
	 * @return empty builder instance
	 * @since 1.2
	 * @see #isLinear()
	 */
	public static Builder ofLinearBuilder() {
		return DEFAULT_LINEAR.toBuilder();
	}

	/**
	 * Construct new empty {@linkplain #isOverlay() overlay} creative {@linkplain Builder builder}.
	 *
	 * @return empty builder instance
	 * @since 1.2
	 * @see #isOverlay()
	 */
	public static Builder ofOverlayBuilder() {
		return DEFAULT_OVERLAY.toBuilder();
	}

	private static void
	deserializeLinearCreativeProtobuf(PlaybackCreative dst, ProtobufReader src) {
		List<IconAsset> icons = new ArrayList<>(0);
		List<LinearAsset> assets = new ArrayList<>(0);
		List<AdEventTracker> trkr = new ArrayList<>(0);
		ArrayMap<String, String> univIds = new ArrayMap<>(0);

		dst.configureLinear();
		while (src.hasRemaining()) {
			int tag = src.readTag();

			if (tag == LINEAR_LINK) {
				dst.link = src.readLen(LinkAsset::ofProtobuf);
			} else if (tag == LINEAR_DUR) {
				dst.linearPlaybackDurationSeconds = src.readInt64();
			} else if (tag == LINEAR_SKIPOFF) {
				dst.linearSkipOffsetSeconds = src.readInt64();
			} else if (tag == LINEAR_ASSET) {
				assets.add(src.readLen(LinearAsset::ofProtobuf));
			} else if (tag == LINEAR_ICON) {
				icons.add(src.readLen(IconAsset::ofProtobuf));
			} else if (tag == LINEAR_EVENT) {
				trkr.add(src.readLen(AdEventTracker::ofProtobuf));
			} else if (tag == LINEAR_UNIVID) {
				Pair<String, String> univId = src.readStringPair();

				univIds.put(univId.first, univId.second);
			}
		}
		dst.icons = CollectionsCompat.toArrayOrEmpty(icons, DEFAULT_LINEAR.icons);
		dst.data = CollectionsCompat.toArrayOrEmpty(assets, DEFAULT_LINEAR.linearAssets());
		dst.eventTrackers = CollectionsCompat.toArrayOrEmpty(trkr, DEFAULT_LINEAR.eventTrackers);
		dst.universalAdIds = univIds.isEmpty() ? DEFAULT_LINEAR.universalAdIds : univIds;
	}

	/**
	 * Deserialize {@linkplain #isLinear() linear} or {@linkplain #isOverlay() overlay} creative
	 * from Protobuf message.
	 *
	 * @param reader reader to deserialize from
	 * @return deserialized creative
	 * @throws RuntimeException coding is malformed
	 * @since 1.2
	 */
	public static PlaybackCreative ofProtobuf(ProtobufReader reader) {
		PlaybackCreative rv = new PlaybackCreative(DEFAULT_LINEAR);
		List<CompanionAd> comp = new ArrayList<>(0);

		while (reader.hasRemaining()) {
			int tag = reader.readTag();

			if (tag == ID) {
				rv.id = reader.readString();
			} else if (tag == SEQ) {
				rv.sequenceAndFlags =
					(rv.sequenceAndFlags & FLAG_OVERLAY) |
					Math.max(0, reader.readInt32());
			} else if (tag == LINEAR) {
				int cookie = reader.beginReadLen();

				deserializeLinearCreativeProtobuf(rv, reader);
				reader.endReadLen(cookie);
			} else if (tag == OVERLAY) {
				int cookie = reader.beginReadLen();

				rv.configureOverlay(DisplayAd.ofDisplayAdProtobuf(reader));
				reader.endReadLen(cookie);
			} else if (tag == COMPREQ) {
				rv.companionRequirement = reader.readInt32();
			} else if (tag == COMP) {
				int cookie = reader.beginReadLen();

				comp.add(CompanionAd.ofProtobuf(reader));
				reader.endReadLen(cookie);
			}
		}
		rv.companions = CollectionsCompat.toArrayOrEmpty(comp, DEFAULT_LINEAR.companions);
		return rv;
	}

	private String id;
	private ArrayMap<String, String> universalAdIds;
	private int sequenceAndFlags;
	private @CompanionRequirementType int companionRequirement;
	private CompanionAd[] companions;

	private LinkAsset link;
	private long linearPlaybackDurationSeconds;
	private long linearSkipOffsetSeconds;
	private Object data;
	private AdEventTracker[] eventTrackers;
	private IconAsset[] icons;

	private PlaybackCreative() {
		this.id = "";
		this.universalAdIds = DisplayAd.ofDisplayAd().universalAdIds();
		this.companionRequirement = AdComEnums.CompanionRequirementNone;
		this.companions = new CompanionAd[0];
		this.link = LinkAsset.of();
		this.data = new LinearAsset[0];
		this.eventTrackers = DisplayAd.ofDisplayAd().eventTrackers();
		this.icons = new IconAsset[0];
	}

	private PlaybackCreative(PlaybackCreative that) {
		this.id = that.id;
		this.universalAdIds = that.universalAdIds;
		this.sequenceAndFlags = that.sequenceAndFlags;
		this.companionRequirement = that.companionRequirement;
		this.companions = that.companions;
		this.link = that.link;
		this.linearPlaybackDurationSeconds = that.linearPlaybackDurationSeconds;
		this.linearSkipOffsetSeconds = that.linearSkipOffsetSeconds;
		this.data = that.data;
		this.eventTrackers = that.eventTrackers;
		this.icons = that.icons;
	}

	/**
	 * Configure {@code this} as an overlay creative.
	 *
	 * @param display underlying display overlay media
	 */
	private void configureOverlay(DisplayAd display) {
		this.sequenceAndFlags |= FLAG_OVERLAY;
		this.data = display;
		this.universalAdIds = display.universalAdIds();
		this.link =
			display.creativeType() == AdComEnums.DisplayCreativeImage ||
			display.creativeType() == AdComEnums.DisplayCreativeNative ? display.link() :
			LinkAsset.of();
		this.eventTrackers = display.eventTrackers();
		this.icons = DEFAULT_OVERLAY.icons;
	}

	/**
	 * Configure creative as a linear creative.
	 */
	private void configureLinear() {
		this.sequenceAndFlags &= ~FLAG_OVERLAY;
		this.data = DEFAULT_LINEAR.data;
		this.universalAdIds = DEFAULT_LINEAR.universalAdIds;
		this.link = DEFAULT_LINEAR.link;
		this.eventTrackers = DEFAULT_LINEAR.eventTrackers;
		this.icons = DEFAULT_LINEAR.icons;
	}

	/**
	 * Creative identifier, unique to ad server.
	 *
	 * @return identifier
	 * @since 1.2
	 * @see Builder#id(String)
	 */
	public String id() {
		return this.id;
	}

	/**
	 * Universal ad identifier count.
	 * <p>If {@code this} is {@linkplain #isOverlay() overlay}, the value returned is equal
	 * to:
	 * {@snippet lang="java" :
	 * this.overlayDisplay() // @link substring="overlayDisplay" target="#overlayDisplay()"
	 *     .universalAdIdCount(); // @link substring="universalAdIdCount" target="DisplayAd#universalAdIdCount()"
	 * }
	 *
	 * @return universal ad id count
	 * @since 1.2
	 * @see Builder#linearUniversalAdIds(Map)
	 * @see #universalAdIdRegistry(int)
	 * @see #universalAdIdValue(int)
	 */
	@SuppressWarnings("JavadocDeclaration")
	public int universalAdIdCount() {
		return this.universalAdIds.size();
	}

	/**
	 * Top-level domain name of universal ad identifier registry, at index.
	 * <p>If {@code this} is {@linkplain #isOverlay() overlay}, the value returned is equal
	 * to:
	 * {@snippet lang="java" :
	 * this.overlayDisplay() // @link substring="overlayDisplay" target="#overlayDisplay()"
	 *     .universalAdIdRegistry(i); // @link substring="universalAdIdRegistry" target="DisplayAd#universalAdIdRegistry(int)"
	 * }
	 *
	 * @param i index to retrieve registry domain at
	 * @return registry domain at index {@code i}
	 * @throws IndexOutOfBoundsException {@code i} is negative or, greater than or equal to
	 * universal ad identifier {@linkplain #universalAdIdCount() count}
	 * @since 1.2
	 * @see Builder#linearUniversalAdIds(Map)
	 * @see #universalAdIdCount()
	 * @see #universalAdIdValue(int)
	 */
	@SuppressWarnings("JavadocDeclaration")
	public String universalAdIdRegistry(int i) {
		return this.universalAdIds.keyAt(i);
	}

	/**
	 * Universal ad identifier, at index.
	 * <p>If {@code this} is {@linkplain #isOverlay() overlay}, the value returned is equal
	 * to:
	 * {@snippet lang="java" :
	 * this.overlayDisplay() // @link substring="overlayDisplay" target="#overlayDisplay()"
	 *     .universalAdIdValue(i); // @link substring="universalAdIdValue" target="DisplayAd#universalAdIdValue(int)"
	 * }
	 *
	 * @param i index to retrieve identifier at
	 * @return identifier at index {@code i}
	 * @throws IndexOutOfBoundsException {@code i} is negative or, greater than or equal to
	 * universal ad identifier {@linkplain #universalAdIdCount() count}
	 * @since 1.2
	 * @see Builder#linearUniversalAdIds(Map)
	 * @see #universalAdIdCount()
	 * @see #universalAdIdRegistry(int)
	 */
	@SuppressWarnings("JavadocDeclaration")
	public String universalAdIdValue(int i) {
		return this.universalAdIds.valueAt(i);
	}

	/**
	 * Sequence at which creative must be executed.
	 *
	 * @return execution sequence
	 * @since 1.2
	 * @see Builder#sequence(int)
	 */
	public @IntRange(from = 0) int sequence() {
		return this.sequenceAndFlags & ~FLAG_OVERLAY;
	}

	/**
	 * Item is overlay media.
	 *
	 * @return {@code true} if, and only if, creative is overlay media
	 * @since 1.2
	 * @see #overlayDisplay()
	 */
	public boolean isOverlay() {
		return (this.sequenceAndFlags & FLAG_OVERLAY) != 0;
	}

	/**
	 * Item is linear media.
	 *
	 * @return {@code true} if, and only if, creative is linear media
	 * @since 1.2
	 * @see #linearAsset(int)
	 * @see #linearIcon(int)
	 */
	public boolean isLinear() {
		return !this.isOverlay();
	}

	/**
	 * Ensure creative is {@linkplain #isOverlay() overlay} media.
	 *
	 * @throws IllegalStateException creative is not overlay media
	 */
	private void checkOverlay() {
		Preconditions.checkState(this.isOverlay());
	}

	/**
	 * Ensure creative is {@linkplain #isLinear() linear} media.
	 *
	 * @throws IllegalStateException creative is not linear media
	 */
	private void checkLinear() {
		Preconditions.checkState(this.isLinear());
	}

	/**
	 * Companion {@linkplain #companion(int) media} execution requirement.
	 *
	 * @return companion execution requirement
	 * @since 1.2
	 * @see Builder#companionRequirement(int)
	 */
	public @CompanionRequirementType int companionRequirement() {
		return this.companionRequirement;
	}

	/**
	 * Companion media count.
	 *
	 * @return companion media count
	 * @since 1.2
	 * @see Builder#companions(Collection)
	 * @see #companion(int)
	 */
	public int companionCount() {
		return this.companions.length;
	}

	/**
	 * Companion media, at index.
	 *
	 * @param i index to retrieve companion media at
	 * @return companion media at index {@code i}
	 * @throws IndexOutOfBoundsException {@code i} is negative or, greater than or equal to
	 * companion media {@linkplain #companionCount() count}
	 * @since 1.2
	 * @see Builder#companions(Collection)
	 * @see #companionCount()
	 */
	public CompanionAd companion(int i) {
		return this.companions[i];
	}

	/**
	 * Link to which user is navigated to when creative is activated (i.e. clicked).
	 * <p>If {@code this} is {@linkplain #isOverlay() overlay}, the value returned is equal
	 * to:
	 * {@snippet lang="java" :
	 * this.overlayDisplay() // @link substring="overlayDisplay" target="#overlayDisplay()"
	 *     .link(); // @link substring="link" target="DisplayAd#link()"
	 * }
	 *
	 * @return activation navigation link
	 * @since 1.2
	 * @see Builder#linearLink(LinkAsset)
	 */
	@SuppressWarnings("JavadocDeclaration")
	public LinkAsset link() {
		return this.link;
	}

	/**
	 * Total linear media playback duration, in seconds.
	 *
	 * @return playback duration
	 * @throws IllegalStateException creative is not {@linkplain #isLinear() linear} media
	 * @since 1.2
	 * @see Builder#linearPlaybackDurationSeconds(long)
	 * @see #isLinear()
	 */
	public long linearPlaybackDurationSeconds() {
		this.checkLinear();
		return this.linearPlaybackDurationSeconds;
	}

	/**
	 * Linear media playback duration, in seconds, after which skip controls may be displayed.
	 *
	 * @return playback duration after which skip controls should be displayed
	 * @throws IllegalStateException creative is not {@linkplain #isLinear() linear} media
	 * @since 1.2
	 * @see Builder#linearSkipOffsetSeconds(long)
	 * @see #isLinear()
	 */
	public long linearSkipOffsetSeconds() {
		this.checkLinear();
		return this.linearSkipOffsetSeconds;
	}

	private LinearAsset[] linearAssets() {
		this.checkLinear();
		return (LinearAsset[]) this.data;
	}

	/**
	 * Linear asset count.
	 *
	 * @return asset count
	 * @throws IllegalStateException creative is not {@linkplain #isLinear() linear} media
	 * @since 1.2
	 * @see Builder#linearAssets(Collection)
	 * @see #linearAsset(int)
	 */
	public int linearAssetCount() {
		return this.linearAssets().length;
	}

	/**
	 * Linear playback asset, at index.
	 *
	 * @param i index to retrieve asset at
	 * @return asset at index {@code i}
	 * @throws IllegalStateException creative is not {@linkplain #isLinear() linear} media
	 * @throws IndexOutOfBoundsException {@code i} is negative or, greater than or equal to asset
	 * {@linkplain #linearAssetCount() count}
	 * @since 1.2
	 * @see Builder#linearAssets(Collection)
	 * @see #isLinear()
	 * @see #linearAssetCount()
	 */
	public LinearAsset linearAsset(int i) {
		return this.linearAssets()[i];
	}

	/**
	 * Overlay display media.
	 *
	 * @return display media
	 * @throws IllegalStateException creative is not {@linkplain #isOverlay() overlay} media
	 * @since 1.2
	 * @see Builder#overlayDisplay(DisplayAd)
	 * @see #isOverlay()
	 */
	public DisplayAd overlayDisplay() {
		this.checkOverlay();
		return (DisplayAd) this.data;
	}

	/**
	 * Count of trackers to execute for creative media related events.
	 * <p>If {@code this} is {@linkplain #isOverlay() overlay}, the value returned is equal
	 * to:
	 * {@snippet lang="java" :
	 * this.overlayDisplay() // @link substring="overlayDisplay" target="#overlayDisplay()"
	 *     .eventTrackerCount(); // @link substring="eventTrackerCount" target="DisplayAd#eventTrackerCount()"
	 * }
	 *
	 * @return event tracker count
	 * @since 1.2
	 * @see Builder#linearEventTrackers(Collection)
	 * @see #eventTracker(int)
	 */
	@SuppressWarnings("JavadocDeclaration")
	public int eventTrackerCount() {
		return this.eventTrackers.length;
	}

	/**
	 * Event tracker, at index.
	 * <p>If {@code this} is {@linkplain #isOverlay() overlay}, the value returned is equal
	 * to:
	 * {@snippet lang="java" :
	 * this.overlayDisplay() // @link substring="overlayDisplay" target="#overlayDisplay()"
	 *     .eventTracker(i); // @link substring="eventTracker" target="DisplayAd#eventTracker(int)"
	 * }
	 *
	 * @param i index to retrieve tracker at
	 * @return event tracker at index {@code i}
	 * @throws IndexOutOfBoundsException {@code i} is negative or, greater than or equal to event
	 * tracker {@linkplain #eventTrackerCount() count}
	 * @since 1.2
	 * @see Builder#linearEventTrackers(Collection)
	 * @see #eventTrackerCount()
	 */
	@SuppressWarnings("JavadocDeclaration")
	public AdEventTracker eventTracker(int i) {
		return this.eventTrackers[i];
	}

	/**
	 * Linear icon asset count.
	 *
	 * @return icon asset count
	 * @throws IllegalStateException creative is not {@linkplain #isLinear() linear} media
	 * @since 1.2
	 * @see Builder#linearIcons(Collection)
	 * @see #isLinear()
	 * @see #linearIcon(int)
	 */
	public int linearIconCount() {
		this.checkLinear();
		return this.icons.length;
	}

	/**
	 * Linear icon asset, at index.
	 *
	 * @param i index to retrieve icon asset at
	 * @return icon asset at index {@code i}
	 * @throws IllegalStateException creative is not {@linkplain #isLinear() linear} media
	 * @throws IndexOutOfBoundsException {@code i} is negative or, greater than or equal to icon
	 * asset {@linkplain #linearIconCount() count}
	 * @since 1.2
	 * @see Builder#linearIcons(Collection)
	 * @see #isLinear()
	 * @see #linearIconCount()
	 */
	public IconAsset linearIcon(int i) {
		this.checkLinear();
		return this.icons[i];
	}

	/**
	 * Construct new {@linkplain Builder builder} initialized from {@code this}.
	 *
	 * @return initialized builder instance
	 * @since 1.2
	 */
	public Builder toBuilder() {
		return new Builder(this);
	}

	@Override
	public void toProtobuf(ProtobufWriter writer) {
		writer.writeString(ID, this.id);
		writer.writeInt32(SEQ, this.sequence());
		writer.writeInt32(COMPREQ, this.companionRequirement);
		writer.writeRepeatLen(COMP, this.companions);

		if (this.isOverlay()) {
			writer.writeLen(OVERLAY, this.overlayDisplay()::toDisplayAdProtobuf);
		} else {
			long cookie = writer.beginWriteLen(LINEAR);

			writer.writeLen(LINEAR_LINK, this.link);
			writer.writeInt64(LINEAR_DUR, this.linearPlaybackDurationSeconds);
			writer.writeInt64(LINEAR_SKIPOFF, this.linearSkipOffsetSeconds);
			writer.writeRepeatLen(LINEAR_ASSET, this.linearAssets());
			writer.writeRepeatLen(LINEAR_ICON, this.icons);
			writer.writeRepeatLen(LINEAR_EVENT, this.eventTrackers);
			for (int i = 0; i < this.universalAdIds.size(); i++) {
				writer.writeStringPair(LINEAR_UNIVID, new Pair<>(
					this.universalAdIds.keyAt(i),
					this.universalAdIds.valueAt(i)
				));
			}
			writer.endWriteLen(cookie);
		}
	}
}
