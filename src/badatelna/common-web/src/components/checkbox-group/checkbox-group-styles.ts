import makeStyles from '@material-ui/core/styles/makeStyles';

export const useStyles = makeStyles(() => ({
  labelLabel: {
    fontSize: 14,
    marginLeft: 5,
    '&.Mui-disabled': {
      color: 'inherit',
    },
  },
  labelRoot: {
    height: 25,
  },
  checkboxControl: {
    margin: 0,
  },
  checkboxGroup: {
    display: 'flex',
    flexDirection: 'column',
  },
}));
