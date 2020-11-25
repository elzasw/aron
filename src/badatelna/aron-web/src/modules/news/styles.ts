import makeStyles from '@material-ui/core/styles/makeStyles';
import { colorBlueVeryLight, colorGreyLight } from '../../styles';

export const useStyles = makeStyles((theme) => {
  const md = theme.breakpoints.up('md');
  const border = `1px solid ${colorGreyLight}`;
  return {
    newsWrapper: {
      background: colorBlueVeryLight,
      minHeight: 'calc(100vh - 96px)',
      padding: theme.spacing(2),
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      [md]: {
        padding: theme.spacing(6),
        paddingTop: theme.spacing(4),
      },
    },
    newsSingleItem: {
      marginBottom: theme.spacing(4),
      maxWidth: 1000,
    },
    itemHeading: {
      borderBottom: border,
      padding: theme.spacing(1),
      display: 'flex',
      alignItems: 'center',
    },
    itemTitle: {
      borderLeft: border,
      marginLeft: theme.spacing(1),
      paddingLeft: theme.spacing(1),
      fontSize: 16,
      [md]: { fontSize: 22 },
    },
    itemText: {
      padding: theme.spacing(1),
    },
    itemLinks: {
      borderTop: border,
      padding: theme.spacing(1),
    },
  };
});
