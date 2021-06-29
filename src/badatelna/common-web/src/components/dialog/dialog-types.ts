import { ReactNode, ReactNodeArray } from 'react';

export interface DialogProps {
  title: ReactNode;
  onConfirm?: () => Promise<boolean | void> | boolean | void;
  onCancel?: () => void;
  onShow?: () => void;

  /**
   * Fires after the children were mounted
   */
  onShown?: () => void;

  showConfirm?: boolean;
  confirmLabel?: ReactNode;

  showClose?: boolean;
  closeLabel?: ReactNode;

  actions?: ReactNodeArray;

  loading?: boolean;

  disableBackdrop?: boolean;

  /**
   * Render prop method. Used instead of direct children to conditionaly render the content.
   */
  children: () => ReactNode;
}

export interface DialogHandle {
  open: () => void;
  close: () => void;
}
