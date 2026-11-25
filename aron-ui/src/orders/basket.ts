import type { OrderInstitution, OrderItem, OrderReadingRoom } from "../api/generated";

/**
 * How the reader wants an ordered record made accessible - the order service's
 * own vocabulary, sent to it verbatim. All three are offered on every item, the
 * first being the default (the old portal's behaviour).
 */
export const ACCESS_TYPES = ["NAHLIZENI", "REPLIKA", "OVERENI_SHODY"] as const;

export type AccessType = (typeof ACCESS_TYPES)[number];

export const DEFAULT_ACCESS_TYPE: AccessType = "NAHLIZENI";

/** One record in the reader's basket: the server's order item plus the reader's choices. */
export interface BasketItem extends OrderItem {
  /** The record, for the link back to it and for finding the item again. */
  uuid: string;

  accessType: AccessType;
}

/**
 * The items one order carries: an order is addressed to one archive - and to
 * one of its reading rooms where the item names one - so the basket is sent
 * group by group.
 */
export interface BasketGroup {
  key: string;
  institution: OrderInstitution;
  badatelna?: OrderReadingRoom;
  items: BasketItem[];
}

export function groupKey(item: Pick<OrderItem, "institution" | "badatelna">): string {
  return item.badatelna
    ? `${item.institution.code}_${item.badatelna.code}`
    : item.institution.code;
}

/** The basket's groups, in the order their first item was added. */
export function groupsOf(items: BasketItem[]): BasketGroup[] {
  const groups = new Map<string, BasketGroup>();
  for (const item of items) {
    const key = groupKey(item);
    const group = groups.get(key);
    if (group) {
      group.items.push(item);
    } else {
      groups.set(key, {
        key,
        institution: item.institution,
        badatelna: item.badatelna,
        items: [item],
      });
    }
  }
  return [...groups.values()];
}

/** "Archive" or "Archive — Reading room": whom a group's order goes to. */
export function groupLabel(group: Pick<BasketGroup, "institution" | "badatelna">): string {
  return group.badatelna
    ? `${group.institution.name} — ${group.badatelna.name}`
    : group.institution.name;
}

/** What the basket and the form call an item: its name, else its fund's number. */
export function itemName(item: BasketItem): string {
  return item.archivalie.popis ?? `NAD ${item.archivalie.cisloNad}`;
}

/**
 * Whether a value read back from storage is an item this build can work with.
 * Storage outlives a release and can be edited by anyone, and one malformed
 * entry would otherwise break the header on every page.
 */
export function isBasketItem(value: unknown): value is BasketItem {
  if (typeof value !== "object" || value === null) {
    return false;
  }
  const item = value as Partial<BasketItem>;
  return (
    typeof item.uuid === "string" &&
    (ACCESS_TYPES as readonly unknown[]).includes(item.accessType) &&
    typeof item.archivalie === "object" &&
    item.archivalie !== null &&
    typeof item.archivalie.cisloNad === "number" &&
    typeof item.institution?.code === "string" &&
    typeof item.institution.name === "string" &&
    (item.badatelna === undefined ||
      (typeof item.badatelna?.code === "string" && typeof item.badatelna.name === "string"))
  );
}
