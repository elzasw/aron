import makeStyles from '@material-ui/core/styles/makeStyles';

export const useStyles = makeStyles((theme) => ({
  appTitle: {
    fontSize: '1.2rem',
    color: '#fff',
  },
  appTitleFirst: {
    fontWeight: 600,
  },
  appTitleClickable: {
    cursor: 'pointer',
  },
  invertColor: {
    filter: "invert(1)"
  }
}));
