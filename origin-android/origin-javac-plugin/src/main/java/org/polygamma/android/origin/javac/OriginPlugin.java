// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.javac;

import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;
import com.sun.tools.javac.api.BasicJavacTask;

/**
 * Origin Java compiler plugin.
 * <p>This plugin accepts two arguments, the vendor namespace and the standard namespace. If the
 * vendor and standard namespaces are different, then all code is refactored under the vendor
 * namespace.
 */
public class OriginPlugin implements Plugin {

	/**
	 * Construct new plugin.
	 */
	public OriginPlugin() {
	}

	@Override
	public String getName() {
		return "PgOrigin";
	}

	@Override
	public void init(JavacTask task, String... args) {
		if (args.length != 2)
			return;

		String dstPkg = args[0];
		String srcPkg = args[1];

		if (srcPkg.equals(dstPkg))
			return;

		task.addTaskListener(new RepackagingTaskListener(
			((BasicJavacTask) task).getContext(),
			dstPkg,
			srcPkg
		));
	}
}
