import React, { PropsWithChildren } from 'react';
import clsx from 'clsx';
import Grid from '@material-ui/core/Grid';
import { MenubarProps, MenubarClassOverrides } from './menubar-types';
import { useStyles } from './menubar-styles';
import { Menu } from './menu/menu';
import { UserBtn } from './user-btn/user-btn';
import { useMenubar } from './menubar-hook';
import { MenubarContext } from './menubar-context';

export function Menubar({
  logo,
  logoUrl = '/',
  logoutSuccessUrl = '/',
  logoutUrl = '/logout',
  title,
  items: providedItems,
  children,
  userBtnActions,
  classOverrides,
}: PropsWithChildren<MenubarProps & MenubarClassOverrides>) {
  const classes = useStyles();

  const { handleLogoClick, items, context } = useMenubar({
    logoUrl,
    items: providedItems,
  });

  return (
    <MenubarContext.Provider value={context}>
      <>
        <div
          className={clsx(
            classes.menuBarWrapper,
            classOverrides?.menuBarWrapper
          )}
        >
          <a
            onClick={handleLogoClick}
            className={clsx(classes.iconlink, classOverrides?.iconlink)}
          >
            {logo}
          </a>

          <div
            className={clsx(
              classes.menuBarInnerWrapper,
              classOverrides?.menuBarInnerWrapper
            )}
          >
            <Grid
              container
              className={clsx(
                classes.menuContainer,
                classOverrides?.menuContainer
              )}
            >
              <Grid>{title}</Grid>
              <Grid>
                <UserBtn
                  logoutUrl={logoutUrl}
                  logoutSuccessUrl={logoutSuccessUrl}
                  actions={userBtnActions}
                  classOverrides={classOverrides}
                />
              </Grid>
            </Grid>
            <Menu items={items} classOverrides={classOverrides} />
          </div>
        </div>
        {children}
      </>
    </MenubarContext.Provider>
  );
}
