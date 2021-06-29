import { makeStyles, createStyles } from '@material-ui/core/styles';

export const useStyles = makeStyles(() =>
  createStyles({
    root: {
      display: 'grid',
      gridAutoRows: 'minmax(min-content, max-content)',
    },
  })
);
