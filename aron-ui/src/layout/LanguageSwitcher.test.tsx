import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it } from "vitest";
import i18n from "../i18n";
import { expectNoA11yViolations } from "../test/a11y";
import LanguageSwitcher from "./LanguageSwitcher";

/** Opens the language menu and returns its options. */
async function openMenu(user: ReturnType<typeof userEvent.setup>) {
  await user.click(screen.getByRole("button"));
  return screen.findAllByRole("menuitemradio");
}

describe("LanguageSwitcher", () => {
  beforeEach(async () => {
    window.localStorage.clear();
  });

  it("is not rendered when the deployment offers a single language", () => {
    const { container } = render(<LanguageSwitcher localizations={["cs_CZ"]} />);

    expect(container).toBeEmptyDOMElement();
  });

  it("offers only languages the deployment declares AND the build has", async () => {
    const user = userEvent.setup();
    render(<LanguageSwitcher localizations={["cs_CZ", "en", "de_DE"]} />);

    const options = await openMenu(user);
    expect(options.map((option) => option.textContent)).toEqual(["ČeštinaCS", "EnglishEN"]);
  });

  it("says which language is in effect, in the menu and on the trigger", async () => {
    const user = userEvent.setup();
    render(<LanguageSwitcher localizations={["cs_CZ", "en"]} />);

    // the trigger shows the short code, the menu the language itself
    expect(screen.getByRole("button")).toHaveTextContent("EN");

    // the deployment lists Czech first, so it is the first option - English is
    // the one in effect
    const options = await openMenu(user);
    expect(options[0]).toHaveAttribute("aria-checked", "false");
    expect(options[1]).toHaveAttribute("aria-checked", "true");
  });

  it("keeps the visible code inside the trigger's accessible name (WCAG 2.5.3)", () => {
    render(<LanguageSwitcher localizations={["cs_CZ", "en"]} />);

    // speech input addresses a control by what it shows ("click EN")
    expect(screen.getByRole("button")).toHaveAccessibleName("Language: English (EN)");
  });

  it("names each language in the language itself, for screen readers", async () => {
    const user = userEvent.setup();
    render(<LanguageSwitcher localizations={["cs_CZ", "en"]} />);

    await openMenu(user);
    // the lang attribute makes a screen reader pronounce the name correctly
    expect(screen.getByText("English")).toHaveAttribute("lang", "en");
    expect(screen.getByText("Čeština")).toHaveAttribute("lang", "cs");
  });

  it("switches the language on selection", async () => {
    const user = userEvent.setup();
    render(<LanguageSwitcher localizations={["cs_CZ", "en"]} />);

    const options = await openMenu(user);
    await user.click(options[0]);

    expect(i18n.language).toBe("cs");
    // the control itself re-renders in the new language
    expect(screen.getByRole("button")).toHaveAccessibleName("Jazyk: Čeština (CS)");
  });

  it("is reachable and opens from the keyboard alone", async () => {
    const user = userEvent.setup();
    render(<LanguageSwitcher localizations={["cs_CZ", "en"]} />);

    await user.tab();
    expect(screen.getByRole("button")).toHaveFocus();

    await user.keyboard("{Enter}");

    expect(await screen.findAllByRole("menuitemradio")).toHaveLength(2);
    // walking the options themselves is Fluent's roving focus (tabster), which
    // needs a real browser - jsdom does not run it. Verified in the browser
    // pass instead (doc/accessibility.md §4, Phase C).
  });

  it("falls back when the reader's language is not offered here", async () => {
    // this deployment offers Czech only: showing English chrome would promise
    // text the server will not render in English
    render(<LanguageSwitcher localizations={["cs_CZ"]} />);

    await waitFor(() => expect(i18n.language).toBe("cs"));
  });

  it("has no structural accessibility violations, open or closed", async () => {
    const user = userEvent.setup();
    const { container, baseElement } = render(<LanguageSwitcher localizations={["cs_CZ", "en"]} />);

    await expectNoA11yViolations(container, ["region"]);

    // the popover renders in a portal, so the whole document is the subject
    await openMenu(user);
    await expectNoA11yViolations(baseElement as HTMLElement, ["region"]);
  });
});
