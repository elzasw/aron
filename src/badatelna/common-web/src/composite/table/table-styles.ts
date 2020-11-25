import makeStyles from '@material-ui/core/styles/makeStyles';

export const useStyles = makeStyles((theme) => ({
  wrapper: {
    overflow: 'hidden',
    position: 'relative',
    height: '100%',
  },
  tableGroupWrapper: {
    height: '100%',
    width: '100%',
    position: 'relative',
  },
  searchWrapper: {
    flex: '0 0 auto',
    padding: '10px 0',
    borderRadius: '10px 10px 0 0',
    display: 'flex',
    justifyContent: 'center',
    boxSizing: 'border-box',
    // height: 68,
  },
  searchTextField: {
    width: '100%',
    marginLeft: 10,
    marginRight: 10,
    marginBottom: 0,
    marginTop: 0,
    backgroundColor: '#fff',

    '& > div': {
      fontSize: 14,
      height: 32,
      '& input[type="search"]::-webkit-search-cancel-button': {
        display: 'none',
      },
    },
  },
  searchTextFieldInput: {
    '&:hover': {
      borderColor: 'red',
    },
  },
  clearIcon: {
    cursor: 'pointer',
    fontSize: 15,
    marginRight: 5,
    '&:hover': {
      color: theme.palette.primary.dark,
    },
  },
  searchIcon: {
    cursor: 'pointer',
    marginRight: 5,
    padding: 10,
    color: theme.palette.primary.dark,
  },
  toolbarWrapper: {
    flex: '0 0 auto',
    display: 'flex',
    alignItems: 'center',
    paddingTop: 5,
    paddingRight: 0,
    paddingLeft: 10,
    paddingBottom: 5,
    justifyContent: 'space-between',
    // height: 45,
    boxSizing: 'border-box',
  },
  toolbarSelected: {
    backgroundColor: theme.palette.primary.main,
  },
  toolbarText: {
    fontWeight: 'bold',
    maxWidth: 250,
    width: '100%',
    whiteSpace: 'nowrap',
  },
  toolbarTextSub: {
    fontSize: '1.1em !important',
    background: `${theme.palette.primary.light}40`,
    width: '20px',
    lineHeight: 'inherit',
    textAlign: 'center',
    marginTop: -2,
    marginBottom: -2,
    marginRight: -3,
  },
  toolbarSelectedLabel: {
    color: theme.palette.primary.contrastText,
    width: '100%',
    display: 'flex',

    alignItems: 'center',

    fontWeight: 'bold',
    justifyContent: 'space-between',
    borderRadius: 2,
    whiteSpace: 'nowrap',

    '& svg': {
      fontSize: '1rem',
      cursor: 'pointer',
    },
  },
  toolbarButtonList: {
    marginTop: 0,
    marginBottom: 0,
    marginRight: 10,
    listStyle: 'none',
    display: 'flex',
  },
  toolbarButtonWrapper: {
    marginLeft: 5,
  },
  toolbarButton: {
    height: 25,
    maxWidth: 60,
    minWidth: 40,
    fontSize: 14,
    cursor: 'pointer',
    padding: '2px 5px',
    backgroundColor: '#f1f3f4',
    '&:hover': {
      backgroundColor: '#e0e2e3',
    },
  },
  toolbarButtonPrimary: {
    backgroundColor: theme.palette.primary.main,
    color: theme.palette.primary.contrastText,
    '&:hover': {
      backgroundColor: theme.palette.primary.dark,
    },
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
  columnDialogList: {
    margin: 0,
    display: 'flex',
    flexDirection: 'column',
    listStyle: 'none',
    paddingLeft: 0,
  },
  columnDialogItem: {
    minWidth: 350,
    display: 'flex',
    flexDirection: 'row',
    alignItems: 'center',
    '&:hover': {
      backgroundColor: 'rgb(241, 243, 244)',
    },
    zIndex: 10999,
  },
  columnDialogItemLabel: {
    flex: '1 1 auto',
    height: 30,
  },
  columnDialogItemDraggable: {
    flex: '0 0 auto',
  },
  filterDialogColumnsWrapper: {
    display: 'flex',
    maxWidth: 900,
  },
  filterDialogItem: {
    width: 290,
    padding: '0 5px',
    position: 'relative',
    boxSizing: 'border-box',
  },
  filterDialogItemSubWrapper: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingBottom: 5,
  },
  filterDialogItemValueWrapper: {
    width: 200,
  },
  dialogCheckBoxLabel: {
    fontSize: 14,
  },
  tableWrapper: {
    overflowX: 'scroll',
    minWidth: '100%',
    maxWidth: '100%',

    '&:focus': {
      outline: 0,
    },
  },
  header: {
    height: 50,
    boxSizing: 'border-box',
    width: 'fit-content',
    overflow: 'visible',
    display: 'flex',
    flexDirection: 'row',
    minWidth: '100%',
    // alignItems: 'center',
    color: 'black',
    backgroundColor: `${theme.palette.primary.light}40`,
    fontWeight: 500,
    whiteSpace: 'nowrap',
    padding: '0 12px',
  },
  tableRowActions: {
    flexShrink: 0,
    width: 50,
    verticalAlign: 'top',
    display: 'flex',
    alignItems: 'center',
    padding: 0,
  },
  tableRowHeader: {
    flexShrink: 0,
    display: 'flex',
    lineHeight: '50px',
    alignItems: 'center',
  },
  tableRowHeaderLabel: {
    display: 'inline-block',
    flex: '1 1 auto',
    overflow: 'hidden',
    maxWidth: '100%',
    whiteSpace: 'nowrap',
    textOverflow: 'ellipsis',
    margin: 'auto',
    textTransform: 'uppercase',
  },
  draggable: {},
  draggableIcon: {
    width: 10,
    cursor: 'col-resize',
  },
  headerCellDraggable: {
    marginLeft: 5,
    marginRight: 5,
  },
  dataWrapper: {
    minHeight: 100,
    minWidth: '100%',
    width: 'fit-content',
    // overflowY: 'scroll',
  },
  scrollContainer: {
    overflowY: 'scroll !important' as 'scroll',
    '& > div': {
      direction: 'ltr',
    },
  },
  row: {
    height: 30,
    boxSizing: 'border-box',
    borderBottom: '1px solid rgb(232, 234, 237)',
    minWidth: '100%',
    '&:hover': {
      backgroundColor: theme.palette.highlight,
    },
    padding: '2px 12px',
    display: 'flex',
    alignItems: 'center',
    flexShrink: 0,
  },
  rowActive: {
    backgroundColor: '#e0e0e0 !important',
    boxShadow: `inset 5px 0px 0 0 ${theme.palette.primary.main}`,
  },
  tableCell: {
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
    overflow: 'hidden',
    display: 'inline-block',
    verticalAlign: 'top',

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
  iconRotate: {
    transform: 'scaleY(-1)',
  },
  sortable: {
    cursor: 'pointer',
  },
  sortSup: {
    fontWeight: 400,
    alignSelf: 'flex-start',
    marginTop: 5,
    marginLeft: -5,
    fontSize: 12,
  },
  columnAlignLeft: {
    justifyContent: 'flex-start !important',
  },
  columnAlignRight: {
    justifyContent: 'flex-end !important',
    paddingRight: 15,
  },
  columnAlignCenter: {
    justifyContent: 'center !important',
  },
  progress: {
    position: 'absolute',
    width: '100%',
  },
}));
