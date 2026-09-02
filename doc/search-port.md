# Search port design (Phase 5)

Status: **approved 2026-08-15** (review resolutions in §5). Indexing is the
keystone of future search capabilities — this design was agreed before
implementation started.

> **Update 2026-08-16 (§6):** the in-memory adapter described in §3 was replaced
> by an embedded **Lucene** adapter (`search.engine=lucene`) and the engine
> decision D-1 is settled — see §6. The port, builder, contract suite and ES
> adapter are unchanged.

## 1. Goals

1. Make indexing and search **testable**: the import → index → search round trip
   must run in plain `mvn test` (no external services) and the same behavior must
   be verifiable against a real Elasticsearch on demand.
2. Introduce the **engine seam** now: the new API (`/api/v1`) reads search results
   only through a port, so the later Hibernate Search / OpenSearch re-evaluation
   (Phase 8) is an adapter decision, not an API rewrite.
3. Do **not** touch the frozen old-API read path (`Params` → `QueryBuilder` → ES).

## 2. Current state (analysis)

Write path (`cz.aron.indexing.IndexingService`), callers: `ApuProcessor` (import:
`deleteApus(apuSourceId)`, `indexApus`) and `PostInitializer` (bootstrap:
create/drop indexes, full reindex incl. `indexRels`).

- `indexApus`: `ApuEntity` + Kryo-deserialized parts → one document per APU:
  fixed fields (`name` + ICU-collated `name.sort` subfield, `description`,
  `type`, `containsDigitalObjects`, nested `rels[targetId, type, groups, label,
  idLabel]`) plus **dynamic fields driven by `types.yaml`**: item-type code →
  typed value; `APU_REF` additionally `~LABEL` / `~ID~LABEL`; `UNITDATE` → range
  value + `~L`/`~H` bounds; `INT~NAME~INDEX` overrides the indexed name;
  `apuSourceId` for reimport deletes. **Only the last two lines of the
  conversion are ES-specific** — the rest builds a neutral map.
- Schema: `es_settings.json` analyzers (folding/tokenizing/stop, CI keyword,
  ICU sort collation) + `@Document` mapping + per-item-type custom mapping
  (analyzer choice by `indexFolding`/`caseInsensitive`, `date_range`, `~L`/`~H`
  dates).
- Reindex trigger: CRC of the indexed-fields config, persisted in
  **`./lastConfigCrc.txt` in the working directory** (fragile — see Q1).
- `rels` index: **write-only within the application** (no in-app reader found;
  the old UI's nested `rels.*` queries hit the nested field of the `apu` index,
  not this index) — see Q3.
- Legacy quirk: `IndexConfig` requires `spring.elasticsearch.rest.uris` —
  normalized as part of this phase (adapter owns its configuration).

## 3. Design

### 3.1 Neutral document model + shared builder (the fidelity trick)

Package `cz.aron.search`:

- `ApuDocument` — engine-neutral: uuid, name, indexedName, description, type,
  containsDigitalObjects, apuSourceId, nested relations, and the dynamic typed
  values (`Map<String, List<Object>>` keyed by item-type code, values already
  typed: String / Integer / year-range / bounds).
- `RelationDocument` — source, relation, target.
- `ApuDocumentBuilder` — the current `convert()` logic extracted **unchanged**
  (Kryo parts walk, `types.yaml` typing, UNITDATE bounds, APU_REF labels,
  `INT~NAME~INDEX`). Pure code, no engine imports → **unit-testable directly**,
  which covers the riskiest logic (field typing) without any engine at all.

Both adapters consume the same `ApuDocument`. The in-memory adapter therefore
indexes byte-identical *input*; only matching/analysis differs — that is what
keeps the fake honest.

### 3.2 Port interface

