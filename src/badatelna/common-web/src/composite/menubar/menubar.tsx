import React, { PropsWithChildren } from 'react';
import { GlobalHotKeys } from 'react-hotkeys';
import { noop } from 'lodash';
import clsx from 'clsx';
import Grid from '@material-ui/core/Grid';
import { MenubarProps } from './menubar-types';
import { useStyles } from './menubar-styles';
import { Menu } from './menu/menu';
import { UserBtn } from './user-btn/user-btn';
import { useMenubar } from './menubar-hook';
import { MenubarContext } from './menubar-context';
import { MenuItem } from './menu/menu-types';
import { HotKeys } from './menubar-types';
import { MenubarClassOverrides } from './menubar-class-overrides-types';

export function Menubar({
  logo,
  logoUrl = '/',
  title,
  items: providedItems,
  hotKeys: providedHotKeys = { keyMap: {}, handlers: {} },
  children,
  userBtnActions,
  displayLogoutBtn,
  beforeUserBtn,
  afterUserBtn,
  classOverrides,
}: PropsWithChildren<MenubarProps & MenubarClassOverrides>) {
  const classes = useStyles();

  const { handleLogoClick, items, context } = useMenubar({
    logoUrl,
    items: providedItems,
  });

  const hotKeys: HotKeys = {
    keyMap: { ...providedHotKeys.keyMap },
    handlers: { ...providedHotKeys.handlers },
  };
  gatherHotKeys(hotKeys, items);

  return (
    <MenubarContext.Provider value={context}>
      <>
        <GlobalHotKeys
          keyMap={hotKeys.keyMap}
          handlers={hotKeys.handlers}
          allowChanges={true}
        />
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
                {beforeUserBtn}
                <UserBtn
                  actions={userBtnActions}
                  displayLogoutBtn={displayLogoutBtn}
                  classOverrides={classOverrides}
                />
                {afterUserBtn}
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

function preventEventWrapperFactory(callback: () => void) {
  return function wrappedCallback(e?: KeyboardEvent) {
    e && e.preventDefault();
    callback();
  };
}

function gatherHotKeys(hotKeys: HotKeys, items?: MenuItem[]) {
  items?.forEach((item) => {
    if (item.keyShortcut !== undefined) {
      hotKeys.keyMap[item.keyShortcut] = item.keyShortcut;
      hotKeys.handlers[item.keyShortcut] = preventEventWrapperFactory(
        item.onClick ?? noop
      );
    }

    gatherHotKeys(hotKeys, item.items);
  });
}
