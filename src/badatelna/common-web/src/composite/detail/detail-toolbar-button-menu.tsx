import React, { useRef, useState, forwardRef } from 'react';
import clsx from 'clsx';
import { Tooltip } from 'components/tooltip/tooltip';
import Divider from '@material-ui/core/Divider';
import Button, { ButtonProps } from '@material-ui/core/Button';
import MuiMenu from '@material-ui/core/Menu';
import MenuItem from '@material-ui/core/MenuItem';
import ListItemIcon from '@material-ui/core/ListItemIcon';
import ListItemText from '@material-ui/core/ListItemText';
import ExpandMoreIcon from '@material-ui/icons/ExpandMore';
import {
  DetailToolbarButtonType,
  DetailToolbarButtonMenuProps,
  DetailToolbarButtonMenuItem,
} from './detail-types';
import { useStyles } from './detail-styles';
import { useEventCallback } from 'utils/event-callback-hook';

export function DetailToolbarButtonMenu({
  label,
  tooltip,
  disabled,
  type = DetailToolbarButtonType.NORMAL,
  items,
  startIcon,
  ...restProps
}: DetailToolbarButtonMenuProps) {
  const classes = useStyles();

  const buttonProps: ButtonProps = { ...restProps };

  if (type === DetailToolbarButtonType.PRIMARY) {
    buttonProps.color = 'primary';
    buttonProps.variant = 'contained';
    buttonProps.classes = { root: classes.toolbarMainButton };
  } else if (type === DetailToolbarButtonType.SECONDARY) {
    buttonProps.color = 'secondary';
    buttonProps.variant = 'contained';
    buttonProps.classes = { root: classes.toolbarMainButton };
  }

  const anchorRef = useRef<HTMLButtonElement | null>(null);
  const [opened, setOpened] = useState<boolean>(false);

  const openMenu = useEventCallback(() => {
    setOpened(true);
  });

  const closeMenu = useEventCallback(() => {
    setOpened(false);
  });

  return (
    <>
      <Tooltip title={tooltip} placement="top-start">
        <Button
          ref={anchorRef}
          onClick={openMenu}
          disabled={disabled}
          classes={{ root: classes.toolbarButton }}
          {...buttonProps}
          variant="outlined"
          startIcon={startIcon}
          endIcon={<ExpandMoreIcon />}
        >
          {label}
        </Button>
      </Tooltip>
      <MuiMenu
        getContentAnchorEl={null}
        anchorEl={anchorRef.current}
        open={opened}
        onClose={closeMenu}
        anchorOrigin={{
          vertical: 'bottom',
          horizontal: 'right',
        }}
        transformOrigin={{
          vertical: 'top',
          horizontal: 'right',
        }}
        MenuListProps={{
          disablePadding: true,
        }}
      >
        {items.map((item, i) => (
          <Item item={item} key={i} onClose={closeMenu} />
        ))}
      </MuiMenu>
    </>
  );
}

interface ItemProps {
  item: DetailToolbarButtonMenuItem;
  onClose: () => void;
}

const Item = forwardRef<any, ItemProps>(function Item(
  {
    item: { label, tooltip, onClick, href, Icon, divider, warning },
    onClose,
  }: ItemProps,
  ref
) {
  const classes = useStyles();

  const handleClick = useEventCallback(() => {
    onClick();
    onClose();
  });

  return divider ? (
    <Divider />
  ) : (
    <Tooltip title={tooltip} placement="right-start">
      <MenuItem
        ref={ref}
        component={Button}
        onClick={handleClick}
        href={href}
        className={clsx(classes.toolbarButton, classes.toolbarButtonMenu, {
          [classes.toolbarButtonWarning]: warning,
        })}
      >
        {Icon && <ListItemIcon>{Icon}</ListItemIcon>}
        <ListItemText primary={label} />
      </MenuItem>
    </Tooltip>
  );
});
