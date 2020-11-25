import React, { useRef, forwardRef } from 'react';
import MenuItem from '@material-ui/core/MenuItem';
import { BulkActionItemProps } from './bulk-action-types';

export function BulkActionItemWithoutRef(
  { action, closeMenu }: BulkActionItemProps,
  ref: React.Ref<any>
) {
  const componentRef = useRef(null);

  return (
    <MenuItem
      ref={ref}
      onClick={() => {
        action.action(componentRef.current);
        closeMenu();
      }}
    >
      <>
        {action.label}
        {action.Component && <action.Component ref={componentRef} />}
      </>
    </MenuItem>
  );
}

export const BulkActionItem = forwardRef(BulkActionItemWithoutRef);
