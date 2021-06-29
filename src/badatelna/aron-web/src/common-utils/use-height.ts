import { useState, useEffect, useCallback } from 'react';

export const useHeight = () => {
  const [height, setHeight] = useState(window.innerHeight);

  // use debounce if needed
  const touchmoveListener = useCallback(
    () => setHeight(window.innerHeight),
    []
  );

  useEffect(() => {
    window.addEventListener('touchmove', touchmoveListener);
  }, [touchmoveListener]);

  return height;
};
