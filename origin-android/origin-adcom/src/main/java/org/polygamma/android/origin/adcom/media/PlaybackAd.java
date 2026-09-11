// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.adcom.media;

import static org.polygamma.android.origin.protobuf.Protobuf.WIRE_LEN;
import static org.polygamma.android.origin.protobuf.Protobuf.fieldTagOf;

import android.annotation.SuppressLint;

import androidx.annotation.IntDef;
import androidx.annotation.ReturnThis;

import org.polygamma.android.origin.protobuf.Protobuf.FieldTag;
import org.polygamma.android.origin.protobuf.ProtobufDecoder;
import org.polygamma.android.origin.protobuf.ProtobufEncoder;
import org.polygamma.android.origin.util.CollectionsCompat;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Audio or video playback advertising media.
 *
 * @since 1.2
 * @see <a href="https://github.com/InteractiveAdvertisingBureau/AdCOM/blob/main/AdCOM%20v1.0%20FINAL.md#object--audio-">AdCOM, version 1.0 - Object: Audio</a>
 * @see <a href="https://github.com/InteractiveAdvertisingBureau/AdCOM/blob/main/AdCOM%20v1.0%20FINAL.md#object--video-">AdCOM, version 1.0 - Object: Video</a>
 */
public final class PlaybackAd extends Ad {

	// `{Audio,Video}Ad`
	/*private static final @FieldTag int MIME				= fieldTagOf(  1, WIRE_LEN);*/
	/*private static final @FieldTag int API				= fieldTagOf(  2, WIRE_LEN);*/
	/*private static final @FieldTag int CTYPE				= fieldTagOf(  3, WIRE_VARINT);*/
	/*private static final @FieldTag int DUR				= fieldTagOf(  4, WIRE_VARINT);*/
	/*private static final @FieldTag int ADM				= fieldTagOf(  5, WIRE_LEN);*/
	/*private static final @FieldTag int CURL				= fieldTagOf(  6, WIRE_LEN);*/
	private static final @FieldTag int PLAYBACK				= fieldTagOf(500, WIRE_LEN);

	// `PlaybackAd`
	private static final @FieldTag int PLAYBACK_TITLE		= fieldTagOf(  1, WIRE_LEN);
	private static final @FieldTag int PLAYBACK_DESC		= fieldTagOf(  2, WIRE_LEN);
	private static final @FieldTag int PLAYBACK_EVENT		= fieldTagOf(  3, WIRE_LEN);
	private static final @FieldTag int PLAYBACK_CREATIVE	= fieldTagOf(  4, WIRE_LEN);

	/**
	 * Audio or video ad media.
	 */
	private static final int TYPE_NONE		= 0x00;

	/**
	 * Audio ad media.
	 */
	private static final int TYPE_AUDIO		= 0x01;

	/**
	 * Video ad media.
	 */
	private static final int TYPE_VIDEO		= 0x02;

	/**
	 * Playback ad media type.
	 */
	@Documented
	@Retention(RetentionPolicy.SOURCE)
	@Target(ElementType.TYPE_USE)
	@IntDef({ TYPE_AUDIO, TYPE_VIDEO, TYPE_NONE })
	private @interface Type {
	}

	private static final PlaybackAd DEFAULT_AUDIO	= new PlaybackAd(TYPE_AUDIO);
	private static final PlaybackAd DEFAULT_VIDEO	= new PlaybackAd(DEFAULT_AUDIO);

	static {
		DEFAULT_VIDEO.type = TYPE_VIDEO;
	}

	/**
	 * Audio or video playback ad {@linkplain PlaybackAd media} builder.
	 *
	 * @since 1.2
	 * @see #ofAudioAdBuilder()
	 * @see #ofVideoAdBuilder()
	 */
	public static final class Builder {

		private PlaybackAd playback;
		private boolean needClone;

		private Builder(PlaybackAd playback) {
			this.playback = playback;
			this.needClone = true;
		}

		private PlaybackAd target() {
			if (this.needClone) {
				this.playback = new PlaybackAd(this.playback);
				this.needClone = false;
			}
			return this.playback;
		}

		/**
		 * Set ad identifier, unique to vendor.
		 *
		 * @param id ad identifier
		 * @return {@code this}
		 * @since 1.2
		 * @see PlaybackAd#id()
		 */
		@ReturnThis
		@SuppressLint("RestrictedApi")
		public Builder id(String id) {
			this.target().setId(id);
			return this;
		}

		/**
		 * Set ad serving identifier, unique to ad server.
		 *
		 * @param id serving identifier
		 * @return {@code this}
		 * @since 1.2
		 * @see PlaybackAd#serveId()
		 */
		@ReturnThis
		@SuppressLint("RestrictedApi")
		public Builder serveId(String id) {
			this.target().setServeId(id);
			return this;
		}

		/**
		 * Set trackers to execute for ad media events.
		 *
		 * @param trkr event trackers
		 * @return {@code this}
		 * @since 1.2
		 * @see PlaybackAd#eventTracker(int)
		 */
		@ReturnThis
		@SuppressLint("RestrictedApi")
		public Builder eventTrackers(Collection<AdEventTracker> trkr) {
			this.target().setEventTrackers(trkr);
			return this;
		}

