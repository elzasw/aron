import { TableField } from 'components/table-field/table-field';
import { TableFieldProps } from 'components/table-field/table-field-types';
import { FormFieldProps } from './wrapper/form-field-wrapper-types';
import { formFieldFactory } from './form-field';

export const FormTableField = formFieldFactory(TableField) as <OBJECT>(
  props: FormFieldProps<TableFieldProps<OBJECT>>
) => JSX.Element;
