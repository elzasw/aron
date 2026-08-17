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
    Long storedFieldsCrc();              // see Q1 (schema-version metadata)
    void storeFieldsCrc(long crc);

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
**ICU dropped (review decision 2026-08-15):** the real requirement is matching
words with and without diacritics (češka ↔ ceska) — CAM and Elza both meet it
with plain `ASCIIFoldingFilter`, no ICU. `es_settings.json` now uses built-in
analyzers only (`standard` tokenizer + `lowercase` + `asciifolding`; Czech stop
words unchanged). The one thing ICU genuinely provided — correct Czech
alphabetical ordering (c < h < ch < i) — moved to an **index-time collation key**
(`ApuDocumentBuilder.czechSortKey`: `java.text.Collator` cs locale, hex-encoded,
stored as the `nameSort` keyword; the Elza pattern, see `ElzaLocale`). Sorting is
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
| `_meta` CRC + rebuild trigger on real ES | contract test `fieldsCrcLivesAndDiesWithTheSchema` under es-it |
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
  name/score sort, TERMS (with `size`) and MAX/MIN aggregations. Not covered
  yet: the nested-rels aggregation shapes of detail pages (planned as a join
  over the `rels` index) and `searchAfter` (the old UI pages by offset).
  Divergences (FTX approximation, relevance ranking, bucket tie order) are
  acceptable for dev/test and must not be used to reason about old-API parity.

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
