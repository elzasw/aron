import { FilterKind, RangeFilter, SearchFilter, TextFilter, ValuesFilter } from "../api/generated";

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
