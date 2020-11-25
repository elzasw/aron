import { useState, useRef, useMemo, ComponentType } from 'react';
import { useEventCallback } from 'utils/event-callback-hook';
import {
  TableColumnState,
  TableColumn,
  TableColumnAlign,
} from '../table-types';
import { DialogHandle } from 'components/dialog/dialog-types';
import { NumberCell } from '../cells/number-cell';
import { BooleanCell } from '../cells/boolean-cell';

export function useTableColumns<OBJECT>({
  columns: providedColumns,
}: {
  columns: TableColumn<OBJECT>[];
}) {
  const columns = useMemo(() => addDefaults(providedColumns), [
    providedColumns,
  ]);

  const [columnsState, setColumnsState] = useState<TableColumnState[]>(
    deriveColumnsState(columns)
  );

  const columnDialogRef = useRef<DialogHandle>(null);
  const openColumnDialog = useEventCallback(() =>
    columnDialogRef.current?.open()
  );
  const closeColumnDialog = useEventCallback(() =>
    columnDialogRef.current?.close()
  );

  const filteredColumns = useMemo(
    () =>
      columnsState
        .map((state) => {
          const column = columns.find(
            (column) => column.datakey === state.datakey
          );
          return column !== undefined ? { ...column, ...state } : undefined;
        })
        .filter((column) => column !== undefined)
        .map((column) => column!)
        .filter((column) => column.visible || column.visible === undefined),
    [columns, columnsState]
  );

  return {
    columnsState,
    filteredColumns,
    setColumnsState,
    columnDialogRef,
    openColumnDialog,
    closeColumnDialog,
  };
}

function addDefaults<OBJECT>(columns: TableColumn<OBJECT>[]) {
  return columns.map((column) => ({
    align: deriveAlign(column.CellComponent),
    ...column,
  }));
}

function deriveAlign<OBJECT>(CellComponent?: ComponentType<any>) {
  switch (CellComponent) {
    case BooleanCell:
      return TableColumnAlign.CENTER;
    case NumberCell:
      return TableColumnAlign.RIGHT;
    default:
      return TableColumnAlign.LEFT;
  }
}

/**
 * Derives default column states from supplied colums.
 */
function deriveColumnsState<OBJECT>(columns: TableColumn<OBJECT>[]) {
  return columns.map(
    ({ visible = true, width, datakey }) =>
      ({ visible, width, datakey } as TableColumnState)
  );
}
