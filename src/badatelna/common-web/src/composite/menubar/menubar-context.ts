import { createContext } from 'react';
import { MenuItem } from './menu/menu-types';

export type MofifyItems = (items: MenuItem[]) => MenuItem[];

export interface MenubarContext {
  modifyItems: (callback: MofifyItems) => void;
}

export const MenubarContext = createContext<MenubarContext>({
  modifyItems: () => () => {
    console.error('No Menubar context was created.');
  },
});
