// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.adcom.media;

import static org.polygamma.android.origin.protobuf.ProtobufField.*;

import androidx.annotation.Dimension;
import androidx.annotation.Px;
import androidx.annotation.ReturnThis;

import org.polygamma.android.origin.adcom.enums.AdApiCode;
import org.polygamma.android.origin.adcom.enums.AdComEnums;
import org.polygamma.android.origin.protobuf.ProtobufReader;
import org.polygamma.android.origin.protobuf.ProtobufSerializable;
import org.polygamma.android.origin.protobuf.ProtobufWriter;
import org.polygamma.android.origin.util.CollectionsCompat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Icon overlay media asset.
 *
 * @since 1.2
 */
public final class IconAsset implements ProtobufSerializable {

	private static final @Tag int PROGRAM		= ofString(  1);
	private static final @Tag int ALT			= ofString(  2);
	private static final @Tag int TOOLTIP		= ofString(  3);
	private static final @Tag int PXRATIO		= ofFloat(   4);
	private static final @Tag int W				= ofInt32(   5);
	private static final @Tag int H				= ofInt32(   6);
	private static final @Tag int X				= ofInt32(   7);
	private static final @Tag int Y				= ofInt32(   8);
	private static final @Tag int DUR			= ofInt64(   9);
	private static final @Tag int OFF			= ofInt64(  10);
	private static final @Tag int API			= ofInt32(  11);
	private static final @Tag int EVENT			= ofMessage(12);
	private static final @Tag int LINK			= ofMessage(13);
	private static final @Tag int DISPLAY		= ofMessage(14);

	private static final IconAsset DEFAULT = new IconAsset();

	/**
	 * Icon overlay media {@linkplain IconAsset asset} builder.
	 *
	 * @since 1.2
	 * @see #ofBuilder()
	 */
	public static final class Builder {

		private IconAsset icon;
		private boolean needClone;

		private Builder(IconAsset icon) {
			this.icon = icon;
			this.needClone = true;
		}

		private IconAsset target() {
			if (this.needClone) {
				this.icon = new IconAsset(this.icon);
				this.needClone = false;
			}
			return this.icon;
		}

		/**
		 * Set name of program represented by icon.
		 *
		 * @param name program name
		 * @return {@code this}
		 * @since 1.2
		 * @see IconAsset#programName()
		 */
		@ReturnThis
		public Builder programName(String name) {
			this.target().programName = name;
			return this;
		}

		/**
		 * Set alternative text to display when icon display asset cannot be executed.
		 *
		 * @param txt display text
		 * @return {@code this}
		 * @since 1.2
		 * @see IconAsset#alternativeText()
		 */
		@ReturnThis
		public Builder alternativeText(String txt) {
			this.target().alternativeText = txt;
			return this;
		}

		/**
		 * Set human-readable text displayed when icon is hovered over.
		 *
		 * @param txt tooltip text
		 * @return {@code this}
		 * @since 1.2
		 * @see IconAsset#tooltipText()
		 */
		@ReturnThis
		public Builder tooltipText(String txt) {
			this.target().tooltipText = txt;
			return this;
		}

		/**
		 * Set duration, in seconds, icon should be displayed for.
		 *
		 * @param dur display duration
		 * @return {@code this}
		 * @since 1.2
		 * @see IconAsset#showDurationSeconds()
		 */
		@ReturnThis
		public Builder showDurationSeconds(long dur) {
			this.target().showDurationSeconds = dur;
			return this;
		}

		/**
		 * Set offset, in seconds, of ad show duration at which icon should be displayed at.
		 *
		 * @param dur offset duration
		 * @return {@code this}
		 * @since 1.2
		 * @see IconAsset#offsetDurationSeconds()
		 */
		@ReturnThis
		public Builder offsetDurationSeconds(long dur) {
			this.target().offsetDurationSeconds = dur;
			return this;
		}

		/**
		 * Set pixel ratio icon is intended for.
		 *
		 * @param pxratio pixel ratio
		 * @return {@code this}
		 * @since 1.2
		 * @see IconAsset#pixelRatio()
		 */
		@ReturnThis
		public Builder pixelRatio(float pxratio) {
			this.target().pixelRatio = pxratio;
			return this;
		}

