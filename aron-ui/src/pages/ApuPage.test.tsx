import { act, fireEvent, screen } from "@testing-library/react";
import { Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApuType, FileType, type ApuDetail } from "../api/generated";
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
