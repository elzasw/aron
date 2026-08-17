# Search relevance and result ordering

Design for field-weighted relevance ranking, user-selectable result ordering and
bounded query cost in the new API (`/api/v1/apu/search`). Companion to
`doc/search-port.md`, which owns the port itself; this document owns what the
port's read side *ranks* and *orders*, and — normatively, in §5 — what search
behavior users and administrators may rely on.

Revised 2026-08-17 after a critical review: diacritics-aware exact tier,
non-scoring gate, `AUTO` sort mode, trailing-`*` operator, multi-valued
`allText`, and the normative behavior specification were added; decisions
R-8…R-12 record the second round.

## 1. Goals

1. **Field-weighted relevance** — a hit in the APU name must outrank a hit deep in
   the metadata, with the weights configurable per deployment, not compiled in.
2. **User-selectable ordering** — relevance, name, dating; relevance the default
   wherever it carries information.
3. **All item contents searchable** — the new API currently searches only
   `name` + `description`, a recall regression against the old API's
   `query_string` over all indexed fields.
4. **Bounded, engine-consistent cost** — explicit limits that both adapters
   honour identically, instead of the silent per-engine defaults in place today.
5. **Documented behavior** — the observable search semantics (multi-word
   queries, diacritics, operators, ordering) are a normative, tested
   specification (§5), not an emergent property of the adapters.

## 2. Reference implementations — CAM and Elza

Both are Hibernate Search + Lucene with dynamic fields; Elza is the evolved
version of the same lineage. What they share is worth copying; the query
mechanics are not.

**Weights are deployment data, not code.**

| | CAM | Elza |
|---|---|---|
| Location | `cam-start/config/indexSearch.yaml` (runtime file) | `ui_setting.xml` in a package → DB `UISettings` (`INDEX_SEARCH`) |
| Model | `FieldSearchConfig`: name, boost, boostExact, boostTransExact, transliterate | `SettingIndexSearch.Field`: same + **boostFulltext** |
| Range in use | 1.0 – 2.0 | 1.0 – 800.0 |

Elza's shipped table (`package-cz-base/src/ui_setting.xml`) encodes four rules:

- **exact ≈ 5 × substring** — matching a whole field beats matching inside it
- **diacritics-folded exact ≈ 0.8 × true exact** — "Rehor" finds "Řehoř", "Řehoř" wins
- **preferred name ≈ 5 × variant name**; **main name ≫ minor name**
- an **exact identifier match dominates** everything (`accessPointId`, boost 100)

Tiers are spaced widely (1 → 800) on purpose: with 1–2× spacing, many weak
matches out-sum one strong match.

**The lesson from Elza's most recent fix** (`addFullTextBoost`,
`ApCachedAccessPointRepositoryImpl`): wildcard `*value*` queries are
**constant-score**, carrying no field-length normalization, so a long child
record that merely contained the terms outranked the short parent whose name *was*
the query. The remedy was a BM25 `match()` clause on the analyzed field for the
whole query string, with its own much larger `boost-fulltext` weight.

**What not to copy.** The wildcard mechanics are the source of that problem.
CAM/Elza need substring matching because they are editor lookups; they pay for it
with lost ranking signal, later patched back. A public fulltext portal uses
analyzed matching with BM25 as the primary mechanism and gets length
normalization for free. We take the *tiering discipline* (including the 0.8×
diacritics rule — see the two exact tiers in §4.2) and the *config-driven
weights*, not the wildcards.

Also worth copying: Elza logs the resolved weight table and the top-100 hit
scores at trace level. Weights cannot be tuned without that.

## 3. Current state in ARON 2 (analysis)

- New-API fulltext is `multiMatch(["name","description"])` with **no weights**
  (`ElasticsearchSearchIndex.buildMainQuery`, `LuceneSearchIndex.buildMainQuery`).
- `SortMode { RELEVANCE, NAME }` exists in the contract; RELEVANCE is bare
  `_score`, NAME is `nameSort`. **Neither has a tie-break**, so paging is
  unstable whenever scores or names repeat.
