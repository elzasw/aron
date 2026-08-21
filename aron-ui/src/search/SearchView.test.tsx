import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import {
  type ApuSearchResponse,
  ApuType,
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

const searchGetFacets = vi.fn(() => Promise.resolve(BUILT_IN_FACETS));
const searchSearch = vi.fn(() => Promise.resolve(RESPONSE));

vi.mock("../api/client", () => ({
  searchApi: {
    searchGetFacets: (...args: unknown[]) => searchGetFacets(...(args as [])),
    searchSearch: (...args: unknown[]) => searchSearch(...(args as [])),
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
