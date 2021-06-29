import {
  ComponentType,
  ReactNode,
  RefAttributes,
  Dispatch,
  SetStateAction,
} from 'react';
import * as Yup from 'yup';
import { DialogHandle } from 'components/dialog/dialog-types';
import { ScrollableSource, Filter, Params } from 'common/common-types';
import { BulkAction } from './bulk-action-menu/bulk-action-types';
import { AbortableFetch } from 'utils/abortable-fetch';
import { Callback } from 'composite/prompt/prompt-types';
import { ExportDialogProps } from 'modules/export/components/dialog/export-dialog/export-dialog-types';

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

export interface TableColumnValueMapper<OBJECT, FIELD = any> {
  (args: {
    column: TableColumn<OBJECT, FIELD>;
    rowValue: OBJECT;
    value: FIELD;
    sorts: TableSort[];
  }): FIELD;
  displayName?: string;
}

export interface TableColumn<OBJECT, FIELD = any> {
  /**
   * Label of the column shown in header component.
   */
  name: string;

  /**
   * Attribute of the RowData object.
   */
  datakey: string;

  /**
   * Display key which will be used in displaying the value.
   *
   * Defaults to datakey.
   */
  displaykey?: string;

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
   * Specifies in which group filter control belongs.
   *
   * Defualts to 0.
   */
  filterGroup?: number;

  /**
   * Specifies order of filter control inside of group.
   *
   * Defualts to 0.
   */
  filterOrder?: number;

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

  /**
   * The URL to link to when the button is clicked.
   */
  href?: string;

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

  className?: string;
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
   * Custom export dialog component.
   */
  ExportDialogComponent?: ComponentType<
    ExportDialogProps & RefAttributes<DialogHandle>
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
   * Additional filters to datasource.
   */
  defaultPreFilters?: Filter[];

  /**
   * Modifies the source to return items of specific states
   */
  include?: string[];

  /**
   * Name of the table to show.
   */
  tableName?: string | null;

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
   * Show named settings button.
   *
   * Default: false.
   */
  showNamedSettingsButton?: boolean;

  /**
   * Show bulk action button.
   */
  showBulkActionButton?: boolean;

  /**
   * Show export button.
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
  tableName: string | null;

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
   * Disable named settings button.
   */
  disabledNamedSettingsButton: boolean;

  /**
   * Disable bulk action button.
   */
  disabledBulkActionButton: boolean;

  /**
   * Disable reset sorts button.
   */
  disabledResetSortsButton: boolean;

  /**
   * Disable export action button.
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
   * Show named settings button.
   *
   * Default: false.
   */
  showNamedSettingsButton: boolean;

  /**
   * Show export button.
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
   * Tag for export location.
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
   * Additional filters to datasource.
   */
  preFilters: Filter[];
  setPreFilters: Dispatch<SetStateAction<Filter[]>>;

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
   * Opens export dialog.
   */
  openExportDialog: () => void;

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

  /**
   * Sets sorts.
   */
  setSorts: (sorts: TableSort[]) => void;
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

export interface TableToolbarButtonActionProps<T = unknown> {
  promptKey: string;
  buttonLabel: ReactNode;
  buttonTooltip?: ReactNode;

  dialogTitle: string;
  dialogText: string;
  dialogWidth?: number;

  FormFields?: ComponentType<{ onConfirm?: Callback; onCancel?: () => void }>;
  formValidationSchema?: Yup.Schema<T>;
  formInitialValues?: T;

  successMessage?: string;
  errorMessage?: string;

  apiCall: (
    params: Params,
    selected: string[],
    formData?: any
  ) => AbortableFetch;

  /**
   * Success callback.
   *
   * default: refresh CRUD source and call onPersisted from DetailContext.
   */
  onSuccess?: () => Promise<void>;

  onError?: (err: Error) => Promise<void>;

  /**
   * Callback handling the JSON result of apiCall.
   */
  onResult?: (result: any) => Promise<any>;

  /**
   * Callback called to determine if the button should be shown.
   * This is called on top of modes property handling.
   *
   * Default: show allways
   */
  onShouldShow?: () => boolean;

  /**
   * Customize button component.
   */
  ButtonComponent?: ComponentType<TableToolbarButtonProps>;

  /**
   * Set default props.
   */
  buttonProps?: Partial<TableToolbarButtonProps>;
}
