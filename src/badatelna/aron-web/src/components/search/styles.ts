import makeStyles from '@material-ui/core/styles/makeStyles';

export const useStyles = makeStyles((theme) => {
  return {
    search: {
      width: '100%',
    },
    searchInner: {
      position: 'relative',
      width: '100%',
      height: 40,
      borderRadius: 5,
      '& $searchTextField .MuiInputBase-root': {
        height: 40,
      },
      '&:hover': {
        boxShadow: 'rgba(0, 0, 0, 0.35) 0px 5px 15px',
      },
    },
    searchBigInner: {
      height: 60,
      '& $searchTextField .MuiInputBase-root': {
        height: 60,
      },
    },
    searchAdvanced: {
      fontSize: '0.7rem',
      fontWeight: 600,
      color: theme.palette.primary.dark,
      cursor: 'pointer',
      zIndex: 1,
    },
    searchIcon: {
      fontSize: '1.8rem',
    },
    searchButton: {
      height: '100%',
      borderRadius: '0 5px 5px 0 !important',
    },
    searchTextField: {
      flexGrow: 1,
      borderRadius: 5,
      '& .MuiInputBase-root': {
        borderRadius: '5px 0 0 5px',
      },
    },
  };
});
