import React from 'react';
import { useEffect } from 'react';

/**
 * Fires form subscription handlers every time it is rerendered.
 */
export function Subscriber({
  fireSubscriptions,
}: {
  fireSubscriptions: () => void;
}) {
  /**
   * Fires subscription handlers on every change.
   */
  useEffect(() => {
    fireSubscriptions();
  });

  return <></>;
}
