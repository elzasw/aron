# Accessibility (přístupnost)

ARON is deployed by public-sector bodies (state and regional archives). Those
deployments are legally required to make the portal accessible and to publish an
accessibility statement about it. This chapter documents what the platform does
technically, what it does not do yet, how conformance is verified and kept, and
what remains the deployment's own responsibility — so a customer can write a
truthful statement instead of guessing.

**This document is evidence, not a conformance certificate.** It records the
current state and the method; the legally binding statement is published by the
deployment (see §7), and its wording should be reviewed by the customer's legal
department.

## 1. Legal frame

| What | Reference |
|---|---|
| Czech act on the accessibility of public-sector websites and mobile applications | zákon č. 99/2019 Sb. |
| EU directive it transposes | (EU) 2016/2102 |
| Technical standard | EN 301 549, whose web chapter references **WCAG 2.1 level AA** |
| Statement model | Commission Implementing Decision (EU) 2018/1523 |

Practical consequences for ARON:

- **Target conformance level: WCAG 2.1 AA.** Newer EN 301 549 revisions align
  with WCAG 2.2 AA; new work targets 2.2 where it costs nothing extra, so the
  platform does not have to be reworked when a deployment's obligations move.
- The deployment must **publish an accessibility statement** listing the parts
  that are not accessible, with reasons (§7 and the appendix).
- The statement must be **reviewed regularly** — treat "at least annually and
  after every significant UI change" as the internal rule.

## 2. Scope

| In scope (this repository) | Out of scope (deployment / other systems) |
|---|---|
| `aron-ui` — the new portal UI (`/api/v1` client) | Deployment-configured content: portal name, page template texts, news, help pages, logo |
| The SPA shell served by `IndexController` (language, deployment prefix) | Digitized archival objects (scans, PDFs) — see §6 |
| API-provided texts that reach the UI (facet labels, section labels) | The old portal UI (`aron.git`, frozen — bugfixes only; not brought to AA) |

The **old API and the old UI are not part of the conformance target**: they are
frozen, and a deployment that still runs the old portal must state that
separately.

## 3. What the platform does today

Architectural properties that carry most of the accessibility weight:

- **Fluent UI v9 as the component base** — its interactive components (buttons,
  checkboxes, selects, dropdowns, message bars) ship with keyboard operation,
  focus handling and ARIA roles, so the portal does not re-implement widgets.
  Custom widgets are built from native elements wherever possible: the dating
  slider is two native `range` inputs, so it is keyboard-operable by
  construction.
- **Document language is real and dynamic** — the shell carries `<html lang>`
  and `src/i18n/index.ts` updates `document.documentElement.lang` when the user
  switches language (pinned by `language.test.ts`). Language names inside the
  switcher carry their own `lang` attribute (WCAG 3.1.1, 3.1.2).
- **Landmark structure on every page** — `<header>`, labelled `<nav>` regions
  (main menu, breadcrumbs, pagination, type counts), `<main>`, `<footer>`.
- **Semantic relationships instead of visual-only cues** — the archival
  description tree uses `role="tree"`/`treeitem` with `aria-level`,
  `aria-expanded` and `aria-selected`; pagination marks the current page with
  `aria-current="page"`; the language switcher uses `aria-pressed`.
- **All search state lives in the URL** (query, filters, page, size, sort).
  Results are reachable by a shareable link, survive reload, and nothing depends
  on a timed interaction (WCAG 2.2.1) or on drag gestures (2.5.7).
- **No CAPTCHA, no session timeout, no auto-playing media** in the public
  portal, so the whole family of criteria around those does not arise.
- **Responsive layout** — below 860 px the search page stacks its sidebar above
  the results and the header wraps (WCAG 1.4.10 reflow; verification pending,
  see §4).
- **Errors are surfaced, never swallowed** — failed API requests appear in a
  visible error bar with the server's message, and a render crash produces a
  readable error page instead of a blank one. The error bar is a live region
  that stays mounted while empty, so a later failure is announced rather than
  only drawn.
- **The repeated header can be bypassed** — a skip link (first Tab stop) moves
  focus to the main region, which is also where focus lands after an in-app
  route change, so a screen reader reads the new page instead of staying in the
  old context (WCAG 2.4.1).
- **Real headings and named controls** — each page owns one `<h1>`, facets are
  `<h2>`-headed groups, and every input carries an accessible name independent
  of its placeholder (the dating fields and slider thumbs name their facet and
  which end of the range they set).
- **Asynchronous outcomes are announced** — filters and paging apply without a
  navigation, so the search page keeps one polite live region reporting the
  result count, the wait and any failure (WCAG 4.1.3).
