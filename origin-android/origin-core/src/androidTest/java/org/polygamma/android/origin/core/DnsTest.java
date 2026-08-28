// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.core;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.os.Build;
import android.util.SparseBooleanArray;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * {@link Dns} tests.
 */
@RunWith(AndroidJUnit4.class)
// dnsjava is incompatible with lower SDK versions
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.N_MR1)
public class DnsTest {

	private static final List<org.xbill.DNS.Message> MESSAGE_TESTS = new ArrayList<>();

	private static void assertHeaderEquals(org.xbill.DNS.Header exp, Dns.Header got) {
		assertEquals(exp.getID(), got.id);
		assertEquals(
			exp.getFlag(org.xbill.DNS.Flags.QR) ? Dns.ResponseMessageType : Dns.QueryMessageType,
			got.messageType
		);
		assertEquals(exp.getOpcode(), got.opcode);
		assertEquals(exp.getFlag(org.xbill.DNS.Flags.AA), got.authoritative);
		assertEquals(exp.getFlag(org.xbill.DNS.Flags.TC), got.truncation);
		assertEquals(exp.getFlag(org.xbill.DNS.Flags.RD), got.recursionDesired);
		assertEquals(exp.getFlag(org.xbill.DNS.Flags.RA), got.recursionAvailable);
		assertEquals(exp.getFlag(org.xbill.DNS.Flags.AD), got.authenticData);
		assertEquals(exp.getFlag(org.xbill.DNS.Flags.CD), got.checkingDisabled);
		assertEquals(exp.getRcode(), got.responseCode);
		assertEquals(exp.getCount(org.xbill.DNS.Section.QUESTION), got.queryCount);
		assertEquals(exp.getCount(org.xbill.DNS.Section.ANSWER), got.answerCount);
		assertEquals(exp.getCount(org.xbill.DNS.Section.AUTHORITY), got.authorityCount);
		assertEquals(exp.getCount(org.xbill.DNS.Section.ADDITIONAL), got.additionalCount);
	}

	private static void assertQueryEquals(org.xbill.DNS.Record exp, Dns.Query got) {
		assertEquals(exp.getName().toString(), got.name);
		assertEquals(exp.getType(), got.recordType);
		assertEquals(exp.getDClass(), got.dnsClass);
	}

	private static void assertRecordEquals(org.xbill.DNS.Record exp, Dns.Record got) {
		assertEquals(exp.getName().toString(), got.name);
		assertEquals(exp.getType(), got.type);
		assertEquals(exp.getDClass(), got.dnsClass);
		assertEquals(exp.getTTL(), got.timeToLive);
		assertArrayEquals(exp.rdataToWireCanonical(), got.data);
	}

	private static void assertMessageEquals(org.xbill.DNS.Message exp, Dns.Message got) {
		assertHeaderEquals(exp.getHeader(), got.header);
		assertEquals(exp.getSection(org.xbill.DNS.Section.QUESTION).size(), got.queries.length);
		for (int i = 0; i < got.queries.length; i++) {
			assertQueryEquals(
				exp.getSection(org.xbill.DNS.Section.QUESTION).get(i),
				got.queries[i]
			);
		}

		assertEquals(exp.getSection(org.xbill.DNS.Section.ANSWER).size(), got.answers.length);
		for (int i = 0; i < got.answers.length; i++) {
			assertRecordEquals(
				exp.getSection(org.xbill.DNS.Section.ANSWER).get(i),
				got.answers[i]
			);
		}

		assertEquals(
			exp.getSection(org.xbill.DNS.Section.AUTHORITY).size(),
			got.authorities.length
		);
		for (int i = 0; i < got.authorities.length; i++) {
			assertRecordEquals(
				exp.getSection(org.xbill.DNS.Section.AUTHORITY).get(i),
				got.authorities[i]
			);
		}

		assertEquals(
			exp.getSection(org.xbill.DNS.Section.ADDITIONAL).size(),
			got.additionals.length
		);
		for (int i = 0; i < got.additionals.length; i++) {
			assertRecordEquals(
				exp.getSection(org.xbill.DNS.Section.ADDITIONAL).get(i),
				got.additionals[i]
			);
		}
	}

