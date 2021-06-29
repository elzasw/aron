import * as React from 'react';
import { ExportProviderProps } from './export-types';
import { ExportContext } from './export-context';

export function ExportProvider({
  children,
  url,
  tags,
  disableSync = false,
  disableAsync = false,
}: React.PropsWithChildren<ExportProviderProps>) {
  const context: ExportContext = React.useMemo(
    () => ({
      url,
      tags,
      disableSync,
      disableAsync,
    }),
    [url, tags, disableAsync, disableSync]
  );

  return (
    <ExportContext.Provider value={context}>{children}</ExportContext.Provider>
  );
}
