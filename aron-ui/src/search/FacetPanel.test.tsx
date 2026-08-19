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
    renderFacet(facet("TITLE~MAIN", FacetType.Fulltext, "Název archivního souboru"));

    // a placeholder disappears once the user types - the control needs a name
    expect(screen.getByRole("textbox", { name: "Název archivního souboru" })).toBeInTheDocument();
  });

  it("heads every facet, so a screen reader can move between them", () => {
    renderFacet(facet("LANG~CODE", FacetType.Enum, "Jazyk"));

    expect(screen.getByRole("heading", { level: 2, name: "Jazyk" })).toBeInTheDocument();
    expect(screen.getByRole("group")).toHaveAccessibleName("Jazyk");
  });

  it("names both ends of the dating range, slider and field alike", () => {
    const bounds: DatingFacetResult = {
      kind: FacetResultKind.Dating,
      code: "UNIT~DATE",
      bounds: { minYear: 1201, maxYear: 1961 },
    };
    renderFacet(facet("UNIT~DATE", FacetType.Unitdate, "Datace vzniku"), bounds);

    expect(screen.getByRole("slider", { name: "Datace vzniku – posuvník od roku" })).toHaveValue(
      "1201",
    );
    expect(screen.getByRole("slider", { name: "Datace vzniku – posuvník do roku" })).toHaveValue(
      "1961",
    );
    // the year fields are numeric and carry their own names
    expect(screen.getByRole("spinbutton", { name: "Datace vzniku – od roku" })).toBeInTheDocument();
    expect(screen.getByRole("spinbutton", { name: "Datace vzniku – do roku" })).toBeInTheDocument();
  });
});
