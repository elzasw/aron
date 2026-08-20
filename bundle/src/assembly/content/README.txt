ARON 2 - Archiv online
======================

Binary distribution of the ARON 2 public portal for a standalone installation.
The portal presents archival records; data is delivered into it by the separate
Transfagent application.


Contents of this bundle
-----------------------

  aron2.jar                      the whole application - backend and web UI in
                                 one executable jar with an embedded web server
  config/application.yml.template annotated configuration template
  README.txt                     this file
  LICENSE                        license terms

The administrator documentation is not part of this bundle; it is published
separately alongside the release.


Requirements
------------

  * JDK 21 or newer
  * PostgreSQL
  * Elasticsearch - optional. The portal ships two interchangeable search
    engines behind one interface (search.engine). "elasticsearch" is the
    production engine; "lucene" is embedded and needs no external service, at
    the price of a reduced legacy search API. Switching is a configuration
    change, not a different build.

The index needs no Elasticsearch plugins - it uses built-in analyzers only.


Installation
------------

1) Create a database and a database user, and make that user its owner:

     create user aron with password '...';
     create database aron owner = aron;

   The schema itself is created and upgraded by the application on startup.

2) Unpack this bundle into an installation directory, for example /opt/aron2.
   The result is aron2.jar with a config/ directory next to it.

3) Copy the template and edit your copy:

     cp config/application.yml.template config/application.yml

   At a minimum set the database connection (spring.datasource), the data
   directories (files.storage, files.transfer.path, tile.folder) and the search
   engine (search.engine). Every key is described in the template.

   Paths in the template are relative to the working directory the application
   is started from. Keep data directories OUTSIDE the installation directory so
   an upgrade cannot overwrite them.

4) Add the deployment's own presentation configuration into config/ and point
   the keys types-config and webResources.* at it: the display model
   (types.yaml), the portal page template, the search facets, the logo and the
   remaining branding files. These files describe the archive being published,
   so they are specific to each deployment and are not part of this bundle.

5) Start the application from the installation directory:

     java -jar aron2.jar

   It reads config/application.yml relative to the working directory, so start
   it from the installation directory - or pass the location explicitly with
   --spring.config.location.

6) Check that it is running:

     http://localhost:8080/actuator/health

   Then configure automatic startup as a service (systemd on Linux, a Windows
   service). The jar is an ordinary Spring Boot application; the upstream
   documentation on installing it as a service applies unchanged.


Deployment behind a reverse proxy
---------------------------------

The same jar serves the portal at the root of a URL and under a subpath
without being rebuilt. For a subpath, either set server.servlet.context-path
(standalone) or send X-Forwarded-Prefix from the proxy.

IMPORTANT - the proxy must NOT forward /cxf/**. Those are the internal
service-to-service SOAP interfaces used by Transfagent for data ingest and for
withdrawing published data; they are deliberately outside the public /api
namespace and must not be reachable from the internet. Optionally set soap.port
to move them to their own listener, and soap.address to bind that listener to
an internal network interface.


Upgrading
---------

Replace aron2.jar and restart. Configuration and data are kept outside the jar,
the database schema is upgraded on startup, and the search index is rebuilt
automatically when the indexed-field configuration changed.
