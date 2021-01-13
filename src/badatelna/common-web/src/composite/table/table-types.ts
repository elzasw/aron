import { ComponentType, ReactNode, RefAttributes } from 'react';
import { DialogHandle } from 'components/dialog/dialog-types';
import { ScrollableSource, Filter } from 'common/common-types';
import { BulkAction } from './bulk-action-menu/bulk-action-types';
import { ReportDialogProps } from 'composite/report-dialog/report-dialog-types';

export interface TableToolbarProps {
  before?: ReactNode;
  after?: ReactNode;
}

export enum TableColumnAlign {
  LEFT = 'LEFT',
  RIGHT = 'RIGHT',
  CENTER = 'CENTER',
}

export interface TableCellProps<OBJECT, FIELD = any> {
  /**
   * Column in which this cell is rendered.
   */
  column: TableColumn<OBJECT>;

  /**
   * Data from RowData for this cell.
   */
  value?: FIELD;

  /**
   * All data of row.
   */
  rowValue: OBJECT;

  /**
   * Row number.
   */
  index: number;
}

export interface FilterComponentProps {
  /**
   * Disabled flag.
   */
  disabled: boolean;

  /**
   * Value from filter for simple use.
   */
  value: any;

  /**
   * OnChange handler for value.
   */
  onChange: (value: any) => void;

  /**
   * Current filter.
   */
  filter: TableFilter;

  /**
   * Current filter.
   */
  state: TableFilterState;

  /**
   * OnChange handler for complex usage.
   */
  onChangeState: (state: TableFilterState) => void;
}

export type TableColumnValueMapper<OBJECT, FIELD = any> = (args: {
  column: TableColumn<OBJECT, FIELD>;
  rowValue: OBJECT;
  value: FIELD;
}) => FIELD;

export interface TableColumn<OBJECT, FIELD = any> {
  /**
   * Label of the column shown in header component.
   */
  name: ReactNode;

  /**
   * Attribute of the RowData object.
   */
  datakey: string;

  /**
   * Sort key which will be used in params building.
   *
   * Defaults to datakey.
   */
  sortkey?: string;

  /**
   * Represents a sorting specification.
   *
   * FIELD: Sort search results
   * GEO_DISTANCE: Geo Distance Sorting
   * SCRIPT: Script Based Sorting
   * SCORE: Track Scores
   */
  sortType?: 'FIELD' | 'GEO_DISTANCE' | 'SCRIPT' | 'SCORE';

  /**
   * Filter key which will be used in params building.
   *
   * Defaults to datakey.
   */
  filterkey?: string;

  /**
   * Width of the column.
   */
  width: number;

  /**
   * Minimum width of the column.
   *
   * Defaults to 50px.
   */
  minWidth?: number;

  /**
   * Is the column visible.
   */
  visible?: boolean;

  /**
   * Is the column sortable.
   *
   * Default is false.
   */
  sortable?: boolean;

  /**
   * Is the column filterable.
   *
   * Default is false.
   */
  filterable?: boolean;

  /**
   * Is the column fixed. Can not be hidden.
   *
   * Default is false.
   */
  fixed?: boolean;

  /**
   * React component for filtering.
   */
  FilterComponent?: ComponentType<FilterComponentProps>;

  /**
   * Filter operation for this column.
   */
  filterOperation?: TableFilterOperation;

  /**
   * Cell component.
   *
   */
  CellComponent: ComponentType<TableCellProps<OBJECT, FIELD>>;

  /**
   * Align of the cell text and header.
   *
   * Defaults to:
   * * NUMBER - right
   * * BOOLEAN - center
   * * others - left
   */
  align?: TableColumnAlign;

  /**
   * Mapper function from row data to cell data.
   *
   * Defaults to ```({rowValue, column}) => rowValue[column.datakey]```
   */
  valueMapper?: TableColumnValueMapper<OBJECT, FIELD>;
}

export interface TableColumnState {
  /**
   * Attribute of the RowData object.
   */
  datakey: string;

  /**
   * Width of the column.
   */
  width: number;

  /**
   * Visibility flag.
   */
  visible: boolean;
}

/**
 * Actionbar button properties.
 */
export interface TableToolbarButtonProps {
  /**
   * Label.
   *
   * Can be string or rendered component.
   */
  label: string | ReactNode;

  /**
   * OnClick handler.
   */
  onClick: () => void;

  endIcon?: ReactNode;

  /**
   * Show the icon as clicked.
   */
  checked?: boolean;

