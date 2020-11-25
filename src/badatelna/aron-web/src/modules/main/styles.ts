import makeStyles from '@material-ui/core/styles/makeStyles';
import {
  appHeaderHeight,
  colorBlueLight,
  colorBlueVeryLight,
} from '../../styles';

export const useStyles = makeStyles((theme) => {
  return {
    main: {
      position: 'relative',
      background: colorBlueVeryLight,
    },
    mainInner: {
      minHeight: `calc(100vh - ${appHeaderHeight})`,
      display: "flex",
      flexDirection: "column",
      justifyContent: "space-between"
    },
    mainBackgroundIcon: {
      transform: 'rotate(15deg)',
      position: 'absolute',
      top: '1rem',
      right: '-10vmin',
      opacity: 0.04,
      width: '35vmax !important',
      height: '35vmax !important',
    },
    mainBody: {
      width: '100%',

      '& h1': {
        textAlign: 'center',
        color: theme.palette.primary.main,
      },
    },
    mainBodyInner: {
      width: '100%',
      maxWidth: 'calc(800px - 1rem)',
      padding: '0 0.5rem',
    },
    mainFavourite: {
      width: '100%',

      '& div': {
        width: '100%',
        cursor: 'pointer',

        [theme.breakpoints.up('md')]: {
          width: '50%',
        },

        '&:hover': {
          color: theme.palette.primary.main,
        },
      },
    },
    mainFavouriteIcon: {
      color: theme.palette.primary.main,
    },
    mainFooter: {
      marginTop: 'auto',
      width: '100%',
      background: theme.palette.primary.dark,
      color: '#fff',
    },
    mainFooterLeft: {
      minWidth: 'calc((100vw - 800px - 8rem) / 2)',
    },
    mainFooterSection: {
      maxWidth: 600,
    },
    mainFooterTitle: {
      fontSize: '0.8rem',
      textTransform: 'uppercase',
      marginLeft: 0
    },
    mainFooterText: {
      color: colorBlueLight,
      fontSize: '0.75rem',
      lineHeight: '1.4rem',
    },
  };
});
