import { TableFieldProps } from 'components/table-field/table-field-types';

export interface InlineTableFieldProps<OBJECT>
  extends Omit<
    TableFieldProps<OBJECT>,
    'FormFieldsComponent' | 'visibleActionsColumn'
  > {
  withRemove?: boolean;
  initNewItem?: () => OBJECT;
}