  disabled?: boolean;

  tooltip?: ReactNode;

  /**
   * Use primary color for the button.
   *
   * Default: false
   */
  primary?: boolean;

  /**
   * Use secondary color for the button.
   *
   * Default: false
   */
  secondary?: boolean;
}

export interface TableRowProps<OBJECT> {
  /**
   * Row data.
   */
  value: OBJECT;

  /**
   * Row number, starting from 0.
   */
  index: number;

  /**
   * Selected flag.
   */
  selected: boolean;
}

export interface TableProps<OBJECT> {
  /**
   * Table ID used for settings storage.
   */
  tableId?: string;

  /**
   * Table version used for settings storage.
   */
  version?: number;

  /**
   * External data source.
   */
  source: ScrollableSource<OBJECT>;

  /**
   * Columns of the table.
   */
  columns: TableColumn<OBJECT>[];

  /**
   * Sets the component to read-only mode.
   */
  disabled?: boolean;

  /**
   * Custom searchbar component.
   */
  SearchbarComponent?: ComponentType;

  /**
   * Custom actionbar bar component.
   */
  ToolbarComponent?: ComponentType<TableToolbarProps>;

  /**
   * Custom column dialog component.
   */
  ColumnDialogComponent?: ComponentType<RefAttributes<DialogHandle>>;

  /**
   * Custom report dialog component.
   */
  ReportDialogComponent?: ComponentType<
    ReportDialogProps & RefAttributes<DialogHandle>
  >;

  /**
   * Custom filter dialog component.
   */
  FilterDialogComponent?: ComponentType<RefAttributes<DialogHandle>>;

  /**
   * Custom header component.
   */
  HeaderComponent?: ComponentType<RefAttributes<HTMLDivElement>>;

  /**
   * Custom row component.
   *
   * Defaults to component rendering one cell for every column + radio for row selection.
   */
  RowComponent?: ComponentType<TableRowProps<OBJECT>>;

  /**
   * Default sorting.
   */
  defaultSorts?: TableSort[];

  /**
   * Name of the table to show.
   */
  tableName?: ReactNode | null;

  /**
   * Show refresh button.
   */
  showRefreshButton?: boolean;

  /**
   * Shows column button.
   */
  showColumnButton?: boolean;

  /**
   * Show filter button.
   */
  showFilterButton?: boolean;

  /**
   * Show bulk action button.
   */
  showBulkActionButton?: boolean;

  /**
   * Show report button.
   */
  showReportButton?: boolean;

  /**
   * Show reset sorts button.
   */
  showResetSortsButton?: boolean;

  /**
   * Show checkbox next to every row and in header.
   */
  showSelectBox?: boolean;

  /**
   * Show searchbar component.
   */
  showSearchbar?: boolean;

  /**
   * Bulk actions.
   */
  bulkActions?: BulkAction<any>[];

  /**
   * Report tag for location.
   */
  reportTag?: string | null;

  height?: number;

  onActiveChange?: (id: string | null) => void;

  toolbarProps?: TableToolbarProps;
}

export interface TableHandle<OBJECT> {
  /**
   * Name of the table to show.
   */
  tableName: ReactNode | null;

  source: ScrollableSource<OBJECT>;

  /**
   * Columns of the table.
   */
  columns: TableColumn<OBJECT>[];

  /**
   * Internal columns states.
   */
  columnsState: TableColumnState[];

  /**
   * Default columns states.
   */
  defaultColumnsState: TableColumnState[];

  /**
   * Table is in read-only mode.
   */
  disabled: boolean;

  /**
   * Disable refresh button.
   */
  disabledRefreshButton: boolean;

  /**
   * Disable column button.
   */
  disabledColumnButton: boolean;

  /**
   * Disable filter button.
   */
  disabledFilterButton: boolean;

  /**
   * Disable bulk action button.
   */
  disabledBulkActionButton: boolean;

  /**
   * Disable reset sorts button.
   */
  disabledResetSortsButton: boolean;

  /**
   * Disable report action button.
   */
  disabledReportButton: boolean;

  /**
   * Show refresh button.
   */
  showRefreshButton: boolean;

  /**
   * Shows column button.
   */
  showColumnButton: boolean;

  /**
   * Show filter button.
   */
  showFilterButton: boolean;

  /**
   * Show report button.
   */
  showReportButton: boolean;

  /**
   * Show bulk action button.
   */
  showBulkActionButton: boolean;

  /**
   * Show reset sorts action button.
   */
  showResetSortsButton: boolean;

