// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.adcom.placement;

import static org.polygamma.android.origin.protobuf.ProtobufField.*;

import android.annotation.SuppressLint;

import androidx.annotation.Dimension;
import androidx.annotation.ReturnThis;

import org.polygamma.android.origin.adcom.enums.ActivationBehavior;
import org.polygamma.android.origin.adcom.enums.AdApiCode;
import org.polygamma.android.origin.adcom.enums.AdComEnums;
import org.polygamma.android.origin.protobuf.ProtobufReader;
import org.polygamma.android.origin.protobuf.ProtobufWriter;
import org.polygamma.android.origin.util.Preconditions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Playback ad media format.
 *
 * @since 1.2
 * @see <a href="https://github.com/InteractiveAdvertisingBureau/AdCOM/blob/main/AdCOM%20v1.0%20FINAL.md#object--audioplacement-">AdCOM, version 1.0 - Object: AudioPlacement</a>
 * @see <a href="https://github.com/InteractiveAdvertisingBureau/AdCOM/blob/main/AdCOM%20v1.0%20FINAL.md#object--videoplacement-">AdCOM, version 1.0 - Object: VideoPlacement</a>
 */
public final class PlaybackAdFormat extends AdFormat {

	// `PlaybackAdFormat`
	/*private static final @Tag int DELAY			= ofSint64(       1);*/
	private static final @Tag int SKIP				= ofBool(         2);
	/*private static final @Tag int SKIPMIN			= ofInt64(        3);*/
	/*private static final @Tag int SKIPAFTER		= ofInt64(        4);*/
	/*private static final @Tag int PLAYMETHOD		= ofPackedInt32(  5);*/
	/*private static final @Tag int PLAYEND			= ofInt32(        6);*/
	private static final @Tag int MIME				= ofString(       7);
	private static final @Tag int API				= ofPackedInt32(  8);
	private static final @Tag int CTYPE				= ofPackedInt32(  9);
	/*private static final @Tag int MINDUR			= ofInt64(       10);*/
	/*private static final @Tag int MAXDUR			= ofInt64(       11);*/
	/*private static final @Tag int RQDDURS			= ofPackedInt64( 12);*/
	/*private static final @Tag int MAXEXT			= ofInt64(       13);*/
	private static final @Tag int MINBITR			= ofInt32(       14);
	private static final @Tag int MAXBITR			= ofInt32(       15);
	/*private static final @Tag int DELIVERY		= ofPackedInt32( 16);*/
	/*private static final @Tag int MAXSEQ			= ofInt32(       17);*/
	/*private static final @Tag int PODDUR			= ofInt32(       18);*/
	/*private static final @Tag int PODID			= ofInt32(       19);*/
	/*private static final @Tag int PODSEQ			= ofSint32(      20);*/
	/*private static final @Tag int SLOTINPOD		= ofSint32(      21);*/
	/*private static final @Tag int MINCPMPERSEC	= ofDouble(      22);*/
	/*private static final @Tag int COMP			= ofMessage(     23);*/
	/*private static final @Tag int COMPTYPE		= ofPackedInt32( 24);*/
	/*private static final @Tag int OVERLAYEXPDIR	= ofPackedInt32( 25);*/
	/*private static final @Tag int EVENT			= ofMessage(    500);*/

	// `VideoAdFormat`
	/*private static final @Tag int VIDEO_PTYPE		= ofInt32(       40);*/
	/*private static final @Tag int VIDEO_POS		= ofInt32(       41);*/
	private static final @Tag int VIDEO_CLKTYPE		= ofInt32(       42);
	private static final @Tag int VIDEO_W			= ofInt32(       43);
	private static final @Tag int VIDEO_H			= ofInt32(       44);
	private static final @Tag int VIDEO_UNIT		= ofInt32(       45);
	/*private static final @Tag int VIDEO_LINEAR	= ofInt32(       46);*/
	/*private static final @Tag int VIDEO_BOXING	= ofBool(        47);*/
	/*private static final @Tag int VIDEO_EXPDIR	= ofPackedInt32( 48);*/