- **Footer links are deployment configuration, not code** — the accessibility
  statement, privacy information or contacts a deployment publishes come typed
  from `/api/v1/ui/config` (see §6).

## 4. Known gaps and the plan

Honest current state. Each gap names the criterion it belongs to, so the
statement can cite it (or so it disappears from the statement once fixed).

### Phase A — structural fixes — **done**

| Fixed | Criterion |
|---|---|
| Skip link past the repeated header, focusing the main region | 2.4.1 Bypass Blocks (A) |
| One real `<h1>` per page, `<h2>` per facet and per record section (Fluent typography renders `<span>` unless `as` is given, which is what hid this) | 1.3.1 (A), 2.4.6 (AA) |
| Accessible names on every input — search boxes, facet text filters, type-ahead, year fields and both slider thumbs (which name their facet and range end) | 3.3.2 (A), 4.1.2 (A) |
| One polite live region on the search page reporting count, wait and failure; the API error bar stays mounted so later errors are announced | 4.1.3 Status Messages (AA) |
| Focus moves to the main region on an in-app route change | 2.4.3 (A, practice) |
| Focus ring restored on the slider thumbs (`:focus-visible`) | 2.4.7 (AA) |

The slider needs no `aria-valuetext`: its value *is* the year, which is what a
reader should hear.

### Phase B — permanent automated gate — **done**

Two checks run in the Maven `test` phase (so also on CI, and skipped together by
`-DskipTests`):

- **ESLint with `eslint-plugin-jsx-a11y`** (`aron-ui/eslint.config.js`, the
  module had no linting at all before): a missing label, a role misuse or a
  handler on a non-interactive element fails the build. Warnings fail too
  (`--max-warnings=0`), so a suppression has to be written down with its reason
  instead of accumulating silently. Formatting is deliberately not linted — it
  is not what breaks a screen reader.
- **axe-core over rendered markup** in the vitest suite via
  `expectNoA11yViolations` (`src/test/a11y.ts`), asserted for the application
  frame and for every facet kind. jsdom has no layout engine, so colour contrast
  is explicitly disabled here and belongs to Phase C; what this does catch —
  structure, names, relationships, duplicate ids — is where regressions actually
  appear.

Setting the gate up also surfaced three React state-sync smells the rules flag
as cascading renders (`react-hooks/set-state-in-effect`). They were fixed by
adjusting state during render instead of in an effect, which is the documented
React pattern and removed one redundant render pass from the search box, the
facet filters and the description tree.

### Phase C — browser-verified checks + manual pass (~1 day + audit)

- What jsdom cannot check runs against the real app in dev mode (H2 + embedded
  Lucene + seed data, no external services): **contrast (1.4.3), reflow at
  320 px / 400 % zoom (1.4.10), focus visibility (2.4.7)** — via Lighthouse or
  Playwright + axe.
- A **manual keyboard pass** (tab order, no traps, everything reachable) and a
  **screen-reader pass with NVDA** (the reference reader for Czech public
  administration) on the core journeys: search → filter → open a record →
  browse the tree. Record the result and the date in §5's log; that record is
  what the statement's "assessment method" field refers to.

### Phase D — the deployment's statement needs a home — **done**

The statement must be reachable from the portal, and a deployment publishes more
than one such page (privacy information, operator contacts). Rather than a
single hardcoded field, the UI configuration carries a **list of footer links**
(`pageTemplate.yaml` → `footer.links` → `/api/v1/ui/config`):

```yaml
footer:
  links:
    - code: ACCESSIBILITY                       # well-known role, UI labels it
      url: https://archiv.example/pristupnost
    - url: https://archiv.example/kontakt       # free link, own label
      label:
        cs: Kontakt
        en: Contact
```

A link is either a **well-known role** (`ACCESSIBILITY`, `PRIVACY`, `TERMS`,
`CONTACT`) that the UI labels in the reader's language, or a free link with its
own label — one string, or a per-language mapping the server resolves like every
other display text. A link with neither fails the startup, the way every other
configuration error here does.

Two consequences worth stating:

- ARON is **not always a standalone portal**. When it is embedded in a
  customer's own web presentation that renders its own footer, the deployment
  configures no links and the obligations belong to the host page — an empty
  list is a valid, deliberate configuration.
- The old portal's footer was raw HTML in the page template
  (`homepage.footerCenter`). Typed links replace it: the client never receives
  markup from configuration, so a deployment cannot inject scripts into the
  portal through it.

## 5. How conformance is maintained

Accessibility decays silently, so it is wired into the normal workflow rather
than audited once:

1. **Definition of done for every UI change** (CLAUDE.md, aron-ui invariants):
   the component is keyboard-operable, has an accessible name, uses a real
   heading/landmark where it structures content, and announces asynchronous
   state changes.
