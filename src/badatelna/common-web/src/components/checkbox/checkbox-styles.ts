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
  highlightedRoot: {
    '&:before': {
      background: theme.palette.editing,
      content: "''",
      display: 'block',
      position: 'absolute',
      top: 5,
      left: 5,
      right: 5,
      bottom: 5,
      zIndex: -2,
    },
  },
  checked: {
    '& svg': {
      fill: theme.palette.primary.dark,
    },
  },
  disabled: {
    '&:before': {
      backgroundColor: 'inherit',
    },
  },
}));
