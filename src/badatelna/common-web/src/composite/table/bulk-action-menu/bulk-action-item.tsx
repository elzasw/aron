import React, { useRef, useContext, forwardRef } from 'react';
import MenuItem from '@material-ui/core/MenuItem';
import { BulkActionItemProps } from './bulk-action-types';
import { TableSelectedContext } from '../table-context';

export const BulkActionItem = forwardRef<HTMLDivElement, BulkActionItemProps>(
  function BulkActionItem({ action, closeMenu }, ref) {
    const componentRef = useRef(null);

    const { selected } = useContext(TableSelectedContext);
    const disabled = action.disableFilteredBulkAction && selected.length === 0;

    return (
      <MenuItem
        component="div"
        ref={ref}
        onClick={() => {
          action.action(componentRef.current);
          closeMenu();
        }}
        disabled={disabled}
      >
        <>
          {action.label}
          {action.Component && <action.Component ref={componentRef} />}
        </>
      </MenuItem>
    );
  }
);
