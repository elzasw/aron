import makeStyles from '@material-ui/core/styles/makeStyles';
import {
  // appHeaderHeight,
  // breadcrumbsHeight,
  colorBlueDark,
  colorBlueVeryLight,
  colorGrey,
  colorGreyLight,
  colorWhite,
  border,
  borderBold,
  colorBlue,
} from '../../styles';

const sidebarWidth = 300;
const sidebarMinWidth = 30;
const daoDialogToolbarHeight = 50;

export interface StyleConfiguration {
  alternativeItemLabel?: boolean;
}

export const useStyles =  makeStyles((theme) => {
  const md = theme.breakpoints.up('md');
  return {
    sidebar: {
      position: 'absolute',
      left: 0,
      // top: `calc(${appHeaderHeight} + ${breadcrumbsHeight})`,
      height: '100%',
      width: sidebarMinWidth,
      background: colorBlueVeryLight,
      borderRight: border,
      transition: 'width 300ms',
      zIndex: 1,

      [md]: {
        width: sidebarWidth,
        overflowY: 'auto',
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
    sidebarButton: {
      width: '100%',
    },
    listContainer: {
      overflowX: 'auto',
      height: '100%',
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
        whiteSpace: "pre-wrap",
      },
    },
    listItemTitleOnly: {
      '& h4': {
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
      overflowX: 'auto',
      height: '100%',
      '& h3': {
        marginTop: 0,
      },
      '& h4': {
        marginTop: 0,
      },
    },
    evidenceDetailDescription: {
      fontWeight: 400,
    },
    evidenceDetailItem: {
      fontSize: 13,
    },
    evidenceDetailItemNotFirst: {
      borderTop: border,
    },
    evidenceDetailItemLabel: ({alternativeItemLabel}:StyleConfiguration) => ({
      width: 150,
      minWidth: 150,
      textTransform: alternativeItemLabel ? 'none' : 'uppercase',
      textAlign: 'right',
      fontWeight: alternativeItemLabel ? 'bold' : 'normal',
    }),
    evidenceDetailItemLabelBorder: {
      borderRight: border,
    },
    evidenceDetailItemText: {
      marginTop: 0,
      marginBottom: 0,
      whiteSpace: "pre-wrap",
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

    evidenceDetailIcon: {
      fontSize: 60,
    },

    findRelatedButton: {
      padding: '4px 8px !important',
    },

    link: {
      cursor: 'pointer',
      textDecoration: 'underline',
      color: 'rgba(0, 0, 0, 0.87)',

      '&:hover': {
        color: 'rgba(0, 0, 0, 0.87)',
      },

      '&:visited': {
        color: 'rgba(0, 0, 0, 0.87)',
      },

      '&:active': {
        color: 'rgba(0, 0, 0, 0.87)',
      },
    },
    archdescRootLink: {
      padding: '10px',
      textDecoration: 'none',
      background: colorBlue,
      color: colorWhite,
      fontSize: '1.1rem',
      borderRadius: '5px',

      '&:hover': {
        background: colorBlueDark,
        color: colorWhite,
      },

      '&:visited': {
        color: colorWhite,
      },

      '&:active': {
        color: colorWhite,
      },
    },
    icon: {
      cursor: 'pointer',
    },

    daoContainer: {
      padding: 10,
      borderBottom: border,
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
      position: 'relative',
      top: 0,
      bottom: 0,
      left: 0,
      right: 0,
      background: 'rgba(0, 0, 0)',
      height: '100%',
    },
    daoDialogFixed: {
      position: 'fixed',
      top: 0,
      bottom: 0,
      left: 0,
      right: 0,
      background: 'rgba(0, 0, 0, 0.85)',
      height: '100vh',
      width: '100vw',
      zIndex: 100,
    },
    daoDialogToolbar: {
      // height: daoDialogToolbarHeight,
      // width: '100vw',
      padding: '10px',
      width: '100%',
      position: 'absolute',
      background: '#0009',
      zIndex: 1,
    },
    daoDialogToolbarInner: {
      // height: 'calc(100% - 8px)',
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
        width: '160px',
        display: 'block',
      },
    },
    daoDialogLeft: {
      zIndex: 10,
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
    daoDialogCenterNoSidebar: {
      [md]: {
        width: 'calc(100% - 160px)',
        height: '100%',
        top: 0,
        left: '160px',
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
    daoDialogNoFiles: {
      color: '#fff',
      width: '100%',
      height: '100%',
    },
    daoDialogSectionPart: {
      background: '#0009',
      height: '100%',
      overflowY: 'auto',
    },
    daoDialogSectionPartLabel: {
      color: '#fff',
      height: 30,
    },
    daoDialogSectionPartContent: {
      padding: '10px',
      overflow: 'auto',
    },
    daoDialogSectionPartThumbnail: {
      width: '100%',
      margin: '2px',
      borderRadius: '5px',
      overflow: 'hidden',
      filter: 'brightness(0.6)',
      cursor: 'pointer',
      alignItems: 'center',
      display: 'flex',

      '&:hover': {
        color: '#fff',
        background: theme.palette.primary.main,
        filter: 'brightness(1)',
      },

      '& > img': {
        width: '100%',
      }
    },
    daoDialogSectionPartActive: {
      cursor: 'auto',
      filter: 'brightness(1)',
      color: '#fff',
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
      // minWidth: 'fit-content',
      height: '100%',
      overflow: 'hidden',
      overflowY: 'auto',
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

    evidenceTextInfo: {
      height: '100%',
      maxWidth: '240px',
      whiteSpace: 'normal',
      display: 'flex',
      alignItems: 'center',
      paddingLeft: theme.spacing(1),
      paddingRight: theme.spacing(1),
      textAlign: 'right',

      [theme.breakpoints.up('md')]: {
        whiteSpace: 'nowrap',
        maxWidth: 'none',
      },
    },

    itemsCount: {
      color: theme.palette.grey[500],
    },

    attachment: {
      border,
      borderRadius: 4,
      width: 'fit-content',
    },

    attachmentLabel: {
      flex: 1,
    },

    attachmentDownload: {
      cursor: 'pointer',
    },

    tooManyresults: {
      '& h3': {
        marginTop: '0.2em',
        marginBottom: '0.4em',
      },
      '& p': {
        marginTop: 0,
        marginBottom: '1.1em',
      },
    },

    shareButtonsContainer: {
      display: 'flex',
      marginBottom: '8px',
    },

    shareIcon: {
      height: '100%',
      width: '100%',
    },

    shareButton: {
      height: '30px',
      width: '30px',
      display: 'flex',
      margin: theme.spacing(0.5),
      overflow: 'hidden',
      borderRadius: theme.shape.borderRadius,
    }
  };
});
