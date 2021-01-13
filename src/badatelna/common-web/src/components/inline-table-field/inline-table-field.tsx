import React, {
  forwardRef,
  RefAttributes,
  ReactElement,
  Ref,
  useMemo,
} from 'react';
import { stubFalse } from 'lodash';
import { TableField } from 'components/table-field/table-field';
import { InlineTableFieldProps } from './inline-table-field-types';
import { TableFieldHandle } from 'components/table-field/table-field-types';
import { InlineTableFieldToolbar } from './inline-table-field-toolbar';
import { InlineTableFieldRow } from './inline-table-field-row';
import { InlineTableFieldContext } from './inline-table-context';

export const InlineTableField = forwardRef(function InlineTableField<OBJECT>(
  {
    ToolbarComponent = InlineTableFieldToolbar,
    RowComponent = InlineTableFieldRow,
    showRadioCond = stubFalse,
    showDetailBtnCond = stubFalse,
    withRemove = true,
    initNewItem = emptyObject,
    ...props
  }: InlineTableFieldProps<OBJECT>,
  ref: Ref<TableFieldHandle>
) {
  const context: InlineTableFieldContext<OBJECT> = useMemo(
    () => ({ withRemove, initNewItem }),
    [withRemove, initNewItem]
  );
  return (
    <InlineTableFieldContext.Provider value={context}>
      <TableField
        ToolbarComponent={ToolbarComponent}
        RowComponent={RowComponent}
        showRadioCond={showRadioCond}
        showDetailBtnCond={showDetailBtnCond}
        {...props}
        ref={ref}
      />
    </InlineTableFieldContext.Provider>
  );
}) as <OBJECT>(
  p: InlineTableFieldProps<OBJECT> & RefAttributes<TableFieldHandle>
) => ReactElement;

function emptyObject(): any {
  return {};
}
