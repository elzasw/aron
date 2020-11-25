import { useState, useContext, useEffect, useMemo } from 'react';
import { EvidenceScreenMode } from '../evidence-types';
import { MenubarContext } from 'composite/menubar/menubar-context';
import { MenuItem } from 'composite/menubar/menu/menu-types';
import { useIntl } from 'react-intl';

const detailStyles: Record<EvidenceScreenMode, React.CSSProperties> = {
  [EvidenceScreenMode.TABLE]: {
    overflow: 'hidden',
    width: '0px',
    padding: 0,
    transition: 'width 1s',
  },
  [EvidenceScreenMode.SPLIT]: {
    width: 'calc(100% - 600px)',
    transition: 'width 1s',
  },
  [EvidenceScreenMode.DETAIL]: {
    width: '100%',
    transition: 'width 1s',
  },
};

const tableStyles: Record<EvidenceScreenMode, React.CSSProperties> = {
  [EvidenceScreenMode.TABLE]: {
    width: '100%',
    transition: 'width 1s',
  },
  [EvidenceScreenMode.SPLIT]: {
    width: '600px',
    transition: 'width 1s',
  },
  [EvidenceScreenMode.DETAIL]: {
    width: '0px',
    transition: 'width 1s',
  },
};

/**
 * Screen mode switcher functionality.
 */
export function useScreenMode() {
  const [screenMode, setScreenMode] = useState<EvidenceScreenMode>(
    EvidenceScreenMode.SPLIT
  );

  const { modifyItems } = useContext(MenubarContext);

  const intl = useIntl();

  const tableStyle = tableStyles[screenMode];
  const detailStyle = detailStyles[screenMode];

  const menuItems: MenuItem[] = useMemo(
    () => [
      {
        label: intl.formatMessage({
          id: 'EAS_MENU_VIEW',
          defaultMessage: 'Zobrazení',
        }),
        items: [
          {
            label: intl.formatMessage({
              id: 'EAS_MENU_VIEW_TABLE',
              defaultMessage: 'Tabulka',
            }),
            onClick: () => {
              setScreenMode(EvidenceScreenMode.TABLE);
            },
          },
          {
            label: intl.formatMessage({
              id: 'EAS_MENU_VIEW_SPLIT',
              defaultMessage: 'Dělené',
            }),
            onClick: () => {
              setScreenMode(EvidenceScreenMode.SPLIT);
            },
          },
          {
            label: intl.formatMessage({
              id: 'EAS_MENU_VIEW_DETAIL',
              defaultMessage: 'Detail',
            }),
            onClick: () => {
              setScreenMode(EvidenceScreenMode.DETAIL);
            },
          },
        ],
      },
    ],
    [intl]
  );

  // adds items to menu
  useEffect(() => {
    modifyItems((items) => [...items, ...menuItems]);

    return () => {
      modifyItems((items) => items.filter((item) => !menuItems.includes(item)));
    };
  }, [menuItems, modifyItems]);

  return {
    screenMode,
    tableStyle,
    detailStyle,
    changeScreenMode: setScreenMode,
  };
}
