import { useCallback, useSyncExternalStore, type RefObject } from "react";

/**
 * The browser's Fullscreen API for one element: whether it is offered here at
 * all (iPhone Safari has no such API - the caller then shows no control, and
 * the short-viewport layout is what serves that reader), whether the element
 * is the fullscreen element now, and a toggle. State follows the document's
 * `fullscreenchange`, so leaving through Escape is seen too.
 */
export default function useFullscreen(ref: RefObject<HTMLElement | null>) {
  const enabled = document.fullscreenEnabled === true;
  const active = useSyncExternalStore(
    (onChange) => {
      document.addEventListener("fullscreenchange", onChange);
      return () => document.removeEventListener("fullscreenchange", onChange);
    },
    () => ref.current !== null && document.fullscreenElement === ref.current,
  );
  const toggle = useCallback(() => {
    const element = ref.current;
    if (element === null) {
      return;
    }
    if (document.fullscreenElement === element) {
      void document.exitFullscreen();
    } else {
      void element.requestFullscreen();
    }
  }, [ref]);
  return { enabled, active, toggle };
}
