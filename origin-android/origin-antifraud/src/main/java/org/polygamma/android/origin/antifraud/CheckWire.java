// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.antifraud;

import static org.polygamma.android.origin.protobuf.Protobuf.WIRE_FIXED32;
import static org.polygamma.android.origin.protobuf.Protobuf.WIRE_FIXED64;
import static org.polygamma.android.origin.protobuf.Protobuf.WIRE_LEN;
import static org.polygamma.android.origin.protobuf.Protobuf.WIRE_VARINT;
import static org.polygamma.android.origin.protobuf.Protobuf.fieldTagOf;

import androidx.annotation.IntDef;

import org.polygamma.android.origin.core.RpcModule;
import org.polygamma.android.origin.core.RpcProcedureId;
import org.polygamma.android.origin.protobuf.Protobuf.FieldTag;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * IVT check wire constants.
 */
interface CheckWire {

	/**
	 * IVT service version.
	 */
	String VERSION = "1.0";

	/**
	 * IVT check procedure id.
	 */
	@RpcProcedureId String RPC_PROCEDURE_ID = RpcModule.idOfProcedure("ivt", "check", VERSION);

	/**
	 * Maximum delay, in seconds, that a recheck can be scheduled at.
	 */
	long MAX_RECHECK_DELAY_SECONDS = TimeUnit.DAYS.toSeconds(7);

	/**
	 * Delay, in seconds, that a recheck should be scheduled after a failure.
	 */
	long ERROR_RECHECK_DELAY_SECONDS = TimeUnit.HOURS.toSeconds(10);

	/**
	 * Begin IVT check session.
	 */
	int SessionBegin 			=   1;

	/**
	 * Perform device attestation.
	 */
	int SessionAttestDevice		=   2;

	/**
	 * Execute tamper machine operations.
	 */
	int SessionTamperMachine	=   3;

	/**
	 * Perform FHE encapsulation of key used for transciphering.
	 */
	int SessionFheEncapsulate	=  10;

	/**
	 * Perform FHE decryption using FHE key pair used to for transciphering.
	 */
	int SessionFheDecrypt		=  11;

	/**
	 * End IVT check session.
	 */
	int SessionEnd				= 100;

	/**
	 * Screen-object proximity sensor measuring in centimeters.
	 */
	int SensorScreenProxCm		= 1;

	/**
	 * Light sensor measuring illumination in SI-lux.
	 */
	int SensorLightIllumSilux	= 2;

	/**
	 * Magnitude of measurements from accelerometer sensor.
	 */
	int SensorAccelMag			= 3;

	/**
	 * Battery level sensor measuring battery level in percentage.
	 */
	int SensorBattLevelPct		= 4;

	/**
	 * Battery temperature sensor measuring in celcius.
	 */
	int SensorBattTempC			= 5;

	/**
	 * Battery voltage sensor measuring in milli-volts.
	 */
	int SensorBattVoltMv		= 6;

	/**
	 * Battery current sensor measuring in microamperes.
	 */
	int SensorBattCurrMua		= 7;

	/**
	 * Smallest sensor type integer.
	 */
	@SensorType int MIN_SENSOR_TYPE = SensorScreenProxCm;

	/**
	 * Highest sensor type integer.
	 */
	@SensorType int MAX_SENSOR_TYPE = SensorBattCurrMua;

	/**
	 * Sensor integer type range.
	 */
	int SENSOR_TYPE_RANGE = (MAX_SENSOR_TYPE - MIN_SENSOR_TYPE) + 1;

	/**
	 * Pop top value placing it into the result stack.
	 */
	int TamperMachinePop			=  1;

	/**
	 * Pop top value and discard it.
	 */
	int TamperMachinePopDiscard		=  2;

	/**
	 * Push value onto top.
	 */
	int TamperMachinePush			=  3;

	/**
	 * Push background asynchronous value onto top.
	 */
	int TamperMachinePushBackAsync	=  4;

	/**
	 * Push foreground asynchronous value onto top.
	 */
	int TamperMachinePushFrontAsync	=  5;

	/**
	 * Push application onto top.
	 */
	int TamperMachinePushApp		=  6;

	/**
	 * PUsh current activity onto top.
	 */
	int TamperMachinePushActivity	=  7;

	/**
	 * Duplicate {@code i}-th value onto top.
	 */
	int TamperMachineDup			=  8;

	/**
	 * Invoke subroutine.
	 */
	int TamperMachineCall			=  9;

	/**
	 * Invoke subroutine asynchronously.
	 */
	int TamperMachineCallAsync		= 10;

	/**
	 * Read property.
	 */
	int TamperMachineRead			= 11;

	/**
	 * Write property.
	 */
	int TamperMachineWrite			= 12;

