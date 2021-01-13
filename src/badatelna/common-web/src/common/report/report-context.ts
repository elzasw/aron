import { createContext } from 'react';

export interface ReportContext {
  url: string;
}

export const ReportContext = createContext<ReportContext>(undefined as any);
