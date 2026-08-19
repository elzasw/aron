import { beforeEach, describe, expect, it } from "vitest";
import i18n, { DEFAULT_LANGUAGE, languageOf, offeredLanguages, setLanguage } from "./index";

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

describe("switching", () => {
  beforeEach(async () => {
    window.localStorage.clear();
    await i18n.changeLanguage(DEFAULT_LANGUAGE);
  });

  it("changes the language and remembers the choice", () => {
    setLanguage("en");

    expect(i18n.language).toBe("en");
    expect(window.localStorage.getItem("aron.language")).toBe("en");
  });

  it("keeps the document language truthful for screen readers", () => {
    setLanguage("en");
    expect(document.documentElement.lang).toBe("en");

    setLanguage("cs");
    expect(document.documentElement.lang).toBe("cs");
  });

  it("ignores a language this build has no strings for", () => {
    setLanguage("de");

    expect(i18n.language).toBe(DEFAULT_LANGUAGE);
    expect(window.localStorage.getItem("aron.language")).toBeNull();
  });
});