		/**
		 * Set exact width, in pixels, of icon.
		 *
		 * @param w icon width
		 * @return {@code this}
		 * @since 1.2
		 * @see IconAsset#widthPx()
		 */
		@ReturnThis
		public Builder widthPx(@Px int w) {
			this.target().widthPx = w;
			return this;
		}

		/**
		 * Set exact height, in pixels, of icon.
		 *
		 * @param h icon height
		 * @return {@code this}
		 * @since 1.2
		 * @see IconAsset#heightPx()
		 */
		@ReturnThis
		public Builder heightPx(@Px int h) {
			this.target().heightPx = h;
			return this;
		}

		/**
		 * Set offset along {@code x} axis, in device independent pixels, at which icon should
		 * be rendered.
		 *
		 * @param x {@code x} axis offset
		 * @return {@code this}
		 * @since 1.2
		 * @see IconAsset#xOffsetDp()
		 */
		@ReturnThis
		public Builder xOffsetDp(@Dimension(unit = Dimension.DP) int x) {
			this.target().xOffsetDp = x;
			return this;
		}

		/**
		 * Set offset along {@code y} axis, in device independent pixels, at which icon should
		 * be rendered.
		 *
		 * @param y {@code y} axis offset
		 * @return {@code this}
		 * @since 1.2
		 * @see IconAsset#yOffsetDp()
		 */
		@ReturnThis
		public Builder yOffsetDp(@Dimension(unit = Dimension.DP) int y) {
			this.target().yOffsetDp = y;
			return this;
		}

		/**
		 * Set ad API required, by code, to execute icon display asset.
		 *
		 * @param api required ad API
		 * @return {@code this}
		 * @since 1.2
		 * @see IconAsset#requiredAdApi()
		 */
		@ReturnThis
		public Builder requiredAdApi(@AdApiCode int api) {
			this.target().requiredAdApi = api;
			return this;
		}

		/**
		 * Set display media assets.
		 *
		 * @param assets display media assets
		 * @return {@code this}
		 * @since 1.2
		 * @see IconAsset#display(int)
		 */
		@ReturnThis
		public Builder display(Collection<IconDisplayAsset> assets) {
			this.target().display = CollectionsCompat.toArrayOrEmpty(assets, DEFAULT.display);
			return this;
		}

		/**
		 * Set link to navigate to when icon media is activated (i.e. clicked).
		 *
		 * @param link activation navigation link
		 * @return {@code this}
		 * @since 1.2
		 * @see IconAsset#link()
		 */
		@ReturnThis
		public Builder link(LinkAsset link) {
			this.target().link = link;
			return this;
		}

		/**
		 * Set media event trackers.
		 *
		 * @param trkr event trackers
		 * @return {@code this}
		 * @since 1.2
		 * @see IconAsset#eventTracker(int)
		 */
		@ReturnThis
		public Builder eventTrackers(Collection<AdEventTracker> trkr) {
			this.target().eventTrackers =
				CollectionsCompat.toArrayOrEmpty(trkr, DEFAULT.eventTrackers);
			return this;
		}

		/**
		 * Build resulting icon asset.
		 *
		 * @return icon instance
		 * @since 1.2
		 */
		public IconAsset build() {
			this.needClone = true;
			return this.icon;
		}
	}

	/**
	 * Default empty icon overlay media asset instance.
	 *
	 * @return empty icon instance
	 * @since 1.2
	 */
	public static IconAsset of() {
		return DEFAULT;
	}

	/**
	 * Construct new empty {@linkplain Builder builder}.
	 *
	 * @return empty builder instance
	 * @since 1.2
	 */
	public static Builder ofBuilder() {
		return DEFAULT.toBuilder();
	}

