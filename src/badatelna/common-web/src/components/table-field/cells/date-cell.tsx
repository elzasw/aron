import React, { memo, ReactElement, useContext } from 'react';
import { format, parseISO } from 'date-fns';
import Typography from '@material-ui/core/Typography';
import { LocaleContext } from 'common/locale/locale-context';
import { TableFieldCellProps } from '../table-field-types';
import { useStyles } from '../table-field-styles';

export const DateCell = memo(function DateCell<OBJECT>({
  value,
}: TableFieldCellProps<OBJECT>) {
  const classes = useStyles();

  const { locale } = useContext(LocaleContext);

  const date = value ? format(parseISO(value), locale.dateFormat) : '';

  return <Typography className={classes.tableCell}>{date}</Typography>;
}) as <OBJECT>(p: TableFieldCellProps<OBJECT>) => ReactElement;
