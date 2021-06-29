import React, { useContext } from 'react';
import { format } from 'date-fns';
import Typography from '@material-ui/core/Typography';
import { LocaleContext } from 'common/locale/locale-context';
import { TableCellProps } from '../table-types';
import { useStyles } from '../table-styles';
import { parseISOTimeSafe } from 'utils/date-utils';

export function TimeCell<OBJECT>({ value }: TableCellProps<OBJECT>) {
  const classes = useStyles();

  const { locale } = useContext(LocaleContext);

  const parsed = parseISOTimeSafe(value);
  const date = parsed ? format(parsed, locale.timeFormat) : '';

  return <Typography className={classes.tableCell}>{date}</Typography>;
}

TimeCell.displayName = 'TimeCell';
