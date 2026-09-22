========
Overview
========

ARON 2 (*Archiv online*, new generation) is a public portal that presents
archival records: fonds, archival descriptions, finding aids, institutions and
the access points that connect them, together with the digitized material
attached to them. It is developed by LightComp and published under the
Apache-2.0 license.

The portal does not create the records it shows. They are delivered by the
separate **Transfagent** application, which reads them from source systems
(Elza, PEvA, …) and pushes them to the portal over an internal SOAP interface.
The portal stores them in PostgreSQL, indexes them for search and serves them
to readers through a web interface and a public REST API.

What a deployment consists of
=============================

- **One executable jar** (``aron.jar``) that contains the backend and the web
  interface. The same jar serves the portal at the root of a URL or under a
  subpath, without being rebuilt.
- **PostgreSQL** as the primary store and the only component that needs a
  backup.
- **A search engine**: Elasticsearch, the production engine, or the embedded
  Lucene engine that needs no external service (see :doc:`../admin/search`).
- **A configuration directory** next to the jar: the main ``application.yml``,
  the display model of the records, the search facets, the page template and
  the branding. The release bundle ships a complete default configuration for a
  Czech archive.

:doc:`../admin/install` describes the installation, :doc:`../admin/upgrading`
the move to a newer version.

Who this guide is for
=====================

This guide is written for the **administrators** who install, configure and
operate a deployment, and for the **integrators** who connect other systems to
it. It describes what the portal does, what it can be configured to do and what
it offers over its interfaces.

It deliberately does **not** contain help for the portal's readers. Which
sections, facets and features a reader sees depends on the deployment's
configuration and on the data its source systems deliver, so no two deployments
look alike. Reader help is therefore written by the institution that runs the
portal; the ``help-url`` setting points the HELP menu item at it (see
:doc:`../admin/configuration`). The same holds for the accessibility statement
a public-sector body publishes about its portal.

**Transfagent has its own documentation**, released with that application.
This guide covers the portal's side of the interface only: the SOAP endpoints
it exposes, what an import does inside the portal and how published data is
withdrawn (see :doc:`../admin/operations`).

How this documentation is versioned
===================================

Each version line of the portal (2.0, 2.1, …) has its own edition of this
guide, published at ``https://docs.lightcomp.cz/aron/<line>/`` — the 2.0 line
at https://docs.lightcomp.cz/aron/2.0/, the development state at
https://docs.lightcomp.cz/aron/main/. The edition is shown in the page title.
Within a line, the *Release notes* chapter lists the changes of every build and
marks those that need an administrator's attention. Before an upgrade, read the changelog entries of
every version between the installed one and the target one.
