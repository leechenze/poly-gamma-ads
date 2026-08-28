// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.javac;

import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.PackageTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Name;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Listener which initiates repackaging of source files.
 */
final class RepackagingTaskListener implements TaskListener {

	private final class ScannerImpl extends TreeScanner<Void, Void> {

		private ScannerImpl() {
		}

		private ArrayList<Name> makeTargetPackageNames(Name.Table table) {
			ArrayList<Name> rv =
				new ArrayList<>(RepackagingTaskListener.this.targetPackage.length);

			for (String part : RepackagingTaskListener.this.targetPackage)
				rv.add(table.fromString(part));
			return rv;
		}

		private JCTree.JCExpression frobPackage(JCTree.JCExpression root) {
			List<Name> names = new ArrayList<>(1);

			while (true) {
				if (root instanceof JCTree.JCIdent) {
					names.addFirst(((JCTree.JCIdent) root).name);
					root = null;
					break;
				} else if (!(root instanceof JCTree.JCFieldAccess)) {
					break;
				}

				JCTree.JCFieldAccess field = (JCTree.JCFieldAccess) root;

				names.addFirst(field.name);
				root = field.selected;
			}

			if (names.size() < RepackagingTaskListener.this.sourcePackage.length)
				return null;

			Name.Table table = names.getFirst().table;

			for (String part : RepackagingTaskListener.this.sourcePackage) {
				if (!names.removeFirst().contentEquals(part))
					return null;
			}

			TreeMaker maker = TreeMaker.instance(RepackagingTaskListener.this.context);
			ArrayList<Name> target = this.makeTargetPackageNames(table);

			if (root == null)
				root = maker.Ident(target.removeFirst());
			target.addAll(names);
			while (!target.isEmpty())
				root = maker.Select(root, target.removeFirst());
			return root;
		}

		@Override
		public Void visitMemberSelect(MemberSelectTree sel, Void ignored) {
			JCTree.JCFieldAccess impl = (JCTree.JCFieldAccess) sel;
			JCTree.JCExpression expr = this.frobPackage(impl.selected);

			if (expr != null)
				impl.selected = expr;
			return super.visitMemberSelect(sel, null);
		}

		@Override
		public Void visitVariable(VariableTree var, Void ignored) {
			JCTree.JCVariableDecl impl = (JCTree.JCVariableDecl) var;
			JCTree.JCExpression expr = this.frobPackage(impl.vartype);

			if (expr != null)
				impl.vartype = expr;
			return super.visitVariable(var, ignored);
		}

		@Override
		public Void visitImport(ImportTree imp, Void ignored) {
			JCTree.JCImport impl =(JCTree.JCImport) imp;
			JCTree.JCExpression expr = this.frobPackage(impl.qualid);

			if (expr != null)
				impl.qualid = (JCTree.JCFieldAccess) expr;
			return super.visitImport(imp, ignored);
		}

		@Override
		public Void visitPackage(PackageTree pkg, Void ignored) {
			JCTree.JCPackageDecl impl = (JCTree.JCPackageDecl) pkg;
			JCTree.JCExpression expr = this.frobPackage(impl.pid);

			if (expr != null)
				impl.pid = expr;
			return super.visitPackage(pkg, ignored);
		}
	}

	private final Context context;
	private final String[] sourcePackage;
	private final String[] targetPackage;

	/**
	 * Construct new listener.
	 *
	 * @param ctxt compilation context
	 * @param dstPkg package to rename to
	 * @param srcPkg source package to rename
	 */
	RepackagingTaskListener(Context ctxt, String dstPkg, String srcPkg) {
		this.context = ctxt;
		this.targetPackage = dstPkg.split("\\.");
		this.sourcePackage = srcPkg.split("\\.");
	}

	@Override
	public void finished(TaskEvent evt) {
		if (evt.getKind() == TaskEvent.Kind.PARSE)
			evt.getCompilationUnit().accept(new ScannerImpl(), null);
	}
}
