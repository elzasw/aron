import * as Yup from 'yup';

export interface FormHandle<DATA = any> {
  formId: string;
  editing: boolean;
  errors: ValidationError[];
  setErrors: (errors: ValidationError[]) => void;
  getFieldValues: () => DATA;
  setFieldValues: (values: DATA) => void;
  setFieldValue: (name: string, value: any) => void;
  submitForm: () => void;
  clearForm: () => void;
  validateForm: () => Promise<ValidationError[]>;
  resetValidation: () => void;
}

export interface FormProps<DATA> {
  editing: boolean;
  initialValues: DATA;
  onSubmit: (values: DATA) => void;
  validationSchema?: Yup.Schema<DATA | undefined>;

  browserSubmit?: boolean;
  action?: string;
  method?: string;
}

/**
 * Error definition.
 */
export interface ValidationError {
  /**
   * Path to attribute in object.
   */
  key: string;

  /**
   * Label to show.
   */
  label: string;

  /**
   * Value from validation process.
   */
  value: string;
}
