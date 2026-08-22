import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import {
  type ApuSearchRequest,
  type ApuSearchResponse,
  ApuType,
  type FacetDef,
  type DatingFacetResult,
  type EnumFacetResult,
  FacetDisplay,
  FacetResultKind,
  FacetType,
  FilterKind,
  QueryMode,
  type RefFacetResult,
  TotalRelation,
} from "../api/generated";
import i18n, { DEFAULT_LANGUAGE } from "../i18n";
import { expectNoA11yViolations } from "../test/a11y";
import SearchView from "./SearchView";
import { DATE_FACET, RELATED_FACET, TYPE_FACET } from "./filters";

/** What the server answers for a general search: the built-in facets, no section ones. */
const BUILT_IN_FACETS = [
  { code: TYPE_FACET, type: FacetType.Enum, label: "Record type", display: FacetDisplay.Always },
  { code: DATE_FACET, type: FacetType.Unitdate, label: "Dating", display: FacetDisplay.Always },
  { code: RELATED_FACET, type: FacetType.Ref, label: "Related to", display: FacetDisplay.Always },
];

const RESPONSE: ApuSearchResponse = {
  total: 4,
  totalRelation: TotalRelation.Eq,
  queryMode: QueryMode.Strict,
  items: [
    { uuid: "u-1", name: "Matrika Přerov", apuType: ApuType.ArchDesc, containsDigitalObjects: false },
  ],
  // the contract types `facets` as the base FacetResult, so each subtype says
  // which one it is - the same discrimination the UI does when reading them
  facets: [
    {
      kind: FacetResultKind.Enum,
      code: TYPE_FACET,
      buckets: [
        { value: "ARCH_DESC", count: 3 },
        { value: "FUND", count: 1 },
      ],
    } as EnumFacetResult,
    {
      kind: FacetResultKind.Dating,
      code: DATE_FACET,
      bounds: { minYear: 1800, maxYear: 1910 },
      undatedCount: 2,
    } as DatingFacetResult,
    { kind: FacetResultKind.Ref, code: RELATED_FACET, buckets: [] } as RefFacetResult,
  ],
  typeCounts: [
    { apuType: ApuType.ArchDesc, count: 3 },
    { apuType: ApuType.Fund, count: 1 },
  ],
};

let facets: FacetDef[] = BUILT_IN_FACETS;
let response: ApuSearchResponse = RESPONSE;

// typed parameters, so an assertion on what the UI asked for is checked at build
// time rather than only when it runs
type FacetsCall = { apuType?: ApuType; lang?: string };
type SearchCall = { apuSearchRequest: ApuSearchRequest };

const searchGetFacets = vi.fn((_call: FacetsCall) => Promise.resolve(facets));
const searchSearch = vi.fn((_call: SearchCall) => Promise.resolve(response));

vi.mock("../api/client", () => ({
  searchApi: {
    searchGetFacets: (call: FacetsCall) => searchGetFacets(call),
    searchSearch: (call: SearchCall) => searchSearch(call),
  },
  apuApi: { apuGetDetail: vi.fn(() => Promise.resolve({ name: "Německo" })) },
}));

/** The general search - no apuType, which is the whole difference from a section. */
function renderGeneralSearch(url = "/apu") {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[url]}>
        <SearchView titleKey="nav.search" />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe("SearchView, general search", () => {
  beforeEach(async () => {
    facets = BUILT_IN_FACETS;
    response = RESPONSE;
    await i18n.changeLanguage(DEFAULT_LANGUAGE);
  });

  it("asks for facets without a section and offers the built-in ones", async () => {
    renderGeneralSearch();

    // no apuType goes out: that is what makes the server answer with the
    // built-in facets rather than a section's configured ones
    await waitFor(() =>
      expect(searchGetFacets).toHaveBeenCalledWith(
        expect.objectContaining({ apuType: undefined }),
      ),
    );
    for (const label of ["Record type", "Dating", "Related to"]) {
      expect(await screen.findByRole("heading", { level: 2, name: label })).toBeInTheDocument();
    }
  });

  it("keeps the type-count chips beside the type facet - they lead elsewhere", async () => {
    renderGeneralSearch();

    // the facet narrows in place; a chip leads into the section, which brings
    // that section's own facets, so both belong here
    expect(await screen.findByRole("checkbox", { name: /Archival records/ })).toBeInTheDocument();
    expect(
      screen.getByRole("link", { name: "Archival records (3)" }),
    ).toHaveAttribute("href", "/arch-desc");
  });

  it("shows a relation constraint once, in the facet rather than twice", async () => {
    const filters = JSON.stringify([
      { kind: FilterKind.Related, facet: RELATED_FACET, apus: ["u-9"] },
    ]);
    renderGeneralSearch(`/apu?f=${encodeURIComponent(filters)}`);

    // the chips exist for searches with no relation facet to show the constraint;
    // where the facet is offered, one is enough - and both carry the same
    // heading, so a duplicate would be invisible to the reader but not to a
    // screen reader working through the sidebar
    expect(await screen.findAllByRole("heading", { level: 2, name: "Related to" })).toHaveLength(1);
    expect(
      await screen.findAllByRole("button", { name: /Remove the relation filter/ }),
    ).toHaveLength(1);
  });

  it("narrows by record type without leaving the general search", async () => {
    renderGeneralSearch();
    const fonds = await screen.findByRole("checkbox", { name: /Archival fonds/ });

    await userEvent.click(fonds);

    // the filter lands in the URL, so the constraint is shareable and undoable
    await waitFor(() =>
      expect(searchSearch).toHaveBeenCalledWith(
        expect.objectContaining({
          apuSearchRequest: expect.objectContaining({
            apuType: undefined,
            filters: [{ kind: FilterKind.Values, facet: TYPE_FACET, values: ["FUND"] }],
          }),
        }),
      ),
    );
  });

  it("has no accessibility violations", async () => {
    const { container } = renderGeneralSearch();
    await screen.findByRole("heading", { level: 2, name: "Dating" });

    // heading-order: the result cards are h3 under the page's h1, a gap this
    // page has always had and that the facet panels neither cause nor cure
    // (doc/accessibility.md §4)
    await expectNoA11yViolations(container, ["heading-order"]);
  });
});

