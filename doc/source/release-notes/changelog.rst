=========
Changelog
=========

The changes of every released build of the 2.0 line, newest first. Each entry
is one change an administrator or an integrator can notice; the categories and
the markers (**config**, **reindex**, **api**, **upgrade**) are explained in
:doc:`release-notes`. The version in a heading is the bundle a deployment
installs, the date is its release date.

Unreleased
----------

- New: orders of archival records, as in the old portal - "Order" on a
  record's page, a basket in the header and an order form per archive. Off
  unless ``webResources.order`` names an ``order.yaml`` - the shipped
  ``application.yml.template`` does, an existing ``application.yml`` has to
  be given the key; the order itself is taken by the deployment's order
  service, which the reverse proxy must publish at ``/api/orders``
  (:ref:`orders`). **config** **api**
- New: the pager of the search results can skip ten pages back and forward,
  as in the old portal.
- New: the pager is repeated below the search results, as in the old portal;
  using it there returns to the top of the list.
- New: a "Clear filters" button in the search sidebar removes every active
  filter at once, as the old portal's did.
- New: the first release of the second generation of the portal.
  :doc:`whatsnew-2.0` describes what it brings compared to the old portal,
  :ref:`upgrading-from-old-portal` what a deployment of the old portal has to
  change.
