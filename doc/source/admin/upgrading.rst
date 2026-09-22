=========
Upgrading
=========

An upgrade replaces the jar and keeps everything else: the configuration and
the data live outside the installation, the database schema is migrated
automatically at startup, and the search index is rebuilt automatically when the
indexed-fields configuration changed. This chapter describes the procedure that
applies to every release, and the steps individual releases add to it.

.. _upgrading-procedure:

General procedure
=================

#. **Read the release notes.** Go through the changelog entries of every version
   between the installed one and the target one (:doc:`../release-notes/changelog`)
   and note the entries marked **config**, **reindex**, **api** and **upgrade**.
   Those are the ones that need something from you; everything else takes effect
   with the new jar.

#. **Back up the database.** PostgreSQL is the only component that needs a
   backup; the search index is derived data and the stored files are not
   changed by an upgrade.

#. **Stop the application.**

#. **Unpack the new bundle** into a new directory, next to the installed one,
   rather than over it. A layout of one directory per release with a ``current``
   link pointing at the one in use makes a rollback a matter of moving the link:

   .. code-block:: text

      /opt/aron/
        releases/aron-2.0.0/      aron.jar + the config/ the bundle ships
        releases/aron-2.0.1/
        current -> releases/aron-2.0.1
        config/application.yml    this deployment's own configuration
        data/                     stored files, tiles, embedded index

   Your own ``application.yml`` stays where it is and is pointed at, or linked
   into, the new release; your data directories are addressed by absolute paths
   so nothing of yours ever lives inside a release directory.

#. **Compare the shipped configuration** with your copies. The bundle's
   ``config/`` is the default configuration of the new version; where you have
   taken a file over and edited it (the display model, the facets, the result
   layout), compare it with the shipped one and carry over what changed. A
   ``diff -r`` between the previous release's ``config/`` and the new one lists
   every difference, record icons included. New keys of ``application.yml``
   are documented in the shipped ``application.yml.template``.

#. **Apply the changes the changelog lists** for the versions you are moving
   across.

#. **Start the application** and watch the log of the first start:

   - Liquibase applies the schema changes of the new version before the
     application accepts requests.
   - ``SearchIndexManager`` compares the indexed-fields configuration with the
     one stored in the index. When they differ — after a **reindex** entry, or
     after you changed ``types.yaml`` — it logs ``Search configuration changed
     … rebuilding the search schema.``, drops the schema and reindexes every
     record from the database. This runs after the application is ready, so
     the portal answers meanwhile, with incomplete search results until
     ``Search index bootstrap completed.`` is logged.
   - A dirty set left behind by an interrupted synchronization is worked off
     (``Search index synchronized: …``).

#. **Verify**: ``/actuator/health`` answers ``UP``, the portal opens, a search
   returns results, a record with digitized material shows its scan. Where the
   deployment discloses the version (``system.expose-version``),
   ``/api/v1/system/info`` names the one now running.

Rolling back
============

Point ``current`` back at the previous release and restart. Two things do not
roll back by themselves:

- **The database schema.** Liquibase migrations are forward-only. A release
  whose changelog carries a schema change is rolled back by restoring the
  backup taken before the upgrade; a release without one runs on the migrated
  schema unchanged.
- **The search index.** The previous version finds an index written by a newer
  configuration and rebuilds it, exactly as the upgrade did. Expect the same
  first-start cost.

Steps of individual releases
============================

A release that needs more than the general procedure has a section here, and
the changelog entry marked **upgrade** links to it. So far no 2.0 release needs
one.

.. _upgrading-from-old-portal:

Coming from the old portal
==========================

The first generation of the portal (now called *G1*) and ARON 2.0 share the
data model, the configuration files and the public URLs, so a deployment moves
over by installing 2.0 as described in :doc:`install` and taking the following
into account. Back up the database before the first start: the schema is
migrated in place.

**URLs.** The application no longer runs under a servlet context path. The old
API stays at ``/api/aron/**`` unchanged, the new one is at ``/api/v1/**``, the
web interface at the root, and the actuator moves from under ``/api/aron`` to
``/actuator/**``. A deployment that served the portal under a subpath sets
``server.servlet.context-path`` or has the reverse proxy send
``X-Forwarded-Prefix``. The proxy must not forward ``/cxf/**``, the internal
SOAP interfaces Transfagent uses; Transfagent addresses the ingest endpoint by
URL, so check its configuration after the move.

**The web interface is served by the jar.** The old portal's separately
deployed frontend and its ``configuration.js`` and ``app/config.json`` have no
successor. What they configured moves into server-side files:

.. list-table::
   :widths: 40 60
   :header-rows: 1

   * - Old portal
     - ARON 2
   * - menu, footer, home page (``configuration.js``, ``homepage.*``)
     - ``pageTemplate.yaml`` — typed ``menu:``, ``footer:`` and ``homepage:``
       sections; see :doc:`configuration`
   * - ``homepage.footerCenter`` / ``footerRight`` (raw HTML)
     - the home page's footer band: prose with named links, no markup
   * - ``useStructuredResults`` + ``app/config.json``
     - ``resultLayout.yaml``; the switch is gone, a record card is shown
       whenever the record carries one
   * - ``showCitationFor`` + ``citation.groovy`` in the working directory
     - ``citation.yaml`` naming the script and the record types
   * - ``treeHorizontalScroll``
     - no successor: the tree pane always scrolls both ways and is resizable

**Search configuration is validated at startup.** A facet condition the portal
cannot read, the facet type ``MULTI_TYPE_REF`` and an ``orderBy`` value other
than ``FREQ`` or ``ASC`` stop the startup with a message naming the file and
the value. The old portal accepted such entries silently and
behaved unpredictably. Start the new version once against a test database and
read the log before the production start. The ``intervals`` key of a facet is
dropped, and ``MULTI_REF_EXT`` facets are served to the old API only.

**Elasticsearch.** The index no longer needs the ICU analysis plugin; built-in
analyzers only. The indexed-fields configuration of 2.0 differs from the old
portal's, so the first start rebuilds the whole index. A deployment may instead
run without Elasticsearch on the embedded engine (``search.engine: lucene``),
at the price of a reduced old-API search; see :doc:`search`.

**Referenced files.** A digital object file the source system referenced by a
local path is served only from the directories listed in
``files.referenced-dirs``. Unset, such files answer 404. The old portal served
any path it was given; list the directories your source systems reference.