- The contract declares `sort?: SortMode = SortMode.RELEVANCE` — the generated
  server model fills the default in, so the server **cannot detect an absent
  `sort`**; any "server decides when unset" rule needs a contract change (§4.4).
- The choice is hard-coded client-side (`SearchView.tsx`: `query ? Relevance :
  Name`), so every future client would have to reimplement it.
- `ItemType.indexBoost` is declared but **dead and mistyped** — `Boolean` in
  Java, documented as `indexBoost: 1.1` in types.yaml, read nowhere. Removed by
  this design (weights live in searchConfig.yaml, §4.3).
- The ES adapter **over-fetches deep pages**: it requests `from + size` full
  documents and skips in the app — page 999 pulls ~10 000 `_source`s over the
  wire. ES supports a native `from`; fixed with rollout step 1 (§7).
- **Verified defect.** Neither `trackTotalHits` nor `trackTotalHitsUpTo` is ever
  set; `RequestConverter` (spring-data-elasticsearch 5.5.9) emits
  `track_total_hits` only when one of them is non-null, so ES's default of 10 000
  applies. Above that, `getTotalHits()` returns exactly 10 000 with relation
  `GTE`, and the relation is discarded. Lucene's `searcher.count()` is exact.
  **The engines already disagree above 10 000 hits and the ES portal presents a
  capped number as if exact.** Fixed first (§4.6) — it is a defect, not a feature.

Reusable as-is: the `nameSort` collation key, the per-code `~L`/`~H` UNITDATE
bounds, and the `folding` analyzer (keyword + lowercase + asciifolding).

## 4. Design

### 4.1 Index side

All additions are computed in `ApuDocumentBuilder` so both engines index
identical bytes — the same trick already used for `czechSortKey`, and the reason
no analyzer divergence can creep into the new tiers. One shared normalizer
(lowercase, whitespace collapse, optional diacritics folding) is used by the
builder **and** by the query planner — exact tiers compare like with like by
construction.

| field | content | purpose |
|---|---|---|
| `allText` | **multi-valued** analyzed field: one entry per searchable value — the name, the description, every item value including APU_REF labels, integers as text, and UNITDATE boundary years | the single gate field (§4.2) and the baseline scoring tier |
| `nameExactCs` | name: lowercased, whitespace-collapsed, **diacritics preserved**; keyword, truncated to 200 chars | true exact-name tier |
| `nameExact` | same, **diacritics folded** | folded exact + prefix tiers |
| `dateL` / `dateH` | min of all `~L`, max of all `~H` (epoch millis, numeric doc-values) | dating sort with no configuration (§4.4) |
| `uuid` doc-values | sortable uuid (Lucene: `SortedDocValuesField`; ES: the existing keyword id) | the final tie-break every sort mode needs (§4.4) |

**Why `allText`.** Making every item searchable (R-3) without it would fan every
token out over ~200 fields; clause count is tokens × fields × tiers. With it the
*gate* is a single field — `allText` answers "does this token occur anywhere in
the APU" — and every other clause is scoring only. Per-field weights are
reserved for the fields a deployment actually cares about.

**Multi-valued, never concatenated.** Each source value is a separate entry, so
a position gap separates them and a quoted phrase can never match across two
different items ("…Praha" ending one item + "kostel…" starting the next must
not match `"Praha kostel"`). ES applies `position_increment_gap` (default 100)
to array values; the Lucene adapter's analyzer must override
`getPositionIncrementGap` to match.

**Analyzer:** `folding_and_tokenizing` (no stop filter), so a query consisting
only of stop words remains answerable (§4.2 fallback). `name` and `description`
keep their existing stop-filtered analyzer for the scoring tiers.

**UNITDATE in `allText`:** the from/to boundary years as plain text — without
them a strict AND breaks "kronika 1945". Only boundary years are searchable this
way: "1945" does **not** match an APU dated 1940–1950 (behavior B13, §5); range
queries remain the dating facet's job. The structured bounds stay separate for
filtering and sorting.

**ENUM values — verification item:** the builder currently indexes the raw ENUM
`value`. If deployments carry internal codes there rather than display labels,
`allText` becomes searchable by codes users never see; in that case the builder
must index the display label (the APU_REF path already does, via
`indexedLabel`). Verify against real data during rollout step 2.

