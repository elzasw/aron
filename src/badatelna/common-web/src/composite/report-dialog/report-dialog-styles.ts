import makeStyles from '@material-ui/core/styles/makeStyles';

export const useStyles = makeStyles(() => ({
  dialogWrapper: {
    width: 800,
  },
  radioRoot: {
    padding: 2,
  },
  labelLabel: {
    fontSize: 14,
    height: 20,
  },
  labelRoot: {
    height: 25,
    /** to make radio buttons smaller */
    '& svg': {
      height: 15,
      width: 15,
    },
  },
  radioControl: {
    margin: 0,
  },
  buttonLabel: {
    textTransform: 'uppercase',
  },
}));
