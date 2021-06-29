import {
  useMemo,
  useRef,
  KeyboardEvent,
  Ref,
  useImperativeHandle,
  useContext,
} from 'react';
import { FixedSizeList } from 'react-window';
import InfiniteLoader from 'react-window-infinite-loader';
import { noop } from 'lodash';
import { useEventCallback } from 'utils/event-callback-hook';
import { useForceRender } from 'utils/force-render';
import { DomainObject } from 'common/common-types';
import { NamedSettingsContext } from 'common/settings/named/named-settings-context';
import { ExportDialog } from 'modules/export/components/dialog/export-dialog/export-dialog';
import { TableProps, TableHandle } from './table-types';
import { TableSearchbar } from './table-searchbar';
import { TableToolbar } from './table-toolbar';
import { TableContext, TableSelectedContext } from './table-context';
import { TableColumnDialog } from './table-column-dialog';
import { TableHeader } from './table-header';
import { TableRow } from './table-row';
import { useTableColumns } from './hooks/table-columns-hook';
import { useTableSort } from './hooks/table-sort-hook';
import { useTableSelect } from './hooks/table-select-hook';
import { useTableFilters } from './hooks/table-filters-hook';
import { TableFilterDialog } from './table-filter-dialog';
import { useTableSearch } from './hooks/table-search-hook';
import { useTableData } from './hooks/table-data-hook';
import { useTableExports } from './hooks/table-export-hook';

