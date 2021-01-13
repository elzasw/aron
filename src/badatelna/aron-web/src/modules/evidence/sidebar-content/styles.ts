import makeStyles from '@material-ui/core/styles/makeStyles';

import { border } from '../../../styles';

export const useStyles = makeStyles((theme) => {
  const md = theme.breakpoints.up('md');
  return {
    filterTitle: {
      fontWeight: 600,
      fontSize: '1.5em',
      color: theme.palette.primary.main,
      margin: theme.spacing(2, 1, 1, 0),
    },
    bottomText: {
      cursor: 'pointer',
      marginTop: theme.spacing(1),
      fontWeight: 500,
      width: 'fit-content',
    },
    listedItem: {
      cursor: 'pointer',
      width: 'fit-content',
      display: 'flex',
      alignItems: 'center',
      paddingBottom: theme.spacing(1),
      '&>div': {
        paddingLeft: theme.spacing(0.5),
      },
      '&:last-child': {
        padding: 0,
      },
    },
    radioButton: {
      padding: 0,
    },

    filterDialog: {
      [md]: { width: '90vw' },
      maxWidth: 1200,
      margin: '-10px -24px -16px -24px', // TODO: for later: style common-web dialog and remove this
    },
    filterDialogLeft: {
      width: '30%',
      borderRight: border,
    },
    filterDialogRight: {
      width: '70%',
    },
    filterDialogItem: {
      width: '100%',
      minWidth: '100%',
      cursor: 'pointer',

      '&:hover': {
        background: theme.palette.primary.light,
        color: '#fff',
      },
    },
    filterDialogItemActive: {
      background: theme.palette.primary.light,
      color: '#fff',
    },
    inputFilterTextField: {
      width: '100%',
    },
    relationshipFilterCreatorWrapper: {
      '& > *': {
        marginBottom: theme.spacing(1),
      },
    },
    relationshipFilterListItem: {
      display: 'flex',
      alignItems: 'center',
      width: '100%',
    },
    relationshipFilterLabel: {
      fontWeight: 'bold',
      marginBottom: theme.spacing(1),
    },
  };
});
