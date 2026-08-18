import { useTranslation } from "react-i18next";
import { languageOf } from "./index";

/**
 * Language to send as the `lang` parameter of endpoints that return
 * server-rendered display text (item and part labels, formatted datings).
 *
 * It must also go into the react-query key of every such request: the response
 * body depends on it, so two languages are two cache entries, and switching the
 * language has to refetch rather than serve the previous one.
 */
export function useApiLanguage(): string {
  const { i18n } = useTranslation();
  return languageOf(i18n.language);
}
