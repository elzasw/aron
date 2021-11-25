import makeStyles from '@material-ui/core/styles/makeStyles';

export const useStyles = makeStyles((theme) => {
  const languageContainerPadding = theme.spacing(0.5);
  return {
    flag: {
      width: 30,
      cursor: 'pointer',
      margin: theme.spacing(0.5),
      borderRadius: theme.shape.borderRadius/2,
    },
    languageContainer: {
      position: 'relative',
      display: 'flex',
      '&:hover $languageSelector': {
        visibility: 'visible',
        }

    },
    languageSelector: {
      visibility: 'hidden',
      position: 'absolute',
      top: languageContainerPadding*-1,
      left: languageContainerPadding*-1,
      padding: languageContainerPadding,
      backgroundColor: theme.palette.primary.main,
      boxShadow: theme.shadows[2],
      display: 'flex',
      flexDirection: 'column',
      borderRadius: theme.shape.borderRadius,
      zIndex: 1000,
    },
  };
});
