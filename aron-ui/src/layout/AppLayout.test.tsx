import { cleanup, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { Link, MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { FooterLinkCode, type UiConfig } from "../api/generated";
import i18n, { DEFAULT_LANGUAGE } from "../i18n";
import { expectNoA11yViolations } from "../test/a11y";
import AppLayout from "./AppLayout";

const footerColumns = {
  columns: [
    {
      heading: "Základní informace",
      paragraphs: [
        {
          runs: [
            { text: "Portál je aplikace " },
            { text: "Testovacího archivu", url: "http://archiv.test.example" },
            { text: "." },
          ],
        },
      ],
      links: [],
    },
  ],
};

const config: UiConfig = {
  name: "Testovací portál",
  // a bilingual deployment: the chrome stays in the source language, so the
  // assertions below read as the strings the code ships
  localizations: ["en", "cs_CZ"],
  menuItems: [],
  footerLinks: [
    { code: FooterLinkCode.Accessibility, url: "https://archiv.example/pristupnost" },
    { label: "Kontakt", url: "https://archiv.example/kontakt" },
  ],
  homePage: { groups: [], footer: footerColumns },
};

// the implementation is passed to vi.fn() rather than set with
// mockResolvedValue: restoreMocks (vitest.config.ts) clears the latter between
// tests, leaving the query with no data
const systemGetInfo = vi.fn<() => Promise<{ name: string; version?: string }>>(() =>
  Promise.resolve({ name: "aron", version: "1.0" }),
);

vi.mock("../api/client", () => ({
  logoUrl: "/api/v1/ui/logo",
  systemApi: { systemGetInfo: () => systemGetInfo() },
  uiApi: { uiGetConfig: vi.fn(() => Promise.resolve(config)) },
}));

function renderLayout(initialPath = "/") {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[initialPath]}>
        <Routes>
          <Route element={<AppLayout />}>
            <Route
              index
              element={
                <>
                  <h1>Úvod</h1>
                  {/* an in-app navigation to drive the route-change behavior */}
                  <Link to="/apu">Vyhledávání</Link>
                </>
              }
            />
            <Route path="apu" element={<h1>Vyhledávání</h1>} />
          </Route>
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe("AppLayout", () => {
  beforeEach(async () => {
    await i18n.changeLanguage(DEFAULT_LANGUAGE);
  });

  it("lets a keyboard user skip the repeated header (WCAG 2.4.1)", async () => {
    const user = userEvent.setup();
    renderLayout();

    // the skip link is the first thing Tab reaches
    await user.tab();
    const skipLink = screen.getByRole("link", { name: "Skip to main content" });
    expect(skipLink).toHaveFocus();

    await user.keyboard("{Enter}");
    expect(screen.getByRole("main")).toHaveFocus();
  });

  it("starts a new page at its top instead of scrolling the header away", async () => {
    const user = userEvent.setup();
    // jsdom implements no scrolling; the spy is also the assertion
    const scrollTo = vi.spyOn(window, "scrollTo").mockImplementation(() => {});
    renderLayout();
    const main = screen.getByRole("main");
    const focus = vi.spyOn(main, "focus");

    await user.click(screen.getByRole("link", { name: "Vyhledávání" }));

    expect(screen.getByRole("heading", { level: 1, name: "Vyhledávání" })).toBeInTheDocument();
    // the reader lands at the top of the new page...
    expect(scrollTo).toHaveBeenCalledWith({ top: 0 });
    // ...and the focus that makes a screen reader read it must not scroll on its
    // own, which would put the main region's top at the viewport top
    expect(focus).toHaveBeenCalledWith({ preventScroll: true });
    expect(main).toHaveFocus();
  });

  it("keeps the deployment's own columns inside the page's one footer", async () => {
    renderLayout();

    // the footer element exists before the configuration arrives, so wait for
    // the configured content rather than for the landmark
    const inline = await screen.findByRole("link", { name: "Testovacího archivu" });
    // one footer, not a band above it: the columns a deployment configures and
    // the links it must publish are parts of the same contentinfo
    const footer = screen.getByRole("contentinfo");
    expect(screen.getAllByRole("contentinfo")).toHaveLength(1);
    expect(footer.textContent).toContain("Portál je aplikace Testovacího archivu.");
    // a link inside the prose is a real anchor, not configured markup
    expect(inline).toHaveAttribute("href", "http://archiv.test.example");
    // the columns need no landmark of their own inside the footer
    expect(screen.queryByRole("region", { name: "Základní informace" })).toBeNull();
  });

  it("carries the columns only where they are configured for", async () => {
    renderLayout("/apu");

    // every page keeps the links it must publish...
    await screen.findByRole("link", { name: "Accessibility statement" });
    const footer = screen.getByRole("contentinfo");
    // ...but not the columns: each row of footer comes out of the routed
    // content, and the record detail cannot spare it
    expect(footer.textContent).not.toContain("Portál je aplikace");
  });

  it("shows the running version only when the deployment discloses it", async () => {
    renderLayout();
    expect(await screen.findByText(/aron/)).toBeTruthy();

    // withheld, the server sends no version at all - so there is nothing to show
    systemGetInfo.mockImplementationOnce(() => Promise.resolve({ name: "aron" }));
    cleanup();
    renderLayout();
    await screen.findByRole("link", { name: "Accessibility statement" });
    expect(screen.queryByText(/aron/)).toBeNull();
  });

  it("publishes the deployment's footer links, labelling the well-known ones itself", async () => {
    renderLayout();

    // the accessibility statement carries no server label - the UI names it
    expect(
      await screen.findByRole("link", { name: "Accessibility statement" }),
    ).toHaveAttribute("href", "https://archiv.example/pristupnost");
    // a free link keeps the label the deployment configured
    expect(screen.getByRole("link", { name: "Kontakt" })).toHaveAttribute(
      "href",
      "https://archiv.example/kontakt",
    );
  });

  it("keeps a live region for API errors mounted, so a later failure is announced", () => {
    renderLayout();

    // an empty region that already exists is what makes an addition announceable
    expect(screen.getByRole("alert")).toBeEmptyDOMElement();
  });

  it("has no structural accessibility violations", async () => {
    const { container } = renderLayout();
    await screen.findByRole("link", { name: "Accessibility statement" });

    await expectNoA11yViolations(container);
  });
});
