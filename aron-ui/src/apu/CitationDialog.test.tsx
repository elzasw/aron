import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApuType, type ApuDetail, type Citation, type UiConfig } from "../api/generated";
import ApuPage from "../pages/ApuPage";
import { expectNoA11yViolations } from "../test/a11y";
import { renderWithProviders } from "../test/render";
import { citationIsOffered } from "./citations";

const FUND: ApuDetail = {
  uuid: "fund",
  name: "Farní úřad Všestary",
  apuType: ApuType.Fund,
  childCount: 0,
  treePath: [],
  parts: [],
  attachments: [],
  digitalObjects: [],
};

/** Same record data, a type the deployment configures no citation form for. */
const ACCESS_POINT: ApuDetail = { ...FUND, uuid: "entity", apuType: ApuType.Entity };

const CITATION: Citation = {
  code: "DEFAULT",
  label: "Citace",
  text: "Státní okresní archiv Hradec Králové, Farní úřad Všestary (1820)",
};

const CONFIG: UiConfig = {
  name: "Test portal",
  localizations: ["en"],
  menuItems: [],
  footerLinks: [],
  citations: [{ code: "DEFAULT", label: "Citace", apuTypes: [ApuType.Fund] }],
};

const records: Record<string, ApuDetail> = { fund: FUND, entity: ACCESS_POINT };

/** Replaced per test: the same button leads to a citation or to a refusal. */
let citations: () => Promise<Citation[]> = () => Promise.resolve([CITATION]);

vi.mock("../api/client", () => ({
  apuApi: {
    apuGetDetail: vi.fn((request: { uuid: string }) => Promise.resolve(records[request.uuid])),
    apuGetTreeNodes: vi.fn(() => Promise.resolve([])),
    apuGetCitations: vi.fn(() => citations()),
  },
  uiApi: { uiGetConfig: vi.fn(() => Promise.resolve(CONFIG)) },
}));

function renderRecord(uuid: string) {
  return renderWithProviders(
    <Routes>
      <Route path="apu/:uuid" element={<ApuPage />} />
    </Routes>,
    { initialEntries: [`/apu/${uuid}`] },
  );
}

/** Clipboard access is not part of jsdom; the dialog's copy path needs it. */
function stubClipboard(writeText = vi.fn(() => Promise.resolve())) {
  Object.defineProperty(navigator, "clipboard", { value: { writeText }, configurable: true });
  return writeText;
}

describe("citations", () => {
  beforeEach(() => {
    citations = () => Promise.resolve([CITATION]);
  });

  it("is offered for the record types the deployment configured, and no others", async () => {
    // configuration decides, so the reader is never offered a citation that
    // cannot be produced
    expect(citationIsOffered(CONFIG.citations, ApuType.Fund)).toBe(true);
    expect(citationIsOffered(CONFIG.citations, ApuType.ArchDesc)).toBe(false);
    expect(citationIsOffered(undefined, ApuType.Fund)).toBe(false);

    const fund = renderRecord("fund");
    expect(
      await screen.findByRole("button", { name: `Create a citation of ${FUND.name}` }),
    ).toHaveTextContent("Create citation");
    fund.unmount();

    renderRecord("entity");
    await screen.findByRole("heading", { level: 1, name: FUND.name });
    expect(screen.queryByRole("button", { name: /Create a citation/ })).not.toBeInTheDocument();
  });

  it("shows the citation and copies it verbatim", async () => {
    const writeText = stubClipboard();
    const { baseElement } = renderRecord("fund");

    await userEvent.click(
      await screen.findByRole("button", { name: `Create a citation of ${FUND.name}` }),
    );

    const dialog = await screen.findByRole("dialog", { name: "Citation" });
    expect(dialog).toHaveTextContent(CITATION.text);
    // one form needs no heading of its own - the dialog's title says what this is
    expect(screen.queryByRole("heading", { name: CITATION.label })).not.toBeInTheDocument();

    await userEvent.click(screen.getByRole("button", { name: "Copy text" }));
    expect(writeText).toHaveBeenCalledWith(CITATION.text);
    // the outcome is announced where the reader is looking
    expect(await screen.findByRole("status")).toHaveTextContent("Text copied to the clipboard.");

    await expectNoA11yViolations(baseElement as HTMLElement, ["region"]);
  });

  it("says when the text could not be copied", async () => {
    stubClipboard(vi.fn(() => Promise.reject(new Error("denied"))));
    renderRecord("fund");

    await userEvent.click(
      await screen.findByRole("button", { name: `Create a citation of ${FUND.name}` }),
    );
    await userEvent.click(await screen.findByRole("button", { name: "Copy text" }));

    expect(await screen.findByRole("status")).toHaveTextContent(
      "The text could not be copied.",
    );
  });

  it("says so when the record's description cannot be cited", async () => {
    // the server answers 422 - a data error the reader can do nothing about, so
    // what they get is this UI's own message
    citations = () => Promise.reject(new Error("Unprocessable Entity"));
    renderRecord("fund");

    await userEvent.click(
      await screen.findByRole("button", { name: `Create a citation of ${FUND.name}` }),
    );

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "A citation of this record could not be created.",
    );
  });

  it("closes on the reader's request", async () => {
    renderRecord("fund");
    await userEvent.click(
      await screen.findByRole("button", { name: `Create a citation of ${FUND.name}` }),
    );
    await screen.findByRole("dialog", { name: "Citation" });

    await userEvent.click(screen.getByRole("button", { name: "Close" }));
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  });
});
