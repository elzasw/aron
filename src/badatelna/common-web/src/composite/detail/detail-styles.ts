import makeStyles from '@material-ui/core/styles/makeStyles';

export const useStyles = makeStyles(() => ({
  outerWrapper: {
    position: 'relative',
    height: '100%',
    display: 'flex',
    flexDirection: 'column',
  },
  wrapper: {
    backgroundColor: '#f1f3f4',
    padding: 10,
    flex: '1 1 auto',
    overflowY: 'scroll',
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
