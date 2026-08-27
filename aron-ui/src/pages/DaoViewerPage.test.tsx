import { fireEvent, screen, waitFor } from "@testing-library/react";
import { Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApuType, FileType, type ApuDetail } from "../api/generated";
import { expectNoA11yViolations } from "../test/a11y";
import { renderWithProviders } from "../test/render";
import DaoViewerPage from "./DaoViewerPage";

/** A recording OpenSeadragon fake: jsdom has no canvas, so the real one cannot run. */
interface FakeViewer {
  opened: unknown[];
  destroy: () => void;
}
const viewers: FakeViewer[] = [];
vi.mock("openseadragon", () => ({
  default: vi.fn(() => {
    const viewer = {
      opened: [] as unknown[],
      open(source: unknown) {
        this.opened.push(source);
      },
      addHandler: vi.fn(),
      destroy: vi.fn(),
      viewport: {
        zoomBy: vi.fn(() => ({ applyConstraints: vi.fn() })),
        goHome: vi.fn(),
        getRotation: vi.fn(() => 0),
        setRotation: vi.fn(),
      },
    };
    viewers.push(viewer);
    return viewer;
  }),
}));

const DETAIL: ApuDetail = {
  uuid: "rec",
  name: "Marriage register",
  apuType: ApuType.ArchDesc,
  childCount: 0,
  treePath: [{ uuid: "rec", name: "Marriage register", depth: 0, pos: 1, childCount: 0 }],
  parts: [],
  attachments: [],
  digitalObjects: [
    {
      uuid: "dao1",
      name: "Kronika obce",
      license: "CC-BY-4.0",
      // resolved server-side from the deployment's daoFooter configuration
      footer: {
        dedication: [
          { text: "Digitized with the support of " },
          { text: "NAKI II", url: "https://example.org/naki" },
          { text: "." },
        ],
        license: [{ text: "CC BY 4.0", url: "https://creativecommons.org/licenses/by/4.0/" }],
        licenseImage: "/api/v1/ui/images/record.svg",
      },
      files: [
        {
          id: "z1",
          fileType: FileType.Tile,
          position: 1,
          dziUrl: "/api/v1/daofile/z1/tiles/image.dzi",
        },
        {
          id: "p1",
          fileType: FileType.Published,
          position: 1,
          contentType: "image/png",
          name: "scan-1.png",
          url: "/api/v1/daofile/p1",
          selected: true,
        },
        { id: "t1", fileType: FileType.Thumbnail, position: 1, url: "/api/v1/daofile/t1" },
        {
          id: "p2",
          fileType: FileType.Published,
          position: 2,
          contentType: "image/png",
          name: "scan-2.png",
          url: "/api/v1/daofile/p2",
        },
        {
          id: "p3",
          fileType: FileType.Published,
          position: 3,
          contentType: "application/pdf",
          name: "notes.pdf",
          url: "/api/v1/daofile/p3",
        },
      ],
    },
  ],
};

vi.mock("../api/client", () => ({
  apuApi: { apuGetDetail: vi.fn(() => Promise.resolve(DETAIL)) },
}));

function renderViewer(entry = "/apu/rec/dao/dao1") {
  return renderWithProviders(
    <Routes>
      <Route path="apu/:uuid/dao/:daoUuid" element={<DaoViewerPage />} />
    </Routes>,
    { initialEntries: [entry] },
  );
}

