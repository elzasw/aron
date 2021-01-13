import React, { ReactNode } from 'react';
import { ValidationError } from 'composite/form/form-types';

export interface LabelOptions {
  bold?: boolean;
  italic?: boolean;
  hide?: boolean;
}

export interface LayoutOptions {
  noUnderline?: boolean;
}

export interface ErrorOptions {
  hide?: boolean;
}

export interface FormFieldWrapperProps {
  required: boolean;
  disabled: boolean;
  label: React.ReactNode;
  labelOptions: LabelOptions;
  layoutOptions: LayoutOptions;
  errorOptions: ErrorOptions;
  before?: ReactNode;
  after?: ReactNode;
  helpLabel?: string;
  errors?: ValidationError[];
}

export type FormFieldProps<COMPONENT_PROPS> = Omit<
  COMPONENT_PROPS,
  'value' | 'onChange'
> &
  Partial<FormFieldWrapperProps> & { name: string; notifyChange?: () => void };
