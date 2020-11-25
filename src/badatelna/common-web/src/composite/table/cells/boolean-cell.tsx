import React, { memo, ReactElement } from 'react';
import clsx from 'clsx';
import Typography from '@material-ui/core/Typography';
import CheckIcon from '@material-ui/icons/Check';
import ClearIcon from '@material-ui/icons/Remove';
import { TableCellProps } from '../table-types';
import { useStyles } from '../table-styles';

export const BooleanCell = memo(function BooleanCell<OBJECT>({
  value,
}: TableCellProps<OBJECT>) {
  const classes = useStyles();

  return (
    <Typography
      classes={{ root: clsx(classes.tableCell, classes.tableCellFlex) }}
    >
      {value ? <CheckIcon /> : <ClearIcon />}
    </Typography>
  );
}) as <OBJECT>(p: TableCellProps<OBJECT>) => ReactElement;
