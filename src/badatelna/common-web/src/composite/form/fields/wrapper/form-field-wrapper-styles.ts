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
  },
  labelError: {
    color: '#CD5360',
  },
  labelText: {
    textAlign: 'right',
    lineHeight: '22px',
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
  boxRoot: {
    display: 'flex',
    justifyContent: 'flex-end',
    alignItems: 'center',
    marginRight: '8px',
  },
  boxError: {
    color: '#CD5360',
  },
}));
