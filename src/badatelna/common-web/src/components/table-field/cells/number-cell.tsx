import React, { memo, ReactElement } from 'react';
import { TableFieldCellProps } from '../table-field-types';
import { TextCell } from './text-cell';

export const NumberCell = memo(function NumberCell<OBJECT>(
  props: TableFieldCellProps<OBJECT>
) {
  return <TextCell {...props} />;
}) as <OBJECT>(p: TableFieldCellProps<OBJECT>) => ReactElement;
