import { useState, useCallback } from 'react';

export function useRef() {
  const [ref, setRef] = useState(null);

  const handleRef = useCallback((node) => {
    setRef(node);
  }, []);

  return [ref as any, handleRef];
}
