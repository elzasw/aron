import { ComponentType, RefAttributes, ElementType, ReactNode } from 'react';
import { SvgIconProps } from '@material-ui/core';
import { DialogHandle } from 'components/dialog/dialog-types';

/**
 * Properties of table cell component.
 */
export interface TableFieldCellProps<TObject> {
  /**
   * Column in which this cell is rendered.
   */
  column: TableFieldColumn<TObject>;

  /**
   * Data from RowData for this cell.
   */
  value: any;

  /**
   * All data of row.
   */
  rowValue: TObject;

  /**
   * Row number.
   */
  index: number;
}

/**
 * Table column definition.
 */
export interface TableFieldColumn<TObject> {
  /**
   * Label of the column shown in header component.
   */
  name: string;

  /**
   * Attribute of the RowData object.
   */
  datakey: string;

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
   * Custom cell component.
   *
   * Defaults to simple text/number component.
   */
  CellComponent?: ComponentType<TableFieldCellProps<TObject>>;
}

export interface TableFieldColumnState {
  /**
   * Attribute of the RowData object.
   */
  datakey: string;

  /**
   * Width of the column.
   */
  width: number;
}

export interface TableFieldRowProps<TObject> {
  /**
   * Row data.
   */
  value: TObject;

  /**
   * Row number, starting from 0.
   */
  index: number;

  /**
   * Selected flag.
   */
  selected: boolean;
}

/**
 * Properties of table form fields component.
 */
export interface TableFieldFormFieldsProps<TObject> {
  /**
   * Existing row data or null if dialog should use defaultData.
   */
  initialValue: TObject | null;
}

export interface TableFieldRemoveDialogProps {
  index: number;
}

/**
 * Properties of table dialog component.
 */
export interface TableFieldDialogProps<TObject> {
  /**
   * Index of edited row or undefined.
   */
  index: number | undefined;

  /**
   * Existing row data or null if dialog should use initNewRow.
   */
  value: TObject | null;

  /**
   * Custom form fields component.
   *
   * Needs to be provided if edit is required. No default implementation is used.
   */
  FormFieldsComponent: ComponentType<TableFieldFormFieldsProps<TObject>>;
}

export interface TableFieldToolbarProps {
  selectedIndex: number | undefined;
}

export interface TableFieldProps<TObject> {
  /**
   * Custom toolbar component.
   *
   * Defaults to toolbar with Add, Edit and Remove buttons.
   * Buttons are shown if allowAdd, allowEdit or allowRemove is specified.
   */
  ToolbarComponent?: ComponentType<TableFieldToolbarProps>;

  /**
   * Custom header component.
   *
   * Defaults to component rendering one cell for every column + one empty cell row selection.
   */
  HeaderComponent?: ComponentType<RefAttributes<HTMLDivElement>>;

  /**
   * Custom row component.
   *
   * Defaults to component rendering one cell for every column + radio for row selection.
   */
  RowComponent?: ComponentType<TableFieldRowProps<TObject>>;

  /**
   * Custom dialog component.
   *
   * Defaults to Dialog which uses FormFieldsComponent and adds buttons for Save and Cancel.
   */
  DialogComponent?: ComponentType<
    TableFieldDialogProps<TObject> & RefAttributes<DialogHandle>
  >;

  /**
   * Custom remove dialog component.
   *
   * Defaults to Dialog which contains a simple confirmation message.
   */
  RemoveDialogComponent?: ComponentType<
    TableFieldRemoveDialogProps & RefAttributes<DialogHandle>
  >;

  /**
   * Custom form fields component.
   *
   * Needs to be provided if edit is required. No default implementation is used.
   */
  FormFieldsComponent?: ComponentType<TableFieldFormFieldsProps<TObject>>;

  /**
   * Columns of the table.
   */
  columns: TableFieldColumn<TObject>[];

  /**
   * Sets the component to read-only mode.
   */
  disabled?: boolean;

  /**
   * Disables the Add button.
   */
  disabledAdd?: boolean;

  /**
   * Disables the Edit button.
   */
  disabledEdit?: boolean;

  /**
   * Disables the Remove button.
   */
  disabledRemove?: boolean;

  /**
   * Allows adding of new row.
   */
  visibleAdd?: boolean;

  /**
   * Allows editing of existing row.
   */
  visibleEdit?: boolean;

  /**
   * Allows removing of existing row.
   */
  visibleRemove?: boolean;

  /**
   * Custom factory method for new row creation.
   *
   * Defaults to empty object with new id.
   */
  initNewRow?: () => TObject;

  /**
   * Rows data.
   */
  value: TObject[] | null | undefined;

  /**
   * Shows the toolbar.
   */
  showToolbar?: boolean;

  /**
   * Change handler.
   */
  onChange: (value: TObject[]) => void;

  /**
   * Select handler
   */
  onSelect?: (row: TObject | null) => void;

  /**
   * Condition handler for displaying radio in row.
   */
  showRadioCond?: (item: TObject) => boolean;

  /**
   * Condition handler for displaying detail button in row.
   */
  showDetailBtnCond?: (item: TObject) => boolean;

  /**
   * Maximum number of rows to show.
   */
  maxRows?: number;
}

export interface TableFieldToolbarButtonProps {
  show: boolean;
  title: ReactNode;
  IconComponent: React.ComponentType<SvgIconProps>;
  disabled: boolean;
  onClick?: () => void;
  component?: ElementType<any>;
  href?: string;
}