2. **Automated gate in the build** — `npm run lint` (ESLint + jsx-a11y) and the
   axe assertions in the vitest suite run in the `test` phase of the `aron-ui`
   module, i.e. in `mvn install` and on CI. A change that regresses structure
   does not build. New page-level tests should call `expectNoA11yViolations`.
3. **Periodic manual audit** — the Phase C keyboard + NVDA pass repeated at
   least annually and before a release that reshapes the UI. Results and dates
   go into the log below.
4. **Statement refresh** — after each audit the deployment updates its published
   statement (the act requires it to stay current).
5. **This chapter is part of the change** — when a gap in §4 is closed or a new
   one appears, it is edited in the same commit, like every other invariant in
   this repository.

### Audit log

| Date | Scope | Method | Result |
|---|---|---|---|
| 2026-08-19 | initial review of the search and record pages | source review (this chapter) | gaps recorded in §4 |
| 2026-08-19 | Phase A + D | implementation + component tests (skip link, footer links, facet naming) | §4 Phase A and D closed; B and C open |
| 2026-08-19 | Phase B | ESLint + jsx-a11y over the whole UI, axe over the frame and every facet kind | no findings left; the gate now runs in every build |

## 6. Deployment responsibilities

Even a fully conformant platform can be deployed inaccessibly. A customer's
statement must cover these, because the platform cannot decide them:

- **Publishing the statement** — configure it as a footer link with the
  `ACCESSIBILITY` code (§4, Phase D). A deployment embedded in a customer portal
  publishes it on the host page instead.
- **Configured texts and documents** — page template texts, news, help pages and
  any linked PDF are the deployment's content; a non-accessible PDF stays
  non-accessible.
- **Logo** — supplied per deployment. The portal marks it decorative and keeps
  the portal name as a screen-reader heading, so a missing description does not
  break the page; a logo that carries information beyond the name needs that
  information in the configured portal name.
- **Section accent colours** — configurable per deployment (`menu` colours).
  They are used as a 4 px underline while the active item is also marked by its
  background, so they are not the sole indicator, but a deployment picking
  extreme values should check contrast.
- **Digitized archival objects** — scans of archival material are images of
  text. They cannot be made fully accessible without transcription, and the
  directive's exemption for **reproductions of items in heritage collections**
  (Art. 1(4)(f) of (EU) 2016/2102) exists precisely for this case. The statement
  should name this exemption explicitly, and, where transcriptions exist, say
  that they are offered.
- **Disproportionate burden** — if a deployment claims it for some part, the
  claim and its justification belong in the statement, not here.

## 7. Appendix: accessibility statement template (Czech)

Text for the deployment to publish (and link via `accessibilityUrl`, Phase D).
Placeholders in `«…»`; the enforcement/complaint route must be verified against
the current wording of zákon č. 99/2019 Sb. before publication.

```markdown
# Prohlášení o přístupnosti

«Název organizace» se zavazuje k zpřístupnění internetových stránek
«adresa portálu» v souladu se zákonem č. 99/2019 Sb., o přístupnosti
internetových stránek a mobilních aplikací.

## Stav souladu s požadavky

Tyto internetové stránky jsou částečně v souladu s normou EN 301 549
(WCAG 2.1 úroveň AA) z důvodu níže uvedených nedostatků.

## Nepřístupný obsah

- **Digitalizované archiválie** (naskenované dokumenty a jejich náhledy)
  nejsou plně přístupné. Jde o reprodukce předmětů z fondů kulturního
  dědictví, které nelze plně zpřístupnit bez pořízení přepisu; tento obsah
  je z působnosti zákona vyňat. Tam, kde je k dispozici přepis, je
  nabízen společně s obrazem.
- «Další zjištěné nedostatky — převezměte z technické dokumentace ARON,
  kapitola Accessibility, §4.»
- «Dokumenty PDF publikované organizací, pokud nejsou přístupné.»

## Vypracování prohlášení

Toto prohlášení bylo vypracováno dne «datum». Metoda: «vlastní posouzení /
posouzení třetí stranou», provedené kombinací automatizovaných kontrol
(axe, ESLint jsx-a11y), ověření v prohlížeči a manuální kontroly klávesnicí
a odečítačem obrazovky NVDA.

Prohlášení bylo naposledy revidováno dne «datum».

## Zpětná vazba a kontaktní údaje

Podněty k přístupnosti tohoto portálu zasílejte na «e-mail / adresa».
Odpovídáme do «lhůta» pracovních dnů.

## Postup pro vymáhání práva

V případě neuspokojivé odpovědi na podnět se můžete obrátit na
«příslušný orgán dozoru podle zákona č. 99/2019 Sb. — ověřte aktuální
znění a kontaktní údaje».
```
