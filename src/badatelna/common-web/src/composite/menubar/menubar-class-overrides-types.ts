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
    activeMenuItem?: string;
    // user-btn.tsx
    userButton?: string;
    userMenu?: string;
    // sub-menu.tsx
    subMenu?: string;
    subMenuTopLevel?: string;
    // sub-menu-item.tsx
    subMenuArrow?: string;
    subMenuItem?: string;
    subMenuItemText?: string;
    subMenuItemIcon?: string;
    subMenuAction?: string;
  };
}