```java
public interface SearchIndex {
    // schema lifecycle
    void createSchema();                 // no-op if current
    void dropSchema();
    Long storedSchemaCrc();              // see Q1 (schema-version metadata)
    void storeSchemaCrc(long crc);

    // write side (used by import + bootstrap reindex)
    void indexApus(Collection<ApuDocument> docs);      // upsert by uuid
    void indexRelations(Collection<RelationDocument> rels);
    void deleteApusBySource(long apuSourceId);

    // read side - deliberately MINIMAL now; grows with the Phase 7 slices (D-9)
    ApuSearchResult search(ApuSearchQuery query);
    // ApuSearchQuery: fulltext?, typed facet filters, paging (capped), sort mode
    // ApuSearchResult: total, hits (uuid, name, description, type,
    //                  containsDigitalObjects), facet buckets (value, count)
}
```

Notes:
- The APU **detail** endpoint of the new API reads the database (entity + Kryo
  parts), not the index — the port stays a search interface.
- Bean selection by property `search.engine` = `elasticsearch` (default) |
  `memory`. `PostInitializer` and `ApuProcessor` depend only on the port.

### 3.3 Adapters

- `cz.aron.search.es.ElasticsearchSearchIndex` — the current IndexingService
  code moved behind the port (bulk indexing, delete-by-query, settings+mapping
  creation). Production default; the only engine deployments use (D-1).
- `cz.aron.search.memory.InMemorySearchIndex` — intentionally minimal:
  - documents in a concurrent map keyed by uuid; delete-by-source = scan;
  - fulltext = token match on normalized text (lowercase + `java.text.Normalizer`
    diacritics folding) over name/description/text fields — mirrors the folding
    analyzer's essence;
  - facet filters = exact match on keyword-typed values; UNITDATE = interval
    overlap on the stored bounds;
  - facet counts = simple term counting; sort = normalized string compare /
    insertion order for relevance.
  - **Explicitly not emulated**: stop words, stemming, phrase/prefix semantics,
    scoring/ranking, exact ICU collation. Anything depending on these is tested
    only against real ES.

### 3.4 Schema lifecycle

`SearchIndexManager` (engine-neutral, replaces the logic in `PostInitializer`):
compares the `types.yaml` indexed-fields CRC with the value stored **in the
index/schema metadata** (ES: index `_meta`; memory: trivial field) and decides
recreate+reindex vs. no-op. This removes `./lastConfigCrc.txt` (Q1) — the marker
then lives and dies with the index it describes, which also fixes the "file says
current but index was recreated elsewhere" drift.

### 3.5 Testing

1. **Builder unit tests** — `ApuDocumentBuilder` against `types.yaml` fixtures:
   typing per item type, UNITDATE bounds, APU_REF labels, `INT~NAME~INDEX`,
   not-indexed types skipped. No engine involved.
2. **Adapter contract suite** — one abstract test class (index → search →
   facets → delete-by-source → reindex-upsert → schema recreate), run:
   - against `InMemorySearchIndex` in plain `mvn test`;
   - against real ES in an opt-in profile `es-it` (`elasticsearch-maven-plugin`
     downloads and runs a single node — no Docker; maven-failsafe ITs; CI-able).
   The suite asserts only behavior both engines must share (folding included —
   "Václav" found by "vaclav"); engine-specific behavior gets ES-only tests.
3. Existing 31 tests stay green throughout (gate for every step).

### 3.6 Dev mode

Spring profile `dev`: H2 + `search.engine=memory` + seed data imported through
the real pipeline (`ApuProcessor.processTestingInputStream`) at startup. Whole
stack runs with zero external services — for UI development (Phase 6/7) and
demos. Seed APUX files: own-authored minimal samples (see Q4).

## 3.7 One abstraction — why the port and not Hibernate Search (review 2026-08-15)

`SearchIndex` is the SINGLE abstraction the application sees; Hibernate Search is
not a second layer on top of it but a candidate *implementation behind it*
(Phase 8). HS cannot be the abstraction now because the frozen old API pins the
physical index layout (`QueryBuilder` addresses concrete fields/analyzers in the
`apu` index: `TITLE~MAIN`, `rels.*`, `name._sort`, `date_range`). HS owns its
schema by design, so adopting it today means either forcing it to emit the legacy
layout (fighting the framework; the "unwrap to the engine" escape hatch would
carry the whole old API, not corner cases) or double indexing (two writers, two
index sets, drift, doubled resources). Honest counterpoint: HS's Lucene backend
would provide the fast in-process test engine that the in-memory adapter
hand-builds — real value, but it does not outweigh the layout pin. At old-API
retirement the layout unfreezes and an HS implementation can replace the ES
adapter behind the same port, validated by the same contract suite; if HS then
covers everything, the port may collapse into it and the in-memory fake retires
in favor of HS-Lucene.

