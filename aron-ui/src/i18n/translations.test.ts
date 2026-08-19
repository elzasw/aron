import { afterEach, describe, expect, it } from "vitest";
import i18n, { BUNDLED_LANGUAGES, BUNDLES, DEFAULT_LANGUAGE } from "./index";

/** i18next appends these to a key when the string depends on a count. */
const PLURAL_SUFFIXES = ["zero", "one", "two", "few", "many", "other"];

/** Counts a portal realistically shows; enough to hit every category of cs and en. */
const COUNTS = [0, 1, 2, 3, 5, 11, 100, 10000];

function flatten(value: unknown, prefix = ""): string[] {
  if (typeof value !== "object" || value === null) {
    return [prefix];
  }
  return Object.entries(value as Record<string, unknown>).flatMap(([key, nested]) =>
    flatten(nested, prefix ? `${prefix}.${key}` : key),
  );
}

/** `search.total_few` -> `search.total`; a key without a plural suffix is its own family. */
function family(key: string): string {
  const separator = key.lastIndexOf("_");
  const suffix = separator < 0 ? "" : key.slice(separator + 1);
  return PLURAL_SUFFIXES.includes(suffix) ? key.slice(0, separator) : key;
}

const keysOf = (language: string) => flatten(BUNDLES[language as keyof typeof BUNDLES]);
const familiesOf = (language: string) => new Set(keysOf(language).map(family));

describe("translation bundles", () => {
  it("cover the same strings in every language", () => {
    // plural families count once: Czech needs _few where English does not, so
    // comparing raw keys would forbid a correct translation
    const source = familiesOf(DEFAULT_LANGUAGE);

    for (const language of BUNDLED_LANGUAGES.filter((l) => l !== DEFAULT_LANGUAGE)) {
      const other = familiesOf(language);
      expect([...source].filter((key) => !other.has(key)), `missing in ${language}`).toEqual([]);
      expect([...other].filter((key) => !source.has(key)), `absent from the source language`).toEqual([]);
    }
  });

  it("carry every plural category their own language needs", () => {
    for (const language of BUNDLED_LANGUAGES) {
      const keys = new Set(keysOf(language));
      const plurals = new Set([...keys].filter((key) => family(key) !== key).map(family));
      const rules = new Intl.PluralRules(language);
      const needed = new Set(COUNTS.map((count) => rules.select(count)));

      for (const base of plurals) {
        for (const category of needed) {
          expect(keys, `${language}: ${base}_${category}`).toContain(`${base}_${category}`);
        }
      }
    }
  });
});

describe("counted strings", () => {
  afterEach(async () => {
    await i18n.changeLanguage(DEFAULT_LANGUAGE);
  });

  it("agree with the number in Czech", async () => {
    await i18n.changeLanguage("cs");

    expect(i18n.t("search.total", { count: 1 })).toBe("Nalezen 1 záznam");
    expect(i18n.t("search.total", { count: 3 })).toBe("Nalezeny 3 záznamy");
    expect(i18n.t("search.total", { count: 5 })).toBe("Nalezeno 5 záznamů");
    // no results is the "other" category, not a special case
    expect(i18n.t("search.total", { count: 0 })).toBe("Nalezeno 0 záznamů");
    expect(i18n.t("apu.digitalObjectFiles", { count: 2 })).toBe("2 soubory");
  });

  it("agree with the number in English", async () => {
    await i18n.changeLanguage("en");

    expect(i18n.t("search.total", { count: 1 })).toBe("1 record found");
    expect(i18n.t("search.total", { count: 3 })).toBe("3 records found");
    expect(i18n.t("apu.digitalObjectFiles", { count: 1 })).toBe("1 file");
  });

  it("render every counted string from the language's own bundle", async () => {
    // a missing plural category falls back to the source language silently.
    // Comparing the rendered string with that language's own template catches
    // it in any script - unlike looking for foreign characters, which only
    // works while the source language has letters the others lack.
    for (const language of BUNDLED_LANGUAGES) {
      await i18n.changeLanguage(language);
      const rules = new Intl.PluralRules(language);
      const bundle = BUNDLES[language] as Record<string, unknown>;
      const families = new Set(keysOf(language).filter((key) => family(key) !== key).map(family));

      for (const base of families) {
        for (const count of COUNTS) {
          const key = `${base}_${rules.select(count)}`;
          const template = key.split(".").reduce<unknown>(
            (node, part) => (node as Record<string, unknown> | undefined)?.[part],
            bundle,
          );
          expect(typeof template, `${language}: ${key}`).toBe("string");
          expect(i18n.t(base, { count }), `${language}: ${base} @ ${count}`).toBe(
            (template as string).replace("{{count}}", String(count)),
          );
        }
      }
    }
  });

  it("leaves a counted string without plural forms alone", async () => {
    await i18n.changeLanguage("cs");

    // showMore only parenthesizes the number - nothing agrees with it
    expect(i18n.t("facets.showMore", { count: 7 })).toBe("Zobrazit více (7)");
  });
});
