import { DomainObject } from 'common/common-types';
import { FormFieldProps } from './wrapper/form-field-wrapper-types';
import { formFieldFactory } from './form-field';
import { RadioGroup } from 'components/radio-group/radio-group';
import { RadioGroupProps } from 'components/radio-group/radio-group-types';

export const FormRadioGroup = formFieldFactory(RadioGroup) as <
  OPTION extends DomainObject
>(
  props: FormFieldProps<RadioGroupProps<OPTION>>
) => JSX.Element;
