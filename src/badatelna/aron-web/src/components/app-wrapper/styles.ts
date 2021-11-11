import makeStyles from '@material-ui/core/styles/makeStyles';

export const useStyles = makeStyles((theme) => ({
  appWrapper: {
    width: '100vw',
    minWidth: '100vw',
    maxWidth: '100vw',
    height: '100%',
    // minHeight: '100vh',
    overflowY: 'hidden',
    background: '#fff',
    overflowX: 'hidden',
    display: 'flex',
    flexDirection: 'column',
  },

  iOSWrapper: {
    paddingBottom: '15vh',
  },
}));