### S4 scope (sharpened)

S4 implements no new engine code — it runs the existing contract suite against a
real Elasticsearch (opt-in `es-it` profile, elasticsearch-maven-plugin, 8.18.x).

What the profile is (clarified 2026-08-15): NOT embedded Elasticsearch — in-JVM
embedding has been unsupported by Elastic since ES 5.x and is not used here. The
profile starts the ordinary official ES distribution as a separate local process
(downloaded once, cached; started/stopped around the failsafe ITs; no Docker).
The ITs see only `search.engine=elasticsearch` + `spring.elasticsearch.uris` —
the provisioning mechanism is invisible to them and therefore swappable.
That swappability is used: `-Des.home` points the profile at an already installed
distribution, which it then leaves alone — no download, no extraction, no delete.
CI relies on it (a prepared image carries the distribution, so a blocking job
never depends on artifacts.elastic.co); locally the default self-provisioning
still applies.
**ICU dropped (review decision 2026-08-15):** the real requirement is matching
words with and without diacritics (češka ↔ ceska) — CAM and Elza both meet it
with plain `ASCIIFoldingFilter`, no ICU. `es_settings.json` now uses built-in
analyzers only (`standard` tokenizer + `lowercase` + `asciifolding`; Czech stop
words unchanged). The one thing ICU genuinely provided — correct Czech
alphabetical ordering (c < h < ch < i) — moved to an **index-time collation key**
(`ContentLocale.sortKey`: `java.text.Collator` of the configured `search.content-locale`,
hex-encoded, stored as the `nameSort` keyword; the Elza pattern, see
`ElzaLocale`). The locale is a deployment setting rather than a compiled-in
specialization, and it is part of the schema fingerprint
(`SearchIndexManager.schemaCrc`), so changing it rebuilds and reindexes. Sorting is
thereby engine-neutral: identical in every adapter, no analysis plugin on any ES
server, one less version-coupled moving part. The old API's `sort=name` maps to
`nameSort` internally; external behavior is preserved (both approaches implement
cs collation). Unit-tested with the c/h/ch/i ordering case. The es-it target
follows the current ES major (9.x) — the Boot-managed 8.x client against a 9.x
server is exactly the combination deployments will run, and the ITs verify it.

Outcome (implemented 2026-08-15): the originally planned third-party automation
(elasticsearch-maven-plugin) was **dropped during implementation** — ES 8.18's
`elasticsearch-plugin install` fails on Windows with an AccessDeniedException on
the staging-dir rename (its own scan holds file handles during the move), and the
maven plugin re-extracts the instance directory on every start, so the plugin
cannot be combined with the direct-unzip workaround. The profile therefore uses
an **own launcher** (antrun): download cached in `~/.m2/es-cache` → fresh extract
→ analysis-icu placed by direct unzip → start with `-p` pidfile on port 19200
(security off, single-node, 512m heap) → HTTP health wait → pidfile kill after
the ITs. Windows and Linux covered; no third-party dependency; the same pattern
serves OpenSearch if ever needed. Verified: the 7 contract ITs pass identically
against real ES 8.18.8 + analysis-icu and against the in-memory adapter.
It is the fidelity anchor for exactly what only a real engine can verify:
`es_settings.json` analyzers (folding), the `_meta` CRC mechanism (new S2 code),
delete-by-query, types.yaml-driven mapping creation. Stretch: a seeded smoke test
of the old API's search endpoints (`/apu/list*`) — today the only untested
old-API area.

Edge-case coverage matrix:

