import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { FooterLinkCode, type UiConfig } from "../api/generated";
import i18n, { DEFAULT_LANGUAGE } from "../i18n";
import AppLayout from "./AppLayout";

const config: UiConfig = {
  name: "Testovací portál",
  localizations: ["cs_CZ"],
  menuItems: [],
  footerLinks: [
    { code: FooterLinkCode.Accessibility, url: "https://archiv.example/pristupnost" },
    { label: "Kontakt", url: "https://archiv.example/kontakt" },
  ],
};

vi.mock("../api/client", () => ({
  logoUrl: "/api/v1/ui/logo",
  // the implementation is passed to vi.fn() rather than set with
  // mockResolvedValue: restoreMocks (vitest.config.ts) clears the latter
  // between tests, leaving the query with no data
  systemApi: { systemGetInfo: vi.fn(() => Promise.resolve({ name: "aron2", version: "1.0" })) },
  uiApi: { uiGetConfig: vi.fn(() => Promise.resolve(config)) },
}));

function renderLayout(initialPath = "/") {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[initialPath]}>
        <Routes>
          <Route element={<AppLayout />}>
            <Route index element={<h1>Úvod</h1>} />
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
    const skipLink = screen.getByRole("link", { name: "Přejít na hlavní obsah" });
    expect(skipLink).toHaveFocus();

    await user.keyboard("{Enter}");
    expect(screen.getByRole("main")).toHaveFocus();
  });

  it("publishes the deployment's footer links, labelling the well-known ones itself", async () => {
    renderLayout();

    // the accessibility statement carries no server label - the UI names it
    expect(
      await screen.findByRole("link", { name: "Prohlášení o přístupnosti" }),
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
});
