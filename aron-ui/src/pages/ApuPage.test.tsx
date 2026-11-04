import { act, fireEvent, screen, within } from "@testing-library/react";
import { Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import {
  ApuType,
  DetailItemKind,
  FileType,
  PartViewType,
  type ApuDetail,
  type JsonItem,
  type RefItem,
  type TextItem,
} from "../api/generated";
import { expectNoA11yViolations } from "../test/a11y";
import { renderWithProviders } from "../test/render";
import { setViewport, setViewportWidth } from "../test/viewport";
import ApuPage from "./ApuPage";

vi.mock("openseadragon", () => ({
  default: vi.fn(() => ({
    open: vi.fn(),
    addHandler: vi.fn(),
    destroy: vi.fn(),
    viewport: {
      zoomBy: vi.fn(() => ({ applyConstraints: vi.fn() })),
      goHome: vi.fn(),
      getRotation: vi.fn(() => 0),
      setRotation: vi.fn(),
    },
  })),
}));

/** A record whose scan must be visible immediately - the old portal's principle. */
const DIGITIZED: ApuDetail = {
  uuid: "rec",
  name: "Privilegia",
  apuType: ApuType.Fund,
  childCount: 0,
  treePath: [{ uuid: "rec", name: "Privilegia", depth: 0, pos: 1, childCount: 0 }],
  parts: [],
  attachments: [],
  digitalObjects: [
    {
      uuid: "dao1",
      license: "CC-BY-4.0",
      footer: {
        license: [{ text: "CC BY 4.0", url: "https://creativecommons.org/licenses/by/4.0/" }],
      },
      files: [
        {
          id: "z1",
          fileType: FileType.Tile,
          position: 1,
          dziUrl: "/api/v1/daofile/z1/tiles/image.dzi",
        },
        { id: "t1", fileType: FileType.Thumbnail, position: 1, url: "/api/v1/daofile/t1" },
      ],
    },
  ],
};

const originator = (n: number): RefItem => ({
  code: "ORIGINATOR~REF",
  label: "Originator",
  kind: DetailItemKind.Ref,
  ref: { uuid: `orig-${n}`, name: `Originator ${n}` },
});

const text = (code: string, label: string, value: string): TextItem => ({
  code,
  label,
  kind: DetailItemKind.Text,
  value,
});

/** A fund with more originators than the description shows at first. */
const MANY_ORIGINATORS: ApuDetail = {
  ...DIGITIZED,
  uuid: "many",
  digitalObjects: [],
  parts: [
    {
      code: "PT_ORIGINATORS",
      label: "Originators",
      viewType: PartViewType.Standalone,
      items: Array.from({ length: 12 }, (_, i) => originator(i + 1)),
    },
    {
      code: "PT_TITLE",
      label: "Title",
      viewType: PartViewType.Grouped,
      items: [
        text("TITLE", "Title", "Town archive"),
        text("NOTE", "Note", "fragment"),
        originator(99),
      ],
    },
  ],
};

const jsonItem = (json: string): JsonItem => ({
  code: "EVIDENCE_UNIT",
  label: "Evidence unit",
  kind: DetailItemKind.Json,
  json,
});

/** The transfagent's "jsoncomponent" for a fund's evidence units, as it comes over the wire. */
const EVIDENCE_COLUMNS = [
  "Název",
  "Nezpracováno",
  "Zpracováno",
  "Inventarizováno",
  "Poškozeno",
  "Časový rozsah",
];
const EVIDENCE_TABLE = JSON.stringify({
  type: "table",
  data: {
    columns: EVIDENCE_COLUMNS.map((name) => ({ name })),
    rows: [
      ["kartony\r\nčást poškozena", "0", "12/3", "0", "2", "1900-1950"],
      ["fotografie", "5", "0", "0", "0", ""],
    ],
  },
});

/** A fund whose info part carries a structured value, once visible and once behind a summary. */
const FUND_INFO: ApuDetail = {
  ...DIGITIZED,
  uuid: "fund",
  digitalObjects: [],
  parts: [
    {
      code: "PT_FUND_INFO",
      label: "Fund info",
      viewType: PartViewType.Standalone,
      items: [text("FUND_LENGTH", "Length", "12 bm"), jsonItem(EVIDENCE_TABLE)],
    },
    {
      code: "PT_NOTE",
      label: "Note",
      viewType: PartViewType.Grouped,
      items: [text("NOTE", "Note", "fragment"), jsonItem(EVIDENCE_TABLE)],
    },
  ],
};

const UNKNOWN_JSON = '{"type":"chart","data":[]}';

const detailByUuid: Record<string, ApuDetail> = {
  rec: DIGITIZED,
  plain: { ...DIGITIZED, uuid: "plain", digitalObjects: [] },
  many: MANY_ORIGINATORS,
  fund: FUND_INFO,
  unknown: {
    ...DIGITIZED,
    uuid: "unknown",
    digitalObjects: [],
    parts: [
      {
        code: "PT_FUND_INFO",
        label: "Fund info",
        viewType: PartViewType.Standalone,
        items: [jsonItem(UNKNOWN_JSON)],
      },
    ],
  },
};

vi.mock("../api/client", () => ({
  apuApi: {
    apuGetDetail: vi.fn((request: { uuid: string }) =>
      Promise.resolve(detailByUuid[request.uuid]),
    ),
    apuGetTreeNodes: vi.fn(() => Promise.resolve([])),
  },
  // the page asks which record types this deployment can cite
  uiApi: { uiGetConfig: vi.fn(() => Promise.resolve({ citations: [] })) },
}));

function renderRecord(uuid: string) {
  return renderWithProviders(
    <Routes>
      <Route path="apu/:uuid" element={<ApuPage />} />
    </Routes>,
    { initialEntries: [`/apu/${uuid}`] },
  );
}

describe("ApuPage with digital objects", () => {
  // wide and tall enough for all three panes; jsdom's default 1024px sits in
  // the medium band, where the description has no column of its own
  beforeEach(() => setViewport(1440, 900));

  it("embeds the viewer as the page's centerpiece, with a link to its own page", async () => {
    const { container } = renderRecord("rec");
    await screen.findByRole("heading", { level: 1, name: "Privilegia" });
    // the viewer is on the page immediately - toolbar, rail, canvas region
    screen.getByRole("toolbar", { name: "Viewer controls" });
    screen.getByRole("list", { name: "Page thumbnails" });
    expect(screen.getByRole("application").getAttribute("aria-label")).toContain("Page 1");
    // the viewer's own page stays a link into the routed viewer
    const ownPage = screen.getByRole("link", { name: "Open on its own page" });
    expect(ownPage.getAttribute("href")).toBe("/apu/rec/dao/dao1?file=z1");
    // the attribution overlay carries the server-resolved license statement
    screen.getByRole("link", { name: "CC BY 4.0" });
    // the description column's width is the reader's, like the tree's
    screen.getByRole("separator", { name: "Width of the archival description column" });
    await expectNoA11yViolations(container);
  });

  it("folds the description column away and brings it back", async () => {
    const { container } = renderRecord("rec");
    await screen.findByRole("heading", { level: 1, name: "Privilegia" });
    fireEvent.click(screen.getByRole("button", { name: "Hide the archival description column" }));
    // folded: the pane is gone, the separator rests, only the expand chevron remains
    expect(screen.queryByRole("separator", { name: "Width of the archival description column" }))
      .toBeNull();
    const expand = screen.getByRole("button", { name: "Show the archival description column" });
    fireEvent.click(expand);
    screen.getByRole("separator", { name: "Width of the archival description column" });
    await expectNoA11yViolations(container);
  });

  it("keeps the plain two-pane layout for a record without them", async () => {
    renderRecord("plain");
    await screen.findByRole("heading", { level: 1, name: "Privilegia" });
    expect(screen.queryByRole("toolbar")).toBeNull();
    expect(screen.queryByRole("application")).toBeNull();
  });

  it("keeps the scan on screen where three panes cannot fit: viewer above the description", async () => {
    // the medium band: the tree beside one right column, no third pane
    setViewportWidth(1024);
    const { container } = renderRecord("rec");
    await screen.findByRole("heading", { level: 1, name: "Privilegia" });
    // the viewer is still embedded...
    screen.getByRole("toolbar", { name: "Viewer controls" });
    // ...but the description is no column of its own: nothing to drag or fold
    expect(
      screen.queryByRole("separator", { name: "Width of the archival description column" }),
    ).toBeNull();
    expect(
      screen.queryByRole("button", { name: "Hide the archival description column" }),
    ).toBeNull();
    // the embedded object is not offered a second time as a gallery link
    expect(screen.queryByRole("link", { name: "Open viewer: Digitised objects" })).toBeNull();
    await expectNoA11yViolations(container);
  });

  it("stacks on a short viewport however wide it is - a phone held sideways", async () => {
    setViewport(1440, 400);
    const { container } = renderRecord("rec");
    await screen.findByRole("heading", { level: 1, name: "Privilegia" });
    // the scan stays embedded, but there is no column beside it to drag or fold
    screen.getByRole("toolbar", { name: "Viewer controls" });
    expect(
      screen.queryByRole("separator", { name: "Width of the archival description column" }),
    ).toBeNull();
    await expectNoA11yViolations(container);
    // turned upright again (tall enough), the three panes are back
    act(() => setViewport(1440, 900));
    await screen.findByRole("separator", { name: "Width of the archival description column" });
  });

  it("shows ten values of one item code and the rest behind a toggle", async () => {
    const { container } = renderRecord("many");
    await screen.findByRole("heading", { level: 1, name: "Privilegia" });
    // one row for the whole code: the label once, not twelve times
    expect(screen.getAllByText("Originator")).toHaveLength(1);
    expect(screen.getAllByRole("link", { name: /^Originator \d+$/ })).toHaveLength(10);
    const showAll = screen.getByRole("button", { name: "Show all (12)" });
    expect(showAll.getAttribute("aria-expanded")).toBe("false");
    await expectNoA11yViolations(container);
    fireEvent.click(showAll);
    expect(screen.getAllByRole("link", { name: /^Originator \d+$/ })).toHaveLength(12);
    screen.getByRole("link", { name: "Originator 12" });
    const hide = screen.getByRole("button", { name: "Hide" });
    expect(hide.getAttribute("aria-expanded")).toBe("true");
    fireEvent.click(hide);
    expect(screen.getAllByRole("link", { name: /^Originator \d+$/ })).toHaveLength(10);
  });

  it("keeps references out of a grouped part's summary and shows them expanded", async () => {
    renderRecord("many");
    await screen.findByRole("heading", { level: 1, name: "Privilegia" });
    const summary = screen.getByRole("button", { name: /Town archive fragment/ });
    expect(summary.textContent).not.toContain("Originator 99");
    expect(screen.queryByRole("link", { name: "Originator 99" })).toBeNull();
    fireEvent.click(summary);
    screen.getByRole("link", { name: "Originator 99" });
  });

  it("lays a structured value out as a table and keeps it out of a summary", async () => {
    const { container } = renderRecord("fund");
    await screen.findByRole("heading", { level: 1, name: "Privilegia" });
    const table = screen.getByRole("table", { name: "Evidence unit" });
    expect(
      within(table)
        .getAllByRole("columnheader")
        .map((cell) => cell.textContent),
    ).toEqual(EVIDENCE_COLUMNS);
    // header row plus one row per evidence unit
    expect(within(table).getAllByRole("row")).toHaveLength(3);
    within(table).getByRole("cell", { name: "12/3" });
    within(table).getByRole("cell", { name: "1900-1950" });
    // the grouped part's one-line summary leaves the table out, like references
    const summary = screen.getByRole("button", { name: /fragment/ });
    expect(summary.textContent).not.toContain("table");
    await expectNoA11yViolations(container);
  });

  it("shows a structured value it cannot lay out as the raw text", async () => {
    renderRecord("unknown");
    await screen.findByRole("heading", { level: 1, name: "Privilegia" });
    expect(screen.queryByRole("table")).toBeNull();
    screen.getByText(UNKNOWN_JSON);
  });

  it("gives the description its own column once the viewport grows enough", async () => {
    setViewportWidth(1024);
    renderRecord("rec");
    await screen.findByRole("heading", { level: 1, name: "Privilegia" });
    expect(
      screen.queryByRole("separator", { name: "Width of the archival description column" }),
    ).toBeNull();
    act(() => setViewportWidth(1440));
    await screen.findByRole("separator", { name: "Width of the archival description column" });
  });
});
