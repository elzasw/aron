import makeStyles from '@material-ui/core/styles/makeStyles';
import { colorBlueVeryLight, colorGreyLight } from '../../styles';

export const useStyles = makeStyles((theme) => {
  const md = theme.breakpoints.up('md');
  const border = `1px solid ${colorGreyLight}`;
  return {
    news: {
      background: colorBlueVeryLight,
      padding: theme.spacing(2),
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      [md]: {
        padding: theme.spacing(6),
        paddingTop: theme.spacing(4),
      },
    },
    newsItem: {
      marginBottom: theme.spacing(4),
      maxWidth: 1000,
    },
    itemHeading: {
      borderBottom: border,
      padding: theme.spacing(1, 2),
      display: 'flex',
      alignItems: 'baseline',
    },
    itemTitle: {
      paddingLeft: theme.spacing(2),
      fontSize: 16,
      fontWeight: 'bold',
      [md]: { fontSize: 22 },
    },
    itemText: {
      padding: theme.spacing(2),
    },
    itemLinks: {
      borderTop: border,
      padding: theme.spacing(1, 2, 2, 2),
    },
  };
});
