import { type QueryClient, queryOptions } from "@tanstack/react-query";
import { apuApi } from "../api/client";
import { ResponseError } from "../api/generated";

/**
 * The name a record is displayed under, by uuid - what a reference facet shows
 * for a selected value. The URL carries the uuid alone (a name there would go
 * stale on a rename and bloat every shared link), and the search response's
 * buckets are the scope's most frequent values, so a selected record that is
 * rarely referenced is not among them. The name is therefore kept here: written
 * by {@link rememberApuName} at the moment the reader picks an option that has
 * one, and otherwise (a shared link, the browser's history) asked of the record
 * itself. A name never changes within a session, so it is never refetched; it
 * leaves the cache by the client's usual garbage collection once nothing shows
 * it any more. A missing record is an answer, not a failure, so 404 is not
 * retried.
 */
export function apuNameQuery(uuid: string) {
  return queryOptions({
    queryKey: ["apu-name", uuid],
    queryFn: async () => (await apuApi.apuGetDetail({ uuid })).name,
    staleTime: Infinity,
    retry: (failureCount, error) =>
      !(error instanceof ResponseError && error.response.status === 404) && failureCount < 2,
  });
}

/** Keeps a name the reader has just seen, so no request has to ask for it later. */
export function rememberApuName(queryClient: QueryClient, uuid: string, name: string): void {
  queryClient.setQueryData(apuNameQuery(uuid).queryKey, name);
}
