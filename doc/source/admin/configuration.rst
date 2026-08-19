=============
Configuration
=============

All configuration is supplied at runtime — nothing deployment-specific is
packaged into the jar. The main file is ``config/application.yml``; it points
to the other configuration files.

application.yml
===============

.. list-table::
   :widths: 32 68
   :header-rows: 1

   * - Key
     - Meaning
   * - ``spring.datasource.*``
     - PostgreSQL connection (URL, username, password).
   * - ``spring.elasticsearch.*``
     - Elasticsearch connection (default ``localhost:9200``); relevant only
       with the Elasticsearch engine.
   * - ``search.engine``
     - Search engine: ``elasticsearch`` (default) or ``lucene`` (embedded,
       no external server).
   * - ``search.lucene.path``
     - Directory of the persisted embedded index; unset = in-memory
       (rebuilt on every start).
   * - ``search.content-locale``
     - Language of the described material, as an IETF language tag (default
       ``cs-CZ``); its alphabet orders name-sorted results for every visitor
       — this is not the visitor's own locale. Changing it reindexes at the
       next startup (see :doc:`operations`); an invalid tag stops the
       startup.
   * - ``types-config``
     - Path to ``types.yaml`` (see below).
   * - ``webResources.pageTemplate``
     - Portal name, localizations and optional menu definition
       (``pageTemplate.yaml``).
   * - ``webResources.news``
     - News shown on the portal home page (``news.yaml``).
   * - ``webResources.favoriteQueries``
     - Preconfigured favourite queries (``favoriteQueries.yaml``).
   * - ``webResources.facets``
     - Facet — and search relevance — configuration
       (``searchConfig.yaml``, see :doc:`search`).
   * - ``webResources.logo`` / ``webResources.topImage``
     - Branding images served to the UI.
   * - ``help-url``
     - External help link shown in the default menu.
   * - ``import.input-dir``
     - Optional input-directory import — a file-based alternative to the
       Transfagent upload (see :doc:`operations`). Unset = off.
   * - ``files.storage``, ``files.transfer.path``
     - File storage of attachments and the working directory of the SOAP
       file transfer.
   * - ``tile.folder``, ``tile.format``
     - Deep-zoom tile cache for large images.
   * - ``transformation-agent-url``
     - SOAP endpoint of the external Transform agent (file/format
       transformations).
   * - ``server.servlet.context-path``
     - Optional subpath for standalone subpath deployments (see
       :doc:`install`).

types.yaml
==========

The display and indexing model of description items ("item types"). For each
item type it defines the data type (``STRING``, ``ENUM``, ``APU_REF``,
``INTEGER``, ``UNITDATE``, ``LINK``, …), the Czech display label and the
indexing flags:

- ``indexed`` — whether the item is written to the search index at all;
- ``fulltext`` — whether the item's value participates in the general
  fulltext search (default: yes; see :doc:`search`);
- ``nameVariant`` — marks the item as a variant name form of the record
  (the other names of an access point); its values rank just below the
  primary name (default: no; see :doc:`search`).

.. important::

   ``types.yaml`` describes **what the search index contains**. Any change to
   its indexed fields is detected at startup and triggers an automatic
   rebuild and reindex (see :doc:`operations`). Weight tuning does *not*
   belong here — see the ``relevance`` section of ``searchConfig.yaml``.

types_localization.yaml
=======================

Optional file **next to** ``types.yaml``; translations of the display labels
into the languages the portal offers. The names in ``types.yaml`` are the
source language and remain the fallback, so a deployment without this file
behaves exactly as before.

.. code-block:: yaml

   partTypes:
     en:
       PT_TITLE: Title
   itemTypes:
     en:
       TITLE_MAIN: Main title

Codes are written in the underscore form used in ``types.yaml``. The language
keys should match the ``localizations`` declared in ``pageTemplate.yaml`` —
those are the languages clients may ask for.

.. note::

   Which language a response is rendered in comes from the request's ``lang``
   parameter (an IETF tag such as ``en`` or ``cs-CZ``), matched against the
   configured ``localizations``; absent or unmatched, the first configured
   localization applies. This is the **reader's** language and affects display
   text only — never which records match, nor their order, which follows
   ``search.content-locale``.

Dating vocabulary
=================

Datings are rendered by the server, because presenting one means applying the
archival grammar — granularity (century, year, month, date), estimated bounds
in brackets, intervals whose sides collapse when equal — and not merely
formatting a date. The dates themselves come from the JDK's CLDR data, so
month names and date order are correct in any language without configuration.

The words around them — how a century reads, the interval separator — are not
in CLDR and ship as resource bundles in the application
(``cz/aron/web/v1/unitdate*.properties``): Czech as the base, plus one file per
further language. Supporting a new language therefore means adding one
properties file, not writing date patterns.

Alongside the rendered string, the API also carries the dating in machine form
(``dating``: bounds, granularity, estimate flags) — for semantic markup and for
consumers that process datings rather than display them.

searchConfig.yaml
=================

Two concerns, both deployment-specific:

- ``facets:`` — which search filters each portal section offers, their type
  (``FULLTEXT``, ``ENUM``, ``MULTI_REF``, ``UNITDATE``, …), labels, ordering
  and display rules;
- ``relevance:`` — the fulltext ranking weights (see :doc:`search`).

Changes take effect after a restart; **no reindex is required**.

pageTemplate.yaml
=================

Portal name, available localizations and optionally the main menu (section
codes, colors, external URLs). Without a ``menu:`` block the default menu is
used (funds, archival description, entities + help from ``help-url``).
Clients never receive raw server configuration files — the UI reads a typed
``/api/v1/ui/config`` endpoint.

.. code-block:: yaml

   name: Archiv online
   localizations:
     - cs_CZ
     - en

``localizations`` declares which languages the portal offers; the first is the
default. The portal ships UI strings for **Czech and English**, so those are
the languages a deployment can list today — the language switcher shows only
the ones declared here *and* present in the build, and a single declared
language shows no switcher at all.

Declaring a language covers the interface strings and the datings. The labels
of the description items follow only if ``types_localization.yaml`` translates
them; untranslated ones fall back to the ``types.yaml`` names.
