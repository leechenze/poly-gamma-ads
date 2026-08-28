# SPDX-License-Identifier: MIT OR Apache-2.0
#
# Configuration file for the Sphinx documentation builder.
#
# For the full list of built-in configuration values, see the documentation:
# https://www.sphinx-doc.org/en/master/usage/configuration.html

from configparser import ConfigParser
from pathlib import Path

import os
import sys

sys.path.append(str((Path(__file__).parent / "_extensions").resolve()))

origin_vendor_group = None
origin_vendor_name = None
origin_vendor_namespace = None
origin_version = None
origin_version_next_major = None
origin_release = None
origin_region = None

with (Path(__file__).parent / ".." / "gradle.properties").open() as props_file:
	parser = ConfigParser()
	parser.read_string(f"[DEFAULT]{os.linesep}{props_file.read()}")

	config = parser["DEFAULT"]
	origin_vendor_group = config["origin.vendor.group"]
	origin_vendor_name = config["origin.vendor.name"]
	origin_vendor_namespace = config["origin.vendor.namespace"]
	origin_region = config["origin.region"]

	ver_major = int(config["origin.version.major"])
	ver_minor = int(config["origin.version.minor"])
	ver_patch_level = int(config["origin.version.patch-level"])
	origin_version = f"{ver_major}.{ver_minor}.{ver_patch_level}"
	origin_version_next_major = f"{ver_major + 1}.0.0"
	if config["origin.version.development"] == "true":
		origin_release = f"{origin_version}-development"
		origin_version = f"0.{origin_version}"
		origin_version_next_major = f"0.{origin_version_next_major}"
	else:
		origin_release = origin_version

# -- Project information -----------------------------------------------------
# https://www.sphinx-doc.org/en/master/usage/configuration.html#project-information

author = f"{origin_vendor_name} Engineering"
copyright = f"2022-%Y, {origin_vendor_name}"
project = "Origin Android SDK"
release = origin_release
version = origin_version

tags.add(f"default_region_{origin_region}")
if origin_vendor_name == "Poly-Gamma":
	# Our documentation always includes everything, including development documentation.
	tags.add("document_development")

# -- General configuration ---------------------------------------------------
# https://www.sphinx-doc.org/en/master/usage/configuration.html#general-configuration

exclude_patterns = ["_build", "Thumbs.db", ".DS_Store"]
extensions = ["origin_subst_code_block", "sphinx.ext.graphviz", "sphinx_rtd_theme"]
highlight_language = "java"
language = "zh"
locale_dirs = ["locale/"]
nitpicky = True
primary_domain = None
rst_epilog = f"""
.. |vendor_group| replace:: {origin_vendor_group}
.. |vendor_name| replace:: {origin_vendor_name}
.. |vendor_namespace| replace:: {origin_vendor_namespace}
.. |version_current| replace:: {origin_version}
"""
templates_path = ["_templates"]

origin_code_block_subst = {
	"<vendor_group>": origin_vendor_group,
	"<vendor_name>": origin_vendor_name,
        "<vendor_name_lower>": origin_vendor_name.lower(),
	"<vendor_namespace>": origin_vendor_namespace,
	"<version_current>": origin_version,
	"<version_future>": origin_version_next_major
}

if tags.has("build_html"):
	exclude_patterns.append("*-latex.rst")
	exclude_patterns.append("*/*-latex.rst")

# -- Options for HTML output -------------------------------------------------
# https://www.sphinx-doc.org/en/master/usage/configuration.html#options-for-html-output

html_copy_source = False
html_favicon = "./_static/favicon.ico"
html_logo = "./_static/logo.png"
html_short_title = "Origin Android SDK"
html_show_sphinx = False
html_static_path = ["_static"]
html_theme = "sphinx_rtd_theme"
html_theme_options = { "display_version": True }
html_title = f"{origin_vendor_name} - Origin Android SDK"
html_use_opensearch = "https://docs.poly-gamma.org"

# -- Options for LaTeX output ------------------------------------------------
# https://www.sphinx-doc.org/en/master/usage/configuration.html#options-for-latex-output

latex_documents = [(
	"index-latex",
	"index.tex",
	f"{origin_vendor_name} - Origin Android SDK",
	f"{origin_vendor_name} Engineering",
	"manual"
)]
latex_elements = {
	"fontpkg": r"""
\setmainfont{FreeSerif}[
	UprightFont = *,
	ItalicFont = *Italic,
	BoldFont = *Bold,
	BoldItalicFont = *BoldItalic
]
\setsansfont{FreeSans}[
	UprightFont = *,
	ItalicFont = *Oblique,
	BoldFont = *Bold,
	BoldItalicFont = *BoldOblique
]
\setmonofont{FreeMono}[
	UprightFont = *,
	ItalicFont = *Oblique,
	BoldFont = *Bold,
	BoldItalicFont = *BoldOblique
]
""",
	"preamble": r"""
% we prefer code-blocks not to split
\sloppy
\widowpenalty=300
\clubpenalty=300
\setlength{\parskip}{3ex plus 2ex minus 2ex}
""",
	"title": f"{origin_vendor_name} - Origin Android SDK"
}
latex_engine = "xelatex"
latex_logo = "./_static/logo.png"
latex_show_urls = "footnote"
