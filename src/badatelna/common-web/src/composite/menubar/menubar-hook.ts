import { useContext, useMemo, useRef } from 'react';
import { useEventCallback } from 'utils/event-callback-hook';
import { NavigationContext } from 'composite/navigation/navigation-context';
import { MenuItem } from './menu/menu-types';
import { MenubarContext, MofifyItems } from './menubar-context';
import { useForceRender } from 'utils/force-render';

export function useMenubar(options: { logoUrl: string; items: MenuItem[] }) {
  const { navigate } = useContext(NavigationContext);

  const { forceRender } = useForceRender();
  const itemsRef = useRef<MenuItem[]>(options.items);

  const handleLogoClick = useEventCallback(() => {
    navigate(options.logoUrl);
  });

  const modifyItems = useEventCallback((callback: MofifyItems) => {
    const newItems = callback(itemsRef.current);

    itemsRef.current = newItems;
    forceRender();
  });

  const context: MenubarContext = useMemo(
    () => ({
      modifyItems,
    }),
    [modifyItems]
  );

  return {
    handleLogoClick,
    context,
    items: itemsRef.current,
  };
}
