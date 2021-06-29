import React from 'react';
import { useLocation } from 'react-router-dom';
import clsx from 'clsx';
import MuiMenuItem from '@material-ui/core/MenuItem';
import Typography from '@material-ui/core/Typography';
import ClickAwayListener from '@material-ui/core/ClickAwayListener';
import { useEventCallback } from 'utils/event-callback-hook';
import { MenuItemProps } from './menu-types';
import { useStyles } from './menu-styles';
import { SubMenu } from './sub-menu';
import { MenubarClassOverrides } from '../menubar-class-overrides-types';

export function MenuItem({
  item,
  index,
  opened,
  onClick,
  onHover,
  onClickAway,
  classOverrides,
}: MenuItemProps & MenubarClassOverrides) {
  const classes = useStyles();

  const handleClick = useEventCallback(() => {
    onClick(index);
  });

  const handleHover = useEventCallback(() => {
    onHover(index);
  });

  const handleClickAway = useEventCallback(() => {
    onClickAway(index);
  });

  const { pathname } = useLocation();

  return (
    <ClickAwayListener onClickAway={handleClickAway}>
      <MuiMenuItem
        className={clsx(
          classes.menuItem,
          classOverrides?.menuItem,
          item?.isActive?.(pathname) && [
            classes.activeMenuItem,
            classOverrides?.activeMenuItem,
          ]
        )}
        onMouseEnter={handleHover}
        onClick={handleClick}
      >
        <Typography
          variant="inherit"
          className={clsx(classes.menuItemText, classOverrides?.menuItemText)}
        >
          {item.label}
        </Typography>

        {opened && (
          <SubMenu
            topLevel
            items={item.items ?? []}
            opened={opened}
            classOverrides={classOverrides}
          />
        )}
      </MuiMenuItem>
    </ClickAwayListener>
  );
}
