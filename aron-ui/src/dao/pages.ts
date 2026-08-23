import { type FileInfo, FileType } from "../api/generated";

/**
 * How a page can be shown: through its deep-zoom pyramid, as a plain image
 * (small scans have no pyramid), or not at all - only offered for download.
 */
export type PageKind = "deepZoom" | "image" | "download";

/** One page of a digital object: up to three renditions of the same scan. */
export interface DaoPage {
  /** 0-based display index (pages are ordered by their declared position). */
  index: number;
  tile?: FileInfo;
  published?: FileInfo;
  thumbnail?: FileInfo;
  kind: PageKind;
  /** The page a viewer opens first (the source system's `selected` flag). */
  selected: boolean;
}

/** The id that identifies a page in a deep link (`?file=`). */
export function pageFileId(page: DaoPage): string | undefined {
  return (page.published ?? page.tile ?? page.thumbnail)?.id;
}

/** Whether any of the page's renditions carries the given file id. */
export function pageHasFile(page: DaoPage, fileId: string): boolean {
  return [page.tile, page.published, page.thumbnail].some((file) => file?.id === fileId);
}

/**
 * Transposes a digital object's flat file list into pages. Renditions of one
 * page share its declared `position` - matching is by that value, never by
 * list order, because a page may lack any of the three roles and index-zipping
 * would then pair the rest wrongly (the old portal additionally stopped at the
 * first gap, truncating the document). Files without a position each stand as
 * their own page after the positioned ones.
 */
export function buildPages(files: FileInfo[]): DaoPage[] {
  const byPosition = new Map<number, { tile?: FileInfo; published?: FileInfo; thumbnail?: FileInfo }>();
  const unpositioned: FileInfo[] = [];
  for (const file of files) {
    if (file.position === undefined || file.position <= 0) {
      unpositioned.push(file);
      continue;
    }
    const page = byPosition.get(file.position) ?? {};
    if (file.fileType === FileType.Tile) {
      page.tile ??= file;
    } else if (file.fileType === FileType.Thumbnail) {
      page.thumbnail ??= file;
    } else {
      page.published ??= file;
    }
    byPosition.set(file.position, page);
  }

  const pages: DaoPage[] = [...byPosition.entries()]
    .sort(([a], [b]) => a - b)
    .map(([, renditions]) => renditions as DaoPage);
  for (const file of unpositioned) {
    if (file.fileType === FileType.Tile) {
      pages.push({ tile: file } as DaoPage);
    } else if (file.fileType === FileType.Thumbnail) {
      pages.push({ thumbnail: file } as DaoPage);
    } else {
      pages.push({ published: file } as DaoPage);
    }
  }

  return pages.map((page, index) => ({
    ...page,
    index,
    kind: kindOf(page),
    selected: [page.tile, page.published, page.thumbnail].some((file) => file?.selected === true),
  }));
}

/** The page to open first: the selected one, else the deep-linked one, else the first. */
export function initialPageIndex(pages: DaoPage[], deepLinkedFileId: string | null): number {
  if (deepLinkedFileId !== null) {
    const linked = pages.find((page) => pageHasFile(page, deepLinkedFileId));
    if (linked !== undefined) {
      return linked.index;
    }
  }
  return pages.find((page) => page.selected)?.index ?? 0;
}

function kindOf(page: Pick<DaoPage, "tile" | "published" | "thumbnail">): PageKind {
  if (page.tile?.dziUrl !== undefined) {
    return "deepZoom";
  }
  if (page.published?.url !== undefined && page.published.contentType?.startsWith("image/") === true) {
    return "image";
  }
  return "download";
}
