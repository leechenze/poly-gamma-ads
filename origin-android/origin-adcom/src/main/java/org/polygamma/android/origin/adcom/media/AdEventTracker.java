// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.adcom.media;

import static org.polygamma.android.origin.protobuf.ProtobufField.*;

import android.util.ArrayMap;
import android.util.Pair;
import android.util.SparseArray;

import androidx.annotation.IntRange;
import androidx.annotation.ReturnThis;

import org.polygamma.android.origin.adcom.enums.AdApiCode;
import org.polygamma.android.origin.adcom.enums.AdComEnums;
import org.polygamma.android.origin.adcom.enums.AdEventTrackerType;
import org.polygamma.android.origin.adcom.enums.AdEventType;
import org.polygamma.android.origin.protobuf.ProtobufReader;
import org.polygamma.android.origin.protobuf.ProtobufSerializable;
import org.polygamma.android.origin.protobuf.ProtobufWriter;
import org.polygamma.android.origin.util.CollectionsCompat;
import org.polygamma.android.origin.util.Preconditions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Advertising media event tracker.
 *
 * @since 1.2
 * @see <a href="https://github.com/InteractiveAdvertisingBureau/AdCOM/blob/main/AdCOM%20v1.0%20FINAL.md#object_event">AdCOM, version 1.0 - Object: Event</a>
 */
public final class AdEventTracker implements ProtobufSerializable {

	private static final @Tag int TYPE		= ofInt32(        1);
	private static final @Tag int METHOD	= ofInt32(        2);
	private static final @Tag int API		= ofPackedInt32(  3);
	private static final @Tag int URL		= ofString(       4);
	private static final @Tag int CDATA		= ofMessage(      5);
	private static final @Tag int ERRORURL	= ofString(     500);
	private static final @Tag int OFFSEC	= ofInt64(      501);
	private static final @Tag int OFFPCT	= ofInt32(      502);

	private static final int FLAG_OFFSET_PERCENT	= 0x10000000;
	private static final int FLAG_OFFSET_EXACT		= 0x20000000;
	private static final int FLAGS_MASK				= 0xf0000000;
	private static final int BITS_PER_FLAGS			= 4;

	private static final AdEventTracker DEFAULT = new AdEventTracker();

	static {
		//noinspection ConstantValue
		assert AdComEnums.MAX_AD_API_CODE < (32 - BITS_PER_FLAGS);
	}

	/**
	 * Advertising media event {@linkplain AdEventTracker tracker} builder.
	 *
	 * @since 1.2
	 * @see #ofBuilder()
	 */
	public static final class Builder {

		private AdEventTracker tracker;
		private boolean needClone;

		private Builder(AdEventTracker tracker) {
			this.tracker = tracker;
			this.needClone = true;
		}

		private AdEventTracker target() {
			if (this.needClone) {
				this.tracker = new AdEventTracker(this.tracker);
				this.needClone = false;
			}
			return this.tracker;
		}

		/**
		 * Set tracked event type.
		 *
		 * @param type event type
		 * @return {@code this}
		 * @since 1.2
		 * @see AdEventTracker#event()
		 */
		@ReturnThis
		public Builder event(@AdEventType int type) {
			this.target().event = type;
			return this;
		}

		/**
		 * Set tracker resource type.
		 *
		 * @param type resource type
		 * @return {@code this}
		 * @since 1.2
		 * @see AdEventTracker#type()
		 */
		@ReturnThis
		public Builder type(@AdEventTrackerType int type) {
			this.target().type = type;
			return this;
		}

