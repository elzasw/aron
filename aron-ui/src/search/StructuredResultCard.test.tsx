import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApuType, type ApuSearchItem, type ResultLayout } from "../api/generated";
import i18n, { DEFAULT_LANGUAGE } from "../i18n";
import { expectNoA11yViolations } from "../test/a11y";
import ResultList from "./ResultList";
import StructuredResultCard from "./StructuredResultCard";
import { lookup } from "./useResultLayout";

// ResultList fetches the layout itself; the card takes it as a prop, so only the
// list test needs the request served
vi.mock("../api/client", () => ({
  searchApi: { searchGetResultLayout: vi.fn(() => Promise.resolve(LAYOUT)) },
}));

const LAYOUT: ResultLayout = {
  fieldSeparator: " | ",
  icons: [{ code: "A_IB", url: "/api/v1/ui/result-images/record.svg", size: 35 }],
  fields: [
    { code: "N", heading: true, bold: true, scale: 1.2 },
    { code: "J_S", prefix: "sign.: ", valueSeparator: ", ", label: "sign.: " },
    { code: "J_F", label: "Archivní soubor" },
  ],
};

/** The fixture record: a heading row, a two-field row, a referenced value. */
const STRUCTURED: NonNullable<ApuSearchItem["structured"]> = {
  code: "A_IB",
  thumbnailUrl: "/api/v1/ui/result-images/record.svg",
  rows: [
    { fields: [{ code: "N", values: [{ text: "Kronika obce Testov" }] }] },
    {
      fields: [
        { code: "J_S", values: [{ text: "K-12" }, { text: "K-12a" }] },
        { code: "J_F", values: [{ text: "Sbírka kronik", refUuid: "fund-uuid" }] },
      ],
    },
  ],
};

function renderCard(
  structured = STRUCTURED,
  layout: ResultLayout = LAYOUT,
  name = "V1D Kronika obce Testov",
) {
  return render(
    <MemoryRouter>
      <StructuredResultCard
        uuid="apu-uuid"
        name={name}
        structured={structured}
        layout={lookup(layout)}
      />
    </MemoryRouter>,
  );
}

describe("StructuredResultCard", () => {
  beforeEach(async () => {
    await i18n.changeLanguage(DEFAULT_LANGUAGE);
  });

  it("makes the heading field the link into the record", () => {
    renderCard();

    const heading = screen.getByRole("heading", { level: 3, name: "Kronika obce Testov" });
    expect(heading).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Kronika obce Testov" })).toHaveAttribute(
      "href",
      "/apu/apu-uuid",
    );
  });

  it("falls back to the record name when no field is marked as the heading", () => {
    // a layout that styles the fields but marks no heading, and a first field
    // that is not the name either
    const structured = { ...STRUCTURED, rows: [STRUCTURED.rows[1]] };
    renderCard(structured, { fields: [], icons: [] });

    // the first field of the first row is the default heading...
    expect(screen.getByRole("heading", { level: 3, name: "K-12 K-12a" })).toBeInTheDocument();

    // ...and with no rows at all the record's own name still names the card
    renderCard({ ...STRUCTURED, rows: [] }, LAYOUT);
    expect(
      screen.getByRole("heading", { level: 3, name: "V1D Kronika obce Testov" }),
    ).toBeInTheDocument();
  });

  it("links a referenced value on its own, not nested in the card", () => {
    renderCard();

    // the old portal nested value links inside one card-wide link; here each
    // link is its own
    expect(screen.getByRole("link", { name: "Sbírka kronik" })).toHaveAttribute(
      "href",
      "/apu/fund-uuid",
    );
    // three links: the heading, the referenced value, the thumbnail
    expect(screen.getAllByRole("link")).toHaveLength(3);
  });

  it("names the thumbnail link and keeps the image itself silent", () => {
    renderCard();

    expect(
      screen.getByRole("link", { name: "Preview: Kronika obce Testov" }),
    ).toHaveAttribute("href", "/apu/apu-uuid");
    // decorative images carry no accessible name
    expect(screen.getByRole("link", { name: /Preview/ }).querySelector("img")).toHaveAttribute(
      "alt",
      "",
    );
  });

  it("uses the source system's own thumbnail target when it gave one", () => {
    renderCard({ ...STRUCTURED, thumbnailLinkUrl: "https://example.org/nahled" });

    expect(screen.getByRole("link", { name: /Preview/ })).toHaveAttribute(
      "href",
      "https://example.org/nahled",
    );
  });

  it("shows the deployment prefix and hides the separators from readers", () => {
    const { container } = renderCard();

    // the prefix is the visible text of a field whose code means nothing
    // (Testing Library trims, the rendered prefix keeps its trailing space)
    expect(screen.getByText("sign.:")).toBeInTheDocument();
    // separators are visual chrome - a reader should not hear " | "
    const hidden = [...container.querySelectorAll("[aria-hidden='true']")].map(
      (node) => node.textContent,
    );
    expect(hidden).toContain(" | ");
    expect(hidden).toContain(", ");
  });

  it("labels a field that has no visible prefix", () => {
    renderCard();

    // J_F carries only a label: it is there for readers, not on screen
    expect(screen.getByText("Archivní soubor:", { exact: false })).toBeInTheDocument();
  });

  it("has no structural accessibility violations", async () => {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    const { container } = render(
      <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <ResultList
          items={[
            {
              uuid: "apu-uuid",
              name: "V1D Kronika obce Testov",
              apuType: ApuType.ArchDesc,
              containsDigitalObjects: false,
              structured: STRUCTURED,
            },
            {
              // the mixed list: a record without a structured presentation
              uuid: "plain-uuid",
              name: "V1D Kronika obce Bukov",
              description: "Bez strukturovaného výsledku",
              apuType: ApuType.ArchDesc,
              containsDigitalObjects: false,
            },
          ]}
        />
      </MemoryRouter>
      </QueryClientProvider>,
    );

    // wait for the layout request the list issues itself, so the assertions see
    // the laid-out card rather than its first, unstyled paint
    await screen.findByText("Archivní soubor:", { exact: false });

    // both card kinds are headed list items, so a mixed page still navigates
    expect(screen.getAllByRole("listitem")).toHaveLength(2);
    expect(screen.getAllByRole("heading", { level: 3 })).toHaveLength(2);
    // "region" and heading-order apply to a whole page, not to one rendered list
    await expectNoA11yViolations(container, ["region", "page-has-heading-one", "heading-order"]);
  });
});
