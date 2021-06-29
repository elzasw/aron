import { DomainObject, ListSource } from 'common/common-types';

export interface CheckboxGroupProps<OPTION extends DomainObject> {
  source: ListSource<OPTION>;
  idMapper?: (option: OPTION) => string;
  labelMapper?: (option: OPTION) => string;
  onChange: (value: Array<string | OPTION> | null) => void;
  disabled?: boolean;
  form?: string;
  value?: Array<string | OPTION> | null;
}