  /**
   * Show checkbox next to every row and in header.
   */
  showSelectBox: boolean;

  /**
   * Bulk actions.
   */
  bulkActions: BulkAction<any>[];

  /**
   * Tag for report location.
   */
  reportTag: string | null;

  /**
   * Number of loaded rows.
   */
  loadedCount: number;

  /**
   * Number of total rows to load.
   */
  totalCount: number;

  filteredColumns: (TableColumn<OBJECT> & TableColumnState)[];

  sorts: TableSort[];
  filters: TableFilter[];
  filtersState: TableFilterState[];

  /**
   * Toggles row selection by id.
   */
  toggleRowSelection: (id: string) => void;

  toggleAllRowSelection: () => void;

  resetSelection: () => void;

  activeRow: string | null;

  setActiveRow: (id: string | null) => void;

  setPrevRowActive: () => void;

  setNextRowActive: () => void;

  /**
   * Refreshes table data.
   */
  refresh: () => void;

  /**
   * Opens column settings dialog.
   */
  openColumnDialog: () => void;

  /**
   * Opens report dialog.
   */
  openReportDialog: () => void;

  /**
   * Closes column settings dialog.
   */
  closeColumnDialog: () => void;

  /**
   * Opens filter dialog.
   */
  openFilterDialog: () => void;

  /**
   * Closes filter dialog.
   */
  closeFilterDialog: () => void;

  /**
   * Changes columns state.
   */
  setColumnsState: (columnsState: TableColumnState[]) => void;

  /**
   * Changes columns state.
   */
  setFiltersState: (filtersState: TableFilterState[]) => void;

  /**
   * Cycles through ASC, DESC, OFF sort on specified column.
   */
  toggleSortColumn: (datakey: string) => void;

  searchQuery: string;

  /**
   * Debounced search query callback.
   */
  setSearchQuery: (q: string) => void;

  /**
   * Resets sorts to default
   */
  resetSorts: () => void;
}

export interface TableSort {
  /**
   * Datakey corresponding to a column datakey.
   */
  datakey: string | undefined;

  /**
   * Real field for api call construction.
   */
  field: string;

  type: 'FIELD' | 'GEO_DISTANCE' | 'SCRIPT' | 'SCORE';

  order: 'ASC' | 'DESC';
}

/**
 * Filter operation.
 */
export enum TableFilterOperation {
  'EQ' = 'EQ',
  'START_WITH' = 'START_WITH',
  'END_WITH' = 'END_WITH',
  'CONTAINS' = 'CONTAINS',
  'NOT_NULL' = 'NOT_NULL',
  'IS_NULL' = 'IS_NULL',
  'GT' = 'GT',
  'LT' = 'LT',
  'GTE' = 'GTE',
  'LTE' = 'LTE',
  'FTX' = 'FTX',
  'FTXF' = 'FTXF',
  'OR' = 'OR',
  'AND' = 'AND',
}

/**
 * Table filter definition.
 */
export interface TableFilter {
  /**
   * Default enabled flag.
   */
  enabled: boolean;

  /**
   * Filter label.
   */
  label: string;

  /**
   * Filter field.
   */
  filterkey: string;

  /**
   * Filter operation.
   */
  operation: TableFilterOperation;

  /**
   * Component for rendering filter item.
   */
  FilterComponent: ComponentType<FilterComponentProps>;

  /**
   * Default filter value.
   */
  value: any;
}

/**
 * State of the filter.
 */
export interface TableFilterState {
  /**
   * Enabled flag.
   */
  enabled: boolean;

  /**
   * Operation of the filter.
   *
   */
  operation: TableFilterOperation;

  /**
   * Value of the filter.
   */
  value: any;

  /**
   * Nested filters.
   *
   * Use Api filters directly
   *
   * FilterComponent can use it.
   */
  filters?: Filter[];

  /**
   * Custom attributes.
   */
  [x: string]: any;
}

export type TableFilterWithState = TableFilter & TableFilterState;

/**
 * Filter dialog item properties.
 */
export interface TableFilterDialogItemProps {
  /**
   * Filter.
   */
  filter: TableFilter;

  /**
   * Filter state.
   */
  state: TableFilterState;

  /**
   * OnClick handler for enabling checkbox.
   */
  onToggle: () => void;

  /**
   * OnChange handler for filter value.
   */
  onChangeValue: (value: any) => void;

  /**
   * OnChange handler for complex filter value.
   */
  onChangeFilterState: (state: TableFilterState) => void;
}