	/**
	 * Unknown whether device is fraudulent or not.
	 */
	int IvtRatingUnknown			=  0;

	/**
	 * Device is controlled by non-human agent.
	 */
	int IvtRatingNonHuman			=  1;

	/**
	 * Device is controlled by human user.
	 */
	int IvtRatingHuman				=  2;

	/**
	 * Session opcode enumeration value marker.
	 */
	@Documented
	@Retention(RetentionPolicy.SOURCE)
	@Target(ElementType.TYPE_USE)
	@IntDef({
		SessionAttestDevice,
		SessionBegin,
		SessionEnd,
		SessionFheDecrypt,
		SessionFheEncapsulate,
		SessionTamperMachine
	})
	@interface SessionOpcode {
	}

	/**
	 * Sensor type enumeration value marker.
	 */
	@Documented
	@Retention(RetentionPolicy.SOURCE)
	@Target(ElementType.TYPE_USE)
	@IntDef({
		SensorAccelMag,
		SensorBattCurrMua,
		SensorBattLevelPct,
		SensorBattTempC,
		SensorBattVoltMv,
		SensorLightIllumSilux,
		SensorScreenProxCm
	})
	@interface SensorType {
	}

	/**
	 * Tamper machine opcode enumeration value marker.
	 */
	@Documented
	@Retention(RetentionPolicy.SOURCE)
	@Target(ElementType.TYPE_USE)
	@IntDef({
		TamperMachineCall,
		TamperMachineCallAsync,
		TamperMachineDup,
		TamperMachinePop,
		TamperMachinePopDiscard,
		TamperMachinePush,
		TamperMachinePushActivity,
		TamperMachinePushApp,
		TamperMachinePushBackAsync,
		TamperMachinePushFrontAsync,
		TamperMachineRead,
		TamperMachineWrite
	})
	@interface TamperMachineOpcode {
	}

	/**
	 * IVT rating enumeration value marker.
	 */
	@Documented
	@Retention(RetentionPolicy.SOURCE)
	@Target(ElementType.TYPE_USE)
	@IntDef({
		IvtRatingHuman,
		IvtRatingNonHuman,
		IvtRatingUnknown
	})
	@interface IvtRating {
	}

	@FieldTag int SensorEntropy_TYPE					= fieldTagOf(  1, WIRE_VARINT);
	@FieldTag int SensorEntropy_COUNT					= fieldTagOf(  2, WIRE_VARINT);
	@FieldTag int SensorEntropy_MU						= fieldTagOf(  3, WIRE_FIXED32);
	@FieldTag int SensorEntropy_D2						= fieldTagOf(  4, WIRE_FIXED32);

	@FieldTag int AndroidEntropy_BOOTID					= fieldTagOf(  1, WIRE_LEN);
	@FieldTag int AndroidEntropy_BOOTCNT				= fieldTagOf(  2, WIRE_VARINT);
	@FieldTag int AndroidEntropy_PID					= fieldTagOf(  3, WIRE_VARINT);
	@FieldTag int AndroidEntropy_UID					= fieldTagOf(  4, WIRE_VARINT);
	@FieldTag int AndroidEntropy_DEVREALTIMESEC			= fieldTagOf(  5, WIRE_VARINT);
	@FieldTag int AndroidEntropy_DEVSLEEPTIMESEC		= fieldTagOf(  6, WIRE_VARINT);
	@FieldTag int AndroidEntropy_APPRUNTIMESEC			= fieldTagOf(  7, WIRE_VARINT);
	@FieldTag int AndroidEntropy_APPSLEEPTIMESEC		= fieldTagOf(  8, WIRE_VARINT);
	@FieldTag int AndroidEntropy_NCPU					= fieldTagOf( 10, WIRE_VARINT);
	@FieldTag int AndroidEntropy_ADVRAMBYTES			= fieldTagOf( 11, WIRE_VARINT);
	@FieldTag int AndroidEntropy_AVAILRAMBYTES			= fieldTagOf( 12, WIRE_VARINT);
	@FieldTag int AndroidEntropy_FREERAMBYTES			= fieldTagOf( 13, WIRE_VARINT);
	@FieldTag int AndroidEntropy_TOTALRAMBYTES			= fieldTagOf( 14, WIRE_VARINT);
	@FieldTag int AndroidEntropy_ADBENABLED				= fieldTagOf( 20, WIRE_VARINT);
	@FieldTag int AndroidEntropy_AIRMODEENABLED			= fieldTagOf( 21, WIRE_VARINT);
	@FieldTag int AndroidEntropy_AUTOTZENABLED			= fieldTagOf( 22, WIRE_VARINT);
	@FieldTag int AndroidEntropy_ACCESSIBENABLED		= fieldTagOf( 23, WIRE_VARINT);
	@FieldTag int AndroidEntropy_BATTCHRGING			= fieldTagOf( 24, WIRE_VARINT);
	@FieldTag int AndroidEntropy_BUILDTAGS				= fieldTagOf( 40, WIRE_LEN);
	@FieldTag int AndroidEntropy_BUILDFP				= fieldTagOf( 41, WIRE_LEN);
	@FieldTag int AndroidEntropy_BUILDPROD				= fieldTagOf( 42, WIRE_LEN);
	@FieldTag int AndroidEntropy_BUILDHW				= fieldTagOf( 43, WIRE_LEN);
	@FieldTag int AndroidEntropy_BUILDDISP				= fieldTagOf( 44, WIRE_LEN);
	@FieldTag int AndroidEntropy_BUILDRADIO				= fieldTagOf( 45, WIRE_LEN);
	@FieldTag int AndroidEntropy_BUILDSOCMAN			= fieldTagOf( 46, WIRE_LEN);
	@FieldTag int AndroidEntropy_BUILDSOCMODEL			= fieldTagOf( 47, WIRE_LEN);
	@FieldTag int AndroidEntropy_BUILDSUPPABI			= fieldTagOf( 48, WIRE_LEN);

