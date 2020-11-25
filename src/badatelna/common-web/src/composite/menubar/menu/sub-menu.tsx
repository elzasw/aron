import React, { useState } from 'react';
import clsx from 'clsx';
import MuiMenuList from '@material-ui/core/MenuList';
import Divider from '@material-ui/core/Divider';
import { useEventCallback } from 'utils/event-callback-hook';
import { SubmenuProps } from './menu-types';
import { useStyles } from './menu-styles';
import { SubMenuItem } from './sub-menu-item';
import { MenubarClassOverrides } from '../menubar-types';

export function SubMenu({
  items,
  opened,
  topLevel,
  classOverrides,
}: SubmenuProps & MenubarClassOverrides) {
  const classes = useStyles();

  const [openedItem, setOpenedItem] = useState<number | null>(null);

  const handleItemHover = useEventCallback((index: number) => {
    setOpenedItem(index);
  });

  return (
    <>
      {opened && (
        <MuiMenuList
          classes={{
            root: clsx(classes.subMenu, classOverrides?.subMenu, {
              [classes.subMenuTopLevel]: topLevel,
            }),
          }}
          disablePadding={true}
        >
          {items.map((item, index) =>
            item.separator ? (
              <Divider key={index} variant="middle" />
            ) : (
              <SubMenuItem
                item={item}
                key={index}
                index={index}
                onHover={handleItemHover}
                opened={index === openedItem}
                classOverrides={classOverrides}
              />
            )
          )}
        </MuiMenuList>
      )}
    </>
  );
}
