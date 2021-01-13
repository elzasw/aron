import { useState, useRef, useMemo, ComponentType, useContext } from 'react';
import { UserSettingsContext } from 'common/settings/user/user-settings-context';
import { TableSettings } from 'common/settings/user/user-settings-types';
import { DialogHandle } from 'components/dialog/dialog-types';
import { useEventCallback } from 'utils/event-callback-hook';
import { useUpdateEffect } from 'utils/update-effect';
import {
  TableColumnState,
  TableColumn,
  TableColumnAlign,
} from '../table-types';
import { NumberCell } from '../cells/number-cell';
import { BooleanCell } from '../cells/boolean-cell';

export function useTableColumns<OBJECT>({
  tableId,
  version,
  columns: providedColumns,
}: {
  tableId: string;
  version: number;
  columns: TableColumn<OBJECT>[];
}) {
  const columns = useMemo(() => addDefaults(providedColumns), [
    providedColumns,
  ]);

  const { getTableSettings, setTableSettings } = useContext(
    UserSettingsContext
  );

  let settings: TableSettings | undefined;
  const defaultColumnsState = deriveColumnsState(columns);
  let initColumnsState: TableColumnState[] = defaultColumnsState;

  if (tableId !== '') {
    settings = getTableSettings(tableId, version);

    if (settings?.columnsState !== undefined) {
      initColumnsState = settings?.columnsState;
    }
  }

  const [columnsState, setColumnsState] = useState<TableColumnState[]>(
    initColumnsState
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

  /**
   * Updates user settings.
   */
  useUpdateEffect(() => {
    const newSettings: TableSettings = {
      ...(settings ?? {}),
      columnsState,
      version,
    };
    setTableSettings(tableId, newSettings);
  }, [columnsState]);

  return {
    columnsState,
    defaultColumnsState,
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
