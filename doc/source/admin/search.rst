======
Search
======

.. note::

   This chapter describes the search behavior introduced by the relevance
   redesign (design document ``doc/search-relevance.md``, decision D-12).
   It is the normative target; the behavior stated here is pinned by
   automated tests — both supported engines (Elasticsearch and embedded
   Lucene) must satisfy it identically. The behavior below is implemented;
   only the relevance tuning on production data is still ongoing.

Search behavior
===============

What a user may rely on, independent of the configured engine:

Multiple words
   Every entered word must occur somewhere in the record (logical AND).
   Word order never decides *whether* a record matches — only how high it
   ranks. If no record matches all words, the portal automatically retries
   with "any of the words" and clearly labels the result as such.

Case and diacritics
   Matching ignores letter case and diacritics: ``rehor`` finds ``Řehoř``.
   Diacritics still matter for **ranking** — a user who types ``Řehoř`` sees
   the exact-diacritics match first.

Phrases
   Words enclosed in double quotes (``"kronika města"``) must occur as an
   exact phrase. A phrase never matches across values of two different
   description items. An unbalanced quote has no special meaning.

Partial words
   Partially typed words match automatically: a fragment of at least three
   letters (``relevance.partialMinLength``) matches *anywhere inside* a
   word — "pardub" and "ardub" both find Pardubice, "univ bratisl" finds
   both "Univerzita Bratislava" and "Bratislavská univerzita". Shorter
   fragments must match a whole word, and fragments never span word
   boundaries. Ranking is unaffected by the extra recall: records matching
   the words exactly rank above word-start matches, which rank above
   mid-word matches. There are no query operators besides quotes — an
   asterisk is a literal character with no meaning. (Substring matching is
   served by trigram companion fields; it costs index size, not query
   time.)

Stop words
   Common stop words of the described material's language ("v", "a", "na", …
   in Czech) never cause empty results; a query consisting only of stop words
   is still answered. The list follows ``search.content-locale`` and is the
   same on both engines. Supported: Czech, Danish, Dutch, English, French,
   German, Hungarian, Italian, Norwegian, Russian, Spanish and Swedish; any
   other language removes nothing, which only makes matching stricter.

What is searched
   All indexed description items participate in the fulltext search unless
   excluded by ``fulltext: false`` in ``types.yaml`` — names, descriptions,
   reference labels, numbers, and the boundary years of datings. Note that
   only the *boundary* years of a dating are text-searchable ("1945" does
   not find a record dated 1940–1950); searching inside date ranges is what
   the dating facet is for.

Facets of the general search
   Searching without choosing a section spans every record type, so the
   section facets of ``searchConfig.yaml`` cannot apply — their item types
   belong to a section. That search therefore offers three facets that need
   **no configuration at all**: the record type, the record's dating and
   "related to". They are part of the application, they appear in every
   deployment, and they cannot be renamed or switched off.

   The dating facet there covers the record's dating whatever item type
   records it, so it also finds records dated only under, say
   ``DATE_OF_CONTENT``. Because it uses the record's whole dating span, a
   record dated in several periods matches whenever any of them does.

   A record with no dating at all is **not** matched by a dating filter — it
   is not in 1805–1852. Where such records are among the results, the filter
   offers to include them and says how many there are; where every record is
   dated, as in an archival description, nothing is shown. This needs no
   configuration either.

Result ordering
===============

.. list-table::
   :widths: 22 78
   :header-rows: 1

   * - Mode
     - Meaning
   * - relevance
     - Best match first (see `How ranking works`_); the default whenever a
       query is entered. Ties are ordered alphabetically.
   * - name (A–Z, Z–A)
     - Alphabetical order of the described material's language
       (``search.content-locale``) — with the default ``cs-CZ`` the Czech
       alphabet (``č`` after ``c``, ``ch`` after ``h``); records without a
       name come last. The default when browsing without a query. Every
       visitor sees the same order, whatever language their browser asks
       for: the order is a property of the catalogue, not of the reader.
   * - dating (ascending, descending)
     - By the earliest (ascending) or latest (descending) dating of the
       record; records without any dating always come last.

Paging is stable under every ordering — a record never moves between pages
while the user browses an unchanged index.

How ranking works
=================

A match is weighted by *where* it occurs. The default tiers, from strongest
to weakest:

1. the record's name equals the query (diacritics-exact above
   diacritics-insensitive),
