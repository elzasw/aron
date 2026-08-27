import { act, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { MenuItemCode, type UiConfig } from "../api/generated";
import { expectNoA11yViolations } from "../test/a11y";
import { renderWithProviders } from "../test/render";
import { setViewportWidth } from "../test/viewport";
import AppHeader from "./AppHeader";

const config: UiConfig = {
  name: "Testovací portál",
  localizations: ["en", "cs_CZ"],
  menuItems: [
    { code: MenuItemCode.Fund },
    { code: MenuItemCode.ArchDesc, color: "#123456" },
    { code: MenuItemCode.Help, url: "https://help.example/" },
  ],
  citations: [],
  footerLinks: [],
};

vi.mock("../api/client", () => ({
  logoUrl: "/api/v1/ui/logo",
  uiApi: { uiGetConfig: vi.fn(() => Promise.resolve(config)) },
}));

function renderHeader(initialPath = "/") {
  return renderWithProviders(
    <>
      <AppHeader />
      <Routes>
        <Route index element={<h1>Home</h1>} />
        <Route path="fund" element={<h1>Fonds</h1>} />
      </Routes>
    </>,
    { initialEntries: [initialPath] },
  );
}

describe("AppHeader", () => {
  // jsdom's default 1024px is a desktop for the header; the compact tests
  // narrow it themselves
  beforeEach(() => setViewportWidth(1024));

  it("shows the configured sections as tabs on a wide viewport", async () => {
    const { container } = renderHeader();
    // the landmark exists before the configuration arrives; the tabs follow it
    expect(await screen.findByRole("link", { name: "Archival fonds" })).toHaveAttribute(
      "href",
      "/fund",
    );
    const nav = screen.getByRole("navigation", { name: "Main menu" });
    expect(screen.getByRole("link", { name: "Help" })).toHaveAttribute(
      "href",
      "https://help.example/",
    );
    expect(nav.querySelectorAll("a")).toHaveLength(3);
    expect(screen.queryByRole("button", { name: "Main menu" })).toBeNull();
    await expectNoA11yViolations(container);
  });

  it("folds the sections into one menu button on a narrow viewport", async () => {
    const user = userEvent.setup();
    setViewportWidth(600);
    const { container } = renderHeader("/fund");
    // the landmark stays; the tabs do not
    const nav = await screen.findByRole("navigation", { name: "Main menu" });
    expect(nav.querySelectorAll("a")).toHaveLength(0);
    // the language switcher keeps its place beside the button
    screen.getByRole("button", { name: /Language:/ });
    await expectNoA11yViolations(container);

    await user.click(screen.getByRole("button", { name: "Main menu" }));
    const items = await screen.findAllByRole("menuitem");
    expect(items.map((item) => item.textContent)).toEqual([
      "Archival fonds",
      "Archival records",
      "Help",
    ]);
    // the section the reader is in says so
    expect(screen.getByRole("menuitem", { name: "Archival fonds" })).toHaveAttribute(
      "aria-current",
      "page",
    );
    expect(screen.getByRole("menuitem", { name: "Help" })).toHaveAttribute(
      "href",
      "https://help.example/",
    );
  });

  it("navigates in-app from the compact menu and closes it", async () => {
    const user = userEvent.setup();
    setViewportWidth(600);
    renderHeader();
    await user.click(await screen.findByRole("button", { name: "Main menu" }));
    await user.click(await screen.findByRole("menuitem", { name: "Archival fonds" }));
    screen.getByRole("heading", { level: 1, name: "Fonds" });
    expect(screen.queryByRole("menuitem")).toBeNull();
  });

  it("switches between tabs and the menu button as the viewport changes", async () => {
    renderHeader();
    await screen.findByRole("link", { name: "Archival fonds" });
    act(() => setViewportWidth(600));
    await screen.findByRole("button", { name: "Main menu" });
    expect(screen.queryByRole("link", { name: "Archival fonds" })).toBeNull();
  });
});
