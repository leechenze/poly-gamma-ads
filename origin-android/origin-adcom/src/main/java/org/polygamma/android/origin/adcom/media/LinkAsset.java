// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.adcom.media;

import static org.polygamma.android.origin.protobuf.Protobuf.WIRE_LEN;
import static org.polygamma.android.origin.protobuf.Protobuf.fieldTagOf;

import android.util.SparseArray;

import org.polygamma.android.origin.protobuf.Protobuf.FieldTag;
import org.polygamma.android.origin.protobuf.ProtobufDecoder;
import org.polygamma.android.origin.protobuf.ProtobufEncoder;
import org.polygamma.android.origin.protobuf.ProtobufSerializable;
import org.polygamma.android.origin.util.CollectionsCompat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Navigation link ad asset.
 *
 * @since 1.2
 * @see <a href="https://github.com/InteractiveAdvertisingBureau/AdCOM/blob/main/AdCOM%20v1.0%20FINAL.md#object--linkasset-">AdCOM, version 1.0 - Object: LinkAsset</a>
 */
public final class LinkAsset implements ProtobufSerializable {

	private static final @FieldTag int URL		= fieldTagOf(1, WIRE_LEN);
	private static final @FieldTag int URLFB	= fieldTagOf(2, WIRE_LEN);
	private static final @FieldTag int TRKR		= fieldTagOf(3, WIRE_LEN);

	/**
	 * Empty navigation link ad asset.
	 */
	private static final LinkAsset DEFAULT =
		new LinkAsset("", "", CollectionsCompat.toStringArrayOrEmpty(Collections.emptyList()));

	/**
	 * Default empty navigation link ad asset instance.
	 *
	 * @return empty instance
	 * @since 1.2
	 */
	public static LinkAsset of() {
		return DEFAULT;
	}

	/**
	 * Construct new navigation link ad asset.
	 *
	 * @param primaryUrl primary navigation link URL
	 * @param fallbackUrl fallback navigation link URL
	 * @param trkUrls navigation tracker URLs
	 * @return resulting asset instance
	 * @since 1.2
	 */
	public static LinkAsset of(String primaryUrl, String fallbackUrl, Collection<String> trkUrls) {
		if (primaryUrl.isEmpty() && fallbackUrl.isEmpty() && trkUrls.isEmpty())
			return DEFAULT;
		return new LinkAsset(
			primaryUrl,
			fallbackUrl,
			CollectionsCompat.toStringArrayOrEmpty(trkUrls)
		);
	}

	/**
	 * Deserialize link ad asset from Protobuf message.
	 *
	 * @param dec decoder to deserialize from
	 * @return deserialized asset instance
	 * @throws RuntimeException coding is malformed
	 * @since 1.2
	 */
	public static LinkAsset ofProtobuf(ProtobufDecoder dec) {
		String purl = "";
		String furl = "";
		List<String> trkUrls = new ArrayList<>();

		while (dec.hasRemaining()) {
			int tag = dec.decodeFieldTag();

			if (tag == URL)
				purl = dec.decodeString();
			else if (tag == URLFB)
				furl = dec.decodeString();
			else if (tag == TRKR)
				trkUrls.add(dec.decodeString());
			else
				dec.skipFieldValue(tag);
		}
		return of(purl, furl, trkUrls);
	}

	private final String primaryUrl;
	private final String fallbackUrl;
	private final String[] trackerUrls;

	private LinkAsset(String primaryUrl, String fallbackUrl, String[] trkUrls) {
		this.primaryUrl = primaryUrl;
		this.fallbackUrl = fallbackUrl;
		this.trackerUrls = trkUrls;
	}

	/**
	 * Primary navigation URL.
	 *
	 * @return primary URL
	 * @since 1.2
	 * @see #fallbackUrl()
	 */
	public String primaryUrl() {
		return this.primaryUrl;
	}

	/**
	 * Fallback navigation URL.
	 * <p>The URL returned, if any, is used when the {@linkplain #primaryUrl() primary} URL cannot
	 * be navigated to on executing device.
	 *
	 * @return fallback URL
	 * @since 1.2
	 * @see #primaryUrl()
	 */
	public String fallbackUrl() {
		return this.fallbackUrl;
	}

	/**
	 * Count of tracker URLs to be executed when navigation is performed.
	 *
	 * @return tracker URL count
	 * @since 1.2
	 * @see #trackerUrl(int)
	 */
	public int trackerUrlCount() {
		return this.trackerUrls.length;
	}

	/**
	 * Tracker URL, at index.
	 * <p>The URL returned may have macros embedded, which may be resolved using {@link
	 * #resolveTrackerUrl(int, SparseArray)}.
	 *
	 * @param i index to retrieve URL at
	 * @return tracker URL at index {@code i}
	 * @throws IndexOutOfBoundsException {@code i} is negative or, greater than or equal to tracker
	 * URL {@linkplain #trackerUrlCount() count}
	 * @since 1.2
	 * @see #trackerUrlCount()
	 * @see #resolveTrackerUrl(int, SparseArray)
	 */
	public String trackerUrl(int i) {
		return this.trackerUrls[i];
	}

	/**
	 * Resolve tracker URL, at index.
	 * <p>This replaces any macros embedded within the tracker {@linkplain #trackerUrl(int) URL},
	 * at index {@code i}, with macro values specified in {@code macros}. For each {@linkplain
	 * org.polygamma.android.origin.adcom.enums.AdTrackerUrlMacroType macro} embedded within the
	 * URL, the macro is substituted with the value mapped to the macro type in {@code macros}.
	 *
	 * @param i index to resolve URL at
	 * @param macros macro type to value mapping
	 * @return resolved tracker URL at index {@code i}
	 * @throws IndexOutOfBoundsException {@code i} is negative or, greater than or equal to tracker
	 * URL {@linkplain #trackerUrlCount() count}
	 * @since 1.2
	 * @see #trackerUrl(int)
	 */
	public String resolveTrackerUrl(int i, SparseArray<String> macros) {
		return TrackerUrl.substituteMacros(this.trackerUrls[i], macros);
	}

	@Override
	public void toProtobuf(ProtobufEncoder enc) {
		enc.encodeStringField(URL, this.primaryUrl)
			.encodeStringField(URLFB, this.fallbackUrl);
		for (String url : this.trackerUrls)
			enc.encodeStringField(TRKR, url);
	}
}
