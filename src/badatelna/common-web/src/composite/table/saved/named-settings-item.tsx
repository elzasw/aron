import React, { forwardRef } from 'react';
import MenuItem from '@material-ui/core/MenuItem';
import ListItemIcon from '@material-ui/core/ListItemIcon';
import ArrowRightIcon from '@material-ui/icons/ArrowRight';
import { NamedSettingsItemProps } from './named-settings-types';

export function NamedSettingsItemWithoutRef(
  { label, onClick, closeMenu, selected }: NamedSettingsItemProps,
  ref: React.Ref<any>
) {
  return (
    <MenuItem
      ref={ref}
      onClick={() => {
        onClick();
        closeMenu();
      }}
    >
      <ListItemIcon>
        {selected && <ArrowRightIcon fontSize="small" />}
      </ListItemIcon>
      <>{label}</>
    </MenuItem>
  );
}

export const NamedSettingsItem = forwardRef(NamedSettingsItemWithoutRef);
