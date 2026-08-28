// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.core;

import android.os.Build;
import android.text.TextUtils;
import android.util.SparseArray;
import android.util.SparseBooleanArray;

import androidx.annotation.IntDef;

import org.polygamma.android.origin.util.Preconditions;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Utilities for querying DNS records.
 */
@SuppressWarnings("RedundantCast")
class Dns {

	/** Client query request. */
	static final @MessageType int QueryMessageType			= 0;
	/** Server record response. */
	static final @MessageType int ResponseMessageType		= 1;

	/** Query request. */
	static final @Opcode int QueryOpcode					= 0;
	/** Status message. */
	static final @Opcode int StatusOpcode					= 2;
	/** Change notification message. */
	static final @Opcode int NotifyOpcode					= 4;
	/** Update message. */
	static final @Opcode int UpdateOpcode					= 5;

	/** No error. */
	static final @ResponseCode int NoErrorResponseCode		=  0;
	/** Format error. */
	static final @ResponseCode int FormErrResponseCode		=  1;
	/** Server failure. */
	static final @ResponseCode int ServFailResponseCode		=  2;
	/** Non-existent domain. */
	static final @ResponseCode int NxDomainResponseCode		=  3;
	/** Not implemented. */
	static final @ResponseCode int NotImpResponseCode		=  4;
	/** Query refused. */
	static final @ResponseCode int RefusedResponseCode		=  5;
	/** Name exists when it should not. */
	static final @ResponseCode int YxDomainResponseCode		=  6;
	/** RR Set exists when it should not. */
	static final @ResponseCode int YxRrSetResponseCode		=  7;
	/** RR Set does not exist when it should. */
	static final @ResponseCode int NxRrSetResponseCode		=  8;
	/** Server not authoritative for zone. */
	static final @ResponseCode int NotAuthResponseCode		=  9;
	/** Name not contained in zone. */
	static final @ResponseCode int NotZoneResponseCode		= 10;
	/** Bad OPT version. */
	static final @ResponseCode int BadVersResponseCode		= 16;
	/** Key not recognized. */
	static final @ResponseCode int BadKeyResponseCode		= 17;
	/** Signature out of time window. */
	static final @ResponseCode int BadTimeResponseCode		= 18;
	/** Bad TKEY mode. */
	static final @ResponseCode int BadModeResponseCode		= 19;
	/** Duplicate key name. */
	static final @ResponseCode int BadNameResponseCode		= 20;
	/** Algorithm not supported. */
	static final @ResponseCode int BadAlgResponseCode		= 21;
	/** Bad truncation. */
	static final @ResponseCode int BadTruncResponseCode		= 22;
	/** Bad or missing server cookie. */
	static final @ResponseCode int BadCookieResponseCode	= 23;

