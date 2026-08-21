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

A deployment consists of one executable jar (``aron.jar``) containing the
backend **and** the web UI, plus an external configuration directory:

.. code-block:: text

   /opt/aron/
     aron.jar
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

   java -jar aron.jar

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
     - **Internal** SOAP interfaces used by Transfagent: ``/cxf/ft`` for data
       ingest (file transfer) and ``/cxf/management`` for withdrawing data it
       has published. Never expose them publicly; they can also be moved to a
       separate port, see below.

Reverse proxy and subpath deployment
====================================

The same jar works at the URL root and under any subpath **without
rebuilding**:

- Standalone under a subpath: set ``server.servlet.context-path`` (for
  example ``/aron``) in ``application.yml``.
- Behind a reverse proxy: forward the prefix in the ``X-Forwarded-Prefix``
  header; the application folds it into the effective context path.

Separate port for the internal interfaces
=========================================

By default the internal SOAP interfaces share the main server port and are kept
private by the proxy rules below. A deployment can instead give them their own
listener::

   soap:
     port: 8081
     address: 10.0.0.5   # optional: bind to the internal interface only

The split is then strict: ``/cxf/**`` is served **only** on ``soap.port``, and
that port serves **only** ``/cxf/**`` — no portal, no REST API, no actuator. With
``soap.address`` the interfaces are not even reachable from the public network,
so a mistake in the proxy configuration cannot expose them. Unset ``soap.port``
keeps the current behaviour, which is also what development uses.

The endpoints keep their paths — ``http://host:8081/cxf/ft`` and
``http://host:8081/cxf/management``. The one exception is a standalone subpath
deployment (``server.servlet.context-path``): the paths carry that prefix on
every port, exactly as they do today on the main one
(``http://host:8081/aron/cxf/ft``).

Proxy rules for a public deployment:

- **Block** ``/cxf/**`` — these are service-to-service interfaces for
  Transfagent (ingest and data withdrawal), not part of the public API. With
  ``soap.port`` configured the proxy has nothing to block: the paths do not
  exist on the public port.
- Decide explicitly whether ``/actuator/**`` is reachable from the outside;
  in most deployments it should be restricted to the monitoring network.
