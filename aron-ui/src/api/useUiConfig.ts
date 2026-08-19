import { useQuery } from "@tanstack/react-query";
import { uiApi } from "./client";
import { useApiLanguage } from "../i18n/useApiLanguage";

/**
 * The deployment's UI configuration (name, menu, footer links). Server-rendered
 * texts follow the reader's language, so the language is part of the cache key;
 * every consumer shares one request.
 */
export function useUiConfig() {
  const lang = useApiLanguage();
  return useQuery({
    queryKey: ["ui-config", lang],
    queryFn: () => uiApi.uiGetConfig({ lang }),
  });
}