	/** {@code A} record. */
	static final @RecordType int ARecordType				=   1;
	/** {@code AAAA} record. */
	static final @RecordType int AaaaRecordType				=  28;
	/** {@code ANY} record. */
	static final @RecordType int AnyRecordType				= 255;
	/** {@code IXFR} record. */
	static final @RecordType int IxFrRecordType				= 251;
	/** {@code AXFR} record. */
	static final @RecordType int AxFrRecordType				= 252;
	/** {@code CAA} record. */
	static final @RecordType int CaaRecordType				= 257;
	/** {@code CDS} record. */
	static final @RecordType int CdsRecordType				=  59;
	/** {@code CDNSKEY} record. */
	static final @RecordType int CdnsKeyRecordType			=  60;
	/** {@code CERT} record. */
	static final @RecordType int CertRecordType				=  37;
	/** {@code CNAME} record. */
	static final @RecordType int CnameRecordType			=   5;
	/** {@code CSYNC} record. */
	static final @RecordType int CsyncRecordType			=  62;
	/** {@code DNSKEY} record. */
	static final @RecordType int DnsKeyRecordType			=  48;
	/** {@code DS} record. */
	static final @RecordType int DsRecordType				=  43;
	/** {@code HINFO} record. */
	static final @RecordType int HinfoRecordType			=  13;
	/** {@code HTTPS} record. */
	static final @RecordType int HttpsRecordType			=  65;
	/** {@code KEY} record. */
	static final @RecordType int KeyRecordType				=  25;
	/** {@code MX} record. */
	static final @RecordType int MxRecordType				=  15;
	/** {@code NAPTR} record. */
	static final @RecordType int NaPtrRecordType			=  35;
	/** {@code NS} record. */
	static final @RecordType int NsRecordType				=   2;
	/** {@code NSEC} record. */
	static final @RecordType int NsecRecordType				=  47;
	/** {@code NSEC3} record. */
	static final @RecordType int Nsec3RecordType			=  50;
	/** {@code NSEC3PARAM} record. */
	static final @RecordType int Nsec3ParamRecordType		=  51;
	/** {@code NULL} record. */
	static final @RecordType int NullRecordType				=  10;
	/** {@code OPENPGPKEY} record. */
	static final @RecordType int OpenPgpKeyRecordType		=  61;
	/** {@code OPT} record. */
	static final @RecordType int OptRecordType				=  41;
	/** {@code PTR} record. */
	static final @RecordType int PtrRecordType				=  12;
	/** {@code RRSIG} record. */
	static final @RecordType int RrSigRecordType			=  46;
	/** {@code SIG} record. */
	static final @RecordType int SigRecordType				=  24;
	/** {@code SOA} record. */
	static final @RecordType int SoaRecordType				=   6;
	/** {@code SRV} record. */
	static final @RecordType int SrvRecordType				=  33;
	/** {@code SSHFP} record. */
	static final @RecordType int SshFpRecordType			=  44;
	/** {@code SVCB} record. */
	static final @RecordType int SvcbRecordType				=  64;
	/** {@code TLSA} record. */
	static final @RecordType int TlsaRecordType				=  52;
	/** {@code TSIG} record. */
	static final @RecordType int TsigRecordType				= 250;
	/** {@code TXT} record. */
	static final @RecordType int TxtRecordType				=  16;
	/** {@code ZERO} record. */
	static final @RecordType int ZeroRecordType				=   0;

	/** Internet. */
	static final @DnsClass int InDnsClass					=   1;
	/** Chaos. */
	static final @DnsClass int ChDnsClass					=   3;
	/** Hesiod. */
	static final @DnsClass int HsDnsClass					=   4;
	/** None. */
	static final @DnsClass int NoneDnsClass					= 254;
	/** Any. */
	static final @DnsClass int AnyDnsClass					= 255;

	/** Mandatory keys in RR. */
	static final @SvcParamKey int MandatorySvcParamKey		= 0;
	/** Additional supported protocols. */
	static final @SvcParamKey int AlpnSvcParamKey			= 1;
	/** No support for default protocol. */
	static final @SvcParamKey int NoDefaultAlpnSvcParamKey	= 2;
	/** Port for alternative endpoint. */
	static final @SvcParamKey int PortSvcParamKey			= 3;
	/** IPv4 address hint. */
	static final @SvcParamKey int Ipv4HintSvcParamKey		= 4;
	/** Encrypted Client Hello configuration list. */
	static final @SvcParamKey int EchConfigListSvcParamKey	= 5;
	/** IPv6 address hint. */
	static final @SvcParamKey int Ipv6HintSvcParamKey		= 6;

	/**
	 * Message type enumeration value marker.
	 */
	@Retention(RetentionPolicy.SOURCE)
	@Target(ElementType.TYPE_USE)
	@IntDef({ QueryMessageType, ResponseMessageType })
	@interface MessageType {
	}

	/**
	 * Message operation code enumeration value marker.
	 */
	@Retention(RetentionPolicy.SOURCE)
	@Target(ElementType.TYPE_USE)
	@IntDef(open = true, value = {
		QueryOpcode,
		StatusOpcode,
		NotifyOpcode,
		UpdateOpcode
	})
	@interface Opcode {
	}

