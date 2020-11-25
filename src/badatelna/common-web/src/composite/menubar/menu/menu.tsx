import React, { useState } from 'react';
import clsx from 'clsx';
import MuiMenuList from '@material-ui/core/MenuList';
import { useEventCallback } from 'utils/event-callback-hook';
import { MenuItem } from './menu-item';
import { MenuProps } from './menu-types';
import { useStyles } from './menu-styles';
import { MenubarClassOverrides } from '../menubar-types';

export function Menu({
  items,
  classOverrides,
}: MenuProps & MenubarClassOverrides) {
  const classes = useStyles();

  const [openedItem, setOpenedItem] = useState<number | null>(null);

  const handleClick = useEventCallback((index: number) => {
    if (openedItem !== index) {
      setOpenedItem(index);
    } else {
      setOpenedItem(null);
    }
  });

  const handleHover = useEventCallback((index: number) => {
    if (openedItem !== null) {
      setOpenedItem(index);
    }
  });

  const handleClickAway = useEventCallback((index: number) => {
    if (openedItem === index) {
      setOpenedItem(null);
    }
  });

  return (
    <MuiMenuList
      disablePadding={true}
      className={clsx(classes.menu, classOverrides?.menu)}
    >
      {items.map((item, index) => (
        <MenuItem
          item={item}
          key={index}
          index={index}
          onClick={handleClick}
          onHover={handleHover}
          onClickAway={handleClickAway}
          opened={index === openedItem}
          classOverrides={classOverrides}
        />
      ))}
    </MuiMenuList>
  );
}
