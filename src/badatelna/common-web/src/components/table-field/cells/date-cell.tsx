import React, { memo, ReactElement, useContext } from 'react';
import { format, parseISO } from 'date-fns';
import { LocaleContext } from 'common/locale/locale-context';
import { TableFieldCellProps } from '../table-field-types';
import { TextCell } from './text-cell';

export const DateCell = memo(function DateCell<OBJECT>(
  props: TableFieldCellProps<OBJECT>
) {
  const { locale } = useContext(LocaleContext);

  const date = props.value
    ? format(parseISO(props.value), locale.dateFormat)
    : '';

  return <TextCell {...props} value={date} />;
}) as <OBJECT>(p: TableFieldCellProps<OBJECT>) => ReactElement;