		/**
		 * Set playback offset, in percentage of playback completion, at which tracker must be
		 * fired.
		 * <p>If a playback offset {@linkplain #playbackOffsetSeconds(long) duration} is
		 * configured, it is removed. Additionally, if event tracker is associated with
		 * non-playback ad media, the configured offset is always ignored.
		 *
		 * @param pct percentage of playback completion or {@code -1} if tracker has no offset
		 * @return {@code this}
		 * @throws IllegalArgumentException {@code pct} is not {@code -1} or greater than {@code
		 * 100}
		 * @since 1.2
		 * @see AdEventTracker#playbackOffsetPercent()
		 * @see #playbackOffsetSeconds(long)
		 */
		@ReturnThis
		public Builder playbackOffsetPercent(@IntRange(from = -1, to = 100) int pct) {
			Preconditions.checkArgument(pct >= -1 && pct <= 100);

			AdEventTracker dst = this.target();

			if (pct == -1) {
				dst.requiredAdApisAndFlags &= ~FLAG_OFFSET_PERCENT;
			} else {
				dst.requiredAdApisAndFlags =
					(dst.requiredAdApisAndFlags & ~FLAG_OFFSET_EXACT) |
					FLAG_OFFSET_PERCENT;
				dst.playbackOffsetSecondsOrPercent = pct;
			}
			return this;
		}

		/**
		 * Set playback offset, in seconds of playback duration, at which tracker must be fired.
		 * <p>If a playback offset {@linkplain #playbackOffsetPercent(int) percentage} is
		 * configured, it is removed. Additionally, if event tracker is associated with
		 * non-playback ad media, the configured offset is always ignored.
		 *
		 * @param off playback offset, in seconds, or {@code -1} if tracker has no offset
		 * @return {@code this}
		 * @throws IllegalArgumentException {@code off} is less than {@code -1}
		 * @since 1.2
		 * @see AdEventTracker#playbackOffsetSeconds()
		 * @see #playbackOffsetPercent(int)
		 */
		@ReturnThis
		public Builder playbackOffsetSeconds(@IntRange(from = -1) long off) {
			Preconditions.checkArgument(off >= -1);

			AdEventTracker dst = this.target();

			if (off == -1L) {
				dst.requiredAdApisAndFlags &= ~FLAG_OFFSET_EXACT;
			} else {
				dst.requiredAdApisAndFlags =
					(dst.requiredAdApisAndFlags & ~FLAG_OFFSET_PERCENT) |
					FLAG_OFFSET_EXACT;
				dst.playbackOffsetSecondsOrPercent = off;
			}
			return this;
		}

		/**
		 * Set tracker resource URL.
		 *
		 * @param url resource URL
		 * @return {@code this}
		 * @since 1.2
		 * @see AdEventTracker#url()
		 */
		@ReturnThis
		public Builder url(String url) {
			this.target().url = url;
			return this;
		}

		/**
		 * Set vendor data forwarded to tracker resource upon execution.
		 *
		 * @param data vendor data mapping
		 * @return {@code this}
		 * @since 1.2
		 * @see AdEventTracker#vendorDataCount()
		 * @see AdEventTracker#vendorDataKey(int)
		 * @see AdEventTracker#vendorDataValue(int)
		 */
		@ReturnThis
		public Builder vendorData(Map<String, String> data) {
			this.target().vendorData =
				data.isEmpty() ? DEFAULT.vendorData :
				CollectionsCompat.arrayMapCopyOf(data);
			return this;
		}

		/**
		 * Set URLs to execute when tracker resource fails to execute.
		 *
		 * @param urls error URLs
		 * @return {@code this}
		 * @since 1.2
		 * @see AdEventTracker#errorUrlCount()
		 * @see AdEventTracker#errorUrl(int)
		 */
		@ReturnThis
		public Builder errorUrls(Collection<String> urls) {
			this.target().errorUrls = CollectionsCompat.toStringArrayOrEmpty(urls);
			return this;
		}

		/**
		 * Set advertising APIs, by code, required to execute tracker resource.
		 *
		 * @param codes required APIs
		 * @return {@code this}
		 * @since 1.2
		 * @see AdEventTracker#isAdApiRequired(int)
		 */
		@ReturnThis
		public Builder requiredAdApis(@AdApiCode int... codes) {
			AdEventTracker dst = this.target();

			dst.requiredAdApisAndFlags &= FLAGS_MASK;
			for (int code : codes) {
				if (code >= 0 && code <= AdComEnums.MAX_AD_API_CODE)
					dst.requiredAdApisAndFlags |= (1 << code);
			}
			return this;
		}

