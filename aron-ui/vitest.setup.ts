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

// jsdom parses media queries but never evaluates one, so every matchMedia list
// stays unmatched forever - and the pages decide their arrangement from size
// queries (src/layout/breakpoints.ts). Evaluate the min/max-width and -height
// features against window.innerWidth/innerHeight - every `and`-joined feature
// of a comma-separated alternative must hold - so a test can pick a viewport
// (src/test/viewport.ts) and components react like in a browser; a query with
// no size feature (reduced motion, ...) keeps jsdom's answer: no match.
function mediaQueryMatches(query: string): boolean {
  return query.split(",").some((part) => {
    const features = [...part.matchAll(/\((min|max)-(width|height):\s*([\d.]+)px\)/g)];
    return (
      features.length > 0 &&
      features.every(([, bound, axis, value]) => {
        const actual = axis === "width" ? window.innerWidth : window.innerHeight;
        return bound === "min" ? actual >= Number(value) : actual <= Number(value);
      })
    );
  });
}

window.matchMedia = (query: string): MediaQueryList => {
  const listeners = new Set<(event: MediaQueryListEvent) => void>();
  let last = mediaQueryMatches(query);
  window.addEventListener("resize", () => {
    const next = mediaQueryMatches(query);
    if (next !== last) {
      last = next;
      const event = Object.assign(new Event("change"), { matches: next, media: query });
      listeners.forEach((listener) => listener(event as MediaQueryListEvent));
    }
  });
  return {
    media: query,
    get matches() {
      return mediaQueryMatches(query);
    },
    addEventListener: (_type: string, listener: (event: MediaQueryListEvent) => void) => {
      listeners.add(listener);
    },
    removeEventListener: (_type: string, listener: (event: MediaQueryListEvent) => void) => {
      listeners.delete(listener);
    },
    addListener: (listener: (event: MediaQueryListEvent) => void) => {
      listeners.add(listener);
    },
    removeListener: (listener: (event: MediaQueryListEvent) => void) => {
      listeners.delete(listener);
    },
    onchange: null,
    dispatchEvent: () => true,
  } as MediaQueryList;
};

// i18next is a module-level singleton, so a test that switches language leaves
// the next one in it. Every test starts in the source language; a test about
// switching says so by switching itself.
beforeEach(async () => {
  await i18n.changeLanguage(DEFAULT_LANGUAGE);
});
