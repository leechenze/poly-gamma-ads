// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.adcom.context;

import static org.polygamma.android.origin.protobuf.Protobuf.*;

import android.annotation.SuppressLint;

import androidx.annotation.ReturnThis;

import org.polygamma.android.origin.adcom.enums.AdComEnums;
import org.polygamma.android.origin.adcom.enums.CategoryTaxonomyCode;
import org.polygamma.android.origin.protobuf.ProtobufDecoder;
import org.polygamma.android.origin.protobuf.ProtobufEncoder;
import org.polygamma.android.origin.util.CollectionsCompat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Non-browser application.
 *
 * @since 0.1
 * @see <a href="https://github.com/InteractiveAdvertisingBureau/AdCOM/blob/main/AdCOM%20v1.0%20FINAL.md#object--app-">AdCOM, version 1.0 - Object: App</a>
 */
public final class App extends DistributionChannel {

	/*private static final @FieldTag int DOMAIN		= fieldTagOf(  1, WIRE_LEN);*/
	/*private static final @FieldTag int CAT		= fieldTagOf(  2, WIRE_LEN);*/
	private static final @FieldTag int SECTCAT		= fieldTagOf(  3, WIRE_LEN);
	private static final @FieldTag int PAGECAT		= fieldTagOf(  4, WIRE_LEN);
	private static final @FieldTag int CATTAX		= fieldTagOf(  5, WIRE_VARINT);
	/*private static final @FieldTag int PRIVPOLICY	= fieldTagOf(  6, WIRE_VARINT);*/
	/*private static final @FieldTag int KWARRAY	= fieldTagOf(  7, WIRE_LEN);*/
	/*private static final @FieldTag int BUNDLE		= fieldTagOf(  8, WIRE_LEN);*/
	private static final @FieldTag int STOREID		= fieldTagOf(  9, WIRE_LEN);
	/*private static final @FieldTag int STOREURL	= fieldTagOf( 10, WIRE_LEN);*/
	private static final @FieldTag int VER			= fieldTagOf( 11, WIRE_LEN);
	private static final @FieldTag int PAID			= fieldTagOf( 12, WIRE_VARINT);
	private static final @FieldTag int DEBUG		= fieldTagOf(500, WIRE_VARINT);
	private static final @FieldTag int SYSTEM		= fieldTagOf(501, WIRE_VARINT);

	private static final int FLAG_PAID		= 0x01;
	private static final int FLAG_DEBUG		= 0x02;
	private static final int FLAG_SYSTEM	= 0x04;

	/**
	 * Empty application instance.
	 */
	private static final App DEFAULT = new App();

	/**
	 * Non-browser {@linkplain App application} builder.
	 *
	 * @since 1.2
	 * @see #ofBuilder()
	 */
	public static final class Builder {

		private App app;
		private boolean needClone;

		private Builder(App app) {
			this.app = app;
			this.needClone = true;
		}

		private App target() {
			if (this.needClone) {
				this.app = new App(this.app);
				this.needClone = false;
			}
			return this.app;
		}

		/**
		 * Set {@linkplain App#id() id} of app, unique to vendor.
		 *
		 * @param id app id or {@linkplain String#isEmpty() empty} if unknown
		 * @return {@code this}
		 * @since 1.2
		 */
		@SuppressLint("RestrictedApi")
		@ReturnThis
		public Builder id(String id) {
			this.target().id = id;
			return this;
		}

		/**
		 * Set app {@linkplain App#name() name}.
		 *
		 * @param name app name or {@linkplain String#isEmpty() empty} if unknown
		 * @return {@code this}
		 * @since 1.2
		 */
		@SuppressLint("RestrictedApi")
		@ReturnThis
		public Builder name(String name) {
			this.target().name = name;
			return this;
		}

		/**
		 * Set id of publisher, unique to vendor, which distributes app.
		 *
		 * @param id publisher id or {@linkplain String#isEmpty() empty} if unknown
		 * @return {@code this}
		 * @since 1.2
		 */
		@SuppressLint("RestrictedApi")
		@ReturnThis
		public Builder publisherId(String id) {
			this.target().publisherId = id;
			return this;
		}

		/**
		 * Set store assigned {@linkplain App#storeId() id} of app.
		 *
		 * @param id store id or {@linkplain String#isEmpty() empty} if unknown
		 * @return {@code this}
		 * @since 1.2
		 */
		@ReturnThis
		public Builder storeId(String id) {
			this.target().storeId = id;
			return this;
		}

		/**
		 * Set app {@linkplain App#version() version}.
		 *
		 * @param ver app version or {@linkplain String#isEmpty() empty} if unknown
		 * @return {@code this}
		 * @since 1.2
		 */
		@ReturnThis
		public Builder version(String ver) {
			this.target().version = ver;
			return this;
		}

