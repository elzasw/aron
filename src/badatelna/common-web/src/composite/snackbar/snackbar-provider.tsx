import React, { PropsWithChildren } from 'react';
import { SnackbarContext } from './snackbar-context';
import { useSnackbar } from './snackbar-hook';
import { SnackbarComponent } from './snackbar-component';
import { SnackbarProviderProps } from './snackbar-types';

/**
 * Main component for snackbar providing context and rendering children + snackbar.
 */
export function SnackbarProvider({
  children,
  timeout,
}: PropsWithChildren<SnackbarProviderProps>) {
  const { context, ref } = useSnackbar();

  return (
    <SnackbarContext.Provider value={context}>
      {children}
      <SnackbarComponent ref={ref} timeout={timeout} />
    </SnackbarContext.Provider>
  );
}
