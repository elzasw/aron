import makeStyles from '@material-ui/core/styles/makeStyles';

export const useStyles = makeStyles((theme) => ({
  radioRoot: {
    padding: 2,
  },
  labelLabel: {
    fontSize: 14,
    marginLeft: 5,
  },
  labelRoot: {
    height: 25,

    /** to make radio buttons smaller */
    '& svg': {
      height: 18,
      width: 18,
    },
  },
  radioControl: {
    margin: 0,
  },
  disabled: {
    backgroundColor: 'inherit',
  },
  highlightedIcon: {
    backgroundColor: `${theme.palette.editing} !important`,
  },
}));
