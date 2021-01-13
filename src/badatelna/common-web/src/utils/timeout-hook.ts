import { useRef, useCallback } from 'react';

export function useTimeout() {
  const timeoutIdRef = useRef<number>();

  const cancel = useCallback(() => {
    const timeoutId = timeoutIdRef.current;
    if (timeoutId) {
      timeoutIdRef.current = undefined;
      clearTimeout(timeoutId);
    }
  }, []);

  const trigger = useCallback(
    (callback: () => void, timeout = 0) => {
      cancel();

      timeoutIdRef.current = window.setTimeout(callback, timeout);
    },
    [cancel]
  );

  return [trigger, cancel] as [typeof trigger, typeof cancel];
}
