import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { expectNoA11yViolations } from "../test/a11y";
import Pagination from "./Pagination";

function renderPagination() {
  return render(
    <Pagination page={2} size={10} total={95} onPage={vi.fn()} onSize={vi.fn()} />,
  );
}

describe("Pagination", () => {
  it("names the page-size dropdown by its visible label", () => {
    renderPagination();

    // Fluent's trigger button has no name of its own - a real Lighthouse
    // finding on the deployed portal
    expect(screen.getByRole("combobox")).toHaveAccessibleName("Per page:");
  });

  it("names every pager control and marks the current page", () => {
    renderPagination();

    for (const name of ["First page", "Previous page", "Next page", "Last page"]) {
      expect(screen.getByRole("button", { name })).toBeInTheDocument();
    }
    expect(screen.getByRole("button", { name: "Page 2" })).toHaveAttribute(
      "aria-current",
      "page",
    );
  });

  it("stands twice on a page without the two being mistaken for each other", () => {
    render(
      <>
        <Pagination page={2} size={10} total={95} onPage={vi.fn()} onSize={vi.fn()} />
        <Pagination page={2} size={10} total={95} onPage={vi.fn()} onSize={vi.fn()} label="Below" />
      </>,
    );

    // one landmark name each, so a screen reader's landmark list tells them apart
    expect(screen.getByRole("navigation", { name: "Result pagination" })).toBeInTheDocument();
    expect(screen.getByRole("navigation", { name: "Below" })).toBeInTheDocument();
    // and each dropdown is named by its own label: axe reports a shared id only
    // as something to review, so nothing else would notice one
    const sizes = screen.getAllByRole("combobox");
    expect(sizes).toHaveLength(2);
    for (const size of sizes) {
      expect(size).toHaveAccessibleName("Per page:");
    }
    expect(sizes[0].getAttribute("aria-labelledby")).not.toBe(
      sizes[1].getAttribute("aria-labelledby"),
    );
  });

  it("has no structural accessibility violations", async () => {
    const { container } = renderPagination();

    // "region" applies to a whole page, not to one rendered bar
    await expectNoA11yViolations(container, ["region"]);
  });
});
