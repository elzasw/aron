import { createContext } from 'react';
import { SnackbarHandle } from './snackbar-types';

export type SnackbarContext = SnackbarHandle;

export const SnackbarContext = createContext<SnackbarContext>({
  showSnackbar: () => {
    console.error(
      'Snackbar context is not created. Wrap your application with SnackbarProvider.'
    );
  },
});
