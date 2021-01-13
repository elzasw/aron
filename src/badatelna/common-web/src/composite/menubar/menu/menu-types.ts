import { ReactNode } from 'react';

export interface MenuItem {
  label: ReactNode;
  separator?: boolean;
  icon?: ReactNode;
  items?: MenuItem[];
  keyShortcut?: string;
  keyShortcutLabel?: ReactNode;
  onClick?: () => void;
}

export interface MenuProps {
  items: MenuItem[];
}

export interface MenuItemProps {
  item: MenuItem;
  opened: boolean;
  index: number;
  onClick: (index: number) => void;
  onHover: (index: number) => void;
  onClickAway: (index: number) => void;
}

export interface SubmenuProps {
  topLevel?: boolean;
  items: MenuItem[];
  opened: boolean;
}

export interface SubmenuItemProps {
  item: MenuItem;
  opened: boolean;
  index: number;
  onHover: (index: number) => void;
}
