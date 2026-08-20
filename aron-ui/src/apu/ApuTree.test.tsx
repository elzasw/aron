import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { TreeDirection, type TreeNode } from "../api/generated";
import i18n, { DEFAULT_LANGUAGE } from "../i18n";
import { expectNoA11yViolations } from "../test/a11y";
import ApuTree from "./ApuTree";

vi.mock("../api/client", () => ({
  apuApi: { apuGetTreeNodes: vi.fn() },
}));

const { apuApi } = await import("../api/client");

// a record in the middle of a large series: windows can be loaded on both sides
const FUND: TreeNode = { uuid: "fund", name: "Archive of the town", depth: 0, pos: 1, childCount: 10 };
const CURRENT: TreeNode = {
  uuid: "current",
  name: "Letter",
  // long enough that the pane can only ever draw part of it
  description: "Vaclav Kucera reports on the sale of the indebted Zvetcinov estate",
  depth: 1,
  pos: 6,
  childCount: 0,
};
const EARLIER: TreeNode = { uuid: "earlier", name: "Earlier letter", depth: 1, pos: 5, childCount: 0 };
const LATER: TreeNode = { uuid: "later", name: "Later letter", depth: 1, pos: 7, childCount: 0 };
/** The window "Load previous" brings in above EARLIER. */
const PRECEDING: TreeNode[] = [2, 3, 4].map((pos) => ({
  uuid: `preceding-${pos}`,
  name: `Letter ${pos}`,
  depth: 1,
  pos,
  childCount: 0,
}));

/** The fan-out the seed path triggers: siblings on both sides of the record. */
function serveFanOut() {
  vi.mocked(apuApi.apuGetTreeNodes).mockImplementation(({ uuid, direction }) => {
    if (uuid === CURRENT.uuid && direction === TreeDirection.Before) {
      return Promise.resolve([EARLIER]);
    }
    if (uuid === CURRENT.uuid && direction === TreeDirection.After) {
      return Promise.resolve([LATER]);
    }
    if (uuid === EARLIER.uuid && direction === TreeDirection.Before) {
      return Promise.resolve(PRECEDING);
    }
    return Promise.resolve([]);
  });
}

function renderTree() {
  return render(
    <MemoryRouter>
      <ApuTree treePath={[FUND, CURRENT]} currentUuid={CURRENT.uuid} />
    </MemoryRouter>,
  );
}

describe("ApuTree", () => {
  beforeEach(async () => {
    await i18n.changeLanguage(DEFAULT_LANGUAGE);
    serveFanOut();
  });

  // restoreMocks undoes the spies, but not properties defined on a prototype
  afterEach(() => {
    Reflect.deleteProperty(HTMLElement.prototype, "clientHeight");
    Reflect.deleteProperty(HTMLElement.prototype, "scrollTop");
    Reflect.deleteProperty(HTMLElement.prototype, "scrollHeight");
  });

  it("shows the record with the siblings it sits between", async () => {
    renderTree();
    await waitFor(() => expect(screen.getAllByRole("treeitem")).toHaveLength(4));
    const rows = screen.getAllByRole("treeitem");
    // tree order: the fund, then the record between the siblings surrounding it
    [FUND.name, EARLIER.name, CURRENT.description, LATER.name].forEach((label, index) => {
      expect(within(rows[index]).getByRole("button", { name: label })).toBeInTheDocument();
    });
  });

  // a node is drawn on one line, so the whole description has to stay reachable
  it("carries the full description as the node's tooltip", async () => {
    renderTree();
    const label = await screen.findByRole("button", { name: CURRENT.description });
    expect(label).toHaveAttribute("title", CURRENT.description);
  });

  it("marks the open record as the current one", async () => {
    renderTree();
    const label = await screen.findByRole("button", { name: CURRENT.description });
    expect(label).toHaveAttribute("aria-current", "page");
    expect(screen.getAllByRole("treeitem")[2]).toHaveAttribute("aria-selected", "true");
  });

  /**
   * jsdom has no layout engine, so the pane and the row are given their
   * geometry by hand; what this pins is the arithmetic and the target - the
   * pane's own scroll position, never the document's, which would drag the
   * header out of view.
   */
  it("brings the open record into the middle of the pane", async () => {
    const PANE_HEIGHT = 240;
    const ROW_TOP = 400;
    const ROW_HEIGHT = 24;
    const scrolled: number[] = [];
    vi.spyOn(HTMLElement.prototype, "getBoundingClientRect").mockImplementation(function (
      this: HTMLElement,
    ) {
      const isPane = this.querySelector('[role="tree"]') !== null;
      const top = isPane ? 0 : ROW_TOP;
      const height = isPane ? PANE_HEIGHT : ROW_HEIGHT;
      return { top, height, bottom: top + height } as DOMRect;
    });
    Object.defineProperty(HTMLElement.prototype, "clientHeight", {
      configurable: true,
      value: PANE_HEIGHT,
    });
    Object.defineProperty(HTMLElement.prototype, "scrollTop", {
      configurable: true,
      get: () => 0,
      set: (value: number) => scrolled.push(value),
    });
    const scrollPage = vi.spyOn(window, "scrollTo");

    renderTree();
    await screen.findByRole("button", { name: CURRENT.description });

    await waitFor(() => expect(scrolled).toEqual([ROW_TOP - (PANE_HEIGHT - ROW_HEIGHT) / 2]));
    expect(scrollPage).not.toHaveBeenCalled();
  });

  /**
   * The pane's height is faked from the number of rows, so inserting above the
   * reader really does grow the content: what this pins is that the reader keeps
   * their place instead of being pushed down by the new rows.
   */
  it("keeps the reader's place when earlier siblings arrive", async () => {
    const ROW = 24;
    let scrollTop = 0;
    Object.defineProperty(HTMLElement.prototype, "scrollHeight", {
      configurable: true,
      get(this: HTMLElement) {
        return ROW * this.querySelectorAll('[role="treeitem"]').length;
      },
    });
    Object.defineProperty(HTMLElement.prototype, "scrollTop", {
      configurable: true,
      get: () => scrollTop,
      set: (value: number) => {
        scrollTop = value;
      },
    });
    const user = userEvent.setup();
    render(
      <MemoryRouter>
        <ApuTree treePath={[FUND, CURRENT]} currentUuid={CURRENT.uuid} />
      </MemoryRouter>,
    );
    await screen.findByRole("button", { name: "Load previous" });

    // the reader has scrolled down before asking for the earlier window
    scrollTop = 4 * ROW;
    await user.click(screen.getByRole("button", { name: "Load previous" }));

    await waitFor(() => expect(screen.getAllByRole("treeitem")).toHaveLength(7));
    expect(scrollTop).toBe(4 * ROW + PRECEDING.length * ROW);
  });

  it("has no accessibility violations", async () => {
    const { container } = renderTree();
    await screen.findByRole("button", { name: CURRENT.description });
    await expectNoA11yViolations(container);
  });
});