	// `AudioAdFormat`
	/*private static final @Tag int AUDIO_FEED		= ofInt32(       40);*/
	/*private static final @Tag int AUDIO_NVOL		= ofInt32(       41);*/

	/**
	 * Audio ad format.
	 */
	private static final int FLAG_AUDIO					= 0x10000000;

	/**
	 * Video ad format.
	 */
	private static final int FLAG_VIDEO					= 0x20000000;

	/**
	 * Skip button is provided.
	 */
	private static final int FLAG_SKIPPABLE				= 0x40000000;

	/**
	 * Mask of flags.
	 */
	private static final int FLAG_MASK					= 0xf0000000;

	private static final PlaybackAdFormat DEFAULT_AUDIO = new PlaybackAdFormat(FLAG_AUDIO);
	private static final PlaybackAdFormat DEFAULT_VIDEO = new PlaybackAdFormat(FLAG_VIDEO);

	/**
	 * Playback ad media {@linkplain PlaybackAdFormat format} builder.
	 *
	 * @since 1.2
	 * @see #ofAudioAdBuilder()
	 * @see #ofVideoAdBuilder()
	 */
	public static final class Builder {

		private PlaybackAdFormat format;
		private boolean needClone;

		private Builder(PlaybackAdFormat format) {
			this.format = format;
			this.needClone = true;
		}

		private PlaybackAdFormat target() {
			if (this.needClone) {
				this.format = new PlaybackAdFormat(this.format);
				this.needClone = false;
			}
			return this.format;
		}

		private PlaybackAdFormat targetVideoAd() {
			this.format.checkVideoAd();
			return this.target();
		}

		/**
		 * Set supported MIME types.
		 *
		 * @param supp supported MIME types or {@linkplain Collection#isEmpty() empty} if all MIME
		 * types are supported
		 * @return {@code this}
		 * @since 1.2
		 * @see PlaybackAdFormat#supportedMime(int)
		 */
		@ReturnThis
		@SuppressLint("RestrictedApi")
		public Builder supportedMimes(Collection<String> supp) {
			this.target().setSupportedMimes(supp);
			return this;
		}

		/**
		 * Set supported ad APIs, by code.
		 *
		 * @param codes supported APIs
		 * @return {@code this}
		 * @since 1.2
		 * @see PlaybackAdFormat#isAdApiSupported(int)
		 */
		@ReturnThis
		@SuppressLint("RestrictedApi")
		public Builder supportedAdApis(@AdApiCode int... codes) {
			this.target().setSupportedAdApis(codes);
			return this;
		}

		/**
		 * Set whether placement provides skip button.
		 *
		 * @param skip {@code true} if, and only if, skip button is provided
		 * @return {@code this}
		 * @since 1.2
		 * @see PlaybackAdFormat#skippable()
		 */
		@ReturnThis
		public Builder skippable(boolean skip) {
			PlaybackAdFormat dst = this.target();

			if (skip)
				dst.activationBehaviorAndFlags |= FLAG_SKIPPABLE;
			else
				dst.activationBehaviorAndFlags &= ~FLAG_SKIPPABLE;
			return this;
		}

		/**
		 * Set minimum supported bit rate, in Kbps.
		 *
		 * @param kbps minimum bit rate
		 * @return {@code this}
		 * @since 1.2
		 * @see #maxBitRateKbps(int)
		 * @see PlaybackAdFormat#minBitRateKbps()
		 */
		@ReturnThis
		public Builder minBitRateKbps(int kbps) {
			this.target().minBitRateKbps = kbps;
			return this;
		}

