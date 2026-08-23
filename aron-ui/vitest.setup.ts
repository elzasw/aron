// DOM matchers (toHaveAttribute, toHaveFocus, toHaveAccessibleName, ...) so the
// assertions read as accessibility statements rather than attribute lookups.
import "@testing-library/jest-dom/vitest";
import { JSDOM } from "jsdom";
import { beforeEach } from "vitest";
import i18n, { DEFAULT_LANGUAGE } from "./src/i18n";

// Node >= 26 declares a global `localStorage` of its own, which stays undefined
// unless the process was started with --localstorage-file. Vitest does not
// overwrite a global that already exists while it populates jsdom's, so both
// the tests and the code under test (the remembered language) would find no
// storage at all. Hand the shadowed globals a real jsdom Storage; on a runtime
// without that binding this is a no-op.
if (globalThis.localStorage === undefined) {
  const { window } = new JSDOM("", { url: "http://localhost" });
  for (const name of ["localStorage", "sessionStorage"] as const) {
    Object.defineProperty(globalThis, name, { configurable: true, value: window[name] });
  }
}

// i18next is a module-level singleton, so a test that switches language leaves
// the next one in it. Every test starts in the source language; a test about
// switching says so by switching itself.
beforeEach(async () => {
  await i18n.changeLanguage(DEFAULT_LANGUAGE);
});
