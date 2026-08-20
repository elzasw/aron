import { useQuery } from "@tanstack/react-query";
import { apuApi } from "../api/client";
import { ResponseError } from "../api/generated";
import { useApiLanguage } from "../i18n/useApiLanguage";

/**
 * The record's render model, as one hook so that every part of the page shares
 * a single cache entry: the record page and the breadcrumb strip both need the
 * record, and asking through the same key fetches it once.
 *
 * The language belongs in the key - the server renders the display text, so a
 * switch must not serve the previous language from the cache. A missing record
 * is an answer, not a failure, so 404 is not retried.
 */
export function useApuDetail(uuid: string | undefined) {
  const lang = useApiLanguage();
  return useQuery({
    queryKey: ["apu-detail", uuid, lang],
    queryFn: () => apuApi.apuGetDetail({ uuid: uuid!, lang }),
    enabled: uuid !== undefined,
    staleTime: 5 * 60 * 1000,
    retry: (failureCount, error) =>
      !(error instanceof ResponseError && error.response.status === 404) && failureCount < 2,
  });
}
