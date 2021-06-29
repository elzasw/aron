import makeStyles from '@material-ui/core/styles/makeStyles';

export const useStyles = makeStyles((theme) => ({
  root: {
    position: 'relative',
    padding: 0,
    verticalAlign: 'top',

    '& .MuiIconButton-label': {
      height: 22,
      width: 22,
    },

    '& svg': {
      fill: '#757575',
    },
  },
  checked: {
    '& svg': {
      fill: theme.palette.primary.dark,
    },
  },
  disabled: {
    backgroundColor: 'inherit',
  },
  highlightedIcon: {
    backgroundColor: theme.palette.editing,
  },
}));
