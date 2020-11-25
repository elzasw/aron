import React, { memo, ReactElement } from 'react';
import Typography from '@material-ui/core/Typography';
import { TableFieldCellProps } from '../table-field-types';
import { useStyles } from '../table-field-styles';

export const TextCell = memo(function TextCell<OBJECT>({
  value,
}: TableFieldCellProps<OBJECT>) {
  const classes = useStyles();

  return <Typography className={classes.tableCell}>{value}</Typography>;
}) as <OBJECT>(p: TableFieldCellProps<OBJECT>) => ReactElement;
