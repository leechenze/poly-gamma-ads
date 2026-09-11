// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.adcom.placement;

import static org.polygamma.android.origin.protobuf.Protobuf.WIRE_LEN;
import static org.polygamma.android.origin.protobuf.Protobuf.WIRE_VARINT;
import static org.polygamma.android.origin.protobuf.Protobuf.fieldTagOf;

import android.annotation.SuppressLint;

import androidx.annotation.Dimension;
import androidx.annotation.ReturnThis;

import org.polygamma.android.origin.adcom.enums.ActivationBehavior;
import org.polygamma.android.origin.adcom.enums.AdApiCode;
import org.polygamma.android.origin.adcom.enums.AdComEnums;
import org.polygamma.android.origin.protobuf.Protobuf.FieldTag;
import org.polygamma.android.origin.protobuf.ProtobufDecoder;
import org.polygamma.android.origin.protobuf.ProtobufEncoder;
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
	/*private static final @FieldTag int DELAY			= fieldTagOf(  1, WIRE_VARINT);*/
	private static final @FieldTag int SKIP				= fieldTagOf(  2, WIRE_VARINT);
	/*private static final @FieldTag int SKIPMIN		= fieldTagOf(  3, WIRE_VARINT);*/
	/*private static final @FieldTag int SKIPAFTER		= fieldTagOf(  4, WIRE_VARINT);*/
	/*private static final @FieldTag int PLAYMETHOD		= fieldTagOf(  5, WIRE_LEN);*/
	/*private static final @FieldTag int PLAYEND		= fieldTagOf(  6, WIRE_VARINT);*/
	private static final @FieldTag int MIME				= fieldTagOf(  7, WIRE_LEN);
	private static final @FieldTag int API				= fieldTagOf(  8, WIRE_LEN);
	private static final @FieldTag int CTYPE			= fieldTagOf(  9, WIRE_LEN);
	/*private static final @FieldTag int MINDUR			= fieldTagOf( 10, WIRE_VARINT);*/
	/*private static final @FieldTag int MAXDUR			= fieldTagOf( 11, WIRE_VARINT);*/
	/*private static final @FieldTag int RQDDURS		= fieldTagOf( 12, WIRE_LEN);*/
	/*private static final @FieldTag int MAXEXT			= fieldTagOf( 13, WIRE_VARINT);*/
	private static final @FieldTag int MINBITR			= fieldTagOf( 14, WIRE_VARINT);
	private static final @FieldTag int MAXBITR			= fieldTagOf( 15, WIRE_VARINT);
	/*private static final @FieldTag int DELIVERY		= fieldTagOf( 16, WIRE_LEN);*/
	/*private static final @FieldTag int MAXSEQ			= fieldTagOf( 17, WIRE_VARINT);*/
	/*private static final @FieldTag int PODDUR			= fieldTagOf( 18, WIRE_VARINT);*/
	/*private static final @FieldTag int PODID			= fieldTagOf( 19, WIRE_VARINT);*/
	/*private static final @FieldTag int PODSEQ			= fieldTagOf( 20, WIRE_VARINT);*/
	/*private static final @FieldTag int SLOTINPOD		= fieldTagOf( 21, WIRE_VARINT);*/
	/*private static final @FieldTag int MINCPMPERSEC	= fieldTagOf( 22, WIRE_FIXED64);*/
	/*private static final @FieldTag int COMP			= fieldTagOf( 23, WIRE_LEN);*/
	/*private static final @FieldTag int COMPTYPE		= fieldTagOf( 24, WIRE_LEN);*/
	/*private static final @FieldTag int OVERLAYEXPDIR	= fieldTagOf( 25, WIRE_LEN);*/
	/*private static final @FieldTag int EVENT			= fieldTagOf(500, WIRE_LEN);*/

	// `VideoAdFormat`
	/*private static final @FieldTag int VIDEO_PTYPE	= fieldTagOf( 40, WIRE_VARINT);*/
	/*private static final @FieldTag int VIDEO_POS		= fieldTagOf( 41, WIRE_VARINT);*/
	private static final @FieldTag int VIDEO_CLKTYPE	= fieldTagOf( 42, WIRE_VARINT);
	private static final @FieldTag int VIDEO_W			= fieldTagOf( 43, WIRE_VARINT);
	private static final @FieldTag int VIDEO_H			= fieldTagOf( 44, WIRE_VARINT);
	private static final @FieldTag int VIDEO_UNIT		= fieldTagOf( 45, WIRE_VARINT);
	/*private static final @FieldTag int VIDEO_LINEAR	= fieldTagOf( 46, WIRE_VARINT);*/
	/*private static final @FieldTag int VIDEO_BOXING	= fieldTagOf( 47, WIRE_VARINT);*/
	/*private static final @FieldTag int VIDEO_EXPDIR	= fieldTagOf( 48, WIRE_LEN);*/

	// `AudioAdFormat`
	/*private static final @FieldTag int AUDIO_FEED		= fieldTagOf( 40, WIRE_VARINT);*/
	/*private static final @FieldTag int AUDIO_NVOL		= fieldTagOf( 41, WIRE_VARINT);*/

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

	// Deserialize playback ad media format from Protobuf message.
	private static PlaybackAdFormat ofProtobuf(ProtobufDecoder dec, PlaybackAdFormat base) {
		PlaybackAdFormat rv = new PlaybackAdFormat(base);
		List<String> mime = new ArrayList<>();

		while (dec.hasRemaining()) {
			int tag = dec.decodeFieldTag();

			if (tag == API) {
				rv.setSupportedAdApiMask(dec.decodePackedUint32Bitmap64());
			} else if (tag == MIME) {
				mime.add(dec.decodeString());
			} else if (tag == SKIP) {
				rv.activationBehaviorAndFlags |= dec.decodeBool() ? FLAG_SKIPPABLE : 0;
			} else if (tag == MINBITR) {
				rv.minBitRateKbps = dec.decodeUint32();
			} else if (tag == MAXBITR) {
				rv.maxBitRateKbps = dec.decodeUint32();
			} else if (rv.isVideoAd() && tag == VIDEO_CLKTYPE) {
				int behavior = dec.decodeUint32();

				if (behavior < 0 || behavior > AdComEnums.MAX_ACTIVATION_BEHAVIOR)
					continue;
				rv.activationBehaviorAndFlags =
					(rv.activationBehaviorAndFlags & FLAG_MASK) |
					(behavior & ~FLAG_MASK);
			} else if (rv.isVideoAd() && tag == VIDEO_W) {
				rv.videoPlayerWidthDp = dec.decodeUint32();
			} else if (rv.isVideoAd() && tag == VIDEO_H) {
				rv.videoPlayerHeightDp = dec.decodeUint32();
			} else {
				dec.skipFieldValue(tag);
			}
		}
		rv.setSupportedMimes(mime);
		return rv;
	}

	/**
	 * Deserialize {@linkplain #isVideoAd() video} ad media format from Protobuf message.
	 *
	 * @param dec decoder to deserialize from
	 * @return resulting video ad media format
	 * @throws RuntimeException coding is malformed
	 * @since 1.2
	 */
	public static PlaybackAdFormat ofVideoAdProtobuf(ProtobufDecoder dec) {
		return ofProtobuf(dec, DEFAULT_VIDEO);
	}

	/**
	 * Deserialize {@linkplain #isAudioAd() audio} ad media format from Protobuf message.
	 *
	 * @param dec decoder to deserialize from
	 * @return resulting audio ad media format
	 * @throws RuntimeException coding is malformed
	 * @since 1.2
	 */
	public static PlaybackAdFormat ofAudioAdProtobuf(ProtobufDecoder dec) {
		return ofProtobuf(dec, DEFAULT_AUDIO);
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
	public void toProtobuf(ProtobufEncoder enc) {
		super.encodeCommonProtobufFields(enc, MIME, API);

		enc.encodeBoolField(SKIP, this.skippable())
			.encodePackedUint32Bitmap32Field(CTYPE, 0x01, AdComEnums.PlaybackCreativeStructured)
			.encodeUnsignedIntField(MINBITR, this.minBitRateKbps)
			.encodeUnsignedIntField(MAXBITR, this.maxBitRateKbps);

		if (this.isVideoAd()) {
			enc.encodeUnsignedIntField(VIDEO_CLKTYPE, this.videoActivationBehavior())
				.encodeUnsignedIntField(VIDEO_W, this.videoPlayerWidthDp)
				.encodeUnsignedIntField(VIDEO_H, this.videoPlayerHeightDp)
				.encodeUnsignedIntField(VIDEO_UNIT, AdComEnums.DimensionDp);
		}
	}
}
