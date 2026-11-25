import { type RefObject, useCallback, useEffect, useRef } from "react";

/**
 * Where the focus goes when a dialog opened from inside another dialog closes:
 * back to the control that opened it, or - when that control is gone, because
 * the action it started removed it (a basket group that was sent or emptied) -
 * to a control that is always there.
 *
 * Fluent returns the focus by itself only to elements marked as targets, and
 * falls back to the last one still in the page: for a dialog opened from the
 * basket that is the header's basket button, behind the basket. The focus then
 * leaves the modal, and the focus management hides the basket from assistive
 * technology while it is still on the screen. So the return is done here.
 *
 * @param nestedOpen whether a dialog opened from this one is open
 * @param fallback   the control to focus when the opener no longer exists
 * @returns call it with the opening control when opening the nested dialog
 */
export function useReturnFocus(
  nestedOpen: boolean,
  fallback: RefObject<HTMLElement>,
): (opener: HTMLElement) => void {
  const opener = useRef<HTMLElement | null>(null);

  useEffect(() => {
    if (nestedOpen || opener.current === null) {
      return;
    }
    const element = opener.current;
    opener.current = null;
    (element.isConnected ? element : fallback.current)?.focus();
  }, [nestedOpen, fallback]);

  return useCallback((element: HTMLElement) => {
    opener.current = element;
  }, []);
}
