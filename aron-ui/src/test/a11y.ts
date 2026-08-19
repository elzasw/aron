import axe from "axe-core";
import { expect } from "vitest";

/**
 * Runs axe over rendered markup and fails with the findings. This catches the
 * structural half of accessibility - roles, names, relationships, duplicate
 * ids - on every test run, which is where regressions actually appear.
 *
 * Colour contrast is never checked here: jsdom has no layout engine, so the
 * rule cannot produce a truthful answer. Contrast and reflow belong to the
 * browser pass (doc/accessibility.md §4, Phase C).
 *
 * @param disabledRules rules that do not apply to a fragment rendered on its
 *                      own, e.g. `region` when the test renders one component
 *                      rather than a whole page
 */
export async function expectNoA11yViolations(
  container: HTMLElement,
  disabledRules: string[] = [],
): Promise<void> {
  const rules: Record<string, { enabled: boolean }> = { "color-contrast": { enabled: false } };
  for (const rule of disabledRules) {
    rules[rule] = { enabled: false };
  }

  const results = await axe.run(container, { rules });
  const findings = results.violations.map(
    (violation) =>
      `${violation.id}: ${violation.help} [${violation.nodes.map((node) => node.target.join(" ")).join(", ")}]`,
  );
  expect(findings, "axe findings").toEqual([]);
}
