import {
  FacetDisplay,
  FilterKind,
  RangeFilter,
  RelatedFilter,
  RelationDirection,
  SearchFilter,
  TextFilter,
  ValuesFilter,
} from "../api/generated";

/**
 * Active filters travel in the `f` URL parameter as the JSON array of the
 * contract's SearchFilter objects - links to filtered results are shareable
 * and survive a reload.
 */
export function parseFilters(param: string | null): SearchFilter[] {
  if (!param) {
    return [];
  }
  try {
    const parsed = JSON.parse(param);
    return Array.isArray(parsed) ? (parsed as SearchFilter[]) : [];
  } catch {
    return [];
  }
}

export function serializeFilters(filters: SearchFilter[]): string | null {
  return filters.length > 0 ? JSON.stringify(filters) : null;
}

export function valuesOf(filters: SearchFilter[], facet: string): string[] {
  const filter = filters.find((f) => f.facet === facet && f.kind === FilterKind.Values);
  return filter ? (filter as ValuesFilter).values : [];
}

export function textOf(filters: SearchFilter[], facet: string): string {
  const filter = filters.find((f) => f.facet === facet && f.kind === FilterKind.Text);
  return filter ? (filter as TextFilter).q : "";
}

export function rangeOf(filters: SearchFilter[], facet: string): { from?: string; to?: string } {
  const filter = filters.find((f) => f.facet === facet && f.kind === FilterKind.Range);
  return filter ? { from: (filter as RangeFilter).from, to: (filter as RangeFilter).to } : {};
}

/** Returns a new filter list with the facet's VALUES selection toggled. */
export function toggleValue(filters: SearchFilter[], facet: string, value: string): SearchFilter[] {
  const current = valuesOf(filters, facet);
  const values = current.includes(value) ? current.filter((v) => v !== value) : [...current, value];
  const rest = filters.filter((f) => !(f.facet === facet && f.kind === FilterKind.Values));
  return values.length > 0 ? [...rest, { kind: FilterKind.Values, facet, values } as ValuesFilter] : rest;
}

/** Returns a new filter list with the facet's TEXT query replaced (empty = removed). */
export function setText(filters: SearchFilter[], facet: string, q: string): SearchFilter[] {
  const rest = filters.filter((f) => !(f.facet === facet && f.kind === FilterKind.Text));
  return q.trim() ? [...rest, { kind: FilterKind.Text, facet, q: q.trim() } as TextFilter] : rest;
}

/** Returns a new filter list with the facet's RANGE bounds replaced (both empty = removed). */
export function setRange(filters: SearchFilter[], facet: string, from: string, to: string): SearchFilter[] {
  const rest = filters.filter((f) => !(f.facet === facet && f.kind === FilterKind.Range));
  const bounds: RangeFilter = { kind: FilterKind.Range, facet };
  if (from.trim()) {
    bounds.from = from.trim();
  }
  if (to.trim()) {
    bounds.to = to.trim();
  }
  return bounds.from || bounds.to ? [...rest, bounds] : rest;
}

/**
 * Reserved code of the built-in relation facet (SearchController.RELATED_FACET):
 * it spans every reference item type and needs no deployment configuration, so
 * "find related" works in the general search where no section facets exist.
 */
export const RELATED_FACET = "~RELATED";

/** The relation filters in the list, in order - each one is its own condition (they AND). */
export function relatedOf(filters: SearchFilter[]): RelatedFilter[] {
  return filters.filter((f) => f.kind === FilterKind.Related) as RelatedFilter[];
}

/** Returns a new filter list with one relation condition added (existing ones are kept). */
export function addRelated(filters: SearchFilter[], apu: string): SearchFilter[] {
  const already = relatedOf(filters).some(
    (f) => f.facet === RELATED_FACET && f.apus.length === 1 && f.apus[0] === apu,
  );
  if (already) {
    return filters;
  }
  const filter: RelatedFilter = {
    kind: FilterKind.Related,
    facet: RELATED_FACET,
    apus: [apu],
    direction: RelationDirection.Both,
  };
  return [...filters, filter];
}

/** Returns a new filter list without the relation condition on the given APU. */
export function removeRelated(filters: SearchFilter[], apu: string): SearchFilter[] {
  return filters.filter(
    (f) => f.kind !== FilterKind.Related || !(f as RelatedFilter).apus.includes(apu),
  );
}

/**
 * Link from a record to the general search restricted to what relates to it -
 * across every section, because a relation is not confined to one. The record's
 * name is deliberately NOT carried in the URL: the search page resolves it from
 * the record itself, so the chip stays right after a rename and shared links
 * stay short.
 */
export function relatedSearchUrl(apu: string): string {
  const params = new URLSearchParams();
  params.set("f", JSON.stringify(addRelated([], apu)));
  return `/apu?${params.toString()}`;
}

/**
 * Whether a facet is offered in the sidebar: by its own display setting, or -
 * the DETAIL case - because it currently carries a constraint, which the reader
 * has to be able to see and undo wherever it came from (the advanced-search
 * dialog, or an action on a record page).
 */
export function facetIsOffered(display: FacetDisplay, hasActiveFilter: boolean): boolean {
  return display === FacetDisplay.Always || hasActiveFilter;
}
