import { screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
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
import HomePage from "../pages/HomePage";
import { expectNoA11yViolations } from "../test/a11y";
import { renderWithProviders } from "../test/render";

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
  imageUrl: "/api/v1/ui/images/matriky.jpg",
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
              imageUrl: "/api/v1/ui/images/mail.svg",
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
  return renderWithProviders(<HomePage />);
}

describe("HomePage", () => {
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

  it("shows the search box alone when the deployment configures no home page", async () => {
    uiGetConfig.mockImplementationOnce(() =>
      Promise.resolve({ ...config, homePage: undefined }),
    );
    renderHome();

    expect(await screen.findByRole("button", { name: "Search" })).toBeTruthy();
    expect(screen.queryByRole("heading", { name: "Mohlo by vás zajímat" })).toBeNull();
  });

  it("has no accessibility violations", async () => {
    const { container } = renderHome();
    await screen.findByRole("heading", { name: "Mohlo by vás zajímat" });

    await expectNoA11yViolations(container);
  });
});
