// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.adcom.media;

import static org.polygamma.android.origin.protobuf.ProtobufField.*;

import androidx.annotation.IntDef;

import org.polygamma.android.origin.protobuf.ProtobufReader;
import org.polygamma.android.origin.protobuf.ProtobufSerializable;
import org.polygamma.android.origin.protobuf.ProtobufWriter;
import org.polygamma.android.origin.util.Preconditions;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Displayable asset of an icon.
 *
 * @since 1.2
 */
public final class IconDisplayAsset implements ProtobufSerializable {

	private static final @Tag int MIME				= ofString(1);
	private static final @Tag int IMGURL			= ofString(2);
	private static final @Tag int IFRAMEURL			= ofString(3);
	private static final @Tag int HTML				= ofString(4);

	private static final @Type int TYPE_IMAGE_URL	= 1;
	private static final @Type int TYPE_IFRAME_URL	= 2;
	private static final @Type int TYPE_HTML_MARKUP	= 3;

	/**
	 * Icon display asset type enumeration discriminant value marker.
	 */
	@Documented
	@Retention(RetentionPolicy.SOURCE)
	@Target(ElementType.TYPE_USE)
	@IntDef({ TYPE_HTML_MARKUP, TYPE_IFRAME_URL, TYPE_IMAGE_URL })
	private @interface Type {
	}

	/**
	 * Construct new {@linkplain #isImageUrlAsset() image URL} asset.
	 *
	 * @param mime image MIME type
	 * @param url image URL
	 * @return asset instance
	 * @since 1.2
	 * @see #isImageUrlAsset()
	 */
	public static IconDisplayAsset ofImageUrlAsset(String mime, String url) {
		return new IconDisplayAsset(TYPE_IMAGE_URL, mime, url);
	}

	/**
	 * Construct new {@linkplain #isIframeUrlAsset() HTML document URL} asset.
	 *
	 * @param url HTML document URL
	 * @return asset instance
	 * @since 1.2
	 * @see #isIframeUrlAsset()
	 */
	public static IconDisplayAsset ofIframeUrlAsset(String url) {
		return new IconDisplayAsset(TYPE_IFRAME_URL, "", url);
	}

	/**
	 * Construct new {@linkplain #isHtmlMarkupAsset() HTML snippet markup} asset.
	 *
	 * @param markup HTML markup
	 * @return asset instance
	 * @since 1.2
	 * @see #isHtmlMarkupAsset()
	 */
	public static IconDisplayAsset ofHtmlMarkupAsset(String markup) {
		return new IconDisplayAsset(TYPE_HTML_MARKUP, "", markup);
	}

	/**
	 * Deserialize icon display media asset from Protobuf message.
	 *
	 * @param reader reader to deserialize from
	 * @return deserialized media asset
	 * @throws RuntimeException coding is malformed
	 * @since 1.2
	 */
	public static IconDisplayAsset ofProtobuf(ProtobufReader reader) {
		String mime = "";
		String data = "";
		@Type int type = 0;

		while (reader.hasRemaining()) {
			int tag = reader.readTag();

			if (tag == MIME) {
				mime = reader.readString();
				continue;
			} else if (tag == IMGURL) {
				type = TYPE_IMAGE_URL;
			} else if (tag == IFRAMEURL) {
				type = TYPE_IFRAME_URL;
			} else if (tag == HTML) {
				type = TYPE_HTML_MARKUP;
			} else {
				continue;
			}
			data = reader.readString();
		}
		return new IconDisplayAsset(type, mime, data);
	}

	private final String mime;
	private final String data;
	private final @Type int type;

	private IconDisplayAsset(@Type int type, String mime, String data) {
		this.mime = mime;
		this.data = data;
		this.type = type;
	}

	/**
	 * Asset MIME type.
	 *
	 * @return MIME type
	 * @since 1.2
	 */
	public String mime() {
		return this.mime;
	}

	/**
	 * Asset data.
	 *
	 * @param exp expected asset type
	 * @return asset data
	 * @throws IllegalStateException asset type is not {@code exp}
	 */
	private String data(@Type int exp) {
		Preconditions.checkState(this.type == exp);
		return this.data;
	}

	/**
	 * Test whether asset is an image URL asset.
	 *
	 * @return {@code true} if, and only if, image asset
	 * @since 1.2
	 * @see #imageUrl()
	 */
	public boolean isImageUrlAsset() {
		return this.type == TYPE_IMAGE_URL;
	}

	/**
	 * Image asset URL.
	 *
	 * @return asset URL
	 * @throws IllegalStateException not an {@linkplain #isImageUrlAsset() image URL} asset
	 * @since 1.2
	 * @see #isImageUrlAsset()
	 */
	public String imageUrl() {
		return this.data(TYPE_IMAGE_URL);
	}

	/**
	 * Test whether asset is an HTML document URL.
	 *
	 * @return {@code true} if, and only if, HTML document URL asset
	 * @since 1.2
	 * @see #iframeUrl()
	 */
	public boolean isIframeUrlAsset() {
		return this.type == TYPE_IFRAME_URL;
	}

	/**
	 * HTML document URL.
	 *
	 * @return asset URL
	 * @throws IllegalStateException not an {@linkplain #isIframeUrlAsset() HTML document URL} asset
	 * @since 1.2
	 * @see #isIframeUrlAsset()
	 */
	public String iframeUrl() {
		return this.data(TYPE_IFRAME_URL);
	}

	/**
	 * Test whether asset is a HTML snippet markup.
	 *
	 * @return {@code true} if, and only if, HTML snippet markup asset
	 * @since 1.2
	 * @see #htmlMarkup()
	 */
	public boolean isHtmlMarkupAsset() {
		return this.type == TYPE_HTML_MARKUP;
	}

	/**
	 * HTML snippet markup.
	 *
	 * @return HTML markup
	 * @throws IllegalStateException not a {@linkplain #isHtmlMarkupAsset() HTML snippet markup} asset
	 * @since 1.2
	 * @see #isHtmlMarkupAsset()
	 */
	public String htmlMarkup() {
		return this.data(TYPE_HTML_MARKUP);
	}

	@Override
	public void toProtobuf(ProtobufWriter writer) {
		writer.writeString(MIME, this.mime);

		int tag;

		switch (this.type) {
		case TYPE_HTML_MARKUP:
			tag = HTML;
			break;
		case TYPE_IFRAME_URL:
			tag = IFRAMEURL;
			break;
		case TYPE_IMAGE_URL:
			tag = IMGURL;
			break;
		default:
			return;
		}
		writer.writeString(tag, this.data);
	}
}
