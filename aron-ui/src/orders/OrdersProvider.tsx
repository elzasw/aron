import {
  createContext,
  type ReactNode,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from "react";
import { type AccessType, type BasketItem, groupKey, isBasketItem } from "./basket";

/**
 * The basket lives in the browser, as the old portal's did: an order is put
 * together over several records and often several visits, and the portal has
 * no accounts to keep it under.
 */
const STORAGE_KEY = "aron.orderItems";

export interface Orders {
  /** Basket items in the order they were added. */
  items: BasketItem[];

  /** Adds a record; one already in the basket is replaced, not doubled. */
  add: (item: BasketItem) => void;

  remove: (uuid: string) => void;

  setAccessType: (uuid: string, accessType: AccessType) => void;

  /** Removes the items of one order (see `groupKey`). */
  removeGroup: (key: string) => void;

  clear: () => void;
}

const OrdersContext = createContext<Orders | null>(null);

function readStored(): BasketItem[] {
  try {
    const parsed: unknown = JSON.parse(window.localStorage.getItem(STORAGE_KEY) ?? "[]");
    return Array.isArray(parsed) ? parsed.filter(isBasketItem) : [];
  } catch {
    // unreadable or unavailable storage is an empty basket, not an error page
    return [];
  }
}

function store(items: BasketItem[]): void {
  try {
    if (items.length === 0) {
      window.localStorage.removeItem(STORAGE_KEY);
    } else {
      window.localStorage.setItem(STORAGE_KEY, JSON.stringify(items));
    }
  } catch {
    // storage full or disabled: the basket still works until the tab closes
  }
}

/**
 * Holds the reader's basket for the whole application - the record page adds
 * to it, the header shows it. A context because those two sit in different
 * branches of the tree and neither owns the other.
 */
export function OrdersProvider({ children }: { children: ReactNode }) {
  const [items, setItems] = useState<BasketItem[]>(readStored);

  useEffect(() => {
    // another tab of the portal changed the basket: one reader, one basket
    function onStorage(event: StorageEvent): void {
      if (event.key === STORAGE_KEY || event.key === null) {
        setItems(readStored());
      }
    }
    window.addEventListener("storage", onStorage);
    return () => window.removeEventListener("storage", onStorage);
  }, []);

  const change = useCallback((next: (current: BasketItem[]) => BasketItem[]) => {
    setItems((current) => {
      const changed = next(current);
      store(changed);
      return changed;
    });
  }, []);

  const orders = useMemo<Orders>(
    () => ({
      items,
      add: (item) =>
        change((current) => [...current.filter((other) => other.uuid !== item.uuid), item]),
      remove: (uuid) => change((current) => current.filter((item) => item.uuid !== uuid)),
      setAccessType: (uuid, accessType) =>
        change((current) =>
          current.map((item) => (item.uuid === uuid ? { ...item, accessType } : item)),
        ),
      removeGroup: (key) => change((current) => current.filter((item) => groupKey(item) !== key)),
      clear: () => change(() => []),
    }),
    [items, change],
  );

  return <OrdersContext.Provider value={orders}>{children}</OrdersContext.Provider>;
}

export function useOrders(): Orders {
  const orders = useContext(OrdersContext);
  if (orders === null) {
    throw new Error("useOrders needs an OrdersProvider above it");
  }
  return orders;
}
