import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { Route, Routes } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";
import {
  ApuType,
  FacetDisplay,
  type ApuDetail,
  FilterKind,
  RelationDirection,
  type SearchFilter,
} from "../api/generated";
import { expectNoA11yViolations } from "../test/a11y";
import { renderWithProviders } from "../test/render";
import {
  RELATED_FACET,
  addRelated,
  facetIsOffered,
  parseFilters,
  relatedOf,
  relatedSearchUrl,
  removeRelated,
} from "./filters";
import RelatedChips from "./RelatedChips";
import ApuPage from "../pages/ApuPage";

const RECORD: ApuDetail = {
  uuid: "9f1d0000-0000-4000-8000-90000000000f",
  name: "Sbírka kronik",
  apuType: ApuType.Fund,
  childCount: 0,
  treePath: [],
  parts: [],
  attachments: [],
  digitalObjects: [],
};

vi.mock("../api/client", () => ({
  apuApi: { apuGetDetail: vi.fn(() => Promise.resolve(RECORD)) },
}));

function renderChips(filters: SearchFilter[], onFilters = vi.fn()) {
  const rendered = renderWithProviders(
    <RelatedChips filters={filters} onFilters={onFilters} />,
  );
  return { ...rendered, onFilters };
}

describe("relation filter", () => {
  it("asks for both ends of the relation, across every section", () => {
    const url = relatedSearchUrl(RECORD.uuid);
    // the general search, not a section: a relation is not confined to one
    expect(url.startsWith("/apu?")).toBe(true);

    const filters = parseFilters(new URLSearchParams(url.slice(url.indexOf("?"))).get("f"));
    expect(relatedOf(filters)).toEqual([
      {
        kind: FilterKind.Related,
        facet: RELATED_FACET,
        apus: [RECORD.uuid],
        direction: RelationDirection.Both,
      },
    ]);
  });

  it("keeps several relation conditions apart, so they narrow rather than widen", () => {
    // two conditions = AND on the server; one condition with two uuids would be OR
    const both = addRelated(addRelated([], "apu-a"), "apu-b");
    expect(relatedOf(both).map((f) => f.apus)).toEqual([["apu-a"], ["apu-b"]]);

    // adding the same record twice changes nothing
    expect(addRelated(both, "apu-a")).toBe(both);

    expect(relatedOf(removeRelated(both, "apu-a")).map((f) => f.apus)).toEqual([["apu-b"]]);
  });

  it("shows the constraint by the record's name and lets the reader undo it", async () => {
    const filters = addRelated([], RECORD.uuid);
    const { onFilters } = renderChips(filters);

    // the uuid stands in until the name arrives, then the name replaces it
    expect(await screen.findByText(RECORD.name)).toBeInTheDocument();

    await userEvent.click(
      screen.getByRole("button", { name: `Remove the relation filter: ${RECORD.name}` }),
    );
    expect(onFilters).toHaveBeenCalledWith([]);
  });


  it("offers the action on the record page as a link into the search", async () => {
    renderWithProviders(
      <Routes>
        <Route path="apu/:uuid" element={<ApuPage />} />
      </Routes>,
      { initialEntries: [`/apu/${RECORD.uuid}`] },
    );

    // a link, not a button: it navigates, so it must open in a new tab too
    const action = await screen.findByRole("link", {
      name: `Find records related to ${RECORD.name}`,
    });
    expect(action).toHaveAttribute("href", relatedSearchUrl(RECORD.uuid));
    expect(action).toHaveTextContent("Find related");
  });


  it("offers a DETAIL facet once it carries a constraint", () => {
    // ALWAYS is offered either way; DETAIL waits for the advanced-search dialog
    expect(facetIsOffered(FacetDisplay.Always, false)).toBe(true);
    expect(facetIsOffered(FacetDisplay.Detail, false)).toBe(false);

    // ... but a constraint the reader did not set from the sidebar must still be
    // visible there, or it cannot be undone
    expect(facetIsOffered(FacetDisplay.Detail, true)).toBe(true);
  });

  it("renders nothing when no relation is active", () => {
    const { container } = renderChips([]);
    expect(container).toBeEmptyDOMElement();
  });

  it("has no accessibility violations", async () => {
    const { container } = renderChips(addRelated([], RECORD.uuid));
    expect(await screen.findByText(RECORD.name)).toBeInTheDocument();
    await expectNoA11yViolations(container);
  });
});
