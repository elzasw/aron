import { ApuType, MenuItemCode } from "./api/generated";

/**
 * Section metadata of the portal. Which sections a deployment shows (and their
 * accent colors) comes from /api/v1/ui/config; this map supplies the in-app
 * route, the i18n label key and the default accent color for each code.
 *
 * Top-level routes are enumerated on both sides: every route here must have a
 * mapping in App.tsx AND in the server-side IndexController.
 */
export interface SectionDef {
  /** In-app route; undefined = external-URL-only item (HELP). */
  route?: string;
  labelKey: string;
  defaultColor: string;
}

export const SECTIONS: Record<MenuItemCode, SectionDef> = {
  [MenuItemCode.Institution]: {
    route: "/institution",
    labelKey: "sections.INSTITUTION",
    defaultColor: "#4e937a",
  },
  [MenuItemCode.Fund]: {
    route: "/fund",
    labelKey: "sections.FUND",
    defaultColor: "#5f5bc2",
  },
  [MenuItemCode.FindingAid]: {
    route: "/finding-aid",
    labelKey: "sections.FINDING_AID",
    defaultColor: "#b55d7c",
  },
  [MenuItemCode.ArchDesc]: {
    route: "/arch-desc",
    labelKey: "sections.ARCH_DESC",
    defaultColor: "#79a7d1",
  },
  [MenuItemCode.Entity]: {
    route: "/entity",
    labelKey: "sections.ENTITY",
    defaultColor: "#fdad3c",
  },
  [MenuItemCode.Originator]: {
    route: "/originator",
    labelKey: "sections.ORIGINATOR",
    defaultColor: "#7c8f4f",
  },
  [MenuItemCode.News]: {
    route: "/news",
    labelKey: "sections.NEWS",
    defaultColor: "#c2574f",
  },
  [MenuItemCode.Help]: {
    labelKey: "sections.HELP",
    // must stay visible against the navy header background
    defaultColor: "#3e6ca3",
  },
};

/**
 * Section a record of this type belongs to - what the breadcrumb strip names
 * between the home page and the record's own path. Partial by nature: a record
 * type the menu has no section for (COLLECTION) simply contributes no crumb.
 */
export const SECTION_OF_APU_TYPE: Partial<Record<ApuType, MenuItemCode>> = {
  [ApuType.Institution]: MenuItemCode.Institution,
  [ApuType.Fund]: MenuItemCode.Fund,
  [ApuType.FindingAid]: MenuItemCode.FindingAid,
  [ApuType.ArchDesc]: MenuItemCode.ArchDesc,
  [ApuType.Entity]: MenuItemCode.Entity,
};
