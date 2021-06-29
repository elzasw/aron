import React, { useContext } from 'react';
import clsx from 'clsx';
import CardActionArea from '@material-ui/core/CardActionArea';
import Typography from '@material-ui/core/Typography';
import { CardActionProps } from '../../dashboard-types';
import { useStyles } from '../card-styles';
import { DashboardContext } from 'modules/dashboard/dashboard-context';

export function CardAction({ navigate, title, value }: CardActionProps) {
  const classes = useStyles();

  const { classes: classesOverride } = useContext(DashboardContext);

  return (
    <CardActionArea onClick={navigate}>
      <Typography
        variant="body2"
        className={clsx(classes.cardAction, classesOverride?.cardAction)}
      >
        {title}
        {value !== undefined && (
          <span
            className={clsx(
              classes.cardActionValue,
              classesOverride?.cardActionValue
            )}
          >
            {value}
          </span>
        )}
      </Typography>
    </CardActionArea>
  );
}
