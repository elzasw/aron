import makeStyles from '@material-ui/core/styles/makeStyles';

export const useStyles = makeStyles((theme) => ({
  menuBarWrapper: {
    // borderBottom: '1px solid rgba(0, 0, 0, 0.08)',
    display: 'flex',
    alignItems: 'flex-end',
    backgroundColor: theme.palette.primary.main,
    top: 0,
    width: '100%',
    boxShadow: theme?.eas?.shadow[0],
  },
  iconlink: {
    width: 62,
    height: 62,
    zIndex: 99,
    cursor: 'pointer',
    color: 'white',
    flex: '0 0 auto',
    padding: 8,
  },
  menuBarInnerWrapper: {
    display: 'flex',
    flexDirection: 'column',
    width: '100%',
    height: 62,
  },
  menuContainer: {
    display: 'flex',
    width: '100%',
    justifyContent: 'space-between',
    alignItems: 'center',
    flex: '0 0 33px',
  },
}));
