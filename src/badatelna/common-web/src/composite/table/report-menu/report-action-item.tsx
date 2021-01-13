import React, { forwardRef } from 'react';
import MenuItem from '@material-ui/core/MenuItem';
import { ReportActionItemProps } from './report-action-types';

export function ReportActionItemWithoutRef(
  { action, closeMenu }: ReportActionItemProps,
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

export const ReportActionItem = forwardRef(ReportActionItemWithoutRef);
