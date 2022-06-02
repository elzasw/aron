import makeStyles from '@material-ui/core/styles/makeStyles';

import { colorGreyLight } from '../../styles';

export const useStyles = makeStyles((theme) => {
  return {
    tree: {
      width: "calc(100% - 20px)",
      '& .MuiTreeItem-content': {
        alignItems: 'flex-start',
      },
    },
    endItem: { color: colorGreyLight },
  };
});
