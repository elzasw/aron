import React from 'react';
import clsx from 'clsx';
import Icon from '@material-ui/core/Icon';
import Typography from '@material-ui/core/Typography';
import ListItemIcon from '@material-ui/core/ListItemIcon';
import ListItemSecondaryAction from '@material-ui/core/ListItemSecondaryAction';
import MuiMenuItem from '@material-ui/core/MenuItem';
import { useEventCallback } from 'utils/event-callback-hook';
import { SubmenuItemProps } from './menu-types';
import { useStyles } from './menu-styles';
import { SubMenu } from './sub-menu';
import { MenubarClassOverrides } from '../menubar-types';

export function SubMenuItem({
  item,
  index,
  opened,
  onHover,
  classOverrides,
}: SubmenuItemProps & MenubarClassOverrides) {
  const classes = useStyles();

  const handleMouseEnter = useEventCallback(() => {
    onHover(index);
  });

  const keyShortcut = item.keyShortcutLabel ?? item.keyShortcut;

  return (
    <MuiMenuItem
      onClick={item.onClick}
      onMouseEnter={handleMouseEnter}
      className={clsx(classOverrides?.subMenuItem)}
      classes={{ root: classes.subMenuItem }}
    >
      <ListItemIcon>{item.icon && <Icon>{item.icon}</Icon>}</ListItemIcon>
      <Typography
        variant="inherit"
        className={clsx(
          classes.subMenuItemText,
          classOverrides?.subMenuItemText
        )}
      >
        {item.label}
      </Typography>
      {item.items?.length && (
        <SubMenu
          items={item.items}
          opened={opened}
          classOverrides={classOverrides}
        />
      )}
      <ListItemSecondaryAction
        className={clsx(classes.subMenuAction, classOverrides?.subMenuAction)}
      >
        {keyShortcut && <span className={classes.shortcut}>{keyShortcut}</span>}

        {(item.items?.length ?? 0) > 0 && (
          <Icon
            className={clsx(classes.subMenuArrow, classOverrides?.subMenuArrow)}
          >
            play_arrow
          </Icon>
        )}
      </ListItemSecondaryAction>
    </MuiMenuItem>
  );
}