	/**
	 * Deserialize icon media from Protobuf message.
	 *
	 * @param reader reader to deserialize from
	 * @return deserialized icon media
	 * @throws RuntimeException coding is malformed
	 * @since 1.2
	 */
	public static IconAsset ofProtobuf(ProtobufReader reader) {
		IconAsset rv = new IconAsset(DEFAULT);
		List<AdEventTracker> trkr = new ArrayList<>(0);
		List<IconDisplayAsset> display = new ArrayList<>(0);

		while (reader.hasRemaining()) {
			int tag = reader.readTag();

			if (tag == PROGRAM) {
				rv.programName = reader.readString();
			} else if (tag == ALT) {
				rv.alternativeText = reader.readString();
			} else if (tag == TOOLTIP) {
				rv.tooltipText = reader.readString();
			} else if (tag == PXRATIO) {
				rv.pixelRatio = reader.readFloat();
			} else if (tag == W) {
				rv.widthPx = reader.readInt32();
			} else if (tag == H) {
				rv.heightPx = reader.readInt32();
			} else if (tag == X) {
				rv.xOffsetDp = reader.readInt32();
			} else if (tag == Y) {
				rv.yOffsetDp = reader.readInt32();
			} else if (tag == DUR) {
				rv.showDurationSeconds = reader.readInt64();
			} else if (tag == OFF) {
				rv.offsetDurationSeconds = reader.readInt64();
			} else if (tag == API) {
				rv.requiredAdApi = reader.readInt32();
			} else if (tag == EVENT) {
				trkr.add(reader.readLen(AdEventTracker::ofProtobuf));
			} else if (tag == LINK) {
				rv.link = reader.readLen(LinkAsset::ofProtobuf);
			} else if (tag == DISPLAY) {
				display.add(reader.readLen(IconDisplayAsset::ofProtobuf));
			}
		}
		rv.eventTrackers = CollectionsCompat.toArrayOrEmpty(trkr, DEFAULT.eventTrackers);
		rv.display = CollectionsCompat.toArrayOrEmpty(display, DEFAULT.display);
		return rv;
	}

	private String programName;
	private String alternativeText;
	private String tooltipText;
	private long showDurationSeconds;
	private long offsetDurationSeconds;
	private float pixelRatio;
	private @Px int widthPx;
	private @Px int heightPx;
	private @Dimension(unit = Dimension.DP) int xOffsetDp;
	private @Dimension(unit = Dimension.DP) int yOffsetDp;
	private @AdApiCode int requiredAdApi;
	private IconDisplayAsset[] display;
	private LinkAsset link;
	private AdEventTracker[] eventTrackers;

	private IconAsset() {
		this.programName = "";
		this.alternativeText = "";
		this.tooltipText = "";
		this.requiredAdApi = AdComEnums.AdApiUnknown;
		this.display = new IconDisplayAsset[0];
		this.link = LinkAsset.of();
		this.eventTrackers = new AdEventTracker[0];
	}

	private IconAsset(IconAsset that) {
		this.programName = that.programName;
		this.alternativeText = that.alternativeText;
		this.tooltipText = that.tooltipText;
		this.showDurationSeconds = that.showDurationSeconds;
		this.offsetDurationSeconds = that.offsetDurationSeconds;
		this.pixelRatio = that.pixelRatio;
		this.widthPx = that.widthPx;
		this.heightPx = that.heightPx;
		this.xOffsetDp = that.xOffsetDp;
		this.yOffsetDp = that.yOffsetDp;
		this.requiredAdApi = that.requiredAdApi;
		this.display = that.display;
		this.link = that.link;
		this.eventTrackers = that.eventTrackers;
	}

	/**
	 * Name of program represented by icon.
	 *
	 * @return program name
	 * @since 1.2
	 */
	public String programName() {
		return this.programName;
	}

	/**
	 * Alternative text to display when icon display asset cannot be executed.
	 *
	 * @return display text
	 * @since 1.2
	 */
	public String alternativeText() {
		return this.alternativeText;
	}

	/**
	 * Human-readable text displayed when icon is hovered over.
	 *
	 * @return tooltip text
	 * @since 1.2
	 */
	public String tooltipText() {
		return this.tooltipText;
	}

	/**
	 * Duration, in seconds, icon should be displayed for.
	 *
	 * @return display duration, or {@code 0} to display indefinitely
	 * @since 1.2
	 */
	public long showDurationSeconds() {
		return this.showDurationSeconds;
	}