/**
 * A section search whose deployment configures a dependent facet: RECORD~TYPE is
 * offered only once UNIT~TYPE has "matrika" selected. That is what the compound
 * when-condition of searchConfig.yaml means, and the old portal evaluates it in
 * its sidebar for the same reason this does - whether it holds depends on the
 * filters, which only the client holds.
 */
describe("SearchView, a facet that waits for another", () => {
  const PARENT = "UNIT~TYPE";
  const DEPENDENT = "RECORD~TYPE";

  const SECTION_FACETS: FacetDef[] = [
    { code: PARENT, type: FacetType.Enum, label: "Kind of material", display: FacetDisplay.Always },
    {
      code: DEPENDENT,
      type: FacetType.Enum,
      label: "Kind of record",
      display: FacetDisplay.Always,
      offeredWhen: [{ facet: PARENT, value: "matrika" }],
    },
  ];

  const SECTION_RESPONSE: ApuSearchResponse = {
    ...RESPONSE,
    facets: [
      {
        kind: FacetResultKind.Enum,
        code: PARENT,
        buckets: [{ value: "matrika", count: 9 }],
      } as EnumFacetResult,
      {
        kind: FacetResultKind.Enum,
        code: DEPENDENT,
        buckets: [{ value: "birth", count: 2 }],
      } as EnumFacetResult,
    ],
    typeCounts: [],
  };

  function renderSection(url: string) {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={[url]}>
          <SearchView apuType={ApuType.ArchDesc} titleKey="nav.search" />
        </MemoryRouter>
      </QueryClientProvider>,
    );
  }

  const f = (filters: unknown[]) => `/arch-desc?f=${encodeURIComponent(JSON.stringify(filters))}`;

  beforeEach(async () => {
    facets = SECTION_FACETS;
    response = SECTION_RESPONSE;
    await i18n.changeLanguage(DEFAULT_LANGUAGE);
  });

  it("is not offered until the selection it waits for is made", async () => {
    renderSection("/arch-desc");

    expect(await screen.findByRole("heading", { level: 2, name: "Kind of material" })).toBeInTheDocument();
    expect(screen.queryByRole("heading", { name: "Kind of record" })).not.toBeInTheDocument();
  });

  it("appears once that value is among the selected ones", async () => {
    renderSection(f([{ kind: FilterKind.Values, facet: PARENT, values: ["kroniky", "matrika"] }]));

    // the reader may be looking at several kinds at once, so any match counts
    expect(await screen.findByRole("heading", { level: 2, name: "Kind of record" })).toBeInTheDocument();
  });

  it("drops its constraint when that selection is gone", async () => {
    // a shared link whose parent selection is absent: the constraint has nothing
    // left to stand on, so it goes rather than narrowing the result invisibly
    renderSection(f([{ kind: FilterKind.Values, facet: DEPENDENT, values: ["birth"] }]));

    await waitFor(() =>
      expect(searchSearch.mock.calls.at(-1)?.[0]).toEqual(
        expect.objectContaining({
          apuSearchRequest: expect.objectContaining({ filters: [] }),
        }),
      ),
    );
    expect(screen.queryByRole("heading", { name: "Kind of record" })).not.toBeInTheDocument();
  });

  it("keeps a constraint whose condition still holds", async () => {
    renderSection(
      f([
        { kind: FilterKind.Values, facet: PARENT, values: ["matrika"] },
        { kind: FilterKind.Values, facet: DEPENDENT, values: ["birth"] },
      ]),
    );

    await screen.findByRole("heading", { level: 2, name: "Kind of record" });
    // nothing was dropped: the last request still carries both
    expect(searchSearch.mock.calls.at(-1)?.[0]).toEqual(
      expect.objectContaining({
        apuSearchRequest: expect.objectContaining({
          filters: [
            { kind: FilterKind.Values, facet: PARENT, values: ["matrika"] },
            { kind: FilterKind.Values, facet: DEPENDENT, values: ["birth"] },
          ],
        }),
      }),
    );
  });
});