| Edge case | Coverage |
|---|---|
| Fake diverges from ES (analysis/semantics) | contract limited to engine-shared behavior + same suite on real ES; engine specifics banned from the contract, ES-only tests instead |
| Ranking/highlights/phrase-prefix (Phase 7 reads) | contract asserts weak invariants (membership, counts); specifics ES-only; dev mode = functional, not relevance-accurate |
| `_meta` CRC + rebuild trigger on real ES | contract test `schemaCrcLivesAndDiesWithTheSchema` under es-it |
| Old-API aggregation queries (direct path, not port) | frozen; URL surface pinned; behavior smoke under es-it with seeded data (stretch) |
| Hit order differs between engines | contract forbids order assertions (documented on ApuSearchResult) |
| Analyzer config drift (es_settings.json edits) | es-it folded-search test breaks loudly; memory adapter approximate by design |

## 4. Migration steps (each gated by a green build)

1. **S1** Extract `ApuDocument` + `ApuDocumentBuilder` (pure refactor;
   IndexingService delegates); add builder unit tests.
2. **S2** Introduce `SearchIndex` port + ES adapter (move IndexingService code);
   switch `ApuProcessor`/`PostInitializer` to the port; `SearchIndexManager`
   with CRC-in-metadata (Q1).
3. **S3** In-memory adapter + contract suite (runs in `mvn test`); test profile
   switches to `search.engine=memory` (Q2).
4. **S4** `es-it` profile: contract suite against real ES via
   elasticsearch-maven-plugin (Q5: version).
5. **S5** Dev mode profile + seed; document the dev loop.
6. Read side (`search(...)`) is implemented in S2–S3 only to the extent the
   contract suite needs; it grows with the Phase 7 slices.

## 5. Review resolutions (2026-08-15)

- **Q1 — agreed:** the reindex CRC moves into index/schema metadata (ES `_meta`);
  `./lastConfigCrc.txt` is ignored afterwards (one extra full reindex on the
  first deployment with the change).
- **Q2 — agreed:** the Phase-2 stopgap `indexing.startup-enabled` is retired;
  tests run the real bootstrap against `search.engine=memory`.
- **Q3 — checked in the old implementation:** the old backend's `RelationApi`
  (inQool eas `DomainApi`) served `/relation` by querying the relation index in
  ES — that is what the old UI's `/relation` calls used. aron2's `rels` index is
  the groundwork for that capability: the port keeps writing it; it will either
  back a future related-listing slice (Phase 7) or be removed at old-API
  retirement if DB-based related queries suffice.
- **Q4 — resolved:** APUX samples from `aron.git/priklady` may be copied for the
  dev-mode seed (no license barrier for the data examples).
- **Q5 — resolved:** the `es-it` profile pins the same version line as the
  Elasticsearch client managed by the Spring Boot BOM — currently **8.18.x**
  (client 8.18.8). This also documents the ES server requirement of aron2
  deployments.

## 6. Engine decision — D-1 settled (2026-08-16)

Re-evaluation requested before Phase 7 (originally deferred to Phase 8).
Decision: **the `SearchIndex` port stays the single abstraction; Hibernate
Search is not adopted; embedded Lucene becomes the second supported engine**
(`search.engine=lucene`), replacing the in-memory adapter everywhere (tests,
dev mode, small/ES-less deployments).

### Hibernate Search — why not (final)

- **Version-matrix coupling.** The only HS line compatible with Spring Boot 3.5
  (ORM 6.6) is HS 7.2 ("limited support"), whose ES backend tops out at
  **ES 8.18 — no ES 9 at all**, while aron2 already runs and tests against
  ES 9.5. HS 8.x requires ORM 7 (= Spring Boot 4 first) and its certified
  matrix trails engine releases by design. The direct client pairing
  (8.18 client ↔ 9.5 server) is proven here in `es-it`.
- **Bootstrap-frozen schema vs. configuration-driven fields.** HS declares the
  full index schema once at Hibernate bootstrap (TypeBinder). The org's own
  "dynamic schema" layers confirm the cost of working around that: Elza's
  `ApCachedAccessPointBinder` pre-creates fields for every part×item×spec code
  combination (with its own TODO about combinatorial bloat), CAM's
  `AeRecordCacheBinder` preallocates **1000 numbered slots** ×3 variants with an
  external name→slot mapping, both via `SpringContext.getBean` static hacks.
  Raw Lucene documents are schemaless — going direct *skips* that whole coping
  layer; types.yaml-driven fields need no pre-declared mapping.
