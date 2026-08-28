import { screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import {
  type ApuSearchRequest,
  type ApuSearchResponse,
  ApuType,
  type EnumFacetResult,
  type FacetDef,
  FacetDisplay,
  FacetResultKind,
  FacetType,
  FilterKind,
  QueryMode,
  TotalRelation,
} from "../api/generated";
import { expectNoA11yViolations } from "../test/a11y";
import { renderWithProviders } from "../test/render";
import SearchView from "./SearchView";

/** A section with one sidebar facet and one that only the advanced search reaches. */
const SECTION_FACETS: FacetDef[] = [
  { code: "UNIT~TYPE", type: FacetType.Enum, label: "Kind of material", display: FacetDisplay.Always },
  {
    code: "LANGUAGE",
    type: FacetType.Enum,
    label: "Language",
    description: "The language the material is written in.",
    display: FacetDisplay.Detail,
  },
];

const RESPONSE: ApuSearchResponse = {
  total: 7,
  totalRelation: TotalRelation.Eq,
  queryMode: QueryMode.Strict,
  items: [],
  facets: [
    {
      kind: FacetResultKind.Enum,
      code: "UNIT~TYPE",
      buckets: [{ value: "matrika", count: 7 }],
    } as EnumFacetResult,
    {
      kind: FacetResultKind.Enum,
      code: "LANGUAGE",
      buckets: [
        { value: "cze", label: "Czech", count: 5 },
        { value: "ger", label: "German", count: 2 },
      ],
    } as EnumFacetResult,
  ],
  typeCounts: [],
};

let facets: FacetDef[] = SECTION_FACETS;

type SearchCall = { apuSearchRequest: ApuSearchRequest };
const searchSearch = vi.fn((call: SearchCall) => {
  // a narrowed draft answers with a smaller total, so the button can be seen
  // to follow the draft rather than the page
  const narrowed = (call.apuSearchRequest.filters ?? []).some((f) => f.facet === "LANGUAGE");
  return Promise.resolve(narrowed ? { ...RESPONSE, total: 2 } : RESPONSE);
});

vi.mock("../api/client", () => ({
  searchApi: {
    searchGetFacets: () => Promise.resolve(facets),
    searchSearch: (call: SearchCall) => searchSearch(call),
  },
  apuApi: { apuGetDetail: vi.fn(() => Promise.resolve({ name: "x" })) },
}));

/** Waits wide enough for a full-suite run under load; the flows here chain several round trips. */
const WAIT = { timeout: 4000 };
/** The long flows chain several round trips; under a full-suite run they need more than vitest's 5 s. */
const LONG = 20000;

/**
 * Opens the dialog and makes sure tabster knows it is the active modal. In jsdom
 * the dialog can be focused by Fluent before tabster's mutation observer has
 * registered it; tabster then sees no active modalizer and marks every modal -
 * this one included - `aria-hidden`, which hides the whole dialog from the
 * role queries - and does so again on every re-render. Focusing an element
 * inside it runs tabster's focus handler, which activates the modal and un-hides
 * it synchronously. A browser has no such gap.
 */
async function openAdvanced(user: ReturnType<typeof userEvent.setup>): Promise<HTMLElement> {
  await user.click(await screen.findByRole("button", { name: "All filters" }));
  const dialog = await screen.findByRole("dialog", { name: "All filters" }, WAIT);
  await waitFor(() => {
    dialog.querySelector<HTMLElement>("input, button")?.focus();
    // the observable sign that tabster holds this dialog as the active modal:
    // the page behind it has left the accessibility tree
    expect(screen.queryByRole("button", { name: "All filters" })).toBeNull();
    expect(dialog).not.toHaveAttribute("aria-hidden", "true");
  }, WAIT);
  return dialog;
}

function renderSection(url = "/arch-desc") {
  return renderWithProviders(<SearchView apuType={ApuType.ArchDesc} titleKey="sections.ARCH_DESC" />, {
    initialEntries: [url],
  });
}

describe("advanced search", () => {
  beforeEach(() => {
    facets = SECTION_FACETS;
    searchSearch.mockClear();
  });

  it("is offered only where a facet is hidden from the sidebar", async () => {
    renderSection();
    expect(await screen.findByRole("button", { name: "All filters" })).toBeInTheDocument();
    // the hidden facet is nowhere on the page until the dialog opens
    expect(screen.queryByRole("heading", { level: 2, name: "Language" })).not.toBeInTheDocument();

    facets = SECTION_FACETS.filter((def) => def.display === FacetDisplay.Always);
    renderSection();
    await screen.findAllByRole("heading", { level: 2, name: "Kind of material" });
    expect(screen.getAllByRole("button", { name: "All filters" })).toHaveLength(1);
  });

  it("shows every facet with its description, and applies the draft in one step", async () => {
    const user = userEvent.setup();
    renderSection();
    const dialog = await openAdvanced(user);
    // after the title: the sidebar facet and the hidden one, in the server's
    // order, the hidden one explained - the dialog is where a reader meets it
    // for the first time
    const headings = within(dialog).getAllByRole("heading", { level: 2 });
    expect(headings.map((h) => h.textContent)).toEqual([
      "All filters",
      "Kind of material",
      "Language",
    ]);
    expect(within(dialog).getByText("The language the material is written in.")).toBeInTheDocument();
    expect(await within(dialog).findByRole("button", { name: "Show 7 records" }, WAIT)).toBeInTheDocument();

    await user.click(await within(dialog).findByRole("checkbox", { name: /German/ }, WAIT));
    // the page behind has not moved: no search with the draft filter went out
    // as the page's own query, and the sidebar still shows no Language facet
    expect(await within(dialog).findByRole("button", { name: "Show 2 records" }, WAIT)).toBeInTheDocument();
    expect(await screen.findAllByRole("heading", { level: 2, name: "Language" }, WAIT)).toHaveLength(1);

    await user.click(await within(dialog).findByRole("button", { name: "Show 2 records" }, WAIT));
    await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument(), WAIT);
    // applied: the page searches with the filter, and the facet that carries
    // it is now in the sidebar, where it can be undone
    await waitFor(() =>
      expect(searchSearch).toHaveBeenLastCalledWith({
        apuSearchRequest: expect.objectContaining({
          filters: [{ kind: FilterKind.Values, facet: "LANGUAGE", values: ["ger"] }],
          size: 10,
        }),
      }),
    );
    expect(await screen.findByRole("heading", { level: 2, name: "Language" }, WAIT)).toBeInTheDocument();
    expect(screen.getByRole("checkbox", { name: /German/ })).toBeChecked();
  }, LONG);

  it("throws the draft away on cancel", async () => {
    const user = userEvent.setup();
    renderSection();
    const dialog = await openAdvanced(user);
    await user.click(await within(dialog).findByRole("checkbox", { name: /German/ }, WAIT));
    await user.click(await within(dialog).findByRole("button", { name: "Cancel" }, WAIT));

    await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument(), WAIT);
    expect(screen.queryByRole("heading", { level: 2, name: "Language" })).not.toBeInTheDocument();
    for (const call of searchSearch.mock.calls) {
      // the page's own searches (a whole page each) never carried the draft
      if (call[0].apuSearchRequest.size !== 1) {
        expect(call[0].apuSearchRequest.filters ?? []).toEqual([]);
      }
    }
  }, LONG);

  it("has no accessibility violations while open", async () => {
    const user = userEvent.setup();
    const { container } = renderSection();
    await user.click(await screen.findByRole("button", { name: "All filters" }));
    await screen.findByRole("button", { name: "Show 7 records" });
    // the dialog is portaled out of the container, so the whole document is
    // checked; the page-level landmark rule is off because the test renders no
    // frame around the view (AppLayout owns the landmarks)
    await expectNoA11yViolations(container.ownerDocument.body, ["region"]);
  }, LONG);
});
