import { useCallback, useState } from 'react';

export function useReportActionMenu() {
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
