import { ReactNode } from 'react';

export interface SnackbarProviderProps {
  timeout: number;
}

/**
 * Variant of snackbar.
 */
export enum SnackbarVariant {
  SUCCESS = 'success',
  WARNING = 'warning',
  ERROR = 'error',
  INFO = 'info',
  BLANK = 'blank',
}

export interface SnackbarHandle {
  showSnackbar: (
    msg: ReactNode,
    variant: SnackbarVariant,
    autohide?: boolean
  ) => void;
}