	@BeforeClass
	public static void init() throws org.xbill.DNS.TextParseException, UnknownHostException {
		org.xbill.DNS.Message exp;

		// Failed answer
		exp = new org.xbill.DNS.Message(1);
		exp.getHeader().setRcode(org.xbill.DNS.Rcode.NXDOMAIN);
		exp.getHeader().setOpcode(org.xbill.DNS.Opcode.QUERY);
		exp.addRecord(new org.xbill.DNS.HTTPSRecord(
			org.xbill.DNS.Name.fromConstantString("local.host."),
			1,
			0,
			0,
			org.xbill.DNS.Name.fromConstantString("local.host"),
			Collections.emptyList()
		), org.xbill.DNS.Section.QUESTION);
		MESSAGE_TESTS.add(exp);

		// Single answer
		exp = new org.xbill.DNS.Message(1);
		exp.getHeader().setRcode(org.xbill.DNS.Rcode.NOERROR);
		exp.getHeader().setOpcode(org.xbill.DNS.Opcode.QUERY);
		exp.addRecord(new org.xbill.DNS.HTTPSRecord(
			org.xbill.DNS.Name.fromConstantString("local.host."),
			1,
			0,
			0,
			org.xbill.DNS.Name.fromConstantString("local.host."),
			Collections.emptyList()
		), org.xbill.DNS.Section.QUESTION);
		exp.addRecord(new org.xbill.DNS.HTTPSRecord(
			org.xbill.DNS.Name.fromConstantString("local.host."),
			1,
			100,
			1,
			org.xbill.DNS.Name.fromConstantString("foo.local.host."),
			Collections.emptyList()
		), org.xbill.DNS.Section.ANSWER);
		MESSAGE_TESTS.add(exp);

		// Single answer, with parameters
		exp = new org.xbill.DNS.Message(1);
		exp.getHeader().setRcode(org.xbill.DNS.Rcode.NOERROR);
		exp.getHeader().setOpcode(org.xbill.DNS.Opcode.QUERY);
		exp.addRecord(new org.xbill.DNS.HTTPSRecord(
			org.xbill.DNS.Name.fromConstantString("local.host."),
			1,
			0,
			0,
			org.xbill.DNS.Name.fromConstantString("local.host."),
			Collections.emptyList()
		), org.xbill.DNS.Section.QUESTION);
		exp.addRecord(new org.xbill.DNS.HTTPSRecord(
			org.xbill.DNS.Name.fromConstantString("local.host."),
			1,
			100,
			1,
			org.xbill.DNS.Name.fromConstantString("foo.local.host."),
			Arrays.asList(
				new org.xbill.DNS.SVCBBase.ParameterAlpn(Collections.singletonList("h2")),
				new org.xbill.DNS.SVCBBase.ParameterPort(123),
				new org.xbill.DNS.SVCBBase.ParameterIpv4Hint(Arrays.asList(
					(Inet4Address) Inet4Address.getByName("1.1.1.1"),
					(Inet4Address) Inet4Address.getByName("2.2.2.2")
				)),
				new org.xbill.DNS.SVCBBase.ParameterIpv6Hint(Arrays.asList(
					(Inet6Address) Inet6Address.getByName("d42e:3704:2042:136d:edac:6bc8:2665:cbd3"),
					(Inet6Address) Inet6Address.getByName("6fe3:1706:8abf:637f:0a71:10e0:8727:3e41")
				))
			)
		), org.xbill.DNS.Section.ANSWER);
		MESSAGE_TESTS.add(exp);

		// Multiple answers, with parameters
		exp = new org.xbill.DNS.Message(1);
		exp.getHeader().setRcode(org.xbill.DNS.Rcode.NOERROR);
		exp.getHeader().setOpcode(org.xbill.DNS.Opcode.QUERY);
		exp.getHeader().setFlag(org.xbill.DNS.Flags.AA);
		exp.getHeader().setFlag(org.xbill.DNS.Flags.TC);
		exp.addRecord(new org.xbill.DNS.HTTPSRecord(
			org.xbill.DNS.Name.fromConstantString("local.host."),
			1,
			0,
			0,
			org.xbill.DNS.Name.fromConstantString("local.host."),
			Collections.emptyList()
		), org.xbill.DNS.Section.QUESTION);
		exp.addRecord(new org.xbill.DNS.HTTPSRecord(
			org.xbill.DNS.Name.fromConstantString("local.host."),
			1,
			100,
			1,
			org.xbill.DNS.Name.fromConstantString("foo.local.host."),
			Arrays.asList(
				new org.xbill.DNS.SVCBBase.ParameterAlpn(Collections.singletonList("h2")),
				new org.xbill.DNS.SVCBBase.ParameterPort(123),
				new org.xbill.DNS.SVCBBase.ParameterIpv4Hint(Arrays.asList(
					(Inet4Address) Inet4Address.getByName("1.1.1.1"),
					(Inet4Address) Inet4Address.getByName("2.2.2.2")
				)),
				new org.xbill.DNS.SVCBBase.ParameterIpv6Hint(Arrays.asList(
					(Inet6Address) Inet6Address.getByName("d42e:3704:2042:136d:edac:6bc8:2665:cbd3"),
					(Inet6Address) Inet6Address.getByName("6fe3:1706:8abf:637f:0a71:10e0:8727:3e41")
				))
			)
		), org.xbill.DNS.Section.ANSWER);
		exp.addRecord(new org.xbill.DNS.HTTPSRecord(
			org.xbill.DNS.Name.fromConstantString("local.host."),
			1,
			200,
			5,
			org.xbill.DNS.Name.fromConstantString("bar.local.host."),
			Collections.emptyList()
		), org.xbill.DNS.Section.ANSWER);
		MESSAGE_TESTS.add(exp);
	}

