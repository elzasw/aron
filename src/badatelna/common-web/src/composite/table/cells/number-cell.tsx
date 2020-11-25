import React, { memo, ReactElement } from 'react';
import Typography from '@material-ui/core/Typography';
import { TableCellProps } from '../table-types';
import { useStyles } from '../table-styles';

export const NumberCell = memo(function NumberCell<OBJECT>({
  value,
}: TableCellProps<OBJECT>) {
  const classes = useStyles();

  return <Typography className={classes.tableCell}>{value}</Typography>;
}) as <OBJECT>(p: TableCellProps<OBJECT>) => ReactElement;
