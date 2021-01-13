import * as React from 'react';
import { ReportProviderProps } from './report-types';
import { ReportContext } from './report-context';

export function ReportSystemProvider({
  children,
  url,
}: React.PropsWithChildren<ReportProviderProps>) {
  const context = React.useMemo(
    () => ({
      url,
    }),
    [url]
  );

  return (
    <ReportContext.Provider value={context}>{children}</ReportContext.Provider>
  );
}
