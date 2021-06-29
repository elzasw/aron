import { ComponentType, RefAttributes, ReactNode } from 'react';
import * as Yup from 'yup';
import { DialogHandle } from 'components/dialog/dialog-types';
import { FormikProps } from 'formik';
import { FormHandle } from 'composite/form/form-types';

export type Callback = (
  values: any,
  formRef?: React.RefObject<FormHandle<any>>
) => void | boolean | Promise<void | boolean>;

export interface Prompt<T = any> {
  key: string;

  dialogTitle: string | ReactNode;
  dialogText: string | ReactNode;
  dialogWidth?: number;
  dialogShowConfirm?: boolean;
  dialogShowClose?: boolean;

  FormFields?: ComponentType<{ onConfirm?: Callback; onCancel?: () => void }>;
  formValidationSchema?: Yup.Schema<T>;
  formInitialValues?: T;
  formOnSubmit?: (values: T, formik: FormikProps<T>) => void;
  clearCallback?: () => void;
}

export interface PromptDialogProps {
  prompt?: Prompt;
  onConfirm: Callback;
  onCancel: () => void;
  loading: boolean;
}

export interface PromptProviderProps {
  PromptDialogComponent?: ComponentType<
    PromptDialogProps & RefAttributes<DialogHandle>
  >;
}

export interface PromptContextType {
  testPrompt: ({ key, callback }: { key: string; callback: Callback }) => void;

  registerPrompts: (promt: Prompt[]) => void;
  unregisterPrompts: (promt: Prompt[]) => void;
}
