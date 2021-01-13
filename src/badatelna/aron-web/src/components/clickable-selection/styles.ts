import makeStyles from '@material-ui/core/styles/makeStyles';

export const useStyles = makeStyles((theme) => {
    return {
        option: {
            display: 'flex',
            alignItems: 'center',
            cursor: 'pointer',
            width: 'fit-content',
            marginBottom: theme.spacing(0.5),
            '& :first-child': {
                marginRight: theme.spacing(0.5),
            },
        },
        radioOption: {
            margin: 0,
            '& :first-child': {
                margin: 0,
                '&:hover': { background: 'none' },
            },
        },
    };
});
