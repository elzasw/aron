import { act, fireEvent, screen } from "@testing-library/react";
import { Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApuType, FileType, type ApuDetail } from "../api/generated";
import { expectNoA11yViolations } from "../test/a11y";
import { renderWithProviders } from "../test/render";
import { setViewportWidth } from "../test/viewport";
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

const detailByUuid: Record<string, ApuDetail> = {
  rec: DIGITIZED,
  plain: { ...DIGITIZED, uuid: "plain", digitalObjects: [] },
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
  // wide enough for all three panes; jsdom's default 1024px sits in the
  // medium band, where the viewer is deliberately not embedded
  beforeEach(() => setViewportWidth(1440));

  it("embeds the viewer as the page's centerpiece, with a fullscreen link", async () => {
    const { container } = renderRecord("rec");
    await screen.findByRole("heading", { level: 1, name: "Privilegia" });
    // the viewer is on the page immediately - toolbar, rail, canvas region
    screen.getByRole("toolbar", { name: "Viewer controls" });
    screen.getByRole("list", { name: "Page thumbnails" });
    expect(screen.getByRole("application").getAttribute("aria-label")).toContain("Page 1");
    // the fullscreen surface stays a link into the routed viewer
    const fullscreen = screen.getByRole("link", { name: "Full screen" });
    expect(fullscreen.getAttribute("href")).toBe("/apu/rec/dao/dao1?file=z1");
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

  it("offers the scan through its gallery card where three panes cannot fit", async () => {
    // the medium band: side-by-side panes, but no room for a third one
    setViewportWidth(1024);
    const { container } = renderRecord("rec");
    await screen.findByRole("heading", { level: 1, name: "Privilegia" });
    // no embedded viewer and no splitter for its description column
    expect(screen.queryByRole("toolbar")).toBeNull();
    expect(
      screen.queryByRole("separator", { name: "Width of the archival description column" }),
    ).toBeNull();
    // the scan is one click away on the routed fullscreen viewer
    const link = screen.getByRole("link", { name: "Open viewer: Digitised objects" });
    expect(link.getAttribute("href")).toBe("/apu/rec/dao/dao1");
    await expectNoA11yViolations(container);
  });

  it("embeds the viewer once the viewport grows enough for three panes", async () => {
    setViewportWidth(1024);
    renderRecord("rec");
    await screen.findByRole("heading", { level: 1, name: "Privilegia" });
    expect(screen.queryByRole("toolbar")).toBeNull();
    act(() => setViewportWidth(1440));
    await screen.findByRole("toolbar", { name: "Viewer controls" });
    expect(screen.queryByRole("link", { name: "Open viewer: Digitised objects" })).toBeNull();
  });
});
