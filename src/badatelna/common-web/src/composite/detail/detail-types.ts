import { ComponentType, ReactNode } from 'react';
import * as Yup from 'yup';
import { CrudSource, DomainObject } from 'common/common-types';
import { ValidationError } from 'composite/form/form-types';

export interface DetailToolbarProps {
  before?: ReactNode;
  after?: ReactNode;
}

export interface DetailProps<OBJECT extends DomainObject> {
  /**
   * External data source.
   */
  source: CrudSource<OBJECT>;

  ToolbarComponent?: ComponentType<DetailToolbarProps>;

  ContainerComponent?: ComponentType;
  FieldsComponent?: ComponentType;
  GeneralFieldsComponent?: ComponentType;

  validationSchema?: Yup.Schema<OBJECT | undefined>;

  /**
   * Callback fired when data from detail are saved or deleted.
   */
  onPersisted?: (id: string | null) => void;

  toolbarProps?: DetailToolbarProps;

  initNewItem?: () => OBJECT;
}

export type RefreshListener = () => void;

export interface DetailHandle<OBJECT extends DomainObject> {
  source: CrudSource<OBJECT>;
  isExisting: boolean;
  mode: DetailMode;
  errors: ValidationError[];

  setActive: (id: string | null) => void;
  refresh: () => void;

  /**
   * Refreshes detaild and grid.
   */
  refreshAll: () => void;
  onPersisted: (id: string | null) => void;

  startNew: (data?: OBJECT) => void;
  startEditing: () => void;
  cancelEditing: () => void;
  validate: () => void;
  save: () => void;
  del: () => void;

  /**
   * Adds new listener, which will be called during refresh.
   *
   * @param listener A reference stable listener
   */
  addRefreshListener: (listener: RefreshListener) => void;

  /**
   * Removes existing listener, which is called during refresh.
   *
   * @param listener A reference stable listener
   */
  removeRefreshListener: (listener: RefreshListener) => void;
}

/**
 * Toolbar button type.
 */
export enum DetailToolbarButtonType {
  NORMAL,
  PRIMARY,
  SECONDARY,
}

/**
 * Toolbar button properties.
 */
export interface DetailToolbarButtonProps {
  /**
   * Label.
   *
   * Can be string or rendered component.
   */
  label: ReactNode;

  /**
   * Button onClick handler, with custom values.
   */
  onClick: () => void;

  /**
   * Optional tooltip.
   */
  tooltip?: ReactNode;

  /**
   * Type of button.
   *
   * Defaults to NORMAL.
   */
  type?: DetailToolbarButtonType;

  /**
   * Disabled flag.
   */
  disabled?: boolean;

  startIcon?: ReactNode;
  endIcon?: ReactNode;
}

export interface DetailToolbarButtonMenuItem {
  label: ReactNode;
  tooltip?: ReactNode;
  onClick: () => void;
}

/**
 * Toolbar button menu properties.
 */
export interface DetailToolbarButtonMenuProps {
  /**
   * Label.
   *
   * Can be string or rendered component.
   */
  label: ReactNode;

  /**
   * Optional tooltip.
   */
  tooltip?: ReactNode;

  /**
   * Type of button.
   *
   * Defaults to NORMAL.
   */
  type?: DetailToolbarButtonType;

  /**
   * Disabled flag.
   */
  disabled?: boolean;

  items: DetailToolbarButtonMenuItem[];

  startIcon?: ReactNode;
}

export enum DetailMode {
  NONE,
  VIEW,
  EDIT,
  NEW,
}
