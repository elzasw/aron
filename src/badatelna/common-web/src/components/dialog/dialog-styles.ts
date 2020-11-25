import makeStyles from '@material-ui/core/styles/makeStyles';

export const useStyles = makeStyles(() => ({
  title: {
    cursor: 'move',
  },
  titleHeader: {
    fontWeight: 700,
    marginTop: 'auto',
    marginBottom: 'auto',
  },
  actions: {
    padding: '10px 24px',
    backgroundColor: 'rgba(0, 0, 0, 0.08)',
  },
  buttonLabel: {
    textTransform: 'capitalize',
  },
}));
