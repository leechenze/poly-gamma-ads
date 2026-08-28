// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.adcom.context;

import static org.polygamma.android.origin.protobuf.ProtobufField.*;

import androidx.annotation.CallSuper;
import androidx.annotation.RestrictTo;

import org.polygamma.android.origin.protobuf.ProtobufSerializable;
import org.polygamma.android.origin.protobuf.ProtobufWriter;

/**
 * Channel through which advertising media is distributed.
 *
 * @since 1.1
 * @see <a href="https://github.com/InteractiveAdvertisingBureau/AdCOM/blob/main/AdCOM%20v1.0%20FINAL.md#abstract_distributionchannel">AdCOM, version 1.0 - Object: DistributionChannel</a>
 */
public class DistributionChannel implements ProtobufSerializable {

	@RestrictTo(RestrictTo.Scope.SUBCLASSES)
	static final @Tag int ID			= ofString( 1);
	@RestrictTo(RestrictTo.Scope.SUBCLASSES)
	static final @Tag int NAME			= ofString( 2);
	@RestrictTo(RestrictTo.Scope.SUBCLASSES)
	static final @Tag int PUB			= ofMessage(3);
	/*static final @Tag int CONTENT		= ofMessage(4);*/
	/*static final @Tag int SITE		= ofMessage(5);*/
	@RestrictTo(RestrictTo.Scope.SUBCLASSES)
	static final @Tag int APP			= ofMessage(6);
	/*static final @Tag int DOOH		= ofMessage(7);*/

	@RestrictTo(RestrictTo.Scope.SUBCLASSES)
	static final @Tag int PUB_ID		= ofString(1);

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
	public void toProtobuf(ProtobufWriter writer) {
		writer.writeString(ID, this.id);
		writer.writeString(NAME, this.name);

		long cookie = writer.beginWriteLen(PUB);

		writer.writeString(PUB_ID, this.publisherId);
		writer.endWriteLen(cookie);
	}
}
