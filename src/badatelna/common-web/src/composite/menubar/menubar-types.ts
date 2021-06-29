import { ReactNode } from 'react';
import { MenuItem } from './menu/menu-types';
import { UserBtnAction } from './user-btn/user-btn-types';

export interface HotKeys {
  keyMap: Record<string, string>;
  handlers: Record<string, () => void>;
}

export interface MenubarProps {
  logo: ReactNode;
  logoUrl?: string;
  title?: ReactNode;
  items: MenuItem[];
  hotKeys?: HotKeys;
  userBtnActions?: UserBtnAction[];
  displayLogoutBtn?: boolean;
  beforeUserBtn?: ReactNode;
  afterUserBtn?: ReactNode;
}
