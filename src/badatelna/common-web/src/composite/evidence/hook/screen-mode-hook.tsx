import React, { useState, useContext, useEffect, useMemo } from 'react';
import { EvidenceScreenMode } from '../evidence-types';
import { MenubarContext } from 'composite/menubar/menubar-context';
import { MenuItem } from 'composite/menubar/menu/menu-types';
import ArrowUpwardIcon from '@material-ui/icons/ArrowUpward';
import ArrowForwardIcon from '@material-ui/icons/ArrowForward';
import ForwardOutlinedIcon from '@material-ui/icons/ForwardOutlined';
import { useIntl } from 'react-intl';
import { useStyles } from './screen-mode-styles';

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

  const classes = useStyles();

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
            keyShortcut: 'shift+right',
            keyShortcutLabel: (
              <>
                <ForwardOutlinedIcon
                  fontSize="small"
                  className={classes.shift}
                />{' '}
                <ArrowForwardIcon fontSize="small" />
              </>
            ),
            onClick: () => {
              console.log('sss');
              setScreenMode(EvidenceScreenMode.TABLE);
            },
          },
          {
            label: intl.formatMessage({
              id: 'EAS_MENU_VIEW_SPLIT',
              defaultMessage: 'Dělené',
            }),
            keyShortcut: 'shift+up',
            keyShortcutLabel: (
              <>
                <ForwardOutlinedIcon
                  fontSize="small"
                  className={classes.shift}
                />{' '}
                <ArrowUpwardIcon fontSize="small" />
              </>
            ),
            onClick: () => {
              setScreenMode(EvidenceScreenMode.SPLIT);
            },
          },
          {
            label: intl.formatMessage({
              id: 'EAS_MENU_VIEW_DETAIL',
              defaultMessage: 'Detail',
            }),
            keyShortcut: 'shift+left',
            keyShortcutLabel: (
              <>
                <ForwardOutlinedIcon
                  fontSize="small"
                  className={classes.shift}
                />{' '}
                <ArrowForwardIcon fontSize="small" className={classes.back} />
              </>
            ),
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
