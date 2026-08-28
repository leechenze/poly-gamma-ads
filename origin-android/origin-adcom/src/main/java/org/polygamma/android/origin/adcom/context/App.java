// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.adcom.context;

import static org.polygamma.android.origin.protobuf.ProtobufField.*;

import android.annotation.SuppressLint;

import androidx.annotation.ReturnThis;

import org.polygamma.android.origin.adcom.enums.AdComEnums;
import org.polygamma.android.origin.adcom.enums.CategoryTaxonomyCode;
import org.polygamma.android.origin.protobuf.ProtobufReader;
import org.polygamma.android.origin.protobuf.ProtobufWriter;
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

	/*private static final @Tag int DOMAIN		= ofString(  1);*/
	/*private static final @Tag int CAT			= ofString(  2);*/
	private static final @Tag int SECTCAT		= ofString(  3);
	private static final @Tag int PAGECAT		= ofString(  4);
	private static final @Tag int CATTAX		= ofInt32(   5);
	/*private static final @Tag int PRIVPOLICY	= ofBool(    6);*/
	/*private static final @Tag int KWARRAY		= ofString(  7);*/
	/*private static final @Tag int BUNDLE		= ofString(  8);*/
	private static final @Tag int STOREID		= ofString(  9);
	/*private static final @Tag int STOREURL	= ofString( 10);*/
	private static final @Tag int VER			= ofString( 11);
	private static final @Tag int PAID			= ofBool(   12);
	private static final @Tag int DEBUG			= ofBool(  500);
	private static final @Tag int SYSTEM		= ofBool(  501);

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
	 * @param reader reader to deserialize from
	 * @return deserialized app
	 * @throws RuntimeException coding is malformed
	 * @since 1.2
	 */
	public static App ofProtobuf(ProtobufReader reader) {
		App rv = new App();
		List<String> sectCats = new ArrayList<>();
		List<String> pageCats = new ArrayList<>();

		while (reader.hasRemaining()) {
			int tag = reader.readTag();

			if (tag == ID) {
				rv.id = reader.readString();
			} else if (tag == NAME) {
				rv.name = reader.readString();
			} else if (tag == PUB) {
				int cookie = reader.beginReadLen();

				while (reader.hasRemaining()) {
					if (reader.readTag() == PUB_ID)
						rv.publisherId = reader.readString();
				}
				reader.endReadLen(cookie);
			} else if (tag == APP) {
				int cookie = reader.beginReadLen();

				while (reader.hasRemaining()) {
					tag = reader.readTag();
					if (tag == SECTCAT)
						sectCats.add(reader.readString());
					else if (tag == PAGECAT)
						pageCats.add(reader.readString());
					else if (tag == CATTAX)
						rv.categoryTaxonomy = reader.readInt32();
					else if (tag == STOREID)
						rv.storeId = reader.readString();
					else if (tag == VER)
						rv.version = reader.readString();
					else if (tag == PAID && reader.readBool())
						rv.flags |= FLAG_PAID;
					else if (tag == DEBUG && reader.readBool())
						rv.flags |= FLAG_DEBUG;
					else if (tag == SYSTEM && reader.readBool())
						rv.flags |= FLAG_SYSTEM;
				}
				reader.endReadLen(cookie);
			}
		}
		rv.sectionCategories = CollectionsCompat.toStringArrayOrEmpty(sectCats);
		rv.pageCategories = CollectionsCompat.toStringArrayOrEmpty(pageCats);
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
	public void toProtobuf(ProtobufWriter writer) {
		super.toProtobuf(writer);

		long cookie = writer.beginWriteLen(DistributionChannel.APP);

		writer.writeString(STOREID, this.storeId);
		writer.writeString(VER, this.version);
		writer.writeRepeatString(SECTCAT, this.sectionCategories);
		writer.writeRepeatString(PAGECAT, this.pageCategories);
		writer.writeInt32(CATTAX, this.categoryTaxonomy);
		writer.writeBool(PAID, this.paid());
		writer.writeBool(DEBUG, this.debuggable());
		writer.writeBool(SYSTEM, this.system());
		writer.endWriteLen(cookie);
	}
}
