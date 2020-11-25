import { useMemo, useRef } from 'react';
import { SnackbarContext } from './snackbar-context';
import { SnackbarHandle } from './snackbar-types';

export function useSnackbar() {
  const ref = useRef<SnackbarHandle>(null);

  const context: SnackbarContext = useMemo(
    () => ({
      showSnackbar: (msg, variant) => ref.current?.showSnackbar(msg, variant),
    }),
    []
  );

  return { context, ref };
}
