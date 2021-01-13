import { makeStyles, Theme, createStyles } from '@material-ui/core/styles';

export const useStyles = makeStyles((theme: Theme) =>
  createStyles({
    row: {
      boxSizing: 'border-box',
      borderBottom: '1px solid #cdcdcd',
      minWidth: '100%',
      height: 30,
      '&:hover': {
        backgroundColor: theme.palette.highlight,
      },
      padding: '2px 24px',
      display: 'flex',
      alignItems: 'center',
      flexShrink: 0,
    },
    tableRowActions: {
      flexShrink: 0,
      width: 30,
      height: 20,
      verticalAlign: 'top',
      display: 'inline-block',
      padding: 0,
      overflow: 'hidden',
      '& svg': {
        cursor: 'pointer',
        width: 20,
        height: 20,
        padding: 0,
      },
    },
    cellWrapper: {
      flexShrink: 0,
      display: 'flex',
      paddingRight: 10,
      boxSizing: 'border-box',
    },
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
  })
);
