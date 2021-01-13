import { createContext } from 'react';

export interface FormFieldContext {
  prefix?: string;
  disabled?: boolean;
  required?: boolean;
}

export const FormFieldContext = createContext<FormFieldContext>({});
