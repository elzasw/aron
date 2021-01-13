import { useState, useCallback, useLayoutEffect, RefObject } from 'react';
import ResizeObserver from 'resize-observer-polyfill';

function getSize(el: Element) {
  if (!el) {
    return {
      width: 0,
      height: 0,
    };
  }

  const boundingRect = el.getBoundingClientRect();
  return {
    width: boundingRect.width,
    height: boundingRect.height,
  };
}

export function useComponentSize(
  elemOrRef: Element | HTMLDocument | RefObject<any>
) {
  const el =
    elemOrRef &&
    (elemOrRef instanceof Element || elemOrRef instanceof HTMLDocument
      ? elemOrRef
      : elemOrRef.current);

  const [ComponentSize, setComponentSize] = useState(getSize(el));

  const handleResize = useCallback(
    function handleResize() {
      if (el) {
        setComponentSize(getSize(el));
      }
    },
    [el]
  );

  useLayoutEffect(
    function () {
      if (!el) {
        return;
      }

      handleResize();
      const resizeObserver = new ResizeObserver(function () {
        handleResize();
      });
      resizeObserver.observe(el);

      return function () {
        resizeObserver.disconnect();
      };
    },
    [el, handleResize]
  );

  return ComponentSize;
}
