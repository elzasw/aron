=============================
ARON 2 — Administration Guide
=============================

ARON 2 (Archiv online, new generation) is a public portal for presenting
archival records. The backend indexes archival presentation units (APU) and
exposes a REST API consumed by a React single-page application; data is
ingested from the separate **Transfagent** application over a SOAP
file-transfer interface.

This guide is intended for administrators who install, configure and operate
an ARON 2 deployment, and for integrators who connect other systems to it. It
contains no help for the portal's readers; :doc:`intro/overview` explains why.
The *Release notes* list the changes of every build and mark those that need an
administrator's attention.

.. toctree::
   :maxdepth: 2
   :numbered: 2
   :caption: Administration guide

   intro/overview
   admin/install
   admin/upgrading
   admin/configuration
   admin/search
   admin/operations

.. toctree::
   :maxdepth: 2
   :caption: Release notes

   release-notes/release-notes

.. todolist::
