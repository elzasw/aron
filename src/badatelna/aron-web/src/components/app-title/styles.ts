import makeStyles from '@material-ui/core/styles/makeStyles';

import { appHeaderHeight as _appHeaderHeight, compactAppHeaderHeight } from '../../styles';

export interface StyleConfiguration {
    compactAppHeader?: boolean;
}

export const useStyles = (configuration?: StyleConfiguration ) => makeStyles((theme) => {
  const appHeaderHeight = configuration?.compactAppHeader ? compactAppHeaderHeight : _appHeaderHeight;
  return {
  appTitle: {
    fontSize: '1.2rem',
    color: '#fff',
  },
  appTitleFull: {
    flex: 1,
  },
  appTitleInFooter: {
    width: '100%',
    display: 'flex',
    justifyContent: 'center',

    ['@media (min-width:1400px)']: {
      justifyContent: 'flex-start',
    },
  },
  appTitleFirst: {
    fontWeight: 600,
  },
  appTitleClickable: {
    cursor: 'pointer',
  },
  invertColor: {
    filter: 'invert(1)',
  },
  appTitleLogo: {
    maxHeight: appHeaderHeight,
    maxWidth: '8em',

    [theme.breakpoints.up('sm')]: {
      maxWidth: '12em',
    },
  },
  appTitleLogoClickable: {
    cursor: 'pointer',
  },
  appTitleTopImage: {
    maxHeight: appHeaderHeight,
    display: 'none',

    [theme.breakpoints.up('sm')]: {
      maxWidth: '11em',
      display: 'block',
    },
  },
  appTitleDivider: {
    display: 'none',

    [theme.breakpoints.up('sm')]: {
      display: 'block',
    },
  },
}})();
