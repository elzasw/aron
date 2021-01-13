import makeStyles from '@material-ui/core/styles/makeStyles';

export const useStyles = makeStyles((theme) => ({
  menu: {
    color: 'rgb(66, 66, 66)',
    backgroundColor: 'white',
    height: 68,
  },
  menuItem: {
    display: 'inline-block',
    padding: '4px 10px',
    overflow: 'visible',
  },
  menuItemText: {
    fontWeight: 400,
    fontSize: 14,
    letterSpacing: 0.2,
  },
  subMenu: {
    position: 'absolute',
    backgroundColor: 'white',
    left: '100%',
    top: 0,
    zIndex: 5,
    boxShadow: theme?.eas?.shadow[1],
  },
  subMenuTopLevel: {
    left: 0,
    top: 29,
  },
  subMenuItem: {
    overflow: 'visible',
    minWidth: 200,
  },
  subMenuItemText: {
    fontWeight: 400,
    fontSize: 14,
    letterSpacing: 0.2,
  },
  subMenuAction: {
    fontSize: '0.875rem !important',
    color: '#7c7c7c',
  },
  subMenuArrow: {
    fontSize: '0.875rem !important',
  },
  shortcut: {},
}));