		/**
		 * Set {@linkplain App#sectionCategory(int) categories} describing current app view section.
		 *
		 * @param cats categories or {@linkplain Collection#isEmpty() empty} if unknown
		 * @return {@code this}
		 * @since 1.2
		 */
		@ReturnThis
		public Builder sectionCategories(Collection<String> cats) {
			this.target().sectionCategories = cats.isEmpty() ? null : cats.toArray(new String[0]);
			return this;
		}

		/**
		 * Set {@linkplain App#pageCategory(int) categories} describing current app view.
		 *
		 * @param cats categories or {@linkplain Collection#isEmpty() empty} if unknown
		 * @return {@code this}
		 * @since 1.2
		 */
		@ReturnThis
		public Builder pageCategories(Collection<String> cats) {
			this.target().pageCategories = cats.isEmpty() ? null : cats.toArray(new String[0]);
			return this;
		}

		/**
		 * Set {@linkplain App#categoryTaxonomy() code} of taxonomy in which categories are
		 * defined.
		 *
		 * @param code taxonomy code or {@code 0} if unknown
		 * @return {@code this}
		 * @since 1.2
		 */
		@ReturnThis
		public Builder categoryTaxonomy(@CategoryTaxonomyCode int code) {
			this.target().categoryTaxonomy = code;
			return this;
		}

		private void toggleFlag(int flag, boolean set) {
			App dst = this.target();

			if (set)
				dst.flags |= flag;
			else
				dst.flags &= ~flag;
		}

		/**
		 * Set whether paid version of app is being used.
		 *
		 * @param paid {@code true} if, and only if, paid version of app is being used
		 * @return {@code this}
		 * @since 1.2
		 */
		@ReturnThis
		public Builder paid(boolean paid) {
			this.toggleFlag(FLAG_PAID, paid);
			return this;
		}

		/**
		 * Set whether app is debuggable.
		 *
		 * @param debug {@code true} if, and only if, app is debuggable
		 * @return {@code this}
		 * @since 1.2
		 */
		@ReturnThis
		public Builder debuggable(boolean debug) {
			this.toggleFlag(FLAG_DEBUG, debug);
			return this;
		}

		/**
		 * Set whether app is installed as part of the operating system image.
		 *
		 * @param sys {@code true} if, and only if, app is a system application
		 * @return {@code this}
		 * @since 1.2
		 */
		@ReturnThis
		public Builder system(boolean sys) {
			this.toggleFlag(FLAG_SYSTEM, sys);
			return this;
		}

		/**
		 * Build resulting app.
		 *
		 * @return resulting app instance
		 * @since 1.2
		 */
		public App build() {
			this.needClone = true;
			return this.app;
		}
	}

	/**
	 * Empty app instance.
	 *
	 * @return app instance
	 * @since 1.2
	 */
	public static App of() {
		return DEFAULT;
	}

	/**
	 * Construct new empty {@linkplain Builder builder} instance.
	 *
	 * @return builder instance
	 * @since 1.2
	 */
	public static Builder ofBuilder() {
		return DEFAULT.toBuilder();
	}

	/**
	 * Deserialize app from Protobuf message.
	 *
	 * @param dec decoder to deserialize from
	 * @return deserialized app
	 * @throws RuntimeException coding is malformed
	 * @since 1.2
	 */
	public static App ofProtobuf(ProtobufDecoder dec) {
		App rv = new App();

		while (dec.hasRemaining()) {
			int tag = dec.decodeFieldTag();

			if (tag == APP)
				dec.decodeLen(rv, App::mergeProtobuf);
			else
				DistributionChannel.decodeProtobufField(rv, dec, tag);
		}
		return rv;
	}

	private String storeId;
	private String version;
	private String[] sectionCategories;
	private String[] pageCategories;
	private @CategoryTaxonomyCode int categoryTaxonomy;
	private int flags;

	private App() {
		super();
		this.storeId = "";
		this.version = "";
		this.sectionCategories = this.pageCategories =
			CollectionsCompat.toStringArrayOrEmpty(Collections.emptyList());
		this.categoryTaxonomy = AdComEnums.CategoryTaxonomyUnknown;
	}

	private App(App that) {
		super(that);
		this.storeId = that.storeId;
		this.version = that.version;
		this.sectionCategories = that.sectionCategories;
		this.pageCategories = that.pageCategories;
		this.categoryTaxonomy = that.categoryTaxonomy;
		this.flags = that.flags;
	}

