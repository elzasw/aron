import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import {
  ApuType,
  type DatingFacetResult,
  type EnumFacetResult,
  type FacetDef,
  FacetDisplay,
  FacetResultKind,
  FacetType,
  FilterKind,
  type RangeFilter,
  type SearchFilter,
} from "../api/generated";
import i18n from "../i18n";
import { expectNoA11yViolations } from "../test/a11y";
import FacetPanel from "./FacetPanel";
import { DATE_FACET, TYPE_FACET } from "./filters";

function facet(code: string, type: FacetType, label: string): FacetDef {
  return { code, type, label, display: FacetDisplay.Always };
}

function renderFacet(
  def: FacetDef,
  result?: DatingFacetResult | EnumFacetResult,
  filters: SearchFilter[] = [],
  onFilters = vi.fn(),
) {
  return {
    onFilters,
    ...render(
      <FacetPanel
        def={def}
        filters={filters}
        result={result}
        apuType={ApuType.Fund}
        query=""
        onFilters={onFilters}
      />,
    ),
  };
}

/** A dating range the reader has applied, as the URL carries it. */
function appliedRange(facetCode: string, includeUndated = false): SearchFilter[] {
  const filter: RangeFilter = { kind: FilterKind.Range, facet: facetCode, from: "1805", to: "1852" };
  if (includeUndated) {
    filter.includeUndated = true;
  }
  return [filter];
}

function dating(code: string, undatedCount?: number): DatingFacetResult {
  return {
    kind: FacetResultKind.Dating,
    code,
    bounds: { minYear: 1201, maxYear: 1961 },
    undatedCount,
  };
}

describe("FacetPanel", () => {
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

  it("offers the undated records only once a dating filter would drop them", async () => {
    const def = facet(DATE_FACET, FacetType.Unitdate, "Dating");

    // no dating filter: the undated records are in the results already, so there
    // is nothing to include and no control to explain
    renderFacet(def, dating(DATE_FACET, 42), []).unmount();
    expect(screen.queryByRole("checkbox")).not.toBeInTheDocument();

    // a filter is on, but every matching record is dated - still nothing to offer
    renderFacet(def, dating(DATE_FACET, 0), appliedRange(DATE_FACET)).unmount();
    expect(screen.queryByRole("checkbox")).not.toBeInTheDocument();

    // both: the reader is told how many records the filter costs them, and can
    // put them back
    const { onFilters } = renderFacet(def, dating(DATE_FACET, 42), appliedRange(DATE_FACET));
    const checkbox = screen.getByRole("checkbox", {
      name: "Include 42 records without dating",
    });
    expect(checkbox).not.toBeChecked();

    await userEvent.click(checkbox);
    expect(onFilters).toHaveBeenCalledWith([
      { kind: FilterKind.Range, facet: DATE_FACET, from: "1805", to: "1852", includeUndated: true },
    ]);
  });

  it("counts the undated records in the reader's own grammar", async () => {
    // Czech needs three plural forms where English needs two; a single string
    // with the number glued in would read right only by accident
    await i18n.changeLanguage("cs");
    renderFacet(
      facet(DATE_FACET, FacetType.Unitdate, "Datace"),
      dating(DATE_FACET, 1),
      appliedRange(DATE_FACET),
    );

    expect(
      screen.getByRole("checkbox", { name: "Zahrnout 1 záznam bez datace" }),
    ).toBeInTheDocument();
  });

  it("explains an option to a screen reader, not only to a mouse", () => {
    // the old portal put this on a hover-only div, which never reached a
    // keyboard or a screen reader
    const def: FacetDef = {
      ...facet("UNIT~TYPE", FacetType.Enum, "Kind of material"),
      optionTooltips: [
        { value: "technický výkres", tooltip: "technical drawings of buildings and products" },
      ],
    };
    const buckets: EnumFacetResult = {
      kind: FacetResultKind.Enum,
      code: "UNIT~TYPE",
      buckets: [
        { value: "technický výkres", count: 4 },
        { value: "matrika", count: 9 },
      ],
    };
    renderFacet(def, buckets);

    expect(screen.getByRole("checkbox", { name: /technický výkres/ })).toHaveAccessibleDescription(
      "technical drawings of buildings and products",
    );
    // an option the deployment says nothing about gets no description
    expect(screen.getByRole("checkbox", { name: /matrika/ })).not.toHaveAccessibleDescription();
  });

  it("names the record types of the built-in type facet", () => {
    // the search response carries the ApuType member; this UI already names those
    const buckets: EnumFacetResult = {
      kind: FacetResultKind.Enum,
      code: TYPE_FACET,
      buckets: [
        { value: "FUND", count: 12 },
        { value: "ARCH_DESC", count: 3 },
      ],
    };
    renderFacet(facet(TYPE_FACET, FacetType.Enum, "Record type"), buckets);

    expect(screen.getByRole("checkbox", { name: /Archival fonds/ })).toBeInTheDocument();
    expect(screen.getByRole("checkbox", { name: /Archival records/ })).toBeInTheDocument();
  });

  it.each([
    ["text", facet("TITLE~MAIN", FacetType.Fulltext, "Title")],
    ["enum", facet("LANG~CODE", FacetType.Enum, "Language")],
    ["dating", facet("UNIT~DATE", FacetType.Unitdate, "Dating")],
    ["dating with the undated offer", facet(DATE_FACET, FacetType.Unitdate, "Dating")],
    [
      "enum with option explanations",
      {
        ...facet("UNIT~TYPE", FacetType.Enum, "Kind of material"),
        optionTooltips: [{ value: "matrika", tooltip: "parish registers" }],
      },
    ],
  ])("has no structural accessibility violations (%s facet)", async (_kind, def) => {
    const { container } = renderFacet(
      def,
      def.type === FacetType.Unitdate
        ? dating(def.code, 42)
        : {
            kind: FacetResultKind.Enum,
            code: def.code,
            buckets: [{ value: "matrika", count: 9 }],
          },
      def.code === DATE_FACET ? appliedRange(DATE_FACET) : [],
    );

    // "region" only makes sense for a whole page, not for one rendered facet
    await expectNoA11yViolations(container, ["region"]);
  });
});
