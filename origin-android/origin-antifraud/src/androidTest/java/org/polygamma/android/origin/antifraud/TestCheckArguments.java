// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.antifraud;

import static org.polygamma.android.origin.antifraud.CheckArgumentTags.*;

import org.polygamma.android.origin.adcom.context.App;
import org.polygamma.android.origin.adcom.context.Device;
import org.polygamma.android.origin.protobuf.ProtobufReader;

final class TestCheckArguments {

	static TestCheckArguments ofProtobuf(ProtobufReader reader) {
		TestCheckArguments rv = new TestCheckArguments();

		while (reader.hasRemaining()) {
			int tag = reader.readTag();

			if (tag == APP)
				rv.app = reader.readLen(App::ofProtobuf);
			else if (tag == DEVICE)
				rv.device = reader.readLen(Device::ofProtobuf);
			else if (tag == ADCOMVER)
				rv.adComVersion = reader.readString();
			else if (tag == ENTROPY)
				rv.entropy = reader.readBytes();
			else if (tag == DIGEST)
				rv.digest = reader.readString();
			else if (tag == TIMESTAMPSEC)
				rv.timestampSeconds = reader.readFixed64();
			else if (tag == ENTMACHINEENC)
				rv.entropyMachineEncryptionMode = reader.readInt32();
			else if (tag == ENTMACHINEKEY)
				rv.entropyMachineKey = reader.readBytes();
			else if (tag == BOOTID)
				rv.bootId = reader.readString();
			else if (tag == BOOTCNT)
				rv.bootCount = reader.readInt32();
			else if (tag == ADB)
				rv.adb = reader.readBool();
			else if (tag == AIRMODE)
				rv.airplaneMode = reader.readBool();
			else if (tag == AUTOTZ)
				rv.autoTimeZone = reader.readBool();
			else if (tag == ACCESSIB)
				rv.accessibility = reader.readBool();
			else if (tag == PID)
				rv.processId = reader.readInt32();
			else if (tag == UID)
				rv.userId = reader.readInt32();
			else if (tag == BUILDTAGS)
				rv.buildTags = reader.readString();
			else if (tag == BUILDFP)
				rv.buildFingerprint = reader.readString();
			else if (tag == BUILDPROD)
				rv.buildProduct = reader.readString();
			else if (tag == BUILDHW)
				rv.buildHardware = reader.readString();
			else if (tag == BUILDDISP)
				rv.buildDisplay = reader.readString();
			else if (tag == BUILDRADIO)
				rv.buildRadio = reader.readString();
		}
		return rv;
	}

	App app = App.of();
	Device device = Device.of();
	String adComVersion = "";
	byte[] entropy = new byte[0];
	String digest = "";
	long timestampSeconds = 0;
	int entropyMachineEncryptionMode = CheckArgumentTags.ENCRYPTION_NONE;
	byte[] entropyMachineKey = new byte[0];
	String bootId = "";
	int bootCount = 0;
	boolean adb = false;
	boolean airplaneMode = false;
	boolean autoTimeZone = false;
	boolean accessibility = false;
	int processId = 0;
	int userId = 0;
	String buildTags = "";
	String buildFingerprint = "";
	String buildProduct = "";
	String buildHardware = "";
	String buildDisplay = "";
	String buildRadio = "";
}
