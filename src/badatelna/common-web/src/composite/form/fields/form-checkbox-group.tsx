import { CheckboxGroup } from '../../../components/checkbox-group/checkbox-group';
import { formFieldFactory } from './form-field';
import { FormFieldProps } from './wrapper/form-field-wrapper-types';
import { DomainObject } from '../../../common/common-types';
import { CheckboxGroupProps } from 'components/checkbox-group/checkbox-group-types';

export const FormCheckboxGroup = formFieldFactory(CheckboxGroup) as <
  OPTION extends DomainObject
>(
  props: FormFieldProps<CheckboxGroupProps<OPTION>>
) => JSX.Element;
