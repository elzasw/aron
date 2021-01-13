import React, { useState, useMemo, useRef } from 'react';
import { stubTrue, noop } from 'lodash';
import { v4 as uuidv4 } from 'uuid';
import {
  TableFieldProps,
  TableFieldColumnState,
  TableFieldColumn,
} from './table-field-types';
import { TableFieldToolbar } from './table-field-toolbar';
import { TableFieldHeader } from './table-field-header';
import { TableFieldRow } from './table-field-row';
import { TableFieldDialog } from './table-field-dialog';
import { TableFieldRemoveDialog } from './table-field-remove-dialog';
import { useEventCallback } from 'utils/event-callback-hook';
import { TableFieldContext } from './table-field-context';
import { DialogHandle } from 'components/dialog/dialog-types';

export function useTableField<OBJECT>(options: TableFieldProps<OBJECT>) {
  const props: Required<TableFieldProps<OBJECT>> = {
    showToolbar: true,
    disabled: false,
    disabledAdd: false,
    disabledEdit: false,
    disabledRemove: false,
    visibleAdd: true,
    visibleEdit: true,
    visibleRemove: true,
    maxRows: 10,
    onSelect: noop,
    initNewRow: defaultInitNewRow,
    showDetailBtnCond: stubTrue,
    showRadioCond: stubTrue,
    ToolbarComponent: TableFieldToolbar,
    HeaderComponent: TableFieldHeader,
    RowComponent: TableFieldRow,
    DialogComponent: TableFieldDialog,
    RemoveDialogComponent: TableFieldRemoveDialog,
    FormFieldsComponent: Empty,
    ...options,
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
    setSelectedIndex(index);

    if (props.value != null) {
      props.onSelect(props.value[index], index);
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
      removeRow,
      saveRow,
      selectRow,
      setColumnWidth,
      showAddDialog,
      showDetailDialog,
      showEditDialog,
      showRemoveDialog,
      value,
    ]
  );

  return {
    props,
    context,
    selectedIndex,
    setSelectedIndex,
    formDialogRef,
    removeDialogRef,
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
