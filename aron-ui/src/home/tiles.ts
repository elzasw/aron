import { type HomeTile, type LinkTile, type SearchTile, TileKind } from "../api/generated";
import { serializeFilters } from "../search/filters";
import { SECTION_OF_APU_TYPE, SECTIONS } from "../sections";

/**
 * A tile is discriminated by `kind`, and because the discriminator is absent
 * from the subtypes (the contract keeps it parent-only so every generator
 * handles it), TypeScript does not narrow on its own - hence one predicate per
 * kind, as `ApuPage` does for detail items.
 */
export function isSearchTile(tile: HomeTile): tile is SearchTile {
  return tile.kind === TileKind.Search;
}

export function isLinkTile(tile: HomeTile): tile is LinkTile {
  return tile.kind === TileKind.Link;
}

/**
 * In-app URL of a tile leading into a section's search. The filters travel in
 * the same `f` parameter the search page reads from anywhere else, so a tile's
 * constraint arrives as an ordinary one: visible in the sidebar and removable.
 *
 * `undefined` when the record type has no section of its own - only COLLECTION,
 * which the server rejects at startup, so this is a guard rather than a case.
 */
export function searchTileUrl(tile: SearchTile): string | undefined {
  const section = SECTION_OF_APU_TYPE[tile.apuType];
  const route = section ? SECTIONS[section].route : undefined;
  if (!route) {
    return undefined;
  }
  const params = new URLSearchParams();
  if (tile.query) {
    params.set("q", tile.query);
  }
  const filters = serializeFilters(tile.filters);
  if (filters) {
    params.set("f", filters);
  }
  const query = params.toString();
  return query ? `${route}?${query}` : route;
}

/** Where the tile leads, or `undefined` for the one case that leads nowhere. */
export function tileUrl(tile: HomeTile): string | undefined {
  return isLinkTile(tile) ? tile.url : isSearchTile(tile) ? searchTileUrl(tile) : undefined;
}
