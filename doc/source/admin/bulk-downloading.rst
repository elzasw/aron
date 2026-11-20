=========================
Limiting bulk downloading
=========================

Digital objects are large, and nothing in a public portal stops one client
from fetching whole volumes of them. Such a download can fill the line or
occupy the server until ordinary readers cannot view the scans — whether it is
one researcher's script or a crawler collecting images.

The portal applies **no limits of its own**: it answers every request it
receives, for everyone, and it has no reader accounts. Protection is therefore
a matter of the deployment — of what stands in front of the portal and of
where the files are hosted. This chapter lists the possibilities, what each of
them needs and what it cannot do.

What the portal serves
======================

Two kinds of requests carry digital objects. Telling them apart is the basis of
every measure below:

.. list-table::
   :widths: 12 44 44
   :header-rows: 1

   * - Kind
     - Paths
     - Character
   * - Tiles
     - ``/api/v1/daofile/{id}/tiles/**``; for the old user interface
       ``/api/aron/tile/{id}/**``
     - The viewer's deep-zoom tiles: many small requests in bursts — a page
       turn, a zoom — with pauses between them. A reader notices every delay.
   * - Files
     - ``/api/v1/daofile/{id}``; for the old user interface
       ``/api/aron/file/{id}`` and ``/api/aron/referencedfiles/{id}``
     - Whole files: the download a reader asks for, but also thumbnails and
       the images of pages that have no tiles. The large ones are long
       transfers, and they are what a bulk download consists of.

Worth knowing when choosing a measure:

- Every one of these requests is answered by the application itself.
- The responses of ``/api/v1`` are public and cacheable for seven days, carry
  ``ETag`` and ``Last-Modified``, and a file can be fetched in parts
  (``Range``) — an interrupted download resumes instead of starting again.
- A file the source system delivered as a **URL reference** is not served by
  the portal at all: the portal hands the URL to the browser and the transfer
  runs between the reader and that server (see ``files.referenced-dirs`` in
  :doc:`configuration`).
- A full image can also be assembled from its tiles. Restricting the file
  download alone therefore moves a determined downloader to the tiles, where
  the same image costs the server hundreds of requests instead of one. Limit
  both kinds.
- The portal has no reader accounts, so it offers neither quotas per user nor
  content reserved for signed-in readers, and it does not package a whole
  archival unit for download.

Finding out what is overloaded
==============================

The right measure depends on what gives way first. The access log of the
reverse proxy tells: which paths are requested, from how many addresses, and
how many bytes they carry.

.. list-table::
   :widths: 40 60
   :header-rows: 1

   * - What is overloaded
     - What helps
   * - The line (bytes)
     - Slowing down and capping the large transfers, or hosting the files
       elsewhere.
   * - The server's workers (many long transfers at once)
     - Capping the transfers a client may run at once; a proxy that takes the
       response over from the application.
   * - A separate image server that renders tiles on request
     - Caching its tiles.

The log also shows whether the load comes from one address or from many. Limits
per address answer the first case; the second needs a check that the visitor is
a browser at all.

The measures at a glance
========================

.. list-table::
   :widths: 22 30 30 18
   :header-rows: 1

   * - Measure
     - What it gives
     - What it cannot do
     - Where it is set up
   * - Published rules of use
     - Considerate users and scripts slow down; the institution has a rule to
       point at.
     - Relies on goodwill.
     - A page of the institution, linked from the footer
       (``footer.links``, :doc:`configuration`)
   * - Limits at the reverse proxy
     - One client cannot take the line or the workers; large transfers get an
       upper bound.
     - Limits count per address: shared addresses need exceptions, and a
       crawler spread over many addresses stays under them.
     - Reverse proxy
   * - A check that the visitor is human
     - Scripts and crawlers that are not browsers are stopped.
     - A person in a browser passes and can then download as before. Programs
       using the public API are stopped too.
     - Reverse proxy, or a service in front
   * - Files hosted outside the portal
     - Their transfers leave the portal's server and line.
     - Works only for objects delivered as URL references; that host needs
       limits of its own.
     - Publishing side (source system, Transfagent)
   * - A protection service in front of the portal
     - Stronger recognition of automated clients than a simple check.
     - All traffic passes through a third party, on that party's terms.
     - DNS and the service's own settings

No single measure is enough; `Combining the measures`_ says which to start
with.

Limits at the reverse proxy
===========================

The proxy is the one place that sees every request before the application
does, so it is where limits belong. Four of them work together:

- **Request rate per client**, with a generous burst. A reader produces bursts
  with pauses, a script a steady stream; a limit with a large burst and a low
  sustained rate lets the first through and holds the second back.
- **Concurrent transfers per client** on the file paths.
- **Speed of a transfer** after its first megabytes, so thumbnails and page
  images pass at full speed and only large files are slowed down.
- **Concurrent transfers of all clients together** on the file paths. With the
  speed limit it bounds what large transfers can take from the line, however
  many addresses they come from.

A refused request should say so: status ``429 Too Many Requests`` makes a
script back off, where a stalled connection only makes it retry.

Example for nginx
-----------------

The values are placeholders. Derive yours from the access log, and after the
change watch the log for refused requests of ordinary readers.

.. code-block:: nginx

   # http context
   limit_req_zone  $binary_remote_addr zone=aron_tiles:20m  rate=20r/s;
   limit_req_zone  $binary_remote_addr zone=aron_files:20m  rate=2r/s;
   limit_conn_zone $binary_remote_addr zone=aron_client:20m;
   limit_conn_zone $server_name        zone=aron_all:1m;
   limit_req_status  429;
   limit_conn_status 429;

   server {
       # tiles: bursts pass, a steady stream is held back
       location ~ ^/api/(v1/daofile/[^/]+/tiles|aron/tile)/ {
           limit_req zone=aron_tiles burst=400 nodelay;
           proxy_pass http://aron;      # as in the portal's main location
       }

       # files: small ones pass at once, large ones are slowed down
       # and only a few run at a time
       location ~ ^/api/(v1/daofile|aron/file|aron/referencedfiles)/[^/]+$ {
           limit_req  zone=aron_files burst=100 delay=40;
           limit_conn aron_client 6;
           limit_conn aron_all 60;
           limit_rate_after 5m;
           limit_rate 1m;
           proxy_pass http://aron;
       }
   }

What to keep in mind:

- ``limit_rate`` applies to each transfer, so a client's share is the speed
  multiplied by its concurrent transfers — here 6 MB/s — and the whole class is
  bounded by the cap for all clients, here 60 MB/s.
- With HTTP/2 nginx counts every concurrent request as a connection.
  Thumbnails therefore count towards ``limit_conn`` while they run, and when
  the cap for all clients is reached they are refused as well. Keep that cap
  well above the usual number of simultaneous short requests.
- ``delay=40`` lets the first forty requests above the rate through at once —
  a row of thumbnails — and delays only the following ones.
- nginx reads a response from the application at full speed and paces the
  client itself, into a temporary file when the response is large
  (``proxy_max_temp_file_size``, 1 GB by default). A slow or slowed-down
  client then does not hold a worker of the application. Leave
  ``proxy_buffering`` on for these paths and give the temporary files room.
- A request whose zone key is empty is not counted. That is how known
  networks — a reading room — are exempted: map their addresses to an empty
  key.
- Under a subpath the patterns start with that prefix
  (``^/aron/api/…``).
- If another proxy or a protection service stands in front, restore the
  visitor's address first (``ngx_http_realip_module``). Otherwise every
  reader shares one limit.

Apache httpd
------------

Apache has no limits per client of its own. ``mod_ratelimit`` slows down each
response (``rate-limit`` in KiB/s, ``rate-initial-burst`` for the part sent at
full speed), but it does not add up what one client transfers. The third-party
``mod_qos`` supplies the rest:

.. list-table::
   :widths: 40 60
   :header-rows: 1

   * - Directive
     - Limit
   * - ``QS_SrvMaxConnPerIP``
     - Connections from one address.
   * - ``QS_LocRequestLimitMatch``
     - Concurrent requests for a URL pattern.
   * - ``QS_LocRequestPerSecLimitMatch``
     - Requests per second for a URL pattern.
   * - ``QS_LocKBytesPerSecLimitMatch``
     - Download bandwidth for a URL pattern.
   * - ``QS_ErrorResponseCode``
     - Status of a refused request.

A proxied transfer occupies one of Apache's workers while it runs, so cap the
concurrent transfers before anything else.

A check that the visitor is human
=================================

A gate in front of the portal asks the browser to prove that it is a browser —
by a short computation or by a third party's check — before the first request
passes, and remembers the result in a cookie. Such gates exist as reverse
proxies to host yourself (Anubis is one) and as services.

It stops what limits per address do not: scripts and crawlers coming from many
addresses. It does not stop a person, who passes it and can then download in
bulk from that browser, and it caps nothing. Before turning one on:

- **Gate the pages and the digital-object paths, not the whole API.** The
  rest of ``/api/v1`` is a public interface other programs may use, and a
  program cannot pass a browser check.
- **Never gate the internal interfaces.** ``/cxf/**`` is called by Transfagent
  and should not be reachable from the public network at all (see
  :doc:`install`).
- **The check is part of the portal for its readers**, so the institution's
  accessibility obligations cover it. Prefer one that needs no interaction.
- **A third party's check sends every reader's address to that party.** That
  belongs in the privacy notice.

Files hosted outside the portal
===============================

A digital object delivered as a URL reference is served by whatever stands
behind that URL — an image server, an object storage, a content delivery
network. The portal passes the URL on and carries none of the bytes, so the
transfers of such objects cannot overload it.

Which objects are delivered this way is decided where the data is published,
not in the portal. Three things to weigh:

- The other host is now the one a bulk download hits. It needs limits of its
  own, and where delivery is billed by volume a downloader runs up the bill.
- Move whole collections, not single popular files: a bulk download walks
  through entire volumes.
- A delivery network used only as a **cache** in front of the portal helps
  little here. Archival scans are requested rarely each, and a bulk download
  asks exactly for those no cache holds.

A protection service in front of the portal
===========================================

Services such as Cloudflare take over the portal's public address, inspect the
traffic and challenge what they consider automated. Their recognition is
stronger than a simple check. Before relying on one:

- **Read the terms for your kind of content.** Some providers restrict sites
  that serve mostly images or large files on their lower plans, and an
  archival portal is almost nothing else.
- **All reader traffic passes through the provider**, which needs a contract
  and the institution's decision.
- **The address must be handed over** — on the lower plans usually the whole
  DNS zone, which a shared domain may not allow.
- **Programs using the public API are challenged too**, unless the plan allows
  exceptions.
- **The portal's own proxy must still see the visitor** (see the last point of
  the nginx notes) and should refuse connections that bypass the service.

Combining the measures
======================

1. **Always:** limits at the reverse proxy, and the rules of use published
   where readers find them. This protects ordinary readers from a single
   heavy client, costs nothing but configuration, and everything else builds
   on it.
2. **When the log shows crawlers from many addresses:** add a check that the
   visitor is human.
3. **When the line itself is the limit:** host the large collections outside
   the portal.

Each measure alone leaves a gap. Limits only slow a downloader down; an outside
host protects only what was moved to it; a human check stops no human.
