import makeStyles from '@material-ui/core/styles/makeStyles';

export const useLayoutStyles = makeStyles((theme) => ({
  flex: {
    display: 'flex',
  },
  flexCentered: {
    display: 'flex',
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
  },
  flexSpaceBetween: {
    display: 'flex',
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  flexEnd: {
    display: 'flex',
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'flex-end',
  },
  flexAlignCenter: {
    display: 'flex',
    flexDirection: 'row',
    alignItems: 'center',
  },
  flexAlignTop: {
    display: 'flex',
    flexDirection: 'row',
    alignItems: 'flex-start',
  },
  flexColumn: {
    display: 'flex',
    flexDirection: 'column',
  },
  flexWrap: {
    flexWrap: 'wrap',
  },
}));
