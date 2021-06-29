import makeStyles from '@material-ui/core/styles/makeStyles';

export const useStyles = makeStyles((theme) => ({
  dragger: {
    height: '100%',
    width: 0,
    backgroundColor: '#e0e0e0',
    cursor: 'col-resize',
    position: 'relative',
  },
  switcherWrapper: {
    display: 'flex',
    alignItems: 'center',
    borderRadius: 16,
    position: 'absolute',
    bottom: 40,
    left: '50%',
    transform: 'translateX(-50%)',
    backgroundColor: `${theme.palette.primary.main}`,
    overflow: 'hidden',
    // boxShadow: '0px 3px 5px -1px rgb(0 0 0 / 20%), 0px 6px 10px 0px rgb(0 0 0 / 14%), 0px 1px 18px 0px rgb(0 0 0 / 12%)',
    boxShadow: theme.shadows[10],
    zIndex: 1200,
  },
  switcherHandle: {
    width: 15,
    paddingLeft: 4,
    paddingRight: 4,
    cursor: 'col-resize',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    borderLeft: '1px solid grey',
    borderRight: '1px solid grey',
    height: 32,
    fontWeight: 900,
    fontSize: '1.5em',
    color: 'white',
  },
  switcherButton: {
    minWidth: 'fit-content',
    height: 32,
    fontWeight: 600,
    color: 'white',
    '&:hover': {
      color: `${theme.palette.grey[300]}`,
    },
    '& svg': {
      fontSize: '2em',
    },
  },
}));
