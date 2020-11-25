import makeStyles from '@material-ui/core/styles/makeStyles';

export const useStyles = makeStyles((theme) => ({
  textField: {
    background: theme.palette.common.white,
    '& .MuiInputBase-root   ': {
      borderRadius: 5,
    },
  },
}));
