import makeStyles from '@material-ui/core/styles/makeStyles';

export const useStyles = makeStyles((theme) => {
  return {
    button: {
      padding: theme.spacing(1.5),
      borderRadius: 5,
    },
  };
});