**Exclusion and rebuild triggers.** An item is excluded from `allText` by
`fulltext: false` in **types.yaml**: it changes the document and therefore
requires a reindex. The indexed-fields CRC currently hashes only `code + type`
(`TypesLoader`), so the flag must be added to its input or the reindex will not
trigger. The fixed fields (`allText`, `nameExactCs`, `nameExact`,
`dateL`/`dateH`, uuid doc-values) are not covered by that CRC at all, so the
stored schema metadata gains a **document-layout version** on both engines — ES
has no equivalent of the Lucene commit-user-data version today.

### 4.2 Query language and plan

**User-visible syntax** — deliberately minimal, fully specified in §5:

- plain words — every word must occur (AND); word order does not affect
  matching, only ranking;
- `"…"` — exact phrase (never across item boundaries);
- `word*` — begins-with on a single word (trailing `*` only);
- everything else is literal: `*` in any other position, unbalanced quotes, and
  all other punctuation carry no operator meaning.

**Tokenization — one canonical analyzer.** The token list that drives the gate
and the `minimumShouldMatch` count comes from **one** canonical analyzer: the
stop-filtered one (`folding_and_tokenizing_stop`) — the most aggressive, so no
scoring field can drop a token the gate requires. If it yields no tokens but the
raw query is non-empty (a stop-word-only query such as "v"), the planner falls
back to the non-stop analyzer and the gate still runs against `allText`, which
keeps stop words (§4.1). Per-field scoring clauses use each field's own
analyzer. At most **32 tokens** are used; extra tokens are ignored (B12).

**Gate — strict AND, non-scoring.** One clause per token — a term match on
`allText` (prefix match for `word*` tokens, phrase match for quoted phrases) —
combined with `minimumShouldMatch` (default: all), executed in **filter
context**: ES `bool.filter`, Lucene `Occur.FILTER`. The gate contributes **no
score**; if it did, every document would receive an unweighted BM25 contribution
on top of the tiers and the weight table would not mean what it says. A document
matching only in `allText` is scored by the baseline tier — that is the tier's
job.

Rationale for AND over OR, and over a graded `minimum_should_match`: this portal
is facet-driven *and* offers user-selected ordering. Ranking is what makes loose
recall tolerable — weak matches sink out of sight. The moment a user picks
"Název A–Z" that protection is gone and partial matches interleave
alphabetically with real ones; facet counts have no ranking to hide behind
either. Strict AND keeps both meaningful.

**Relaxation on zero hits only.** If the strict query matches nothing, the
server retries once with the gate relaxed to `minimumShouldMatch: 1`. Facet
filters and the `apuType` restriction are **never** relaxed. The response
reports `queryMode: STRICT | RELAXED`, so the UI can render *"Pro dotaz „…"
nebyly nalezeny žádné výsledky. Zobrazujeme výsledky pro některá slova."* The
extra round trip costs nothing precisely because the first query matched
nothing.

`minimumShouldMatch` for the strict pass is configurable (§4.3), so a deployment
can choose e.g. 75% without a code change. Both engines express it natively (ES
`bool.minimum_should_match`, Lucene `BooleanQuery.setMinimumNumberShouldMatch` —
the latter takes an int the planner computes from the token count).

**Scoring tiers** (`should` clauses; weights multiply BM25 sub-scores):

| tier | clause | default weight |
|---|---|---|
| exact name, diacritics preserved | `term(nameExactCs, normCs(Q))` | 1000 |
| exact name, folded | `term(nameExact, norm(Q))` | 800 |
| name prefix | `prefix(nameExact, norm(Q))` | 200 |
| name phrase | `match_phrase(name, Q)` | 100 |
| name all terms | `match(name, Q, AND)` | 50 |
| reference labels | `match(<CODE>~LABEL, Q)` | 10 |
| description phrase | `match_phrase(description, Q)` | 8 |
| description terms | `match(description, Q)` | 2 |
| `allText` baseline | `match(allText, Q)` | 1 |
| promoted item type | `match(<CODE>, Q)` | as configured |