describe("DaoViewerPage", () => {
  beforeEach(() => {
    viewers.length = 0;
  });

  it("shows the digital object and opens its selected page in the viewer", async () => {
    const { container } = renderViewer();
    expect(await screen.findByRole("heading", { level: 1, name: "Kronika obce" })).toBeTruthy();
    // the attribution overlay: server-resolved runs render as anchors
    screen.getByText("Digitized with the support of", { exact: false });
    const licenseLink = screen.getByRole("link", { name: "CC BY 4.0" });
    expect(licenseLink.getAttribute("href")).toBe("https://creativecommons.org/licenses/by/4.0/");
    // the page-number overlay repeats the toolbar's position for the eye
    screen.getByText("1/3", { exact: false });
    expect((screen.getByLabelText("Page") as HTMLInputElement).value).toBe("1");
    // the deep-zoom source of page 1 reaches OpenSeadragon
    await waitFor(() => {
      expect(viewers).toHaveLength(1);
      expect(viewers[0].opened).toContain("/api/v1/daofile/z1/tiles/image.dzi");
    });
    await expectNoA11yViolations(container);
  });

  it("turns pages from the toolbar and announces the position", async () => {
    renderViewer();
    await screen.findByRole("heading", { level: 1, name: "Kronika obce" });
    fireEvent.click(screen.getByRole("button", { name: "Next page" }));
    expect((screen.getByLabelText("Page") as HTMLInputElement).value).toBe("2");
    expect(screen.getByRole("status").textContent).toBe("Page 2 of 3");
    // a page without a pyramid opens as a plain image source
    await waitFor(() => {
      expect(viewers[0].opened).toContainEqual({ type: "image", url: "/api/v1/daofile/p2" });
    });
  });

  it("turns pages with the keyboard outside the canvas and the input", async () => {
    renderViewer();
    const heading = await screen.findByRole("heading", { level: 1, name: "Kronika obce" });
    fireEvent.keyDown(heading, { key: "ArrowRight" });
    expect((screen.getByLabelText("Page") as HTMLInputElement).value).toBe("2");
    fireEvent.keyDown(heading, { key: "End" });
    expect((screen.getByLabelText("Page") as HTMLInputElement).value).toBe("3");
    fireEvent.keyDown(heading, { key: "Home" });
    expect((screen.getByLabelText("Page") as HTMLInputElement).value).toBe("1");
    // inside the page-number input the arrows belong to the input
    fireEvent.keyDown(screen.getByLabelText("Page"), { key: "ArrowRight" });
    expect((screen.getByLabelText("Page") as HTMLInputElement).value).toBe("1");
  });

  it("honors the ?file= deep link and offers a download card for pages with no image", async () => {
    renderViewer("/apu/rec/dao/dao1?file=p3");
    await screen.findByRole("heading", { level: 1, name: "Kronika obce" });
    expect((screen.getByLabelText("Page") as HTMLInputElement).value).toBe("3");
    screen.getByText("This page has no preview.");
    const download = screen.getByRole("link", { name: "Download notes.pdf" });
    expect(download.getAttribute("href")).toBe("/api/v1/daofile/p3?download=true");
  });

  it("marks the current page in the thumbnail rail", async () => {
    renderViewer();
    await screen.findByRole("heading", { level: 1, name: "Kronika obce" });
    const rail = screen.getByRole("list", { name: "Page thumbnails" });
    const thumbs = rail.querySelectorAll("button");
    expect(thumbs).toHaveLength(3);
    expect(thumbs[0].getAttribute("aria-current")).toBe("true");
    fireEvent.click(thumbs[2]);
    expect(thumbs[2].getAttribute("aria-current")).toBe("true");
    expect(thumbs[0].getAttribute("aria-current")).toBeNull();
  });

  it("offers the old viewer's view controls: jumps, toggles and image settings", async () => {
    renderViewer();
    await screen.findByRole("heading", { level: 1, name: "Kronika obce" });
    // the ten-page jump clamps at the document's end
    fireEvent.click(screen.getByRole("button", { name: "Ten pages forward" }));
    expect((screen.getByLabelText("Page") as HTMLInputElement).value).toBe("3");
    // navigator and viewport lock are real toggles
    const navigator = screen.getByRole("button", { name: "Overview map" });
    fireEvent.click(navigator);
    expect(navigator.getAttribute("aria-pressed")).toBe("true");
    screen.getByRole("button", { name: "Keep the view between pages" });
    // the settings panel carries labelled sliders
    fireEvent.click(screen.getByRole("button", { name: "Image settings" }));
    await screen.findByRole("slider", { name: "Brightness" });
    screen.getByRole("slider", { name: "Contrast" });
    screen.getByRole("button", { name: "Reset" });
  });

  it("explains every control in one place - a finger gets no tooltip", async () => {
    renderViewer();
    await screen.findByRole("heading", { level: 1, name: "Kronika obce" });
    fireEvent.click(screen.getByRole("button", { name: "What the controls do" }));
    const help = await screen.findByRole("dialog", { name: "What the controls do" });
    const names = Array.from(help.querySelectorAll("li")).map((item) => item.textContent);
    expect(names).toContain("Next page");
    expect(names).toContain("Keep the view between pages");
    expect(names).toContain("Download this page");
    expect(names).toContain("Image settings");
    expect(help.textContent).toContain("Left and right arrows turn pages");
  });

  it("offers no browser fullscreen where the browser has none (jsdom, iPhone Safari)", async () => {
    renderViewer();
    await screen.findByRole("heading", { level: 1, name: "Kronika obce" });
    expect(screen.queryByRole("button", { name: "Full screen" })).toBeNull();
    // its own page is what the viewer is, so no link to it either
    expect(screen.queryByRole("link", { name: "Open on its own page" })).toBeNull();
  });

  it("takes the whole viewer into the browser's fullscreen and back", async () => {
    // jsdom has no Fullscreen API: a recording one, removed again afterwards
    let fullscreenElement: Element | null = null;
    const requestFullscreen = vi.fn(function (this: Element) {
      // the fake must remember which element asked, exactly as the browser does
      // eslint-disable-next-line @typescript-eslint/no-this-alias
      fullscreenElement = this;
      document.dispatchEvent(new Event("fullscreenchange"));
      return Promise.resolve();
    });
    const exitFullscreen = vi.fn(() => {
      fullscreenElement = null;
      document.dispatchEvent(new Event("fullscreenchange"));
      return Promise.resolve();
    });
    Object.defineProperty(document, "fullscreenEnabled", { configurable: true, value: true });
    Object.defineProperty(document, "fullscreenElement", {
      configurable: true,
      get: () => fullscreenElement,
    });
    Object.defineProperty(Element.prototype, "requestFullscreen", {
      configurable: true,
      value: requestFullscreen,
    });
    Object.defineProperty(document, "exitFullscreen", { configurable: true, value: exitFullscreen });
    try {
      renderViewer();
      await screen.findByRole("heading", { level: 1, name: "Kronika obce" });
      const toggle = screen.getByRole("button", { name: "Full screen" });
      expect(toggle.getAttribute("aria-pressed")).toBe("false");
      fireEvent.click(toggle);
      // the element that went fullscreen holds the whole viewer, not only the canvas
      expect(requestFullscreen).toHaveBeenCalledTimes(1);
      expect(fullscreenElement).not.toBeNull();
      expect(fullscreenElement!.contains(screen.getByRole("toolbar"))).toBe(true);
      expect(fullscreenElement!.contains(screen.getByRole("application"))).toBe(true);
      const exit = await screen.findByRole("button", { name: "Exit full screen" });
      expect(exit.getAttribute("aria-pressed")).toBe("true");
      fireEvent.click(exit);
      expect(exitFullscreen).toHaveBeenCalledTimes(1);
      await screen.findByRole("button", { name: "Full screen" });
    } finally {
      delete (document as { fullscreenEnabled?: boolean }).fullscreenEnabled;
      delete (document as { fullscreenElement?: Element | null }).fullscreenElement;
      delete (document as { exitFullscreen?: () => Promise<void> }).exitFullscreen;
      delete (Element.prototype as { requestFullscreen?: () => Promise<void> }).requestFullscreen;
    }
  });

  it("says so when the digital object does not exist", async () => {
    renderViewer("/apu/rec/dao/unknown");
    expect(await screen.findByText("The digital object was not found.")).toBeTruthy();
    screen.getByRole("link", { name: "Back to the record" });
  });
});
