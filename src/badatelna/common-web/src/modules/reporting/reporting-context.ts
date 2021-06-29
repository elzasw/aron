import { createContext } from 'react';
import { ReportDefinition, Report } from './reporting-types';

export interface ReportingContext {
  url: string;

  loadDefinitions: () => Promise<ReportDefinition[]>;
  generate: (id: string, input: any) => Promise<Report>;
}

export const ReportingContext = createContext<ReportingContext>(
  undefined as any
);
