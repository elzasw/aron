import React, { PropsWithChildren, useContext } from 'react';
import { FormFieldProps } from './wrapper/form-field-wrapper-types';
import { FormFieldWrapper } from './wrapper/form-field-wraper';
import { FormContext } from '../form-context';

export type FormCustomFieldProps = Omit<FormFieldProps<any>, 'name'>;

export function FormCustomField({
  name,
  label,
  disabled = false,
  required = false,
  labelOptions = {},
  layoutOptions = {},
  errorOptions = {},
  children,
}: PropsWithChildren<FormCustomFieldProps>) {
  const { errors } = useContext(FormContext);

  const fieldErrors = errors.filter((e) => e.key === name);

  return (
    <FormFieldWrapper
      label={label}
      required={required}
      disabled={disabled}
      labelOptions={labelOptions}
      layoutOptions={layoutOptions}
      errorOptions={errorOptions}
      errors={fieldErrors}
    >
      {children}
    </FormFieldWrapper>
  );
}
