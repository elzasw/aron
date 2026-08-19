import { useQuery } from "@tanstack/react-query";
import { searchApi } from "../api/client";
import type { ResultFieldStyle, ResultLayout } from "../api/generated";
import { useApiLanguage } from "../i18n/useApiLanguage";

/** Field separator when the deployment configures none (the old portal's default). */
export const DEFAULT_FIELD_SEPARATOR = " | ";

/** Rendered width of a record icon when neither the icon nor the deployment sets one. */
export const DEFAULT_ICON_SIZE = 32;

const EMPTY: ResultLayout = { fields: [], icons: [] };

/**
 * The deployment's layout of structured search results, looked up by code.
 *
 * Fetched once per language and kept: it changes only with the deployment's
 * configuration. The language is part of the query key because prefixes and
 * labels are server-rendered text - without it a language switch would serve
 * the previous language from cache.
 */
export interface ResultLayoutLookup {
  fieldSeparator: string;
  styleOf: (code: string) => ResultFieldStyle | undefined;
  iconOf: (code: string) => { url: string; size: number } | undefined;
  /**
   * Code of the field whose text is the record's heading, or undefined when the
   * deployment marks none - callers then use the record's own name.
   */
  headingCode?: string;
}

export function useResultLayout(): ResultLayoutLookup {
  const lang = useApiLanguage();
  const { data } = useQuery({
    queryKey: ["result-layout", lang],
    queryFn: () => searchApi.searchGetResultLayout({ lang }),
    staleTime: Infinity,
  });
  return lookup(data ?? EMPTY);
}

/** Turns the layout document into by-code lookups; exported for tests. */
export function lookup(layout: ResultLayout): ResultLayoutLookup {
  const fields = new Map(layout.fields.map((field) => [field.code, field]));
  const icons = new Map(layout.icons.map((icon) => [icon.code, icon]));
  return {
    fieldSeparator: layout.fieldSeparator ?? DEFAULT_FIELD_SEPARATOR,
    styleOf: (code) => fields.get(code),
    iconOf: (code) => {
      const icon = icons.get(code);
      return icon ? { url: icon.url, size: icon.size ?? DEFAULT_ICON_SIZE } : undefined;
    },
    headingCode: layout.fields.find((field) => field.heading)?.code,
  };
}
