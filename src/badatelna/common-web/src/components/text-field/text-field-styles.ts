import makeStyles from '@material-ui/core/styles/makeStyles';

export const useStyles = makeStyles(() => ({
  root: {
    height: 22,
    verticalAlign: 'top',
    '&:before': {
      border: 0,
    },
  },

  input: {
    padding: '2pt',
  },
}));
