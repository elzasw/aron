import React, { forwardRef, PropsWithChildren } from 'react';
import { ItemWrapperProps } from './dnd-grid-types';

export const ItemWrapper = forwardRef<
  HTMLDivElement,
  PropsWithChildren<ItemWrapperProps>
>(function ItemWrapper({ overlay = false, style, children, ...props }, ref) {
  const inlineStyles: React.CSSProperties = {
    opacity: overlay ? '0.2' : '1',
    ...style,
  };

  return (
    <div ref={ref} style={inlineStyles} {...props}>
      {children}
    </div>
  );
});
