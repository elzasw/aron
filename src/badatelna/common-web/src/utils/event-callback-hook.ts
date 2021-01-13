import { useRef, useLayoutEffect, useCallback } from 'react';

/**
 * Aimed to be easier to use than useCallback and solve problems raised in this ticket.
 *
 * useEventCallback doesn't need any dependencies list. The returned function should not be used during rendering.
 *
 */
export function useEventCallback<T extends (...args: any[]) => any>(fn: T): T {
  const ref: any = useRef(fn);

  // we copy a ref to the callback scoped to the current state/props on each render
  useLayoutEffect(() => {
    ref.current = fn;
  });

  return useCallback(
    (...args: any[]) => ref.current.apply(void 0, args),
    []
  ) as T;
}