		/**
		 * Set whether ad media assets are delivered securely via HTTPS.
		 *
		 * @param secure {@code true} if, and only if, assets are delivered securely
		 * @return {@code this}
		 * @since 1.2
		 * @see PlaybackAd#secure()
		 */
		@ReturnThis
		@SuppressLint("RestrictedApi")
		public Builder secure(boolean secure) {
			this.target().setSecure(secure);
			return this;
		}

		/**
		 * Set human-readable ad title text.
		 *
		 * @param title title text
		 * @return {@code this}
		 * @since 1.2
		 * @see PlaybackAd#titleText()
		 */
		@ReturnThis
		public Builder titleText(String title) {
			this.target().titleText = title;
			return this;
		}

		/**
		 * Set human-readable ad description text.
		 *
		 * @param desc description text
		 * @return {@code this}
		 * @since 1.2
		 * @see PlaybackAd#descriptionText()
		 */
		@ReturnThis
		public Builder descriptionText(String desc) {
			this.target().descriptionText = desc;
			return this;
		}

		/**
		 * Set playback creative media.
		 *
		 * @param creatives creative media
		 * @return {@code this}
		 * @since 1.2
		 * @see PlaybackAd#creative(int)
		 */
		@ReturnThis
		public Builder creatives(Collection<PlaybackCreative> creatives) {
			this.target().creatives =
				CollectionsCompat.toArrayOrEmpty(creatives, DEFAULT_AUDIO.creatives);
			return this;
		}

		/**
		 * Build resulting ad media instance.
		 *
		 * @return ad media instance
		 * @since 1.2
		 */
		public PlaybackAd build() {
			this.needClone = true;
			return this.playback;
		}
	}

	/**
	 * Default empty {@linkplain #isVideoAd() video} ad media instance.
	 *
	 * @return empty video ad media instance
	 * @since 1.2
	 * @see #isVideoAd()
	 */
	public static PlaybackAd ofVideoAd() {
		return DEFAULT_VIDEO;
	}

	/**
	 * Default empty {@linkplain #isAudioAd() audio} ad media instance.
	 *
	 * @return empty audio ad media instance
	 * @since 1.2
	 * @see #isAudioAd()
	 */
	public static PlaybackAd ofAudioAd() {
		return DEFAULT_AUDIO;
	}

	/**
	 * Construct new empty {@linkplain #isVideoAd() video} ad media builder.
	 *
	 * @return empty builder instance
	 * @since 1.2
	 * @see #isVideoAd()
	 */
	public static Builder ofVideoAdBuilder() {
		return DEFAULT_VIDEO.toBuilder();
	}

	/**
	 * Construct new empty {@linkplain #isAudioAd() audio} ad media builder.
	 *
	 * @return empty builder instance
	 * @since 1.2
	 * @see #isAudioAd()
	 */
	public static Builder ofAudioAdBuilder() {
		return DEFAULT_AUDIO.toBuilder();
	}

	/**
	 * Deserialize playback ad media from a {@code PlaybackAd} Protobuf message.
	 * <p>The {@link #isAudioAd()} and {@link #isVideoAd()} methods, of the returned ad media, are
	 * guaranteed to both return {@code false}.
	 *
	 * @param dec decoder to deserialize from
	 * @return deserialized ad media
	 * @throws RuntimeException coding is malformed
	 * @since 1.2
	 */
	public static PlaybackAd ofPlaybackAdProtobuf(ProtobufDecoder dec) {
		PlaybackAd rv = new PlaybackAd(DEFAULT_VIDEO);

		rv.type = TYPE_NONE;
		rv.mergePlaybackAdProtobuf(dec);
		return rv;
	}

	// Deserialize audio or video playback ad media from a Protobuf message.
	private static PlaybackAd ofAudioOrVideoAdProtobuf(PlaybackAd base, ProtobufDecoder dec) {
		PlaybackAd rv = new PlaybackAd(base);

		while (dec.hasRemaining()) {
			int tag = dec.decodeFieldTag();

			if (tag == PLAYBACK)
				dec.decodeLen(rv, PlaybackAd::mergePlaybackAdProtobuf);
			else
				dec.skipFieldValue(tag);
		}
		return rv;
	}

	/**
	 * Deserialize {@linkplain #isVideoAd() video} playback ad media from a Protobuf message.
	 *
	 * @param dec decoder to deserialize from
	 * @return deserialized ad media
	 * @throws RuntimeException coding is malformed
	 * @since 1.2
	 * @see #isVideoAd()
	 */
	public static PlaybackAd ofVideoAdProtobuf(ProtobufDecoder dec) {
		return ofAudioOrVideoAdProtobuf(DEFAULT_VIDEO, dec);
	}

