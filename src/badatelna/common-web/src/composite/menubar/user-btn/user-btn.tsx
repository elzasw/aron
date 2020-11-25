import React, { useState, useRef, useContext } from 'react';
import clsx from 'clsx';
import { useEventCallback } from 'utils/event-callback-hook';
import Button from '@material-ui/core/Button';
import MuiMenu from '@material-ui/core/Menu';
import PersonIcon from '@material-ui/icons/Person';
import { useStyles } from './user-btn-styles';
import { UserBtnProps } from './user-btn-types';
import { abortableFetch } from 'utils/abortable-fetch';
import { UserContext } from 'common/user/user-context';
import { UserBtnItem } from './user-btn-item';
import { MenubarClassOverrides } from '../menubar-types';

/**
 * Logout call
 *
 * @param logoutUrl Url of logout service
 */
export function logout(logoutUrl: string) {
  return abortableFetch(logoutUrl, {
    method: 'POST',
  });
}

export function UserBtn({
  logoutSuccessUrl,
  logoutUrl,
  actions = [],
  classOverrides,
}: UserBtnProps & MenubarClassOverrides) {
  const classes = useStyles();

  const { user } = useContext(UserContext);

  const anchorRef = useRef<HTMLButtonElement | null>(null);
  const [opened, setOpened] = useState<boolean>(false);

  const openMenu = useEventCallback(() => {
    setOpened(true);
  });

  const closeMenu = useEventCallback(() => {
    setOpened(false);
  });

  const handleLogoutClick = useEventCallback(async () => {
    const response = logout(logoutUrl);
    await response.none();

    window.location.href = logoutSuccessUrl;
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
        <UserBtnItem
          action={{ label: 'Odhlásit', action: handleLogoutClick }}
          onClose={closeMenu}
        />
      </MuiMenu>
    </>
  );
}
