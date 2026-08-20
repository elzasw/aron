import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { useState } from "react";
import { describe, expect, it, vi } from "vitest";
import { expectNoA11yViolations } from "../test/a11y";
import Splitter from "./Splitter";

/** The page owns the width, so the harness does what ApuPage does. */
function Harness({ onCommit }: { onCommit?: (value: number) => void }) {
  const [width, setWidth] = useState(320);
  return (
    <>
      <Splitter
        label="Tree width"
        value={width}
        min={200}
        max={640}
        onChange={setWidth}
        onCommit={onCommit}
      />
      <output>{width}</output>
    </>
  );
}

const separator = () => screen.getByRole("separator", { name: "Tree width" });

describe("Splitter", () => {
  it("publishes the width it controls", () => {
    render(<Harness />);
    expect(separator()).toHaveAttribute("aria-orientation", "vertical");
    expect(separator()).toHaveAttribute("aria-valuenow", "320");
    expect(separator()).toHaveAttribute("aria-valuemin", "200");
    expect(separator()).toHaveAttribute("aria-valuemax", "640");
  });

  // resizing must never depend on a drag: the keyboard is the whole point of
  // the focusable separator (WCAG 2.1.1, 2.5.7)
  it("resizes from the keyboard", async () => {
    const user = userEvent.setup();
    render(<Harness />);
    await user.tab();
    expect(separator()).toHaveFocus();

    await user.keyboard("{ArrowRight}");
    expect(separator()).toHaveAttribute("aria-valuenow", "344");
    await user.keyboard("{ArrowLeft}{ArrowLeft}");
    expect(separator()).toHaveAttribute("aria-valuenow", "296");
  });

  it("keeps the width within its bounds", async () => {
    const user = userEvent.setup();
    render(<Harness />);
    await user.tab();

    await user.keyboard("{Home}{ArrowLeft}");
    expect(separator()).toHaveAttribute("aria-valuenow", "200");
    await user.keyboard("{End}{ArrowRight}");
    expect(separator()).toHaveAttribute("aria-valuenow", "640");
  });

  it("reports the width the reader settled on", async () => {
    const user = userEvent.setup();
    const onCommit = vi.fn();
    render(<Harness onCommit={onCommit} />);
    await user.tab();

    await user.keyboard("{ArrowRight}");
    expect(onCommit).toHaveBeenCalledWith(344);
  });

  it("has no accessibility violations", async () => {
    const { container } = render(<Harness />);
    await expectNoA11yViolations(container);
  });
});
