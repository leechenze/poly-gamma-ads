// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.antifraud;

import static org.polygamma.android.origin.protobuf.ProtobufField.*;

/**
 * IVT check remote procedure (RPC) call argument tags.
 */
interface CheckArgumentTags {
	int ENCRYPTION_NONE		= 0;
	int ENCRYPTION_AES		= 1;

	@Tag int APP			= ofMessage(   1);
	@Tag int DEVICE			= ofMessage(   2);
	@Tag int ADCOMVER		= ofString(    3);
	@Tag int ENTROPY		= ofBytes(   100);
	@Tag int DIGEST			= ofString(  101);
	@Tag int TIMESTAMPSEC	= ofFixed64( 102);
	@Tag int ENTMACHINEENC	= ofInt32(   103);
	@Tag int ENTMACHINEKEY	= ofBytes(   104);
	@Tag int BOOTID			= ofString(  500);
	@Tag int BOOTCNT		= ofInt32(   501);
	@Tag int ADB			= ofBool(    502);
	@Tag int AIRMODE		= ofBool(    503);
	@Tag int AUTOTZ			= ofBool(    504);
	@Tag int ACCESSIB		= ofBool(    505);
	@Tag int PID			= ofInt32(   506);
	@Tag int UID			= ofInt32(   507);
	@Tag int BUILDTAGS		= ofString(  508);
	@Tag int BUILDFP		= ofString(  509);
	@Tag int BUILDPROD		= ofString(  510);
	@Tag int BUILDHW		= ofString(  511);
	@Tag int BUILDDISP		= ofString(  512);
	@Tag int BUILDRADIO		= ofString(  513);
}
