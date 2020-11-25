import { useRef } from 'react';

export function useFirstRender(callback: () => void) {
  const firstRender = useRef<boolean>(true);

  if (firstRender.current) {
    firstRender.current = false;
    callback();
  }
}
