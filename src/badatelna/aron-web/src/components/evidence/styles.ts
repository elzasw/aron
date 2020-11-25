import makeStyles from '@material-ui/core/styles/makeStyles';
import {
  appHeaderHeight,
  breadcrumbsHeight,
  colorBlueVeryLight,
  colorGreyLight,
  border,
} from '../../styles';

const height = `calc(100vh - ${appHeaderHeight} - ${breadcrumbsHeight} - 1px)`;
const sidebarWidth = 300;
const sidebarMinWidth = 30;

export const useStyles = makeStyles((theme) => {
  const md = theme.breakpoints.up('md');
  return {
    evidence: {
      position: 'relative',
      height: height,
      minHeight: height,
      maxHeight: height,
    },
    evidenceMain: {
      height: '100%',
    },
    sidebar: {
      position: 'absolute',
      top: 0,
      left: 0,
      height: '100%',
      width: sidebarMinWidth,
      background: colorBlueVeryLight,
      borderRight: border,
      transition: 'width 0.3s',
      overflow: 'hidden',

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
      maxHeight: '100%',
      width: `calc(100% - ${sidebarMinWidth}px)`,
      marginLeft: sidebarMinWidth,
      overflowY: 'auto',

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

    evidenceDetail: {
      '& h3': {
        marginTop: 0,
      },
      '& h4': {
        marginTop: 0,
      },
      '& p': {
        marginTop: 0,
      },
    },
    evidenceDetailItem: {
      fontSize: 13,
    },
    evidenceDetailItemLabel: {
      width: 180,
      minWidth: 180,
      textTransform: 'uppercase',
      textAlign: 'right',
    },

    toggleSidebarButtonOpen: {
      transform: 'rotate(180deg)',
      transition: 'transform 500ms ease',
    },
    toggleSidebarButtonClose: {
      transition: 'transform 500ms ease',
    },

    bold: {
      fontWeight: 600,
    },

    evidenceDetailTop: {
      borderBottom: border,
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

    link: {
      cursor: 'pointer',
      textDecoration: 'underline',
    },

    dao: {},
    daoPreview: {
      width: '15rem',
      height: '10rem',
      maxWidth: '100%',
      maxHeight: '80vh',
      border,
    },
    daoDialog: {
      position: 'fixed',
      top: 0,
      bottom: 0,
      left: 0,
      right: 0,
      background: 'rgba(0, 0, 0, 0.75)',
    },
    daoDialogIcon: {
      color: '#fff',
    },
  };
});
