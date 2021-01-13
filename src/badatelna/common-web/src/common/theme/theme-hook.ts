import { useMemo } from 'react';
// import './theme-fix';
import createMuiTheme from '@material-ui/core/styles/createMuiTheme';
import { ThemeProviderProps } from './theme-types';

export function useTheme({
  primary,
  secondary,
  editing,
  highlight,
}: ThemeProviderProps) {
  const theme = useMemo(
    () =>
      createMuiTheme({
        typography: {
          fontFamily: ['"Public Sans"'].join(','),
          fontSize: 12,
          h6: {
            fontSize: '0.9rem',
          },
        },
        palette: {
          primary,
          secondary,
          editing,
          highlight,
        },
        overrides: {
          MuiCssBaseline: {
            '@global': {
              html: {
                height: '100%',
              },
              body: {
                height: '100%',
                overscrollBehavior: 'none',
                overflow: 'hidden',
              },
              '#app': {
                height: '100%',
                display: 'flex',
                flexDirection: 'column',
              },
            },
          },
          MuiOutlinedInput: {
            root: {
              borderRadius: 0,
            },
          },
          MuiInput: {
            root: {
              backgroundColor: editing,
              transition: 'width 0.1s ease-in-out',

              '&.Mui-disabled': {
                backgroundColor: 'inherit',
                color: 'inherit',
              },
            },
          },
          MuiButton: {
            root: {
              borderRadius: 0,
            },
          },
          MuiCard: {
            root: {
              borderRadius: 0,
            },
          },
          MuiCardHeader: {
            root: {
              paddingBottom: 5,
            },
          },
          MuiList: {
            root: {
              '&:focus': {
                outline: 0,
              },
            },
          },
          MuiMenu: {
            paper: {
              borderRadius: 0,
            },
          },
          MuiTooltip: {
            tooltip: {
              borderRadius: 0,
            },
          },
          MuiIconButton: {
            root: {
              borderRadius: 0,
            },
          },
          MuiDialog: {
            paper: {
              borderRadius: 0,
              // width: 700,
              backgroundColor: '#f1f3f4',
            },
          },
          MuiDialogTitle: {
            root: {
              height: 32,
              minHeight: '32px!important',
              backgroundColor: 'rgba(0, 0, 0, 0.08)',
              padding: '0 24px!important',
              display: 'flex',
            },
          },
          MuiDialogContent: {
            dividers: {
              paddingTop: 10,
            },
          },
        },
        eas: {
          shadow: [
            '0px 0px 10px #e0e2e3', // default in menuBarWrapper [menubar-styles.ts]
            '0px 5px 5px -3px rgba(0,0,0,0.2), 0px 8px 10px 1px rgba(0,0,0,0.14), 0px 3px 14px 2px rgba(0,0,0,0.12)', // defualt in subMenu [menu-styles.ts]
          ],
        },
      }),
    [primary, editing, highlight]
  );

  return theme;
}
