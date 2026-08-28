# SPDX-License-Identifier: MIT OR Apache-2.0
#
# This replaces the standard `code-block` directive with a directive which allows text substitution.
# The `subst` option can be used, on a per-directive basis, to define a JSON object mapping
# substitution key to substitution value. The global `origin_code_block_subst` configuration can
# be set to a substitution mapping dictionary, which is applied to all code blocks. Substitution
# mappings specified in the `subst` option take precedence over the global `origin_code_block_subst`.
#

from typing import List

from docutils.parsers.rst import directives

from sphinx.application import Sphinx
from sphinx.directives.code import CodeBlock
from sphinx.util.docutils import SphinxDirective

import json

class SubstCodeBlock(SphinxDirective):
	"""
	A `code-block` directive permitting text substitution.
	"""

	final_argument_whitespace = CodeBlock.final_argument_whitespace
	has_content = CodeBlock.has_content
	option_spec = {
		**CodeBlock.option_spec,
		"subst": directives.unchanged_required
	}
	optional_arguments = CodeBlock.optional_arguments + 1
	required_arguments = CodeBlock.required_arguments

	def _resolve_subst(self) -> dict[str, str]:
		cfg = self.env.app.config.origin_code_block_subst
		opt = self.options.get("subst")
		if not opt:
			return cfg

		subst = dict(cfg or {})
		for k, v in json.loads(opt).items():
			subst[k] = f"{v}"
		return subst

	def run(self) -> List:
		code = self.content
		subst = self._resolve_subst()
		if subst:
			code = []
			for line in self.content:
				for k, v in subst.items():
					line = line.replace(k, v)
				code.append(line)
		return CodeBlock(
			arguments = self.arguments,
			block_text = self.block_text,
			content = code,
			content_offset = self.content_offset,
			lineno = self.lineno,
			name = self.name,
			options = { **(self.options or {}), "subst": None },
			state = self.state,
			state_machine = self.state_machine
		).run()

def setup(app: Sphinx) -> dict[str, any]:
	app.add_config_value(
		"origin_code_block_subst",
		{},
		"",
		types = dict,
		description = "Global substitution mapping applied to all code blocks."
	)
	app.add_directive("code-block", SubstCodeBlock, override = True)
	return {
		"env_version": 1,
		"version": "1.0",
		"parallel_read_safe": True,
		"parallel_write_safe": True
	}
