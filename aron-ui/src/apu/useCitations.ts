import { useQuery } from "@tanstack/react-query";
import { apuApi } from "../api/client";
import { useApiLanguage } from "../i18n/useApiLanguage";

/**
 * Citations of a record, asked for when the reader opens them rather than with
 * the record: the text is composed from the deployment's citation configuration,
 * so it is not part of the record's cache entry and carries no ETag.
 *
 * The language belongs in the key like everywhere else - it decides the form's
 * name (the citation itself is in the archives' own language). A record whose
 * description cannot be cited answers 422, which is an answer rather than a
 * transient failure, so nothing is retried.
 */
export function useCitations(uuid: string) {
  const lang = useApiLanguage();
  return useQuery({
    queryKey: ["apu-citations", uuid, lang],
    queryFn: () => apuApi.apuGetCitations({ uuid, lang }),
    retry: false,
  });
}
