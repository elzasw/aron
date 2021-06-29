import makeStyles from '@material-ui/core/styles/makeStyles';

export const useStyles = makeStyles((theme) => ({
  container: {
    flexDirection: 'column',
    display: 'flex',
    width: '100%',
    height: '500px',
  },
  fixed: {
    flex: '0 0 auto',
  },
  autosize: {
    flex: '1 1 auto',
  },
}));
