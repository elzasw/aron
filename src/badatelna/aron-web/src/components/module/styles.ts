import makeStyles from '@material-ui/core/styles/makeStyles';
import {
  appHeaderHeight,
  breadcrumbsHeight,
  colorText,
  border,
} from '../../styles';

export const useStyles = makeStyles((theme) => {
  return {
    module: {
      minHeight: `calc(100% - ${appHeaderHeight})`,
    },
    breadcrumbs: {
      height: breadcrumbsHeight,
      borderBottom: border,
      fontSize: 12,
    },
    breadcrumbsLink: {
      color: colorText,
      textDecoration: 'none',

      '&:hover': {
        textDecoration: 'underline',
      },
    },
  };
});