- **Indexing model mismatch.** HS's core value is automatic ORM-entity↔index
  sync; ARON indexes explicitly from the import pipeline (batch per ApuSource,
  delete-by-source, CRC-driven rebuild) — we would disable the framework's main
  feature and keep its constraints.
- **Old API pins the physical ES layout** until Phase 8 — HS would force legacy
  layout emulation or double indexing.
- Both Elza and CAM use the HS **Lucene backend only**; there is no in-house
  HS-over-ES experience to reuse.

### Embedded Lucene adapter — why yes

Triggers confirmed by the product owner: ES-less deployments are a real need
(incl. the internal test environment) and dev-mode search realism matters.
ES is Lucene inside — analysis (tokenization, lowercase, ASCII folding) and
scoring match the production adapter **by construction**, replacing the
imitation-based in-memory fake. One adapter now covers small deployments, dev
mode and the default test suite; the contract suite runs on Lucene by default
and on real ES under `es-it`, unchanged. Storage: `search.lucene.path`
(persisted) or in-memory when unset.

Phase 8's re-evaluation thereby shrinks to a pure engine/adapter choice
(e.g. OpenSearch) — the abstraction question is closed.

## 7. Old API on the embedded engine — the `OldApiSearch` seam (2026-08-17)

Originally an ES-less deployment served the new API only. That kept the old UI
unusable on ES-less environments (the internal test server) and left the old
API's search endpoints as the only untested old-API area — both hurt Phase 7
work, where the old UI is the side-by-side reference for the new frontend.

Resolution: the four frozen search endpoints (`/api/aron/apu/list*`) now sit
behind a dedicated internal seam, `cz.aron.indexing.OldApiSearch`, selected by
`search.engine`:

- `EsOldApiSearch` — the former direct `Params → QueryBuilder → ES` code moved
  verbatim out of the controller. Still frozen, still the parity reference;
  production old-API deployments keep requiring Elasticsearch.
- `search.lucene.LuceneOldApiSearch` — **dev/test-grade** translation of the
  same `Params` model onto the embedded index. Possible only because
  `ApuDocumentBuilder` makes the index content identical across engines; the
  class is pure query translation. Covers the request shapes the old UI sends:
  boolean filter trees (EQ/FTXF/FTX/RANGE/CONTAINS/AKF), offset paging,
  name/score sort, TERMS (with `size`) and MAX/MIN aggregations, and the nested
  `rels` aggregations (§13). Not covered: `searchAfter` (the old UI pages by
  offset). Divergences (FTX approximation, relevance ranking, bucket tie order)
  are acceptable for dev/test and must not be used to reason about old-API
  parity.

The port's own model (`ApuSearchQuery`) deliberately did NOT grow for this —
the "abstraction must not carry the whole old API" argument from §6 applies to
the port as much as to Hibernate Search. The seam is old-API-private and dies
with the old API.

Testing: `LuceneOldApiSearchTest` (plain unit, translator behavior) and
`OldApiSearchTest` (endpoints end-to-end with the old UI's literal request
JSON) run in the default suite; `OldApiSurfaceTest` pins that the endpoints
answer without Elasticsearch. ES-specific behavior stays under `es-it`.

## 8. Read-side growth: facet options, REF buckets, dating bounds (2026-08-17)

The old UI served as the requirements catalog (see §7): its per-facet N+1
queries (option type-ahead, year-slider bounds) become server-side features of
the new API. The port grew accordingly - still new-API-shaped:

- `BucketRequest(bucketField, filterField, size)` replaces the plain bucket
  field set: reference facets enumerate the composite `<code>~ID~LABEL` field
  while their Values filter sits on `<code>`, and the explicit pairing drives
  the multi-select exclusion. Buckets are now ordered (count desc, value asc)
  and capped by the request.
