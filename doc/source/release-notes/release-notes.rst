=============
Release notes
=============

The release notes have two levels. **What's new** describes a version line as
a whole: what it brings compared to the previous one and where in this guide
the new features are configured. The **changelog** lists every released build
of the line, newest first, with one entry per change an administrator or an
integrator can notice.

How to read a changelog entry
=============================

Every entry starts with a category:

- **New** — a feature or a setting that did not exist before.
- **Changed** — existing behavior that now works differently.
- **Fixed** — a defect and what now works.
- **Removed** — a feature or a setting that is gone, with its successor when
  there is one.

An entry that needs the administrator's attention carries a marker after the
category. The markers are what to scan a changelog for before an upgrade:

- **config** — a configuration key was added, changed its default or its
  meaning, or was dropped. The entry names the key.
- **reindex** — the search index rebuilds itself at the first start of the new
  version. The portal is up meanwhile, but search results are incomplete until
  the rebuild has finished, and the first start takes longer.
- **api** — a change of the REST or SOAP contracts that a client may notice.
- **upgrade** — a step the administrator has to take. The entry links to the
  section of :doc:`../admin/upgrading` that describes it.

An entry that carries no marker needs nothing from the administrator: the
change takes effect with the new jar.

Issue numbers in an entry refer to the project's issue tracker. Pre-release
builds (BETA, RC) get no heading of their own; their changes appear under the
final version they lead to, because the heading names what a deployment
installs.

.. toctree::
   :maxdepth: 1

   whatsnew-2.0
   changelog
