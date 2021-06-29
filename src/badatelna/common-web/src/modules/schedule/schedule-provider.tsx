import * as React from 'react';
import { ScheduleProviderProps } from './schedule-types';
import { ScheduleContext } from './schedule-context';

export function ScheduleProvider({
  children,
  jobUrl,
  runUrl,
}: React.PropsWithChildren<ScheduleProviderProps>) {
  const context: ScheduleContext = React.useMemo(
    () => ({
      jobUrl,
      runUrl,
    }),
    [jobUrl, runUrl]
  );

  return (
    <ScheduleContext.Provider value={context}>
      {children}
    </ScheduleContext.Provider>
  );
}