- `boundsFields` + `Bounds(minMillis, maxMillis)`: dating bounds of UNITDATE
  facets, computed with the facet's own Range filter excluded. Range filters
  moved from the engines' main query into the facet-filter layer (post_filter
  on ES, exclusion-aware clauses on Lucene) so the exclusion works; hits and
  totals are unaffected.
- `POST /api/v1/facets/{code}/options` is a **controller composition** over
  `search()` (one BucketRequest + a document-level label Text filter), with an
  engine-neutral label post-filter in `SearchController` (folded all-words-
  last-prefix match) - no dedicated port operation.

The Lucene min/max brick added for the old API's MIN/MAX metrics
(`LuceneSearchIndex.minMaxMillis`) now serves both the old-API aggregations and
the port's bounds - the convergence intended in §7.

## 9. Relation filter — "find related" (2026-08-21)

The old portal's *Najít související* button sent a hand-built boolean tree to
the generic old API: `AND(NOT(id EQ uuid), AKF(uuid))`, where `AKF` matched the
uuid against every keyword field of the document. Only two kinds of field could
ever equal a uuid — the document's own `id` (hence the `NOT`) and the dynamic
`APU_REF` item fields — so the effective meaning was *every record whose
reference item points at this one*. Both engines already serve `AKF` on the
frozen old-API path (`QueryBuilder`, `LuceneOldApiSearch`); this section is
about the new API.

The port grew **one** filter, `FieldFilter.Related(refFields, targets, uuids)`,
matching a document that either references any of `targets` through any of
`refFields`, or is itself one of `uuids`. The two halves are ORed — one
relation, either end of it.

Three decisions are worth keeping:

- **Everything interpretive is resolved above the port.** `refFields` comes from
  the facet's scope (one item field, or every reference field for the built-in
  `~RELATED` facet), and `uuids` from expanding the named APUs' own visible
  references. The adapters therefore need neither types.yaml nor the display
  model — the port's query model stays new-API-shaped.
- **The clause belongs in the main query, not the facet-filter layer.** Facet
  filters are excluded per facet to give multi-select its "widen your own
  selection" behavior; a relation restriction is not a facet the reader can
  widen, so it must constrain bucket counts too. Putting it where the Text
  filters already sit gets that for free on both engines (ES aggregations run on
  the query, before the post_filter) and is pinned by
  `relatedFilterNarrowsFacetBucketsToo`.
