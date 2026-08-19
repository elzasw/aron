import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it } from "vitest";
import i18n, { DEFAULT_LANGUAGE } from "../i18n";
import LanguageSwitcher from "./LanguageSwitcher";

describe("LanguageSwitcher", () => {
  beforeEach(async () => {
    window.localStorage.clear();
    await i18n.changeLanguage(DEFAULT_LANGUAGE);
  });

  it("is not rendered when the deployment offers a single language", () => {
    const { container } = render(<LanguageSwitcher localizations={["cs_CZ"]} />);

    expect(container).toBeEmptyDOMElement();
  });

  it("offers only languages the deployment declares AND the build has", () => {
    render(<LanguageSwitcher localizations={["cs_CZ", "en", "de_DE"]} />);

    expect(screen.getAllByRole("button").map((button) => button.textContent)).toEqual(["CS", "EN"]);
  });

  it("marks the active language as pressed", () => {
    render(<LanguageSwitcher localizations={["cs_CZ", "en"]} />);

    expect(screen.getByRole("button", { name: "Čeština (CS)" })).toHaveAttribute("aria-pressed", "true");
    expect(screen.getByRole("button", { name: "English (EN)" })).toHaveAttribute("aria-pressed", "false");
  });

  it("names each language in the language itself, for screen readers", () => {
    render(<LanguageSwitcher localizations={["cs_CZ", "en"]} />);

    // the lang attribute makes a screen reader pronounce the name correctly
    expect(screen.getByRole("button", { name: "English (EN)" })).toHaveAttribute("lang", "en");
    expect(screen.getByRole("group")).toHaveAccessibleName("Jazyk");
  });

  it("keeps the visible code inside the accessible name (WCAG 2.5.3)", () => {
    render(<LanguageSwitcher localizations={["cs_CZ", "en"]} />);

    // speech input addresses a control by what it shows ("click CS"), so the
    // spelled-out name must contain that text - a real Lighthouse finding
    for (const code of ["CS", "EN"]) {
      const button = screen.getByText(code);
      expect(button).toHaveAccessibleName(expect.stringContaining(code));
    }
  });

  it("switches the language on click", async () => {
    const user = userEvent.setup();
    render(<LanguageSwitcher localizations={["cs_CZ", "en"]} />);

    await user.click(screen.getByRole("button", { name: "English (EN)" }));

    expect(i18n.language).toBe("en");
    // the control itself re-renders in the new language
    expect(screen.getByRole("group")).toHaveAccessibleName("Language");
  });

  it("is operable from the keyboard alone", async () => {
    const user = userEvent.setup();
    render(<LanguageSwitcher localizations={["cs_CZ", "en"]} />);

    await user.tab();
    expect(screen.getByRole("button", { name: "Čeština (CS)" })).toHaveFocus();
    await user.tab();
    expect(screen.getByRole("button", { name: "English (EN)" })).toHaveFocus();

    await user.keyboard("{Enter}");
    expect(i18n.language).toBe("en");
  });

  it("falls back when the reader's language is not offered here", async () => {
    await i18n.changeLanguage("en");

    // this deployment offers Czech only: showing English chrome would promise
    // text the server will not render in English
    render(<LanguageSwitcher localizations={["cs_CZ"]} />);

    expect(i18n.language).toBe("cs");
  });
});