	private App mergeProtobuf(ProtobufDecoder dec) {
		List<String> sectCats = new ArrayList<>();
		List<String> pageCats = new ArrayList<>();

		while (dec.hasRemaining()) {
			int tag = dec.decodeFieldTag();

			if (tag == SECTCAT)
				sectCats.add(dec.decodeString());
			else if (tag == PAGECAT)
				pageCats.add(dec.decodeString());
			else if (tag == CATTAX)
				this.categoryTaxonomy = dec.decodeUint32();
			else if (tag == STOREID)
				this.storeId = dec.decodeString();
			else if (tag == VER)
				this.version = dec.decodeString();
			else if (tag == PAID)
				this.flags |= dec.decodeBool() ? FLAG_PAID : 0;
			else if (tag == DEBUG)
				this.flags |= dec.decodeBool() ? FLAG_DEBUG : 0;
			else if (tag == SYSTEM)
				this.flags |= dec.decodeBool() ? FLAG_SYSTEM : 0;
			else
				dec.skipFieldValue(tag);
		}
		this.sectionCategories = CollectionsCompat.toStringArrayOrEmpty(sectCats);
		this.pageCategories = CollectionsCompat.toStringArrayOrEmpty(pageCats);
		return this;
	}

	/**
	 * Count of categories describing current application view section.
	 *
	 * @return category count
	 * @since 1.2
	 * @see #sectionCategory(int)
	 */
	public int sectionCategoryCount() {
		return this.sectionCategories.length;
	}

	/**
	 * Category, at index, describing current application view section.
	 *
	 * @param i index to retrieve category at
	 * @return category at index {@code i}
	 * @throws RuntimeException {@code i} is negative or, greater than or equal to category
	 * {@linkplain #sectionCategoryCount() count}
	 * @since 1.2
	 * @see #sectionCategoryCount()
	 */
	public String sectionCategory(int i) {
		return this.sectionCategories[i];
	}

	/**
	 * Count of categories describing current application view.
	 *
	 * @return category count
	 * @since 1.2
	 * @see #pageCategory(int)
	 */
	public int pageCategoryCount() {
		return this.pageCategories.length;
	}

	/**
	 * Category, at index, describing current application view.
	 *
	 * @param i inddex to retrieve category at
	 * @return category at index {@code i}
	 * @throws RuntimeException {@code i} is negative or, greater than or equal to category
	 * {@linkplain #pageCategoryCount() count}
	 * @since 1.2
	 * @see #pageCategoryCount()
	 */
	public String pageCategory(int i) {
		return this.pageCategories[i];
	}

	/**
	 * Code of taxonomy in which categories are defined.
	 *
	 * @return category taxonomy code or {@code 0} if unknown
	 * @since 1.2
	 */
	public @CategoryTaxonomyCode int categoryTaxonomy() {
		return this.categoryTaxonomy;
	}

	/**
	 * App store assigned app id.
	 *
	 * @return app store assigned id
	 * @since 0.1
	 */
	public String storeId() {
		return this.storeId;
	}

	/**
	 * App version.
	 *
	 * @return version
	 * @since 0.1
	 */
	public String version() {
		return this.version;
	}

	/**
	 * Using paid version of app.
	 *
	 * @return {@code true} if, and only if, paid version of app is being used
	 * @since 0.1
	 */
	public boolean paid() {
		return (this.flags & FLAG_PAID) != 0;
	}

	/**
	 * App is debuggable.
	 *
	 * @return {@code true} if, and only if, app is debuggable
	 * @since 1.2
	 */
	public boolean debuggable() {
		return (this.flags & FLAG_DEBUG) != 0;
	}

	/**
	 * App is installed as part of the operating system image.
	 *
	 * @return {@code true} if, and only if, app is installed as part of the system image
	 * @since 1.2
	 */
	public boolean system() {
		return (this.flags & FLAG_SYSTEM) != 0;
	}

	/**
	 * Construct new builder initialized from {@code this}.
	 *
	 * @return builder instance
	 * @since 0.1
	 */
	public Builder toBuilder() {
		return new Builder(this);
	}

	@Override
	public void toProtobuf(ProtobufEncoder enc) {
		super.toProtobuf(enc);
		enc.encodeLenField(APP, this, (app, appEnc) -> {
			appEnc.encodeStringField(STOREID, app.storeId)
				.encodeStringField(VER, app.version)
				.encodeUnsignedIntField(CATTAX, app.categoryTaxonomy)
				.encodeBoolField(PAID, app.paid())
				.encodeBoolField(DEBUG, app.debuggable())
				.encodeBoolField(SYSTEM, app.system());
			for (String cat : app.sectionCategories)
				appEnc.encodeStringField(SECTCAT, cat);
			for (String cat : app.pageCategories)
				appEnc.encodeStringField(PAGECAT, cat);
		});
	}
}
