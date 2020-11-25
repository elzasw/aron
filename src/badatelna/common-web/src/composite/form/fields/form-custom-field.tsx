import React, { PropsWithChildren } from 'react';
import { FormFieldProps } from './wrapper/form-field-wrapper-types';
import { FormFieldWrapper } from './wrapper/form-field-wraper';

export type FormCustomFieldProps = Omit<FormFieldProps<any>, 'name'>;

export function FormCustomField({
  label,
  disabled = false,
  required = false,
  labelOptions = {},
  children,
}: PropsWithChildren<FormCustomFieldProps>) {
  return (
    <FormFieldWrapper
      label={label}
      required={required}
      disabled={disabled}
      labelOptions={labelOptions}
    >
      {children}
    </FormFieldWrapper>
  );
}
