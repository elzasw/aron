import { createContext } from 'react';
import { FormHandle } from './form-types';

export type FormContext<T> = FormHandle<T>;

export const FormContext = createContext<FormContext<any>>(undefined as never);
