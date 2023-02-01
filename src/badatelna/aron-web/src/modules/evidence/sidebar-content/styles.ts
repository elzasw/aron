import makeStyles from '@material-ui/core/styles/makeStyles';

import { border } from '../../../styles';

export const useStyles = makeStyles((theme) => {
  const md = theme.breakpoints.up('md');
  return {
    filterTitle: {
      fontWeight: 600,
      fontSize: '1.3em',
      color: theme.palette.primary.main,
      margin: theme.spacing(2, 1, 1, 0),
      display: 'flex',
    },
    filterDescription: {
      marginBottom: theme.spacing(1),
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
    listedItemCheckbox: {
      alignItems: 'flex-start',

      '& > div': {
        paddingTop: 3,
      },
    },
    radioButton: {
      padding: 0,
    },

    filterDialog: {
      [md]: { width: '90vw' },
      height: 600,
      maxWidth: 1200,
      maxHeight: '70vh',
      margin: '-10px -24px -16px -24px', // TODO: for later: style common-web dialog and remove this
      flex: 1,
      overflow: 'hidden',
      
    },
    filterDialogLeft: {
      width: '30%',
      borderRight: border,
      overflow: 'auto',
      height: '100%',
    },
    filterDialogRight: {
      width: '70%',
      overflow: 'auto',
      height: '100%',
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
    rangeFilterSlider: {
      marginTop: 30,
    },
    rangeFilterSliderWrapper: {
      width: '90%',
      margin: 'auto',
    },
    rangeFilterRefreshButton: {
      position: 'absolute',
      right: theme.spacing(2),
    },
    rangeFilterTitle: {
      maxWidth: 'calc(100% - 1.1em)',
    },
    autocompleteFilter: {
      '& .EasInput-root': {
        '& div[role=button]': {
          height: 'auto',
          minHeight: 20,
          maxWidth: '100%',
          marginBottom: 5,
          '& .MuiChip-label': {
            whiteSpace: 'normal',
          },
        },
      },
    },
  };
});
