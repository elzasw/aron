import React, { forwardRef } from 'react';
import MenuItem from '@material-ui/core/MenuItem';
import { ExportActionItemProps } from './export-action-types';

export function ExportActionItemWithoutRef(
  { action, closeMenu }: ExportActionItemProps,
  ref: React.Ref<any>
) {
  return (
    <MenuItem
      ref={ref}
      onClick={() => {
        action.action();
        closeMenu();
      }}
    >
      <>
        {action.label}
        {action.Component && <action.Component />}
      </>
    </MenuItem>
  );
}

export const ExportActionItem = forwardRef(ExportActionItemWithoutRef);
