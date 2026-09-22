# Configuration file for the Sphinx documentation builder.
#
# For the full list of built-in configuration values, see the documentation:
# https://www.sphinx-doc.org/en/master/usage/configuration.html

import os

# -- Project information -----------------------------------------------------

project = 'ARON 2'
copyright = '2026, LightComp v.o.s.'
author = 'LightComp v.o.s.'

# The version line these pages describe ("2.0"), supplied by the build: the
# pipeline derives it from the release branch, a local build shows "dev". It is
# never written into the repository - the pom is the source of truth for
# versions. Sphinx puts it into the default html_title.
version = os.environ.get('ARON_DOC_VERSION', 'dev')
release = version

# -- General configuration ---------------------------------------------------

extensions = [
    'sphinx.ext.todo',
]

# Administrator documentation is written in English (end-user help served by
# the portal itself is Czech).
language = 'en'

# If true, `todo` and `todoList` produce output, else they produce nothing.
todo_include_todos = True

# -- Options for HTML output -------------------------------------------------

html_theme = 'sphinx_rtd_theme'