		/**
		 * Set maximum supported bit rate, in Kbps.
		 *
		 * @param kbps maximum bit rate
		 * @return {@code this}
		 * @since 1.2
		 * @see #minBitRateKbps(int)
		 * @see PlaybackAdFormat#maxBitRateKbps()
		 */
		@ReturnThis
		public Builder maxBitRateKbps(int kbps) {
			this.target().maxBitRateKbps = kbps;
			return this;
		}

		/**
		 * Set video ad media player width, in device independent pixels.
		 *
		 * @param w player width
		 * @return {@code this}
		 * @throws IllegalStateException not building a {@linkplain PlaybackAdFormat#isVideoAd()
		 * video} ad media format
		 * @since 1.2
		 * @see #videoPlayerHeightDp(int)
		 * @see PlaybackAdFormat#videoPlayerWidthDp()
		 */
		@ReturnThis
		public Builder videoPlayerWidthDp(@Dimension(unit = Dimension.DP) int w) {
			this.targetVideoAd().videoPlayerWidthDp = w;
			return this;
		}

		/**
		 * Set video ad media player height, in device independent pixels.
		 *
		 * @param h player height
		 * @return {@code this}
		 * @throws IllegalStateException not building a {@linkplain PlaybackAdFormat#isVideoAd()
		 * video} ad media format
		 * @since 1.2
		 * @see #videoPlayerWidthDp(int)
		 * @see PlaybackAdFormat#videoPlayerHeightDp()
		 */
		@ReturnThis
		public Builder videoPlayerHeightDp(@Dimension(unit = Dimension.DP) int h) {
			this.targetVideoAd().videoPlayerHeightDp = h;
			return this;
		}

		/**
		 * Set behavior when video media is activated.
		 *
		 * @param behavior activation behavior
		 * @return {@code this}
		 * @throws IllegalStateException not building a {@linkplain PlaybackAdFormat#isVideoAd()
		 * video} ad media format
		 * @since 1.2
		 * @see PlaybackAdFormat#videoActivationBehavior()
		 */
		@ReturnThis
		public Builder videoActivationBehavior(@ActivationBehavior int behavior) {
			Preconditions.checkArgument((behavior & ~FLAG_MASK) == behavior);

			PlaybackAdFormat dst = this.targetVideoAd();

			dst.activationBehaviorAndFlags =
				(dst.activationBehaviorAndFlags & FLAG_MASK) | behavior;
			return this;
		}

		/**
		 * Build resulting format.
		 *
		 * @return resulting format instance
		 * @since 1.2
		 */
		public PlaybackAdFormat build() {
			this.needClone = true;
			return this.format;
		}
	}

	/**
	 * Default empty audio ad media format instance.
	 *
	 * @return empty format instance
	 * @since 1.2
	 * @see #isAudioAd()
	 */
	public static PlaybackAdFormat ofAudioAd() {
		return DEFAULT_AUDIO;
	}

	/**
	 * Default empty video ad media format instance.
	 *
	 * @return empty format instance
	 * @since 1.2
	 * @see #isVideoAd()
	 */
	public static PlaybackAdFormat ofVideoAd() {
		return DEFAULT_VIDEO;
	}

	/**
	 * Construct new empty audio ad media format {@linkplain Builder builder}.
	 *
	 * @return new builder instance
	 * @since 1.2
	 * @see #isAudioAd()
	 */
	public static Builder ofAudioAdBuilder() {
		return DEFAULT_AUDIO.toBuilder();
	}

	/**
	 * Construct new empty video ad media format {@linkplain Builder builder}.
	 *
	 * @return new builder instance
	 * @since 1.2
	 * @see #isVideoAd()
	 */
	public static Builder ofVideoAdBuilder() {
		return DEFAULT_VIDEO.toBuilder();
	}

