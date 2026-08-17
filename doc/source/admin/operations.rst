==========
Operations
==========

Search index lifecycle
======================

The search index is **derived data** — it can always be rebuilt from the
database, and it needs no backup. At every startup the application compares
the current indexing configuration (the indexed fields of ``types.yaml`` and
the internal document layout version) with the values stored in the index's
own metadata:

- unchanged → the index is reused as-is;
- changed → the schema is dropped, recreated and **all APUs are reindexed
  automatically**.

No manual reindex procedure exists or is needed; to force a rebuild of a
persisted embedded-Lucene index, delete its directory
(``search.lucene.path``) and restart.

Data ingest
===========

Transfagent (SOAP file transfer)
--------------------------------

The regular ingest path: the separate **Transfagent** application collects
data from source systems (Elza, PEvA, …) and pushes APUX packages and files
over the SOAP file-transfer interface at ``/cxf/ft``. This endpoint is
internal — see the proxy rules in :doc:`install`.

Input-directory import
----------------------

A file-based alternative for supported data inputs, enabled by
``import.input-dir``. Each subdirectory of the input directory is one
transfer (``apusrc-*.xml`` or ``dao-*.xml``, optionally a ``files/``
directory). The directory is scanned at startup; transfers whose content has
not changed are skipped using a content-hash journal stored in the database
(``import_journal`` table), so leaving processed transfers in place is
harmless.

Database
========

PostgreSQL is the primary store and the only component that requires backup.
Schema changes are applied automatically at startup via Liquibase — no manual
migration steps between releases.

Monitoring
==========

Spring Boot Actuator is available under ``/actuator`` (outside the ``/api``
namespace):

- ``/actuator/health`` — liveness/readiness, suitable for probes;
- further endpoints (metrics, info) according to the actuator configuration
  in ``application.yml``.

.. todo::

   Describe the recommended logging configuration and log locations once the
   deployment packaging (service unit, log rotation) is settled.
