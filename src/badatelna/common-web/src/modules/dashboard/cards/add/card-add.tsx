import React, { useContext } from 'react';
import clsx from 'clsx';
import Card from '@material-ui/core/Card';
import CardContent from '@material-ui/core/CardContent';
import IconButton from '@material-ui/core/IconButton';
import AddIcon from '@material-ui/icons/Add';
import { DashboardContext } from '../../dashboard-context';
import { useStyles } from '../card-styles';

export function CardAdd() {
  const classes = useStyles();

  const { openAddDialog } = useContext(DashboardContext);

  return (
    <>
      <Card
        onClick={openAddDialog}
        variant="outlined"
        className={clsx(classes.card, classes.cardAdd)}
      >
        <CardContent className={classes.cardAddContent}>
          <IconButton color="primary">
            <AddIcon />
          </IconButton>
        </CardContent>
      </Card>
    </>
  );
}
