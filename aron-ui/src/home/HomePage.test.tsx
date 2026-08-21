import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import {
  ApuType,
  FilterKind,
  type LinkTile,
  type SearchTile,
  TileKind,
  TileStyle,
  type UiConfig,
  type ValuesFilter,
} from "../api/generated";
import i18n, { DEFAULT_LANGUAGE } from "../i18n";
import HomePage from "../pages/HomePage";
import { expectNoA11yViolations } from "../test/a11y";

// named rather than inlined: a tile sits in an Array<HomeTile> and a filter in
// an Array<SearchFilter>, and a fresh literal of a subtype in either place trips
// TypeScript's excess-property check
const unitType: ValuesFilter = {
  kind: FilterKind.Values,
  facet: "UNIT~TYPE",
  values: ["matrika"],
};

const matriky: SearchTile = {
  kind: TileKind.Search,
  label: "Matriky",
  note: "církevní i civilní",
  imageUrl: "/api/v1/ui/result-images/matriky.jpg",
  imagePositionY: "30%",
  columnSpan: 2,
  rowSpan: 2,
  apuType: ApuType.ArchDesc,
  query: "Zámrsk",
  filters: [unitType],
};

const funds: SearchTile = {
  kind: TileKind.Search,
  label: "Archivní soubory",
  apuType: ApuType.Fund,
  filters: [],
};

const portaFontium: LinkTile = {
  kind: TileKind.Link,
  label: "Porta fontium",
  url: "https://www.portafontium.eu",
};

const config: UiConfig = {
  name: "Testovací portál",
  localizations: ["en"],
  menuItems: [],
  footerLinks: [],
  homePage: {
    groups: [
      {
        label: "Mohlo by vás zajímat",
        style: TileStyle.Grid,
        tiles: [matriky, funds],
      },
      {
        label: "Jiné zdroje",
        style: TileStyle.List,
        tiles: [portaFontium],
      },
    ],
    footer: {
      columns: [
        {
          heading: "Základní informace",
          paragraphs: [
            {
              runs: [
                { text: "Portál je aplikace " },
                { text: "Testovacího archivu", url: "http://archiv.test.example" },
                { text: " a zpřístupňuje popis archiválií." },
              ],
            },
          ],
          links: [],
        },
        {
          heading: "Kontakt",
          paragraphs: [],
          links: [
            {
              label: "badatelna@test.example",
              url: "mailto:badatelna@test.example",
              imageUrl: "/api/v1/ui/result-images/mail.svg",
            },
          ],
        },
      ],
    },
  },
};

const uiGetConfig = vi.fn(() => Promise.resolve(config));

vi.mock("../api/client", () => ({
  logoUrl: "/api/v1/ui/logo",
  uiApi: { uiGetConfig: (...args: unknown[]) => uiGetConfig(...(args as [])) },
}));

function renderHome() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <HomePage />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe("HomePage", () => {
  beforeEach(async () => {
    await i18n.changeLanguage(DEFAULT_LANGUAGE);
  });

  it("names each configured group and offers its tiles as links", async () => {
    renderHome();

    // a group is a named region, so a reader can be told what a set of links is for
    expect(await screen.findByRole("heading", { name: "Mohlo by vás zajímat" })).toBeTruthy();
    expect(screen.getByRole("region", { name: "Jiné zdroje" })).toBeTruthy();
    // the note is part of the tile's accessible name, not a hover-only tooltip
    expect(screen.getByRole("link", { name: /Matriky/ }).textContent).toContain("církevní i civilní");
  });

  it("sends a section tile into that section's search with its filters applied", async () => {
    renderHome();

    const tile = await screen.findByRole("link", { name: /Matriky/ });
    // the filters travel in the same `f` parameter the search page reads
    // anywhere else, so the constraint arrives as one the reader can undo
    const href = tile.getAttribute("href")!;
    expect(href.startsWith("/arch-desc?")).toBe(true);
    const params = new URLSearchParams(href.slice(href.indexOf("?")));
    expect(params.get("q")).toBe("Zámrsk");
    expect(JSON.parse(params.get("f")!)).toEqual([
      { kind: "VALUES", facet: "UNIT~TYPE", values: ["matrika"] },
    ]);

    // a tile with no filters is just the section
    expect(screen.getByRole("link", { name: "Archivní soubory" }).getAttribute("href")).toBe("/fund");
  });

  it("keeps a tile leading out of the portal a plain link", async () => {
    renderHome();

    const external = await screen.findByRole("link", { name: "Porta fontium" });
    expect(external.getAttribute("href")).toBe("https://www.portafontium.eu");
    // no target=_blank: a reader is not moved to another tab without warning
    expect(external.getAttribute("target")).toBeNull();
  });

  it("renders footer prose with its link inside the sentence", async () => {
    renderHome();

    const band = await screen.findByRole("region", { name: "About this portal" });
    expect(band.textContent).toContain("Portál je aplikace Testovacího archivu a zpřístupňuje popis archiválií.");
    const inline = screen.getByRole("link", { name: "Testovacího archivu" });
    expect(inline.getAttribute("href")).toBe("http://archiv.test.example");

    // a mark is decoration; the label names the link
    const contact = screen.getByRole("link", { name: "badatelna@test.example" });
    expect(contact.querySelector("img")!.getAttribute("alt")).toBe("");
  });

  it("shows the search box alone when the deployment configures no home page", async () => {
    uiGetConfig.mockImplementationOnce(() =>
      Promise.resolve({ ...config, homePage: undefined }),
    );
    renderHome();

    expect(await screen.findByRole("button", { name: "Search" })).toBeTruthy();
    expect(screen.queryByRole("region", { name: "About this portal" })).toBeNull();
    expect(screen.queryByRole("heading", { name: "Mohlo by vás zajímat" })).toBeNull();
  });

  it("has no accessibility violations", async () => {
    const { container } = renderHome();
    await screen.findByRole("heading", { name: "Mohlo by vás zajímat" });

    await expectNoA11yViolations(container);
  });
});
