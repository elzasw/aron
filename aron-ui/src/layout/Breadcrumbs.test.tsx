import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, within } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApuType, type ApuDetail, type UiConfig } from "../api/generated";
import i18n, { DEFAULT_LANGUAGE } from "../i18n";
import ApuPage from "../pages/ApuPage";
import { expectNoA11yViolations } from "../test/a11y";
import AppLayout from "./AppLayout";

const RECORD_NAME = "Vaclav Kucera reports on the sale of the estate";

/** A record two levels down its fund - the trail has something to show. */
const RECORD: ApuDetail = {
  uuid: "record",
  name: RECORD_NAME,
  apuType: ApuType.ArchDesc,
  childCount: 0,
  treePath: [
    { uuid: "fund", name: "Archive of the town of Pardubice", depth: 0, pos: 1, childCount: 3 },
    { uuid: "series", name: "III. Files", depth: 1, pos: 3, childCount: 1 },
    { uuid: "record", name: RECORD_NAME, depth: 2, pos: 1, childCount: 0 },
  ],
  parts: [],
  attachments: [],
  digitalObjects: [],
};

const config: UiConfig = {
  name: "Test portal",
  localizations: ["en"],
  menuItems: [],
  footerLinks: [],
};

vi.mock("../api/client", () => ({
  logoUrl: "/api/v1/ui/logo",
  systemApi: { systemGetInfo: vi.fn(() => Promise.resolve({ name: "aron", version: "1.0" })) },
  uiApi: { uiGetConfig: vi.fn(() => Promise.resolve(config)) },
  apuApi: {
    apuGetDetail: vi.fn(() => Promise.resolve(RECORD)),
    apuGetTreeNodes: vi.fn(() => Promise.resolve([])),
  },
}));

function renderAt(path: string) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[path]}>
        <Routes>
          <Route element={<AppLayout />}>
            <Route path="fund" element={<h1>Archival fonds</h1>} />
            <Route path="apu/:uuid" element={<ApuPage />} />
          </Route>
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

const trail = () => screen.getByRole("navigation", { name: "Breadcrumb" });

describe("Breadcrumbs", () => {
  beforeEach(async () => {
    await i18n.changeLanguage(DEFAULT_LANGUAGE);
  });

  it("names the section the reader is in", () => {
    renderAt("/fund");
    expect(within(trail()).getByRole("link", { name: "Home" })).toBeInTheDocument();
    expect(within(trail()).getByText("Archival fonds")).toHaveAttribute("aria-current", "page");
  });

  // one location, one trail: the record's place in the archival description
  // continues the strip instead of repeating it inside the page
  it("continues into the record's place in the description", async () => {
    renderAt("/apu/record");
    await screen.findByRole("heading", { level: 1, name: RECORD_NAME });

    expect(screen.getAllByRole("navigation", { name: "Breadcrumb" })).toHaveLength(1);
    expect(within(trail()).getAllByRole("link").map((link) => link.textContent)).toEqual([
      "Home",
      "Archival records",
      "Archive of the town of Pardubice",
      "III. Files",
    ]);
    expect(within(trail()).getByText(RECORD_NAME)).toHaveAttribute("aria-current", "page");
  });

  it("has no accessibility violations", async () => {
    const { container } = renderAt("/apu/record");
    await screen.findByRole("heading", { level: 1, name: RECORD_NAME });
    await expectNoA11yViolations(container);
  });
});
