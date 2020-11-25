import React from 'react';
import { FilterComponentProps } from '../table-types';
import { TextField } from 'components/text-field/text-field';

export function FilterTextCell({
  disabled,
  value,
  onChange,
}: FilterComponentProps) {
  return (
    <TextField
      //placeholder={!disabled ? 'zadejte text...' : ''}
      value={value}
      disabled={disabled}
      onChange={onChange}
    />
  );
}
