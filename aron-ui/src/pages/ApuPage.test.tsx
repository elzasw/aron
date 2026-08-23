import { screen } from "@testing-library/react";
import { Route, Routes } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";
import { ApuType, FileType, type ApuDetail } from "../api/generated";
import { expectNoA11yViolations } from "../test/a11y";
import { renderWithProviders } from "../test/render";
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
    // the license is stated beside the viewer
    screen.getByText("Licence: CC-BY-4.0");
    await expectNoA11yViolations(container);
  });

  it("keeps the plain two-pane layout for a record without them", async () => {
    renderRecord("plain");
    await screen.findByRole("heading", { level: 1, name: "Privilegia" });
    expect(screen.queryByRole("toolbar")).toBeNull();
    expect(screen.queryByRole("application")).toBeNull();
  });
});