The two exact tiers implement Elza's 0.8× diacritics rule: "Rehor" finds
"Řehoř" (folded tier), but a user typing "Řehoř" sees the true match first
(both tiers fire). Because the weights multiply BM25 rather than constant
scores, the field-length normalization Elza had to add explicitly comes for
free: a short name that *is* the query outranks a long record that merely
contains it. Prefix on a normalized keyword replaces `*Q*` — same intent, no
leading wildcard, no lost ranking signal.

### 4.3 Configuration split

The split follows what a change costs, and mirrors the existing facet/types
division:

| concern | file | effect of a change |
|---|---|---|
| what the document contains (`indexed`, `fulltext`, `indexFolding`, `caseInsensitive`) | types.yaml | reindex (CRC-tracked) |
| how a match is weighted and ordered | searchConfig.yaml | restart, **no reindex** |

(searchConfig.yaml is read at startup; there is no runtime reload — the point of
the split is avoiding the reindex, not avoiding the restart.)

Every key optional; the defaults of §4.2 apply to anything unset, so a deployment
with no `relevance:` block still ranks sensibly. Config speaks **logical** names
(item-type codes as in the facet config), never physical index fields — the
server resolves an APU_REF code to its `~LABEL` field.

```yaml
relevance:
  # Minimum share of query tokens a document must match (default: all).
  minimumShouldMatch: 100%
  # Retry relaxed when the strict query yields no hits.
  relaxOnNoHits: true

  # Built-in fields (all optional, defaults shown).
  name:        { exactCs: 1000, exact: 800, prefix: 200, phrase: 100, terms: 50 }
  refLabels:   { phrase: 12, terms: 10 }
  description: { phrase: 8, terms: 2 }
  allText:     { terms: 1 }

  # Item types promoted above the allText baseline; unlisted types stay
  # searchable at the allText weight.
  items:
    - source: TITLE
      phrase: 60
      terms: 30
```

Startup validation: unknown item-type codes are reported, weights must be
positive numbers.

### 4.4 Ordering

```
enum SortMode { AUTO, RELEVANCE, NAME, NAME_DESC, DATE_ASC, DATE_DESC }
```

`AUTO` is the **schema default** — the current contract declares
`sort?: SortMode = RELEVANCE`, so an absent value is filled in by generated
models and the server can never see "unset"; an explicit `AUTO` member makes the
server-side rule visible in the contract instead of hiding it behind absence.
`NAME` keeps its meaning, so the change stays additive.

| mode | sort keys (in order) | missing values |
|---|---|---|
| `AUTO` | resolves to RELEVANCE when a query is present, NAME otherwise | — |
| `RELEVANCE` | score desc, `nameSort` asc, `uuid` asc | — |
| `NAME` | `nameSort` asc, `uuid` asc | name last |
| `NAME_DESC` | `nameSort` desc, `uuid` asc | name last |
| `DATE_ASC` | `dateL` asc, `nameSort` asc, `uuid` asc | undated last |
| `DATE_DESC` | `dateH` desc, `nameSort` asc, `uuid` asc | undated last |

- **Every mode ends in `uuid`** — that is what makes paging stable; a fonds with
  thousands of identically-dated, identically-named items must not shuffle
  between pages. (This is why uuid doc-values are part of the layout bump, §4.1.)
- **RELEVANCE tie-breaks on `nameSort`** because without a query every document
  scores equally and bare score order is index order — it looks random and
  shifts after a reindex. Elza does the same (score, then sortable preferred
  name).
- **Undated APUs sort last in both date directions.** The date fields are
  numeric, so missing-last is `SortField.setMissingValue(Long.MAX_VALUE)`
  ascending / `Long.MIN_VALUE` descending in Lucene and `missing: _last` in ES —
  an APU without dating should never lead the list.

Because the dating fields are derived, no per-section configuration is needed and
the modes are available everywhere; a deployment that genuinely needs a specific
item type can still override it. Nothing has to advertise which modes exist.

UI: a sort control beside the result count (Nejlépe odpovídající / Název A–Z /
Název Z–A / Datace vzestupně / Datace klesající), keyboard-operable with an ARIA
label, and the selection carried in the URL so results stay shareable.

