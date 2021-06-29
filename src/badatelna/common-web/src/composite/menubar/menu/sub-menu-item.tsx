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
import { MenubarClassOverrides } from '../menubar-class-overrides-types';

export function SubMenuItem({
  item,
  index,
  opened,
  onHover,
  classOverrides,
  SubMenuComponent,
}: SubmenuItemProps & MenubarClassOverrides) {
  const classes = useStyles();

  const handleMouseEnter = useEventCallback(() => {
    onHover(index);
  });

  const keyShortcut = item.keyShortcutLabel ?? item.keyShortcut;

  return (
    <MuiMenuItem
      component={item.href ? 'a' : 'div'}
      href={item.href}
      onClick={(e: React.MouseEvent) => {
        e.preventDefault();

        if (item.onClick) {
          item.onClick();
        }
      }}
      onMouseEnter={handleMouseEnter}
      className={clsx(classOverrides?.subMenuItem)}
      classes={{ root: classes.subMenuItem }}
    >
      <ListItemIcon classes={{ root: clsx(classOverrides?.subMenuItemIcon) }}>
        {item.icon && <Icon>{item.icon}</Icon>}
      </ListItemIcon>
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
        <SubMenuComponent
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