2. the name begins with the query,
3. the name contains the query as a phrase,
4. the name contains all query words,
5. a **variant name form** matches the same way (the other names of an
   access point — Praha/Prague; item types marked ``nameVariant: true`` in
   ``types.yaml``) — the whole ladder repeats one level lower, a preferred
   name counting roughly five times a variant,
6. labels of referenced records (originators, places, …),
7. the description,
8. any other indexed item.

Within a tier, standard fulltext scoring applies — shorter fields matching
more of the query score higher, so a record whose name *is* the query
outranks a record that merely mentions it.

Which facets a section offers
=============================

Each facet in ``searchConfig.yaml`` carries an optional ``when`` condition
saying where it is offered. Only two forms exist, and **anything else stops
the startup** rather than being quietly ignored — a condition the server
cannot read used to mean "offered everywhere", which is invisible until a
reader is shown a facet belonging to another kind of record.

.. code-block:: yaml

   facets:
     # bound to one section
     - when:
         apuType: FUND
       type: FULLTEXT
       source: TITLE

     # the section, plus a dependency on what another facet has selected
     - when:
         all:
           - apuType: ARCH_DESC
           - filter: REGISTRY_TYPE
             value: rejstřík zeměpisný
       type: MULTI_REF
       source: REG_GEO_REF

``apuType`` must be one of ``ARCH_DESC``, ``COLLECTION``, ``ENTITY``,
``FINDING_AID``, ``FUND``, ``INSTITUTION``. A facet with no ``when`` at all —
or an ``all`` naming no ``apuType`` — belongs to every section.

.. note::

   The new portal does not yet act on the ``filter``/``value`` half: such a
   facet is offered throughout its section rather than only once that value
   is selected. The old portal honours it. The condition is still checked, so
   a mistake in it is reported at startup.

Ordering a facet's values
=========================

``orderBy`` sorts an enumerated facet's values by frequency (``FREQ``, the
default) or alphabetically (``ASC``). ``order`` overrides that for the values
an archive wants offered first, whatever their frequency:

.. code-block:: yaml

   - when:
       apuType: ARCH_DESC
     type: ENUM
     source: UNIT_TYPE
     orderBy: FREQ
     order:
       - matrika
       - kroniky

The listed values lead, in the order given; everything else follows in
``orderBy`` order, so the list names a leading run rather than the whole
facet. A listed value the data does not contain simply does not appear.

Tuning the weights
==================

Weights are deployment configuration — the ``relevance:`` section of
``searchConfig.yaml``. Every key is optional; without the section the
built-in defaults above apply. Changes take effect after a restart, **no
reindex needed**.

.. code-block:: yaml

   relevance:
     # Minimum share of query words a record must match (default: all).
     minimumShouldMatch: 100%
     # Retry with "any word" when nothing matches (default: true).
     relaxOnNoHits: true

     # Minimum fragment length for automatic partial (substring) matching.
     partialMinLength: 3

     # Built-in fields with their default weights.
     name:         { exact: 1000, exactFolded: 800, prefix: 200, phrase: 100, terms: 50, wordPrefix: 30, contains: 15 }
     nameVariants: { exact: 200, exactFolded: 160, prefix: 40, phrase: 20, terms: 10, wordPrefix: 8, contains: 4 }
     refLabels:    { phrase: 12, terms: 10 }
     description: { phrase: 8, terms: 2 }
     allText:     { terms: 1 }

     # Item types promoted above the general-fulltext baseline.
     items:
       - source: TITLE
         phrase: 60
         terms: 30

Unknown item-type codes and non-positive weights are reported at startup.

To inspect ranking on real data, enable trace logging for the search layer —
the server then logs the resolved weight table, the planned query and the
scores of the top hits.

Limits
======

Protective limits, configurable in ``application.yml``:

.. list-table::
   :widths: 40 15 45
   :header-rows: 1

   * - Key
     - Default
     - Effect
   * - ``search.max-window``
     - 10 000
     - Maximum page window (``from + size``); deeper paging is rejected.
   * - ``search.track-total-hits-up-to``
     - 10 000
     - Result totals are exact up to this count; above it the portal shows
       "more than N results". Clients may request a higher accuracy per query
       (``totalUpTo``).
   * - ``search.track-total-hits-max``
     - 100 000
     - Upper bound for the per-query ``totalUpTo`` override.
   * - ``search.max-facet-buckets``
     - 200
     - Hard cap on facet bucket counts.
   * - ``search.request-timeout``
     - 5 s
     - Per-query time budget.

At most 32 query words are used; extra words are ignored.
