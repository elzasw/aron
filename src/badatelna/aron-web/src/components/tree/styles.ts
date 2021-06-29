import makeStyles from '@material-ui/core/styles/makeStyles';

import { colorGreyLight } from '../../styles';

export const useStyles = makeStyles((theme) => {
  return {
    tree: {
      '& .MuiTreeItem-content': {
        alignItems: 'flex-start',
      },
    },
    endItem: { color: colorGreyLight },
  };
});