	/**
	 * Message response code enumeration value marker.
	 */
	@Retention(RetentionPolicy.SOURCE)
	@Target(ElementType.TYPE_USE)
	@IntDef(open = true, value = {
		NoErrorResponseCode,
		FormErrResponseCode,
		ServFailResponseCode,
		NxDomainResponseCode,
		NotImpResponseCode,
		RefusedResponseCode,
		YxDomainResponseCode,
		YxRrSetResponseCode,
		NxRrSetResponseCode,
		NotAuthResponseCode,
		NotZoneResponseCode,
		BadVersResponseCode,
		BadKeyResponseCode,
		BadTimeResponseCode,
		BadModeResponseCode,
		BadNameResponseCode,
		BadAlgResponseCode,
		BadTruncResponseCode,
		BadCookieResponseCode
	})
	@interface ResponseCode {
	}

	/**
	 * DNS record type enumeration value marker.
	 */
	@Retention(RetentionPolicy.SOURCE)
	@Target(ElementType.TYPE_USE)
	@IntDef(open = true, value = {
		ARecordType,
		AaaaRecordType,
		AnyRecordType,
		IxFrRecordType,
		AxFrRecordType,
		CaaRecordType,
		CdsRecordType,
		CdnsKeyRecordType,
		CertRecordType,
		CnameRecordType,
		CsyncRecordType,
		DnsKeyRecordType,
		DsRecordType,
		HinfoRecordType,
		HttpsRecordType,
		KeyRecordType,
		MxRecordType,
		NaPtrRecordType,
		NsRecordType,
		NsecRecordType,
		Nsec3RecordType,
		Nsec3ParamRecordType,
		NullRecordType,
		OpenPgpKeyRecordType,
		OptRecordType,
		PtrRecordType,
		RrSigRecordType,
		SigRecordType,
		SoaRecordType,
		SrvRecordType,
		SshFpRecordType,
		SvcbRecordType,
		TlsaRecordType,
		TsigRecordType,
		TxtRecordType,
		ZeroRecordType
	})
	@interface RecordType {
	}

	/**
	 * DNS record class enumeration value marker.
	 */
	@Retention(RetentionPolicy.SOURCE)
	@Target(ElementType.TYPE_USE)
	@IntDef(open = true, value = {
		InDnsClass,
		ChDnsClass,
		HsDnsClass,
		NoneDnsClass,
		AnyDnsClass
	})
	@interface DnsClass {
	}

	/**
	 * Service record parameter key enumeration value marker.
	 */
	@Retention(RetentionPolicy.SOURCE)
	@Target(ElementType.TYPE_USE)
	@IntDef(open = true, value = {
		MandatorySvcParamKey,
		AlpnSvcParamKey,
		NoDefaultAlpnSvcParamKey,
		PortSvcParamKey,
		Ipv4HintSvcParamKey,
		EchConfigListSvcParamKey,
		Ipv6HintSvcParamKey
	})
	@interface SvcParamKey {
	}

	/**
	 * Message header.
	 */
	static final class Header {
		int id;
		@MessageType int messageType;
		@Opcode int opcode;
		boolean authoritative;
		boolean truncation;
		boolean recursionDesired;
		boolean recursionAvailable;
		boolean authenticData;
		boolean checkingDisabled;
		@ResponseCode int responseCode;
		int queryCount;
		int answerCount;
		int authorityCount;
		int additionalCount;

		/**
		 * Construct empty header.
		 */
		Header() {
		}
	}

	/**
	 * DNS query.
	 */
	static final class Query {
		String name;
		@RecordType int recordType;
		@DnsClass int dnsClass;
		boolean mdnsUnicastResponse;

		/**
		 * Construct empty query.
		 */
		Query() {
			this.name = "";
		}
	}

	/**
	 * DNS record.
	 */
	static final class Record {
		String name;
		@RecordType int type;
		@DnsClass int dnsClass;
		@SuppressWarnings("unused")
		boolean mdnsCacheFlush;
		int timeToLive;
		byte[] data;

		/**
		 * Construct empty record.
		 */
		Record() {
			this.name = "";
		}
	}

	/**
	 * DNS message.
	 */
	static final class Message {
		Header header;
		Query[] queries;
		Record[] answers;
		Record[] authorities;
		Record[] additionals;

		/**
		 * Construct empty message.
		 */
		Message() {
		}
	}

