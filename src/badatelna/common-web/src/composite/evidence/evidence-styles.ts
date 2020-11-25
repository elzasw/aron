import makeStyles from '@material-ui/core/styles/makeStyles';

export const useStyles = makeStyles(() => ({
  evidence: {
    flexGrow: 1,
    flexShrink: 1,
  },
  switcherWrapper: {
    display: 'flex',
    alignItems: 'center',
    borderRadius: 12,
    position: 'fixed',
    bottom: 40,
    left: 530,
    border: '1px solid #e8eaed',
    backgroundColor: 'white',
    overflow: 'hidden',
    boxShadow:
      '0px 1px 3px 0px rgba(0,0,0,0.2), 0px 1px 1px 0px rgba(0,0,0,0.14), 0px 2px 1px -1px rgba(0,0,0,0.12)',
    '& svg': {
      cursor: 'pointer',
      '&:hover': {
        backgroundColor: '#f1f3f4',
      },
    },
    transitionProperty: 'left',
    transitionDuration: '1s',
    zIndex: 999,
  },
  switcherDisabledCollapse: {
    left: 56,
    width: 'fit-content',
    height: 'fit-content',
  },
  switcherDisabledExpand: {
    left: 'calc(100% - 40px)',
    width: 'fit-content',
    height: 'fit-content',
  },
  disabledIcon: {
    display: 'none',
  },
}));
