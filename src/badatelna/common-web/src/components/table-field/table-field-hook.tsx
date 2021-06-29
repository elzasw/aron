import React, { useState, useMemo, useRef } from 'react';
import { stubTrue, noop, compact, get, set } from 'lodash';
import { v4 as uuidv4 } from 'uuid';
import {
  TableFieldProps,
  TableFieldColumnState,
  TableFieldColumn,
  TableFieldFormFieldsProps,
} from './table-field-types';
import { TableFieldToolbar } from './table-field-toolbar';
import { TableFieldHeader } from './table-field-header';
import { TableFieldRow } from './table-field-row';
import { TableFieldDialog } from './table-field-dialog';
import { TableFieldRemoveDialog } from './table-field-remove-dialog';
import { useEventCallback } from 'utils/event-callback-hook';
import { TableFieldContext } from './table-field-context';
import { DialogHandle } from 'components/dialog/dialog-types';
import { arrayMove } from 'utils/array-move';
import { useIntl, FormattedMessage } from 'react-intl';
import { FormNumberField } from 'composite/form/fields/form-number-field';

export function useTableField<OBJECT>(options: TableFieldProps<OBJECT>) {
  const intl = useIntl();

  const props: Required<TableFieldProps<OBJECT>> = {
    showToolbar: true,
    disabled: false,
    disabledAdd: false,
    disabledEdit: false,
    disabledRemove: false,
    visibleAdd: true,
    visibleEdit: true,
    visibleRemove: true,
    visibleActionsColumn: true,
    useDnDOrdering: false,
    useOrderColumn: false,
    orderColumnPath: 'order',
    maxRows: 10,
    noMinHeigth: false,
    onSelect: noop,
    initNewRow: defaultInitNewRow,
    showDetailBtnCond: stubTrue,
    showRadioCond: stubTrue,
    ToolbarComponent: TableFieldToolbar,
    HeaderComponent: TableFieldHeader,
    RowComponent: TableFieldRow,
    DialogComponent: TableFieldDialog,
    RemoveDialogComponent: TableFieldRemoveDialog,
    ...options,
    columns: useMemo(
      () =>
        compact([
          options.useOrderColumn
            ? {
                name: intl.formatMessage({
                  id: 'EAS_TABLE_FIELD_COLUMN_ORDER',
                  defaultMessage: 'Pořadí',
                }),
                datakey: options.orderColumnPath ?? 'order',
                width: 100,
                visible: true,
              }
            : undefined,
          ...options.columns,
        ]) as TableFieldColumn<OBJECT>[],
      [intl, options.columns, options.orderColumnPath, options.useOrderColumn]
    ),
    FormFieldsComponent: useMemo(
      () =>
        function FormFieldsComponent(props: TableFieldFormFieldsProps<OBJECT>) {
          return (
            <>
              {options.useOrderColumn && (
                <FormNumberField
                  name={options.orderColumnPath ?? 'order'}
                  disabled
                  label={
                    <FormattedMessage
                      id="EAS_TABLE_FIELD_FIELD_LABEL_ORDER"
                      defaultMessage="Pořadí"
                    />
                  }
                  helpLabel={intl.formatMessage({
                    id: 'EAS_TABLE_FIELD_FIELD_HELP_ORDER',
                    defaultMessage: ' ',
                  })}
                />
              )}
              {options.FormFieldsComponent && (
                <options.FormFieldsComponent {...props} />
              )}
            </>
          );
        },
      [
        intl,
        options.FormFieldsComponent,
        options.orderColumnPath,
        options.useOrderColumn,
      ]
    ),
  };

  const value = props.value ?? [];

  const [selectedIndex, setSelectedIndex] = useState<number | undefined>();
  const [columnsState, setColumnsState] = useState<TableFieldColumnState[]>(
    deriveColumnsState(props.columns)
  );

  const formDialogRef = useRef<DialogHandle>(null);
  const removeDialogRef = useRef<DialogHandle>(null);

  const showAddDialog = useEventCallback(() => {
    setSelectedIndex(undefined);
    formDialogRef.current?.open();
  });

  const showEditDialog = useEventCallback((index: number) => {
    setSelectedIndex(index);

    formDialogRef.current?.open();
  });

  const showRemoveDialog = useEventCallback((index: number) => {
    setSelectedIndex(index);

    removeDialogRef.current?.open();
  });

  const showDetailDialog = useEventCallback((index: number) => {
    setSelectedIndex(index);

    formDialogRef.current?.open();
  });

  const saveRow = useEventCallback(
    (index: number | undefined, object: OBJECT) => {
      let newValue: OBJECT[];
      if (index !== undefined) {
        newValue = [
          ...value.slice(undefined, index),
          object,
          ...value.slice(index + 1, undefined),
        ];
      } else {
        newValue = [...value, object];
      }

      props.onChange(newValue);
    }
  );

  const removeRow = useEventCallback((index: number) => {
    const newValue = value.filter((_, i) => i !== index);
    props.onChange(newValue);
    setSelectedIndex(undefined);
  });

  const selectRow = useEventCallback((index: number) => {
    if (index !== selectedIndex) {
      setSelectedIndex(index);

      if (props.value != null) {
        props.onSelect(props.value[index], index);
      }
    } else {
      setSelectedIndex(undefined);

      props.onSelect(null, index);
    }
  });

  const setColumnWidth = useEventCallback((datakey: string, width: number) => {
    setColumnsState((columnsState) => {
      const index = columnsState.findIndex(
        (column) => column.datakey === datakey
      );

      if (index !== -1) {
        return [
          ...columnsState.slice(0, index),
          {
            ...columnsState[index],
            width: Math.max(props.columns[index].minWidth ?? 50, width),
          },
          ...columnsState.slice(index + 1, undefined),
        ];
      } else {
        return columnsState;
      }
    });
  });

  /**
   * Swaps rows positions and optionaly also order attribute.
   */
  const swapRows = useEventCallback(
    ({ oldIndex, newIndex }: { oldIndex: number; newIndex: number }) => {
      const newValue = arrayMove(value, oldIndex, newIndex);

      if (props.useOrderColumn) {
        const oldRow = { ...newValue[oldIndex] };
        const newRow = { ...newValue[newIndex] };
        newValue[oldIndex] = oldRow;
        newValue[newIndex] = newRow;

        const oldRowNum = get(oldRow, props.orderColumnPath);
        const newRowNum = get(newRow, props.orderColumnPath);
        set(oldRow as any, props.orderColumnPath, newRowNum);
        set(newRow as any, props.orderColumnPath, oldRowNum);
      }

      props.onChange(newValue);
    }
  );

  const filteredColumns = columnsState
    .map((state) => {
      const column = props.columns.find(
        (column) => column.datakey === state.datakey
      );
      return column !== undefined ? { ...column, ...state } : undefined;
    })
    .filter((column) => column !== undefined)
    .map((column) => column!)
    .filter((column) => column.visible || column.visible === undefined);

  const context: TableFieldContext<OBJECT> = useMemo(
    () => ({
      disabled: props.disabled,
      columns: props.columns,
      value,
      disabledAdd: props.disabledAdd,
      disabledEdit: props.disabledEdit,
      disabledRemove: props.disabledRemove,
      visibleAdd: props.visibleAdd,
      visibleEdit: props.visibleEdit,
      visibleRemove: props.visibleRemove,
      visibleActionsColumn: props.visibleActionsColumn,
      onSelect: props.onSelect,
      columnsState,
      filteredColumns,
      showAddDialog,
      showEditDialog,
      showRemoveDialog,
      showDetailDialog,
      saveRow,
      removeRow,
      selectRow,
      setColumnWidth,
      showDetailBtnCond: props.showDetailBtnCond,
      showRadioCond: props.showRadioCond,
      initNewRow: props.initNewRow,
      useDnDOrdering: props.useDnDOrdering,
    }),
    [
      columnsState,
      filteredColumns,
      props.columns,
      props.disabled,
      props.disabledAdd,
      props.disabledEdit,
      props.disabledRemove,
      props.initNewRow,
      props.onSelect,
      props.showDetailBtnCond,
      props.showRadioCond,
      props.visibleAdd,
      props.visibleEdit,
      props.visibleRemove,
      props.visibleActionsColumn,
      removeRow,
      saveRow,
      selectRow,
      setColumnWidth,
      showAddDialog,
      showDetailDialog,
      showEditDialog,
      showRemoveDialog,
      value,
      props.useDnDOrdering,
    ]
  );

  return {
    props,
    context,
    selectedIndex,
    setSelectedIndex,
    formDialogRef,
    removeDialogRef,
    swapRows,
  };
}

export function Empty() {
  return <></>;
}

function defaultInitNewRow(): any {
  return {
    id: uuidv4(),
  };
}

function deriveColumnsState<TObject>(
  columns: TableFieldColumn<TObject>[]
): TableFieldColumnState[] {
  return columns.map((column) => ({
    datakey: column.datakey,
    width: column.width,
  }));
}