export function useTable<OBJECT extends DomainObject>(
  options: TableProps<OBJECT>,
  ref: Ref<TableHandle<OBJECT>>
) {
  const namedSettingsContext = useContext(NamedSettingsContext);

  const props: Required<TableProps<OBJECT>> = {
    tableId: '',
    version: 0,
    height: 10 * 30,
    disabled: false,
    tableName: null,
    defaultSorts: useMemo(
      () => [{ field: 'id', datakey: 'id', order: 'ASC', type: 'FIELD' }],
      []
    ),
    defaultPreFilters: [],
    showRefreshButton: true,
    showColumnButton: true,
    showFilterButton: true,
    showNamedSettingsButton: namedSettingsContext.defaultTableNamedSettings,
    showBulkActionButton: true,
    showReportButton: true,
    showSelectBox: true,
    showSearchbar: true,
    showResetSortsButton: true,
    SearchbarComponent: TableSearchbar,
    ToolbarComponent: TableToolbar,
    HeaderComponent: TableHeader,
    RowComponent: TableRow,
    ColumnDialogComponent: TableColumnDialog,
    FilterDialogComponent: TableFilterDialog,
    ExportDialogComponent: ExportDialog,
    bulkActions: useMemo(() => [], []),
    reportTag: null,
    include: useMemo(() => [], []),
    onActiveChange: noop,
    toolbarProps: {},
    ...options,
  };

  const loaderRef = useRef<InfiniteLoader>(null);
  const listRef = useRef<FixedSizeList>(null);

  const { forceRender } = useForceRender();

  const {
    columnsState,
    defaultColumnsState,
    filteredColumns,
    setColumnsState,
    columnDialogRef,
    openColumnDialog,
    closeColumnDialog,
  } = useTableColumns({
    tableId: props.tableId,
    version: props.version,
    columns: props.columns,
  });

  const {
    filterDialogRef,
    openFilterDialog,
    closeFilterDialog,
    filters,
    filtersState,
    setFiltersState,
    preFilters,
    setPreFilters,
  } = useTableFilters({
    tableId: props.tableId,
    version: props.version,
    columns: props.columns,
    initPreFilters: props.defaultPreFilters,
  });

  const {
    exportDialogRef,
    openExportDialog,
    closeExportDialog,
  } = useTableExports();

  const { sorts, toggleSortColumn, resetSorts, setSorts } = useTableSort({
    tableId: props.tableId,
    version: props.version,
    columns: props.columns,
    defaultSorts: props.defaultSorts,
  });

  const { searchQuery, setSearchQuery } = useTableSearch({
    tableId: props.tableId,
    version: props.version,
  });

  const {
    selected,
    activeRow,
    toggleAllRowSelection,
    toggleRowSelection,
    resetSelection,
    setActiveRow,
    setNextRowActive,
    setPrevRowActive,
  } = useTableSelect({
    source: props.source,
    listRef,
    onActiveChange: props.onActiveChange,
  });

  useTableData({
    source: props.source,
    searchQuery,
    sorts,
    preFilters,
    filters,
    filtersState,
    loaderRef,
    include: props.include,
  });

  /**
   * Refreshes table source and infinite loader cache.
   */
  const refresh = useEventCallback(() => {
    props.source.reset();
    forceRender();

    requestAnimationFrame(() => {
      loaderRef.current?.resetloadMoreItemsCache(true);
    });
  });

  const handleKeyNavigation = useEventCallback(
    (e: KeyboardEvent<HTMLDivElement>) => {
      if (e.key === 'ArrowDown') {
        e.preventDefault(); // prevent default to prevent unwanted scrolling
        setNextRowActive();
      }

      if (e.key === 'ArrowUp') {
        e.preventDefault(); // prevent default to prevent unwanted scrolling
        setPrevRowActive();
      }
    }
  );

  const context: TableContext<OBJECT> = useMemo(
    () => ({
      columns: props.columns,
      source: props.source,
      bulkActions: props.bulkActions,
      reportTag: props.reportTag,
      columnsState,
      disabled: props.disabled,
      disabledRefreshButton: false,
      disabledColumnButton: false,
      disabledFilterButton: false,
      disabledBulkActionButton: false,
      disabledReportButton: false,
      disabledResetSortsButton: false,
      disabledNamedSettingsButton: false,
      showRefreshButton: props.showRefreshButton,
      showColumnButton: props.showColumnButton,
      showFilterButton: props.showFilterButton,
      showNamedSettingsButton: props.showNamedSettingsButton,
      showBulkActionButton: props.showBulkActionButton,
      showReportButton: props.showReportButton,
      showSelectBox: props.showSelectBox,
      showResetSortsButton: props.showResetSortsButton,
      tableName: props.tableName,
      totalCount: props.source.count,
      loadedCount: props.source.items.length,
      filteredColumns,
      searchQuery,
      sorts,
      filters,
      filtersState,
      preFilters,
      setPreFilters,
      toggleAllRowSelection,
      toggleRowSelection,
      resetSelection,
      activeRow,
      setActiveRow,
      setPrevRowActive,
      setNextRowActive,
      refresh,
      setColumnsState,
      defaultColumnsState,
      openColumnDialog,
      closeColumnDialog,
      openFilterDialog,
      closeFilterDialog,
      openExportDialog,
      closeExportDialog,
      toggleSortColumn,
      setSearchQuery,
      setFiltersState,
      resetSorts,
      setSorts,
    }),
    [
      props.columns,
      props.source,
      props.bulkActions,
      props.reportTag,
      props.disabled,
      props.showRefreshButton,
      props.showColumnButton,
      props.showFilterButton,
      props.showNamedSettingsButton,
      props.showBulkActionButton,
      props.showReportButton,
      props.showSelectBox,
      props.showResetSortsButton,
      props.tableName,
      columnsState,
      filteredColumns,
      searchQuery,
      sorts,
      filters,
      filtersState,
      preFilters,
      setPreFilters,
      toggleAllRowSelection,
      toggleRowSelection,
      resetSelection,
      activeRow,
      setActiveRow,
      setPrevRowActive,
      setNextRowActive,
      refresh,
      setColumnsState,
      defaultColumnsState,
      openColumnDialog,
      closeColumnDialog,
      openFilterDialog,
      closeFilterDialog,
      openExportDialog,
      closeExportDialog,
      toggleSortColumn,
      setSearchQuery,
      setFiltersState,
      resetSorts,
      setSorts,
    ]
  );

  useImperativeHandle(ref, () => context, [context]);

  const selectedContext: TableSelectedContext = useMemo(
    () => ({
      selected,
      activeRow,
    }),
    [selected, activeRow]
  );

  return {
    props,
    context,
    selectedContext,
    columnDialogRef,
    loaderRef,
    listRef,
    filterDialogRef,
    exportDialogRef,
    handleKeyNavigation,
  };
}
