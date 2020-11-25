import makeStyles from '@material-ui/core/styles/makeStyles';

export const useStyles = makeStyles((theme) => ({
  item: {
    paddingTop: '2pt',
    paddingBottom: '2pt',
    fontFamily: theme.typography.fontFamily,
  },
  itemText: {
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
    overflow: 'hidden',
  },
  itemSelected: {
    backgroundColor: theme.palette.grey[300],
  },
  itemFocused: {
    backgroundColor: theme.palette.highlight,
  },
}));