- **Direction is asymmetric about `visible="false"`, deliberately.** APUX marks
  an item index-only to make *its own* record findable by it ("slouží jen pro
  dohledání jednotky popisu"). Searching *inwards* therefore honors such
  references; searching *outwards* ignores them, or the reader would be led to
  records the page they came from never showed. A village indexed against both
  its region and the whole country stays findable under either, while its own
  related search offers only what it displays.

No index change and no reindex: the fields queried are the ones
`ApuDocumentBuilder` has always written. The self-exclusion of the old tree is
gone with the field it worked around — a record now matches itself only by
genuinely referencing itself.

The separate `rels` index (§5, Q3) is still not needed for this: the flat
`<code>` fields evaluate the filter exactly, because the field name *is* the
relation type. Its remaining candidate use is enumerating *which* relation types
connect to a given record (with counts) for a narrowing UI — the old portal's
`MULTI_REF_EXT`, whose entity type-ahead also needs the never-populated
`incomingRelTypeGroups`. Both stay unimplemented; the `relation` DB table
(source, relation, target) can answer the same question with plain SQL if that
slice lands.

## 10. Built-in facets of the general search (2026-08-21)

A reader who searches without picking a section could not narrow the result at
all: facets are section-scoped, so the controller offered none and answered 400
for any filter. Yet "everything from 1805 to 1852" is exactly the question a
cross-section search invites.

Three reserved facets now answer it, registered in `BuiltInFacets` and served
by `getFacets` when no `apuType` is given: `~TYPE` (record type), `~DATE` (the
record's dating) and the older `~RELATED`. They are product features, not
deployment configuration — a search spanning every record type has no section's
item types to configure against — and their codes are tilde-prefixed, which a
real item-type code cannot be, so both kinds of facet share one filter
vocabulary and one validation path.

Four decisions are worth keeping:

- **The port learned one reserved field, not a second filter kind.**
  `FieldFilter.ANY_DATING` means "the record's dating, whichever item type
  carries it". It bounds a slider on the `dateL`/`dateH` pair
  `ApuDocumentBuilder.computeDateBounds` has always written and the
  `DATE_ASC`/`DATE_DESC` sorts already read; what it *filters* is every dating
  item type, resolved above the port (see §11).
- **`~TYPE` reuses the existing `type` field and bucket machinery.** Its filter
  is an ordinary `Values` on `type` and its buckets an ordinary bucket request,
  so multi-select comes free through the same `excludedField` mechanism. The
  separate `typeCounts` aggregation — the chips that lead *into* a section — now
  excludes a filter on `type` as well as the query's own `apuType`, for the same
  reason: a reader who selected one type must still see the alternatives.
- **A dating filter's exclusion of undated records is disclosed, not softened.**
  `Bounds` grew `undatedCount`, counted over the same documents as the bounds
  (own RANGE filter excluded) so it does not move with the slider. ES gets it
  from the bounds filter aggregation's `doc_count` minus the existing
  `value_count`, Lucene from two cheap counts. `FieldFilter.Range.includeUndated`
  then ORs "no dating in this field" into the clause. Default off: the filter
  means what it says.
- **`~RELATED` has no buckets.** Its options are records found by name through
  the ordinary search endpoint, because enumerating the targets of every
  reference field at once is a union no engine answers cheaply — and the reader
  is looking for one record, not for a distribution.

`~TYPE` and `~DATE` are rejected in a section search (400). A section already is
one record type, and its configured dating facets each name the item type they
date; offering a second, wider slider beside them would make the two disagree
for reasons no reader could reconstruct.

## 11. Dating: the intervals, not their hull (2026-08-22)

`~L`/`~H` are the **hull** of a record's datings — the earliest lower and the
latest upper bound — and until now that was also what a dating filter matched
on. A record dated 1850–1860 as a unit and 1600 for the original it copies has a
hull of 1600–1860, so it answered a search for 1700: a period it was never
assigned. The document-level `dateL`/`dateH` made this worse for the built-in
`~DATE` facet, whose hull spans every dating item type at once.

**The old portal never behaved that way.** It sends its dating filter as
`{field: <item code>, operation: RANGE, gte, lte}` — the item field itself, not
the bound fields, which it uses only for the slider's ends. On Elasticsearch that
field is a multi-valued `date_range`, so a range query there has always matched
per interval with the default `INTERSECTS`. Hull matching was ARON 2's own
regression, and this closes it.

What changed:

- **`FieldFilter.Range` carries a list of fields and matches per interval.**
  Elasticsearch queries the `date_range` field it already maps and populates —
  no mapping change, **no reindex**. Lucene indexes each dating as a
  multi-valued `LongRange` (`newIntersectsQuery` matches when any of them
  overlaps) and the layout version is bumped, so persisted embedded indexes
  rebuild themselves on startup.
- **The field list is resolved above the port**, from the display model, exactly
  as the relation facet's scope is: one field for a section's dating facet, every
  indexed UNITDATE item type for `~DATE`. Adapters stay free of types.yaml, and
  `ANY_DATING` never reaches them as a filter field.
- **Bounds became a request, not a field name** (`ApuSearchQuery.BoundsRequest`).
  Multi-select needs to know which filters belong to the facet whose bounds are
  being computed, and once a facet spans several fields its identity is no longer
  its field. `BucketRequest` already separated "the field I aggregate" from "the
  field whose filter I ignore" for the same reason; bounds now do too — `~DATE`
  bounds on the record-level hull while excluding filters on every dating field.
- **"Undated" stays a question about the bound field**, not about the intervals,
  so `includeUndated` and the `undatedCount` shown beside the slider cannot
  disagree.
- **The old API's Lucene path matches per interval too**, closing a divergence
  that made the embedded engine answer the frozen API differently from
  Elasticsearch. `LuceneOldApiSearch` no longer rewrites a UNITDATE range into a
  hull intersection.

Sorting is unaffected: a sort needs one key per document, and the hull's ends are
the right ones (`dateL` ascending, `dateH` descending, undated last).

The case that tells the two apart needs a record with **two** datings of one item
type, which only the interval form of a fixture can express — hence
`DocumentFixtures`, where the shapes `ApuDocumentBuilder` produces are written
once for every test that builds a document by hand.

**What the ES run caught, and why the default suite could not.** Two things, and
both are worth knowing before the next adapter change. One was a plain bug on a
path only Elasticsearch takes: the post_filter passed `null` where the exclusion
list was expected, so every ES query failed — the Lucene adapter had the same
latent slip and the default suite caught it there. The other is structural: the
new fixtures dated a record under `DATE_OTHER`, an item type the test
`types.yaml` never declared. Lucene is schemaless and indexed it; Elasticsearch
maps its fields from the display model and had nowhere to put it, so the query
matched nothing. `ApuDocumentBuilder` skips an unrecognized item type, so no real
document can carry such a field — the fixture was the unrealistic part, and the
model now declares the second dating type. Both say the same thing: the port's
contract test is only as good as its last `-Pes-it` run.

## 13. The nested `rels` aggregations on the embedded engine (2026-09-02)

The old UI has two request shapes the Lucene path answered with an empty result
of the right structure (§7): the autocomplete of every **reference facet**
(`NESTED(rels) > FILTER(rels.type, rels.label) > TERMS(rels.idLabel)`) and the
entity detail's **relationship-type list** (`FILTER(apu) > NESTED(rels) >
FILTER(rels.targetId, rels.groups) > TERMS(rels.type)`). On an ES-less
deployment that left the old UI - the side-by-side reference for the new
frontend - without its reference facets, which is exactly what the seam exists
to avoid.

§7 planned a join over the `rels` index. It is not needed: **the flat document
already carries every relation**, because `ApuDocumentBuilder` writes, per
APU_REF item type, the target's uuid (`<code>`), its indexed label
(`<code>~LABEL`) and the pair (`<code>~ID~LABEL` = `uuid|label`). A relation is
therefore reconstructible from one `~ID~LABEL` term: the item type is its type
and carries its groups, and the term's two halves are the target and its label.
`LuceneOldApiSearch.relScope` enumerates those terms for the item types the
filter admits and counts each against the current query - the mechanism the
document-scope TERMS aggregation already used. No join, no new index field, no
reindex.

Why the conditions are evaluated on the reconstructed relation instead of being
translated into a query: a `rels.*` filter selects **relations**, while a
document clause selects records. A description referencing two funds, one of
them matching what the reader typed, matches the document clause through the
first and would then contribute the second as an option too - the bug the
nested query exists to prevent. So the filter becomes a `RelCondition` with two
halves: which item types can possibly match (`rels.type`/`rels.groups` are
decided by the type alone, so a type-scoped filter enumerates one field instead
of all of them) and whether a concrete relation matches. Because a relation's
count depends on its value alone, a further condition narrows an
already-enumerated scope in place.

Two things this cannot reproduce, both stated in the class javadoc:

- the label a condition matches is the target's **displayed** label (the
  `~ID~LABEL` half), where ES matches the **indexed** one (`<code>~LABEL`).
  They differ only where the target carries an `INT~NAME~INDEX` override, and
  the flat form is the one the reader sees in the dropdown;
- a NESTED bucket's own count is the **records** in scope, not their relations.
  Counting those means enumerating every relation value in the index, and the
  old UI reads only the buckets below it. A rel-scope FILTER bucket does report
  relations, which is what ES reports there.

An empty `FTXF` value keeps matching nothing, as an empty
`match_phrase_prefix` does on Elasticsearch - which is why a reference facet's
autocomplete offers its options only once the reader types. That is v1
behaviour, not an omission.

Testing: `LuceneOldApiSearchTest` covers the translator (both shapes, the
two-funds record that tells a per-relation condition from a per-record one, the
group gate, the empty query) and `OldApiSearchTest` runs the old UI's literal
request JSON end to end.
