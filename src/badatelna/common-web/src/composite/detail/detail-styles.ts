import makeStyles from '@material-ui/core/styles/makeStyles';

export const useStyles = makeStyles((theme) => ({
  outerWrapper: {
    position: 'relative',
    height: '100%',
    display: 'flex',
    minWidth: 700,
    overflow: 'hidden',
    flexDirection: 'column',
  },
  wrapper: {
    borderRadius: theme.eas?.radius,
    backgroundColor: '#f1f3f4',
    padding: 10,
    flex: '1 1 auto',
    overflowY: 'scroll',
    overflowX: 'hidden',
    scrollBehavior: 'smooth',
  },
  toolbarButton: {
    height: 32,
    minWidth: 40,
    cursor: 'pointer',
    padding: '2px 10px',
    backgroundColor: '#f1f3f4',
    '&:hover': {
      backgroundColor: '#e0e2e3',
    },
  },
  toolbarButtonMenu: {
    width: '100%',
    textTransform: 'none',
  },
  toolbarButtonWarning: {
    backgroundColor: `${theme.palette.error.dark}40`,
    '&:hover': {
      backgroundColor: `${theme.palette.error.dark}60`,
    },
  },
  toolbarWrapper: {
    padding: 10,
    paddingLeft: 0,
    display: 'flex',
    flex: '0 0 auto',
    minHeight: 52,
  },
  toolbarIndentLeft: {
    marginLeft: 10,
  },
  toolbarMainButton: {
    height: 32,
    minWidth: 40,
    cursor: 'pointer',
    padding: '2px 10px',
    boxShadow: '0 0 0',
  },
  loaderWrapper: {
    width: '100%',
    height: '100%',
    position: 'absolute',
    backgroundColor: '#e1e2e340',
    zIndex: 1000,
  },
  loader: {
    position: 'absolute',
    left: 'calc(50% - 20px)',
    top: 'calc(50% - 20px)',
  },
}));
