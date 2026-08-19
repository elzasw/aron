==========
Operations
==========

Search index lifecycle
======================

The search index is **derived data** — it can always be rebuilt from the
database, and it needs no backup. At every startup the application compares
the current indexing configuration (the indexed fields of ``types.yaml``, the
content locale ``search.content-locale`` and the internal document layout
version) with the values stored in the index's own metadata:

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

Withdrawing published data
--------------------------

Data is deleted the same way it arrives — on request of the publishing system.
Transfagent calls the internal management interface at ``/cxf/management``
(SOAP, contract ``wsdl/aron_core.wsdl``, operation ``DeleteApuSources``) with
the uuids of the APUX packages to withdraw. For each package the portal deletes
its APUs, their attachments and relations, and removes them from the search
index; digital objects are only detached, because they are transferred
separately and are reattached if the package is imported again. Uuids that are
not present are ignored, so a repeated request is harmless.

Two consequences worth knowing:

- Binary files of deleted attachments stay in the file storage
  (``files.storage``) — as they do after a reimport.
- Withdrawal does not touch the input-directory journal. A package deleted
  this way and still present as a transfer folder is **not** reimported at the
  next startup unless its content changes; delete the folder, or its
  ``import_journal`` row, if the data should come back.

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
