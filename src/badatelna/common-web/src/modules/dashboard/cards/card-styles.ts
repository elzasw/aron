import { makeStyles, createStyles } from '@material-ui/core/styles';

export const useStyles = makeStyles((theme) =>
  createStyles({
    card: {
      cursor: 'pointer',
      position: 'relative',
      border: 0,
      background: `${theme.palette.primary.main}33`,

      '&:hover': {
        background: `${theme.palette.primary.dark}33`,
      },
    },
    cardUniversal: {
      height: 91,
    },
    cardCustom: {
      height: 155,
    },
    cardAdd: {
      height: 155,
      border: `2px solid ${theme.palette.primary.main}33`,
      background: `inherit`,

      '&:hover': {
        background: `${theme.palette.grey[400]}33`,
      },
    },
    cardAddContent: {
      paddingTop: theme.spacing(1) + 'px!important',
      paddingBottom: theme.spacing(1) + 'px!important',
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      justifyContent: 'center',
      height: '100%',
    },
    cardActions: {
      border: 0,
      maxHeight: 54 * 3,
      overflowY: 'auto',
    },
    cardCustomContent: {
      paddingTop: theme.spacing(3),
      paddingBottom: theme.spacing(1) + 'px!important',
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
    },
    cardUniversalContent: {
      paddingTop: theme.spacing(3),
      paddingBottom: theme.spacing(1) + 'px!important',
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
    },
    cardUniversalTitle: {
      fontWeight: 600,
      fontSize: 12,
      lineHeight: 1.6,
      paddingTop: 10,
      paddingBottom: 10,
    },
    cardUniversalValue: {
      fontWeight: 400,
      fontSize: 10,
    },
    cardUniversalMenu: {
      left: 0,
      position: 'absolute',
      float: 'right',
      display: 'flex',
      flexDirection: 'column',
    },
    cardRemove: {
      right: 0,
      position: 'absolute',
      float: 'left',
      display: 'flex',
      flexDirection: 'column',
    },
    cardRemoveButton: {
      color: `${theme.palette.primary.main}55`,

      '&:hover': {
        color: `${theme.palette.primary.main}FF`,
      },
    },
    cardAction: {
      padding: '8px 16px',
      lineHeight: '1.5em',
    },

    cardActionValue: {
      fontWeight: 600,
      fontSize: 16,
      float: 'right',
    },
  })
);
