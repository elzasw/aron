import makeStyles from '@material-ui/core/styles/makeStyles';

export const useStyles = makeStyles(() => ({
  underline: {
    borderBottomColor: '#cdcdcd',
    borderBottomWidth: 1,
    borderBottomStyle: 'solid',
  },
  spacing: {
    marginTop: 2,
    marginBottom: 2,
  },
  labelRoot: {
    color: '#333',
    display: 'flex',
    justifyContent: 'flex-end',
    marginRight: 8,
  },
  labelDisabled: {
    color: '#333 !important',
  },
  labelBold: {
    fontWeight: 700,
  },
  labelItalic: {
    fontStyle: 'italic',
  },
}));
