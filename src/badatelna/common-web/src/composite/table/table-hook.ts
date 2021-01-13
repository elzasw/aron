import {
  useMemo,
  useRef,
  KeyboardEvent,
  Ref,
  useImperativeHandle,
} from 'react';
import { noop } from 'lodash';
import InfiniteLoader from 'react-window-infinite-loader';
import { useEventCallback } from 'utils/event-callback-hook';
import { DomainObject } from 'common/common-types';
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
import { FixedSizeList } from 'react-window';
import { useForceRender } from 'utils/force-render';
import { useTableReports } from './hooks/table-report-hook';
import { ReportDialog } from 'composite/report-dialog/report-dialog';

export function useTable<OBJECT extends DomainObject>(
  options: TableProps<OBJECT>,
  ref: Ref<TableHandle<OBJECT>>
) {
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
    showRefreshButton: true,
    showColumnButton: true,
    showFilterButton: true,
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
    ReportDialogComponent: ReportDialog,
    bulkActions: useMemo(() => [], []),
    reportTag: null,
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
  } = useTableFilters({
    tableId: props.tableId,
    version: props.version,
    columns: props.columns,
  });

  const {
    reportDialogRef,
    openReportDialog,
    closeReportDialog,
  } = useTableReports();

  const { sorts, toggleSortColumn, resetSorts } = useTableSort({
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
    filters,
    filtersState,
    loaderRef,
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
      showRefreshButton: props.showRefreshButton,
      showColumnButton: props.showColumnButton,
      showFilterButton: props.showFilterButton,
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
      openReportDialog,
      closeReportDialog,
      toggleSortColumn,
      setSearchQuery,
      setFiltersState,
      resetSorts,
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
      openReportDialog,
      closeReportDialog,
      toggleSortColumn,
      setSearchQuery,
      setFiltersState,
      resetSorts,
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
    reportDialogRef,
    handleKeyNavigation,
  };
}
