import React, { useContext, MouseEvent } from 'react';
import { useIntl } from 'react-intl';
import clsx from 'clsx';
import Box from '@material-ui/core/Box';
import Card from '@material-ui/core/Card';
import CardContent from '@material-ui/core/CardContent';
import IconButton from '@material-ui/core/IconButton';
import CloseIcon from '@material-ui/icons/Close';
import { useStyles } from '../card-styles';
import { CardCustomProps } from 'modules/dashboard/dashboard-types';
import { DashboardContext } from 'modules/dashboard/dashboard-context';
import { useEventCallback } from 'utils/event-callback-hook';

export function CardCustom({
  navigate,
  actions,
  children,
  definitionId,
}: CardCustomProps) {
  const intl = useIntl();
  const classes = useStyles();

  const { remove, classes: classesOverride } = useContext(DashboardContext);

  const handleRemove = useEventCallback((event: MouseEvent) => {
    remove(definitionId);

    event.stopPropagation();
  });

  return (
    <>
      <Card
        onClick={navigate}
        variant="outlined"
        className={clsx(
          classes.card,
          classes.cardCustom,
          classesOverride?.card
        )}
      >
        <Box className={classes.cardRemove}>
          <IconButton
            size="small"
            className={classes.cardRemoveButton}
            aria-label={intl.formatMessage({
              id: 'EAS_DASHBOARD_CARD_CUSTOM_REMOVE_BTN_ARIA',
              defaultMessage: 'Odstranit',
            })}
            onClick={handleRemove}
          >
            <CloseIcon fontSize={'small'} />
          </IconButton>
        </Box>
        <CardContent className={classes.cardCustomContent}>
          {children}
        </CardContent>
      </Card>

      <Card className={classes.cardActions} variant="outlined">
        {actions}
      </Card>
    </>
  );
}
