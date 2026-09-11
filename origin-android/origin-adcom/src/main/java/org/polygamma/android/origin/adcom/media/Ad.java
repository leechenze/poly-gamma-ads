// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.adcom.media;

import static org.polygamma.android.origin.protobuf.Protobuf.WIRE_LEN;
import static org.polygamma.android.origin.protobuf.Protobuf.WIRE_VARINT;
import static org.polygamma.android.origin.protobuf.Protobuf.fieldTagOf;

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import org.polygamma.android.origin.protobuf.Protobuf.FieldTag;
import org.polygamma.android.origin.protobuf.ProtobufDecoder;
import org.polygamma.android.origin.protobuf.ProtobufEncoder;
import org.polygamma.android.origin.protobuf.ProtobufSerializable;
import org.polygamma.android.origin.util.CollectionsCompat;

import java.util.Collection;

/**
 * Root advertising media structure.
 *
 * @since 1.2
 * @see <a href="https://github.com/InteractiveAdvertisingBureau/AdCOM/blob/main/AdCOM%20v1.0%20FINAL.md#object--ad-">AdCOM, version 1.0 - Object: Ad</a>
 */
public class Ad implements ProtobufSerializable {

	private static final @FieldTag int ID			= fieldTagOf(  1, WIRE_LEN);
	/*private static final @FieldTag int ADOMAIN	= fieldTagOf(  2, WIRE_LEN);*/
	/*private static final @FieldTag int BUNDLE		= fieldTagOf(  3, WIRE_LEN);*/
	/*private static final @FieldTag int IURL		= fieldTagOf(  4, WIRE_LEN);*/
	/*private static final @FieldTag int CAT		= fieldTagOf(  5, WIRE_LEN);*/
	/*private static final @FieldTag int CATTAX		= fieldTagOf(  6, WIRE_VARINT);*/
	/*private static final @FieldTag int LANG		= fieldTagOf(  7, WIRE_LEN);*/
	/*private static final @FieldTag int ATTR		= fieldTagOf(  8, WIRE_LEN);*/
	private static final @FieldTag int SECURE		= fieldTagOf(  9, WIRE_VARINT);
	/*private static final @FieldTag int MRATING	= fieldTagOf( 10, WIRE_VARINT);*/
	/*private static final @FieldTag int INIT		= fieldTagOf( 11, WIRE_FIXED64);*/
	/*private static final @FieldTag int LASTMOD	= fieldTagOf( 12, WIRE_FIXED64);*/
	private static final @FieldTag int DISPLAY		= fieldTagOf( 13, WIRE_LEN);
	private static final @FieldTag int VIDEO		= fieldTagOf( 14, WIRE_LEN);
	private static final @FieldTag int AUDIO		= fieldTagOf( 15, WIRE_LEN);
	/*private static final @FieldTag int AUDIT		= fieldTagOf( 16, WIRE_LEN);*/
	private static final @FieldTag int SERVEID		= fieldTagOf(500, WIRE_LEN);
	/*private static final @FieldTag int SERVERNAME	= fieldTagOf(501, WIRE_LEN);*/
	/*private static final @FieldTag int SERVERVER	= fieldTagOf(502, WIRE_LEN);*/

	private static final Ad DEFAULT = new Ad((Void) null);

	/**
	 * Default empty ad instance without any media type.
	 *
	 * @return empty ad instance
	 * @since 1.2
	 */
	public static Ad of() {
		return DEFAULT;
	}

	/**
	 * Deserialize ad from Protobuf message.
	 *
	 * @param dec decoder to deserialize from
	 * @return deserialized ad
	 * @throws RuntimeException coding is malformed
	 * @since 1.2
	 */
	public static Ad ofProtobuf(ProtobufDecoder dec) {
		String id = "";
		String serveId = "";
		boolean secure = false;
		Ad rv = null;

		while (dec.hasRemaining()) {
			int tag = dec.decodeFieldTag();

			if (tag == ID) {
				id = dec.decodeString();
			} else if (tag == SERVEID) {
				serveId = dec.decodeString();
			} else if (tag == SECURE) {
				secure = dec.decodeBool();
			} else if (tag == AUDIO) {
				rv = dec.decodeLen(PlaybackAd::ofAudioAdProtobuf);
			} else if (tag == DISPLAY) {
				rv = dec.decodeLen(DisplayAd::ofDisplayAdProtobuf);
			} else if (tag == VIDEO) {
				rv = dec.decodeLen(PlaybackAd::ofVideoAdProtobuf);
			} else {
				dec.skipFieldValue(tag);
			}
		}
		if (rv == null)
			rv = new Ad(DEFAULT);
		rv.setId(id);
		rv.setServeId(serveId);
		rv.setSecure(secure);
		return rv;
	}

