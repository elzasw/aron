import makeStyles from '@material-ui/core/styles/makeStyles';

export const useStyles = makeStyles(() => ({
  root: {
    paddingTop: '2pt',
    paddingBottom: '2pt',
    paddingLeft: '2pt',
  },
  list: {
    paddingTop: 0,
    paddingBottom: 0,
  },
  item: {
    paddingTop: '2pt',
    paddingBottom: '2pt',
  },
  input: {
    '&::before': {
      border: 0,
    },
    height: 22,
    width: '100%',
    verticalAlign: 'top',

    // classes cannot be send to wrapping Input, therefore we need to use direct override
    '&.Mui-disabled': {
      backgroundColor: 'inherit',
      color: 'inherit',
    },

    '& .MuiSelect-icon.Mui-disabled': {
      color: 'rgba(0, 0, 0, 0.54)',
    },
  },
  adornment: {
    marginRight: 15,
  },
}));
