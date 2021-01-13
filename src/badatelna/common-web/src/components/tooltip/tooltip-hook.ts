import React, { useState, ReactNode } from 'react';
import { useEventCallback } from 'utils/event-callback-hook';

export function useTooltip(
  type: 'HOVER' | 'CLICKABLE',
  children: ReactNode,
  childrenProps: any
) {
  const [opened, setOpened] = useState(false);

  const openTooltip = useEventCallback(() => setOpened(true));
  const closeTooltip = useEventCallback(() => setOpened(false));

  const wrappedChildren = React.Children.toArray(children)
    .filter(React.isValidElement)
    .map((child) => React.cloneElement(child, childrenProps))[0];

  return {
    opened,
    openTooltip,
    closeTooltip,
    wrappedChildren,
  };
}
