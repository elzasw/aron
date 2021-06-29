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
  addorment: {
    '& button': {
      padding: '3px',
    },
    '& svg': {
      height: 20,
      width: 20,
    },
  },
  dissabledAddorment: {
    display: 'none',
  },
}));