### 4.5 Type counts, not type priors

The response carries `typeCounts: [{ apuType, count }]` — a terms aggregation on
the existing `type` keyword. Cardinality is a handful of values, so it is
effectively free in ES and trivial in the Lucene adapter's existing bucket
counter. It follows the same multi-select rule as the facets and **ignores the
`apuType` restriction itself**, so a user browsing funds still sees "Archivní
entity 8" and can switch.

This is why no per-`apuType` ranking prior is needed: the unanswerable question
*"does a fund outrank a description item?"* is replaced by letting the user
choose. The cross-section search keeps a single score-ordered mixed list as a
quick lookup, with type labels visible and one click to narrow to a single type.

### 4.6 Limits

Placed where cost actually is. Note that **alphabetical ordering is not a cost
centre** — sorting on the `nameSort` doc-values keyword is cheaper than scoring
and costs the same at 10 000 hits as at 10. Deep paging, exact counting, facet
cardinality and leading wildcards are.

| limit | default | why |
|---|---|---|
| `search.max-window` on `from + size` | 10 000 | ES's own window ceiling |
| `search.track-total-hits-up-to` | 10 000 | makes the existing ES cap explicit **and mirrors it in Lucene**, so the engines finally agree |
| `totalRelation: EQ \| GTE` in the response | — | lets the UI render *"více než 10 000 výsledků"* honestly instead of today's bare "10 000" |
| optional per-request override | server-capped | clients may ask for a higher or exact count; the server clamps |
| `search.max-facet-buckets` | 200 | hard cap over whatever searchConfig.yaml requests |
| max query tokens | 32 | bounds the tokens × fields × tiers fan-out; extras ignored (B12) |
| `search.request-timeout` | 5 s | ES `timeout`, Lucene `TimeLimitingCollector`; last-resort guard |

Alongside the window cap, rollout step 1 removes the ES adapter's over-fetch
(§3): deep pages use ES's native `from` instead of fetching `from + size` full
documents and skipping in the app. (Lucene's top-`from+size` collection is
inherent to top-k search and cheap — doc IDs, not stored fields.)

On the cost of exact totals: ES stops counting once the tracking limit is
reached, and a full exact count is a match iteration without scoring — typically
tens of milliseconds on a single-shard index of a few million documents. Lucene's
`searcher.count()` is O(1) for a plain term query (term `docFreq`) and a full
iteration for compound ones. **Exact totals are affordable at ARON's scale on
both engines**; the cap is a guardrail against pathological queries and a
consistency device, not a performance necessity — hence a clamped request
override rather than a refusal.

No `*Q*` ranking clauses anywhere; leading and inner wildcards are not operators
at all (§4.2). `searchAfter` cursor paging for deep access (harvest, export)
stays a separate later item.

### 4.7 Code placement

Following the `ApuDocumentBuilder` precedent — keep the risky logic pure and
engine-free so it can be unit-tested without an engine:

- `cz.aron.search.relevance.RelevanceConfig` — engine-neutral, loaded from
  searchConfig.yaml, validated at startup (§4.3).
- `cz.aron.search.relevance.RelevanceQueryPlanner` — **pure logic, no engine
  imports**: `(fulltext, apuType, config) → List<ScoredClause>` where
  `ScoredClause(field, MatchKind, value, weight, role)`,
  `MatchKind ∈ {TERM, PREFIX, PHRASE, ALL_TERMS, ANY_TERM}` and
  `role ∈ {GATE, SCORE}` — the role is what adapters map to filter vs. scoring
  context (§4.2). The planner owns tokenization (canonical analyzer + fallback),
  operator parsing (quotes, trailing `*`), the token cap, and weight resolution.
  Plain-JUnit testable, the `ApuDocumentBuilderTest` pattern.
- Each adapter translates `ScoredClause` mechanically into its own query type;
  `GATE` clauses go to ES `bool.filter` / Lucene `Occur.FILTER`.
- `ApuSearchQuery` gains only the extra `SortMode` values — the port's query
  model stays minimal and new-API-shaped, per the standing rule in
  `doc/search-port.md`.

