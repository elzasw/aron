import * as React from 'react';
import {
  ReportingProviderProps,
  ReportDefinition,
  Report,
} from './reporting-types';
import { ReportingContext } from './reporting-context';
import { useEventCallback } from 'utils/event-callback-hook';
import { AbortableFetch } from 'utils/abortable-fetch';
import { listReportDefinitions, generateReport } from './reporting-api';

export function ReportingProvider({
  children,
  url,
}: React.PropsWithChildren<ReportingProviderProps>) {
  const loadFetch = React.useRef<AbortableFetch | null>(null);
  const generateFetch = React.useRef<AbortableFetch | null>(null);

  const loadDefinitions = useEventCallback(async () => {
    if (loadFetch.current !== null) {
      loadFetch.current.abort();
    }

    loadFetch.current = listReportDefinitions(url);

    const definitions: ReportDefinition[] = await loadFetch.current.json();
    return definitions;
  });

  const generate = useEventCallback(async (id: string, input: any) => {
    if (generateFetch.current !== null) {
      generateFetch.current.abort();
    }

    generateFetch.current = generateReport(url, id, input ?? {});

    const report: Report = await generateFetch.current.json();
    return report;
  });

  const context: ReportingContext = React.useMemo(
    () => ({
      url,
      loadDefinitions,
      generate,
    }),
    [url, loadDefinitions, generate]
  );

  return (
    <ReportingContext.Provider value={context}>
      {children}
    </ReportingContext.Provider>
  );
}
