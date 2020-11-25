import React, { ReactNode } from 'react';

export interface LabelOptions {
  bold?: boolean;
  italic?: boolean;
  hide?: boolean;
}

export interface FormFieldWrapperProps {
  required: boolean;
  disabled: boolean;
  label: React.ReactNode;
  labelOptions: LabelOptions;
  before?: ReactNode;
  after?: ReactNode;
}

export type FormFieldProps<COMPONENT_PROPS> = Omit<
  COMPONENT_PROPS,
  'value' | 'onChange'
> &
  Partial<FormFieldWrapperProps> & { name: string; notifyChange?: () => void };
