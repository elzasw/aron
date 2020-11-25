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

export function useTable<OBJECT extends DomainObject>(
  options: TableProps<OBJECT>,
  ref: Ref<TableHandle<OBJECT>>
) {
  const props: Required<TableProps<OBJECT>> = {
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
    showSelectBox: true,
    SearchbarComponent: TableSearchbar,
    ToolbarComponent: TableToolbar,
    HeaderComponent: TableHeader,
    RowComponent: TableRow,
    ColumnDialogComponent: TableColumnDialog,
    FilterDialogComponent: TableFilterDialog,
    bulkActions: useMemo(() => [], []),
    onActiveChange: noop,
    toolbarProps: {},
    ...options,
  };

  const loaderRef = useRef<InfiniteLoader>(null);
  const listRef = useRef<FixedSizeList>(null);

  const { forceRender } = useForceRender();

  const {
    columnsState,
    filteredColumns,
    setColumnsState,
    columnDialogRef,
    openColumnDialog,
    closeColumnDialog,
  } = useTableColumns({ columns: props.columns });

  const {
    filterDialogRef,
    openFilterDialog,
    closeFilterDialog,
    filters,
    filtersState,
    setFiltersState,
  } = useTableFilters({ columns: props.columns });

  const { sorts, toggleSortColumn } = useTableSort({
    columns: props.columns,
    defaultSorts: props.defaultSorts,
  });

  const { searchQuery, setSearchQuery } = useTableSearch();

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
      columnsState,
      disabled: props.disabled,
      disabledRefreshButton: false,
      disabledColumnButton: false,
      disabledFilterButton: false,
      disabledBulkActionButton: false,
      showRefreshButton: props.showRefreshButton,
      showColumnButton: props.showColumnButton,
      showFilterButton: props.showFilterButton,
      showBulkActionButton: props.showBulkActionButton,
      showSelectBox: props.showSelectBox,
      tableName: props.tableName,
      totalCount: props.source.count,
      loadedCount: props.source.items.length,
      filteredColumns,
      sorts,
      filters,
      filtersState,
      toggleAllRowSelection,
      toggleRowSelection,
      resetSelection,
      setActiveRow,
      setPrevRowActive,
      setNextRowActive,
      refresh,
      setColumnsState,
      openColumnDialog,
      closeColumnDialog,
      openFilterDialog,
      closeFilterDialog,
      toggleSortColumn,
      setSearchQuery,
      setFiltersState,
    }),
    [
      props.columns,
      props.source,
      props.bulkActions,
      props.disabled,
      props.showRefreshButton,
      props.showColumnButton,
      props.showFilterButton,
      props.showBulkActionButton,
      props.showSelectBox,
      props.tableName,
      columnsState,
      filteredColumns,
      sorts,
      filters,
      filtersState,
      toggleAllRowSelection,
      toggleRowSelection,
      resetSelection,
      setActiveRow,
      setPrevRowActive,
      setNextRowActive,
      refresh,
      setColumnsState,
      openColumnDialog,
      closeColumnDialog,
      openFilterDialog,
      closeFilterDialog,
      toggleSortColumn,
      setSearchQuery,
      setFiltersState,
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
    handleKeyNavigation,
  };
}
