import { useCallback, useState } from 'react';

export function useExportActionMenu() {
  const [opened, setOpened] = useState<boolean>(false);

  const openMenu = useCallback(() => {
    setOpened(true);
  }, []);

  const closeMenu = useCallback(() => {
    setOpened(false);
  }, []);

  return {
    opened,
    openMenu,
    closeMenu,
    actions: [],
  };
}
