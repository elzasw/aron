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
}

export interface SnackbarHandle {
  showSnackbar: (msg: string, variant: SnackbarVariant) => void;
}