	@FieldTag int PlainEntropy_CHANNEL					= fieldTagOf(  1, WIRE_LEN);
	@FieldTag int PlainEntropy_REGS						= fieldTagOf(  2, WIRE_LEN);
	@FieldTag int PlainEntropy_DEVICE					= fieldTagOf( 50, WIRE_LEN);
	@FieldTag int PlainEntropy_GPSCLOCKDRIFTSEC			= fieldTagOf( 51, WIRE_VARINT);
	@FieldTag int PlainEntropy_NETCLOCKDRIFTSEC			= fieldTagOf( 52, WIRE_VARINT);
	@FieldTag int PlainEntropy_SENSOR					= fieldTagOf( 53, WIRE_LEN);
	@FieldTag int PlainEntropy_CANATTESTDEVICE			= fieldTagOf( 54, WIRE_VARINT);
	@FieldTag int PlainEntropy_ANDROID					= fieldTagOf(500, WIRE_LEN);

	@FieldTag int DynamicEntropy_FLAG					= fieldTagOf(  1, WIRE_VARINT);
	@FieldTag int DynamicEntropy_U32					= fieldTagOf(  2, WIRE_VARINT);
	@FieldTag int DynamicEntropy_U64					= fieldTagOf(  3, WIRE_VARINT);
	@FieldTag int DynamicEntropy_F32					= fieldTagOf(  4, WIRE_FIXED32);
	@FieldTag int DynamicEntropy_F64					= fieldTagOf(  5, WIRE_FIXED64);
	@FieldTag int DynamicEntropy_PFLAG					= fieldTagOf(  6, WIRE_LEN);
	@FieldTag int DynamicEntropy_PU32					= fieldTagOf(  7, WIRE_LEN);
	@FieldTag int DynamicEntropy_PU64					= fieldTagOf(  8, WIRE_LEN);
	@FieldTag int DynamicEntropy_PF32					= fieldTagOf(  9, WIRE_LEN);
	@FieldTag int DynamicEntropy_PF64					= fieldTagOf( 10, WIRE_LEN);
	@FieldTag int DynamicEntropy_BLOB					= fieldTagOf( 11, WIRE_LEN);
	@FieldTag int DynamicEntropy_STR					= fieldTagOf( 12, WIRE_LEN);
	@FieldTag int DynamicEntropy_MSG					= fieldTagOf( 13, WIRE_LEN);
	@FieldTag int DynamicEntropy_LISTSEQ				= fieldTagOf( 14, WIRE_VARINT);
	@FieldTag int DynamicEntropy_MAPSEQ					= fieldTagOf( 15, WIRE_VARINT);
	@FieldTag int DynamicEntropy_NIL					= fieldTagOf( 16, WIRE_VARINT);

	@FieldTag int CipherEntropy_ID						= fieldTagOf(  1, WIRE_LEN);
	@FieldTag int CipherEntropy_SCHEMA					= fieldTagOf(  2, WIRE_LEN);
	@FieldTag int CipherEntropy_CTCONTENT				= fieldTagOf(  3, WIRE_LEN);

	@FieldTag int SessionRequest_ADCOMVER				= fieldTagOf(  1, WIRE_LEN);
	@FieldTag int SessionRequest_LASTDIGEST				= fieldTagOf(  2, WIRE_LEN);
	@SuppressWarnings("unused")
	@FieldTag int SessionRequest_PTENT					= fieldTagOf( 20, WIRE_LEN);
	@FieldTag int SessionRequest_PTENTFLATE				= fieldTagOf( 21, WIRE_LEN);
	@FieldTag int SessionRequest_CTENT					= fieldTagOf( 40, WIRE_LEN);

