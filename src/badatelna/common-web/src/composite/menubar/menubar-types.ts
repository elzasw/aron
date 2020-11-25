import { ReactNode } from 'react';
import { MenuItem } from './menu/menu-types';
import { UserBtnAction } from './user-btn/user-btn-types';

export interface MenubarClassOverrides {
  classOverrides?: {
    // menubar.tsx
    menuBarWrapper?: string;
    iconlink?: string;
    menuBarInnerWrapper?: string;
    menuContainer?: string;
    // menu.tsx
    menu?: string;
    // menu-item.tsx
    menuItem?: string;
    menuItemText?: string;
    // user-btn.tsx
    userButton?: string;
    userMenu?: string;
    // sub-menu.tsx
    subMenu?: string;
    // sub-menu-item.tsx
    subMenuArrow?: string;
    subMenuItem?: string;
    subMenuItemText?: string;
    subMenuAction?: string;
  };
}

export interface MenubarProps {
  logo: ReactNode;
  logoUrl?: string;
  logoutUrl?: string;
  logoutSuccessUrl?: string;
  title?: ReactNode;
  items: MenuItem[];
  userBtnActions?: UserBtnAction[];
}
