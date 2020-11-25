import { useRef, useLayoutEffect, useCallback } from 'react';

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
