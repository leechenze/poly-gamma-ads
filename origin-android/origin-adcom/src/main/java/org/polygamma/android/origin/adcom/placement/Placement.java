// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.adcom.placement;

import static org.polygamma.android.origin.protobuf.ProtobufField.*;

import androidx.annotation.ReturnThis;

import org.polygamma.android.origin.protobuf.ProtobufReader;
import org.polygamma.android.origin.protobuf.ProtobufSerializable;
import org.polygamma.android.origin.protobuf.ProtobufWriter;
import org.polygamma.android.origin.util.Preconditions;

/**
 * Description of placement through which ad media is distributed.
 *
 * @since 1.2
 * @see <a href="https://github.com/InteractiveAdvertisingBureau/AdCOM/blob/main/AdCOM%20v1.0%20FINAL.md#object_placement">AdCOM, version 1.0 - Object: Placement</a>
 */
public final class Placement implements ProtobufSerializable {

	private static final @Tag int TAGID				= ofString(  1);
	/*private static final @Tag int SSAI			= ofInt32(   2);*/
	/*private static final @Tag int SDK				= ofString(  3);*/
	/*private static final @Tag int SDKVER			= ofString(  4);*/
	/*private static final @Tag int REWARD			= ofBool(    5);*/
	/*private static final @Tag int WLANG			= ofString(  6);*/
	private static final @Tag int SECURE			= ofBool(    7);
	private static final @Tag int ADMX				= ofBool(    8);
	private static final @Tag int CURLX				= ofBool(    9);
	private static final @Tag int DISPLAY			= ofMessage(10);
	private static final @Tag int VIDEO				= ofMessage(11);
	private static final @Tag int AUDIO				= ofMessage( 12);

	private static final int FLAG_SECURE	= 0x01;
	private static final int FLAG_ADMX		= 0x02;
	private static final int FLAG_CURLX		= 0x04;

	private static final Placement DEFAULT = new Placement();

	/**
	 * {@linkplain Placement} builder.
	 *
	 * @since 1.2
	 * @see #ofBuilder()
	 */
	public static final class Builder {

		private Placement placement;
		private boolean needClone;

		private Builder(Placement plcmt) {
			this.placement = plcmt;
			this.needClone = true;
		}

		private Placement target() {
			if (this.needClone) {
				this.placement = new Placement(this.placement);
				this.needClone = false;
			}
			return this.placement;
		}

		/**
		 * Set placement identifier, unique to vendor.
		 *
		 * @param id placement identifier
		 * @return {@code this}
		 * @since 1.2
		 * @see Placement#id()
		 */
		@ReturnThis
		public Builder id(String id) {
			this.target().id = id;
			return this;
		}

		/**
		 * Set format supported for display ad media.
		 *
		 * @param fmt supported display format
		 * @return {@code this}
		 * @since 1.2
		 * @see Placement#display()
		 */
		@ReturnThis
		public Builder display(DisplayAdFormat fmt) {
			this.target().display = fmt;
			return this;
		}

		/**
		 * Set format supported for video ad media.
		 *
		 * @param fmt supported video format
		 * @return {@code this}
		 * @throws IllegalStateException {@code fmt} is not for an {@linkplain
		 * PlaybackAdFormat#isVideoAd() video} ad
		 * @since 1.2
		 * @see Placement#video()
		 * @see PlaybackAdFormat#isVideoAd()
		 */
		@ReturnThis
		public Builder video(PlaybackAdFormat fmt) {
			Preconditions.checkArgument(fmt.isVideoAd());
			this.target().video = fmt;
			return this;
		}

		/**
		 * Set format supported for audio ad media.
		 *
		 * @param fmt supported audio format
		 * @return {@code this}
		 * @throws IllegalStateException {@code fmt} is not for an {@linkplain
		 * PlaybackAdFormat#isAudioAd() audio} ad
		 * @since 1.2
		 * @see Placement#audio()
		 * @see PlaybackAdFormat#isAudioAd()
		 */
		@ReturnThis
		public Builder audio(PlaybackAdFormat fmt) {
			Preconditions.checkArgument(fmt.isAudioAd());
			this.target().audio = fmt;
			return this;
		}

		/**
		 * Set whether ad media assets must be delivered securely via HTTPS.
		 *
		 * @param secure {@code true} if, and only if, assets must be delivered securely
		 * @return {@code this}
		 * @since 1.2
		 * @see Placement#secure()
		 */
		@ReturnThis
		public Builder secure(boolean secure) {
			this.target().toggleFlag(FLAG_SECURE, secure);
			return this;
		}

		/**
		 * Set whether inline creative markup is supported.
		 *
		 * @param supp {@code true} if, and only if, inline creative markup is supported
		 * @return {@code this}
		 * @since 1.2
		 * @see Placement#supportsInlineMarkup()
		 */
		@ReturnThis
		public Builder supportsInlineMarkup(boolean supp) {
			this.target().toggleFlag(FLAG_ADMX, supp);
			return this;
		}

		/**
		 * Set whether creative markup loading through a URL is supported or not.
		 *
		 * @param supp {@code true} if, and only if, creative markup URLs are supported
		 * @return {@code this}
		 * @since 1.2
		 * @see Placement#supportsMarkupUrl()
		 */
		@ReturnThis
		public Builder supportsMarkupUrl(boolean supp) {
			this.target().toggleFlag(FLAG_CURLX, supp);
			return this;
		}

