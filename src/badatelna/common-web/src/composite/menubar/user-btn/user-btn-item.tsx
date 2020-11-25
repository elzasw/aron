import React, { forwardRef } from 'react';
import MenuItem from '@material-ui/core/MenuItem';
import ListItemText from '@material-ui/core/ListItemText';
import { UserBtnItemProps } from './user-btn-types';
import { useEventCallback } from 'utils/event-callback-hook';

export const UserBtnItem = forwardRef<any, UserBtnItemProps>(
  function UserBtnItem(
    { action: { label, action }, onClose }: UserBtnItemProps,
    ref
  ) {
    const handleClick = useEventCallback(() => {
      if (action !== undefined) {
        action();
        onClose();
      }
    });

    return (
      <MenuItem ref={ref}>
        <ListItemText primary={label} onClick={handleClick} />
      </MenuItem>
    );
  }
);
