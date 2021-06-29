import makeStyles from '@material-ui/core/styles/makeStyles';

export const useStyles = makeStyles((theme) => ({
  tableActions: {
    backgroundColor: 'rgba(0, 0, 0, 0.08)',
    height: 30,
    width: '100%',
    boxSizing: 'border-box',
    verticalAlign: 'top',
    display: 'flex',
    padding: '0 12px',
    '& svg': {
      cursor: 'pointer',
      width: 20,
      height: 20,
      padding: '2pt',
    },
  },
  buttonGroup: {
    display: 'flex',
    marginLeft: -5, // offset first icon left padding
  },
  iconButton: {
    padding: '0 5px',
    margin: '0',
  },
  componentWrapper: {
    marginTop: -2,
    minWidth: '100%',
    width: '100%',
    display: 'inline-block',
    fontFamily: theme.typography.fontFamily,
  },
  grid: {
    overflow: 'auto',
    paddingBottom: 5,
    backgroundColor: 'rgb(0, 0, 0, 0.08)',
  },

  header: {
    height: 35,
    boxSizing: 'border-box',
    width: 'fit-content',
    overflow: 'visible',
    display: 'flex',
    flexDirection: 'row',
    minWidth: '100%',
    // alignItems: 'center',
    color: theme.palette.getContrastText('#252525'),
    backgroundColor: '#252525',
    fontWeight: 500,
    whiteSpace: 'nowrap',
    padding: '0 18px!important',
  },
  tableRowActions: {
    flexShrink: 0,
    width: 50,
    height: 20,
    verticalAlign: 'top',
    display: 'inline-flex',
    padding: 0,
    overflow: 'hidden',
    '& svg': {
      cursor: 'pointer',
      width: 20,
      height: 20,
      padding: '2pt',
    },
  },
  tableRowHeader: {
    flexShrink: 0,
    display: 'flex',
    lineHeight: '35px',
  },
  tableRowHeaderLabel: {
    display: 'inline-block',
    flex: '1 1 auto',
    overflow: 'hidden',
    maxWidth: '100%',
    whiteSpace: 'nowrap',
    textOverflow: 'ellipsis',
    margin: 'auto',
  },
  rowDraggable: {
    alignSelf: 'center',
  },
  draggable: {
    zIndex: 999,
  },
  draggableIcon: {
    width: 10,
    cursor: 'col-resize',
  },
  headerCellDraggable: {
    marginLeft: 5,
  },
  tableWrapper: {
    minWidth: '100%',
    display: 'inline-block',
  },
  dataWrapper: {
    minHeight: 60,
    minWidth: '100%',
    width: 'fit-content',
    // overflowY: 'scroll',
  },
  noMinHeigth: {
    minWidth: 'inherit !important',
    minHeight: 'auto !important',
    marginTop: '0 !important',
    paddingBottom: '0 !important',
  },
  row: {
    boxSizing: 'border-box',
    borderBottom: '1px solid #cdcdcd',
    minWidth: '100%',
    minHeight: 30,
    '&:hover': {
      backgroundColor: theme.palette.highlight,
    },
    padding: '2px 16px',
    display: 'flex',
    alignItems: 'center',
    flexShrink: 0,
    cursor: 'pointer',
  },
  defaultCursor: {
    cursor: 'default',
  },
  radioButton: {
    padding: 0,
    // verticalAlign: 'top',
  },
  tableCell: {
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
    overflow: 'hidden',
    display: 'inline-block',
    verticalAlign: 'top',
    width: '100%',

    '& input': {
      textOverflow: 'ellipsis',
      whiteSpace: 'nowrap',
    },
  },
  tableCellFlex: {
    display: 'flex',
  },
  cellWrapper: {
    flexShrink: 0,
    display: 'flex',
    paddingRight: 10,
    boxSizing: 'border-box',
  },
  dialogTitleHeader: {
    fontWeight: 700,
    marginTop: 'auto',
    marginBottom: 'auto',
  },
  dialogActions: {
    padding: '10px 24px',
    backgroundColor: 'rgba(0, 0, 0, 0.08)',
  },
  dialogButtonLabel: {
    textTransform: 'capitalize',
  },
}));
