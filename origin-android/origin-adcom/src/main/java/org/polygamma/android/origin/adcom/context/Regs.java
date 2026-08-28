// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.adcom.context;

import static org.polygamma.android.origin.protobuf.ProtobufField.*;

import android.annotation.SuppressLint;

import androidx.annotation.ReturnThis;

import org.polygamma.android.origin.protobuf.ProtobufReader;
import org.polygamma.android.origin.protobuf.ProtobufSerializable;
import org.polygamma.android.origin.protobuf.ProtobufWriter;

import java.util.Arrays;

/**
 * Legal, governmental, or industry regulations context.
 *
 * @since 0.1
 * @see <a href="https://github.com/InteractiveAdvertisingBureau/AdCOM/blob/main/AdCOM%20v1.0%20FINAL.md#object--regs-">AdCOM, version 1.0 - Object: Regs</a>
 * @see <a href="https://github.com/InteractiveAdvertisingBureau/openrtb2.x/blob/main/2.6.md#323---object-regs-">OpenRTB, version 2.6 - Object: Regs</a>
 */
public final class Regs implements ProtobufSerializable {

	private static final @Tag int COPPA			= ofBool(          1);
	private static final @Tag int GDPR			= ofBool(          2);
	private static final @Tag int GPP			= ofString(        3);
	private static final @Tag int GPPSID		= ofPackedInt32(   4);
	/*private static final @Tag int USPRIVACY	= ofString(     200);*/
	private static final @Tag int PIPL			= ofBool(       500);

	private static final int FLAG_COPPA		= 0x01;
	private static final int FLAG_GDPR		= 0x02;
	private static final int FLAG_PIPL		= 0x04;

	/**
	 * Empty regulations context.
	 */
	private static final Regs DEFAULT = new Regs();

	/**
	 * {@linkplain Regs Regulations} context builder.
	 *
	 * @since 0.1
	 * @see #ofBuilder()
	 */
	public static final class Builder {

		private Regs regs;
		private boolean needClone;

		private Builder(Regs regs) {
			this.regs = regs;
			this.needClone = true;
		}

		private Regs target() {
			if (this.needClone) {
				this.regs = new Regs(this.regs);
				this.needClone = false;
			}
			return this.regs;
		}

		@ReturnThis
		private Builder toggleFlag(int flag, boolean on) {
			Regs dst = this.target();

			if (on)
				dst.flags |= flag;
			else
				dst.flags &= ~flag;
			return this;
		}

		/**
		 * Set whether COPPA regulations, as established by USA FTC, are in effect.
		 *
		 * @param coppa {@code true} if, and only if, COPPA is in effect
		 * @return {@code this}
		 * @since 0.1
		 * @see Regs#coppa()
		 */
		@ReturnThis
		@SuppressLint("ReturnThis")
		public Builder coppa(boolean coppa) {
			return this.toggleFlag(FLAG_COPPA, coppa);
		}

		/**
		 * Set whether General Data Protection Regulation (GPDR), as established by the European
		 * Union, is in effect.
		 *
		 * @param gdpr {@code true} if, and only if, GDPR is in effect
		 * @return {@code this}
		 * @since 0.1
		 * @see Regs#gdpr()
		 */
		@ReturnThis
		@SuppressLint("ReturnThis")
		public Builder gdpr(boolean gdpr) {
			return this.toggleFlag(FLAG_GDPR, gdpr);
		}

		/**
		 * Set Global Privacy Platform (GPP) consent string.
		 *
		 * @param gpp consent string or {@linkplain String#isEmpty() empty} if unavailable
		 * @return {@code this}
		 * @since 0.1
		 * @see Regs#gpp()
		 */
		@ReturnThis
		public Builder gpp(String gpp) {
			this.target().gpp = gpp;
			return this;
		}

		/**
		 * Set {@linkplain #gpp() GPP} sections in effect.
		 *
		 * @param sids section ids
		 * @return {@code this}
		 * @since 1.2
		 * @see Regs#applicableGppSectionId(int)
		 */
		@ReturnThis
		public Builder applicableGppSectionIds(int... sids) {
			this.target().applicableGppSectionIds = sids.length == 0 ?
				DEFAULT.applicableGppSectionIds :
				Arrays.copyOf(sids, sids.length);
			return this;
		}

		/**
		 * Set whether Personal Information Protection Law (PIPL), as established by the People's
		 * Republic of China, is in effect.
		 *
		 * @param pipl {@code true} if, and only if, PIPL is in effect
		 * @return {@code this}
		 * @since 0.1
		 * @see Regs#pipl()
		 */
		@ReturnThis
		@SuppressLint("ReturnThis")
		public Builder pipl(boolean pipl) {
			return this.toggleFlag(FLAG_PIPL, pipl);
		}

