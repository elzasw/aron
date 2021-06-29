import React, { memo, ReactElement, useContext } from 'react';
import { format } from 'date-fns';
import { LocaleContext } from 'common/locale/locale-context';
import { TableFieldCellProps } from '../table-field-types';
import { TextCell } from './text-cell';
import { parseISOTimeSafe } from 'utils/date-utils';

export const TimeCell = memo(function TimeCell<OBJECT>(
  props: TableFieldCellProps<OBJECT>
) {
  const { locale } = useContext(LocaleContext);

  const parsed = parseISOTimeSafe(props.value);

  const date = parsed ? format(parsed, locale.timeFormat) : '';

  return <TextCell {...props} value={date} />;
}) as <OBJECT>(p: TableFieldCellProps<OBJECT>) => ReactElement;
