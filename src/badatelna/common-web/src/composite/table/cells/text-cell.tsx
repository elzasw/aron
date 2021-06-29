import React from 'react';
import Typography from '@material-ui/core/Typography';
import { TableCellProps } from '../table-types';
import { useStyles } from '../table-styles';

export function TextCell<OBJECT>({ value }: TableCellProps<OBJECT>) {
  const classes = useStyles();

  return <Typography className={classes.tableCell}>{value}</Typography>;
}

TextCell.displayName = 'TextCell';
