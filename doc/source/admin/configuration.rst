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
  fulltext search (default: yes; see :doc:`search`).

.. important::

   ``types.yaml`` describes **what the search index contains**. Any change to
   its indexed fields is detected at startup and triggers an automatic
   rebuild and reindex (see :doc:`operations`). Weight tuning does *not*
   belong here — see the ``relevance`` section of ``searchConfig.yaml``.

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