	@AfterClass
	public static void destroy() {
		MESSAGE_TESTS.clear();
	}

	@Test
	public void testDecodeMessage() {
		for (org.xbill.DNS.Message exp : MESSAGE_TESTS)
			assertMessageEquals(exp, Dns.decodeMessage(ByteBuffer.wrap(exp.toWire())));
	}

	@Test
	public void testDecodeSvcb() throws UnknownHostException {
		for (org.xbill.DNS.Message msg : MESSAGE_TESTS) {
			for (org.xbill.DNS.Record rec : msg.getSection(org.xbill.DNS.Section.ANSWER)) {
				if (!(rec instanceof org.xbill.DNS.SVCBBase))
					continue;

				org.xbill.DNS.SVCBBase exp = (org.xbill.DNS.SVCBBase) rec;
				Dns.Svcb got = Dns.decodeSvcb(ByteBuffer.wrap(exp.rdataToWireCanonical()));

				assertEquals(exp.getSvcPriority(), got.priority);
				assertEquals(exp.getTargetName().toString(), got.targetName);
				assertEquals(exp.getSvcParamKeys().size(), got.parameters.size());

				for (int key : exp.getSvcParamKeys()) {
					org.xbill.DNS.SVCBBase.ParameterBase expParam = exp.getSvcParamValue(key);
					Object gotParam = got.parameters.get(key);

					assertNotNull(gotParam);
					if (expParam instanceof org.xbill.DNS.SVCBBase.ParameterMandatory) {
						org.xbill.DNS.SVCBBase.ParameterMandatory expMan =
							(org.xbill.DNS.SVCBBase.ParameterMandatory) expParam;
						SparseBooleanArray gotMan = (SparseBooleanArray) gotParam;

						assertEquals(expMan.getValues().size(), gotMan.size());
						for (int expKey : expMan.getValues())
							assertTrue(gotMan.get(expKey));
					} else if (expParam instanceof org.xbill.DNS.SVCBBase.ParameterAlpn) {
						assertEquals(
							((org.xbill.DNS.SVCBBase.ParameterAlpn) expParam).getValues(),
							Arrays.asList((String[]) gotParam)
						);
					} else if (expParam instanceof org.xbill.DNS.SVCBBase.ParameterNoDefaultAlpn) {
						assertEquals(true, gotParam);
					} else if (expParam instanceof org.xbill.DNS.SVCBBase.ParameterPort) {
						assertEquals(
							((org.xbill.DNS.SVCBBase.ParameterPort) expParam).getPort(),
							gotParam
						);
					} else if (expParam instanceof org.xbill.DNS.SVCBBase.ParameterIpv4Hint) {
						assertEquals(
							((org.xbill.DNS.SVCBBase.ParameterIpv4Hint) expParam).getAddresses(),
							Arrays.asList((Inet4Address[]) gotParam)
						);
					} else if (expParam instanceof org.xbill.DNS.SVCBBase.ParameterIpv6Hint) {
						assertEquals(
							((org.xbill.DNS.SVCBBase.ParameterIpv6Hint) expParam).getAddresses(),
							Arrays.asList((Inet6Address[]) gotParam)
						);
					} else {
						assertArrayEquals(expParam.toWire(), (byte[]) gotParam);
					}
				}
			}
		}
	}
}
