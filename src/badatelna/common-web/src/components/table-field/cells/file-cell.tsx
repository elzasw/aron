import React from 'react';
import { TableFieldCellProps } from '../table-field-types';
import { noop } from 'lodash';
import { FileField } from 'components/file-field/file-field';

export function FileCell<OBJECT>({ value }: TableFieldCellProps<OBJECT>) {
  return (
    <FileField
      disabled={true}
      value={value}
      onChange={noop}
      showUpload={false}
      showClear={false}
    />
  );
}