	/**
	 * Deserialize playback ad media format from Protobuf message.
	 *
	 * @param reader reader to deserialize from
	 * @param base base format
	 * @return resulting format
	 * @throws RuntimeException coding is malformed
	 */
	private static PlaybackAdFormat ofProtobuf(ProtobufReader reader, PlaybackAdFormat base) {
		PlaybackAdFormat rv = new PlaybackAdFormat(base);
		List<String> mime = new ArrayList<>();

		while (reader.hasRemaining()) {
			int tag = reader.readTag();

			if (tag == API) {
				rv.setSupportedAdApiMask(reader.readWordBitmap(0));
			} else if (tag == MIME) {
				mime.add(reader.readString());
			} else if (tag == SKIP) {
				if (reader.readBool())
					rv.activationBehaviorAndFlags |= FLAG_SKIPPABLE;
			} else if (tag == MINBITR) {
				rv.minBitRateKbps = reader.readInt32();
			} else if (tag == MAXBITR) {
				rv.maxBitRateKbps = reader.readInt32();
			} else if (rv.isVideoAd() && tag == VIDEO_CLKTYPE) {
				int behavior = reader.readInt32();

				if (behavior < 0 || behavior > AdComEnums.MAX_ACTIVATION_BEHAVIOR)
					continue;
				rv.activationBehaviorAndFlags =
					(rv.activationBehaviorAndFlags & FLAG_MASK) |
					(behavior & ~FLAG_MASK);
			} else if (rv.isVideoAd() && tag == VIDEO_W) {
				rv.videoPlayerWidthDp = reader.readInt32();
			} else if (rv.isVideoAd() && tag == VIDEO_H) {
				rv.videoPlayerHeightDp = reader.readInt32();
			}
		}
		rv.setSupportedMimes(mime);
		return rv;
	}

	/**
	 * Deserialize {@linkplain #isVideoAd() video} ad media format from Protobuf message.
	 *
	 * @param reader reader to deserialize from
	 * @return resulting video ad media format
	 * @throws RuntimeException coding is malformed
	 * @since 1.2
	 */
	public static PlaybackAdFormat ofVideoAdProtobuf(ProtobufReader reader) {
		return ofProtobuf(reader, DEFAULT_VIDEO);
	}

	/**
	 * Deserialize {@linkplain #isAudioAd() audio} ad media format from Protobuf message.
	 *
	 * @param reader reader to deserialize from
	 * @return resulting audio ad media format
	 * @throws RuntimeException coding is malformed
	 * @since 1.2
	 */
	public static PlaybackAdFormat ofAudioAdProtobuf(ProtobufReader reader) {
		return ofProtobuf(reader, DEFAULT_AUDIO);
	}

	private int minBitRateKbps;
	private int maxBitRateKbps;
	private @Dimension(unit = Dimension.DP) int videoPlayerWidthDp;
	private @Dimension(unit = Dimension.DP) int videoPlayerHeightDp;
	private int activationBehaviorAndFlags;

	private PlaybackAdFormat(int flags) {
		super();
		this.activationBehaviorAndFlags = flags;
	}

	private PlaybackAdFormat(PlaybackAdFormat that) {
		super(that);
		this.minBitRateKbps = that.minBitRateKbps;
		this.maxBitRateKbps = that.maxBitRateKbps;
		this.videoPlayerWidthDp = that.videoPlayerWidthDp;
		this.videoPlayerHeightDp = that.videoPlayerHeightDp;
		this.activationBehaviorAndFlags = that.activationBehaviorAndFlags;
	}

	/**
	 * Format is for audio ads.
	 *
	 * @return {@code true} if, and only if, audio ad format
	 * @since 1.2
	 */
	public boolean isAudioAd() {
		return (this.activationBehaviorAndFlags & FLAG_AUDIO) != 0;
	}

	/**
	 * Format is for video ads.
	 *
	 * @return {@code true} if, and only if, video ad format
	 * @since 1.2
	 */
	public boolean isVideoAd() {
		return (this.activationBehaviorAndFlags & FLAG_VIDEO) != 0;
	}

