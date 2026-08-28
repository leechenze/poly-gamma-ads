// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.adcom.media;

import static org.polygamma.android.origin.protobuf.ProtobufField.*;

import android.annotation.SuppressLint;

import androidx.annotation.IntDef;
import androidx.annotation.ReturnThis;

import org.polygamma.android.origin.protobuf.ProtobufReader;
import org.polygamma.android.origin.protobuf.ProtobufWriter;
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
	/*private static final @Tag int MIME			= ofString(       1);*/
	/*private static final @Tag int API				= ofPackedInt32(  2);*/
	/*private static final @Tag int CTYPE			= ofInt32(        3);*/
	/*private static final @Tag int DUR				= ofInt64(        4);*/
	/*private static final @Tag int ADM				= ofString(       5);*/
	/*private static final @Tag int CURL			= ofString(       6);*/
	private static final @Tag int PLAYBACK			= ofString(     500);

	// `PlaybackAd`
	private static final @Tag int PLAYBACK_TITLE	= ofString(       1);
	private static final @Tag int PLAYBACK_DESC		= ofString(       2);
	private static final @Tag int PLAYBACK_EVENT	= ofMessage(      3);
	private static final @Tag int PLAYBACK_CREATIVE	= ofMessage(      4);

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
	 * Deserialize {@code PlaybackAd} Protobuf message into playback ad media.
	 *
	 * @param dst ad media to deserialize into
	 * @param src reader to deserialize from
	 * @throws RuntimeException coding is malformed
	 */
	private static void deserializePlaybackAdProtobuf(PlaybackAd dst, ProtobufReader src) {
		List<AdEventTracker> trkr = new ArrayList<>(0);
		List<PlaybackCreative> creatives = new ArrayList<>(0);

		while (src.hasRemaining()) {
			int tag = src.readTag();

			if (tag == PLAYBACK_TITLE)
				dst.titleText = src.readString();
			else if (tag == PLAYBACK_DESC)
				dst.descriptionText = src.readString();
			else if (tag == PLAYBACK_EVENT)
				trkr.add(src.readLen(AdEventTracker::ofProtobuf));
			else if (tag == PLAYBACK_CREATIVE)
				creatives.add(src.readLen(PlaybackCreative::ofProtobuf));
		}
		dst.creatives = CollectionsCompat.toArrayOrEmpty(creatives, DEFAULT_AUDIO.creatives);
		dst.setEventTrackers(trkr);
	}

	/**
	 * Deserialize playback ad media from a {@code PlaybackAd} Protobuf message.
	 * <p>The {@link #isAudioAd()} and {@link #isVideoAd()} methods, of the returned ad media, are
	 * guaranteed to both return {@code false}.
	 *
	 * @param reader reader to deserialize from
	 * @return deserialized ad media
	 * @throws RuntimeException coding is malformed
	 * @since 1.2
	 */
	public static PlaybackAd ofPlaybackAdProtobuf(ProtobufReader reader) {
		PlaybackAd rv = new PlaybackAd(DEFAULT_VIDEO);

		rv.type = TYPE_NONE;
		deserializePlaybackAdProtobuf(rv, reader);
		return rv;
	}

	/**
	 * Deserialize {@linkplain #isAudioAd() audio} or {@linkplain #isVideoAd() video} playback ad
	 * media from a Protobuf message.
	 *
	 * @param base base media to deserialize with
	 * @param reader reader to deserialize from
	 * @return deserialized ad media
	 * @throws RuntimeException coding is malformed
	 */
	private static PlaybackAd ofAudioOrVideoAdProtobuf(PlaybackAd base, ProtobufReader reader) {
		PlaybackAd rv = new PlaybackAd(base);

		while (reader.hasRemaining()) {
			int tag = reader.readTag();

			if (tag == PLAYBACK) {
				int cookie = reader.beginReadLen();

				deserializePlaybackAdProtobuf(rv, reader);
				reader.endReadLen(cookie);
			}
		}
		return rv;
	}

	/**
	 * Deserialize {@linkplain #isVideoAd() video} playback ad media from a Protobuf message.
	 *
	 * @param reader reader to deserialize from
	 * @return deserialized ad media
	 * @throws RuntimeException coding is malformed
	 * @since 1.2
	 * @see #isVideoAd()
	 */
	public static PlaybackAd ofVideoAdProtobuf(ProtobufReader reader) {
		return ofAudioOrVideoAdProtobuf(DEFAULT_VIDEO, reader);
	}

	/**
	 * Deserialize {@linkplain #isAudioAd() audio} playback ad media from a Protobuf message.
	 *
	 * @param reader reader to deserialize from
	 * @return deserialized ad media
	 * @throws RuntimeException coding is malformed
	 * @since 1.2
	 * @see #isAudioAd()
	 */
	public static PlaybackAd ofAudioAdProtobuf(ProtobufReader reader) {
		return ofAudioOrVideoAdProtobuf(DEFAULT_AUDIO, reader);
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

	/**
	 * Playback media is audio.
	 *
	 * @return {@code true} if, and only if, audio playback ad media
	 * @since 1.2
	 * @see #ofAudioAd()
	 * @see #ofAudioAdBuilder()
	 * @see #ofAudioAdProtobuf(ProtobufReader)
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
	 * @see #ofVideoAdProtobuf(ProtobufReader)
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
	 * @param writer writer to serialize into
	 * @since 1.2
	 */
	public void toPlaybackAdProtobuf(ProtobufWriter writer) {
		writer.writeString(PLAYBACK_TITLE, this.titleText);
		writer.writeString(PLAYBACK_DESC, this.descriptionText);
		writer.writeRepeatLen(PLAYBACK_EVENT, super.eventTrackers());
		writer.writeRepeatLen(PLAYBACK_CREATIVE, this.creatives);
	}

	/**
	 * Serialize playback ad media as an {@linkplain #isAudioAd() audio} or {@linkplain
	 * #isVideoAd() video} ad media Protobuf message.
	 *
	 * @param writer writer to serialize into
	 * @since 1.2
	 */
	public void toAudioOrVideoAdProtobuf(ProtobufWriter writer) {
		long cookie = writer.beginWriteLen(PLAYBACK);

		this.toPlaybackAdProtobuf(writer);
		writer.endWriteLen(cookie);
	}
}
