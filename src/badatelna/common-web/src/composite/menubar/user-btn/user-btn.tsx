import React, { useState, useRef, useContext } from 'react';
import clsx from 'clsx';
import { useEventCallback } from 'utils/event-callback-hook';
import Button from '@material-ui/core/Button';
import MuiMenu from '@material-ui/core/Menu';
import PersonIcon from '@material-ui/icons/Person';
import { useStyles } from './user-btn-styles';
import { UserBtnProps } from './user-btn-types';
import { UserContext } from 'common/user/user-context';
import { UserBtnItem } from './user-btn-item';
import { FormattedMessage } from 'react-intl';
import { MenubarClassOverrides } from '../menubar-class-overrides-types';

export function UserBtn({
  actions = [],
  classOverrides,
  displayLogoutBtn = true,
}: UserBtnProps & MenubarClassOverrides) {
  const classes = useStyles();

  const { user, logout } = useContext(UserContext);

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
      <Button
        ref={anchorRef}
        variant="contained"
        color="primary"
        onClick={openMenu}
        disableElevation
        className={clsx(classOverrides?.userButton)}
        startIcon={<PersonIcon className={classes.personIcon} />}
      >
        {user?.name}
      </Button>
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
        PaperProps={{
          className: clsx(classOverrides?.userMenu),
        }}
        MenuListProps={{
          disablePadding: true,
        }}
      >
        {actions.map((action, i) => (
          <UserBtnItem action={action} key={i} onClose={closeMenu} />
        ))}
        {displayLogoutBtn && (
          <UserBtnItem
            action={{
              label: (
                <FormattedMessage
                  id="EAS_MENU_BTN_LOGOUT"
                  defaultMessage="Odhlásit"
                />
              ),
              action: logout,
            }}
            onClose={closeMenu}
          />
        )}
      </MuiMenu>
    </>
  );
}
