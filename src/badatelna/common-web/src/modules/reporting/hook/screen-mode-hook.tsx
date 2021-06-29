import React, { useContext, useEffect, useMemo, useRef } from 'react';
import { useIntl } from 'react-intl';
import ArrowUpwardIcon from '@material-ui/icons/ArrowUpward';
import ArrowForwardIcon from '@material-ui/icons/ArrowForward';
import ForwardOutlinedIcon from '@material-ui/icons/ForwardOutlined';
import { SplitScreenHandle } from 'components/split-screen/split-screen-types';
import { MenubarContext } from 'composite/menubar/menubar-context';
import { MenuItem } from 'composite/menubar/menu/menu-types';
import { useStyles } from './screen-mode-styles';

/**
 * Screen mode switcher functionality.
 */
export function useScreenMode() {
  const splitScreenRef = useRef<SplitScreenHandle>(null);

  const { modifyItems } = useContext(MenubarContext);

  const classes = useStyles();

  const intl = useIntl();

  const menuItems: MenuItem[] = useMemo(
    () => [
      {
        label: intl.formatMessage({
          id: 'EAS_REPORTING_MENU_VIEW',
          defaultMessage: 'Zobrazení',
        }),
        items: [
          {
            label: intl.formatMessage({
              id: 'EAS_REPORTING_MENU_VIEW_DEFINITIONS',
              defaultMessage: 'Výběr',
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
              splitScreenRef.current?.handleFullSizeLeft();
            },
          },
          {
            label: intl.formatMessage({
              id: 'EAS_REPORTING_MENU_VIEW_SPLIT',
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
              splitScreenRef.current?.handleMoveToMiddle();
            },
          },
          {
            label: intl.formatMessage({
              id: 'EAS_REPORTING_MENU_VIEW_DATA',
              defaultMessage: 'Report',
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
              splitScreenRef.current?.handleFullSizeRight();
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

  return { splitScreenRef };
}
