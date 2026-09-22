======================
What's new in ARON 2.0
======================

ARON 2.0 is the second generation of the portal. It keeps the data model, the
configuration files and the public URLs of the old portal, and replaces the
rest: the reader interface, the search layer, the API and the way the portal is
built and deployed. This page summarizes what a deployment gets and where in
this guide each part is described. A deployment of the old portal finds the
steps it has to take in :ref:`upgrading-from-old-portal`.

One application, one artifact
=============================

The backend and the web interface are **one executable jar**, released inside
a bundle together with a complete default configuration for a Czech archive and
an installation readme. The same jar serves the portal at the root of a URL and
under a subpath, chosen by configuration or by the reverse proxy, without a
rebuild. There is no separately deployed frontend and no static configuration
file served next to it.

The internal SOAP interfaces for Transfagent live under ``/cxf/*``, outside the
public ``/api`` namespace, and can be moved to a **listener of their own**
(``soap.port``) so that the public port never answers them.
See :doc:`../admin/install`.

A new reader interface
======================

The web interface is rewritten. It is **responsive** — the search page stacks
its sidebar and the header folds its section tabs on a narrow screen — and
built to be **accessible** (keyboard operation, ARIA labeling, WCAG 2.1 AA as
the target), because the public-sector bodies that deploy the portal are legally
required to publish an accessible site. What the reader sees:

- **A configurable home page**: the search box plus groups of tiles that lead
  into a section's search or to an external page, and a footer band of prose
  and links, all typed configuration in ``pageTemplate.yaml``. No raw HTML is
  configured anywhere.
- **A record page** with the archival description as a resizable tree pane, the
  record's description beside it and, when the record has digitized material,
  the **scan shown immediately** in an embedded deep-zoom viewer; further
  pages and files stay one click away. The viewer also has a page of its own
  for deep links and the browser's full-screen mode.
- **Search** with a dual-thumb dating slider, checkbox facets with optional
  explanations, an **advanced search** dialog that offers a section's whole
  facet list as a draft and counts the results before it is applied, and
  **find related**, which lists every record that references the one on
  screen.
- **Citations** of a record in the form the deployment's archivists agreed on,
  copied verbatim to the clipboard.
- **Czech and English** interfaces. The reader's language is remembered; a
  deployment sets the default and which languages are offered.

See :doc:`../admin/configuration` for the page template, the home page, the
citation forms and the structured search results.

Search
======

Search is built on an engine-neutral layer with **two interchangeable
engines**: Elasticsearch, the production engine, and an **embedded Lucene**
engine that needs no external service and suits smaller deployments, test
installations and development. Switching is a configuration change. The
Elasticsearch index uses **built-in analyzers only**; no analysis plugin has to
be installed on the server.

Results are **ranked by relevance** with field weights a deployment can tune,
or ordered by name or by dating. A query matches all its words, supports quoted
phrases and a trailing ``*``, matches Czech inflected forms and ignores
diacritics. Facets can depend on another facet's selection, order their leading
values by the deployment's choice and carry a tooltip per value. The general
search across all record types has built-in facets for the record type, the
dating and the related records.

A dating filter matches the record's **individual datings**, not the span
between them: a record dated 1850–1860 and again in 1600 is not found for 1700.
Undated records are excluded from a dating filter, and the reader is told how
many there are and can include them. See :doc:`../admin/search`.

Data ingest and the search index
================================

The search index is **written after an import has committed, from the
database**. A failed import therefore changes nothing in the index, and a
renamed fund renames itself in every description's card and reference facet
without those descriptions being re-delivered. Records to re-index can be
requested with one SQL statement; a full rebuild happens by itself whenever the
indexed-fields configuration changes.

Besides the SOAP file transfer from Transfagent, transfer folders in a
configured **input directory** are imported at startup, with a journal that
skips unchanged folders. Transfagent **withdraws** published data through a
management SOAP interface that shares the import's lock, so a deletion can never
interleave with an import. See :doc:`../admin/operations`.

A new API, and the old one kept
===============================

The portal has a new REST API under ``/api/v1``, defined in a TypeSpec contract
whose OpenAPI description is published with the sources. It is the API the new
interface uses and the one third parties should build on. The old API under
``/api/aron`` is **preserved verbatim**, including the permalink redirects, so
existing links and clients keep working; it is frozen and receives fixes only.

Configuration
=============

The configuration files of the old portal keep their names and their content:
``application.yml``, the display model ``types.yaml``, the facets
``searchConfig.yaml``, ``pageTemplate.yaml``. What is new:

- **Translations live in sibling files**: ``types_localization.yaml``,
  ``searchConfig_localization.yaml`` and the like translate the configured
  labels without editing the configured file. The reader's language is a
  request parameter of the API, so responses stay cacheable.
- **Datings are rendered by the server** in the reader's language, with the
  archival grammar of estimates, centuries and intervals.
- **Structured search results** (record cards prepared by the source system)
  are shown whenever the record carries one; their presentation is configured
  in ``resultLayout.yaml``.
- **Citation forms** are configured in ``citation.yaml``, several per
  deployment, each with the record types it covers and its own script.
- **Mistakes fail the startup**: an unknown key, a facet condition the portal
  cannot read, a home-page tile that leads to a search the portal would reject,
  an image that cannot be served. The error names the file and the value, so a
  configuration mistake is found by the administrator, not by a reader.
- **Disclosure is opt-in**: the running version and the diagnostic reason of a
  failed request are sent only where a deployment turns them on.

See :doc:`../admin/configuration`.
