# Structured search results

Status: **implemented 2026-08-19** (PLAN.md Phase 9, decision D-13). Written as
a proposal, kept as the design record; §6 holds the answers that settled it.

The old portal can present a search hit as a **pre-rendered, multi-row record
card** instead of the plain name + description line. The rows are produced by
the source system (Transfagent), travel in the APUX `<result>` element and are
stored verbatim; the portal only lays them out. The old UI switches the whole
list to that presentation with the deployment flag `useStructuredResults: true`.

This document is how the feature landed in the new UI (`aron-ui`) and the new
API (`/api/v1`), and why it needs **no** on/off configuration there.

## 1. Goals

1. New-UI parity: a deployment whose data carries structured results shows them.
2. **No feature flag.** A record that has a structured presentation is shown with
   it; a record that has none keeps the plain card. Availability is data, not
   configuration (§4.1).
3. Keep the presentation *vocabulary* (which row is the heading, prefixes,
   icons, colors) where it belongs — in deployment configuration, translated by
   the sibling-file rule — instead of hard-coding one customer's codes.
4. Accessible by construction (§4.7). The old implementation nests links inside
   a link; that must not be reproduced.
5. Do not touch the frozen old API and do not migrate the stored blobs.

## 2. What the feature actually is (analysis of the old implementation)

### 2.1 Data shape

Producer: Transfagent, e.g.
`cz.aron.customizations.soaplzen.ImportArchDescResultBuilderImpl` (Plzeň /
Porta fontium). It builds a `Result` and writes it as **JSON into the APUX
`<result>` element** (`aron_apux.xsd`: `result` = optional `LongString`,
"Strukturovany vysledek vyhledavani APU"). The wire model
(`transfagent/.../openapi/result-api.yaml`) is deliberately abbreviated:

