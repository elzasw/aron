import { render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import {
  ApuType,
  type DatingFacetResult,
  type FacetDef,
  FacetDisplay,
  FacetResultKind,
  FacetType,
} from "../api/generated";
import i18n, { DEFAULT_LANGUAGE } from "../i18n";
import { expectNoA11yViolations } from "../test/a11y";
import FacetPanel from "./FacetPanel";

function facet(code: string, type: FacetType, label: string): FacetDef {
  return { code, type, label, display: FacetDisplay.Always };
}

function renderFacet(def: FacetDef, result?: DatingFacetResult) {
  return render(
    <FacetPanel
      def={def}
      filters={[]}
      result={result}
      apuType={ApuType.Fund}
      query=""
      onFilters={vi.fn()}
    />,
  );
}

describe("FacetPanel", () => {
  beforeEach(async () => {
    await i18n.changeLanguage(DEFAULT_LANGUAGE);
  });

  it("names a text filter by its facet, not by a placeholder alone", () => {
    renderFacet(facet("TITLE~MAIN", FacetType.Fulltext, "Fonds name"));

    // a placeholder disappears once the user types - the control needs a name
    expect(screen.getByRole("textbox", { name: "Fonds name" })).toBeInTheDocument();
  });

  it("heads every facet, so a screen reader can move between them", () => {
    renderFacet(facet("LANG~CODE", FacetType.Enum, "Language"));

    expect(screen.getByRole("heading", { level: 2, name: "Language" })).toBeInTheDocument();
    expect(screen.getByRole("group")).toHaveAccessibleName("Language");
  });

  it("names both ends of the dating range, slider and field alike", () => {
    const bounds: DatingFacetResult = {
      kind: FacetResultKind.Dating,
      code: "UNIT~DATE",
      bounds: { minYear: 1201, maxYear: 1961 },
    };
    renderFacet(facet("UNIT~DATE", FacetType.Unitdate, "Dating"), bounds);

    expect(screen.getByRole("slider", { name: "Dating – slider from year" })).toHaveValue("1201");
    expect(screen.getByRole("slider", { name: "Dating – slider to year" })).toHaveValue("1961");
    // the year fields are numeric and carry their own names
    expect(screen.getByRole("spinbutton", { name: "Dating – from year" })).toBeInTheDocument();
    expect(screen.getByRole("spinbutton", { name: "Dating – to year" })).toBeInTheDocument();
  });

  it.each([
    ["text", facet("TITLE~MAIN", FacetType.Fulltext, "Title")],
    ["enum", facet("LANG~CODE", FacetType.Enum, "Language")],
    ["dating", facet("UNIT~DATE", FacetType.Unitdate, "Dating")],
  ])("has no structural accessibility violations (%s facet)", async (_kind, def) => {
    const { container } = renderFacet(
      def,
      def.type === FacetType.Unitdate
        ? {
            kind: FacetResultKind.Dating,
            code: def.code,
            bounds: { minYear: 1201, maxYear: 1961 },
          }
        : undefined,
    );

    // "region" only makes sense for a whole page, not for one rendered facet
    await expectNoA11yViolations(container, ["region"]);
  });
});
