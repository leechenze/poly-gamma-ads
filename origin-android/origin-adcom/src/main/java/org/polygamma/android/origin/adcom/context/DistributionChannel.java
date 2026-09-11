// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.adcom.context;

import static org.polygamma.android.origin.protobuf.Protobuf.*;

import androidx.annotation.CallSuper;
import androidx.annotation.RestrictTo;

import org.polygamma.android.origin.protobuf.ProtobufDecoder;
import org.polygamma.android.origin.protobuf.ProtobufEncoder;
import org.polygamma.android.origin.protobuf.ProtobufSerializable;

/**
 * Channel through which advertising media is distributed.
 *
 * @since 1.1
 * @see <a href="https://github.com/InteractiveAdvertisingBureau/AdCOM/blob/main/AdCOM%20v1.0%20FINAL.md#abstract_distributionchannel">AdCOM, version 1.0 - Object: DistributionChannel</a>
 */
public class DistributionChannel implements ProtobufSerializable {

	@RestrictTo(RestrictTo.Scope.SUBCLASSES)
	static final @FieldTag int ID			= fieldTagOf(1, WIRE_LEN);
	@RestrictTo(RestrictTo.Scope.SUBCLASSES)
	static final @FieldTag int NAME			= fieldTagOf(2, WIRE_LEN);
	@RestrictTo(RestrictTo.Scope.SUBCLASSES)
	static final @FieldTag int PUB			= fieldTagOf(3, WIRE_LEN);
	/*static final @FieldTag int CONTENT	= fieldTagOf(4, WIRE_LEN);*/
	/*static final @FieldTag int SITE		= fieldTagOf(5, WIRE_LEN);*/
	@RestrictTo(RestrictTo.Scope.SUBCLASSES)
	static final @FieldTag int APP			= fieldTagOf(6, WIRE_LEN);
	/*static final @FieldTag int DOOH		= fieldTagOf(7, WIRE_LEN);*/

	@RestrictTo(RestrictTo.Scope.SUBCLASSES)
	static final @FieldTag int PUB_ID		= fieldTagOf(1, WIRE_LEN);

	static void
	decodeProtobufField(DistributionChannel dst, ProtobufDecoder dec, @FieldTag int tag) {
		if (tag == ID) {
			dst.id = dec.decodeString();
		} else if (tag == NAME) {
			dst.name = dec.decodeString();
		} else if (tag == PUB) {
			dst.publisherId = dec.decodeLen(pub -> {
				String id = "";

				while (pub.hasRemaining()) {
					int pubTag = pub.decodeFieldTag();

					if (pubTag == PUB_ID)
						id = pub.decodeString();
					else
						pub.skipFieldValue(pubTag);
				}
				return id;
			});
		} else {
			dec.skipFieldValue(tag);
		}
	}

	@RestrictTo(RestrictTo.Scope.SUBCLASSES)
	String id;
	@RestrictTo(RestrictTo.Scope.SUBCLASSES)
	String name;
	@RestrictTo(RestrictTo.Scope.SUBCLASSES)
	String publisherId;

	/**
	 * Construct new empty distribution channel.
	 */
	DistributionChannel() {
		this.id = "";
		this.name = "";
		this.publisherId = "";
	}

	/**
	 * Construct new distribution channel, copying from another.
	 *
	 * @param that channel to copy from
	 */
	DistributionChannel(DistributionChannel that) {
		this.id = that.id;
		this.name = that.name;
		this.publisherId = that.publisherId;
	}

	/**
	 * Channel identifier, unique to vendor.
	 *
	 * @return identifier
	 * @since 1.1
	 */
	public final String id() {
		return this.id;
	}

	/**
	 * Channel name.
	 *
	 * @return name
	 * @since 1.1
	 */
	public final String name() {
		return this.name;
	}

	/**
	 * Distribution channel publisher identifier, unique to vendor.
	 *
	 * @return identifier
	 * @since 1.1
	 */
	public final String publisherId() {
		return this.publisherId;
	}

	@Override
	@CallSuper
	public void toProtobuf(ProtobufEncoder enc) {
		enc.encodeStringField(ID, this.id)
			.encodeStringField(NAME, this.name)
			.encodeLenField(
				PUB, this.publisherId,
				(pubId, pubEnc) -> pubEnc.encodeStringField(PUB_ID, pubId)
			);
	}
}
