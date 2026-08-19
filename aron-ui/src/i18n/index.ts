import i18n from "i18next";
import { initReactI18next } from "react-i18next";
import cs from "./cs.json";
import en from "./en.json";

/**
 * Languages this build ships strings for. Czech is the source language and the
 * fallback; a deployment decides which of these it actually offers through the
 * `localizations` of /api/v1/ui/config, so shipping a bundle does not force it
 * on anyone.
 */
export const BUNDLED_LANGUAGES = ["cs", "en"] as const;

export type BundledLanguage = (typeof BUNDLED_LANGUAGES)[number];

export const DEFAULT_LANGUAGE: BundledLanguage = "cs";

/**
 * Every further language must cover the Czech key set - Czech is the source
 * language, so a missing key would silently fall back to it and ship a
 * half-translated page. That rule is checked by `translations.test.ts` rather
 * than by the type system: plural families legitimately differ per language
 * (Czech needs `_few`, English does not), so identical key sets are the wrong
 * test - equal key sets *with plural suffixes stripped* is the right one.
 */
export const BUNDLES: Record<BundledLanguage, unknown> = { cs, en };

const STORAGE_KEY = "aron.language";

/**
 * Language subtag of a server localization code: those are written IETF-like
 * but with an underscore (`cs_CZ`), and the UI's strings are per language, not
 * per region.
 */
export function languageOf(localization: string): string {
  return localization.trim().replace("_", "-").split("-")[0].toLowerCase();
}

function isBundled(language: string): language is BundledLanguage {
  return (BUNDLED_LANGUAGES as readonly string[]).includes(language);
}

/** The reader's stored choice, if it is still a language this build has. */
function storedLanguage(): BundledLanguage | undefined {
  try {
    const stored = window.localStorage.getItem(STORAGE_KEY);
    return stored && isBundled(stored) ? stored : undefined;
  } catch {
    // storage can be unavailable (private mode, blocked cookies) - not fatal
    return undefined;
  }
}

/** First browser preference this build has strings for. */
function preferredLanguage(): BundledLanguage | undefined {
  for (const candidate of window.navigator.languages ?? []) {
    const language = languageOf(candidate);
    if (isBundled(language)) {
      return language;
    }
  }
  return undefined;
}

i18n.use(initReactI18next).init({
  // the reader's own choice wins over the browser's preference; neither can
  // pick a language this build has no strings for
  lng: storedLanguage() ?? preferredLanguage() ?? DEFAULT_LANGUAGE,
  fallbackLng: DEFAULT_LANGUAGE,
  supportedLngs: BUNDLED_LANGUAGES,
  resources: {
    cs: { translation: cs },
    en: { translation: en },
  },
  interpolation: {
    // React already escapes rendered values
    escapeValue: false,
  },
});

/** Switches the UI language and remembers the choice for the next visit. */
export function setLanguage(language: string): void {
  if (!isBundled(language) || language === i18n.language) {
    return;
  }
  void i18n.changeLanguage(language);
  try {
    window.localStorage.setItem(STORAGE_KEY, language);
  } catch {
    // a reader without storage simply starts from the default again
  }
}

// screen readers and hyphenation need the document's language to be truthful
const applyDocumentLanguage = (language: string) => {
  document.documentElement.lang = languageOf(language);
};
applyDocumentLanguage(i18n.language);
i18n.on("languageChanged", applyDocumentLanguage);

export default i18n;

/**
 * Languages actually offered to the reader: those the deployment declares,
 * narrowed to the ones this build has strings for, deduplicated and in the
 * deployment's own order (its first is its default).
 */
export function offeredLanguages(localizations: string[]): BundledLanguage[] {
  const offered: BundledLanguage[] = [];
  for (const localization of localizations) {
    const language = languageOf(localization);
    if (isBundled(language) && !offered.includes(language)) {
      offered.push(language);
    }
  }
  return offered;
}
