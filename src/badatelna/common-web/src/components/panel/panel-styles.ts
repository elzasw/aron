import makeStyles from '@material-ui/core/styles/makeStyles';

export const useStyles = makeStyles(() => ({
  root: {
    backgroundColor: 'inherit',
    // paddingBottom: 5,
    boxShadow: 'none',
    /** to disable moving down while expanding */
    margin: '0 0 5px 0!important',
    '&::before': {
      opacity: 0,
    },
  },
  summaryRoot: {
    minHeight: '32px !important',
    height: 32,
    backgroundColor: 'rgba(0, 0, 0, 0.08)',
  },
  summaryRootWithBorder: {
    border: '1px solid #cdcdcd',
  },
  summaryFixed: {
    cursor: 'default !important',
  },
  detailsRoot: {
    padding: 0,
    borderBottomColor: '#cdcdcd',
    borderBottomWidth: 1,
    borderBottomStyle: 'solid',
  },
  detailRootWithSideBorder: {
    borderLeftColor: '#cdcdcd',
    borderLeftWidth: 1,
    borderLeftStyle: 'solid',
    borderRightColor: '#cdcdcd',
    borderRightWidth: 1,
    borderRightStyle: 'solid',
  },
  labelRoot: {
    fontWeight: 700,
  },
}));
