import { ComponentType, ReactNode, Dispatch, SetStateAction } from 'react';
import * as Yup from 'yup';
import { CrudSource, DomainObject } from 'common/common-types';
import { FormHandle, ValidationError } from 'composite/form/form-types';
import { AbortableFetch } from 'utils/abortable-fetch';
import { Callback } from 'composite/prompt/prompt-types';

export type DetailToolbarButtonName = 'NEW' | 'EDIT' | 'REMOVE';

export interface DetailToolbarProps<OBJECT extends DomainObject> {
  before?: ReactNode;
  after?: ReactNode;

  showButton?: ({
    button,
    source,
  }: {
    button: DetailToolbarButtonName;
    source: CrudSource<OBJECT>;
  }) => boolean;
  disableButton?: ({
    button,
    source,
  }: {
    button: DetailToolbarButtonName;
    source: CrudSource<OBJECT>;
  }) => boolean;
}

export interface DetailProps<OBJECT extends DomainObject> {
  /**
   * External data source.
   */
  source: CrudSource<OBJECT>;

  ToolbarComponent?: ComponentType<DetailToolbarProps<OBJECT>>;

  ContainerComponent?: ComponentType;
  FieldsComponent?: ComponentType;
  GeneralFieldsComponent?: ComponentType;

  validationSchema?: Yup.Schema<OBJECT | undefined>;

  /**
   * Callback fired when data from detail are saved or deleted.
   */
  onPersisted?: (id: string | null) => void;

  toolbarProps?: DetailToolbarProps<OBJECT>;

  initNewItem?: () => OBJECT;

  /**
   * Shows error panel on top of form.
   *
   * Default: false
   */
  showErrorPanel?: boolean;

  /**
   * Resets scrollbar position
   *
   * Default: false
   */
  resetScrollbarPosition?: boolean;
}

export type RefreshListener = () => void;

export interface DetailHandle<OBJECT extends DomainObject> {
  source: CrudSource<OBJECT>;
  isExisting: boolean;
  mode: DetailMode;
  setMode: Dispatch<SetStateAction<DetailMode>>;
  formRef?: FormHandle<OBJECT> | null;
  showErrorPanel: boolean;

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
  validate: () => Promise<ValidationError[]>;
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
   * Hypertext reference.
   */
  href?: string;

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
  Icon?: ReactNode;
  divider?: boolean;
  warning?: boolean;
  onClick: () => void;
  href?: string;
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

export interface DetailToolbarButtonActionProps<T = unknown> {
  promptKey: string;
  buttonLabel: ReactNode;
  buttonTooltip?: ReactNode;

  buttonDisabled?: boolean;

  dialogTitle: string;
  dialogText: string;
  dialogWidth?: number;

  FormFields?: ComponentType<{ onConfirm?: Callback; onCancel?: () => void }>;
  formValidationSchema?: Yup.Schema<T>;
  formInitialValues?: T;

  successMessage?: string;
  errorMessage?: string;

  apiCall: (id: string, formData?: any) => AbortableFetch;

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
   * Show the action button only in this views.
   *
   * Default: VIEW
   */
  modes?: DetailMode[];

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
  ButtonComponent?: ComponentType<DetailToolbarButtonProps>;

  /**
   * Set default props.
   */
  buttonProps?: Partial<DetailToolbarButtonProps>;
}
