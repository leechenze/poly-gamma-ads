// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.adcom.media;

import static org.polygamma.android.origin.protobuf.Protobuf.WIRE_LEN;
import static org.polygamma.android.origin.protobuf.Protobuf.WIRE_VARINT;
import static org.polygamma.android.origin.protobuf.Protobuf.fieldTagOf;

import org.polygamma.android.origin.protobuf.Protobuf.FieldTag;
import org.polygamma.android.origin.protobuf.ProtobufDecoder;
import org.polygamma.android.origin.protobuf.ProtobufEncoder;
import org.polygamma.android.origin.protobuf.ProtobufSerializable;

/**
 * Playback companion ad media.
 *
 * @since 1.2
 */
public final class CompanionAd implements ProtobufSerializable {

	private static final @FieldTag int PLCMTID	= fieldTagOf(1, WIRE_LEN);
	private static final @FieldTag int DISPLAY	= fieldTagOf(2, WIRE_LEN);
	private static final @FieldTag int VCM		= fieldTagOf(3, WIRE_VARINT);

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
	 * @param dec decoder to deserialize from
	 * @return deserialized companion media instance
	 * @throws RuntimeException coding is malformed
	 * @since 1.2
	 */
	public static CompanionAd ofProtobuf(ProtobufDecoder dec) {
		String plcmtId = "";
		DisplayAd display = DisplayAd.ofDisplayAd();
		boolean vcm = false;

		while (dec.hasRemaining()) {
			int tag = dec.decodeFieldTag();

			if (tag == PLCMTID)
				plcmtId = dec.decodeString();
			else if (tag == DISPLAY)
				display = dec.decodeLen(DisplayAd::ofDisplayAdProtobuf);
			else if (tag == VCM)
				vcm = dec.decodeBool();
			else
				dec.skipFieldValue(tag);
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
	public void toProtobuf(ProtobufEncoder enc) {
		enc.encodeStringField(PLCMTID, this.placementId)
			.encodeBoolField(VCM, this.endCard)
			.encodeLenField(DISPLAY, this.display, DisplayAd::toDisplayAdProtobuf);
	}
}