	/**
	 * {@code SVCB} and {@code HTTPS} resource record.
	 */
	static final class Svcb {
		int priority;
		String targetName;
		final SparseArray<Object> parameters;

		/**
		 * Construct empty message.
		 */
		Svcb() {
			this.targetName = "";
			this.parameters = new SparseArray<>();
		}
	}

	/**
	 * Construct view of a buffer's subregion.
	 *
	 * @param src buffer to construct view of
	 * @param pos position within buffer view should begin at (inclusive)
	 * @param len length, in bytes, of view
	 * @return resulting view
	 */
	private static ByteBuffer slice(ByteBuffer src, int pos, int len) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM)
			return src.slice(pos, len);

		src = src.duplicate();
		src.position(pos)
			.limit(pos + len);
		return src.slice();
	}

	/**
	 * Decode length-delimited string.
	 *
	 * @param msg buffer to decode from
	 * @return decoded string
	 */
	private static String decodeString(ByteBuffer msg) {
		int len = msg.get() & 0xff;
		ByteBuffer str = slice(msg, msg.position(), len);

		msg.position(msg.position() + len);
		return StandardCharsets.UTF_8.decode(str).toString();
	}

	/**
	 * Decode name.
	 *
	 * @param msg message to decode name from
	 * @return decoded name
	 * @throws IllegalStateException coding is malformed
	 */
	private static String decodeName(ByteBuffer msg) {
		int startPos = msg.position();
		List<String> labels = new ArrayList<>(3);

		while (true) {
			int val = msg.get(msg.position()) & 0xff;

			if (val == 0) {
				// Section 3.1
				msg.get();
				labels.add("");
				break;
			} else if ((val & 0xc0) == 0xc0) {
				// 4.1.4. Message Compression
				labels.add(decodeName(slice(msg, msg.getShort() & 0x3fff, startPos)));
				break;
			}

			Preconditions.checkState((val & 0xc0) == 0);

			String label = decodeString(msg);

			Preconditions.checkState(label.length() < 64);
			labels.add(label);
		}

		String name = TextUtils.join(".", labels);

		Preconditions.checkState(name.length() < 255);
		return name.isEmpty() && !labels.isEmpty() ? "." : name;
	}

	/**
	 * Decode record.
	 *
	 * @param msg buffer to decode from
	 * @return decoded record
	 * @throws IllegalStateException coding is malformed
	 */
	private static Record decodeRecord(ByteBuffer msg) {
		Record rv = new Record();

		rv.name = decodeName(msg);
		rv.type = (@RecordType int) (msg.getShort() & 0xffff);
		rv.dnsClass = (@DnsClass int) (msg.getShort() & 0xffff);
		if (rv.type != OptRecordType && (rv.dnsClass & 0x8000) != 0) {
			rv.dnsClass = (@DnsClass int) (rv.dnsClass & ~0x8000);
			rv.mdnsCacheFlush = true;
		}

		rv.timeToLive = msg.getInt();
		rv.data = new byte[msg.getShort() & 0xffff];
		msg.get(rv.data);
		return rv;
	}

	/**
	 * Decode record query.
	 *
	 * @param msg buffer to decode from
	 * @return decoded query
	 * @throws IllegalStateException coding is malformed
	 */
	private static Query decodeQuery(ByteBuffer msg) {
		Query rv = new Query();

		rv.name = decodeName(msg);
		rv.recordType = (@RecordType int) (msg.getShort() & 0xffff);
		rv.dnsClass = (@DnsClass int) (msg.getShort() & 0xffff);
		rv.mdnsUnicastResponse = (rv.dnsClass & 0x8000) != 0;
		if (rv.mdnsUnicastResponse)
			rv.dnsClass = (@DnsClass int) (rv.dnsClass & ~0x8000);
		return rv;
	}

	/**
	 * Decode message header.
	 *
	 * @param msg buffer to decode from
	 * @return decoded header
	 */
	private static Header decodeHeader(ByteBuffer msg) {
		Header rv = new Header();

		rv.id = msg.getShort() & 0xffff;

		int bits16 = msg.get() & 0xff;
		int bits24 = msg.get() & 0xff;

		rv.messageType = (bits16 & 0x80) == 0x80 ? ResponseMessageType : QueryMessageType;
		rv.opcode = (bits16 & 0x78) >>> 3;
		rv.authoritative = (bits16 & 0x4) == 0x4;
		rv.truncation = (bits16 & 0x2) == 0x2;
		rv.recursionDesired = (bits16 & 0x1) == 0x1;

		rv.recursionAvailable = (bits24 & 0x80) == 0x80;
		rv.authenticData = (bits24 & 0x20) == 0x20;
		rv.checkingDisabled = (bits24 & 0x10) == 0x10;
		rv.responseCode = (@ResponseCode int) (bits24 & 0xf);

		rv.queryCount = msg.getShort() & 0xffff;
		rv.answerCount = msg.getShort() & 0xffff;
		rv.authorityCount = msg.getShort() & 0xffff;
		rv.additionalCount = msg.getShort() & 0xffff;
		return rv;
	}

	/**
	 * Decode DNS message.
	 *
	 * @param msg buffer to decode from
	 * @return decoded message
	 * @throws IllegalStateException coding is malformed
	 */
	static Message decodeMessage(ByteBuffer msg) {
		Message rv = new Message();

		rv.header = decodeHeader(msg);
		rv.queries = new Query[rv.header.queryCount];
		rv.answers = new Record[rv.header.answerCount];
		rv.authorities = new Record[rv.header.authorityCount];
		rv.additionals = new Record[rv.header.additionalCount];

		for (int i = 0; i < rv.queries.length; i++)
			rv.queries[i] = decodeQuery(msg);
		for (int i = 0; i < rv.answers.length; i++)
			rv.answers[i] = decodeRecord(msg);
		for (int i = 0; i < rv.authorities.length; i++)
			rv.authorities[i] = decodeRecord(msg);
		for (int i = 0; i < rv.additionals.length; i++)
			rv.additionals[i] = decodeRecord(msg);
		return rv;
	}

	/**
	 * Decode {@code SVCB} record resource.
	 *
	 * @param msg buffer to decode from
	 * @return decoded record reference
	 * @throws IllegalStateException coding is malformed
	 */
	static Svcb decodeSvcb(ByteBuffer msg) {
		Svcb rv = new Svcb();

		rv.priority = msg.getShort() & 0xffff;
		rv.targetName = decodeName(msg);

		while (msg.remaining() >= 4) {
			int key = msg.getShort() & 0xffff;
			int len = msg.getShort() & 0xffff;
			ByteBuffer valBuff = slice(msg, msg.position(), len);
			Object val;

			msg.position(msg.position() + len);
			if (key == MandatorySvcParamKey) {
				SparseBooleanArray keys = new SparseBooleanArray();

				while (valBuff.hasRemaining())
					keys.put(valBuff.getShort() & 0xffff, true);
				val = keys;
			} else if (key == AlpnSvcParamKey) {
				List<String> alpn = new ArrayList<>(1);

				while (valBuff.hasRemaining())
					alpn.add(decodeString(valBuff));
				val = alpn.toArray(new String[0]);
			} else if (key == NoDefaultAlpnSvcParamKey) {
				val = true;
			} else if (key == PortSvcParamKey) {
				val = valBuff.getShort() & 0xffff;
			} else if (key == Ipv4HintSvcParamKey || key == Ipv6HintSvcParamKey) {
				List<Object> addrs = new ArrayList<>(1);

				while (valBuff.hasRemaining()) {
					byte[] name = new byte[key == Ipv4HintSvcParamKey ? 4 : 16];

					valBuff.get(name);
					try {
						addrs.add(InetAddress.getByAddress(name));
					} catch (UnknownHostException err) {
						throw new IllegalStateException(err);
					}
				}
				//noinspection SuspiciousToArrayCall
				val =
					addrs.toArray(
						key == Ipv4HintSvcParamKey ? new Inet4Address[0] :
						new Inet6Address[0]
					);
			} else {
				byte[] data = new byte[len];

				valBuff.get(data);
				val = data;
			}
			rv.parameters.put(key, val);
		}
		return rv;
	}

	private Dns() {
	}
}
