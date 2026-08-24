import type { ApuType, CitationForm } from "../api/generated";

/**
 * Whether this deployment offers a citation for a record of this type. The forms
 * come from `/api/v1/ui/config`, so the action appears only where a citation can
 * actually be produced - a reader is never offered one that would fail, and the
 * page needs no request to find out.
 */
export function citationIsOffered(
  forms: CitationForm[] | undefined,
  apuType: ApuType,
): boolean {
  return (forms ?? []).some((form) => form.apuTypes.includes(apuType));
}
