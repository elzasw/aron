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
    {
      uuid: "u-1",
      name: "Pardubice",
      description: "okresní město",
      apuType: ApuType.Entity,
      containsDigitalObjects: false,
    },
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

  it("offers only records a relation can point at, and says which is which", async () => {
    renderGeneralSearch();
    const picker = await screen.findByRole("textbox", { name: "Related to – find a record" });

    await userEvent.type(picker, "par");

    // one apuType per request would not do: a relation points at an access
    // point, an archive, a fond or an aid, and never at an archival record - so
    // the scope travels as a filter on the built-in type facet
    await waitFor(() =>
      expect(searchSearch).toHaveBeenCalledWith(
        expect.objectContaining({
          apuSearchRequest: expect.objectContaining({
            query: "par",
            filters: [
              {
                kind: FilterKind.Values,
                facet: TYPE_FACET,
                values: ["ENTITY", "INSTITUTION", "FUND", "FINDING_AID"],
              },
            ],
          }),
        }),
      ),
    );

    // two access points of one name are common, so the offer carries what tells
    // them apart
    const offered = await screen.findByRole("button", { name: /Pardubice/ });
    expect(offered).toHaveTextContent("okresní město");
  });

  it("empties the box once a condition is added, so the next one can be typed", async () => {
    renderGeneralSearch();
    const picker = await screen.findByRole("textbox", { name: "Related to – find a record" });
    await userEvent.type(picker, "par");

    await userEvent.click(await screen.findByRole("button", { name: /Pardubice/ }));

    expect(picker).toHaveValue("");
    await waitFor(() =>
      expect(searchSearch.mock.calls.at(-1)?.[0]).toEqual(
        expect.objectContaining({
          apuSearchRequest: expect.objectContaining({
            filters: [expect.objectContaining({ kind: FilterKind.Related, apus: ["u-1"] })],
          }),
        }),
      ),
    );
  });

  it("reports an empty result only once the query could be one", async () => {
    response = { ...RESPONSE, items: [] };
    renderGeneralSearch();
    const picker = await screen.findByRole("textbox", { name: "Related to – find a record" });

    // three characters is long enough for the server to match part of a word, so
    // an empty answer is a real one
    await userEvent.type(picker, "čes");
    expect(await screen.findByText("No matching records")).toBeInTheDocument();

    // back below that, the same emptiness says only that a word is unfinished -
    // the search still runs, because a short name matches itself exactly
    await userEvent.type(picker, "{backspace}{backspace}");
    await waitFor(() =>
      expect(screen.queryByText("No matching records")).not.toBeInTheDocument(),
    );
    await waitFor(() =>
      expect(searchSearch).toHaveBeenCalledWith(
        expect.objectContaining({ apuSearchRequest: expect.objectContaining({ query: "č" }) }),
      ),
    );
  });

  it("has no accessibility violations", async () => {
    const { container } = renderGeneralSearch();
    await screen.findByRole("heading", { level: 2, name: "Dating" });

    await expectNoA11yViolations(container);
  });

  it("nests its headings, so the outline can be walked", async () => {
    // the page title, then the results as a section of it, then one heading per
    // record. Without the middle one the outline jumped from h1 to h3, which is
    // what a screen-reader user navigates by
    renderGeneralSearch();
    await screen.findByRole("heading", { level: 2, name: "Search results" });

    const levels = screen
      .getAllByRole("heading")
      .map((heading) => Number(heading.tagName.substring(1)));
    for (let i = 1; i < levels.length; i++) {
      expect(levels[i], `heading ${i} after level ${levels[i - 1]}`).toBeLessThanOrEqual(
        levels[i - 1] + 1,
      );
    }
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