	/**
	 * Deserialize {@linkplain #isAudioAd() audio} playback ad media from a Protobuf message.
	 *
	 * @param dec decoder to deserialize from
	 * @return deserialized ad media
	 * @throws RuntimeException coding is malformed
	 * @since 1.2
	 * @see #isAudioAd()
	 */
	public static PlaybackAd ofAudioAdProtobuf(ProtobufDecoder dec) {
		return ofAudioOrVideoAdProtobuf(DEFAULT_AUDIO, dec);
	}

	private String titleText;
	private String descriptionText;
	private PlaybackCreative[] creatives;
	private @Type int type;

	private PlaybackAd(@SuppressWarnings("SameParameterValue") @Type int type) {
		super();
		this.titleText = "";
		this.descriptionText = "";
		this.creatives = new PlaybackCreative[0];
		this.type = type;
	}

	private PlaybackAd(PlaybackAd that) {
		super(that);
		this.titleText = that.titleText;
		this.descriptionText = that.descriptionText;
		this.creatives = that.creatives;
		this.type = that.type;
	}

	// Deserialize `PlaybackAd` Protobuf message into playback ad media.
	private PlaybackAd mergePlaybackAdProtobuf(ProtobufDecoder src) {
		List<AdEventTracker> trkr = new ArrayList<>(0);
		List<PlaybackCreative> creatives = new ArrayList<>(0);

		while (src.hasRemaining()) {
			int tag = src.decodeFieldTag();

			if (tag == PLAYBACK_TITLE)
				this.titleText = src.decodeString();
			else if (tag == PLAYBACK_DESC)
				this.descriptionText = src.decodeString();
			else if (tag == PLAYBACK_EVENT)
				trkr.add(src.decodeLen(AdEventTracker::ofProtobuf));
			else if (tag == PLAYBACK_CREATIVE)
				creatives.add(src.decodeLen(PlaybackCreative::ofProtobuf));
			else
				src.skipFieldValue(tag);
		}
		this.creatives = CollectionsCompat.toArrayOrEmpty(creatives, DEFAULT_AUDIO.creatives);
		this.setEventTrackers(trkr);
		return this;
	}

	/**
	 * Playback media is audio.
	 *
	 * @return {@code true} if, and only if, audio playback ad media
	 * @since 1.2
	 * @see #ofAudioAd()
	 * @see #ofAudioAdBuilder()
	 * @see #ofAudioAdProtobuf(ProtobufDecoder)
	 */
	public boolean isAudioAd() {
		return this.type == TYPE_AUDIO;
	}

	/**
	 * Playback media is video.
	 *
	 * @return {@code true} if, and only if, video playback ad media
	 * @since 1.2
	 * @see #ofVideoAd()
	 * @see #ofVideoAdBuilder()
	 * @see #ofVideoAdProtobuf(ProtobufDecoder)
	 */
	public boolean isVideoAd() {
		return this.type == TYPE_VIDEO;
	}

	/**
	 * Human-readable ad title text.
	 *
	 * @return title text
	 * @since 1.2
	 * @see Builder#titleText(String)
	 */
	public String titleText() {
		return this.titleText;
	}

	/**
	 * Human-readable ad description text.
	 *
	 * @return description text
	 * @since 1.2
	 * @see Builder#descriptionText(String)
	 */
	public String descriptionText() {
		return this.descriptionText;
	}

	/**
	 * Playback creative media count.
	 *
	 * @return creative media count
	 * @since 1.2
	 * @see Builder#creatives(Collection)
	 * @see #creative(int)
	 */
	public int creativeCount() {
		return this.creatives.length;
	}

	/**
	 * Playback creative media, at index.
	 *
	 * @param i index to retrieve item at
	 * @return media item at index {@code i}
	 * @throws IndexOutOfBoundsException {@code i} is negative or, greater than or equal to item
	 * {@linkplain #creativeCount() count}
	 * @since 1.2
	 * @see Builder#creatives(Collection)
	 * @see #creativeCount()
	 */
	public PlaybackCreative creative(int i) {
		return this.creatives[i];
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

	/**
	 * Serialize playback ad media as a {@code PlaybackAd} Protobuf message.
	 *
	 * @param enc encoder to serialize into
	 * @since 1.2
	 */
	public void toPlaybackAdProtobuf(ProtobufEncoder enc) {
		enc.encodeStringField(PLAYBACK_TITLE, this.titleText)
			.encodeStringField(PLAYBACK_DESC, this.descriptionText);

		for (AdEventTracker trkr : this.eventTrackers())
			enc.encodeMessageField(PLAYBACK_EVENT, trkr);
		for (PlaybackCreative cr : this.creatives)
			enc.encodeMessageField(PLAYBACK_CREATIVE, cr);
	}

	/**
	 * Serialize playback ad media as an {@linkplain #isAudioAd() audio} or {@linkplain
	 * #isVideoAd() video} ad media Protobuf message.
	 *
	 * @param enc encoder to serialize into
	 * @since 1.2
	 */
	public void toAudioOrVideoAdProtobuf(ProtobufEncoder enc) {
		enc.encodeLenField(PLAYBACK, this, PlaybackAd::toPlaybackAdProtobuf);
	}
}
