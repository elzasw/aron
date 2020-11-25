import makeStyles from '@material-ui/core/styles/makeStyles';

export const useStyles = makeStyles(() => ({
  uploadButton: {
    boxShadow: '0 0 0',
    padding: '2pt',
    height: 23,
    '& p': {
      // contradict the default 'none'
      display: 'block',
    },
  },
  downloadButton: {
    minWidth: '0',
    marginLeft: '2pt',
    boxShadow: '0 0 0',
    padding: '2pt',
    height: 23,
    '& p': {
      // contradict the default 'none'
      display: 'block',
    },
  },
}));
