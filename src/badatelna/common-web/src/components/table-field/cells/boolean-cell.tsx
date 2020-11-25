import React, { memo, ReactElement } from 'react';
import clsx from 'clsx';
import Typography from '@material-ui/core/Typography';
import CheckIcon from '@material-ui/icons/Check';
import ClearIcon from '@material-ui/icons/Remove';
import { TableFieldCellProps } from '../table-field-types';
import { useStyles } from '../table-field-styles';

export const BooleanCell = memo(function BooleanCell<OBJECT>({
  value,
}: TableFieldCellProps<OBJECT>) {
  const classes = useStyles();

  return (
    <Typography
      classes={{ root: clsx(classes.tableCell, classes.tableCellFlex) }}
    >
      {value ? <CheckIcon /> : <ClearIcon />}
    </Typography>
  );
}) as <OBJECT>(p: TableFieldCellProps<OBJECT>) => ReactElement;
