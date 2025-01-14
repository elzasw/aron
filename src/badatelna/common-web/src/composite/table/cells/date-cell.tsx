import React, { useContext } from 'react';
import { format, parseISO } from 'date-fns';
import Typography from '@material-ui/core/Typography';
import { LocaleContext } from 'common/locale/locale-context';
import { TableCellProps } from '../table-types';
import { useStyles } from '../table-styles';

export function DateCell<OBJECT>({ value }: TableCellProps<OBJECT>) {
  const classes = useStyles();

  const { locale } = useContext(LocaleContext);

  const date = value ? format(parseISO(value), locale.dateFormat) : '';

  return <Typography className={classes.tableCell}>{date}</Typography>;
}

DateCell.displayName = 'DateCell';
