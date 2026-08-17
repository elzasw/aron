# Configuration file for the Sphinx documentation builder.
#
# For the full list of built-in configuration values, see the documentation:
# https://www.sphinx-doc.org/en/master/usage/configuration.html

# -- Project information -----------------------------------------------------

project = 'ARON 2'
copyright = '2026, LightComp v.o.s.'
author = 'LightComp v.o.s.'

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
