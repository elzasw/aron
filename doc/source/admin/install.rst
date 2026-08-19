============
Installation
============

Prerequisites
=============

.. list-table::
   :widths: 25 75
   :header-rows: 1

   * - Component
     - Notes
   * - Java 21 (JRE or JDK)
     - The application is a single executable jar.
   * - PostgreSQL
     - Primary data store (APUs, imported packages, import journal).
   * - Elasticsearch (optional)
     - Default search engine. Deployments may instead run the **embedded
       Lucene** engine (``search.engine: lucene``), which needs no external
       search server; Elasticsearch remains the recommended production
       engine. No Elasticsearch analysis plugins are required — the index
       uses built-in analyzers only.

Artifact and layout
===================

A deployment consists of one executable jar (``aron2.jar``) containing the
backend **and** the web UI, plus an external configuration directory:

.. code-block:: text

   /opt/aron2/
     aron2.jar
     config/
       application.yml        # main configuration (template ships with the sources)
       types.yaml             # display / indexing model of description items
       searchConfig.yaml      # facet (and search relevance) configuration
       pageTemplate.yaml      # portal name, localization, menu
       news.yaml
       favoriteQueries.yaml
       images/                # logo, top image

``config/application.yml`` is resolved relative to the working directory; a
template is provided at ``distribution/config/application.yml.template`` in the
source tree.

Running
=======

.. code-block:: shell

   java -jar aron2.jar

The application creates and migrates its database schema automatically
(Liquibase) and bootstraps the search index at startup (see
:doc:`operations`).

URL layout
==========

.. list-table::
   :widths: 30 70
   :header-rows: 1

   * - Path
     - Purpose
   * - ``/``
     - Public portal (single-page application).
   * - ``/api/v1/**``
     - Current REST API.
   * - ``/api/aron/**``
     - Legacy REST API (kept for compatibility, frozen).
   * - ``/actuator/**``
     - Spring Boot Actuator (health, metrics). Intentionally outside the
       ``/api`` namespace.
   * - ``/cxf/**``
     - **Internal** SOAP file-transfer endpoint used by Transfagent for data
       ingest. Never expose it publicly.

Reverse proxy and subpath deployment
====================================

The same jar works at the URL root and under any subpath **without
rebuilding**:

- Standalone under a subpath: set ``server.servlet.context-path`` (for
  example ``/aron``) in ``application.yml``.
- Behind a reverse proxy: forward the prefix in the ``X-Forwarded-Prefix``
  header; the application folds it into the effective context path.

Proxy rules for a public deployment:

- **Block** ``/cxf/**`` — it is a service-to-service interface for the
  Transfagent ingest, not part of the public API.
- Decide explicitly whether ``/actuator/**`` is reachable from the outside;
  in most deployments it should be restricted to the monitoring network.
