import makeStyles from '@material-ui/core/styles/makeStyles';
import {
  appHeaderHeight,
  breadcrumbsHeight,
  colorBlueVeryLight,
  colorGrey,
  colorGreyLight,
  border,
  borderBold,
} from '../../styles';

const height = `calc(100vh - ${appHeaderHeight} - ${breadcrumbsHeight} - 1px)`;
const sidebarWidth = 300;
const sidebarMinWidth = 30;
const daoDialogToolbarHeight = 50;

export const useStyles = makeStyles((theme) => {
  const md = theme.breakpoints.up('md');
  return {
    sidebar: {
      position: 'fixed',
      left: 0,
      top: `calc(${appHeaderHeight} + ${breadcrumbsHeight})`,
      height,
      width: sidebarMinWidth,
      background: colorBlueVeryLight,
      borderRight: border,
      transition: 'width 300ms',
      zIndex: 1,

      [md]: {
        width: sidebarWidth,
      },
    },
    sidebarContent: {
      width: '100%',
      opacity: 0,
      transition: 'opacity 300ms ease',
      [md]: {
        opacity: 1,
      },
    },
    sidebarVisible: {
      width: sidebarWidth,
      overflowY: 'auto',
    },
    sidebarContentVisible: {
      display: 'block',
      opacity: 1,
    },
    visibleButton: {
      width: '100%',
      borderBottom: border,
      cursor: 'pointer',
      display: 'flex',
      justifyContent: 'flex-end',
      [md]: {
        display: 'none',
      },
    },
    sidebarItem: {
      '& h5': {
        color: theme.palette.primary.main,
        marginTop: '2rem',
        marginBottom: '0.75rem',
      },
      '& p': {
        margin: '0.8rem 4px 0',
        color: theme.palette.primary.dark,
        cursor: 'pointer',
        fontSize: 12,

        '&:hover': {
          textDecoration: 'underline',
        },
      },
      '& span': {
        color: colorGreyLight,
      },
    },
    list: {
      height: '100%',
      minHeight: '100%',
      width: `calc(100% - ${sidebarMinWidth}px)`,
      marginLeft: sidebarMinWidth,

      [md]: {
        width: `calc(100% - ${sidebarWidth}px)`,
        marginLeft: sidebarWidth,
      },
    },
    listItem: {
      width: '100%',
      borderRadius: 4,
      padding: `0.8rem 1.25rem`,
      cursor: 'pointer',

      '&:hover': {
        boxShadow: 'rgba(0, 0, 0, 0.35) 0px 5px 15px',
      },

      '& h4': {
        margin: '0 0 0.5rem',
        color: theme.palette.primary.main,
      },

      '& p': {
        margin: 0,
      },
    },
    listEmpty: {
      fontSize: 15,
    },
    listPageNumber: {
      display: 'flex',
      whiteSpace: 'nowrap',
      alignItems: 'center',
    },
    pageSizeSelect: {
      '& > div': {
        minWidth: 40,
      },
      '& div.MuiSelect-select.MuiSelect-select': {
        paddingRight: 2,
      },
    },
    evidenceDetail: {
      '& h3': {
        marginTop: 0,
      },
      '& h4': {
        marginTop: 0,
      },
    },
    evidenceDetailItem: {
      fontSize: 13,
    },
    evidenceDetailItemNotFirst: {
      borderTop: border,
    },
    evidenceDetailItemLabel: {
      width: 150,
      minWidth: 150,
      textTransform: 'uppercase',
      textAlign: 'right',
      borderRight: border,
    },
    evidenceDetailItemText: {
      marginTop: 0,
      marginBottom: 0,
      paddingBottom: theme.spacing(1),
    },

    toggleSidebarButtonOpen: {
      transform: 'rotate(180deg)',
      transition: 'transform 500ms ease',
      //alignItem: 'center',
    },
    toggleSidebarButtonClose: {
      transition: 'transform 500ms ease',
      //justifyContent: 'center',
    },

    bold: {
      fontWeight: 600,
    },

    evidenceDetailTop: {
      borderBottom: borderBold,
    },

    evidenceDetailTopIcon: {
      transform: 'rotate(90deg)',
      cursor: 'pointer',
    },

    evidenceDetailTopIconOpen: {
      transform: 'rotate(270deg)',
    },

    evidenceDetailImage: {
      fontSize: 50,
    },
    evidenceDetailImageBig: {
      fontSize: 150,
    },

    link: {
      cursor: 'pointer',
      textDecoration: 'underline',
    },
    icon: {
      cursor: 'pointer',
    },

    dao: {},
    daoPreview: {
      width: 150,
      height: 100,
      border,
    },
    daoPreviewImage: {
      width: '100%',
      height: '100%',
      maxWidth: '100%',
      maxHeight: '100%',

      '& > img': {
        maxWidth: '100%',
        maxHeight: '100%',
        objectFit: 'contain',
      },
      '& > svg': {
        fontSize: 60,
      },
    },
    daoShowAll: {
      width: 80,
      height: 100,
      border,
      cursor: 'pointer',
      textAlign: 'center',

      '&:hover': {
        border: `1px solid ${colorGrey}`,
      },
    },
    daoDialog: {
      position: 'fixed',
      top: 0,
      bottom: 0,
      left: 0,
      right: 0,
      background: 'rgba(0, 0, 0, 0.85)',
      zIndex: 10000,
    },
    daoDialogToolbar: {
      height: daoDialogToolbarHeight,
      width: '100vw',
    },
    daoDialogToolbarInner: {
      height: 'calc(100% - 8px)',
    },
    loadingPlaceholder: {
      height: 4,
    },
    daoDialogIcon: {
      color: '#fff',
      cursor: 'pointer',
      fontSize: 24,

      [md]: {
        fontSize: 30,
      },
    },
    daoDialogIconDisabled: {
      color: colorGrey,
      cursor: 'auto',
    },
    daoDialogSection: {
      position: 'absolute',
      top: daoDialogToolbarHeight,
      height: `calc(100% - ${daoDialogToolbarHeight}px)`,
      overflow: 'hidden',
    },
    daoDialogSide: {
      width: '50%',
      display: 'none',

      [md]: {
        width: '20%',
        display: 'block',
      },
    },
    daoDialogLeft: {
      left: 0,
      padding: '0 8px 8px 16px',
    },
    daoDialogCenter: {
      left: 0,
      width: '100%',
      display: 'none',

      [md]: {
        width: '60%',
        left: '20%',
        display: 'block',
      },
    },
    daoDialogRight: {
      right: 0,
      padding: '30px 16px 8px 8px',
    },
    daoDialogSideOpen: {
      display: 'block',
    },
    daoDialogCenterOpen: {
      display: 'block',
    },
    daoDialogSectionPart: {},
    daoDialogSectionPartLabel: {
      color: '#fff',
      height: 30,
    },
    daoDialogSectionPartContent: {
      background: '#fff',
      maxHeight: `calc(50vh - ${daoDialogToolbarHeight / 2}px - 30px - 8px)`,
      overflowY: 'auto',

      '& div': {
        cursor: 'pointer',
        padding: '4px 8px',

        '&:hover': {
          color: '#fff',
          background: theme.palette.primary.main,
        },
      },
    },
    daoDialogSectionPartActive: {
      cursor: 'auto',
      color: '#fff',
      background: theme.palette.primary.main,
    },
    daoDialogMetadata: {
      background: '#fff',
      maxHeight: `calc(100vh - ${daoDialogToolbarHeight}px - 30px - 8px)`,
      overflowY: 'auto',
    },
    daoDialogMetadataLabel: {
      width: 'calc(50% - 8px)',
      paddingRight: 8,
      textAlign: 'right',
    },
    daoDialogMenu: {
      [md]: {
        display: 'none',
      },
    },
    daoDialogMenuPlaceholder: {
      display: 'none',
      width: 30,

      [md]: {
        display: 'block',
      },
    },

    treeWrapper: {
      width: '100%',
      minWidth: 'fit-content',
      height: '100%',
      overflow: 'auto',
    },
    treeResizeHandle: {
      width: '100%',
      background: '#fff',
      borderTop: border,
      borderBottom: border,
    },
    treeResizeHandleIcon: {
      fontSize: 14,
    },
    treeCollapseIcon: {
      cursor: 'pointer',
    },

    findRelatedButton: {
      height: '100%',
      cursor: 'pointer',
      background: colorBlueVeryLight,
      whiteSpace: 'nowrap',

      '&:hover': {
        color: '#fff',
        background: theme.palette.primary.main,
      },
    },
  };
});