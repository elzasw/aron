import { useRef, ComponentType, useMemo, useState, useContext } from 'react';
import { UserSettingsContext } from 'common/settings/user/user-settings-context';
import { TableSettings } from 'common/settings/user/user-settings-types';
import { DialogHandle } from 'components/dialog/dialog-types';
import { useEventCallback } from 'utils/event-callback-hook';
import { useUpdateEffect } from 'utils/update-effect';
import {
  TableFilter,
  TableColumn,
  TableFilterOperation,
  TableFilterState,
} from '../table-types';
import { TextCell } from '../cells/text-cell';
import { NumberCell } from '../cells/number-cell';
import { DateCell } from '../cells/date-cell';
import { DateTimeCell } from '../cells/date-time-cell';
import { BooleanCell } from '../cells/boolean-cell';
import { TimeCell } from '../cells/time-cell';
import { TableFilterCells } from '../table-filter-cells';

export function useTableFilters<OBJECT>({
  tableId,
  version,
  columns,
}: {
  tableId: string;
  version: number;
  columns: TableColumn<OBJECT>[];
}) {
  /**
   * Filters definition.
   */
  const filters = useMemo(() => deriveFilters(columns), [columns]);

  const { getTableSettings, setTableSettings } = useContext(
    UserSettingsContext
  );

  let settings: TableSettings | undefined;
  let initFiltersState: TableFilterState[] = deriveFiltersState(filters);

  if (tableId !== '') {
    settings = getTableSettings(tableId, version);

    if (settings?.filtersState !== undefined) {
      initFiltersState = settings?.filtersState;
    }
  }

  /**
   * Filters state.
   */
  const [filtersState, setFiltersState] = useState<TableFilterState[]>(
    initFiltersState
  );

  const filterDialogRef = useRef<DialogHandle>(null);
  const openFilterDialog = useEventCallback(() =>
    filterDialogRef.current?.open()
  );
  const closeFilterDialog = useEventCallback(() =>
    filterDialogRef.current?.close()
  );

  /**
   * Updates user settings.
   */
  useUpdateEffect(() => {
    const newSettings: TableSettings = {
      ...(settings ?? {}),
      filtersState,
      version,
    };
    setTableSettings(tableId, newSettings);
  }, [filtersState]);

  return {
    filterDialogRef,
    openFilterDialog,
    closeFilterDialog,
    filters,
    filtersState,
    setFiltersState,
  };
}

/**
 * Derives default filters from supplied colums.
 */
function deriveFilters<OBJECT>(columns: TableColumn<OBJECT>[]) {
  return columns
    .filter(({ filterable = false }) => filterable)
    .map(
      ({
        name,
        datakey,
        filterkey,
        CellComponent,
        FilterComponent,
        filterOperation,
      }) =>
        ({
          enabled: false,
          label: name,
          operation: filterOperation ?? deriveOperation(CellComponent),
          filterkey: filterkey || datakey,
          FilterComponent: FilterComponent ?? deriveComponent(CellComponent),
          value: '',
        } as TableFilter)
    );
}

function deriveOperation(CellComponent?: ComponentType<any>) {
  switch (CellComponent) {
    case BooleanCell:
      return TableFilterOperation.EQ;
    case NumberCell:
      return TableFilterOperation.AND;
    case TextCell:
      return TableFilterOperation.CONTAINS;
    case DateCell:
      return TableFilterOperation.AND;
    case DateTimeCell:
      return TableFilterOperation.AND;
    default:
      return TableFilterOperation.NOT_NULL;
  }
}

function deriveComponent(CellComponent?: ComponentType<any>) {
  switch (CellComponent) {
    case BooleanCell:
      return TableFilterCells.FilterBooleanCell;
    case DateCell:
      return TableFilterCells.FilterDateCell;
    case DateTimeCell:
      return TableFilterCells.FilterDateTimeCell;
    case TimeCell:
      return TableFilterCells.FilterTimeCell;
    case NumberCell:
      return TableFilterCells.FilterNumberCell;
    case TextCell:
    default:
      return TableFilterCells.FilterTextCell;
  }
}

/**
 * Derives filter states from supplied/constructed filters.
 */
function deriveFiltersState(filters: TableFilter[]) {
  return filters.map(
    (filter) =>
      ({
        enabled: false,
        value: null,
        operation: filter.operation,
      } as TableFilterState)
  );
}
