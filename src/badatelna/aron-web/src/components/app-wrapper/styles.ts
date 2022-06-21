import makeStyles from '@material-ui/core/styles/makeStyles';

export const useStyles = makeStyles((theme) => {
  const md = theme.breakpoints.up('md');
  return {
    appWrapper: {
      // minHeight: '100vh',
      overflowY: 'auto',
      background: '#fff',
      overflowX: 'hidden',
      display: 'flex',
      flexDirection: 'column',
   
      [md]: {
        overflowY: 'hidden',
        height: '100%',
        width: '100vw',
        minWidth: '100vw',
        maxWidth: '100vw',
      },
    },
   
    iOSWrapper: {
      paddingBottom: '15vh',
    },
  }
});
