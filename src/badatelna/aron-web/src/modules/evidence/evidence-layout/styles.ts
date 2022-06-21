import makeStyles from '@material-ui/core/styles/makeStyles';
import {
    border,
    colorGrey,
    colorWhite
} from '../../../styles';

export interface StyleConfiguration {
  alternativeItemLabel?: boolean;
}

export const useStyles =  makeStyles((theme) => {
  const md = theme.breakpoints.up('md');
  const lg = theme.breakpoints.up('lg');

  return {
    wrapperStyle : {
      display: "flex", 
      flexDirection: 'column',
      height: 'auto',
      [md]: {
        flexDirection: 'row',
        height: '100%',
      }
    },
    treePaneStyle:{ 
      width: '100%',
      height: '100%',
      flexGrow: 0,
      padding: '10px',
      paddingRight: '0',
    },
    daoPaneStyle:{
      width: "100%", 
      height: 'auto',
      flexShrink: 0,
      flexGrow: 1,
      [md]:{
        flexShrink: 0,
        width: '100%',
        height: '80%',
      },
      [lg]:{
        width: "50%", 
        height: '100%',
        minWidth: '500px',
        flexGrow: 2,
      }
    },
    descPaneStyle:{ 
      width: '100%', 
      height: '100%',
      overflow: 'auto',
      flexGrow: 1,
    },
    itemDetailWrapper:{
      flexDirection: 'column',
      display: 'flex', 
      flexGrow: 1,
      [md]:{
        flexDirection: 'column',
        overflowY: 'auto',
        overflowX: 'hidden',
      },
      [lg]:{
        flexDirection: 'row',
      }
    },
    treeResizeHandleHorizontal: {
      position: 'absolute',
      width: '100%',
      height: '100%',
      background: colorWhite,
      color: colorGrey,
    },
    treeResizeHandleLeft: {
      borderLeft: border,
    },
    treeResizeHandleRight: {
      borderRight: border,
    },
    treeResizeHandleBottom: {
      borderBottom: border,
    },
    treeResizeHandleVertical: {
      position: 'absolute',
      width: '100%',
      height: '100%',
      background: colorWhite,
      color: colorGrey,
    },
    treeResizeHandleIcon: {
      fontSize: 14,
    },
  }
});