| Field | Meaning |
|---|---|
| `id` | APU uuid (redundant with the hit's own uuid) |
| `t` | record-shape code — an archival level in practice: `A`, `A_R`, `A_S`, `A_F`, `A_I`, `A_IM` (matrika), `A_IB` (kniha), `A_D` (výkres/mapa), `A_IP` |
| `tn` | thumbnail: **either** an absolute URL (`https://…/x.jp2`) **or** a bare deployment image name (`pf.svg`) |
| `tn_link` | where the thumbnail leads; unset = the record's detail page |
| `l` | rows, in display order: `l[row][field]` |
| `l[r][f].t` | field code: `N` name, `D` dating, `CONT` content, `J_S` signatures, `J_RO` / `J_IC` / `J_UC` numbers, `J_I` institution, `J_F` fund, `J_C` parent context, `J_G` geo entities |
| `l[r][f].v[]` | the field's values; each value has its text and an optional uuid — with the uuid the value is a link to that APU's detail |

Note what these codes are **not**: they are not `types.yaml` item-type codes and
not `ApuType` values. They are a presentational vocabulary invented per
deployment / source system, so nothing in the portal can label them on its own.

### 2.2 How the old UI renders it

`aron-web/src/modules/evidence/evidence-results/evidence-results.tsx`:

- Each hit is one card: optional type icon, then one row element per row with
  `display:flex; flex-wrap:wrap`, then the thumbnail on the right.
- Within a row, fields are joined by `itemSeparator` (default `" | "`); within a
  field, values by the field's own `separator` (default a space). A value with an
  uuid becomes a link to the APU detail.
- Styling comes from a **deployment file fetched from `/app/config.json`** (an
  nginx-served asset, not the API): `typeIcon`, `typeIcons[{type, icon}]`,
  `typeIconSize`, `itemSeparator`, and
  `itemProperties[{type, class: header|text, color, size, bold, prefix, icon, separator}]`.
  Icons resolve against `/app/`, as do relative `tn` values.
- **It is not an HTML table** — it is a list of records, each with its own rows
  and its own field set. Column alignment across records does not happen (see
  the open question in §6).

The list is served by `POST /apu/listresults` (old API), which is `/apu/list`
plus a DB lookup of the stored blob per hit; hits without a blob get a
placeholder row reading "Prazdny vysledek".

## 3. Starting point in ARON 2 (before this change)

The **backend half already existed** and was wired end to end:

- `aron_apux.xsd` carries `result`; `ApuProcessor.parseResult` parses the JSON
  into `cz.aron.api.rest.model.StructuredResult` and stores it Kryo-encoded in
  `apu.result` (`ApuEntity#result`, `${blob}`, nullable) —
  `StructuredResultSerializer`, classes registered in `KryoSerializer`.
- `ApuEntityRepository.findAllResultsByUuidIn` → `ApuService.findAllResultsByUuidIn`
  (batched by 1000, one pooled `Kryo`), keyed by uuid string; `apu.uuid` is
  indexed (`idx_apu_uuid`).
- Old-API `ApuApi.listResults` serves `/apu/listresults` with the same
  placeholder behavior as before; pinned by `OldApiSurfaceTest` and
  `OldApiSearchTest`.

Missing: anything in `/api/v1`, anything in `aron-ui`, the layout configuration,
and image serving. No test fixture carried a `<result>` element, so the parse →
store → serve path was only exercised with `null`.

### 3.1 Two defects found while reading this path

1. **`tn_link` is mis-mapped.** The wire format (and the old backend, and the old
   UI) uses `tn_link`; `aron-core/src/main/resources/openapi/aron-api.yaml`
   declares the property as `tnLink`, so the generated model binds and emits
   `tnLink`. Consequence: `ApuProcessor` **drops the thumbnail link on import**,
   and `/apu/listresults` emits a name the old UI does not read. Fix: rename the
   property back to `tn_link` in the frozen spec — a bugfix restoring the
   documented wire format, which the freeze allows (see the open question in §6).
   Needs a fixture test.
2. **The stored blob is a Kryo dump of a generated class.** Regenerating the old
   API with a different field order silently breaks every stored blob. This is
   pre-existing and out of scope here, but the new code must not add a second
   reader of that shape — §4.5 maps it once, at read time.

## 4. Design

### 4.1 Availability without configuration

The rule is per record:

> A search hit that has a stored structured result is rendered as a structured
> card. A hit without one keeps the plain name + description card.

No flag, because the flag carries no information the data does not: the old
`useStructuredResults` existed only because the old UI chose between **two
endpoints** (`/apu/listview` vs. `/apu/listresults`) before seeing any data. The
new API returns the structured result *inside the hit*, so the choice moves to
where the answer already is.

Cost: `search` gains one DB lookup per page — the stored blob for at most `size`
(≤ 100) uuids on an indexed column, i.e. exactly what `/apu/listresults` already
does today. When no record has a result the lookup returns all-null and the
response is unchanged.

One escape hatch, for a deployment that has the data but prefers plain cards:
`search.structured-results: AUTO` (default) | `OFF`. `OFF` skips the lookup
entirely. There is deliberately no `ALWAYS` — it could only mean "invent a
placeholder", which is what the old API's "Prazdny vysledek" does and what we
are not repeating.

Mixed lists are possible in principle. In practice a source system produces
results for whole APU types (the Plzeň customization does it for ARCH_DESC,
FUND, FINDING_AID and ENTITY), so lists are homogeneous; the per-record fallback
is a safety net for a partially reimported corpus, not a design goal.

**Rejected alternative:** index a `hasStructuredResult` flag next to
`containsDigitalObjects` in `ApuDocument`. It is the right *kind* of field, and
`ApuDocumentBuilder` already receives the `ApuEntity`, but it buys nothing here
(presence is known from the lookup we do anyway) and costs a search-schema
fingerprint bump plus a full reindex on upgrade. It becomes attractive only if
"only records with a structured presentation" ever has to be a *filter*.

### 4.2 Contract (`api/main.tsp`)

Readable names — the old `t` / `l` / `v` / `tn` abbreviations were payload golf
and must not leak into the new contract. Added to the existing hit model:

```tsp
model ApuSearchItem {
  uuid: string;
  name: string;
  description?: string;
  apuType: ApuType;
  containsDigitalObjects: boolean;

  /**
   * Structured presentation delivered by the source system, when this record
   * has one. Present per record: clients that get no `structured` render the
   * name and description as usual. Field and record codes are a deployment
   * vocabulary - `getResultLayout` says how to lay them out.
   */
  structured?: StructuredResult;
}

/** Pre-rendered structured presentation of one record. */
model StructuredResult {
  /** Record-shape code (an archival level in practice); keys the layout's icon. */
  code: string;

  /** Rows in display order. */
  rows: ResultRow[];

  /**
   * Thumbnail image, ready to use: an external absolute URL when the source
   * delivered one, otherwise the deployment image resolved against this
   * deployment's own prefix.
   */
  thumbnailUrl?: string;

  /** Where the thumbnail leads; unset = this record's detail page. */
  thumbnailLinkUrl?: string;
}

/** One row: the fields shown on one line. */
model ResultRow {
  fields: ResultField[];
}

/** One field of a row. */
model ResultField {
  /** Field code; keys the layout's styling, prefix and label. */
  code: string;

  values: ResultValue[];
}

/** One value; with `refUuid` it is a link to another record. */
model ResultValue {
  text: string;

  /** Referenced APU - the value renders as a link to its detail. */
  refUuid?: string;
}
```

`id` from the stored blob is dropped: the hit already carries `uuid`, and a blob
whose `id` disagrees with its row is a data defect, not a second identity.

`ResultValue` stays a single model with an optional `refUuid` rather than a
discriminated union: there is one kind of value, optionally linked. The
`@discriminator` pattern earns its keep when the *shape* differs per kind
(`DetailItem`, `SearchFilter`) and would be ceremony here.

Layout, a separate cached GET next to `getFacets`:

```tsp
namespace Search {
  /**
   * Deployment layout of structured search results: how the field and record
   * codes of `StructuredResult` are presented. Language-dependent (prefixes
   * and labels are deployment text); empty when the deployment configures
   * none, in which case clients apply their own defaults.
   */
  @get
  @route("/api/v1/search/result-layout")
  op getResultLayout(...LangParam): ResultLayout;
}

model ResultLayout {
  /** Styling per field code; a code not listed renders as plain text. */
  fields: ResultFieldStyle[];

  /** Icon per `StructuredResult.code`. */
  icons: ResultIcon[];

  /** Separator between fields of one row; unset = the client default (" | "). */
  fieldSeparator?: string;
}

model ResultFieldStyle {
  code: string;

  /**
   * This field is the record's heading - its text becomes the link to the
   * record's detail. At most one field per record should be marked; without
   * any, clients fall back to the record's `name`.
   */
  heading?: boolean;

  /** Localized text shown before the values ("sign.: "). */
  prefix?: string;

  /**
   * Localized accessible name of the field, for readers who get no visual
   * context. Defaults to `prefix` when that is set.
   */
  label?: string;

  /** Separator between the field's own values; unset = a space. */
  valueSeparator?: string;

  /** CSS color; unset = the client's body color. */
  color?: string;

  /** Font-size multiplier (1 = base text). */
  scale?: float32;

  bold?: boolean;

  /** Icon shown before the field, ready to use (see `ResultIcon.url`). */
  iconUrl?: string;
}

model ResultIcon {
  /** The `StructuredResult.code` this icon belongs to. */
  code: string;

  /** Ready-to-use URL of the deployment image. */
  url: string;

  /** Rendered width in CSS pixels; unset = the client default. */
  size?: int32;
}
```

Alternative considered: fold the layout into `/api/v1/ui/config`. Rejected —
`UiConfig` is portal chrome (name, menu, footer) loaded on every page, while
this is search presentation needed only on search pages, is per-language in the
same way `getFacets` is, and may later want an `apuType` parameter. Keeping it
in `Search` also keeps the two documents' cache lifetimes independent.

### 4.3 Layout configuration

New `webResources` keys, sibling-localized per the standing rule:

```yaml
webResources:
  resultLayout: ./config/resultLayout.yaml     # optional; absent = client defaults
  images: ./config/images/results/       # optional; icons + thumbnail fallbacks
```

```yaml
# resultLayout.yaml - source language
fieldSeparator: " | "
icons:
  - code: A_IM
    image: matrika.svg
    size: 28
  - code: A_IB
    image: kniha.svg
fields:
  - code: N
    heading: true
    scale: 1.15
    bold: true
  - code: J_S
    prefix: "sign.: "
    valueSeparator: ", "
  - code: J_G
    label: "Místa"
    color: "#5f5bc2"
```

```yaml
# resultLayout_localization.yaml
en:
  J_S:
    prefix: "ref.: "
  J_G:
    label: "Places"
```

`cz.aron.commons.LocalizationFile.besides(...)` already derives and reads the
sibling file; the configured file stays the source language and the fallback, so
adding a language never edits it. An unknown key in `resultLayout.yaml` fails
the startup (configuration errors must surface), mirroring `UiConfigLoader`.

`class: header|text` and the old `size` scale (`1 + (size-1)*0.1` em) are not
carried over: `heading: true` says what it means, and `scale` is a plain
multiplier. Old `/app/config.json` files are therefore *converted*, not copied —
a one-off, documented in the admin docs.

### 4.4 Images

`GET /api/v1/ui/images/{name}` serves one file from
`webResources.images`, content type from the extension — the same shape as
`UiController.uiGetLogo`, plus:

- `name` must match `[A-Za-z0-9._-]{1,128}` and the resolved path must stay
  inside the configured directory (`normalize()` + `startsWith`); anything else
  is rejected before any read. Path traversal is the whole risk surface here.
- unsupported extension → 404, not 500: the file set is deployment data.
- `Cache-Control: public, max-age=…` and a small in-memory cache; these are
  branding assets, requested once per card type per page.

**URLs are resolved server-side.** `tn` is either an external absolute URL
(passed through) or a bare image name; the layout's `image` is always a name.
The server turns a name into `<contextPath>/api/v1/ui/images/<name>`
using the request's own context path, which already folds `X-Forwarded-Prefix`
(`forward-headers-strategy: framework`). Clients therefore get one
always-usable `thumbnailUrl` / `url` and need no resolution rule, and the
one-jar rule holds: nothing absolute is baked into a build.

### 4.5 Server implementation

- `cz.aron.web.v1.ResultLayoutLoader` — parses `resultLayout.yaml` + its sibling
  once at startup, renders `ResultLayout` per locale (image URLs resolved per
  request). Modelled on `UiConfigLoader`.
- `cz.aron.web.v1.StructuredResultMapper` — the **single** place that reads the
  stored `cz.aron.api.rest.model.StructuredResult` and produces the `/api/v1`
  model. Nothing else in the new API touches the old model; the blob format is
  unchanged, so there is no migration.
- `SearchController.searchSearch` — after building `items`, when
  `search.structured-results` is `AUTO`, one call to
  `ApuService.findAllResultsByUuidIn(pageUuids)` and one `setStructured` per hit
  whose blob is non-null. Absent blob → field absent. No placeholder.
- `SearchController` gains `getResultLayout`, delegating to the loader with
  `presentationLocales.resolve(lang)`. Its body varies by language, so **if** it
  ever gets an ETag, the language goes into the variant
  (`HttpUtils.computeExpired(..., variant)`).

The old API keeps its current behavior verbatim, placeholder included —
`OldApiSurfaceTest` and `OldApiSearchTest` must stay green unchanged; the
`tn_link` fix of §3.1 is the one deliberate contract change and needs its own
assertion.

### 4.6 UI implementation (`aron-ui`)

- `src/search/ResultList.tsx` decides per item: `item.structured` present →
  `StructuredResultCard`, otherwise today's card. The list becomes a real
  `<ul>` / `<li>`.
- new `src/search/StructuredResultCard.tsx`:
  - the record icon from `layout.icons` when the record's `code` has one, on a
    tile in the portal header's colour (`PRIMARY_DARK`) spanning the card's
    height and ending halfway across the icon, so the icon's right half lies on
    the card - the old portal's results list, whose icon background is its own
    primary colour,
  - one element per row; fields of a row joined by
    `layout.fieldSeparator ?? " | "`, values of a field by
    `style.valueSeparator ?? " "`,
  - a value with `refUuid` → a link to `/apu/{refUuid}`,
  - the heading field's text → a link to `/apu/{uuid}`; no heading field → the
    record's `name` as the heading link above the rows, so every card is
    reachable,
  - thumbnail last, `max-height` capped, wrapping below the text on narrow
    viewports.
- `src/search/useResultLayout.ts` — `useQuery(["result-layout", lang], …, { staleTime: Infinity })`.
  **`lang` is part of the key** (prefixes and labels are server-rendered text).
- New i18n keys in both bundles (Czech source): `search.thumbnailAlt`,
  `search.thumbnailLink` (accessible name of an off-site thumbnail link). No
  fallback-heading key is needed — the record's own name is used.

Defaults when no layout is configured: no icons, no prefixes, `" | "` between
fields, a space between values, the **first** field of the **first** row as the
heading. That renders real data legibly with no `resultLayout.yaml` at all,
which is what makes §4.1's "no configuration" claim true rather than nominal.

### 4.7 Accessibility

Non-negotiable per `doc/accessibility.md`; the old implementation fails the first
two, so they are called out explicitly.

1. **No nested interactive elements.** The old card is one big link with links
   inside it — invalid HTML, and a keyboard user cannot reach the inner targets
   predictably. The new card is not a link: the heading is the link, referenced
   values are their own links, the thumbnail link is its own.
2. **Every card has an accessible name**: the heading link's text (heading field
   or the record's `name`). A card must never be a wall of unlabeled values with
   no reachable detail link.
3. **Fields are labeled for readers who cannot see the layout.** A field code is
   opaque, so `ResultFieldStyle.label` (defaulting to `prefix`) is rendered as a
   visually-hidden prefix when there is no visible one. Without this a screen
   reader reads a run of bare values.
4. **Thumbnails**: `alt=""` when the thumbnail leads to the record's own detail —
   the heading link already names it, so the image is redundant rather than
   informative. When `thumbnailLinkUrl` points elsewhere, that link carries an
   accessible name from `search.thumbnailLink` with the record's name
   interpolated.
5. **Heading semantics**: the heading link sits in an `<h3>` inside the `<li>`,
   consistent with the page's `<h1>` (section title). One heading per card.
6. **Responsive**: rows wrap (`flex-wrap`), the thumbnail moves below the text
   under the existing 860 px breakpoint, nothing scrolls horizontally.
7. Colors from `resultLayout.yaml` are deployment-chosen and can fail contrast.
   The UI applies them to text on its own background only, never as a
   background, and the admin docs state the AA obligation. Contrast checking
   itself is Phase C of `doc/accessibility.md`.
8. `doc/accessibility.md` §4 is updated in the same commit; the new page test
   calls `expectNoA11yViolations`.

### 4.8 Localization

`prefix` and `label` are deployment text → sibling file (§4.3). The **values**
are content: they arrive already rendered from the source system and are never
translated by the portal — the same rule as APU names, descriptions and ENUM
item values. Field codes are not translated either; they are keys.

## 5. Testing

- `StructuredResultMapperTest` — plain unit test, no Spring: old model → new
  model, including a value with and without `refUuid`, empty `rows`, and an
  external vs. a named thumbnail.
- `ResultLayoutLoaderTest` — parsing, sibling-file overlay, locale fallback,
  unknown key fails the load.
- A fixture with a real `<result>` JSON under
  `src/test/resources/test-config/import`, so the import → store → search round
  trip is finally covered with non-null data (today only `null` is covered).
  Additive, unique ids.
- Extend `NewApiV1Test` (generated typed client): a hit with `structured` and a
  hit without it in the same response, plus `getResultLayout` in two languages.
- `OldApiSearchTest` — add the `tn_link` assertion of §3.1.
- `aron-ui`: `StructuredResultCard.test.tsx` — heading link target and
  accessible name, a referenced value is its own link, the fallback when no
  field is marked `heading`, `expectNoA11yViolations`; plus the
  language-in-query-key behavior of `useResultLayout`.

## 6. Answers that settled the design (2026-08-19)

1. **"As a table" means the card, not an HTML table.** The word described the
   look of the old portal's per-record cards approximately, not a column-aligned
   grid; the card is what shipped. A genuine table across records is still not
   possible from this data (each record carries its own field set) and remains
   out of scope.
2. **No converter for the old `/app/config.json`.** Deployments are reconfigured
   by hand, so `resultLayout.yaml` keeps the clearer key names. The mapping is
   documented key-by-key in the administration guide (configuration chapter,
   "Coming from the old portal"), derived from a near-production MZA
   configuration.
3. **`search.structured-results: OFF` stays.**
4. **The `tn_link` fix is a bugfix** and was applied to the frozen spec; the
   Java field name is unchanged (`tnLink`), so the persisted Kryo blobs are
   unaffected — only the JSON name is restored.

## 7. What shipped

Backend

- `aron-api.yaml`: `tnLink` → `tn_link` (§3.1); pinned by
  `OldApiSearchTest.listResultsServesTheStoredBlobInTheWireFormat`, which also
  asserts `tnLink` is *absent*.
- `main.tsp`: `ApuSearchItem.structured`, `StructuredResult`/`ResultRow`/
  `ResultField`/`ResultValue`, `ResultLayout`/`ResultFieldStyle`/`ResultIcon`,
  `Search.getResultLayout`, `Ui.getResultImage`; regenerated
  `api/openapi/aron-openapi-v1.yaml` committed with it.
- `cz.aron.web.v1`: `StructuredResultMapper` (stored → contract, the only
  reader), `ResultLayoutLoader` (`resultLayout.yaml` + sibling, strict keys),
  `DeploymentImages` (name validation + URL building), `SearchController`
  (`attachStructuredResults`, `searchGetResultLayout`), `UiController`
  (`uiGetResultImage`).
- `webResources.resultLayout` / `webResources.images` and
  `search.structured-results` in the configuration template and the admin docs.

UI

- `useResultLayout` (language in the query key, `staleTime: Infinity`),
  `StructuredResultCard`, `ResultList` branching per record and rendering a real
  `<ul>`/`<li>` with an `<h3>` per card for **both** card kinds,
  `search.thumbnailLink` in both bundles.

Tests

- `StructuredResultMapperTest`, `ResultLayoutLoaderTest`, `DeploymentImagesTest`
  (plain unit tests); `NewApiV1Test` — a mixed response, the layout in two
  languages, the image endpoint including the traversal cases; a real `<result>`
  in the `detail-transfer` fixture so import → store → search is finally covered
  with non-null data; `StructuredResultCard.test.tsx` with the a11y gate.

Docs

- `doc/accessibility.md` §4 (what the card fixes) and §6 (what the deployment
  owes), admin-docs configuration chapter incl. the old-config key mapping,
  PLAN.md Phase 9 + D-13, CLAUDE.md invariants.

## 8. Deliberately not done

- No placeholder card for a record without a result (the old API's "Prazdny
  vysledek"): absence is absence.
- No `useStructuredResults`-style flag (§4.1).
- No indexed `hasStructuredResult` field (§4.1, rejected alternative).
- No migration of the stored Kryo blobs, and no second reader of the old model
  beyond `StructuredResultMapper`.
- The blob-format fragility of §3.1(2) is recorded, not fixed here.
