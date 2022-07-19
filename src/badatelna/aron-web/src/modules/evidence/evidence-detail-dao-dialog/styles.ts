import makeStyles from '@material-ui/core/styles/makeStyles';
import { colorGrey } from '../../../styles';

const daoDialogToolbarHeight = 50;

export interface StyleConfiguration {
  alternativeItemLabel?: boolean;
}

export const useStyles =  makeStyles((theme) => {
  const md = theme.breakpoints.up('md');
  return {
    icon: {
      cursor: 'pointer',
    },
    bold: {
      fontWeight: 600,
    },
    daoDialog: {
      position: 'relative',
      top: 0,
      bottom: 0,
      left: 0,
      right: 0,
      background: 'rgba(0, 0, 0)',
      height: '100%',
      zIndex: 0,
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
    daoDialogSection: {
      position: 'absolute',
      top: daoDialogToolbarHeight,
      height: `calc(100% - ${daoDialogToolbarHeight}px)`,
      overflow: 'hidden',
    },
    daoDialogSide: {
      width: '100%',
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
      padding: '10px 25%',
      overflow: 'auto',
      [md]: {
        padding: '10px',
      }
    },
    daoThumbnailContainer: {
      width: '100%',
      borderRadius: '5px',
      overflow: 'hidden',
      cursor: 'pointer',
      position: 'relative',
      marginBottom: '4px'
    },
    daoThumbnailTitle: {
      color: 'white',
      position: 'absolute',
      zIndex: 10,
      padding: '5px 10px',
      textShadow: '0px 0px 8px black',
      bottom: 0,
      lineHeight: '1em',
      width: '100%',
      background: '#0007',
      maxHeight: '100%',
      whiteSpace: 'nowrap',
      overflow: 'hidden',
      textOverflow: 'ellipsis',
    },
    daoThumbnail: {
      width: '100%',
      filter: 'brightness(0.6)',
      alignItems: 'center',
      display: 'flex',

      '$daoThumbnailContainer:hover &': {
        color: '#fff',
        background: theme.palette.primary.main,
        filter: 'brightness(1)',
      },

      '& > img': {
        width: '100%',
      }
    },
    daoDialogSectionPartActive: {
      outline: `2px solid ${theme.palette.primary.main}`,

      '& $daoThumbnail': {
        cursor: 'auto',
        filter: 'brightness(1)',
        color: '#fff',
      }
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
  };
});
