import makeStyles from '@material-ui/core/styles/makeStyles';

export const useStyles = makeStyles((theme) => ({
  item: {
    padding: '2pt 0',
    fontFamily: theme.typography.fontFamily,
  },
  itemSelected: {
    backgroundColor: theme.palette.grey[300],
  },
  itemFocused: {
    backgroundColor: theme.palette.highlight,
  },
}));