		/**
		 * Build resulting tracker.
		 *
		 * @return tracker instance
		 * @since 1.2
		 */
		public AdEventTracker build() {
			this.needClone = true;
			return this.tracker;
		}
	}

	/**
	 * Default empty tracker instance.
	 *
	 * @return empty tracker instance
	 * @since 1.2
	 */
	public static AdEventTracker of() {
		return DEFAULT;
	}

	/**
	 * Construct new empty {@linkplain Builder builder}.
	 *
	 * @return empty builder instance
	 * @since 1.2
	 */
	public static Builder ofBuilder() {
		return DEFAULT.toBuilder();
	}

	/**
	 * Deserialize tracker from Protobuf message.
	 *
	 * @param reader reader to deserialize tracker from
	 * @return deserialized tracker
	 * @throws RuntimeException coding is malformed
	 * @since 1.2
	 */
	public static AdEventTracker ofProtobuf(ProtobufReader reader) {
		AdEventTracker rv = new AdEventTracker(DEFAULT);
		List<String> errUrls = new ArrayList<>();

		while (reader.hasRemaining()) {
			int tag = reader.readTag();

			if (tag == TYPE) {
				rv.event = reader.readInt32();
			} else if (tag == METHOD) {
				rv.type = reader.readInt32();
			} else if (tag == API) {
				rv.requiredAdApisAndFlags |= (int) (reader.readWordBitmap(0) & 0xffffffffL);
			} else if (tag == URL) {
				rv.url = reader.readString();
			} else if (tag == CDATA) {
				Pair<String, String> cdata = reader.readStringPair();

				if (rv.vendorData == DEFAULT.vendorData)
					rv.vendorData = new ArrayMap<>(1);
				rv.vendorData.put(cdata.first, cdata.second);
			} else if (tag == ERRORURL) {
				errUrls.add(reader.readString());
			} else if (tag == OFFSEC) {
				rv.requiredAdApisAndFlags =
					(rv.requiredAdApisAndFlags & ~FLAG_OFFSET_PERCENT) |
					FLAG_OFFSET_EXACT;
				rv.playbackOffsetSecondsOrPercent = reader.readInt64();
			} else if (tag == OFFPCT) {
				rv.requiredAdApisAndFlags =
					(rv.requiredAdApisAndFlags & ~FLAG_OFFSET_EXACT) |
					FLAG_OFFSET_PERCENT;
				rv.playbackOffsetSecondsOrPercent = reader.readInt32();
			}
		}
		rv.errorUrls = CollectionsCompat.toStringArrayOrEmpty(errUrls);
		return rv;
	}

	private @AdEventType int event;
	private @AdEventTrackerType int type;
	private long playbackOffsetSecondsOrPercent;
	private String url;
	private ArrayMap<String, String> vendorData;
	private String[] errorUrls;
	private int requiredAdApisAndFlags;

	private AdEventTracker() {
		this.event = AdComEnums.AdEventUnknown;
		this.type = AdComEnums.AdEventTrackerUnknown;
		this.url = "";
		this.vendorData = new ArrayMap<>(0);
		this.errorUrls = CollectionsCompat.toStringArrayOrEmpty(Collections.emptyList());
		this.requiredAdApisAndFlags = 0;
	}

	private AdEventTracker(AdEventTracker that) {
		this.event = that.event;
		this.type = that.type;
		this.playbackOffsetSecondsOrPercent = that.playbackOffsetSecondsOrPercent;
		this.url = that.url;
		this.vendorData = that.vendorData;
		this.errorUrls = that.errorUrls;
		this.requiredAdApisAndFlags = that.requiredAdApisAndFlags;
	}

	/**
	 * Tracked event type.
	 *
	 * @return event type
	 * @since 1.2
	 * @see Builder#event(int)
	 */
	public @AdEventType int event() {
		return this.event;
	}