### 4.8 Observability

`search.explain-logging` at trace level dumps the resolved weight table and the
planned clauses, plus the top-N hit scores — Elza's `logSearchConfig` and score
dump, which exist there for a reason. Without it the weights cannot be tuned
against real data.

## 5. Normative behavior specification

The rows below are the **contract of search behavior**: each is a guarantee
observable through the public API, phrased without engine terms, and pinned by a
test. A change to any row is a deliberate contract change — it must update this
table and its pinning test together (the `OldApiSurfaceTest` discipline, applied
to semantics instead of URLs). The Czech user help (rollout step 6) and the
administrator documentation (`doc/source/admin/search.rst`) are written *from*
this table so they cannot diverge from what is tested.

| # | Guarantee | Pinned by |
|---|---|---|
| B1 | Multiple words: every word must occur somewhere in the APU (AND); word order never affects *whether* something matches, only its rank | contract test |
| B2 | Matching is case-insensitive throughout | contract test |
| B3 | Diacritics-insensitive matching: "rehor" finds "Řehoř"; when diacritics are typed, the exact-diacritics name ranks above the folded match | contract test |
| B4 | `"…"` is an exact phrase; a phrase never matches across values of two different items; an unbalanced quote is treated as a literal character | planner unit + contract test |
| B5 | Stop words ("v", "a", "na") never cause empty results; a query consisting only of stop words still searches them | planner unit + contract test |
| B6 | `word*` means begins-with; `*` in any other position is literal; there are no other operators | planner unit + contract test |
| B7 | Zero strict hits → one automatic relaxed retry (any-word), reported as `queryMode: RELAXED`; facet filters and the section restriction are never relaxed | API test |
| B8 | Relevance order (ordering guarantee only, never absolute scores): exact name → name begins-with → phrase in name → all words in name → reference labels → description → anything else | contract test |
| B9 | Alphabetical order follows the Czech alphabet (č after c, ch after h); items without a name sort last | contract test |
| B10 | Date order uses the earliest (`ASC`) / latest (`DESC`) dating of the APU; undated APUs always sort last; paging is stable under every sort mode | contract test |
| B11 | Totals are exact up to the configured limit, above it reported as "more than N" (`totalRelation: GTE`) | API test |
| B12 | At most 32 query words are used; extra words are ignored | planner unit test |
| B13 | Only the boundary years of a dating are text-searchable ("1945" does not match an APU dated 1940–1950); searching inside date ranges is the dating facet's job | documented limitation, contract test |

"Contract test" = `SearchIndexContractTest` (both engines must pass identically);
"planner unit" = `RelevanceQueryPlannerTest` (no Spring, no engine);
"API test" = `NewApiV1Test` via the generated typed client.

**Relevance quality** (are the *right* documents on top for real queries) is not
pinnable by unit assertions. It is covered by a **golden-query set**: 15–20 real
queries against amadeus data with expected top hits, run under `-Pes-it` as a
report, not a build failure. Without it, every future weight tweak is tuned
blind against the last complaint.

## 6. Testing

- **Unit** (no Spring): `RelevanceQueryPlannerTest` — canonical tokenization and
  stop-word fallback, quoted-phrase and trailing-`*` parsing, literal treatment
  of stray operators, token cap, gate/score role assignment, weight resolution
  and config fallbacks, `minimumShouldMatch` computation.
- **`SearchIndexContractTest`** gains the ordering and matching invariants of §5
  (B1–B6, B8–B10, B13) — never absolute scores, which are engine-specific. Both
  engines run the identical assertions; that is the mechanism that keeps the
  behavior specification true on ES *and* embedded Lucene.
- **API level** (`NewApiV1Test`, generated typed client): `AUTO` resolution,
  `queryMode` reporting (B7), `totalRelation` (B11), `typeCounts` ignoring the
  `apuType` restriction.
- **Golden queries** under `-Pes-it` for relevance quality (§5).

## 7. Rollout

