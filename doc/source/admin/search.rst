======
Search
======

.. note::

   This chapter describes the search behavior introduced by the relevance
   redesign (design document ``doc/search-relevance.md``, decision D-12).
   It is the normative target; sections not yet available in a release are
   marked with a todo. The behavior stated here is pinned by automated
   tests — both supported engines (Elasticsearch and embedded Lucene) must
   satisfy it identically.

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

Begins-with
   A trailing asterisk (``kron*``) matches words beginning with the prefix.
   An asterisk in any other position is a literal character; there are no
   other query operators.

Stop words
   Common Czech stop words ("v", "a", "na", …) never cause empty results; a
   query consisting only of stop words is still answered.

What is searched
   All indexed description items participate in the fulltext search unless
   excluded by ``fulltext: false`` in ``types.yaml`` — names, descriptions,
   reference labels, numbers, and the boundary years of datings. Note that
   only the *boundary* years of a dating are text-searchable ("1945" does
   not find a record dated 1940–1950); searching inside date ranges is what
   the dating facet is for.

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
     - Czech alphabetical order (``č`` after ``c``, ``ch`` after ``h``);
       records without a name come last. The default when browsing without
       a query.
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
5. labels of referenced records (originators, places, …),
6. the description,
7. any other indexed item.

Within a tier, standard fulltext scoring applies — shorter fields matching
more of the query score higher, so a record whose name *is* the query
outranks a record that merely mentions it.

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

     # Built-in fields with their default weights.
     name:        { exactCs: 1000, exact: 800, prefix: 200, phrase: 100, terms: 50 }
     refLabels:   { phrase: 12, terms: 10 }
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
       "more than N results". Clients may request a higher (server-capped)
       accuracy per query.
   * - ``search.max-facet-buckets``
     - 200
     - Hard cap on facet bucket counts.
   * - ``search.request-timeout``
     - 5 s
     - Per-query time budget.

At most 32 query words are used; extra words are ignored.

.. todo::

   Implementation of this chapter is being rolled out in stages (see the
   rollout plan in ``doc/search-relevance.md``): total-count reporting first,
   then the index additions, ranking, ordering modes and type counts. Update
   the note at the top of this chapter as stages land.
