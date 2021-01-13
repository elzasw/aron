import { createContext } from 'react';
import { TableFieldColumn, TableFieldColumnState } from './table-field-types';

export interface TableFieldContext<TObject> {
  /**
   * Table is in read-only mode.
   */
  disabled: boolean;

  /**
   * Adding is didabled.
   */
  disabledAdd: boolean;

  /**
   * Editing is disabled.
   */
  disabledEdit: boolean;

  /**
   * Removing is disabled.
   */
  disabledRemove: boolean;

  /**
   * Add button should be visible.
   */
  visibleAdd: boolean;

  /**
   * Edit button should be visible.
   */
  visibleEdit: boolean;

  /**
   * Remove button should be visible.
   */
  visibleRemove: boolean;

  /**
   * Select handler
   */
  onSelect?: (row: TObject | null, index: number) => void;

  /**
   * Shows empty dialog.
   */
  showAddDialog: () => void;

  /**
   * Shows dialog with data of specified row.
   */
  showEditDialog: (index: number) => void;

  /**
   * Triggers Remove action.
   */
  showRemoveDialog: (index: number) => void;

  /**
   * Click handler for View button.
   */
  showDetailDialog: (index: number) => void;

  /**
   * Handler for row selection.
   */
  selectRow: (index: number) => void;

  /**
   * Removes the specified row by index.
   */
  removeRow: (index: number) => void;

  /**
   * Replaces row at specified index with new data or inserts at the end.
   */
  saveRow: (index: number | undefined, item: TObject) => void;

  /**
   * OnChange handler for width passing down current datakey and new width.
   */
  setColumnWidth: (datakey: string, width: number) => void;

  /**
   * Condition handler for displaying radio in row.
   */
  showRadioCond: (item: TObject) => boolean;

  /**
   * Condition handler for displaying detail button in row.
   */
  showDetailBtnCond: (item: TObject) => boolean;

  /**
   * Columns of the table.
   */
  columns: TableFieldColumn<TObject>[];

  columnsState: TableFieldColumnState[];

  filteredColumns: (TableFieldColumn<TObject> & TableFieldColumnState)[];

  value: TObject[];

  /**
   * Custom factory method for new row creation.
   *
   * Defaults to empty object with new id.
   */
  initNewRow: () => TObject;
}

export const TableFieldContext = createContext<TableFieldContext<any>>(
  undefined as any
);