		/**
		 * Construct resulting placement instance.
		 *
		 * @return placement instance
		 * @since 1.2
		 */
		public Placement build() {
			this.needClone = true;
			return this.placement;
		}
	}

	/**
	 * Default empty placement instance.
	 *
	 * @return empty placement instance
	 * @since 1.2
	 */
	public static Placement of() {
		return DEFAULT;
	}

	/**
	 * Construct new empty {@linkplain Builder builder}.
	 *
	 * @return builder instance
	 * @since 1.2
	 */
	public static Builder ofBuilder() {
		return DEFAULT.toBuilder();
	}

	/**
	 * Deserialize placement from Protobuf message.
	 *
	 * @param reader reader to deserialize from
	 * @return deserialized placement
	 * @throws RuntimeException coding is malformed
	 * @since 1.2
	 */
	public static Placement ofProtobuf(ProtobufReader reader) {
		Placement plcmt = new Placement();

		while (reader.hasRemaining()) {
			int tag = reader.readTag();

			if (tag == TAGID)
				plcmt.id = reader.readString();
			else if (tag == SECURE && reader.readBool())
				plcmt.flags |= FLAG_SECURE;
			else if (tag == ADMX && reader.readBool())
				plcmt.flags |= FLAG_ADMX;
			else if (tag == CURLX && reader.readBool())
				plcmt.flags |= FLAG_CURLX;
			else if (tag == DISPLAY)
				plcmt.display = reader.readLen(DisplayAdFormat::ofProtobuf);
			else if (tag == VIDEO)
				plcmt.video = reader.readLen(PlaybackAdFormat::ofVideoAdProtobuf);
			else if (tag == AUDIO)
				plcmt.audio = reader.readLen(PlaybackAdFormat::ofAudioAdProtobuf);
		}
		return plcmt;
	}

	private String id;
	private DisplayAdFormat display;
	private PlaybackAdFormat video;
	private PlaybackAdFormat audio;
	private int flags;

	private Placement() {
		this.id = "";
		this.display = DisplayAdFormat.of();
		this.video = PlaybackAdFormat.ofVideoAd();
		this.audio = PlaybackAdFormat.ofAudioAd();
	}

	private Placement(Placement that) {
		this.id = that.id;
		this.display = that.display;
		this.video = that.video;
		this.audio = that.audio;
		this.flags = that.flags;
	}

	/**
	 * Placement identifier, unique to vendor.
	 *
	 * @return placement identifier
	 * @since 1.2
	 * @see Builder#id(String)
	 */
	public String id() {
		return this.id;
	}

	/**
	 * Format supported for display ad media.
	 *
	 * @return supported display format
	 * @since 1.2
	 * @see Builder#display(DisplayAdFormat)
	 */
	public DisplayAdFormat display() {
		return this.display;
	}

	/**
	 * Format supported for video ad media.
	 *
	 * @return supported video format
	 * @since 1.2
	 * @see Builder#video(PlaybackAdFormat)
	 * @see PlaybackAdFormat#isVideoAd()
	 */
	public PlaybackAdFormat video() {
		return this.video;
	}

	/**
	 * Format supported for audio ad media.
	 *
	 * @return supported audio format
	 * @since 1.2
	 * @see Builder#audio(PlaybackAdFormat)
	 * @see PlaybackAdFormat#isAudioAd()
	 */
	public PlaybackAdFormat audio() {
		return this.audio;
	}

	/**
	 * Set or clear flag.
	 *
	 * @param flag flag to set or clear
	 * @param on {@code true} or {@code false} to set or clear flag, respectively
	 */
	private void toggleFlag(int flag, boolean on) {
		if (on)
			this.flags |= flag;
		else
			this.flags &= ~flag;
	}

	/**
	 * Ad media assets must be delivered securely via HTTPS.
	 *
	 * @return {@code true} if, and only if, assets must be delivered securely
	 * @since 1.2
	 * @see Builder#secure(boolean)
	 */
	public boolean secure() {
		return (this.flags & FLAG_SECURE) != 0;
	}

	/**
	 * Inline creative markup is supported.
	 *
	 * @return {@code true} if, and only if, inline creative markup is supported
	 * @since 1.2
	 * @see Builder#supportsInlineMarkup(boolean)
	 */
	public boolean supportsInlineMarkup() {
		return (this.flags & FLAG_ADMX) != 0;
	}

	/**
	 * Creative markup loading through a URL is supported.
	 *
	 * @return {@code true} if, and only if, creative markup URLs are supported
	 * @since 1.2
	 * @see Builder#supportsMarkupUrl(boolean)
	 */
	public boolean supportsMarkupUrl() {
		return (this.flags & FLAG_CURLX) != 0;
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
		writer.writeString(TAGID, this.id);
		writer.writeBool(SECURE, this.secure());
		writer.writeBool(ADMX, this.supportsInlineMarkup());
		writer.writeBool(CURLX, this.supportsMarkupUrl());
		writer.writeLen(DISPLAY, this.display);
		writer.writeLen(VIDEO, this.video);
		writer.writeLen(AUDIO, this.audio);
	}
}
