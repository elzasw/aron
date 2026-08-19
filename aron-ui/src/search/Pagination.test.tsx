import { render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import i18n, { DEFAULT_LANGUAGE } from "../i18n";
import { expectNoA11yViolations } from "../test/a11y";
import Pagination from "./Pagination";

function renderPagination() {
  return render(
    <Pagination page={2} size={10} total={95} onPage={vi.fn()} onSize={vi.fn()} />,
  );
}

describe("Pagination", () => {
  beforeEach(async () => {
    await i18n.changeLanguage(DEFAULT_LANGUAGE);
  });

  it("names the page-size dropdown by its visible label", () => {
    renderPagination();

    // Fluent's trigger button has no name of its own - a real Lighthouse
    // finding on the deployed portal
    expect(screen.getByRole("combobox")).toHaveAccessibleName("Počet na stránce:");
  });

  it("names every pager control and marks the current page", () => {
    renderPagination();

    for (const name of ["První stránka", "Předchozí stránka", "Další stránka", "Poslední stránka"]) {
      expect(screen.getByRole("button", { name })).toBeInTheDocument();
    }
    expect(screen.getByRole("button", { name: "Stránka 2" })).toHaveAttribute(
      "aria-current",
      "page",
    );
  });

  it("has no structural accessibility violations", async () => {
    const { container } = renderPagination();

    // "region" applies to a whole page, not to one rendered bar
    await expectNoA11yViolations(container, ["region"]);
  });
});