	/**
	 * Tracker resource type.
	 *
	 * @return resource type
	 * @since 1.2
	 * @see Builder#type(int)
	 */
	public @AdEventTrackerType int type() {
		return this.type;
	}

	/**
	 * Playback offset, in percentage of playback completion, at which tracker must be fired.
	 * <p>If this returns a non-negative value, then {@link #playbackOffsetSeconds()} is guaranteed
	 * to return {@code -1}.
	 *
	 * @return offset percentage, between {@code 0} and {@code 100}, or {@code -1} if tracker does
	 * not have a playback percent offset configured
	 * @since 1.2
	 * @see Builder#playbackOffsetPercent(int)
	 * @see #playbackOffsetSeconds()
	 */
	public @IntRange(from = -1, to = 100) int playbackOffsetPercent() {
		return (this.requiredAdApisAndFlags & FLAG_OFFSET_PERCENT) == 0 ? -1 :
			(int) (this.playbackOffsetSecondsOrPercent & 0xffL);
	}

	/**
	 * Playback offset, in seconds of playback, at which tracker must be fired.
	 * <p>If this returns a non-negative value, then {@link #playbackOffsetPercent()} is guaranteed
	 * to return {@code -1}.
	 *
	 * @return offset, in seconds, or {@code -1} if tracker does not have a playback offset
	 * duration configured
	 * @since 1.2
	 * @see Builder#playbackOffsetSeconds(long)
	 * @see #playbackOffsetPercent()
	 */
	public @IntRange(from = -1) long playbackOffsetSeconds() {
		return (this.requiredAdApisAndFlags & FLAG_OFFSET_EXACT) == 0 ? -1 :
			this.playbackOffsetSecondsOrPercent;
	}

	/**
	 * Tracker resource URL.
	 * <p>The URL returned may have macros embedded, which may be resolved using {@link
	 * #resolveUrl(SparseArray)}.
	 *
	 * @return resource URL
	 * @since 1.2
	 * @see Builder#url(String)
	 * @see #resolveUrl(SparseArray)
	 */
	public String url() {
		return this.url;
	}

	/**
	 * Resolve tracker resource URL.
	 * <p>This replaces any macros embedded within the resource {@linkplain #url() URL} with
	 * macro values specified in {@code macros}. For each {@linkplain
	 * org.polygamma.android.origin.adcom.enums.AdTrackerUrlMacroType macro} embedded within the
	 * URL, the macro is substituted with the value mapped to the macro type in {@code macros}.
	 *
	 * @param macros macro type to value mapping
	 * @return resolved resource URL
	 * @since 1.2
	 * @see #url()
	 * @see org.polygamma.android.origin.adcom.enums.AdTrackerUrlMacroType
	 */
	public String resolveUrl(SparseArray<String> macros) {
		return TrackerUrl.substituteMacros(this.url, macros);
	}

	/**
	 * Vendor tracker data count.
	 *
	 * @return data count
	 * @since 1.2
	 * @see Builder#vendorData(Map)
	 * @see #vendorDataKey(int)
	 * @see #vendorDataValue(int)
	 */
	public int vendorDataCount() {
		return this.vendorData.size();
	}

	/**
	 * Vendor tracker data key, at index.
	 *
	 * @param i index to retrieve key at
	 * @return data key at index {@code i}
	 * @throws IndexOutOfBoundsException {@code i} is negative or, greater than or equal to vendor
	 * data {@linkplain #vendorDataCount() count}
	 * @since 1.2
	 * @see Builder#vendorData(Map)
	 * @see #vendorDataCount()
	 * @see #vendorDataValue(int)
	 */
	public String vendorDataKey(int i) {
		return this.vendorData.keyAt(i);
	}

