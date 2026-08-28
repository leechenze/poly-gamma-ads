// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.adcom.media;

import static org.polygamma.android.origin.protobuf.ProtobufField.*;

import org.polygamma.android.origin.protobuf.ProtobufReader;
import org.polygamma.android.origin.protobuf.ProtobufSerializable;
import org.polygamma.android.origin.protobuf.ProtobufWriter;

/**
 * Playback companion ad media.
 *
 * @since 1.2
 */
public final class CompanionAd implements ProtobufSerializable {

	private static final @Tag int PLCMTID	= ofString( 1);
	private static final @Tag int DISPLAY	= ofMessage(2);
	private static final @Tag int VCM		= ofBool(   3);

	/**
	 * Construct new companion ad media.
	 *
	 * @param plcmtId id of placement companion is targeting
	 * @param display display media of companion
	 * @param endCard {@code true} if, and only if, companion is intended for end-card
	 * @return companion media instance
	 * @since 1.2
	 */
	public static CompanionAd of(String plcmtId, DisplayAd display, boolean endCard) {
		return new CompanionAd(plcmtId, display, endCard);
	}

	/**
	 * Deserialize companion ad media from Protobuf message.
	 *
	 * @param reader reader to deserialize from
	 * @return deserialized companion media instance
	 * @throws RuntimeException coding is malformed
	 * @since 1.2
	 */
	public static CompanionAd ofProtobuf(ProtobufReader reader) {
		String plcmtId = "";
		DisplayAd display = DisplayAd.ofDisplayAd();
		boolean vcm = false;

		while (reader.hasRemaining()) {
			int tag = reader.readTag();

			if (tag == PLCMTID)
				plcmtId = reader.readString();
			else if (tag == DISPLAY)
				display = reader.readLen(DisplayAd::ofDisplayAdProtobuf);
			else if (tag == VCM)
				vcm = reader.readBool();
		}
		return of(plcmtId, display, vcm);
	}

	private final String placementId;
	private final DisplayAd display;
	private final boolean endCard;

	private CompanionAd(String plcmtId, DisplayAd display, boolean endCard) {
		this.placementId = plcmtId;
		this.display = display;
		this.endCard = endCard;
	}

	/**
	 * Identifier of placement companion is intended for.
	 *
	 * @return placement id or {@linkplain String#isEmpty() empty} if undefined
	 * @since 1.2
	 */
	public String placementId() {
		return this.placementId;
	}

	/**
	 * Display media of companion.
	 *
	 * @return display media
	 * @since 1.2
	 */
	public DisplayAd display() {
		return this.display;
	}

	/**
	 * Media is intended for end-card.
	 *
	 * @return {@code true} if, and only if, end-card rendering
	 * @since 1.2
	 */
	public boolean endCard() {
		return this.endCard;
	}

	@Override
	public void toProtobuf(ProtobufWriter writer) {
		writer.writeString(PLCMTID, this.placementId);

		long cookie = writer.beginWriteLen(DISPLAY);

		this.display.toDisplayAdProtobuf(writer);
		writer.endWriteLen(cookie);

		writer.writeBool(VCM, this.endCard);
	}
}
