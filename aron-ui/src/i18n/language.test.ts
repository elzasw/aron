import { beforeEach, describe, expect, it } from "vitest";
import i18n, {
  DEFAULT_LANGUAGE,
  languageOf,
  offeredLanguages,
  pickInitialLanguage,
  setLanguage,
} from "./index";

describe("language codes", () => {
  it("takes the language subtag of a server localization code", () => {
    // the deployment writes them IETF-like but with an underscore
    expect(languageOf("cs_CZ")).toBe("cs");
    expect(languageOf("en_US")).toBe("en");
    expect(languageOf("en-GB")).toBe("en");
    expect(languageOf("  EN  ")).toBe("en");
  });
});

describe("offered languages", () => {
  it("keeps the deployment's order - its first is its default", () => {
    expect(offeredLanguages(["en", "cs_CZ"])).toEqual(["en", "cs"]);
  });

  it("drops languages this build has no strings for", () => {
    // a deployment may declare more than the UI ships; showing those would
    // switch the chrome to a language with no bundle
    expect(offeredLanguages(["cs_CZ", "de_DE", "en"])).toEqual(["cs", "en"]);
  });

  it("deduplicates regional variants of one language", () => {
    expect(offeredLanguages(["en_US", "en_GB"])).toEqual(["en"]);
  });

  it("is empty when nothing matches", () => {
    expect(offeredLanguages(["de", "fr"])).toEqual([]);
  });
});

describe("initial language", () => {
  it("prefers the reader's choice, then the browser, then the deployment", () => {
    expect(pickInitialLanguage("cs", "en", "en")).toBe("cs");
    expect(pickInitialLanguage(undefined, "cs", "en")).toBe("cs");
    // nobody asked for a language this build has: the deployment's own default
    // decides, which is how a Czech deployment stays Czech
    expect(pickInitialLanguage(undefined, undefined, "cs")).toBe("cs");
  });

  it("skips candidates this build has no strings for", () => {
    expect(pickInitialLanguage("de", "fr", "cs")).toBe("cs");
    expect(pickInitialLanguage(undefined, "de", undefined)).toBe(DEFAULT_LANGUAGE);
  });

  it("falls back to the source language when nothing is known", () => {
    expect(pickInitialLanguage(undefined, undefined, undefined)).toBe("en");
  });
});

describe("switching", () => {
  beforeEach(async () => {
    window.localStorage.clear();
  });

  it("changes the language and remembers the choice", () => {
    setLanguage("cs");

    expect(i18n.language).toBe("cs");
    expect(window.localStorage.getItem("aron.language")).toBe("cs");
  });

  it("keeps the document language truthful for screen readers", () => {
    setLanguage("cs");
    expect(document.documentElement.lang).toBe("cs");

    setLanguage("en");
    expect(document.documentElement.lang).toBe("en");
  });

  it("ignores a language this build has no strings for", () => {
    setLanguage("de");

    expect(i18n.language).toBe(DEFAULT_LANGUAGE);
    expect(window.localStorage.getItem("aron.language")).toBeNull();
  });
});