	/**
	 * Vendor tracker data value, at index.
	 *
	 * @param i index to retrieve value at
	 * @return data value at index {@code i}
	 * @throws IndexOutOfBoundsException {@code i} is negative or, greater than or equal to vendor
	 * data {@linkplain #vendorDataCount() count}
	 * @since 1.2
	 * @see Builder#vendorData(Map)
	 * @see #vendorDataCount()
	 * @see #vendorDataKey(int)
	 */
	public String vendorDataValue(int i) {
		return this.vendorData.valueAt(i);
	}

	/**
	 * Count of error URLs.
	 *
	 * @return error URL count
	 * @since 1.2
	 * @see Builder#errorUrls(Collection)
	 * @see #errorUrl(int)
	 */
	public int errorUrlCount() {
		return this.errorUrls.length;
	}

	/**
	 * Error URL, at index.
	 * <p>The URL returned may have macros embedded, which may be resolved using {@link
	 * #resolveErrorUrl(int, SparseArray)}.
	 *
	 * @param i index to retrieve URL at
	 * @return error URL at index {@code i}
	 * @throws IndexOutOfBoundsException {@code i} is negative or, greater than or equal to error
	 * URL {@linkplain #errorUrlCount() count}
	 * @since 1.2
	 * @see Builder#errorUrls(Collection)
	 * @see #errorUrlCount()
	 * @see #resolveErrorUrl(int, SparseArray)
	 */
	public String errorUrl(int i) {
		return this.errorUrls[i];
	}

	/**
	 * Resolve error URL, at index.
	 * <p>This replaces any macros embedded within the error {@linkplain #errorUrl(int) URL}, at
	 * index {@code i}, with macro values specified in {@code macros}. For each {@linkplain
	 * org.polygamma.android.origin.adcom.enums.AdTrackerUrlMacroType macro} embedded within the
	 * URL, the macro is substituted with the value mapped to the macro type in {@code macros}.
	 *
	 * @param i index to retrieve URL at
	 * @param macros macro type to value mapping
	 * @return resolved error URL
	 * @throws IndexOutOfBoundsException {@code i} is negative or, greater than or equal to error
	 * URL {@linkplain #errorUrlCount() count}
	 * @since 1.2
	 * @see #errorUrl(int)
	 */
	public String resolveErrorUrl(int i, SparseArray<String> macros) {
		return TrackerUrl.substituteMacros(this.errorUrls[i], macros);
	}

	/**
	 * Test whether an advertising API, by code, is required to execute tracker.
	 *
	 * @param code code of API to test
	 * @return {@code true} if, and only if, API is required
	 * @since 1.2
	 * @see Builder#requiredAdApis(int...)
	 */
	public boolean isAdApiRequired(@AdApiCode int code) {
		return (
			code >= 0 &&
			code <= AdComEnums.MAX_AD_API_CODE &&
			(this.requiredAdApisAndFlags & (1 << code)) != 0
		);
	}

	/**
	 * Construct new {@linkplain Builder builder} initialized from {@code this}.
	 *
	 * @return new builder instance
	 * @since 1.2
	 */
	public Builder toBuilder() {
		return new Builder(this);
	}

	@Override
	public void toProtobuf(ProtobufWriter writer) {
		writer.writeInt32(TYPE, this.event);
		writer.writeInt32(METHOD, this.type);
		writer.writeWordBitmap(
			API,
			Integer.toUnsignedLong(this.requiredAdApisAndFlags & ~FLAGS_MASK),
			0
		);
		writer.writeString(URL, this.url);
		writer.writeRepeatString(ERRORURL, this.errorUrls);

		for (int i = 0; i < this.vendorData.size(); i++) {
			writer.writeStringPair(
				CDATA,
				new Pair<>(this.vendorData.keyAt(i), this.vendorData.valueAt(i))
			);
		}

		if ((this.requiredAdApisAndFlags & FLAG_OFFSET_EXACT) != 0)
			writer.writeInt64(OFFSEC, this.playbackOffsetSecondsOrPercent);
		else if ((this.requiredAdApisAndFlags & FLAG_OFFSET_PERCENT) != 0)
			writer.writeInt32(OFFPCT, (int) this.playbackOffsetSecondsOrPercent);
	}
}
