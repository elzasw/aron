import makeStyles from '@material-ui/core/styles/makeStyles';

export const useStyles = makeStyles((theme) => ({
  paper: {
    width: '600px',
    height: '300px',
  },
  popper: {
    width: '500px',
    zIndex: 1500,
  },
  icon: {
    cursor: 'pointer',
    color: 'rgba(0, 0, 0, 0.54)',
  },
  iconOpened: {
    transform: 'rotate(180deg)',
  },
  wrapper: {
    width: '100%',
    display: 'flex',
    flexDirection: 'column',
    backgroundColor: theme.palette.editing,
  },
  wrapperDisabled: {
    backgroundColor: 'inherit',
  },
  chips: {
    marginTop: 3,
    display: 'flex',
    alignItems: 'center',

    flexWrap: 'wrap',
  },
  chipsWrapper: {
    flex: 1,
    display: 'flex',
  },
  chip: {
    height: 20,
  },
  input: {
    flexGrow: 1,
    flexShrink: 1,
    minWidth: '30%',
  },
  clearButton: {
    marginRight: -6,
  },
}));