	/**
	 * Offset, in seconds, of ad show duration at which icon should be displayed at.
	 *
	 * @return offset duration
	 * @since 1.2
	 */
	public long offsetDurationSeconds() {
		return this.offsetDurationSeconds;
	}

	/**
	 * Pixel ratio icon is intended for.
	 *
	 * @return pixel ratio
	 * @since 1.2
	 */
	public float pixelRatio() {
		return this.pixelRatio;
	}

	/**
	 * Exact width, in pixels, of icon.
	 *
	 * @return icon width
	 * @since 1.2
	 * @see #heightPx()
	 */
	public @Px int widthPx() {
		return this.widthPx;
	}

	/**
	 * Exact height, in pixels, of icon.
	 *
	 * @return icon height
	 * @since 1.2
	 * @see #widthPx()
	 */
	public @Px int heightPx() {
		return this.heightPx;
	}

	/**
	 * Offset along {@code x} axis, in device independent pixels, at which icon should be rendered.
	 *
	 * @return {@code x} axis offset
	 * @since 1.2
	 * @see #yOffsetDp()
	 */
	public @Dimension(unit = Dimension.DP) int xOffsetDp() {
		return this.xOffsetDp;
	}

	/**
	 * Offset along {@code y} axis, in device independent pixels, at which icon should be rendered.
	 *
	 * @return {@code y} axis offset
	 * @since 1.2
	 * @see #xOffsetDp()
	 */
	public @Dimension(unit = Dimension.DP) int yOffsetDp() {
		return this.yOffsetDp;
	}

	/**
	 * Ad API required, by code, to execute icon display asset.
	 *
	 * @return required ad API
	 * @since 1.2
	 */
	public @AdApiCode int requiredAdApi() {
		return this.requiredAdApi;
	}

	/**
	 * Display media asset count.
	 *
	 * @return asset count
	 * @since 1.2
	 * @see #display(int)
	 */
	public int displayCount() {
		return this.display.length;
	}

	/**
	 * Display media asset, at index.
	 *
	 * @param i index to retrieve asset at
	 * @return asset at index {@code i}
	 * @throws IndexOutOfBoundsException {@code i} is negative or, greater than or equal to asset
	 * {@linkplain #displayCount() count}
	 * @since 1.2
	 * @see #displayCount()
	 */
	public IconDisplayAsset display(int i) {
		return this.display[i];
	}

	/**
	 * Link to navigate to when icon media is activated (i.e. clicked).
	 *
	 * @return activation navigation link
	 * @since 1.2
	 */
	public LinkAsset link() {
		return this.link;
	}

	/**
	 * Media event tracker count.
	 *
	 * @return event tracker count
	 * @since 1.2
	 * @see #eventTracker(int)
	 */
	public int eventTrackerCount() {
		return this.eventTrackers.length;
	}

	/**
	 * Media event tracker, at index.
	 *
	 * @param i index to retrieve tracker at
	 * @return tracker at index {@code i}
	 * @throws IndexOutOfBoundsException {@code i} is negative or, greater than or equal to tracker
	 * {@linkplain #eventTrackerCount() count}
	 * @since 1.2
	 * @see #eventTrackerCount()
	 */
	public AdEventTracker eventTracker(int i) {
		return this.eventTrackers[i];
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
		writer.writeString(PROGRAM, this.programName);
		writer.writeString(ALT, this.alternativeText);
		writer.writeString(TOOLTIP, this.tooltipText);
		writer.writeFloat(PXRATIO, this.pixelRatio);
		writer.writeInt32(W, this.widthPx);
		writer.writeInt32(H, this.heightPx);
		writer.writeInt32(X, this.xOffsetDp);
		writer.writeInt32(Y, this.yOffsetDp);
		writer.writeInt64(DUR, this.showDurationSeconds);
		writer.writeInt64(OFF, this.offsetDurationSeconds);
		writer.writeInt64(API, this.requiredAdApi);
		writer.writeRepeatLen(EVENT, this.eventTrackers);
		writer.writeLen(LINK, this.link);
		writer.writeRepeatLen(DISPLAY, this.display);
	}
}