	@FieldTag int SessionResult_RATING					= fieldTagOf(  1, WIRE_VARINT);
	@FieldTag int SessionResult_CONF					= fieldTagOf(  2, WIRE_VARINT);
	@FieldTag int SessionResult_DIGEST					= fieldTagOf(  3, WIRE_LEN);
	@FieldTag int SessionResult_RECKTIMESTAMPSEC		= fieldTagOf(  4, WIRE_VARINT);

	@FieldTag int AttestDeviceRequest_CHALLENGE			= fieldTagOf(  1, WIRE_LEN);

	@FieldTag int AttestDeviceResponse_CERT				= fieldTagOf(  1, WIRE_LEN);

	@FieldTag int TamperMachineOperation_CODE			= fieldTagOf(  1, WIRE_VARINT);
	@FieldTag int TamperMachineOperation_U32			= fieldTagOf( 10, WIRE_VARINT);
	@FieldTag int TamperMachineOperation_S32			= fieldTagOf( 11, WIRE_VARINT);
	@FieldTag int TamperMachineOperation_U64			= fieldTagOf( 12, WIRE_VARINT);
	@FieldTag int TamperMachineOperation_S64			= fieldTagOf( 13, WIRE_VARINT);
	@FieldTag int TamperMachineOperation_F32			= fieldTagOf( 14, WIRE_FIXED32);
	@FieldTag int TamperMachineOperation_F64			= fieldTagOf( 15, WIRE_FIXED64);
	@FieldTag int TamperMachineOperation_BLOB			= fieldTagOf( 16, WIRE_LEN);
	@FieldTag int TamperMachineOperation_STR			= fieldTagOf( 17, WIRE_LEN);

	@FieldTag int TamperMachineRequest_OP				= fieldTagOf(  1, WIRE_LEN);

	@FieldTag int TamperMachineResponse_STACK			= fieldTagOf(  1, WIRE_LEN);

	@FieldTag int FheParameters_LWEDIM					= fieldTagOf(  1, WIRE_VARINT);
	@FieldTag int FheParameters_LOGNOISEB				= fieldTagOf(  2, WIRE_VARINT);
	@FieldTag int FheParameters_MSGMOD					= fieldTagOf(  3, WIRE_VARINT);
	@FieldTag int FheParameters_CARRYMOD				= fieldTagOf(  4, WIRE_VARINT);

	@FieldTag int FheEncapsulateRequest_PARAMS			= fieldTagOf(  1, WIRE_LEN);

	@FieldTag int FheEncapsulateResponse_PARAMS			= fieldTagOf(  1, WIRE_LEN);
	@FieldTag int FheEncapsulateResponse_PKMASKSEED		= fieldTagOf( 10, WIRE_LEN);
	@FieldTag int FheEncapsulateResponse_PKBODY			= fieldTagOf( 11, WIRE_LEN);
	@FieldTag int FheEncapsulateResponse_CTENTKEYMASK	= fieldTagOf( 20, WIRE_LEN);
	@FieldTag int FheEncapsulateResponse_CTENTKEYBODY	= fieldTagOf( 21, WIRE_LEN);

	@FieldTag int FheDecryptRequest_CTMASK				= fieldTagOf(  1, WIRE_LEN);
	@FieldTag int FheDecryptRequest_CTBODY				= fieldTagOf(  2, WIRE_LEN);

	@FieldTag int FheDecryptResponse_PT					= fieldTagOf(  1, WIRE_LEN);

	@FieldTag int SessionOperation_CODE					= fieldTagOf(  1, WIRE_VARINT);
	@FieldTag int SessionOperation_PAYLOAD				= fieldTagOf(  2, WIRE_LEN);
	@FieldTag int SessionOperation_ERROR				= fieldTagOf(  3, WIRE_LEN);
	@FieldTag int SessionOperation_PAYLOADPAD			= fieldTagOf(  4, WIRE_VARINT);

	@FieldTag int CheckArguments_SESSKEYSEED			= fieldTagOf(  1, WIRE_LEN);
	@FieldTag int CheckArguments_SESSID					= fieldTagOf(  2, WIRE_LEN);
	@FieldTag int CheckArguments_TIMESTAMPSEC			= fieldTagOf(  3, WIRE_VARINT);
	@FieldTag int CheckArguments_OP						= fieldTagOf( 10, WIRE_LEN);
	@FieldTag int CheckArguments_ERROR					= fieldTagOf( 20, WIRE_LEN);

	@FieldTag int CheckResult_SESSID					= fieldTagOf(  1, WIRE_LEN);
	@FieldTag int CheckResult_OP						= fieldTagOf( 10, WIRE_LEN);
}