1. **Total-count fix + native `from`** — explicit tracking limit on both
   engines, `totalRelation` in the contract, UI "více než N", ES over-fetch
   removed. A defect fix; goes first and stands alone.
   **Done 2026-08-17**: `totalRelation` + `totalUpTo` in the contract;
   `search.track-total-hits-up-to` (default 10 000) /
   `search.track-total-hits-max` (override clamp, 100 000) /
   `search.max-window` (10 000, violations → 400); the cap is deterministic
   on both engines — above `totalUpTo` always `(totalUpTo, GTE)`, even when
   the engine happens to know the exact count (ES match-all shortcut).
2. **Index side** — multi-valued `allText` (position gap), `nameExactCs`,
   `nameExact`, `dateL`/`dateH`, uuid doc-values, `fulltext` flag in the CRC,
   document-layout version on both engines, ENUM label verification (§4.1). One
   reindex.
3. **Relevance** — config, planner (syntax, gate, tiers, relaxation), both
   adapters, contract tests, built-in defaults.
4. **Ordering** — `AUTO` + new modes in the contract, tie-breaks in both
   adapters, UI sort control.
5. **Type counts** — aggregation, contract, UI chips.
6. **Documentation** — Czech user help ("Nápověda k vyhledávání": quotes,
   diacritics, AND, `word*`, sort modes, "více než N") served through the
   existing help mechanism; administrator search chapter
   (`doc/source/admin/search.rst`) — both derived from §5.
7. **Tuning** on real Elza data with the score dump; golden-query set fixed.

The old API stays frozen throughout: `EsOldApiSearch` is the parity reference and
is not touched.

## 8. Decisions settled

First round (2026-08-17):

| # | Decision |
|---|---|
| R-1 | Weights are query-side configuration in **searchConfig.yaml**; index-side flags stay in types.yaml; dead `ItemType.indexBoost` removed |
| R-2 | **Strict AND** across analyzed tokens, quoted phrases mandatory, relaxation only on zero hits, `minimumShouldMatch` configurable |
| R-3 | **All items searchable** via `allText`, opt-out with `fulltext: false`; weights promote chosen fields above that baseline |
| R-4 | Years and integers **included** in `allText` |
| R-5 | No per-`apuType` ranking prior; **`typeCounts`** in the response instead, mixed list kept as a quick lookup with one-click narrowing |
| R-6 | Totals capped with `totalRelation` reported; clamped per-request override allowed |
| R-7 | Dating sort on **derived** `dateL`/`dateH`, no configuration required, undated last |

Second round (2026-08-17, after critical review):

| # | Decision |
|---|---|
| R-8 | Two exact-name tiers — diacritics-preserving above folded (Elza's 0.8× rule, §4.2); one shared normalizer between builder and planner |
| R-9 | The gate is **non-scoring** (filter context) and spans `allText` only; scores come exclusively from the weighted tiers; one canonical tokenizer (stop-filtered, with non-stop fallback) drives the gate and token counting |
| R-10 | `SortMode.AUTO` as the contract default (server-side resolution made explicit); every sort mode ends in a `uuid` tie-break; date sorts are numeric with missing-last |
| R-11 | Trailing `word*` is the only wildcard operator; `*` elsewhere and unbalanced quotes are literal; relaxation is reported via `queryMode` in the response |
| R-12 | The observable behavior is a **normative, tested specification** (§5); Czech user help and the admin docs derive from it; relevance quality guarded by a golden-query set under `-Pes-it` |

## 9. Deliberately not done

- **Per-`apuType` priors and a digital-object boost** — editorial ranking
  decisions; `typeCounts` (R-5) solves the actual problem.
- **Wildcard / substring ranking clauses** — the CAM/Elza mechanism, and the
  source of their constant-score problem (§2). Leading and inner wildcards are
  not supported at all.
- **Fuzzy matching / spell-correction** — not in scope; the zero-hit relaxation
  (R-2) covers the common typo case indirectly.
- **Result highlighting** ("why did this match" snippets) — a natural follow-up
  both engines support; deliberately deferred, not part of this design.
- **Hibernate Search** — rejected in `doc/search-port.md` §6; not reopened here.
- **`searchAfter` cursor paging** and **grouped per-type overview blocks** —
  separate later items, not needed for this design.
