import makeStyles from '@material-ui/core/styles/makeStyles';

export const useStyles = makeStyles((theme) => {
  return {
    fileViewer: {
      width: '100%',
      height: '100%',
    },
    imageViewer: { width: '100%', height: '100%' },
    pdfViewerError: {
      color: theme.palette.error.main,
    },
  };
});