	private String id;
	private String serveId;
	private AdEventTracker[] eventTrackers;
	private boolean secure;

	private Ad(@Nullable Void ignored) {
		this.id = "";
		this.serveId = "";
		this.eventTrackers = new AdEventTracker[0];
	}

	/**
	 * Construct new ad, copying from another.
	 *
	 * @param that ad to copy from
	 */
	@RestrictTo(RestrictTo.Scope.SUBCLASSES)
	Ad(Ad that) {
		this.id = that.id;
		this.serveId = that.serveId;
		this.eventTrackers = that.eventTrackers;
		this.secure = that.secure;
	}

	/**
	 * Construct new empty ad.
	 */
	@RestrictTo(RestrictTo.Scope.SUBCLASSES)
	Ad() {
		this(DEFAULT);
	}

	/**
	 * Ad identifier, unique to vendor.
	 *
	 * @return ad identifier
	 * @since 1.2
	 */
	public final String id() {
		return this.id;
	}

	/**
	 * Set ad identifier, unique to vendor.
	 *
	 * @param id ad identifier
	 */
	@RestrictTo(RestrictTo.Scope.SUBCLASSES)
	final void setId(String id) {
		this.id = id;
	}

	/**
	 * Ad serving identifier, unique to ad server.
	 *
	 * @return serving identifier
	 * @since 1.2
	 */
	public final String serveId() {
		return this.serveId;
	}

	/**
	 * Set ad serving identifier, unique to ad server.
	 *
	 * @param id serving identifier
	 */
	@RestrictTo(RestrictTo.Scope.SUBCLASSES)
	final void setServeId(String id) {
		this.serveId = id;
	}

	/**
	 * Event tracker array.
	 *
	 * @return event trackers
	 */
	final AdEventTracker[] eventTrackers() {
		return this.eventTrackers;
	}

	/**
	 * Count of trackers to execute for ad media related events.
	 *
	 * @return event tracker count
	 * @since 1.2
	 * @see #eventTracker(int)
	 */
	public final int eventTrackerCount() {
		return this.eventTrackers.length;
	}

	/**
	 * Tracker, at index, to execute for ad media event.
	 *
	 * @param i index to retrieve tracker at
	 * @return tracker at index {@code i}
	 * @throws IndexOutOfBoundsException {@code i} is negative or, greater than or equal to
	 * event tracker {@linkplain #eventTrackerCount() count}
	 * @since 1.2
	 * @see #eventTrackerCount()
	 */
	public final AdEventTracker eventTracker(int i) {
		return this.eventTrackers[i];
	}

	/**
	 * Set trackers to execute for ad media events.
	 *
	 * @param trkrs event trackers
	 */
	@RestrictTo(RestrictTo.Scope.SUBCLASSES)
	final void setEventTrackers(Collection<AdEventTracker> trkrs) {
		this.eventTrackers = CollectionsCompat.toArrayOrEmpty(trkrs, DEFAULT.eventTrackers);
	}

	/**
	 * Ad media assets are delivered securely via HTTPS.
	 *
	 * @return {@code true} if, and only if, ad media assets are delivered securely
	 * @since 1.2
	 */
	public final boolean secure() {
		return this.secure;
	}

	/**
	 * Set whether ad media assets are delivered securely via HTTPS.
	 *
	 * @param secure {@code true} if, and only if, ad media assets are delivered securely
	 */
	@RestrictTo(RestrictTo.Scope.SUBCLASSES)
	final void setSecure(boolean secure) {
		this.secure = secure;
	}

	@Override
	public final void toProtobuf(ProtobufEncoder enc) {
		enc.encodeStringField(ID, this.id)
			.encodeStringField(SERVEID, this.serveId)
			.encodeBoolField(SECURE, this.secure);

		if (this instanceof DisplayAd) {
			enc.encodeLenField(DISPLAY, (DisplayAd) this, DisplayAd::toDisplayAdProtobuf);
		} else if (this instanceof PlaybackAd) {
			enc.encodeLenField(
				((PlaybackAd) this).isAudioAd() ? AUDIO : VIDEO, (PlaybackAd) this,
				PlaybackAd::toAudioOrVideoAdProtobuf
			);
		}
	}
}
