import { useCallback, useSyncExternalStore } from "react";

/**
 * Whether the viewport currently matches a CSS media query, updated live.
 * For layout decisions the stylesheet cannot make - where the query changes
 * what is rendered, not how it is laid out.
 */
export default function useMediaQuery(query: string): boolean {
  const subscribe = useCallback(
    (onChange: () => void) => {
      const list = window.matchMedia(query);
      list.addEventListener("change", onChange);
      return () => list.removeEventListener("change", onChange);
    },
    [query],
  );
  return useSyncExternalStore(subscribe, () => window.matchMedia(query).matches);
}
