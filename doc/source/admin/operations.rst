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

Why a request failed
====================

When a request fails, the portal answers with a status and, in the log, the
reason: which record was not found, which filter the section does not have,
which piece of a description a citation could not be built from. The reason is
**not** in the response body by default: Spring's
``server.error.include-message`` is ``never``, so a client sees only

.. code-block:: json

   {"timestamp":"…","status":422,"error":"Unprocessable Entity","path":"/api/v1/apu/…/citations"}

That is deliberate. Setting ``server.error.include-message: always`` adds the
reason to **every** error response. For the errors the portal reports on
purpose that is exactly what someone debugging needs ("No such APU.", the
missing part of a citation). For an unexpected error — status 500 — the same
field carries the exception's own message, which in this stack can name a
database column, a file path on the server or an internal component. Stack
traces are a separate setting and stay off either way.

Hence the recommendation:

- **public portal** — leave it off and read the reason in the log. The reader
  is shown the portal's own message anyway; the portal's reasons are
  English diagnostics written for whoever debugs, not text a reader could act
  on.
- **test or evaluation server** — turn it on. It is the same reasoning as
  ``system.expose-version``: such a server exists to be inspected, and having
  the reason in the response saves a trip to the log. The commented block is
  in ``application.yml.template``.

Failed requests are never silent in the UI either: they appear in a visible
error bar, with the server's message when the deployment sends one.

.. todo::

   Describe the recommended logging configuration and log locations once the
   deployment packaging (service unit, log rotation) is settled.
