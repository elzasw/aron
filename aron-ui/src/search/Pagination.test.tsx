import { fireEvent, render, screen } from "@testing-library/react";
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

    for (const name of [
      "First page",
      "10 pages back",
      "Previous page",
      "Next page",
      "10 pages forward",
      "Last page",
    ]) {
      expect(screen.getByRole("button", { name })).toBeInTheDocument();
    }
    expect(screen.getByRole("button", { name: "Page 2" })).toHaveAttribute(
      "aria-current",
      "page",
    );
  });

  // 30 pages of ten
  function jumpFrom(page: number, name: string) {
    const onPage = vi.fn();
    const { unmount } = render(
      <Pagination page={page} size={10} total={300} onPage={onPage} onSize={vi.fn()} />,
    );
    fireEvent.click(screen.getByRole("button", { name }));
    unmount();
    return onPage;
  }

  it("skips ten pages back and forward", () => {
    expect(jumpFrom(15, "10 pages back")).toHaveBeenCalledWith(5);
    expect(jumpFrom(15, "10 pages forward")).toHaveBeenCalledWith(25);
  });

  it("lands on the end when it is nearer than a jump", () => {
    // the old portal asked for page 0 here
    expect(jumpFrom(3, "10 pages back")).toHaveBeenCalledWith(1);
    expect(jumpFrom(28, "10 pages forward")).toHaveBeenCalledWith(30);
  });

  it("offers no jump past the first or the last page", () => {
    const { unmount } = render(
      <Pagination page={1} size={10} total={300} onPage={vi.fn()} onSize={vi.fn()} />,
    );
    expect(screen.getByRole("button", { name: "10 pages back" })).toBeDisabled();
    expect(screen.getByRole("button", { name: "10 pages forward" })).toBeEnabled();
    unmount();

    render(<Pagination page={30} size={10} total={300} onPage={vi.fn()} onSize={vi.fn()} />);
    expect(screen.getByRole("button", { name: "10 pages back" })).toBeEnabled();
    expect(screen.getByRole("button", { name: "10 pages forward" })).toBeDisabled();
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
