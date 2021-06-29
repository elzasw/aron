import React from 'react';
import { ReadonlyFieldProps } from './readonly-field-types';
import { TextField } from 'components/text-field/text-field';
import { identity } from 'lodash';

export function ReadonlyField({
  dataMapper = identity,
  ...props
}: ReadonlyFieldProps) {
  return <TextField {...props} value={dataMapper(props.value)} disabled />;
}
