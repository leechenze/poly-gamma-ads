# SPDX-License-Identifier: MIT OR Apache-2.0

-repackageclasses org.polygamma.android.origin.protobuf

-assumevalues public class org.polygamma.android.origin.protobuf.Protobuf {
	static int MAX_VARINT32_SIZE return 5;
	static int MAX_VARINT64_SIZE return 10;
	static int varintSizeOfBits(int) return 0..10;

	public static int wireTypeOfFieldTag(int) return 0..5;
}

-assumevalues public class org.polygamma.android.origin.protobuf.ProtobufEncoder {
	private static int sizeOfVarint32(int) return 1..5;
	private static int sizeOfVarint64(long) return 1..10;
}
