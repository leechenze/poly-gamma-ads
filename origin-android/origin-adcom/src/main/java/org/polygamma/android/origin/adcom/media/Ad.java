// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.adcom.media;

import static org.polygamma.android.origin.protobuf.ProtobufField.*;

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import org.polygamma.android.origin.protobuf.ProtobufReader;
import org.polygamma.android.origin.protobuf.ProtobufSerializable;
import org.polygamma.android.origin.protobuf.ProtobufWriter;
import org.polygamma.android.origin.util.CollectionsCompat;

import java.util.Collection;

/**
 * Root advertising media structure.
 *
 * @since 1.2
 * @see <a href="https://github.com/InteractiveAdvertisingBureau/AdCOM/blob/main/AdCOM%20v1.0%20FINAL.md#object--ad-">AdCOM, version 1.0 - Object: Ad</a>
 */
public class Ad implements ProtobufSerializable {

	private static final @Tag int ID			= ofString(       1);
	/*private static final @Tag int ADOMAIN		= ofString(       2);*/
	/*private static final @Tag int BUNDLE		= ofString(       3);*/
	/*private static final @Tag int IURL		= ofString(       4);*/
	/*private static final @Tag int CAT			= ofString(       5);*/
	/*private static final @Tag int CATTAX		= ofInt32(        6);*/
	/*private static final @Tag int LANG		= ofString(       7);*/
	/*private static final @Tag int ATTR		= ofPackedInt32(  8);*/
	private static final @Tag int SECURE		= ofBool(         9);
	/*private static final @Tag int MRATING		= ofInt32(       10);*/
	/*private static final @Tag int INIT		= ofFixed64(     11);*/
	/*private static final @Tag int LASTMOD		= ofFixed64(     12);*/
	private static final @Tag int DISPLAY		= ofMessage(     13);
	private static final @Tag int VIDEO			= ofMessage(     14);
	private static final @Tag int AUDIO			= ofMessage(     15);
	/*private static final @Tag int AUDIT		= ofMessage(     16);*/
	private static final @Tag int SERVEID		= ofString(     500);
	/*private static final @Tag int SERVERNAME	= ofString(     501);*/
	/*private static final @Tag int SERVERVER	= ofString(     502);*/

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
	 * @param reader reader to deserialize from
	 * @return deserialized ad
	 * @throws RuntimeException coding is malformed
	 * @since 1.2
	 */
	public static Ad ofProtobuf(ProtobufReader reader) {
		String id = "";
		String serveId = "";
		boolean secure = false;
		Ad rv = null;

		while (reader.hasRemaining()) {
			int tag = reader.readTag();

			if (tag == ID) {
				id = reader.readString();
			} else if (tag == SERVEID) {
				serveId = reader.readString();
			} else if (tag == SECURE) {
				secure = reader.readBool();
			} else if (tag == AUDIO || tag == DISPLAY || tag == VIDEO) {
				int cookie = reader.beginReadLen();

				rv =
					tag == DISPLAY ? DisplayAd.ofDisplayAdProtobuf(reader) :
					tag == AUDIO ? PlaybackAd.ofAudioAdProtobuf(reader) :
					PlaybackAd.ofVideoAdProtobuf(reader);
				reader.endReadLen(cookie);
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
	public final void toProtobuf(ProtobufWriter writer) {
		writer.writeString(ID, this.id);
		writer.writeString(SERVEID, this.serveId);
		writer.writeBool(SECURE, this.secure);

		if (this instanceof DisplayAd) {
			long cookie = writer.beginWriteLen(DISPLAY);

			((DisplayAd) this).toDisplayAdProtobuf(writer);
			writer.endWriteLen(cookie);
		} else if (this instanceof PlaybackAd) {
			long cookie = writer.beginWriteLen(((PlaybackAd) this).isAudioAd() ? AUDIO : VIDEO);

			((PlaybackAd) this).toAudioOrVideoAdProtobuf(writer);
			writer.endWriteLen(cookie);
		}
	}
}
