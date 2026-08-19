import i18n from "i18next";
import { initReactI18next } from "react-i18next";
import cs from "./cs.json";
import en from "./en.json";

/**
 * Languages this build ships strings for. English is the source language and
 * the last-resort fallback; a deployment decides which of these it actually
 * offers through the `localizations` of /api/v1/ui/config, so shipping a bundle
 * does not force it on anyone, and a Czech deployment simply declares Czech
 * first (see {@link pickInitialLanguage}).
 */
export const BUNDLED_LANGUAGES = ["en", "cs"] as const;

export type BundledLanguage = (typeof BUNDLED_LANGUAGES)[number];

/** Source language: what an untranslated string falls back to. */
export const DEFAULT_LANGUAGE: BundledLanguage = "en";

/**
 * Every further language must cover the English key set - English is the source
 * language, so a missing key would silently fall back to it and ship a
 * half-translated page. That rule is checked by `translations.test.ts` rather
 * than by the type system: plural families legitimately differ per language
 * (Czech needs `_few`, English does not), so identical key sets are the wrong
 * test - equal key sets *with plural suffixes stripped* is the right one.
 */
export const BUNDLES: Record<BundledLanguage, unknown> = { en, cs };

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

/**
 * The deployment's default language, which the server injects into the SPA
 * shell (`<html lang>`, see IndexController). Reading it here rather than
 * waiting for /api/v1/ui/config keeps the first paint in the right language.
 * Absent in the Vite dev server, and an unsubstituted token means the build was
 * served without the server at all.
 */
function deploymentLanguage(): string | undefined {
  const injected = document.documentElement.lang;
  return injected && !injected.startsWith("__") ? languageOf(injected) : undefined;
}

/**
 * Language to start in. The reader's own choice wins, then the browser's
 * preference, then what the deployment declared first in its `localizations`
 * (this is how a Czech deployment stays Czech for everyone who has not asked
 * for something else), and only then the source language. A candidate this
 * build has no strings for is skipped rather than shown half-translated; a
 * language this *deployment* does not offer is corrected by LanguageSwitcher
 * once the configuration arrives.
 */
export function pickInitialLanguage(
  stored: string | undefined,
  preferred: string | undefined,
  deployment: string | undefined,
): BundledLanguage {
  for (const candidate of [stored, preferred, deployment]) {
    if (candidate !== undefined && isBundled(candidate)) {
      return candidate;
    }
  }
  return DEFAULT_LANGUAGE;
}

i18n.use(initReactI18next).init({
  lng: pickInitialLanguage(storedLanguage(), preferredLanguage(), deploymentLanguage()),
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
