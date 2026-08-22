=============
Configuration
=============

All configuration is supplied at runtime — nothing deployment-specific is
packaged into the jar. The main file is ``config/application.yml``; it points
to the other configuration files.

application.yml
===============

.. list-table::
   :widths: 32 68
   :header-rows: 1

   * - Key
     - Meaning
   * - ``spring.datasource.*``
     - PostgreSQL connection (URL, username, password).
   * - ``spring.elasticsearch.*``
     - Elasticsearch connection (default ``localhost:9200``); relevant only
       with the Elasticsearch engine.
   * - ``search.engine``
     - Search engine: ``elasticsearch`` (default) or ``lucene`` (embedded,
       no external server).
   * - ``search.lucene.path``
     - Directory of the persisted embedded index; unset = in-memory
       (rebuilt on every start).
   * - ``search.content-locale``
     - Language of the described material, as an IETF language tag (default
       ``cs-CZ``); its alphabet orders name-sorted results for every visitor
       and its stop words are the ones searching ignores (see :doc:`search`)
       — this is not the visitor's own locale. Changing it reindexes at the
       next startup (see :doc:`operations`); an invalid tag stops the
       startup.
   * - ``types-config``
     - Path to ``types.yaml`` (see below).
   * - ``webResources.pageTemplate``
     - Portal name, localizations and optional menu definition
       (``pageTemplate.yaml``).
   * - ``webResources.news``
     - News shown on the portal home page (``news.yaml``).
   * - ``webResources.favoriteQueries``
     - Preconfigured favourite queries (``favoriteQueries.yaml``).
   * - ``webResources.facets``
     - Facet — and search relevance — configuration
       (``searchConfig.yaml``, see :doc:`search`).
   * - ``webResources.logo`` / ``webResources.topImage``
     - Branding images served to the UI.
   * - ``webResources.resultLayout``
     - Optional layout of the structured search results
       (``resultLayout.yaml``, see below). Unset = the built-in defaults.
   * - ``webResources.images``
     - Optional directory of the images the portal serves by name: the record
       and field icons of the structured results, the pictures of the home
       page's tiles, and thumbnails delivered by name. Several directories may
       be listed, separated by commas — a name is looked up in the order given,
       the first hit wins. Only plain file names are served, never paths.
   * - ``search.structured-results``
     - ``AUTO`` (default) attaches the structured presentation to every record
       that has one; ``OFF`` disables the feature for the whole deployment.
   * - ``system.expose-version``
     - ``false`` (default) keeps the running version out of the footer and out
       of ``/api/v1/system/info`` — a reader can do nothing with it. ``true``
       publishes it, which is what a test server or a support case wants.
   * - ``help-url``
     - External help link shown in the default menu.
   * - ``import.input-dir``
     - Optional input-directory import — a file-based alternative to the
       Transfagent upload (see :doc:`operations`). Unset = off.
   * - ``files.storage``, ``files.transfer.path``
     - File storage of attachments and the working directory of the SOAP
       file transfer.
   * - ``tile.folder``, ``tile.format``
     - Deep-zoom tile cache for large images.
   * - ``transformation-agent-url``
     - SOAP endpoint of the external Transform agent (file/format
       transformations).
   * - ``server.servlet.context-path``
     - Optional subpath for standalone subpath deployments (see
       :doc:`install`).
   * - ``soap.port``
     - Optional separate port for the internal SOAP interfaces
       (``/cxf/**``). Unset = they share the main server port. When set, the
       split is strict: ``/cxf/**`` is served only on this port and this port
       serves nothing else (see :doc:`install`).
   * - ``soap.address``
     - Optional network interface the ``soap.port`` listener binds to (for
       example an internal address). Unset = all interfaces. An unusable
       address stops the startup.

types.yaml
==========

The display and indexing model of description items ("item types"). For each
item type it defines the data type (``STRING``, ``ENUM``, ``APU_REF``,
``INTEGER``, ``UNITDATE``, ``LINK``, …), the Czech display label and the
indexing flags:

- ``indexed`` — whether the item is written to the search index at all;
- ``fulltext`` — whether the item's value participates in the general
  fulltext search (default: yes; see :doc:`search`);
- ``nameVariant`` — marks the item as a variant name form of the record
  (the other names of an access point); its values rank just below the
  primary name (default: no; see :doc:`search`).

.. note::

   Values of ``ENUM`` items are **not** configured here and are not translated
   by the portal: Transfagent delivers them as the text to display. Like record
   names and descriptions they are content, so their language is the language
   the source system recorded them in.

.. important::

   ``types.yaml`` describes **what the search index contains**. Any change to
   its indexed fields is detected at startup and triggers an automatic
   rebuild and reindex (see :doc:`operations`). Weight tuning does *not*
   belong here — see the ``relevance`` section of ``searchConfig.yaml``.

Translating configured text
===========================

Text a deployment writes into its configuration — item labels, the portal name,
facet titles — is translated in a **sibling file**: next to ``<config>.yaml``
put ``<config>_localization.yaml``. The configured file keeps one language, the
source language, which stays the fallback; further languages are added without
touching it. Every such file is optional, so a deployment that adds none behaves
exactly as before.

.. list-table::
   :widths: 40 60
   :header-rows: 1

   * - Sibling file
     - Translates
   * - ``types_localization.yaml``
     - Labels of part types and item types (``types.yaml``).
   * - ``searchConfig_localization.yaml``
     - Facet ``title``, ``tooltip`` and ``description``, and the per-option
       explanations of ``tooltips`` (``searchConfig.yaml``, see
       :doc:`search`).
   * - ``pageTemplate_localization.yaml``
     - Portal name (``pageTemplate.yaml``).

The one exception is **footer links**, whose labels are written inline in
``pageTemplate.yaml`` (``label: {cs: …, en: …}``): a link is identified by its
URL rather than by a code, so there is no stable key a sibling file could use.

types_localization.yaml
-----------------------

Translations of the display labels in ``types.yaml``.

.. code-block:: yaml

   partTypes:
     en:
       PT_TITLE: Title
   itemTypes:
     en:
       TITLE_MAIN: Main title

Codes are written in the underscore form used in ``types.yaml``. The language
keys should match the ``localizations`` declared in ``pageTemplate.yaml`` —
those are the languages clients may ask for.

searchConfig_localization.yaml
------------------------------

Display texts of the facets, keyed by the facet's ``source`` code exactly as
written in ``searchConfig.yaml``. Any of the three texts may be omitted; an
omitted one falls back to ``searchConfig.yaml``.

.. code-block:: yaml

   facets:
     en:
       LANG_CODE:
         title: Language
         tooltip: Language of the described material

A facet without an explicit ``title`` is labelled by its item type instead, so
translating it belongs in ``types_localization.yaml``.

pageTemplate_localization.yaml
------------------------------

The portal name; a single field, so no codes are involved.

.. code-block:: yaml

   name:
     en: Archives online

.. note::

   Which language a response is rendered in comes from the request's ``lang``
   parameter (an IETF tag such as ``en`` or ``cs-CZ``), matched against the
   configured ``localizations``; absent or unmatched, the first configured
   localization applies. This is the **reader's** language and affects display
   text only — never which records match, nor their order, which follows
   ``search.content-locale``.

Dating vocabulary
=================

Datings are rendered by the server, because presenting one means applying the
archival grammar — granularity (century, year, month, date), estimated bounds
in brackets, intervals whose sides collapse when equal — and not merely
formatting a date. The dates themselves come from the JDK's CLDR data, so
month names and date order are correct in any language without configuration.

The words around them — how a century reads, the interval separator — are not
in CLDR and ship as resource bundles in the application
(``cz/aron/web/v1/unitdate*.properties``): Czech as the base, plus one file per
further language. Supporting a new language therefore means adding one
properties file, not writing date patterns.

Alongside the rendered string, the API also carries the dating in machine form
(``dating``: bounds, granularity, estimate flags) — for semantic markup and for
consumers that process datings rather than display them.

searchConfig.yaml
=================

Two concerns, both deployment-specific:

- ``facets:`` — which search filters each portal section offers, their type
  (``FULLTEXT``, ``ENUM``, ``MULTI_REF``, ``UNITDATE``, …), labels, ordering
  and display rules;
- ``relevance:`` — the fulltext ranking weights (see :doc:`search`).

Changes take effect after a restart; **no reindex is required**.

pageTemplate.yaml
=================

Portal name, available localizations and optionally the main menu (section
codes, colors, external URLs). Without a ``menu:`` block the default menu is
used (funds, archival description, entities + help from ``help-url``).
Clients never receive raw server configuration files — the UI reads a typed
``/api/v1/ui/config`` endpoint.

.. code-block:: yaml

   name: Archiv online
   localizations:
     - cs_CZ
     - en

``localizations`` declares which languages the portal offers; the first is the
default. The portal ships UI strings for **Czech and English**, so those are
the languages a deployment can list today — the language switcher shows only
the ones declared here *and* present in the build, and a single declared
language shows no switcher at all.

Declaring a language covers the interface strings and the datings. The labels
of the description items follow only if ``types_localization.yaml`` translates
them; untranslated ones fall back to the ``types.yaml`` names.

Home page
=========

The optional ``homepage:`` block of ``pageTemplate.yaml`` fills the portal's
home page below the search box: groups of prepared entry points, and the
deployment's own footer band. Without the block the home page is the search box
alone, which is a legitimate configuration.

Every value a reader sees can be written either as one string (the source
language) or as a mapping of language to text.

.. code-block:: yaml

   homepage:
     groups:
       - label: { cs: Mohlo by vás zajímat, en: You might be interested in }
         style: GRID            # GRID = picture tiles, LIST = rows of links (default)
         tiles:
           - label: { cs: Matriky, en: Parish registers }
             note: { cs: církevní i civilní, en: church and civil }
             image: { name: matriky.jpg, positionY: top }
             columnSpan: 2      # 1 or 2 grid cells; GRID only
             rowSpan: 2
             apuType: ARCH_DESC
             filters:
               - facet: UNIT_TYPE
                 values: [matrika]
           - label: Porta fontium
             url: https://www.portafontium.eu
     footer:
       columns:
         - heading: { cs: Základní informace, en: About }
           paragraphs:
             - text:
                 cs: "Portál je aplikace {archiv} a zpřístupňuje popis archiválií."
                 en: "The portal is an application of the {archiv}."
               links:
                 archiv:
                   label: { cs: Státního oblastního archivu, en: State Regional Archives }
                   url: https://archiv.example
         - heading: { cs: Kontakt, en: Contact }
           links:
             - label: badatelna@archiv.example
               url: "mailto:badatelna@archiv.example"

Tiles
-----

A tile leads **either** out of the portal or into one section's search, never
both and never neither:

- ``url`` — an absolute ``http``/``https``/``mailto`` address. In-app
  destinations are the other kind; a bare path is refused.
- ``apuType`` plus optional ``query`` and ``filters`` — the section's search
  with those constraints already applied. The reader sees them in the search
  sidebar and can remove them, so a tile is a starting point rather than a
  locked scope.

Filters are written with the same vocabulary as ``searchConfig.yaml``: ``facet``
is the description item's code with underscores, exactly as written there. The
kind of filter is **not** written — the facet's configured type decides it, so
the two files cannot disagree:

- ``ENUM`` / ``MULTI_REF`` facet — ``values: [...]``
- ``FULLTEXT`` facet — ``q: text``
- ``UNITDATE`` facet — ``from:`` and/or ``to:`` (a year, a date, or a full
  timestamp)

``image`` names a file of ``webResources.images`` (see above);
``positionX``/``positionY`` move the crop of a picture whose subject is not in
the middle, and take a percentage or a CSS keyword. In a ``GRID`` group the
picture fills the tile; in a ``LIST`` group it is a small mark beside the label.
A ``GRID`` tile with no picture keeps the portal's primary colour.

The **startup fails** on a mistake rather than serving a tile that cannot work:
an unknown facet for that section, a filter whose shape does not fit the
facet's type, ``apuType: COLLECTION`` (the one record type with no section of
its own), a span outside 1–2, an image that is not a readable file, an unknown
key. Filter *values* cannot be checked — they are data, so a tile may
legitimately find nothing in a given deployment.

Footer band
-----------

Columns of an optional ``heading``, prose ``paragraphs`` and a list of
``links``; prose comes first. A link takes ``label`` (or a well-known ``code``,
which the portal labels itself), ``url``, and an optional ``image`` — the small
mark a reader recognises the link by, again a file of
``webResources.images``. Marks are files rather than names the portal
knows, so a service that renames itself is a file the deployment swaps.

Prose carries its links **inside** the sentence. The deployment writes a named
``{placeholder}`` and defines it in that paragraph's ``links`` mapping; the
portal resolves it into a real link. There is deliberately no way to put markup
in configuration.

Because a link's position in a sentence follows the target language's grammar,
the placeholder travels with the text. Every placeholder must resolve **and**
every defined link must be used **in each language variant** — a link the
English sentence forgets is one the English reader never gets, so either
mistake stops the startup. A stray brace does too; there is no escape for a
literal one.

The columns are shown inside the portal's own page footer, and only on the home
page: every row of footer takes height from the content above it, and the record
detail — one screen tall, with its own scrolling panes — cannot spare it. The
links a deployment is required to publish (the accessibility statement above
all) belong in ``footer.links``; they sit in the same footer on every page.

Structured search results
=========================

A source system can deliver, alongside a record, a **pre-rendered structured
presentation** of it: rows of coded fields, optionally a thumbnail. Transfagent
carries it in the APUX ``result`` element and the portal stores it with the
record. A search hit that has one is presented with it; a hit that has none is
presented by its name and description as usual.

There is no switch to turn this on. Whether a record has a structured
presentation is a property of the data, so the portal simply uses it when it is
there — a deployment whose source system delivers none never sees the feature,
and a partially reimported one stays readable. ``search.structured-results:
OFF`` exists for the opposite case: a deployment that *has* the data but prefers
the plain presentation.

The codes in the data (``A_IB``, ``N``, ``J_S``, …) are the source system's own,
so the portal cannot know what they mean. ``resultLayout.yaml`` says how to
present them; without it the rows still render, only without icons, prefixes
and an emphasised heading.

.. code-block:: yaml

   fieldSeparator: " | "     # between fields of one row (default " | ")
   iconSize: 35              # default record-icon width in pixels
   icons:                    # keyed by the record code
     - code: A_IB
       image: archival-item-book.svg
     - code: A_IM
       image: archival-item-matrika.svg
       size: 28
   fields:                   # keyed by the field code
     - code: N
       heading: true         # this field's text links to the record
       scale: 1.2            # font size, 1 = base text
       bold: true
     - code: J_IC
       prefix: "Inv. č.: "   # visible text before the values
       image: J_IC.svg
     - code: J_S
       valueSeparator: ", "  # between the field's own values (default a space)
       color: "#666666"
       label: "Signatura"    # field name for screen readers, when no prefix shows

Field keys: ``code``, ``heading``, ``prefix``, ``label``, ``valueSeparator``,
``color``, ``scale``, ``bold``, ``image``. Icon keys: ``code``, ``image``,
``size``. An unknown key stops the startup rather than being ignored, and so
does an ``image`` reference when ``webResources.images`` is not
configured.

Notes on the individual keys:

- **heading** marks the one field whose text becomes the link into the record.
  Mark exactly one. With none marked, the first field of the first row is used;
  if that yields no text, the record's own name is.
- **label** is what a screen reader announces before the values. A field with a
  ``prefix`` needs none — the prefix is already its name. A field with neither
  is announced as bare values, which is why giving every styled field one of
  the two is part of meeting the accessibility obligation (see
  ``doc/accessibility.md``).
- **color** is applied to text only, never as a background. The contrast against
  the card background is the deployment's responsibility (WCAG 2.1 AA requires
  4.5:1 for body text).
- **image** is a plain file name inside ``webResources.images`` — no
  subdirectories, no paths. The portal serves those files at
  ``/api/v1/ui/images/<name>`` (``svg``, ``png``, ``jpeg``) and builds the
  URLs itself, so nothing has to be hosted next to the application. When the
  setting lists several directories, the name is looked up in each in turn.

``prefix`` and ``label`` are display text, so they are translated in the sibling
``resultLayout_localization.yaml``:

.. code-block:: yaml

   fields:
     en:
       J_IC:
         prefix: "Inv. no.: "
       J_S:
         label: "Reference code"

The **values** are never translated: they arrive already rendered from the
source system, like record names and descriptions.

Coming from the old portal
--------------------------

The old portal read the same information from a static ``app/config.json``
served next to the application, switched on with ``useStructuredResults: true``
in ``configuration.js``. Both are gone: the layout is server configuration now
and the switch is unnecessary. The keys map as follows.

.. list-table::
   :widths: 40 60
   :header-rows: 1

   * - ``config.json``
     - ``resultLayout.yaml``
   * - ``itemProperties[].type``
     - ``fields[].code``
   * - ``class: header``
     - ``heading: true``
   * - ``size: N``
     - ``scale`` — the old value was ``1 + (N-1)/10`` em, so ``size: 3`` becomes
       ``scale: 1.2``
   * - ``separator``
     - ``valueSeparator``
   * - ``icon``
     - ``image`` (the file moves into ``webResources.images``)
   * - ``itemSeparator``
     - ``fieldSeparator``
   * - ``typeIcons[].type`` / ``.icon``
     - ``icons[].code`` / ``.image``
   * - ``typeIconSize``
     - ``iconSize``
   * - ``typeIcon``
     - dropped — configuring ``icons`` is what enables them
   * - ``useStructuredResults``
     - dropped — see above; the opposite case is
       ``search.structured-results: OFF``

``color``, ``bold`` and ``prefix`` keep their names and meaning. Prefixes that
were Czech-only in ``config.json`` can now be translated in the sibling file.
