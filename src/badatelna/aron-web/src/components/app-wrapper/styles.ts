import makeStyles from '@material-ui/core/styles/makeStyles';

export const useStyles = makeStyles((theme) => ({
  appWrapper: {
    width: '100vw',
    minWidth: '100vw',
    maxWidth: '100vw',
    height: '100vh',
    minHeight: '100vh',
    overflowY: 'auto',
    background: '#fff',
    overflowX: 'hidden',

    '& img': {
      'user-drag': 'none',
      'user-select': 'none',
      '-moz-user-select': 'none',
      '-webkit-user-drag': 'none',
      '-webkit-user-select': 'none',
      '-ms-user-select': 'none',
    },
  },
}));