	/**
	 * Placement provides skip button.
	 *
	 * @return {@code true} if, and only if, placement provides skip button
	 * @since 1.2
	 * @see Builder#skippable(boolean)
	 */
	public boolean skippable() {
		return (this.activationBehaviorAndFlags & FLAG_SKIPPABLE) != 0;
	}

	/**
	 * Ensure format is for {@linkplain #isVideoAd() video} ad media.
	 *
	 * @throws IllegalStateException format is not for video ad media
	 */
	private void checkVideoAd() {
		Preconditions.checkState(this.isVideoAd());
	}

	/**
	 * Minimum supported bit rate, in Kbps.
	 *
	 * @return minimum bit rate
	 * @since 1.2
	 * @see #maxBitRateKbps()
	 * @see Builder#minBitRateKbps(int)
	 */
	public int minBitRateKbps() {
		return this.minBitRateKbps;
	}

	/**
	 * Maximum supported bit rate, in Kbps.
	 *
	 * @return maximum bit rate
	 * @since 1.2
	 * @see #minBitRateKbps()
	 * @see Builder#maxBitRateKbps(int)
	 */
	public int maxBitRateKbps() {
		return this.maxBitRateKbps;
	}

	/**
	 * Video ad media player width, in device independent pixels.
	 *
	 * @return player width
	 * @throws IllegalStateException not {@linkplain #isVideoAd() video} ad media format
	 * @since 1.2
	 * @see #videoPlayerHeightDp()
	 * @see #isVideoAd()
	 * @see Builder#videoPlayerWidthDp(int)
	 */
	public @Dimension(unit = Dimension.DP) int videoPlayerWidthDp() {
		this.checkVideoAd();
		return this.videoPlayerWidthDp;
	}

	/**
	 * Video ad media player height, in device independent pixels.
	 *
	 * @return player height
	 * @throws IllegalStateException not {@linkplain #isVideoAd() video} ad media format
	 * @since 1.2
	 * @see #videoPlayerWidthDp()
	 * @see #isVideoAd()
	 * @see Builder#videoPlayerHeightDp(int)
	 */
	public @Dimension(unit = Dimension.DP) int videoPlayerHeightDp() {
		this.checkVideoAd();
		return this.videoPlayerHeightDp;
	}

	/**
	 * Behavior when video media is activated.
	 *
	 * @return activation behavior
	 * @throws IllegalStateException not {@linkplain #isVideoAd() video} ad media format
	 * @since 1.2
	 * @see #isVideoAd()
	 * @see Builder#videoActivationBehavior(int)
	 */
	@SuppressLint("WrongConstant")
	public @ActivationBehavior int videoActivationBehavior() {
		this.checkVideoAd();
		return this.activationBehaviorAndFlags & ~FLAG_MASK;
	}

	/**
	 * Construct new {@linkplain Builder builder} initialized from {@code this}.
	 *
	 * @return new builder instance
	 * @since 1.2
	 */
	public Builder toBuilder() {
		return new Builder(this);
	}

	@Override
	public void toProtobuf(ProtobufWriter writer) {
		super.writeCommonProtobufFields(writer, MIME, API);
		writer.writeBool(SKIP, this.skippable());
		writer.writeWordBitmap(
			CTYPE,
			Integer.toUnsignedLong(0x01),
			AdComEnums.PlaybackCreativeStructured
		);
		writer.writeInt32(MINBITR, this.minBitRateKbps);
		writer.writeInt32(MAXBITR, this.maxBitRateKbps);

		if (this.isVideoAd()) {
			writer.writeInt32(VIDEO_CLKTYPE, this.videoActivationBehavior());
			writer.writeInt32(VIDEO_W, this.videoPlayerWidthDp);
			writer.writeInt32(VIDEO_H, this.videoPlayerHeightDp);
			writer.writeInt32(VIDEO_UNIT, AdComEnums.DimensionDp);
		}
	}
}