		/**
		 * Build resulting regulations.
		 *
		 * @return regulations instance
		 * @since 1.2
		 */
		public Regs build() {
			this.needClone = true;
			return this.regs;
		}
	}

	/**
	 * Default empty regulations instance.
	 *
	 * @return empty instance
	 * @since 1.2
	 */
	public static Regs of() {
		return DEFAULT;
	}

	/**
	 * Construct new empty {@linkplain Builder builder}.
	 *
	 * @return empty builder
	 * @since 0.1
	 */
	public static Builder ofBuilder() {
		return DEFAULT.toBuilder();
	}

	/**
	 * Deserialize regulations from Protobuf message.
	 *
	 * @param reader reader to deserialize from
	 * @return deserialized regulations
	 * @throws RuntimeException coding is malformed
	 * @since 1.2
	 */
	public static Regs ofProtobuf(ProtobufReader reader) {
		Regs rv = new Regs(DEFAULT);

		while (reader.hasRemaining()) {
			int tag = reader.readTag();

			if (tag == GPP) {
				rv.gpp = reader.readString();
			} else if (tag == GPPSID) {
				rv.applicableGppSectionIds = reader.readPackedInt32();
			} else if (tag == COPPA) {
				if (reader.readBool())
					rv.flags |= FLAG_COPPA;
			} else if (tag == GDPR) {
				if (reader.readBool())
					rv.flags |= FLAG_GDPR;
			} else if (tag == PIPL) {
				if (reader.readBool())
					rv.flags |= FLAG_PIPL;
			}
		}
		return rv;
	}

	private String gpp;
	private int[] applicableGppSectionIds;
	private int flags;

	private Regs() {
		this.gpp = "";
		this.applicableGppSectionIds = new int[0];
	}

	private Regs(Regs that) {
		this.gpp = that.gpp;
		this.applicableGppSectionIds = that.applicableGppSectionIds;
		this.flags = that.flags;
	}

	/**
	 * COPPA regulations, as established by USA FTC, are in effect.
	 *
	 * @return {@code true} if, and only if, COPPA is in effect
	 * @since 0.1
	 * @see Builder#coppa(boolean)
	 */
	public boolean coppa() {
		return (this.flags & FLAG_COPPA) != 0;
	}

	/**
	 * General Data Protection Regulation (GDPR), as established by the European Union, is in
	 * effect.
	 *
	 * @return {@code true} if, and only if, GDPR is in effect
	 * @since 0.1
	 * @see Builder#gdpr(boolean)
	 */
	public boolean gdpr() {
		return (this.flags & FLAG_GDPR) != 0;
	}

	/**
	 * Global Privacy Platform (GPP) consent string.
	 *
	 * @return consent string or {@linkplain String#isEmpty() empty} if unavailable
	 * @since 0.1
	 * @see Builder#gpp(String)
	 */
	public String gpp() {
		return this.gpp;
	}

	/**
	 * Count of GPP section ids in effect.
	 *
	 * @return section id count
	 * @since 1.2
	 * @see #applicableGppSectionId(int)
	 * @see Builder#applicableGppSectionIds(int...)
	 */
	public int applicableGppSectionIdCount() {
		return this.applicableGppSectionIds.length;
	}

	/**
	 * GPP section id in effect at index.
	 *
	 * @param i index to retrieve section id at
	 * @return section id at index {@code i}
	 * @throws IndexOutOfBoundsException {@code i} is negative or, greater than or equal to
	 * applicable section id {@linkplain #applicableGppSectionIdCount() count}
	 * @since 1.2
	 * @see #applicableGppSectionIdCount()
	 * @see Builder#applicableGppSectionIds(int...)
	 */
	public int applicableGppSectionId(int i) {
		return this.applicableGppSectionIds[i];
	}

	/**
	 * Personal Information Protection Law (PIPL), as established by the People's Republic of
	 * China, is in effect.
	 *
	 * @return {@code true} if, and only if, PIPL is in effect
	 * @since 0.1
	 * @see Builder#pipl(boolean)
	 */
	public boolean pipl() {
		return (this.flags & FLAG_PIPL) != 0;
	}

	/**
	 * Construct new builder initialized from {@code this}.
	 *
	 * @return new builder instance
	 * @since 0.1
	 */
	public Builder toBuilder() {
		return new Builder(this);
	}

	@Override
	public void toProtobuf(ProtobufWriter writer) {
		writer.writeString(GPP, this.gpp);
		writer.writePackedInt32(GPPSID, this.applicableGppSectionIds);
		writer.writeBool(COPPA, this.coppa());
		writer.writeBool(GDPR, this.gdpr());
		writer.writeBool(PIPL, this.pipl());
	}
}
