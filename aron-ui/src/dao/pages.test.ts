import { describe, expect, it } from "vitest";
import { FileType, type FileInfo } from "../api/generated";
import { buildPages, initialPageIndex, pageFileId } from "./pages";

const tile = (id: string, position?: number): FileInfo => ({
  id,
  fileType: FileType.Tile,
  position,
  dziUrl: `/api/v1/daofile/${id}/tiles/image.dzi`,
});
const published = (id: string, position?: number, contentType = "image/jpeg"): FileInfo => ({
  id,
  fileType: FileType.Published,
  position,
  contentType,
  url: `/api/v1/daofile/${id}`,
});
const thumbnail = (id: string, position?: number): FileInfo => ({
  id,
  fileType: FileType.Thumbnail,
  position,
  url: `/api/v1/daofile/${id}`,
});

describe("buildPages", () => {
  it("matches renditions by declared position, not list order", () => {
    // page 2 has no thumbnail: index-zipping would pair page 3's thumbnail with it
    const pages = buildPages([
      published("p1", 1),
      published("p2", 2),
      published("p3", 3),
      thumbnail("t1", 1),
      thumbnail("t3", 3),
      tile("z1", 1),
    ]);
    expect(pages).toHaveLength(3);
    expect(pages[1].published?.id).toBe("p2");
    expect(pages[1].thumbnail).toBeUndefined();
    expect(pages[2].thumbnail?.id).toBe("t3");
  });

  it("does not stop at a position gap", () => {
    // the old portal truncated the document at the first missing position
    const pages = buildPages([published("p1", 1), published("p5", 5)]);
    expect(pages).toHaveLength(2);
    expect(pages[1].published?.id).toBe("p5");
  });

  it("appends files without a position as their own pages", () => {
    const pages = buildPages([published("p1", 1), published("loose")]);
    expect(pages).toHaveLength(2);
    expect(pages[1].published?.id).toBe("loose");
  });

  it("decides the kind from what the page can show", () => {
    const pages = buildPages([
      tile("z1", 1),
      published("p2", 2),
      published("pdf", 3, "application/pdf"),
    ]);
    expect(pages.map((page) => page.kind)).toEqual(["deepZoom", "image", "download"]);
  });

  it("a page is selected when any of its renditions is", () => {
    const files = [published("p1", 1), { ...thumbnail("t2", 2), selected: true }];
    expect(buildPages(files).map((page) => page.selected)).toEqual([false, true]);
  });
});

describe("initialPageIndex", () => {
  const pages = buildPages([
    published("p1", 1),
    { ...published("p2", 2), selected: true },
    thumbnail("t3", 3),
  ]);

  it("prefers the deep-linked file over the selected page", () => {
    expect(initialPageIndex(pages, "t3")).toBe(2);
  });

  it("falls back to the selected page, then the first", () => {
    expect(initialPageIndex(pages, null)).toBe(1);
    expect(initialPageIndex(pages, "unknown")).toBe(1);
    expect(initialPageIndex(buildPages([published("p1", 1)]), null)).toBe(0);
  });
});

describe("pageFileId", () => {
  it("identifies a page by its best rendition", () => {
    const pages = buildPages([thumbnail("t1", 1), tile("z1", 1)]);
    expect(pageFileId(pages[0])).toBe("z1");
  });
});
